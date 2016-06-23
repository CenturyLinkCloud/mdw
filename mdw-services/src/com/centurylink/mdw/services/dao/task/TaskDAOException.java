/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.task;

import com.centurylink.mdw.common.exception.DataAccessException;


public class TaskDAOException extends DataAccessException {

     private static final long serialVersionUID = 1L;


	/**
	 * @param pMessage
	 */
	public TaskDAOException(String pMessage) {
		super(pMessage);

	}

	/**
	 * @param pCode
	 * @param pMessage
	 */
	public TaskDAOException(int pCode, String pMessage) {
		super(pCode, pMessage);

	}

	/**
	 * @param pCode
	 * @param pMessage
	 * @param pTh
	 */
	public TaskDAOException(int pCode, String pMessage, Throwable pTh) {
		super(pCode, pMessage, pTh);

	}
}
