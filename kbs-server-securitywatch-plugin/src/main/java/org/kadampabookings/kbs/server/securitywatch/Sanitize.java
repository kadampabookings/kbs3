package org.kadampabookings.kbs.server.securitywatch;

import java.util.regex.Pattern;

/**
 * What the report keeps of text a third party wrote. Every string from a source goes through here before it enters a
 * report: control and format characters removed (line breaks, bidirectional overrides, zero-width characters), runs of
 * spaces collapsed, and a hard length cap. The page renders the result as a text node, so markup is shown, not
 * interpreted; no URL from a source is ever stored (the page builds links from ids).
 *
 * @author Claude Code
 */
final class Sanitize {

    private Sanitize() {
    }

    static final int SUMMARY_MAX = 300;
    static final int SHORT_MAX = 64;

    private static final Pattern UNWANTED = Pattern.compile("[\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}\\u2028\\u2029]");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    /** The ids the page may link (the contract's section 6); others are shown without a link. */
    static final Pattern LINKABLE_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{2,63}$");

    private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?|ftp)://\\S+|\\bwww\\.\\S+");

    /**
     * A summary: cleaned, any URL in it removed (no URL from a source is stored), then cut to {@link #SUMMARY_MAX}
     * characters (an ellipsis marks a cut). Null stays null.
     */
    static String summary(String text) {
        return cut(clean(text == null ? null : URL.matcher(text).replaceAll(" ")), SUMMARY_MAX);
    }

    /** An id, a version, a name: cleaned and cut to {@link #SHORT_MAX}. */
    static String shortText(String text) {
        return cut(clean(text), SHORT_MAX);
    }

    static String clean(String text) {
        if (text == null)
            return null;
        String cleaned = SPACES.matcher(UNWANTED.matcher(text).replaceAll(" ")).replaceAll(" ").strip();
        return cleaned.isEmpty() ? null : cleaned;
    }

    static String cut(String text, int max) {
        if (text == null || text.codePointCount(0, text.length()) <= max)
            return text;
        int end = text.offsetByCodePoints(0, max - 1);
        return text.substring(0, end).stripTrailing() + "…";
    }
}
