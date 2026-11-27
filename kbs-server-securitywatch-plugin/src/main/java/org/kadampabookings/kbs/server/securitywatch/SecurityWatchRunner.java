package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;

/**
 * Starts a run and carries it to its end: claim a row ({@link SecurityWatchStore#claimManual} /
 * {@link SecurityWatchStore#claimSchedule}), prune the old runs, run the check, record the outcome.
 *
 * <p><b>The off switch is enforced here</b>, at the one place a run can start: with the check switched off
 * ({@link SecurityWatchConfig#isEnabled()}) nothing is claimed and nothing calls out, whoever asks.
 *
 * <p><b>The check itself is a seam.</b> {@link #check} produces the report's JSON for a claimed run, or fails with
 * a {@link RunFailure} carrying one of the contract's fixed error codes. The module installs {@link SecurityWatchCheck}
 * at start ({@link SecurityWatchModule}); until then (and in the checks) it fails with {@code internal}. A source
 * that fails does not fail the run: the check reports it inside a succeeded run.
 *
 * <p>After a run succeeds, {@link UrgentAlert} compares its report with the previous succeeded one and mails the
 * platform super administrators about what became urgent. Its failure never touches the run.
 *
 * <p>A run is never left running by this class: whatever the check does, the row is finished as {@code succeeded} or
 * {@code failed}; and should the server die mid-run, the next claim marks it failed ({@code timeout}).
 *
 * @author Claude Code
 */
final class SecurityWatchRunner {

    private SecurityWatchRunner() {
    }

    /** The error code of a run that failed for a reason of its own (an exception, a bug): never a source's words. */
    static final String ERROR_INTERNAL = "internal";

    /** A run that failed with one of the contract's fixed error codes. */
    static final class RunFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String code;

        RunFailure(String code) {
            super(code, null, false, false);
            this.code = code;
        }

