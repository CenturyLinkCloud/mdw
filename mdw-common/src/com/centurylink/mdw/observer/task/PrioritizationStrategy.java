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

import java.util.Date;

import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.task.TaskTemplate;

public interface PrioritizationStrategy extends RegisteredService {

    /**
     * Due date is determined before priority (using the same instance).
     * Return null to defer to default calculation based on SLA. 
     */
    public Date determineDueDate(TaskTemplate taskTemplate) throws StrategyException;
    
    /**
     * May be executed repeatedly when due date is changed.
     */
    public int determinePriority(TaskTemplate taskTemplate, Date dueDate) throws StrategyException;
}
