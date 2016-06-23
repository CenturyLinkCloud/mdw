/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

import com.centurylink.mdw.variable.VariableTranslator;

/**
 * Provides instances of VariableTranslators to the MDW runtime engine.
 * In an OSGi environment workflow client bundles register as VariableTranslatorProviders so that
 * the MDW engine can request a variable translator instance that is loaded within the context of
 * the client bundle.  This allows client-provided implementors full access through their
 * ClassLoaders to that bundle's Java classes and resources.
 */
public interface VariableTranslatorProvider extends Provider<VariableTranslator> {

    public VariableTranslator getInstance(String variableType)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException;
    
    public Class<?> getClass(String className) throws ClassNotFoundException;
}
