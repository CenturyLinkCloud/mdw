/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.container;

import com.centurylink.mdw.common.MdwException;

public class ContainerException extends MdwException {
    
    private static final long serialVersionUID = 1L;

    public ContainerException(String message) {
        super(message);
    }
    
    public ContainerException(String message, Throwable cause) {
        super(-1, message, cause);
    }
}
