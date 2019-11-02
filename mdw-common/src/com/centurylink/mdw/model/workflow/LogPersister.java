package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.util.log.StandardLogger;

@FunctionalInterface
public interface LogPersister {
    default void persist(Long processInstanceId, Long instanceId, StandardLogger.LogLevel level, String message) {
        persist(processInstanceId, instanceId, level, message, null);
    }

    void persist(Long processInstanceId, Long instanceId, StandardLogger.LogLevel level, String message, Throwable t);
}
