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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class RoundRobinAutoAssignStrategy implements AutoAssignStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<Long,User> latestAssignees = new HashMap<Long,User>();

    public User selectAssignee(TaskInstance taskInstance) throws ObserverException {
        try {
            List<String> groups = ServiceLocator.getTaskServices().getGroupsForTaskInstance(taskInstance);
            UserManager userManager = ServiceLocator.getUserManager();
            User[] taskUsers = userManager.getUsersForGroups(groups.toArray(new String[groups.size()]));
            if (taskUsers == null || taskUsers.length == 0) {
                return null;
            }

            User assignee = taskUsers[0];
            User lastAssignee = latestAssignees.get(taskInstance.getTaskId());

            if (lastAssignee != null) {
                for (int i = 0; i < taskUsers.length; i++) {
                    User user = taskUsers[i];
                    if (user.getId().equals(lastAssignee.getId())) {
                        if (i < taskUsers.length - 1) {
                            assignee = taskUsers[i+1];
                            break;
                        }
                    }
                }
            }

            latestAssignees.put(taskInstance.getTaskId(), assignee);
            return assignee;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }
}
