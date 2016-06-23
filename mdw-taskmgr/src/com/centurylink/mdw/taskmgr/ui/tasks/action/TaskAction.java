/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.action;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.web.ui.UIException;

/**
 * Represents an action to be performed on a task instance or a
 * collection of task instances (the view-tier model object).
 */
public class TaskAction
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public static final String DEFAULT_COMMENT = "";
  public static final String DEFAULT_USER = "[Select User]";
  public static final String DEFAULT_DESTINATION = "[Select Destination]";

  private String action;
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }

  private boolean dynamic;
  public boolean isDynamic() { return dynamic; }
  public void setDynamic(boolean dynamic) { this.dynamic = dynamic; }

  private boolean commentRequired;
  public boolean isCommentRequired() { return commentRequired; }
  public void setCommentRequired(boolean commentRequired) { this.commentRequired = commentRequired; }

  public TaskAction()
  {
  }

  public List<SelectItem> getUserList()
  {
    try
    {
      return new Users().list(DEFAULT_USER);
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return null;
    }
  }

  public List<SelectItem> getUserNames() throws CachingException
  {
    List<SelectItem> items = new ArrayList<SelectItem>();
    items.add(new SelectItem(""));
    for (UserVO user : Users.getUsersForUserWorkgroups())
    {
      items.add(new SelectItem(user.getCuid()));
    }
    return items;
  }

  public List<SelectItem> getUserFullNames() throws CachingException
  {
    List<SelectItem> items = new ArrayList<SelectItem>();
    items.add(new SelectItem("[Select User]"));
    for (UserVO user : Users.getUsersForUserWorkgroups())
    {
      items.add(new SelectItem(user.getName()));
    }
    return items;
  }

  public static List<TaskDestination> getTaskDestinations() throws UIException
  {
    List<ForTask> forTasks = TaskActions.getForTasks("Forward");

    TaskDetail taskDetail = DetailManager.getInstance().getTaskDetail();

    if (taskDetail != null && forTasks != null)
    {
      for (int i = 0; i < forTasks.size(); i++)
      {
        ForTask forTask = forTasks.get(i);
        if (forTask.getTaskName().equals(taskDetail.getTaskName()))
          return forTask.getDestinations();
      }
    }

    return null;
  }

  private List<SelectItem> destinationSelectItems;
  public List<SelectItem> getDestinations() throws UIException
  {
    destinationSelectItems = new ArrayList<SelectItem>();
    destinationSelectItems.add(new SelectItem(DEFAULT_DESTINATION, DEFAULT_DESTINATION));
    List<TaskDestination> destinations = getTaskDestinations();
    if (destinations != null)
    {
      for (TaskDestination destination : destinations)
        destinationSelectItems.add(new SelectItem(destination.getName(), destination.getLabel()));
    }

    return destinationSelectItems;
  }

  // some actions apply to a user (eg assign)
  private long userId;
  public String getUserId()
  {
    return "" + userId;
  }
  public void setUserId(String userId)
  {
    this.userId = Long.parseLong(userId);
  }
  public boolean isUserEmpty()
  {
    return userId == 0;
  }

  // some actions require a destination (ie forward)
  private String destination = DEFAULT_DESTINATION;
  public String getDestination() { return destination; }
  public void setDestination(String dest) { this.destination = dest; }

  public boolean isDestinationEmpty()
  {
    return destination == null || destination.trim().length() == 0 || destination.equals(DEFAULT_DESTINATION);
  }

  // some actions require a comment
  private String comment = DEFAULT_COMMENT;
  public String getComment()
  {
    return comment;
  }
  public void setComment(String comment)
  {
    this.comment = comment;
  }
  public boolean isCommentEmpty()
  {
    return comment == null || comment.trim().length() == 0 || comment.equals(DEFAULT_COMMENT);
  }
  public String getDefaultComment()
  {
    return DEFAULT_COMMENT;
  }

  public com.centurylink.mdw.model.data.task.TaskAction getTaskAction()
  {
    com.centurylink.mdw.model.data.task.TaskAction taskAction = new com.centurylink.mdw.model.data.task.TaskAction();
    taskAction.setTaskActionName(getAction());
    taskAction.setDynamic(isDynamic());
    return taskAction;
  }

  public void setTaskAction(com.centurylink.mdw.model.data.task.TaskAction taskAction)
  {
    setAction(taskAction.getTaskActionName());
    setDynamic(taskAction.isDynamic());
  }

  public String getTaskInstanceId() throws UIException
  {
    TaskDetail taskDetail = DetailManager.getInstance().getTaskDetail();
    if (taskDetail == null)
      return null;
    else
      return taskDetail.getInstanceId();
  }

  public void setTaskInstanceId(String instanceId)
  {
    // hidden input for taskInstanceId is only to be read
  }

}
