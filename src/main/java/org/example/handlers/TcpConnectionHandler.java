package org.example.handlers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.example.model.ConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.core.net.NetSocket;
import io.vertx.reactivex.core.parsetools.RecordParser;
import io.vertx.reactivex.core.streams.Pump;

import lombok.extern.slf4j.Slf4j;

/**
 * A handler to tackle incoming server TCP connections.
 * This handler is invoked, whenever is TCP connection is made.
 * It checks if the max TCP connections represented by {@link TcpConnectionManager#isMaxConnReached()} is satisfied.
 * If so, it immediately terminates that connection.
 *
 * Since incoming messages could be streamed, we need a proper mechanism which can asynchronously separate each events.
 * That is, lets say the incoming buffer(events or messages) is streaming and delimited by '\n' and the input was the
 * following:
 * <p>
 * <pre>
 * buffer1:HELLO\nHOW ARE Y
 * buffer2:OU?\nI AM
 * buffer3: DOING OK
 * buffer4:\n
 * </pre>
 * For us to process, the output needs to be:<p>
 * <pre>
 * buffer1:HELLO
 * buffer2:HOW ARE YOU?
 * buffer3:I AM DOING OK
 * </pre>
 *
 * This is achieved by using {@link RecordParser}. Refer its documentation for more details.
 * It also makes use of Vertx reactive stream, to pump the incoming messages, handle backpressure, reduce data loss.
 * Refer {@link Pump} and {@link Flowable} for more details.
 *
 * In addition, this handler attaches seperate handlers for exception event, connection closure event.
 *
 */
@Slf4j
@Component
public class TcpConnectionHandler {

    private static final String NEWLINE = "\n";

    // This number is optimized to achieve 2M events in 10 secs
    private static final int MAX_WRITE_QUEUE_SIZE = 20480 * 1024;
    private static final int TIME_INTERVAL_PUMP_STATS = 10000;
    private static final AtomicLong EVENT_COUNT = new AtomicLong();

    private final CloseConnectionHandler closeConnectionHandler;
    private final ErrorHandler errorHandler;
    private final TcpConnectionManager tcpConnectionManager;

    private final BasicMessageSanityHandler basicMessageSanityHandler;
    private final DeduplicationHandler deduplicationHandler;
    private final TerminateConnRequestHandler terminateConnRequestHandler;
    private final Vertx vertx;

    @Value("${enable.additional.stats:false}")
    private boolean enableAdditionalStats;

    @Autowired
    public TcpConnectionHandler(
            final CloseConnectionHandler closeConnectionHandler,
            final ErrorHandler errorHandler,
            final TcpConnectionManager tcpConnectionManager,
            final BasicMessageSanityHandler basicMessageSanityHandler,
            final DeduplicationHandler deduplicationHandler,
            final TerminateConnRequestHandler terminateConnRequestHandler,
            final Vertx vertx,
            @Value("${dest.file.absolute.path:/tmp/numbers.log}") final String destAbsoluteFilePath) {

        this.closeConnectionHandler = closeConnectionHandler;
        this.errorHandler = errorHandler;
        this.tcpConnectionManager = tcpConnectionManager;
        this.basicMessageSanityHandler = basicMessageSanityHandler;
        this.deduplicationHandler = deduplicationHandler;
        this.terminateConnRequestHandler = terminateConnRequestHandler;
        this.vertx = vertx;
    }

    /**
     * Handles incoming connections.
     *
     * @param connectionEvent connectionEvent
     * @param asyncFile AsyncFile
     */
    public void handle(final NetSocket connectionEvent, final AsyncFile asyncFile) {
        Preconditions.checkArgument(Objects.nonNull(connectionEvent), "Netsocket event cannot be null");
        Preconditions.checkArgument(Objects.nonNull(asyncFile), "Async file cannot be null");

        final AtomicLong eventCountForConn = new AtomicLong();

        final ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .remoteHostName(connectionEvent.remoteAddress().host())
                .remotePort(connectionEvent.remoteAddress().port())
                .writeHandlerId(connectionEvent.writeHandlerID())
                .build();

        log.trace("A connection has been initiated by :{}", connectionInfo.getConnectionId());
        if (tcpConnectionManager.isMaxConnReached()) {
            log.error("Max connection reached. Closing connection {}", connectionInfo.getConnectionId());
            connectionEvent.close();
            throw new MaxTcpConnectionsReachedException(String.format("Max active connections of reached"));
        }
        this.tcpConnectionManager.add(connectionInfo);

        // Attach all the required handlers.
        connectionEvent
                .closeHandler(closeEvent -> this.closeConnectionHandler.handle(connectionInfo))
                .exceptionHandler(errorEvent -> this.errorHandler.handle(connectionEvent, errorEvent));

        /*
         * It parses the incoming buffer and stream lines it in a manner so that one can consume each unqiue message
         * separately.
         */
        final RecordParser parser = RecordParser.newDelimited("\n", connectionEvent);

        // Apply all the required filters, so that only validated messages flow through.
        final Flowable flowable = parser.toFlowable()
                .map(this.terminateConnRequestHandler::handle)
                .map(this.basicMessageSanityHandler::handle)
                .map(this.deduplicationHandler::handle)
                .filter(this::isNonEmptyBuffer)
                .map(buffer -> buffer.getDelegate())
                .map(buffer -> io.vertx.core.buffer.Buffer.buffer().appendBuffer(buffer).appendString(NEWLINE))
                .doOnError(throwable -> errorHandler.handle(connectionEvent, throwable))
                .filter(buffer -> this.isQueueDrained(asyncFile, parser))
                .map(buffer -> {
                    EVENT_COUNT.incrementAndGet();
                    eventCountForConn.incrementAndGet();
                    return buffer;
                });

        // Start streaming the events from TCP socket to the file.
        final Pump pump = Pump.pump(flowable, asyncFile);
        pump.setWriteQueueMaxSize(MAX_WRITE_QUEUE_SIZE);
        pump.start();

        // Lets print some stats in the console to correlate the numbers
        if (this.enableAdditionalStats) {
            this.vertx.setPeriodic(TIME_INTERVAL_PUMP_STATS, event -> {
                log.info("Event count so far before sending to write stream {}", EVENT_COUNT.get());
            });

            this.vertx.setPeriodic(TIME_INTERVAL_PUMP_STATS, event -> {
                log.info("Event count so far for connection {} before sending to write stream {}", connectionInfo.getConnectionId(), eventCountForConn.get());
            });

            this.vertx.setPeriodic(TIME_INTERVAL_PUMP_STATS, event -> {
                log.info("Pumped event count for client connection {} to write stream so far {}", connectionInfo.getConnectionId(), pump.numberPumped());
            });
        }
    }

    private boolean isQueueDrained(final AsyncFile asyncFile, final RecordParser parser) {
        if (asyncFile.writeQueueFull()) {
            log.info("Seems like write queue is full... pausing");
            parser.pause();
            asyncFile.drainHandler(ev -> {
                log.info("Resuming now....");
                parser.resume();
            });
        }
        return true;
    }

    private boolean isNonEmptyBuffer(final Buffer buffer) {
        return buffer.length() > 0;
    }
}
