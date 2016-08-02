/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.environments;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

import com.centurylink.mdw.model.value.project.EnvironmentException;
import com.centurylink.mdw.model.value.project.EnvironmentVO;
import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.reports.ProjectAccess;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

/**
 * Environment locator for Designer/RCP.
 */
public class PlugInLocator implements EnvironmentLocator
{
  public URL getDiscoveryUrl() throws EnvironmentException
  {
    // load from plug-in preferences
    Preferences[] nodes = new IEclipsePreferences[] { InstanceScope.INSTANCE.getNode("com.centurylink.mdw.designer.ui") };
    String urlBase = Platform.getPreferencesService().get("MdwPrefsDiscoveryUrl", DEFAULT_BASE_URL, nodes);
    if (!urlBase.endsWith("/"))
      urlBase += "/";
    String contextRoot = urlBase.endsWith("Discovery/") ? "" : "MDWWeb/";
    if (urlBase.indexOf("lxdnd696") >= 0)
      contextRoot = "MDWExampleWeb/";  // compatibility
    String path = urlBase.endsWith("Discovery/") ? DISCOVERY_URL_PATH_FILE : DISCOVERY_URL_PATH_SERVICE;
    try {
      return new URL(urlBase + contextRoot + path);
    }
    catch (MalformedURLException ex) {
      throw new EnvironmentException(ex.getMessage(), ex);
    }
  }

  private List<EnvironmentVO> environments;
  public List<EnvironmentVO> getEnvironments()
  {
    return environments;
  }

  public EnvironmentVO getEnvironment(String name)
  {
    for (EnvironmentVO environment : environments)
    {
      if (environment.getLabel().equals(name))
        return environment;
    }
    return null;
  }

  public EnvironmentVO getDesignEnvironment(String jdbcUrl)
  {
    for (EnvironmentVO environment : environments)
    {
      if (jdbcUrl.equals(environment.getJdbcUrl()))
        return environment;
    }
    return null;
  }

  public URL getReportsWebAppUrl(String jdbcUrl) throws EnvironmentException
  {
    try
    {
      // prefer local project envs
      for (WorkflowApplication workflowApp : projectAccess.getLocalProjectWorkflowApps())
      {
        if (jdbcUrl.equals(workflowApp.getEnvironmentList().get(0).getEnvironmentDb().getJdbcUrl()));
          return new URL(getDesignEnvironment(jdbcUrl).getReportsBaseUrl());
      }
      return new URL(getDesignEnvironment(jdbcUrl).getReportsBaseUrl());
    }
    catch (MalformedURLException ex)
    {
      throw new EnvironmentException(ex.getMessage(), ex);
    }
  }

  public URL getTaskManagerUrl(String jdbcUrl) throws EnvironmentException
  {
    try
    {
      // prefer local project envs
      for (WorkflowApplication workflowApp : projectAccess.getLocalProjectWorkflowApps())
      {
        if (jdbcUrl.equals(workflowApp.getEnvironmentList().get(0).getEnvironmentDb().getJdbcUrl()));
          return new URL(getDesignEnvironment(jdbcUrl).getTaskManagerBaseUrl());
      }
      return new URL(getDesignEnvironment(jdbcUrl).getReportsBaseUrl());
    }
    catch (MalformedURLException ex)
    {
      throw new EnvironmentException(ex.getMessage(), ex);
    }
  }

  private ProjectAccess projectAccess;

  public void initialize(List<WorkflowApplication> allWorkflowApps) throws EnvironmentException
  {
    environments = new ArrayList<EnvironmentVO>();

    List<String> bamRelatedApps = new ArrayList<String>();

    projectAccess = new ProjectAccess();
    List<WorkflowApplication> workflowApps = projectAccess.findWorkflowApps();

    for (WorkflowApplication localApp : workflowApps)
    {
      // add local env
      WorkflowEnvironment localEnv = localApp.getEnvironmentList().get(0);
      localEnv.getEnvironmentDb().setPassword(localEnv.getEnvironmentDb().getPassword());
      EnvironmentVO localEnvVO = new EnvironmentVO(localApp, localEnv);
      localApp.getEnvironmentList().get(0).getEnvironmentDb().getPassword();
      if (!environments.contains(localEnvVO))
        environments.add(localEnvVO);
    }


    if (environments.isEmpty())
    {
      // add the mdw sandbox environments
      for (WorkflowApplication app : allWorkflowApps)
      {
        if (app.getName().equals("MDW"))
        {
          for (WorkflowEnvironment env : app.getEnvironmentList())
            environments.add(new EnvironmentVO(app, env));
        }
      }
    }

    // add bam-related environments
    for (WorkflowApplication app : allWorkflowApps)
    {
      if (bamRelatedApps.contains(app.getName()))
      {
        for (WorkflowEnvironment env : app.getEnvironmentList())
        {
          EnvironmentVO envVO = new EnvironmentVO(app, env);
          if (!environments.contains(envVO))
            environments.add(envVO);
        }
      }
    }

    Collections.sort(environments);
  }

  @Override
  public void log(IStatus status)
  {
    MdwReports.getInstance().getLog().log(status);
  }

}
