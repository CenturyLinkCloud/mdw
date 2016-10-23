/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import com.centurylink.mdw.app.WorkflowException;

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
