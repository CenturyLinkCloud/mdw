/*
 * Copyright (C) 2019 CenturyLink, Inc.
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
package com.centurylink.mdw.microservice;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.base.ActivityCache;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.process.ActivityLogger;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Periodically notifies {@link com.centurylink.mdw.activity.types.DependenciesWaitActivity} activities
 * to re-check whether their dependencies have been met, and if so to proceed.
 * Interval is specified on the Configurator Events tab.
 */
@RegisteredService(value=StartupService.class)
public class DependenciesFallbackPublish implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final String DEPENDENCIES_WAIT_IMPL_CATEGORY
            = com.centurylink.mdw.activity.types.DependenciesWaitActivity.class.getName();
    private static final String FALLBACK_CHECK_INTERVAL_ATTRIBUTE = "FallbackCheckInterval";

    private Map<Activity,ScheduledFuture> activitySchedules = new HashMap<>();

    @Override
    public void onStartup() throws StartupException {
        try {
            List<Activity> activities = ActivityCache.getActivities(DEPENDENCIES_WAIT_IMPL_CATEGORY);
            // stagger requests
            int extra = PropertyManager.getIntegerProperty("mdw.dependencies.fallback.extra", 30);
            int delay = extra;
            for (Activity activity : activities) {
                String interval = activity.getAttribute(FALLBACK_CHECK_INTERVAL_ATTRIBUTE);
                if (interval != null) {
                    try {
                        int intervalSecs = Integer.parseInt(interval);
                        if (intervalSecs > 0) {
                            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                            ScheduledFuture schedule = scheduler.scheduleAtFixedRate(() -> {
                                try {
                                    for (ActivityInstance activityInstance : getActivityInstances(activity)) {
                                        doCheckAndNotify(activity, activityInstance);
                                    }
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }, intervalSecs + delay, intervalSecs, TimeUnit.SECONDS);
                            logger.info("Fallback check scheduled at interval = " + intervalSecs + "s for "
                                    + getActivityLabel(activity));
                            activitySchedules.put(activity, schedule);
                            delay += extra;
                        }
                    } catch (Exception ex) {
                        logger.error("Error scheduling fallback check for " + getActivityLabel(activity), ex);
                    }
                }
            }
        } catch (DataAccessException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private static final String WAITING_ACTIVITIES_SQL
            = "select ai.activity_instance_id, pi.process_instance_id from ACTIVITY_INSTANCE ai\n"
            + "  inner join PROCESS_INSTANCE pi on ai.process_instance_id = pi.process_instance_id\n"
            + "  where ai.activity_id = ? and pi.process_id = ? and ai.status_cd = ? and pi.status_cd = ?";

    private List<ActivityInstance> getActivityInstances(Activity activity) throws SQLException {
        List<ActivityInstance> activityInstances = new ArrayList<>();
        try (DbAccess dbAccess = new DbAccess()) {
            ResultSet rs = dbAccess.runSelect(WAITING_ACTIVITIES_SQL, activity.getId(), activity.getProcessId(),
                    WorkStatus.STATUS_WAITING, WorkStatus.STATUS_IN_PROGRESS);
            while (rs.next()) {
                ActivityInstance activityInstance = new ActivityInstance();
                activityInstance.setActivityId(activity.getId());
                activityInstance.setId(rs.getLong("activity_instance_id"));
                activityInstance.setProcessInstanceId(rs.getLong("process_instance_id"));
                activityInstance.setProcessId(activity.getProcessId());
                activityInstance.setPackageName(activity.getPackageName());
                activityInstance.setProcessName(activity.getProcessName());
                activityInstance.setProcessVersion(activity.getProcessVersion());
                activityInstances.add(activityInstance);
            }
            return activityInstances;
        }
    }

    protected void doCheckAndNotify(Activity activity, ActivityInstance activityInstance) throws ServiceException {
        Map<String,String> events = getEvents(activity, activityInstance);
        try {
            insertNeededEventWaits(events, activityInstance);
            WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
            for (String eventName : events.keySet()) {
                int res = workflowServices.notify(eventName, null, 2);
                String msg = "Notified activity instance " + activityInstance.getId() + ": " + eventName
                        + " with result = " + res;
                logger.info(msg);
                LogLevel logLevel = (res == EventInstance.RESUME_STATUS_NO_WAITERS ? LogLevel.DEBUG : LogLevel.INFO);
                ActivityLogger.persist(activityInstance.getProcessInstanceId(), activityInstance.getActivityId(),
                        logLevel, msg, null);
            }
        }
        catch (SQLException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * returns a map of event name to compCode
     */
    protected Map<String,String> getEvents(Activity activity, ActivityInstance activityInstance)
            throws ServiceException {
        Map<String,String> events = new HashMap<>();
        List<String[]> eventSpecs = Attribute.parseTable(activity.getAttribute(WorkAttributeConstant.WAIT_EVENT_NAMES),
                ',', ';', 3);
        ProcessRuntimeContext runtimeContext = ServiceLocator.getWorkflowServices().getContext(
                activityInstance.getProcessInstanceId());
        for (String[] eventSpec : eventSpecs) {
            String eventName = runtimeContext.evaluateToString(eventSpec[0]);
            String compCode = eventSpec[1];
            if (compCode == null || compCode.isEmpty())
                compCode = EventType.EVENTNAME_FINISH;
            events.put(eventName, compCode);
        }
        return events;
    }

    private static final String EVENT_WAIT_SQL = "select event_wait_instance_id from EVENT_WAIT_INSTANCE\n"
            + "where event_name = ? and event_wait_instance_owner = ? and event_wait_instance_owner_id = ?";

    private void insertNeededEventWaits(Map<String,String> events, ActivityInstance activityInstance)
            throws SQLException {
        for (String eventName : events.keySet()) {
            // find out if there's an EVENT_WAIT_INSTANCE
            try (DbAccess dbAccess = new DbAccess()) {
                ResultSet rs = dbAccess.runSelect(EVENT_WAIT_SQL, eventName, OwnerType.ACTIVITY_INSTANCE,
                        activityInstance.getId());
                if (!rs.next()) {
                    EngineDataAccessDB edao = new EngineDataAccessDB();
                    try {
                        edao.getDatabaseAccess().openConnection();
                        edao.createEventWaitInstance(activityInstance.getId(), eventName, events.get(eventName));
                        String msg = "Inserted missing EVENT_WAIT_INSTANCE for activity " + activityInstance.getId()
                                + ": " + eventName;
                        logger.info(msg);
                        ActivityLogger.persist(activityInstance.getProcessInstanceId(),
                                activityInstance.getActivityId(), LogLevel.INFO, msg, null);
                    } finally {
                        edao.getDatabaseAccess().closeConnection();
                    }
                }
            }
        }
    }

    protected String getActivityLabel(Activity activity) {
        return activity.getPackageName() + "/" + activity.getProcessName()
                + " v" + activity.getProcessVersion() + " : " + activity.getLogicalId();
    }

    @Override
    public void onShutdown() {
        for (Activity activity : activitySchedules.keySet()) {
            activitySchedules.get(activity).cancel(true);
        }
    }

    @Override
    public boolean isEnabled() {
        return PropertyManager.getBooleanProperty("mdw.dependencies.fallback.enabled", false);
    }
}
