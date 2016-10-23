/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.cloud;

import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.VariableTranslator;

public class MyFloatTranslator extends VariableTranslator {

    public String toString(Object obj) throws TranslationException {
        return obj.toString();
    }

    @Override
    public Object toObject(String str) throws TranslationException {
        return new Float(str);
    }

}
