/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.listener;


// CUSTOM IMPORTS -----------------------------------------------------
import com.centurylink.mdw.common.exception.MDWException;

import java.io.Serializable;

// JAVA IMPORTS -------------------------------------------------------


public class ListenerException extends MDWException implements Serializable{

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