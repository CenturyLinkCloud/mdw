/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.startup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.exception.StartupException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.startup.StartupClass;
import com.centurylink.mdw.model.data.monitor.ScheduledEvent;
import com.centurylink.mdw.services.event.ScheduledEventQueue;

/**
 * Registers all the application timers.
 */
public class TimerTaskRegistration implements StartupClass {

    private static final String PROPERTY_TIMER_CLASS = "TimerClass";
    private static final String PROPERTY_SCHEDULE = "Schedule";  // cron expression, e.g. 0 30 14 * * = 2:30pm

    private static final String CERTIFIED_MESSAGE_JOB = "CertifiedMessageJob";
    private static final String SCHEDULED_JOB = "ScheduledJob";
    private static final String UNSCHEDULED_EVENTS = "UnscheduledEventsTimer";
    private static final String GLOBAL_APPLICATION_STATUS_JOB = "GlobalApplicationStatusJob" ;

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Collection<Timer> _timers;

    /**
     * Invoked when the server starts up.
     */
    public void onStartup() throws StartupException {
        _timers = new ArrayList<Timer>();
        configureTimers();

        startCertifiedMessageMonitor();
    }

    private void configureTimers() throws StartupException {
        try {
            String v = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_MESSENGER);
            int period_default = ("jms".equalsIgnoreCase(v))?60:5;
            int delay = PropertyManager.getIntegerProperty(PropertyNames.MDW_TIMER_INITIAL_DELAY, 120);
            int unschedDelay = PropertyManager.getIntegerProperty(PropertyNames.UNSCHEDULED_EVENTS_CHECK_DELAY, 90);
            int unschedPeriod = PropertyManager.getIntegerProperty(PropertyNames.UNSCHEDULED_EVENTS_CHECK_INTERVAL, 300);
            int period = PropertyManager.getIntegerProperty(PropertyNames.MDW_TIMER_CHECK_INTERVAL, period_default);
            int asPeriod = PropertyManager.getIntegerProperty(PropertyNames.MDW_APPLICATION_STATUS_CHECK_INTERVAL, 300);
            Map<String, Properties> timers = getAllScheduledEvents();

            boolean enableAppStatusMntr = PropertyManager.getBooleanProperty(PropertyNames.MDW_APPLICATION_STATUS_MONITOR, true);
            if (enableAppStatusMntr) {
                ApplicationStatusMonitor statusMonitor = new ApplicationStatusMonitor();
                Timer timer =new Timer(GLOBAL_APPLICATION_STATUS_JOB);
                timer.schedule(statusMonitor, delay*1000L, asPeriod*1000L);
                _timers.add(timer);
            }

            ScheduledEventMonitor scheduler = new ScheduledEventMonitor();
            Timer aTimer = new Timer(SCHEDULED_JOB);
            aTimer.schedule(scheduler, delay*1000L, period*1000L);
            _timers.add(aTimer);


            UnscheduledEventMonitor unscheduledEventMonitor = new UnscheduledEventMonitor();
            Timer unscheduledTimer = new Timer(UNSCHEDULED_EVENTS);
            unscheduledTimer.schedule(unscheduledEventMonitor, unschedDelay*1000L, unschedPeriod*1000L);
            _timers.add(unscheduledTimer);

            for (String name : timers.keySet()) {
                Properties timerProps = timers.get(name);
                String schedule = timerProps.getProperty(PROPERTY_SCHEDULE);
                String clsnameAndArgs = timerProps.getProperty(PROPERTY_TIMER_CLASS);
                if (schedule==null || schedule.trim().length()==0) continue;
                ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
                queue.scheduleCronJob(ScheduledEvent.SCHEDULED_JOB_PREFIX + clsnameAndArgs, schedule);
            }
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new StartupException(ex.getMessage());
        }
    }

    // start certified message monitor
    private void startCertifiedMessageMonitor() throws StartupException {
        try {
            int delay = PropertyManager.getIntegerProperty(PropertyNames.MDW_CERTIFIED_MESSAGE_INITIAL_DELAY, 300);
            long period = PropertyManager.getIntegerProperty(PropertyNames.MDW_CERTIFIED_MESSAGE_CHECK_INTERVAL, 60);
            CertifiedMessageMonitor monitor = new CertifiedMessageMonitor();
            Timer aTimer = new Timer(CERTIFIED_MESSAGE_JOB);
            aTimer.schedule(monitor, delay*1000L, period*1000L);
            _timers.add(aTimer);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new StartupException(ex.getMessage());
        }
    }

    public void restartTimers() throws StartupException {
        this.onShutdown();
        this.onStartup();
    }

    public void onShutdown() {
//        logger.info("methodEntry-->TimerTaskRegistration.shutdown()");
        if (_timers == null || _timers.isEmpty()) {
            return;
        }
        Iterator<Timer> it = _timers.iterator();
        while (it.hasNext()) {
            Timer aTimer = (Timer) it.next();
            aTimer.cancel();
        }
//        logger.info("methodExit-->TimerTaskRegistration.shutdown()");

    }

    private Map<String, Properties> getAllScheduledEvents() throws IOException, XmlException, PropertyException {

        Map<String, Properties> timerTasks = new HashMap<String, Properties>();
        Properties timerTasksProperties = PropertyManager.getInstance().getProperties(PropertyNames.MDW_TIMER_TASK);
        for (String pn : timerTasksProperties.stringPropertyNames()) {
            String[] pnParsed = pn.split("\\.");
            if (pnParsed.length==5) {
                String name = pnParsed[3];
                String attrname = pnParsed[4];
                Properties procspec = timerTasks.get(name);
                if (procspec==null) {
                    procspec = new Properties();
                    timerTasks.put(name, procspec);
                }
                String value = timerTasksProperties.getProperty(pn);
                procspec.put(attrname, value);
            }
        }
        return timerTasks;
    }
}
