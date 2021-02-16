package org.example.handlers;

import org.example.server.ShutdownManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.reactivex.core.buffer.Buffer;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

@ExtendWith(MockitoExtension.class)
class TerminateConnRequestHandlerTest {

    @Mock
    private ShutdownManager mockShutdownManager;

    @Test
    @DisplayName("When connection termination event succeeds")
    void whenConnTerminateEventSuccess() {
        final TerminateConnRequestHandler terminateConnRequestHandler =
                new TerminateConnRequestHandler(this.mockShutdownManager);

        assertThat(terminateConnRequestHandler.handle(Buffer.buffer("terminate")), equalTo(Buffer.buffer()));
        verify(this.mockShutdownManager).initiateShutdown(eq(0));
    }

    @Test
    @DisplayName("When connection termination event succeeds")
    void whenEventNotConnTermination() {
        final TerminateConnRequestHandler terminateConnRequestHandler =
                new TerminateConnRequestHandler(this.mockShutdownManager);

        terminateConnRequestHandler.handle(Buffer.buffer("nonterminate"));
        verify(this.mockShutdownManager, times(0)).initiateShutdown(eq(0));
    }

    @Test
    @DisplayName("Invalid input fails the handle")
    public void whenEventHandlingFailsBecauseOfInvalidInput() {
        final TerminateConnRequestHandler terminateConnRequestHandler =
                new TerminateConnRequestHandler(this.mockShutdownManager);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            terminateConnRequestHandler.handle(Buffer.buffer());
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            terminateConnRequestHandler.handle(null);
        });
    }
}
