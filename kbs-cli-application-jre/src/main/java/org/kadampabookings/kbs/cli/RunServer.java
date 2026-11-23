package org.kadampabookings.kbs.cli;

import dev.webfx.platform.boot.ApplicationBooter;
import dev.webfx.stack.db.query.cli.ServerEndpoint;
import dev.webfx.stack.db.query.cli.SessionCache;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Which server this run is talking to — answered before the platform boots, because the bus connects during it.
 *
 * <h3>Why this exists at all</h3>
 *
 * <p>{@code BusCallModuleBooter} runs at {@code COMMUNICATION_REGISTER_BOOT_LEVEL} (5) and touches
 * {@code BusService.bus()}, so the websocket bus connects <em>during</em> the boot — four levels before any
 * application job starts. The server therefore has to be known by {@code CONF_BOOT_LEVEL} (3), when the
 * configuration is substituted, and the only things available that early are the main arguments
 * ({@code ApplicationBooter.getMainArgs()}, set before {@code boot()}) and the filesystem.
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

    /** The endpoint this run is about, or null when neither the arguments nor the cache names one. */
    public static ServerEndpoint endpoint() {
        ServerEndpoint fromArguments = fromArguments();
        return fromArguments != null ? fromArguments : fromCachedSession();
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
