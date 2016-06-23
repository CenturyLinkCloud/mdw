/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

public class StartupException extends com.centurylink.mdw.common.exception.StartupException {

    public StartupException(String message) {
        super(message);
    }
    
    public StartupException(String message, Throwable th) {
        super(-1, message, th);
    }

}
