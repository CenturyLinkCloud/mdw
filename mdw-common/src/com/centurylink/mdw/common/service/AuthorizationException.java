package com.centurylink.mdw.common.service;

import com.centurylink.mdw.model.Status;

public class AuthorizationException extends ServiceException {

    public AuthorizationException() {
        super(Status.FORBIDDEN.getCode(), Status.FORBIDDEN.getMessage());
    }

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
