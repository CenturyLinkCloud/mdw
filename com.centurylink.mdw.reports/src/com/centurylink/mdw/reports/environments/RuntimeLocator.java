/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.environments;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.project.EnvironmentException;
import com.centurylink.mdw.model.value.project.EnvironmentVO;
import com.centurylink.mdw.workflow.ManagedNode;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

// TODO properties read is blowing up

public class RuntimeLocator implements EnvironmentLocator
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public URL getDiscoveryUrl() throws EnvironmentException
  {
    try
    {
      String urlBase = System.getProperty("mdw.discovery.url");
      if (urlBase == null || urlBase.isEmpty())
        urlBase = DEFAULT_BASE_URL;
      if (!urlBase.endsWith("/"))
        urlBase += "/";
      String contextRoot = urlBase.endsWith("Discovery/") ? "" : "MDWWeb/";
      if (urlBase.indexOf("lxdnd696") >= 0)
        contextRoot = "MDWExampleWeb/";  // compatibility
      String path = urlBase.endsWith("Discovery/") ? DISCOVERY_URL_PATH_FILE : DISCOVERY_URL_PATH_SERVICE;
      return new URL(urlBase + contextRoot + path);
    }
    catch (MalformedURLException ex) {
      throw new EnvironmentException(ex.getMessage(), ex);
    }
  }

  private List<EnvironmentVO> environments;
  public List<EnvironmentVO> getEnvironments() { return environments; }

  public EnvironmentVO getEnvironment(String name)
  {
    for (EnvironmentVO environment : environments)
    {
      if (environment.getLabel().equals(name))
        return environment;
    }
    return null;
  }

  private EnvironmentVO designEnvironment;
  public EnvironmentVO getDesignEnvironment(String jdbcUrl)
  {
    return designEnvironment;
  }

  public URL getReportsWebAppUrl(String jdbcUrl) throws EnvironmentException
  {
    try
    {
      return new URL(ApplicationContext.getReportsUrl() + "/reports/birt.jsf");
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
      return new URL(ApplicationContext.getTaskManagerUrl());
    }
    catch (MalformedURLException ex)
    {
      throw new EnvironmentException(ex.getMessage(), ex);
    }
  }

  @Override
  public void initialize(List<WorkflowApplication> allWorkflowApps) throws EnvironmentException
  {
    String host = null;
    int port = 0;
    try
    {
      host = ApplicationContext.getServerHost();
      port = ApplicationContext.getServerPort();
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new EnvironmentException("Unable to determine web context root", ex);
    }

    environments = new ArrayList<EnvironmentVO>();

    for (WorkflowApplication app : allWorkflowApps)
    {
      for (WorkflowEnvironment env : app.getEnvironmentList())
      {
        EnvironmentVO envVO = new EnvironmentVO(app, env);
        for (ManagedNode node : envVO.getWorkflowEnv().getManagedServerList())
        {
          if ((node.getHost().startsWith(host) && node.getPort().intValue() == port))
          {
            logger.info("Reports Design Environment: " + envVO.getLabel());
            designEnvironment = envVO;
            break;
          }
        }
      }
    }

    if (designEnvironment == null)
    {
      // add the mdw sandbox environments
      for (WorkflowApplication app : allWorkflowApps)
      {
        if (app.getName().equals("MDW"))
        {
          List<WorkflowEnvironment> envs = app.getEnvironmentList();
          for (int i = 0; i < envs.size(); i++)
          {
            if (i == envs.size() -1) // add the last one
              designEnvironment = new EnvironmentVO(app, envs.get(i));
            else
              environments.add(new EnvironmentVO(app, envs.get(i)));
          }
        }
      }
    }

    if (designEnvironment != null)
    {
      environments.add(designEnvironment);
      String related = PropertyManager.getProperty("mdw.bam.related.apps"); // allow override locally
      if (related == null)
        related = designEnvironment.getWorkflowApp().getBamRelatedApps(); // fall back to the old way through cfg mgr
      if (related != null)
      {
        StringTokenizer st = new StringTokenizer(related, ",");
        while (st.hasMoreTokens())
        {
          String relatedApp = st.nextToken();
          // add bam-related environments
          for (WorkflowApplication app : allWorkflowApps)
          {
            if (app.getName().equals(relatedApp))
            {
              for (WorkflowEnvironment env : app.getEnvironmentList())
              {
                EnvironmentVO envVO = new EnvironmentVO(app, env);
                if (!environments.contains(envVO))
                  environments.add(envVO);
              }
            }
          }
        }
      }
    }

    Collections.sort(environments);
  }

  @Override
  public void log(IStatus status)
  {
    if (status.getException() == null)
      logger.severe(status.getMessage());
    else
      logger.severeException(status.getMessage(), status.getException());
  }
}
