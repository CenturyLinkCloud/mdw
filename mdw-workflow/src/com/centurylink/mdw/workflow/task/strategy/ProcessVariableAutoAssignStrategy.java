/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.strategy;

import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;

/**
 * Assign tasks based on the CUID contained in process variable. The variable
 * name is specified as a task attribute. Expressions are also supported (eg:
 * #{myJaxbElem.assigneeField}).
 * Note: when a task assignee attribute is present, reassigning the task also
 * updates the process variable (or expression).
 */
public class ProcessVariableAutoAssignStrategy implements AutoAssignStrategy {

    public UserVO selectAssignee(TaskInstanceVO taskInstanceVO) throws ObserverException {
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(taskInstanceVO.getTaskId());
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