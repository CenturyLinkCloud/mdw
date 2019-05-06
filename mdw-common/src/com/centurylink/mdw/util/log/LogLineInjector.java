package com.centurylink.mdw.util.log;

import com.centurylink.mdw.common.service.RegisteredService;

import javax.annotation.Nonnull;

/**
 * Insert into log line for SLF4J SimpleLogger.
 */
@FunctionalInterface
public interface LogLineInjector extends RegisteredService {
    /**
     * Prefix should be enclosed in square brackets so as not to break log watchers.
     */
    String prefix();
    default String suffix() { return null; }
}
