/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks;

import java.util.Date;
import java.util.List;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.detail.ModelWrapper;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;

/**
 * UI model object representing a full task instance, with both the task data
 * and the instance data.  Also can contain the list of columns to be included
 * in the task's list display.
 */
public class FullTaskInstance extends ListItem implements ModelWrapper
{
  /**
   * Initialize from detail manager.
   */
  public FullTaskInstance() throws UIException
  {
    _taskInstance = DetailManager.getInstance().getTaskDetail().getFullTaskInstance().getTaskInstance();
  }

  public FullTaskInstance(TaskInstanceVO ti)
  {
    _taskInstance = ti;
  }

  public FullTaskInstance(long taskInstanceId)
  {
    _taskInstance = new TaskInstanceVO();
    _taskInstance.setTaskInstanceId(new Long(taskInstanceId));
  }

  public Object getWrappedInstance()
  {
    return getTaskInstance();
  }

  private TaskInstanceVO _taskInstance;
  public TaskInstanceVO getTaskInstance() { return _taskInstance; }
  public void setTaskInstance(TaskInstanceVO ti) { _taskInstance = ti; }

  public long getTaskId()
  {
    return getTaskInstance().getTaskId().longValue();
  }

  public Long getInstanceId()
  {
    return getTaskInstance().getTaskInstanceId();
  }

  public Long getId()
  {
    return getTaskInstance().getTaskInstanceId();
  }

  public String getWrappedId()
  {
    return getInstanceId().toString();
  }

  public String getAssociatedInstanceId()
  {
    if (getTaskInstance().getAssociatedTaskInstanceId() == null
        || getTaskInstance().getAssociatedTaskInstanceId().longValue() == 0)
    {
      return null;
    }
    else
    {
      return getTaskInstance().getAssociatedTaskInstanceId().toString();
    }
  }

  public boolean isSummaryOnly()
  {
    return getTaskInstance().isSummaryOnly();
  }

  public boolean isDetailOnly()
  {
    return getTaskInstance().isDetailOnly();
  }

  public String getRemoteDetailUrl() throws UIException
  {
    return getTaskInstance().getTaskInstanceUrl();
  }

  public String getOwnerApplicationName()
  {
    String ownerApp = getTaskInstance().getOwnerApplicationName();
    if (ownerApp == null || ownerApp.equals(TaskInstanceVO.DETAILONLY)) {
      return null;
    }
    else {
      String app = ownerApp;
      int idx = app.indexOf('@');
      if (idx > 0)
        app = ownerApp.substring(0, idx);
      return app;
    }
  }

  public String getCategoryCode()
  {
    return getTaskInstance().getCategoryCode();
  }

  public String getName()
  {
    return getTaskInstance().getTaskName();
  }

  public void setName(String name)
  {
    getTaskInstance().setTaskName(name);
  }

  public String getStatus()
  {
    return new TaskStatuses().decode(new Long(getTaskInstance().getStatusCode().longValue()));
  }

  @Deprecated
  public boolean isInFinalState()
  {
    return isInFinalStatus();
  }

  public boolean isInFinalStatus()
  {
    return getTaskInstance().isInFinalStatus();
  }

  public void setInFinalStatus(boolean inFinalStatus)
  {
    getTaskInstance().setInFinalStatus(inFinalStatus);
  }

  public Integer getStateCode()
  {
    return getTaskInstance().getStateCode();
  }

  public Integer getStatusCode()
  {
    return getTaskInstance().getStatusCode();
  }

  public String getAdvisory()
  {
    return new TaskStates().decodeForAdvisory(new Long(getTaskInstance().getStateCode().longValue()));
  }

  public Date getStartDate()
  {
    return StringHelper.stringToDate(getTaskInstance().getStartDate());
  }

  public void setStartDate(Date startDate)
  {
    getTaskInstance().setStartDate(StringHelper.dateToString(startDate));
  }

  public Date getEndDate()
  {
    return StringHelper.stringToDate(getTaskInstance().getEndDate());
  }

  public void setEndDate(Date endDate)
  {
    getTaskInstance().setEndDate(StringHelper.dateToString(endDate));
  }

