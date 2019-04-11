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
package com.centurylink.mdw.observer.task;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.observer.ObserverException;

/**
 * Represents an algorithm for automatically assigning a task instance to a user.
 */
public interface AutoAssignStrategy extends RegisteredService {
    
    /**
     * Finds the next assignee for a task instance.
     * @param taskInstanceVO
     * @return the assignee, or null if no appropriate users
     */
    public User selectAssignee(TaskInstance taskInstanceVO) throws ObserverException;
}
