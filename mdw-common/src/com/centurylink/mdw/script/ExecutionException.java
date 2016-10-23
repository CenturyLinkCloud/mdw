/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

import com.centurylink.mdw.common.MDWException;

public class ExecutionException extends MDWException {

    private static final long serialVersionUID = 1L;

    public ExecutionException(String message) {
        super(message);
    }
    
    public ExecutionException(String message, Throwable t){
        super(-1, message, t);
    }
    
    public ExecutionException(int code, String message, Throwable t){
        super(code, message, t);
    }
}
