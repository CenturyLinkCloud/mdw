package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.monitor.ActivityMonitor;

import java.time.Instant;
import java.util.Map;

@Monitor(value="Activity Timing", category=ActivityMonitor.class)
public class ActivityTimingMonitor implements ActivityMonitor {

    @Override
    public Map<String,Object> onStart(ActivityRuntimeContext context) {
        context.logDebug("Start: " + Instant.now());
        return null;
    }
}
