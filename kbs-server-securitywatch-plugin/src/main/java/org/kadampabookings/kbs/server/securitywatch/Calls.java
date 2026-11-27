package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Every call one run makes to the sources, through one {@link SourceClient}: it records, per check, whether every call
 * succeeded (the contract: a check is ok only if every call to that source succeeded in this run) and the first
 * failure's code; and, for the measurements, how many calls were made, the largest answer and the time spent.
 *
 * <p><b>One retry.</b> A call that timed out, or was answered 403, 429 or 503 (the rate and overload answers), is
 * tried once more after a pause ({@link #retryPauseMillis}, longer for the NVD, whose pacing it must respect): one slow
 * answer must not make a component "could not be checked" for a whole day. A reply whose status is not 2xx is a failed
 * call ({@code http-error}), so the sources only ever see answers they can parse.
 *
 * <p><b>Bounded.</b> Two limits keep a run well inside the 30 minutes after which a running row is taken for dead:
 * <ul>
 *   <li>a source that still times out after its retry is not called again in this run (its remaining calls fail at
 *       once with {@code timeout}): a source that is down costs one call, its retry and the pause — at most 2 × 60 s +
 *       30 s — not one of those per library or product;</li>
 *   <li>a run budget ({@link #RUN_BUDGET_MILLIS}): once spent, no call starts (each fails at once with {@code timeout}),
 *       so a run takes at most the budget plus the one call in flight (2 × 60 s + 30 s) plus the assembly — about
 *       23 minutes in the worst case, against about 3.5 in a normal run.</li>
 * </ul>
 *
 * @author Claude Code
 */
final class Calls {

    /** No call starts once this much of the run has gone by. */
    static final long RUN_BUDGET_MILLIS = 20 * 60 * 1000;
    /** The pause before a retry, for every source but the NVD (which passes its own). */
    static final long RETRY_PAUSE_MILLIS = 5_000;

    private final SourceClient client;
    private final long retryPauseMillis;
    private final long deadline;
    private final Map<String, String> failures = new LinkedHashMap<>();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final Set<String> givenUp = new LinkedHashSet<>();
    private int largest, retries;
    private long millis;

    Calls(SourceClient client) {
        this(client, RETRY_PAUSE_MILLIS, RUN_BUDGET_MILLIS);
    }

    /** With the pause before a retry and the run's budget (the checks pass 0 and a short budget). */
    Calls(SourceClient client, long retryPauseMillis, long budgetMillis) {
        this.client = client;
        this.retryPauseMillis = retryPauseMillis;
        this.deadline = System.currentTimeMillis() + budgetMillis;
    }

    long retryPauseMillis() {
        return retryPauseMillis;
    }

    Future<String> get(String check, String url, int maxBytes, long timeoutMillis) {
        return get(check, url, maxBytes, timeoutMillis, retryPauseMillis);
    }

    /** A GET with its own pause before the retry (the NVD's, for its rate limit). */
    Future<String> get(String check, String url, int maxBytes, long timeoutMillis, long pauseMillis) {
        return record(check, () -> client.get(url, maxBytes, timeoutMillis), pauseMillis, false);
    }

    /**
     * A GET for which 404 is an answer, not a failure: it completes with null and the check stays ok (OSV answers 404
     * for an advisory that was withdrawn).
     */
    Future<String> getOrNotFound(String check, String url, int maxBytes, long timeoutMillis) {
        return record(check, () -> client.get(url, maxBytes, timeoutMillis), retryPauseMillis, true);
    }

    Future<String> post(String check, String url, String body, int maxBytes, long timeoutMillis) {
        return record(check, () -> client.post(url, body, maxBytes, timeoutMillis), retryPauseMillis, false);
    }

    private Future<String> record(String check, Supplier<Future<SourceClient.Reply>> call, long pauseMillis, boolean notFoundIsAnswer) {
        return attempt(check, call, notFoundIsAnswer)
            .compose(Future::succeededFuture, e -> {
                if (!retryable(e) || givenUp.contains(check) || System.currentTimeMillis() >= deadline)
                    return Future.failedFuture(e);
                retries++;
                return Async.delay(pauseMillis).compose(v -> attempt(check, call, notFoundIsAnswer));
            })
            .onFailure(e -> {
                fail(check, SourceClient.codeOf(e));
                if (e instanceof SourceClient.SourceFailure f && SourceClient.SourceFailure.TIMEOUT.equals(f.code))
                    givenUp.add(check); // the source is not answering: no more calls to it in this run
            });
    }

    /** One try: refused at once when the source was given up or the run's budget is spent. */
    private Future<String> attempt(String check, Supplier<Future<SourceClient.Reply>> call, boolean notFoundIsAnswer) {
        if (givenUp.contains(check) || System.currentTimeMillis() >= deadline)
            return Future.failedFuture(new SourceClient.SourceFailure(SourceClient.SourceFailure.TIMEOUT));
        counts.merge(check, 1, Integer::sum);
        long t0 = System.currentTimeMillis();
        Future<SourceClient.Reply> reply;
        try {
            reply = call.get();
        } catch (RuntimeException e) {
            reply = Future.failedFuture(new SourceClient.SourceFailure(SourceClient.SourceFailure.HTTP_ERROR));
        }
        return reply.compose(r -> {
            millis += System.currentTimeMillis() - t0;
            if (r.body() != null)
                largest = Math.max(largest, r.body().length());
            if (notFoundIsAnswer && r.status() == 404)
                return Future.succeededFuture(null);
            return r.ok() ? Future.succeededFuture(r.body())
                : Future.failedFuture(new SourceClient.SourceFailure(SourceClient.SourceFailure.HTTP_ERROR, r.status()));
        }, e -> {
            millis += System.currentTimeMillis() - t0;
            return Future.failedFuture(e);
        });
    }

    /** A timeout, or a rate or overload answer: worth one more try. */
    static boolean retryable(Throwable e) {
        if (!(e instanceof SourceClient.SourceFailure f))
            return false;
        return SourceClient.SourceFailure.TIMEOUT.equals(f.code) || f.status == 403 || f.status == 429 || f.status == 503;
    }

    /** Marks a check failed with {@code code}; the first failure's code is the one reported. */
    void fail(String check, String code) {
        failures.putIfAbsent(check, code);
    }

    /** The failure code of a check, or null when every call to it succeeded (or none was made). */
    String failureOf(String check) {
        return failures.get(check);
    }

    int callCount() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    Map<String, Integer> callCounts() {
        return counts;
    }

    int retryCount() {
        return retries;
    }

    int largestResponse() {
        return largest;
    }

    long sourceMillis() {
        return millis;
    }
}
