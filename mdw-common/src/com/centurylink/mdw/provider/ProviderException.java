/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.provider;


public class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
    }
    
    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }

}
