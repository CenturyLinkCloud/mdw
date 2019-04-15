package com.centurylink.mdw.dashboard;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.services.system.SystemMetrics;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;

@RegisteredService(StartupService.class)
public class SystemMetricsStartup implements StartupService {

    @Override
    public void onStartup() throws StartupException {
        SystemMetrics.getInstance().activate();
    }

    @Override
    public void onShutdown() {
        SystemMetrics.getInstance().deactivate();
    }

    @Override
    public boolean isEnabled() {
        return PropertyManager.getBooleanProperty("mdw.metrics.autostart", true);
    }
}
