/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.exception;

public class DataAccessException extends MDWException{
    private static final long serialVersionUID = 1L;


	/**
	 * @param pMessage
	 */
	public DataAccessException(String pMessage) {
		super(pMessage);

	}

	/**
	 * @param pCode
	 * @param pMessage
	 */
	public DataAccessException(int pCode, String pMessage) {
		super(pCode, pMessage);

	}

	/**
	 * @param pCode
	 * @param pMessage
	 * @param pTh
	 */
	public DataAccessException(int pCode, String pMessage, Throwable pTh) {
		super(pCode, pMessage, pTh);

	}

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

}
