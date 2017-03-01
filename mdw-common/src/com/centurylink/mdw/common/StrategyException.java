/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common;

public class StrategyException extends MdwException {

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
