/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.provider;

import com.centurylink.mdw.cache.CachingException;

public class CacheException extends CachingException {

    public CacheException(String message) {
        super(message);
    }

    public CacheException(String message, Throwable th) {
        super(message, th);
    }
}
