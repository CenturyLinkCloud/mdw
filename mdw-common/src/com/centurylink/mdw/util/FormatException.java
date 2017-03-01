/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import com.centurylink.mdw.common.MdwException;

public class FormatException extends MdwException {
    private static final long serialVersionUID = 1L;

    public FormatException(String msg) {
        super(msg);
    }
    
    public FormatException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
