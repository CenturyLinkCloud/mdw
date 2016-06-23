/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.startup;

import java.util.TimerTask;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.status.ApplicationStatusManager;

public class ApplicationStatusMonitor extends TimerTask {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public void run() {
        try {
            ApplicationStatusManager statusMgrObj = new ApplicationStatusManager();
            statusMgrObj.setGlobalApplicationStatus();
        }
        catch (Throwable t) {
            logger.severeException(t.getMessage(), t);
        }
    }
}
