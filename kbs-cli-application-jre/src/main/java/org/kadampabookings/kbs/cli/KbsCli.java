package org.kadampabookings.kbs.cli;

import dev.webfx.stack.db.query.cli.Audience;
import dev.webfx.stack.db.query.cli.CallerSession;
import dev.webfx.stack.db.query.cli.CliArgs;
import dev.webfx.stack.db.query.cli.ClientRuntime;
import dev.webfx.stack.db.query.cli.ConsolePrompt;
import dev.webfx.stack.db.query.cli.DqlResult;
import dev.webfx.stack.db.query.cli.DqlRunner;
import dev.webfx.stack.db.query.cli.ResultPrinter;
import dev.webfx.stack.db.query.cli.RunLog;
import dev.webfx.stack.db.query.cli.ServerEndpoint;
import dev.webfx.stack.db.query.cli.SessionCache;
import one.modality.crm.shared.services.authn.ModalityUserPrincipal;
import org.kadampabookings.kbs.cli.readscope.Invariant;
import org.kadampabookings.kbs.cli.readscope.Probe;
import org.kadampabookings.kbs.cli.readscope.Probes;
import org.kadampabookings.kbs.cli.readscope.RehearsalMatrix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The KBS command-line client: asks a real caller's questions of a real server and asserts invariants.
 *
 * <h3>Named for the program, not for v1's content</h3>
 *
 * <p>Every probe this ships with is about read scope, so calling it a read-scope probe tool would be accurate
 * today and wrong by the second subject. The trajectory for it is the one use of direct database access that
 * survives closing that access off — running an arbitrary statement as a real caller — and {@code query -c}
 * already does that. At that point {@code probe} is one subcommand of several, so the subject-matter naming
 * lives where it costs nothing to be specific: inside, in {@link Probes}, {@link RehearsalMatrix} and
 * {@link Invariant}.
 *
 * <h3>What it is for</h3>
 *
 * <p>The read-authorisation arc has two kinds of instrument and neither can ask a question of a running server
 * as a real caller. The {@code *Check} programs assert rules offline and cannot see a runtime value; the
 * production census observes what happened and cannot ask anything — it waits for traffic. This is the third
 * kind, and the immediate reason for it is that the chat read-scope rehearsal can prove the scope does not
 * OVER-restrict and cannot prove it restricts: the support inbox's event picker only offers events the
 * operator holds, so the operator cannot ask about an event they should not see. A statement issued outside
 * the UI can.
 *
 * <h3>Where it starts</h3>
 *
 * <p>Not here: {@code Main-Class} is the platform's {@code ApplicationBooter}, as in every WebFX executable,
 * and {@link KbsCliJob} is what it reaches. The server a run talks to is supplied to the bus by
 * {@link KbsCliVariablesResolver} while the configuration is substituted, which is early enough because the
 * bus connects during the boot rather than lazily.
 *
 * <h3>Exit codes, because something will run this unattended</h3>
 *
 * <ul>
 *   <li>{@code 0} — every applicable invariant held, and at least one was evaluated.</li>
 *   <li>{@code 1} — an invariant was VIOLATED.</li>
 *   <li>{@code 2} — the run could not be made: a usage mistake, no session, a refused query, a wrong
 *       audience.</li>
 *   <li>{@code 3} — nothing could be evaluated (no baseline recorded, or a probe/statement with no
 *       invariant). The numbers are printed; nothing was asserted. A separate code from 0 on purpose — a
 *       rehearsal that quietly asserted nothing and exited zero is the failure shape this arc has already
 *       been misled by once.</li>
 * </ul>
 *
 * @author Claude Code
 */
public final class KbsCli {

    static final String DATA_SOURCE_ID = "MDS";
    static final int DEFAULT_LIMIT = 100;
    static final int DEFAULT_TIMEOUT_MILLIS = 30_000;

    static final int EXIT_OK = 0, EXIT_VIOLATED = 1, EXIT_ERROR = 2, EXIT_NOTHING_EVALUATED = 3;

    private KbsCli() {}

    /**
     * Runs one command and answers its exit code. Never throws: a usage mistake is a message and a code, not a
     * stack trace.
     *
     * <p>Called by {@link KbsCliJob}, which is what the platform's own {@code ApplicationBooter} reaches. There
     * is no {@code main} here — see that class for why, and for why it is the thing that ends the process.
     */
    public static int run(String[] args) {
        try {
            return dispatch(args);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("✖ " + e.getMessage());
            return EXIT_ERROR;
        } catch (Exception e) {
            System.err.println("✖ " + e);
            return EXIT_ERROR;
        }
    }

