/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer;

import com.centurylink.mdw.common.MdwException;

/**
 * ObserverException
 *
  */
public class ObserverException extends MdwException {

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
