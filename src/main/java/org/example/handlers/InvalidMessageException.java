package org.example.handlers;

import org.example.server.BaseRuntimeException;

public class InvalidMessageException extends BaseRuntimeException {
    public InvalidMessageException(final String msg, final Object...args) {
        super(msg, args);
    }
}
