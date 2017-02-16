/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import com.centurylink.mdw.common.exception.MDWException;

public class DiscoveryException extends MDWException {
    private static final long serialVersionUID = 1L;

    public DiscoveryException(String message) {
        super(message);
    }

    public DiscoveryException(String message, Throwable th) {
        super(-1, message, th);
    }

}
