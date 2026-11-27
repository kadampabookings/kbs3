package org.kadampabookings.kbs.server.securitywatch;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assembles the report (the contract's section 2) from what one run found. Pure: everything it needs is in
 * {@link Input}; it reads no clock, file or source of its own, so every rule below is tested on fixed inputs.
 *
 * <h3>Which action a component gets — the decision table</h3>
 *
 * <b>One action per component, one per library</b> (the contract's grouping rule): everything the report says about a
 * component — its flaws, certain and uncertain together, a newer release, the end of its support — is ONE action; a
 * library list gives one action per library. The component's <b>fix kind</b> is the first of {@code upgrade},
 * {@code code-change}, {@code redeploy} its inventory {@code howTo} carries (none: it has no fix kind).
 *
 * <p>A flaw is <b>uncertain</b> when the running version is only declared, or the flaw was found by the keyword pass,
 * by CISA's product name, without a version to compare, or was not looked up; it counts like the others and is flagged.
 *
 * <p>The <b>version to go to</b>: for {@code upgrade} and {@code code-change}, the highest of the flaws' fixes, the
 * line's newest release when it is newer than what runs, and — when support has ended or ends within
 * {@code eolSoonDays} — the line to move to: the inventory's {@code lifecycle.moveTo} as written ({@code stable}), which
 * replaces any version, else the newest release of the newest maintained long-term-support line (when the product marks
 * any), else of the newest maintained line; for {@code redeploy} (a component read live, whose
 * redeploy pulls a rebuilt base image) there is one when the line has a newer release, or the base image was published
 * after our build, or our image is older than {@code imageMaxAgeDays} — its {@code to} is that newer release, or null
 * when the only reason is the base image (and always null for the OS image, and for a component compared through
 * {@code compareFact}, whose newest release is numbered differently). Those two also have {@code latest: null}.
 *
 * <table>
 *   <tr><th>#</th><th>When</th><th>The action's kind</th></tr>
 *   <tr><td>1</td><td>managed by the provider</td><td>none; status {@code managed}</td></tr>
 *   <tr><td>2</td><td>the component has a fix kind and a version to go to</td><td>the fix kind; {@code to} = that
 *       version (for a library: the lowest version fixing every flaw that has a fix; a flaw without one stays in the
 *       same action, {@code fixedIn: null})</td></tr>
 *   <tr><td>3</td><td>else, a certain flaw applies</td><td>{@code wait-vendor} (nothing fixes it yet, or a redeploy would
 *       change nothing)</td></tr>
 *   <tr><td>4</td><td>else, flaws may apply (all uncertain), or a declared version is older than
 *       {@code declaredStaleAfterDays}</td><td>{@code verify}</td></tr>
 *   <tr><td>5</td><td>else, support has ended or ends within {@code eolSoonDays}</td><td>{@code decide}, due on the
 *       support end</td></tr>
 *   <tr><td>6</td><td>else</td><td>no action</td></tr>
 * </table>
 *
 * <p>A newer release does not count for a declared version that is only the release line itself ({@code 2023},
 * {@code 1.27}): nothing can be compared with it, so only the support date applies. {@code howTo} is the kind's steps,
 * followed by the {@code decide} steps when support has ended or ends soon. {@code needs} only on {@code upgrade},
 * {@code code-change} and {@code redeploy}. Severity is the worst flaw's, {@code none} without flaws.
 * {@code publishedOn}: the day CISA listed it for a known-exploited action, else the earliest publication among the
 * worst flaws, else the newer release's day, else the support end once passed. {@code dueDate}: {@code publishedOn}
 * plus the inventory's response time (known exploited, critical, high; none below high); for {@code decide}, the
 * support end. The id is {@code kind:componentId:target-or-version}.
 *
 * <h3>Status, in decreasing order of concern</h3>
 * {@code managed} for the provider's; {@code unknown} when the running version could not be read; {@code vulnerable}
 * (an open flaw); {@code eol}; {@code unknown} (a check this component relied on failed); {@code eol-soon};
 * {@code update} (a newer release, or a newer base image); {@code ok}.
 *
 * @author Claude Code
 */
final class ReportAssembler {

    private ReportAssembler() {
    }

    static final int SCHEMA_VERSION = 1;
    static final int MAX_REPORT_CHARS = 512 * 1024;
    static final int MAX_ADVISORIES = 25;
    static final List<String> CHECK_IDS = List.of("inventory", "libraries-java", "libraries-npm", "osv", "nvd", "kev",
        "endoflife", "dockerhub");

    /** What one component's sources said. */
    record Findings(List<Flaw> flaws, Set<String> failedChecks, EndOfLifeSource.Line line, Instant baseImagePushed) {
        static Findings empty() {
            return new Findings(List.of(), Set.of(), null, null);
        }
    }

    /**
     * Everything a report is made of.
     *
     * @param inventory   null when it could not be read ({@code checkFailures} then says why)
     * @param javaLibraries / npmLibraries  null when the list could not be read
     */
    record Input(String environment, int runId, String trigger, Instant startedAt, Instant generatedAt,
                 RuntimeFacts.Values facts, Inventory inventory, Map<String, String> checkFailures,
                 List<Libraries.Library> javaLibraries, List<Libraries.Library> npmLibraries,
                 Map<String, Findings> findings) {
    }

    /**
     * The report as JSON text, never over {@link #MAX_REPORT_CHARS}: fewer advisories per action (25, 10, 3), then none
     * (the actions and their counts stay); a report still too large without any is not stored — the run fails with
     * {@code too-large}.
     */
    static String assembleJson(Input input) {
        for (int advisories : new int[]{MAX_ADVISORIES, 10, 3, 0}) {
            String json = JsonTree.write(assemble(input, advisories));
            if (json.length() <= MAX_REPORT_CHARS)
                return json;
        }
        throw new SecurityWatchRunner.RunFailure(SourceClient.SourceFailure.TOO_LARGE);
    }

    static Map<String, Object> assemble(Input input, int maxAdvisories) {
        LocalDate today = input.generatedAt().atZone(ZoneOffset.UTC).toLocalDate();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", SCHEMA_VERSION);
        report.put("environment", input.environment());
        report.put("generatedAt", instant(input.generatedAt()));
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("id", input.runId());
        run.put("trigger", input.trigger());
        run.put("startedAt", instant(input.startedAt()));
        run.put("finishedAt", instant(input.generatedAt()));
        report.put("run", run);
        List<Object> checks = new ArrayList<>();
        for (String id : CHECK_IDS) {
            String failure = input.checkFailures().get(id);
            Map<String, Object> check = new LinkedHashMap<>();
            check.put("id", id);
            check.put("ok", failure == null);
            check.put("error", failure);
            checks.add(check);
        }
        report.put("checks", checks);
        List<Object> components = new ArrayList<>();
        List<Map<String, Object>> actions = new ArrayList<>();
        List<Object> assessed = new ArrayList<>();
        Inventory inventory = input.inventory();
        if (inventory != null) {
            for (Inventory.Component c : inventory.components) {
                if (!c.shownIn(input.environment()))
                    continue;
                Findings findings = input.findings().getOrDefault(c.id(), Findings.empty());
                components.add(component(input, inventory, c, findings, today, actions, maxAdvisories));
            }
            Set<String> shown = new LinkedHashSet<>();
            for (Inventory.Component c : inventory.components)
                if (c.shownIn(input.environment()))
                    shown.add(c.id());
            for (Inventory.Assessed a : inventory.assessed)
                if (shown.contains(a.componentId()) && inForce(a, today)) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", a.id());
                    entry.put("componentId", a.componentId());
                    entry.put("decision", a.decision());
                    entry.put("reason", a.reason());
                    entry.put("decidedOn", day(a.decidedOn()));
                    entry.put("reviewBy", day(a.reviewBy()));
                    assessed.add(entry);
                }
        }
        // Unique ids across the report (the page keys on them): one action per component, one per library, so a repeat
        // would be a bug — refused rather than shipped
        Set<String> ids = new LinkedHashSet<>();
        for (Map<String, Object> action : actions)
            if (!ids.add((String) action.get("id")))
                throw new IllegalStateException("two actions with one id"); // not the id: it names a component and a version
        actions.sort(Comparator.<Map<String, Object>, String>comparing(a -> (String) a.get("dueDate"),
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(a -> Cvss.rank((String) a.get("severity")))
            .thenComparing(a -> (String) a.get("id")));
        report.put("components", components);
        report.put("actions", new ArrayList<Object>(actions));
        report.put("assessed", assessed);
        return report;
    }

    static boolean inForce(Inventory.Assessed a, LocalDate today) {
        return a.reviewBy() == null || !a.reviewBy().isBefore(today);
    }

    // ===== One component ==============================================================================================

    private static Map<String, Object> component(Input input, Inventory inventory, Inventory.Component c, Findings findings,
                                                 LocalDate today, List<Map<String, Object>> allActions, int maxAdvisories) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", c.id());
        out.put("layer", c.layer());
        out.put("name", c.name());
        out.put("where", c.where());
        out.put("managedBy", c.managedBy());
        RuntimeFacts.Values facts = input.facts();
        Instant built = buildInstant(facts);
        Inventory.Running r = c.running();
        Map<String, Object> running = null;
        String version = null;
        boolean unreadable = false;
        Integer packageCount = null;
        List<Libraries.Library> libraries = null;
        Set<String> failed = new LinkedHashSet<>(findings.failedChecks());
        if (r != null) {
            running = new LinkedHashMap<>();
            switch (r.source()) {
                case "live" -> {
                    version = fact(facts, r.fact());
                    // The OS image without a build time is a development machine's OS: nothing about the image to read
                    if (version == null || ("osRelease".equals(r.fact()) && built == null)) {
                        running = null;
                        version = null;
                        unreadable = true;
                        failed.clear();
                    } else {
                        running.put("version", version);
                        running.put("source", "live");
                        running.put("asOf", instant(input.startedAt()));
                        running.put("fact", r.fact());
                    }
                }
                case "build" -> {
                    libraries = "java".equals(r.list()) ? input.javaLibraries() : input.npmLibraries();
                    running.put("version", null);
                    running.put("source", "build");
                    running.put("asOf", built == null ? null : instant(built));
                    running.put("fact", null);
                    if (libraries == null)
                        failed.add("java".equals(r.list()) ? "libraries-java" : "libraries-npm");
                    else
                        packageCount = libraries.size();
                }
                default -> {
                    version = r.version();
                    running.put("version", version);
                    running.put("source", "declared");
                    running.put("asOf", day(r.asOf()));
                    running.put("fact", null);
                }
            }
        }
        out.put("running", running);
        out.put("packageCount", packageCount);
        boolean managed = "provider".equals(c.managedBy());
        EndOfLifeSource.Line line = unreadable || managed ? null : findings.line();
        // The OS image's "newer release" is a newer base image, which has no version: no latest (the contract)
        boolean osImage = r != null && "osRelease".equals(r.fact());
        // Nor does a component compared through another fact: the line's newest release is in the vendor's numbering,
        // which shown beside the running version would read as a pending upgrade (the comparison still decides "newer")
        boolean otherNumbering = c.lifecycle() != null && c.lifecycle().compareFact() != null;
        out.put("latest", line == null || line.latest() == null || osImage || otherNumbering ? null
            : latest(line.latest(), line.latestOn()));
        out.put("supportEnds", line == null ? null : day(line.supportEnds()));

        // Flaws that count: the severity floor of the NVD (known-exploited always counts), minus what was assessed away
        List<Flaw> open = new ArrayList<>();
        if (!managed && !unreadable)
            for (Flaw flaw : findings.flaws()) {
                if (c.nvd() != null && flaw.target() == null && !flaw.knownExploited()
                    && Cvss.rank(flaw.severity()) > Cvss.rank(c.nvd().minSeverity()))
                    continue;
                if (isAssessed(inventory, c, flaw, today))
                    continue;
                open.add(flaw);
            }

        Map<String, ActionDraft> drafts = new LinkedHashMap<>();
        if (!managed && !unreadable)
            decide(input, inventory, c, findings, line, version, built, open, today, libraries, drafts);

        List<String> actionIds = new ArrayList<>();
        for (ActionDraft draft : drafts.values()) {
            Map<String, Object> action = draft.toJson(c, inventory, maxAdvisories);
            actionIds.add((String) action.get("id"));
            allActions.add(action);
        }
        String status;
        boolean newer = newerRelease(c, line, version, facts);
        boolean imageStale = imageStale(c, inventory, findings, built, input.generatedAt());
        if (managed)
            status = "managed";
        else if (unreadable)
            status = "unknown";
        else if (!open.isEmpty())
            status = "vulnerable";
        else if (line != null && line.eol())
            status = "eol";
        else if (!failed.isEmpty())
            status = "unknown";
        else if (line != null && line.supportEnds() != null
                 && !line.supportEnds().isAfter(today.plusDays(inventory.eolSoonDays)))
            status = "eol-soon";
        else if (newer || imageStale)
            status = "update";
        else
            status = "ok";
        out.put("status", status);
        out.put("openAdvisories", open.size());
        out.put("failedChecks", managed ? List.of() : new ArrayList<>(failed));
        out.put("actionIds", actionIds);
        return out;
    }

    /** The decision table of the class comment: one action per component, one per library. */
    private static void decide(Input input, Inventory inventory, Inventory.Component c, Findings findings,
                               EndOfLifeSource.Line line, String version, Instant built, List<Flaw> open, LocalDate today,
                               List<Libraries.Library> libraries, Map<String, ActionDraft> drafts) {
        String fixKind = c.firstHowTo("upgrade", "code-change", "redeploy");
        boolean declared = c.running() != null && "declared".equals(c.running().source());
        boolean library = c.running() != null && "build".equals(c.running().source());
        boolean supportEnding = line != null && line.supportEnds() != null
                                && (line.eol() || !line.supportEnds().isAfter(today.plusDays(inventory.eolSoonDays)));
        if (library) {
            Map<String, List<Flaw>> byLibrary = new LinkedHashMap<>();
            for (Flaw flaw : open)
                byLibrary.computeIfAbsent(flaw.target() == null ? "?" : flaw.target(), t -> new ArrayList<>()).add(flaw);
            for (Map.Entry<String, List<Flaw>> entry : byLibrary.entrySet()) {
                String target = entry.getKey();
                List<Flaw> flaws = entry.getValue();
                String to = highestFix(flaws);
                String kind = fixKind != null && to != null ? fixKind
                    : flaws.stream().anyMatch(f -> !f.uncertain()) ? "wait-vendor" : "verify";
                // One action per package, whatever the number of versions installed: from the lowest of them
                ActionDraft draft = new ActionDraft(kind + ":" + c.id() + ":" + target, kind, target,
                    lowestInstalled(target, flaws, libraries), declared);
                draft.flaws.addAll(flaws);
                draft.to = kind.equals(fixKind) ? to : null;
                drafts.put(draft.id, draft);
            }
            return;
        }
        boolean newer = newerRelease(c, line, version, input.facts());
        boolean imageStale = imageStale(c, inventory, findings, built, input.generatedAt());
        boolean osImage = c.running() != null && "osRelease".equals(c.running().fact());
        boolean staleDeclaration = declared && c.running().asOf() != null
                                   && c.running().asOf().plusDays(inventory.declaredStaleAfterDays).isBefore(today);
        // The version to go to, and whether the fix kind has one (the class comment)
        String to = null;
        boolean target;
        if ("redeploy".equals(fixKind)) {
            target = newer || imageStale;
            // Never a version in another numbering than the running one's (compareFact), nor one for the OS image
            if (newer && !osImage && (c.lifecycle() == null || c.lifecycle().compareFact() == null))
                to = line.latest();
        } else {
            to = highestFix(open);
            if (newer)
                to = higher(to, line.latest());
            if (supportEnding)
                // Moving off the line fixes everything the line has: the inventory's moveTo, as written, else the line
                // to move to (the newest maintained long-term-support one, else the newest maintained one)
                to = c.lifecycle() != null && c.lifecycle().moveTo() != null ? c.lifecycle().moveTo()
                    : higher(to, line.newestMaintained());
            target = to != null;
        }
        boolean certain = open.stream().anyMatch(f -> !f.uncertain() && !declared);
        String kind;
        if (fixKind != null && target)
            kind = fixKind;
        else if (certain)
            kind = "wait-vendor";
        else if (!open.isEmpty() || staleDeclaration)
            kind = "verify";
        else if (supportEnding)
            kind = "decide";
        else
            return;
        ActionDraft draft = new ActionDraft(null, kind, null, version, declared);
        draft.flaws.addAll(open);
        draft.to = kind.equals(fixKind) ? to : null;
        draft.decideSteps = supportEnding && !"decide".equals(kind);
        if ("decide".equals(kind))
            draft.dueOverride = line.supportEnds();
        if (newer && line != null)
            draft.fallbackPublishedOn = line.latestOn();
        else if (line != null && line.eol())
            draft.fallbackPublishedOn = line.supportEnds();
        draft.id = kind + ":" + c.id() + ":" + idPart(draft.to != null ? draft.to : version != null ? version : "unknown");
        drafts.put(draft.id, draft);
    }

    /** The lowest version of a package installed (the list holds one entry per version), else among its flaws. */
    static String lowestInstalled(String name, List<Flaw> flaws, List<Libraries.Library> libraries) {
        String lowest = null;
        if (libraries != null)
            for (Libraries.Library library : libraries)
                if (library.name().equals(name) && (lowest == null || Versions.isLower(library.version(), lowest)))
                    lowest = library.version();
        if (lowest == null)
            for (Flaw flaw : flaws)
                if (flaw.targetVersion() != null && (lowest == null || Versions.isLower(flaw.targetVersion(), lowest)))
                    lowest = flaw.targetVersion();
        return lowest;
    }

    /**
     * The flaws of one action with one entry per shown id: a flaw reached twice (one advisory for each installed version
     * of a package, or two source records with the same CVE alias) is kept once, the more complete record — the one
     * with more of score, summary, fix, publication day and CISA listing.
     */
    static List<Flaw> distinctById(List<Flaw> flaws) {
        Map<String, Flaw> byId = new LinkedHashMap<>();
        for (Flaw flaw : flaws)
            byId.merge(flaw.id(), flaw, (kept, other) -> completeness(other) > completeness(kept) ? other : kept);
        return new ArrayList<>(byId.values());
    }

    private static int completeness(Flaw f) {
        return (f.cvss() != null ? 1 : 0) + (f.summary() != null ? 1 : 0) + (f.fixedIn() != null ? 1 : 0)
               + (f.publishedOn() != null ? 1 : 0) + (f.knownExploited() ? 1 : 0) + (f.uncertain() ? 0 : 1);
    }

    /** The highest of the flaws' fixes: the lowest version that fixes every one of them that has a fix; null when none. */
    static String highestFix(List<Flaw> flaws) {
        String to = null;
        for (Flaw flaw : flaws)
            to = higher(to, flaw.fixedIn());
        return to;
    }

    private static String higher(String a, String b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        return Versions.isLower(a, b) ? b : a;
    }

    /** One action: its flaws, and what it moves from and to. */
    static final class ActionDraft {
        String id;
        final String kind, target, from;
        final boolean declared;
        String to;
        final List<Flaw> flaws = new ArrayList<>();
        boolean decideSteps;
        LocalDate fallbackPublishedOn, dueOverride;

        ActionDraft(String id, String kind, String target, String from, boolean declared) {
            this.id = id;
            this.kind = kind;
            this.target = target;
            this.from = from;
            this.declared = declared;
        }

        /** A flaw only may apply: the version is only declared, or the flaw was not found by version. */
        boolean uncertain(Flaw flaw) {
            return declared || flaw.uncertain();
        }

        Map<String, Object> toJson(Inventory.Component c, Inventory inventory, int maxAdvisories) {
            List<Flaw> sorted = distinctById(flaws);
            sorted.sort(Comparator.<Flaw>comparingInt(f -> Cvss.rank(f.severity()))
                .thenComparing(f -> !f.knownExploited())
                .thenComparing(this::uncertain)
                .thenComparing(f -> f.cvss() == null ? 0.0 : -f.cvss())
                .thenComparing(Flaw::id));
            String severity = sorted.isEmpty() ? "none" : sorted.get(0).severity();
            boolean knownExploited = sorted.stream().anyMatch(Flaw::knownExploited);
            LocalDate publishedOn;
            if (knownExploited)
                publishedOn = sorted.stream().filter(Flaw::knownExploited).map(Flaw::kevAddedOn).min(LocalDate::compareTo).orElse(null);
            else if (!sorted.isEmpty())
                publishedOn = sorted.stream().filter(f -> f.severity().equals(severity)).map(Flaw::publishedOn)
                    .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null);
            else
                publishedOn = fallbackPublishedOn;
            LocalDate due = dueOverride;
            if (due == null && publishedOn != null) {
                Inventory.ResponseTimes times = inventory.responseTimes;
                if (knownExploited)
                    due = publishedOn.plusDays((long) Math.ceil(times.knownExploitedHours() / 24.0));
                else if ("critical".equals(severity))
                    due = publishedOn.plusDays(times.criticalDays());
                else if ("high".equals(severity))
                    due = publishedOn.plusDays(times.highDays());
            }
            Map<String, Object> counts = new LinkedHashMap<>();
            counts.put("total", sorted.size());
            for (String s : new String[]{"critical", "high", "medium", "low"})
                counts.put(s, (int) sorted.stream().filter(f -> s.equals(f.severity())).count());
            counts.put("uncertain", (int) sorted.stream().filter(this::uncertain).count());
            List<Object> advisories = new ArrayList<>();
            for (Flaw flaw : sorted.subList(0, Math.min(maxAdvisories, sorted.size()))) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("id", flaw.id());
                a.put("severity", flaw.severity());
                a.put("cvss", flaw.cvss());
                a.put("summary", flaw.summary());
                a.put("fixedIn", flaw.fixedIn());
                a.put("knownExploited", flaw.knownExploited());
                a.put("publishedOn", day(flaw.publishedOn()));
                a.put("uncertain", uncertain(flaw));
                advisories.add(a);
            }
            boolean fixKind = isFixKind(kind);
            boolean fix = fixKind || ("verify".equals(kind) && sorted.stream().anyMatch(f -> f.fixedIn() != null));
            List<String> howTo = new ArrayList<>(c.howTo().getOrDefault(kind, List.of()));
            if (decideSteps)
                howTo.addAll(c.howTo().getOrDefault("decide", List.of()));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", id);
            out.put("componentId", c.id());
            out.put("kind", kind);
            out.put("target", target);
            out.put("from", from);
            out.put("to", fixKind ? to : null);
            out.put("severity", severity);
            out.put("knownExploited", knownExploited);
            out.put("fixAvailable", fix);
            out.put("publishedOn", day(publishedOn));
            out.put("dueDate", day(due));
            out.put("owner", c.owner() == null ? "" : c.owner());
            out.put("needs", fixKind ? c.needs() : List.of());
            out.put("counts", counts);
            out.put("howTo", howTo);
            out.put("advisories", advisories);
            return out;
        }
    }

    static boolean isFixKind(String kind) {
        return "upgrade".equals(kind) || "code-change".equals(kind) || "redeploy".equals(kind);
    }

    // ===== Rules ======================================================================================================

    static boolean isAssessed(Inventory inventory, Inventory.Component c, Flaw flaw, LocalDate today) {
        for (Inventory.Assessed a : inventory.assessed)
            if (a.componentId().equals(c.id()) && flaw.ids().contains(a.id()) && inForce(a, today))
                return true;
        return false;
    }

    /** Whether the line's newest release is newer than what runs, compared on comparable versions only. */
    static boolean newerRelease(Inventory.Component c, EndOfLifeSource.Line line, String version, RuntimeFacts.Values facts) {
        if (line == null || line.latest() == null || version == null)
            return false;
        // A declared version that is only the release line (2023, 1.27) says nothing about the release that runs
        if (isDeclaredLine(c, line, version))
            return false;
        // The OS image is renewed by redeploying a rebuilt base image, not by moving to a point release
        if (c.running() != null && "osRelease".equals(c.running().fact()))
            return false;
        String running = c.lifecycle() != null && c.lifecycle().compareFact() != null
            ? comparable(fact(facts, c.lifecycle().compareFact())) : comparable(version);
        String latest = comparable(line.latest());
        return running != null && latest != null && Versions.compare(latest, running) > 0;
    }

    /** A declared version equal to the release line's name: no release can be compared with it, only the support date. */
    static boolean isDeclaredLine(Inventory.Component c, EndOfLifeSource.Line line, String version) {
        return c.running() != null && "declared".equals(c.running().source()) && line != null && line.cycle() != null
               && line.cycle().equals(version);
    }

    /** A component read live with a redeploy how-to: the base image newer than our build, or our image too old. */
    static boolean imageStale(Inventory.Component c, Inventory inventory, Findings findings, Instant built, Instant now) {
        if (built == null || c.running() == null || !"live".equals(c.running().source()) || !c.howTo().containsKey("redeploy"))
            return false;
        if (findings.baseImagePushed() != null && findings.baseImagePushed().isAfter(built))
            return true;
        return Duration.between(built, now).toDays() > inventory.imageMaxAgeDays;
    }

    /** A version as an id part: a release name with spaces ({@code Example OS 22.04.5 LTS}) gives its version. */
    static String idPart(String version) {
        if (version == null || !version.contains(" "))
            return version;
        String inside = comparable(version);
        return inside != null ? inside : version.replace(' ', '-');
    }

    private static final Pattern VERSION_IN_TEXT = Pattern.compile("\\d+u\\d+|\\d+(?:[.+_]\\d+)*");

    /**
     * The version in a text, in a form {@link Versions} compares: {@code Example OS 3.2.1 LTS} gives {@code 3.2.1},
     * a vendor's {@code Vendor11.70+15-CA} gives {@code 11.70.15}, {@code 9u101-b02} gives {@code 9u101}.
     */
    static String comparable(String text) {
        if (text == null)
            return null;
        Matcher m = VERSION_IN_TEXT.matcher(text);
        return m.find() ? m.group().replace('+', '.').replace('_', '.') : null;
    }

    static String fact(RuntimeFacts.Values facts, String name) {
        if (facts == null || name == null)
            return null;
        return switch (name) {
            case "javaVersion" -> facts.javaVersion();
            case "javaVendorVersion" -> facts.javaVendorVersion();
            case "osRelease" -> facts.osRelease();
            case "osVersionId" -> facts.osVersionId();
            case "postgresVersion" -> facts.postgresVersion();
            default -> null;
        };
    }

    static Instant buildInstant(RuntimeFacts.Values facts) {
        if (facts == null || facts.buildTimestamp() == null)
            return null;
        try {
            return Instant.parse(facts.buildTimestamp());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Map<String, Object> latest(String version, LocalDate on) {
        Map<String, Object> latest = new LinkedHashMap<>();
        latest.put("version", version);
        latest.put("releasedOn", day(on));
        return latest;
    }

    static String instant(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    static String day(LocalDate day) {
        return day == null ? null : day.toString();
    }
}
