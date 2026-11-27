package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.meta.Meta;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the running server can say about itself, read live at each call: its Java runtime, its operating system
 * release, the database it talks to, when its image was built, and which environment it was built for. Any value
 * that cannot be read is null — never a guess.
 *
 * <ul>
 *   <li>{@code javaVersion} — the {@code java.version} property, which carries the full update number (for example
 *       {@code 11.0.25.1}; {@link Runtime#version()} drops nothing either, but adds the build).</li>
 *   <li>{@code javaVendorVersion} — the {@code java.vendor.version} property: the distribution's own release name,
 *       which its advisories use (null on a runtime that does not set it).</li>
 *   <li>{@code osRelease}, {@code osVersionId} — {@code PRETTY_NAME} and {@code VERSION_ID} of {@code /etc/os-release}
 *       (or {@code /usr/lib/os-release}, its standard fallback); null where neither exists (a developer's machine on
 *       another system).</li>
 *   <li>{@code postgresVersion} — the version the database reports, its first word (a packaged server appends its
 *       build, e.g. {@code 15.8 (Debian ...)}).</li>
 *   <li>{@code buildTimestamp} — the {@code APP_BUILD_TIMESTAMP} environment variable the deployment's image sets
 *       (null on a local run), read as the server's monitoring already reads it.</li>
 *   <li>{@code environment} — {@code production}, {@code staging} or {@code development}, as the Maven profile baked
 *       it into the build ({@link Meta}); anything else, including no profile, reads as development, as the session
 *       token's development-only test aids read it.</li>
 * </ul>
 *
 * @author Claude Code
 */
final class RuntimeFacts {

    private RuntimeFacts() {
    }

    /** The facts, as read. */
    record Values(String javaVersion, String javaVendorVersion, String osRelease, String osVersionId,
                  String postgresVersion, String buildTimestamp, String environment) {
    }

    /** Where the OS release is read from, in order. */
    static final String[] OS_RELEASE_FILES = {"/etc/os-release", "/usr/lib/os-release"};

    /** An os-release file is a few hundred bytes; anything far larger is not one. */
    private static final int OS_RELEASE_MAX_BYTES = 64 * 1024;

    /** Reads every fact; the database's version is the only asynchronous one, and it never fails (null instead). */
    static Future<Values> read() {
        Map<String, String> osRelease = readOsRelease();
        String javaVersion = property("java.version");
        String javaVendorVersion = property("java.vendor.version");
        String buildTimestamp = environmentVariable("APP_BUILD_TIMESTAMP");
        String environment = environment();
        return SecurityWatchStore.postgresVersion()
            .map(RuntimeFacts::firstWord)
            .otherwise((String) null)
            .map(postgres -> new Values(javaVersion, javaVendorVersion, osRelease.get("PRETTY_NAME"),
                osRelease.get("VERSION_ID"), postgres, buildTimestamp, environment));
    }

    /** {@code production}, {@code staging} or {@code development}. */
    static String environment() {
        try {
            if (Meta.isProduction())
                return "production";
            if (Meta.isStaging())
                return "staging";
        } catch (RuntimeException e) {
            // No meta information: the safe reading is the one that claims nothing about a real deployment
        }
        return "development";
    }

    private static String property(String key) {
        try {
            return blankToNull(System.getProperty(key));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String environmentVariable(String key) {
        try {
            return blankToNull(System.getenv(key));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Map<String, String> readOsRelease() {
        for (String file : OS_RELEASE_FILES) {
            Path path = Path.of(file);
            try {
                if (!Files.isRegularFile(path))
                    continue;
                byte[] bytes;
                try (InputStream in = Files.newInputStream(path)) {
                    bytes = in.readNBytes(OS_RELEASE_MAX_BYTES + 1);
                }
                if (bytes.length > OS_RELEASE_MAX_BYTES)
                    return Map.of();
                return parseOsRelease(new String(bytes, StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException e) {
                return Map.of();
            }
        }
        return Map.of();
    }

    /**
     * The {@code KEY=value} lines of an os-release file, values unquoted as its format defines (shell-style: single
     * or double quotes, backslash escapes inside double quotes). Comments, blank lines and malformed lines are skipped.
     */
    static Map<String, String> parseOsRelease(String content) {
        Map<String, String> values = new LinkedHashMap<>();
        if (content == null)
            return values;
        for (String rawLine : content.split("\n")) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#"))
                continue;
            int equals = line.indexOf('=');
            if (equals <= 0)
                continue;
            String key = line.substring(0, equals);
            if (!key.matches("[A-Z0-9_]+"))
                continue;
            String value = unquote(line.substring(equals + 1));
            if (value != null)
                values.put(key, blankToNull(value));
        }
        values.values().removeIf(v -> v == null);
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'"))
            return value.substring(1, value.length() - 1);
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            String inner = value.substring(1, value.length() - 1);
            StringBuilder sb = new StringBuilder(inner.length());
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '\\' && i + 1 < inner.length() && "\"\\$`".indexOf(inner.charAt(i + 1)) >= 0)
                    c = inner.charAt(++i);
                sb.append(c);
            }
            return sb.toString();
        }
        if (value.startsWith("\"") || value.startsWith("'"))
            return null; // an unterminated quote: malformed, skipped
        return value;
    }

    static String firstWord(String text) {
        if (text == null)
            return null;
        String trimmed = text.strip();
        int space = trimmed.indexOf(' ');
        return blankToNull(space < 0 ? trimmed : trimmed.substring(0, space));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
