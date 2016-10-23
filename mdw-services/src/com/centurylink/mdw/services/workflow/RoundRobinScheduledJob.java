/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.workflow;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.monitor.LoadBalancedScheduledJob;
import com.centurylink.mdw.services.EventManager;
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
            List<String> serverList = ApplicationContext.getManagedServerList(); // host:8181,host:8282,host:8383,host:8484
            EventManager eventManager = ServiceLocator.getEventManager();
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
                if (nextServerInst.equals(ApplicationContext.getServerHostPort())) {
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
                    logger.debug("Scheduled job running on instance : " + ApplicationContext.getServerHostPort());
                eventManager.updateEventInstance(eventName, null, null, null, null, null, 0, ApplicationContext.getServerHostPort());
                run(args);
            }
        }
        catch (DataAccessException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }
}
