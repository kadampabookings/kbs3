package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Check for what the endpoints answer, without a database: the replies of section 3 of the contract, the refusal
 * keys and their framing, the JSON escaping, how a claim's answer is read, the os-release parsing, and the off switch
 * refusing before anything is claimed.
 *
 * <p>No test framework: this repository declares no JUnit, so this runs from main() and exits non-zero on failure,
 * like the lifecycle plugin's checks. The report used is the fictional contract fixture ({@code report-v1.json} in the
 * test resources, byte-identical to the back office's). The SQL itself (claims, expiry, the one-running collision, the
 * schedule window, the summary projection) is rehearsed on a disposable postgres:17, not here.
 */
public class SecurityWatchRepliesCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    static void equal(String what, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        check(what + (ok ? "" : "\n         expected " + expected + "\n         actual   " + actual), ok);
    }

    public static void main(String[] args) throws Exception {
        String fixture;
        try (InputStream in = SecurityWatchRepliesCheck.class.getResourceAsStream("report-v1.json")) {
            if (in == null)
                throw new IllegalStateException("report-v1.json is missing from the test resources");
            fixture = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        }
        Instant now = Instant.parse("2026-11-26T09:15:02.734Z");

        System.out.println("status reply");
        SecurityWatchStore.Run run = new SecurityWatchStore.Run(413, "manual", "running", "2026-11-26T09:14:00Z", null, null);
        RuntimeFacts.Values runtime = new RuntimeFacts.Values("21.0.9", "Vendor21.38+21-CA", "Example OS 1.2", "1.2",
            "17.2", "2026-11-25T18:20:00Z", "production");
        String status = SecurityWatchReplies.status(true, now, new SecurityWatchStore.Status(run, fixture), runtime);
        equal("fields in the contract's order, the report spliced in as stored, instants to the second",
            "{\"enabled\":true,\"serverTime\":\"2026-11-26T09:15:02Z\","
            + "\"run\":{\"id\":413,\"trigger\":\"manual\",\"status\":\"running\",\"startedAt\":\"2026-11-26T09:14:00Z\",\"finishedAt\":null,\"error\":null},"
            + "\"report\":" + fixture + ","
            + "\"runtime\":{\"javaVersion\":\"21.0.9\",\"javaVendorVersion\":\"Vendor21.38+21-CA\",\"osRelease\":\"Example OS 1.2\","
            + "\"postgresVersion\":\"17.2\",\"buildTimestamp\":\"2026-11-25T18:20:00Z\"}}", status);
        equal("never ran, switched off: run and report null, runtime still filled (all null here)",
            "{\"enabled\":false,\"serverTime\":\"2026-11-26T09:15:02Z\",\"run\":null,\"report\":null,"
            + "\"runtime\":{\"javaVersion\":null,\"javaVendorVersion\":null,\"osRelease\":null,\"postgresVersion\":null,\"buildTimestamp\":null}}",
            SecurityWatchReplies.status(false, now, new SecurityWatchStore.Status(null, null), null));
        check("a failed run with a code", SecurityWatchReplies.runJson(new SecurityWatchStore.Run(9, "schedule", "failed",
            "2026-11-26T06:00:07Z", "2026-11-26T06:30:07Z", "timeout")).endsWith("\"error\":\"timeout\"}"));

        System.out.println("summary reply");
        String actions = "[{\"dueDate\": \"2026-11-21\", \"severity\": \"critical\", \"knownExploited\": true}]";
        equal("three fields per action, as the database extracted them",
            "{\"enabled\":true,\"serverTime\":\"2026-11-26T09:15:02Z\",\"generatedAt\":\"2026-11-26T06:02:11Z\",\"actions\":" + actions + "}",
            SecurityWatchReplies.summary(true, now, new SecurityWatchStore.Summary("2026-11-26T06:02:11Z", actions)));
        equal("no report yet: generatedAt null, actions []",
            "{\"enabled\":true,\"serverTime\":\"2026-11-26T09:15:02Z\",\"generatedAt\":null,\"actions\":[]}",
            SecurityWatchReplies.summary(true, now, null));

        System.out.println("checkNow reply and refusals");
        equal("checkNow", "{\"runId\":413}", SecurityWatchReplies.checkNow(413));
        check("disabled framed", SecurityWatchReplies.DISABLED_REFUSAL.startsWith("[SecurityWatchDisabledError] "));
        check("busy framed", SecurityWatchReplies.BUSY_REFUSAL.startsWith("[SecurityWatchBusyError] "));
        check("too soon framed", SecurityWatchReplies.TOO_SOON_REFUSAL.startsWith("[SecurityWatchTooSoonError] "));
        check("no second factor framed with the gateways' key",
            SecurityWatchAccess.NO_SECOND_FACTOR_REFUSAL.startsWith("[AuthnPasskeyAdminNotPermittedError] "));
        check("a start the database refused is framed with its key", SecurityWatchReplies.START_FAILED.startsWith("[SecurityWatchStartError] "));

        System.out.println("claims");
        equal("an id", 413, SecurityWatchStore.claimOf("413").runId());
        equal("busy", SecurityWatchReplies.BUSY_REFUSAL, SecurityWatchReplies.refusalOf(SecurityWatchStore.claimOf("busy")));
        equal("too soon", SecurityWatchReplies.TOO_SOON_REFUSAL, SecurityWatchReplies.refusalOf(SecurityWatchStore.claimOf("too-soon")));
        equal("an unexpected word is busy, never a run", SecurityWatchReplies.BUSY_REFUSAL,
            SecurityWatchReplies.refusalOf(SecurityWatchStore.claimOf("-1")));
        check("null (schedule: nothing to do) has neither id nor refusal word",
            SecurityWatchStore.claimOf(null).runId() == null && SecurityWatchStore.claimOf(null).refusal() == null);
        equal("a claim with a run is not refused", null, SecurityWatchReplies.refusalOf(SecurityWatchStore.claimOf("7")));
        check("the one-running index, as an error, is recognised",
            SecurityWatchStore.isOneRunningCollision(new RuntimeException(
                "ERROR: duplicate key value violates unique constraint \"security_watch_run_one_running_idx\" (23505)")));
        check("another error is not", !SecurityWatchStore.isOneRunningCollision(new RuntimeException("connection closed")));

        System.out.println("JSON escaping");
        equal("quotes, backslash, control characters, line separators",
            "\"a\\\"b\\\\c\\nd\\u0001e\\u2028\"", SecurityWatchJson.quote("a\"b\\c\nd\u0001e "));
        String hostile = new SecurityWatchJson.ObjectWriter().string("osRelease", "X\",\"enabled\":true,\"y\":\"").toJson();
        equal("a value cannot add a field", "{\"osRelease\":\"X\\\",\\\"enabled\\\":true,\\\"y\\\":\\\"\"}", hostile);

        System.out.println("runtime facts");
        Map<String, String> os = RuntimeFacts.parseOsRelease(
            "# comment\nNAME=\"Example OS\"\nVERSION_ID=\"1.2\"\nPRETTY_NAME=\"Example OS 1.2 \\\"LTS\\\"\"\nID=example\nEMPTY=\nbad line\nUNTERMINATED=\"x\n");
        equal("PRETTY_NAME unquoted and unescaped", "Example OS 1.2 \"LTS\"", os.get("PRETTY_NAME"));
        equal("VERSION_ID", "1.2", os.get("VERSION_ID"));
        equal("unquoted value", "example", os.get("ID"));
        check("empty, malformed and unterminated values are absent",
            !os.containsKey("EMPTY") && !os.containsKey("UNTERMINATED") && os.size() == 4);
        equal("postgres version: its first word", "17.2", RuntimeFacts.firstWord("17.2 (Debian 17.2-1.pgdg120+1)"));
        equal("blank is null", null, RuntimeFacts.firstWord("  "));
        check("environment is one of the three", java.util.Set.of("production", "staging", "development")
            .contains(RuntimeFacts.environment()));

        System.out.println("off switch");
        check("the configuration never loaded: off", !SecurityWatchConfig.isEnabled());
        Future<Integer> started = SecurityWatchRunner.startManual(7);
        check("checkNow while off is refused at once, before any claim",
            started.isComplete() && started.failed() && SecurityWatchReplies.DISABLED_REFUSAL.equals(started.cause().getMessage()));
        Future<Integer> scheduled = SecurityWatchRunner.startScheduled();
        check("a scheduled tick while off does nothing", scheduled.isComplete() && scheduled.succeeded() && scheduled.result() == null);

        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0)
            System.exit(1);
    }
}
