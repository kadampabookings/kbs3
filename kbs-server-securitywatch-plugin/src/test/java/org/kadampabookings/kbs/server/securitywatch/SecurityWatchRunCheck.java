package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Check for a whole run, from the fictional test inventory and lockfile ({@code directory/} in the test resources)
 * against recorded public answers ({@link RecordedSourceClient}): what the report says, and what each source failure
 * does to it. No network, no database.
 */
public class SecurityWatchRunCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    static void equal(String what, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        check(what + (ok ? "" : "  (expected " + expected + ", got " + actual + ")"), ok);
    }

    static final Instant NOW = Instant.parse("2026-11-27T06:00:00Z");
    static final RuntimeFacts.Values FACTS = new RuntimeFacts.Values("21.0.9", "Vendor21.38+21-CA", "Example OS 3.2.1 LTS",
        "3.2", "7.0.0", "2026-11-01T00:00:00Z", "development");

    static String javaList(int fictional) {
        StringBuilder sb = new StringBuilder("The following files have been resolved:\n");
        sb.append("   org.apache.struts:struts2-core:jar:2.5.10 -- module struts2.core\n");
        for (int i = 0; i < fictional; i++)
            sb.append("   org.example:lib-").append(i).append(":jar:1.").append(i).append('\n');
        return sb.toString();
    }

    static Path directory() throws Exception {
        URL url = SecurityWatchRunCheck.class.getResource("directory/inventory.json");
        return Path.of(url.toURI()).getParent();
    }

    static Map<String, Object> run(RecordedSourceClient client, String dir, String javaList) throws Exception {
        return run(client, dir, javaList, FACTS);
    }

    static Map<String, Object> run(RecordedSourceClient client, String dir, String javaList, RuntimeFacts.Values facts) throws Exception {
        SecurityWatchCheck check = new SecurityWatchCheck(client, () -> NOW, 0, () -> dir,
            () -> Future.succeededFuture(facts), () -> javaList);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> json = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        // Started on a Vert.x context, as the bus delivers a call
        VERTX.runOnContext(v -> check.run(41, "manual").onComplete(ar -> {
            if (ar.succeeded()) json.set(ar.result()); else error.set(ar.cause());
            latch.countDown();
        }));
        if (!latch.await(60, TimeUnit.SECONDS))
            throw new IllegalStateException("the run did not finish");
        if (error.get() != null)
            throw new IllegalStateException("the run failed", error.get());
        return JsonTree.parseObject(json.get());
    }

    static Map<String, Object> byId(Map<String, Object> report, String list, String id) {
        for (Object item : JsonTree.list(report, list)) {
            Map<String, Object> o = JsonTree.asObject(item);
            if (id.equals(JsonTree.string(o, "id")))
                return o;
        }
        return null;
    }

    /** Every component, action, advisory and counts object carries exactly the contract's fields (section 2). */
    static boolean contractShape(Map<String, Object> report) {
        Set<String> component = Set.of("id", "layer", "name", "where", "managedBy", "running", "packageCount", "latest",
            "supportEnds", "status", "openAdvisories", "failedChecks", "actionIds");
        Set<String> running = Set.of("version", "source", "asOf", "fact");
        Set<String> action = Set.of("id", "componentId", "kind", "target", "from", "to", "severity", "knownExploited",
            "fixAvailable", "publishedOn", "dueDate", "owner", "needs", "counts", "howTo", "advisories");
        Set<String> counts = Set.of("total", "critical", "high", "medium", "low", "uncertain");
        Set<String> advisory = Set.of("id", "severity", "cvss", "summary", "fixedIn", "knownExploited", "publishedOn", "uncertain");
        Set<String> top = Set.of("schemaVersion", "environment", "generatedAt", "run", "checks", "components", "actions", "assessed");
        if (!report.keySet().equals(top))
            return false;
        for (Object c : JsonTree.list(report, "components")) {
            Map<String, Object> m = JsonTree.asObject(c);
            if (!m.keySet().equals(component) || (m.get("running") != null && !JsonTree.object(m, "running").keySet().equals(running)))
                return false;
        }
        for (Object a : JsonTree.list(report, "actions")) {
            Map<String, Object> m = JsonTree.asObject(a);
            if (!m.keySet().equals(action) || !JsonTree.object(m, "counts").keySet().equals(counts))
                return false;
            for (Object adv : JsonTree.list(m, "advisories"))
                if (!JsonTree.asObject(adv).keySet().equals(advisory))
                    return false;
        }
        return true;
    }

    static String check(Map<String, Object> report, String id) {
        Map<String, Object> c = byId(report, "checks", id);
        return JsonTree.isTrue(c, "ok") ? "ok" : JsonTree.string(c, "error");
    }

    static List<String> strings(Map<String, Object> o, String key) {
        List<String> list = new ArrayList<>();
        for (Object item : JsonTree.list(o, key))
            list.add((String) item);
        return list;
    }

    /**
     * The server's runtime, not a stand-in: a Vert.x instance registered as the platform's, the platform scheduler being
     * Vert.x's (the run must be on a class path without the JRE scheduler, as the server is: checked below), and every run
     * started on a Vert.x context, as a bus call is. A 0 ms timer fails here as it does on the server.
     */
    static final io.vertx.core.Vertx VERTX = io.vertx.core.Vertx.vertx();

    public static void main(String[] args) throws Exception {
        dev.webfx.platform.util.vertx.VertxInstance.setVertx(VERTX);
        try {
            System.out.println("the server's runtime");
            equal("the platform scheduler is Vert.x's, as on the server", "VertxSchedulerProvider",
                dev.webfx.platform.scheduler.Scheduler.getProvider().getClass().getSimpleName());
            boolean refused;
            try {
                dev.webfx.platform.scheduler.Scheduler.runInBackground(() -> { });
                refused = false;
            } catch (IllegalArgumentException e) {
                refused = true; // "Cannot schedule a timer with delay < 1 ms": the trap the first real run fell into
            }
            check("so the platform's runInBackground (a 0 ms timer) is refused here as there", refused);
            checks();
        } catch (Throwable e) {
            e.printStackTrace();
            fail++;
        }
        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail > 0 ? 1 : 0);
    }

    static void checks() throws Exception {
        String dir = directory().toString();

        System.out.println("a full run on recorded answers");
        RecordedSourceClient client = new RecordedSourceClient();
        Map<String, Object> report = run(client, dir, javaList(20));
        for (String id : ReportAssembler.CHECK_IDS)
            equal("check " + id + " ok", "ok", check(report, id));
        equal("schema version", 1, JsonTree.integer(report, "schemaVersion"));
        equal("environment", "development", JsonTree.string(report, "environment"));
        equal("run", "{\"id\":41,\"trigger\":\"manual\",\"startedAt\":\"2026-11-27T06:00:00Z\",\"finishedAt\":\"2026-11-27T06:00:00Z\"}",
            JsonTree.write(report.get("run")));
        check("every call went to an allowed https host", client.asked.stream().allMatch(SourceClient::isAllowed));
        check("no URL from a source in the report", !JsonTree.write(report).replace("\"https://", "").contains("http"));
        equal("components, in the inventory's order (development shows every environment's)",
            List.of("server-libraries", "web-libraries", "store", "edge", "os", "cloud", "prod-only"),
            JsonTree.list(report, "components").stream().map(o -> JsonTree.string(JsonTree.asObject(o), "id")).toList());
        for (Object item : JsonTree.list(report, "components"))
            check("component " + JsonTree.string(JsonTree.asObject(item), "id") + " has every field, nulls explicit",
                JsonTree.asObject(item).keySet().equals(Set.of("id", "layer", "name", "where", "managedBy", "running", "packageCount",
                    "latest", "supportEnds", "status", "openAdvisories", "failedChecks", "actionIds")));
        for (Object item : JsonTree.list(report, "actions"))
            check("action " + JsonTree.string(JsonTree.asObject(item), "id") + " has every field",
                JsonTree.asObject(item).keySet().equals(Set.of("id", "componentId", "kind", "target", "from", "to", "severity",
                    "knownExploited", "fixAvailable", "publishedOn", "dueDate", "owner", "needs", "counts", "howTo", "advisories")));

        Map<String, Object> java = byId(report, "components", "server-libraries");
        equal("java list: package count", 21, JsonTree.integer(java, "packageCount"));
        equal("java list: running from the build", "{\"version\":null,\"source\":\"build\",\"asOf\":\"2026-11-01T00:00:00Z\",\"fact\":null}",
            JsonTree.write(java.get("running")));
        equal("java list: vulnerable", "vulnerable", JsonTree.string(java, "status"));
        Map<String, Object> struts = byId(report, "actions", "code-change:server-libraries:org.apache.struts:struts2-core");
        check("one action per library", struts != null);
        equal("to: the lowest version fixing both flaws", "2.5.13", JsonTree.string(struts, "to"));
        equal("from", "2.5.10", JsonTree.string(struts, "from"));
        equal("severity: the worst flaw's", "critical", JsonTree.string(struts, "severity"));
        check("known exploited (in the KEV slice)", JsonTree.isTrue(struts, "knownExploited"));
        equal("published on the day CISA listed it", "2021-11-03", JsonTree.string(struts, "publishedOn"));
        equal("due 72 hours later", "2021-11-06", JsonTree.string(struts, "dueDate"));
        equal("counts", "{\"total\":2,\"critical\":1,\"high\":0,\"medium\":1,\"low\":0,\"uncertain\":0}", JsonTree.write(struts.get("counts")));
        equal("advisories worst first, CVE ids shown", List.of("CVE-2017-5638", "CVE-2016-4465"),
            JsonTree.list(struts, "advisories").stream().map(a -> JsonTree.string(JsonTree.asObject(a), "id")).toList());
        equal("cvss computed from the vector (3.0, scope changed)", 10.0,
            JsonTree.number(JsonTree.asObject(JsonTree.list(struts, "advisories").get(0)), "cvss"));
        equal("how-to of the kind", List.of("Raise the version in the build file"), strings(struts, "howTo"));

        Map<String, Object> npm = byId(report, "components", "web-libraries");
        equal("npm: only what runs (no dev, devOptional, link or workspace folder), the same package+version once", 3,
            JsonTree.integer(npm, "packageCount"));
        Map<String, Object> minimist = byId(report, "actions", "code-change:web-libraries:minimist");
        equal("npm fix", "1.2.6", JsonTree.string(minimist, "to"));
        equal("critical, not exploited: due 7 days after publication", "2022-03-25", JsonTree.string(minimist, "dueDate"));

        Map<String, Object> store = byId(report, "components", "store");
        equal("live version is exactly the fact", "{\"version\":\"7.0.0\",\"source\":\"live\",\"asOf\":\"2026-11-27T06:00:00Z\",\"fact\":\"postgresVersion\"}",
            JsonTree.write(store.get("running")));
        equal("open advisories: 2 by version + 1 early warning, the assessed one left out", 3, JsonTree.integer(store, "openAdvisories"));
        equal("status", "vulnerable", JsonTree.string(store, "status"));
        check("support end from the line 7.0", JsonTree.string(store, "supportEnds") != null);
        List<String> storeActions = strings(store, "actionIds");
        equal("store: ONE action for the flaws, the early warning, the newer release and the ended line",
            List.of("upgrade:store:8.12.0"), storeActions);
        Map<String, Object> upgrade = byId(report, "actions", "upgrade:store:8.12.0");
        equal("upgrade: line ended, so to the newest maintained line", "8.12.0", JsonTree.string(upgrade, "to"));
        equal("upgrade: severity high", "high", JsonTree.string(upgrade, "severity"));
        equal("upgrade: needs from the inventory", List.of("maintenance-window"), strings(upgrade, "needs"));
        equal("upgrade: 3 flaws, 1 uncertain (the early warning)", "{\"total\":3,\"critical\":0,\"high\":3,\"medium\":0,\"low\":0,\"uncertain\":1}",
            JsonTree.write(upgrade.get("counts")));
        Map<String, Object> warning = JsonTree.list(upgrade, "advisories").stream().map(JsonTree::asObject)
            .filter(a -> "CVE-2099-0001".equals(JsonTree.string(a, "id"))).findFirst().orElse(Map.of());
        equal("early warning: no score, so high", "high", JsonTree.string(warning, "severity"));
        check("early warning: flagged uncertain, the version matches not", JsonTree.isTrue(warning, "uncertain")
            && JsonTree.list(upgrade, "advisories").stream().map(JsonTree::asObject)
                .filter(a -> !"CVE-2099-0001".equals(JsonTree.string(a, "id"))).noneMatch(a -> JsonTree.isTrue(a, "uncertain")));
        equal("early-warning summary: control characters removed, markup kept as text",
            "A crafted request crashes Example Server. <b>Not</b> markup.", JsonTree.string(warning, "summary"));
        check("a keyword hit about another product, or not our version, is dropped",
            !JsonTree.write(report).contains("CVE-2099-0002") && !JsonTree.write(report).contains("CVE-2099-0003"));
        check("the assessed flaw is not in any action", !JsonTree.write(report.get("actions")).contains("CVE-2023-28856"));

        Map<String, Object> edge = byId(report, "components", "edge");
        equal("declared, stale: a verify action", List.of("verify:edge:1.0"), strings(edge, "actionIds"));
        equal("stale verify has no severity", "none", JsonTree.string(byId(report, "actions", "verify:edge:1.0"), "severity"));
        Map<String, Object> os = byId(report, "components", "os");
        equal("os: a newer base image than our build gives a redeploy", List.of("redeploy:os:3.2.1"), strings(os, "actionIds"));
        equal("os: redeploy to nothing named", null, byId(report, "actions", "redeploy:os:3.2.1").get("to"));
        equal("os: no latest", null, os.get("latest"));
        equal("os: status update", "update", JsonTree.string(os, "status"));
        equal("os: version is the PRETTY_NAME", "Example OS 3.2.1 LTS", JsonTree.string(JsonTree.object(os, "running"), "version"));
        equal("provider's: managed, nothing to do", "managed", JsonTree.string(byId(report, "components", "cloud"), "status"));
        equal("assessed[]: in force only", List.of("CVE-2023-28856"),
            JsonTree.list(report, "assessed").stream().map(a -> JsonTree.string(JsonTree.asObject(a), "id")).toList());
        check("the report is under 512 KB", JsonTree.write(report).length() < ReportAssembler.MAX_REPORT_CHARS);

        System.out.println("the KEV download times out");
        RecordedSourceClient noKev = new RecordedSourceClient();
        noKev.failures.put("cisa.gov", "timeout");
        Map<String, Object> r2 = run(noKev, dir, javaList(20));
        equal("kev check failed with its code", "timeout", check(r2, "kev"));
        equal("the run still succeeded, other checks ok", "ok", check(r2, "nvd"));
        check("the component with KEV names lists kev", strings(byId(r2, "components", "store"), "failedChecks").contains("kev"));
        check("a library list relies on it too: its flaws can no longer be marked exploited",
            strings(byId(r2, "components", "server-libraries"), "failedChecks").contains("kev"));
        check("a component with no flaws to mark does not list it",
            !strings(byId(r2, "components", "os"), "failedChecks").contains("kev"));
        check("without the catalogue nothing is marked exploited",
            !JsonTree.isTrue(byId(r2, "actions", "code-change:server-libraries:org.apache.struts:struts2-core"), "knownExploited"));

        System.out.println("the NVD answers 429 once, then 200: one retry");
        RecordedSourceClient rate = new RecordedSourceClient();
        rate.failures.put("virtualMatchString=cpe%3A2.3%3Aa%3Aredis%3Aredis%3A7.0.0", "status:429");
        rate.failTimes.put("virtualMatchString=cpe%3A2.3%3Aa%3Aredis%3Aredis%3A7.0.0", 1);
        Map<String, Object> r3 = run(rate, dir, javaList(20));
        equal("nvd ok after the retry", "ok", check(r3, "nvd"));
        equal("the version query was asked twice", 2L,
            rate.asked.stream().filter(u -> u.contains("virtualMatchString=cpe%3A2.3%3Aa%3Aredis%3Aredis%3A7.0.0")).count());

        System.out.println("the NVD answers 503 twice: the check fails, the component says so");
        RecordedSourceClient down = new RecordedSourceClient();
        down.failures.put("virtualMatchString=cpe%3A2.3%3Aa%3Aredis%3Aredis%3A7.0.0", "status:503");
        Map<String, Object> r4 = run(down, dir, javaList(20));
        equal("nvd failed", "http-error", check(r4, "nvd"));
        check("store lists nvd", strings(byId(r4, "components", "store"), "failedChecks").contains("nvd"));
        equal("store still vulnerable (the early warning holds), never clean", "vulnerable", JsonTree.string(byId(r4, "components", "store"), "status"));

        System.out.println("an OSV detail lookup fails");
        RecordedSourceClient osv = new RecordedSourceClient();
        osv.failures.put("vulns/GHSA-xg75-68x3-7p3q", "status:500");
        Map<String, Object> r5 = run(osv, dir, javaList(20));
        equal("osv failed", "http-error", check(r5, "osv"));
        check("the library list lists osv", strings(byId(r5, "components", "server-libraries"), "failedChecks").contains("osv"));
        Map<String, Object> struts5 = byId(r5, "actions", "code-change:server-libraries:org.apache.struts:struts2-core");
        check("the flaw not looked up stays in the library's one action, uncertain",
            struts5 != null && JsonTree.integer(JsonTree.object(struts5, "counts"), "uncertain") == 1
            && strings(byId(r5, "components", "server-libraries"), "actionIds").size() == 1);

        System.out.println("an OSV advisory withdrawn (404)");
        RecordedSourceClient gone = new RecordedSourceClient();
        gone.failures.put("vulns/GHSA-xg75-68x3-7p3q", "status:404");
        Map<String, Object> r5b = run(gone, dir, javaList(20));
        equal("osv stays ok", "ok", check(r5b, "osv"));
        Map<String, Object> struts5b = byId(r5b, "actions", "code-change:server-libraries:org.apache.struts:struts2-core");
        check("the withdrawn flaw is dropped, not kept as unknown", struts5b != null
            && JsonTree.integer(JsonTree.object(struts5b, "counts"), "total") == 1
            && JsonTree.integer(JsonTree.object(struts5b, "counts"), "uncertain") == 0);

        System.out.println("no build timestamp (a run from an IDE): every field still as the contract says");
        RuntimeFacts.Values ide = new RuntimeFacts.Values("21.0.9", null, "Example OS 3.2.1 LTS", "3.2", "7.0.0", null, "development");
        Map<String, Object> r9 = run(new RecordedSourceClient(), dir, javaList(20), ide);
        equal("library list: asOf null, not invented", "{\"version\":null,\"source\":\"build\",\"asOf\":null,\"fact\":null}",
            JsonTree.write(byId(r9, "components", "server-libraries").get("running")));
        equal("the OS image: running null, unknown", "unknown", JsonTree.string(byId(r9, "components", "os"), "status"));
        check("every component, action, advisory and count has exactly the contract's fields", contractShape(r9));
        check("and so does the report with a build timestamp", contractShape(report));

        System.out.println("a component with no running block, on a server with a build timestamp");
        Path copy = Files.createTempDirectory("security-watch-check");
        String inv = RecordedSourceClient.resource("directory/inventory.json").replace("\"components\": [",
            "\"components\": [\n    {\"id\": \"notes\", \"layer\": \"other\", \"name\": \"Notes\", \"where\": \"Elsewhere\", \"managedBy\": \"us\", \"owner\": \"Ops\"},");
        Files.writeString(copy.resolve("inventory.json"), inv);
        Files.writeString(copy.resolve("package-lock.json"), RecordedSourceClient.resource("directory/package-lock.json"));
        Map<String, Object> r10 = run(new RecordedSourceClient(), copy.toString(), javaList(20));
        Map<String, Object> notes = byId(r10, "components", "notes");
        check("the run succeeds and the component is listed, running null", notes != null && notes.get("running") == null
            && "ok".equals(JsonTree.string(notes, "status")));
        check("Docker Hub still asked for the others", "redeploy:os:3.2.1".equals(strings(byId(r10, "components", "os"), "actionIds").get(0)));

        System.out.println("endoflife.date does not answer");
        RecordedSourceClient eol = new RecordedSourceClient();
        eol.failures.put("endoflife.date", "http-error");
        Map<String, Object> r6 = run(eol, dir, javaList(20));
        equal("endoflife failed", "http-error", check(r6, "endoflife"));
        check("store lists endoflife, and no support date is invented",
            strings(byId(r6, "components", "store"), "failedChecks").contains("endoflife")
            && JsonTree.string(byId(r6, "components", "store"), "supportEnds") == null);

        System.out.println("a Java list too short to be one");
        Map<String, Object> r7 = run(new RecordedSourceClient(), dir, javaList(3));
        equal("libraries-java missing", "missing", check(r7, "libraries-java"));
        Map<String, Object> j7 = byId(r7, "components", "server-libraries");
        equal("unknown, not clean", "unknown", JsonTree.string(j7, "status"));
        equal("no package count", null, j7.get("packageCount"));

        System.out.println("no inventory");
        RecordedSourceClient none = new RecordedSourceClient();
        Map<String, Object> r8 = run(none, dir + "/no-such-folder", javaList(20));
        equal("inventory missing", "missing", check(r8, "inventory"));
        equal("the sources were not called", 0, none.asked.size());
        equal("their checks read disabled", "disabled", check(r8, "osv"));
        equal("no components", 0, JsonTree.list(r8, "components").size());

    }
}
