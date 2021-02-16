package org.example.server;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.example.handlers.TcpConnectionHandler;
import org.example.metrics.MetricsCollector;
import org.example.metrics.MetricsReporter;
import org.example.server.boot.ContextRunnerImpl;
import org.example.server.boot.ContextRunnerRx;
import org.example.server.boot.ContextRunnerRxImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.reactivex.Observable;
import io.vertx.core.Handler;
import io.vertx.core.file.OpenOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.file.AsyncFile;
import io.vertx.reactivex.core.net.NetServer;

import lombok.extern.slf4j.Slf4j;

/**
 * The main start up class invoked when spring starts loading the context.
 * It starts a TCP server sock at port defined by {@link #tcpPort}.
 *
 * It registers {@link MetricsCollector} with vertx {@link io.vertx.core.eventbus.EventBus#localConsumer(String)}. This allows one to publish
 * event specific metadata asynchronously.
 *
 * It also registers {@link MetricsReporter} with {@link io.vertx.core.Vertx#setPeriodic(long, Handler)}. This allows
 * {@link MetricsReporter} to print the statistics periodically as defined by {@link #metricsReportIntervalSecs}.
 *
 * It adds {@link TcpConnectionHandler} as the main handler for all incoming connections.
 * It used {@link io.vertx.reactivex.core.file.FileSystem} to write into the file.
 * It attaches the error handlers for each of the components used.
 */
@Component
@Slf4j
public class TCPServer {

    private static final int MAX_SERVER_START_TIME_MINS = 5;

    private final Vertx vertx;
    private final EventBus eventBus;
    private final TcpConnectionHandler tcpConnectionHandler;
    private final MetricsCollector metricsCollector;
    private final MetricsReporter metricsReporter;
    private final int metricsReportIntervalSecs;
    private final int tcpPort;
    private final int numberOfServerInstances;
    private final String destAbsoluteFilePath;

    @Autowired
    public TCPServer(
            final Vertx vertx,
            final EventBus eventBus,
            final TcpConnectionHandler tcpConnectionHandler,
            final MetricsCollector metricsCollector,
            final MetricsReporter metricsReporter,
            @Value("${metrics.report.interval.in.ms:10000}") final int metricsReportIntervalSecs,
            @Value("${server.port:4000}") final int tcpPort,
            @Value("${num.of.server.instances:16}") final int numberOfServerInstances,
            @Value("${dest.file.absolute.path:/tmp/numbers.log}") final String destAbsoluteFilePath) {

        this.vertx = vertx;
        this.eventBus = eventBus;
        this.tcpConnectionHandler = tcpConnectionHandler;
        this.metricsCollector = metricsCollector;
        this.metricsReporter = metricsReporter;
        this.metricsReportIntervalSecs = metricsReportIntervalSecs;
        this.tcpPort = tcpPort;
        this.numberOfServerInstances = numberOfServerInstances;
        this.destAbsoluteFilePath = destAbsoluteFilePath;
    }
    /**
     * As described above, it initializes a TCP server at the given port, registers a metrics collector and metrics
     * reporter. Above all, it registers a handler to tackle all incoming connections.
     */
    @PostConstruct
    public void initializeTcpListener() throws InterruptedException, ExecutionException, TimeoutException, IOException {

        // Make sure one has write access, cleanup file during startup.
        if (!Files.exists(Paths.get(this.destAbsoluteFilePath).getParent())) {
            throw new IllegalStateException(String.format(
                "Destination file where messages will be written is not defined %s", this.destAbsoluteFilePath));
        }
        Files.deleteIfExists(Paths.get(this.destAbsoluteFilePath));
        FileChannel.open(Paths.get(this.destAbsoluteFilePath),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)
                .close();

        log.info("All messages will written to file sitting at {}", this.destAbsoluteFilePath);

        final AsyncFile asyncFile = this.vertx.fileSystem().openBlocking(this.destAbsoluteFilePath,
                new OpenOptions().setAppend(true).setWrite(true).setCreate(true));

        asyncFile.exceptionHandler(event -> {
            log.error("Error while writing into file", event.getCause());
        });

        final ContextRunnerRx contextRunnerRx = new ContextRunnerRxImpl(
                new ContextRunnerImpl((io.vertx.core.Vertx)this.vertx.getDelegate()));
        contextRunnerRx.executeBlocking(
                this.numberOfServerInstances,
                () -> this.createNetServer(asyncFile),
                MAX_SERVER_START_TIME_MINS, TimeUnit.MINUTES);

        // Attach the metrics collector to the event bus
        this.eventBus.localConsumer(MetricsCollector.name(), this.metricsCollector:: handle);
        this.vertx.setPeriodic(this.metricsReportIntervalSecs, this.metricsReporter);

        log.info("Server is running at {}", this.tcpPort);
    }

    private Observable<NetServer> createNetServer(final AsyncFile asyncFile) {
        return vertx.createNetServer()
            .connectHandler(netSocket -> this.tcpConnectionHandler.handle(netSocket, asyncFile))
            .rxListen(this.tcpPort)
            .toObservable();
    }
}