    static int dispatch(String[] args) throws Exception {
        String command = args.length == 0 ? "help" : args[0];
        String[] rest = args.length == 0 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
        return switch (command) {
            case "help", "--help", "-h" -> {
                System.out.println(help());
                yield EXIT_OK;
            }
            case "probes" -> {
                System.out.println("Probes:\n");
                for (Probe probe : Probes.all())
                    System.out.println("  " + probe.describe() + "\n      " + probe.dql() + "\n");
                yield EXIT_OK;
            }
            case "rehearsal" -> {
                System.out.println(RehearsalMatrix.plan());
                yield EXIT_OK;
            }
            case "signin" -> signIn(rest);
            case "signout" -> signOut(rest);
            case "status" -> status(rest);
            case "probe" -> probe(rest);
            case "query" -> query(rest);
            default -> throw new IllegalArgumentException(
                "Unknown command '" + command + "'. Try: help, probes, rehearsal, signin, status, probe,"
                + " query, signout.");
        };
    }

    // ---------------------------------------------------------------- commands

    private static int signIn(String[] args) throws Exception {
        CliArgs parsed = commonSpec().option("username").flag("with-code").parse(args);
        boolean withCode = parsed.flag("with-code");
        Path confDirectory = confDirectory(parsed);
        ServerEndpoint endpoint = endpointOf(parsed.required("server",
            "there is no default server, on purpose — a probe must say which one it asked"));
        Audience audience = audienceOf(parsed.required("audience",
            "a run must say which arm it is testing; see the help text"));
        String username = parsed.value("username", null);
        if (username == null)
            username = ConsolePrompt.readLine("Username: ");
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("No username was given.");

        SessionCache cache = new SessionCache(confDirectory);
        // A verification code is read exactly as a password is - from a terminal, into a char[] this method
        // clears, never from the command line. It mints a session, so it IS a credential; that it is six
        // digits and dies in minutes makes it short-lived, not harmless.
        char[] secret = null;
        try (ClientRuntime runtime = ClientRuntime.connect(endpoint, DATA_SOURCE_ID, timeout(parsed))) {
            // REQUESTING AND REDEEMING HAPPEN IN ONE RUN, and the server requires it. A code is looked up by
            // value AND by the loginRunId that asked for it - "the entry tab MUST be the request tab", which
            // is what stops two people sharing six digits in the same minute and is what makes the attempt cap
            // mean anything. Asking in one command and redeeming in another gave two runIds and an honest
            // "[LoginLinkUnrecognisedError] Magic link not found". So this command asks, then waits with the
            // connection open while somebody fetches the code.
            if (withCode) {
                CallerSession.sendVerificationCode(username, endpoint, audience, timeout(parsed));
                System.out.println("A verification code was sent to " + username + ".");
                System.out.println("This run must redeem it: the server ties a code to the run that asked for"
                                   + " it, so the code is useless to any other command. Waiting.");
            }
            secret = ConsolePrompt.readPassword(
                (withCode ? "Verification code for " : "Password for ") + username + ": ");
            CallerSession session = withCode
                ? CallerSession.signInWithVerificationCode(username, secret, audience, timeout(parsed))
                : CallerSession.signIn(username, secret, audience, timeout(parsed));
            String audienceProblem = session.confirmAudience();
            if (audienceProblem != null) {
                // The session exists server-side but is the wrong arm. NOT cached: a cache of it would be
                // presented by the next run, which would then test the arm this one refused.
                session.signOut(timeout(parsed));
                throw new IllegalStateException(audienceProblem);
            }
            cache.store(new SessionCache.Entry(session.token(), username, audience, endpoint.describe(),
                                               System.currentTimeMillis()));
            System.out.println("Signed in as " + username + ", confirmed " + audience.optionValue()
                               + " (read back from the server's own token signature).");
            System.out.println("Cached for " + SessionCache.TTL_MILLIS / 60000 + " minutes in "
                               + cache.file() + " — a live session token; mode 600.");
            return EXIT_OK;
        } finally {
            if (secret != null)
                Arrays.fill(secret, '\0');
        }
    }

