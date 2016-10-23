/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.monitor;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

/**
 * Activity monitors can be registered through @RegisteredService annotations to get
 * called whenever an MDW workflow activity is invoked.
 */
public interface ActivityMonitor extends RegisteredService {

    /**
     * Called when an activity instance is to be started.
     * @param runtimeContext the activity workflow context
     * @return optional map containing new or updated process variable values
     * TODO: return type void (variables should be passed and set in runtimeContext)
     */
    public Map<String,Object> onStart(ActivityRuntimeContext runtimeContext);

    /**
     * Non-null means bypass execution with the returned result code.
     * @return optional map with variable values to set in this activity's process.
     */
    public String onExecute(ActivityRuntimeContext runtimeContext);

    /**
     * Called when an activity instance is successfully completed.
     * @return optional map with variable values to override in this activity's process.
     */
    public Map<String,Object> onFinish(ActivityRuntimeContext runtimeContext);

    /**
     * Called when an activity instance fails due to error.
     * TODO: make exception available
     */
    public void onError(ActivityRuntimeContext runtimeContext);

}
