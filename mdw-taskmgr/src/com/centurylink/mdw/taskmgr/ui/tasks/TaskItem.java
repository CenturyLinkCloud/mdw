/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.data.common.TimeInterval;
import com.centurylink.mdw.model.data.task.TaskType;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.task.cache.TaskCategoryCache;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.taskmgr.ui.EditableItem;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.categories.TaskCategories;
import com.centurylink.mdw.taskmgr.ui.tasks.variable.TaskVariables;
import com.centurylink.mdw.taskmgr.ui.workgroups.Workgroups;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Wraps a model TaskVO instance to provide the list item functionality
 * for dynamically displaying columns according to the layout configuration.
 */
public class TaskItem extends ListItem implements EditableItem
{

  public static final String ITEM_BEAN = "taskItem";
  private TaskVO taskVO;
  private TimeInterval sla;
  private TimeInterval alert;

  public TaskItem()
  {
    taskVO = new TaskVO();
  }

  public TaskItem(TaskVO task)
  {
    taskVO = task;
  }

  public void setTask(TaskVO task)
  {
    taskVO = task;
  }

  public TaskVO getTask()
  {
    return taskVO;
  }

  public Long getId()
  {
    return taskVO.getTaskId();
  }

  public String getName()
  {
    return taskVO.getTaskName();
  }

  public void setName(String name)
  {
    taskVO.setTaskName(name);
  }

  public String getComment()
  {
    return taskVO.getComment();
  }

  public void setComment(String comment)
  {
    taskVO.setComment(comment);
  }

  public String getTaskCategoryId()
  {
	Long catId = TaskCategoryCache.getTaskCategoryId(taskVO.getTaskCategory());
	if (catId==null) return null;
    return catId.toString();
  }

  public String getTaskCategory() throws UIException
  {
	Long catId = TaskCategoryCache.getTaskCategoryId(taskVO.getTaskCategory());
    return new TaskCategories().decode(catId);
  }

  public String getTaskCategoryCode() throws UIException
  {
    return taskVO.getTaskCategory();
  }

  public void setTaskCategory(String pTaskCategory) throws UIException
  {
    taskVO.setTaskCategory(pTaskCategory);
  }

  public void setTaskCategoryId(String pCatId)
  {
	String category = TaskCategoryCache.getTaskCategoryCode(new Long(pCatId));
    taskVO.setTaskCategory(category);
  }

  public String getTaskType()
  {
    if (TaskType.TASK_TYPE_GUI.toString().equals(getTaskTypeId()))
      return "GUI";
    else if (TaskType.TASK_TYPE_WORKFLOW.toString().equals(getTaskTypeId()))
      return "Workflow";
    else
      return null;
  }

  public String getTaskTypeId()
  {
    if (taskVO.getTaskTypeId() == null)
      return null;
    return taskVO.getTaskTypeId().toString();
  }

  public void setTaskTypeId(String pTypeId)
  {
    taskVO.setTaskTypeId(new Integer(pTypeId));
  }

  public TimeInterval getSla()
  {
    if (null != sla)
      return sla;
    if (taskVO.isShallow())
      taskVO = TaskTemplateCache.getTaskTemplate(getId());
    return sla = new TimeInterval(taskVO.getSlaSeconds());
  }

  public void setSla()
  {
    taskVO.setSlaSeconds(sla.getTotalSeconds());
  }

  public TimeInterval getAlert()
  {
    if (null != alert)
      return alert;
    if (taskVO.isShallow())
      taskVO = TaskTemplateCache.getTaskTemplate(getId());
    return alert = new TimeInterval(taskVO.getAlertIntervalSeconds());
  }

  public void setAlert()
  {
    taskVO.setAlertIntervalSeconds(alert.getTotalSeconds());
  }

  /**
   * Only reason for not calling the getAttribute() method directly on TaskVO is because of the shallow logic
   * present in the getTaskAttributes() method
   */
  private String getAttrValue(String pAttrName) {
    List<AttributeVO> taskAttributes = this.getTaskAttributes();
    if (null == taskAttributes) return null;
    for (AttributeVO attr : taskAttributes) {
      if (attr.getAttributeName().equalsIgnoreCase(pAttrName)) {
        return attr.getAttributeValue();
      }
    }
    return null;
  }

  private void setAttributeValue(String pAttrName, String pAttrVal) {
    taskVO.setAttribute(pAttrName, pAttrVal);
  }

