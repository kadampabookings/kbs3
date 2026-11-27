package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Library flaws from OSV.dev: which of a list's libraries, at their versions, have known flaws ({@code querybatch},
 * which decides applicability for the version itself), then each flaw's details ({@code vulns/{id}}).
 *
 * <p>A detail lookup answered 404 means the advisory was withdrawn: that flaw is dropped, and the check stays ok. Any other
 * failure keeps the flaw, by its id, as uncertain, and fails the check.
 *
 * <p>What leaves the server: library names and versions, nothing else.
 *
 * <p>Bounds: {@value #BATCH_SIZE} queries per batch call; a query whose answer is paged is asked again at most
 * {@value #MAX_PAGES} times; at most {@value #MAX_DETAILS} flaws are looked up per run. A flaw beyond that cap is still
 * reported, by its id, as uncertain (a {@code verify} action: it applies, but its fix was not looked up), and the
 * {@code osv} check fails with {@code too-large} so the page says the answer is incomplete.
 *
 * @author Claude Code
 */
final class OsvSource {

    private OsvSource() {
    }

    static final String CHECK = "osv";
    static final String BATCH_URL = "https://api.osv.dev/v1/querybatch";
    static final String VULN_URL = "https://api.osv.dev/v1/vulns/";
    static final int BATCH_SIZE = 500;
    static final int MAX_PAGES = 5;
    static final int MAX_DETAILS = 400;
    static final int MAX_BATCH_BYTES = 4 * 1024 * 1024;
    static final int MAX_VULN_BYTES = 1024 * 1024;
    static final long TIMEOUT_MILLIS = 30_000;

    /** What OSV said about one list: the flaws per library, and whether any of this list's calls failed. */
    static final class Result {
        final Map<Libraries.Library, List<Flaw>> flaws = new LinkedHashMap<>();
        boolean failed;
    }

    /** Asks OSV about every library of a list ({@code ecosystem} = {@code Maven} or {@code npm}). Never fails. */
    static Future<Result> check(Calls calls, String ecosystem, List<Libraries.Library> libraries, Set<String> detailsBudget) {
        Result result = new Result();
        Map<Libraries.Library, List<String>> hits = new LinkedHashMap<>();
        List<List<Libraries.Library>> batches = new ArrayList<>();
        for (int i = 0; i < libraries.size(); i += BATCH_SIZE)
            batches.add(libraries.subList(i, Math.min(libraries.size(), i + BATCH_SIZE)));
        return Async.each(batches, batch -> queryBatch(calls, ecosystem, batch, hits, result))
            .compose(v -> {
                Map<String, Map<String, Object>> details = new LinkedHashMap<>();
                Set<String> ids = new LinkedHashSet<>();
                hits.values().forEach(ids::addAll);
                List<String> toFetch = new ArrayList<>();
                for (String id : ids)
                    if (detailsBudget.size() < MAX_DETAILS || detailsBudget.contains(id)) {
                        detailsBudget.add(id);
                        toFetch.add(id);
                    }
                if (toFetch.size() < ids.size())
                    calls.fail(CHECK, SourceClient.SourceFailure.TOO_LARGE);
                Set<String> withdrawn = new LinkedHashSet<>();
                return Async.each(toFetch, id -> calls.getOrNotFound(CHECK, VULN_URL + URLEncoder.encode(id, StandardCharsets.UTF_8),
                        MAX_VULN_BYTES, TIMEOUT_MILLIS)
                        .map(text -> {
                            if (text == null)
                                withdrawn.add(id); // 404: the advisory was withdrawn — no flaw, and no failure
                            else
                                details.put(id, JsonTree.parseObject(text));
                            return null;
                        })
                        .otherwise(e -> {
                            result.failed = true;
                            if (!(e instanceof SourceClient.SourceFailure))
                                calls.fail(CHECK, SourceClient.SourceFailure.INVALID_RESPONSE);
                            return null;
                        }))
                    .map(done -> {
                        for (Map.Entry<Libraries.Library, List<String>> hit : hits.entrySet()) {
                            if (hit.getValue().stream().allMatch(withdrawn::contains))
                                continue;
                            List<Flaw> flaws = new ArrayList<>();
                            for (String id : hit.getValue()) {
                                if (withdrawn.contains(id))
                                    continue;
                                Map<String, Object> vuln = details.get(id);
                                flaws.add(vuln != null ? flawOf(vuln, ecosystem, hit.getKey()) : unknownFlaw(id, hit.getKey()));
                            }
                            result.flaws.put(hit.getKey(), flaws);
                        }
                        return result;
                    });
            });
    }

    private static Future<Void> queryBatch(Calls calls, String ecosystem, List<Libraries.Library> batch,
                                           Map<Libraries.Library, List<String>> hits, Result result) {
        Map<Integer, String> tokens = new LinkedHashMap<>();
        for (int i = 0; i < batch.size(); i++)
            tokens.put(i, null);
        return page(calls, ecosystem, batch, tokens, hits, result, 0);
    }

    /** One round of the batch: the queries still to ask ({@code tokens}: index → page token, null for the first page). */
    private static Future<Void> page(Calls calls, String ecosystem, List<Libraries.Library> batch, Map<Integer, String> tokens,
                                     Map<Libraries.Library, List<String>> hits, Result result, int round) {
        List<Integer> indexes = new ArrayList<>(tokens.keySet());
        return calls.post(CHECK, BATCH_URL, batchBody(ecosystem, batch, indexes, tokens), MAX_BATCH_BYTES, TIMEOUT_MILLIS)
            .compose(text -> {
                List<Object> results = JsonTree.list(JsonTree.parseObject(text), "results");
                if (results.size() != indexes.size())
                    throw new IllegalArgumentException("answers do not match the queries");
                Map<Integer, String> next = new LinkedHashMap<>();
                for (int k = 0; k < results.size(); k++) {
                    Map<String, Object> answer = JsonTree.asObject(results.get(k));
                    Libraries.Library library = batch.get(indexes.get(k));
                    for (Object vuln : JsonTree.list(answer, "vulns")) {
                        String id = JsonTree.string(JsonTree.asObject(vuln), "id");
                        if (id != null && Sanitize.LINKABLE_ID.matcher(id).matches()) {
                            List<String> ids = hits.computeIfAbsent(library, l -> new ArrayList<>());
                            if (!ids.contains(id))
                                ids.add(id);
                        }
                    }
                    String token = JsonTree.string(answer, "next_page_token");
                    if (token != null && !token.isEmpty())
                        next.put(indexes.get(k), token);
                }
                if (next.isEmpty())
                    return Future.<Void>succeededFuture();
                if (round + 1 >= MAX_PAGES) {
                    calls.fail(CHECK, SourceClient.SourceFailure.TOO_LARGE);
                    result.failed = true;
                    return Future.<Void>succeededFuture();
                }
                return page(calls, ecosystem, batch, next, hits, result, round + 1);
            })
            .recover(e -> {
                result.failed = true;
                if (!(e instanceof SourceClient.SourceFailure))
                    calls.fail(CHECK, SourceClient.SourceFailure.INVALID_RESPONSE);
                return Future.succeededFuture();
            });
    }

    /** The querybatch body for these queries of the batch, with their page tokens. */
    static String batchBody(String ecosystem, List<Libraries.Library> batch, List<Integer> indexes, Map<Integer, String> tokens) {
        List<Object> queries = new ArrayList<>();
        for (int index : indexes) {
            Libraries.Library library = batch.get(index);
            Map<String, Object> query = new LinkedHashMap<>();
            query.put("package", Map.of("ecosystem", ecosystem, "name", library.name()));
            query.put("version", library.version());
            String token = tokens.get(index);
            if (token != null)
                query.put("page_token", token);
            queries.add(query);
        }
        return JsonTree.write(Map.of("queries", queries));
    }

    /** A flaw from an OSV record, for this library. */
    static Flaw flawOf(Map<String, Object> vuln, String ecosystem, Libraries.Library library) {
        String ownId = JsonTree.string(vuln, "id");
        List<String> aliases = new ArrayList<>();
        for (Object alias : JsonTree.list(vuln, "aliases"))
            if (alias instanceof String s && Sanitize.LINKABLE_ID.matcher(s).matches())
                aliases.add(s);
        Set<String> ids = new LinkedHashSet<>();
        if (ownId != null)
            ids.add(ownId);
        ids.addAll(aliases);
        Double score = null;
        for (Object entry : JsonTree.list(vuln, "severity")) {
            Map<String, Object> s = JsonTree.asObject(entry);
            String type = JsonTree.string(s, "type");
            if (type != null && type.startsWith("CVSS_V3")) {
                score = Cvss.baseScore(JsonTree.string(s, "score"));
                if (score != null)
                    break;
            }
        }
        String own = Cvss.severityWord(JsonTree.string(JsonTree.object(vuln, "database_specific"), "severity"));
        String severity = own != null ? own : score != null ? Cvss.severityOf(score) : "medium";
        if ("none".equals(severity))
            severity = "low";
        String summary = JsonTree.string(vuln, "summary");
        if (summary == null || summary.isBlank())
            summary = JsonTree.string(vuln, "details");
        return new Flaw(Sanitize.shortText(Flaw.displayId(ownId, aliases)), ids, score, severity, Sanitize.summary(summary),
            day(JsonTree.string(vuln, "published")), Sanitize.shortText(fixedIn(vuln, ecosystem, library.name(), library.version())),
            false, false, library.name(), library.version(), null);
    }

    /** A flaw OSV reported but whose details were not looked up (beyond the cap, or the lookup failed). */
    static Flaw unknownFlaw(String id, Libraries.Library library) {
        return new Flaw(id, Set.of(id), null, "medium", null, null, null, true, false, library.name(), library.version(), null);
    }

    /**
     * The lowest version above {@code version} that fixes the flaw for this package: from the {@code affected} ranges
     * of the package in this ecosystem — the range that contains the version gives its {@code fixed}; when none can be
     * placed (a version scheme the comparison does not follow), the lowest {@code fixed} above the version. Null when
     * the range containing the version has no fix (none published yet).
     */
    static String fixedIn(Map<String, Object> vuln, String ecosystem, String name, String version) {
        String lowestAbove = null;
        boolean containedWithoutFix = false;
        String containedFix = null;
        for (Object item : JsonTree.list(vuln, "affected")) {
            Map<String, Object> affected = JsonTree.asObject(item);
            Map<String, Object> pkg = JsonTree.object(affected, "package");
            if (!ecosystem.equalsIgnoreCase(String.valueOf(JsonTree.string(pkg, "ecosystem")))
                || !name.equals(JsonTree.string(pkg, "name")))
                continue;
            for (Object r : JsonTree.list(affected, "ranges")) {
                Map<String, Object> range = JsonTree.asObject(r);
                String type = JsonTree.string(range, "type");
                if ("GIT".equals(type))
                    continue;
                String introduced = null;
                for (Object e : JsonTree.list(range, "events")) {
                    Map<String, Object> event = JsonTree.asObject(e);
                    if (event == null)
                        continue;
                    if (event.containsKey("introduced"))
                        introduced = JsonTree.string(event, "introduced");
                    String fixed = JsonTree.string(event, "fixed");
                    String lastAffected = JsonTree.string(event, "last_affected");
                    if (fixed != null) {
                        if (Versions.compare(fixed, version) > 0 && (lowestAbove == null || Versions.isLower(fixed, lowestAbove)))
                            lowestAbove = fixed;
                        if (inRange(version, introduced, fixed, false)
                            && (containedFix == null || Versions.isLower(fixed, containedFix)))
                            containedFix = fixed;
                        introduced = null;
                    } else if (lastAffected != null) {
                        if (inRange(version, introduced, lastAffected, true))
                            containedWithoutFix = true;
                        introduced = null;
                    }
                }
                if (introduced != null && inRange(version, introduced, null, false))
                    containedWithoutFix = true; // introduced and never fixed
            }
        }
        if (containedFix != null)
            return containedFix;
        return containedWithoutFix ? null : lowestAbove;
    }

    private static boolean inRange(String version, String introduced, String end, boolean endIncluded) {
        boolean afterStart = introduced == null || "0".equals(introduced) || Versions.compare(version, introduced) >= 0;
        if (!afterStart)
            return false;
        if (end == null)
            return true;
        int c = Versions.compare(version, end);
        return endIncluded ? c <= 0 : c < 0;
    }

    static LocalDate day(String instant) {
        if (instant == null || instant.length() < 10)
            return null;
        try {
            return LocalDate.parse(instant.substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
