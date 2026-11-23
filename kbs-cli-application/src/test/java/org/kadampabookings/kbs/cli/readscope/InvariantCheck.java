package org.kadampabookings.kbs.cli.readscope;

import org.kadampabookings.kbs.cli.readscope.Invariant.Measurement;
import org.kadampabookings.kbs.cli.readscope.Invariant.Verdict;

/**
 * That each relation says yes only when it should, no when it should, and "not evaluated" when it cannot tell.
 *
 * <h3>The three outcomes, and why the third is as important as the second</h3>
 *
 * <p>An invariant that cannot be evaluated — no baseline, or nobody said whether the caller is granted the
 * event — must say so, loudly, rather than defaulting either way. Defaulting to "holds" is a rehearsal that
 * passes having asserted nothing, which is the shape of instrument this arc has already been misled by: on 20
 * November a probe that could not see its own subject reported zero and was believed, at a cost of three days
 * and two censuses. Defaulting to "violated" is an instrument that fails for the wrong reason, which is how a
 * check stops being run.
 *
 * <h3>Each relation's real failure</h3>
 *
 * <ul>
 *   <li>{@code NEVER_WIDER} — a scope that widened a read. The only direction that must never happen.</li>
 *   <li>{@code EMPTY_WHERE_NOT_GRANTED} — the scope is not restricting. The half the UI rehearsal cannot
 *       ask, because the event picker will not offer an event the operator does not hold.</li>
 *   <li>{@code EQUAL_WHERE_GRANTED} — 11 November: 249 conversations to 0.</li>
 *   <li>{@code OWN_ROWS_ONLY} — the member arm reading the whole table.</li>
 *   <li>{@code UNCHANGED_FOR_EVERY_CALLER} — enforcement leaking past the roots it names.</li>
 * </ul>
 *
 * <p><b>Probed by deletion:</b> each relation's comparison was removed in turn and this check failed each
 * time; all were restored. Removing the null-baseline guards turned four checks into NullPointerExceptions
 * rather than failures — also a fail, and the reason those guards come first.
 *
 * @author Claude Code
 */
public class InvariantCheck {

    static int pass, fail;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    static Measurement measurement(long scoped, Long unscoped, Boolean reachable) {
        return new Measurement("a-probe", "a-caller", scoped, unscoped, reachable);
    }

    static boolean holds(Invariant invariant, Measurement measurement) {
        return invariant.evaluate(measurement).outcome() == Verdict.Outcome.HOLDS;
    }

    static boolean violated(Invariant invariant, Measurement measurement) {
        return invariant.evaluate(measurement).violated();
    }

    static boolean notEvaluated(Invariant invariant, Measurement measurement) {
        return invariant.evaluate(measurement).outcome() == Verdict.Outcome.NOT_EVALUATED;
    }

