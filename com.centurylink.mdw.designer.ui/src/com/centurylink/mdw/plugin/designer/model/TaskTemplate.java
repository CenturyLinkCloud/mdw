/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.plugin.designer.properties.editor.TimeInterval.Units;

public class TaskTemplate extends WorkflowAsset
{
  public static final String TASK_TYPE = "TaskType"; // AutoForm or Custom
  public static final String AUTOFORM = "AutoForm";
  public static final String CUSTOM = "Custom";
  public static final String AUTOFORM_PAGELET = "com.centurylink.mdw.base/AutoFormManualTask.pagelet";
  public static final String CUSTOM_PAGELET = "com.centurylink.mdw.base/CustomManualTask.pagelet";

  private TaskVO taskVO;
  public TaskVO getTaskVO() { return taskVO; }
  public void setTaskVO(TaskVO taskVO)
  {
    this.taskVO = taskVO;
    setRuleSetVO(taskVO);
  }

  public Entity getActionEntity()
  {
    return Entity.TaskTemplate;
  }

  public TaskTemplate()
  {
    super();
    this.taskVO = new TaskVO();
    setLanguage(AUTOFORM);
  }

  public TaskTemplate(String taskType)
  {
    this();
    setLanguage(taskType);
  }

  public TaskTemplate(TaskVO taskVO, WorkflowPackage packageVersion)
  {
    super(taskVO, packageVersion);
    this.taskVO = taskVO;
    // set asset name different from task name
    setName(taskVO.getName());
  }

  public TaskTemplate(TaskTemplate cloneFrom)
  {
    super(cloneFrom);
    this.taskVO = cloneFrom.taskVO;
    setName(taskVO.getName());
  }

  public String getTaskName()
  {
    return taskVO.getTaskName();
  }

  @Override
  public String getTitle()
  {
    return "Task Template";
  }

  @Override
  public String getIcon()
  {
    return "task.gif";
  }

  public String getLogicalId()
  {
    return taskVO.getLogicalId();
  }

  public void setLogicalId(String logicalId)
  {
    taskVO.setLogicalId(logicalId);
  }

  public WorkflowAsset getPagelet()
  {
    if (taskVO.isAutoformTask())
      return getProject().getAsset(AUTOFORM_PAGELET);
    else
      return getProject().getAsset(CUSTOM_PAGELET);
  }

  public String getAttribute(String name)
  {
    if ("name".equals(name))
      return taskVO.getTaskName();
    else if ("logicalId".equals(name))
      return taskVO.getLogicalId();
    else if ("category".equals(name))
      return taskVO.getTaskCategory();
    else if ("description".equals(name))
      return taskVO.getComment();
    else if ("TaskSLA".equals(name) || "ALERT_INTERVAL".equals(name))
    {
      String value = getRuleSetVO().getAttribute(name);
      if (value == null)
        return null;
      int seconds = Integer.parseInt(value);
      if (seconds % 86400 == 0)
      {
        getRuleSetVO().setAttribute(name + "_UNITS", Units.Days.toString()); // temp
        return String.valueOf((int)seconds/86400);
      }
      else
      {
        getRuleSetVO().setAttribute(name + "_UNITS", Units.Hours.toString());
        return String.valueOf((int)seconds/3600);
      }
    }
    else
      return getRuleSetVO().getAttribute(name);
  }

  public void setAttribute(String name, String value)
  {
    if ("name".equals(name))
      taskVO.setTaskName(value);
    else if ("logicalId".equals(name))
      taskVO.setLogicalId(value);
    else if ("category".equals(name))
      taskVO.setTaskCategory(value);
    else if ("description".equals(name))
      taskVO.setComment(value);
    else if ("TaskSLA".equals(name) || "ALERT_INTERVAL".equals(name))
    {
      if (value == null)
        getRuleSetVO().setAttribute(name, value);
      else
      {
        int entered = Integer.parseInt(value);
        String units = getRuleSetVO().getAttribute(name + "_UNITS");
        if ("Days".equals(units))
          getRuleSetVO().setAttribute(name, String.valueOf(entered * 86400));
        else
          getRuleSetVO().setAttribute(name, String.valueOf(entered * 3600));
      }
    }
    else if ("TaskSLA_UNITS".equals(name) || "ALERT_INTERVAL_UNITS".equals(name))
    {
      String valAttrName = name.substring(0, name.length() - 6);
      String val = getAttribute(valAttrName);
      if (val != null)
      {
        int entered = Integer.parseInt(val);
        if ("Days".equals(value))
          getRuleSetVO().setAttribute(valAttrName, String.valueOf(entered * 86400));
        else
          getRuleSetVO().setAttribute(valAttrName, String.valueOf(entered * 3600));
      }
    }
    else
      getRuleSetVO().setAttribute(name, value);

    fireAttributeValueChanged(name, value);
  }

  public void removeAttribute(String name)
  {
    if ("category".equals(name))
      taskVO.setTaskCategory(null);
    else if ("description".equals(name))
      taskVO.setComment(null);
    else
      getRuleSetVO().removeAttribute(name);
    fireAttributeValueChanged(name, null);
  }

  public String validate()
  {
    if (getProject().isRequireAssetExtension())
    {
      int lastDot = getName().lastIndexOf('.');
      if (lastDot == -1)
        return "Assets require a filename extension";
      if (!getName().substring(lastDot).equals(".task"))
        return getLanguage() + " assets must have .task extension.";
    }
    return null;
  }

  private static List<String> languages;
  @Override
  public List<String> getLanguages()
  {
    if (languages == null)
    {
      languages = new ArrayList<String>();
      languages.add(AUTOFORM);
      languages.add(CUSTOM);
    }
    return languages;
  }
}
