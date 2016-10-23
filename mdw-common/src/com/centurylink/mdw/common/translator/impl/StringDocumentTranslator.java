/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;

public class StringDocumentTranslator extends DocumentReferenceTranslator
{
    public Object realToObject(String str) throws TranslationException {
        return str;
    }

    public String realToString(Object obj) throws TranslationException {
        return obj.toString();
    }
}
