package com.centurylink.mdw.startup;

import com.centurylink.mdw.common.service.RegisteredService;

/**
 * Registered startup service.
 */
public interface StartupService extends RegisteredService {

    void onStartup() throws StartupException;

    void onShutdown();

    default boolean isEnabled() { return true; }
}
