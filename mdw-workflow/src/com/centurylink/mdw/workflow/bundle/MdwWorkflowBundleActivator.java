/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.bundle;

import java.util.Date;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.service.AwaitingStartupException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

public class MdwWorkflowBundleActivator implements BundleActivator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        logger.info("mdw-workflow startup...");

        try {
            ApplicationContext.setOsgiBundleContext(bundleContext);
            ApplicationContext.setBundleActivationTime("mdw-workflow", new Date());
            logger.info("mdw-workflow startup completed.");
        }
        catch (AwaitingStartupException ex) {
            logger.info("Awaiting startup: " + ex.getMessage());
            throw ex;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.severeException(ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
    }
}