/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import com.centurylink.mdw.common.MDWException;

public class ParseException extends MDWException {

    private static final long serialVersionUID = 1L;

    public ParseException(String msg) {
        super(msg);
    }
    
    public ParseException(String msg, Throwable th) {
        super(msg, th);
    }
}
