package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flaws of products from the NVD's CVE API, for components that name CPE products in {@code flaws.nvd}.
 *
 * <h3>Which calls</h3>
 * <ul>
 *   <li><b>By product and version</b> (unless {@code recentOnly}, or the version is unknown):
 *       {@code virtualMatchString=<cpe prefix>:<version>} — the NVD returns the CVEs whose applicability statements
 *       match that version. Measured against the live API on public products: it gives what {@code cpeName} with the
 *       full CPE gives, minus old CVEs where the product is only the platform of another one; a later patch release
 *       gives none of the CVEs its release fixed. The answer is still re-checked here against each CVE's configurations
 *       ({@link #evaluate}): a match where our product is not marked vulnerable, or whose version is an unbounded
 *       wildcard, is dropped; the fix is the matching range's {@code versionEndExcluding}.</li>
 *   <li><b>By product, recent only</b> ({@code recentOnly}, or no version known): the product's CVEs published in the
 *       last {@value #RECENT_DAYS} days, each uncertain (a {@code verify} action).</li>
 *   <li><b>Early warning, by keyword</b>: {@code keywordSearch} (exact phrase) over the CVEs published in the last
 *       {@value #RECENT_DAYS} days — what the NVD has not analysed yet. A keyword hit whose configurations name the
 *       product is decided like a version match; one with configurations that do not name it is dropped (the keyword
 *       was incidental); one with no configurations yet is uncertain, with severity high when unscored (the contract's
 *       early-warning rule) — kept only when published by one of the component's {@code sources} (its vendor's CNA),
 *       when the inventory names them: measured on the live API, a widely used product's name over 30 days matched over a
 *       hundred unanalysed CVEs, under two thirds of them from that product's own CNA, the rest other products that merely
 *       mention it.</li>
 * </ul>
 *
 * <h3>Bounds</h3>
 * One call at a time, paced to the public limit (5 requests per 30 seconds: one every {@value #PACE_MILLIS} ms); one
 * retry, after {@value #RETRY_DELAY_MILLIS} ms, of a call that timed out or was answered 403, 429 or 503 ({@link Calls});
 * identical queries asked once per run; {@value #PAGE_SIZE} CVEs per page, at most {@value #MAX_PAGES} pages
 * per query (beyond: the query's components list {@code nvd} in failedChecks and the check fails {@code too-large}).
 *
 * <p>What leaves the server: product identifiers (CPE prefixes), the version asked about, keywords, a date window.
 *
 * @author Claude Code
 */
final class NvdSource {

    static final String CHECK = "nvd";
    static final String URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
    static final int PAGE_SIZE = 200;
    static final int MAX_PAGES = 5;
    static final int MAX_BYTES = 16 * 1024 * 1024;
    static final long TIMEOUT_MILLIS = 30_000;
    static final long PACE_MILLIS = 6_500;
    static final long RETRY_DELAY_MILLIS = 30_000;
    static final int RECENT_DAYS = 30;
    private static final DateTimeFormatter NVD_INSTANT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final Calls calls;
    private final Instant windowStart, windowEnd;
    private final long paceMillis;
    private final Map<String, Future<List<Map<String, Object>>>> cache = new LinkedHashMap<>();
    private long lastCall;

    /**
     * @param windowStart the start of the early-warning window (normally now minus {@value #RECENT_DAYS} days)
     * @param paceMillis  the pause between two calls ({@value #PACE_MILLIS} in production, 0 against recordings)
     */
    NvdSource(Calls calls, Instant windowStart, Instant windowEnd, long paceMillis) {
        this.calls = calls;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.paceMillis = paceMillis;
    }

    /** What the NVD said about one component: its flaws, and whether any of its own calls failed. */
    record Result(List<Flaw> flaws, boolean failed) {
    }

    /** Every query of one component, one after the other. Never fails. */
    Future<Result> check(Inventory.Nvd nvd, String version) {
        boolean byVersion = version != null && !nvd.recentOnly();
        Map<String, Flaw> flaws = new LinkedHashMap<>();
        boolean[] failed = {false};
        List<Query> queries = new ArrayList<>();
        for (String product : nvd.products())
            queries.add(byVersion ? new Query(versionQuery(product, version), Origin.VERSION)
                : new Query(recentProductQuery(product), Origin.RECENT_PRODUCT));
        for (String keyword : nvd.keywords())
            queries.add(new Query(keywordQuery(keyword), Origin.KEYWORD));
        return Async.each(queries, query -> fetch(query.parameters())
                .map(cves -> {
                    for (Map<String, Object> cve : cves) {
                        Flaw flaw = flawOf(cve, nvd.products(), byVersion ? version : null, query.origin(), nvd.sources());
                        if (flaw != null) {
                            Flaw known = flaws.get(flaw.id());
                            // A certain finding outranks an uncertain one for the same CVE
                            if (known == null || (known.uncertain() && !flaw.uncertain()))
                                flaws.put(flaw.id(), flaw);
                        }
                    }
                    return null;
                })
                .otherwise(e -> {
                    failed[0] = true;
                    return null;
                }))
            .map(v -> new Result(new ArrayList<>(flaws.values()), failed[0]));
    }

    enum Origin {VERSION, RECENT_PRODUCT, KEYWORD}

    private record Query(String parameters, Origin origin) {
    }

    String versionQuery(String product, String version) {
        return "virtualMatchString=" + enc(product + ":" + version) + "&noRejected";
    }

    String recentProductQuery(String product) {
        return "virtualMatchString=" + enc(product) + window() + "&noRejected";
    }

    String keywordQuery(String keyword) {
        return "keywordSearch=" + enc(keyword) + "&keywordExactMatch" + window() + "&noRejected";
    }

    private String window() {
        return "&pubStartDate=" + enc(NVD_INSTANT.format(windowStart)) + "&pubEndDate=" + enc(NVD_INSTANT.format(windowEnd));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** All pages of one query, asked once per run however many components ask it. */
    private Future<List<Map<String, Object>>> fetch(String parameters) {
        return cache.computeIfAbsent(parameters, p -> page(p, 0, new ArrayList<>()));
    }

    private Future<List<Map<String, Object>>> page(String parameters, int startIndex, List<Map<String, Object>> collected) {
        String url = URL + "?" + parameters + "&resultsPerPage=" + PAGE_SIZE + "&startIndex=" + startIndex;
        return paced(url)
            .compose(text -> {
                Map<String, Object> answer;
                try {
                    answer = JsonTree.parseObject(text);
                } catch (IllegalArgumentException e) {
                    calls.fail(CHECK, SourceClient.SourceFailure.INVALID_RESPONSE);
                    return Future.failedFuture(new SourceClient.SourceFailure(SourceClient.SourceFailure.INVALID_RESPONSE));
                }
                for (Object item : JsonTree.list(answer, "vulnerabilities")) {
                    Map<String, Object> cve = JsonTree.object(JsonTree.asObject(item), "cve");
                    if (cve != null)
                        collected.add(cve);
                }
                Integer total = JsonTree.integer(answer, "totalResults");
                int next = startIndex + PAGE_SIZE;
                if (total == null || next >= total)
                    return Future.succeededFuture(collected);
                if (next / PAGE_SIZE >= MAX_PAGES) {
                    calls.fail(CHECK, SourceClient.SourceFailure.TOO_LARGE);
                    return Future.failedFuture(new SourceClient.SourceFailure(SourceClient.SourceFailure.TOO_LARGE));
                }
                return page(parameters, next, collected);
            });
    }

    /**
     * One call, after the pacing pause. Its one retry (a timeout, or 403/429/503: {@link Calls}) waits
     * {@value #RETRY_DELAY_MILLIS} ms, longer than the pacing, so it keeps to the public rate limit too.
     */
    private Future<String> paced(String url) {
        long wait = lastCall == 0 ? 0 : lastCall + paceMillis - System.currentTimeMillis();
        return Async.delay(wait).compose(v -> {
            lastCall = System.currentTimeMillis();
            return calls.get(CHECK, url, MAX_BYTES, TIMEOUT_MILLIS, paceMillis == 0 ? 0 : RETRY_DELAY_MILLIS)
                .onComplete(ar -> lastCall = System.currentTimeMillis());
        });
    }

    // ===== Reading a CVE (pure) =======================================================================================

    enum Applicability {APPLIES, NOT_APPLICABLE, NO_CONFIGURATION, OTHER_PRODUCT, PRODUCT_NO_VERSION}

    record Match(Applicability applicability, String fixedIn) {
    }

    /**
     * Whether a CVE applies to {@code version} of one of these products, from its configurations: only entries marked
     * {@code vulnerable} for one of the products count; an exact version must equal ours, a wildcard needs version
     * bounds that contain ours (an unbounded wildcard says nothing and is ignored). The fix is the highest
     * {@code versionEndExcluding} among the entries that apply (null when one applies without it).
     */
    static Match evaluate(Map<String, Object> cve, List<String> products, String version) {
        List<Map<String, Object>> matches = new ArrayList<>();
        List<Object> configurations = JsonTree.list(cve, "configurations");
        for (Object configuration : configurations)
            collect(JsonTree.list(JsonTree.asObject(configuration), "nodes"), products, matches, 0);
        if (matches.isEmpty())
            return new Match(configurations.isEmpty() ? Applicability.NO_CONFIGURATION : Applicability.OTHER_PRODUCT, null);
        if (version == null)
            return new Match(Applicability.PRODUCT_NO_VERSION, null);
        boolean applies = false, unfixed = false;
        String fix = null;
        for (Map<String, Object> m : matches) {
            String[] fields = splitCpe(JsonTree.string(m, "criteria"));
            String exact = fields.length > 5 ? fields[5] : "*";
            String startIn = JsonTree.string(m, "versionStartIncluding"), startEx = JsonTree.string(m, "versionStartExcluding");
            String endIn = JsonTree.string(m, "versionEndIncluding"), endEx = JsonTree.string(m, "versionEndExcluding");
            boolean hit;
            if (!"*".equals(exact) && !"-".equals(exact))
                hit = Versions.compare(unescape(exact), version) == 0;
            else if (startIn == null && startEx == null && endIn == null && endEx == null)
                hit = false; // an unbounded wildcard
            else
                hit = (startIn == null || Versions.compare(version, startIn) >= 0)
                      && (startEx == null || Versions.compare(version, startEx) > 0)
                      && (endIn == null || Versions.compare(version, endIn) <= 0)
                      && (endEx == null || Versions.compare(version, endEx) < 0);
            if (hit) {
                applies = true;
                if (endEx == null)
                    unfixed = true;
                else if (fix == null || Versions.isLower(fix, endEx))
                    fix = endEx;
            }
        }
        if (!applies)
            return new Match(Applicability.NOT_APPLICABLE, null);
        return new Match(Applicability.APPLIES, unfixed ? null : fix);
    }

    private static void collect(List<Object> nodes, List<String> products, List<Map<String, Object>> into, int depth) {
        if (depth > 8)
            return;
        for (Object n : nodes) {
            Map<String, Object> node = JsonTree.asObject(n);
            if (node == null || JsonTree.isTrue(node, "negate"))
                continue;
            for (Object c : JsonTree.list(node, "cpeMatch")) {
                Map<String, Object> match = JsonTree.asObject(c);
                if (match != null && JsonTree.isTrue(match, "vulnerable") && isProduct(JsonTree.string(match, "criteria"), products))
                    into.add(match);
            }
            collect(JsonTree.list(node, "children"), products, into, depth + 1);
        }
    }

    /** Whether a CPE's part, vendor and product are those of one of the prefixes ({@code cpe:2.3:a:vendor:product}). */
    static boolean isProduct(String criteria, List<String> products) {
        if (criteria == null)
            return false;
        String[] fields = splitCpe(criteria);
        if (fields.length < 5)
            return false;
        String head = String.join(":", fields[0], fields[1], fields[2], fields[3], fields[4]).toLowerCase(java.util.Locale.ROOT);
        for (String product : products)
            if (head.equals(product.toLowerCase(java.util.Locale.ROOT)))
                return true;
        return false;
    }

    /** A CPE's fields, split on the colons that are not escaped. */
    static String[] splitCpe(String cpe) {
        if (cpe == null)
            return new String[0];
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < cpe.length(); i++) {
            char c = cpe.charAt(i);
            if (c == '\\' && i + 1 < cpe.length()) {
                current.append(c).append(cpe.charAt(++i));
            } else if (c == ':') {
                fields.add(current.toString());
                current.setLength(0);
            } else
                current.append(c);
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private static String unescape(String field) {
        return field.replace("\\", "");
    }

    /**
     * A flaw from a CVE found by a query of {@code origin}, for a component of these products at {@code version} (null
     * when no version is compared); null when the CVE does not concern the component.
     */
    static Flaw flawOf(Map<String, Object> cve, List<String> products, String version, Origin origin) {
        return flawOf(cve, products, version, origin, List.of());
    }

    /**
     * As above; {@code sources} (the inventory's {@code flaws.nvd.sources}), when not empty, keeps an unanalysed keyword
     * hit only when its {@code sourceIdentifier} (the CNA that published it) is one of them.
     */
    static Flaw flawOf(Map<String, Object> cve, List<String> products, String version, Origin origin, List<String> sources) {
        String id = JsonTree.string(cve, "id");
        if (id == null || !Sanitize.LINKABLE_ID.matcher(id).matches() || "Rejected".equalsIgnoreCase(JsonTree.string(cve, "vulnStatus")))
            return null;
        Match match = evaluate(cve, products, version);
        boolean uncertain, earlyWarning = origin != Origin.VERSION;
        switch (match.applicability()) {
            case APPLIES -> uncertain = false;
            case PRODUCT_NO_VERSION -> uncertain = true;
            case NO_CONFIGURATION -> {
                if (origin != Origin.KEYWORD)
                    return null;
                if (!sources.isEmpty() && !sources.contains(JsonTree.string(cve, "sourceIdentifier")))
                    return null; // another publisher's CVE that only mentions the keyword
                uncertain = true;
            }
            default -> {
                return null;
            }
        }
        Double score = score(cve);
        String severity = score != null ? Cvss.severityOf(score) : earlyWarning ? "high" : "medium";
        if ("none".equals(severity))
            severity = "low";
        return new Flaw(id, Set.of(id), score, severity, Sanitize.summary(englishDescription(cve)),
            OsvSource.day(JsonTree.string(cve, "published")), Sanitize.shortText(match.fixedIn()), uncertain, earlyWarning,
            null, null, null);
    }

    /** The base score: CVSS v3.1, else v3.0, else v4.0 — the NVD's own (Primary) entry first. */
    static Double score(Map<String, Object> cve) {
        Map<String, Object> metrics = JsonTree.object(cve, "metrics");
        for (String key : new String[]{"cvssMetricV31", "cvssMetricV30", "cvssMetricV40"}) {
            Double fallback = null;
            for (Object item : JsonTree.list(metrics, key)) {
                Map<String, Object> metric = JsonTree.asObject(item);
                Double score = JsonTree.number(JsonTree.object(metric, "cvssData"), "baseScore");
                if (score == null || score < 0 || score > 10)
                    continue;
                if ("Primary".equals(JsonTree.string(metric, "type")))
                    return score;
                if (fallback == null)
                    fallback = score;
            }
            if (fallback != null)
                return fallback;
        }
        return null;
    }

    private static String englishDescription(Map<String, Object> cve) {
        for (Object item : JsonTree.list(cve, "descriptions")) {
            Map<String, Object> d = JsonTree.asObject(item);
            if ("en".equals(JsonTree.string(d, "lang")))
                return JsonTree.string(d, "value");
        }
        return null;
    }

    /** The distinct queries asked so far (for the measurements). */
    Set<String> queries() {
        return new LinkedHashSet<>(cache.keySet());
    }
}
