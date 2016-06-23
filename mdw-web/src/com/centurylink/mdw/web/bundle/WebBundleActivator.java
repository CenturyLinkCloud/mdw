/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.bundle;

import java.util.Date;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.service.AwaitingStartupException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

public class WebBundleActivator implements BundleActivator
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  @Override
  public void start(BundleContext bundleContext) throws Exception
  {
    logger.info("activating mdw-web...");
    try
    {
      ApplicationContext.setOsgiBundleContext(bundleContext);
      ApplicationContext.setBundleActivationTime("mdw-web", new Date());
      logger.info("mdw-web activated.");
    }
    catch (AwaitingStartupException ex)
    {
      logger.info("Awaiting startup: " + ex.getMessage());
      throw ex;
    }
  }

  @Override
  public void stop(BundleContext context) throws Exception
  {

  }

}
