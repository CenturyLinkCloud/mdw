/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.exception;

import com.centurylink.mdw.common.exception.MDWException;

public class CachingException extends MDWException {

	public CachingException(String message) {
        super(message);
    }

    public CachingException(int code, String message) {
        super(code, message);
    }

    public CachingException(String message, Throwable th) {
        super(message, th);
    }

    public CachingException(int code, String message, Throwable th) {
        super(code, message, th);
    }
}
