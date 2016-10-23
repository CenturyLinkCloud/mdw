/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

public class AuthorizationException extends ServiceException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(int code, String message) {
        super(code, message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthorizationException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
