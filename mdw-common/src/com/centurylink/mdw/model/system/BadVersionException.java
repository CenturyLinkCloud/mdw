package com.centurylink.mdw.model.system;

public class BadVersionException extends RuntimeException {

    public BadVersionException(String message) {
        super(message);
    }

    public BadVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