        String code() {
            return code;
        }
    }

    /** What a claimed run does: the report's JSON text, or a failure. */
    interface Check {
        Future<String> run(int runId, String trigger);
    }

    private static volatile Check check = (runId, trigger) -> Future.failedFuture(new RunFailure(ERROR_INTERNAL));

    /** Installs the check (the module's own wiring, and the checks' doubles). */
    static void setCheck(Check newCheck) {
        check = newCheck;
    }

    /**
     * A manual run for {@code personId}: claimed now, run in the background. Answers the new run's id, or fails with
     * the contract's refusal ({@code SecurityWatchDisabledError}, {@code SecurityWatchBusyError},
     * {@code SecurityWatchTooSoonError}).
     */
    static Future<Integer> startManual(Integer personId) {
        if (!SecurityWatchConfig.isEnabled()) {
            Console.log(SecurityWatchLog.PREFIX + "Check now by person " + personId + " refused: the security watch is switched off");
            return Future.failedFuture(SecurityWatchReplies.DISABLED_REFUSAL);
        }
        return SecurityWatchStore.claimManual(personId)
            .recover(cause -> {
                Console.log(SecurityWatchLog.PREFIX + "Check now by person " + personId + " could not be claimed ("
                            + describe(cause) + ")");
                return Future.failedFuture(SecurityWatchReplies.START_FAILED);
            })
            .compose(claim -> {
                String refusal = SecurityWatchReplies.refusalOf(claim);
                if (refusal != null) {
                    Console.log(SecurityWatchLog.PREFIX + "Check now by person " + personId + " refused: " + claim.refusal());
                    return Future.failedFuture(refusal);
                }
                int runId = claim.runId();
                Console.log(SecurityWatchLog.PREFIX + "Run " + runId + " started (manual, person " + personId + ")");
                runInBackground(runId, SecurityWatchStore.TRIGGER_MANUAL);
                return Future.succeededFuture(runId);
            });
    }

    /**
     * A scheduled run, for the daily tick of a server task: answers the new run's id, or null when this tick has
     * nothing to do (switched off, a run already succeeded since the last 05:00 UTC, a run started in the last 55 minutes,
     * a run running, or another task
     * won the claim). Never fails.
     */
    static Future<Integer> startScheduled() {
        if (!SecurityWatchConfig.isEnabled())
            return Future.succeededFuture(null);
        return SecurityWatchStore.claimSchedule()
            .map(claim -> {
                if (claim.runId() == null)
                    return (Integer) null;
                Console.log(SecurityWatchLog.PREFIX + "Run " + claim.runId() + " started (schedule)");
                runInBackground(claim.runId(), SecurityWatchStore.TRIGGER_SCHEDULE);
                return claim.runId();
            })
            .recover(cause -> {
                Console.log(SecurityWatchLog.PREFIX + "The scheduled check could not be claimed ("
                            + describe(cause) + ")");
                return Future.succeededFuture(null);
            });
    }

    private static void runInBackground(int runId, String trigger) {
        long t0 = System.currentTimeMillis();
        SecurityWatchStore.prune()
            .onSuccess(pruned -> {
                if (pruned != null && pruned > 0)
                    Console.log(SecurityWatchLog.PREFIX + "Run " + runId + ": deleted " + pruned + " runs older than " + SecurityWatchStore.RETENTION);
            })
            .otherwise((Integer) null) // a failed prune is retried by the next run; it must not stop this one
            .compose(ignored -> runCheck(runId, trigger))
            .compose(report -> SecurityWatchStore.finish(runId, SecurityWatchStore.STATUS_SUCCEEDED, null, report)
                .map(written -> {
                    if (written)
                        UrgentAlert.afterSuccess(runId, report);
                    return outcome(written, SecurityWatchStore.STATUS_SUCCEEDED, null);
                }))
            .recover(cause -> {
                String code = cause instanceof RunFailure failure ? failure.code() : ERROR_INTERNAL;
                if (!(cause instanceof RunFailure))
                    Console.log(SecurityWatchLog.PREFIX + "Run " + runId + " failed: " + describe(cause));
                return SecurityWatchStore.finish(runId, SecurityWatchStore.STATUS_FAILED, code, null)
                    .map(written -> outcome(written, SecurityWatchStore.STATUS_FAILED, code));
            })
            .onComplete(ar -> Console.log(SecurityWatchLog.PREFIX + "Run " + runId + " (" + trigger + ") "
                + (ar.succeeded() ? ar.result() : "NOT RECORDED (" + describe(ar.cause()) + ")")
                + " in " + (System.currentTimeMillis() - t0) + " ms"));
    }

    /**
     * A failure as the log may show it: its class, where in this module it was thrown, and its message only when the
     * message cannot be carrying data — short, plain, with no quotes (a parse error quotes the text it could not read,
     * which may come from a source or the inventory). The aggregated log must not receive a source's text or an
     * inventory value.
     */
    static String describe(Throwable cause) {
        if (cause == null)
            return "no cause";
        StringBuilder line = new StringBuilder(cause.getClass().getSimpleName());
        for (StackTraceElement frame : cause.getStackTrace())
            if (frame.getClassName().startsWith(SecurityWatchRunner.class.getPackageName() + ".")) {
                line.append(" at ").append(frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1))
                    .append('.').append(frame.getMethodName()).append(':').append(frame.getLineNumber());
                break;
            }
        String message = cause.getMessage();
        if (message != null && SAFE_MESSAGE.matcher(message).matches())
            line.append(": ").append(message);
        else if (message != null)
            line.append(" (message withheld)");
        return line.toString();
    }

    private static final java.util.regex.Pattern SAFE_MESSAGE = java.util.regex.Pattern.compile("[A-Za-z0-9 .,:;<>=()/_-]{1,160}");

    /** The check, guarded: one that throws instead of failing its future fails the run all the same. */
    private static Future<String> runCheck(int runId, String trigger) {
        try {
            Future<String> report = check.run(runId, trigger);
            return report != null ? report : Future.failedFuture(new RunFailure(ERROR_INTERNAL));
        } catch (RuntimeException e) {
            return Future.failedFuture(e);
        }
    }

    private static String outcome(boolean written, String status, String code) {
        return (written ? status : "ended as " + status + " but it was no longer running (marked failed by a later start?)")
               + (code == null ? "" : " (" + code + ")");
    }
}
