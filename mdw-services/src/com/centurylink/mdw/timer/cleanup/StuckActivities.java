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
package com.centurylink.mdw.timer.cleanup;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.monitor.ScheduledJob;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.sql.*;
import java.util.*;

/**
 * This script fixes stuck activities by failing them and then retrying them
 * A stuck activity means it's "In Progress" status and older than {ActivityAgeInMinutes}
 * Add following to mdw.yaml
timer.task:
 StuckActivities:
    TimerClass: com.centurylink.mdw.timer.cleanup.StuckActivities
    Schedule: 30 2 * * *    # to run hourly at 30 minutes past the hour use : Schedule: 30 * * * ? *
    ActivityAgeInMinutes: 1800 # How old activity instance should be to be a candidate for fixing
    MaximumActivities: 10  # How many stuck activity instances to be retried in each run

 * if you need to make change in above properties then first delete the db entry by identifying the row using
 * this sql:  select * from event_instance where event_name like '%ScheduledJob%'
 * Then re-start the server/instance for new clean-up properties to be effective.
 *
 * The query used to identify stuck activities will perform a full table scan, so
 * consider creating an index on ACTIVITY_INSTANCE table if it is causing performance issues.
 */

public class StuckActivities implements ScheduledJob {

    private static boolean running = false;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void run(CallURL args) {
        if (!running) {
            running = true;
            logger.info("methodEntry-->StuckActivities.run()");
            int activityAge;  //Seconds
            int maxActivities;
            try {
                Properties cleanupTaskProperties = PropertyManager.getInstance().getProperties(PropertyNames.MDW_TIMER_TASK);
                activityAge = Integer.parseInt(cleanupTaskProperties.getProperty(PropertyNames.MDW_TIMER_TASK + ".StuckActivities.ActivityAgeInSeconds", "1800"));  // 30 minutes
                maxActivities = Integer.parseInt(cleanupTaskProperties.getProperty(PropertyNames.MDW_TIMER_TASK + ".StuckActivities.MaximumActivities", "10"));

                List<Long> activityList = failStuckActivities(activityAge, maxActivities);
                if (!activityList.isEmpty())
                    retryActivities(activityList);
            }
            catch (PropertyException e) {
                logger.info("StuckActivities.run() : Properties not found" + e.getMessage());
            }
            finally {
                running = false;
            }
            logger.info("methodExit-->StuckActivities.run()");
        }
    }

    private List<Long> failStuckActivities(int activityAge, int maxActivities) {
        List<Long> list = new ArrayList<>();
        EngineDataAccessDB edao = new EngineDataAccessDB();
        DatabaseAccess db = edao.getDatabaseAccess();

        try {
            db.openConnection();

            StringBuilder sqlBuf = new StringBuilder("select ai.activity_instance_id, ai.activity_id, pi.process_id from ACTIVITY_INSTANCE ai ");
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
                act.setProcessId(rs.getLong(3));
                actList.add(act);
            }

            Process procDef;
            boolean retry = false;
            // Update activity instances' status
            for (ActivityInstance act : actList) {
                procDef = ProcessCache.getProcess(act.getProcessId());
                if (procDef == null)
                    act.setMessage("Auto-failed by StuckActivities job - No Retry due to missing Process definition");
                else if (procDef.getActivityByLogicalId("A" + act.getActivityId()) == null)
                    act.setMessage("Auto-failed by StuckActivities job - No Retry due to missing Activity for Process definition");
                else if (procDef.isService())
                    act.setMessage("Auto-failed by StuckActivities job - No Retry due to being Service Process");
                else {
                    act.setMessage("Auto-failed / Retried by StuckActivities job");
                    retry = true;
                }
                logger.info("Failing stuck activity instance " + act.getId());
                edao.setActivityInstanceStatus(act, WorkStatus.STATUS_FAILED, null);
                if (retry)
                    list.add(act.getId());  // Add to list after activity instance is updated
            }
        } catch (Exception e) {
            logger.error("Error while trying to fail stuck activities", e);
        } finally {
            db.closeConnection();
        }
        return list;
    }

    private void retryActivities(List<Long> activityList) {
        WorkflowServices wfs = ServiceLocator.getWorkflowServices();
        for (Long id : activityList) {
            logger.info("Retrying stuck activity instance " + id);
            try {
                wfs.actionActivity(id, UserAction.Action.Retry.toString(), null, "MDW");
            } catch (ServiceException e) {
                logger.error("Error while retrying activity instance", e);
            }
        }
    }
}
