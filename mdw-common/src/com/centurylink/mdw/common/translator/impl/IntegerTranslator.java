/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;


// CUSTOM IMPORTS -----------------------------------------------------

// JAVA IMPORTS -------------------------------------------------------

import com.centurylink.mdw.common.translator.VariableTranslator;

/**
 *
 */
public class IntegerTranslator extends VariableTranslator {

	// CONSTANTS ------------------------------------------------------

	// CLASS VARIABLES ------------------------------------------------

	// INSTANCE VARIABLES ---------------------------------------------

	// CONSTRUCTORS ---------------------------------------------------

	// PUBLIC AND PROTECTED METHODS -----------------------------------
    /**
     * Converts the passed in object to a string
     * @param pObject
     * @return String
     */
    public String toString(Object pObject){
        return pObject.toString();
    }

    /**
     * converts the passed in Integer to an equivalent object
     * @param pStr
     * @return Object
     */
    public Object toObject(String pStr){
        return new Integer(pStr);
    }

   	// PRIVATE METHODS ------------------------------------------------

	// ACCESSOR METHODS -----------------------------------------------

	// INNER CLASSES --------------------------------------------------
}