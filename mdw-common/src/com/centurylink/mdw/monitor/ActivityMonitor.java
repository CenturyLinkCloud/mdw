package com.centurylink.mdw.monitor;

import com.centurylink.mdw.common.service.RegisteredService;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;

import java.util.Map;

/**
 * Activity monitors can be registered through @Monitor annotations to get
 * (optionally) called whenever an MDW workflow activity is invoked.
 *
 * Activities may be dehydrated and then subsequently resumed asynchronously.
 * These methods are invoked on different instances, and instance-level members should not
 * be stored for access from lifecycle methods.
 *
 * Values for document variables in the ActivityRuntimeContext are current as of
 * activity start.  If a monitor requires guaranteed up-to-date document values, it
 * should retrieve the document for update using the MDW Java API.
 * TODO: onDelay() for wait/task activities.
 */
public interface ActivityMonitor extends RegisteredService, Monitor {

    /**
     * Called when an activity instance is to be started.
     * @param context the activity workflow context
     * @return optional map containing new or updated process variable values
     */
    default Map<String,Object> onStart(ActivityRuntimeContext context) {
        return null;
    }

    /**
     * Non-null means bypass execution with the returned result code.
     * @return optional map with variable values to set in this activity's process.
     */
    default String onExecute(ActivityRuntimeContext context) {
        return null;
    }

    /**
     * Called when an activity instance is successfully completed.
     * @return optional map with variable values to override in this activity's process.
     */
    default Map<String,Object> onFinish(ActivityRuntimeContext context) {
        return null;
    }

    /**
     * Called when an activity instance fails due to error.
     */
    default void onError(ActivityRuntimeContext context) {
    }

    default void onSuspend(ActivityRuntimeContext context) {
    }

}
