/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.monitor.AdapterMonitor;
import com.centurylink.mdw.monitor.OfflineMonitor;
import com.centurylink.mdw.observer.ObserverException;
import com.centurylink.mdw.observer.task.TaskNotifier;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class OfflineMonitorTrigger<T extends RuntimeContext> {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private int maxRetries = 1;   // We do not want to hold process flow for BAM messages.  Move on without sending BAM message if no thread available

    private OfflineMonitor<T> monitor;
    private T runtimeContext;

    public OfflineMonitorTrigger(OfflineMonitor<T> monitor, T runtimeContext) {
        this.monitor = monitor;
        this.runtimeContext = runtimeContext;
    }

    public void fire(final String event) {
        if (monitor.handlesEvent(runtimeContext, event)) {
//            int poll_interval = PropertyManager.getIntegerProperty(PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL, PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL_DEFAULT);
            int count = 0;
            ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
            Runnable command = new Runnable() {
                public void run() {
                    if (monitor instanceof AdapterMonitor) {
                        // TODO implement for Adapters
                        throw new IllegalArgumentException("Offline monitoring not supported for AdapterMonitors");
                    }
                    else {
                        if (event.equals(WorkStatus.LOGMSG_START) || event.equals(WorkStatus.LOGMSG_PROC_START))
                            monitor.onStart(runtimeContext);
                        else if (event.equals(WorkStatus.LOGMSG_COMPLETE) || event.equals(WorkStatus.LOGMSG_PROC_COMPLETE))
                            monitor.onFinish(runtimeContext);
                        else if (event.equals(WorkStatus.LOGMSG_FAILED))
                            monitor.onError(runtimeContext);
                    }
                }
            };
            // Wait for an available thread if necessary - Max re-tries
            while (count < maxRetries && !threadPool.execute(ThreadPoolProvider.WORKER_MONITOR, monitor.getClass().getSimpleName(), command)) {
//                try {
                    String msg = ThreadPoolProvider.WORKER_MONITOR + " has no thread available for Offline Monitor " + monitor.getClass().getName();
                    // make this stand out
                    logger.warnException(msg, new Exception(msg));
                    logger.info(threadPool.currentStatus());
                    count++;
//                    Thread.sleep(poll_interval*1000);
//                }
//                catch (InterruptedException e) {}
            }
            if (count == maxRetries)
                logger.severe("Unable to launch monitor thread after " + count + " attempts");
        }
    }

    public void fire(final String event, final String outcome) {
        if (monitor.handlesEvent(runtimeContext, event)) {
//            int poll_interval = PropertyManager.getIntegerProperty(PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL, PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL_DEFAULT);
            int count = 0;
            ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
            Runnable command = new Runnable() {
                public void run() {
                    if (monitor instanceof TaskNotifier) {
                        TaskNotifier taskNotifier = (TaskNotifier) monitor;
                        try {
                            taskNotifier.sendNotice((TaskRuntimeContext)runtimeContext, event, outcome);
                        }
                        catch (ObserverException ex) {
                            logger.severeException(ex.getMessage(), ex);
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Offline monitoring not supported for " + monitor.getClass());
                    }
                }
            };
            // Wait for an available thread if necessary - Max re-tries
            while (count < maxRetries && !threadPool.execute(ThreadPoolProvider.WORKER_MONITOR, monitor.getClass().getSimpleName(), command)) {
//                try {
                    String msg = ThreadPoolProvider.WORKER_MONITOR + " has no thread available for Offline Monitor " + monitor.getClass().getName();
                    // make this stand out
                    logger.warnException(msg, new Exception(msg));
                    logger.info(threadPool.currentStatus());
                    count++;
//                    Thread.sleep(poll_interval*1000);
//                }
//                catch (InterruptedException e) {}
            }
            if (count == maxRetries)
                logger.severe("Unable to launch monitor thread after " + count + " attempts");
        }
    }
}
