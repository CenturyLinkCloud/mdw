/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.translator.VariableTranslator;

public class StringTranslator extends VariableTranslator {

    public Object toObject(String str){
        return str;
    }

    public String toString(Object obj) {
        return (String)obj;
    }
}