/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.config;


import com.centurylink.mdw.common.MDWException;

/**
 * ServiceLocatorException
 *
  */
public class PropertyException extends MDWException {

	private static final long serialVersionUID = 1L;

	public PropertyException(String pMessage){
        super(pMessage);
    }

    public PropertyException(int pCode, String pMessage){
        super(pCode, pMessage);

    }

    public PropertyException(int pCode, String pMessage, Throwable pTh){
        super(pCode, pMessage, pTh);

    }



}
