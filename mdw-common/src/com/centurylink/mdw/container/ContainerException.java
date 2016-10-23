/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container;

import com.centurylink.mdw.common.MDWException;

public class ContainerException extends MDWException {
    
    private static final long serialVersionUID = 1L;

    public ContainerException(String message) {
        super(message);
    }
    
    public ContainerException(String message, Throwable cause) {
        super(-1, message, cause);
    }
}
