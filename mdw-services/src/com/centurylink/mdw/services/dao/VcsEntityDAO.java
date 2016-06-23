/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.version4.CommonDataAccess;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.asset.AssetHeader;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.dao.task.cache.TaskTemplateCache;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;

/**
 * Shared access to common workflow entities.  The common methods assume a set table alias and a currently open connection.
 */
public class VcsEntityDAO extends CommonDataAccess {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected static final String PROC_INST_COLS = "pi.master_request_id, pi.process_instance_id, pi.process_id, pi.owner, pi.owner_id, " +
            "pi.status_cd, pi.start_dt, pi.end_dt, pi.compcode, pi.comments";
    protected static final String TASK_INST_COLS = "ti.task_instance_id, ti.task_id, ti.task_instance_status, ti.task_instance_owner, ti.task_instance_owner_id, " +
            "ti.task_inst_secondary_owner, task_inst_secondary_owner_id, ti.task_claim_user_id, ti.task_start_dt, ti.task_end_dt, ti.comments, ti.task_instance_state, " +
            "ti.due_date, ti.priority, ti.master_request_id, ti.task_instance_referred_as";

    protected VcsEntityDAO(DatabaseAccess db, int databaseVersion, int supportedVersion) {
        super(db, databaseVersion, supportedVersion);
    }

    /**
     * Assumes pi.* table prefix.
     */
    protected ProcessInstanceVO buildProcessInstance(ResultSet rs) throws SQLException {
        ProcessInstanceVO pi = new ProcessInstanceVO();
        pi.setMasterRequestId(rs.getString("master_request_id"));
        pi.setId(rs.getLong("process_instance_id"));
        pi.setProcessId(rs.getLong("process_id"));
        pi.setOwner(rs.getString("owner"));
        pi.setOwnerId(rs.getLong("owner_id"));
        int statusCode = rs.getInt("status_cd");
        pi.setStatus(WorkStatuses.getWorkStatuses().get(statusCode));
        pi.setStartDate(rs.getTimestamp("start_dt"));
        pi.setEndDate(rs.getTimestamp("end_dt"));
        pi.setCompletionCode(rs.getString("compcode"));
        pi.setComment(rs.getString("comments"));
        // avoid loading into ProcessVOCache
        if (pi.getComment() != null) {
            AssetHeader assetHeader = new AssetHeader(pi.getComment());
            pi.setProcessName(assetHeader.getName());
            pi.setProcessVersion(assetHeader.getVersion());
            pi.setPackageName(assetHeader.getPackageName());
        }
        return pi;
    }

    /**
     * Assumes ti.* table prefix.
     */
    protected TaskInstanceVO buildTaskInstance(ResultSet rs) throws SQLException {
        TaskInstanceVO task = new TaskInstanceVO();
        task.setTaskInstanceId(rs.getLong("task_instance_id"));
        task.setTaskId(rs.getLong("task_id"));
        task.setStatusCode(rs.getInt("task_instance_status"));
        task.setOwnerType(rs.getString("task_instance_owner"));
        task.setOwnerId(rs.getLong("task_instance_owner_id"));
        task.setSecondaryOwnerType(rs.getString("task_inst_secondary_owner"));
        task.setSecondaryOwnerId(rs.getLong("task_inst_secondary_owner_id"));
        task.setTaskClaimUserId(rs.getLong("task_claim_user_id"));
        task.setStartDate(rs.getTimestamp("task_start_dt"));
        task.setEndDate(rs.getTimestamp("task_end_dt"));
        task.setComments(rs.getString("comments"));
        task.setStateCode(rs.getInt("task_instance_state"));
        task.setDueDate(rs.getTimestamp("due_date"));
        task.setPriority(rs.getInt("priority"));
        task.setMasterRequestId(rs.getString("master_request_id"));
        TaskVO taskVO = TaskTemplateCache.getTaskTemplate(task.getTaskId());
        if (taskVO == null) {
            String ref = rs.getString("task_instance_referred_as");
            logger.severe("ERROR: Task instance ID " + task.getTaskInstanceId() + " missing task definition (" + ref + ").");
            task.setTaskName(ref);
            task.setInvalid(true);
        }
        else {
            task.setCategoryCode(taskVO.getTaskCategory());
            task.setTaskName(taskVO.getTaskName());
            task.setDescription(taskVO.getComment());
        }
        try {
            if (task.getTaskClaimUserId() != 0L) {
                UserVO user = UserGroupCache.getUser(task.getTaskClaimUserId());
                if (user == null)
                    logger.severe("ERROR: Cannot find user for id: " + task.getTaskClaimUserId());
                else
                    task.setTaskClaimUserCuid(user.getCuid());
            }
        }
        catch (CachingException ex) {
            logger.severeException(ex.getMessage(),  ex);
        }
        return task;
   }

}
