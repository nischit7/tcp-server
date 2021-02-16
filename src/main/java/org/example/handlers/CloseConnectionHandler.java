package org.example.handlers;

import java.util.Objects;

import org.example.model.ConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import lombok.extern.slf4j.Slf4j;

/**
 * A handler to listen to any connection closer requests.
 * This handler is invoked, if the client closes the connection, network disruption close TCP connection or the
 * server itself closes the connection.
 * It invokes {@link TcpConnectionManager} to remove the connection metadata that caused the closure.
 */
@Component
@Slf4j
public class CloseConnectionHandler {

    private TcpConnectionManager tcpConnectionManager;

    @Autowired
    public CloseConnectionHandler(final TcpConnectionManager tcpConnectionManager) {
        this.tcpConnectionManager = tcpConnectionManager;
    }

    /**
     * A handler to listen to any connection closer requests.
     *
     * @param connectionInfo An instance of {@link ConnectionInfo} representing the connection metadata
     */
    public void handle(final ConnectionInfo connectionInfo) {
        Preconditions.checkArgument(Objects.nonNull(connectionInfo), "Connection info cannot be null");

        log.trace("Connection is being closed for {}", connectionInfo.getConnectionId());
        this.tcpConnectionManager.remove(connectionInfo);
    }
}
