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
        System.out.println("Printing ROWS anonymously needs saying so twice");
        // Why only the anonymous case: `probe` gates rows on a catalogued probe declaring
        // showsPersonalData, and free-form DQL has nothing to declare, so a signed-in `query` still has no
        // gate. What --anonymous changed is what the read MEANS -- no caller to attribute it to -- and the
        // arithmetic of what comes back. Data a signed-out caller can reach is by definition exposed, so
        // these rows are the likeliest of any to hold somebody's details. That is not hypothetical: the
        // Document exposure surfaced from an ad-hoc anonymous query that projected three person_ columns
        // and printed a booker's details with nothing in the way.
        check("rows from an anonymous run are refused without the second flag",
              KbsCli.anonymousRowsRefusal(true, false, false) != null);
        check("and the refusal names the flag that allows them",
              KbsCli.anonymousRowsRefusal(true, false, false).contains("--confirm-personal-data"));
        check("and offers --count as the way to answer without them",
              KbsCli.anonymousRowsRefusal(true, false, false).contains("--count"));
        check("and says it is a speed bump, not a prohibition",
              KbsCli.anonymousRowsRefusal(true, false, false).contains("speed bump"));
        check("--count needs no such flag: a count projects nothing",
              KbsCli.anonymousRowsRefusal(true, true, false) == null);
        check("confirming lets the rows through",
              KbsCli.anonymousRowsRefusal(true, false, true) == null);
        check("a SIGNED-IN query is untouched — it always printed rows and still does",
              KbsCli.anonymousRowsRefusal(false, false, false) == null);
        check("including when it asks for a count", KbsCli.anonymousRowsRefusal(false, true, false) == null);
        // Deliberately NOT a check on the columns selected. A list of person_/email/passport is wrong the
        // first time somebody adds to the schema, and a gate with holes is worse than an honest bump.
        check("the gate does not depend on what the statement selects, so it cannot be outgrown",
              KbsCli.anonymousRowsRefusal(true, false, false)
                  .equals(KbsCli.anonymousRowsRefusal(true, false, false)));
        check("the help says rows need the second flag",
              KbsCli.help().contains("--confirm-personal-data"));

        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
