/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.event;

import com.centurylink.mdw.app.WorkflowException;

public class EventHandlerException extends WorkflowException {

    public EventHandlerException(String message){
        super(message);
    }

    public EventHandlerException(int code, String message){
        super(code, message);

    }

    public EventHandlerException(int code, String message, Throwable th){
        super(code, message, th);

    }
}
