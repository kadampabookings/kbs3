package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import one.modality.crm.shared.services.authn.ModalityUserPrincipal;

/**
 * Check for the fences {@link SecurityWatchAccess} decides before asking the database: who is refused at once, with
 * the shared refusal that says nothing about why, and without the work ever starting.
 *
 * <p>The membership and second-factor answers need a database, and are exercised against one: a front-office session,
 * a staff member who is not a platform super administrator, and a session without a second factor (the plan's
 * end-to-end test).
 */
public class SecurityWatchAccessCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    /** RouteAccessGuard.refused()'s words. */
    private static final String REFUSED = "This operation is not available to you";

    private static Object state(Object principal, String sessionFamilyId, Boolean backOffice) {
        Object state = StateAccessor.createEmptyState();
        if (principal != null)
            state = StateAccessor.setUserId(state, principal);
        if (sessionFamilyId != null)
            state = StateAccessor.setSessionFamilyId(state, sessionFamilyId);
        if (backOffice != null)
            state = StateAccessor.setVerifiedBackoffice(state, backOffice);
        return state;
    }

    private static void refusedAtOnce(String what, Object state, boolean secondFactor) {
        boolean[] started = {false};
        Future<String> answer = ThreadLocalStateHolder.runWithState(state, () -> secondFactor
            ? SecurityWatchAccess.whenPlatformSuperAdminWithFactor("check", caller -> { started[0] = true; return Future.succeededFuture("x"); })
            : SecurityWatchAccess.<String>whenPlatformSuperAdmin("check", caller -> { started[0] = true; return Future.succeededFuture("x"); }));
        check(what + (secondFactor ? "" : " (summary rule)"), answer.isComplete() && answer.failed()
            && REFUSED.equals(answer.cause().getMessage()) && !started[0]);
    }

    public static void main(String[] args) {
        ModalityUserPrincipal staff = new ModalityUserPrincipal(456, 7);
        ModalityUserPrincipal supportView = new ModalityUserPrincipal(456, 7, 99);
        for (boolean secondFactor : new boolean[]{true, false}) {
            refusedAtOnce("no state at all", null, secondFactor);
            refusedAtOnce("no signed-in user", state(null, "family-1", Boolean.TRUE), secondFactor);
            refusedAtOnce("a front-office session", state(staff, "family-1", Boolean.FALSE), secondFactor);
            refusedAtOnce("a session whose audience was never verified", state(staff, "family-1", null), secondFactor);
            refusedAtOnce("no session this server established", state(staff, null, Boolean.TRUE), secondFactor);
            refusedAtOnce("a support view, even on a back-office session", state(supportView, "family-1", Boolean.TRUE), secondFactor);
            refusedAtOnce("a principal naming no person", state(new ModalityUserPrincipal(null, 7), "family-1", Boolean.TRUE), secondFactor);
        }
        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0)
            System.exit(1);
    }
}
