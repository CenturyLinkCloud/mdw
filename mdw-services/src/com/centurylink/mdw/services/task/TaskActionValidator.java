/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.task;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;

public class TaskActionValidator {

    private TaskInstanceVO taskInstance;

    public TaskActionValidator(TaskInstanceVO taskInstance) {
        this.taskInstance = taskInstance;
    }

    public void validateAction(UserVO user, String taskAction) throws TaskValidationException, DataAccessException {
        if (TaskAction.isCompleting(taskAction)) {
            if (!user.getId().equals(taskInstance.getAssigneeId()))
                throw new TaskValidationException(ServiceException.NOT_AUTHORIZED, "Task Instance " + taskInstance.getId() + " not assigned to user " + user.getCuid());
            TaskVO task = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
            List<VariableVO> variables = task.getVariables();
            if (variables != null) {
                List<String> missingRequired = new ArrayList<String>();
                VariableInstanceVO[] variableInsts = null; // don't retrieve unless needed
                for (VariableVO variable : variables) {
                    if (variable.isRequired()) {
                        if (variableInsts == null)
                            variableInsts = ServiceLocator.getTaskManager().getProcessInstanceVariables(taskInstance.getOwnerId());
                        Object value = null;
                        for (VariableInstanceVO variableInst : variableInsts) {
                            if (variableInst.getName().equals(variable.getName()))
                                value = variableInst.getData();
                        }
                        if (value == null || value.toString().isEmpty())
                            missingRequired.add(variable.getName());
                    }
                }
                if (!missingRequired.isEmpty()) {
                    String missing = "Missing required values: ";
                    for (int i = 0; i < missingRequired.size(); i++) {
                        missing += "'" + missingRequired.get(i) + "'";
                        missing += (i < missingRequired.size() - 1) ? "," : ".";
                    }
                    throw new TaskValidationException(ServiceException.BAD_REQUEST, missing);
                }
            }
        }
    }
}
