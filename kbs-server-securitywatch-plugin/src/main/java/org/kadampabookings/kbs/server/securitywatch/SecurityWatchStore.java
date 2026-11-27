package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.db.query.QueryArgumentBuilder;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;
import dev.webfx.stack.db.submit.SubmitArgumentBuilder;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.SubmitService;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.util.function.Supplier;

/**
 * Every statement on {@code security_watch_run}, run AS THE SERVER: raw SQL (no DQL — the table is deliberately not
 * in the domain model, so no client query can reach it), with the caller's state removed from the thread first, so
 * nothing downstream that reads a caller's state attributes the statement to them.
 *
 * <p>Every value is bound; the statements are constants. Instants are formatted by the database as
 * {@code 2026-11-26T06:02:11Z} (UTC, whole seconds), so the replies do not depend on how a driver maps timestamps.
 * The report is read as JSON text and handed on as it is — it was written by this server, from a value it built.
 *
 * @author Claude Code
 */
final class SecurityWatchStore {

    private SecurityWatchStore() {
    }

    /** A run still running after this long is taken to have died with its server, and marked failed (timeout). */
    static final String RUN_EXPIRY = "30 minutes";
    /** A manual check is refused this soon after the last run started, whatever its trigger or status. */
    static final String MANUAL_MIN_GAP = "10 minutes";
    /** A scheduled check starts only when no run of any kind started within this window (the hourly tick's margin). */
    static final String SCHEDULE_MIN_GAP = "55 minutes";
    /** The hour of the day, UTC, from which a new day's scheduled run is due. */
    static final int SCHEDULE_HOUR_UTC = 5;

    /** The most recent 05:00 UTC, as a timestamptz: today's once it has passed, else yesterday's. */
    static final String LAST_DAILY_START =
        "(case when (now() at time zone 'UTC') >= date_trunc('day', now() at time zone 'UTC') + interval '" + SCHEDULE_HOUR_UTC + " hours'"
        + " then date_trunc('day', now() at time zone 'UTC') + interval '" + SCHEDULE_HOUR_UTC + " hours'"
        + " else date_trunc('day', now() at time zone 'UTC') - interval '" + (24 - SCHEDULE_HOUR_UTC) + " hours' end) at time zone 'UTC'";
    /** Runs older than this are deleted. */
    static final String RETENTION = "400 days";

    /** The error code of a run marked failed because it outlived {@link #RUN_EXPIRY}. */
    static final String ERROR_TIMEOUT = "timeout";

    static final String STATUS_RUNNING = "running";
    static final String STATUS_SUCCEEDED = "succeeded";
    static final String STATUS_FAILED = "failed";
    static final String TRIGGER_SCHEDULE = "schedule";
    static final String TRIGGER_MANUAL = "manual";

    private static String instant(String column) {
        return "to_char(" + column + " at time zone 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')";
    }

    /** The latest SUCCEEDED run's report: the page keeps showing it while a later run runs or after one failed. */
    private static final String LATEST_REPORT =
        "select report from security_watch_run where status = 'succeeded' and report is not null"
        + " order by started_at desc, id desc limit 1";

    /**
     * One row, always: the latest run of any status (all its columns null when none ever ran), and the latest
     * succeeded report as JSON text (null when none).
     */
    static final String STATUS_SQL =
        "select r.id, r.trigger, r.status, " + instant("r.started_at") + " as started_at, " + instant("r.finished_at")
        + " as finished_at, r.error, (select s.report::text from (" + LATEST_REPORT + ") s) as report"
        + " from (select 1) one left join lateral"
        + " (select * from security_watch_run order by started_at desc, id desc limit 1) r on true";

    /**
     * One row, always: the latest succeeded report's {@code generatedAt} (null when none), and its actions reduced to
     * the three fields the home screen counts, as a JSON array in the report's order ({@code []} when none). Extracted
     * by the database, so nothing else of the report leaves it on this path.
     */
    static final String SUMMARY_SQL =
        "select r.report->>'generatedAt' as generated_at,"
        + " coalesce((select jsonb_agg(jsonb_build_object('severity', a.value->'severity', 'dueDate', a.value->'dueDate',"
        + " 'knownExploited', a.value->'knownExploited') order by a.ordinality)"
        + " from jsonb_array_elements(case when jsonb_typeof(r.report->'actions') = 'array'"
        + " then r.report->'actions' else '[]'::jsonb end) with ordinality a), '[]'::jsonb)::text as actions"
        + " from (select 1) one left join lateral (" + LATEST_REPORT + ") r on true";

    /**
     * The first step of every claim: a row still running after {@link #RUN_EXPIRY} is marked failed. Its count is read
     * by the insert below, which makes the database finish this update before it inserts — so the expired row has left
     * the one-running index when the new one meets it.
     */
    private static final String EXPIRE_CTE =
        "with expired as (update security_watch_run set status = 'failed', finished_at = now(), error = '" + ERROR_TIMEOUT + "'"
        + " where status = 'running' and started_at < now() - interval '" + RUN_EXPIRY + "' returning id),"
        // What stood before this statement: a live running row (not about to expire), and the last start of any run
        + " before as (select count(*) filter (where status = 'running' and started_at >= now() - interval '" + RUN_EXPIRY + "') as running,"
        + " max(started_at) as last_started from security_watch_run),";

