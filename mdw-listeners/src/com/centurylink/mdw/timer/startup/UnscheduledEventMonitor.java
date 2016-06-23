/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.startup;

import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.monitor.UnscheduledEvent;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.pooling.ConnectionPoolRegistration;

/**
 * The timer task that monitors MDW unscheduled events.
 */
public class UnscheduledEventMonitor  extends TimerTask {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    boolean hasRun = false;

    /**
     * Invoked periodically by the timer thread
     */
    @Override
    public void run() {
        try {
            int maxEvents = PropertyManager.getIntegerProperty(PropertyNames.UNSCHEDULED_EVENTS_BATCH_SIZE, 100);
            if (maxEvents == 0 && hasRun) // indicates only run at startup (old behavior)
                return;
            int minAge = PropertyManager.getIntegerProperty(PropertyNames.UNSCHEDULED_EVENTS_MIN_AGE, 3600);
            if (minAge < 300) {
                logger.severe(PropertyNames.UNSCHEDULED_EVENTS_MIN_AGE + " cannot be less than 300 (5 minutes)");
                minAge = 300;
            }

            long curTime = DatabaseAccess.getCurrentTime();
            Date olderThan = new Date(curTime - minAge*1000);
            Date now = new Date(curTime);
            logger.log(LogLevel.TRACE, "Processing unscheduled events older than " + olderThan + " at: " + now);
            int count = processUnscheduledEvents(olderThan, maxEvents);
            if (count > 0)
                logger.log(LogLevel.INFO, "Processing " + count + " unscheduled events at " + new Date(DatabaseAccess.getCurrentTime()));
        }
        catch (Throwable t) {
            logger.severeException(t.getMessage(), t);
        }
    }

    private int processUnscheduledEvents(Date olderThan, int max) throws DataAccessException {
        // load a batch of unscheduled events
        hasRun = true;
        EventManager eventManager = ServiceLocator.getEventManager();
        List<UnscheduledEvent> unscheduledEvents = eventManager.getUnscheduledEventList(olderThan, max);
        List<UnscheduledEvent> leftover = eventManager.processInternalEvents(unscheduledEvents);
        ConnectionPoolRegistration.processUnscheduledEvents(unscheduledEvents);
        return unscheduledEvents.size() - leftover.size();
    }
}