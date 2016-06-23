/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;

public class StringDocumentTranslator extends DocumentReferenceTranslator
{
    public Object realToObject(String str) throws TranslationException {
        return str;
    }

    public String realToString(Object obj) throws TranslationException {
        return obj.toString();
    }
}
