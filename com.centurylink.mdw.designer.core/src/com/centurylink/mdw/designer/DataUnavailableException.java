/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer;

import com.centurylink.mdw.dataaccess.DataAccessOfflineException;

/**
 * Non-fatal version of DataAccessOfflineException.
 */
public class DataUnavailableException extends DataAccessOfflineException {

    public DataUnavailableException(String msg) {
        super(msg);
    }

    public DataUnavailableException(String msg, Throwable t) {
        super(msg, t);
    }
}
