package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.net.URI;
import java.util.Set;

/**
 * How the check talks to a public source: one GET or POST, answering the status and the body as text. The production
 * implementation is {@link VertxSourceClient} (Vert.x core's HttpClient, bounded in time and size); the checks use recorded answers.
 *
 * <p>Every call names its own response-size cap and timeout. A call that cannot be answered fails with a
 * {@link SourceFailure} carrying one of the contract's fixed codes — never a source's own words.
 *
 * @author Claude Code
 */
interface SourceClient {

    /** What a source answered: the HTTP status and the body. */
    record Reply(int status, String body) {
        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    Future<Reply> get(String url, int maxBytes, long timeoutMillis);

    Future<Reply> post(String url, String jsonBody, int maxBytes, long timeoutMillis);

    /** A call that failed, as one of the contract's fixed codes. */
    final class SourceFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        static final String TIMEOUT = "timeout", HTTP_ERROR = "http-error", TOO_LARGE = "too-large",
            INVALID_RESPONSE = "invalid-response", MISSING = "missing", DISABLED = "disabled";
        final String code;
        /** The HTTP status when there was one (an http-error), else 0: for the pacing of retries, never shown. */
        final int status;

        SourceFailure(String code) {
            this(code, 0);
        }

        SourceFailure(String code, int status) {
            super(code + (status == 0 ? "" : " " + status), null, false, false);
            this.code = code;
            this.status = status;
        }
    }

    /** The only hosts the check may call, all over https: the contract's section 5. */
    Set<String> ALLOWED_HOSTS = Set.of("api.osv.dev", "services.nvd.nist.gov", "www.cisa.gov", "endoflife.date", "hub.docker.com");

    /** Whether {@code url} is an https URL to one of {@link #ALLOWED_HOSTS}, on the default port, with no user part. */
    static boolean isAllowed(String url) {
        try {
            URI uri = new URI(url);
            return "https".equals(uri.getScheme()) && uri.getRawUserInfo() == null && uri.getPort() == -1
                   && uri.getHost() != null && ALLOWED_HOSTS.contains(uri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    /** The code of a failure: a {@link SourceFailure}'s own, anything else {@code invalid-response}. */
    static String codeOf(Throwable cause) {
        return cause instanceof SourceFailure f ? f.code : SourceFailure.INVALID_RESPONSE;
    }
}
