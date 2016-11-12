/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cache;

public class CachingException extends RuntimeException {

    public CachingException(String message) {
        super(message);
    }

    public CachingException(String message, Throwable th) {
        super(message, th);
    }
}
