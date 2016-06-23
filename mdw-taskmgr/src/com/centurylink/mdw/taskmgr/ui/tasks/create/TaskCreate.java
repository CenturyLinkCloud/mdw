/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.create;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.taskmgr.ui.tasks.Tasks;
import com.centurylink.mdw.taskmgr.ui.tasks.categories.TaskCategories;

public class TaskCreate
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();
  
  private long _categoryId = 0;
  private long _taskId = 0;
  private Date _dueDate = null;
  private String _comments = "";
  private String _commentsMaxLength;
  private String _relatedId = null;

  public String getCategoryId()
  {
    return "" + _categoryId;
  }

  public void setCategoryId(long categoryId)
  {
    _categoryId = categoryId; 
  }

  public void setCategoryId(String categoryId)
  {
    _categoryId = Long.parseLong(categoryId); 
  }
  
  public String getTaskId()
  {
    return "" + _taskId;
  }
  
  public void setTaskId(long taskId)
  {
    _taskId = taskId;
  }
  
  public void setTaskId(String taskId)
  {
    if (taskId != null)
    {
      setTaskId(Long.parseLong(taskId));
    }
        
    if (_taskId == 0)
    {
      _dueDate = null;
    }
    else if (getDueDateResetParam())
    {
      _dueDate = Tasks.getDueDateForTask(_taskId);
    }
  }

  public Date getDueDate()
  {
    return _dueDate;
  }

  public void setDueDate(Date dueDate)
  {
    if (!getDueDateResetParam())    
      _dueDate = dueDate;
  }

  public String getComments()
  {
    return _comments;
  }

  public void setComments(String comments)
  {
    _comments = comments;
  }
  
  public void setCommentsMaxLength(String pCommentsMaxLength)
  {
    _commentsMaxLength = pCommentsMaxLength;
  }
  
  public String getCommentsMaxLength()
  {
    try
    {
      if (_commentsMaxLength == null){
        PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
        setCommentsMaxLength(propMgr.getStringProperty("MDWFramework.TaskManagerWeb", "create.task.comments.max.length"));
      }
      return _commentsMaxLength;
    }
    catch (PropertyException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }    
  }
  
  public List<SelectItem> getTaskNamesForSelectedCategoryId()
  {
    if (_categoryId > 0)
      return Tasks.getTaskSelectItemsForCategory(new Long(getCategoryId()), "<Select a Task>");

    /* clear out old selections */
    setTaskId(0);
    _dueDate = null;

    return new ArrayList<SelectItem>();
  }
  
  public List<SelectItem> getTaskCategorySelectItems()
  {
    return TaskCategories.getTaskCategorySelectItems(TaskCategories.getTaskCategories(), "<Select a Category>");
  }

  public boolean isCreateAllowed()
  {
    return (_categoryId > 0 && _taskId > 0);
  }

  public String getRelatedId()
  {
    return _relatedId;
  }
  
  public void setRelatedId(String pRelatedId)
  {
    this._relatedId = pRelatedId;
  }
  
  public boolean getDueDateResetParam()
  {
    String ddReset = (String) FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("dueDateReset");
    return new Boolean(ddReset).booleanValue();
  }
  
}