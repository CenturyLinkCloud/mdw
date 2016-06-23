/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.activity;


/**
 * @deprecated
 * @see com.centurylink.mdw.activity.ActivityException
 */
@Deprecated
public class ControlledActivityException extends ActivityException {

    public ControlledActivityException(String message) {
        super(message);
    }

    public ControlledActivityException(int code, String message) {
        super(code, message);

    }

    public ControlledActivityException(int code, String message, Throwable th) {
        super(code, message, th);

    }

    public ControlledActivityException(String message, Throwable th) {
        super(message, th);

    }
}
