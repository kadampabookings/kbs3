package org.kadampabookings.kbs.server.securitywatch;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * The three endpoints' replies and refusals, as pure functions of what was read — the contract's section 3
 * ({@code docs/security/security-watch.md} in the deployment repository). Refusals are failed replies whose message
 * starts with an i18n key in square brackets, the project's convention, which the back office maps to its own text.
 *
 * @author Claude Code
 */
final class SecurityWatchReplies {

    private SecurityWatchReplies() {
    }

    /** {@code checkNow} while the check is switched off on this server. */
    static final String DISABLED_KEY = "SecurityWatchDisabledError";
    /** {@code checkNow} while a run is running. */
    static final String BUSY_KEY = "SecurityWatchBusyError";
    /** {@code checkNow} less than ten minutes after the last run started. */
    static final String TOO_SOON_KEY = "SecurityWatchTooSoonError";

    static final String DISABLED_REFUSAL = framed(DISABLED_KEY, "The security watch is switched off on this server");
    static final String BUSY_REFUSAL = framed(BUSY_KEY, "A check is already running");
    static final String TOO_SOON_REFUSAL = framed(TOO_SOON_KEY, "The last check started less than ten minutes ago");

    /** {@code checkNow} could not record the start (a database failure; its words stay in the server log). */
    static final String START_KEY = "SecurityWatchStartError";
    static final String START_FAILED = framed(START_KEY, "The check could not be started");

    static String framed(String key, String text) {
        return "[%s] %s".formatted(key, text);
    }

    /** An instant as the contract writes it: UTC, whole seconds, {@code 2026-11-26T09:15:02Z}. */
    static String instant(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    /**
     * The {@code status} reply: whether the check is switched on, the server's clock, the latest run (null when none),
     * the latest succeeded report as stored (null when none), and the runtime facts read for this reply.
     */
    static String status(boolean enabled, Instant serverTime, SecurityWatchStore.Status stored, RuntimeFacts.Values runtime) {
        SecurityWatchStore.Run run = stored == null ? null : stored.run();
        String reportJson = stored == null ? null : stored.reportJson();
        return new SecurityWatchJson.ObjectWriter()
            .bool("enabled", enabled)
            .string("serverTime", instant(serverTime))
            .raw("run", run == null ? null : runJson(run))
            .raw("report", reportJson)
            .raw("runtime", runtimeJson(runtime))
            .toJson();
    }

    static String runJson(SecurityWatchStore.Run run) {
        return new SecurityWatchJson.ObjectWriter()
            .number("id", run.id())
            .string("trigger", run.trigger())
            .string("status", run.status())
            .string("startedAt", run.startedAt())
            .string("finishedAt", run.finishedAt())
            .string("error", run.error())
            .toJson();
    }

    /** The runtime facts the contract's status reply carries (the report itself carries the others). */
    static String runtimeJson(RuntimeFacts.Values runtime) {
        RuntimeFacts.Values r = runtime != null ? runtime : new RuntimeFacts.Values(null, null, null, null, null, null, null);
        return new SecurityWatchJson.ObjectWriter()
            .string("javaVersion", r.javaVersion())
            .string("javaVendorVersion", r.javaVendorVersion())
            .string("osRelease", r.osRelease())
            .string("postgresVersion", r.postgresVersion())
            .string("buildTimestamp", r.buildTimestamp())
            .toJson();
    }

    /** The {@code summary} reply: three fields per action of the latest succeeded report, extracted by the database. */
    static String summary(boolean enabled, Instant serverTime, SecurityWatchStore.Summary summary) {
        return new SecurityWatchJson.ObjectWriter()
            .bool("enabled", enabled)
            .string("serverTime", instant(serverTime))
            .string("generatedAt", summary == null ? null : summary.generatedAt())
            .raw("actions", summary == null || summary.actionsJson() == null ? "[]" : summary.actionsJson())
            .toJson();
    }

    /** The {@code checkNow} reply: the id of the run just started. */
    static String checkNow(int runId) {
        return new SecurityWatchJson.ObjectWriter().number("runId", runId).toJson();
    }

    /** The refusal a claim that found no slot answers with; null when the claim has a run. */
    static String refusalOf(SecurityWatchStore.Claim claim) {
        if (claim == null)
            return BUSY_REFUSAL;
        if (claim.runId() != null)
            return null;
        return SecurityWatchStore.Claim.TOO_SOON.equals(claim.refusal()) ? TOO_SOON_REFUSAL : BUSY_REFUSAL;
    }
}
