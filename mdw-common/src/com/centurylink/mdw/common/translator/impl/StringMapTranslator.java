/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

// CUSTOM IMPORTS -----------------------------------------------------

// JAVA IMPORTS -------------------------------------------------------

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;

/**
 * 
 */
public class StringMapTranslator extends VariableTranslator {

    // CONSTANTS ------------------------------------------------------
    private static final String EQUALS = "=";

    private static final String NAME_VALUE_DELIMETER = "~";

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
        Map<String,String> retMap = null;
        if (StringHelper.isEmpty(pStr) || EMPTY_STRING.equals(pStr)) {
            return new HashMap<String,String>();
        }
        StringTokenizer tokenizer = new StringTokenizer(pStr,
                NAME_VALUE_DELIMETER);
        retMap = new HashMap<String,String>(tokenizer.countTokens());

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String name = token.substring(0, token.indexOf(EQUALS));
            String val = token.substring(token.indexOf(EQUALS) + 1);
            retMap.put(name, val);
        }
        return retMap;
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
        @SuppressWarnings("unchecked")
        Map<String,String> map = (Map<String,String>) pObject;
        if (map == null || map.isEmpty()) {
            return EMPTY_STRING;
        }
        StringBuffer buff = new StringBuffer();
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            String val = map.get(name);
            if (val == null) {
                continue;
            }
            buff.append(name).append(EQUALS).append(val);
            if (it.hasNext()) {
                buff.append(NAME_VALUE_DELIMETER);
            }

        }
        String st = buff.toString();
        return st.substring(0, st.length());
    }

    // PRIVATE METHODS ------------------------------------------------

    // ACCESSOR METHODS -----------------------------------------------

    // INNER CLASSES --------------------------------------------------

}
