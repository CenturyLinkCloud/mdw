/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.camel.bundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.camel.activity.CamelActivityProviderBean;
import com.centurylink.mdw.camel.cxf.VariableTranslatorProviderBean;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.cache.CacheRegistration;

public class CamelBundleActivator implements BundleActivator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void start(BundleContext bundleContext) throws Exception {
        logger.info("Starting MDW Camel support.");

        CamelActivityProviderBean.setBundleContext(bundleContext);
        VariableTranslatorProviderBean.setBundleContext(bundleContext);

        new CacheRegistration().refreshCache("CamelRouteCache");
        logger.info("MDW Camel startup completed.");
    }

    public void stop(BundleContext context) throws Exception {
    }

}
