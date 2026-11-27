package org.kadampabookings.kbs.server.securitywatch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check for the decision table of {@link ReportAssembler}, row by row, on fictional components and hand-made findings
 * (the rows a recorded run does not reach: {@link SecurityWatchRunCheck} covers the others on a whole run).
 */
public class ReportAssemblerCheck {

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
    static final LocalDate TODAY = LocalDate.parse("2026-11-27");

    static Inventory inventory(String components) throws Inventory.Invalid {
        return Inventory.parse("{\"schemaVersion\":1,\"eolSoonDays\":180,\"declaredStaleAfterDays\":90,\"imageMaxAgeDays\":14,"
                               + "\"components\":[" + components + "],\"assessed\":[]}");
    }

    static Flaw flaw(String id, String severity, String fixedIn, boolean uncertain, String target, String targetVersion) {
        return new Flaw(id, Set.of(id), null, severity, "Fictional flaw " + id, LocalDate.parse("2026-11-20"), fixedIn,
            uncertain, false, target, targetVersion, null);
    }

    static Map<String, Object> report(Inventory inventory, Map<String, ReportAssembler.Findings> findings,
                                      RuntimeFacts.Values facts, List<Libraries.Library> java) {
        return ReportAssembler.assemble(new ReportAssembler.Input("production", 1, "schedule", NOW, NOW, facts, inventory,
            new LinkedHashMap<>(), java, List.of(), findings), ReportAssembler.MAX_ADVISORIES);
    }

    static Map<String, Object> action(Map<String, Object> report, String id) {
        for (Object item : JsonTree.list(report, "actions"))
            if (id.equals(JsonTree.string(JsonTree.asObject(item), "id")))
                return JsonTree.asObject(item);
        return null;
    }

    static Map<String, Object> component(Map<String, Object> report, int i) {
        return JsonTree.asObject(JsonTree.list(report, "components").get(i));
    }

    static List<String> actionIds(Map<String, Object> report) {
        List<String> ids = new ArrayList<>();
        for (Object item : JsonTree.list(report, "actions"))
            ids.add(JsonTree.string(JsonTree.asObject(item), "id"));
        return ids;
    }

    static final RuntimeFacts.Values FACTS = new RuntimeFacts.Values("21.0.9", "Vendor21.38+21-CA", "Example OS 3.2.1 LTS",
        "3.2", "17.2", "2026-11-25T00:00:00Z", "production");

    static final String LIVE_DB = "{\"id\":\"db\",\"layer\":\"data\",\"name\":\"Database\",\"managedBy\":\"us\",\"owner\":\"Data\","
                                  + "\"running\":{\"source\":\"live\",\"fact\":\"postgresVersion\"},\"howTo\":{\"upgrade\":[\"Upgrade\"]%s}}";

