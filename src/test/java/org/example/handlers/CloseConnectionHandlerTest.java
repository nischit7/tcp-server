package org.example.handlers;

import org.example.model.ConnectionInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloseConnectionHandlerTest {

    @Mock
    private TcpConnectionManager mockTcpConnectionManager;

    @Test
    @DisplayName("When handling close connection event succeeds")
    void whenCloseConnectionSucceeds() {
        final CloseConnectionHandler closeConnectionHandler = new CloseConnectionHandler(this.mockTcpConnectionManager);

        closeConnectionHandler.handle(ConnectionInfo.builder().remotePort(4000).remoteHostName("localhost").build());
        verify(this.mockTcpConnectionManager).remove(any(ConnectionInfo.class));
    }

    @Test
    @DisplayName("Should fail for invalid inputs")
    void whenInputsAreInvalid() {
        final CloseConnectionHandler closeConnectionHandler = new CloseConnectionHandler(this.mockTcpConnectionManager);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            closeConnectionHandler.handle(null);
        });
    }
}
