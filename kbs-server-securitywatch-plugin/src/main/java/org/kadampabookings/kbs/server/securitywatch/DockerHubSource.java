package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * When the base image's tag was last published, from Docker Hub's public API
 * ({@code /v2/repositories/{repository}/tags/{tag}}: {@code tag_last_pushed}, else {@code last_updated}). A base image
 * published after our image was built means a redeploy would pick up its fixes.
 *
 * <p>What leaves the server: the base image's repository and tag.
 *
 * @author Claude Code
 */
final class DockerHubSource {

    private DockerHubSource() {
    }

    static final String CHECK = "dockerhub";
    static final String URL = "https://hub.docker.com/v2/repositories/";
    static final int MAX_BYTES = 256 * 1024;
    static final long TIMEOUT_MILLIS = 30_000;
    private static final Pattern REPOSITORY = Pattern.compile("[a-z0-9][a-z0-9._-]*(/[a-z0-9][a-z0-9._-]*)?");
    private static final Pattern TAG = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9._-]{0,127}");

    /** The tag's last publication; null when it could not be read (the call failed, or the answer had no date). */
    static Future<Instant> lastPushed(Calls calls, Inventory.BaseImage image) {
        if (image == null || image.repository() == null || image.tag() == null
            || !REPOSITORY.matcher(image.repository()).matches() || !TAG.matcher(image.tag()).matches()) {
            calls.fail(CHECK, SourceClient.SourceFailure.MISSING);
            return Future.succeededFuture(null);
        }
        String repository = image.repository().contains("/") ? image.repository() : "library/" + image.repository();
        return calls.get(CHECK, URL + repository + "/tags/" + image.tag(), MAX_BYTES, TIMEOUT_MILLIS)
            .map(DockerHubSource::parse)
            .otherwise(e -> {
                if (!(e instanceof SourceClient.SourceFailure))
                    calls.fail(CHECK, SourceClient.SourceFailure.INVALID_RESPONSE);
                return null;
            });
    }

    static Instant parse(String text) {
        Map<String, Object> tag = JsonTree.parseObject(text);
        String when = JsonTree.string(tag, "tag_last_pushed");
        if (when == null)
            when = JsonTree.string(tag, "last_updated");
        if (when == null)
            throw new IllegalArgumentException("no publication date");
        try {
            return Instant.parse(when);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("not an instant");
        }
    }
}
