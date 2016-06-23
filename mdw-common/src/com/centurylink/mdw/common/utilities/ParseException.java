/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;

import com.centurylink.mdw.common.exception.MDWException;

public class ParseException extends MDWException {

    private static final long serialVersionUID = 1L;

    public ParseException(String msg) {
        super(msg);
    }
    
    public ParseException(String msg, Throwable th) {
        super(msg, th);
    }
}
