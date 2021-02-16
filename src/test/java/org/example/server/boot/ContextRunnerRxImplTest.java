package org.example.server.boot;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.reactivex.Observable;
import io.vertx.core.Vertx;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

@Disabled
class ContextRunnerRxImplTest {
    private ContextRunnerRx contextRunner;

    @BeforeEach
    public void setup() {
        contextRunner = new ContextRunnerRxImpl(new ContextRunnerImpl(Vertx.vertx()));
    }

    @Test
    public void success() throws InterruptedException, ExecutionException, TimeoutException {
        final List<String> results = contextRunner.executeBlocking(2, () -> {
            if (Thread.currentThread().getClass().getName().startsWith("io.vertx")) {
                return Observable.just("OK");
            } else {
                return Observable.error(new RuntimeException("Not on event loop!"));
            }
        }, 10, TimeUnit.MILLISECONDS);
        assertThat(String.join(".", results), equalTo("OK.OK"));
    }

    @Test
    public void failure() throws InterruptedException, ExecutionException, TimeoutException {
        Assertions.assertThrows(ExecutionException.class, () -> {
            contextRunner.executeBlocking(2, () -> Observable.error(new RuntimeException()), 10, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    public void whenContexttimesout() throws InterruptedException, ExecutionException, TimeoutException {
        Assertions.assertThrows(TimeoutException.class, () -> {
            contextRunner.executeBlocking(1, () -> Observable.empty(), 10, TimeUnit.MILLISECONDS);
        });
    }
}
