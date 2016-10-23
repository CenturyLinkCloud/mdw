/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

public class DataAccessOfflineException extends DataAccessException {

    public DataAccessOfflineException(String msg) {
        super(msg);
    }

    public DataAccessOfflineException(String msg, Throwable t) {
        super(msg, t);
    }

}
