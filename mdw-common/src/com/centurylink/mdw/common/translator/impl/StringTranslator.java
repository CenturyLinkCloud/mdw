/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.common.translator.VariableTranslator;


/**
 *
 */
public class StringTranslator extends VariableTranslator {

    /**
     * converts the passed in String to an equivalent object
     * @param pStr
     * @return Object
     */
    public Object toObject(String pStr){
        return pStr;
    }

    public String toString(Object obj) {
        return (String)obj;
    }
}