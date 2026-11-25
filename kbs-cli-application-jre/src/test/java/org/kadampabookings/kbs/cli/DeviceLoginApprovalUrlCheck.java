package org.kadampabookings.kbs.cli;

/**
 * That this run names the approval page rather than guessing where it is, and refuses to dress a typo up
 * as a link.
 *
 * <p>The defect behind these lines: {@code device-login} printed {@code <connected origin>/device-login}
 * whatever was asked for. The page is a BACK-OFFICE route and the bus is reached at the front-office host
 * on staging and at :8080 in development, so the printed link was wrong in every configuration except the
 * one where somebody happened to point {@code --server} at the back office. It was found on the first real
 * staging run, by following it to a page that does not exist.
 *
 * <p>A wrong link is worse than no link here, because the whole control in this flow is that the operator
 * goes somewhere they already trust and types a code they read on their own terminal. So the default is to
 * NAME the page, and {@code --approval-origin} is how an operator who knows their back office gets a link.
 *
 * @author Claude Code
 */
public final class DeviceLoginApprovalUrlCheck {

    private static int pass, fail;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (ok) pass++; else fail++;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    /** That the value is refused, and refused with a message naming the flag. */
    private static void refused(String raw) {
        try {
            String got = KbsCli.approvalOrigin(raw);
            check("refused: \"" + raw + "\" (got \"" + got + "\")", false);
        } catch (IllegalArgumentException e) {
            check("refused: \"" + raw + "\" — " + (e.getMessage() != null
                  && e.getMessage().contains("--approval-origin") ? "and the message names the flag" : "?"),
                  e.getMessage() != null && e.getMessage().contains("--approval-origin"));
        }
    }

    public static void main(String[] args) {
        section("Saying nothing is the default, because this client cannot know where the back office is");
        check("no flag means no link", KbsCli.approvalOrigin(null) == null);
        check("and neither does an empty one", KbsCli.approvalOrigin("") == null);
        check("nor whitespace", KbsCli.approvalOrigin("   ") == null);

        section("An origin the operator gave is carried through exactly");
        check("https host", "https://backoffice.example.org"
              .equals(KbsCli.approvalOrigin("https://backoffice.example.org")));
        check("with a port", "http://localhost:5173"
              .equals(KbsCli.approvalOrigin("http://localhost:5173")));
        check("a trailing slash is dropped, so the path is not doubled up",
              "https://bo.example.org".equals(KbsCli.approvalOrigin("https://bo.example.org/")));
        check("surrounding whitespace is trimmed",
              "https://bo.example.org".equals(KbsCli.approvalOrigin("  https://bo.example.org  ")));

        section("Anything that is not an origin is refused, never printed as a link");
        refused("backoffice.example.org");          // no scheme: not followable
        refused("wss://backoffice.example.org");    // the bus URL, the easiest thing to paste by mistake
        refused("file:///etc/passwd");
        refused("javascript:alert(1)");
        refused("https://");                        // no host
        refused("not a url at all");

        section("An origin is not a page — a whole link is refused rather than quietly trimmed");
        // The likeliest mistake is pasting the page itself, which trimming would turn into
        // .../device-login/device-login. Refusing says what the flag is; trimming teaches it a false shape.
        refused("https://bo.example.org/device-login");
        refused("https://bo.example.org/some/path");
        refused("https://bo.example.org?next=/device-login");
        refused("https://bo.example.org#fragment");
        refused("https://user:secret@bo.example.org");

        System.out.println();
        System.out.println("DeviceLoginApprovalUrlCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
