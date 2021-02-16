package org.example.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Holds basic connection information such as remote address, port and handler id.
 * Its only used to track who initiated connection and who disconnected.
 */
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class ConnectionInfo {

    private static final String COLON = ":";

    private String remoteHostName;
    private int remotePort;
    private String writeHandlerId;

    /**
     * Returns the connection id.
     *
     * @return A string respresenting the connection id.
     */
    public String getConnectionId() {
        final StringBuilder connectionId = new StringBuilder();
        connectionId.append(this.remoteHostName);
        connectionId.append(COLON);
        connectionId.append(this.remotePort);
        return connectionId.toString();
    }
}
