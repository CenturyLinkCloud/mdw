/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;


import com.centurylink.mdw.common.MDWException;

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
