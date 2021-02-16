package org.example.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.reactivex.core.eventbus.Message;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

@ExtendWith(MockitoExtension.class)
class MetricsCollectorTest {

    @Mock
    private Message mockMessage;

    private MetricsCollector metricsCollector;

    @BeforeEach
    void beforeEach(final TestInfo info) {

        // Stubbing not needed
        if (info.getTags().contains("whenMetricsCollectedFailsAsInputNull")) {
            return;
        }

        this.metricsCollector = new MetricsCollector();

        when(this.mockMessage.body())
                .thenReturn(EventType.DUPLICATE.name())
                .thenReturn(EventType.DUPLICATE.name())
                .thenReturn(EventType.NEW.name());
    }

    @Test
    @DisplayName("When the handler is invoked to collect metrics successfully")
    void whenMetricsCollectedSuccess() {

        this.metricsCollector.handle(this.mockMessage);
        this.metricsCollector.handle(this.mockMessage);
        this.metricsCollector.handle(this.mockMessage);

        assertThat(this.metricsCollector.getDuplicateNumCount(), equalTo(2L));
        assertThat(this.metricsCollector.getUniqueNumsCountForTimePeriod(), equalTo(1L));
        assertThat(this.metricsCollector.getTotalUniqueNumsCount(), equalTo(1L));
    }

    @Test
    @DisplayName("When the handler is invoked to collect metrics successfully")
    void whenMetricsCollectedResetWorks() {

        this.metricsCollector.handle(this.mockMessage);
        this.metricsCollector.handle(this.mockMessage);
        this.metricsCollector.handle(this.mockMessage);

        assertThat(this.metricsCollector.getDuplicateNumCount(), equalTo(2L));
        assertThat(this.metricsCollector.getUniqueNumsCountForTimePeriod(), equalTo(1L));
        assertThat(this.metricsCollector.getTotalUniqueNumsCount(), equalTo(1L));

        this.metricsCollector.resetDuplicateNumCounter();
        this.metricsCollector.resetUniqueNumCounter();

        assertThat(this.metricsCollector.getDuplicateNumCount(), equalTo(0L));
        assertThat(this.metricsCollector.getUniqueNumsCountForTimePeriod(), equalTo(0L));
        assertThat(this.metricsCollector.getTotalUniqueNumsCount(), equalTo(1L));
    }

    @Test
    @DisplayName("When the handler is invoked to collect metrics successfully")
    @Tag("whenMetricsCollectedFailsAsInputNull")
    void whenMetricsCollectedFailsAsInputNull() {
        final MetricsCollector metricsCollect = new MetricsCollector();
        assertThrows(IllegalArgumentException.class, () -> {
            metricsCollect.handle(null);
        });
    }
}
