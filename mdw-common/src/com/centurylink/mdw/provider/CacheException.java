/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.provider;

import com.centurylink.mdw.cache.CachingException;

public class CacheException extends CachingException {
    
    public CacheException(String message) {
        super(message);
    }
    
    public CacheException(String message, Throwable th) {
        super(-1, message, th);
    }
}
