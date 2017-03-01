/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.config;


import com.centurylink.mdw.common.MdwException;

/**
 * ServiceLocatorException
 *
  */
public class PropertyException extends MdwException {

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
