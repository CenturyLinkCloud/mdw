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

import java.util.Date;
import java.util.TimerTask;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;

/**
 * The timer task that monitors MDW scheduled events.
 */
public class ScheduledEventMonitor extends TimerTask {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Invoked periodically by the timer thread
     */
    @Override
    public void run() {
        try {
            ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
            int batch_size = PropertyManager.getIntegerProperty(PropertyNames.SCHEDULED_EVENTS_BATCH_SIZE, 1000);
            int count = 0;
            Date now = new Date(DatabaseAccess.getCurrentTime());
            logger.log(LogLevel.TRACE, "Processing scheduled events at: " + now);
            ScheduledEvent event = queue.getNextReadyEvent(now);
            while (event!=null && count < batch_size) {
                count++;
                queue.processEvent(event, now);
                event = queue.getNextReadyEvent(now);
            }
        }
        catch (Throwable t) {
            logger.severeException(t.getMessage(), t);
        }
    }

}
