/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import com.centurylink.mdw.common.exception.MDWException;

public class ServiceException extends MDWException {

    public static final int BAD_REQUEST = 400;
    public static final int NOT_AUTHORIZED = 401;
    public static final int NOT_FOUND = 404;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(int code, String message) {
        super(code, message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