    public static void main(String[] args) throws Exception {
        EndOfLifeSource.Line supported = new EndOfLifeSource.Line("17", "17.2", LocalDate.parse("2026-08-01"),
            LocalDate.parse("2029-11-08"), false, "19.0", LocalDate.parse("2026-11-16"));

        System.out.println("row 3: a library flaw with no fix");
        Inventory libs = inventory("{\"id\":\"libs\",\"layer\":\"application\",\"name\":\"Libraries\",\"managedBy\":\"us\",\"owner\":\"Server\","
                                   + "\"running\":{\"source\":\"build\",\"list\":\"java\"},\"howTo\":{\"code-change\":[\"Change\"],\"wait-vendor\":[\"Wait\"]}}");
        Map<String, Object> r3 = report(libs, Map.of("libs", new ReportAssembler.Findings(
                List.of(flaw("GHSA-aaaa-bbbb-cccc", "high", null, false, "org.example:lib", "1.0")), Set.of(), null, null)),
            FACTS, List.of(new Libraries.Library("org.example:lib", "1.0")));
        Map<String, Object> wait = action(r3, "wait-vendor:libs:org.example:lib");
        check("wait-vendor for the library", wait != null);
        check("no fix available, no target version", !JsonTree.isTrue(wait, "fixAvailable") && wait.get("to") == null);
        equal("how-to of wait-vendor", List.of("Wait"), JsonTree.list(wait, "howTo"));

        System.out.println("one action per library: a fixed and an unfixed flaw together");
        Map<String, Object> rm = report(libs, Map.of("libs", new ReportAssembler.Findings(List.of(
                flaw("GHSA-aaaa-bbbb-cccc", "high", null, false, "org.example:lib", "1.0"),
                flaw("GHSA-dddd-eeee-ffff", "critical", "1.4", false, "org.example:lib", "1.0"),
                flaw("GHSA-gggg-hhhh-iiii", "medium", "1.2", false, "org.example:lib", "1.0")), Set.of(), null, null)),
            FACTS, List.of(new Libraries.Library("org.example:lib", "1.0")));
        equal("one code-change for the library", List.of("code-change:libs:org.example:lib"), actionIds(rm));
        Map<String, Object> lib = action(rm, "code-change:libs:org.example:lib");
        equal("to the lowest version fixing every flaw that has a fix", "1.4", JsonTree.string(lib, "to"));
        equal("three flaws, none uncertain", "{\"total\":3,\"critical\":1,\"high\":1,\"medium\":1,\"low\":0,\"uncertain\":0}",
            JsonTree.write(lib.get("counts")));
        check("the unfixed flaw stays in it with fixedIn null", JsonTree.list(lib, "advisories").stream()
            .anyMatch(a -> "GHSA-aaaa-bbbb-cccc".equals(JsonTree.string(JsonTree.asObject(a), "id")) && JsonTree.asObject(a).get("fixedIn") == null));

        System.out.println("one package installed at two versions, and one CVE reached through two records");
        List<Flaw> twoVersions = List.of(
            new Flaw("CVE-2026-0040", Set.of("GHSA-aaaa-0000-0001", "CVE-2026-0040"), 7.5, "high", "one flaw", LocalDate.parse("2026-11-01"),
                "1.5", false, false, "pkg", "1.0", null),
            new Flaw("CVE-2026-0040", Set.of("GHSA-aaaa-0000-0001", "CVE-2026-0040"), 7.5, "high", "one flaw", LocalDate.parse("2026-11-01"),
                "2.3", false, false, "pkg", "2.0", null),
            new Flaw("CVE-2026-0041", Set.of("GHSA-aaaa-0000-0002", "CVE-2026-0041"), null, "medium", null, null,
                null, false, false, "pkg", "2.0", null),
            new Flaw("CVE-2026-0041", Set.of("OSV-2026-0041", "CVE-2026-0041"), 9.1, "critical", "the complete record", LocalDate.parse("2026-11-02"),
                "2.2", false, false, "pkg", "2.0", null));
        Map<String, Object> rv2 = report(libs, Map.of("libs", new ReportAssembler.Findings(twoVersions, Set.of(), null, null)),
            FACTS, List.of(new Libraries.Library("pkg", "2.0"), new Libraries.Library("pkg", "1.0")));
        equal("one action for the package, whatever its versions", List.of("code-change:libs:pkg"), actionIds(rv2));
        Map<String, Object> pkg = action(rv2, "code-change:libs:pkg");
        equal("from the lowest installed version", "1.0", JsonTree.string(pkg, "from"));
        equal("to a version fixing every installed one", "2.3", JsonTree.string(pkg, "to"));
        List<String> advisoryIds = JsonTree.list(pkg, "advisories").stream().map(a -> JsonTree.string(JsonTree.asObject(a), "id")).toList();
        equal("each flaw once", List.of("CVE-2026-0041", "CVE-2026-0040"), advisoryIds);
        equal("counted once", 2, JsonTree.integer(JsonTree.object(pkg, "counts"), "total"));
        Map<String, Object> kept = JsonTree.asObject(JsonTree.list(pkg, "advisories").get(0));
        check("the more complete record is the one kept", "the complete record".equals(JsonTree.string(kept, "summary"))
            && Double.valueOf(9.1).equals(JsonTree.number(kept, "cvss")));
        equal("and its severity counts", "critical", JsonTree.string(pkg, "severity"));

        System.out.println("row 5: a declared version with a certain flaw");
        Inventory declared = inventory("{\"id\":\"box\",\"layer\":\"other\",\"name\":\"Box\",\"managedBy\":\"other-party\",\"owner\":\"Partner\","
                                       + "\"running\":{\"source\":\"declared\",\"version\":\"7.1.1\",\"asOf\":\"2026-11-20\"},\"howTo\":{\"verify\":[\"Ask\"]}}");
        Map<String, Object> r5 = report(declared, Map.of("box", new ReportAssembler.Findings(
            List.of(flaw("CVE-2026-0005", "critical", "7.1.2", false, null, null)), Set.of(), null, null)), FACTS, null);
        equal("verify, not a fix", List.of("verify:box:7.1.1"), actionIds(r5));
        check("a fix is known, so fixAvailable", JsonTree.isTrue(action(r5, "verify:box:7.1.1"), "fixAvailable"));
        equal("critical: due 7 days after publication", "2026-11-27", JsonTree.string(action(r5, "verify:box:7.1.1"), "dueDate"));
        equal("status vulnerable", "vulnerable", JsonTree.string(component(r5, 0), "status"));
        equal("a declared version: the flaw only may apply", 1,
            JsonTree.integer(JsonTree.object(action(r5, "verify:box:7.1.1"), "counts"), "uncertain"));
        check("and the advisory says so", JsonTree.isTrue(JsonTree.asObject(JsonTree.list(action(r5, "verify:box:7.1.1"), "advisories").get(0)), "uncertain"));
        equal("no needs on a verify", List.of(), JsonTree.list(action(r5, "verify:box:7.1.1"), "needs"));

        System.out.println("row 8, redeploy kind: a fix exists but nothing newer than what runs");
        Inventory runtime = inventory("{\"id\":\"rt\",\"layer\":\"runtime\",\"name\":\"Runtime\",\"managedBy\":\"us\",\"owner\":\"Server\","
                                      + "\"running\":{\"source\":\"live\",\"fact\":\"javaVersion\"},\"howTo\":{\"redeploy\":[\"Redeploy\"],\"wait-vendor\":[\"Mitigate\"]}}");
        EndOfLifeSource.Line same = new EndOfLifeSource.Line("21", "21.0.9", LocalDate.parse("2026-10-21"),
            LocalDate.parse("2031-09-30"), false, "25.0.1", null);
        Map<String, Object> r8 = report(runtime, Map.of("rt", new ReportAssembler.Findings(
            List.of(flaw("CVE-2026-0008", "high", "21.0.10", false, null, null)), Set.of(),
            same, Instant.parse("2026-11-20T00:00:00Z"))), FACTS, null);
        equal("wait-vendor: a redeploy would change nothing", List.of("wait-vendor:rt:21.0.9"), actionIds(r8));
        equal("its how-to", List.of("Mitigate"), JsonTree.list(action(r8, "wait-vendor:rt:21.0.9"), "howTo"));
        Map<String, Object> r8b = report(runtime, Map.of("rt", new ReportAssembler.Findings(
            List.of(flaw("CVE-2026-0008", "high", "21.0.10", false, null, null)), Set.of(),
            same, Instant.parse("2026-11-26T00:00:00Z"))), FACTS, null);
        equal("the base image is newer than our build: redeploy", List.of("redeploy:rt:21.0.9"), actionIds(r8b));
        equal("no version to name: to null, never a made-up target", null, action(r8b, "redeploy:rt:21.0.9").get("to"));

        System.out.println("a runtime compared through compareFact (the vendor's numbering)");
        Inventory vendor = inventory("{\"id\":\"rt\",\"layer\":\"runtime\",\"name\":\"Runtime\",\"managedBy\":\"us\",\"owner\":\"Server\","
            + "\"running\":{\"source\":\"live\",\"fact\":\"javaVersion\"},"
            + "\"lifecycle\":{\"endoflife\":\"example-jdk\",\"cycle\":\"21\",\"compareFact\":\"javaVendorVersion\"},"
            + "\"howTo\":{\"redeploy\":[\"Redeploy\"]}}");
        EndOfLifeSource.Line vendorNewer = new EndOfLifeSource.Line("21", "21.52.203", LocalDate.parse("2026-08-18"),
            LocalDate.parse("2031-09-30"), false, "25.30.1", null);
        Map<String, Object> rv = report(vendor, Map.of("rt", new ReportAssembler.Findings(List.of(), Set.of(), vendorNewer,
            Instant.parse("2026-11-20T00:00:00Z"))), FACTS, null);
        equal("latest is null: never shown beside the running version in another numbering", null, component(rv, 0).get("latest"));
        equal("the vendor's 21.52.203 is newer than Vendor21.38+21: one redeploy", List.of("redeploy:rt:21.0.9"), actionIds(rv));
        equal("its to is null too", null, action(rv, "redeploy:rt:21.0.9").get("to"));
        equal("status update", "update", JsonTree.string(component(rv, 0), "status"));
        EndOfLifeSource.Line vendorOlder = new EndOfLifeSource.Line("21", "21.30.1", LocalDate.parse("2026-02-01"),
            LocalDate.parse("2031-09-30"), false, "25.30.1", null);
        Map<String, Object> rvo = report(vendor, Map.of("rt", new ReportAssembler.Findings(List.of(), Set.of(), vendorOlder,
            Instant.parse("2026-11-20T00:00:00Z"))), FACTS, null);
        check("the vendor's line older than what runs: ok, no action, latest still null", "ok".equals(JsonTree.string(component(rvo, 0), "status"))
            && actionIds(rvo).isEmpty() && component(rvo, 0).get("latest") == null);

        System.out.println("row 9: a certain flaw with no fix");
        Map<String, Object> r9 = report(inventory(String.format(LIVE_DB, "")), Map.of("db", new ReportAssembler.Findings(
            List.of(flaw("CVE-2026-0009", "medium", null, false, null, null)), Set.of(), supported, null)), FACTS, null);
        equal("wait-vendor", List.of("wait-vendor:db:17.2"), actionIds(r9));
        equal("medium: no due date", null, action(r9, "wait-vendor:db:17.2").get("dueDate"));

        System.out.println("row 10: no flaw, a newer release of the line");
        EndOfLifeSource.Line newer = new EndOfLifeSource.Line("17", "17.12", LocalDate.parse("2026-11-09"),
            LocalDate.parse("2029-11-08"), false, "19.0", null);
        Map<String, Object> r10 = report(inventory(String.format(LIVE_DB, "")), Map.of("db", new ReportAssembler.Findings(
            List.of(), Set.of(), newer, null)), FACTS, null);
        Map<String, Object> update = action(r10, "upgrade:db:17.12");
        check("upgrade to the newest release", update != null);
        equal("severity none", "none", JsonTree.string(update, "severity"));
        equal("published on the release day", "2026-11-09", JsonTree.string(update, "publishedOn"));
        equal("no due date", null, update.get("dueDate"));
        equal("status update", "update", JsonTree.string(component(r10, 0), "status"));
        equal("latest shown", "{\"version\":\"17.12\",\"releasedOn\":\"2026-11-09\"}", JsonTree.write(component(r10, 0).get("latest")));

        System.out.println("row 10, except a declared version that is only the release line");
        Inventory line = inventory("{\"id\":\"runner\",\"layer\":\"other\",\"name\":\"Runner\",\"managedBy\":\"us\",\"owner\":\"Ops\","
                                   + "\"running\":{\"source\":\"declared\",\"version\":\"2023\",\"asOf\":\"2026-11-20\"},"
                                   + "\"lifecycle\":{\"endoflife\":\"example-os\",\"cycle\":\"2023\"},\"howTo\":{\"verify\":[\"Look\"]}}");
        EndOfLifeSource.Line os2023 = new EndOfLifeSource.Line("2023", "2023.12.20261120.0", LocalDate.parse("2026-11-23"),
            LocalDate.parse("2029-06-30"), false, "2023.12.20261120.0", null);
        Map<String, Object> rl = report(line, Map.of("runner", new ReportAssembler.Findings(List.of(), Set.of(), os2023, null)), FACTS, null);
        equal("no update action for a line", List.of(), actionIds(rl));
        equal("status ok, not update", "ok", JsonTree.string(component(rl, 0), "status"));
        check("the newest release and the support date are still shown", component(rl, 0).get("latest") != null
            && "2029-06-30".equals(JsonTree.string(component(rl, 0), "supportEnds")));
        EndOfLifeSource.Line ending2023 = new EndOfLifeSource.Line("2023", "2023.12.20261120.0", LocalDate.parse("2026-11-23"),
            LocalDate.parse("2027-03-01"), false, "2025.1.1", null);
        Map<String, Object> rle = report(line, Map.of("runner", new ReportAssembler.Findings(List.of(), Set.of(), ending2023, null)), FACTS, null);
        equal("a line whose support ends soon: eol-soon", "eol-soon", JsonTree.string(component(rle, 0), "status"));
        Inventory linePatch = inventory("{\"id\":\"runner\",\"layer\":\"other\",\"name\":\"Runner\",\"managedBy\":\"us\",\"owner\":\"Ops\","
                                        + "\"running\":{\"source\":\"declared\",\"version\":\"2023.9.1\",\"asOf\":\"2026-11-20\"},"
                                        + "\"lifecycle\":{\"endoflife\":\"example-os\",\"cycle\":\"2023\"},\"howTo\":{\"upgrade\":[\"Patch\"]}}");
        Map<String, Object> rp = report(linePatch, Map.of("runner", new ReportAssembler.Findings(List.of(), Set.of(), os2023, null)), FACTS, null);
        equal("a declared full version still gets its update", "update", JsonTree.string(component(rp, 0), "status"));

        System.out.println("one action for everything about a component");
        Inventory full = inventory("{\"id\":\"db\",\"layer\":\"data\",\"name\":\"Database\",\"managedBy\":\"us\",\"owner\":\"Data\","
            + "\"needs\":[\"maintenance-window\"],\"running\":{\"source\":\"live\",\"fact\":\"postgresVersion\"},"
            + "\"howTo\":{\"upgrade\":[\"Upgrade\"],\"decide\":[\"Choose\"]}}");
        EndOfLifeSource.Line endingNewer = new EndOfLifeSource.Line("17", "17.12", LocalDate.parse("2026-11-09"),
            LocalDate.parse("2027-03-31"), false, "19.0", null);
        Map<String, Object> rf1 = report(full, Map.of("db", new ReportAssembler.Findings(List.of(
            flaw("CVE-2026-0030", "high", "17.8", false, null, null),
            new Flaw("CVE-2026-0031", Set.of("CVE-2026-0031"), null, "high", "early", LocalDate.parse("2026-11-25"), null, true, true, null, null, null)),
            Set.of(), endingNewer, null)), FACTS, null);
        equal("certain flaw, uncertain flaw, newer release, support ending: ONE action", List.of("upgrade:db:19.0"), actionIds(rf1));
        Map<String, Object> one = action(rf1, "upgrade:db:19.0");
        equal("counts with the uncertain one", "{\"total\":2,\"critical\":0,\"high\":2,\"medium\":0,\"low\":0,\"uncertain\":1}",
            JsonTree.write(one.get("counts")));
        equal("how-to: the kind's steps, then decide's (support ends soon)", List.of("Upgrade", "Choose"), JsonTree.list(one, "howTo"));
        equal("needs on a fix kind", List.of("maintenance-window"), JsonTree.list(one, "needs"));

        System.out.println("row 5: decide only when nothing else applies");
        Inventory onlyDecide = inventory("{\"id\":\"app\",\"layer\":\"other\",\"name\":\"App\",\"managedBy\":\"other-party\",\"owner\":\"Partner\","
            + "\"needs\":[\"provider-console\"],\"running\":{\"source\":\"live\",\"fact\":\"postgresVersion\"},\"howTo\":{\"decide\":[\"Choose\"]}}");
        EndOfLifeSource.Line ends = new EndOfLifeSource.Line("17", "17.2", LocalDate.parse("2026-08-01"),
            LocalDate.parse("2027-03-31"), false, "19.0", null);
        Map<String, Object> rd5 = report(onlyDecide, Map.of("app", new ReportAssembler.Findings(List.of(), Set.of(), ends, null)), FACTS, null);
        Map<String, Object> decideOnly = action(rd5, "decide:app:17.2");
        check("decide", decideOnly != null);
        equal("due on the support end", "2027-03-31", JsonTree.string(decideOnly, "dueDate"));
        equal("its steps once", List.of("Choose"), JsonTree.list(decideOnly, "howTo"));
        equal("no needs on decide", List.of(), JsonTree.list(decideOnly, "needs"));

        System.out.println("row 12 with decide, and eol-soon");
        EndOfLifeSource.Line ending = new EndOfLifeSource.Line("17", "17.2", LocalDate.parse("2026-08-01"),
            LocalDate.parse("2027-03-31"), false, "19.0", null);
        Map<String, Object> r12 = report(inventory(String.format(LIVE_DB, ",\"decide\":[\"Choose\"]")), Map.of("db",
            new ReportAssembler.Findings(List.of(), Set.of(), ending, null)), FACTS, null);
        equal("a fix kind and support ending soon: the fix to the newest maintained line, not decide",
            List.of("upgrade:db:19.0"), actionIds(r12));
        equal("with the decide steps after its own", List.of("Upgrade", "Choose"), JsonTree.list(action(r12, "upgrade:db:19.0"), "howTo"));
        equal("status eol-soon", "eol-soon", JsonTree.string(component(r12, 0), "status"));
        EndOfLifeSource.Line ended = new EndOfLifeSource.Line("17", "17.2", LocalDate.parse("2026-08-01"),
            LocalDate.parse("2026-11-01"), true, "19.0", null);
        Map<String, Object> r12b = report(inventory(String.format(LIVE_DB, "")), Map.of("db",
            new ReportAssembler.Findings(List.of(), Set.of(), ended, null)), FACTS, null);
        equal("no decide how-to: the fix kind to the newest maintained line", List.of("upgrade:db:19.0"), actionIds(r12b));
        equal("published on the support end", "2026-11-01", JsonTree.string(action(r12b, "upgrade:db:19.0"), "publishedOn"));
        equal("status eol", "eol", JsonTree.string(component(r12b, 0), "status"));
        Inventory moving = inventory("{\"id\":\"proxy\",\"layer\":\"edge\",\"name\":\"Proxy\",\"managedBy\":\"us\",\"owner\":\"Server\","
            + "\"running\":{\"source\":\"declared\",\"version\":\"1.27\",\"asOf\":\"2026-11-20\"},"
            + "\"lifecycle\":{\"endoflife\":\"example-proxy\",\"cycle\":\"1.27\",\"moveTo\":\"stable\"},\"howTo\":{\"code-change\":[\"Change the image\"]}}");
        EndOfLifeSource.Line proxyEnded = new EndOfLifeSource.Line("1.27", "1.27.5", LocalDate.parse("2025-04-16"),
            LocalDate.parse("2025-06-24"), true, "1.31.10", null);
        Map<String, Object> rmv = report(moving, Map.of("proxy", new ReportAssembler.Findings(List.of(
            flaw("CVE-2026-0060", "high", "1.27.4", false, null, null)), Set.of(), proxyEnded, null)), FACTS, null);
        equal("moveTo replaces the suggested version", List.of("code-change:proxy:stable"), actionIds(rmv));
        equal("to = stable, as written", "stable", JsonTree.string(action(rmv, "code-change:proxy:stable"), "to"));

        System.out.println("statuses: unknown unless already worse");
        Map<String, Object> failedOnly = report(inventory(String.format(LIVE_DB, "")), Map.of("db",
            new ReportAssembler.Findings(List.of(), Set.of("nvd"), supported, null)), FACTS, null);
        equal("a failed check, nothing else: unknown", "unknown", JsonTree.string(component(failedOnly, 0), "status"));
        Map<String, Object> failedEol = report(inventory(String.format(LIVE_DB, "")), Map.of("db",
            new ReportAssembler.Findings(List.of(), Set.of("nvd"), ended, null)), FACTS, null);
        equal("a failed check, support ended: eol", "eol", JsonTree.string(component(failedEol, 0), "status"));
        Map<String, Object> clean = report(inventory(String.format(LIVE_DB, "")), Map.of("db",
            new ReportAssembler.Findings(List.of(), Set.of(), supported, null)), FACTS, null);
        equal("nothing found: ok", "ok", JsonTree.string(component(clean, 0), "status"));

        System.out.println("the NVD severity floor, and a known-exploited flaw below it");
        Inventory floor = inventory("{\"id\":\"nas\",\"layer\":\"other\",\"name\":\"NAS\",\"managedBy\":\"us\",\"owner\":\"Ops\","
                                    + "\"running\":{\"source\":\"declared\",\"version\":\"1.0\",\"asOf\":\"2026-11-20\"},"
                                    + "\"flaws\":{\"nvd\":{\"products\":[\"cpe:2.3:a:example:nas\"],\"minSeverity\":\"critical\"}}}");
        Flaw high = flaw("CVE-2026-0011", "high", null, false, null, null);
        Flaw exploited = flaw("CVE-2026-0012", "medium", null, false, null, null).withKev(LocalDate.parse("2026-11-25"));
        Map<String, Object> rf = report(floor, Map.of("nas", new ReportAssembler.Findings(List.of(high, exploited), Set.of(), null, null)), FACTS, null);
        equal("only the exploited one counts", 1, JsonTree.integer(component(rf, 0), "openAdvisories"));
        equal("due 3 days after CISA listed it", "2026-11-28", JsonTree.string(action(rf, "verify:nas:1.0"), "dueDate"));

        System.out.println("the OS image without a build time (development)");
        Inventory os = inventory("{\"id\":\"os\",\"layer\":\"runtime\",\"name\":\"OS\",\"managedBy\":\"us\",\"owner\":\"Server\","
                                 + "\"running\":{\"source\":\"live\",\"fact\":\"osRelease\"},\"howTo\":{\"redeploy\":[\"Redeploy\"]}}");
        RuntimeFacts.Values dev = new RuntimeFacts.Values("21.0.9", null, "Example OS 26.04 LTS", "26.04", "17.2", null, "development");
        Map<String, Object> rd = report(os, Map.of("os", new ReportAssembler.Findings(List.of(), Set.of("dockerhub"), null, null)), dev, null);
        Map<String, Object> osc = component(rd, 0);
        check("running null, unknown, no failed checks, no action", osc.get("running") == null
            && "unknown".equals(JsonTree.string(osc, "status")) && JsonTree.list(osc, "failedChecks").isEmpty() && actionIds(rd).isEmpty());

        System.out.println("the OS image with a build time");
        EndOfLifeSource.Line osLine = new EndOfLifeSource.Line("3.2", "3.2.4", LocalDate.parse("2026-09-01"),
            LocalDate.parse("2029-06-01"), false, "4.0.1", null);
        Map<String, Object> ro = report(os, Map.of("os", new ReportAssembler.Findings(List.of(), Set.of(), osLine,
            Instant.parse("2026-11-26T00:00:00Z"))), FACTS, null);
        equal("latest is null: the image has no version of its own", null, component(ro, 0).get("latest"));
        equal("the support date still shown", "2029-06-01", JsonTree.string(component(ro, 0), "supportEnds"));
        equal("a newer base image: one redeploy", List.of("redeploy:os:3.2.1"), actionIds(ro));
        equal("its to is null", null, action(ro, "redeploy:os:3.2.1").get("to"));
        Map<String, Object> roFresh = report(os, Map.of("os", new ReportAssembler.Findings(List.of(), Set.of(), osLine,
            Instant.parse("2026-11-20T00:00:00Z"))), FACTS, null);
        equal("a newer point release alone is not an update of the image", "ok", JsonTree.string(component(roFresh, 0), "status"));

        System.out.println("assessed flaws");
        Inventory assessed = Inventory.parse("{\"schemaVersion\":1,\"components\":[" + String.format(LIVE_DB, "") + "],\"assessed\":["
            + "{\"id\":\"CVE-2026-0020\",\"componentId\":\"db\",\"decision\":\"not-applicable\",\"reason\":\"r\",\"decidedOn\":\"2026-11-01\",\"reviewBy\":\"2027-01-01\"},"
            + "{\"id\":\"CVE-2026-0021\",\"componentId\":\"db\",\"decision\":\"accepted\",\"reason\":\"r\",\"decidedOn\":\"2026-01-01\",\"reviewBy\":\"2026-11-26\"}]}");
        Map<String, Object> ra = report(assessed, Map.of("db", new ReportAssembler.Findings(List.of(
            flaw("CVE-2026-0020", "high", "17.3", false, null, null), flaw("CVE-2026-0021", "high", "17.3", false, null, null)),
            Set.of(), supported, null)), FACTS, null);
        equal("one in force is left out, one past its review counts again", 1, JsonTree.integer(component(ra, 0), "openAdvisories"));
        equal("assessed[] lists the one in force", 1, JsonTree.list(ra, "assessed").size());

        System.out.println("size cap: fewer advisories when the report is too big");
        List<Flaw> many = new ArrayList<>();
        for (int i = 0; i < 600 * 25; i++)
            many.add(new Flaw("CVE-2026-" + (10000 + i), Set.of("CVE-2026-" + (10000 + i)), 7.5, "high", "x".repeat(300),
                LocalDate.parse("2026-11-20"), "9." + i, false, false, "org.example:lib" + (i % 600), "1.0", null));
        String big = ReportAssembler.assembleJson(new ReportAssembler.Input("production", 1, "schedule", NOW, NOW, FACTS, libs,
            new LinkedHashMap<>(), List.of(new Libraries.Library("org.example:lib", "1.0")), List.of(),
            Map.of("libs", new ReportAssembler.Findings(many, Set.of(), null, null))));
        int shown = JsonTree.list(JsonTree.asObject(JsonTree.list(JsonTree.parseObject(big), "actions").get(0)), "advisories").size();
        check("600 libraries with 25 flaws each: under 512 KB (" + big.length() + " characters), " + shown + " advisories per action",
            big.length() <= ReportAssembler.MAX_REPORT_CHARS && shown < ReportAssembler.MAX_ADVISORIES);

        System.out.println("size cap: too large even without advisories -> the run fails, nothing over the limit is stored");
        List<Flaw> huge = new ArrayList<>();
        for (int i = 0; i < 5000; i++)
            huge.add(new Flaw("CVE-2026-" + (20000 + i), Set.of("CVE-2026-" + (20000 + i)), 7.5, "high", "y", LocalDate.parse("2026-11-20"),
                "9.0", false, false, "org.example.with.a.long.group.name.to.fill.the.report:library-number-" + i, "1.0", null));
        try {
            String tooBig = ReportAssembler.assembleJson(new ReportAssembler.Input("production", 1, "schedule", NOW, NOW, FACTS, libs,
                new LinkedHashMap<>(), List.of(new Libraries.Library("org.example:lib", "1.0")), List.of(),
                Map.of("libs", new ReportAssembler.Findings(huge, Set.of(), null, null))));
            check("5000 one-flaw libraries: refused, not stored (got " + tooBig.length() + " characters)", false);
        } catch (SecurityWatchRunner.RunFailure e) {
            equal("5000 one-flaw libraries: the run fails with", "too-large", e.code());
        }

        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail > 0 ? 1 : 0);
    }
}
