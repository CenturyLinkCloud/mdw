/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import com.centurylink.mdw.model.data.task.TaskAction;

public class TaskActionConverter implements Converter
{
  public Object getAsObject(FacesContext context, UIComponent component, String value)
    throws ConverterException
  {
      if (value == null)
        return null;
      
      TaskAction taskAction = new TaskAction();
      if (value.startsWith("~"))
      {
        taskAction.setDynamic(true);
        taskAction.setTaskActionName(value.substring(1));
      }
      else
      {
        taskAction.setDynamic(false);
        taskAction.setTaskActionName(value);
      }
      
      return taskAction;
  }

  public String getAsString(FacesContext context, UIComponent component, Object value)
    throws ConverterException
  {
    if (value == null || !(value instanceof TaskAction))
      return null;
    
    TaskAction taskAction = (TaskAction) value;
    if (taskAction.isDynamic())
      return "~" + taskAction.getTaskActionName();
    else
      return taskAction.getTaskActionName();
  }

}