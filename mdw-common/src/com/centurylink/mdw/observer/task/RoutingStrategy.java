/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import java.util.List;

import com.centurylink.mdw.common.StrategyException;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskTemplate;

public interface RoutingStrategy extends RegisteredService {

    /**
     * Governs which workgroups a newly-created task instance should be associated with.
     * 
     * @param taskTemplate - the task template definition
     * @param taskInstance - the newly-created task instance
     * @return list of workgroup names as determined by the strategy
     */
    List<String> determineWorkgroups(TaskTemplate taskTemplate, TaskInstance taskInstance) throws StrategyException;
}
