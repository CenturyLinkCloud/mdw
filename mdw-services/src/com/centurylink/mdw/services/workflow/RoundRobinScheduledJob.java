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
package com.centurylink.mdw.services.workflow;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.monitor.LoadBalancedScheduledJob;
import com.centurylink.mdw.services.EventServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public abstract class RoundRobinScheduledJob extends LoadBalancedScheduledJob {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static List<String> serverInstQueue  = new ArrayList<String>();

    public void runOnLoadBalancedInstance(CallURL args) {
        try {
            List<String> serverList = ApplicationContext.getServerList().getHostPortList(); // host:8181,host:8282,host:8383,host:8484
            EventServices eventManager = ServiceLocator.getEventServices();
            String eventName = "ScheduledJob." + this.getClass().getName();
            boolean runOnCurrentInstance = true;

            // round robin logic to find next available server
            if (serverList.size() > 1) {
                EventInstance event = eventManager.getEventInstance(eventName);
                if (event != null && !StringHelper.isEmpty(event.getComments())) {
                    int index = serverList.indexOf(event.getComments());
                    if (index > -1) {
                        if (index == serverList.size() - 1) {
                            serverInstQueue = serverList;
                        }
                        else {
                            for (int i = index; i < serverList.size() - 1; i++) {
                                serverInstQueue.add(serverList.get(i + 1));
                            }
                            for (int i = 0; i <= index; i++) {
                                serverInstQueue.add(serverList.get(i));
                            }
                        }
                    }
                }
            }
            for (String nextServerInst : serverInstQueue) {
                boolean success = false;
                if (nextServerInst.equals(ApplicationContext.getServer().toString())) {
                    break;
                } else {
                    success = super.runOnDifferentManagedServer(nextServerInst); // if one instance is offline, user next in the queue
                    if (success) {
                        runOnCurrentInstance = false;
                        break;
                    }
                }
            }
            if (runOnCurrentInstance) {
                if (logger.isDebugEnabled())
                    logger.debug("Scheduled job running on instance : " + ApplicationContext.getServer());
                eventManager.updateEventInstance(eventName, null, null, null, null, null, 0, ApplicationContext.getServer().toString());
                run(args);
            }
        }
        catch (DataAccessException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }
}
