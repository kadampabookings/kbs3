package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.util.vertx.VertxInstance;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * The production {@link SourceClient}: Vert.x core's {@code HttpClient}, so every call is bounded in time and in size
 * from the process that serves users (it runs with {@code -XX:+ExitOnOutOfMemoryError}: one oversized or stalled
 * answer must never be able to restart it).
 *
 * <ul>
 *   <li><b>Hosts</b>: only an https URL to the contract's fixed hosts is requested ({@link SourceClient#isAllowed});
 *       TLS with the JVM's default trust and host-name verification.</li>
 *   <li><b>Redirects are not followed</b>: a 3xx answer is a failed call ({@code http-error}), so a call can never end
 *       up on a host outside the list.</li>
 *   <li><b>Time</b>: a connect timeout ({@value #CONNECT_TIMEOUT_MILLIS} ms), an idle timeout (no byte for
 *       {@value #IDLE_TIMEOUT_SECONDS} s), and a deadline for the whole call (the caller's {@code timeoutMillis}),
 *       after which the request is reset and the call fails with {@code timeout}.</li>
 *   <li><b>Size</b>: a declared {@code Content-Length} above the cap is refused before any byte is read; otherwise the
 *       body is read as a stream with a running count of its (decompressed) bytes, and the request is reset and the
 *       call failed with {@code too-large} the moment the count passes the cap. At most the cap plus one network
 *       buffer is ever held.</li>
 *   <li><b>Nothing identifying</b>: the User-Agent is {@value #USER_AGENT}; no cookie (the client keeps none), no
 *       credential, no key.</li>
 * </ul>
 *
 * <p>One client for the module, created on first use, without keep-alive (a run makes a few dozen calls a day).
 *
 * @author Claude Code
 */
final class VertxSourceClient implements SourceClient {

    static final String USER_AGENT = "security-watch";
    static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    static final int IDLE_TIMEOUT_SECONDS = 30;

    private final Supplier<Vertx> vertx;
    private volatile HttpClient client;

    VertxSourceClient() {
        this(VertxInstance::getVertx);
    }

    /** For a harness that brings its own Vert.x instance. */
    VertxSourceClient(Supplier<Vertx> vertx) {
        this.vertx = vertx;
    }

    private HttpClient client() {
        HttpClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null)
                    client = newClient(vertx.get());
                c = client;
            }
        }
        return c;
    }

    /** The module's client: the options of the class comment (the scratch harness builds the same one). */
    static HttpClient newClient(Vertx vertx) {
        return vertx.createHttpClient(new HttpClientOptions()
            .setConnectTimeout(CONNECT_TIMEOUT_MILLIS)
            .setIdleTimeout(IDLE_TIMEOUT_SECONDS)
            .setIdleTimeoutUnit(TimeUnit.SECONDS)
            .setKeepAlive(false)
            .setTrustAll(false)
            .setVerifyHost(true)
            .setMaxRedirects(0)
            .setDecompressionSupported(true));
    }

    @Override
    public Future<Reply> get(String url, int maxBytes, long timeoutMillis) {
        return call(HttpMethod.GET, url, null, maxBytes, timeoutMillis);
    }

    @Override
    public Future<Reply> post(String url, String jsonBody, int maxBytes, long timeoutMillis) {
        return call(HttpMethod.POST, url, jsonBody, maxBytes, timeoutMillis);
    }

    private Future<Reply> call(HttpMethod method, String url, String body, int maxBytes, long timeoutMillis) {
        if (!SourceClient.isAllowed(url))
            return Future.failedFuture(new SourceFailure(SourceFailure.INVALID_RESPONSE));
        return send(client(), vertx.get(), method, url, body, maxBytes, timeoutMillis);
    }

    /**
     * The call itself, without the host check — package-private so the scratch harness can aim it at a local test
     * server; production goes through {@link #call}.
     */
    static Future<Reply> send(HttpClient client, Vertx vertx, HttpMethod method, String url, String body, int maxBytes,
                              long timeoutMillis) {
        Promise<Reply> promise = Promise.promise();
        AtomicBoolean done = new AtomicBoolean();
        HttpClientRequest[] request = {null};
        long timer = vertx.setTimer(Math.max(1, timeoutMillis), id -> {
            if (done.compareAndSet(false, true)) {
                if (request[0] != null)
                    request[0].reset();
                promise.tryFail(new SourceFailure(SourceFailure.TIMEOUT));
            }
        });
        Runnable finish = () -> vertx.cancelTimer(timer);
        RequestOptions options = new RequestOptions()
            .setMethod(method)
            .setAbsoluteURI(url)
            .setFollowRedirects(false)
            .setIdleTimeout(TimeUnit.SECONDS.toMillis(IDLE_TIMEOUT_SECONDS))
            .putHeader("User-Agent", USER_AGENT)
            .putHeader("Accept", "application/json");
        if (body != null)
            options.putHeader("Content-Type", "application/json");
        client.request(options)
            .compose(req -> {
                request[0] = req;
                // A reset of ours (deadline, cap) arrives here too: the call has already failed with its own code, so only
                // an error before that fails it — and none is left for Vert.x to log as unhandled
                req.exceptionHandler(e -> {
                    if (done.compareAndSet(false, true)) {
                        finish.run();
                        promise.tryFail(new SourceFailure(SourceFailure.HTTP_ERROR));
                    }
                });
                if (done.get()) { // the deadline passed while connecting
                    req.reset();
                    return io.vertx.core.Future.failedFuture("timeout");
                }
                return body == null ? req.send() : req.send(Buffer.buffer(body, "UTF-8"));
            })
            .onComplete(ar -> {
                if (ar.failed()) {
                    if (done.compareAndSet(false, true)) {
                        finish.run();
                        promise.tryFail(new SourceFailure(SourceFailure.HTTP_ERROR));
                    }
                    return;
                }
                var response = ar.result();
                // First, so the resets below (redirect, declared size) are handled rather than logged as unhandled
                response.exceptionHandler(e -> fail(done, finish, response.request(), promise,
                    new SourceFailure(SourceFailure.HTTP_ERROR)));
                int status = response.statusCode();
                if (status >= 300 && status < 400) { // a redirect is never followed
                    fail(done, finish, response.request(), promise, new SourceFailure(SourceFailure.HTTP_ERROR, status));
                    return;
                }
                String length = response.getHeader("Content-Length");
                if (length != null && declaredTooLong(length, maxBytes)) {
                    fail(done, finish, response.request(), promise, new SourceFailure(SourceFailure.TOO_LARGE));
                    return;
                }
                Buffer content = Buffer.buffer();
                response.handler(chunk -> {
                    if (done.get())
                        return;
                    if (content.length() + chunk.length() > maxBytes) {
                        fail(done, finish, response.request(), promise, new SourceFailure(SourceFailure.TOO_LARGE));
                        return;
                    }
                    content.appendBuffer(chunk);
                });
                response.endHandler(v -> {
                    if (done.compareAndSet(false, true)) {
                        finish.run();
                        promise.tryComplete(new Reply(status, content.toString(StandardCharsets.UTF_8)));
                    }
                });
            });
        return promise.future();
    }

    private static void fail(AtomicBoolean done, Runnable finish, HttpClientRequest request, Promise<Reply> promise,
                             SourceFailure failure) {
        if (!done.compareAndSet(false, true))
            return;
        finish.run();
        try {
            request.reset();
        } catch (RuntimeException ignored) {
            // already closed: nothing more to stop
        }
        promise.tryFail(failure);
    }

    private static boolean declaredTooLong(String contentLength, int maxBytes) {
        try {
            return Long.parseLong(contentLength.trim()) > maxBytes;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
