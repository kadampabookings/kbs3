package org.kadampabookings.kbs.server.jobs.volunteeringretention;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.meta.Meta;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.util.Booleans;
import dev.webfx.stack.cloud.image.CloudImageService;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitService;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;

import java.util.regex.Pattern;

/**
 * Applies the volunteering GDPR retention policy on a schedule, by calling the
 * {@code volunteering_retention_sweep()} database function installed by migration V0074
 * and deleting due applicant photographs from the image store. Volunteering runs against
 * a perpetual event that never closes, so no event-closure clean-up ever reaches it —
 * this job is its retention mechanism. Periods and switches live in configuration
 * ({@code kbs.server.volunteeringretention}); the job ships disabled, and even enabled it
 * stays in dry-run (counts only, no data change) until {@code dryRun} is explicitly
 * false — the periods are a DPO ruling, not a developer default.
 *
 * <p>The photograph pass is separately gated by {@code cdnDeleteEnabled} because the
 * image store is shared infrastructure: only the environment whose database is
 * authoritative for the Bunny storage zone (production) may delete objects from it — a
 * staging server sweeping its own copy of the data must never remove pictures the
 * production rows still reference. Order matters within the pass: the CDN object is
 * deleted first and the {@code photo} column NULLed only on success, so a failed delete
 * is simply re-listed on the next run; and the SQL side refuses stage-2 row deletion
 * while {@code photo} is set, so an application row can never disappear leaving its
 * photograph orphaned in the store. Operations reference:
 * {@code docs/operations/volunteering-retention.md} (aggregate repo).
 */
public final class VolunteeringRetentionJob implements ApplicationJob {

    private static final String CONFIG_PATH = "kbs.server.volunteeringretention";
    private static final String LOG_PREFIX = "[volunteering-retention] ";
    /** Recorded in the caller column of each volunteering_retention_run audit row. */
    private static final String AUDIT_CALLER = "kbs-server";

    private boolean dryRun = true;
    private boolean cdnDeleteEnabled;
    private int runPeriodHours = 24;
    private int batchLimit = 500;
    private int photoBatchLimit = 50;
    private int tokenGraceDays = 30;
    private int shareCodeMaxAgeDays = 90;
    private int stage1Months = 3;
    private int specialNeedsMonths = 3;
    private int stage2NeverServedMonths = 12;
    private int stage2ServedMonths = 24;

    /** A sweep latched this long is presumed lost (dropped connection whose future never
     * completes); the next tick logs it and proceeds — the database-side advisory lock,
     * not this flag, is what guarantees single-writer. */
    private static final long STUCK_SWEEP_MS = 6 * 3_600_000L;

    /**
     * The exact shape the image service assigns to an applicant portrait, mirroring
     * {@code ImageCategoryPolicy.VOLUNTEERS} in the images plugin (kept as a local copy
     * rather than a dependency on that server plugin).
     *
     * <p>Anchored on purpose, and the anchoring is load-bearing: the storage key is
     * concatenated straight into the authenticated DELETE url by the Bunny provider, and
     * the {@code photo} column is written by the public application form, so a prefix test
     * alone would accept {@code volunteers/../events/event-1.png} and turn this job into a
     * deputy for deleting objects the applicant never owned. That is exactly the trap
     * ImageCategoryPolicy documents.
     */
    private static final Pattern VOLUNTEER_PHOTO_KEY = Pattern.compile(
            "volunteers/vol-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");

    private Object dataSourceId;
    private volatile boolean stopped;
    private boolean sweepRunning;
    private long sweepStartedAtMs;
    private Scheduled periodicScheduled;

    @Override
    public void onStart() {
        ConfigLoader.onConfigLoaded(CONFIG_PATH, this::onConfigLoaded);
    }

    @Override
    public void onStop() {
        stopped = true;
        if (periodicScheduled != null)
            periodicScheduled.cancel();
    }

