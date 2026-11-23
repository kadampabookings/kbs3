package org.kadampabookings.kbs.cli.readscope;

import org.kadampabookings.kbs.cli.readscope.RehearsalMatrix.Caller;

/**
 * That the matrix declares reachability by GRANT, covers the three caller kinds precondition 3 names, and
 * answers "not declared" rather than "unreachable" for anything outside the grid.
 *
 * <h3>Why the last one matters most</h3>
 *
 * <p>{@code EMPTY_WHERE_NOT_GRANTED} asserts that a caller reads NOTHING of an event. If the matrix answered
 * "unreachable" for an event nobody measured — a typo in {@code --eventId}, an event added to staging since —
 * the tool would assert zero about a cell no human had looked at, and a pass would mean nothing. Null is the
 * answer that makes the invariant skip rather than invent.
 *
 * <h3>And why the all-zero row is the useful one</h3>
 *
 * <p>{@code event-1840} holds a grant for an event that is not in the grid, so every one of its cells is a
 * must-be-zero. That row is made entirely of the question the UI rehearsal cannot ask, which is the reason
 * this tool exists rather than a second sitting at the support inbox.
 *
 * <p><b>Probed by deletion:</b> removing the {@code EVENTS.containsKey} guard made the "not declared" checks
 * fail; removing the {@code unrestricted} short-circuit made the baseline caller unreachable everywhere. Both
 * were restored.
 *
 * @author Claude Code
 */
public class RehearsalMatrixCheck {

    static int pass, fail;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        System.out.println("The three caller kinds precondition 3 names are all present");
        check("an unrestricted caller — the one whose runs supply the baseline",
              RehearsalMatrix.CALLERS.stream().anyMatch(Caller::unrestricted));
        check("exactly one, so there is no ambiguity about which run is the baseline",
              RehearsalMatrix.CALLERS.stream().filter(Caller::unrestricted).count() == 1);
        check("at least two organisation-granted callers — one proves one arm",
              RehearsalMatrix.CALLERS.stream().filter(c -> c.grant().startsWith("organisation grant")).count() >= 2);
        check("at least two event-granted callers",
              RehearsalMatrix.CALLERS.stream().filter(c -> c.grant().startsWith("event grant")).count() >= 2);
        check("every caller names the grant that explains its row",
              RehearsalMatrix.CALLERS.stream().allMatch(c -> c.grant() != null && c.grant().length() > 10));
        check("every caller names a staging person, so the operator knows who to sign in as",
              RehearsalMatrix.CALLERS.stream().allMatch(c -> c.personId() > 0));
        check("labels are unique",
              RehearsalMatrix.CALLERS.stream().map(Caller::label).distinct().count()
              == RehearsalMatrix.CALLERS.size());

        System.out.println();
        System.out.println("The unrestricted caller reaches everything, by construction and not by listing");
        Caller unrestricted = RehearsalMatrix.caller("unrestricted");
        check("it resolves by label", unrestricted != null && unrestricted.unrestricted());
        for (Integer eventId : RehearsalMatrix.EVENTS.keySet())
            check("  reaches event " + eventId,
                  Boolean.TRUE.equals(RehearsalMatrix.reachable(unrestricted, eventId)));
        check("and reaches an event that is not even in the grid — it has no predicate at all",
              Boolean.TRUE.equals(RehearsalMatrix.reachable(unrestricted, 9999)));

        System.out.println();
        System.out.println("A granted caller reaches what it is granted, and nothing else in the grid");
        Caller org1 = RehearsalMatrix.caller("org-1");
        check("org-1 reaches 1857 (org 1)", Boolean.TRUE.equals(RehearsalMatrix.reachable(org1, 1857)));
        check("org-1 reaches 1898 (org 1)", Boolean.TRUE.equals(RehearsalMatrix.reachable(org1, 1898)));
        check("org-1 reaches 2021 (org 1)", Boolean.TRUE.equals(RehearsalMatrix.reachable(org1, 2021)));
        check("org-1 does NOT reach 1901 (org 187)", Boolean.FALSE.equals(RehearsalMatrix.reachable(org1, 1901)));
        check("org-1 does NOT reach 1882 (org 152)", Boolean.FALSE.equals(RehearsalMatrix.reachable(org1, 1882)));
        check("org-1 does NOT reach 1957 (org 151)", Boolean.FALSE.equals(RehearsalMatrix.reachable(org1, 1957)));
        check("org-1 does NOT reach 1883 (org 151)", Boolean.FALSE.equals(RehearsalMatrix.reachable(org1, 1883)));

        Caller event1857 = RehearsalMatrix.caller("event-1857");
        check("an EVENT grant does not widen to its organisation — 1857 yes, 1898 no, both org 1",
              Boolean.TRUE.equals(RehearsalMatrix.reachable(event1857, 1857))
              && Boolean.FALSE.equals(RehearsalMatrix.reachable(event1857, 1898)));

        System.out.println();
        System.out.println("The all-zero row — every cell is the question the UI cannot ask");
        Caller event1840 = RehearsalMatrix.caller("event-1840");
        check("event-1840 resolves", event1840 != null);
        for (Integer eventId : RehearsalMatrix.EVENTS.keySet())
            check("  must read nothing of event " + eventId,
                  Boolean.FALSE.equals(RehearsalMatrix.reachable(event1840, eventId)));

        System.out.println();
        System.out.println("Anything outside the grid is NOT DECLARED, never assumed unreachable");
        check("an event not in the grid answers null for a granted caller",
              RehearsalMatrix.reachable(org1, 9999) == null);
        check("and so does a typo'd event id",
              RehearsalMatrix.reachable(event1840, 18570) == null);
        check("no caller at all answers null", RehearsalMatrix.reachable(null, 1857) == null);
        check("an unknown label does not resolve to a caller",
              RehearsalMatrix.caller("no-such-caller") == null);

        System.out.println();
        System.out.println("The grid covers the events the runbook walks");
        for (int eventId : new int[] {1857, 1898, 1901, 1882, 1957, 2021, 1883})
            check("  event " + eventId + " is in the grid", RehearsalMatrix.EVENTS.containsKey(eventId));
        check("every event names its organisation, which is what explains the row",
              RehearsalMatrix.EVENTS.values().stream().allMatch(name -> name.contains("org ")));

        System.out.println();
        System.out.println("The plan tells an operator what to run, in order");
        String plan = RehearsalMatrix.plan();
        check("it says the matrix is about grants, not recorded counts", plan.contains("not recorded counts"));
        check("it names the script that re-derives it", plan.contains("predict-chat-read-scope.sql"));
        check("it starts with the baseline run", plan.contains("--as-baseline"));
        check("it names the member arm separately", plan.contains("my-conversations"));
        check("it names the unenforced root", plan.contains("unenforced-root"));
        check("every caller label appears in it",
              RehearsalMatrix.CALLERS.stream().allMatch(c -> plan.contains(c.label())));

        System.out.println();
        System.out.println("RehearsalMatrixCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
