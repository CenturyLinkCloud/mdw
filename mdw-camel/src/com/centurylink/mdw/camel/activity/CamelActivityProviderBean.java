/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel.activity;

import org.osgi.framework.BundleContext;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.common.provider.ActivityProvider;
import com.centurylink.mdw.common.provider.ProviderException;

public class CamelActivityProviderBean implements ActivityProvider {

    public GeneralActivity getInstance(String className)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return Class.forName(className).asSubclass(GeneralActivity.class).newInstance();
    }

    public String getAlias() throws ProviderException {
        return "mdwCamelActivityProvider";
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
