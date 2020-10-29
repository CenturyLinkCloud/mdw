package com.centurylink.mdw.util;

import com.centurylink.mdw.common.MdwException;

public class ParseException extends MdwException {

    public ParseException(String msg) {
        super(msg);
    }

    public ParseException(String msg, Throwable th) {
        super(msg, th);
    }
}
