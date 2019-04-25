package com.centurylink.mdw.monitor;

public class MonitorException extends RuntimeException {

    public MonitorException(String message) {
        super(message);
    }

    public MonitorException(String message, Throwable cause) {
        super(message, cause);
    }
}
