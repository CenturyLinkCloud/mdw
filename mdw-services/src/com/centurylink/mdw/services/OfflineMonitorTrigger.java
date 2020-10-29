package com.centurylink.mdw.services;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.model.workflow.WorkStatus.InternalLogMessage;
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

    public void fire(final InternalLogMessage logMessage) {
        if (monitor.handlesEvent(runtimeContext, logMessage.message)) {
            int count = 0;
            ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
            Runnable command = () -> {
                if (monitor instanceof AdapterMonitor) {
                    // TODO implement for Adapters
                    throw new IllegalArgumentException("Offline monitoring not supported for AdapterMonitors");
                }
                else {
                    if (logMessage == InternalLogMessage.ACTIVITY_START || logMessage == InternalLogMessage.PROCESS_START)
                        monitor.onStart(runtimeContext);
                    else if (logMessage == InternalLogMessage.ACTIVITY_COMPLETE || logMessage == InternalLogMessage.PROCESS_COMPLETE)
                        monitor.onFinish(runtimeContext);
                    else if (logMessage == InternalLogMessage.ACTIVITY_FAIL)
                        monitor.onError(runtimeContext);
                }
            };
            // Wait for an available thread if necessary - Max re-tries
            while (count < maxRetries && !threadPool.execute(ThreadPoolProvider.WORKER_MONITOR, monitor.getClass().getSimpleName(), command)) {
                    String msg = ThreadPoolProvider.WORKER_MONITOR + " has no thread available for Offline Monitor " + monitor.getClass().getName();
                    // make this stand out
                    logger.warn(msg, new Exception(msg));
                    logger.info(threadPool.currentStatus());
                    count++;
            }
            if (count == maxRetries)
                logger.error("Unable to launch monitor thread after " + count + " attempts");
        }
    }

    public void fire(final String event, final String outcome) {
        if (monitor.handlesEvent(runtimeContext, event)) {
            int count = 0;
            ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
            Runnable command = () -> {
                if (monitor instanceof TaskNotifier) {
                    TaskNotifier taskNotifier = (TaskNotifier) monitor;
                    try {
                        taskNotifier.sendNotice((TaskRuntimeContext)runtimeContext, event, outcome);
                    }
                    catch (ObserverException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
                else {
                    throw new IllegalArgumentException("Offline monitoring not supported for " + monitor.getClass());
                }
            };
            // Wait for an available thread if necessary - Max re-tries
            while (count < maxRetries && !threadPool.execute(ThreadPoolProvider.WORKER_MONITOR, monitor.getClass().getSimpleName(), command)) {
                    String msg = ThreadPoolProvider.WORKER_MONITOR + " has no thread available for Offline Monitor " + monitor.getClass().getName();
                    // make this stand out
                    logger.warn(msg, new Exception(msg));
                    logger.info(threadPool.currentStatus());
                    count++;
            }
            if (count == maxRetries)
                logger.error("Unable to launch monitor thread after " + count + " attempts");
        }
    }
}
