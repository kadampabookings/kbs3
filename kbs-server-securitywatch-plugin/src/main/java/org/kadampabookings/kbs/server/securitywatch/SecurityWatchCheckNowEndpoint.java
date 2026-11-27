package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;

/**
 * "Check now": starts a run in the background and answers its id ({@code {"runId": 413}}); the page then polls
 * {@code status}. Refused with {@code SecurityWatchDisabledError} while the check is switched off on this server,
 * {@code SecurityWatchBusyError} while a run is running, and {@code SecurityWatchTooSoonError} less than ten minutes
 * after the last run started.
 *
 * <p>The argument is ignored. Platform super administrators on a back-office session that presented a second factor
 * only, re-checked on every call ({@link SecurityWatchAccess}); the person id is recorded on the run.
 *
 * @author Claude Code
 */
public final class SecurityWatchCheckNowEndpoint extends AsyncFunctionBusCallEndpoint<Object, String> {

    /** The bus address — mirrored in the back office's feature-local address constants. */
    public static final String ADDRESS = "kbs/service/securitywatch/checkNow";

    public SecurityWatchCheckNowEndpoint() {
        super(ADDRESS, argument -> SecurityWatchAccess.whenPlatformSuperAdminWithFactor("checkNow",
            caller -> SecurityWatchRunner.startManual(caller.personId()).map(SecurityWatchReplies::checkNow)));
        SecurityWatchModule.start();
    }
}
