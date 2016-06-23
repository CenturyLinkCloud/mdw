/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.startup;

import java.util.TimerTask;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.event.CertifiedMessageManager;

public class CertifiedMessageMonitor extends TimerTask {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
	 * Invoked periodically by the container
	 */
    @Override
	public void run() {
        try {
            CertifiedMessageManager messageQueue = CertifiedMessageManager.getSingleton();
            messageQueue.retryAll();
        }
        catch (Throwable t) {
            logger.severeException(t.getMessage(), t);
        }
    }


}
