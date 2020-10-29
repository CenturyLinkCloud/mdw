package com.centurylink.mdw.script;

public class ExecutionException extends RuntimeException {

    public ExecutionException(String message) {
        super(message);
    }

    public ExecutionException(String message, Throwable t) {
        super(message, t);
    }
}
