package com.centurylink.mdw.monitor;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.workflow.RuntimeContext;

public interface Monitor {

    default boolean isOffline() {
        return this instanceof OfflineMonitor<?>;
    }

    default boolean isEnabled(RuntimeContext context) {
        com.centurylink.mdw.annotations.Monitor annotation =
                getClass().getAnnotation(com.centurylink.mdw.annotations.Monitor.class);
        if (annotation == null) {
            return true; // old-style global
        }
        else {
            String attr = context.getAttribute(WorkAttributeConstant.MONITORS);
            if (attr == null) {
                // not explicity specified in attribute
                return annotation.defaultEnabled();
            }
            else {
                return new MonitorAttributes(attr).isEnabled(this.getClass().getName());
            }
        }
    }
}
