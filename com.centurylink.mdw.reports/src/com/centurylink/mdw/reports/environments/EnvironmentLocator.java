/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.environments;

import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import com.centurylink.mdw.model.value.project.EnvironmentException;
import com.centurylink.mdw.model.value.project.EnvironmentVO;
import com.centurylink.mdw.workflow.WorkflowApplication;

public interface EnvironmentLocator
{
  public static final String DEFAULT_BASE_URL = "http://lxdenvmtc143.dev.qintra.com:7021/Discovery";
  public static final String DISCOVERY_URL_PATH_SERVICE = "Services/GetConfigFile?name=ConfigManagerProjects.xml";
  public static final String DISCOVERY_URL_PATH_FILE = "ConfigManagerProjects.xml";

  public URL getDiscoveryUrl() throws EnvironmentException;
  public List<EnvironmentVO> getEnvironments();
  public EnvironmentVO getEnvironment(String name);
  public EnvironmentVO getDesignEnvironment(String jdbcUrl);
  public URL getReportsWebAppUrl(String jdbcUrl) throws EnvironmentException;
  public URL getTaskManagerUrl(String jdbcUrl) throws EnvironmentException;
  public void initialize(List<WorkflowApplication> allWorkflowApps) throws EnvironmentException;

  public void log(IStatus status);
}
