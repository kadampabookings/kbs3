package org.kadampabookings.kbs.server.securitywatch;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The third-party libraries the applications run, as OSV names them: Java ({@code Maven}, {@code group:artifact}) and
 * the web apps' npm packages.
 *
 * <ul>
 *   <li><b>Java</b> — the list the build writes into the server's jar ({@link #JAVA_RESOURCE}, the output of
 *       {@code maven-dependency-plugin:list}: a header line, then {@code group:artifact:type[:classifier]:version}
 *       lines, each perhaps followed by {@code " (optional)"} and {@code " -- module …"}, and colour codes when Maven ran on a terminal). From a
 *       development run (no list in the classes) it falls back to the class path's jars that sit in a Maven repository
 *       layout ({@code …/<group path>/<artifact>/<version>/<artifact>-<version>[-classifier].jar}).</li>
 *   <li><b>npm</b> — {@code package-lock.json} (lockfileVersion 2 or 3: its {@code packages} map), without the root
 *       entry, links to the workspace's own packages, and the packages marked {@code dev} (build-time only: only what
 *       the applications run is checked). The name is what follows the last {@code node_modules/}.</li>
 * </ul>
 *
 * <p>The same name and version listed twice count once. Fewer than {@link #MIN_JAVA_LIBRARIES} Java libraries means
 * the list was not found, not that the server has few: the check then fails with {@code missing}.
 *
 * @author Claude Code
 */
final class Libraries {

    private Libraries() {
    }

    /** Where the build writes the Java list, inside the jar. */
    static final String JAVA_RESOURCE = "kbs/security-watch/java-libraries.txt";
    /** Fewer than this many Java libraries is a list that was not found. */
    static final int MIN_JAVA_LIBRARIES = 20;

    record Library(String name, String version) {
    }

    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");

    /** The libraries of a {@code dependency:list} output, in its order, without duplicates. */
    static List<Library> parseJavaList(String text) {
        Map<String, Library> libraries = new LinkedHashMap<>();
        if (text == null)
            return List.of();
        for (String raw : text.split("\n")) {
            String line = ANSI.matcher(raw).replaceAll("").strip();
            int module = line.indexOf(" -- ");
            if (module >= 0)
                line = line.substring(0, module).strip();
            // Maven marks an optional dependency " (optional)", before the module part when there is one
            if (line.endsWith(" (optional)"))
                line = line.substring(0, line.length() - " (optional)".length()).strip();
            if (line.isEmpty() || line.contains(" "))
                continue; // the header, and anything that is not one coordinate
            String[] parts = line.split(":");
            // group:artifact:type:version, group:artifact:type:classifier:version, each perhaps with :scope
            if (parts.length < 4 || parts.length > 6)
                continue;
            String version;
            if (parts.length == 4)
                version = parts[3];
            else if (parts.length == 5)
                version = isScope(parts[4]) ? parts[3] : parts[4];
            else
                version = parts[4];
            if (parts[0].isEmpty() || parts[1].isEmpty() || version.isEmpty())
                continue;
            Library library = new Library(parts[0] + ":" + parts[1], version);
            libraries.putIfAbsent(library.name() + "@" + library.version(), library);
        }
        return new ArrayList<>(libraries.values());
    }

    private static boolean isScope(String word) {
        return switch (word) {
            case "compile", "runtime", "provided", "system", "test", "import" -> true;
            default -> false;
        };
    }

    /**
     * The Maven coordinates of the class path's jars that sit in a repository layout; anything else (a module's own
     * classes folder, a jar elsewhere) is skipped.
     */
    static List<Library> fromClassPath(String classPath, String separator) {
        Map<String, Library> libraries = new LinkedHashMap<>();
        if (classPath == null)
            return List.of();
        for (String entry : classPath.split(Pattern.quote(separator))) {
            Library library = fromRepositoryPath(entry.replace(File.separatorChar, '/'));
            if (library != null)
                libraries.putIfAbsent(library.name() + "@" + library.version(), library);
        }
        return new ArrayList<>(libraries.values());
    }

    /** {@code …/repository/org/example/lib/1.2/lib-1.2.jar} gives {@code org.example:lib} 1.2; anything else null. */
    static Library fromRepositoryPath(String path) {
        if (path == null || !path.endsWith(".jar"))
            return null;
        String[] segments = path.split("/");
        int n = segments.length;
        if (n < 5)
            return null;
        String file = segments[n - 1], version = segments[n - 2], artifact = segments[n - 3];
        if (!file.startsWith(artifact + "-" + versionBase(version)))
            return null;
        int repository = -1;
        for (int i = n - 4; i >= 0; i--)
            if (segments[i].equals("repository")) {
                repository = i;
                break;
            }
        if (repository < 0 || repository >= n - 4)
            return null;
        String group = String.join(".", java.util.Arrays.copyOfRange(segments, repository + 1, n - 3));
        return group.isEmpty() ? null : new Library(group + ":" + artifact, version);
    }

    /** A snapshot's folder is {@code 1.0-SNAPSHOT} while its file may carry a timestamp: compare the base only. */
    private static String versionBase(String version) {
        return version.endsWith("-SNAPSHOT") ? version.substring(0, version.length() - "SNAPSHOT".length()) : version;
    }

    /** The packages of a {@code package-lock.json} the applications run; throws when it is not a lockfile. */
    static List<Library> parseNpmLock(String text) {
        Map<String, Object> root = JsonTree.parseObject(text);
        Map<String, Object> packages = JsonTree.object(root, "packages");
        if (packages == null)
            throw new IllegalArgumentException("no packages map (lockfileVersion 2 or 3 expected)");
        Map<String, Library> libraries = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : packages.entrySet()) {
            String path = entry.getKey();
            Map<String, Object> info = JsonTree.asObject(entry.getValue());
            // devOptional: needed only by build-time packages or optional ones of theirs - not installed for production
            if (path.isEmpty() || info == null || JsonTree.isTrue(info, "link") || JsonTree.isTrue(info, "dev")
                || JsonTree.isTrue(info, "devOptional"))
                continue;
            int last = path.lastIndexOf("node_modules/");
            if (last < 0)
                continue; // a workspace package's own folder, not a dependency
            String name = path.substring(last + "node_modules/".length());
            String version = JsonTree.string(info, "version");
            if (name.isEmpty() || version == null)
                continue;
            Library library = new Library(name, version);
            libraries.putIfAbsent(name + "@" + version, library);
        }
        return new ArrayList<>(libraries.values());
    }
}
