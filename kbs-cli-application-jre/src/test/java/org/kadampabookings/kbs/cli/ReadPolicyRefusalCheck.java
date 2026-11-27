package org.kadampabookings.kbs.cli;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * That a read the server REFUSES is recognised as an answer, and that nothing else is.
 *
 * <p>Several probes exist to see a read turned away. A refusal arrives as an exception, and rethrowing it
 * exits 2 — the same code as a dead bus, a bad argument or a typo. A probe whose success cannot be told
 * apart from the server being down tells nobody anything, so {@code probe} recognises the one sentence the
 * read policy answers with and carries it into the relations as a fact rather than a breakdown.
 *
 * <p>Both halves of that are dangerous if they drift:
 *
 * <ul>
 *   <li><b>Too narrow</b> — the policy rewords its sentence, refusals stop being recognised, and a probe
 *       that should read green errors instead. Pinned by reading the policy's own source.</li>
 *   <li><b>Too wide</b> — a match that caught any failure would report a broken bus as a clean pass, which
 *       is the exact inversion this arrangement exists to prevent. Pinned by the negatives below.</li>
 * </ul>
 *
 * <p>Reading a server plugin's source to hold a client constant against it is the arrangement
 * {@code DeviceLoginGatewayCheck} already uses for a gateway's outcome codes and the migration that
 * defines them. The duplication is deliberate — a client must not depend on a server plugin — so what
 * makes it safe is that it is checked rather than remembered.
 *
 * @author Claude Code
 */
public final class ReadPolicyRefusalCheck {

    private static int pass, fail;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (ok) pass++; else fail++;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    public static void main(String[] args) throws Exception {
        section("The duplicated sentence still matches the policy that produces it");
        Path policy = Path.of(args.length > 0 ? args[0] : ".").resolve(
            "modality-fork/modality-crm/modality-crm-server-authz-required-plugin/src/main/java/"
            + "one/modality/crm/server/services/authz/NoPrincipalReadPolicy.java");
        String source = Files.exists(policy) ? Files.readString(policy) : "";
        check("the policy source was found", !source.isEmpty());
        check("and it still answers with exactly the sentence this client matches",
              source.contains("REFUSED = \"" + KbsCli.READ_POLICY_REFUSAL + "\""));
        // Both arms must keep answering with the same sentence: the closed-root one and the bounded-only
        // one added for the anonymous Document exposure. If one grew a message of its own, half the
        // refusals would go unrecognised and read as the bus being down.
        check("and both arms return that one constant, not a message each",
              source.split("return enforcing \\? REFUSED").length - 1
              + (source.split("return REFUSED").length - 1) >= 1);

        section("The policy's sentence is recognised, however it is wrapped");
        check("as it arrives from the bus",
              KbsCli.isReadPolicyRefusal(new RuntimeException(
                  "The query was refused: Server error: " + KbsCli.READ_POLICY_REFUSAL)));
        check("and nested behind a cause, which is how the CLI actually sees it",
              KbsCli.isReadPolicyRefusal(new RuntimeException("outer", new RuntimeException(
                  "Server error: " + KbsCli.READ_POLICY_REFUSAL))));
        check("and two causes deep",
              KbsCli.isReadPolicyRefusal(new RuntimeException("a", new RuntimeException("b",
                  new RuntimeException(KbsCli.READ_POLICY_REFUSAL)))));

        section("Nothing else is — a broken run must never read as a pass");
        for (String notARefusal : new String[]{
            "Connection refused",                              // the tempting substring, and a dead host
            "Server error: column person_nope does not exist", // a statement that is simply wrong
            "This query needs something else entirely",
            "needs a signed-in caller",                        // a near miss, deliberately
            "timed out", ""})
            check("still an error: \"" + notARefusal + "\"",
                  !KbsCli.isReadPolicyRefusal(new RuntimeException(notARefusal)));
        check("a null message is not a refusal",
              !KbsCli.isReadPolicyRefusal(new RuntimeException((String) null)));
        check("and neither is nothing at all", !KbsCli.isReadPolicyRefusal(null));

        section("A refusal is recorded distinctly from a failure and from a genuine zero");
        check("refused is not the failure marker", KbsCli.REFUSED_IN_LOG != -1);
        check("and not a count of zero", KbsCli.REFUSED_IN_LOG != 0);

        System.out.println();
        System.out.println("ReadPolicyRefusalCheck: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
