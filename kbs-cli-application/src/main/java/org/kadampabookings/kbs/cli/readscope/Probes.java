package org.kadampabookings.kbs.cli.readscope;

import dev.webfx.stack.db.query.cli.Audience;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The probes, as the design note tabulated them.
 *
 * <p>Statements are verbatim from the note; the field names were checked against the domain model
 * ({@code Conversation.event}, {@code Conversation.viewerPerson}, {@code ChatMessage.conversation},
 * {@code ChatMessageReaction.message}, {@code ChatPresence.agent}) and the roots against
 * {@code ChatReadScope.ENFORCED_ROOTS}, which holds {@code Conversation}, {@code ChatMessage} and
 * {@code ChatMessageReaction} on the staging branch.
 *
 * <p><b>Counts are server-side.</b> Every statement here is {@code select count(1) as n from ...}, which is
 * ordinary DQL — so a count is never bounded by how many rows this tool was willing to carry, and every
 * invariant can be a comparison of counts.
 *
 * @author Claude Code
 */
public final class Probes {

    /** The parameter name used wherever a probe takes an event's primary key. */
    public static final String EVENT = "eventId";

    /** Conversations of one event: the first enforced root, and the one whose rule is a local column test. */
    public static final Probe CONVERSATIONS_FOR_EVENT = new Probe(
        "conversations-for-event",
        "How many conversations of one event this back-office caller can read. The support inbox's own"
        + " question, asked about an event the picker may not offer.",
        Audience.BACK_OFFICE,
        "select count(1) as n from Conversation where event=$1",
        List.of(EVENT),
        "select id, status, type, createdAt from Conversation where event=$1 order by id",
        List.of(Invariant.NEVER_WIDER, Invariant.EMPTY_WHERE_NOT_GRANTED, Invariant.EQUAL_WHERE_GRANTED),
        false);

    /**
     * Messages of one event.
     *
     * <p>The root that closes the exposure: the front office reads a thread as
     * {@code ChatMessage where conversation=$1}, so scoping {@code Conversation} alone would hide the listing
     * and leave the bodies readable to any signed-in member who changed one id. Its rule is a subquery
     * evaluated per message row, with a second nested inside it for the back-office arm — so this is also the
     * probe whose TIMING matters, not only whose count does.
     */
    public static final Probe MESSAGES_FOR_EVENT = new Probe(
        "messages-for-event",
        "How many chat messages of one event this back-office caller can read.",
        Audience.BACK_OFFICE,
        "select count(1) as n from ChatMessage where conversation.event=$1",
        List.of(EVENT),
        // Deliberately without `content`: the count answers every invariant, and the bodies are what the
        // scope exists to protect. Ask for one with `query -c` if a specific message is genuinely in question.
        "select id, conversation, kind, createdAt from ChatMessage where conversation.event=$1 order by id",
        List.of(Invariant.NEVER_WIDER, Invariant.EMPTY_WHERE_NOT_GRANTED, Invariant.EQUAL_WHERE_GRANTED),
        false);

    /**
     * Reactions on the messages of one event — the third root a client reaches with a conversation id of its
     * own choosing, and so the one that would leave the same enumeration open one table further along.
     */
    public static final Probe REACTIONS_FOR_EVENT = new Probe(
        "reactions-for-event",
        "How many reactions on one event's messages this back-office caller can read.",
        Audience.BACK_OFFICE,
        "select count(1) as n from ChatMessageReaction where message.conversation.event=$1",
        List.of(EVENT),
        "select id, emoji, message, createdAt from ChatMessageReaction where message.conversation.event=$1 order by id",
        List.of(Invariant.NEVER_WIDER, Invariant.EMPTY_WHERE_NOT_GRANTED, Invariant.EQUAL_WHERE_GRANTED),
        false);

