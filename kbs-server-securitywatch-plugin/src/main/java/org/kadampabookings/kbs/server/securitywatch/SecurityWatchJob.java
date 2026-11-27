package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;

/**
 * The hourly tick: about {@value #FIRST_DELAY_MILLIS} ms after boot (once the boot migrations have run), then every
 * hour, {@link SecurityWatchRunner#startScheduled()} — which starts a run only when the contract's schedule says so
 * (no run succeeded since the last 05:00 UTC, none started in the last 55 minutes), whatever the number of server tasks
 * ticking.
 *
 * <p>It runs in every environment, development included: the switch ({@code enabled}) decides, not the build — off,
 * the tick does nothing at all.
 *
 * @author Claude Code
 */
public final class SecurityWatchJob implements ApplicationJob {

    static final long FIRST_DELAY_MILLIS = 2 * 60 * 1000;
    static final long PERIOD_MILLIS = 60 * 60 * 1000;

    private Scheduled first, periodic;

    @Override
    public void onStart() {
        SecurityWatchModule.start();
        first = Scheduler.scheduleDelay(FIRST_DELAY_MILLIS, () -> {
            tick();
            periodic = Scheduler.schedulePeriodic(PERIOD_MILLIS, SecurityWatchJob::tick);
        });
    }

    @Override
    public void onStop() {
        if (first != null)
            first.cancel();
        if (periodic != null)
            periodic.cancel();
    }

    private static void tick() {
        try {
            SecurityWatchRunner.startScheduled();
        } catch (RuntimeException e) {
            Console.log(SecurityWatchLog.PREFIX + "⚠️ The hourly tick failed (" + e.getClass().getSimpleName() + ")");
        }
    }
}
