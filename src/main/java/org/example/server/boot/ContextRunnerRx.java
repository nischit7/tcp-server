package org.example.server.boot;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.reactivex.Observable;

/**
 * When using Vertx Beans instead of verticles, there is no concept of instances.
 * These utilities offer an easy way to replicate instances by executing user-supplied callbacks
 * on new event loops.
 * The results of these asynchronous calls are collated and made available to the client.
 */
public interface ContextRunnerRx {

    /**
     * Execute user-supplied code on a new event loop and provide the collated results asynchronously.
     *
     * @param instances the number of times to execute the code (and the number of event loops to use)
     * @param supplier supplies an {@code Observable} after executing the code; e.g., {@code Observable<HttpServer>}
     * @param <T> the type of object we are creating; e.g., {@code HttpServer}
     * @return an {@code Observable} that either emits a collated {@code List} of items, or an error.
     */
    <T> Observable<List<T>> execute(int instances, Supplier<Observable<T>> supplier);
    /**
     * Execute user-supplied code on a new event loop and provide the collated results synchronously.
     *
     * @param instances the number of times to execute the code (and the number of event loops to use)
     * @param supplier supplies an {@code Observable} after executing the code; e.g., {@code Observable<HttpServer>}
     * @param timeout how long to wait for a result
     * @param unit unit for the timeout
     * @param <T> the type of object we are creating; e.g., {@code HttpServer}
     * @return a collated {@code List} of items
     */
    <T> List<T> executeBlocking(int instances, Supplier<Observable<T>> supplier, long timeout, TimeUnit unit);
}
