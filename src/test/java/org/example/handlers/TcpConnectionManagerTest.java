package org.example.handlers;

import org.example.model.ConnectionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

class TcpConnectionManagerTest {

    @Test
    @DisplayName("When all connection handling succeeds")
    public void whenConnectionHandlingSucceeds() {
        final TcpConnectionManager tcpConnectionManager = new TcpConnectionManager(2);
        final ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .remoteHostName("localhost2")
                .remotePort(4000)
                .build();
        tcpConnectionManager.add(ConnectionInfo.builder().remoteHostName("localhost1").remotePort(4000).build());
        tcpConnectionManager.add(connectionInfo);

        assertThat(tcpConnectionManager.isMaxConnReached(), equalTo(true));
        tcpConnectionManager.remove(connectionInfo);
        assertThat(tcpConnectionManager.isMaxConnReached(), equalTo(false));
    }
}
