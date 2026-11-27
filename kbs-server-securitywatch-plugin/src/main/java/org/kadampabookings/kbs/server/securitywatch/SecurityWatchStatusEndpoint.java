package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

import java.time.Instant;

/**
 * The security watch page's main read: whether the check is switched on here, the server's clock, the latest run, the
 * report of the latest SUCCEEDED run (so a failed run never blanks the page), and the runtime facts read for this
 * reply. A JSON string, as the contract's section 3 describes it.
 *
 * <p>The argument is ignored ({@code []} or null). Platform super administrators on a back-office session that
 * presented a second factor only, re-checked on every call ({@link SecurityWatchAccess}).
 *
 * @author Claude Code
 */
public final class SecurityWatchStatusEndpoint extends AsyncFunctionBusCallEndpoint<Object, String> {

    /** The bus address — mirrored in the back office's feature-local address constants. */
    public static final String ADDRESS = "kbs/service/securitywatch/status";

    public SecurityWatchStatusEndpoint() {
        super(ADDRESS, argument -> SecurityWatchAccess.whenPlatformSuperAdminWithFactor("status", caller -> reply()));
        SecurityWatchModule.start();
    }

    private static Future<String> reply() {
        boolean enabled = SecurityWatchConfig.isEnabled();
        Future<SecurityWatchStore.Status> stored = SecurityWatchStore.status();
        Future<RuntimeFacts.Values> runtime = RuntimeFacts.read();
        return Future.all(stored, runtime).map(both -> SecurityWatchReplies.status(enabled, Instant.now(),
            both.resultAt(0), both.resultAt(1)));
    }
}
