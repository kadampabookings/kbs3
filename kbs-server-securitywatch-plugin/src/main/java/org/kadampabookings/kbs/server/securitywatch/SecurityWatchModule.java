package org.kadampabookings.kbs.server.securitywatch;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The module's start, once, whichever of its parts boots first (the job, or an endpoint): listen for the
 * configuration, and install the real check into the runner.
 *
 * @author Claude Code
 */
final class SecurityWatchModule {

    private SecurityWatchModule() {
    }

    private static final AtomicBoolean STARTED = new AtomicBoolean();

    static void start() {
        SecurityWatchConfig.listen();
        if (STARTED.compareAndSet(false, true))
            SecurityWatchRunner.setCheck(new SecurityWatchCheck());
    }
}
