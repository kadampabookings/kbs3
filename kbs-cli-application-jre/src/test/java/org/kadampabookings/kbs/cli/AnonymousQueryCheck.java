package org.kadampabookings.kbs.cli;

/**
 * That {@code query --anonymous} is a run with no session, and says so honestly: it needs {@code --server}, and it
 * refuses {@code --audience}, which a signed-out caller cannot carry.
 *
 * <p>Why the refusal matters: an anonymous caller has no token for the server to sign an arm into, and this
 * executable claims neither arm ({@code Meta.getBackoffice()} is null for it). Accepting {@code --audience
 * back-office} would let a run report that it tested the back office when it tested nobody's arm — the
 * failure this tool was commissioned after.
 *
 * <p>The run itself (no cache opened, nothing restored) is exercised against a real server, not here.
 *
 * @author Claude Code
 */
public final class AnonymousQueryCheck {

    private static int pass, fail;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (ok) pass++; else fail++;
    }

    public static void main(String[] args) {
        System.out.println("AnonymousQueryCheck");

        check("a run that is not anonymous is not this rule's business",
              KbsCli.anonymousRefusal(false, null, null) == null
              && KbsCli.anonymousRefusal(false, null, "back-office") == null);

        String noServer = KbsCli.anonymousRefusal(true, null, null);
        check("anonymous without --server is refused, naming the option",
              noServer != null && noServer.contains("--server"));

        String withAudience = KbsCli.anonymousRefusal(true, "wss://example.org", "back-office");
        check("anonymous with --audience is refused, naming the option",
              withAudience != null && withAudience.contains("--audience"));
        check("whichever arm is asked for",
              KbsCli.anonymousRefusal(true, "wss://example.org", "front-office") != null);

        check("anonymous with a server and no audience is a run that can be made",
              KbsCli.anonymousRefusal(true, "wss://example.org", null) == null);

        check("the run log and the screen call the caller 'anonymous'", "anonymous".equals(KbsCli.ANONYMOUS_CALLER));

        check("the help says what --anonymous is for and what it needs",
              KbsCli.help().contains("--anonymous") && KbsCli.help().contains("signed-out visitor"));

        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
