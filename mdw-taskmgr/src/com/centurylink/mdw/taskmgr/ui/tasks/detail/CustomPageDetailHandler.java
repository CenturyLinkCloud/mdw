/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.detail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.services.task.TaskManagerAccess;
import com.centurylink.mdw.taskmgr.ui.tasks.FullTaskInstance;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class CustomPageDetailHandler
{
  public static final String CUSTOM_PAGE_OUTCOME = "customPage";

  private FullTaskInstance taskInstance;
  public FullTaskInstance getTaskInstance() { return taskInstance; }
  public void setTaskInstance(FullTaskInstance taskInstance) { this.taskInstance = taskInstance; }

  public CustomPageDetailHandler(FullTaskInstance taskInstance)
  {
    this.taskInstance = taskInstance;
  }

  public String go() throws UIException
  {
    try
    {
      FacesVariableUtil.navigate(new URL(getUrl()));
    }
    catch (IOException ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }

    if (isCompatibilityRendering())
      return getCompatibilityNavOutcome();
    else
    {
      TaskVO taskTemplate = taskInstance.getTaskVO();
      String customPage = taskTemplate.getCustomPage();
      if (customPage == null)
        throw new UIException("No custom page defined for task id: " + taskInstance.getTaskId());

      return getNavOutcome(customPage, taskTemplate.getCustomPageAssetVersion());
    }
  }

  protected String getCompatibilityNavOutcome()
  {
    return CUSTOM_PAGE_OUTCOME;
  }

  public String getNavOutcome(String customPage, String customPageVersion)
  {
    return CUSTOM_PAGE_OUTCOME;
  }

  protected TaskVO getTaskTemplate()
  {
    return taskInstance.getTaskVO();
  }

  protected boolean isCompatibilityRendering() {
    return taskInstance.getTaskVO().isCompatibilityRendering();
  }

  public String getUrl() throws UIException
  {
    TaskVO taskTemplate = taskInstance.getTaskVO();
    String customPage = taskTemplate.getCustomPage();
    if (customPage == null)
      throw new UIException("No custom page defined for task id: " + taskInstance.getTaskId());

    boolean isHubRequest = TaskListScopeActionController.getInstance().isMdwHubRequest();

    try
    {
      if (isCompatibilityRendering())
      {
        if (isHubRequest)
        {
          // redirect to old TaskManager detail page
          return TaskManagerAccess.getInstance().getTaskInstanceUrl(taskInstance.getTaskInstance());
        }
        else
        {
          return getCompatibilityPageUrl(customPage);
        }
      }
      else
      {
        return getPageUrl(customPage, taskTemplate.getCustomPageAssetVersion());
      }
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  protected String getPageUrl(String customPage, String customPageVersion) throws IOException
  {
    String mdwHubUrl = ApplicationContext.getMdwHubUrl();
    if (FacesVariableUtil.getCurrentPackage() != null)
      mdwHubUrl += "/" + FacesVariableUtil.getCurrentPackage().getPackageName();
    FacesVariableUtil.setValue(TaskAttributeConstant.PAGE_PARAM, customPage);
    if (customPageVersion != null)
      FacesVariableUtil.setValue(TaskAttributeConstant.CUSTOM_PAGE_ASSET_VERSION, customPageVersion);
    return mdwHubUrl + "/" + TaskAttributeConstant.PAGE_PATH + customPage + getParams();
  }

  protected String getCompatibilityPageUrl(String customPage) throws IOException
  {
    String taskManagerUrl = ApplicationContext.getTaskManagerUrl();
    if (FacesVariableUtil.getCurrentPackage() != null)
      taskManagerUrl += "/" + FacesVariableUtil.getCurrentPackage().getPackageName();
    FacesVariableUtil.setValue(TaskAttributeConstant.PAGE_PARAM, customPage);
    return taskManagerUrl + "/" + TaskAttributeConstant.PAGE_COMPATIBILITY_PATH + customPage + getParams();
  }

  /**
   * <p>
   * Build a query string based on the original request
   * <br/>Note that it only includes any parameters defined in the
   * <b>mdw.taskmanager.included.request.params</b> property
   * <br/>
    * </p>
   * @return a query string like "?param1=value1&param2=value2"
   * @throws UnsupportedEncodingException
   */
  public String getParams() throws UnsupportedEncodingException
  {
    StringBuffer paramsModifier = new StringBuffer("");
    FacesContext facesContext = FacesContext.getCurrentInstance();
    List<String> includedParams = getIncludedCustomParams();
    Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
    for (String paramName : params.keySet())
    {
      if (includedParams.contains(paramName))
      {
        paramsModifier.append("&").append(paramName).append("=")
            .append(URLEncoder.encode(params.get(paramName), "UTF-8"));
      }

    }
    return paramsModifier.toString();
  }

  /**
   *
   * @return a list of included request parameters
   */
  public List<String> getIncludedCustomParams()
  {
    List<String> params = new ArrayList<String>();
    /**
     * Read the property If it's empty then ignore
     */
    String paramNames = PropertyManager
        .getProperty(PropertyNames.MDW_TASKMANAGER_INCLUDED_REQUEST_PARAMS);
    if (paramNames != null && !"".equals(paramNames))
    {
      String[] paramTable = paramNames.split(",");
      for (int j = 0; j < paramTable.length; j++)
      {
        String param = paramTable[j].trim();
        params.add(param);
      }
    }

    return params;
  }

}