    /**
     * The member arm: the caller's own conversations.
     *
     * <p>{@code CALLER_PERSON} is written in the STATEMENT and bound by the endpoint. A client that sent a
     * {@code caller.person} parameter instead would be refused outright, which is the point of writing it this
     * way — see {@code ClientPath.refusalFor}.
     */
    public static final Probe MY_CONVERSATIONS = new Probe(
        "my-conversations",
        "How many conversations this front-office member can read. Should be their own and no others.",
        Audience.FRONT_OFFICE,
        "select count(1) as n from Conversation where viewerPerson=CALLER_PERSON",
        List.of(),
        "select id, event, status, createdAt from Conversation where viewerPerson=CALLER_PERSON order by id",
        List.of(Invariant.NEVER_WIDER, Invariant.OWN_ROWS_ONLY),
        false);

    /**
     * A root deliberately left OUT of the first flip, as the tripwire for enforcement leaking past the roots
     * it names. If this ever differs between a scoped caller and an unrestricted one, something nobody
     * reviewed has started being enforced.
     */
    public static final Probe UNENFORCED_ROOT = new Probe(
        "unenforced-root",
        "Agent presence rows — an entity outside ENFORCED_ROOTS, which must answer the same for every caller"
        + " before and after a flip.",
        null, // either arm: that it is the same in both is the assertion
        "select count(1) as n from ChatPresence where agent",
        List.of(),
        "select id, person, event, lastSeenAt from ChatPresence where agent order by id",
        List.of(Invariant.NEVER_WIDER, Invariant.UNCHANGED_FOR_EVERY_CALLER),
        false);

    /**
     * The one the design note said would fail. It did, and that is why {@code Person} is enforced.
     *
     * <p><b>What it measured: 59,858 — the whole table.</b> {@code ClientReadScope} held a single provider
     * covering only the three chat roots, so no row scoping applied to {@code person}; the only registered
     * read policy refuses callers with NO principal; and the deny list is a secrets list, on which
     * {@code first_name}, {@code last_name} and {@code email} do not appear. The front office had stopped
     * ASKING — its batch address-to-name query was replaced by the {@code matchingAccounts} endpoint — but
     * the server never started refusing, and nothing else could measure that gap.
     *
     * <p><b>The member arm closes it</b> ({@code PersonReadScope}, on staging since 2026-11-25): a
     * front-office caller reads the people on their own account, and the rows elsewhere linked to one of
     * them. The expected answer here is therefore no longer the table but a handful — a median of 1 on
     * staging, 51 for the largest account.
     *
     * <p><b>Still no invariant, but the reason has changed.</b> It is no longer that there is nothing to
     * satisfy; it is that there is nothing to compare against. {@code OWN_ROWS_ONLY} needs an unscoped
     * baseline, and {@code ChatReadScope.tokenForCaller} hands MEMBER to every front-office caller — a
     * platform super administrator included, because the arm is the verified audience signed into the session
     * token, not the caller's tier. No front-office run can record that baseline. Taking it from the back
     * office, where {@code Person} stays unscoped until the back-office arm is built, would let one probe
     * span both audiences, which is the one thing {@code Probe.audienceRefusal} exists to prevent. That is a
     * design decision rather than a line of code, so it is written down here instead of taken.
     */
    public static final Probe PERSONS_VISIBLE = new Probe(
        "persons-visible",
        "How many people a signed-in front-office member can count. The whole table before the member arm"
        + " (59,858 on staging); their own account's people after it.",
        Audience.FRONT_OFFICE,
        "select count(1) as n from Person",
        List.of(),
        "select id, firstName, lastName, email from Person order by id",
        List.of(),
        true);

    private static final Map<String, Probe> BY_NAME = new LinkedHashMap<>();

    static {
        for (Probe probe : List.of(CONVERSATIONS_FOR_EVENT, MESSAGES_FOR_EVENT, REACTIONS_FOR_EVENT,
                                   MY_CONVERSATIONS, UNENFORCED_ROOT, PERSONS_VISIBLE))
            BY_NAME.put(probe.name(), probe);
    }

    private Probes() {}

    public static List<Probe> all() {
        return List.copyOf(BY_NAME.values());
    }

    /** The probe with this name, or null. */
    public static Probe byName(String name) {
        return BY_NAME.get(name);
    }

    /** The names, for a refusal message. */
    public static String names() {
        return String.join(", ", BY_NAME.keySet());
    }
}
