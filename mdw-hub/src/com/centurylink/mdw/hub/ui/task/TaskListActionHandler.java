/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActions;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class TaskListActionHandler extends TaskActionHandler {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected SortableList getTaskList() {
        return (SortableList) getTarget();
    }

    public void processAction(ActionEvent event) throws AbortProcessingException {
        super.processAction(event);
        FacesVariableUtil.setValue("taskListActionHandler", this);
        getTaskAction().setCommentRequired(TaskActions.isBulkCommentRequired(getTaskAction().getAction()));
        if (getTarget() instanceof TaskDetail) {
            processAction(getTarget());
        } else {
            preProcessTaskListAction(getTaskList());
            processAction(getTaskList().getMarked());
        }
        if (getErrors() != null && getErrors().size() > 0) {
            for (int i = 0; i < getErrors().size(); i++) {
                String error = getErrors().get(i);
                logger.severe("Task action ERROR: " + error);
                FacesVariableUtil.addMessage(error);
            }
            throw new AbortProcessingException("Errors in performing task action.");
        }
    }

    public String handleAction() throws UIException {
        return refresh();
    }
}
