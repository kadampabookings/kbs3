package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Release lines from endoflife.date's API v1 ({@code /api/v1/products/{product}/}): for the line a component is on,
 * its newest release and day, and its end of support; and the newest release of the newest maintained line (the
 * target when support has ended).
 *
 * <p>Field names as measured on 26 November 2026: {@code result.releases[]} with {@code name} (the line),
 * {@code isEol}, {@code eolFrom}, {@code isMaintained}, {@code isLts}, {@code latest {name, date}}. A product is asked
 * once per run. The line to move to at the end of support is the newest maintained long-term-support line when the
 * product marks any release as LTS, else the newest maintained line.
 *
 * <p>What leaves the server: the product name.
 *
 * @author Claude Code
 */
final class EndOfLifeSource {

    private EndOfLifeSource() {
    }

    static final String CHECK = "endoflife";
    static final String URL = "https://endoflife.date/api/v1/products/";
    static final int MAX_BYTES = 2 * 1024 * 1024;
    static final long TIMEOUT_MILLIS = 30_000;
    private static final Pattern PRODUCT = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

    record Line(String cycle, String latest, LocalDate latestOn, LocalDate supportEnds, boolean eol,
                String newestMaintained, LocalDate newestMaintainedOn) {
    }

    /** The product's releases (the {@code result} object), asked once per run per product; null on failure. */
    static Future<Map<String, Object>> product(Calls calls, String product, Map<String, Future<Map<String, Object>>> cache) {
        if (product == null || !PRODUCT.matcher(product).matches())
            return Future.succeededFuture(null);
        return cache.computeIfAbsent(product, p ->
            calls.get(CHECK, URL + URLEncoder.encode(p, StandardCharsets.UTF_8) + "/", MAX_BYTES, TIMEOUT_MILLIS)
                .map(text -> JsonTree.object(JsonTree.parseObject(text), "result"))
                .otherwise(e -> {
                    if (!(e instanceof SourceClient.SourceFailure))
                        calls.fail(CHECK, SourceClient.SourceFailure.INVALID_RESPONSE);
                    return null;
                }));
    }

    /** The line called {@code cycle} in a product's releases, or null when there is none of that name. */
    static Line line(Map<String, Object> product, String cycle, LocalDate today) {
        if (product == null || cycle == null)
            return null;
        Map<String, Object> found = null, newest = null, newestLts = null;
        boolean anyLts = false;
        // Releases come newest first: the first maintained one, and the first maintained long-term-support one
        for (Object item : JsonTree.list(product, "releases")) {
            Map<String, Object> release = JsonTree.asObject(item);
            String name = JsonTree.string(release, "name");
            if (cycle.equals(name))
                found = release;
            boolean lts = JsonTree.isTrue(release, "isLts");
            anyLts |= lts;
            boolean maintained = JsonTree.isTrue(release, "isMaintained") || !JsonTree.isTrue(release, "isEol");
            if (maintained && JsonTree.string(JsonTree.object(release, "latest"), "name") != null) {
                if (newest == null)
                    newest = release;
                if (lts && newestLts == null)
                    newestLts = release;
            }
        }
        // A product that marks long-term-support lines: move to the newest of those, not to a short-lived one
        Map<String, Object> target = anyLts && newestLts != null ? newestLts : newest;
        String targetVersion = target == null ? null : JsonTree.string(JsonTree.object(target, "latest"), "name");
        LocalDate targetOn = target == null ? null : OsvSource.day(JsonTree.string(JsonTree.object(target, "latest"), "date"));
        if (found == null)
            return null;
        Map<String, Object> latest = JsonTree.object(found, "latest");
        LocalDate eolFrom = OsvSource.day(JsonTree.string(found, "eolFrom"));
        boolean eol = JsonTree.isTrue(found, "isEol") || (eolFrom != null && !eolFrom.isAfter(today));
        return new Line(cycle, Sanitize.shortText(JsonTree.string(latest, "name")), OsvSource.day(JsonTree.string(latest, "date")),
            eolFrom, eol, Sanitize.shortText(targetVersion), targetOn);
    }

    /** The line of a component: the inventory's {@code cycle}, else derived from the running version or a fact. */
    static String cycleOf(Inventory.Lifecycle lifecycle, String runningVersion, Map<String, String> facts) {
        if (lifecycle == null)
            return null;
        if (lifecycle.cycle() != null)
            return lifecycle.cycle();
        if (lifecycle.cycleFromFact() != null)
            return facts.get(lifecycle.cycleFromFact());
        if ("major".equals(lifecycle.cycleFromVersion()))
            return Versions.leadingNumbers(runningVersion, 1);
        if ("minor".equals(lifecycle.cycleFromVersion()))
            return Versions.leadingNumbers(runningVersion, 2);
        return null;
    }

    static Map<String, Future<Map<String, Object>>> newCache() {
        return new LinkedHashMap<>();
    }
}
