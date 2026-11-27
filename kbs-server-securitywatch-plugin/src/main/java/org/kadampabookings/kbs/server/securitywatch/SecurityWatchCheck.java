package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The daily check: what runs, against the public sources, into one report. Installed as the runner's
 * {@link SecurityWatchRunner.Check}.
 *
 * <ol>
 *   <li><b>Inventory</b> ({@code <directory>/inventory.json}, at most {@value #MAX_INVENTORY_BYTES} bytes). Unreadable,
 *       too large or invalid: the {@code inventory} check fails, the report has no components, and no source is
 *       called (their checks read {@code disabled}). Components whose {@code environments} exclude this server are
 *       left out ({@code development} shows them all).</li>
 *   <li><b>Facts</b>: {@link RuntimeFacts}; the Java libraries ({@link Libraries}, from the jar, else the class path:
 *       fewer than {@value Libraries#MIN_JAVA_LIBRARIES} = {@code missing}); the npm libraries
 *       ({@code <directory>/package-lock.json}, at most {@value #MAX_LOCKFILE_BYTES} bytes).</li>
 *   <li><b>Sources</b>, one call at a time: KEV, OSV (library lists), NVD (components with {@code flaws.nvd}),
 *       endoflife.date (components with a {@code lifecycle}), Docker Hub (once, when a component read live can be
 *       redeployed and the image has a build time). A failing call fails its check and marks the components that
 *       relied on it; it never fails the run.</li>
 *   <li><b>Assembly</b> ({@link ReportAssembler}), off the event loop.</li>
 * </ol>
 *
 * @author Claude Code
 */
final class SecurityWatchCheck implements SecurityWatchRunner.Check {

    static final int MAX_INVENTORY_BYTES = 1024 * 1024;
    static final int MAX_LOCKFILE_BYTES = 5 * 1024 * 1024;
    static final int MAX_JAVA_LIST_BYTES = 2 * 1024 * 1024;

    private final SourceClient client;
    private final Supplier<Instant> clock;
    private final long nvdPaceMillis;
    private final Supplier<String> directory;
    private final Supplier<Future<RuntimeFacts.Values>> facts;
    private final Supplier<String> javaListText;

    /** The production check. */
    SecurityWatchCheck() {
        this(new VertxSourceClient(), Instant::now, NvdSource.PACE_MILLIS, SecurityWatchConfig::directory,
            RuntimeFacts::read, SecurityWatchCheck::readJavaListResource);
    }

    /** For a harness or a check: its own sources, clock, pacing, directory, facts and Java list. */
    SecurityWatchCheck(SourceClient client, Supplier<Instant> clock, long nvdPaceMillis, Supplier<String> directory,
                       Supplier<Future<RuntimeFacts.Values>> facts, Supplier<String> javaListText) {
        this.client = client;
        this.clock = clock;
        this.nvdPaceMillis = nvdPaceMillis;
        this.directory = directory;
        this.facts = facts;
        this.javaListText = javaListText;
    }

    /** The last run's measurements, for the log and the live harness. */
    volatile String lastMeasurements;

    @Override
    public Future<String> run(int runId, String trigger) {
        Instant started = clock.get();
        // Against recordings (no pacing) the retry does not pause either
        Calls calls = nvdPaceMillis == 0 ? new Calls(client, 0, Calls.RUN_BUDGET_MILLIS) : new Calls(client);
        Map<String, String> failures = new LinkedHashMap<>();
        String environment = RuntimeFacts.environment();
        return facts.get().otherwise((RuntimeFacts.Values) null)
            .compose(values -> Async.background(() -> readLocal(environment))
                .compose(local -> sources(calls, environment, values, local, started, failures)
                    .compose(findings -> Async.background(() -> {
                        failures.putAll(local.failures);
                        for (String check : List.of(KevSource.CHECK, OsvSource.CHECK, NvdSource.CHECK, EndOfLifeSource.CHECK, DockerHubSource.CHECK)) {
                            String code = calls.failureOf(check);
                            if (code != null)
                                failures.putIfAbsent(check, code);
                        }
                        Instant generated = clock.get();
                        String json = ReportAssembler.assembleJson(new ReportAssembler.Input(environment, runId, trigger,
                            started, generated, values, local.inventory, failures, local.java, local.npm, findings));
                        lastMeasurements = "calls " + calls.callCount() + " " + calls.callCounts() + " (retries " + calls.retryCount() + "), largest answer "
                                           + calls.largestResponse() + " characters, " + Duration.between(started, generated).toSeconds()
                                           + " s in all, report " + json.length() + " characters, checks failed " + failures;
                        Console.log(SecurityWatchLog.PREFIX + "Run " + runId + ": " + lastMeasurements);
                        return json;
                    }))));
    }

    // ===== Local files ================================================================================================

    /** What the server reads of its own: the inventory and the two library lists. */
    static final class Local {
        Inventory inventory;
        List<Libraries.Library> java, npm;
        final Map<String, String> failures = new LinkedHashMap<>();
    }

    Local readLocal(String environment) {
        Local local = new Local();
        Path dir = Path.of(directory.get());
        try {
            local.inventory = Inventory.parse(readCapped(dir.resolve("inventory.json"), MAX_INVENTORY_BYTES));
        } catch (Inventory.Invalid e) {
            fail(local, "inventory", e.code, "the inventory is invalid (" + e.getMessage() + ")");
        } catch (SourceClient.SourceFailure e) {
            fail(local, "inventory", e.code, "the inventory could not be read (" + e.code + ")");
        }
        if (local.inventory == null) {
            for (String check : ReportAssembler.CHECK_IDS)
                local.failures.putIfAbsent(check, SourceClient.SourceFailure.DISABLED);
            return local;
        }
        boolean needsJava = false, needsNpm = false;
        for (Inventory.Component c : local.inventory.components)
            if (c.shownIn(environment) && c.running() != null && "build".equals(c.running().source())) {
                needsJava |= "java".equals(c.running().list());
                needsNpm |= "npm".equals(c.running().list());
            }
        if (needsJava) {
            String text = javaListText.get();
            List<Libraries.Library> list = text != null ? Libraries.parseJavaList(text)
                : Libraries.fromClassPath(System.getProperty("java.class.path"), File.pathSeparator);
            if (list.size() >= Libraries.MIN_JAVA_LIBRARIES)
                local.java = list;
            else
                fail(local, "libraries-java", SourceClient.SourceFailure.MISSING, "no list of the Java libraries (" + list.size() + " found)");
        }
        if (needsNpm) {
            try {
                local.npm = Libraries.parseNpmLock(readCapped(dir.resolve("package-lock.json"), MAX_LOCKFILE_BYTES));
            } catch (SourceClient.SourceFailure e) {
                fail(local, "libraries-npm", e.code, "no front-end lockfile (" + e.code + ")");
            } catch (IllegalArgumentException e) {
                fail(local, "libraries-npm", SourceClient.SourceFailure.INVALID_RESPONSE, "the front-end lockfile is invalid");
            }
        }
        return local;
    }

    private static void fail(Local local, String check, String code, String why) {
        local.failures.putIfAbsent(check, code);
        Console.log(SecurityWatchLog.PREFIX + check + ": " + why);
    }

    /** A file's text, refused above {@code maxBytes}; a missing file is {@code missing}. */
    static String readCapped(Path path, int maxBytes) {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] bytes = in.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes)
                throw new SourceClient.SourceFailure(SourceClient.SourceFailure.TOO_LARGE);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new SourceClient.SourceFailure(SourceClient.SourceFailure.MISSING);
        } catch (IOException e) {
            throw new SourceClient.SourceFailure(SourceClient.SourceFailure.MISSING);
        }
    }

    /** The list the build wrote into the jar, or null when there is none (a run from the IDE). */
    static String readJavaListResource() {
        for (ClassLoader loader : new ClassLoader[]{Thread.currentThread().getContextClassLoader(),
            SecurityWatchCheck.class.getClassLoader(), ClassLoader.getSystemClassLoader()}) {
            if (loader == null)
                continue;
            try (InputStream in = loader.getResourceAsStream(Libraries.JAVA_RESOURCE)) {
                if (in == null)
                    continue;
                byte[] bytes = in.readNBytes(MAX_JAVA_LIST_BYTES + 1);
                return bytes.length > MAX_JAVA_LIST_BYTES ? null : new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    // ===== Sources ====================================================================================================

    private Future<Map<String, ReportAssembler.Findings>> sources(Calls calls, String environment, RuntimeFacts.Values values,
                                                                   Local local, Instant started, Map<String, String> failures) {
        Map<String, ReportAssembler.Findings> findings = new LinkedHashMap<>();
        if (local.inventory == null)
            return Future.succeededFuture(findings);
        Inventory inventory = local.inventory;
        LocalDate today = started.atZone(ZoneOffset.UTC).toLocalDate();
        Instant built = ReportAssembler.buildInstant(values);
        List<Inventory.Component> checked = new ArrayList<>();
        for (Inventory.Component c : inventory.components)
            if (c.shownIn(environment) && !"provider".equals(c.managedBy()) && runningVersionOrList(c, values, built))
                checked.add(c);
        Map<String, List<Flaw>> flaws = new LinkedHashMap<>();
        Map<String, Set<String>> failed = new LinkedHashMap<>();
        Map<String, EndOfLifeSource.Line> lines = new LinkedHashMap<>();
        for (Inventory.Component c : checked) {
            flaws.put(c.id(), new ArrayList<>());
            failed.put(c.id(), new LinkedHashSet<>());
        }
        boolean needsKev = checked.stream().anyMatch(c -> c.nvd() != null || c.osvEcosystem() != null || !c.kev().isEmpty());
        List<Map<String, KevSource.Entry>> kev = new ArrayList<>(java.util.Collections.singletonList(null));
        Instant[] basePushed = {null};
        NvdSource nvd = new NvdSource(calls, started.minus(Duration.ofDays(NvdSource.RECENT_DAYS)), started, nvdPaceMillis);
        Map<String, Future<Map<String, Object>>> eolCache = EndOfLifeSource.newCache();
        Set<String> osvBudget = new LinkedHashSet<>();
        Future<Void> chain = !needsKev ? Future.succeededFuture()
            : KevSource.load(calls).map(catalogue -> {
                kev.set(0, catalogue);
                return null;
            });
        // OSV, for the library lists
        chain = chain.compose(v -> Async.each(checked, c -> {
            if (c.osvEcosystem() == null || c.running() == null || !"build".equals(c.running().source()))
                return Future.succeededFuture();
            List<Libraries.Library> libraries = "java".equals(c.running().list()) ? local.java : local.npm;
            if (libraries == null)
                return Future.succeededFuture();
            return OsvSource.check(calls, c.osvEcosystem(), libraries, osvBudget).map(result -> {
                result.flaws.values().forEach(flaws.get(c.id())::addAll);
                if (result.failed)
                    failed.get(c.id()).add(OsvSource.CHECK);
                return null;
            });
        }));
        // NVD
        chain = chain.compose(v -> Async.each(checked, c -> {
            if (c.nvd() == null)
                return Future.succeededFuture();
            return nvd.check(c.nvd(), runningVersion(c, values)).map(result -> {
                flaws.get(c.id()).addAll(result.flaws());
                if (result.failed())
                    failed.get(c.id()).add(NvdSource.CHECK);
                return null;
            });
        }));
        // endoflife.date
        chain = chain.compose(v -> Async.each(checked, c -> {
            if (c.lifecycle() == null)
                return Future.succeededFuture();
            String version = runningVersion(c, values);
            String cycle = EndOfLifeSource.cycleOf(c.lifecycle(), version, factsMap(values));
            return EndOfLifeSource.product(calls, c.lifecycle().product(), eolCache).map(product -> {
                EndOfLifeSource.Line line = EndOfLifeSource.line(product, cycle, today);
                if (line != null)
                    lines.put(c.id(), line);
                else {
                    failed.get(c.id()).add(EndOfLifeSource.CHECK);
                    if (product != null)
                        // Which component, not which line: a version beside a component name is not for the aggregated log
                        Console.log(SecurityWatchLog.PREFIX + "Component " + c.id() + ": its release line is not on endoflife.date"
                                    + " (check its lifecycle in the inventory)");
                }
                return null;
            });
        }));
        // Docker Hub, once
        boolean needsDockerHub = built != null && checked.stream().anyMatch(SecurityWatchCheck::redeploysLive);
        if (needsDockerHub)
            chain = chain.compose(v -> DockerHubSource.lastPushed(calls, inventory.baseImage).map(when -> {
                basePushed[0] = when;
                if (when == null)
                    for (Inventory.Component c : checked)
                        if (redeploysLive(c))
                            failed.get(c.id()).add(DockerHubSource.CHECK);
                return null;
            }));
        return chain.map(v -> {
            for (Inventory.Component c : checked) {
                List<Flaw> marked = new ArrayList<>();
                Set<String> seen = new LinkedHashSet<>();
                for (Flaw flaw : flaws.get(c.id())) {
                    Flaw f = flaw.withKev(KevSource.addedOn(kev.get(0), flaw.ids()));
                    marked.add(f);
                    seen.addAll(f.ids());
                }
                for (Flaw byName : KevSource.recentByName(kev.get(0), c.kev(), today))
                    if (!seen.contains(byName.id()))
                        marked.add(byName);
                // Every component whose flaws could be in the catalogue relied on it: without it, none is marked exploited
                if (kev.get(0) == null && needsKev && reliesOnKev(c))
                    failed.get(c.id()).add(KevSource.CHECK);
                findings.put(c.id(), new ReportAssembler.Findings(marked, failed.get(c.id()), lines.get(c.id()), basePushed[0]));
            }
            return findings;
        });
    }

    /** A component read live that the base image's redeploy renews: the Docker Hub check is about it. */
    static boolean redeploysLive(Inventory.Component c) {
        return c.running() != null && "live".equals(c.running().source()) && c.howTo().containsKey("redeploy");
    }

    /** A component whose flaws the catalogue marks exploited (OSV or NVD flaws), or that watches names in it. */
    static boolean reliesOnKev(Inventory.Component c) {
        return c.osvEcosystem() != null || c.nvd() != null || !c.kev().isEmpty();
    }

    /** Whether there is anything to check: a version read live (with an image build time for the OS), a list, or a declaration. */
    private static boolean runningVersionOrList(Inventory.Component c, RuntimeFacts.Values values, Instant built) {
        if (c.running() == null)
            return true;
        if ("live".equals(c.running().source()))
            return ReportAssembler.fact(values, c.running().fact()) != null && !("osRelease".equals(c.running().fact()) && built == null);
        return true;
    }

    /** The version to compare: the live fact's version part (Example OS 3.2.1 LTS gives 3.2.1), or the declared one. */
    static String runningVersion(Inventory.Component c, RuntimeFacts.Values values) {
        if (c.running() == null)
            return null;
        if ("live".equals(c.running().source())) {
            String fact = ReportAssembler.fact(values, c.running().fact());
            return "osRelease".equals(c.running().fact()) ? Versions.firstVersionIn(fact) : fact;
        }
        return "declared".equals(c.running().source()) ? c.running().version() : null;
    }

    private static Map<String, String> factsMap(RuntimeFacts.Values values) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : List.of("javaVersion", "javaVendorVersion", "osRelease", "osVersionId", "postgresVersion"))
            map.put(name, ReportAssembler.fact(values, name));
        return map;
    }
}
