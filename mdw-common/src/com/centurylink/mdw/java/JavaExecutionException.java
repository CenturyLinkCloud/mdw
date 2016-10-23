/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.java;

public class JavaExecutionException extends MdwJavaException {
    
    private static final long serialVersionUID = 1L;

    public JavaExecutionException(int code, String message) {
        super(code, message);
    }

    public JavaExecutionException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
    
    public JavaExecutionException(String message) {
        this(-1, message);
    }
    
    public JavaExecutionException(String message, Throwable cause) {
        this(-1, message, cause);
    }
}
