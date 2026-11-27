package org.kadampabookings.kbs.server.securitywatch;

/**
 * The few JSON replies of this module, written by hand: small flat objects, plus JSON text the database produced
 * (the stored report, the summary's actions) spliced in raw rather than parsed and written again. Every string is
 * escaped here, so a value read from a system property, a file or the database cannot break out of its quotes.
 *
 * @author Claude Code
 */
final class SecurityWatchJson {

    private SecurityWatchJson() {
    }

    /** A JSON object written field by field, in the order the fields are added. */
    static final class ObjectWriter {

        private final StringBuilder json = new StringBuilder("{");
        private boolean first = true;

        private ObjectWriter key(String key) {
            if (!first)
                json.append(',');
            first = false;
            json.append(quote(key)).append(':');
            return this;
        }

        /** A string, or {@code null}. */
        ObjectWriter string(String key, String value) {
            key(key).json.append(value == null ? "null" : quote(value));
            return this;
        }

        /** A whole number, or {@code null}. */
        ObjectWriter number(String key, Integer value) {
            key(key).json.append(value == null ? "null" : value.toString());
            return this;
        }

        ObjectWriter bool(String key, boolean value) {
            key(key).json.append(value);
            return this;
        }

        /**
         * JSON text produced by the database or by another writer, as it is — {@code null} when absent. Only for text
         * this server wrote itself (a {@code jsonb} column, an {@code ObjectWriter}): never for text from a caller.
         */
        ObjectWriter raw(String key, String jsonText) {
            key(key).json.append(jsonText == null ? "null" : jsonText);
            return this;
        }

        String toJson() {
            return json + "}";
        }
    }

    /** {@code value} as a JSON string literal: quotes, backslashes and every control character escaped. */
    static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    // Other control characters, and the two line separators JavaScript once refused inside strings
                    if (c < 0x20 || c == ' ' || c == ' ')
                        sb.append(String.format("\\u%04x", (int) c));
                    else
                        sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
