package org.example.handlers;

import org.example.server.BaseRuntimeException;

public class UnableToWriteToFileException extends BaseRuntimeException {
    public UnableToWriteToFileException(final String msg, final Object...args) {
        super(msg, args);
    }

    public UnableToWriteToFileException(final Throwable throwable, final String msg, final Object...args) {
        super(throwable, msg, args);
    }
}
