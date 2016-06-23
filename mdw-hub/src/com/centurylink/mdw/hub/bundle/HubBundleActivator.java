/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.bundle;

import java.util.Date;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.service.AwaitingStartupException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

public class HubBundleActivator implements BundleActivator {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void start(BundleContext bundleContext) throws Exception {
        logger.info("activating mdw-hub...");
        try
        {
            ApplicationContext.setOsgiBundleContext(bundleContext);
            if (ApplicationContext.isDevelopment())
                System.setProperty("faces.PROJECT_STAGE", "Development");
        }
        catch (AwaitingStartupException ex)
        {
          logger.info("Awaiting startup: " + ex.getMessage());
          throw ex;
        }

        ApplicationContext.setBundleActivationTime("mdw-hub", new Date());
        logger.info("mdw-hub activated.");
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }
}
