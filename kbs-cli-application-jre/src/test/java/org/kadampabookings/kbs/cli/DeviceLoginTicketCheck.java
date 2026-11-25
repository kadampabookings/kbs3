package org.kadampabookings.kbs.cli;

/**
 * That a device-login reply is read field by field, and refused whole when it is not one.
 *
 * <p>The failures this guards are quiet ones on a terminal. A ticket built from a half-understood reply
 * prints {@code Type null} and sends somebody to a page with nothing to type; one with a nonsense expiry
 * either waits for a moment that never comes or gives up while the person is still reading the code. Both
 * look like the tool working.
 *
 * <p>The clamp has a second job beyond the wait: the number is also what the operator is TOLD, so an absurd
 * one is a lie on screen and not merely a long sit.
 *
 * @author Claude Code
 */
public final class DeviceLoginTicketCheck {

    private static int pass, fail;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (ok) pass++; else fail++;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    public static void main(String[] args) {
        section("A well-formed reply is read as it stands");
        DeviceLoginTicket ticket = DeviceLoginTicket.parse("{\"code\":\"ABCD-EFGH\",\"expiresInSeconds\":300}");
        check("it parses", ticket != null);
        check("the code is carried through exactly as the server formatted it",
              ticket != null && "ABCD-EFGH".equals(ticket.code()));
        check("and the expiry with it", ticket != null && ticket.expiresInSeconds() == 300);

        section("Anything that is not a reply is refused WHOLE, never half-read");
        for (String notAReply : new String[]{
            null, "", "   ", "not json", "[]", "\"a string\"", "42",
            "{}",                                                  // neither field
            "{\"expiresInSeconds\":300}",                          // no code to print
            "{\"code\":\"ABCD\"}",                                 // no expiry to wait for
            "{\"code\":null,\"expiresInSeconds\":300}",
            "{\"code\":\"\",\"expiresInSeconds\":300}",
            "{\"code\":\"   \",\"expiresInSeconds\":300}",
            "{\"code\":\"ABCD\",\"expiresInSeconds\":\"300\"}",    // a string where a number belongs
            "{\"code\":\"ABCD\",\"expiresInSeconds\":0}",
            "{\"code\":\"ABCD\",\"expiresInSeconds\":-5}",
        })
            check("refused: " + (notAReply == null ? "null" : "\"" + notAReply + "\""),
                  DeviceLoginTicket.parse(notAReply) == null);

        section("A non-String reply is refused too — the wire can carry anything");
        check("an Integer is not a reply", DeviceLoginTicket.parse(42) == null);
        check("nor is a bare object", DeviceLoginTicket.parse(new Object()) == null);

        section("An absurd expiry is clamped, because it is printed as well as waited on");
        DeviceLoginTicket hour = DeviceLoginTicket.parse("{\"code\":\"ABCD\",\"expiresInSeconds\":86400}");
        check("a day is cut to an hour", hour != null && hour.expiresInSeconds() == 3600);
        DeviceLoginTicket fine = DeviceLoginTicket.parse("{\"code\":\"ABCD\",\"expiresInSeconds\":3600}");
        check("an hour exactly is kept", fine != null && fine.expiresInSeconds() == 3600);
        DeviceLoginTicket five = DeviceLoginTicket.parse("{\"code\":\"ABCD\",\"expiresInSeconds\":300}");
        check("and the five minutes the server actually sends is untouched",
              five != null && five.expiresInSeconds() == 300);

        section("Nothing about identity is read, because the reply holds none");
        // Pinned so that a later 'improvement' adding a person or account to the ticket has to argue with
        // this line first: the pending request names nobody ON PURPOSE, which is what stops the flow being
        // used to find out which accounts are enabled for it.
        check("the record has exactly the two fields it is allowed",
              DeviceLoginTicket.class.getRecordComponents().length == 2);
        DeviceLoginTicket withExtra = DeviceLoginTicket.parse(
            "{\"code\":\"ABCD\",\"expiresInSeconds\":300,\"username\":\"someone@example.org\"}");
        check("a server that volunteered a username would have it ignored, not carried",
              withExtra != null && withExtra.code().equals("ABCD") && withExtra.expiresInSeconds() == 300);

        System.out.println();
        System.out.println("DeviceLoginTicketCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
