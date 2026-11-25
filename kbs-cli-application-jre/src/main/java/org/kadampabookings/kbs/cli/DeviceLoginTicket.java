package org.kadampabookings.kbs.cli;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.json.Json;

/**
 * What the server answers when this terminal asks to be approved: a code to print, and how long it is good
 * for. Not a session, and it names nobody — the pending request it describes holds no identity at all.
 *
 * <h3>Why it is parsed rather than trusted</h3>
 *
 * <p>The reply is a JSON string off the wire. Reading it field by field, and refusing the whole thing when
 * any field is missing or the wrong type, is what stops a half-understood answer reaching the screen: a
 * ticket with a null code would print "Type null" and send somebody to a page with nothing to type, while a
 * nonsense expiry would have this run wait for a moment that never comes or give up before the person has
 * finished reading.
 *
 * <p>Refusal is {@code null} rather than an exception. The caller already has a sentence for "this server
 * did not answer with a code" — the likeliest cause being a server whose device-login gateway is not
 * deployed — and that is more use than a parse error naming a field.
 *
 * @param code             the code, formatted as the server chose to show it; printed and never sent on
 * @param expiresInSeconds how long the request stands, as the server counted it when asked
 *
 * @author Claude Code
 */
public record DeviceLoginTicket(String code, long expiresInSeconds) {

    private static final String CODE_KEY = "code";
    private static final String EXPIRES_KEY = "expiresInSeconds";

    /** Longest a request may claim to stand. Beyond this the server is not one this build understands. */
    private static final long MAX_EXPIRES_IN_SECONDS = 60 * 60;

    /**
     * Reads a reply, or null when it is not one.
     *
     * <p>The expiry is clamped as well as checked. A server that answered with an hour would otherwise have
     * this terminal sit waiting for one, holding a connection open long after the code it printed had died
     * — and the number is also what the operator is told, so an absurd one is a lie on screen rather than
     * merely a long wait.
     */
    public static DeviceLoginTicket parse(Object reply) {
        if (!(reply instanceof String text) || text.isBlank())
            return null;
        ReadOnlyAstObject object = Json.parseObjectSilently(text);
        if (object == null)
            return null;
        String code = object.getString(CODE_KEY);
        if (code == null || code.isBlank())
            return null;
        Object expires = object.get(EXPIRES_KEY);
        if (!(expires instanceof Number number))
            return null;
        long seconds = number.longValue();
        if (seconds <= 0)
            return null;
        return new DeviceLoginTicket(code, Math.min(seconds, MAX_EXPIRES_IN_SECONDS));
    }
}
