/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;

import java.util.StringTokenizer;

/**
 * 
 */
public class LongArrayTranslator extends VariableTranslator {

    // CONSTANTS ------------------------------------------------------

    // CLASS VARIABLES ------------------------------------------------

    // INSTANCE VARIABLES ---------------------------------------------

    // CONSTRUCTORS ---------------------------------------------------

    // PUBLIC AND PROTECTED METHODS -----------------------------------
    /**
     * converts the passed in String to an equivalent object
     * 
     * @param pStr
     * @return Object
     */
    public Object toObject(String pStr) {
        Long[] retArr = null;
        if (StringHelper.isEmpty(pStr) || EMPTY_STRING.equals(pStr)) {
            return new Long[0];
        }
        StringTokenizer tokenizer = new StringTokenizer(pStr, ARRAY_DELIMETER);
        retArr = new Long[tokenizer.countTokens()];
        int index = 0;
        while (tokenizer.hasMoreTokens()) {
            retArr[index] = new Long(tokenizer.nextToken());
            index++;
        }
        return retArr;
    }

    /**
     * Converts the passed in object to a string
     * 
     * @param pObject
     * @return String
     */
    public String toString(Object pObject) {
        if (pObject instanceof String) {
            return (String) pObject;
        }
        Long[] objArr = (Long[]) pObject;
        if (objArr == null || objArr.length == 0) {
            return EMPTY_STRING;
        }
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < objArr.length; i++) {
            buff.append(objArr[i].toString()).append(ARRAY_DELIMETER);
        }
        String st = buff.toString();
        return st.substring(0, st.length() - 1);
    }

    // PRIVATE METHODS ------------------------------------------------

    // ACCESSOR METHODS -----------------------------------------------

    // INNER CLASSES --------------------------------------------------

}