package org.kadampabookings.kbs.cli.readscope;

/**
 * The relations a probe's answer must satisfy — relations, never remembered numbers.
 *
 * <h3>Why not expected counts</h3>
 *
 * <p>Because they rot, and an instrument that fails for the wrong reason stops being run. Between 17 and 23
 * November staging gained a platform super administrator and lost three chat messages and a reaction; a tool
 * asserting "249" would have failed on all three mornings and been switched off before the morning it
 * mattered. Each relation below holds whatever the data is.
 *
 * <h3>Where the unscoped side comes from</h3>
 *
 * <p>From the same probe, run once as a platform super administrator. Such a caller reads under the
 * {@code unrestricted} scope token, whose rules are all null — no predicate at all — so its answer IS the
 * unscoped answer. No database access is needed anywhere in this tool, which is the property that lets an
 * agent run it against staging at all.
 *
 * @author Claude Code
 */
public enum Invariant {

    /**
     * {@code scoped(E) <= unscoped(E)}, for every caller and every event.
     *
     * <p>The one direction that must never happen: a scope that WIDENS a read. It is the cheapest check here
     * and the only one that applies to every probe of every caller, so it is also the one that would catch a
     * condition injected with the wrong precedence — {@code <filter> and A or B} read as
     * {@code (<filter> and A) or B}, which is a real defect this codebase has pinned elsewhere.
     */
    NEVER_WIDER("scoped <= unscoped") {
        @Override
        public Verdict evaluate(Measurement measurement) {
            Long unscoped = measurement.unscopedCount();
            if (unscoped == null)
                return Verdict.notEvaluated(this, "no unscoped baseline has been recorded for this probe and"
                                                  + " parameters — run it once with --as-baseline as a platform"
                                                  + " super administrator");
            if (measurement.scopedCount() <= unscoped)
                return Verdict.holds(this, measurement.scopedCount() + " <= " + unscoped);
            return Verdict.violated(this, "this caller read MORE (" + measurement.scopedCount()
                                          + ") than an unrestricted caller (" + unscoped + "). The scope widened"
                                          + " a read, which is the direction that must never happen.");
        }
    },

    /**
     * {@code scoped(E) = 0} where the caller holds no grant reaching E.
     *
     * <p><b>This is the half the UI rehearsal cannot ask.</b> The support inbox is an event-centric route whose
     * picker only offers events the operator holds, so an operator cannot select an event they should not see,
     * and "shows 0" cannot be observed there. A statement issued outside the UI can ask it, and that is the
     * single reason this tool exists rather than a second sitting at the page.
     */
    EMPTY_WHERE_NOT_GRANTED("scoped = 0 where the caller holds no grant reaching this event") {
        @Override
        public Verdict evaluate(Measurement measurement) {
            if (!Boolean.FALSE.equals(measurement.reachable()))
                return Verdict.notEvaluated(this, "this event is reachable by this caller (or reachability was"
                                                  + " not stated), so this invariant does not apply");
            if (measurement.scopedCount() == 0)
                return Verdict.holds(this, "0, as it must be");
            return Verdict.violated(this, "this caller read " + measurement.scopedCount() + " row(s) of an event"
                                          + " no grant of theirs reaches. The scope is not restricting.");
        }
    },

    /**
     * {@code scoped(E) = unscoped(E)} where the caller IS granted E.
     *
     * <p>The 11 November failure, stated as a relation: an operator's inbox went from 249 conversations to 0
     * because an unrestricted caller's grant arrays were empty by construction and every rule matched nothing.
     * Nobody would have noticed a subtler wrong number, which is why this compares against the unscoped answer
     * rather than against "looks about right".
     */
    EQUAL_WHERE_GRANTED("scoped = unscoped where the caller is granted this event") {
        @Override
        public Verdict evaluate(Measurement measurement) {
            if (!Boolean.TRUE.equals(measurement.reachable()))
                return Verdict.notEvaluated(this, "this event is not reachable by this caller (or reachability"
                                                  + " was not stated), so this invariant does not apply");
            Long unscoped = measurement.unscopedCount();
            if (unscoped == null)
                return Verdict.notEvaluated(this, "no unscoped baseline has been recorded for this probe and"
                                                  + " parameters");
            if (measurement.scopedCount() == unscoped)
                return Verdict.holds(this, measurement.scopedCount() + " = " + unscoped);
            return Verdict.violated(this, "this caller read " + measurement.scopedCount() + " of the " + unscoped
                                          + " row(s) an unrestricted caller reads, for an event they ARE granted."
                                          + " This is the 11 November failure: the scope is over-restricting.");
        }
    },

