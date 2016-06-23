/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.tasks.action;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.el.MethodExpression;
import javax.faces.component.UIComponent;
import javax.faces.event.ActionListener;

import org.richfaces.component.html.HtmlDropDownMenu;
import org.richfaces.component.html.HtmlMenuGroup;
import org.richfaces.component.html.HtmlMenuItem;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class TaskActionMenu extends HtmlDropDownMenu
{
  public TaskActionMenu()
  {

  }

  public TaskActionMenu(String ownerId, List<TaskAction> actions) throws UIException, CachingException
  {
    setValue("Actions >");

    for (int i = 0; i < actions.size(); i++)
    {
      TaskAction taskAction = actions.get(i);
      TaskDetail taskDetail = ownerId.toLowerCase().indexOf("taskdetail") >= 0 ? DetailManager.getInstance().getTaskDetail() : null;
      boolean immediate = taskDetail == null ? false : !TaskActions.isAutosaveEnabled(taskAction.getTaskActionName(), taskDetail.getStatus());
      boolean reqComment = taskDetail == null ? TaskActions.isBulkCommentRequired(taskAction.getTaskActionName()) : TaskActions.isCommentRequired(taskAction.getTaskActionName(), taskDetail.getStatus());

      if (taskAction.getTaskActionName().equals(TaskAction.ASSIGN))
      {
        HtmlMenuGroup menuGroup = new HtmlMenuGroup();
        menuGroup.setId(ownerId + "_mdwAssignMenuGroup");  // needs an id due to RichFaces bug
        menuGroup.setValue(taskAction.getLabel());
        menuGroup.getAttributes().put("nonAlias", taskAction.getTaskActionName());
        // below has no effect (https://issues.jboss.org/browse/RF-10218)
        menuGroup.setDirection("right-down");
        menuGroup.getAttributes().put("jointPoint", "br");
        getChildren().add(menuGroup);
        //  if taskDetail, only users from groups for this task instance
        UserVO[] uservos = null;
        if (taskDetail != null) {
        	List<String> groups = taskDetail.getFullTaskInstance().getTaskInstance().getGroups();
        	if (groups == null || groups.size() == 0) {
        		uservos = Users.getUsersForUserWorkgroups();
        	} else {
                uservos = Users.getUsersForWorkgroups(groups.toArray(new String[0]));
        	}
        } else {
        	uservos = Users.getUsersForUserWorkgroups();
        }
        for (UserVO user : uservos)
        {
          addMenuItem(menuGroup, TaskAction.ASSIGN, user.getName(), "au" + user.getId(), user.getId(), immediate, reqComment);
        }
      }
      else if (taskAction.getTaskActionName().equals(TaskAction.FORWARD))
      {
        HtmlMenuGroup menuGroup = new HtmlMenuGroup();
        menuGroup.setId(ownerId + "_mdwForwardMenuGroup"); // needs an id due to RichFaces bug
        menuGroup.setValue(taskAction.getLabel());
        menuGroup.getAttributes().put("nonAlias", taskAction.getTaskActionName());
        // below has no effect (https://issues.jboss.org/browse/RF-10218)
        menuGroup.setDirection("right-down");
        getChildren().add(menuGroup);
        List<TaskDestination> dests = com.centurylink.mdw.taskmgr.ui.tasks.action.TaskAction.getTaskDestinations();
        // sort
        Collections.sort(dests,new Comparator<TaskDestination>() {

				@Override
				public int compare(TaskDestination o1, TaskDestination o2) {
					return o1.getName().compareTo(o2.getName());
				}
			}
		);
        String task_group = null;
        if (taskDetail != null) {
        	List<String> groups = taskDetail.getFullTaskInstance().getTaskInstance().getGroups();
        	if(groups != null && groups.size() == 1) {
        		task_group = groups.get(0);
        	}
        }
        for (int j = 0; j < dests.size(); j++) {
          TaskDestination dest = dests.get(j);
          if (! StringHelper.isEmpty(task_group) && task_group.equals(dest.getName())) {
        	  continue;
          }
          addMenuItem(menuGroup, TaskAction.FORWARD, dest.getLabel(), "ad" + i + "_" + j, dest.getName(), immediate, reqComment);
        }
      }
      else
      {
        addMenuItem(this, taskAction.getTaskActionName(), taskAction.getLabel(), "a" + i, taskAction.getTaskActionName(), immediate, reqComment);
      }
    }
  }

  protected void addMenuItem(UIComponent parent, String action, String label, String seqId, Object data, boolean immediate, boolean reqComment)
  {
    HtmlMenuItem menuItem = new HtmlMenuItem();
    menuItem.setId(parent.getClientId() + "_" + seqId + "_taskActionMenu_go");
    menuItem.setValue(label);
    menuItem.setData(data);
    MethodExpression me = FacesVariableUtil.createMethodExpression("#{taskActionController.refresh}", String.class, null);
    menuItem.setActionExpression(me);
    menuItem.addActionListener((ActionListener)FacesVariableUtil.getValue("taskActionController"));
    parent.getChildren().add(menuItem);
    menuItem.setImmediate(immediate);
    if (reqComment)
      menuItem.setOnselect("showCommentDialog('" + action + "', '" + data + "'); return false;");
  }

}