    private void onConfigLoaded(Config config) {
        if (config == null || !Booleans.isTrue(config.getBoolean("enabled"))) {
            Console.log(LOG_PREFIX + "Disabled by config — volunteering retention is not applied by this server");
            return;
        }
        if (Meta.isDevelopment() && !Booleans.isTrue(config.getBoolean("enabledInDevelopment"))) {
            // Second guard for developer machines, whose default datasource may reach a real
            // database: enabled alone must not make a local server erase staging or prod data.
            Console.log(LOG_PREFIX + "Not sweeping on a development machine (enabledInDevelopment is not set)");
            return;
        }
        // Missing or unparseable dryRun stays TRUE: only an explicit false performs writes.
        dryRun = !Booleans.isFalse(config.getBoolean("dryRun"));
        cdnDeleteEnabled = Booleans.isTrue(config.getBoolean("cdnDeleteEnabled"));
        runPeriodHours = intConfig(config, "runPeriodHours", runPeriodHours);
        batchLimit = intConfig(config, "batchLimit", batchLimit);
        photoBatchLimit = intConfig(config, "photoBatchLimit", photoBatchLimit);
        tokenGraceDays = intConfig(config, "tokenGraceDays", tokenGraceDays);
        shareCodeMaxAgeDays = intConfig(config, "shareCodeMaxAgeDays", shareCodeMaxAgeDays);
        stage1Months = intConfig(config, "stage1Months", stage1Months);
        specialNeedsMonths = intConfig(config, "specialNeedsMonths", specialNeedsMonths);
        stage2NeverServedMonths = intConfig(config, "stage2NeverServedMonths", stage2NeverServedMonths);
        stage2ServedMonths = intConfig(config, "stage2ServedMonths", stage2ServedMonths);

        Console.log(LOG_PREFIX + "Enabled (dryRun=" + dryRun + ", cdnDeleteEnabled=" + cdnDeleteEnabled
                + ", runPeriodHours=" + runPeriodHours
                + ", stage1Months=" + stage1Months + ", stage2Months=" + stage2NeverServedMonths
                + "/" + stage2ServedMonths + " never-served/served) — waiting for the datasource");
        // Jobs start before the datasource and its model are initialized, and querying that
        // early corrupts the whole boot — wait for both, like DbMigrationJob and MailerJob.
        LocalDataSourceService.onInitialised(() ->
                DataSourceModelService.loadDataSourceModel(DataSourceModelService.getDefaultDataSourceId())
                        .onFailure(e -> {
                            Console.log("⛔️ " + LOG_PREFIX + "Could not load the data source model — retention job not started");
                            Console.log(e);
                        })
                        .onSuccess(model -> {
                            dataSourceId = model.getDataSourceId();
                            // First run straight away (a daily job should not wait a day to
                            // report what it would do), then at the configured period.
                            runSweep();
                            periodicScheduled = Scheduler.schedulePeriodic(runPeriodHours * 3_600_000L, this::runSweep);
                        }));
    }

    private static int intConfig(Config config, String key, int defaultValue) {
        Integer value = config.getInteger(key);
        return value != null ? value : defaultValue;
    }

    private void runSweep() {
        if (stopped)
            return;
        if (sweepRunning) { // periods are hours; a still-running sweep means trouble, not overlap
            if (System.currentTimeMillis() - sweepStartedAtMs < STUCK_SWEEP_MS) {
                Console.log(LOG_PREFIX + "Previous sweep still in flight — skipping this tick");
                return;
            }
            // Without this escape, one future that never completes would silently switch
            // the retention job off until the next server restart.
            Console.log("⚠️ " + LOG_PREFIX + "Previous sweep appears stuck (> " + (STUCK_SWEEP_MS / 3_600_000L)
                    + "h) — proceeding anyway; the database advisory lock protects against a double run");
        }
        sweepRunning = true;
        sweepStartedAtMs = System.currentTimeMillis();
        Future<?> sweepFuture;
        try {
            sweepFuture = executeSweep();
        } catch (Exception e) {
            // A synchronous throw must never escape to the event loop — it would take the server down
            sweepFuture = Future.failedFuture(e);
        }
        sweepFuture.onComplete(ar -> {
            sweepRunning = false;
            if (ar.failed()) {
                Console.log("⛔️ " + LOG_PREFIX + "Sweep failed (will retry at the next period)");
                Console.log(ar.cause());
            }
        });
    }

