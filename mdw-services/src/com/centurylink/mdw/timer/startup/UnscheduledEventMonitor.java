/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.timer.startup;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.monitor.UnscheduledEvent;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.services.process.InternalEventDriver;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

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
                logger.error(PropertyNames.UNSCHEDULED_EVENTS_MIN_AGE + " cannot be less than 300 (5 minutes)");
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
            logger.error(t.getMessage(), t);
        }
    }

    private int processUnscheduledEvents(Date olderThan, int max) throws DataAccessException {
        // load a batch of unscheduled events
        hasRun = true;
        List<UnscheduledEvent> unscheduledEvents = getUnscheduledEventList(olderThan, max);
        List<UnscheduledEvent> leftover = processInternalEvents(unscheduledEvents);
        return unscheduledEvents.size() - leftover.size();
    }

    /**
     * Load all internal events older than the specified time up to a max of batchSize.
     */
    public List<UnscheduledEvent> getUnscheduledEventList(Date olderThan, int batchSize) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getUnscheduledEventList(olderThan, batchSize);
        } catch (SQLException e) {
            throw new DataAccessException(-1, "Failed to get unscheduled event list", e);
        } finally {
            edao.stopTransaction(transaction);
        }
    }


    public List<UnscheduledEvent> processInternalEvents(List<UnscheduledEvent> eventList) {
        List<UnscheduledEvent> returnList = new ArrayList<>();
        ThreadPoolProvider thread_pool = ApplicationContext.getThreadPoolProvider();
        for (UnscheduledEvent one : eventList) {
            if (EventInstance.ACTIVE_INTERNAL_EVENT.equals(one.getReference())) {
                InternalEventDriver command = new InternalEventDriver(one.getName(), one.getMessage());
                if (!thread_pool.execute(ThreadPoolProvider.WORKER_SCHEDULER, one.getName(), command)) {
                    String msg = ThreadPoolProvider.WORKER_SCHEDULER + " has no thread available for Unscheduled event: " + one.getName() + " message:\n" + one.getMessage();
                    // make this stand out
                    logger.warn(msg, new Exception(msg));
                    logger.info(thread_pool.currentStatus());
                    returnList.add(one);
                }
            }
            else
                returnList.add(one);
        }
        return returnList;
    }
}