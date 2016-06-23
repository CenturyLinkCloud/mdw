/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import java.util.Map;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;

public interface TaskValuesProvider {

    /**
     * Collect task values for the given runtime context.
     * @return a map from name to value object
     */
    public Map<String,Value> collect(TaskRuntimeContext runtimeContext);

    /**
     * Apply values
     * @param runtimeContext
     * @param values
     * @return map of changed variables
     */
    public void apply(TaskRuntimeContext runtimeContext, Map<String,String> values) throws ServiceException;

}