    /** Inserts the running row unless another one is running; a concurrent insert that wins makes this one do nothing. */
    private static final String ON_CONFLICT = " on conflict ((true)) where status = 'running' do nothing returning id)";

    /**
     * A manual start ($1 = the requesting person id): answers the new run's id as text, or {@code busy} (a run is
     * running, or another start won the race) or {@code too-soon} (the last run started under {@link #MANUAL_MIN_GAP}
     * ago). One statement, atomic on its own.
     */
    static final String CLAIM_MANUAL_SQL =
        EXPIRE_CTE
        + " inserted as (insert into security_watch_run (trigger, status, requested_by_person_id)"
        + " select 'manual', 'running', $1::int from before, (select count(*) from expired) e"
        + " where before.running = 0"
        + " and (before.last_started is null or before.last_started <= now() - interval '" + MANUAL_MIN_GAP + "')"
        + ON_CONFLICT
        + " select coalesce((select id::text from inserted), case"
        + " when (select running from before) > 0 then 'busy'"
        + " when (select last_started from before) > now() - interval '" + MANUAL_MIN_GAP + "' then 'too-soon'"
        + " else 'busy' end)";

    /**
     * A scheduled start (the contract's "When a run starts"): answers the new run's id as text, or null when this tick
     * has nothing to do — a run (of any trigger) has SUCCEEDED since the most recent 05:00 UTC, a run of any kind
     * started within {@link #SCHEDULE_MIN_GAP}, a run is running, or another task's start won the race. So one run a day
     * shortly after 05:00 UTC, a failed run retried at the next hourly tick, and several server tasks ticking together
     * still produce one run. One statement.
     */
    static final String CLAIM_SCHEDULE_SQL =
        EXPIRE_CTE
        + " inserted as (insert into security_watch_run (trigger, status)"
        + " select 'schedule', 'running' from before, (select count(*) from expired) e"
        + " where before.running = 0"
        + " and not exists (select 1 from security_watch_run where status = 'succeeded' and finished_at >= " + LAST_DAILY_START + ")"
        + " and not exists (select 1 from security_watch_run where started_at > now() - interval '" + SCHEDULE_MIN_GAP + "')"
        + ON_CONFLICT
        + " select (select id::text from inserted)";

    /** The report of the latest succeeded run other than $1, as JSON text (null when none): what the urgent mail compares with. */
    static final String PREVIOUS_REPORT_SQL =
        "select report::text from security_watch_run where status = 'succeeded' and report is not null and id <> $1::int"
        + " order by started_at desc, id desc limit 1";

    /**
     * Ends a run ($1 id, $2 status, $3 error code or null, $4 report JSON or null) — only while it is still running:
     * a run another start has already marked failed (timeout) stays failed. Answers the id when it was written.
     *
     * <p>{@code $4::text::jsonb}, not {@code $4::jsonb}: the driver infers a parameter's type from its cast, and a
     * Java String bound to a jsonb parameter is stored as one JSON string holding the text, not as the object. psql
     * binds text, so a rehearsal there does not show it (found on the first run inside a real server, 27 Nov 2026).
     */
    static final String FINISH_SQL =
        "update security_watch_run set status = $2::text, finished_at = now(), error = $3::text, report = $4::text::jsonb"
        + " where id = $1::int and status = 'running' returning id";

    /** Deletes the finished runs older than {@link #RETENTION}; answers how many. */
    static final String PRUNE_SQL =
        "with deleted as (delete from security_watch_run where status <> 'running'"
        + " and started_at < now() - interval '" + RETENTION + "' returning 1) select count(*) from deleted";

    /** The database's version, as it reports it. */
    static final String POSTGRES_VERSION_SQL = "select current_setting('server_version')";

    // ===== The rows, as the endpoints read them ======================================================================

    /** The latest run (null fields when none ever ran) and the latest succeeded report as JSON text. */
    record Status(Run run, String reportJson) {
    }

    /** One run, as the status reply shows it. */
    record Run(Integer id, String trigger, String status, String startedAt, String finishedAt, String error) {
    }

    /** The summary's projection: the report's generatedAt and the actions' JSON array. */
    record Summary(String generatedAt, String actionsJson) {
    }

    /** What a claim found: the new run's id, or why there is none ({@code busy}, {@code too-soon}, or null: skip). */
    record Claim(Integer runId, String refusal) {
        static final String BUSY = "busy";
        static final String TOO_SOON = "too-soon";
    }

    static Future<Status> status() {
        return query(STATUS_SQL).map(SecurityWatchStore::statusOf);
    }

