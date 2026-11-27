package org.kadampabookings.kbs.cli.readscope;

import dev.webfx.stack.db.query.cli.Audience;

/**
 * The probes that measure what a caller with NO session may read, and the one relation that judges them.
 *
 * <p>These exist because of a live finding: {@code NoPrincipalReadPolicy} closes {@code Person} to a caller
 * with no principal, but keys on the ROOT entity — and {@code document} carries 56 denormalised
 * {@code person_} columns holding the same facts, including e-mail, phone and passport. Staging answered
 * 294,060 bookings with a legible booker to a run with no session at all
 * ({@code docs/security/anonymous-document-exposure.md}).
 *
 * <p>Every case below guards a way this pair could quietly stop meaning what it says:
 *
 * <ul>
 *   <li>a probe about signed-out callers run WITH a session answers a different question under the same
 *       name — the failure {@code audienceRefusal} prevents one level further out;</li>
 *   <li>the relation attached to a signed-in probe would assert that an authorised caller reads nothing,
 *       which is a broken query rather than a security property;</li>
 *   <li>and the COUNT form must never project the columns the finding is about, because only the row form
 *       is behind {@code --confirm-personal-data}.</li>
 * </ul>
 *
 * @author Claude Code
 */
public final class AnonymousProbeCheck {

    private static int pass, fail;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (ok) pass++; else fail++;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    private static Invariant.Measurement measured(long scoped, Long unscoped) {
        return new Invariant.Measurement("booker-details-without-session", "anonymous", scoped, unscoped, null);
    }

    public static void main(String[] args) {
        Probe documents = Probes.byName("documents-without-session");
        Probe details = Probes.byName("booker-details-without-session");

        section("Both probes are in the catalogue and declare that they need no session");
        check("documents-without-session exists", documents != null);
        check("booker-details-without-session exists", details != null);
        check("documents-without-session is anonymous", documents != null && documents.anonymous());
        check("booker-details-without-session is anonymous", details != null && details.anonymous());
        check("neither claims an arm — a signed-out caller carries no token to put one in",
              documents != null && documents.audience() == null
              && details != null && details.audience() == null);

        section("An anonymous probe refuses a signed-in run, and vice versa");
        // The whole point: the same statement asked with a session answers for THAT caller, and filing it
        // under a name that says "no session" is how a closed hole would look open, or an open one closed.
        check("asked with a session, it refuses", details != null && details.anonymityRefusal(false) != null);
        check("and the refusal says to use --anonymous",
              details != null && details.anonymityRefusal(false).contains("--anonymous"));
        check("asked anonymously, it is content", details != null && details.anonymityRefusal(true) == null);
        Probe signedIn = Probes.byName("persons-visible");
        check("a signed-in probe refuses --anonymous", signedIn != null && signedIn.anonymityRefusal(true) != null);
        check("and is content without it", signedIn != null && signedIn.anonymityRefusal(false) == null);
        check("the arm refusal still works independently of anonymity",
              signedIn != null && signedIn.audienceRefusal(Audience.BACK_OFFICE) != null);

        section("Only the booker probe asserts anything, and the other's silence is deliberate");
        // documents-without-session must NOT assert zero: anonymous Document reads are intended (the cart
        // and payment-return pages need them), and use-cart-view's existence probe relies in writing on
        // generic reads not being row-filtered. Asserting zero there would demand a fix that breaks a page.
        check("booker-details asserts the no-session relation",
              details != null && details.invariants().contains(Invariant.NOTHING_WITHOUT_A_SESSION));
        check("documents-without-session asserts NOTHING — zero is not known to be the right answer there",
              documents != null && documents.invariants().isEmpty());

        section("The relation is never attached to a probe that HAS a session");
        // On a signed-in probe this would assert that an authorised caller reads nothing.
        for (Probe probe : Probes.all())
            if (probe.invariants().contains(Invariant.NOTHING_WITHOUT_A_SESSION))
                check(probe.name() + " carries it and is anonymous", probe.anonymous());
        check("and at least one probe carries it, so the loop above is not vacuous",
              Probes.all().stream().anyMatch(p -> p.invariants().contains(Invariant.NOTHING_WITHOUT_A_SESSION)));

        section("Zero holds, anything above it is the finding, and no baseline is needed");
        // Needing no baseline is the point: every other relation compares against what an unrestricted
        // caller reads. This one asserts there was nothing to reach, so null must not stop it evaluating.
        check("0 holds", Invariant.NOTHING_WITHOUT_A_SESSION.evaluate(measured(0, null))
                             .outcome() == Invariant.Verdict.Outcome.HOLDS);
        check("1 is violated", Invariant.NOTHING_WITHOUT_A_SESSION.evaluate(measured(1, null)).violated());
        check("294,060 is violated — the number staging actually answered",
              Invariant.NOTHING_WITHOUT_A_SESSION.evaluate(measured(294_060, null)).violated());
        check("it evaluates with NO baseline, rather than reporting 'not evaluated'",
              Invariant.NOTHING_WITHOUT_A_SESSION.evaluate(measured(0, null))
                  .outcome() != Invariant.Verdict.Outcome.NOT_EVALUATED);
        check("and a baseline, if one existed, changes nothing",
              Invariant.NOTHING_WITHOUT_A_SESSION.evaluate(measured(0, 294_713L))
                  .outcome() == Invariant.Verdict.Outcome.HOLDS);
        check("the violation names the absence of a session, not a scope",
              Invariant.NOTHING_WITHOUT_A_SESSION.evaluate(measured(5, null)).detail().contains("NO SESSION"));

        section("The COUNT form never projects what only the ROW form is gated on");
        // Only --rows is behind --confirm-personal-data. A count that selected person_ columns would print
        // personal data with no second flag, which is the gap this catalogue entry exists to avoid.
        check("the booker count selects a count and no person_ column",
              details != null && details.dql().startsWith("select count(1) as n from Document")
              && !details.dql().contains("select person_"));
        check("but it does FILTER on one, which is what makes the number mean something",
              details != null && details.dql().contains("person_firstName is not null"));
        check("the row form does project them — hence the gate",
              details != null && details.rowsDql().contains("person_email"));
        check("and the probe declares itself personal, so --rows needs --confirm-personal-data",
              details != null && details.showsPersonalData());
        check("the neighbouring probe's rows carry no person_ column, so it is not marked personal",
              documents != null && !documents.rowsDql().contains("person_") && !documents.showsPersonalData());

        System.out.println();
        System.out.println("AnonymousProbeCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
