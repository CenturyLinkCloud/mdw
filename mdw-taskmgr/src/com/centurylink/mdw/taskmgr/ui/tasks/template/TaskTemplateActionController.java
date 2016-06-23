/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.template;

import javax.faces.event.ValueChangeEvent;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.taskmgr.ui.EditableItemActionController;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.tasks.TaskItem;
import com.centurylink.mdw.taskmgr.ui.workgroups.WorkgroupTree;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class TaskTemplateActionController extends EditableItemActionController
{
  public static final String CONTROLLER_BEAN = "taskTemplateActionController";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private String _selectedUserGroups;
  // a comma separated string of notice groups
  private String _selectedNoticeGroups;

  /**
   * Called as a ValueChangeListener when the ids selected in the usergroup
   * picklists have been updated.
   *
   * @param event the ValueChangeEvent
   */
  public void userGroupValueChanged(ValueChangeEvent event)
  {
    _selectedUserGroups = event.getNewValue().toString();
  }

  /**
   * Called as a ValueChangeListener when the ids selected in noticegroup
   * picklists have been updated.
   *
   * @param event the ValueChangeEvent
   */
  public void noticeGroupValueChanged(ValueChangeEvent event)
  {
    _selectedNoticeGroups = event.getNewValue().toString();
  }

  public String saveTask() throws UIException
  {
    TaskItem taskItem = (TaskItem)FacesVariableUtil.getValue(TaskItem.ITEM_BEAN);
    if (null != _selectedUserGroups) {
      taskItem.setGroups(_selectedUserGroups.replaceAll(",", "#"));
    }
    if (null != _selectedNoticeGroups) {
      taskItem.setNoticeGroups(_selectedNoticeGroups.replaceAll(",", "#"));
    }
    setItem(taskItem);
    taskItem.save();
    return null;
  }

  public String deleteTask() throws UIException
  {
    TaskItem taskItem = (TaskItem)FacesVariableUtil.getValue(TaskItem.ITEM_BEAN);
    setItem(taskItem);
    taskItem.delete();
    return null;
  }


  /**
   * For drilling into task template from task detail page.
   */
  public String displayTaskTemplate()
  {
    try
    {
      TaskVO taskvo = DetailManager.getInstance().getTaskDetail().getFullTaskInstance().getTaskVO();
      WorkgroupTree workgroupTree = WorkgroupTree.getInstance();
      workgroupTree.setTaskDetailTemplate(taskvo);
      FacesVariableUtil.setValue("workgroupTree", workgroupTree);
      return "go_admin";
    }
    catch (Exception ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return "go_error";
    }
  }

  public boolean isEditableByCurrentUser()
  {
    AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
    return authUser.isInRoleForAnyGroup(UserRoleVO.USER_ADMIN);
  }

}