    static Status statusOf(QueryResult rows) {
        if (rows == null || rows.getRowCount() == 0)
            return new Status(null, null);
        Integer id = Numbers.toInteger(rows.getValue(0, 0));
        Run run = id == null ? null : new Run(id, text(rows.getValue(0, 1)), text(rows.getValue(0, 2)),
            text(rows.getValue(0, 3)), text(rows.getValue(0, 4)), text(rows.getValue(0, 5)));
        return new Status(run, text(rows.getValue(0, 6)));
    }

    static Future<Summary> summary() {
        return query(SUMMARY_SQL).map(rows -> rows == null || rows.getRowCount() == 0 ? new Summary(null, "[]")
            : new Summary(text(rows.getValue(0, 0)), orEmptyArray(text(rows.getValue(0, 1)))));
    }

    /** A manual start for {@code personId}; see {@link #CLAIM_MANUAL_SQL}. */
    static Future<Claim> claimManual(Integer personId) {
        return firstKey(CLAIM_MANUAL_SQL, personId)
            .map(SecurityWatchStore::claimOf)
            // The one-running index, should the statement ever meet it as an error rather than a conflict it handles
            .recover(cause -> isOneRunningCollision(cause) ? Future.succeededFuture(new Claim(null, Claim.BUSY))
                : Future.failedFuture(cause));
    }

    /** A scheduled start; see {@link #CLAIM_SCHEDULE_SQL}. */
    static Future<Claim> claimSchedule() {
        return firstKey(CLAIM_SCHEDULE_SQL)
            .map(SecurityWatchStore::claimOf)
            .recover(cause -> isOneRunningCollision(cause) ? Future.succeededFuture(new Claim(null, null))
                : Future.failedFuture(cause));
    }

    /** What the claim statements answer, read: digits are the new run's id; anything else is the refusal word. */
    static Claim claimOf(Object answer) {
        String text = text(answer);
        if (text == null)
            return new Claim(null, null);
        Integer id = text.chars().allMatch(Character::isDigit) && !text.isEmpty() ? Numbers.toInteger(text) : null;
        if (id != null)
            return new Claim(id, null);
        return new Claim(null, Claim.TOO_SOON.equals(text) ? Claim.TOO_SOON : Claim.BUSY);
    }

    static boolean isOneRunningCollision(Throwable cause) {
        String message = cause == null ? null : cause.getMessage();
        return message != null && (message.contains("security_watch_run_one_running_idx") || message.contains("23505"));
    }

    /** Ends a run; true when it was still running and is now written, false when it had already been ended. */
    static Future<Boolean> finish(int runId, String status, String error, String reportJson) {
        return firstKey(FINISH_SQL, runId, status, error, reportJson).map(key -> key != null);
    }

    /** Deletes the runs older than {@link #RETENTION}; answers how many. */
    static Future<Integer> prune() {
        return firstKey(PRUNE_SQL).map(Numbers::toInteger);
    }

    /** The latest succeeded report before run {@code runId}, as JSON text, or null (none, or the read failed). */
    static Future<String> previousReport(int runId) {
        return query(PREVIOUS_REPORT_SQL, runId)
            .map(rows -> rows == null || rows.getRowCount() == 0 ? null : text(rows.getValue(0, 0)))
            .otherwise((String) null);
    }

    /** The database's version, or null when it cannot be read. */
    static Future<String> postgresVersion() {
        return query(POSTGRES_VERSION_SQL)
            .map(rows -> rows == null || rows.getRowCount() == 0 ? null : text(rows.getValue(0, 0)))
            .otherwise((String) null);
    }

    // ===== Plumbing ==================================================================================================

    /** A raw read as the server. */
    static Future<QueryResult> query(String sql, Object... parameters) {
        return asServer(() -> QueryService.executeQuery(new QueryArgumentBuilder()
            .setDataSourceId(DataSourceModelService.getDefaultDataSourceId())
            .setStatement(sql) // a constant; every value is bound
            .setParameters(parameters)
            .build()));
    }

    /**
     * A write as the server, in its own transaction, answering the first column of the first row it returns (the
     * submit path's "generated key"), or null when it returned none.
     */
    private static Future<Object> firstKey(String sql, Object... parameters) {
        return asServer(() -> SubmitService.executeSubmit(new SubmitArgumentBuilder()
                .setDataSourceId(DataSourceModelService.getDefaultDataSourceId())
                .setStatement(sql) // a constant; every value is bound
                .setParameters(parameters)
                .setReturnGeneratedKeys(true)
                .build()))
            .map(SecurityWatchStore::firstKeyOf);
    }

    private static Object firstKeyOf(SubmitResult result) {
        Object[] keys = result == null ? null : result.getGeneratedKeys();
        return keys == null || keys.length == 0 ? null : keys[0];
    }

    /** The statement is the server's, not the caller's. Read on the calling thread. */
    static <T> T asServer(Supplier<T> call) {
        return ThreadLocalStateHolder.runWithState(StateAccessor.createEmptyState(), call);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static String orEmptyArray(String json) {
        return json == null ? "[]" : json;
    }
}
