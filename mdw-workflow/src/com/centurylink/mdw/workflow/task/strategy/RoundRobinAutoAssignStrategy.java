package com.centurylink.mdw.workflow.task.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.AutoAssignStrategy;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class RoundRobinAutoAssignStrategy implements AutoAssignStrategy {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<Long,User> latestAssignees = new HashMap<Long,User>();

    public User selectAssignee(TaskInstance taskInstance) throws ObserverException {
        try {
            List<String> groups = ServiceLocator.getTaskServices().getGroupsForTaskInstance(taskInstance);
            User[] taskUsers = ServiceLocator.getUserServices().getWorkgroupUsers(groups).toArray(new User[0]);
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
            logger.error(ex.getMessage(), ex);
            throw new ObserverException(-1, ex.getMessage(), ex);
        }
    }
}