    private static int signOut(String[] args) throws Exception {
        CliArgs parsed = commonSpec().parse(args);
        SessionCache cache = new SessionCache(confDirectory(parsed));
        SessionCache.Entry entry = cache.load(System.currentTimeMillis());
        if (entry == null) {
            cache.clear();
            System.out.println("No cached session.");
            return EXIT_OK;
        }
        // Read the server into RunServer's memo while the cache still exists. The bus configuration
        // substitutes its variables on READ, not once at boot, so the resolver is asked inside
        // ClientRuntime.connect() below — after the clear() on the next line. Before this call, signout
        // deleted the only record of its own server and then refused itself with "The bus configuration
        // resolved to ws://localhost:80 but wss://… was asked for", leaving the session open server-side.
        RunServer.endpoint();
        // Clear the local copy FIRST. If the server round trip fails, the token is already off this disk,
        // which is the half that can be guaranteed; a token still in a file after a failed logout is worse
        // than a session still open on a server that will expire it within the access window.
        cache.clear();
        ServerEndpoint endpoint = ServerEndpoint.parse(entry.server());
        if (endpoint == null) {
            // The cache recorded no usable server, so there is nothing to tell. Said plainly rather than
            // thrown: the local half - the only half this command can guarantee - has already been done.
            System.out.println("The cache is gone. It recorded no usable server, so the session was not ended"
                               + " server-side; it will expire on its own.");
            return EXIT_OK;
        }
        try (ClientRuntime runtime = ClientRuntime.connect(endpoint, DATA_SOURCE_ID, timeout(parsed))) {
            CallerSession.restore(entry, timeout(parsed)).signOut(timeout(parsed));
            System.out.println("Signed out, and the cache is gone.");
        } catch (RuntimeException e) {
            System.out.println("The cache is gone. The server was not reached (" + e.getMessage()
                               + "), so the session will expire on its own.");
        }
        return EXIT_OK;
    }

    private static int status(String[] args) throws Exception {
        CliArgs parsed = commonSpec().parse(args);
        SessionCache cache = new SessionCache(confDirectory(parsed));
        SessionCache.Entry entry = cache.load(System.currentTimeMillis());
        if (entry == null) {
            System.out.println("No usable cached session. Run 'signin' from a terminal.");
            return EXIT_OK;
        }
        long ageMillis = System.currentTimeMillis() - entry.storedAtMillis();
        System.out.println("Cached session:");
        System.out.println("  caller   " + entry.username());
        System.out.println("  audience " + (entry.audience() == null ? "(unknown)" : entry.audience().optionValue()));
        System.out.println("  server   " + entry.server());
        System.out.println("  age      " + ageMillis / 1000 + "s of " + SessionCache.TTL_MILLIS / 1000 + "s");
        System.out.println("  file     " + cache.file() + " (the token itself is not printed, ever)");
        return EXIT_OK;
    }

