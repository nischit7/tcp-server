package org.example.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsReporterTest {

    @Mock
    private MetricsCollector mockMetricsCollector;

    @Test
    @DisplayName("When it displays metrics report successfully")
    void whenMetricsReportedSuccess() {
        final MetricsReporter metricsReporter = new MetricsReporter(mockMetricsCollector);
        metricsReporter.handle(1000L);

        verify(this.mockMetricsCollector).resetUniqueNumCounter();
        verify(this.mockMetricsCollector).resetDuplicateNumCounter();
    }
}
