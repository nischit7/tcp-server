package org.example.handlers;

import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import io.vertx.reactivex.core.buffer.Buffer;

import lombok.extern.slf4j.Slf4j;

/**
 * It checks for the sanity of the incoming event.
 * Basically if the event is anything other than a 9 digit decimal, it simply rejects them.
 * If the message event is good, it proceeds further.
 */
@Component
@Slf4j
public class BasicMessageSanityHandler {

    private static final Pattern NINE_DIGITS_REGEX = Pattern.compile("^\\d{9}$");

    /**
     * If the incoming message is not a 9 digit decimal, it will close the connection without writing any message to
     * the client and returns to the calling method.
     * If the message is good, it proceeds further.
     *
     * @param event An instance of {@link Buffer}.
     */
    public Buffer handle(final Buffer event) {
        Preconditions.checkArgument(Objects.nonNull(event), "Buffer event cannot be null");

        log.trace("Validating basic sanity of the incoming message {}", event.toString());
        if (!NINE_DIGITS_REGEX.matcher(event.toString()).matches()) {
            throw new InvalidMessageException("Only 9 digit numbers are allowed");
        }
        return event;
    }
}