  /**
   * Method that returns all the attributes
   */
  public List<AttributeVO> getTaskAttributes()
  {
    if (taskVO.isShallow())
      taskVO = TaskTemplateCache.getTaskTemplate(getId());
    return taskVO.getAttributes();
  }

  public List<VariableVO> getVariables()
  {
    if (taskVO.isShallow())
      taskVO = TaskTemplateCache.getTaskTemplate(getId());

    return this.taskVO.getVariables();
  }

  public void setVariables(List<VariableVO> variables)
  {
    this.taskVO.setVariables(variables);
  }

  public boolean hasVariable(Long variableId)
  {
    for (VariableVO varVO : getVariables())
    {
      if (varVO.getVariableId().equals(variableId))
        return true;
    }
    return false;
  }

  public String getLogicalId()
  {
    return taskVO.getLogicalId();
  }

  public void setLogicalId(String logicalId)
  {
    taskVO.setLogicalId(logicalId);
  }

  public String getGroups()
  {
    return this.getAttrValue(TaskAttributeConstant.GROUPS);
  }

  public void setGroups(String groups)
  {
    this.setAttributeValue(TaskAttributeConstant.GROUPS, groups);
  }

  public String getNotices()
  {
    return this.getAttrValue(TaskAttributeConstant.NOTICES);
  }

  public void setNotices(String notices)
  {
    this.setAttributeValue(TaskAttributeConstant.NOTICES, notices);
  }

  public String getNoticeGroups()
  {
    return this.getAttrValue(TaskAttributeConstant.NOTICE_GROUPS);
  }

  public void setNoticeGroups(String noticeGroups)
  {
    this.setAttributeValue(TaskAttributeConstant.NOTICE_GROUPS, noticeGroups);
  }

  public String getNoticeRecipients()
  {
    return this.getAttrValue(TaskAttributeConstant.RECIPIENT_EMAILS);
  }

  public void setNoticeRecipients(String noticeRecipients)
  {
    this.setAttributeValue(TaskAttributeConstant.RECIPIENT_EMAILS, noticeRecipients);
  }

  public String getAutoAssign()
  {
    return this.getAttrValue(TaskAttributeConstant.AUTO_ASSIGN);
  }

  public void setAutoAssign(String autoAssign)
  {
    this.setAttributeValue(TaskAttributeConstant.AUTO_ASSIGN, autoAssign);
  }

  public String getPriorityStrategy()
  {
    return this.getAttrValue(TaskAttributeConstant.PRIORITY_STRATEGY);
  }

  public void setPriorityStrategy(String priorityStrategy)
  {
    this.setAttributeValue(TaskAttributeConstant.PRIORITY_STRATEGY, priorityStrategy);
  }

  public String getRoutingStrategy()
  {
    return this.getAttrValue(TaskAttributeConstant.ROUTING_STRATEGY);
  }

  public void setRoutingStrategy(String routingStrategy)
  {
    this.setAttributeValue(TaskAttributeConstant.ROUTING_STRATEGY, routingStrategy);
  }

  public String getSubTaskStrategy()
  {
    return this.getAttrValue(TaskAttributeConstant.SUBTASK_STRATEGY);
  }

  public void setSubTaskStrategy(String subTaskStrategy)
  {
    this.setAttributeValue(TaskAttributeConstant.SUBTASK_STRATEGY, subTaskStrategy);
  }

  public String getIndices()
  {
    return this.getAttrValue(TaskAttributeConstant.INDICES);
  }

  public void setIndices(String indices)
  {
    this.setAttributeValue(TaskAttributeConstant.INDICES, indices);
  }

  private Long _processId;
  public Long getProcessId() { return _processId; }
  public void setProcessId(Long processId) { this._processId = processId; }

  public List<SelectItem> getVariableSelectItems()
  {
    return TaskVariables.getVariableSelectItems(TaskVariables.getProcessVariables(_processId), "");
  }

  public List<SelectItem> getWorkgroupsForTask()
  {
    TaskVO task = getTask();

    return Tasks.getUserGroupSelectItems(task.getUserGroups());
  }

  public List<SelectItem> getWorkgroupsNotForTask()
  {
    TaskVO task = getTask();
    List<String> assignedGroups = task.getUserGroups();
    UserGroupVO[] allGroups = Workgroups.getActiveGroups().toArray(new UserGroupVO[0]);
    // remove assigned groups from list
    List<String> unAssigned = getUnmappedGroups(assignedGroups, allGroups);
    return Tasks.getUserGroupSelectItems(unAssigned);
  }

  public List<SelectItem> getNoticegroupsForTask()
  {
    TaskVO task = getTask();

    return Tasks.getUserGroupSelectItems(task.getNoticeGroups());
  }

