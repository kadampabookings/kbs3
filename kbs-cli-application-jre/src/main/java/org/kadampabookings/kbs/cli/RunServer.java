package org.kadampabookings.kbs.cli;

import dev.webfx.platform.boot.ApplicationBooter;
import dev.webfx.stack.db.query.cli.ServerEndpoint;
import dev.webfx.stack.db.query.cli.SessionCache;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Which server this run is talking to — answerable from the moment the process starts, because nothing later
 * is guaranteed to still be there when the question is asked.
 *
 * <h3>Why this exists at all</h3>
 *
 * <p>The bus's server comes from the configuration, and the configuration substitutes its variables
 * <em>on read</em> ({@code ConfigImpl} calls {@code Substitutor.substitute} per value) rather than once at a
 * boot level. The read happens when the bus is created: at {@code COMMUNICATION_REGISTER_BOOT_LEVEL} (5) in an
 * ordinary WebFX client, where {@code BusCallModuleBooter} touches {@code BusService.bus()} during the boot;
 * and in a command-line program, where that registration is deferred, inside the command itself.
 *
 * <p>So this must be answerable early and must not change afterwards, and the only things available before a
 * command exists are the main arguments ({@code ApplicationBooter.getMainArgs()}, set before {@code boot()})
 * and the filesystem. Both are read here — see {@link #endpoint()} for why the answer is then memoised.
 *
 * <h3>The two sources, and why they are one concept</h3>
 *
 * <p>A run names its server either on the command line ({@code signin --server …}) or by having signed in
 * earlier, in which case the cached session records it. Both are the same question — "which server is this
 * run about" — and both are read here so that {@link KbsCliVariablesResolver} and the commands cannot disagree
 * about the answer. A command still cross-checks what it was given against the cache; see
 * {@code KbsCli.requireAudience}.
 *
 * <p>Reading the session cache from a variables resolver looks like a layering surprise, and it is deliberate:
 * this is the executable, which is where the bus, the arguments and the session are integrated, and the
 * alternative is to demand {@code --server} on every invocation for no benefit — the session cache exists
 * precisely so an operator does not have to repeat it. Nothing secret is read for this: only the
 * {@code server} field.
 *
 * @author Claude Code
 */
public final class RunServer {

    private RunServer() {}

    /**
     * The endpoint this run is about, or null when neither the arguments nor the cache names one.
     *
     * <p><b>Memoised, and that is not an optimisation.</b> The configuration substitutes its variables on
     * every READ ({@code ConfigImpl} calls {@code Substitutor.substitute} per value), not once at a boot
     * level — so this is asked whenever something first reads the bus's server, which is inside
     * {@code ClientRuntime.connect()}. {@code signout} deletes the session cache before it connects, on
     * purpose, and without this memo the answer changed between the start of the command and that read: the
     * configuration fell back to {@code localhost:80} and the command refused itself on the mismatch, so the
     * server-side session was never ended. A one-shot process is about exactly one server; answering
     * differently depending on WHEN it is asked is the defect.
     */
    public static ServerEndpoint endpoint() {
        if (memoisedEndpoint == null) {
            ServerEndpoint fromArguments = fromArguments();
            memoisedEndpoint = fromArguments != null ? fromArguments : fromCachedSession();
        }
        return memoisedEndpoint;
    }

    private static ServerEndpoint memoisedEndpoint;

    /**
     * Forgets the memo, so that a check can set different arguments and ask again. Nothing in a run calls it:
     * a one-shot process resolves its server once and keeps it, which is the whole point of the memo.
     */
    static void forgetMemoisedEndpoint() {
        memoisedEndpoint = null;
    }

    /**
     * {@code --server} as given on the command line, in either {@code --server v} or {@code --server=v} form.
     *
     * <p>Scanned rather than parsed with {@code CliArgs}, because this runs before a command has been chosen
     * and so before there is a spec to parse against. A malformed value answers null and the configuration
     * check downstream reports it; this is not the place to refuse, because refusing here would abort a boot.
     */
    static ServerEndpoint fromArguments() {
        String[] arguments = ApplicationBooter.getMainArgs();
        if (arguments == null)
            return null;
        for (int i = 0; i < arguments.length; i++) {
            String argument = arguments[i];
            if (argument == null)
                continue;
            if (argument.startsWith("--server="))
                return ServerEndpoint.parse(argument.substring("--server=".length()));
            if ("--server".equals(argument) && i + 1 < arguments.length)
                return ServerEndpoint.parse(arguments[i + 1]);
        }
        return null;
    }

    /** The server the cached session was established against, or null when there is no usable cache. */
    static ServerEndpoint fromCachedSession() {
        try {
            SessionCache.Entry entry = new SessionCache(confDirectory()).load(System.currentTimeMillis());
            return entry == null ? null : ServerEndpoint.parse(entry.server());
        } catch (IOException | RuntimeException e) {
            // An unreadable or over-permissive cache is the commands' problem to report, with a message that
            // says what to do about it. Here it only means "no server from this source": throwing would abort
            // the boot, from a variables resolver, with a stack trace about a credential file.
            return null;
        }
    }

    /**
     * Where the session cache lives, honouring {@code --conf-dir} if it was given.
     *
     * <p>Duplicates what {@code KbsCli} does with the parsed arguments, for the same reason
     * {@link #fromArguments} scans: there is no command yet. The launcher refuses {@code --conf-dir} anyway.
     */
    static Path confDirectory() {
        String[] arguments = ApplicationBooter.getMainArgs();
        if (arguments != null)
            for (int i = 0; i < arguments.length; i++) {
                String argument = arguments[i];
                if (argument == null)
                    continue;
                if (argument.startsWith("--conf-dir="))
                    return Path.of(argument.substring("--conf-dir=".length()));
                if ("--conf-dir".equals(argument) && i + 1 < arguments.length)
                    return Path.of(arguments[i + 1]);
            }
        return Path.of("conf");
    }
}
