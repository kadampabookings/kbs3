package org.kadampabookings.kbs.cli.readscope;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which identity should be able to reach which event — the rehearsal's matrix, as GRANTS rather than counts.
 *
 * <h3>What is declared here, and what is not</h3>
 *
 * <p>The runbook's table carries two different kinds of fact in one grid: how many rows each cell holds
 * (which changes whenever anybody works on staging) and whether the cell is reachable at all (which changes
 * only when a grant is added or removed). Only the second is here.
 *
 * <p>That split is the whole reason this tool asserts relations rather than numbers. The counts come from the
 * server at run time — the scoped one from the caller's own probe, the unscoped one from the same probe run
 * once as a platform super administrator. What this class supplies is the one thing neither run can know: for
 * a given caller and event, which of the two relations applies, {@code = unscoped} or {@code = 0}.
 *
 * <h3>Keeping it true</h3>
 *
 * <p>Re-derive it with {@code scripts/predict-chat-read-scope.sql} before a sitting. A cell that disagrees
 * with the grant tables is itself a finding — two instruments disagreeing is worth more than either agreeing
 * with itself — so a mismatch should be read before it is edited away.
 *
 * <p><b>The 2026-11-23 snapshot.</b> 33 of 43 staging grant rows name an organisation rather than an event,
 * and 18 of 25 back-office users hold no event or event-type grant at all — which is why the organisation arm
 * of the scope condition is not optional and why an event-granted operator reaches so little of the grid.
 *
 * @author Claude Code
 */
public final class RehearsalMatrix {

    /**
     * One identity in the rehearsal, named by what kind of caller it is rather than by who it is.
     *
     * @param label        what {@code --caller} takes
     * @param personId     the staging person, so the operator can tell which account to sign in as
     * @param grant        the grant that explains the row, in words
     * @param reachable    the events this caller's grants reach, among {@link #EVENTS}
     * @param unrestricted whether this is the platform super administrator whose runs supply the baseline
     */
    public record Caller(String label, int personId, String grant, Set<Integer> reachable, boolean unrestricted) {}

    /**
     * The events the rehearsal walks, with the organisation each belongs to.
     *
     * <p>Seven, because those are the ones staging holds conversations for. The grid's value is in its zeros
     * as much as its totals: a cell that should be zero is the half the UI cannot ask, since the picker will
     * not offer an event the operator does not hold.
     */
    public static final Map<Integer, String> EVENTS = new LinkedHashMap<>();

    static {
        EVENTS.put(1857, "Summer Festival 2026 (org 1)");
        EVENTS.put(1898, "Fall Festival 2026 (org 1)");
        EVENTS.put(1901, "US NEDC 2026 (org 187)");
        EVENTS.put(1882, "UKDC 2026 (org 152)");
        EVENTS.put(1957, "GSD Weekend (org 151)");
        EVENTS.put(2021, "STTP - RT (GToM Part 1) (org 1)");
        EVENTS.put(1883, "ITTP 2026 (org 151)");
    }

    /**
     * The three kinds precondition 3 names — event-granted, organisation-granted and unrestricted — plus a
     * second of each, because "testing with one account proves one arm" is the finding this rehearsal exists
     * to answer.
     */
    public static final List<Caller> CALLERS = List.of(
        new Caller("unrestricted", 6, "platform super administrator — no predicate at all",
                   Set.of(1857, 1898, 1901, 1882, 1957, 2021, 1883), true),
        new Caller("org-1", 3826, "organisation grant: organisation 1",
                   Set.of(1857, 1898, 2021), false),
        new Caller("org-187", 21203, "organisation grant: organisation 187",
                   Set.of(1901), false),
        new Caller("event-1857", 982, "event grant: event 1857",
                   Set.of(1857), false),
        new Caller("event-1901", 39593, "event grant: event 1901",
                   Set.of(1901), false),
        // Event 1840 is not one of the seven, so every cell of this row is a zero - which makes it the most
        // useful row in the grid: it is entirely made of the question the UI rehearsal cannot ask.
        new Caller("event-1840", 2496, "event grant: event 1840, which holds none of the events below",
                   Set.of(), false));

    private RehearsalMatrix() {}

    /** The caller with this label, or null. */
    public static Caller caller(String label) {
        for (Caller caller : CALLERS)
            if (caller.label().equals(label))
                return caller;
        return null;
    }

    /** The labels, for a refusal message. */
    public static String labels() {
        StringBuilder labels = new StringBuilder();
        for (Caller caller : CALLERS) {
            if (labels.length() > 0)
                labels.append(", ");
            labels.append(caller.label());
        }
        return labels.toString();
    }

    /**
     * Whether this caller's grants reach this event, or null when nothing here says.
     *
     * <p>Null rather than false for an event outside the grid: "not declared" and "declared unreachable" are
     * different, and an invariant that treated the first as the second would assert {@code = 0} about an event
     * nobody measured — so a typo in {@code --eventId} would come back as a pass.
     */
    public static Boolean reachable(Caller caller, int eventId) {
        if (caller == null)
            return null;
        if (caller.unrestricted())
            return true; // no predicate at all, so every event is reachable by construction
        if (!EVENTS.containsKey(eventId))
            return null;
        return caller.reachable().contains(eventId);
    }

    /** The grid, for the {@code rehearsal} command — which identity to sign in as, and what to run. */
    public static String plan() {
        StringBuilder plan = new StringBuilder();
        plan.append("The rehearsal matrix — reachability by GRANT, not recorded counts.\n");
        plan.append("Re-derive with: scripts/agent-db/psql-staging -f scripts/predict-chat-read-scope.sql\n\n");
        plan.append(String.format("%-14s %-8s %s%n", "caller", "person", "events its grants reach"));
        plan.append(String.format("%-14s %-8s %s%n", "------", "------", "-----------------------"));
        for (Caller caller : CALLERS) {
            StringBuilder events = new StringBuilder();
            for (Integer eventId : EVENTS.keySet())
                if (Boolean.TRUE.equals(reachable(caller, eventId))) {
                    if (events.length() > 0)
                        events.append(' ');
                    events.append(eventId);
                }
            plan.append(String.format("%-14s %-8d %s%n", caller.label(), caller.personId(),
                                      events.length() == 0 ? "(none — every cell is a must-be-zero)" : events));
        }
        plan.append('\n');
        plan.append("Order of runs:\n");
        plan.append("  1. Sign in as person 6 (unrestricted) and record the baseline for every event:\n");
        for (Integer eventId : EVENTS.keySet())
            plan.append("       probe conversations-for-event --eventId ").append(eventId).append(" --as-baseline\n");
        plan.append("     (and the same for messages-for-event and reactions-for-event)\n");
        plan.append("  2. For each other caller, sign in and run the same probes WITHOUT --as-baseline,\n");
        plan.append("     passing --caller <label>. The tool then knows which relation applies to each cell.\n");
        plan.append("  3. The member arm is separate: sign in as person 6 in the FRONT office and run\n");
        plan.append("       probe my-conversations\n");
        plan.append("  4. unenforced-root is run once per caller, in either arm, and must not move.\n");
        return plan.toString();
    }
}
