/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.common.exception.DataAccessException;

public class DataAccessOfflineException extends DataAccessException {

    public DataAccessOfflineException(String msg) {
        super(msg);
    }

    public DataAccessOfflineException(String msg, Throwable t) {
        super(msg, t);
    }

}
