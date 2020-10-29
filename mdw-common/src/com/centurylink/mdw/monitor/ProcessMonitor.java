package com.centurylink.mdw.monitor;

import java.util.Map;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;

/**
 * Process monitors can be registered through @Monitor annotations to get
 * notified during the lifecycle of an MDW workflow process.
 *
 * Processes may run asynchronously.  Therefore interface methods will
 * be invoked on different instances, and instance-level members should not
 * be stored for access from lifecycle methods.
 */
public interface ProcessMonitor extends RegisteredService, Monitor {

    /**
     * Return map contains variable values to be populated on the process instance.
     * Variables should be defined in the process as INPUT mode.
     * Return null if no variable updates are required.
     */
    default Map<String,Object> onStart(ProcessRuntimeContext context) {
        return null;
    }

    /**
     * Invoked on process completion.
     * Return map is currently not used.
     */
    default Map<String,Object> onFinish(ProcessRuntimeContext context) {
        return null;
    }

    default void onError(ProcessRuntimeContext context) {

    }
}
