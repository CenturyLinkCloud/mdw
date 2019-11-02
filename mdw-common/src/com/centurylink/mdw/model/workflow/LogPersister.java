package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.util.log.StandardLogger;

@FunctionalInterface
public interface LogPersister {
    default void persist(Long instanceId, StandardLogger.LogLevel level, String message) {
        persist(instanceId, level, message, null);

    }

    void persist(Long instanceId, StandardLogger.LogLevel level, String message, Throwable t);
}
