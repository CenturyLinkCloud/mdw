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

public class ReportsBundleActivator implements BundleActivator
{
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        logger.info("Activating mdw-reports...");
        try {
            ApplicationContext.setOsgiBundleContext(bundleContext);
        }
        catch (AwaitingStartupException ex) {
            logger.info("Awaiting startup: " + ex.getMessage());
            throw ex;
        }

        ApplicationContext.setBundleActivationTime("mdw-reports", new Date());
        logger.info("mdw-reports Activated.");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.info("Stopping MDWReports");
    }
  }
