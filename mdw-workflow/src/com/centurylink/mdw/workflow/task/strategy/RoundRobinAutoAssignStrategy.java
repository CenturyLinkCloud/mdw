/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.task.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.UserManager;

public class RoundRobinAutoAssignStrategy implements AutoAssignStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<Long,UserVO> latestAssignees = new HashMap<Long,UserVO>();

    public UserVO selectAssignee(TaskInstanceVO taskInstanceVO) throws ObserverException {
        try {
			TaskManager taskManager = ServiceLocator.getTaskManager();
        	List<String> groups = taskManager.getGroupsForTaskInstance(taskInstanceVO);
            UserManager userManager = ServiceLocator.getUserManager();
        	UserVO[] taskUsers = userManager.getUsersForGroups(groups.toArray(new String[groups.size()]));
            if (taskUsers == null || taskUsers.length == 0) {
                return null;
            }

            UserVO assignee = taskUsers[0];
            UserVO lastAssignee = latestAssignees.get(taskInstanceVO.getTaskId());

            if (lastAssignee != null) {
                for (int i = 0; i < taskUsers.length; i++) {
                    UserVO user = taskUsers[i];
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
