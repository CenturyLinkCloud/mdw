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
package com.centurylink.mdw.workflow.task.strategy;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

/**
 * Assign tasks based on the CUID contained in process variable. The variable
 * name is specified as a task attribute. Expressions are also supported (eg:
 * #{myJaxbElem.assigneeField}).
 * Note: when a task assignee attribute is present, reassigning the task also
 * updates the process variable (or expression).
 */
public class ProcessVariableAutoAssignStrategy implements AutoAssignStrategy {

    public User selectAssignee(TaskInstance taskInstance) throws ObserverException {
        try {
            TaskTemplate taskTemplate = TaskTemplateCache.getTaskTemplate(taskInstance.getTaskId());
            String assigneeVarSpec = taskTemplate.getAttribute(TaskAttributeConstant.ASSIGNEE_VAR);
            if (StringUtils.isBlank(assigneeVarSpec))
                throw new ObserverException("Missing task attribute: " + TaskAttributeConstant.ASSIGNEE_VAR);

            TaskRuntimeContext runtimeContext = ServiceLocator.getTaskServices().getContext(taskInstance);
            String cuid;
            if (TaskRuntimeContext.isExpression(assigneeVarSpec))
                cuid = runtimeContext.evaluateToString(assigneeVarSpec);
            else
                cuid = runtimeContext.getValues().get(assigneeVarSpec).toString();

            return UserGroupCache.getUser(cuid);
        }
        catch (IOException | ServiceException ex) {
            throw new ObserverException(-1, "Problem auto-assigning task instance " + taskInstance.getId(), ex);
        }
    }
}