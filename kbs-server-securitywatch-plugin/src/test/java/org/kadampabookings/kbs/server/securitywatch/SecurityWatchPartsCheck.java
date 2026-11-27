package org.kadampabookings.kbs.server.securitywatch;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Check for the pure parts the check is built from: version comparison, CVSS scores, the JSON reader, the library
 * lists, OSV's fixed version, the NVD's applicability, KEV name matching, text cleaning, the inventory reader and the
 * urgent mail's selection and escaping. Fictional values and recorded public answers only.
 */
public class SecurityWatchPartsCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    static void equal(String what, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        check(what + (ok ? "" : "  (expected " + expected + ", got " + actual + ")"), ok);
    }

    static boolean throwsIllegal(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("versions");
        check("17.2 < 17.12", Versions.compare("17.2", "17.12") < 0);
        check("1.27 = 1.27.0", Versions.compare("1.27", "1.27.0") == 0);
        check("2.15.0-rc1 < 2.15.0", Versions.compare("2.15.0-rc1", "2.15.0") < 0);
        check("19.0.0-rc.1 < 19.0.0", Versions.compare("19.0.0-rc.1", "19.0.0") < 0);
        check("5.3.39.Final = 5.3.39", Versions.compare("5.3.39.Final", "5.3.39") == 0);
        check("2.5.10 < 2.5.10.1", Versions.compare("2.5.10", "2.5.10.1") < 0);
        check("9u101 < 9u103-b02", Versions.compare("9u101", "9u103-b02") < 0);
        check("v1.2.3 = 1.2.3", Versions.compare("v1.2.3", "1.2.3") == 0);
        check("1.0.1 > 1.0.rc1", Versions.compare("1.0.1", "1.0.rc1") > 0);
        check("2.0-SNAPSHOT < 2.0", Versions.compare("2.0-SNAPSHOT", "2.0") < 0);
        equal("major of 17.2", "17", Versions.leadingNumbers("17.2", 1));
        equal("minor of 7.0.15", "7.0", Versions.leadingNumbers("7.0.15", 2));
        equal("version in a release name", "3.2.1", Versions.firstVersionIn("Example OS 3.2.1 LTS"));
        equal("comparable vendor version", "11.70.15", ReportAssembler.comparable("Vendor11.70+15-CA"));

        System.out.println("CVSS v3");
        equal("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H = 9.8", 9.8, Cvss.baseScore("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"));
        equal("AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N = 4.3", 4.3, Cvss.baseScore("CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:N/A:N"));
        equal("AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:H/A:H = 7.4", 7.4, Cvss.baseScore("CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:H/A:H"));
        equal("AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N = 6.1", 6.1, Cvss.baseScore("CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N"));
        equal("AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H = 7.8", 7.8, Cvss.baseScore("CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H"));
        equal("3.0, scope changed, temporal metric ignored = 10.0", 10.0, Cvss.baseScore("CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H/E:H"));
        equal("no impact = 0.0", 0.0, Cvss.baseScore("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N"));
        equal("an incomplete vector scores nothing", null, Cvss.baseScore("CVSS:3.1/AV:N/AC:L"));
        equal("a v2 vector scores nothing", null, Cvss.baseScore("AV:N/AC:L/Au:N/C:P/I:P/A:P"));
        equal("bands", List.of("critical", "high", "medium", "low", "none"),
            List.of(Cvss.severityOf(9.0), Cvss.severityOf(7.0), Cvss.severityOf(4.0), Cvss.severityOf(0.1), Cvss.severityOf(0.0)));
        equal("GHSA's MODERATE is medium", "medium", Cvss.severityWord("MODERATE"));

        System.out.println("JSON reader");
        equal("nested values", "{\"a\":[1,2.5,-3e2,true,null,\"x\\n\\u00e9\"]}",
            JsonTree.write(JsonTree.parse("{\"a\":[1,2.5,-3e2,true,null,\"x\\n\\u00e9\"]}")).replace("é", "\\u00e9").replace("-300", "-3e2"));
        check("trailing characters refused", throwsIllegal(() -> JsonTree.parse("{} x")));
        check("a raw control character refused", throwsIllegal(() -> JsonTree.parse("\"a\u0001\"")));
        check("deep nesting refused", throwsIllegal(() -> JsonTree.parse("[".repeat(100) + "]".repeat(100))));
        check("a leading zero refused", throwsIllegal(() -> JsonTree.parse("01")));
        check("an unterminated string refused", throwsIllegal(() -> JsonTree.parse("\"abc")));

        System.out.println("library lists");
        List<Libraries.Library> java = Libraries.parseJavaList("The following files have been resolved:\n"
            + "\u001B[36m   org.example:a:jar:1.0\u001B[m -- module a\n   org.example:b:jar:tests:2.0:compile\n"
            + "   org.example:c:jar:3.0:runtime -- module c (auto)\n   org.example:a:jar:1.0\n\n");
        equal("coordinates, colour codes stripped, classifier and scope handled, duplicates once",
            List.of("org.example:a@1.0", "org.example:b@2.0", "org.example:c@3.0"),
            java.stream().map(l -> l.name() + "@" + l.version()).toList());
        List<Libraries.Library> optional = Libraries.parseJavaList("The following files have been resolved:\n"
            + "   org.example:opt:jar:1.0:compile (optional)\n   org.example:opt2:jar:2.0 (optional) -- module opt2\n"
            + "   org.example:opt3:jar:3.0:runtime (optional) -- module opt3 (auto)\n");
        equal("an optional dependency is kept, with or without a module part",
            List.of("org.example:opt@1.0", "org.example:opt2@2.0", "org.example:opt3@3.0"),
            optional.stream().map(l -> l.name() + "@" + l.version()).toList());
        equal("a jar in a repository layout", new Libraries.Library("org.example.group:lib", "1.2.3"),
            Libraries.fromRepositoryPath("/home/x/.m2/repository/org/example/group/lib/1.2.3/lib-1.2.3.jar"));
        equal("a snapshot with a timestamped file", new Libraries.Library("org.example:lib", "1.0-SNAPSHOT"),
            Libraries.fromRepositoryPath("/r/repository/org/example/lib/1.0-SNAPSHOT/lib-1.0-20260101.120000-3.jar"));
        equal("a classes folder is not a library", null, Libraries.fromRepositoryPath("/work/module/target/classes"));
        equal("class path", 1, Libraries.fromClassPath("/work/target/classes" + File.pathSeparator
            + "/r/repository/org/example/lib/1.0/lib-1.0.jar", File.pathSeparator).size());
        check("a lockfile without packages is refused", throwsIllegal(() -> Libraries.parseNpmLock("{\"lockfileVersion\":1}")));

        System.out.println("OSV fixed versions (recorded advisories)");
        Map<String, Object> struts = JsonTree.parseObject(RecordedSourceClient.resource("recorded/osv-vuln-GHSA-j77q-2qqg-6989.json"));
        equal("the fix of the range containing 2.5.10", "2.5.10.1", OsvSource.fixedIn(struts, "Maven", "org.apache.struts:struts2-core", "2.5.10"));
        equal("the fix of the 2.3 line for 2.3.5", "2.3.32", OsvSource.fixedIn(struts, "Maven", "org.apache.struts:struts2-core", "2.3.5"));
        Flaw f = OsvSource.flawOf(struts, "Maven", new Libraries.Library("org.apache.struts:struts2-core", "2.5.10"));
        equal("the CVE alias is shown", "CVE-2017-5638", f.id());
        check("both ids kept for matching", f.ids().containsAll(Set.of("GHSA-j77q-2qqg-6989", "CVE-2017-5638")));
        equal("the advisory's own rating", "critical", f.severity());
        Map<String, Object> unfixed = JsonTree.parseObject("{\"id\":\"GHSA-xxxx-xxxx-xxxx\",\"affected\":[{\"package\":{\"ecosystem\":\"npm\",\"name\":\"p\"},"
            + "\"ranges\":[{\"type\":\"SEMVER\",\"events\":[{\"introduced\":\"0\"}]}]}]}");
        equal("introduced and never fixed: no fix", null, OsvSource.fixedIn(unfixed, "npm", "p", "1.0.0"));
        Map<String, Object> noScore = JsonTree.parseObject("{\"id\":\"GHSA-yyyy-yyyy-yyyy\",\"summary\":\"s\",\"severity\":[{\"type\":\"CVSS_V4\",\"score\":\"CVSS:4.0/AV:N\"}]}");
        equal("no rating and no v3 vector: medium", "medium", OsvSource.flawOf(noScore, "npm", new Libraries.Library("p", "1")).severity());

        System.out.println("NVD applicability (recorded CVEs)");
        Map<String, Object> nvd = JsonTree.parseObject(RecordedSourceClient.resource("recorded/nvd-version.json"));
        Map<String, Object> cve = null;
        for (Object v : JsonTree.list(nvd, "vulnerabilities")) {
            Map<String, Object> c = JsonTree.object(JsonTree.asObject(v), "cve");
            if ("CVE-2023-28856".equals(JsonTree.string(c, "id")))
                cve = c;
        }
        List<String> redis = List.of("cpe:2.3:a:redis:redis");
        equal("7.0.0 is in [7.0.0, 7.0.11): fixed in 7.0.11", new NvdSource.Match(NvdSource.Applicability.APPLIES, "7.0.11"),
            NvdSource.evaluate(cve, redis, "7.0.0"));
        equal("7.0.11 is out", NvdSource.Applicability.NOT_APPLICABLE, NvdSource.evaluate(cve, redis, "7.0.11").applicability());
        equal("6.1.0 falls between the ranges", NvdSource.Applicability.NOT_APPLICABLE, NvdSource.evaluate(cve, redis, "6.1.0").applicability());
        equal("another product's prefix: not ours", NvdSource.Applicability.OTHER_PRODUCT,
            NvdSource.evaluate(cve, List.of("cpe:2.3:a:example:other"), "7.0.0").applicability());
        equal("no version to compare", NvdSource.Applicability.PRODUCT_NO_VERSION, NvdSource.evaluate(cve, redis, null).applicability());
        Map<String, Object> unbounded = JsonTree.parseObject("{\"id\":\"CVE-2000-0001\",\"configurations\":[{\"nodes\":[{\"operator\":\"OR\",\"cpeMatch\":["
            + "{\"vulnerable\":true,\"criteria\":\"cpe:2.3:a:redis:redis:*:*:*:*:*:*:*:*\"}]}]}]}");
        equal("an unbounded wildcard says nothing", NvdSource.Applicability.NOT_APPLICABLE, NvdSource.evaluate(unbounded, redis, "7.0.0").applicability());
        Map<String, Object> platform = JsonTree.parseObject("{\"id\":\"CVE-2000-0002\",\"configurations\":[{\"operator\":\"AND\",\"nodes\":["
            + "{\"operator\":\"OR\",\"cpeMatch\":[{\"vulnerable\":true,\"criteria\":\"cpe:2.3:a:example:plugin:1.0:*:*:*:*:*:*:*\"}]},"
            + "{\"operator\":\"OR\",\"cpeMatch\":[{\"vulnerable\":false,\"criteria\":\"cpe:2.3:a:redis:redis:7.0.0:*:*:*:*:*:*:*\"}]}]}]}");
        equal("our product only as the platform: not ours", NvdSource.Applicability.OTHER_PRODUCT, NvdSource.evaluate(platform, redis, "7.0.0").applicability());
        Map<String, Object> exact = JsonTree.parseObject("{\"id\":\"CVE-2000-0003\",\"published\":\"2026-11-01T00:00:00.000\",\"configurations\":[{\"nodes\":[{\"operator\":\"OR\",\"cpeMatch\":["
            + "{\"vulnerable\":true,\"criteria\":\"cpe:2.3:o:example:box:7.1.1:*:*:*:*:*:*:*\"}]}]}],\"metrics\":{\"cvssMetricV31\":[{\"type\":\"Secondary\",\"cvssData\":{\"baseScore\":6.0}},{\"type\":\"Primary\",\"cvssData\":{\"baseScore\":9.1}}]}}");
        equal("an exact version applies, no fix known", new NvdSource.Match(NvdSource.Applicability.APPLIES, null),
            NvdSource.evaluate(exact, List.of("cpe:2.3:o:example:box"), "7.1.1"));
        equal("the NVD's own (Primary) score first", 9.1, NvdSource.score(exact));
        check("a rejected CVE is dropped", NvdSource.flawOf(JsonTree.parseObject("{\"id\":\"CVE-2000-0004\",\"vulnStatus\":\"Rejected\"}"),
            redis, "7.0.0", NvdSource.Origin.KEYWORD) == null);
        Map<String, Object> unanalysed = JsonTree.parseObject("{\"id\":\"CVE-2000-0005\",\"sourceIdentifier\":\"security@vendor.example\","
            + "\"published\":\"2026-11-20T00:00:00.000\",\"descriptions\":[{\"lang\":\"en\",\"value\":\"A flaw.\"}],\"configurations\":[]}");
        check("an unanalysed keyword hit from the vendor's CNA is kept", NvdSource.flawOf(unanalysed, redis, "7.0.0",
            NvdSource.Origin.KEYWORD, List.of("security@vendor.example")) != null);
        check("from another publisher it is dropped", NvdSource.flawOf(unanalysed, redis, "7.0.0",
            NvdSource.Origin.KEYWORD, List.of("security@other.example")) == null);
        check("with no sources listed every publisher is kept", NvdSource.flawOf(unanalysed, redis, "7.0.0",
            NvdSource.Origin.KEYWORD, List.of()) != null);
        equal("CPE fields split on unescaped colons", 13, NvdSource.splitCpe("cpe:2.3:a:alvaro_herrera:pl\\/php\\:x:1.0:*:*:*:*:*:*:*").length);

        System.out.println("KEV names");
        KevSource.Entry javaSe = new KevSource.Entry("CVE-2000-0010", "Oracle", "Java SE", LocalDate.parse("2026-11-20"), "Oracle Java SE flaw", null);
        check("vendor equal, product contained, case ignored", KevSource.matches(javaSe, new Inventory.KevName("oracle", "java")));
        check("vendor alone matches", KevSource.matches(javaSe, new Inventory.KevName("Oracle", null)));
        check("another vendor does not", !KevSource.matches(javaSe, new Inventory.KevName("Example", "Java")));
        check("a product not contained does not", !KevSource.matches(javaSe, new Inventory.KevName("Oracle", "Database")));
        Map<String, KevSource.Entry> catalogue = new LinkedHashMap<>();
        catalogue.put(javaSe.cve(), javaSe);
        catalogue.put("CVE-2000-0011", new KevSource.Entry("CVE-2000-0011", "Oracle", "Java SE", LocalDate.parse("2024-01-01"), "old", null));
        List<Flaw> byName = KevSource.recentByName(catalogue, List.of(new Inventory.KevName("Oracle", "Java")), LocalDate.parse("2026-11-27"));
        equal("only the listing of the last 30 days, as an uncertain exploited flaw", List.of("CVE-2000-0010"),
            byName.stream().map(Flaw::id).toList());
        check("uncertain, exploited", byName.get(0).uncertain() && byName.get(0).knownExploited());
        KevSource.parse(RecordedSourceClient.resource("recorded/kev.json"));
        check("an empty catalogue is refused", throwsIllegal(() -> KevSource.parse("{\"vulnerabilities\":[]}")));

        System.out.println("text from sources");
        equal("controls and bidi overrides out, spaces collapsed", "a b c", Sanitize.summary("a\u0000\n b‮c"));
        equal("URLs removed", "see for details.", Sanitize.summary("see https://example.org/advisory?id=1 for details."));
        String longText = "x".repeat(400);
        equal("cut to 300 with an ellipsis", 300, Sanitize.summary(longText).codePointCount(0, Sanitize.summary(longText).length()));
        check("linkable id", Sanitize.LINKABLE_ID.matcher("GHSA-j77q-2qqg-6989").matches() && !Sanitize.LINKABLE_ID.matcher("../x").matches());
        check("hosts", SourceClient.isAllowed("https://endoflife.date/api/v1/products/redis/") && !SourceClient.isAllowed("http://endoflife.date/")
                       && !SourceClient.isAllowed("https://endoflife.date.example.org/"));

        System.out.println("inventory");
        check("the test inventory reads", Inventory.parse(RecordedSourceClient.resource("directory/inventory.json")).components.size() == 7);
        for (String bad : List.of("{\"schemaVersion\":2}", "not json", "{\"schemaVersion\":1,\"components\":[{\"id\":\"a\",\"layer\":\"x\",\"name\":\"n\",\"managedBy\":\"us\"}]}",
            "{\"schemaVersion\":1,\"components\":[{\"id\":\"a\",\"layer\":\"data\",\"name\":\"n\",\"managedBy\":\"us\"},{\"id\":\"a\",\"layer\":\"data\",\"name\":\"n\",\"managedBy\":\"us\"}]}",
            "{\"schemaVersion\":1,\"components\":[{\"id\":\"a\",\"layer\":\"data\",\"name\":\"n\",\"managedBy\":\"us\",\"howTo\":{\"reboot\":[]}}]}",
            "{\"schemaVersion\":1,\"components\":[{\"id\":\"a\",\"layer\":\"data\",\"name\":\"n\",\"managedBy\":\"us\",\"running\":{\"source\":\"declared\",\"version\":\"1\",\"asOf\":\"26/11/2026\"}}]}"))
            try {
                Inventory.parse(bad);
                check("refused: " + bad.substring(0, Math.min(60, bad.length())), false);
            } catch (Inventory.Invalid e) {
                check("refused (" + e.code + "): " + bad.substring(0, Math.min(60, bad.length())), "invalid-response".equals(e.code));
            }
        Inventory.Component prodOnly = Inventory.parse(RecordedSourceClient.resource("directory/inventory.json")).components.get(6);
        check("environments: shown in production and development, not staging",
            prodOnly.shownIn("production") && prodOnly.shownIn("development") && !prodOnly.shownIn("staging"));

        System.out.println("one retry, bounded");
        RecordedSourceClient slowOnce = new RecordedSourceClient();
        slowOnce.failures.put("cisa.gov", "timeout");
        slowOnce.failTimes.put("cisa.gov", 1);
        Calls calls = new Calls(slowOnce, 0, Calls.RUN_BUDGET_MILLIS);
        dev.webfx.platform.async.Future<String> kev = calls.get(KevSource.CHECK, KevSource.URL, KevSource.MAX_BYTES, 1000);
        check("a timeout is retried once and the answer kept", kev.succeeded() && calls.failureOf(KevSource.CHECK) == null
            && slowOnce.asked.size() == 2 && calls.retryCount() == 1);
        RecordedSourceClient busy = new RecordedSourceClient();
        busy.failures.put("endoflife.date", "status:429");
        busy.failTimes.put("endoflife.date", 1);
        Calls calls2 = new Calls(busy, 0, Calls.RUN_BUDGET_MILLIS);
        check("a 429 is retried once", calls2.get(EndOfLifeSource.CHECK, EndOfLifeSource.URL + "redis/", 1 << 20, 1000).succeeded()
            && busy.asked.size() == 2);
        RecordedSourceClient notFound = new RecordedSourceClient();
        Calls calls3 = new Calls(notFound, 0, Calls.RUN_BUDGET_MILLIS);
        check("a 404 is not retried", calls3.get(EndOfLifeSource.CHECK, EndOfLifeSource.URL + "nosuchproduct/", 1 << 20, 1000).failed()
            && notFound.asked.size() == 1 && "http-error".equals(calls3.failureOf(EndOfLifeSource.CHECK)));
        RecordedSourceClient down = new RecordedSourceClient();
        down.failures.put("api.osv.dev", "timeout");
        Calls calls4 = new Calls(down, 0, Calls.RUN_BUDGET_MILLIS);
        for (int i = 0; i < 5; i++)
            calls4.get(OsvSource.CHECK, OsvSource.VULN_URL + "GHSA-aaaa-bbbb-000" + i, 1 << 20, 1000);
        check("a source still timing out after its retry is given up: 5 calls cost 2 tries, not 10 (" + down.asked.size() + ")",
            down.asked.size() == 2 && "timeout".equals(calls4.failureOf(OsvSource.CHECK)));
        check("another source is still called", calls4.get(EndOfLifeSource.CHECK, EndOfLifeSource.URL + "redis/", 1 << 20, 1000).succeeded());
        RecordedSourceClient late = new RecordedSourceClient();
        Calls spent = new Calls(late, 0, 0);
        check("the run's budget spent: no call starts, the check reads timeout",
            spent.get(EndOfLifeSource.CHECK, EndOfLifeSource.URL + "redis/", 1 << 20, 1000).failed() && late.asked.isEmpty()
            && "timeout".equals(spent.failureOf(EndOfLifeSource.CHECK)));

        System.out.println("endoflife.date: the line to move to");
        Map<String, Object> withLts = JsonTree.parseObject("{\"releases\":["
            + "{\"name\":\"27\",\"isLts\":false,\"isMaintained\":true,\"latest\":{\"name\":\"27.0.1\",\"date\":\"2026-10-01\"}},"
            + "{\"name\":\"25\",\"isLts\":true,\"isMaintained\":true,\"latest\":{\"name\":\"25.0.5\",\"date\":\"2026-10-02\"}},"
            + "{\"name\":\"21\",\"isLts\":true,\"isMaintained\":true,\"eolFrom\":\"2026-12-01\",\"latest\":{\"name\":\"21.0.9\",\"date\":\"2026-10-03\"}}]}");
        equal("a product marking LTS lines: the newest maintained LTS line, not the newest line", "25.0.5",
            EndOfLifeSource.line(withLts, "21", LocalDate.parse("2026-11-27")).newestMaintained());
        Map<String, Object> noLts = JsonTree.parseObject("{\"releases\":["
            + "{\"name\":\"1.31\",\"isLts\":false,\"isMaintained\":true,\"latest\":{\"name\":\"1.31.10\",\"date\":\"2026-11-01\"}},"
            + "{\"name\":\"1.27\",\"isLts\":false,\"isMaintained\":false,\"isEol\":true,\"latest\":{\"name\":\"1.27.5\",\"date\":\"2025-04-16\"}}]}");
        equal("none marked: the newest maintained line", "1.31.10",
            EndOfLifeSource.line(noLts, "1.27", LocalDate.parse("2026-11-27")).newestMaintained());

        System.out.println("SQL parameters the Vert.x driver binds as their cast says");
        // The driver infers a parameter's type from its cast: a Java String bound to $n::jsonb is encoded as a JSON
        // STRING scalar (the report was stored as one on the first real run). Text first, then jsonb.
        Pattern straightToJson = Pattern.compile("\\$\\d+::jsonb?\\b");
        int constants = 0;
        List<String> offenders = new java.util.ArrayList<>();
        for (Field field : SecurityWatchStore.class.getDeclaredFields())
            if (field.getType() == String.class && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Object sql = field.get(null);
                constants++;
                if (sql instanceof String text && straightToJson.matcher(text).find())
                    offenders.add(field.getName());
            }
        check("no SecurityWatchStore SQL constant casts a parameter straight to json/jsonb (" + constants + " read) " + offenders,
            constants > 5 && offenders.isEmpty());
        check("FINISH_SQL binds the report as text, then casts", SecurityWatchStore.FINISH_SQL.contains("$4::text::jsonb"));

        System.out.println("failed-run log line");
        String withheld = SecurityWatchRunner.describe(new IllegalArgumentException("Text '7.4.33-secret' could not be parsed"));
        check("a message quoting data is withheld: " + withheld, withheld.startsWith("IllegalArgumentException") && !withheld.contains("7.4.33"));
        check("a plain platform message is kept", SecurityWatchRunner.describe(
            new IllegalArgumentException("Cannot schedule a timer with delay < 1 ms")).endsWith(": Cannot schedule a timer with delay < 1 ms"));

        System.out.println("urgent mail");
        String previous = "{\"actions\":[{\"id\":\"upgrade:db:17.4\",\"componentId\":\"db\",\"kind\":\"upgrade\",\"target\":null,\"severity\":\"critical\",\"knownExploited\":false}]}";
        String current = "{\"environment\":\"production\",\"components\":[{\"id\":\"db\",\"name\":\"Data<b>base</b>\"},{\"id\":\"libs\",\"name\":\"Libraries\"}],\"actions\":["
            + "{\"id\":\"upgrade:db:17.8\",\"componentId\":\"db\",\"kind\":\"upgrade\",\"target\":null,\"severity\":\"critical\",\"knownExploited\":false},"
            + "{\"id\":\"code-change:libs:x\",\"componentId\":\"libs\",\"kind\":\"code-change\",\"target\":\"x\",\"severity\":\"high\",\"knownExploited\":true,"
            + "\"from\":\"1\",\"to\":\"2\",\"dueDate\":\"2026-11-30\",\"advisories\":[{\"id\":\"CVE-2000-0020\"}]},"
            + "{\"id\":\"code-change:libs:y\",\"componentId\":\"libs\",\"kind\":\"code-change\",\"target\":\"y\",\"severity\":\"high\",\"knownExploited\":false}]}";
        List<Map<String, Object>> fresh = UrgentAlert.newlyUrgent(current, previous);
        equal("already urgent (a newer fix moved its id) is not news; newly exploited is; high is not urgent",
            List.of("code-change:libs:x"), fresh.stream().map(a -> JsonTree.string(a, "id")).toList());
        equal("no previous report: all urgent ones", 2, UrgentAlert.newlyUrgent(current, null).size());
        String yesterday = "{\"actions\":[{\"id\":\"verify:db:17.2\",\"componentId\":\"db\",\"kind\":\"verify\",\"target\":null,\"severity\":\"critical\",\"knownExploited\":false},"
            + "{\"id\":\"wait-vendor:libs:x\",\"componentId\":\"libs\",\"kind\":\"wait-vendor\",\"target\":\"x\",\"severity\":\"critical\",\"knownExploited\":true}]}";
        equal("the same component and target under another kind is not news", List.of(),
            UrgentAlert.newlyUrgent(current, yesterday).stream().map(a -> JsonTree.string(a, "id")).toList());
        int urgent = UrgentAlert.newlyUrgent(current, null).size();
        String body = UrgentAlert.body(urgent), subject = UrgentAlert.subject(urgent);
        boolean leaks = false;
        for (String word : List.of("Data", "base", "Libraries", "libs", "db", "17.8", "CVE-2000-0020", "code-change", "upgrade",
            "2026-11-30", "high", "known exploited", ">x<", " x "))
            leaks |= body.contains(word) || subject.contains(word);
        check("the mail carries no name, library, version, flaw id, kind, severity or date from the report", !leaks);
        check("the count is the only value: the body of 2 and of 3 differ by it alone",
            UrgentAlert.body(3).equals(body.replace("2 actions", "3 actions").replace("2 urgent actions", "3 urgent actions")));
        check("no link in the mail", !body.contains("href") && !body.contains("http"));
        equal("one action: singular", "Security watch: 1 urgent action", UrgentAlert.subject(1));
        check("mailed in production only", UrgentAlert.mailsIn("production") && !UrgentAlert.mailsIn("staging")
            && !UrgentAlert.mailsIn("development") && !UrgentAlert.mailsIn(null));
        equal("subject", "Security watch: 2 urgent actions", subject);

        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail > 0 ? 1 : 0);
    }
}
