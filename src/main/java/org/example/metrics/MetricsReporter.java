/**
 * Open Source. Apache 2.0
 */
package org.example.metrics;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;

import lombok.extern.slf4j.Slf4j;

/**
 * Reports the metrics to standard out on a periodic basis.
 * It prints:
 *   - Count of unique numbers captured in that time interval.
 *   - Count of duplicate numbers captured in that time interval.
 *   - Total count of all unique numbers captured.
 *
 * All the metrics reported are stored in server memory.
 * After printing the stats, it resets the counters meant for that interval.
 */
@Component
@Slf4j
public class MetricsReporter implements Handler<Long> {

    private final MetricsCollector metricsCollector;

    public MetricsReporter(final MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void handle(final Long event) {
        log.info("Received {} unique numbers, {} duplicates. Unique total: {}",
                this.metricsCollector.getUniqueNumsCountForTimePeriod(),
                this.metricsCollector.getDuplicateNumCount(),
                this.metricsCollector.getTotalUniqueNumsCount());

        this.metricsCollector.resetDuplicateNumCounter();
        this.metricsCollector.resetUniqueNumCounter();
    }
}
