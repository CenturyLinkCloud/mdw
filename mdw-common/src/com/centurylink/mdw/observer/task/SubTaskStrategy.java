/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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