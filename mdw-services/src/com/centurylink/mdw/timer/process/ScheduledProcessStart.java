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
package com.centurylink.mdw.timer.process;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.listener.ExternalEventHandlerBase;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Clean up old database entries from processes that are older than a specified amount
 */
public class ScheduledProcessStart extends ExternalEventHandlerBase
        implements ScheduledJob {

    private static final String PROCESS_NAME = "ProcessName";

    private StandardLogger logger;

    /**
     * Method that gets invoked periodically by the container
     *
     */
    public void run(CallURL args) {

        logger = LoggerUtil.getStandardLogger();
        String processName = args.getParameters().remove(PROCESS_NAME);

        try {
            Long processId = super.getProcessId(processName);
            ProcessEngineDriver driver = new ProcessEngineDriver();
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
            String timetag = sdf.format(new Date(DatabaseAccess.getCurrentTime()));
            String masterRequestId = "ScheduledProcess."+processName+"."+timetag;
            Long procInstId = driver.startProcess(processId, masterRequestId,
                    OwnerType.SYSTEM, new Long(timetag), args.getParameters(), null, null, null);
            logger.info("[ScheduledProcessStart] Started " + processName
                    + " Process Instance ID = " + procInstId);
        } catch (Exception ex) {
            logger.severeException("[ScheduledProcessStart] Error in starting "
                    + processName, ex);
        }
    }

    /**
     * This is not used - the class extends ExternalEventHandlerBase
     * only to get access to convenient methods
     */
    @Override
    public String handleEventMessage(String msg, Object msgdoc,
            Map<String, String> metainfo) throws EventHandlerException {
        return null;
    }


}
