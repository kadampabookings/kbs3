package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import one.modality.crm.server.authn.gateway.shared.RouteAccessGuard;
import one.modality.crm.server.authn.gateway.shared.SecondFactorAccess;
import one.modality.crm.server.authn.gateway.shared.SecondFactorAccess.ApproverStanding;
import one.modality.crm.server.authn.gateway.shared.SuperAdminMembership;
import one.modality.crm.shared.services.authn.ModalityAuthenticationI18nKeys;
import one.modality.crm.shared.services.authn.ModalityUserPrincipal;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Who may read the security watch or start a check, asked again on EVERY call of every endpoint.
 *
 * <p>The report is a list of what is unpatched — an attacker's to-do list — so the audience is the narrowest the
 * system has: a <b>platform</b> super administrator (one whose super-administrator row names no organisation), on a
 * back-office session. The same rule as the second-factor administration of the authentication gateways
 * ({@code ModalityTotpAuthenticationGateway.requireSuperAdmin}, which is private), rebuilt here from its public parts:
 *
 * <ol>
 *   <li><b>Not a support view.</b> A support view carries the VIEWED member's person id in its principal, so every
 *       check after this one would be asked about the wrong person. Asked first only so the log can name the agent:
 *       the next check refuses a support view too.</li>
 *   <li><b>A back-office caller</b>, as {@link RouteAccessGuard#backOfficeCallerPersonId} decides it: a real
 *       principal, on a session this server established, whose token says it was opened for the back office, and
 *       naming a person.</li>
 *   <li><b>A platform super administrator</b>, as {@link SuperAdminMembership#isPlatformSuperAdmin} answers it.</li>
 *   <li><b>A session that presented a second factor</b>, as {@link SecondFactorAccess#approverStanding} answers it —
 *       for everything but {@link #whenPlatformSuperAdmin}: {@code REFUSED} (a password session once some super
 *       administrator holds an approved factor) fails with {@code AuthnPasskeyAdminNotPermittedError}, the key the
 *       back office's Sign-in security page already explains; {@code BOOTSTRAP} (no super administrator holds a factor
 *       yet) passes with a warning in the log, as it does in the gateways.</li>
 * </ol>
 *
 * <p>Every refusal of the first three checks is {@link RouteAccessGuard#refused()}: the caller learns that it was
 * refused, and no more — in particular not which check said no. The log says which, by person id only (never an email
 * or a name): it ships to aggregation. A lookup that fails or throws is a no.
 *
 * <p>The caller's state is read on the CALLING thread, synchronously, before the first asynchronous step:
 * {@link ThreadLocalStateHolder} is restored as soon as the synchronous part of the bus call returns. That is why
 * the session family is captured up front, before the membership lookup.
 *
 * @author Claude Code
 */
final class SecurityWatchAccess {

    private SecurityWatchAccess() {
    }

    /** The refusal a super administrator on a session without a second factor gets — a key the client can explain. */
    static final String NO_SECOND_FACTOR_REFUSAL = "[%s] Sign in with your passkey or authenticator app to do this"
        .formatted(ModalityAuthenticationI18nKeys.AuthnPasskeyAdminNotPermittedError);

    /** The staff member a call was allowed for: their person id, normalised to an Integer. */
    record Caller(Integer personId) {
    }

    /**
     * The checks a caller cleared before the membership lookup: their person id and the session family whose standing
     * the second-factor check asks about.
     */
    private record BackOfficeCaller(Integer personId, String sessionFamilyId) {
    }

    /**
     * Runs {@code work} for a platform super administrator on a back-office session that presented a second factor
     * (or the bootstrap standing), and refuses everybody else. MUST be called on the caller's thread.
     *
     * @param operation which endpoint is asking, for the log line of a refusal
     * @param work      what to do once allowed — not started otherwise
     */
    static <T> Future<T> whenPlatformSuperAdminWithFactor(String operation, Function<Caller, Future<T>> work) {
        return when(operation, true, work);
    }

    /**
     * {@link #whenPlatformSuperAdminWithFactor} without the second-factor check — for the home screen's summary only,
     * which carries three fields per action (severity, due date, known exploited) and nothing that says what is
     * affected, so the count shows on any back-office session of a platform super administrator. MUST be called on the
     * caller's thread.
     */
    static <T> Future<T> whenPlatformSuperAdmin(String operation, Function<Caller, Future<T>> work) {
        return when(operation, false, work);
    }

    private static <T> Future<T> when(String operation, boolean secondFactor, Function<Caller, Future<T>> work) {
        BackOfficeCaller caller = backOfficeCaller(operation);
        if (caller == null)
            return RouteAccessGuard.refused();
        Integer personId = caller.personId();
        return failClosed(() -> SuperAdminMembership.isPlatformSuperAdmin(personId,
                EntityStore.create(DataSourceModelService.getDefaultDataSourceModel())), operation, personId)
            .compose(platform -> {
                if (!platform)
                    return refuse(operation, "person " + personId + " is not a platform super administrator");
                if (!secondFactor)
                    return work.apply(new Caller(personId));
                return standing(caller.sessionFamilyId(), operation, personId)
                    .compose(standing -> {
                        if (standing == ApproverStanding.REFUSED) {
                            logRefusal(operation, "person " + personId + " is on a session that presented no second factor");
                            return Future.failedFuture(NO_SECOND_FACTOR_REFUSAL);
                        }
                        if (standing == ApproverStanding.BOOTSTRAP)
                            Console.log(SecurityWatchLog.PREFIX + "⚠️ Bootstrap: person " + personId + " reads the security watch ("
                                        + operation + ") on a password session because no super administrator holds an approved factor yet");
                        return work.apply(new Caller(personId));
                    });
            });
    }

    /**
     * The first two checks of the class comment; null when either refuses. Reads the caller's state, so it runs on the
     * caller's thread, before anything asynchronous.
     */
    private static BackOfficeCaller backOfficeCaller(String operation) {
        Object state = ThreadLocalStateHolder.getThreadLocalState();
        Object userId = StateAccessor.getUserId(state);
        ModalityUserPrincipal principal = userId instanceof ModalityUserPrincipal p ? p : null;
        String refusal = null;
        Integer personId = null;
        if (principal != null && principal.isSupportView())
            // The agent's id, not the viewed member's: the member did not ask for anything
            refusal = "support view (agent person " + principal.getSupportAgentPersonId() + ")";
        else {
            // The decision is the shared fence's; the log line only says whose call it was
            Object callerPersonId = RouteAccessGuard.backOfficeCallerPersonId(state);
            if (callerPersonId == null || principal == null)
                refusal = principal == null ? "no signed-in user"
                    : "person " + principal.getUserPersonId() + " is not on a verified back-office session";
            else if ((personId = Numbers.toInteger(callerPersonId)) == null)
                refusal = "the principal names no person";
        }
        if (refusal != null) {
            logRefusal(operation, refusal);
            return null;
        }
        // Not null here: backOfficeCallerPersonId refuses a state without a session family
        return new BackOfficeCaller(personId, StateAccessor.getSessionFamilyId(state));
    }

    /** The session's standing, failing closed: a lookup that throws or fails is REFUSED. */
    private static Future<ApproverStanding> standing(String sessionFamilyId, String operation, Integer personId) {
        Future<ApproverStanding> standing;
        try {
            standing = SecondFactorAccess.approverStanding(sessionFamilyId);
        } catch (RuntimeException e) {
            standing = Future.failedFuture(e);
        }
        return standing
            .recover(cause -> {
                Console.log(SecurityWatchLog.PREFIX + operation + ": the second-factor check failed for person " + personId
                            + " (" + (cause == null ? null : cause.getMessage()) + ")");
                return Future.succeededFuture(ApproverStanding.REFUSED);
            })
            .map(s -> s == null ? ApproverStanding.REFUSED : s);
    }

    /** A membership lookup, failing closed: one that throws or fails is not a yes, and its words go to the log. */
    private static Future<Boolean> failClosed(Supplier<Future<Boolean>> lookup, String operation, Integer personId) {
        Future<Boolean> membership;
        try {
            membership = lookup.get();
        } catch (RuntimeException e) {
            membership = Future.failedFuture(e);
        }
        return membership
            .recover(cause -> {
                Console.log(SecurityWatchLog.PREFIX + operation + ": the platform super administrator check failed for person "
                            + personId + " (" + (cause == null ? null : cause.getMessage()) + ")");
                return Future.succeededFuture(false);
            })
            .map(Boolean.TRUE::equals);
    }

    private static void logRefusal(String operation, String why) {
        Console.log(SecurityWatchLog.PREFIX + operation + " refused: " + why);
    }

    private static <T> Future<T> refuse(String operation, String why) {
        logRefusal(operation, why);
        return RouteAccessGuard.refused();
    }
}
