package org.kadampabookings.kbs.cli.readscope;

import dev.webfx.stack.db.query.cli.Audience;
import dev.webfx.stack.db.query.cli.ClientPath;
import dev.webfx.stack.db.query.cli.DqlRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * That every probe asks a question this tool can actually ask, of the arm it belongs to.
 *
 * <h3>What is pinned, and the failure behind each</h3>
 *
 * <ul>
 *   <li><b>Every count probe is a count statement.</b> A probe whose statement selected rows would have its
 *       count refused at run time, in the middle of a rehearsal, rather than here.</li>
 *   <li><b>Every probe names its arm, or says it belongs to both.</b> A back-office probe asked as a member
 *       compiles under the member scope token and answers a different question; printing that under the
 *       back-office heading is the whole failure this tool guards against.</li>
 *   <li><b>No probe binds a {@code caller.*} parameter.</b> The server refuses one outright, so such a probe
 *       would come back "refused" and be read as a scope refusal.</li>
 *   <li><b>The three enforced roots are all covered.</b> {@code ENFORCED_ROOTS} holds {@code Conversation},
 *       {@code ChatMessage} and {@code ChatMessageReaction}; a flip of three roots rehearsed with two probes
 *       leaves one root untested, which is a smaller version of exactly the "testing with one account"
 *       finding that made this rehearsal necessary.</li>
 *   <li><b>{@code persons-visible} asserts nothing and is marked personal.</b> Not for want of something to
 *       satisfy — since {@code PersonReadScope} the member arm has a rule — but for want of anything to
 *       compare against: every front-office caller gets the MEMBER token, so no run in this probe's own
 *       audience can record the unscoped baseline {@code OWN_ROWS_ONLY} needs. See {@code Probes}.</li>
 * </ul>
 *
 * <p><b>Probed by deletion:</b> removing the comparison in {@code Probe.audienceRefusal}, and separately
 * changing one probe's statement to a row select, each made this check fail; both were restored.
 *
 * @author Claude Code
 */
public class ProbeCatalogueCheck {

    static int pass, fail;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        System.out.println("Every probe is askable");
        Set<String> names = new HashSet<>();
        DqlRunner runner = new DqlRunner("MDS", 1000);
        for (Probe probe : Probes.all()) {
            check(probe.name() + " has a unique name", names.add(probe.name()));
            check(probe.name() + " counts on the server ('select count(1) as n from ...')",
                  probe.dql().startsWith("select count(1) as n from "));
            check(probe.name() + " has a summary someone can read",
                  probe.summary() != null && probe.summary().length() > 20);
            check(probe.name() + " is a statement the client path accepts",
                  ClientPath.refusalFor(runner.argumentFor(probe.dql(), new Object[probe.parameterCount()])) == null);
            if (probe.hasRows())
                check(probe.name() + "'s row form selects rows, not a count",
                      !probe.rowsDql().contains("count(1)"));
            check(probe.name() + " is retrievable by name", Probes.byName(probe.name()) == probe);
        }
        check("an unknown name is not resolved", Probes.byName("no-such-probe") == null);

        System.out.println();
        System.out.println("Every probe belongs to an arm, and refuses the other one");
        check("conversations-for-event is back office",
              Probes.CONVERSATIONS_FOR_EVENT.audience() == Audience.BACK_OFFICE);
        check("and refuses a front-office session",
              Probes.CONVERSATIONS_FOR_EVENT.audienceRefusal(Audience.FRONT_OFFICE) != null);
        check("and accepts a back-office one",
              Probes.CONVERSATIONS_FOR_EVENT.audienceRefusal(Audience.BACK_OFFICE) == null);
        check("my-conversations is front office",
              Probes.MY_CONVERSATIONS.audience() == Audience.FRONT_OFFICE);
        check("and refuses a back-office session",
              Probes.MY_CONVERSATIONS.audienceRefusal(Audience.BACK_OFFICE) != null);
        check("persons-visible is front office — the member is the caller whose reach is the finding",
              Probes.PERSONS_VISIBLE.audience() == Audience.FRONT_OFFICE);
        check("unenforced-root belongs to BOTH arms, which is the assertion",
              Probes.UNENFORCED_ROOT.audience() == null
              && Probes.UNENFORCED_ROOT.audienceRefusal(Audience.FRONT_OFFICE) == null
              && Probes.UNENFORCED_ROOT.audienceRefusal(Audience.BACK_OFFICE) == null);
        check("an unconfirmed audience is refused by a probe that names an arm",
              Probes.MY_CONVERSATIONS.audienceRefusal(null) != null);

