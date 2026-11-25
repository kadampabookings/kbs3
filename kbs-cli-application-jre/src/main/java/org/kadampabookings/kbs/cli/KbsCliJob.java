package org.kadampabookings.kbs.cli;

import dev.webfx.platform.boot.ApplicationBooter;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.stack.db.query.cli.CallerSession;

/**
 * Runs the command, and is the whole of this executable's entry point.
 *
 * <h3>Why there is no {@code main}</h3>
 *
 * <p>Because the platform has one. {@code ApplicationBooter} is the {@code Main-Class}, as it is for every
 * WebFX executable, and the arguments reach the application through {@code ApplicationBooter.getMainArgs()}.
 * The two things that once seemed to require a bespoke {@code main} are both handled by the platform:
 * the server reaches the bus through {@link KbsCliVariablesResolver} at configuration time, and a kit with no
 * application to launch no longer aborts the boot (that was fixed in {@code WebFxKitLauncherProvider} rather
 * than worked around here).
 *
 * <h3>Why it exits</h3>
 *
 * <p>{@code JreSchedulerProvider} holds a thread pool whose threads are non-daemon, so a boot never returns on
 * its own: a one-shot command has to end the process itself, whoever started it. The exit code is the
 * command's — see {@link KbsCli} for what each one means — and it is the one thing about this tool that
 * something unattended reads.
 *
 * <p>One consequence, accepted: exiting from {@code onStart} leaves {@code onStop} uncalled, because the
 * shutdown hook is registered after module booting returns. Nothing here needs unwinding — the tool holds a
 * socket and a read-only session, and the session outlives the process by design.
 *
 * @author Claude Code
 */
public final class KbsCliJob implements ApplicationJob {

    /** What this program calls itself on the server's monitoring pages. See {@link CallerSession#setClientName}. */
    private static final String CLIENT_NAME = "KBS CLI";

    @Override
    public void onStart() {
        // Named before any command runs, because the name rides in the state of the first message sent. The
        // generic module defaults to "CLI", which already tells a session apart from a browser; this says WHICH.
        CallerSession.setClientName(CLIENT_NAME);
        System.exit(KbsCli.run(ApplicationBooter.getMainArgs()));
    }
}
