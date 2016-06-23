/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import com.centurylink.mdw.common.exception.ObserverException;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.UserVO;

/**
 * Represents an algorithm for automatically assigning a task instance to a user.
 */
public interface AutoAssignStrategy extends RegisteredService {
    
    /**
     * Finds the next assignee for a task instance.
     * @param taskInstanceVO
     * @return the assignee, or null if no appropriate users
     */
    public UserVO selectAssignee(TaskInstanceVO taskInstanceVO) throws ObserverException;
}
