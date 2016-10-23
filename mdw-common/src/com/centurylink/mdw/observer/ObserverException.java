/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer;

import com.centurylink.mdw.common.MDWException;

/**
 * ObserverException
 *
  */
public class ObserverException extends MDWException {

	private static final long serialVersionUID = 1L;

	public ObserverException(String pMessage){
        super(pMessage);
    }

    public ObserverException(int pCode, String pMessage){
        super(pCode, pMessage);

    }

    public ObserverException(String pMessage, Throwable pTh){
        super(pMessage, pTh);
    }

    public ObserverException(int pCode, String pMessage, Throwable pTh){
        super(pCode, pMessage, pTh);

    }



}
