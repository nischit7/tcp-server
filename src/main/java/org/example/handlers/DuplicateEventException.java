package org.example.handlers;

import org.example.server.BaseRuntimeException;

public class DuplicateEventException extends BaseRuntimeException {
    public DuplicateEventException(final String msg, final Object...args) {
        super(msg, args);
    }
}
