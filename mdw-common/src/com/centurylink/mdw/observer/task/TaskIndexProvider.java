/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;

public interface TaskIndexProvider extends RegisteredService {

    /**
     * Collect the task indices for the given runtime context.
     */
    public Map<String,String> collect(TaskRuntimeContext runtimeContext);
}