    /**
     * A member reads their own rows and no others.
     *
     * <p>The front-office arm. Its unscoped baseline is the whole table, so equality with it would be the
     * failure rather than the success — what is asserted is that the caller reads strictly less than everybody
     * and never more, with the exact figure checked by eye against the one conversation person 6 is known to
     * hold. A stricter machine-checkable form needs a second identity to compare against, which is a v2
     * question.
     */
    OWN_ROWS_ONLY("a member reads their own rows and no others") {
        @Override
        public Verdict evaluate(Measurement measurement) {
            Long unscoped = measurement.unscopedCount();
            if (unscoped == null)
                return Verdict.notEvaluated(this, "no unscoped baseline has been recorded for this probe");
            if (measurement.scopedCount() > unscoped)
                return Verdict.violated(this, "this member read more (" + measurement.scopedCount()
                                              + ") than the whole table (" + unscoped + ").");
            if (unscoped > 0 && measurement.scopedCount() == unscoped)
                return Verdict.violated(this, "this member read EVERY row (" + unscoped + "). The member arm is"
                                              + " unenforced: a scope that returns the whole table is not a scope.");
            return Verdict.holds(this, measurement.scopedCount() + " of " + unscoped
                                       + " — confirm by eye that it is this member's own");
        }
    },

    /**
     * An entity outside {@code ENFORCED_ROOTS} answers the same for every caller.
     *
     * <p>The tripwire for enforcement leaking past the roots it names. {@code ChatPresence} is deliberately
     * left out of the first flip, so if its count ever differs between a scoped caller and an unrestricted one,
     * a root nobody reviewed has started being enforced.
     */
    UNCHANGED_FOR_EVERY_CALLER("an unenforced root answers the same for every caller") {
        @Override
        public Verdict evaluate(Measurement measurement) {
            Long unscoped = measurement.unscopedCount();
            if (unscoped == null)
                return Verdict.notEvaluated(this, "no unscoped baseline has been recorded for this probe");
            if (measurement.scopedCount() == unscoped)
                return Verdict.holds(this, measurement.scopedCount() + " = " + unscoped);
            return Verdict.violated(this, "this caller read " + measurement.scopedCount() + " where an"
                                          + " unrestricted caller reads " + unscoped + ". Enforcement has"
                                          + " reached an entity outside ENFORCED_ROOTS.");
        }
    },

    /**
     * {@code scoped = 0}, for a caller with no session at all.
     *
     * <p>The only relation here that needs NO baseline, and that is the point of it. Every other invariant
     * compares what this caller read against what an unrestricted one reads, which presumes the read was
     * legitimate and asks only how far it reached. This one asserts there was nothing to reach: a caller
     * who is nobody must read none of it, so zero is the whole of the expectation and any number above it
     * is the finding.
     *
     * <p>It therefore VIOLATES while an exposure stands, which is intended rather than a defect in the
     * probe. A standing measurement of an open hole should read red until the hole closes, and go on
     * reading green afterwards as the guard against its return. Recording today's number as an accepted
     * baseline would do the opposite: it would make the exposure the thing we check has not changed.
     *
     * <p>Attach it only to a probe declared {@code anonymous}. On a signed-in probe it would assert that an
     * authorised caller reads nothing, which is not a security property but a broken query.
     */
    /**
     * {@code scoped > 0}, for a read a signed-out caller is SUPPOSED to be able to do.
     *
     * <p>The counterweight, and the reason it exists is that the dangerous failure here is not a hole left
     * open but a door shut too hard. Anonymous {@code Document} reads carry resume-payment and the cart
     * probe: a remedy one notch too broad does not leak anything, it stops people paying. That failure is
     * silent in every probe that only measures exposure, because they all go greener as it gets worse.
     *
     * <p>A refusal VIOLATES this, which is the whole point of keeping a refusal distinct from a zero.
     */
    BOUNDED_READ_STILL_ANSWERS("a bounded read a signed-out caller is meant to make still answers") {
        @Override
        public Verdict evaluate(Measurement measurement) {
            if (measurement.refused())
                return Verdict.violated(this, "REFUSED. A read the cart and payment-return pages depend on"
                                              + " was turned away: this is a remedy that has gone too wide,"
                                              + " and the symptom in production is people unable to pay.");
            if (measurement.scopedCount() > 0)
                return Verdict.holds(this, measurement.scopedCount() + " — still answered");
            return Verdict.violated(this, "answered 0. Either the parameter names nothing, or a read those"
                                          + " pages depend on now returns nothing, which looks to a booker"
                                          + " exactly like their booking having vanished.");
        }
    },

