/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.exception;


import com.centurylink.mdw.common.exception.MDWException;

/**
 * ServiceLocatorException
 *
  */
public class ServiceLocatorException extends MDWException {

	private static final long serialVersionUID = 1L;

	public ServiceLocatorException(String pMessage){
        super(pMessage);
    }

    public ServiceLocatorException(int pCode, String pMessage){
        super(pCode, pMessage);

    }

    public ServiceLocatorException(int pCode, String pMessage, Throwable pTh){
        super(pCode, pMessage, pTh);

    }



}
