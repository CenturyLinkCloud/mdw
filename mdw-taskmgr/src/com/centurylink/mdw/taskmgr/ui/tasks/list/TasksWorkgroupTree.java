/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.list;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.richfaces.component.UITree;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.taskmgr.ui.filter.FilterManager;
import com.centurylink.mdw.taskmgr.ui.tasks.filter.TaskFilter;
import com.centurylink.mdw.taskmgr.ui.user.UserReport;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.taskmgr.ui.workgroups.WorkgroupTree;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.input.DateRangeInput;

public class TasksWorkgroupTree extends WorkgroupTree
{
  @Override
  public void setGroup(UserGroupVO group) throws UIException
  {
    super.setGroup(group);
    if (group != null)
    {
      UserReport userReport = (UserReport) FacesVariableUtil.getValue("userReport");
      userReport.setName("userTasksChart");
      Map<String,String> params = new HashMap<String,String>();
      params.put("workgroup", group.getName());
      userReport.setParams(params);
    }
  }

  public void setUser(String user) throws UIException, CachingException
  {
    setGroup(null);
    setUser(Users.getUserByCuid(user));
    TaskFilter userTasksFilter = (TaskFilter) FilterManager.getInstance().getUserTasksFilter();
    userTasksFilter.setUserName(user);
    // assumes Today view
    DateRangeInput dueDateInput = (DateRangeInput) userTasksFilter.getInput("dueDate");
    dueDateInput.setToDate(new Date());
  }

  @Override
  public void setScopedUser(ScopedUser scopedUser) throws UIException
  {
    super.setScopedUser(scopedUser);
    TaskFilter userTasksFilter = (TaskFilter) FilterManager.getInstance().getUserTasksFilter();
    userTasksFilter.setUserName(scopedUser.getUserVO().getCuid());
    // assumes Today view
    DateRangeInput dueDateInput = (DateRangeInput) userTasksFilter.getInput("dueDate");
    dueDateInput.setToDate(new Date());
  }

  public Boolean adviseNodeOpened(UITree uiTree)
  {
    if (uiTree.getModelTreeNode().getData() instanceof UserGroupVO)
    {
      UserGroupVO groupVO = (UserGroupVO) uiTree.getModelTreeNode().getData();
      if (groupVO.getName().equals(FacesVariableUtil.getRequestParamValue("tasksGroup")))
        return true;
    }
    else
    {
      return super.adviseNodeOpened(uiTree);
    }

    return null;
  }

  @Override
  public Boolean adviseNodeSelected(UITree uiTree)
  {
    if (uiTree.getModelTreeNode().getData() instanceof ScopedUser)
    {
      ScopedUser sUser = (ScopedUser) uiTree.getModelTreeNode().getData();
      UserVO userVO = sUser.getUserVO();
      String reqUser = (String)FacesVariableUtil.getRequestParamValue("userView");
      if (reqUser != null)
      {
        if (userVO.getCuid().equals(reqUser))
        {
          if (uiTree.getTreeNode().getParent().getData() instanceof UserGroupVO)
          {
            UserGroupVO groupVO = (UserGroupVO) uiTree.getTreeNode().getParent().getData();
            if (groupVO.getName().equals(FacesVariableUtil.getRequestParamValue("tasksGroup")))
              return true;
          }
        }
      }
      else
      {
        return super.adviseNodeSelected(uiTree);
      }
    }
    if (getGroup() != null && uiTree.getModelTreeNode().getData() instanceof UserGroupVO)
      return true;

    return null;
  }
}
