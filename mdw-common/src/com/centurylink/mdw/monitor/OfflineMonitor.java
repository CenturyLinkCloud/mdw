package com.centurylink.mdw.monitor;

import java.util.Map;

import com.centurylink.mdw.model.workflow.RuntimeContext;

/**
 * Indicates that a monitor should be run in a separate thread so as not
 * to bog down mainstream workflow processing.  The onStart() method has
 * a return value compatible with process and activity monitors, but when
 * run as offline, the return value is ignored.
 */
public interface OfflineMonitor<T extends RuntimeContext> {

    boolean handlesEvent(T context, String event);

    Map<String,Object> onStart(T context);
    Map<String,Object> onFinish(T context);
    void onError(T context);

}
