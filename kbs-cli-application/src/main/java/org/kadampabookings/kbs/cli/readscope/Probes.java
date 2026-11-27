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
        false,
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
        false,
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
        false,
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
        false,
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
        false,
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
        false,
        "select count(1) as n from Person",
        List.of(),
        "select id, firstName, lastName, email from Person order by id",
        List.of(),
        true);

    // ===== what a caller with NO SESSION may read =================================================

    /**
     * How many bookings a signed-out caller can count. <b>A measurement, with no invariant, and that is
     * deliberate.</b>
     *
     * <p>Staging answered 294,713 on 2026-11-27 — the whole table. The obvious assertion would be that it
     * should be zero, and it is the wrong one: {@code NoPrincipalReadPolicy} leaves {@code Document} open
     * to a caller with no principal ON PURPOSE, because the cart and payment-return pages are reached
     * without signing in and read {@code Document} to show a booker their own booking. Worse,
     * {@code use-cart-view}'s existence probe relies in writing on generic reads NOT being row-filtered, so
     * that it can tell "expired link" from "not yours". Closing this root outright would break that.
     *
     * <p>So this probe exists to be read beside its neighbour rather than to pass or fail. If that one ever
     * reaches zero while this one does not, the fix was the right shape: the booking still exists to a
     * stranger, the booker does not.
     */
    public static final Probe DOCUMENTS_WITHOUT_SESSION = new Probe(
        "documents-without-session",
        "How many bookings a caller with no session can count (294,713 on staging, 2026-11-27). A"
        + " measurement, not an assertion: anonymous Document reads are deliberate — the cart and"
        + " payment-return pages need them — so zero is not necessarily the right answer here.",
        null, // no arm: a signed-out caller carries no token, so there is none to be about
        true,
        "select count(1) as n from Document",
        List.of(),
        "select id, event from Document order by id",
        List.of(),
        false);

    /**
     * How many bookings carry a booker's details readable by a caller with no session. <b>This is the one
     * that must reach zero.</b>
     *
     * <p>Staging answered 294,060 on 2026-11-27, and the row form returns names, e-mail addresses and
     * passport numbers. {@code document} holds 56 denormalised {@code person_} columns — the booker's
     * details as they stood at booking time — and {@code NoPrincipalReadPolicy} keys on the ROOT entity, so
     * closing {@code Person} says nothing about the copy of {@code Person} living on {@code Document}.
     * Written up in {@code docs/security/anonymous-document-exposure.md}.
     *
     * <p><b>Why this pair of numbers and not one.</b> The neighbouring probe counts bookings; this one
     * counts bookings whose booker is legible. The gap between them is the whole finding, and keeping them
     * apart is what lets a fix be judged: a remedy that denies the {@code person_} columns while leaving
     * the root readable sends this to zero and leaves that one alone, which is precisely the shape the
     * cart pages can survive.
     *
     * <p><b>It fails today, by design.</b> See {@link Invariant#NOTHING_WITHOUT_A_SESSION}: a standing
     * measurement of an open exposure should read red until it closes.
     *
     * <p>Marked as personal data, so its rows need {@code --confirm-personal-data} as well as
     * {@code --rows}. That gate is why this belongs here rather than as a {@code query} invocation — a
     * catalogued probe can declare what it will show, which free-form DQL cannot.
     */
    public static final Probe BOOKER_DETAILS_WITHOUT_SESSION = new Probe(
        "booker-details-without-session",
        "How many bookings expose their booker's name to a caller with no session (294,060 on staging,"
        + " 2026-11-27). Must be zero: document's 56 denormalised person_ columns carry names, e-mail,"
        + " phone and passport, and closing Person does not close the copy of it living here.",
        null, // no arm: a signed-out caller carries no token, so there is none to be about
        true,
        "select count(1) as n from Document where person_firstName is not null",
        List.of(),
        "select id, person_firstName, person_lastName, person_email from Document"
        + " where person_firstName is not null order by id",
        List.of(Invariant.NOTHING_WITHOUT_A_SESSION),
        true);

    /**
     * A read a signed-out caller is SUPPOSED to be able to make, bound to one cart. <b>The counterweight
     * to its two neighbours, and the only one here whose failure costs money rather than privacy.</b>
     *
     * <p>Anonymous {@code Document} reads are not a mistake to be undone: resume-payment and the cart
     * probe are reached without signing in, and {@code NoPrincipalReadPolicy}'s second arm turns away the
     * UNBOUNDED ones precisely so these bounded ones can go on working. Nothing measured that. Both
     * exposure probes get greener the harder the door is shut, so a remedy one notch too broad would have
     * read as a complete success while people could not pay.
     *
     * <p><b>The uuid is a parameter and never a literal.</b> A cart uuid is the capability that lets a
     * signed-out caller read one booking — it IS the authorisation — so baking one into this catalogue
     * would be committing a credential to git, and a probe that carried its own key would stop measuring
     * the control it is named for. Supply one with {@code --cartUuid}.
     *
     * <p>Not marked as personal data: the count is a number, and the row form takes the booking's id and
     * nothing from the booker. The whole question here is whether the read answers at all.
     */
    public static final Probe CART_LOOKUP_WITHOUT_SESSION = new Probe(
        "cart-lookup-without-session",
        "That a signed-out caller can still read the one booking a cart uuid names — the read"
        + " resume-payment and the cart probe depend on. Must stay above zero: this is the probe that"
        + " catches a fix to the anonymous Document exposure that went one notch too wide.",
        null, // no arm: a signed-out caller carries no token, so there is none to be about
        true,
        "select count(1) as n from Document where cart.uuid = $1",
        List.of("cartUuid"),
        "select id from Document where cart.uuid = $1 order by id",
        List.of(Invariant.BOUNDED_READ_STILL_ANSWERS),
        false);

    /**
     * That {@code Person} itself is still closed to a caller with no session. <b>The control, and the only
     * one of this group that passes today.</b>
     *
     * <p>It is the contrast that made the Document finding legible: {@code NoPrincipalReadPolicy} closes
     * {@code Person} and {@code FrontendAccount}, and the exposure was the copy of Person living on
     * {@code Document} escaping a rule keyed on the ROOT. Nothing stood guard over the closed half, so a
     * regression there would have been found the same way the open half was — by somebody looking.
     *
     * <p>It also earns its place as the worked example of the other two. A refused read is what both
     * exposure probes should answer once the bounded-only arm enforces, and this one answers that way
     * NOW — so the machinery that tells a refusal from a breakdown is exercised against a live server
     * before anything depends on it, rather than first being trusted on the day it matters.
     */
    public static final Probe PERSONS_WITHOUT_SESSION = new Probe(
        "persons-without-session",
        "That Person stays closed to a caller with no session — the control the Document finding is"
        + " measured against, and the one probe here that holds today. Expect it to be REFUSED, which is"
        + " a stronger pass than a count of zero.",
        null, // no arm: a signed-out caller carries no token, so there is none to be about
        true,
        "select count(1) as n from Person",
        List.of(),
        "select id from Person order by id",
        // REFUSED, not merely "nothing". Person is a closed root, so refusal is the only answer a server
        // has here — and an answer of any size, zero included, means the run carried a principal it should
        // not have. That makes this probe the canary for the HARNESS as well as the control for the server.
        List.of(Invariant.REFUSED_WITHOUT_A_SESSION),
        false);

    private static final Map<String, Probe> BY_NAME = new LinkedHashMap<>();

    static {
        for (Probe probe : List.of(CONVERSATIONS_FOR_EVENT, MESSAGES_FOR_EVENT, REACTIONS_FOR_EVENT,
                                   MY_CONVERSATIONS, UNENFORCED_ROOT, PERSONS_VISIBLE,
                                   DOCUMENTS_WITHOUT_SESSION, BOOKER_DETAILS_WITHOUT_SESSION,
                                   CART_LOOKUP_WITHOUT_SESSION, PERSONS_WITHOUT_SESSION))
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
