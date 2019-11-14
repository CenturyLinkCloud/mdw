/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.service.data;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;
import com.centurylink.mdw.util.log.ActivityLog;
import com.centurylink.mdw.util.log.ActivityLogLine;
import com.centurylink.mdw.util.log.StandardLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class WorkflowDataAccess extends CommonDataAccess {

    public ProcessList getProcessInstances(Query query) throws DataAccessException {
        try {
            List<ProcessInstance> procInsts = new ArrayList<>();
            db.openConnection();
            long count = -1;
            String where;
            if (query.getFind() != null) {
                try {
                    // numeric value means instance id or master request id
                    long findInstId = Long.parseLong(query.getFind());
                    where = "where (pi.process_instance_id like '" + findInstId
                            + "%' or pi.master_request_id like '" + query.getFind() + "%')\n";
                }
                catch (NumberFormatException ex) {
                    // otherwise master request id
                    where = "where pi.master_request_id like '" + query.getFind() + "%'\n";
                    String[] processIds = query.getArrayFilter("processIds");
                    if (processIds != null && processIds.length > 0) {
                        where += getProcessIdsClause(processIds);
                    }
                }
            }
            else {
                where = buildWhere(query);
            }
            String countSql = "select count(process_instance_id) from PROCESS_INSTANCE pi\n" + where;
            ResultSet rs = db.runSelect(countSql);
            if (rs.next())
                count = rs.getLong(1);

            String orderBy = buildOrderBy(query);
            StringBuilder sql = new StringBuilder();
            if (query.getMax() != Query.MAX_ALL)
              sql.append(db.pagingQueryPrefix());
            sql.append("select ").append(PROC_INST_COLS).append(" from PROCESS_INSTANCE pi\n").append(where).append(orderBy);
            if (query.getMax() != Query.MAX_ALL)
                sql.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));
            rs = db.runSelect(sql.toString());
            while (rs.next())
                procInsts.add(buildProcessInstance(rs));

            ProcessList list = new ProcessList(ProcessList.PROCESS_INSTANCES, procInsts);
            list.setTotal(count);
            list.setRetrieveDate(DatabaseAccess.getDbDate());
            return list;
        }
        catch (SQLException ex) {
            throw new DataAccessException("Failed to retrieve Processes", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public long getProcessInstanceCount(Query query) throws DataAccessException {
        try {
            db.openConnection();
            long count = -1;
            String where = buildWhere(query);
            String countSql = "select count(process_instance_id) from PROCESS_INSTANCE pi\n" + where;
            ResultSet rs = db.runSelect(countSql);
            if (rs.next())
                count = rs.getLong(1);
            return count;
        }
        catch (SQLException ex) {
            throw new DataAccessException("Failed to retrieve Processes", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private String buildWhere(Query query) throws DataAccessException {
        long instanceId = query.getLongFilter("instanceId");
        if (instanceId > 0)
            return "where pi.process_instance_id = " + instanceId + "\n"; // ignore other criteria

        StringBuilder sb = new StringBuilder();
        sb.append("where 1 = 1 ");

        // masterRequestId
        String masterRequestId = query.getFilter("masterRequestId");
        if (masterRequestId != null)
            sb.append(" and pi.master_request_id = '" + masterRequestId + "'\n");

        String owner = query.getFilter("owner");
        if (owner == null) {
            // default excludes embedded subprocs - unless searching for activityInstanceId
            if (!(query.getLongFilter("activityInstanceId") > 0L))
                sb.append(" and pi.owner != '").append(OwnerType.MAIN_PROCESS_INSTANCE).append("'\n");
            if ("true".equals(query.getFilter("master")))
                sb.append(" and pi.owner NOT IN ( '").append(OwnerType.PROCESS_INSTANCE).append("' , '").append(OwnerType.ERROR).append("' )\n");
        }
        else {
            sb.append(" and pi.owner = '").append(owner).append("'");
            String ownerId = query.getFilter("ownerId");
            if (ownerId != null)
                sb.append(" and pi.owner_id = ").append(ownerId).append("\n");
        }

        // processId
        String processId = query.getFilter("processId");
        if (processId != null) {
            sb.append(" and pi.process_id = ").append(processId).append("\n");
        }
        else {
            // processIds
            String[] processIds = query.getArrayFilter("processIds");
            if (processIds != null && processIds.length > 0) {
                sb.append(getProcessIdsClause(processIds));
            }
        }

        // secondaryOwnerId
        // for async subprocess invokers, secondary_owner is null so must match that case as well
        String secondaryOwner = query.getFilter("secondaryOwner");
        if (secondaryOwner != null) {
            sb.append(" and (pi.secondary_owner is null or pi.secondary_owner = '").append(secondaryOwner).append("')\n");
        }
        long secondaryOwnerId = query.getLongFilter("secondaryOwnerId");
        if (secondaryOwnerId > 0) {
            sb.append(" and (pi.secondary_owner_id is null or pi.secondary_owner_id = ");
            sb.append(secondaryOwnerId).append(")\n");
        }

        // activityInstanceId
        long activityInstanceId = query.getLongFilter("activityInstanceId");
        if (activityInstanceId > 0) {
            sb.append(" and pi.process_instance_id in (select process_instance_id from ACTIVITY_INSTANCE where activity_instance_id =");
            sb.append(activityInstanceId).append(")\n");
        }
        // status
        String status = query.getFilter("status");
        if (status != null && !status.equals("[Any]")) {
            if (status.equals(WorkStatus.STATUSNAME_ACTIVE)) {
                sb.append(" and pi.status_cd not in (")
                  .append(WorkStatus.STATUS_COMPLETED)
                  .append(",").append(WorkStatus.STATUS_FAILED)
                  .append(",").append(WorkStatus.STATUS_CANCELLED)
                  .append(",").append(WorkStatus.STATUS_PURGE)
                  .append(")\n");
            }
            else {
                sb.append(" and pi.status_cd = ").append(WorkStatuses.getCode(status)).append("\n");
            }
        }
        // startDate
        try {
            Date startDate = query.getDateFilter("startDate");
            if (startDate != null) {
                String start = getOracleDateFormat().format(startDate);
                if (db.isMySQL())
                    sb.append(" and pi.start_dt >= STR_TO_DATE('").append(start).append("','%d-%M-%Y')\n");
                else
                    sb.append(" and pi.start_dt >= '").append(start).append("'\n");
            }
        }
        catch (ParseException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        // template
        String template = query.getFilter("template");
        if (template != null)
            sb.append(" and template = '" + template + "'");
        // values
        Map<String,String> values = query.getMapFilter("values");
        if (values != null) {
            for (String varName : values.keySet()) {
                String varValue = values.get(varName);
                sb.append("\n and exists (select vi.variable_inst_id from VARIABLE_INSTANCE vi ");
                sb.append(" where vi.process_inst_id = pi.process_instance_id and vi.variable_name = '").append(varName).append("'");
                sb.append(" and vi.variable_value = '").append(varValue).append("')");
            }
        }
        return sb.toString();
    }

    private String getProcessIdsClause(String[] processIds) {
        StringBuilder sb = new StringBuilder();
        if (processIds != null && processIds.length > 0) {
            sb.append(" and pi.process_id in (");
            for (int i = 0; i < processIds.length; i++) {
                sb.append(processIds[i]);
                if (i < processIds.length - 1)
                    sb.append(",");
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    private String buildOrderBy(Query query) {
        StringBuilder sb = new StringBuilder();
        sb.append(" order by process_instance_id");
        if (query.isDescending())
            sb.append(" desc");
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Useful for inferring process name and version without definition.
     */
    public String getLatestProcessInstanceComments(Long processId) throws DataAccessException {
        StringBuilder query = new StringBuilder();
        query.append("select process_instance_id, comments from PROCESS_INSTANCE\n");
        query.append("where process_instance_id = (select max(process_instance_id) from PROCESS_INSTANCE ");
        query.append("where process_id = ? and comments is not null)");

        try {
            db.openConnection();
            ResultSet rs = db.runSelect(query.toString(), processId);
            if (rs.next())
                return rs.getString("comments");
            else
                return null;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void addActivityLog(Long processInstanceId, Long activityInstanceId, String level, String thread, String message)
            throws DataAccessException {
        String sql = "insert into ACTIVITY_LOG" +
                "\n (process_instance_id, activity_instance_id, log_level, thread, message)" +
                "\n values (?, ?, ?, ?, ?)";
        try {
            db.openConnection();
            Object[] args = new Object[5];
            args[0] = processInstanceId;
            args[1] = activityInstanceId;
            args[2] = level;
            args[3] = thread;
            args[4] = message;
            db.runUpdate(sql, args);
            db.commit();
        }
        catch (SQLException ex) {
            throw new DataAccessException("Failed to add activity log for " + activityInstanceId + ": " + message, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public ActivityLog getActivityLog(Long activityInstanceId) throws DataAccessException {
        String sql = "select * from ACTIVITY_LOG where activity_instance_id = ? order by CREATE_DT";
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(sql, activityInstanceId);
            return buildActivityLog(rs);
        }
        catch (SQLException ex) {
            throw new DataAccessException("Failed to retrieve activity log: " + activityInstanceId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public ActivityLog getProcessLog(Long processInstanceId, Long[] activityInstanceIds) throws DataAccessException {
        StringBuilder sql = new StringBuilder("select * from ACTIVITY_LOG\nwhere activity_instance_id in (");
        Object[] args = new Object[activityInstanceIds.length];
        for (int i = 0; i < activityInstanceIds.length; i++) {
            sql.append("?");
            if (i < activityInstanceIds.length - 1)
                sql.append(", ");
            args[i] = activityInstanceIds[i];
        }
        sql.append(")\n");
        sql.append("order by activity_instance_id desc, create_dt");
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString(), args);
            return buildActivityLog(rs);
        }
        catch (SQLException ex) {
            throw new DataAccessException("Failed to retrieve process log: " + processInstanceId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public ActivityLog getProcessLog(Long processInstanceId, boolean withActivities) throws DataAccessException {
        String sql = "select * from ACTIVITY_LOG\nwhere process_instance_id = ?\n";
        if (!withActivities)
            sql += "and activity_instance_id is null\n";
        sql += "order by CREATE_DT";
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(sql, processInstanceId);
            return buildActivityLog(rs);
        }
        catch (SQLException ex) {
            throw new DataAccessException("Failed to retrieve process log: " + processInstanceId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    /**
     * Returns null if now rows.
     */
    private ActivityLog buildActivityLog(ResultSet rs) throws SQLException {
        Long processInstanceId = null;
        List<ActivityLogLine> logLines = new ArrayList<>();
        while (rs.next()) {
            if (processInstanceId == null)
                processInstanceId = rs.getLong("PROCESS_INSTANCE_ID");
            Long actInstId = rs.getLong("ACTIVITY_INSTANCE_ID");
            Date when = rs.getTimestamp("CREATE_DT");
            String level = rs.getString("LOG_LEVEL");
            String thread = rs.getString("THREAD");
            String message = rs.getString("MESSAGE");
            if (message != null)
                message = message.replace("\r", "");
            logLines.add(new ActivityLogLine(actInstId, when.toInstant(), StandardLogger.LogLevel.valueOf(level), thread, message));
        }

        if (processInstanceId == null) {
            return null;
        }
        else {
            ActivityLog activityLog = new ActivityLog(processInstanceId);
            activityLog.setLogLines(logLines);
            ZonedDateTime serverTime = ZonedDateTime.now();
            activityLog.setServerZoneOffset(serverTime.getOffset().getTotalSeconds());
            long dbDiffSecs = DatabaseAccess.getDbTimeDiff() / 1000;
            activityLog.setDbZoneOffset(activityLog.getServerZoneOffset() + dbDiffSecs);
            return activityLog;
        }
    }

}