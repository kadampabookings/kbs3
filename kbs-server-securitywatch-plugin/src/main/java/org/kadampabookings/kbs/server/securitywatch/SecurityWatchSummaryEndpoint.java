package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

import java.time.Instant;

/**
 * The home screen's count: for each action of the latest succeeded report, its severity, due date and whether it is
 * known to be exploited — nothing that says what is affected. Extracted by the database, so the report itself never
 * leaves it on this path.
 *
 * <p>The argument is ignored. Platform super administrators on a back-office session, re-checked on every call —
 * WITHOUT the second-factor check ({@link SecurityWatchAccess#whenPlatformSuperAdmin}), so the count always shows.
 *
 * @author Claude Code
 */
public final class SecurityWatchSummaryEndpoint extends AsyncFunctionBusCallEndpoint<Object, String> {

    /** The bus address — mirrored in the back office's feature-local address constants. */
    public static final String ADDRESS = "kbs/service/securitywatch/summary";

    public SecurityWatchSummaryEndpoint() {
        super(ADDRESS, argument -> SecurityWatchAccess.whenPlatformSuperAdmin("summary", caller -> {
            boolean enabled = SecurityWatchConfig.isEnabled();
            return SecurityWatchStore.summary()
                .map(summary -> SecurityWatchReplies.summary(enabled, Instant.now(), summary));
        }));
        SecurityWatchModule.start();
    }
}
