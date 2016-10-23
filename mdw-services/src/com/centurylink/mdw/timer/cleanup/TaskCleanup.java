/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.cleanup;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.task.TaskStatuses;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.service.data.task.TaskTemplateCache;
import com.centurylink.mdw.services.workflow.RoundRobinScheduledJob;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Updates task_instance_state to 5 (Invalid) for VCS Asset manual tasks with missing templates.
 * Otherwise, these are only updated in memory (as retrieved), which can lead to confusing
 * results when filtering and sorting task lists.
 */
public class TaskCleanup extends RoundRobinScheduledJob {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final String ACTION_CLEAN = "clean";
    private static final String ACTION_RECOVER = "recover";
    private static final String ARG_JDBC_URL = "jdbcUrl";
    private static final String ARG_ASSET_LOC = "assetLoc";
    private static final String ARG_TASK_REF = "taskRef";
    private static String SELECT = "select TASK_INSTANCE_ID, TASK_ID, TASK_INSTANCE_STATUS, TASK_INSTANCE_REFERRED_AS from TASK_INSTANCE"
            + " where TASK_INSTANCE_STATE != 5 and TASK_INSTANCE_STATUS not in (" + TaskStatus.STATUS_COMPLETED + ", " + TaskStatus.STATUS_CANCELLED + ")";
    private static String UPDATE_CLEAN = "update TASK_INSTANCE set TASK_INSTANCE_STATE = " + TaskState.STATE_INVALID + ", TASK_CLAIM_USER_ID = null where TASK_INSTANCE_ID = ?";
    private static String UPDATE_RECOVER = "update TASK_INSTANCE set TASK_INSTANCE_STATE = " + TaskState.STATE_OPEN + ", TASK_INSTANCE_STATUS = " + TaskStatus.STATUS_OPEN
            + ", TASK_CLAIM_USER_ID = null where TASK_INSTANCE_STATE = " + TaskState.STATE_INVALID + " and TASK_INSTANCE_REFERRED_AS = ?";

    /**
     * Run with no parameters when within the container.
     * If ARG_JDBC_URL is missing, container MDW datasource is used.
     * If ARG_ASSET_LOC is missing then MDW property "mdw.asset.location" is used.
     * ACTION_RECOVER requires ARG_TASK_REF (eg: "com.centurylink.mdw.demo.intro v1.0.53/Manual Acknowledgment").
     */
    @Override
    public void run(CallURL args) {

        DatabaseAccess db = null;
        try {
            // known task template ids
            List<Long> taskTemplateIds = new ArrayList<Long>();
            List<TaskTemplate> taskTemplates = null;
            String assetLoc = args.getParameter(ARG_ASSET_LOC);
            if (assetLoc != null) {
                VersionControlGit vc = new VersionControlGit();
                vc.connect(null, "mdw", null, new File(assetLoc));
                ProcessLoader loader = new LoaderPersisterVcs("mdw", new File(assetLoc), vc, new MdwBaselineData());
                taskTemplates = loader.getTaskTemplates();
            }
            else {
                taskTemplates = TaskTemplateCache.getTaskTemplates();
            }
            for (TaskTemplate taskTemplate : taskTemplates)
                taskTemplateIds.add(taskTemplate.getTaskId());

            // query for non-final task instances
            db = new DatabaseAccess(args.getParameter(ARG_JDBC_URL));
            db.openConnection();

            String query = SELECT;
            int limit = getBatchLimit();
            if (limit > 0) {
                if (db.isOracle())
                    query += " and rownum < limit";
                else
                    query += "limit " + limit;
            }
            query += "order by TASK_INSTANCE_ID desc";

            if (ACTION_CLEAN.equals(args.getAction())) {
                logger.info("Running task cleanup job...");
                ResultSet rs = db.runSelect(query, null);
                List<Long> invalidInstanceIds = new ArrayList<Long>();
                while (rs.next()) {
                    Long instanceId = rs.getLong("TASK_INSTANCE_ID");
                    Long templateId = rs.getLong("TASK_ID");
                    if (!taskTemplateIds.contains(templateId)) {
                        String status = TaskStatuses.getTaskStatuses().get(rs.getInt("TASK_INSTANCE_STATUS"));
                        String refAs = rs.getString("TASK_INSTANCE_REFERRED_AS");
                        logger.warn("ERROR: " + status + " Task instance " + instanceId + " missing template (" + refAs + "), updating to Invalid.");
                        invalidInstanceIds.add(instanceId);
                    }
                }
                if (invalidInstanceIds.size() > 0) {
                    logger.severe("Updating " + invalidInstanceIds.size() + " task instances to Invalid state.");
                    for (Long invalidInstanceId : invalidInstanceIds)
                        db.runUpdate(UPDATE_CLEAN, new Object[]{invalidInstanceId});
                }
                else {
                    logger.info("Found no invalid task instances to clean up.");
                }
            }
            else if (ACTION_RECOVER.equals(args.getAction())) {
                String taskRef = args.getParameter(ARG_TASK_REF);
                if (taskRef == null)
                    throw new IllegalArgumentException("Missing arg: " + ARG_TASK_REF);
                logger.info("Running task recovery job for instances with ref: '" + taskRef + "'...");
                int recovered = db.runUpdate(UPDATE_RECOVER, new Object[]{taskRef});
                if (recovered > 0)
                    logger.severe("Recovered " + recovered + " task instances from Invalid state.");
                else
                    logger.severe("Found no Invalid tasks with ref '" + taskRef + "' to recover.");
            }
            db.commit();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }

    }

    protected int getBatchLimit() {
        return 0; // no limit by default
    }

    public static void main(String[] args) {
        TaskCleanup taskCleanup = new TaskCleanup();
        if ((args.length == 3 || args.length == 4) && (args[0].equals(ACTION_CLEAN) || args[0].equals(ACTION_RECOVER))) {
            String action = args[0];
            String utf8 = "utf-8";
            try {
                String params = ARG_JDBC_URL + "=" + URLEncoder.encode(args[1], utf8) + "&" + ARG_ASSET_LOC + "=" + URLEncoder.encode(args[2], utf8);
                if (args.length == 4)
                    params += "&" + ARG_TASK_REF + "=" + URLEncoder.encode(args[3], utf8);
                taskCleanup.run(new CallURL(action + "?" + params));
            }
            catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        }
        else {
            System.err.println("Arguments: clean <jdbcUrl> <assetLoc> \n" +
                    " or recover <jdbcUrl> <assetLoc> <taskRef>");
        }
    }
}