  public String getAssignee()
  {
    return getTaskInstance().getTaskClaimUserCuid();
  }

  public void setAssignee(String assignee)
  {
    getTaskInstance().setTaskClaimUserCuid(assignee);
  }

  public UserVO getAssignedUser() throws CachingException
  {
    if (getAssignee() == null)
      return null;
    return UserGroupCache.getUser(getAssignee());
  }

  public String getAssigneeName() throws UIException
  {
    try
    {
      UserVO assignedUser = getAssignedUser();
      return assignedUser == null ? null : assignedUser.getName();
    }
    catch (CachingException ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public Date getDueDate()
  {
    return getTaskInstance().getDueDate();
  }

  public void setDueDate(Date dueDate)
  {
    getTaskInstance().setDueDate(dueDate);
  }

  public String getOrderId()
  {
    return getTaskInstance().getOrderId();
  }

  public String getMasterRequestId()
  {
    return getOrderId();
  }

  public String getComments()
  {
    return getTaskInstance().getComments();
  }

  public void setComments(String comments)
  {
    getTaskInstance().setComments(comments);
  }

  public String getDescription()
  {
    return Tasks.getTask(getTaskInstance().getTaskId()).getComment();
  }

  public String getMessage()
  {
    return getTaskInstance().getActivityMessage();
  }

  public String getActivity()
  {
    return getTaskInstance().getActivityName();
  }


  public Long getProcessInstanceId()
  {
    if (OwnerType.PROCESS_INSTANCE.equals(getOwnerType()))
      return getTaskInstance().getOwnerId();
    else
      return null;
  }

  public String getCreator()
  {
    if (OwnerType.USER.equals(getOwnerType()))
    {
      try
      {
        UserVO user = Users.getUser(getOwnerId());
        return user == null ? null : user.getName();
      }
      catch (CachingException ex)
      {
        ex.printStackTrace();
        return null;
      }
    }
    else
    {
      return null;
    }
  }

  public String getOwnerType()
  {
    return getTaskInstance().getOwnerType();
  }

  public Long getOwnerId()
  {
    return getTaskInstance().getOwnerId();
  }

  public String getSecondaryOwnerType()
  {
    return getTaskInstance().getSecondaryOwnerType();
  }

  public Long getSecondaryOwnerId()
  {
    return getTaskInstance().getSecondaryOwnerId();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (!(o instanceof FullTaskInstance))
      return false;
    return  (this.getTaskInstance()).equals(((FullTaskInstance)o).getTaskInstance());
  }

  /**
   * Special values for task instances are queried variables.
   */
  @Override
  protected Object getAttributeValueSpecial(String name)
  {
    if (getTaskInstance() == null || getTaskInstance().getVariables() == null)
      return null;
    return getTaskInstance().getVariables().get(name);
  }

  public boolean isGeneralTask()
  {
    return OwnerType.DOCUMENT.equals(getSecondaryOwnerType());
  }

  public boolean isAutoformTask()
  {
	return getTaskVO().isAutoformTask();
  }

  public boolean isHasCustomPage()
  {
    return getTaskVO().isHasCustomPage();
  }

  public TaskVO getTaskVO()
  {
    return TaskTemplateCache.getTaskTemplate(_taskInstance.getTaskId());
  }

  public boolean isSubTask()
  {
    return getTaskInstance().isSubTask();
  }

  public Long getMasterTaskInstanceId()
  {
    return getTaskInstance().getMasterTaskInstanceId();
  }

  public void setPriority(int priority) { getTaskInstance().setPriority(priority); }

  public int getPriority()
  {
      return getTaskInstance().getPriority() != null ? getTaskInstance().getPriority() : 0;
  }

  public String getWorkgroups()
  {
    return getTaskInstance().getWorkgroupsString();
  }

  private List<FullTaskInstance> subTaskInstances;
  public List<FullTaskInstance> getSubTaskInstances() { return subTaskInstances; }
  public void setSubTaskInstances(List<FullTaskInstance> stis) { this.subTaskInstances = stis; }
  public boolean isHasSubTaskInstances() { return subTaskInstances != null && !subTaskInstances.isEmpty(); };
}