  public List<SelectItem> getNoticegroupsNotForTask()
  {
    TaskVO task = getTask();
    List<String> assignedGroups = task.getNoticeGroups();
    UserGroupVO[] allGroups = Workgroups.getActiveGroups().toArray(new UserGroupVO[0]);
    // remove assigned groups from list
    List<String> unAssigned = getUnmappedGroups(assignedGroups, allGroups);
    return Tasks.getUserGroupSelectItems(unAssigned);
  }

  public List<String> getUnmappedGroups(List<String> assignedGroups, UserGroupVO[] allGroups)
  {
    List<String> unAssigned = new ArrayList<String>();
    for (UserGroupVO allGroup : allGroups)
    {
      boolean found = false;
      if (null != assignedGroups)
      {
        for (String assignedGroup : assignedGroups)
        {
          if (allGroup.getName().equals(assignedGroup))
          {
            found = true;
            break;
          }
        }
      }
      if (!found)
      {
        unAssigned.add(allGroup.getName());
      }
    }
    return unAssigned;
  }

  /**
   * Method that adds the Item
   */
  public void add() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.createTask(taskVO, false);
      auditLogUserAction(Action.Create, Entity.Task, getId(), getName());
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  /**
   * Method that removes the Item
   */
  public void delete() throws UIException
  {
    try
    {
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.deleteTask(this.taskVO.getTaskId());
      auditLogUserAction(Action.Delete, Entity.Task, getId(), getName());
      Tasks.syncList("taskTemplateList");
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  /**
   * Method that updates the item
   */
  public void save() throws UIException
  {
    try
    {
      if (null == taskVO.getTaskTypeId()) {
        taskVO.setTaskTypeId(TaskType.TASK_TYPE_TEMPLATE);
      }
      this.setSla();
      this.setAlert();
      TaskManager taskMgr = RemoteLocator.getTaskManager();
      taskMgr.updateTask(taskVO, true);
      auditLogUserAction(Action.Change, Entity.Task, getId(), getName());
      SortableList.syncList("taskTemplateList");
    }
    catch (Exception ex)
    {
      throw new UIException(ex.getMessage(), ex);
    }
  }

  public boolean isEditableByCurrentUser()
  {
    AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
    return authUser.isInRoleForAnyGroup(UserRoleVO.USER_ADMIN);
  }

  public String getSelectedTab() { return "general"; }
  public void setSelectedTab(String selected)
  {
    // ignored so selection will revert to general
  }

  private List<NoticeOutcome> noticeOutcomes;

  public List<NoticeOutcome> getNoticeOutcomes()
  {
    if (noticeOutcomes == null)
    {
      String attrVal = getAttrValue(TaskAttributeConstant.NOTICES);
      if (attrVal == null)
        attrVal = TaskVO.getDefaultNotices();
      noticeOutcomes = getNoticeOutcomesFromAttributeValue(attrVal);
    }

    return noticeOutcomes;
  }

  private List<NoticeOutcome> getNoticeOutcomesFromAttributeValue(String attrValue)
  {
    List<NoticeOutcome> outcomes = new ArrayList<NoticeOutcome>();
    int columnCount = StringHelper.delimiterColumnCount(attrValue, ",", "\\,");
    List<String[]> rows = StringHelper.parseTable(attrValue, ',', ';', columnCount);
    for (String[] row : rows)
    {
      outcomes.add(new NoticeOutcome(row));
    }
    return outcomes;
  }

  private void setNoticeOutcomeAttributeValue()
  {
    List<String[]> rows = new ArrayList<String[]>();
    for (NoticeOutcome outcome : getNoticeOutcomes())
    {
      rows.add(outcome.toRow());
    }
    setAttributeValue(TaskAttributeConstant.NOTICES, StringHelper.serializeTable(rows));
  }

  public class NoticeOutcome
  {
    private String label;
    public String getLabel() { return label; }

    private String template;
    public String getTemplate() { return template; }
    public void setTemplate(String template)
    {
      this.template = template;
      setNoticeOutcomeAttributeValue();
    }

    private String notifier;
    public String getNotifier() { return notifier; }

    public NoticeOutcome(String[] row)
    {
      label = row[0];
      template = row[1];
      notifier = row.length>3 ?  row[3] : row[2];
    }

    public String[] toRow()
    {
      String[] row = new String[3];
      row[0] = label;
      row[1] = template;
      row[2] = notifier;
      return row;
    }
  }

}
