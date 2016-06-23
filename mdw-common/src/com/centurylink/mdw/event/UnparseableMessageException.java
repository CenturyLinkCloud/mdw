/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import com.centurylink.mdw.common.WorkflowException;

public class UnparseableMessageException extends WorkflowException {

    public UnparseableMessageException(String message){
        super(message);
    }

    public UnparseableMessageException(int code, String message){
        super(code, message);

    }

    public UnparseableMessageException(int code, String message, Throwable th){
        super(code, message, th);

    }
}
