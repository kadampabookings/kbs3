package org.kadampabookings.kbs.cli;

/**
 * That the guest login keeps the two promises its own output makes.
 *
 * <p>A guest is the third thing a caller can be — not a signed-in person, not nobody — and the only way
 * into one is a booking's cart link, which the server redeems before it mints. That makes this the one
 * login here whose credential is also an ordinary URL fragment, and the one command in a read-only tool
 * that can write.
 *
 * <p>Written after a review of code that had none, and the review found the thing these lines now hold:
 * the cart uuid was an OPTION, so a credential that mints a session went into the shell's history for
 * good — eighty lines below this file's own rule that a session-minting secret is read from a terminal
 * and never from the command line.
 *
 * @author Claude Code
 */
public final class GuestLoginCheck {

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
        String help = KbsCli.help();

        section("The cart uuid is a prompted secret, and there is no way to put it on a command line");
        // The defect this pins: as an option it sat in ~/.zsh_history for good and in `ps` for the run.
        check("no --cartUuid option on the guest command", !help.contains("guest  --cartUuid"));
        check("the help says it is prompted for", help.contains("PROMPTED FOR"));
        check("and says why — it must not sit in shell history",
              help.contains("shell history"));
        // An escape hatch for scripting is exactly how the value returns to a command line.
        check("no --cart-uuid or --cartUuid flag is offered anywhere as a bypass",
              !help.contains("--cartUuid") && !help.contains("--cart-uuid"));

        section("It announces that it can WRITE, alone among these commands");
        // GuestBookingAccess.linkToRedeem mints and attaches a link when the cart has none. Every other
        // command only reads, so an operator has no reason to expect this unless told.
        check("the help says it can write", help.contains("IT CAN WRITE"));
        check("and names what the server does", help.contains("mints and attaches"));
        check("and says where to do it", help.contains("Staging, not production"));

        section("The identity is recorded as a label, never as the address");
        // ModalityGuestPrincipal holds an e-mail and nothing else, and the server's own CallerLabel maps
        // it to "guest" for the same reason: an address in a log is the one thing this must not leave.
        check("the caller label is 'guest'", "guest".equals(KbsCli.GUEST_CALLER));
        check("the help promises the address is never printed or logged",
              help.contains("never printed or logged"));

        section("It is listed as a command a reader can find");
        check("the dispatch's unknown-command message names it", help.contains("guest"));
        check("and the help describes what a guest IS, not just how to log in as one",
              help.contains("neither a signed-in person nor nobody"));

        System.out.println();
        System.out.println("GuestLoginCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
