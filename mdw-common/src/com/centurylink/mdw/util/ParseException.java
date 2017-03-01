/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import com.centurylink.mdw.common.MdwException;

public class ParseException extends MdwException {

    private static final long serialVersionUID = 1L;

    public ParseException(String msg) {
        super(msg);
    }
    
    public ParseException(String msg, Throwable th) {
        super(msg, th);
    }
}
