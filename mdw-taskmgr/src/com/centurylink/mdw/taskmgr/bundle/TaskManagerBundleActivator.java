/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.bundle;

import java.util.Date;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.service.AwaitingStartupException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

// touch
public class TaskManagerBundleActivator implements BundleActivator
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  @Override
  public void start(BundleContext bundleContext) throws Exception
  {
    logger.info("Activating mdw-taskmgr...");
    try {
      ApplicationContext.setOsgiBundleContext(bundleContext);
      ApplicationContext.setBundleActivationTime("mdw-taskmgr", new Date());
      logger.info("mdw-taskmgr activated.");
    }
    catch (AwaitingStartupException ex)
    {
      logger.info("Awaiting startup: " + ex.getMessage());
      throw ex;
    }
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception
  {
  }

}
