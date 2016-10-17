/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.task;

import java.util.TimerTask;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;

/**
 * Startup monitor for tracking task state.
 */
public class TaskMonitor extends TimerTask {

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void run() {
        logger.info("methodEntry-->TaskMonitor.run()");
        int updatedCount = 0;
        try {
    		TaskManager taskMgr = ServiceLocator.getTaskManager();
            updatedCount = taskMgr.updateTaskInstanceStateAsAlert();
            logger.info("Tasks updated to Alert State=" + updatedCount);
            updatedCount = taskMgr.updateTaskInstanceStateAsJeopardy();
            logger.info("Tasks updated to Jeopardy State=" + updatedCount);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            // throw new RuntimeException(ex);
        }
        logger.info("methodExit-->TaskMonitor.run()");
    }
}
