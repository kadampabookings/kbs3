package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CISA's catalogue of Known Exploited Vulnerabilities: one download per run (about 2 MB, slow to serve — the timeout is
 * generous). Gives, for a flaw id, the day CISA listed it as exploited; and the entries matching a component's names.
 *
 * <p><b>Matching by name</b> ({@code flaws.kev} of a component): the vendor equal to CISA's {@code vendorProject}, the
 * product (when given) CONTAINED in CISA's {@code product}, both ignoring case. Containment rather than equality,
 * because CISA's product names carry the edition (a product name followed by its edition or an abbreviation in brackets) — equality
 * would never match them. A flaw found only by name is uncertain (a {@code verify} action), and only one CISA listed in
 * the last {@value #RECENT_DAYS} days counts: older listings are long-known flaws that the version checks either found
 * already or ruled out.
 *
 * @author Claude Code
 */
final class KevSource {

    private KevSource() {
    }

    static final String CHECK = "kev";
    static final String URL = "https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json";
    static final int MAX_BYTES = 16 * 1024 * 1024;
    static final long TIMEOUT_MILLIS = 60_000;
    static final int RECENT_DAYS = 30;

    record Entry(String cve, String vendor, String product, LocalDate dateAdded, String name, String description) {
    }

    /** The catalogue, by CVE id; null when the download or its parsing failed. */
    static Future<Map<String, Entry>> load(Calls calls) {
        return calls.get(CHECK, URL, MAX_BYTES, TIMEOUT_MILLIS)
            .compose(text -> Async.background(() -> parse(text)))
            .otherwise(e -> {
                if (!(e instanceof SourceClient.SourceFailure))
                    calls.fail(CHECK, SourceClient.SourceFailure.INVALID_RESPONSE);
                return null;
            });
    }

    static Map<String, Entry> parse(String text) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        List<Object> list = JsonTree.list(JsonTree.parseObject(text), "vulnerabilities");
        if (list.isEmpty())
            throw new IllegalArgumentException("an empty catalogue");
        for (Object item : list) {
            Map<String, Object> v = JsonTree.asObject(item);
            String cve = JsonTree.string(v, "cveID");
            LocalDate added = OsvSource.day(JsonTree.string(v, "dateAdded"));
            if (cve == null || added == null || !Sanitize.LINKABLE_ID.matcher(cve).matches())
                continue;
            entries.put(cve, new Entry(cve, JsonTree.string(v, "vendorProject"), JsonTree.string(v, "product"), added,
                JsonTree.string(v, "vulnerabilityName"), JsonTree.string(v, "shortDescription")));
        }
        return entries;
    }

    /** The day CISA listed any of these ids, or null. */
    static LocalDate addedOn(Map<String, Entry> catalogue, Set<String> ids) {
        if (catalogue == null)
            return null;
        LocalDate earliest = null;
        for (String id : ids) {
            Entry entry = catalogue.get(id);
            if (entry != null && (earliest == null || entry.dateAdded().isBefore(earliest)))
                earliest = entry.dateAdded();
        }
        return earliest;
    }

    static boolean matches(Entry entry, Inventory.KevName name) {
        String vendor = lower(entry.vendor()), product = lower(entry.product());
        return vendor != null && vendor.equals(lower(name.vendor()))
               && (name.product() == null || (product != null && product.contains(lower(name.product()))));
    }

    /** The recent entries matching any of a component's names, as uncertain flaws. */
    static List<Flaw> recentByName(Map<String, Entry> catalogue, List<Inventory.KevName> names, LocalDate today) {
        List<Flaw> flaws = new ArrayList<>();
        if (catalogue == null || names.isEmpty())
            return flaws;
        LocalDate since = today.minusDays(RECENT_DAYS);
        for (Entry entry : catalogue.values()) {
            if (entry.dateAdded().isBefore(since))
                continue;
            for (Inventory.KevName name : names)
                if (matches(entry, name)) {
                    String summary = entry.name() != null ? entry.name() : entry.description();
                    flaws.add(new Flaw(entry.cve(), Set.of(entry.cve()), null, "high", Sanitize.summary(summary),
                        entry.dateAdded(), null, true, true, null, null, entry.dateAdded()));
                    break;
                }
        }
        return flaws;
    }

    private static String lower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }
}
