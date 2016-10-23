/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.provider;

import com.centurylink.mdw.variable.VariableTranslator;

/**
 * Provides instances of VariableTranslators to the MDW runtime engine.
 */
public interface VariableTranslatorProvider extends Provider<VariableTranslator> {

    public VariableTranslator getInstance(String variableType)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    public Class<?> getClass(String className) throws ClassNotFoundException;
}
