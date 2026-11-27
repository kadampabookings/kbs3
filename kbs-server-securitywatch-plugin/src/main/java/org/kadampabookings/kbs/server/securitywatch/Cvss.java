package org.kadampabookings.kbs.server.securitywatch;

import java.util.HashMap;
import java.util.Map;

/**
 * CVSS v3 base scores and the severity they mean, for advisories that publish a vector but no score of their own
 * (OSV's {@code CVSS_V3} entries carry the vector only).
 *
 * <p>The base-score equations of the CVSS v3.1 specification (section 7.1), with its {@code Roundup}; a {@code 3.0}
 * vector is scored the same way (3.0's rounding differs only in rare floating-point edge cases). Only the eight base
 * metrics are read; temporal and environmental ones are ignored, as a base score does. A vector that is not a
 * complete v3 base vector scores null — never a guess.
 *
 * <p>Severity bands (the contract's, which are CVSS's own): 9.0+ critical, 7.0+ high, 4.0+ medium, above 0 low,
 * 0 none.
 *
 * @author Claude Code
 */
final class Cvss {

    private Cvss() {
    }

    /** The base score of a {@code CVSS:3.x/...} vector, or null when it is not a complete v3 base vector. */
    static Double baseScore(String vector) {
        if (vector == null || !(vector.startsWith("CVSS:3.0/") || vector.startsWith("CVSS:3.1/")))
            return null;
        Map<String, String> m = new HashMap<>();
        String[] parts = vector.split("/");
        for (int i = 1; i < parts.length; i++) {
            int colon = parts[i].indexOf(':');
            if (colon <= 0)
                return null;
            m.put(parts[i].substring(0, colon), parts[i].substring(colon + 1));
        }
        String scope = m.get("S");
        if (!"U".equals(scope) && !"C".equals(scope))
            return null;
        boolean changed = "C".equals(scope);
        Double av = pick(m.get("AV"), "N", 0.85, "A", 0.62, "L", 0.55, "P", 0.2);
        Double ac = pick(m.get("AC"), "L", 0.77, "H", 0.44);
        Double pr = pick(m.get("PR"), "N", 0.85, "L", changed ? 0.68 : 0.62, "H", changed ? 0.5 : 0.27);
        Double ui = pick(m.get("UI"), "N", 0.85, "R", 0.62);
        Double c = pick(m.get("C"), "H", 0.56, "L", 0.22, "N", 0.0);
        Double in = pick(m.get("I"), "H", 0.56, "L", 0.22, "N", 0.0);
        Double a = pick(m.get("A"), "H", 0.56, "L", 0.22, "N", 0.0);
        if (av == null || ac == null || pr == null || ui == null || c == null || in == null || a == null)
            return null;
        double iss = 1 - (1 - c) * (1 - in) * (1 - a);
        double impact = changed ? 7.52 * (iss - 0.029) - 3.25 * Math.pow(iss - 0.02, 15) : 6.42 * iss;
        double exploitability = 8.22 * av * ac * pr * ui;
        if (impact <= 0)
            return 0.0;
        return roundUp(Math.min(changed ? 1.08 * (impact + exploitability) : impact + exploitability, 10));
    }

    private static Double pick(String value, Object... pairs) {
        if (value == null)
            return null;
        for (int i = 0; i < pairs.length; i += 2)
            if (pairs[i].equals(value))
                return (Double) pairs[i + 1];
        return null;
    }

    /** CVSS v3.1's Roundup: the smallest one-decimal number at or above the input, free of floating-point noise. */
    static double roundUp(double input) {
        long intInput = Math.round(input * 100000);
        if (intInput % 10000 == 0)
            return intInput / 100000.0;
        return (Math.floor(intInput / 10000.0) + 1) / 10.0;
    }

    /** The severity a score means; null for a null score. */
    static String severityOf(Double score) {
        if (score == null)
            return null;
        if (score >= 9.0)
            return "critical";
        if (score >= 7.0)
            return "high";
        if (score >= 4.0)
            return "medium";
        return score > 0 ? "low" : "none";
    }

    /** The severity words in decreasing order of concern; the rank of a word is its index. */
    static final String[] SEVERITIES = {"critical", "high", "medium", "low", "none"};

    /** 0 for critical … 4 for none (and anything unknown). */
    static int rank(String severity) {
        for (int i = 0; i < SEVERITIES.length; i++)
            if (SEVERITIES[i].equals(severity))
                return i;
        return SEVERITIES.length - 1;
    }

    /** A severity word from an advisory's own rating ({@code CRITICAL}, {@code HIGH}, {@code MODERATE}, {@code LOW}), or null. */
    static String severityWord(String rating) {
        if (rating == null)
            return null;
        return switch (rating.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "CRITICAL" -> "critical";
            case "HIGH", "IMPORTANT" -> "high";
            case "MODERATE", "MEDIUM" -> "medium";
            case "LOW" -> "low";
            default -> null;
        };
    }
}
