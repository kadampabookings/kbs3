package org.kadampabookings.kbs.cli;

import one.modality.crm.shared.services.authn.ModalityUserPrincipal;
import org.kadampabookings.kbs.cli.readscope.RehearsalMatrix;

/**
 * That a run cannot claim to be a rehearsal identity it is not.
 *
 * <h3>Why this is worth its own check</h3>
 *
 * <p>{@code --caller} decides which relation applies to a cell, so the wrong label makes the matrix answer
 * about somebody else's grants and report a pass. Three of the rehearsal's five staging accounts are shared
 * by other persons - one by nine - so a login address does not establish which person a session belongs to.
 * On the {@code event-1840} row, every cell of which must be zero, a sweep of the wrong person produces
 * output identical to a correct one: there is no number that would look wrong.
 *
 * @author Claude Code
 */
public final class CallerBindingCheck {

    private static int pass, fail;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (ok) pass++; else fail++;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    public static void main(String[] args) {
        RehearsalMatrix.Caller org1 = RehearsalMatrix.caller("org-1");
        RehearsalMatrix.Caller event1840 = RehearsalMatrix.caller("event-1840");

        section("A claim is checked against the session's own principal");
        check("the right person passes",
              KbsCli.callerRefusal(org1, new ModalityUserPrincipal(org1.personId(), 15)) == null);
        check("another person is refused",
              KbsCli.callerRefusal(org1, new ModalityUserPrincipal(37116, 15)) != null);
        check("and the refusal names BOTH persons, because either one alone explains nothing",
              refusal(org1, 37116).contains(String.valueOf(org1.personId()))
              && refusal(org1, 37116).contains("37116"));

        section("The all-zeros row, where no number would look wrong");
        // 2496's account is shared by nine persons. A sweep as any of the other eight returns zero for every
        // event - which is exactly what this row predicts - so this refusal is the only thing between a
        // correct run and a meaningless one.
        check("a wrong person on the negative control is refused too",
              KbsCli.callerRefusal(event1840, new ModalityUserPrincipal(55476, 15)) != null);
        check("and the right one passes",
              KbsCli.callerRefusal(event1840, new ModalityUserPrincipal(event1840.personId(), 15)) == null);

        section("No claim, and no answer");
        check("without --caller there is nothing to contradict",
              KbsCli.callerRefusal(null, new ModalityUserPrincipal(37116, 15)) == null);
        check("a principal that does not decode is a REFUSAL, not a pass",
              KbsCli.callerRefusal(org1, null) != null);
        check("nor does a principal of the wrong shape pass",
              KbsCli.callerRefusal(org1, "not-a-principal") != null);
        check("and that refusal says the claim could not be checked, rather than that it failed",
              KbsCli.callerRefusal(org1, null).contains("cannot be checked"));

        System.out.println();
        System.out.println("CallerBindingCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }

    private static String refusal(RehearsalMatrix.Caller caller, int personId) {
        String message = KbsCli.callerRefusal(caller, new ModalityUserPrincipal(personId, 15));
        return message == null ? "" : message;
    }
}
