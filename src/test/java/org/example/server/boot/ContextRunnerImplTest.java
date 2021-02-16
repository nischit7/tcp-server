package org.example.server.boot;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

class ContextRunnerImplTest {
    private ContextRunner contextRunner;

    @BeforeEach
    public void setup() {
        this.contextRunner = new ContextRunnerImpl(Vertx.vertx());
    }

    @Test
    public void success() throws InterruptedException, ExecutionException, TimeoutException {
        final List<String> results = this.contextRunner.executeBlocking(2, handler -> {
            if (Thread.currentThread().getClass().getName().startsWith("io.vertx")) {
                handler.handle(Future.succeededFuture("OK"));
            } else {
                handler.handle(Future.failedFuture("Not on event loop!"));
            }
        }, 10, TimeUnit.MILLISECONDS);
        assertThat(String.join(".", results), equalTo("OK.OK"));
    }

    @Test
    public void failure() throws InterruptedException, ExecutionException, TimeoutException {
        Assertions.assertThrows(ExecutionException.class, () -> {
            this.contextRunner.executeBlocking(2, handler ->
                    handler.handle(Future.failedFuture("Something bad happened")), 100, TimeUnit.MILLISECONDS);
        });
    }
}
