/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.auth;

import com.centurylink.mdw.common.MdwException;

public class MdwSecurityException extends MdwException {

    public static final int UNTRUSTED_CERT = 101;

    private int errorCode;

    public int getCode() {
        return errorCode;
    }

    public MdwSecurityException(String message) {
        super(-1, message);
    }

    public MdwSecurityException(String message, Throwable cause) {
        super(-1, message, cause);
    }

    public MdwSecurityException(int code, String message) {
        super(code, message);
        errorCode = code;
    }

    public MdwSecurityException(int code, String message, Throwable cause) {
        super(code, message, cause);
        errorCode = code;
    }
}