    private static int probe(String[] args) throws Exception {
        CliArgs.Spec spec = commonSpec()
            .option("caller").option("limit").option("offset")
            .flag("as-baseline").flag("rows").flag("confirm-personal-data");
        // Every probe's parameter names become options, so --eventId works for the probes that take one and
        // is an unknown option for the probes that do not. Declaring a name twice is harmless; the point is
        // that a parameter added to a probe needs no second edit here.
        for (Probe anyProbe : Probes.all())
            for (String parameter : anyProbe.parameters())
                spec.option(parameter);
        CliArgs parsed = spec.parse(args);

        String name = parsed.positional(0, null);
        if (name == null)
            throw new IllegalArgumentException("Which probe? One of: " + Probes.names()
                                               + ". 'probes' describes each one.");
        Probe probe = Probes.byName(name);
        if (probe == null)
            throw new IllegalArgumentException("No probe called '" + name + "'. One of: " + Probes.names() + ".");

        Object[] parameters = parametersFor(probe, parsed);
        RehearsalMatrix.Caller caller = callerOf(parsed);
        Path logDirectory = logDirectory(parsed);
        SessionCache cache = new SessionCache(confDirectory(parsed));
        SessionCache.Entry entry = requireSession(cache);
        Audience audience = requireAudience(entry, parsed);

        String audienceProblem = probe.audienceRefusal(audience);
        if (audienceProblem != null)
            throw new IllegalStateException(audienceProblem);

        boolean showRows = parsed.flag("rows");
        if (showRows && !probe.hasRows())
            throw new IllegalArgumentException("Probe '" + probe.name() + "' has no row form.");
        if (showRows && probe.showsPersonalData() && !parsed.flag("confirm-personal-data"))
            throw new IllegalArgumentException(
                "Probe '" + probe.name() + "' answers with personal data, so showing its rows needs"
                + " --confirm-personal-data as well as --rows. The count already makes the point this probe"
                + " exists to make; the rows put names on a screen. (Not a prohibition — a speed bump.)");

        RunLog runLog = new RunLog(logDirectory);
        BaselineStore baselines = new BaselineStore(logDirectory);
        String baselineKey = BaselineStore.keyFor(probe.name(), parameters);

        try (ClientRuntime runtime = ClientRuntime.connect(ServerEndpoint.parse(entry.server()), DATA_SOURCE_ID,
                                                           timeout(parsed))) {
            CallerSession session = CallerSession.restore(entry, timeout(parsed));
            String confirmation = session.confirmAudience();
            if (confirmation != null)
                throw new IllegalStateException(confirmation);
            String callerProblem = callerRefusal(caller, session.userId());
            if (callerProblem != null)
                throw new IllegalStateException(callerProblem);

            DqlRunner runner = new DqlRunner(DATA_SOURCE_ID, timeout(parsed));
            long count;
            try {
                count = runner.run(probe.dql(), parameters).count();
            } catch (RuntimeException e) {
                runLog.append(entry.username(), audience, probe.name(), probe.dql(), -1);
                throw e;
            }
            runLog.append(entry.username(), audience, probe.name(), probe.dql(), count);

            System.out.println("probe    " + probe.name());
            System.out.println("caller   " + entry.username()
                               + (caller == null ? "" : " (--caller " + caller.label() + ": " + caller.grant() + ")"));
            System.out.println("audience " + audience.optionValue() + " (confirmed from the token)");
            System.out.println("dql      " + probe.dql());
            if (parameters.length > 0)
                System.out.println("with     " + Arrays.toString(parameters));
            ResultPrinter.printCount(System.out, "count", count);

            if (parsed.flag("as-baseline")) {
                baselines.store(baselineKey, count, System.currentTimeMillis());
                System.out.println();
                System.out.println("Recorded as the UNSCOPED baseline for " + baselineKey + " in "
                                   + baselines.file() + ".");
                System.out.println("That is only true if this session is a platform super administrator — a"
                                   + " caller whose scope has no predicate at all. The tool cannot check that,"
                                   + " and a baseline taken as anybody else makes every later comparison"
                                   + " meaningless rather than wrong.");
                return EXIT_OK;
            }

            if (showRows) {
                System.out.println();
                if (probe.showsPersonalData())
                    System.out.println("⚠️  These rows are personal data. They are not logged; the screen is"
                                       + " yours to clear.");
                int limit = parsed.intValue("limit", DEFAULT_LIMIT);
                int offset = parsed.intValue("offset", 0);
                DqlResult rows = runner.runPaged(probe.rowsDql(), parameters, limit, offset);
                runLog.append(entry.username(), audience, probe.name() + " (rows)", probe.rowsDql(), rows.rowCount());
                ResultPrinter.printRows(System.out, rows, offset);
            }

            BaselineStore.Baseline baseline = baselines.load(baselineKey);
            System.out.println();
            if (baseline == null)
                System.out.println("No unscoped baseline for " + baselineKey + " — run this probe once as a"
                                   + " platform super administrator with --as-baseline.");
            else
                System.out.println("unscoped " + baseline.count() + " (recorded "
                                   + Instant.ofEpochMilli(baseline.recordedAtMillis()) + ")");

            Boolean reachable = caller == null || parameters.length == 0 ? null
                : RehearsalMatrix.reachable(caller, ((Number) parameters[0]).intValue());
            if (caller != null && parameters.length > 0 && reachable == null)
                System.out.println("The matrix does not declare whether " + caller.label() + " reaches this"
                                   + " event, so the per-cell invariants are not evaluated.");

            Invariant.Measurement measurement = new Invariant.Measurement(
                probe.name(), caller == null ? entry.username() : caller.label(), count,
                baseline == null ? null : baseline.count(), reachable);

            System.out.println();
            if (probe.invariants().isEmpty()) {
                System.out.println("This probe asserts nothing: it is a measurement. " + probe.summary());
                return EXIT_NOTHING_EVALUATED;
            }
            int violations = 0, held = 0;
            for (Invariant invariant : probe.invariants()) {
                Invariant.Verdict verdict = invariant.evaluate(measurement);
                System.out.println(verdict.describe());
                if (verdict.violated())
                    violations++;
                else if (verdict.outcome() == Invariant.Verdict.Outcome.HOLDS)
                    held++;
            }
            if (violations > 0)
                return EXIT_VIOLATED;
            return held == 0 ? EXIT_NOTHING_EVALUATED : EXIT_OK;
        }
    }

