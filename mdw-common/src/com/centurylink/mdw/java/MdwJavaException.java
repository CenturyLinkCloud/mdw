/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.java;

import com.centurylink.mdw.common.MdwException;

public class MdwJavaException extends MdwException {

    public MdwJavaException(int code, String message) {
        super(code, message);
    }

    public MdwJavaException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public MdwJavaException(String message) {
        this(-1, message);
    }

    public MdwJavaException(String message, Throwable cause) {
        this(-1, message, cause);
    }
}
