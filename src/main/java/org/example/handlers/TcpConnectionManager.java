package org.example.handlers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.example.model.ConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * A simple helper class to maintain connection metadata information.
 * Its mainly used to track how many connections have been provided by the server.
 */
@Component
@Slf4j
public class TcpConnectionManager {

    private final Map<String, ConnectionInfo> connectionMap = new ConcurrentHashMap<>();

    private int maxAllowedTcoConnections;

    @Autowired
    public TcpConnectionManager(@Value("${max.allowed.tcp.connections:5}") final int maxAllowedTcoConnections) {
        this.maxAllowedTcoConnections = maxAllowedTcoConnections;
    }

    /**
     * Returns {@code true} if max connections have reached.
     *
     * @return boolean
     */
    public boolean isMaxConnReached() {
        return this.connectionMap.size() >= this.maxAllowedTcoConnections;
    }

    /**
     * Adds the connection info.
     *
     * @param connectionInfo An instance of {@link ConnectionInfo}.
     */
    public void add(final ConnectionInfo connectionInfo) {
        this.connectionMap.put(connectionInfo.getConnectionId(), connectionInfo);
    }

    /**
     * Removes the connection info.
     *
     * @param connectionInfo An instance of {@link ConnectionInfo}.
     */
    public void remove(final ConnectionInfo connectionInfo) {
        this.connectionMap.remove(connectionInfo.getConnectionId());
    }
}