    NOTHING_WITHOUT_A_SESSION("a caller with no session reads none of this") {
        @Override
        public Verdict evaluate(Measurement measurement) {
            // A REFUSAL IS THE STRONGER PASS, and accepting it is what keeps this relation honest across
            // fixes whose shape nobody has settled yet. Deny the person_ columns and the count becomes 0;
            // refuse the unbounded statement outright and there is no count at all. Both mean the caller
            // got nothing, which is the property. Asserting a zero COUNT would have read the second as a
            // failure — the probe erroring where it should have gone green — and a probe whose pass looks
            // like a crash is worse than no probe.
            if (measurement.refused())
                return Verdict.holds(this, "refused outright — the read never ran, which is more than zero");
            if (measurement.scopedCount() == 0)
                return Verdict.holds(this, "0 — a caller with no session reads none of this");
            return Verdict.violated(this, "a caller with NO SESSION read " + measurement.scopedCount()
                                          + ". Nothing authenticated this run: whatever these rows hold is"
                                          + " readable by anyone who can reach the bus.");
        }
    };

    private final String statement;

    Invariant(String statement) {
        this.statement = statement;
    }

    /** The relation, in words, for the output. */
    public String statement() {
        return statement;
    }

    /** Whether this measurement satisfies this relation. */
    public abstract Verdict evaluate(Measurement measurement);

    /**
     * What a probe's answer was, with everything an invariant needs to judge it.
     *
     * @param scopedCount   what this caller read
     * @param unscopedCount what a platform super administrator read, or null when no baseline is recorded
     * @param reachable     whether this caller is granted this event, or null when nobody has said
     */
    public record Measurement(String probeName, String callerLabel, long scopedCount, Long unscopedCount,
                              Boolean reachable, boolean refused) {

        /** A measurement of an answer, which is what every probe before the no-session pair produced. */
        public Measurement(String probeName, String callerLabel, long scopedCount, Long unscopedCount,
                           Boolean reachable) {
            this(probeName, callerLabel, scopedCount, unscopedCount, reachable, false);
        }

        /**
         * The server turned this read away, so there is no count to judge.
         *
         * <p>Kept as a state of its own rather than folded into a zero, because the two are different
         * facts: zero means the read ran and found nothing, a refusal means it never ran. A relation that
         * wants "this caller got nothing" is satisfied by both, and one that wants "this caller can still
         * work" is satisfied by neither — and those are opposite directions, so the distinction has to
         * survive as far as the relation.
         */
        public static Measurement refused(String probeName, String callerLabel) {
            return new Measurement(probeName, callerLabel, -1, null, null, true);
        }
    }

    /** One relation's answer. */
    public record Verdict(Invariant invariant, Outcome outcome, String detail) {

        public enum Outcome {HOLDS, VIOLATED, NOT_EVALUATED}

        static Verdict holds(Invariant invariant, String detail) {
            return new Verdict(invariant, Outcome.HOLDS, detail);
        }

        static Verdict violated(Invariant invariant, String detail) {
            return new Verdict(invariant, Outcome.VIOLATED, detail);
        }

        static Verdict notEvaluated(Invariant invariant, String detail) {
            return new Verdict(invariant, Outcome.NOT_EVALUATED, detail);
        }

        public boolean violated() {
            return outcome == Outcome.VIOLATED;
        }

        /**
         * One line.
         *
         * <p>{@code ??} is printed as loudly as a failure on purpose: a rehearsal that quietly evaluated
         * nothing and exited zero is exactly the shape of instrument this arc has already been misled by once.
         */
        public String describe() {
            String mark = switch (outcome) {
                case HOLDS -> "ok  ";
                case VIOLATED -> "FAIL";
                case NOT_EVALUATED -> "??  ";
            };
            return "  " + mark + " " + invariant.statement() + " — " + detail;
        }
    }
}
