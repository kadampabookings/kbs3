package org.kadampabookings.kbs.server.securitywatch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader and writer for this module: what public sources answer (third-party text, so read defensively)
 * and the report this module writes.
 *
 * <p>Reading gives plain Java values: {@link Map} (insertion-ordered) for an object, {@link List} for an array,
 * {@link String}, {@link BigDecimal} for a number, {@link Boolean}, and {@code null}. It is strict JSON (RFC 8259):
 * anything else is an {@link IllegalArgumentException}, which the callers turn into the {@code invalid-response}
 * code. Nesting deeper than {@link #MAX_DEPTH} is refused, so a hostile answer cannot exhaust the stack. The size of
 * the text is the caller's to bound (every source has its own cap).
 *
 * <p>Writing takes the same kinds of values (any {@link Number}; a {@link Map}'s keys as strings) and escapes every
 * string, so a value read from a source cannot break out of its quotes.
 *
 * <p>Its own code rather than the platform's AST parser: no service provider to find, no dependency for a module that
 * only needs this, and a depth limit the generated parser does not offer.
 *
 * @author Claude Code
 */
final class JsonTree {

    private JsonTree() {
    }

    static final int MAX_DEPTH = 64;

    // ===== Reading ===================================================================================================

    static Object parse(String text) {
        if (text == null)
            throw new IllegalArgumentException("no JSON text");
        Parser parser = new Parser(text);
        parser.skipWhitespace();
        Object value = parser.value(0);
        parser.skipWhitespace();
        if (parser.pos != text.length())
            throw parser.error("trailing characters");
        return value;
    }

    /** The text parsed as an object, or an {@link IllegalArgumentException}. */
    static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map))
            throw new IllegalArgumentException("not a JSON object");
        return asObject(value);
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        IllegalArgumentException error(String what) {
            return new IllegalArgumentException("invalid JSON at " + pos + ": " + what);
        }

        void skipWhitespace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
                    pos++;
                else
                    break;
            }
        }

        Object value(int depth) {
            if (depth > MAX_DEPTH)
                throw error("nested too deeply");
            if (pos >= s.length())
                throw error("unexpected end");
            char c = s.charAt(pos);
            switch (c) {
                case '{': return object(depth);
                case '[': return array(depth);
                case '"': return string();
                case 't': return literal("true", Boolean.TRUE);
                case 'f': return literal("false", Boolean.FALSE);
                case 'n': return literal("null", null);
                default:
                    if (c == '-' || (c >= '0' && c <= '9'))
                        return number();
                    throw error("unexpected character");
            }
        }

        private Object literal(String word, Object value) {
            if (!s.startsWith(word, pos))
                throw error("unexpected literal");
            pos += word.length();
            return value;
        }

        private Map<String, Object> object(int depth) {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // {
            skipWhitespace();
            if (pos < s.length() && s.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (pos >= s.length() || s.charAt(pos) != '"')
                    throw error("expected a key");
                String key = string();
                skipWhitespace();
                if (pos >= s.length() || s.charAt(pos) != ':')
                    throw error("expected ':'");
                pos++;
                skipWhitespace();
                map.put(key, value(depth + 1));
                skipWhitespace();
                if (pos >= s.length())
                    throw error("unexpected end");
                char c = s.charAt(pos++);
                if (c == '}')
                    return map;
                if (c != ',')
                    throw error("expected ',' or '}'");
            }
        }

        private List<Object> array(int depth) {
            List<Object> list = new ArrayList<>();
            pos++; // [
            skipWhitespace();
            if (pos < s.length() && s.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(value(depth + 1));
                skipWhitespace();
                if (pos >= s.length())
                    throw error("unexpected end");
                char c = s.charAt(pos++);
                if (c == ']')
                    return list;
                if (c != ',')
                    throw error("expected ',' or ']'");
            }
        }

        private String string() {
            pos++; // opening quote
            StringBuilder sb = null;
            int start = pos;
            while (true) {
                if (pos >= s.length())
                    throw error("unterminated string");
                char c = s.charAt(pos);
                if (c == '"') {
                    String result = sb == null ? s.substring(start, pos) : sb.append(s, start, pos).toString();
                    pos++;
                    return result;
                }
                if (c < 0x20)
                    throw error("control character in a string");
                if (c == '\\') {
                    if (sb == null)
                        sb = new StringBuilder();
                    sb.append(s, start, pos);
                    pos++;
                    if (pos >= s.length())
                        throw error("unterminated escape");
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > s.length())
                                throw error("short unicode escape");
                            try {
                                sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            } catch (NumberFormatException ex) {
                                throw error("bad unicode escape");
                            }
                            pos += 4;
                            break;
                        default:
                            throw error("bad escape");
                    }
                    start = pos;
                } else
                    pos++;
            }
        }

        private BigDecimal number() {
            int start = pos;
            if (s.charAt(pos) == '-')
                pos++;
            if (pos >= s.length())
                throw error("bad number");
            if (s.charAt(pos) == '0')
                pos++;
            else if (!digits())
                throw error("bad number");
            if (pos < s.length() && s.charAt(pos) == '.') {
                pos++;
                if (!digits())
                    throw error("bad fraction");
            }
            if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                pos++;
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-'))
                    pos++;
                if (!digits())
                    throw error("bad exponent");
            }
            if (pos - start > 64)
                throw error("number too long");
            try {
                return new BigDecimal(s.substring(start, pos));
            } catch (NumberFormatException | ArithmeticException e) {
                throw error("bad number");
            }
        }

        private boolean digits() {
            int start = pos;
            while (pos < s.length() && s.charAt(pos) >= '0' && s.charAt(pos) <= '9')
                pos++;
            return pos > start;
        }
    }

    // ===== Navigating ================================================================================================

    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : Collections.emptyList();
    }

    static Map<String, Object> object(Map<String, Object> map, String key) {
        return map == null ? null : asObject(map.get(key));
    }

    static List<Object> list(Map<String, Object> map, String key) {
        return map == null ? Collections.emptyList() : asList(map.get(key));
    }

    /** A string member, or null when absent or not a string. */
    static String string(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value instanceof String s ? s : null;
    }

    /** A number member as a double, or null. */
    static Double number(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Number n ? n.doubleValue() : null;
    }

    /** A whole-number member, or null (also for a number with a fraction or out of range). */
    static Integer integer(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof BigDecimal d) {
            try {
                return d.intValueExact();
            } catch (ArithmeticException e) {
                return null;
            }
        }
        return value instanceof Integer i ? i : null;
    }

    static boolean isTrue(Map<String, Object> map, String key) {
        return map != null && Boolean.TRUE.equals(map.get(key));
    }

    // ===== Writing ===================================================================================================

    static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb, 0);
        return sb.toString();
    }

    private static void write(Object value, StringBuilder sb, int depth) {
        if (depth > MAX_DEPTH)
            throw new IllegalArgumentException("nested too deeply");
        if (value == null)
            sb.append("null");
        else if (value instanceof String s)
            sb.append(SecurityWatchJson.quote(s));
        else if (value instanceof Boolean b)
            sb.append(b);
        else if (value instanceof BigDecimal d)
            sb.append(d.toPlainString());
        else if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d))
                sb.append("null");
            else
                sb.append(BigDecimal.valueOf(d).stripTrailingZeros().toPlainString());
        } else if (value instanceof Number n)
            sb.append(n.longValue());
        else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first)
                    sb.append(',');
                first = false;
                sb.append(SecurityWatchJson.quote(String.valueOf(entry.getKey()))).append(':');
                write(entry.getValue(), sb, depth + 1);
            }
            sb.append('}');
        } else if (value instanceof Iterable<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first)
                    sb.append(',');
                first = false;
                write(item, sb, depth + 1);
            }
            sb.append(']');
        } else
            sb.append(SecurityWatchJson.quote(value.toString()));
    }
}
