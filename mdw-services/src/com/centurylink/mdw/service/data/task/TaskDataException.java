/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.data.task;

import com.centurylink.mdw.dataaccess.DataAccessException;


public class TaskDataException extends DataAccessException {

     private static final long serialVersionUID = 1L;


    /**
     * @param pMessage
     */
    public TaskDataException(String pMessage) {
        super(pMessage);

    }

    /**
     * @param pCode
     * @param pMessage
     */
    public TaskDataException(int pCode, String pMessage) {
        super(pCode, pMessage);

    }

    /**
     * @param pCode
     * @param pMessage
     * @param pTh
     */
    public TaskDataException(int pCode, String pMessage, Throwable pTh) {
        super(pCode, pMessage, pTh);

    }
}
