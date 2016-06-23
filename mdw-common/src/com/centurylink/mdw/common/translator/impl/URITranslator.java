/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;


import com.centurylink.mdw.common.translator.VariableTranslator;

import java.net.URI;

/**
 *
 */
public class URITranslator extends VariableTranslator {

    /**
     * Converts the passed in object to a string
     * @param pObject
     * @return String
     */
    public String toString(Object pObject){
       return pObject.toString();
    }

    /**
     * converts the passed in String to an equivalent object
     * @param pStr
     * @return Object
     */
    public Object toObject(String pStr){
      return URI.create(pStr);
    }

}