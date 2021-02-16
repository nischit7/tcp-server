package org.example.server.boot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation for {@link ContextRunner}.
 */
@Slf4j
public class ContextRunnerImpl implements ContextRunner {

    private final Vertx vertx;

    public ContextRunnerImpl(final Vertx vertx) {
        this.vertx = vertx;
    }
    @Override
    public <T> void execute(
            final int instances,
            final Consumer<Handler<AsyncResult<T>>> consumer,
            final Handler<AsyncResult<List<T>>> resultHandler) {

        if (Vertx.currentContext() != null) {
            throw new IllegalStateException("Already on a Vert.x thread!");
        }
        final ResultCollector<T> collector = new ResultCollector<>(instances, resultHandler);
        for (int i = 0; i < instances; i++) {
            wrap(consumer).accept(result -> {
                if (result.succeeded()) {
                    collector.pushResult(result.result());
                } else {
                    resultHandler.handle(Future.failedFuture(result.cause()));
                }
            });
        }
    }
    @Override
    public <T> List<T> executeBlocking(
            final int instances,
            final Consumer<Handler<AsyncResult<T>>> consumer,
            final long timeout,
            final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        final CompletableFuture<List<T>> future = new CompletableFuture<>();
        execute(instances, consumer, result -> {
            if (result.succeeded()) {
                future.complete(result.result());
            } else {
                future.completeExceptionally(result.cause());
            }
        });
        return future.get(timeout, unit);
    }

    private <T> Consumer<Handler<AsyncResult<T>>>  wrap(final Consumer<Handler<AsyncResult<T>>> consumer) {
        final Context context = this.vertx.getOrCreateContext();
        return resultHandler -> context.runOnContext(
            v -> consumer.accept(result -> context.runOnContext(v1 -> resultHandler.handle(result))));
    }

    private static final class ResultCollector<T> {
        private final int count;
        private final Handler<AsyncResult<List<T>>> resultHandler;
        private final List<T> results = new ArrayList<>();

        private ResultCollector(final int count, final Handler<AsyncResult<List<T>>> resultHandler) {
            this.count = count;
            this.resultHandler = resultHandler;
        }

        private synchronized void pushResult(final T result) {
            if (results.size() == count) {
                log.warn("Your callback must supply one result, and only one result, to ContextRunner. (Trying to add {0}.)", result);
            } else {
                results.add(result);
                if (results.size() == count) {
                    resultHandler.handle(Future.succeededFuture(results));
                }
            }
        }
    }
}