    private static int query(String[] args) throws Exception {
        CliArgs parsed = commonSpec()
            .option("c").option("f").option("limit").option("offset")
            .repeatable("param")
            .flag("count")
            .parse(args);
        String statement = parsed.value("c", null);
        String file = parsed.value("f", null);
        if (statement == null && file == null)
            throw new IllegalArgumentException("Give a statement with -c \"...\" or a file with -f <path>.");
        if (statement != null && file != null)
            throw new IllegalArgumentException("Give -c or -f, not both.");

        List<Object> parameterValues = new ArrayList<>();
        for (String value : parsed.values("param"))
            parameterValues.add(asParameter(value));
        Object[] parameters = parameterValues.toArray();

        Path logDirectory = logDirectory(parsed);
        Path confDirectory = confDirectory(parsed);
        if (file != null)
            statement = readStatementFile(confDirectory, file);
        SessionCache cache = new SessionCache(confDirectory);
        SessionCache.Entry entry = requireSession(cache);
        Audience audience = requireAudience(entry, parsed);
        RunLog runLog = new RunLog(logDirectory);

        try (ClientRuntime runtime = ClientRuntime.connect(ServerEndpoint.parse(entry.server()), DATA_SOURCE_ID,
                                                           timeout(parsed))) {
            CallerSession session = CallerSession.restore(entry, timeout(parsed));
            String confirmation = session.confirmAudience();
            if (confirmation != null)
                throw new IllegalStateException(confirmation);
            DqlRunner runner = new DqlRunner(DATA_SOURCE_ID, timeout(parsed));
            System.out.println("caller   " + entry.username() + " (" + audience.optionValue() + ", confirmed)");
            // A statement from -c was typed by the operator, so echoing it says what ran. A statement from
            // -f is FILE CONTENT, and printing file content is what turned this command into a way to read
            // a file aloud — see readStatementFile. The same reasoning governs what reaches the run log,
            // which is the LESS protected of the two: it is neither mode 600 nor named in the credential
            // guard, so content that must not be printed must certainly not be appended there.
            String shown = file == null ? statement : "(from " + file + ")";
            System.out.println("dql      " + shown);
            if (parsed.flag("count")) {
                long count = runner.run(statement, parameters).count();
                runLog.append(entry.username(), audience, file == null ? "-c" : "-f", shown, count);
                ResultPrinter.printCount(System.out, "count", count);
            } else {
                int limit = parsed.intValue("limit", DEFAULT_LIMIT);
                int offset = parsed.intValue("offset", 0);
                DqlResult result = runner.runPaged(statement, parameters, limit, offset);
                runLog.append(entry.username(), audience, file == null ? "-c" : "-f", shown, result.rowCount());
                ResultPrinter.printRows(System.out, result, offset);
            }
            // No invariant: an arbitrary statement is a probe record with none, exactly as the design note
            // intended. Nothing was asserted, and the exit code says so rather than implying a pass.
            return EXIT_NOTHING_EVALUATED;
        }
    }

    // ---------------------------------------------------------------- shared option handling

    private static CliArgs.Spec commonSpec() {
        return new CliArgs.Spec()
            .option("server").option("audience").option("conf-dir").option("log-dir").option("timeout");
    }

    private static Object[] parametersFor(Probe probe, CliArgs parsed) {
        Object[] parameters = new Object[probe.parameterCount()];
        for (int i = 0; i < parameters.length; i++) {
            String name = probe.parameters().get(i);
            String value = parsed.value(name, null);
            if (value == null)
                throw new IllegalArgumentException("Probe '" + probe.name() + "' needs --" + name
                                                   + " (bound to $" + (i + 1) + ").");
            parameters[i] = asParameter(value);
        }
        return parameters;
    }

