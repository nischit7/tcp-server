package org.example.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Handler;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.core.net.NetSocket;
import io.vertx.reactivex.core.net.SocketAddress;
import io.vertx.reactivex.core.parsetools.RecordParser;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TcpConnectionHandlerTest {

    @Mock
    private CloseConnectionHandler mockCloseConnectionHandler;

    @Mock
    private ErrorHandler mockErrorHandler;

    @Mock
    private TcpConnectionManager mockTcpConnectionManager;

    @Mock
    private BasicMessageSanityHandler mockBasicMessageSanityHandler;

    @Mock
    private DeduplicationHandler mockDeduplicationHandler;

    @Mock
    private TerminateConnRequestHandler mockTerminateConnRequestHandler;

    @Mock
    private NetSocket mockNetSocket;

    @Mock
    private SocketAddress mockSocketAddress;

    @Mock
    private AsyncFile mockAsyncFile;

    @Mock
    private Vertx mockVertx;

    private TcpConnectionHandler tcpConnectionHandler;

    private String destinationPath;

    @BeforeEach
    public void beforeEach(final TestInfo testInfo) throws IOException {

        final Optional<String> targetBuildDir = Optional.ofNullable(System.getProperty("build.dir"));
        this.destinationPath = targetBuildDir.orElseThrow(() -> new RuntimeException()) + "/" + "numbers.log";
        Files.deleteIfExists(Paths.get(this.destinationPath));

        this.tcpConnectionHandler = new TcpConnectionHandler(
            this.mockCloseConnectionHandler,
            this.mockErrorHandler,
            this.mockTcpConnectionManager,
            this.mockBasicMessageSanityHandler,
            this.mockDeduplicationHandler,
            this.mockTerminateConnRequestHandler,
            this.mockVertx,
            this.destinationPath);

        if (testInfo.getTags().contains("handleTcpConnFailsForNullInput")) {
            return;
        }

        when(this.mockNetSocket.remoteAddress()).thenReturn(this.mockSocketAddress);
        when(this.mockSocketAddress.host()).thenReturn("localhost");
        when(this.mockSocketAddress.port()).thenReturn(4000);

        if (testInfo.getTags().contains("handleTcpConnFailsWhenMaxReaches")) {
            return;
        }

        when(this.mockNetSocket.closeHandler(any(Handler.class))).thenReturn(this.mockNetSocket);
        when(this.mockNetSocket.exceptionHandler(any(Handler.class))).thenReturn(this.mockNetSocket);
    }

    @Test
    @DisplayName("When tcp connection is handled successfully")
    void handleTcpConnSuccess() {
        when(this.mockAsyncFile.getDelegate()).thenReturn(Mockito.mock(io.vertx.core.file.AsyncFile.class));
        this.tcpConnectionHandler.handle(this.mockNetSocket, this.mockAsyncFile);

        verify(this.mockNetSocket, times(0)).close();
        verify(this.mockNetSocket).closeHandler(any());
        verify(this.mockNetSocket).exceptionHandler(any());
    }

    @Test
    @Tag("handleTcpConnFailsWhenMaxReaches")
    @DisplayName("When max tcp connection is reached, connectio should be rejected")
    void handleTcpConnFailsWhenMaxReaches() {
        when(this.mockTcpConnectionManager.isMaxConnReached()).thenReturn(true);

        Assertions.assertThrows(MaxTcpConnectionsReachedException.class, () -> {
            this.tcpConnectionHandler.handle(this.mockNetSocket, this.mockAsyncFile);
            verify(this.mockNetSocket, times(1)).close();
            verify(this.mockNetSocket, times(0)).handler(any(RecordParser.class));
            verify(this.mockNetSocket, times(0)).closeHandler(any());
            verify(this.mockNetSocket, times(0)).exceptionHandler(any());
        });
    }

    @Test
    @DisplayName("Fails for invalid input")
    @Tag("handleTcpConnFailsForNullInput")
    void handleTcpConnFailsForNullInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            this.tcpConnectionHandler.handle(null, this.mockAsyncFile);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            this.tcpConnectionHandler.handle(this.mockNetSocket, null);
        });
    }
}
