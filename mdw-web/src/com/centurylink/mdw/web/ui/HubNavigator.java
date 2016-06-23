/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.ui;

import java.net.URL;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class HubNavigator
{

  public String goMyTasks() throws UIException
  {
    redirectToMdwHub("/taskList/myTasks");
    return null;
  }

  public String goWorkgroupTasks() throws UIException
  {
    redirectToMdwHub("/taskList/workgroupTasks");
    return null;
  }

  public String goOrders() throws UIException
  {
    redirectToMdwHub("/orders/orderList.jsf");  // TODO: tech agnostic
    return null;
  }

  public String goProcesses() throws UIException
  {
    redirectToMdwHub("/process/instanceList.jsf");  // TODO: tech agnostic
    return null;
  }

  public String goRequests() throws UIException
  {
    redirectToMdwHub("/requests/requestsList.jsf");  // TODO: tech agnostic
    return null;
  }

  public String goSystem() throws UIException
  {
    redirectToMdwHub("/system/systemInformation.jsf");  // TODO: tech agnostic
    return null;
  }

  public String goAdmin() throws UIException
  {
    redirectToTaskManager("/admin/groups.jsf");
    return null;
  }

  protected void redirectToMdwHub(String path) throws UIException
  {
    try
    {
      String url = ApplicationContext.getMdwHubUrl() + path;
      FacesVariableUtil.navigate(new URL(url));
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  protected void redirectToTaskManager(String path) throws UIException
  {
    try
    {
      String url = ApplicationContext.getTaskManagerUrl() + path;
      FacesVariableUtil.navigate(new URL(url));
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

}
