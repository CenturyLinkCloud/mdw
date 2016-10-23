/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.monitor;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;

/**
 * Process monitors can be registered through @RegisteredService annotations to get
 * notified during the lifecycle of an MDW workflow process.
 */
public interface ProcessMonitor extends RegisteredService {

    /**
     * Return map contains variable values to be populated on the process instance.
     * Variables should be defined in the process as INPUT mode.
     * Return null if no variable updates are required.
     */
    public Map<String,Object> onStart(ProcessRuntimeContext runtimeContext);

    /**
     * Invoked on process completion.
     * Return map is currently not used.
     */
    public Map<String,Object> onFinish(ProcessRuntimeContext runtimeContext);

}
