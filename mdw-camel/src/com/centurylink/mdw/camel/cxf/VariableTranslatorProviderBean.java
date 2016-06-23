/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel.cxf;

import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.provider.ProviderException;
import com.centurylink.mdw.common.provider.VariableTranslatorProvider;
import com.centurylink.mdw.common.translator.VariableTranslator;

public class VariableTranslatorProviderBean implements VariableTranslatorProvider  {

    public VariableTranslator getInstance(String variableType)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return Class.forName(variableType).asSubclass(VariableTranslator.class).newInstance();
    }

    public Class<?> getClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }    
    
    
    public String getAlias() throws ProviderException {
        return "mdwCxfTranslatorProvider";
    }
    
    private static BundleContext bundleContext;
    public BundleContext getBundleContext() {
        return bundleContext;
    }
    public static void setBundleContext(BundleContext bc) {
        bundleContext = bc;
    }

    // NOT USED
    public String getProperty(String name) {
        return null;
    }
    public void setProperty(String name, String value) {
    }
}
