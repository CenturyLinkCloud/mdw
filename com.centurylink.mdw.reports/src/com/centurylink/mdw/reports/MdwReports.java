/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports;

import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.model.value.project.EnvironmentException;
import com.centurylink.mdw.reports.environments.EnvironmentLocator;
import com.centurylink.mdw.reports.environments.PlugInLocator;
import com.centurylink.mdw.reports.environments.RuntimeLocator;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument;

public class MdwReports extends Plugin
{
  public static final String PLUGIN_ID = "com.centurylink.mdw.reports";

  private static MdwReports instance;
  public static MdwReports getInstance()
  {
    if (instance == null)
      instance = new MdwReports();  // non-OSGI (ie: webapp)

    return instance;
  }

  public void start(BundleContext context) throws Exception
  {
    super.start(context);
    instance = this;
  }

  public void stop(BundleContext context) throws Exception
  {
    instance = null;
    super.stop(context);
  }

  public static void log(Throwable e)
  {
    log(new Status(IStatus.ERROR, MdwReports.PLUGIN_ID, IStatus.ERROR, "Error", e));
  }

  public static void log(Throwable e, Object[] args)
  {
    log(new Status(IStatus.ERROR, MdwReports.PLUGIN_ID, IStatus.ERROR, "Error", e));
    log("args received: " + formatArgs(args));
  }

  public static void log(String message)
  {
    log(new Status(IStatus.ERROR, MdwReports.PLUGIN_ID, IStatus.ERROR, message, null));
  }

  public static void log(IStatus status)
  {
    try
    {
      getInstance().getEnvironmentLocator().log(status);
    }
    catch (Exception ex)
    {
      getInstance().getLog().log(status);
      ex.printStackTrace();
    }
  }

  public boolean isRuntimeEnv()
  {
    return System.getProperty("runtimeEnv") != null;
  }

  private EnvironmentLocator environmentLocator;
  public EnvironmentLocator getEnvironmentLocator() throws EnvironmentException {
    if (environmentLocator == null) {
      if (isRuntimeEnv())
        environmentLocator = new RuntimeLocator();
      else
        environmentLocator = new PlugInLocator();

      URL discoveryUrl = environmentLocator.getDiscoveryUrl();

      try
      {
        HttpHelper httpHelper = new HttpHelper(discoveryUrl);
        String xml = httpHelper.get();
        ConfigManagerProjectsDocument doc = ConfigManagerProjectsDocument.Factory.parse(xml, Compatibility.namespaceOptions());
        environmentLocator.initialize(doc.getConfigManagerProjects().getWorkflowAppList());
      }
      catch (EnvironmentException ex)
      {
        log(ex);
        throw ex;
      }
      catch (Exception ex)
      {
        log(ex);
        throw new EnvironmentException(ex.getMessage(), ex);
      }
    }
    return environmentLocator;
  }

  public static String formatArgs(Object[] args)
  {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < args.length; i++)
    {
      sb.append(args[i]);
      if (i < args.length - 1)
        sb.append(',');
    }
    return sb.toString();
  }

  public void clear()
  {
    environmentLocator = null;
  }
}
