package org.example.handlers;

import java.util.Objects;

import org.example.server.ShutdownManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import io.vertx.reactivex.core.buffer.Buffer;

import lombok.extern.slf4j.Slf4j;

/**
 * When any of the incoming message event equals to "terminate".
 * It initiates termination of all TCP connections (and not just the sender of the message) and server shutdown.
 * By invoking {@link ShutdownManager#initiateShutdown(int)}, all TCP connections are graciously shutdown as per the
 * spring bean lifecycle and only then the server terminates itself.
 */
@Component
@Slf4j
public class TerminateConnRequestHandler {

    private static final String TERMINATE_MSG = "terminate";
    private static final int NORMAL_EXIT_CODE = 0;

    private final ShutdownManager shutdownManager;

    @Autowired
    public TerminateConnRequestHandler(final ShutdownManager shutdownManager) {
        this.shutdownManager = shutdownManager;
    }

    /**
     * Handlers specifically termination of the server, if the incoming message is "terminate".
     *
     * @param event An instance of {@link Buffer} representing the incoming message.
     */
    public Buffer handle(final Buffer event) {
        Preconditions.checkArgument(Objects.nonNull(event), "Message event cannot be null");
        Preconditions.checkArgument(event.length() > 0, "Message event cannot be empty");

        if (event.toString().equals(TERMINATE_MSG)) {
            log.info("Shutdown Initiated");
            this.shutdownManager.initiateShutdown(NORMAL_EXIT_CODE);
            return Buffer.buffer();
        }
        return event;
    }
}
