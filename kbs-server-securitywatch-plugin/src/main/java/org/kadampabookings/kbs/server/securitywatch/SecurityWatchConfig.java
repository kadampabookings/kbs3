package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Strings;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whether the security watch runs on this server, and where it reads the deployment's inventory from.
 *
 * <p><b>The switch fails closed.</b> The check calls public services from the process that serves users, so it runs
 * only once {@code enabled} reads {@code true} (in any letter case). Every other state means OFF, and says so at boot:
 * the value unset or anything else, the configuration not loaded yet (a call that arrives before the callback has
 * run), the load failing, and the section missing from the server's aggregated {@code src-root.json} altogether. That
 * last one is the likeliest — a {@code declare@} file is read only once {@code webfx update} has merged it into the
 * application — so it is named in the boot line rather than left to look like a deliberate "off". OFF, the server
 * never calls out.
 *
 * <p>Declared in {@code declare@kbs.server.securitywatch.properties}; that file says how an environment turns it on,
 * and why a line in a variables file does not.
 *
 * @author Claude Code
 */
final class SecurityWatchConfig {

    private SecurityWatchConfig() {
    }

    /** Where the settings live; matches the module's declare@ file and the server's src-root.json. */
    static final String CONFIG_PATH = "kbs.server.securitywatch";

    /** Where the inventory is read from when the setting is absent or empty. */
    static final String DEFAULT_DIRECTORY = "/opt/kbs/security-watch";

    // Written once by the config-loaded callback, read by every call: volatile is the contract. Off until then,
    // which is the fail-closed reading of "not known yet".
    private static volatile boolean enabled;
    private static volatile String directory = DEFAULT_DIRECTORY;

    private static final AtomicBoolean LISTENING = new AtomicBoolean();

    /**
     * Starts listening for the configuration, once, however many endpoints ask. Called from the endpoints'
     * constructors, which run at boot (every endpoint is instantiated then), so the boot log says which state this
     * server is in before anybody opens the page.
     */
    static void listen() {
        if (!LISTENING.compareAndSet(false, true))
            return;
        ConfigLoader.onConfigLoaded(CONFIG_PATH, SecurityWatchConfig::onConfigLoaded);
    }

    private static void onConfigLoaded(Config config) {
        if (config == null) {
            enabled = false;
            Console.log(SecurityWatchLog.PREFIX + "⚠️ " + CONFIG_PATH + " is missing from the server's configuration"
                        + " (src-root.json was not regenerated with `webfx update`?) — the security watch is OFF on this server");
            return;
        }
        // Read as what it is, a ReadOnlyAstObject: naming the type here is what makes `webfx update` keep
        // webfx-platform-ast a compile dependency of this module (it detects imports only)
        ReadOnlyAstObject settings = config;
        boolean on = "true".equalsIgnoreCase(read(settings, "enabled"));
        String dir = read(settings, "directory");
        directory = Strings.isEmpty(dir) ? DEFAULT_DIRECTORY : dir;
        enabled = on;
        Console.log(SecurityWatchLog.PREFIX + (on
            ? "The security watch is ENABLED on this server: a daily check of public sources, inventory read from " + directory
            : "The security watch is OFF on this server (enabled is not true): it never calls out; inventory directory would be " + directory));
    }

    /** A setting as trimmed text; a value that is not a scalar reads as absent, like any other unexpected value. */
    private static String read(ReadOnlyAstObject settings, String key) {
        try {
            return Strings.trim(settings.getString(key));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** True only once the configuration has loaded and said {@code true} (in any letter case). */
    static boolean isEnabled() {
        return enabled;
    }

    /** The directory holding the inventory and the front-end lockfile; never null. */
    static String directory() {
        return directory;
    }
}
