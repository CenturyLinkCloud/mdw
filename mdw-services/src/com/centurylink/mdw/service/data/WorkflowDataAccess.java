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

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.cache.asset.VariableTypeCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.model.asset.AssetHeader;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.util.DateHelper;
import com.centurylink.mdw.util.log.ActivityLog;
import com.centurylink.mdw.util.log.ActivityLogLine;
import com.centurylink.mdw.util.log.StandardLogger;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class WorkflowDataAccess extends CommonDataAccess {

    private static final String ACTIVITY_INSTANCE_COLS = "ai.activity_instance_id, ai.activity_id,"
            + " ai.start_dt, ai.end_dt, ai.compcode, ai.status_message, ai.status_cd,"
            + " pi.process_instance_id, pi.process_id, pi.master_request_id";

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
                where = buildProcessWhere(query);
            }
            String countSql = "select count(process_instance_id) from PROCESS_INSTANCE pi\n" + where;
            ResultSet rs = db.runSelect(countSql);
            if (rs.next())
                count = rs.getLong(1);

            String orderBy = buildProcessOrderBy(query);
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
            String where = buildProcessWhere(query);
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

    protected String buildProcessWhere(Query query) throws DataAccessException {
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

    protected String getProcessIdsClause(String[] processIds) {
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

    protected String buildProcessOrderBy(Query query) {
        StringBuilder sb = new StringBuilder();
        sb.append(" order by process_instance_id");
        if (query.isDescending())
            sb.append(" desc");
        sb.append("\n");
        return sb.toString();
    }

    protected String buildActivityOrderBy(Query query) {
        StringBuilder sb = new StringBuilder();
        sb.append(" order by activity_instance_id");
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

    public ActivityInstance getActivityInstance(Long instanceId) throws DataAccessException {
        Query query = new Query();
        query.setFilter("instanceId", instanceId);
        String sql = "select " + ACTIVITY_INSTANCE_COLS + " from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi\n"
                + "where ai.process_instance_id = pi.process_instance_id and ai.activity_instance_id = ?";
        try (DbAccess dbAccess = new DbAccess()) {
            ResultSet rs = dbAccess.runSelect(sql, instanceId);
            if (rs.next())
                return buildActivityInstance(rs);
            else
                return null;
        } catch (SQLException | IOException ex) {
            throw new DataAccessException("Error retrieving activity instance: " + instanceId, ex);
        }
    }

    /**
     * Get latest activity instance for process instance and id.
     */
    public ActivityInstance getActivityInstance(Long processInstanceId, Long activityId) throws DataAccessException {

        String sql = "select ACTIVITY_INSTANCE_ID, STATUS_CD, START_DT, END_DT, STATUS_MESSAGE, ACTIVITY_ID, PROCESS_INSTANCE_ID"
                + " from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID = ? and ACTIVITY_ID = ? order by ACTIVITY_INSTANCE_ID desc";

        try (DbAccess dbAccess = new DbAccess()) {
            ResultSet rs = dbAccess.runSelect(sql, processInstanceId, activityId);
            if (rs.next()) {
                ActivityInstance activityInstance = new ActivityInstance();
                activityInstance.setId(rs.getLong("ACTIVITY_INSTANCE_ID"));
                activityInstance.setStatusCode(rs.getInt("STATUS_CD"));
                activityInstance.setStartDate(rs.getTimestamp("START_DT"));
                activityInstance.setEndDate(rs.getTimestamp("END_DT"));
                activityInstance.setMessage(rs.getString("STATUS_MESSAGE"));
                activityInstance.setActivityId(rs.getLong("ACTIVITY_ID"));
                activityInstance.setProcessInstanceId(rs.getLong("PROCESS_INSTANCE_ID"));
                return activityInstance;
            } else {
                return null;
            }
        }
        catch (SQLException ex) {
            throw new DataAccessException("Error retrieving milestone for pi=" + processInstanceId + ", a=" + activityId, ex);
        }
    }

    public ActivityList getActivityInstances(Query query) throws DataAccessException {
        try {
            List<ActivityInstance> actInsts = new ArrayList<>();
            db.openConnection();
            long count = -1;
            String where = buildActivityWhere(query);
            String countSql = "select count(activity_instance_id) from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi\n" + where;
            ResultSet rs = db.runSelect(countSql);
            if (rs.next())
                count = rs.getLong(1);

            String orderBy = buildActivityOrderBy(query);
            StringBuilder sql = new StringBuilder();
            if (query.getMax() != Query.MAX_ALL)
                sql.append(db.pagingQueryPrefix());
            sql.append("select " + ACTIVITY_INSTANCE_COLS + "\nfrom ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi\n");
            sql.append(where).append(orderBy);
            if (query.getMax() != Query.MAX_ALL)
                sql.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));
            rs = db.runSelect(sql.toString());
            while (rs.next())
                actInsts.add(buildActivityInstance(rs));

            ActivityList list = new ActivityList(ActivityList.ACTIVITY_INSTANCES, actInsts);
            list.setTotal(count);
            list.setRetrieveDate(DatabaseAccess.getDbDate());
            return list;
        }
        catch (SQLException | IOException ex) {
            throw new DataAccessException("Failed to retrieve activities", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    protected String buildActivityWhere(Query query) throws DataAccessException {
        long instanceId = query.getLongFilter("instanceId");
        if (instanceId > 0)
            return "where ai.activity_instance_id = " + instanceId + "\n"; // ignore other criteria

        StringBuilder sb = new StringBuilder();
        sb.append("where ai.process_instance_id = pi.process_instance_id");

        // masterRequestId
        String masterRequestId = query.getFilter("masterRequestId");
        if (masterRequestId != null)
            sb.append(" and pi.master_request_id = '" + masterRequestId + "'\n");

        // status
        String status = query.getFilter("status");
        if (status != null && !status.equals("[Any]")) {
            if (status.equals("[Stuck]")) {
                sb.append(" and ai.status_cd in (")
                        .append(WorkStatus.STATUS_IN_PROGRESS)
                        .append(",").append(WorkStatus.STATUS_FAILED)
                        .append(",").append(WorkStatus.STATUS_WAITING)
                        .append(")\n");
            }
            else {
                sb.append(" and ai.status_cd = ").append(WorkStatuses.getCode(status)).append("\n");
            }
        }
        // startDate
        try {
            Date startDate = query.getDateFilter("startDate");
            if (startDate != null) {
                String start = getOracleDateFormat().format(startDate);
                if (db.isMySQL())
                    sb.append(" and ai.start_dt >= STR_TO_DATE('").append(start).append("','%d-%M-%Y')\n");
                else
                    sb.append(" and ai.start_dt >= '").append(start).append("'\n");
            }
        }
        catch (ParseException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }

        // activity => <procId>:A<actId>
        String activity = query.getFilter("activity");
        if (activity != null) {
            if (db.isOracle()) {
                sb.append(" and (pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID) = '" + activity + "'");
            }
            else {
                sb.append(" and CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) = '" + activity + "'");
            }
        }

        return sb.toString();
    }

    public ActivityInstance buildActivityInstance(ResultSet rs) throws SQLException, IOException {
        ActivityInstance ai = new ActivityInstance();
        ai.setId(rs.getLong("activity_instance_id"));
        ai.setActivityId(rs.getLong("activity_id"));
        ai.setDefinitionId("A" + ai.getActivityId());
        ai.setStartDate(rs.getTimestamp("start_dt"));
        ai.setEndDate(rs.getTimestamp("end_dt"));
        ai.setResult(rs.getString("compcode"));
        ai.setMessage(rs.getString("status_message"));
        ai.setStatusCode(rs.getInt("status_cd"));
        ai.setStatus(WorkStatuses.getName(ai.getStatusCode()));
        ai.setProcessInstanceId(rs.getLong("process_instance_id"));
        ai.setProcessId(rs.getLong("process_id"));
        ai.setMasterRequestId(rs.getString("master_request_id"));
        Process process = ProcessCache.getProcess(ai.getProcessId());
        if (process != null) {
            ai.setProcessName(process.getName());
            ai.setProcessVersion(process.getVersionString());
            Package pkg = PackageCache.getPackage(process.getPackageName());
            if (pkg != null)
                ai.setPackageName(pkg.getName());
        }
        return ai;
    }

    /**
     * For VCS-defined processes, relies on comment having been set.
     * Also sets status name from code.
     */
    protected void populateNameVersionStatus(ProcessInstance processInstance) throws DataAccessException {
        if (processInstance.getComment() != null) {
            AssetVersionSpec spec = AssetVersionSpec.parse(processInstance.getComment());
            processInstance.setProcessName(spec.getName());
            processInstance.setProcessVersion(spec.getVersion());
            String pkgNameVer = spec.getPackageName();
            if (pkgNameVer != null) {
                int spaceV = pkgNameVer.indexOf(" v");
                if (spaceV > 0 && pkgNameVer.length() > spaceV + 2)
                    processInstance.setPackageName(pkgNameVer.substring(0, spaceV));
                else
                    processInstance.setPackageName(spec.getPackageName());
            }

        }
        if (processInstance.getStatusCode() != null) {
            processInstance.setStatus(WorkStatuses.getName(processInstance.getStatusCode()));
        }
    }

    protected List<ProcessInstance> getProcessInstancesForOwner(String ownerType, Long ownerId) throws SQLException, DataAccessException {
        List<ProcessInstance> instanceList = null;
        String query = "select pi.PROCESS_INSTANCE_ID, pi.PROCESS_ID, pi.MASTER_REQUEST_ID," +
                " pi.STATUS_CD, pi.START_DT, pi.END_DT, pi.COMPCODE, pi.COMMENTS, pi.SECONDARY_OWNER, pi.SECONDARY_OWNER_ID" +
                " from PROCESS_INSTANCE pi" +
                " where pi.OWNER = '" + ownerType + "' and pi.OWNER_ID = ? order by pi.PROCESS_INSTANCE_ID";
        ResultSet rs = db.runSelect(query, ownerId);
        while (rs.next()) {
            if (instanceList == null)
                instanceList = new ArrayList<>();
            Long processId = rs.getLong("PROCESS_ID");
            String comment = rs.getString("COMMENTS");
            ProcessInstance pi = new ProcessInstance(processId, "");
            pi.setId(rs.getLong("PROCESS_INSTANCE_ID"));
            pi.setOwner(ownerType);
            pi.setOwnerId(ownerId);
            pi.setMasterRequestId(rs.getString("MASTER_REQUEST_ID"));
            pi.setStatusCode(rs.getInt("STATUS_CD"));
            pi.setStartDate(DateHelper.dateToString(rs.getTimestamp("START_DT")));
            pi.setEndDate(DateHelper.dateToString(rs.getTimestamp("END_DT")));
            pi.setCompletionCode(rs.getString("COMPCODE"));
            pi.setComment(comment);
            pi.setSecondaryOwner(rs.getString("SECONDARY_OWNER"));
            pi.setSecondaryOwnerId(rs.getLong("SECONDARY_OWNER_ID"));
            populateNameVersionStatus(pi);
            instanceList.add(pi);
        }

        return instanceList;
    }

    public ProcessInstance getProcessInstance(Long instanceId) throws DataAccessException {
        try {
            String q = "select PROCESS_INSTANCE_ID from PROCESS_INSTANCE where PROCESS_INSTANCE_ID=?";
            db.openConnection();
            ResultSet rs = db.runSelect(q, instanceId);
            if (!rs.next())
                return null;
            return getProcessInstanceAll(instanceId);
        } catch (SQLException ex) {
            throw new DataAccessException(0, "Failed to process instance: " + instanceId, ex);
        } finally {
            db.closeConnection();
        }
    }

    public ProcessInstance getProcessInstanceAll(Long procInstId)
            throws DataAccessException {
        try {
            db.openConnection();
            ProcessInstance procInstInfo = this.getProcessInstanceBase0(procInstId);
            List<ActivityInstance> actInstList = new ArrayList<ActivityInstance>();
            String query = "select ACTIVITY_INSTANCE_ID,STATUS_CD,START_DT,END_DT," +
                    "    STATUS_MESSAGE,ACTIVITY_ID,COMPCODE" +
                    " from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID=?" +
                    " order by ACTIVITY_INSTANCE_ID";
            ResultSet rs = db.runSelect(query, procInstId);
            ActivityInstance actInst;
            while (rs.next()) {
                actInst = new ActivityInstance();
                actInst.setId(new Long(rs.getLong(1)));
                actInst.setStatusCode(rs.getInt(2));
                actInst.setStartDate(rs.getTimestamp(3));
                actInst.setEndDate(rs.getTimestamp(4));
                actInst.setMessage(rs.getString(5));
                actInst.setActivityId(new Long(rs.getLong(6)));
                actInst.setCompletionCode(rs.getString(7));
                actInstList.add(actInst);
            }
            procInstInfo.setActivities(actInstList);
            List<TransitionInstance> workTransInstanceList
                    = new ArrayList<TransitionInstance>();
            query = "select WORK_TRANS_INST_ID,STATUS_CD,START_DT,END_DT,WORK_TRANS_ID" +
                    " from WORK_TRANSITION_INSTANCE" +
                    " where PROCESS_INST_ID=? order by WORK_TRANS_INST_ID";
            rs = db.runSelect(query, procInstId);
            TransitionInstance workTransInstance;
            while (rs.next()) {
                workTransInstance = new TransitionInstance();
                workTransInstance.setTransitionInstanceID(rs.getLong(1));
                workTransInstance.setProcessInstanceID(procInstId);
                workTransInstance.setStatusCode(rs.getInt(2));
                workTransInstance.setStartDate(DateHelper.dateToString(rs.getTimestamp(3)));
                workTransInstance.setEndDate(DateHelper.dateToString(rs.getTimestamp(4)));
                workTransInstance.setTransitionID(rs.getLong(5));
                workTransInstanceList.add(workTransInstance);
            }
            procInstInfo.setTransitions(workTransInstanceList);
            List<VariableInstance> variableDataList = new ArrayList<VariableInstance>();
            query = "select VARIABLE_INST_ID, VARIABLE_ID, VARIABLE_VALUE, VARIABLE_NAME, VARIABLE_TYPE_ID " +
                    "from VARIABLE_INSTANCE where PROCESS_INST_ID=? order by lower(VARIABLE_NAME)";
            rs = db.runSelect(query, procInstId);
            while (rs.next()) {
                VariableInstance data = new VariableInstance();
                data.setInstanceId(rs.getLong(1));
                data.setVariableId(rs.getLong(2));
                data.setStringValue(rs.getString(3));
                data.setName(rs.getString(4));
                data.setType(getVariableType(rs.getLong(5)));
                variableDataList.add(data);
            }
            procInstInfo.setVariables(variableDataList);
            return procInstInfo;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to load process instance runtime info", e);
        } finally {
            db.closeConnection();
        }
    }

    public ProcessInstance getProcessInstanceBase(Long procInstId) throws DataAccessException {
        try {
            db.openConnection();
            return getProcessInstanceBase0(procInstId);
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to process instance", e);
        } finally {
            db.closeConnection();
        }
    }

    public ProcessList getProcessInstanceList(Map<String,String> criteria, int pageIndex, int pageSize, String orderBy) throws DataAccessException {
        try {
            db.openConnection();

            // count query
            Long count;
            String query = buildCountQuery(criteria);
            ResultSet rs = db.runSelect(query);
            if (rs.next())
                count = new Long(rs.getLong(1));
            else
                count = new Long(-1);

            // instances query
            if (orderBy == null)
                orderBy = " ORDER BY PROCESS_INSTANCE_ID DESC\n";
            int startIndex = pageSize == Query.MAX_ALL ? Query.MAX_ALL : (pageIndex - 1) * pageSize;
            int endIndex = startIndex + pageSize;
            query = buildQuery(criteria, startIndex, endIndex, orderBy);

            rs = db.runSelect(query);
            List<ProcessInstance> mdwProcessInstanceList = new ArrayList<ProcessInstance>();
            while (rs.next()) {
                ProcessInstance pi = new ProcessInstance(rs.getLong(8), rs.getString(9));
                pi.setOwner(rs.getString(6));
                pi.setOwnerId(rs.getLong(7));
                pi.setMasterRequestId(rs.getString(2));
                pi.setStatusCode(rs.getInt(3));
                pi.setStartDate(DateHelper.dateToString(rs.getTimestamp(4)));
                pi.setId(rs.getLong(1));
                pi.setComment(rs.getString(10));
                pi.setEndDate(DateHelper.dateToString(rs.getTimestamp(5)));
                mdwProcessInstanceList.add(pi);
            }

            ProcessList processList = new ProcessList(ProcessList.PROCESS_INSTANCES, mdwProcessInstanceList);
            processList.setRetrieveDate(DatabaseAccess.getDbDate());
            processList.setCount(mdwProcessInstanceList.size());
            processList.setTotal(count);
            for (ProcessInstance process : processList.getItems())
                populateNameVersionStatus(process);
            return processList;
        } catch (Exception e) {
            throw new DataAccessException(0,"error to load child process instance list", e);
        } finally {
            db.closeConnection();
        }
    }

    public ProcessList getProcessInstanceList(Map<String,String> criteria, Map<String,String> variables, int pageIndex, int pageSize, String orderBy)
            throws DataAccessException {
        ProcessList procList = getProcessInstanceList(criteria, null, variables, pageIndex, pageSize, orderBy);
        for (ProcessInstance process : procList.getItems())
            populateNameVersionStatus(process);
        return procList;
    }

    public ProcessList getProcessInstanceList(
            Map<String,String> criteria, List<String> variableNames, Map<String,String> variables,
            int pageIndex, int pageSize, String orderBy) throws DataAccessException {

        if ((variableNames == null || variableNames.isEmpty()) && (variables == null || variables.isEmpty()))
            return getProcessInstanceList(criteria, pageIndex, pageSize, orderBy);

        try {
            db.openConnection();

            String query = buildCountQuery(criteria, variables);
            ResultSet rs = db.runSelect(query);
            Long count;
            if (rs.next())
                count = new Long(rs.getLong(1));
            else
                count = new Long(-1);

            if (orderBy == null)
                orderBy = " ORDER BY PROCESS_INSTANCE_ID DESC\n";
            int startIndex = pageSize == Query.MAX_ALL ? Query.MAX_ALL : (pageIndex - 1) * pageSize;
            int endIndex = startIndex + pageSize;
            query = buildQuery(criteria, variableNames, variables, startIndex, endIndex, orderBy);

            rs = db.runSelect(query);
            List<ProcessInstance> mdwProcessInstanceList = new ArrayList<ProcessInstance>();
            while (rs.next()) {
                ProcessInstance pi = new ProcessInstance(rs.getLong(8), rs.getString(9));
                pi.setOwner(rs.getString(6));
                pi.setOwnerId(rs.getLong(7));
                pi.setMasterRequestId(rs.getString(2));
                pi.setStatusCode(rs.getInt(3));
                pi.setStartDate(DateHelper.dateToString(rs.getTimestamp(4)));
                pi.setId(rs.getLong(1));
                pi.setComment(rs.getString(10));
                pi.setEndDate(DateHelper.dateToString(rs.getTimestamp(5)));
                if (variableNames != null && variableNames.size() > 0) {
                    List<VariableInstance> vars = new ArrayList<VariableInstance>();
                    for (String varName : variableNames) {
                        String name = varName.startsWith("DATE:") ? varName.substring(5) : varName;
                        String varVal = rs.getString(name.toUpperCase());
                        VariableInstance varInstInfo = new VariableInstance();
                        varInstInfo.setName(name);
                        varInstInfo.setStringValue(varVal);
                        vars.add(varInstInfo);
                    }
                    pi.setVariables(vars);
                }
                mdwProcessInstanceList.add(pi);
            }
            ProcessList procList = new ProcessList(ProcessList.PROCESS_INSTANCES, mdwProcessInstanceList);
            procList.setRetrieveDate(DatabaseAccess.getDbDate());
            procList.setCount(mdwProcessInstanceList.size());
            procList.setTotal(count);
            return procList;
        }
        catch (Exception e) {
            throw new DataAccessException(-1, "Error loading process instance list", e);
        }
        finally {
            db.closeConnection();
        }
    }

    protected String buildCountQuery(Map<String,String> criteria, Map<String,String> variablesCriteria) {
        if (variablesCriteria == null || variablesCriteria.isEmpty())
            return buildCountQuery(criteria);

        StringBuffer sqlBuff = new StringBuffer();
        sqlBuff.append("select count(pi2.process_instance_id)\n");
        sqlBuff.append("from (\n");
        sqlBuff.append(" select pi.*\n");
        sqlBuff.append(buildVariablesClause(criteria, null, variablesCriteria));
        sqlBuff.append(") pi2");
        return sqlBuff.toString();
    }

    protected String buildQuery(Map<String,String> criteria, List<String> variables, Map<String,String> variableCriteria, int startIndex, int endIndex, String orderBy) {
        StringBuffer sqlBuff = new StringBuffer();
        if (startIndex != Query.MAX_ALL)
            sqlBuff.append(db.pagingQueryPrefix());
        sqlBuff.append("select pis.process_instance_id, pis.master_request_id, pis.status_cd, pis.start_dt, pis.end_dt, ")
                .append("pis.owner, pis.owner_id, pis.process_id, '' as process_name, pis.comments");
        if (variables != null && variables.size() > 0) {
            for (String varName : variables)
                sqlBuff.append(", ").append(varName.startsWith("DATE:") ? varName.substring(5) : varName);
        }
        sqlBuff.append("\n    from (\n");
        sqlBuff.append("  select pi.* ");
        sqlBuff.append(buildVariablesSelect(variables));
        sqlBuff.append(buildVariablesClause(criteria, variables, variableCriteria));
        sqlBuff.append(") pis\n");
        if (orderBy != null)
            sqlBuff.append("\n").append(orderBy);
        if (startIndex != Query.MAX_ALL)
            sqlBuff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        return sqlBuff.toString();
    }

    protected String buildVariablesClause(Map<String,String> criteria, List<String> variables, Map<String,String> variableCriteria) {
        StringBuffer sqlBuff = new StringBuffer();
        sqlBuff.append(" from PROCESS_INSTANCE pi\n");
        sqlBuff.append(" where pi.process_id > 0\n"); // since common starts with AND
        buildQueryCommon(sqlBuff, criteria, null);
        if (variableCriteria != null) {
            for (String varName : variableCriteria.keySet()) {
                String varValue = variableCriteria.get(varName);
                boolean isDate = varName.startsWith("DATE:");
                Long variableTypeId = null;
                if (isDate) {
                    varName = varName.substring(5);
                    variableTypeId = VariableTypeCache.getTypeId("java.util.Date");
                }

                sqlBuff.append("\n and exists (select vi.variable_inst_id from VARIABLE_INSTANCE vi")
                        .append(" where vi.process_inst_id = pi.process_instance_id")
                        .append(" and vi.variable_name = '" + varName + "'");

                if (isDate && variableTypeId != null) {
                    sqlBuff.append(" and vi.VARIABLE_TYPE_ID = " + variableTypeId); // date var type
                    if (db.isMySQL())
                        sqlBuff.append("\n and (select concat(substr(ivi.VARIABLE_VALUE, 5, 7), substr(ivi.VARIABLE_VALUE, 25))");
                    else
                        sqlBuff.append("\n and (select substr(ivi.VARIABLE_VALUE, 5, 7) || substr(ivi.VARIABLE_VALUE, 25)");
                    sqlBuff.append("\n     from VARIABLE_INSTANCE ivi  where ivi.variable_type_id = " + variableTypeId);
                    sqlBuff.append("\n     and ivi.variable_inst_id = vi.variable_inst_id");
                    sqlBuff.append("\n     and ivi.variable_name = '" + varName + "') = '"+ varValue + "') ");
                }
                else {
                    if (varValue != null && ((varValue.trim().toLowerCase().startsWith("like ") && varValue.indexOf('%') >=0 )
                            || (varValue.trim().toLowerCase().startsWith("in ") && varValue.indexOf('(') >=0)))
                        sqlBuff.append(" and vi.VARIABLE_VALUE " + varValue + ") ");
                    else
                        sqlBuff.append(" and vi.VARIABLE_VALUE = '" + varValue + "') ");
                }
            }
        }
        return sqlBuff.toString();
    }

    protected String buildQuery(Map<String,String> criteria, int startIndex, int endIndex, String orderBy) {
        StringBuffer sqlBuff = new StringBuffer();
        if (startIndex != Query.MAX_ALL)
            sqlBuff.append(db.pagingQueryPrefix());
        sqlBuff.append("SELECT ");
        if (startIndex != Query.MAX_ALL)
            sqlBuff.append("/*+ NO_USE_NL(pi r) */ ");
        sqlBuff.append("pi.PROCESS_INSTANCE_ID, pi.MASTER_REQUEST_ID, pi.STATUS_CD, pi.START_DT, ");
        sqlBuff.append("pi.END_DT, pi.OWNER, pi.OWNER_ID, pi.PROCESS_ID, '' as NAME, pi.COMMENTS\n");
        sqlBuff.append("FROM PROCESS_INSTANCE pi\n");
        sqlBuff.append("where 1=1 ");
        if (!OwnerType.MAIN_PROCESS_INSTANCE.equals(criteria.get("owner")))
            sqlBuff.append(" and pi.OWNER!='" + OwnerType.MAIN_PROCESS_INSTANCE +"' ");
        buildQueryCommon(sqlBuff, criteria, orderBy);
        if (startIndex != Query.MAX_ALL)
            sqlBuff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        return sqlBuff.toString();
    }

    protected String buildProcessNameClause(String qualifiedName) {
        int slash = qualifiedName.indexOf('/');
        String pkg = qualifiedName.substring(0, slash);
        String proc = qualifiedName.substring(slash + 1);
        return " AND (pi.COMMENTS like '" + pkg + " v%/" + proc + " v%'"
                + " OR pi.COMMENTS like '" + pkg + "/" + proc + " v%')";

    }

    protected ProcessInstance getProcessInstanceBase0(Long processInstanceId) throws SQLException, DataAccessException {
        String query = "select PROCESS_ID, OWNER, OWNER_ID, MASTER_REQUEST_ID, " +
                "STATUS_CD, START_DT, END_DT, COMPCODE, COMMENTS, TEMPLATE, SECONDARY_OWNER, SECONDARY_OWNER_ID\n" +
                "from PROCESS_INSTANCE where PROCESS_INSTANCE_ID = ?";
        ResultSet rs = db.runSelect(query, processInstanceId);
        if (!rs.next())
            throw new SQLException("Cannot find process instance ID: " + processInstanceId);
        ProcessInstance pi = new ProcessInstance(rs.getLong("PROCESS_ID"), "");
        pi.setId(processInstanceId);
        pi.setOwner(rs.getString("OWNER"));
        pi.setOwnerId(rs.getLong("OWNER_ID"));
        pi.setMasterRequestId(rs.getString("MASTER_REQUEST_ID"));
        pi.setStatusCode(rs.getInt("STATUS_CD"));
        pi.setStartDate(DateHelper.dateToString(rs.getTimestamp("START_DT")));
        pi.setEndDate(DateHelper.dateToString(rs.getTimestamp("END_DT")));
        pi.setCompletionCode(rs.getString("COMPCODE"));
        pi.setComment(rs.getString("COMMENTS"));
        pi.setTemplate(rs.getString("TEMPLATE"));
        if (pi.getTemplate() != null) {
            AssetHeader templateHeader = new AssetHeader(pi.getTemplate());
            pi.setTemplate(templateHeader.getName());
            pi.setTemplatePackage(templateHeader.getPackageName());
            pi.setTemplateVersion(templateHeader.getVersion());
        }
        pi.setSecondaryOwner(rs.getString("SECONDARY_OWNER"));
        if (pi.getSecondaryOwner() != null)
            pi.setSecondaryOwnerId(rs.getLong("SECONDARY_OWNER_ID"));
        populateNameVersionStatus(pi);
        return pi;
    }

    public Linked<ProcessInstance> getProcessInstanceCallHierarchy(Long processInstanceId) throws DataAccessException {
        try {
            db.openConnection();
            ProcessInstance startingInstance = getProcessInstanceBase0(processInstanceId);
            Linked<ProcessInstance> startingLinked = new Linked<>(startingInstance);
            Linked<ProcessInstance> top = startingLinked;
            // callers
            while (OwnerType.PROCESS_INSTANCE.equals(top.get().getOwner())) {
                ProcessInstance caller = getProcessInstanceBase0(top.get().getOwnerId());
                Linked<ProcessInstance> callerLinked = new Linked<>(caller);
                top.setParent(callerLinked);
                callerLinked.getChildren().add(top);
                top = callerLinked;
            }
            // called
            addCalledHierarchy(startingLinked);
            return top;
        } catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        } finally {
            db.closeConnection();
        }
    }

    private void addCalledHierarchy(Linked<ProcessInstance> caller) throws SQLException, DataAccessException {
        ProcessInstance callerProcInst = caller.get();
        List<ProcessInstance> calledInsts = getProcessInstancesForOwner(OwnerType.PROCESS_INSTANCE, callerProcInst.getId());
        if (calledInsts != null) {
            for (ProcessInstance calledInst : calledInsts) {
                Linked<ProcessInstance> child = new Linked<>(calledInst);
                child.setParent(caller);
                caller.getChildren().add(child);
                addCalledHierarchy(child);
            }
        }
    }

    protected String buildCountQuery(Map<String,String> pMap) {
        StringBuffer sqlBuff = new StringBuffer();
        sqlBuff.append("SELECT count(pi.process_instance_id) ");
        sqlBuff.append("FROM PROCESS_INSTANCE pi ");
        sqlBuff.append("WHERE pi.PROCESS_ID is not null "); // just to allow next condition to have "and"
        buildQueryCommon(sqlBuff, pMap, null);
        return sqlBuff.toString();
    }

    public String buildVariablesSelect(List<String> variables) {
        StringBuffer buff = new StringBuffer();
        if (variables != null && variables.size() > 0) {
            for (String varName : variables) {
                String name = varName.startsWith("DATE:") ? varName.substring(5) : varName;
                buff.append(",\n");
                buff.append("    (select vi.VARIABLE_VALUE from VARIABLE_INSTANCE vi "
                        + " where pi.PROCESS_INSTANCE_ID = vi.PROCESS_INST_ID "
                        + " and vi.variable_name = '" + name + "') " + name);
            }
        }
        return buff.toString();
    }

    protected void buildQueryCommon(StringBuffer sqlBuff, Map<String,String> pMap, String orderBy) {

        String wildcardStr = "";
        if (pMap.containsKey("processName")) {
            sqlBuff.append(buildProcessNameClause(pMap.get("processName")));
        }
        if (pMap.containsKey("processId")){
            sqlBuff.append(" AND pi.PROCESS_ID = "+new Long((String)pMap.get("processId")));
        }
        if (pMap.containsKey("processIdList")){
            sqlBuff.append(" AND pi.PROCESS_ID in " + pMap.get("processIdList"));
        }
        if (pMap.containsKey("id")){
            sqlBuff.append(" AND pi.PROCESS_INSTANCE_ID = "+new Long((String)pMap.get("id")));
        }
        if (pMap.containsKey("ownerId")){
            sqlBuff.append(" AND pi.OWNER_ID = "+new Long((String)pMap.get("ownerId")));
        }
        if (pMap.containsKey("ownerIdList")){
            sqlBuff.append(" AND pi.OWNER_ID in " + pMap.get("ownerIdList"));
        }
        if (pMap.containsKey("owner")){
            String ownerType = pMap.get("owner");
            if (ownerType.startsWith("~")) sqlBuff.append(" AND pi.OWNER like '"+ownerType.substring(1)+"'");
            else sqlBuff.append(" AND pi.OWNER = '"+ownerType+"'");
        }
        if (pMap.containsKey("masterRequestId")){
            //AK..added on 05/12/2011..If wildcard provided and string length is >= 3, only then apply wildcard search in SQL query; else not
            wildcardStr = pMap.get("masterRequestId");
            if ( (wildcardStr.contains("%")) && (wildcardStr.length() >=3) )
            {
                sqlBuff.append(" AND pi.MASTER_REQUEST_ID LIKE '" + wildcardStr + "'");
            }
            else
            {
                sqlBuff.append(" AND pi.MASTER_REQUEST_ID = '" + wildcardStr + "'");
            }
        }
        if (pMap.containsKey("masterRequestIdIgnoreCase")){
            //AK..added on 05/12/2011..If wildcard provided and string length is >= 3, only then apply wildcard search in SQL query; else not
            wildcardStr = pMap.get("masterRequestIdIgnoreCase").toUpperCase();
            if ((wildcardStr.contains("%")) && (wildcardStr.length() >=3))
            {
                sqlBuff.append(" AND UPPER(pi.MASTER_REQUEST_ID) LIKE UPPER('" + wildcardStr + "')");
            }
            else
            {
                sqlBuff.append(" AND UPPER(pi.MASTER_REQUEST_ID) = UPPER('" + wildcardStr + "')");
            }
        }
        if (pMap.containsKey("statusCode")){
            sqlBuff.append(" AND pi.STATUS_CD = "+new Integer((String)pMap.get("statusCode")));
        }
        if (pMap.containsKey("statusCodeList") && !StringUtils.isBlank(pMap.get("statusCodeList"))){
            sqlBuff.append(" AND pi.STATUS_CD in (" + pMap.get("statusCodeList") + ")");
        }
        if (pMap.containsKey("startDatefrom")){
            if (db.isMySQL())
                sqlBuff.append(" AND pi.START_DT >= STR_TO_DATE('"+pMap.get("startDatefrom")+"','%d-%M-%Y')");
            else
                sqlBuff.append(" AND pi.START_DT >= '"+pMap.get("startDatefrom")+"'");
        }
        if (pMap.containsKey("startDateto")){
            if (db.isMySQL())
                sqlBuff.append(" AND pi.START_DT <= STR_TO_DATE('"+pMap.get("startDateto")+"','%d-%M-%Y')");
            else
                sqlBuff.append(" AND pi.START_DT <= '"+pMap.get("startDateto")+"'");
        }
        if (pMap.containsKey("endDatefrom")){
            if (db.isMySQL())
                sqlBuff.append(" AND pi.END_DT >= STR_TO_DATE('"+pMap.get("endDatefrom")+"','%d-%M-%Y')");
            else
                sqlBuff.append(" AND pi.END_DT >= '"+pMap.get("endDatefrom")+"'");
        }
        if (pMap.containsKey("endDateto")){
            if(db.isMySQL())
                sqlBuff.append(" AND pi.END_DT <= STR_TO_DATE('"+pMap.get("endDateto")+"','%d-%M-%Y')");
            else
                sqlBuff.append(" AND pi.END_DT <= '"+pMap.get("endDateto")+"'");
        }
        else if (pMap.containsKey("endDateTo")){
            // leave this criterion for backward compatibility, even though case is inconsistent
            sqlBuff.append(" AND pi.END_DT <= '"+pMap.get("endDateTo")+"'");
        }

        // new-style parameters
        if (pMap.containsKey("ids")) {
            sqlBuff.append(" AND pi.PROCESS_ID in (").append(pMap.get("ids")).append(")");
        }

        if (orderBy != null)
            sqlBuff.append("\n").append(orderBy);
    }

    private final List<VariableType> variableTypes = new MdwBaselineData().getVariableTypes();

    protected String getVariableType(Long id) {
        if (variableTypes == null) {
            return VariableTypeCache.getTypeName(id);
        }
        else {
            for (VariableType variableType : variableTypes) {
                if (variableType.getVariableTypeId().longValue() == id.longValue())
                    return variableType.getVariableType();
            }
            // If didn't find the type, look in cache
            return VariableTypeCache.getTypeName(id);
        }
    }
}