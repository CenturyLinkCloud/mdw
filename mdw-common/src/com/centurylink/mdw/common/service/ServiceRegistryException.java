/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import com.centurylink.mdw.common.MDWException;

public class ServiceRegistryException extends MDWException {

    public ServiceRegistryException(String message) {
        super(message);
    }

    public ServiceRegistryException(String message, Throwable t) {
        super(message, t);
    }
    
}
