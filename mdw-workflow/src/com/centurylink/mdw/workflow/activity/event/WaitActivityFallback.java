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
package com.centurylink.mdw.workflow.activity.event;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.activity.ActivityCache;
import com.centurylink.mdw.services.process.ActivityLogger;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Periodically queries waiting activities to perform fallback processing to fix stuck instances.
 * Check interval is specified on the Configurator Events tab (see {@link #getCheckIntervalAttribute()}).
 * Activity pagelet needs to be updated accordingly.
 */
public abstract class WaitActivityFallback implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Returns the activity implementor class name whose waits are to be checked.
     */
    public abstract String getImplementor();

    public abstract void process(Activity activity, ActivityInstance activityInstance) throws ServiceException;

    /**
     * Override this method to enable the startup service that performs fallback checks.
     */
    public boolean isEnabled() {
        return false;
    }

    /**
     * This is the activity design attribute that specifies the check interval.
     */
    public String getCheckIntervalAttribute() {
        return "FallbackCheckInterval";
    }

    /**
     * Each activity in all process definitins gets a separate thread/schedule for processing.
     * This returns the staggering interval between scheduler initiation for each defined activity.
     */
    public int getStagger() {
        return PropertyManager.getIntegerProperty(PropertyNames.MDW_WAIT_FALLBACK_STAGGER, 30);
    }

    /**
     * Max batch size per check cycle.  From property "mdw.wait.fallback.max", with default of 1000.
     */
    public int getMaxActivities() {
        return PropertyManager.getIntegerProperty(PropertyNames.MDW_WAIT_FALLBACK_MAX, 1000);
    }

    /**
     * Min age for activity to be processed (in seconds).
     * From property "mdw.wait.fallback.age", with default of 300.
     */
    public int getMinActivityAge() {
        return PropertyManager.getIntegerProperty(PropertyNames.MDW_WAIT_FALLBACK_AGE, 300);
    }

    private Map<Activity,ScheduledFuture> activitySchedules = new HashMap<>();

    /**
     *  Per cycle, duplicate events will not be processed.
     */
    private Map<String,String> uniqueEvents = new HashMap<>();

    @Override
    final public void onStartup() throws StartupException {
        try {
            List<Activity> activities = getActivities();
            int extra = getStagger();
            int delay = extra;
            for (Activity activity : activities) {
                String interval = activity.getAttribute(getCheckIntervalAttribute());
                if (interval != null) {
                    try {
                        int intervalSecs = Integer.parseInt(interval);
                        if (intervalSecs > 0) {
                            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new FallbackThreadFactory(activity));
                            ScheduledFuture schedule = scheduler.scheduleAtFixedRate(() -> {
                                try {
                                    for (ActivityInstance activityInstance : getActivityInstances(activity)) {
                                        try {
                                            process(activity, activityInstance);
                                        }
                                        catch (Exception ex) {
                                            String msg = "Processing failed for " + getActivityLabel(activity);
                                            logger.error(msg, ex);
                                            ActivityLogger.persist(activityInstance.getProcessInstanceId(), activityInstance.getId(), LogLevel.ERROR, msg, ex);
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }, intervalSecs + delay, intervalSecs, TimeUnit.SECONDS);
                            logger.info("Fallback check scheduled at interval = " + intervalSecs + "s for " + getActivityLabel(activity));
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

    @Override
    final public void onShutdown() {
        for (Activity activity : activitySchedules.keySet()) {
            activitySchedules.get(activity).cancel(true);
        }
    }

    protected String getActivityLabel(Activity activity) {
        return activity.getPackageName() + "/" + activity.getProcessName()
                + " v" + activity.getProcessVersion() + " : " + activity.getLogicalId();
    }

    /**
     * Returns activities from non-archived process definitions which need to be periodically processed.
     */
    protected List<Activity> getActivities() throws DataAccessException {
        return ActivityCache.getActivities(getImplementor(), isIncludeArchived());
    }

    protected boolean isIncludeArchived() {
        return PropertyManager.getBooleanProperty(PropertyNames.MDW_WAIT_FALLBACK_ARCHIVED, false);
    }

    private static final String WAITING_ACTIVITIES_SQL
            = "select ai.activity_instance_id, ai.start_dt, ai.end_dt, pi.process_instance_id, pi.master_request_id\n"
            + " from ACTIVITY_INSTANCE ai inner join PROCESS_INSTANCE pi on ai.process_instance_id = pi.process_instance_id\n"
            + "  where ai.activity_id = ? and pi.process_id = ? and ai.status_cd = ? and pi.status_cd = ? and ai.start_dt < ?\n"
            + " order by ai.activity_instance_id\n";

    /**
     * This retrieves waiting activities matching the Activity definition whose age is older than
     * {@link #getMinActivityAge()}.  Batch size is governed by {@link #getMaxActivities()}.
     */
    protected List<ActivityInstance> getActivityInstances(Activity activity) throws SQLException {
        List<ActivityInstance> activityInstances = new ArrayList<>();
        String sql = WAITING_ACTIVITIES_SQL;
        // oldest first
        sql += "  ";

        int maxActivities = getMaxActivities();
        if (new DatabaseAccess(null).isOracle()) {
            sql += " fetch first " + maxActivities + " rows only\n";
        }
        else {
            sql += " limit " + maxActivities + "\n";
        }

        long cutoff = DatabaseAccess.getCurrentTime() - getMinActivityAge() * 1000;

        try (DbAccess dbAccess = new DbAccess()) {
            ResultSet rs = dbAccess.runSelect(sql, activity.getId(), activity.getProcessId(),
                    WorkStatus.STATUS_WAITING, WorkStatus.STATUS_IN_PROGRESS, new Timestamp(cutoff));
            while (rs.next()) {
                ActivityInstance activityInstance = new ActivityInstance();
                activityInstance.setActivityId(activity.getId());
                activityInstance.setProcessId(activity.getProcessId());
                activityInstance.setPackageName(activity.getPackageName());
                activityInstance.setProcessName(activity.getProcessName());
                activityInstance.setProcessVersion(activity.getProcessVersion());
                activityInstance.setId(rs.getLong("activity_instance_id"));
                activityInstance.setProcessInstanceId(rs.getLong("process_instance_id"));
                activityInstance.setMasterRequestId(rs.getString("master_request_id"));
                activityInstance.setStartDate(rs.getTimestamp("start_dt"));
                activityInstance.setEndDate(rs.getTimestamp("end_dt"));
                activityInstances.add(activityInstance);
            }
            return activityInstances;
        }
    }

    class FallbackThreadFactory implements ThreadFactory {
        private String prefix;
        FallbackThreadFactory(Activity activity) {
            prefix = (activity.getProcessName() + "v" + activity.getProcessVersion() + ":" + activity.getLogicalId()).replace(' ', '_');
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            t.setName(prefix + t.getId());
            return t;
        }
    }
}