    private Future<?> executeSweep() {
        // The sweep goes through the QUERY path on purpose: it returns a result set (the
        // per-rule counts), which SubmitService cannot deliver — its SubmitResult carries
        // only a row count. The whole sweep is a single statement, so the query-pool
        // connection is held no longer than any report query would hold it.
        return QueryService.executeQuery(QueryArgument.builder()
                        .setDataSourceId(dataSourceId)
                        // Raw SQL, so the language is deliberately left unset: any non-null
                        // language sends the statement through the DQL translator, which
                        // cannot parse SQL.
                        .setStatement("select rule, affected from volunteering_retention_sweep($1,$2,$3,$4,$5,$6,$7,$8,$9)")
                        .setParameters(dryRun, batchLimit, tokenGraceDays, shareCodeMaxAgeDays,
                                stage1Months, specialNeedsMonths, stage2NeverServedMonths, stage2ServedMonths,
                                AUDIT_CALLER)
                        .build())
                .compose(result -> {
                    long blockedOnPhoto = 0;
                    StringBuilder counts = new StringBuilder();
                    for (int row = 0; row < result.getRowCount(); row++) {
                        String rule = String.valueOf((Object) result.getValue(row, 0));
                        Object affected = result.getValue(row, 1);
                        if ("skipped_lock".equals(rule)) {
                            // Another server instance (blue/green overlap) holds the sweep
                            // lock; it owns this run entirely — including the photo pass.
                            Console.log(LOG_PREFIX + "Another instance holds the sweep lock — standing down for this run");
                            return Future.succeededFuture();
                        }
                        if ("stage2_blocked_on_photo".equals(rule))
                            blockedOnPhoto = Long.parseLong(String.valueOf(affected));
                        if (counts.length() > 0)
                            counts.append(", ");
                        counts.append(rule).append('=').append(affected);
                    }
                    Console.log(LOG_PREFIX + (dryRun ? "Dry run — due: " : "Swept: ") + counts);
                    if (!dryRun && !cdnDeleteEnabled && blockedOnPhoto > 0)
                        // Without this, the two-flag go-live (dryRun off, cdnDelete forgotten)
                        // would leave every photographed application undeletable for ever,
                        // visible only to whoever reads the audit table.
                        Console.log("⚠️ " + LOG_PREFIX + blockedOnPhoto + " application(s) are past their full retention"
                                + " period but cannot be deleted while their photograph exists and cdnDeleteEnabled is false");
                    return deleteDuePhotos();
                });
    }

    private Future<Void> deleteDuePhotos() {
        if (dryRun || !cdnDeleteEnabled) // the sweep's photo_due count still reports the backlog
            return Future.succeededFuture();
        return QueryService.executeQuery(QueryArgument.builder()
                        .setDataSourceId(dataSourceId)
                        .setStatement("select id, photo from volunteering_retention_due_photos($1,$2)")
                        .setParameters(stage1Months, photoBatchLimit)
                        .build())
                .compose(photos -> photos.getRowCount() == 0
                        ? Future.succeededFuture()
                        : CloudImageService.readyFuture() // Bunny provider completes it once its config loads
                                .compose(ready -> deletePhotoChain(photos, 0)));
    }

    private Future<Void> deletePhotoChain(QueryResult photos, int index) {
        if (stopped || index >= photos.getRowCount())
            return Future.succeededFuture();
        Object applicationId = photos.getValue(index, 0);
        String photoKey = String.valueOf((Object) photos.getValue(index, 1));
        if (!VOLUNTEER_PHOTO_KEY.matcher(photoKey).matches()) {
            // Either a KBS2-imported external URL, or a value that is not a key this server
            // ever issued. Both are left for a manual ruling (the runbook's orphan/legacy
            // section) and stay visible in stage2_blocked_on_photo: deleting a foreign
            // reference from our zone would be meaningless, and NULLing it would discard
            // the only record of where the picture lives.
            Console.log("⚠️ " + LOG_PREFIX + "Application " + applicationId
                    + " holds a photo reference that is not a server-issued volunteer key"
                    + " — needs a manual ruling, skipping");
            return deletePhotoChain(photos, index + 1);
        }
        // CDN object first, column second: a failed delete leaves photo set, so the row is
        // re-listed next run — that IS the retry mechanism (the listing is randomly ordered,
        // so a persistently failing key cannot monopolise the batch). The provider answers
        // 404 as success, so an already-gone object cannot wedge the backlog. The photo=$2
        // clause makes the NULLing a no-op if the row changed hands between the two statements.
        return CloudImageService.delete(photoKey, false)
                .compose(deleted -> SubmitService.executeSubmit(SubmitArgument.builder()
                                .setDataSourceId(dataSourceId)
                                .setLanguage("SQL")
                                .setStatement("update volunteering_application set photo = null where id = $1 and photo = $2")
                                .setParameters(applicationId, photoKey)
                                .build())
                        .<Void>mapEmpty())
                .otherwise(e -> {
                    Console.log("⛔️ " + LOG_PREFIX + "Photo delete failed for application " + applicationId
                            + " (will retry next run): " + e.getMessage());
                    return null;
                })
                .compose(ignored -> deletePhotoChain(photos, index + 1));
    }
}
