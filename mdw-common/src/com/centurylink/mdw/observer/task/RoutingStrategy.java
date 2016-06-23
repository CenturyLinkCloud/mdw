/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import java.util.List;

import com.centurylink.mdw.common.exception.StrategyException;
import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;

public interface RoutingStrategy extends RegisteredService {

    /**
     * Governs which workgroups a newly-created task instance should be associated with.
     * 
     * @param taskTemplate - the task template definition
     * @param taskInstance - the newly-created task instance
     * @return list of workgroup names as determined by the strategy
     */
    List<String> determineWorkgroups(TaskVO taskTemplate, TaskInstanceVO taskInstance) throws StrategyException;
}
