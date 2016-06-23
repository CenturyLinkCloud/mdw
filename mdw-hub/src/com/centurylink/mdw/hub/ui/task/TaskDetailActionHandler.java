/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.task;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;

import com.centurylink.mdw.taskmgr.ui.tasks.action.TaskActions;
import com.centurylink.mdw.taskmgr.ui.tasks.detail.TaskDetail;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class TaskDetailActionHandler extends TaskActionHandler {

    public TaskDetail getTaskDetail() {
        return (TaskDetail) getTarget();
    }

    public void processAction(ActionEvent event) throws AbortProcessingException {
        super.processAction(event);
        FacesVariableUtil.setValue("taskDetailActionHandler", this);
        getTaskAction().setCommentRequired(TaskActions.isCommentRequired(getTaskAction().getAction(),getTaskDetail().getStatus()));
        // determine dynamicism
        getTaskAction().setDynamic(true);
        for (com.centurylink.mdw.model.data.task.TaskAction stdTaskAction : getTaskDetail().getStandardTaskActions()) {
            if (stdTaskAction.getLabel().equals(getTaskAction().getAction())) {
                getTaskAction().setDynamic(false);
                break;
            }
        }
        preProcessTaskDetailAction(event, getTaskDetail());
        processAction(getTarget());

        if (getErrors() != null && getErrors().size() > 0) {
            for (int i = 0; i < getErrors().size(); i++) {
                FacesVariableUtil.addMessage((String) getErrors().get(i));
            }
            throw new AbortProcessingException("Errors in performing task action.");
        }
    }

    public String handleAction() throws UIException {
        return refresh();
    }
}
