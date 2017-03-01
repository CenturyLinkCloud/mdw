/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import com.centurylink.mdw.common.MdwException;

public class ServiceRegistryException extends MdwException {

    public ServiceRegistryException(String message) {
        super(message);
    }

    public ServiceRegistryException(String message, Throwable t) {
        super(message, t);
    }
    
}
