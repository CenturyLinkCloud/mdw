/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.MdwException;

/**
 *
 */
public class ProcessException extends MdwException {

    private static final long serialVersionUID = 1L;
    /**
     * @param pMessage
     */
    public ProcessException(String pMessage) {
        super(pMessage);
    }

    /**
     * @param pCode
     * @param pMessage
     */
    public ProcessException(int pCode, String pMessage) {
        super(pCode, pMessage);
    }

    /**
     * @param pCode
     * @param pMessage
     * @param pTh
     */
    public ProcessException(int pCode, String pMessage, Throwable pTh) {
        super(pCode, pMessage, pTh);
    }


    public ProcessException(String message, Throwable t) {
        super(message, t);
    }


}
