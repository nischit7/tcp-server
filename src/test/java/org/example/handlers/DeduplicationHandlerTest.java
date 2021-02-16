package org.example.handlers;

import org.example.metrics.EventType;
import org.example.metrics.MetricsCollector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.eventbus.EventBus;

import lombok.extern.slf4j.Slf4j;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DeduplicationHandlerTest {

    @Captor
    ArgumentCaptor<String> metricCollectorNameCaptor;

    @Captor
    ArgumentCaptor<String> metricPayloadCaptor;

    @Mock
    private EventBus mockEventBus;

    @Test
    @DisplayName("When deduplication succeeds")
    void whenDeDuplicationSuccess() {

        final DeduplicationHandler deduplicationHandler = new DeduplicationHandler(
                this.mockEventBus, 10);
        final String duplicateMsg = "sameMsg1";
        assertThat(deduplicationHandler.handle(Buffer.buffer(duplicateMsg)), equalTo(Buffer.buffer(duplicateMsg)));
        assertThat(deduplicationHandler.handle(Buffer.buffer("diffMsg1")), equalTo(Buffer.buffer("diffMsg1")));
        assertThat(deduplicationHandler.handle(Buffer.buffer(duplicateMsg)), equalTo(Buffer.buffer()));

        verify(this.mockEventBus, times(3)).publish(this.metricCollectorNameCaptor.capture(), this.metricPayloadCaptor.capture());

        assertThat(this.metricCollectorNameCaptor.getAllValues()
                .stream()
                .allMatch(captor -> MetricsCollector.name().equals(captor)), equalTo(true));

        // First two occurrences will be unique
        assertThat(EventType.valueOf(
                metricPayloadCaptor.getAllValues().get(0)),
                equalTo(EventType.NEW));

        assertThat(EventType.valueOf(
                metricPayloadCaptor.getAllValues().get(1)),
                equalTo(EventType.NEW));

        // This will be a duplicate event
        assertThat(EventType.valueOf(
                metricPayloadCaptor.getAllValues().get(2)),
                equalTo(EventType.DUPLICATE));
    }

    @Test
    @DisplayName("When the same event is sent after expiry")
    void whenTheSameEventCanBeSentAfterExpiry() throws InterruptedException {
        log.info("Sleeping for sometime to allow for event expiry.....");
        final int expiryTimeSecs = 10;

        final DeduplicationHandler deduplicationHandler = new DeduplicationHandler(
                this.mockEventBus, expiryTimeSecs);
        final String duplicateMsg = "sameMsg1";
        assertThat(deduplicationHandler.handle(Buffer.buffer(duplicateMsg)), equalTo(Buffer.buffer(duplicateMsg)));

        Thread.sleep(1000);
        Thread.sleep((expiryTimeSecs + 10) * 1000);
        assertThat(deduplicationHandler.handle(Buffer.buffer(duplicateMsg)), equalTo(Buffer.buffer(duplicateMsg)));

        verify(this.mockEventBus, times(2)).publish(this.metricCollectorNameCaptor.capture(), this.metricPayloadCaptor.capture());

        assertThat(this.metricCollectorNameCaptor.getAllValues()
                .stream()
                .allMatch(captor -> MetricsCollector.name().equals(captor)), equalTo(true));

        // Both occurrences will be published as unique events
        assertThat(metricPayloadCaptor.getAllValues()
                .stream().map(captor -> EventType.valueOf(captor))
                .allMatch(eventMetadata -> eventMetadata == EventType.NEW), equalTo(true));
    }

    @Test
    @DisplayName("When inputs are invalid")
    public void whenHandlingFailsForInvalidInputs() {
        final DeduplicationHandler deduplicationHandler = new DeduplicationHandler(
                this.mockEventBus, 10);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            deduplicationHandler.handle(null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            deduplicationHandler.handle(Buffer.buffer());
        });
    }
}
