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
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.reactivex.core.buffer.Buffer;

@ExtendWith(MockitoExtension.class)
class BasicMessageSanityHandlerTest {

    static Stream<Arguments> invalidMessages() {
        return Arrays.stream(
            new Arguments[] {
                Arguments.of(Buffer.buffer()),
                Arguments.of(Buffer.buffer(" ")),
                Arguments.of(Buffer.buffer("123")),
                Arguments.of(Buffer.buffer("ABCDEFGHI")),
                Arguments.of(Buffer.buffer("ABC*&FGHI")),
            });
    }

    static Stream<Arguments> validMessages() {
        return Arrays.stream(
            new Arguments[] {
                Arguments.of(Buffer.buffer("123456789")),
                Arguments.of(Buffer.buffer("000000001")),
                Arguments.of(Buffer.buffer("823459245")),
            });
    }

    @ParameterizedTest
    @MethodSource("invalidMessages")
    @DisplayName("When inputs are invalid, it should stop processing")
    public void whenHandlingInvalidMessagesStopProcessing(final Buffer buffer) {

        final BasicMessageSanityHandler basicMessageSanityHandler = new BasicMessageSanityHandler();

        Assertions.assertThrows(InvalidMessageException.class, () -> {
            basicMessageSanityHandler.handle(buffer);
        });
    }

    @ParameterizedTest
    @MethodSource("validMessages")
    @DisplayName("When inputs are valid, it should forward for further processing")
    public void whenHandlingSucceedsForValidMsgs(final Buffer buffer) {

        final BasicMessageSanityHandler basicMessageSanityHandler = new BasicMessageSanityHandler();

        basicMessageSanityHandler.handle(buffer);
    }

    @Test
    @DisplayName("When inputs are null")
    public void whenInputsAreNull() {
        final BasicMessageSanityHandler basicMessageSanityHandler = new BasicMessageSanityHandler();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            basicMessageSanityHandler.handle(null);
        });
    }
}
