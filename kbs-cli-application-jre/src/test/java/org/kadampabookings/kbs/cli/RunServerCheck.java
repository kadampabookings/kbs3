package org.kadampabookings.kbs.cli;

import dev.webfx.platform.boot.ApplicationBooter;
import dev.webfx.platform.boot.spi.ApplicationBooterProvider;
import dev.webfx.stack.db.query.cli.Audience;
import dev.webfx.stack.db.query.cli.ServerEndpoint;
import dev.webfx.stack.db.query.cli.SessionCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Which server a run talks to, and that answering it can never abort a boot.
 *
 * <h3>Why this is checked offline rather than by signing in</h3>
 *
 * <p>The answer is needed at {@code CONF_BOOT_LEVEL}, before a command exists, so it is read by scanning the
 * main arguments and — when they name no server — the cached session. The second path is the one no
 * end-to-end run can demonstrate without a credential: a probe reuses the server it signed in against, and
 * nothing in this session can sign in. So it is pinned here instead, with a cache written to a temporary
 * directory.
 *
 * <h3>And the property that matters more than the answer</h3>
 *
 * <p>Neither the lookup nor the resolver may THROW. Both run inside the boot — the resolver during
 * configuration substitution — and a throw there aborts the boot before any command can report anything, which
 * is exactly how the kit launcher's refusal killed every command that needed a server. So a malformed
 * {@code --server}, an unreadable cache and an absent cache all have to answer "nothing", and let the
 * configuration check downstream say so at a moment a command can be refused.
 *
 * <p><b>Probed by deletion:</b> removing the cache fallback made the cached-session cases fail; removing the
 * try/catch in {@code fromCachedSession} turned the over-permissive-cache case into a thrown
 * {@code IOException} rather than a null. Both were restored.
 *
 * @author Claude Code
 */
public class RunServerCheck {

    static int pass, fail;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    /**
     * Sets the main arguments without booting.
     *
     * <p>{@code ApplicationBooter.start(provider, args)} records the arguments and then calls
     * {@code provider.boot()}, so a provider whose {@code boot()} does nothing gives the arguments to
     * {@code getMainArgs()} with no platform behind them — which is all these cases need.
     */
    static void args(String... arguments) {
        ApplicationBooter.start(new ApplicationBooterProvider() {
            @Override
            public void boot() {
            }
        }, arguments);
    }

    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("run-server-check");
        directory.toFile().deleteOnExit();
        String conf = directory.toString();

        System.out.println("The command line names the server");
        args("signin", "--server", "wss://backoffice.example.org", "--audience", "back-office");
        check("a spaced --server is read, with the scheme's port",
              new ServerEndpoint("backoffice.example.org", 443, true).equals(RunServer.endpoint()));
        args("signin", "--server=ws://localhost:8080");
        check("an =-joined --server is read too",
              new ServerEndpoint("localhost", 8080, false).equals(RunServer.endpoint()));
        args("signin", "--server");
        check("a dangling --server answers nothing rather than throwing", RunServer.endpoint() == null);
        args("signin", "--server", "not-a-url");
        check("a malformed --server answers nothing rather than throwing", RunServer.endpoint() == null);
        args("help");
        check("a command that names no server answers nothing", RunServer.endpoint() == null);
        args();
        check("no arguments at all answers nothing", RunServer.endpoint() == null);

        System.out.println();
        System.out.println("Otherwise the cached session's server — the path no sign-in-less run can show");
        SessionCache cache = new SessionCache(directory);
        cache.store(new SessionCache.Entry("a-token-shaped-string", "operator", Audience.BACK_OFFICE,
                                           "wss://cached.example.org:443", System.currentTimeMillis()));
        args("probe", "conversations-for-event", "--eventId", "1857", "--conf-dir", conf);
        check("a probe with no --server takes the server it signed in against",
              new ServerEndpoint("cached.example.org", 443, true).equals(RunServer.endpoint()));
        args("probe", "conversations-for-event", "--conf-dir=" + conf);
        check("an =-joined --conf-dir is honoured as well",
              new ServerEndpoint("cached.example.org", 443, true).equals(RunServer.endpoint()));
        args("probe", "--server", "wss://explicit.example.org", "--conf-dir", conf);
        check("an explicit --server WINS over the cache, so a command can be refused on the mismatch",
              new ServerEndpoint("explicit.example.org", 443, true).equals(RunServer.endpoint()));

        System.out.println();
        System.out.println("A cache that cannot be used answers nothing, and never throws");
        SessionCacheCheckSupport.setMode(cache.file(), "rw-r--r--");
        args("probe", "--conf-dir", conf);
        check("an over-permissive cache answers nothing rather than propagating its IOException",
              RunServer.endpoint() == null);
        SessionCacheCheckSupport.setMode(cache.file(), "rw-------");
        Files.deleteIfExists(cache.file());
        args("probe", "--conf-dir", conf);
        check("an absent cache answers nothing", RunServer.endpoint() == null);
        args("probe", "--conf-dir", "/no/such/directory/at/all");
        check("a conf directory that does not exist answers nothing", RunServer.endpoint() == null);

        System.out.println();
        System.out.println("The resolver answers three names and nothing else");
        args("signin", "--server", "wss://backoffice.example.org");
        KbsCliVariablesResolver resolver = new KbsCliVariablesResolver();
        check("the host", Optional.of("backoffice.example.org").equals(resolver.resolveVariable(ServerEndpoint.HOST_VARIABLE)));
        check("the port", Optional.of("443").equals(resolver.resolveVariable(ServerEndpoint.PORT_VARIABLE)));
        check("whether it is SSL", Optional.of("true").equals(resolver.resolveVariable(ServerEndpoint.SSL_VARIABLE)));
        check("and NOTHING else, so it cannot shadow another variable this build resolves",
              resolver.resolveVariable("MODALITY_DATABASE_PASSWORD").isEmpty()
              && resolver.resolveVariable("BUS_PREFIX").isEmpty()
              && resolver.resolveVariable("SESSION_TOKEN_SIGNING_KEY").isEmpty());
        args("help");
        check("with no server to report it answers nothing, letting the configuration fall back and the "
              + "run be refused on the mismatch",
              resolver.resolveVariable(ServerEndpoint.HOST_VARIABLE).isEmpty());

        System.out.println();
        System.out.println("RunServerCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
