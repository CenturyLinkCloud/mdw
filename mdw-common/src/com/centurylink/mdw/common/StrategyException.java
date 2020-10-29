package com.centurylink.mdw.common;

public class StrategyException extends MdwException {

    public StrategyException(String message) {
        super(message);
    }

    public StrategyException(int code, String message) {
        super(code, message);
    }

    public StrategyException(String message, Throwable cause) {
        super(message, cause);
    }

    public StrategyException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