        System.out.println();
        System.out.println("The three enforced roots are all covered");
        check("Conversation", covers("Conversation"));
        check("ChatMessage", covers("ChatMessage"));
        check("ChatMessageReaction", covers("ChatMessageReaction"));
        check("and ChatPresence is probed as the UNENFORCED root, not as a fourth enforced one",
              Probes.UNENFORCED_ROOT.dql().contains("ChatPresence")
              && Probes.UNENFORCED_ROOT.invariants().contains(Invariant.UNCHANGED_FOR_EVERY_CALLER));

        System.out.println();
        System.out.println("Caller values are asked for in the statement, never bound");
        check("my-conversations writes CALLER_PERSON in the statement",
              Probes.MY_CONVERSATIONS.dql().contains("CALLER_PERSON"));
        check("and takes no parameters, so nothing can be bound in its place",
              Probes.MY_CONVERSATIONS.parameterCount() == 0);

        System.out.println();
        System.out.println("Event probes take exactly one event id");
        for (Probe probe : new Probe[] {Probes.CONVERSATIONS_FOR_EVENT, Probes.MESSAGES_FOR_EVENT,
                                        Probes.REACTIONS_FOR_EVENT}) {
            check(probe.name() + " takes one parameter, the event",
                  probe.parameterCount() == 1 && Probes.EVENT.equals(probe.parameters().get(0)));
            check(probe.name() + " binds it as $1",
                  probe.dql().contains("$1") && !probe.dql().contains("$2"));
            check(probe.name() + " asserts all three event relations",
                  probe.invariants().containsAll(List.of(Invariant.NEVER_WIDER,
                                                         Invariant.EMPTY_WHERE_NOT_GRANTED,
                                                         Invariant.EQUAL_WHERE_GRANTED)));
        }

        System.out.println();
        System.out.println("The measurement probe asserts nothing and says so");
        check("persons-visible has no invariant — no front-office run can record the baseline one needs",
              Probes.PERSONS_VISIBLE.invariants().isEmpty());
        check("and is marked as personal, so its rows need a second flag",
              Probes.PERSONS_VISIBLE.showsPersonalData());
        check("while the chat probes are not — their counts and ids are not personal data",
              !Probes.CONVERSATIONS_FOR_EVENT.showsPersonalData()
              && !Probes.MESSAGES_FOR_EVENT.showsPersonalData());
        check("and the message row form deliberately omits the message bodies",
              !Probes.MESSAGES_FOR_EVENT.rowsDql().contains("content"));

        System.out.println();
        System.out.println("A probe record with no invariant is a legal record — the arbitrary-statement shape");
        Probe arbitrary = new Probe("ad-hoc", "whatever was typed", null, false,
                                    "select count(1) as n from Event", List.of(), null, null, false);
        check("it constructs", arbitrary.invariants().isEmpty() && arbitrary.parameters().isEmpty());
        check("it belongs to no arm, so no arm refuses it",
              arbitrary.audienceRefusal(Audience.FRONT_OFFICE) == null);

        System.out.println();
        System.out.println("ProbeCatalogueCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }

    /**
     * Whether some probe with the event relations reads this entity as its ROOT.
     *
     * <p>{@code "from <Entity> "} exactly, because 'ChatMessage' is a prefix of 'ChatMessageReaction': a
     * substring test would let the reaction probe stand in for the message one and report a root as covered
     * when it is not.
     */
    static boolean covers(String entity) {
        for (Probe probe : Probes.all())
            if (probe.invariants().contains(Invariant.EQUAL_WHERE_GRANTED)
                && probe.dql().contains("from " + entity + " "))
                return true;
        return false;
    }
}
