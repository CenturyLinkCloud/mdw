/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.hub.ui.MenuBuilder;
import com.centurylink.mdw.hub.ui.MenuItem;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActions;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskDestination;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.web.ui.UIException;

public class TaskListMenuBuilder implements MenuBuilder {

    private String menuId;
    private List<TaskAction> taskActions;

    public TaskListMenuBuilder(String menuId, List<TaskAction> taskActions) {
        this.menuId = menuId;
        this.taskActions = taskActions;
    }

    public MenuType getType() {
        return MenuType.toolbar;
    }

    public String getImage() {
        return "/images/action.gif";
    }

    public String getLabel() {
        return "Actions";
    }

    public String getStyleClass() {
        return null;
    }

    public String getHandler() {
        return "taskListActionHandler";
    }

    public String getHandlerMethod() {
        return "handleAction";
    }

    public String getExecute() {
        return "@form";
    }

    public String getRender() {
        return "@form :mdwMainMessages";
    }

    public List<MenuItem> getMenu() throws UIException {

        List<MenuItem> items = new ArrayList<MenuItem>();
        for (int i = 0; i < taskActions.size(); i++) {
            TaskAction taskAction = taskActions.get(i);
            String action = taskAction.getTaskActionName();
            MenuItem item;
            if (action.equals(TaskAction.ASSIGN)) {
                String groupId = menuId + "_mdw" + action + "MenuGroup";
                item = new MenuItem(groupId, taskAction.getLabel(), action);
                List<MenuItem> children = new ArrayList<MenuItem>();
                try {
                    for (UserVO user : Users.getUsersForUserWorkgroups()) {
                        MenuItem child = new MenuItem(groupId + "_" + "au" + user.getId() + "_taskActionMenu_go", user.getName(), action);
                        child.setData(user.getId());
                        children.add(child);
                    }
                }
                catch (CachingException ex) {
                    throw new UIException(ex.getMessage(), ex);
                }
                item.setChildren(children);
            }
            else if (action.equals(TaskAction.FORWARD)) {
                String groupId = menuId + "_mdw" + action + "MenuGroup";
                item = new MenuItem(groupId, taskAction.getLabel(), action);
                List<MenuItem> children = new ArrayList<MenuItem>();
                List<TaskDestination> dests = com.centurylink.mdw.taskmgr.ui.tasks.action.TaskAction.getTaskDestinations();
                // sort (TODO: TaskDestination implements Comparable)
                Collections.sort(dests, new Comparator<TaskDestination>() {
                    public int compare(TaskDestination o1, TaskDestination o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                for (int j = 0; j < dests.size(); j++) {
                    TaskDestination dest = dests.get(j);
                    MenuItem child = new MenuItem(groupId + "_" + "ad" + i + "_" + j + "_taskActionMenu_go", dest.getLabel(), action);
                    child.setData(dest.getName());
                    children.add(child);
                }
                item.setChildren(children);
            }
            else {
                item = new MenuItem("a" + i, taskAction.getLabel(), action);
            }
            item.setRequireComment(TaskActions.isBulkCommentRequired(taskAction.getTaskActionName()));
            items.add(item);
        }
        return items;
    }
}
