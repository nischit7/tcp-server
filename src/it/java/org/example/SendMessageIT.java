package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.net.NetClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.net.NetClient;
import io.vertx.reactivex.core.net.NetSocket;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

/**
 * Integration tests that spins the TCP server, invokes TCP connection, sends messages and validates them.
 * Each test will ensure, the content written to the destination file contains the expected messages.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "spring.config.location=classpath:application-it.properties",
        classes = SimpleTCPApplication.class)
@EnableAutoConfiguration
@Slf4j
public class SendMessageIT {

    private static final String NEWLINE = "\n";

    @Value("${tcp.dest.server.name:127.0.0.1}")
    private String destHostName;

    @Value("${tcp.dest.server.port:4000}")
    private int destHostPort;

    @Value("${connection.timeout.in.ms:10000}")
    private int connctionTimeout;

    @Value("${max.reconnection.attempts:10}")
    private int maxReconAttempts;

    @Value("${max.reconnection.attempt.interval:500}")
    private int maxReconAttemptInterval;

    @Value("${dest.file.absolute.path}")
    private String destAbsoluteFilePath;

    /**
     * A simple test case to connect to the server, send a message and disconnect.
     */
    @DisplayName("Send a successful single message from a single client")
    @Test
    public void sendSingleMsgSuccess() {

        final Vertx vertx = Vertx.vertx();
        final NetClient client = getNetClient(vertx);
        final String msg = "" + RandomGenerator.randomInt();
        final List<NetSocket> openConnections = new ArrayList<>();
        final String response = vertx.<String>rxExecuteBlocking(event -> {
            client.connect(this.destHostPort, this.destHostName, res -> {
                if (res.succeeded()) {
                    final NetSocket socket = res.result();
                    log.info("Message to write {}", msg);
                    socket.write(msg + NEWLINE);
                    event.complete(msg);
                    openConnections.add(socket);
                } else {
                    event.fail(res.cause());
                }
            });
        }).blockingGet();

        SendMessageIT.sleep(2000);
        openConnections.forEach(netSocket -> netSocket.close());
        SendMessageIT.sleep(2000);
        assertThat(response, equalTo(msg));
    }

    /**
     * Connect to the server and send an invalid message.
     * Asserts that the server has disconnected the client connection.
     */
    @DisplayName("Send a invalid single message from single client and get connection closed")
    @Test
    public void sendSingleInvalidMsgFail() {

        final Vertx vertx = Vertx.vertx();
        final NetClient client = getNetClient(vertx);
        final String msg = "invalidmsg";
        final String response = vertx.<String>rxExecuteBlocking(event -> {
            client.connect(this.destHostPort, this.destHostName, res -> {
                if (res.succeeded()) {
                    final NetSocket socket = res.result();
                    socket.write(msg + NEWLINE);
                    socket.closeHandler(closeEvent -> {
                        // Complete the future only when disconnect happens
                        event.complete(msg);
                    });
                } else {
                    event.fail(res.cause());
                }
            });
        }).blockingGet();
        assertThat(response, equalTo(msg));
    }

    /**
     * As a single client, send series of messages.
     * Validates if all the messages are written to numbers.log
     *
     * @throws IOException For file handling issues
     */
    @DisplayName("Send multiple messages from a single client are accepted successfully")
    @Test
    public void sendMultipleMessagesSingleClientSuccess() throws IOException {

        //FileChannel.open(Paths.get(this.destAbsoluteFilePath), StandardOpenOption.WRITE).truncate(0).close();

        final Vertx vertx = Vertx.vertx();
        final NetClient client = getNetClient(vertx);
        final List<String> msgList = generateRandomNum(5);
        final String expectedMsg = "success";
        final List<NetSocket> openConnections = new ArrayList<>();
        final String response = vertx.<String>rxExecuteBlocking(event -> {
            client.connect(this.destHostPort, this.destHostName, res -> {
                if (res.succeeded()) {
                    final NetSocket socket = res.result();
                    for (String msg: msgList) {
                        socket.write(msg + NEWLINE);
                    }
                    event.complete(expectedMsg);
                    openConnections.add(socket);
                } else {
                    event.fail(res.cause());
                }
            });
        }).blockingGet();

        SendMessageIT.sleep(5000);

        // Ensure all the messages are written to file
        assertThat(Files.readAllLines(Paths.get(this.destAbsoluteFilePath))
                .containsAll(msgList), equalTo(true));

        openConnections.forEach(netSocket -> netSocket.close());
        SendMessageIT.sleep(2000);
        assertThat(response, equalTo(expectedMsg));
    }

    /**
     * As a single client, send a mix of unique messages and duplicate messages.
     * Validates only the unique messages are written to numbers.log
     *
     * @throws IOException For file handling issues
     */
    @DisplayName("Send multiple duplicate messages from a single client")
    @Test
    public void sendMultipleDuplicateMessagesSingleClientSuccess() throws IOException {

        //FileChannel.open(Paths.get(this.destAbsoluteFilePath), StandardOpenOption.WRITE).truncate(0).close();

        final Vertx vertx = Vertx.vertx();
        final NetClient client = getNetClient(vertx);
        final int duplicateMsg = RandomGenerator.randomInt();
        final List<String> randomNums = generateRandomNum(5);
        final List<String> msgList = new ArrayList<>();
        msgList.add("" + duplicateMsg);
        msgList.addAll(randomNums);
        msgList.add("" + duplicateMsg);

        final String expectedMsg = "success";
        final List<NetSocket> openConnections = new ArrayList<>();
        final String response = vertx.<String>rxExecuteBlocking(event -> {
            client.connect(this.destHostPort, this.destHostName, res -> {
                if (res.succeeded()) {
                    final NetSocket socket = res.result();
                    for (String msg: msgList) {
                        log.info("Message to write {}", msg);
                        socket.write(msg + NEWLINE);
                    }
                    event.complete(expectedMsg);
                    openConnections.add(socket);
                } else {
                    event.fail(res.cause());
                }
            });
        }).blockingGet();

        SendMessageIT.sleep(5000);

        // Ensure all the messages are written to file
        // Remove the last duplicate element
        msgList.remove(msgList.size() - 1);
        assertThat(Files.readAllLines(Paths.get(this.destAbsoluteFilePath))
                .containsAll(msgList), equalTo(true));

        openConnections.forEach(netSocket -> netSocket.close());
        SendMessageIT.sleep(2000);
        assertThat(response, equalTo(expectedMsg));
    }

    /**
     * Creates upto 5 TCP clients.
     * Each client will trigger a message.
     * Validates if all the messages are written to numbers.log
     *
     * @throws IOException For file handling issues
     */
    @DisplayName("Connect multiple clients upto max connections allowed and succeed in sending messages")
    @Test
    public void whenMultipleClientsUptoMaxConnectionSuccess() throws IOException {

        //FileChannel.open(Paths.get(this.destAbsoluteFilePath), StandardOpenOption.WRITE).truncate(0).close();

        final Vertx vertx = Vertx.vertx();
        final NetClient client = getNetClient(vertx);
        final List<String> msgList = generateRandomNum(5);
        final String expectedMsg = "success";
        final List<NetSocket> openConnections = new ArrayList<>();

        // Adding some delay intentionally to allow all messages to finish
        final Single<List<String>> responsesObservableList = Observable.fromArray(msgList.toArray(new String[0]))
                .flatMap(msg -> vertx.<String>rxExecuteBlocking(event -> {
                    client.connect(this.destHostPort, this.destHostName, res -> {
                        if (res.succeeded()) {
                            final NetSocket socket = res.result();
                            socket.write(msg + NEWLINE);
                            event.complete(expectedMsg);
                            openConnections.add(socket);
                        } else {
                            event.fail(res.cause());
                        }
                    });
                }).toObservable()).delay(2000, TimeUnit.MILLISECONDS)
                        .toList();

        assertThat(responsesObservableList.blockingGet()
                .stream()
                .allMatch(res -> res.equals(expectedMsg)),
                equalTo(true));

        // Ensure all the messages are written to file
        SendMessageIT.sleep(5000);
        assertThat(Files.readAllLines(Paths.get(this.destAbsoluteFilePath))
                .containsAll(msgList), equalTo(true));

        // Clean up open connections
        SendMessageIT.sleep(2000);
        openConnections.forEach(netSocket -> netSocket.close());
        SendMessageIT.sleep(2000);
    }

    /**
     * Creates 5 TCP clients, to reach the max cap of allowed connections.
     * Creates another client connection.
     * Server should reject the last TCP connection.
     * Validates if all the messages sent by the first 5 clients are written to numbers.log
     *
     * @throws IOException For file handling issues
     */
    @DisplayName("Exceed max client connections allowed and get connection rejected for any additional client")
    @Test
    public void whenMaxClientConnectionExceeds() throws IOException {

        //FileChannel.open(Paths.get(this.destAbsoluteFilePath), StandardOpenOption.WRITE).truncate(0).close();

        final Vertx vertx = Vertx.vertx();
        final NetClient client = getNetClient(vertx);
        final List<String> msgList = generateRandomNum(5);
        final String expectedMsg = "success";
        final AtomicBoolean connClosed = new AtomicBoolean(false);

        final List<NetSocket> openConnections = new ArrayList<>();

        // First we will make 5 client connection
        final Single<List<String>> responsesObservableList = Observable.fromArray(msgList.toArray(new String[0]))
                .flatMap(msg -> {
                    return vertx.<String>rxExecuteBlocking(event -> {
                        client.connect(this.destHostPort, this.destHostName, res -> {
                            if (res.succeeded()) {
                                final NetSocket socket = res.result();
                                socket.closeHandler(closeEvent -> {
                                    connClosed.compareAndSet(false, true);
                                });
                                socket.write(msg + NEWLINE);
                                event.complete(expectedMsg);
                                openConnections.add(socket);
                            } else {
                                event.fail(res.cause());
                            }
                        });
                    }).toObservable();
                })
                .delay(2000, TimeUnit.MILLISECONDS)
                .toList();

        assertThat(responsesObservableList.blockingGet()
                .stream()
                .allMatch(res -> res.equals(expectedMsg)),
                equalTo(true));

        assertThat(connClosed.get(), equalTo(false));

        // Next we will attempt a 6th connection, which should fail
        final String response = vertx.<String>rxExecuteBlocking(event -> {
            client.connect(this.destHostPort, this.destHostName, res -> {
                if (res.succeeded()) {
                    final NetSocket socket = res.result();
                    socket.write("" + RandomGenerator.randomInt() + NEWLINE);
                    SendMessageIT.sleep(2000);
                    socket.closeHandler(closeEvent -> {
                        log.info("Connection closed");
                        event.complete(expectedMsg);
                    });
                } else {
                    event.fail(res.cause());
                }
            });
        }).blockingGet();
        assertThat(response, equalTo(expectedMsg));

        // Ensure all the messages are written to file
        assertThat(Files.readAllLines(Paths.get(this.destAbsoluteFilePath))
                .containsAll(msgList), equalTo(true));

        // Clean up open connections
        SendMessageIT.sleep(2000);
        openConnections.forEach(netSocket -> netSocket.close());
        SendMessageIT.sleep(2000);

    }

    /**
     * Creates 4 TCP clients.
     * Creates the 5th client, to reach the max cap of allowed connections.
     * Tries to create 6th client connection, gets rejected by the customer.
     * Disconnects the 5th client, to drop the connection count below he max cap of allowed connections.
     * Attempts again as 6th client, it should successfully connect and send the message.
     * Validates if all the messages sent by the first 5 clients and 6th client are written to numbers.log
     *
     * @throws IOException For file handling issues
     */
    @DisplayName("Exceed max client connections, disconnct a client, and attempt to connect as another client should succeed")
    @Test
    public void whenClientDisconnectsNewClientShouldSucceed() throws IOException {

        //FileChannel.open(Paths.get(this.destAbsoluteFilePath), StandardOpenOption.WRITE).truncate(0).close();

        final Vertx vertx = Vertx.vertx();
        final NetClient client = getNetClient(vertx);
        final List<String> msgList = generateRandomNum(4);
        final String expectedMsg = "success";
        final AtomicBoolean connClosed = new AtomicBoolean(false);

        final List<NetSocket> openConnections = new ArrayList<>();

        // First we will make 4 client connection
        log.info("Attempting to connect as 4 separate clients");
        final Single<List<String>> responsesObservableList = Observable.fromArray(msgList.toArray(new String[0]))
                .flatMap(msg -> {
                    return vertx.<String>rxExecuteBlocking(event -> {
                        client.connect(this.destHostPort, this.destHostName, res -> {
                            if (res.succeeded()) {
                                final NetSocket socket = res.result();
                                socket.closeHandler(closeEvent -> {
                                    connClosed.compareAndSet(false, true);
                                });
                                socket.write(msg + NEWLINE);
                                event.complete(expectedMsg);
                                openConnections.add(socket);
                            } else {
                                event.fail(res.cause());
                            }
                        });
                    }).toObservable();
                })
                .delay(2000, TimeUnit.MILLISECONDS)
                .toList();

        assertThat(responsesObservableList.blockingGet()
                .stream()
                .allMatch(res -> res.equals(expectedMsg)),
                equalTo(true));

        assertThat(connClosed.get(), equalTo(false));

        // Next we will attempt a 5th connection, which should succeed
        log.info("Attempting to connect as 5th client");
        final List<NetSocket> lastConns = new ArrayList<>();
        final List<String> fifthClientMsgList = generateRandomNum(1);
        connClosed.getAndSet(false);

        final String response = vertx.<String>rxExecuteBlocking(event -> {
            client.connect(this.destHostPort, this.destHostName, res -> {
                if (res.succeeded()) {
                    final NetSocket socket = res.result();
                    socket.write(fifthClientMsgList.get(0) + NEWLINE);
                    socket.closeHandler(closeEvent -> {
                        connClosed.compareAndSet(false, true);
                    });
                    event.complete(expectedMsg);
                    lastConns.add(socket);
                } else {
                    event.fail(res.cause());
                }
            });
        }).blockingGet();

        SendMessageIT.sleep(2000);
        assertThat(response, equalTo(expectedMsg));
        assertThat(connClosed.get(), equalTo(false));

        // Next we will attempt a 6th connection, which should fail
        log.info("Attempting to connect as 6th client");
        final List<String> sixthClientMsgList = generateRandomNum(1);
        String addlnClientResponse = vertx.<String>rxExecuteBlocking(event -> {
            client.connect(this.destHostPort, this.destHostName, res -> {
                if (res.succeeded()) {
                    final NetSocket socket = res.result();
                    socket.write(sixthClientMsgList.get(0) + NEWLINE);
                    SendMessageIT.sleep(2000);
                    socket.closeHandler(closeEvent -> {
                        log.info("Connection closed");
                        event.complete(expectedMsg);
                    });
                } else {
                    event.fail(res.cause());
                }
            });
        }).blockingGet();
        SendMessageIT.sleep(2000);
        assertThat(addlnClientResponse, equalTo(expectedMsg));

        // Next we will disconnect the 5th client so that connection count goes below max allowed.
        log.info("Attempting to disconnect as 5th client");
        lastConns.forEach(netSocket -> netSocket.close());

        // Let the 6th client attempt again to connect, should succeed
        log.info("Attempting to connect again as 6th client");
        final List<NetSocket> addlnClientConns = new ArrayList<>();
        final AtomicBoolean addlnClientConnClosed = new AtomicBoolean();
        addlnClientConnClosed.getAndSet(false);
        addlnClientResponse = vertx.<String>rxExecuteBlocking(event -> {
            client.connect(this.destHostPort, this.destHostName, res -> {
                if (res.succeeded()) {
                    final NetSocket socket = res.result();
                    socket.write(sixthClientMsgList.get(0) + NEWLINE);
                    event.complete(expectedMsg);
                    lastConns.add(socket);
                    socket.closeHandler(closeEvent -> {
                        addlnClientConnClosed.compareAndSet(false, true);
                    });
                    addlnClientConns.add(socket);
                } else {
                    event.fail(res.cause());
                }
            });
        }).blockingGet();

        SendMessageIT.sleep(2000);
        assertThat(addlnClientResponse, equalTo(expectedMsg));
        assertThat(addlnClientConnClosed.get(), equalTo(false));

        // Ensure all the messages are written to file
        SendMessageIT.sleep(5000);
        final List<String> allMsgs = new ArrayList<>();
        allMsgs.addAll(msgList);
        allMsgs.addAll(fifthClientMsgList);
        allMsgs.addAll(sixthClientMsgList);
        assertThat(Files.readAllLines(Paths.get(this.destAbsoluteFilePath))
                .containsAll(allMsgs), equalTo(true));

        // Clean up open connections
        SendMessageIT.sleep(2000);
        openConnections.forEach(netSocket -> netSocket.close());
        addlnClientConns.forEach(netSocket -> netSocket.close());
        SendMessageIT.sleep(2000);
    }

    public static List<String> generateRandomNum(final int requiredNumberOfRandomNum) {
        final List<String> randomNums = new ArrayList<>();
        IntStream.range(0, requiredNumberOfRandomNum)
                .forEach(num -> {
                    randomNums.add("" + RandomGenerator.randomInt());
                });
        return randomNums;
    }

    private NetClient getNetClient(final Vertx vertx) {
        final NetClientOptions options = new NetClientOptions()
                .setConnectTimeout(this.connctionTimeout)
                .setReconnectAttempts(this.maxReconAttempts)
                .setReconnectInterval(this.maxReconAttemptInterval);

        return vertx.createNetClient(options);
    }

    private static void sleep(final long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (final InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
