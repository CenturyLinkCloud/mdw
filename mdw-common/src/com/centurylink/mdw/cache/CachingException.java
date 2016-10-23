/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cache;

import com.centurylink.mdw.common.MDWException;

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
