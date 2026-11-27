package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.util.vertx.VertxInstance;
import io.vertx.core.Vertx;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The few asynchronous shapes the check needs: one step after another over a list (the sources are called one at a
 * time), a pause (the retry pause, the NVD's pacing), and work moved off the event loop (reading files, parsing large
 * answers, assembling the report).
 *
 * <p>On Vert.x directly, not through the platform {@code Scheduler}: on the server its provider is Vert.x's, whose
 * {@code runInBackground} is a timer of 0 ms — which Vert.x refuses ("Cannot schedule a timer with delay &lt; 1 ms") and
 * which would run on the event loop anyway. Background work goes to a Vert.x worker ({@code executeBlocking}); a pause
 * is a Vert.x timer of at least 1 ms, and a pause of 0 or less is no pause at all.
 *
 * @author Claude Code
 */
final class Async {

    private Async() {
    }

    /** Runs {@code step} for each item, one after the other; a step that fails stops nothing (it is the step's to recover). */
    static <T> Future<Void> each(Iterable<T> items, Function<T, Future<?>> step) {
        return next(items.iterator(), step);
    }

    private static <T> Future<Void> next(Iterator<T> it, Function<T, Future<?>> step) {
        if (!it.hasNext())
            return Future.succeededFuture();
        T item = it.next();
        Future<?> done;
        try {
            done = step.apply(item);
        } catch (RuntimeException e) {
            done = Future.failedFuture(e);
        }
        return done.<Void>transform(ar -> Future.succeededFuture()).compose(v -> next(it, step));
    }

    /** A future that completes after {@code millis}; at once when {@code millis} is 0 or less (never a timer under 1 ms). */
    static Future<Void> delay(long millis) {
        if (millis <= 0)
            return Future.succeededFuture();
        Promise<Void> promise = Promise.promise();
        vertx().setTimer(Math.max(1, millis), id -> promise.tryComplete(null));
        return promise.future();
    }

    /** {@code work} on a Vert.x worker thread, off the event loop; its exception fails the future. */
    static <T> Future<T> background(Supplier<T> work) {
        Promise<T> promise = Promise.promise();
        vertx().<T>executeBlocking(work::get, false).onComplete(ar -> {
            if (ar.succeeded())
                promise.tryComplete(ar.result());
            else
                promise.tryFail(ar.cause());
        });
        return promise.future();
    }

    private static Vertx vertx() {
        Vertx vertx = VertxInstance.getVertx();
        if (vertx == null)
            throw new IllegalStateException("no Vert.x instance");
        return vertx;
    }
}
