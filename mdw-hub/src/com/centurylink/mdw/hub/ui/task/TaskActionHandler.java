/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlForm;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;

import org.richfaces.component.UIDropDownMenu;
import org.richfaces.component.UIMenuGroup;
import org.richfaces.component.UIMenuItem;

import com.centurylink.mdw.hub.jsf.component.ActionMenu;
import com.centurylink.mdw.hub.ui.list.ListMenu;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskAction;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;

public class TaskActionHandler extends TaskActionController {

    private Object target;
    protected Object getTarget() { return target; }

    public void processAction(ActionEvent event) throws AbortProcessingException {
        getErrors().clear();
        target = getTarget(event.getComponent());
        TaskAction taskAction = ((TaskAction) FacesVariableUtil.getValue("taskAction"));
        setTaskAction(taskAction);

        // comments dialog
        if ("submitCommentsHiddenButton".equals(event.getComponent().getId())) {
            UIComponent parent = event.getComponent().getParent();
            for (UIComponent child : parent.getChildren()) {
                if (child instanceof HtmlInputHidden) {
                    HtmlInputHidden hidden = (HtmlInputHidden) child;
                    if (hidden.getId().equals("submitCommentsHiddenAction"))
                        taskAction.setAction(String.valueOf(hidden.getValue()));
                    else if (hidden.getId().equals("submitCommentsHiddenComment"))
                        taskAction.setComment(hidden.getValue() == null ? "" : hidden.getValue().toString());
                    else if (hidden.getId().equals("submitCommentsHiddenItem")) {
                        if (taskAction.getAction().equals(com.centurylink.mdw.model.data.task.TaskAction.ASSIGN))
                            taskAction.setUserId(hidden.getValue() == null ? null : hidden.getValue().toString());
                        else if (taskAction.getAction().equals(com.centurylink.mdw.model.data.task.TaskAction.FORWARD))
                            taskAction.setDestination(hidden.getValue() == null ? null : hidden.getValue().toString());
                    }
                }
            }
        }

        // handle selection from richfaces menu
        if (event.getSource() instanceof UIMenuItem) {
            UIMenuItem item = (UIMenuItem) event.getSource();
            if (item.getParent() instanceof UIMenuGroup) {
                // submenu selection
                UIMenuGroup group = (UIMenuGroup) item.getParent();
                Object nonAlias = group.getAttributes().get("nonAlias");
                taskAction.setAction(nonAlias != null ? nonAlias.toString() : group.getValue().toString());
                nonAlias = item.getData(); // item level
                if (taskAction.getAction().equals(com.centurylink.mdw.model.data.task.TaskAction.ASSIGN))
                    taskAction.setUserId(nonAlias != null ? nonAlias.toString() : item.getValue().toString());
                else if (taskAction.getAction().equals(com.centurylink.mdw.model.data.task.TaskAction.FORWARD))
                    taskAction.setDestination(nonAlias != null ? nonAlias.toString() : item.getValue().toString());
            }
            else {
                Object nonAlias = item.getData();
                taskAction.setAction(nonAlias != null ? nonAlias.toString() : item.getValue().toString());
            }
        }

        if (target == null) {
            throw new IllegalArgumentException("Missing target object for TaskAction.");
        }
    }

    protected Object getTarget(UIComponent component) {
        UIDropDownMenu actionMenu = component.getId().equals("submitCommentsHiddenButton") ? getActionMenuSibling(component) : getActionMenuParent(component);
        return actionMenu.getAttributes().get("target");
    }

    private ActionMenu getActionMenuParent(UIComponent component) {
        if (component.getParent() == null)
            return null;
        else if (component.getParent() instanceof ActionMenu)
            return (ActionMenu) component.getParent();
        else
            return getActionMenuParent(component.getParent());
    }

    private ActionMenu getActionMenuSibling(UIComponent component) {
        if (component.getParent() instanceof HtmlPanelGroup)
            component = component.getParent();
        if (component.getParent() == null)
            return null;
        if (!(component.getParent() instanceof HtmlForm))
            component = component.getParent();
        for (UIComponent sibling : component.getChildren()) {
            if (sibling instanceof ActionMenu && !(sibling instanceof ListMenu))
                return (ActionMenu) sibling;
        }
        return null;
    }
}
