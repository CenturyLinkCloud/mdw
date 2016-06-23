/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.variable;

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.model.value.process.PackageVO;

public interface VariableTranslator {

    public void setPackage(PackageVO pkg);
    
    public abstract String toString(Object pObject) throws TranslationException;

    public abstract Object toObject(String pStr) throws TranslationException;
}
