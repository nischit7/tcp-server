package org.example.handlers;

import org.example.server.BaseRuntimeException;

/**
 * This exception is thrown when the TCP connection count exceeds the max allowed by the server. The max number can be
 * validated using {@link TcpConnectionManager#isMaxConnReached()}.
 */
public class MaxTcpConnectionsReachedException extends BaseRuntimeException {

    public MaxTcpConnectionsReachedException(final String msg, final Object...args) {
        super(msg, args);
    }
}
