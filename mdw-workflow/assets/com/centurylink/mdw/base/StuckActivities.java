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
package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.process.ActivityLogger;
import com.centurylink.mdw.services.workflow.RoundRobinScheduledJob;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * This script fixes stuck activities by failing them and then retrying them
 * A stuck activity means it's "In Progress" status and older than {ActivityAgeInMinutes}
 * Add following to mdw.yaml:
 * <pre>
 *   StuckActivities:
 *     job:
 *       enabled: true   # default=false
 *       scheduler: 30 12 * * ? * # cron expression defining the schedule (example shows daily at 12:30 AM)
 *       ActivityAgeInSeconds: 2000 # minimum age of eligible activities default=1800
 *       MaximumActivities: 100 # maximum number of activities processed per cycle default=10
 * </pre>
 * The query used to identify stuck activities performs a full table scan, so
 * consider creating an index on ACTIVITY_INSTANCE table if it is causing performance issues.
 */
@ScheduledJob(value="StuckActivities", schedule="${props['mdw.StuckActivities.job.scheduler']}", enabled="${props['mdw.StuckActivities.job.enabled']}", defaultEnabled= false, isExclusive=true)
public class StuckActivities extends RoundRobinScheduledJob implements com.centurylink.mdw.model.monitor.ScheduledJob  {

    private static boolean running = false;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void run(CallURL args) {
        if (!running) {
            running = true;
            logger.debug("methodEntry-->StuckActivities.run()");
            try {
                int activityAge = PropertyManager.getIntegerProperty("mdw.StuckActivities.job.ActivityAgeInSeconds", 1800);  // 30 minutes
                int maxActivities = PropertyManager.getIntegerProperty("mdw.StuckActivities.job.MaximumActivities", 10);
                logger.info("StuckActivities run (): with Properties activityAge : " + activityAge + "maxActivities : " + maxActivities);
                List<ActivityInstance> activityList = failStuckActivities(activityAge, maxActivities);
                if (!activityList.isEmpty())
                    retryActivities(activityList);
            } catch (PropertyException e) {
                logger.info("StuckActivities.run() : Properties not found" + e.getMessage());
            } finally {
                running = false;
            }
            logger.info("methodExit-->StuckActivities.run()");
        }
    }

    private List<ActivityInstance> failStuckActivities(int activityAge, int maxActivities) {
        List<ActivityInstance> list = new ArrayList<>();
        EngineDataAccessDB edao = new EngineDataAccessDB();
        DatabaseAccess db = edao.getDatabaseAccess();

        try {
            db.openConnection();

            StringBuilder sqlBuf = new StringBuilder("select ai.activity_instance_id, ai.activity_id, pi.process_instance_id, pi.process_id from ACTIVITY_INSTANCE ai ");
            sqlBuf.append("INNER JOIN PROCESS_INSTANCE pi on ai.process_instance_id=pi.process_instance_id ");
            sqlBuf.append("where ai.status_cd=").append(WorkStatus.STATUS_IN_PROGRESS).append(" and ai.start_dt < ? and ");
            sqlBuf.append("pi.status_cd not in (").append(WorkStatus.STATUS_COMPLETED).append(",").append(WorkStatus.STATUS_CANCELLED).append(") ");

            if (db.isMySQL())
                sqlBuf.append("limit ").append(maxActivities);
            else
                sqlBuf.append("FETCH FIRST ").append(maxActivities).append(" ROWS ONLY");  // Requires Oracle 12c

            Object arg = new Timestamp(db.getDatabaseTime() - activityAge * 1000);

            // Get activity instances matching criteria
            ResultSet rs = db.runSelect(sqlBuf.toString(), arg);

            List<ActivityInstance> actList = new ArrayList<>();
            while (rs.next()) {
                ActivityInstance act = new ActivityInstance();
                act.setId(rs.getLong(1));
                act.setActivityId(rs.getLong(2));
                act.setProcessInstanceId(rs.getLong(3));
                act.setProcessId(rs.getLong(4));
                actList.add(act);
            }

            Process procDef;
            boolean retry = false;
            // Update activity instances' status
            for (ActivityInstance act : actList) {
                procDef = ProcessCache.getProcess(act.getProcessId());
                if (procDef == null)
                    act.setMessage("Auto-failed by StuckActivities job - No Retry due to missing Process definition");
                else if (procDef.getActivity("A" + act.getActivityId(), false) == null)
                    act.setMessage("Auto-failed by StuckActivities job - No Retry due to missing Activity for Process definition");
                else if (procDef.isService())
                    act.setMessage("Auto-failed by StuckActivities job - No Retry due to being Service Process");
                else {
                    act.setMessage("Auto-failed / Retried by StuckActivities job");
                    retry = true;
                }
                String msg = "Failing stuck activity instance " + act.getId();
                logger.info(msg);
                ActivityLogger.persist(act.getProcessInstanceId(), act.getId(), StandardLogger.LogLevel.INFO, msg);
                edao.setActivityInstanceStatus(act, WorkStatus.STATUS_FAILED, null);
                if (retry)
                    list.add(act);  // Add to list after activity instance is updated
            }
        } catch (Exception e) {
            logger.error("Error while trying to fail stuck activities", e);
        } finally {
            db.closeConnection();
        }
        return list;
    }

    private void retryActivities(List<ActivityInstance> activities) {
        WorkflowServices wfs = ServiceLocator.getWorkflowServices();
        for (ActivityInstance activity : activities) {
            String msg = "Retrying stuck activity instance " + activity.getId();
            logger.info(msg);
            ActivityLogger.persist(activity.getProcessInstanceId(), activity.getId(), StandardLogger.LogLevel.INFO, msg);
            try {
                wfs.actionActivity(activity.getId(), UserAction.Action.Retry.toString(), null, "MDW");
            } catch (ServiceException e) {
                logger.error("Error while retrying activity instance", e);
            }
        }
    }
}
