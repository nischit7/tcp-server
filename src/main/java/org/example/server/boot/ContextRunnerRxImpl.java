package org.example.server.boot;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.vertx.core.Future;

/**
 * Implementation for {@link ContextRunnerRx}.
 */
public class ContextRunnerRxImpl implements ContextRunnerRx {

    private final ContextRunner delegate;

    public ContextRunnerRxImpl(final ContextRunner delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Observable<List<T>> execute(final int instances, final Supplier<Observable<T>> supplier) {
        return Observable.create(subscriber -> doExecute(subscriber, instances, supplier));
    }

    @Override
    public <T> List<T> executeBlocking(
            final int instances, final Supplier<Observable<T>> supplier, final long timeout, final TimeUnit unit) {
        return execute(instances, supplier).blockingSingle();
    }

    private <T> void doExecute(
            final ObservableEmitter<? super List<T>> subscriber,
            final int instances,
            final Supplier<Observable<T>> supplier) {
        delegate.<T>execute(instances,
                resultHandler -> supplier.get().subscribe(
                    result -> resultHandler.handle(Future.succeededFuture(result)),
                    throwable -> resultHandler.handle(Future.failedFuture(throwable))),

                result -> {
                    if (result.succeeded()) {
                        subscriber.onNext(result.result());
                        subscriber.onComplete();
                    } else {
                        subscriber.onError(result.cause());
                    }
                });
    }
}
