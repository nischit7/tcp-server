package org.example.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Initiates a shutdown of the server, if one invokes {@link #initiateShutdown(int)}.
 * Care must be taken to invoke only when needed.
 * This is a special requirement to build this service.
 * When a client sends a message with the text "terminate", a handler
 * {@link org.example.handlers.TerminateConnRequestHandler} invokes this class to close all TCP connections and
 * gracefully shutdown the service. The good news with this approach is we don't have to explicitly close each and
 * every TCP connection. The closure of such connections are handled by respective vertx connection lifecycle handlers.
 */
@Component
public class ShutdownManager {

    private final ApplicationContext appContext;

    @Autowired
    public ShutdownManager(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    /**
     * Initiates a shutdown of the server. It starts by asking spring context to graceful dispose all the beans.
     * The return code is just for output purpose. A non-zero return code can be sent only when there is an error.
     * For the event handling functionality the server handles, the return code will always be zero.
     *
     * @param returnCode The return code.
     */
    public void initiateShutdown(final int returnCode) {
        SpringApplication.exit(appContext, () -> returnCode);
    }
}
