package org.kadampabookings.kbs.server.securitywatch;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The deployment's inventory: what it runs besides what the server reads live, how each part is checked, who acts on
 * it and how, and the flaws someone decided do not apply. Read from {@code inventory.json} in the configured directory
 * at every run (the format is the contract's section 4); nothing of it is in this module, which is public.
 *
 * <p>Read strictly: a wrong schema version, a component without an id, two components with one id, or a value outside
 * the known layers, owners and kinds makes the whole file invalid ({@link Invalid}) — a half-read inventory would
 * report the parts it dropped as if they did not exist.
 *
 * @author Claude Code
 */
final class Inventory {

    static final int SCHEMA_VERSION = 1;
    static final Set<String> LAYERS = Set.of("application", "runtime", "data", "edge", "other");
    static final Set<String> MANAGED_BY = Set.of("us", "provider", "other-party");
    static final Set<String> KINDS = Set.of("redeploy", "upgrade", "code-change", "verify", "decide", "wait-vendor");
    static final Set<String> FACTS = Set.of("javaVersion", "osRelease", "postgresVersion");
    static final Set<String> SEVERITY_WORDS = Set.of("critical", "high", "medium", "low");

    final ResponseTimes responseTimes;
    final int declaredStaleAfterDays, imageMaxAgeDays, eolSoonDays;
    final BaseImage baseImage;
    final List<Component> components;
    final List<Assessed> assessed;

    record ResponseTimes(int knownExploitedHours, int criticalDays, int highDays) {
    }

    record BaseImage(String repository, String tag) {
    }

    /** How the running version is known: {@code live} (a fact), {@code build} (a list), or {@code declared}. */
    record Running(String source, String fact, String list, String version, LocalDate asOf) {
    }

    /**
     * @param moveTo what to move to at the end of support, as written ({@code stable}), when the newest maintained line is
     *               not the right answer; it replaces the suggested version
     */
    record Lifecycle(String product, String cycle, String cycleFromVersion, String cycleFromFact, String compareFact,
                     String moveTo) {
    }

    /**
     * @param sources the NVD source identifiers (the CNAs) whose unanalysed CVEs the early-warning pass keeps; empty =
     *                every source (measured: the name of a widely used product as a keyword otherwise brings in
     *                dozens of other products' CVEs that merely mention it)
     */
    record Nvd(List<String> products, List<String> keywords, String minSeverity, boolean recentOnly, List<String> sources) {
    }

    record KevName(String vendor, String product) {
    }

    record Component(String id, String layer, String name, String where, String managedBy, String owner,
                     List<String> needs, List<String> environments, Running running, Lifecycle lifecycle,
                     String osvEcosystem, Nvd nvd, List<KevName> kev, Map<String, List<String>> howTo) {

        boolean shownIn(String environment) {
            return environments == null || "development".equals(environment) || environments.contains(environment);
        }

        /** The first of {@code kinds} the component's how-to carries, or null. */
        String firstHowTo(String... kinds) {
            for (String kind : kinds)
                if (howTo.containsKey(kind))
                    return kind;
            return null;
        }
    }

    record Assessed(String id, String componentId, String decision, String reason, LocalDate decidedOn, LocalDate reviewBy) {
    }

    /** The file could not be used; {@code code} is the check's error code. */
    static final class Invalid extends Exception {
        private static final long serialVersionUID = 1L;
        final String code;

        Invalid(String code, String why) {
            super(why, null, false, false);
            this.code = code;
        }
    }

    private Inventory(ResponseTimes responseTimes, int declaredStaleAfterDays, int imageMaxAgeDays, int eolSoonDays,
                      BaseImage baseImage, List<Component> components, List<Assessed> assessed) {
        this.responseTimes = responseTimes;
        this.declaredStaleAfterDays = declaredStaleAfterDays;
        this.imageMaxAgeDays = imageMaxAgeDays;
        this.eolSoonDays = eolSoonDays;
        this.baseImage = baseImage;
        this.components = components;
        this.assessed = assessed;
    }

    static Inventory parse(String text) throws Invalid {
        Map<String, Object> root;
        try {
            root = JsonTree.parseObject(text);
        } catch (IllegalArgumentException e) {
            throw new Invalid("invalid-response", "not JSON: " + e.getMessage());
        }
        if (!Integer.valueOf(SCHEMA_VERSION).equals(JsonTree.integer(root, "schemaVersion")))
            throw new Invalid("invalid-response", "schemaVersion is not " + SCHEMA_VERSION);
        Map<String, Object> times = JsonTree.object(root, "responseTimes");
        ResponseTimes responseTimes = new ResponseTimes(positive(times, "knownExploitedHours", 72),
            positive(times, "criticalDays", 7), positive(times, "highDays", 14));
        Map<String, Object> image = JsonTree.object(root, "baseImage");
        BaseImage baseImage = image == null ? null : new BaseImage(JsonTree.string(image, "repository"), JsonTree.string(image, "tag"));
        List<Component> components = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (Object item : JsonTree.list(root, "components")) {
            Component component = component(JsonTree.asObject(item));
            if (!ids.add(component.id()))
                throw new Invalid("invalid-response", "two components are called " + component.id());
            components.add(component);
        }
        List<Assessed> assessed = new ArrayList<>();
        for (Object item : JsonTree.list(root, "assessed")) {
            Map<String, Object> a = JsonTree.asObject(item);
            String id = required(a, "id"), componentId = required(a, "componentId"), decision = required(a, "decision");
            if (!decision.equals("not-applicable") && !decision.equals("accepted"))
                throw new Invalid("invalid-response", "unknown decision " + decision);
            assessed.add(new Assessed(id, componentId, decision, JsonTree.string(a, "reason"),
                day(a, "decidedOn"), day(a, "reviewBy")));
        }
        return new Inventory(responseTimes, positive(root, "declaredStaleAfterDays", 90),
            positive(root, "imageMaxAgeDays", 14), positive(root, "eolSoonDays", 180), baseImage, components, assessed);
    }

    private static Component component(Map<String, Object> c) throws Invalid {
        if (c == null)
            throw new Invalid("invalid-response", "a component is not an object");
        String id = required(c, "id");
        String layer = oneOf(c, "layer", LAYERS);
        String managedBy = oneOf(c, "managedBy", MANAGED_BY);
        Map<String, Object> r = JsonTree.object(c, "running");
        Running running = null;
        if (r != null) {
            String source = required(r, "source");
            switch (source) {
                case "live" -> running = new Running(source, oneOf(r, "fact", FACTS), null, null, null);
                case "build" -> running = new Running(source, null, oneOf(r, "list", Set.of("java", "npm")), null, null);
                case "declared" -> running = new Running(source, null, null, required(r, "version"), day(r, "asOf"));
                default -> throw new Invalid("invalid-response", id + ": unknown running source " + source);
            }
        }
        Map<String, Object> l = JsonTree.object(c, "lifecycle");
        Lifecycle lifecycle = l == null ? null : new Lifecycle(required(l, "endoflife"), JsonTree.string(l, "cycle"),
            JsonTree.string(l, "cycleFromVersion"), JsonTree.string(l, "cycleFromFact"), JsonTree.string(l, "compareFact"),
            Sanitize.shortText(JsonTree.string(l, "moveTo")));
        Map<String, Object> flaws = JsonTree.object(c, "flaws");
        String osv = JsonTree.string(JsonTree.object(flaws, "osv"), "ecosystem");
        Map<String, Object> n = JsonTree.object(flaws, "nvd");
        Nvd nvd = null;
        if (n != null) {
            String minSeverity = JsonTree.string(n, "minSeverity");
            if (minSeverity != null && !SEVERITY_WORDS.contains(minSeverity))
                throw new Invalid("invalid-response", id + ": unknown minSeverity " + minSeverity);
            nvd = new Nvd(strings(n, "products"), strings(n, "keywords"), minSeverity == null ? "high" : minSeverity,
                JsonTree.isTrue(n, "recentOnly"), strings(n, "sources"));
        }
        List<KevName> kev = new ArrayList<>();
        for (Object item : JsonTree.list(flaws, "kev")) {
            Map<String, Object> k = JsonTree.asObject(item);
            kev.add(new KevName(required(k, "vendor"), JsonTree.string(k, "product")));
        }
        Map<String, List<String>> howTo = new LinkedHashMap<>();
        Map<String, Object> h = JsonTree.object(c, "howTo");
        if (h != null)
            for (String kind : h.keySet()) {
                if (!KINDS.contains(kind))
                    throw new Invalid("invalid-response", id + ": unknown how-to kind " + kind);
                howTo.put(kind, strings(h, kind));
            }
        List<String> environments = c.containsKey("environments") ? strings(c, "environments") : null;
        return new Component(id, layer, required(c, "name"), JsonTree.string(c, "where"), managedBy,
            JsonTree.string(c, "owner"), strings(c, "needs"), environments, running, lifecycle, osv, nvd,
            List.copyOf(kev), howTo);
    }

    private static List<String> strings(Map<String, Object> map, String key) {
        List<String> list = new ArrayList<>();
        for (Object item : JsonTree.list(map, key))
            if (item instanceof String s)
                list.add(s);
        return List.copyOf(list);
    }

    private static String required(Map<String, Object> map, String key) throws Invalid {
        String value = JsonTree.string(map, key);
        if (value == null || value.isBlank())
            throw new Invalid("invalid-response", "missing " + key);
        return value;
    }

    private static String oneOf(Map<String, Object> map, String key, Set<String> allowed) throws Invalid {
        String value = required(map, key);
        if (!allowed.contains(value))
            throw new Invalid("invalid-response", "unknown " + key + " " + value);
        return value;
    }

    private static int positive(Map<String, Object> map, String key, int fallback) throws Invalid {
        if (map == null || !map.containsKey(key))
            return fallback;
        Integer value = JsonTree.integer(map, key);
        if (value == null || value <= 0)
            throw new Invalid("invalid-response", key + " is not a positive whole number");
        return value;
    }

    private static LocalDate day(Map<String, Object> map, String key) throws Invalid {
        String value = JsonTree.string(map, key);
        if (value == null)
            return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new Invalid("invalid-response", key + " is not a day: " + value);
        }
    }
}
