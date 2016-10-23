/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.sync;

import com.centurylink.mdw.app.WorkflowException;

public class SynchronizationException extends WorkflowException {

    public SynchronizationException(String message) {
        super(message);
    }

    public SynchronizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
