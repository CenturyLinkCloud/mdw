/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.myfaces.shared_tomahawk.renderkit.RendererUtils;

import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActions;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

/**
 * Task Action base component.  Used in taskAction.xhtml.
 */
public class TaskAction extends UIInput
{
  public static final String COMPONENT_TYPE = "com.centurylink.mdw.taskmgr.jsf.components.TaskAction";

  private Object _applyTo;
  public void setApplyTo(Object o) { _applyTo = o; }
  public Object getApplyTo()
  {
    if (_applyTo != null)
      return _applyTo;
    return FacesVariableUtil.getObject(getValueExpression("applyTo"));
  }

  private String _actionFor;
  public void setActionFor(String s) { _actionFor = s; }
  public String getActionFor()
  {
    if (_actionFor != null)
      return _actionFor;
    return FacesVariableUtil.getString(getValueExpression("actionFor"));
  }

  private String _optionalValidationFor;
  public void setOptionalValidationFor(String s) { _optionalValidationFor = s; }
  public String getOptionalValidationFor()
  {
    if (_optionalValidationFor != null)
      return _optionalValidationFor;
    return FacesVariableUtil.getString(getValueExpression("optionalValidationFor"));
  }

  public Object saveState(FacesContext context)
  {
    Object[] values = new Object[4];
    values[0] = super.saveState(context);
    values[1] = _applyTo;
    values[2] = _actionFor;
    values[3] = _optionalValidationFor;
    return values;
  }

  public void restoreState(FacesContext context, Object state)
  {
    Object[] values = (Object[]) state;
    super.restoreState(context, values[0]);
    _applyTo = values[1];
    _actionFor = (String)values[2];
    _optionalValidationFor = (String)values[3];
  }

  public void decode(FacesContext facesContext)
  {
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    if (isTaskActionInitiated(facesContext))
    {
      // optionally skip validation based on TaskAction config
      @SuppressWarnings("unchecked")
      List<String> actionForList = (List<String>) externalContext.getRequestMap().get(RendererUtils.ACTION_FOR_LIST);
      if (getActionFor() != null)
      {
        actionForList = new ArrayList<String>();
        externalContext.getRequestMap().put(RendererUtils.ACTION_FOR_LIST, actionForList);
        for (String actionFor : getActionFor().split(","))
          actionForList.add(actionFor);
      }
      if (actionForList != null && getOptionalValidationFor() != null)
      {
        if (getApplyTo() instanceof TaskDetail)
        {
          TaskDetail taskDetail = (TaskDetail) getApplyTo();
          String status = taskDetail == null ? null : taskDetail.getStatus();
          String action = getActionFromRequest(facesContext);
          boolean isAutosave = TaskActions.isAutosaveEnabled(action, status);
          if (!isAutosave)
            actionForList.remove(getOptionalValidationFor());
        }
      }
    }
    super.decode(facesContext);
  }

  private boolean isTaskActionInitiated(FacesContext facesContext)
  {
    ExternalContext externalContext = facesContext.getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = (String) iter.next();
      if ((paramName.endsWith("taskAction_go") && "Go".equals(externalContext.getRequestParameterMap().get(paramName)))
          || paramName.endsWith("taskActionMenu_go") || paramName.endsWith("taskActionMenu_go:hidden"))
      {
        return true;
      }
    }
    return false;
  }

  private String getActionFromRequest(FacesContext facesContext)
  {
    ExternalContext externalContext = facesContext.getExternalContext();
    for (Iterator<String> iter = externalContext.getRequestParameterNames(); iter.hasNext(); )
    {
      String paramName = iter.next();
      if (paramName.endsWith("taskActionSelect"))
      {
        return externalContext.getRequestParameterMap().get(paramName);
      }
    }
    return null;
  }
}
