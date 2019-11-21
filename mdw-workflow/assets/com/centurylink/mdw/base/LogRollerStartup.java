package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LogRoller;

@RegisteredService(StartupService.class)
public class LogRollerStartup implements StartupService {
    @Override
    public void onStartup() throws StartupException {
        LogRoller.getInstance().start();
    }

    @Override
    public void onShutdown() {
        LogRoller.getInstance().stop();
    }

    @Override
    public boolean isEnabled() {
        return PropertyManager.getBooleanProperty(PropertyNames.MDW_LOG_ROLLER_ENABLED, false);
    }
}
