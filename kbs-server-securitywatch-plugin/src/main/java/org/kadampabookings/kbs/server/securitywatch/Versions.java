package org.kadampabookings.kbs.server.securitywatch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Comparing version strings well enough for what the sources publish: dotted numbers ({@code 3.12}, {@code 2.4.7}),
 * Maven versions with qualifiers ({@code 2.15.0-rc1}, {@code 5.3.39.Final}), semantic versions with pre-releases
 * ({@code 19.0.0-rc.1}), and Java's update form ({@code 9u101}, {@code 9u103-b02}).
 *
 * <p>The rules: a version is cut into numbers and words at {@code . - + _}; numbers compare as numbers; a missing
 * number counts as 0 ({@code 2.4} = {@code 2.4.0}); a pre-release word ({@code alpha}, {@code beta}, {@code rc},
 * {@code m}, {@code snapshot}, {@code pre}, {@code dev}, {@code preview}, {@code ea}) comes before the release it
 * precedes; a release word ({@code final}, {@code ga}, {@code release}) is the release itself; Java's {@code 8uNNN}
 * reads as {@code 8.0.NNN} and drops its {@code -bNN} build. Not a full implementation of any one ecosystem's rules —
 * which is why a comparison that matters (does this flaw apply?) is left to the source wherever it can decide it
 * (OSV and NVD answer for the version asked).
 *
 * @author Claude Code
 */
final class Versions {

    private Versions() {
    }

    private static final Set<String> PRE_RELEASE = Set.of("alpha", "a", "beta", "b", "rc", "cr", "m", "milestone",
        "snapshot", "pre", "preview", "dev", "ea");
    private static final Set<String> RELEASE = Set.of("final", "ga", "release", "r");

    /** Negative, zero or positive, as {@code a} is lower than, equal to or higher than {@code b}. Null sorts lowest. */
    static int compare(String a, String b) {
        if (a == null || b == null)
            return a == null ? (b == null ? 0 : -1) : 1;
        List<Object> x = tokens(a), y = tokens(b);
        int n = Math.max(x.size(), y.size());
        for (int i = 0; i < n; i++) {
            Object p = i < x.size() ? x.get(i) : null, q = i < y.size() ? y.get(i) : null;
            int c = compareToken(p, q);
            if (c != 0)
                return c;
        }
        return 0;
    }

    static boolean isLower(String a, String b) {
        return compare(a, b) < 0;
    }

    private static int compareToken(Object p, Object q) {
        if (p == null && q == null)
            return 0;
        if (p == null)
            return -compareToken(q, null);
        if (q == null) {
            if (p instanceof BigInteger n)
                return n.signum();
            String word = (String) p;
            if (PRE_RELEASE.contains(word))
                return -1;
            return RELEASE.contains(word) ? 0 : 1;
        }
        if (p instanceof BigInteger m && q instanceof BigInteger n)
            return m.compareTo(n);
        if (p instanceof BigInteger) // a number outranks any word in the same place (1.0.1 > 1.0.rc1)
            return 1;
        if (q instanceof BigInteger)
            return -compareToken(q, p);
        String v = (String) p, w = (String) q;
        int rv = rank(v), rw = rank(w);
        return rv != rw ? Integer.compare(rv, rw) : v.compareTo(w);
    }

    /** Pre-release words first, then release words, then any other word. */
    private static int rank(String word) {
        return PRE_RELEASE.contains(word) ? 0 : RELEASE.contains(word) ? 1 : 2;
    }

    /** The version cut into numbers ({@link BigInteger}) and lower-case words. */
    static List<Object> tokens(String version) {
        String v = normalise(version);
        List<Object> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Boolean digits = null;
        for (int i = 0; i <= v.length(); i++) {
            char c = i < v.length() ? v.charAt(i) : '.';
            boolean separator = c == '.' || c == '-' || c == '+' || c == '_';
            boolean digit = c >= '0' && c <= '9';
            if (separator || (digits != null && digit != digits)) {
                if (current.length() > 0)
                    tokens.add(digits ? new BigInteger(current.toString()) : current.toString());
                current.setLength(0);
                digits = null;
                if (separator)
                    continue;
            }
            current.append(c);
            digits = digit;
        }
        return tokens;
    }

    /** Lower case, no leading {@code v}, Java's {@code 9u101[-b02]} as {@code 9.0.101}. */
    static String normalise(String version) {
        String v = version.trim().toLowerCase(Locale.ROOT);
        if (v.length() > 1 && v.charAt(0) == 'v' && Character.isDigit(v.charAt(1)))
            v = v.substring(1);
        java.util.regex.Matcher java8 = JAVA_UPDATE.matcher(v);
        if (java8.matches())
            v = java8.group(1) + ".0." + java8.group(2);
        return v;
    }

    private static final java.util.regex.Pattern JAVA_UPDATE = java.util.regex.Pattern.compile("(\\d+)u(\\d+)(?:-b\\d+)?");

    /** The first {@code n} numeric parts ({@code major} of {@code 3.2} is {@code 3}); null when it has none. */
    static String leadingNumbers(String version, int n) {
        if (version == null)
            return null;
        List<String> parts = new ArrayList<>();
        for (Object token : tokens(version)) {
            if (!(token instanceof BigInteger))
                break;
            parts.add(token.toString());
            if (parts.size() == n)
                break;
        }
        return parts.isEmpty() ? null : String.join(".", parts);
    }

    /** The first dotted version inside a text ({@code 3.2.1} in {@code Example OS 3.2.1 LTS}), or null. */
    static String firstVersionIn(String text) {
        if (text == null)
            return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)+|\\d+u\\d+").matcher(text);
        return m.find() ? m.group() : null;
    }
}
