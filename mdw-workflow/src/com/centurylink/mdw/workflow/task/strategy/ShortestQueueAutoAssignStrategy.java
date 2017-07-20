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

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class ShortestQueueAutoAssignStrategy implements AutoAssignStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public User selectAssignee(TaskInstance taskInstanceVO) throws ObserverException {
        try {
            TaskManager taskManager = ServiceLocator.getTaskManager();
            List<String> groups = taskManager.getGroupsForTaskInstance(taskInstanceVO);
            UserManager userManager = ServiceLocator.getUserManager();
            User[] taskUsers = userManager.getUsersForGroups(groups.toArray(new String[groups.size()]));
            if (taskUsers == null || taskUsers.length == 0) {
                return null;
            }

            User assignee = taskUsers[0];
            int shortest = Integer.MAX_VALUE;
            for (User user : taskUsers) {
                int depth = ServiceLocator.getTaskServices().getTasks(new Query().setFilter("assigneee", user.getCuid())).getCount();
                if (depth < shortest) {
                    assignee = user;
                    shortest = depth;
                }
            }
            return assignee;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }
}