    public static void main(String[] args) {
        System.out.println("scoped <= unscoped — the direction that must never happen");
        check("fewer than unscoped holds", holds(Invariant.NEVER_WIDER, measurement(133, 249L, null)));
        check("equal to unscoped holds", holds(Invariant.NEVER_WIDER, measurement(249, 249L, null)));
        check("zero holds", holds(Invariant.NEVER_WIDER, measurement(0, 249L, null)));
        check("MORE than unscoped is a violation", violated(Invariant.NEVER_WIDER, measurement(250, 249L, null)));
        check("one more is a violation — not a rounding allowance",
              violated(Invariant.NEVER_WIDER, measurement(1, 0L, null)));
        check("without a baseline it is NOT evaluated, rather than assumed",
              notEvaluated(Invariant.NEVER_WIDER, measurement(133, null, null)));
        check("and the explanation tells the operator how to record one",
              Invariant.NEVER_WIDER.evaluate(measurement(133, null, null)).detail().contains("--as-baseline"));

        System.out.println();
        System.out.println("scoped = 0 where no grant reaches the event — the half the UI cannot ask");
        check("zero for an unreachable event holds",
              holds(Invariant.EMPTY_WHERE_NOT_GRANTED, measurement(0, 249L, false)));
        check("ANY row for an unreachable event is a violation",
              violated(Invariant.EMPTY_WHERE_NOT_GRANTED, measurement(1, 249L, false)));
        check("and the message says the scope is not restricting",
              Invariant.EMPTY_WHERE_NOT_GRANTED.evaluate(measurement(1, 249L, false)).detail()
                  .contains("not restricting"));
        check("it does not apply to a reachable event",
              notEvaluated(Invariant.EMPTY_WHERE_NOT_GRANTED, measurement(133, 249L, true)));
        check("and it does not apply when nobody said whether the event is reachable",
              notEvaluated(Invariant.EMPTY_WHERE_NOT_GRANTED, measurement(133, 249L, null)));
        check("it needs no baseline — zero is zero",
              holds(Invariant.EMPTY_WHERE_NOT_GRANTED, measurement(0, null, false)));

        System.out.println();
        System.out.println("scoped = unscoped where the caller IS granted — the 11 November failure");
        check("equal holds", holds(Invariant.EQUAL_WHERE_GRANTED, measurement(133, 133L, true)));
        check("SHORT is a violation", violated(Invariant.EQUAL_WHERE_GRANTED, measurement(132, 133L, true)));
        check("EMPTY is a violation — this is the 249-to-0 case",
              violated(Invariant.EQUAL_WHERE_GRANTED, measurement(0, 249L, true)));
        check("and the message names 11 November, so the reader knows what they are looking at",
              Invariant.EQUAL_WHERE_GRANTED.evaluate(measurement(0, 249L, true)).detail()
                  .contains("11 November"));
        check("it does not apply to an unreachable event",
              notEvaluated(Invariant.EQUAL_WHERE_GRANTED, measurement(0, 249L, false)));
        check("without a baseline it is NOT evaluated",
              notEvaluated(Invariant.EQUAL_WHERE_GRANTED, measurement(133, null, true)));
        check("an event with no conversations at all still holds at 0 = 0",
              holds(Invariant.EQUAL_WHERE_GRANTED, measurement(0, 0L, true)));

        System.out.println();
        System.out.println("a member reads their own rows and no others");
        check("one of many holds", holds(Invariant.OWN_ROWS_ONLY, measurement(1, 249L, null)));
        check("reading the WHOLE table is a violation — a scope that returns everything is not a scope",
              violated(Invariant.OWN_ROWS_ONLY, measurement(249, 249L, null)));
        check("reading more than the whole table is a violation",
              violated(Invariant.OWN_ROWS_ONLY, measurement(250, 249L, null)));
        check("reading none holds — a member with no conversations is not a failure",
              holds(Invariant.OWN_ROWS_ONLY, measurement(0, 249L, null)));
        check("an empty table does not read as 'the member saw everything'",
              holds(Invariant.OWN_ROWS_ONLY, measurement(0, 0L, null)));
        check("without a baseline it is NOT evaluated",
              notEvaluated(Invariant.OWN_ROWS_ONLY, measurement(1, null, null)));

        System.out.println();
        System.out.println("an unenforced root answers the same for every caller");
        check("equal holds", holds(Invariant.UNCHANGED_FOR_EVERY_CALLER, measurement(12, 12L, null)));
        check("any difference is a violation, in either direction",
              violated(Invariant.UNCHANGED_FOR_EVERY_CALLER, measurement(11, 12L, null))
              && violated(Invariant.UNCHANGED_FOR_EVERY_CALLER, measurement(13, 12L, null)));
        check("and the message says enforcement has reached an entity outside ENFORCED_ROOTS",
              Invariant.UNCHANGED_FOR_EVERY_CALLER.evaluate(measurement(11, 12L, null)).detail()
                  .contains("ENFORCED_ROOTS"));
        check("without a baseline it is NOT evaluated",
              notEvaluated(Invariant.UNCHANGED_FOR_EVERY_CALLER, measurement(12, null, null)));

        System.out.println();
        System.out.println("Every relation states itself, and a verdict reads as one line");
        for (Invariant invariant : Invariant.values()) {
            check(invariant.name() + " states its relation in words",
                  invariant.statement() != null && !invariant.statement().isBlank());
            Verdict verdict = invariant.evaluate(measurement(0, null, null));
            check(invariant.name() + " never throws on a measurement with nothing in it", verdict != null);
            check(invariant.name() + "'s verdict prints its relation and its detail",
                  verdict.describe().contains(invariant.statement()) && verdict.describe().contains("—"));
        }
        check("a not-evaluated verdict is marked, not blank — silence is the failure this arc was misled by",
              Invariant.NEVER_WIDER.evaluate(measurement(0, null, null)).describe().contains("??"));
        check("a violation is marked FAIL",
              Invariant.NEVER_WIDER.evaluate(measurement(1, 0L, null)).describe().contains("FAIL"));

        System.out.println();
        System.out.println("InvariantCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