    /**
     * A command-line value as a bind parameter.
     *
     * <p>Numbers are passed as numbers, because a query parameter is a raw scalar and the server coerces
     * nothing: an id sent as a string fails server-side rather than matching.
     */
    static Object asParameter(String value) {
        String trimmed = value.strip();
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException notAnInt) {
            try {
                return Long.valueOf(trimmed);
            } catch (NumberFormatException notALong) {
                return value;
            }
        }
    }

    private static SessionCache.Entry requireSession(SessionCache cache) throws Exception {
        SessionCache.Entry entry = cache.load(System.currentTimeMillis());
        if (entry == null)
            throw new IllegalStateException(
                "No usable cached session. Run 'signin --server <url> --audience <front-office|back-office>'"
                + " from a terminal first; the cached session then carries unattended runs for "
                + SessionCache.TTL_MILLIS / 60000 + " minutes.");
        if (entry.server() == null || ServerEndpoint.parse(entry.server()) == null)
            throw new IllegalStateException("The cached session does not record a usable server. Sign in again.");
        return entry;
    }

    /**
     * The audience this run is about: the cached one, refusing when the command line asks for another.
     *
     * <p>A mismatch is refused rather than reconciled. The alternative — silently using the cached audience —
     * is a run that was asked for one arm and tested the other, which is the failure this tool was
     * commissioned after and must not reproduce in its own option handling.
     */
    private static Audience requireAudience(SessionCache.Entry entry, CliArgs parsed) {
        Audience cached = entry.audience();
        if (cached == null)
            throw new IllegalStateException("The cached session does not record an audience. Sign in again.");
        String requested = parsed.value("audience", null);
        if (requested != null && audienceOf(requested) != cached)
            throw new IllegalStateException("--audience " + requested + " was asked for, but the cached session"
                                            + " is a " + cached.optionValue() + " one. Sign out and sign in"
                                            + " again for the other arm.");
        String server = parsed.value("server", null);
        if (server != null && !server.equals(entry.server()))
            throw new IllegalStateException("--server " + server + " was asked for, but the cached session is"
                                            + " for " + entry.server() + ".");
        return cached;
    }

    private static ServerEndpoint endpointOf(String url) {
        ServerEndpoint endpoint = ServerEndpoint.parse(url);
        if (endpoint == null)
            throw new IllegalArgumentException(
                "'" + url + "' is not a server this tool can parse. Give a scheme and a host, for example"
                + " wss://backoffice.staging.example.org or http://localhost:8080.");
        return endpoint;
    }

    private static Audience audienceOf(String value) {
        Audience audience = Audience.ofOptionValue(value);
        if (audience == null)
            throw new IllegalArgumentException("--audience must be front-office or back-office, not '"
                                               + value + "'.");
        return audience;
    }

    /**
     * Why this session is not the caller the command claimed, or null when it is.
     *
     * <h3>Why the claim has to be checked rather than printed</h3>
     *
     * <p>{@code --caller} tells the tool which relation applies to a cell: {@code = unscoped} where that
     * identity holds a grant reaching the event, {@code = 0} where it does not. Pass the wrong label and the
     * matrix answers about somebody else's grants, and the run reports a pass. Printing both the label and the
     * username on every run was the mitigation; this is the fix.
     *
     * <h3>What made it necessary rather than tidy</h3>
     *
     * <p>A login address is not an identity. Three of the rehearsal's five staging accounts are shared by
     * other persons - one of them by nine - so the address says nothing about which person the server
     * authenticated as. On the {@code event-1840} row, where every cell must be zero, a sweep of the wrong
     * person produces output identical to a correct one. Through the 2026-11-24 rehearsal this was caught by
     * running {@code select id from Person where id=CALLER_PERSON} by hand before each sweep: a manual guard
     * in front of the one mistake this tool exists to prevent, which is a guard that will eventually be
     * skipped.
     *
     * <p>It became cheap only once the client could read its own principal - before
     * {@code modality-crm-shared-authn} was declared, {@code session.userId()} would not decode at all.
     *
     * <h3>Refusing when it cannot tell</h3>
     *
     * <p>An unreadable principal is a refusal, not a shrug, for the reason the audience assertion refuses: a
     * check that passes when it cannot see its subject is worse than no check, because it reads as
     * confirmation.
     *
     * <p>Takes the userId rather than the session, so that the comparison - which is all this is - can be
     * checked without one. {@code CallerBindingCheck} does exactly that.
     */
    static String callerRefusal(RehearsalMatrix.Caller caller, Object userId) {
        if (caller == null)
            return null; // no claim was made, so there is nothing to contradict
        Object personId = ModalityUserPrincipal.getUserPersonId(userId);
        if (personId == null)
            return "This run claims --caller " + caller.label() + ", but the session does not say which person"
                   + " it belongs to, so the claim cannot be checked. Refusing rather than reporting a result"
                   + " that may be about another caller's grants. Sign in again; if it persists, the principal"
                   + " is not being decoded (modality-crm-shared-authn supplies that codec).";
        if (!(personId instanceof Number number) || number.intValue() != caller.personId())
            return "This run claims --caller " + caller.label() + ", which is person " + caller.personId()
                   + ", but this session belongs to person " + personId + ". The matrix would have answered"
                   + " about the wrong grants - and on a row whose cells are all zero, that answer is"
                   + " indistinguishable from a correct one. Sign in as person " + caller.personId()
                   + ", or drop --caller to get a count with no invariant attached.";
        return null;
    }

    private static RehearsalMatrix.Caller callerOf(CliArgs parsed) {
        String label = parsed.value("caller", null);
        if (label == null)
            return null;
        RehearsalMatrix.Caller caller = RehearsalMatrix.caller(label);
        if (caller == null)
            throw new IllegalArgumentException("--caller must be one of: " + RehearsalMatrix.labels() + ".");
        return caller;
    }

    private static Path confDirectory(CliArgs parsed) {
        return Path.of(parsed.value("conf-dir", "conf"));
    }

    /**
     * The DQL held in {@code file}, refusing any path inside the configuration directory.
     *
     * <p><b>Why this refusal exists.</b> Without it, {@code query -f} is a way to read a file aloud: it
     * printed what it read, and the configuration directory is where the session cache lives — a file this
     * tool may open and an agent may not. So {@code -f conf/read-scope-cli-session.properties} laundered a
     * live session token out through the one process permitted to read it, and then appended it to the run
     * log, which is neither mode 600 nor named in the credential guard. The deny rules were never crossed.
     * The token came out through the tool, which is the shape of hole a deny list cannot see.
     *
     * <p>Compared on REAL paths, so {@code ../conf/x}, a trailing {@code /.} and a symbolic link into the
     * directory all resolve to the same place before the comparison. A path that cannot be resolved — the
     * directory does not exist, or the file does not — falls back to absolute-and-normalised, which still
     * catches the spelling anyone would actually type.
     *
     * <p><b>What it does not do.</b> It does not make {@code -f} safe for arbitrary files. The content
     * still becomes a statement and still travels to the server, where a statement that does not parse is
     * logged — and {@code ClientQueryGuard}'s masking has the same limit this tool's does, covering quoted
     * strings and numbers but not a bare {@code key=value}. So {@code -f} on somebody else's secret can
     * still put it in the SERVER's log, which this refusal does not reach. It closes the one file this tool
     * itself guarantees is a credential; the rest remains the operator's own error.
     *
     * <p>Note also that the directory check can be stepped around with {@code --conf-dir}, which names a
     * different directory and so makes this comparison miss. That is why the echo was fixed as well rather
     * than instead: with {@code -f} printing the PATH, the content does not reach the terminal or the run
     * log however the directory is named. The two halves are independent on purpose.
     */
    static String readStatementFile(Path confDirectory, String file) throws IOException {
        Path statementPath = realOrAbsolute(Path.of(file));
        if (statementPath.startsWith(realOrAbsolute(confDirectory)))
            throw new IllegalArgumentException(
                "Refusing to read " + file + ": it is inside the configuration directory (" + confDirectory
                + "), which holds this tool's session cache. Give the statement with -c instead.");
        return Files.readString(statementPath).strip();
    }

    /**
     * A path in one canonical form, whether or not it exists: links resolved as far up as the filesystem
     * can resolve them, the rest normalised.
     *
     * <p><b>Falling back to plain {@code toAbsolutePath().normalize()} is not good enough, and the check
     * caught it.</b> The two paths being compared would then be in DIFFERENT forms whenever one exists and
     * the other does not — and on macOS that is routine, because {@code /tmp} and {@code /var} are links,
     * so a resolved path gains a {@code /private} prefix an unresolved one lacks. {@code startsWith} is
     * then false between a directory and a file inside it, which fails open: the refusal below would not
     * fire. Resolving the deepest ancestor that DOES exist and re-appending the rest puts both in the same
     * form in every case.
     */
    private static Path realOrAbsolute(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            Path absolute = path.toAbsolutePath().normalize();
            Path parent = absolute.getParent();
            Path name = absolute.getFileName();
            // At the filesystem root there is no ancestor left to resolve, and nothing more to normalise.
            return parent == null || name == null ? absolute : realOrAbsolute(parent).resolve(name);
        }
    }

    private static Path logDirectory(CliArgs parsed) {
        return Path.of(parsed.value("log-dir", "logs"));
    }

    private static long timeout(CliArgs parsed) {
        return parsed.intValue("timeout", DEFAULT_TIMEOUT_MILLIS);
    }

    static String help() {
        return """
            The KBS command-line client — asks a real caller's questions of a real server.

            It signs in as a caller, holds that session, and issues DQL over the bus through exactly the path a
            browser app uses: the same executeQuery endpoint, carrying the caller's own session, so every probe
            passes the client query guard, the read deny list, the read scope and the server-supplied caller
            parameters. A probe that bypassed those would test nothing, because they are the subject.

            Every probe it ships with today is about read scope; 'query' runs an arbitrary statement under the
            same controls.

            USAGE
              help                     this
              probes                   the probe catalogue, with each statement
              rehearsal                the rehearsal matrix and the order of runs

              signin   --server <url> --audience <front-office|back-office> [--username <u>] [--with-code]
                                       prompts for a password - or, with --with-code, asks the server to mail
                                       a verification code and waits for it to be typed here. Confirms the
                                       audience the server actually granted, and caches the session for 30
                                       minutes (mode 600, in conf/)
              status                   what is cached - never the token itself
              signout                  ends the session and deletes the cache

              probe <name> [--<param> <value>] [--caller <label>] [--as-baseline]
                           [--rows [--confirm-personal-data]] [--limit <n>] [--offset <n>]
              query  -c "<dql>" | -f <file> [--param <value>]... [--count]
                           [--limit <n>] [--offset <n>]
                           -f refuses any path inside the conf directory, and prints the path rather
                           than the file's contents: it must not become a way to read a file aloud.

            OPTIONS
              --server <url>      wss://host[:port] or http://localhost:8080. No default: a probe must say
                                  which server it asked.
              --audience <arm>    front-office or back-office. The arm is NOT cosmetic - it selects the scope
                                  token a read compiles under, so the two arms are different questions. The
                                  tool reads the audience back off the server's own token signature and
                                  refuses to probe if it is not the one asked for.
              --with-code         sign in with a mailed verification code instead of a password. This one
                                  command does both halves, because the server ties a code to the run that
                                  asked for it - a code requested by one command cannot be redeemed by
                                  another. The mailbox is NOT a second factor: an account that must present
                                  one gets a recovery-only session, whose audience this tool then refuses to
                                  probe the back office with.
              --caller <label>    which rehearsal identity this is, so the tool knows whether the event should
                                  be reachable. One of: %s
              --as-baseline       record this count as the UNSCOPED answer. Only true when run as a platform
                                  super administrator, whose scope has no predicate at all.
              --rows              show rows as well as the count. Paged by the SERVER (DQL limit/offset), so
                                  a row count is never a count - run the count form for that.
              --limit <n>         rows per page, default %d.
              --conf-dir <dir>    where the session cache lives, default conf/. scripts/kbs refuses
                                  it, so that every run shares one cache.
              --log-dir <dir>     where the run log and the baselines live, default logs/.
              --timeout <ms>      per bus call, default %d.

            EXIT CODES
              0  every applicable invariant held, and at least one was evaluated
              1  an invariant was VIOLATED
              2  the run could not be made (usage, no session, a refused query, the wrong audience)
              3  nothing was asserted (no baseline recorded, or a probe/statement with no invariant)

            LIMITATION, WRITTEN DOWN RATHER THAN WORKED AROUND
              v1 signs in with a username and password only. No Java client can answer a second factor, so
              this tool CANNOT be pointed at an account with a factor enrolled - it will fail at the login,
              not do something subtle. v2, a device-authorisation grant in which the operator approves the CLI
              from a browser (factor included) and the server mints a separate revocable session for it, is
              sketched in docs/security/read-scope-test-tool-design.md and is not built.

            BOUNDARIES
              Staging for agent runs; production probes are the maintainer's. A CLI holding a production
              back-office session is the same category as the production database. The tool runs anywhere; who
              runs it where is the rule.
            """.formatted(RehearsalMatrix.labels(), DEFAULT_LIMIT, DEFAULT_TIMEOUT_MILLIS);
    }
}
