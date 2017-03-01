/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.listener;


import java.io.Serializable;

import com.centurylink.mdw.common.MdwException;

// JAVA IMPORTS -------------------------------------------------------


public class ListenerException extends MdwException implements Serializable{

    // CONSTANTS ------------------------------------------------------

    // CLASS VARIABLES ------------------------------------------------
    public static final long serialVersionUID = 1L;
    // INSTANCE VARIABLES ---------------------------------------------

    // CONSTRUCTORS ---------------------------------------------------
    public ListenerException(String pMessage){
        super(pMessage);
    }

    /**
     * @param pCode
     * @param pMEssage
     */
    public ListenerException(int pCode, String pMessage){
        super(pCode, pMessage);

    }

    /**
     * @param pCode
     * @param pMessage
     * @param pTh
     */
    public ListenerException(int pCode, String pMessage, Throwable pTh){
        super(pCode, pMessage, pTh);

    }
    // PUBLIC AND PROTECTED METHODS -----------------------------------

    // PRIVATE METHODS ------------------------------------------------

    // ACCESSOR METHODS -----------------------------------------------

    // INNER CLASSES --------------------------------------------------
}