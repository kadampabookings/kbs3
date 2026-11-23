package org.kadampabookings.kbs.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The unscoped answers, recorded by a platform super administrator's run so the next caller's run can compare.
 *
 * <h3>Why a file rather than two numbers typed by hand</h3>
 *
 * <p>Because the relay is what the design note set out to close. A rehearsal that reads a number off one
 * screen and types it into another is where "249 became 0" was noticed only because zero is obviously wrong;
 * a subtler figure would have survived the retyping. One run writes, the next reads, and nothing passes
 * through a person.
 *
 * <h3>What is in it, and what therefore is not</h3>
 *
 * <p>Counts, keyed by probe and parameters. No rows, no identities beyond the probe name, nothing about any
 * person — so it is not a secret and does not need the treatment the session cache gets. It IS staleness-prone,
 * which is why every entry records when it was taken and the tool prints that alongside the comparison.
 *
 * @author Claude Code
 */
public final class BaselineStore {

    public static final String FILE_NAME = "read-scope-baselines.properties";

    private final Path file;

    public BaselineStore(Path directory) {
        this.file = directory.resolve(FILE_NAME);
    }

    public Path file() {
        return file;
    }

    /** One recorded baseline. */
    public record Baseline(long count, long recordedAtMillis) {}

    /**
     * The key a baseline is stored under: the probe and the values it was asked with.
     *
     * <p>Parameters are part of the key because {@code conversations-for-event} answers a different question
     * for every event, and a baseline keyed by probe alone would compare event 1857's scoped count against
     * event 1901's unscoped one — a comparison that would hold or fail for no reason anybody could trace.
     */
    public static String keyFor(String probeName, Object[] parameters) {
        StringBuilder key = new StringBuilder(probeName);
        if (parameters != null)
            for (Object parameter : parameters)
                key.append('|').append(parameter);
        return key.toString();
    }

    /** The recorded baseline, or null when there is none. */
    public Baseline load(String key) throws IOException {
        if (!Files.exists(file))
            return null;
        Properties properties = read();
        String count = properties.getProperty(key);
        if (count == null)
            return null;
        try {
            return new Baseline(Long.parseLong(count.strip()),
                                Long.parseLong(properties.getProperty(key + ".at", "0").strip()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Records a baseline, replacing any earlier one for the same key. */
    public void store(String key, long count, long nowMillis) throws IOException {
        Files.createDirectories(file.getParent());
        Properties properties = Files.exists(file) ? read() : new Properties();
        properties.setProperty(key, String.valueOf(count));
        properties.setProperty(key + ".at", String.valueOf(nowMillis));
        try (var out = Files.newOutputStream(file)) {
            properties.store(out, "Unscoped baselines for the read-scope probe CLI."
                                  + " Counts only - taken by a platform super administrator's run.");
        }
    }

    private Properties read() throws IOException {
        Properties properties = new Properties();
        try (var in = Files.newInputStream(file)) {
            properties.load(in);
        }
        return properties;
    }
}
