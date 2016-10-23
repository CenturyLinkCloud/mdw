/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import com.centurylink.mdw.common.service.ServiceException;

public class TaskValidationException extends ServiceException {

    public TaskValidationException(String message) {
        super(message);
    }

    public TaskValidationException(int code, String message) {
        super(code, message);
    }

    public TaskValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskValidationException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
