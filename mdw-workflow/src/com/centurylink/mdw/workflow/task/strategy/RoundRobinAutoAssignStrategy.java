/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class RoundRobinAutoAssignStrategy implements AutoAssignStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<Long,User> latestAssignees = new HashMap<Long,User>();

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
            User lastAssignee = latestAssignees.get(taskInstanceVO.getTaskId());

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

            latestAssignees.put(taskInstanceVO.getTaskId(), assignee);
            return assignee;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }
}
