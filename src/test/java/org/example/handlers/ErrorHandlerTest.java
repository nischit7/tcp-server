package org.example.handlers;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.reactivex.core.net.NetSocket;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {

    @Mock
    private NetSocket mockNetSocket;

    static Stream<Arguments> multipleExceptions() {
        return Arrays.stream(
            new Arguments[] {
                Arguments.of(new MaxTcpConnectionsReachedException("Max conn"), true),
                Arguments.of(new InvalidMessageException("Invalid msg"), true),
                Arguments.of(new UnableToWriteToFileException("Write error"), false),
                Arguments.of(new DuplicateEventException("Duplicate msg"), false),
                Arguments.of(new RuntimeException("runtime msg"), false),
            });
    }

    @ParameterizedTest
    @MethodSource("multipleExceptions")
    @DisplayName("Test handlers of all exception")
    public void handleMultipleExceptions(final Throwable throwable, final boolean expectToCloseConn) {
        final ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.handle(this.mockNetSocket, throwable);
        if (expectToCloseConn) {
            verify(this.mockNetSocket).close();
        }
    }

    @Test
    @DisplayName("When null net socket is sent")
    public void whenInvalidNetSocketIsSent() {
        final ErrorHandler errorHandler = new ErrorHandler();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            errorHandler.handle(null, new RuntimeException());
        });
    }

    @Test
    @DisplayName("When null throwable is sent")
    public void whenInvalidThrowableIsSent() {
        final ErrorHandler errorHandler = new ErrorHandler();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            errorHandler.handle(this.mockNetSocket, null);
        });
    }
}
