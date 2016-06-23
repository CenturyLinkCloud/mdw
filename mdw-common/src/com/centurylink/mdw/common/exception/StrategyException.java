/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.exception;

public class StrategyException extends MDWException {

    private static final long serialVersionUID = 1L;

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
