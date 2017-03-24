/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.services.task;

import java.util.ArrayList;
import java.util.List;

import javax.el.PropertyNotFoundException;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.variable.Variable;

public class TaskActionValidator {

    private TaskRuntimeContext runtimeContext;

    public TaskActionValidator(TaskRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public void validateAction(UserTaskAction action) throws TaskValidationException {

        if (action.getTaskAction() == null)
            throw new TaskValidationException("Missing task action");
        if (action.getUser() == null)
            throw new TaskValidationException("Missing task action user");

        try {
            TaskAction allowableAction = getAllowableAction(action);
            if (allowableAction == null) {
                StringBuilder msg = new StringBuilder();
                msg.append("Action '").append(action.getTaskAction());
                msg.append("' not allowed by user ").append(action.getUser());
                msg.append(" on task instance: ").append(runtimeContext.getInstanceId());
                if (runtimeContext.getAssignee() != null && !runtimeContext.getAssignee().isEmpty())
                    msg.append(" assigned to ").append(runtimeContext.getAssignee());
                if (!TaskStatus.STATUSNAME_ASSIGNED.equals(runtimeContext.getStatus()))
                    msg.append(" with status ").append(runtimeContext.getStatus());
                msg.append(".");
                throw new TaskValidationException(ServiceException.BAD_REQUEST, msg.toString());
            }
            if (allowableAction.isRequireComment() && (action.getComment() == null || action.getComment().isEmpty())) {
                throw new TaskValidationException(ServiceException.BAD_REQUEST, "Comment required for Action: " + action.getTaskAction()
                        + " for task instance: " + runtimeContext.getInstanceId() + " with status " + runtimeContext.getStatus());
            }
            if (TaskAction.isCompleting(action.getTaskAction())) {
                if (!action.getUser().equals(runtimeContext.getAssignee())) {
                    throw new TaskValidationException(ServiceException.NOT_AUTHORIZED, "Task Instance "
                        + runtimeContext.getInstanceId() + " not assigned to user " + action.getUser());
                }
                TaskTemplate taskTemplate = runtimeContext.getTaskTemplate();
                List<Variable> variables = taskTemplate.getVariables();
                if (variables != null) {
                    List<String> missingRequired = new ArrayList<String>();
                    for (Variable variable : variables) {
                        if (variable.isRequired()) {
                            try {
                                Object value = runtimeContext.getValue(variable.getName());
                                if (value == null || value.toString().isEmpty())
                                    missingRequired.add(variable.getName());
                            }
                            catch (PropertyNotFoundException ex) {
                                runtimeContext.logException(ex.getMessage(), ex);
                                missingRequired.add(variable.getName());
                            }
                        }
                    }
                    if (!missingRequired.isEmpty()) {
                        String missing = "Missing required value(s): ";
                        for (int i = 0; i < missingRequired.size(); i++) {
                            missing += "'" + missingRequired.get(i) + "'";
                            missing += (i < missingRequired.size() - 1) ? "," : ".";
                        }
                        throw new TaskValidationException(ServiceException.BAD_REQUEST, missing);
                    }
                }
            }
        }
        catch (TaskValidationException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new TaskValidationException(ServiceException.INTERNAL_ERROR, "Unable to validate action: " + action.getTaskAction()
                    + " on task instance: " + runtimeContext.getInstanceId(), ex);
        }
    }

    protected TaskAction getAllowableAction(UserTaskAction taskAction) throws Exception {
        for (TaskAction allowableAction : AllowableTaskActions.getTaskDetailActions(taskAction.getUser(), runtimeContext)) {
            if (allowableAction.getTaskActionName().equals(taskAction.getAction().toString()))
                return allowableAction;
        }
        return null;
    }
}
