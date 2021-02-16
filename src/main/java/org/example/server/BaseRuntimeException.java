package org.example.server;

/**
 * Common base exception that all functional exceptions can extend.
 * Sub classes can be created to handle specific custom exception scenarios
 */
public class BaseRuntimeException extends RuntimeException {

    private final Object[]  args;

    /**
     * A simple constructor.
     *
     * @param msg String
     * @param args Object[]
     */
    public BaseRuntimeException(final String msg, final Object...args) {
        super(msg);
        this.args = args;
    }

    /**
     * A simple constructor.
     *
     * @param args Object[]
     */
    public BaseRuntimeException(final Throwable throwable, final String msg, final Object...args) {
        super(msg, throwable);
        this.args = args;
    }
}
