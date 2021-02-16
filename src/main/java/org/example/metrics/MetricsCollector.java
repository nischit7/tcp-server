package org.example.metrics;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.reactivex.core.eventbus.Message;

/**
 * Collects the metrics in server memory.
 * It captures the following:
 *   - Count of unique numbers captured in that time interval.
 *   - Count of duplicate numbers captured in that time interval.
 *   - Total count of all unique numbers captured.
 *
 * One has to explicitly call {@link MetricsCollector#resetDuplicateNumCounter()} and
 * {@link MetricsCollector#resetUniqueNumCounter()}, if it needs to recapture those counteres.
 *
 * Since all the metrics is stored in memory (not distributed), all stats will be reset when server is restarted.
 * This metrics collector is registered on {@link io.vertx.core.eventbus.EventBus#localConsumer(String)}.
 * When any produces publishes a event, this metrics collector gets called and the stats are stored.
 *
 * Note that for simplicity, the metrics collector gets registered as a local consumer.
 * In a real world service, this can be distributed across nodes. That means using
 * {@link io.vertx.core.eventbus.EventBus#registerDefaultCodec(Class, MessageCodec)} ), events can be consumed across
 * multiple nodes participating in the cluster. If we enhance the metrics collector to use distributed cache,
 * one will have stats that resembles real world setup.
 */
@Component
public class MetricsCollector implements Handler<Message<String>> {

    private AtomicLong totalUniqueNumCounter = new AtomicLong();
    private AtomicLong uniqueNumCounterForTimePeriod = new AtomicLong();
    private AtomicLong duplicateNumCounter = new AtomicLong();

    /**
     * Returns the address name that can be used when registering in vertx event bus.
     *
     * @return String. Returns Event bus address name
     */
    public static String name() {
        return MetricsCollector.class.getName();
    }

    /**
     * Returns count of total unique numbers.
     *
     * @return int Count of total unique numbers
     */
    public long getTotalUniqueNumsCount() {
        return this.totalUniqueNumCounter.get();
    }

    /**
     * Returns count of unique numbers in a time period.
     *
     * @return int Count of unique numbers in a time period.
     */
    public long getUniqueNumsCountForTimePeriod() {
        return this.uniqueNumCounterForTimePeriod.get();
    }

    /**
     * Returns count of duplicate numbers captured in a time period.
     *
     * @return int
     */
    public long getDuplicateNumCount() {
        return this.duplicateNumCounter.get();
    }

    /**
     * Resets the {@link #uniqueNumCounterForTimePeriod}.
     */
    public void resetUniqueNumCounter() {
        this.uniqueNumCounterForTimePeriod.getAndSet(0);
    }

    /**
     * Resets the {@link #duplicateNumCounter}.
     */
    public void resetDuplicateNumCounter() {
        this.duplicateNumCounter.getAndSet(0);
    }

    /**
     * Handles are published metrics.
     *
     * @param event Incoming event
     */
    @Override
    public void handle(final Message<String> event) {
        Preconditions.checkArgument(Objects.nonNull(event), "Event cannot be null");

        if (EventType.valueOf(event.body()) == EventType.DUPLICATE) {
            this.duplicateNumCounter.incrementAndGet();
        } else if (EventType.valueOf(event.body()) == EventType.NEW) {
            this.totalUniqueNumCounter.incrementAndGet();
            this.uniqueNumCounterForTimePeriod.incrementAndGet();
        }
    }
}
