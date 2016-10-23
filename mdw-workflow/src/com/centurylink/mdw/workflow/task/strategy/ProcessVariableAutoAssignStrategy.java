/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.strategy;

import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.util.StringHelper;

/**
 * Assign tasks based on the CUID contained in process variable. The variable
 * name is specified as a task attribute. Expressions are also supported (eg:
 * #{myJaxbElem.assigneeField}).
 * Note: when a task assignee attribute is present, reassigning the task also
 * updates the process variable (or expression).
 */
public class ProcessVariableAutoAssignStrategy implements AutoAssignStrategy {

    public User selectAssignee(TaskInstance taskInstanceVO) throws ObserverException {
        TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(taskInstanceVO.getTaskId());
        String assigneeVarSpec = taskVO.getAttribute(TaskAttributeConstant.ASSIGNEE_VAR);
        if (StringHelper.isEmpty(assigneeVarSpec))
            throw new ObserverException("Missing task attribute: " + TaskAttributeConstant.ASSIGNEE_VAR);

        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            TaskRuntimeContext runtimeContext = taskMgr.getTaskRuntimeContext(taskInstanceVO);
            String cuid;
            if (runtimeContext.isExpression(assigneeVarSpec))
                cuid = runtimeContext.evaluateToString(assigneeVarSpec);
            else
                cuid = runtimeContext.getVariables().get(assigneeVarSpec).toString();

            return UserGroupCache.getUser(cuid);
        }
        catch (Exception ex) {
            throw new ObserverException(-1, "Problem auto-assigning task: " + taskInstanceVO.getId()
                    + " based on process variable: " + assigneeVarSpec, ex);
        }
    }
}