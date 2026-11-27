package org.kadampabookings.kbs.cli.readscope;

import dev.webfx.stack.db.query.cli.Audience;

import java.util.List;

/**
 * One question this tool knows how to ask, and what must be true of the answer.
 *
 * <h3>The shape, and where it departs from the design note</h3>
 *
 * <p>The note specified {@code {name, dql, params, invariant}} so that an arbitrary statement would later be
 * the same record with no invariant, and nothing would have to be restructured to let one in. That holds: an
 * arbitrary statement is this record with no invariants and no audience. Four differences, each forced by the
 * note's own content:
 *
 * <ul>
 *   <li><b>{@code invariants} is a list.</b> The note's table pairs three relations with one probe —
 *       {@code scoped <= unscoped} always, {@code = 0} where no grant reaches the event, {@code = unscoped}
 *       where one does. A single field would have meant choosing which of them to check.</li>
 *   <li><b>{@code audience} may be null</b>, meaning "either arm". The note's {@code unenforced-root} probe is
 *       listed as "both — must not change at a flip", and that is the point of it.</li>
 *   <li><b>{@code parameters}</b> names what the operator must supply, in {@code $1} order. It is the names,
 *       not the values: a probe is a question, and the event it is asked about belongs to the run.</li>
 *   <li><b>{@code rowsDql}</b>, because a count and the rows behind it are two statements. Deriving one from
 *       the other by rewriting a select list is the kind of string surgery that works until a probe has a join
 *       in it.</li>
 * </ul>
 *
 * <h3>{@code showsPersonalData}</h3>
 *
 * <p>Not a prohibition, a speed bump — the note's words. Such a probe answers with counts unless {@code --rows}
 * is passed with {@code --confirm-personal-data}, so the instrument does not casually do at scale the thing it
 * exists to detect. A count of 59,714 where the invariant expects "the caller's own" makes the point without a
 * name on screen.
 *
 * @author Claude Code
 */
public record Probe(String name,
                    String summary,
                    Audience audience,
                    boolean anonymous,
                    String dql,
                    List<String> parameters,
                    String rowsDql,
                    List<Invariant> invariants,
                    boolean showsPersonalData) {

    public Probe {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        invariants = invariants == null ? List.of() : List.copyOf(invariants);
    }

    /** Whether this probe can show rows at all, as opposed to answering only with a count. */
    public boolean hasRows() {
        return rowsDql != null;
    }

    /** How many values a run must supply. */
    public int parameterCount() {
        return parameters.size();
    }

    /**
     * Why this probe must not be asked by a run with(out) a session, or null when it may.
     *
     * <p>Kept apart from {@link #audienceRefusal} because it is a different question, asked earlier. An
     * arm is a property of a session's token; anonymity is the absence of a session altogether, so there is
     * no token to carry an arm and nothing to confirm. A probe written for a signed-out caller measures
     * what the server hands to anybody, and running it with a session would answer a different question
     * under the same name — which is the whole failure {@code audienceRefusal} exists to prevent, one
     * level further out.
     */
    public String anonymityRefusal(boolean runAnonymously) {
        if (anonymous == runAnonymously)
            return null;
        return anonymous
            ? "Probe '" + name + "' measures what a caller with NO session may read, so it must be run with"
              + " --anonymous. Asked with a session it would answer for that caller instead, under a name"
              + " that says otherwise."
            : "Probe '" + name + "' is written for a signed-in caller and --anonymous was given. With no"
              + " session there is no arm, no grant and no person for it to be about, so the number would"
              + " mean nothing the probe's name claims.";
    }

    /** Why this probe must not be asked under {@code actual}, or null when it may. */
    public String audienceRefusal(Audience actual) {
        if (audience == null || audience == actual)
            return null;
        return "Probe '" + name + "' belongs to the " + audience.optionValue() + " arm, and this session is a "
               + (actual == null ? "(unconfirmed)" : actual.optionValue()) + " one. Asking it here would"
               + " produce a number under the wrong heading: the scope token a read compiles under is chosen"
               + " by the session's audience, so the two arms are different questions, not two views of one.";
    }

    /** A one-line description for {@code probes} and the help text. */
    public String describe() {
        return name + "  [" + (anonymous ? "no session" : audience == null ? "either arm" : audience.optionValue()) + "]"
               + (parameters.isEmpty() ? "" : " <" + String.join("> <", parameters) + ">")
               + (showsPersonalData ? "  (counts only unless --rows)" : "")
               + "\n    " + summary;
    }
}
