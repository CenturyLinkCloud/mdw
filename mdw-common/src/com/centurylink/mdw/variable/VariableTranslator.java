/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.variable;

import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.translator.TranslationException;

public interface VariableTranslator {

    public void setPackage(Package pkg);
    
    public abstract String toString(Object pObject) throws TranslationException;

    public abstract Object toObject(String pStr) throws TranslationException;
}
