package org.example.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.example.server.BaseRuntimeException;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import io.vertx.reactivex.core.net.NetSocket;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.isNull;

/**
 * An error handler that can be attached to any exception handler needed.
 * That means, if one wants to have a single handler during connection errors, message parsing errors and so on.
 * Since this service is small, a single error handler is sufficient for now.
 * As the functionality increases, if there is need for multiple error handlers, it can be achieved too.
 */
@Component
@Slf4j
public class ErrorHandler {

    private Map<Class<? extends BaseRuntimeException>, BiConsumer<BaseRuntimeException, NetSocket>> excepMapper = new HashMap<>();

    public ErrorHandler() {
        excepMapper.put(MaxTcpConnectionsReachedException.class, this::handleMaxTcp);
        excepMapper.put(InvalidMessageException.class, this::handleInvalidMsg);
        excepMapper.put(UnableToWriteToFileException.class, this::handleUnableToWrite);
        excepMapper.put(DuplicateEventException.class, this::handleDuplicateEvent);
    }

    /**
     * Based on the exception, it invokes the appropriate error handler.
     *
     * @param netSocket An instance of {@link NetSocket}.
     * @param throwable A throwable
     */
    public void handle(final NetSocket netSocket, final Throwable throwable) {
        Preconditions.checkArgument(Objects.nonNull(netSocket), "Net socket cannot be null");
        Preconditions.checkArgument(Objects.nonNull(throwable), "Throwable cannot be null");

        final Optional<BaseRuntimeException> rootCauseBaseEx = getCustomRootCause(throwable);
        if (rootCauseBaseEx.isPresent()) {
            this.excepMapper.get(rootCauseBaseEx.get().getClass())
                    .accept(rootCauseBaseEx.get(), netSocket);
        } else if (throwable instanceof IOException) {
            /*
             * "Connection reset by peer" is one of the common errors, especially when it involves TCP connection
             * event streaming. We will suppress the unwanted noise unless needed.
             */
            log.trace("Connection reset", throwable);
        } else {
            this.handleUnknown(throwable, netSocket);
        }
    }

    private void handleUnknown(final Throwable throwable, final NetSocket netSocket) {
        log.error("An unhandled exception was thrown", throwable);
        netSocket.end();
    }

    private void handleMaxTcp(final BaseRuntimeException exp, final NetSocket netSocket) {
        log.error("Handling max connections reached", exp);
        netSocket.close();
    }

    private void handleInvalidMsg(final BaseRuntimeException exp, final NetSocket netSocket) {
        log.trace("Invalid message sent", exp);
        netSocket.close();
    }

    private void handleUnableToWrite(final BaseRuntimeException exp, final NetSocket netSocket) {
        log.error("Unable to write into file", exp);
    }

    private void handleDuplicateEvent(final BaseRuntimeException exp, final NetSocket netSocket) {
        log.error("Duplicate message sent", exp);
    }

    private Optional<BaseRuntimeException> getCustomRootCause(final Throwable throwable) {
        final Optional<BaseRuntimeException> baseExOpt;

        if (isNull(throwable)) {
            baseExOpt = Optional.empty();
        } else if (throwable instanceof BaseRuntimeException) {
            baseExOpt = Optional.of((BaseRuntimeException)throwable);
        } else {
            baseExOpt = getCustomRootCause(throwable.getCause());
        }
        return baseExOpt;
    }
}
