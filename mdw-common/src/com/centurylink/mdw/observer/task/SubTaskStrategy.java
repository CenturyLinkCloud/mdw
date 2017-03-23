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

import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.task.TaskRuntimeContext;

public interface SubTaskStrategy extends RegisteredService {

    /**
     * Governs which subtasks and how many are spawned by a master task.
     *
     * @param masterTaskContext - for the newly-created task instance
     * @return String which should be a valid SubTaskPlan XML document.
     */
    String getSubTaskPlan(TaskRuntimeContext masterTaskContext) throws StrategyException;
}