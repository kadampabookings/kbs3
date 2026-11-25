package org.kadampabookings.kbs.cli;

import dev.webfx.stack.db.query.cli.SessionCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * That {@code query -f} cannot be pointed at this tool's own session cache.
 *
 * <h3>What this is protecting</h3>
 *
 * <p>{@code -f} reads a file and runs its content as DQL, and the command printed what it read and appended
 * it to the run log. The configuration directory holds {@link SessionCache}'s file — a live session token,
 * mode 600, named in the repository's deny rules and in the credential guard so that an agent may USE the
 * session and may not READ it. {@code -f conf/read-scope-cli-session.properties} walked straight through
 * that split: the CLI is permitted to open its own cache, so nothing was violated on the way, and the token
 * arrived on the terminal and in a log file that has neither the mode nor the guard entry.
 *
 * <p>Found by David's review of the device-login design note (2026-11-25), in code that had already shipped.
 * The lesson worth keeping is the shape rather than the instance: a deny list stops a reader reaching a
 * file, and says nothing about a permitted reader being asked to read it aloud.
 *
 * <p>The spellings below are not decoration. A refusal that compared the strings as typed would pass the
 * first case and fail every other one, and {@code ../conf/} is the spelling anybody would reach for first.
 *
 * @author Claude Code
 */
public final class StatementFileCheck {

    private static int pass, fail;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (ok) pass++; else fail++;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    public static void main(String[] args) throws IOException {
        Path root = Files.createTempDirectory("kbs-cli-statement-file-check");
        Path conf = Files.createDirectories(root.resolve("conf"));
        Path cache = conf.resolve(SessionCache.FILE_NAME);
        Files.writeString(cache, "token=not-a-real-token\nusername=someone\n");
        Path probe = root.resolve("probe.dql");
        Files.writeString(probe, "  select count(1) from Person  ");

        section("The session cache is refused, however it is spelled");
        check("by its ordinary path",
              refused(conf, cache.toString()));
        check("through a parent hop (../conf/…), which is what anybody would type from a sibling directory",
              refused(conf, root.resolve("logs").resolve("..").resolve("conf")
                               .resolve(SessionCache.FILE_NAME).toString()));
        check("through a trailing /. on the directory",
              refused(conf, conf.resolve(".").resolve(SessionCache.FILE_NAME).toString()));

        section("And so is anything else in the configuration directory");
        Path neighbour = conf.resolve("something-else.dql");
        Files.writeString(neighbour, "select count(1) from Person");
        check("a perfectly innocent file living there is refused too — the rule is the directory, not the"
              + " file name, so a cache renamed tomorrow is still covered",
              refused(conf, neighbour.toString()));

        section("A symbolic link does not get round it");
        Path link = root.resolve("innocent.dql");
        boolean linksSupported = true;
        try {
            Files.createSymbolicLink(link, cache);
        } catch (IOException | UnsupportedOperationException e) {
            linksSupported = false; // a filesystem without links cannot have this hole either
        }
        check(linksSupported ? "a link from outside pointing into conf/ is resolved and refused"
                             : "(skipped: this filesystem does not support symbolic links)",
              !linksSupported || refused(conf, link.toString()));

        section("Ordinary use is untouched");
        check("a statement file outside conf/ is read",
              "select count(1) from Person".equals(KbsCli.readStatementFile(conf, probe.toString())));
        check("and it is stripped, as before",
              !KbsCli.readStatementFile(conf, probe.toString()).startsWith(" "));

        section("The refusal says what to do instead");
        String message = refusalMessage(conf, cache.toString());
        check("it names the file the operator gave", message.contains(SessionCache.FILE_NAME));
        check("it says why — the configuration directory", message.contains("configuration directory"));
        check("it points at -c", message.contains("-c"));
        check("and it does NOT quote the file's contents, which is the whole point",
              !message.contains("not-a-real-token"));

        System.out.println();
        System.out.println("StatementFileCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }

    private static boolean refused(Path confDirectory, String file) {
        return refusalMessage(confDirectory, file) != null;
    }

    /** The refusal, or null when the file was read — never the content, which a failure must not print. */
    private static String refusalMessage(Path confDirectory, String file) {
        try {
            KbsCli.readStatementFile(confDirectory, file);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (IOException e) {
            return null; // an unreadable file is a different failure, and not the one under test
        }
    }
}
