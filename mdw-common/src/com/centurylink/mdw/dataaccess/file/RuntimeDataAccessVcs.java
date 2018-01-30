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
package com.centurylink.mdw.dataaccess.file;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.centurylink.mdw.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.event.EventLog;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityList;
import com.centurylink.mdw.model.workflow.LinkedProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.model.workflow.TransitionInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;
import com.centurylink.mdw.util.StringHelper;

/**
 * Used for VCS-based assets in the runtime container (not Designer).
 * Accesses the db, but takes into account the fact that the design-time tables don't exist.
 * Also handles compatibility for pre-VCS processes that still reside in the db.
 */
public class RuntimeDataAccessVcs extends CommonDataAccess implements RuntimeDataAccess {

    private List<VariableType> variableTypes;

    public RuntimeDataAccessVcs(DatabaseAccess db, int databaseVersion, int supportedVersion, BaselineData baselineData) {
        super(db, databaseVersion, supportedVersion);
        this.variableTypes = baselineData.getVariableTypes();
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
                workTransInstance.setTransitionInstanceID(new Long(rs.getLong(1)));
                workTransInstance.setProcessInstanceID(procInstId);
                workTransInstance.setStatusCode(rs.getInt(2));
                workTransInstance.setStartDate(StringHelper.dateToString(rs.getTimestamp(3)));
                workTransInstance.setEndDate(StringHelper.dateToString(rs.getTimestamp(4)));
                workTransInstance.setTransitionID(new Long(rs.getLong(5)));
                workTransInstanceList.add(workTransInstance);
            }
            procInstInfo.setTransitions(workTransInstanceList);
            List<VariableInstance> variableDataList = new ArrayList<VariableInstance>();
            query = "select VARIABLE_INST_ID, VARIABLE_ID, VARIABLE_VALUE, VARIABLE_NAME, VARIABLE_TYPE_ID " +
                    "from VARIABLE_INSTANCE where PROCESS_INST_ID=? order by lower(VARIABLE_NAME)";
            rs = db.runSelect(query, procInstId);
            while (rs.next()) {
                VariableInstance data = new VariableInstance();
                data.setInstanceId(new Long(rs.getLong(1)));
                data.setVariableId(new Long(rs.getLong(2)));
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

    @Override
    public ProcessList getProcessInstanceList(Map<String,String> criteria, int pageIndex, int pageSize, String orderBy) throws DataAccessException {
        try {
            db.openConnection();

            // count query
            Long count;
            String query = buildCountQuery(criteria);
            ResultSet rs = db.runSelect(query, null);
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

            rs = db.runSelect(query, null);
            List<ProcessInstance> mdwProcessInstanceList = new ArrayList<ProcessInstance>();
            while (rs.next()) {
                ProcessInstance pi = new ProcessInstance(rs.getLong(8), rs.getString(9));
                pi.setOwner(rs.getString(6));
                pi.setOwnerId(rs.getLong(7));
                pi.setMasterRequestId(rs.getString(2));
                pi.setStatusCode(rs.getInt(3));
                pi.setStartDate(StringHelper.dateToString(rs.getTimestamp(4)));
                pi.setId(rs.getLong(1));
                pi.setComment(rs.getString(10));
                pi.setEndDate(StringHelper.dateToString(rs.getTimestamp(5)));
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

    @Override
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
            ResultSet rs = db.runSelect(query, null);
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

            rs = db.runSelect(query, null);
            List<ProcessInstance> mdwProcessInstanceList = new ArrayList<ProcessInstance>();
            while (rs.next()) {
                ProcessInstance pi = new ProcessInstance(rs.getLong(8), rs.getString(9));
                pi.setOwner(rs.getString(6));
                pi.setOwnerId(rs.getLong(7));
                pi.setMasterRequestId(rs.getString(2));
                pi.setStatusCode(rs.getInt(3));
                pi.setStartDate(StringHelper.dateToString(rs.getTimestamp(4)));
                pi.setId(rs.getLong(1));
                pi.setComment(rs.getString(10));
                pi.setEndDate(StringHelper.dateToString(rs.getTimestamp(5)));
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

    public List<ProcessInstance> getProcessInstanceList(String owner, String secondaryOwner,
            Long secondaryOwnerId, String orderBy) throws DataAccessException {
        try {
            db.openConnection();

            if (orderBy == null)
                orderBy = " ORDER BY PROCESS_INSTANCE_ID DESC\n";

            String query = "SELECT pi.PROCESS_INSTANCE_ID, pi.PROCESS_ID, pi.OWNER_ID, pi.STATUS_CD, pi.START_DT, pi.END_DT, pi.CREATE_DT, r.rule_set_name, pi.MASTER_REQUEST_ID, pi.COMMENTS"
                    + " FROM PROCESS_INSTANCE pi,  rule_set r WHERE pi.PROCESS_ID=r.RULE_SET_ID "
                    + " AND SECONDARY_OWNER_ID = " + secondaryOwnerId + " AND pi.OWNER = '" + owner + "' AND SECONDARY_OWNER = '" + secondaryOwner +"'";
            query = query + orderBy;
            ResultSet rs = db.runSelect(query, null);
            List<ProcessInstance> mdwProcessInstanceList = new ArrayList<ProcessInstance>();
            while (rs.next()) {
                ProcessInstance pi = new ProcessInstance(rs.getLong(2), rs.getString(8));
                pi.setOwner(owner);
                pi.setOwnerId(rs.getLong(3));
                pi.setMasterRequestId(rs.getString(9));
                pi.setStatusCode(rs.getInt(4));
                pi.setStartDate(StringHelper.dateToString(rs.getTimestamp(5)));
                pi.setId(rs.getLong(1));
                pi.setComment(rs.getString(10));
                pi.setEndDate(StringHelper.dateToString(rs.getTimestamp(6)));
                mdwProcessInstanceList.add(pi);
            }
            return mdwProcessInstanceList;
        }
        catch (Exception e) {
            throw new DataAccessException(0, "error to load child process instance list", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public int deleteProcessInstances(List<Long> processInstanceIds) throws DataAccessException {
        try {
            db.openConnection();
            int count = 0;
            // TODO rewrite query to avoid iterating
            for (Long processInstanceId : processInstanceIds)
                count += deleteOneProcessInstance(processInstanceId);
            db.commit();
            return count;
        } catch(Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete process instances", e);
        } finally {
            db.closeConnection();
        }
    }

    public int deleteProcessInstancesForProcess(Long processId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "select PROCESS_INSTANCE_ID from PROCESS_INSTANCE where PROCESS_ID=?";
            ResultSet rs = db.runSelect(query, processId);
            List<String> procInstIdList = new ArrayList<String>();
            while (rs.next()) {
                procInstIdList.add(rs.getString(1));
            }
            for (String procInstId : procInstIdList) {
                deleteOneProcessInstance(new Long(procInstId));
                db.commit();    // commit for each deletion
            }
            return procInstIdList.size();
        } catch(Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete process instance", e);
        } finally {
            db.closeConnection();
        }
    }

    private int deleteOneProcessInstance(Long processInstanceId) throws SQLException {
        int count = 0, n;
        String query = "select ATTRIBUTE_VALUE from ATTRIBUTE where ATTRIBUTE_OWNER='SYSTEM' and ATTRIBUTE_NAME='"
                + PropertyNames.MDW_DB_VERSION + "'";
        ResultSet rs = db.runSelect(query, null);

        query = "select PROCESS_INSTANCE_ID"
                + " from PROCESS_INSTANCE"
                + " where OWNER='" + OwnerType.PROCESS_INSTANCE + "' and OWNER_ID=?";
        rs = db.runSelect(query, processInstanceId);
        List<Long> childProcInstIds = new ArrayList<Long>();
        while (rs.next()) {
            childProcInstIds.add(rs.getLong(1));
        }
        for (Long childProcInstId : childProcInstIds) {
            count += deleteOneProcessInstance(childProcInstId);
        }

        query = "delete from EVENT_WAIT_INSTANCE where WORK_TRANS_INSTANCE_ID in"
                + " (select wti.WORK_TRANS_INST_ID from WORK_TRANSITION_INSTANCE wti"
                + " where wti.PROCESS_INST_ID=?)";
        n = db.runUpdate(query, processInstanceId);
        count += n;

        query = "delete from WORK_TRANSITION_INSTANCE where PROCESS_INST_ID=?";
        n = db.runUpdate(query, processInstanceId);
        count += n;
        query = "delete from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID=?";
        n = db.runUpdate(query, processInstanceId);
        count += n;
        query = "delete from VARIABLE_INSTANCE where PROCESS_INST_ID=?";
        n = db.runUpdate(query, processInstanceId);
        count += n;
        query = "delete from INSTANCE_INDEX where OWNER_TYPE='TASK_INSTANCE' and INSTANCE_ID in " +
                " (select TASK_INSTANCE_ID from TASK_INSTANCE " +
                "  where TASK_INSTANCE_OWNER='" + OwnerType.PROCESS_INSTANCE +
                "'   and TASK_INSTANCE_OWNER_ID=?)";
        n = db.runUpdate(query, processInstanceId);
        count += n;
        query = "delete from TASK_INST_GRP_MAPP where TASK_INSTANCE_ID in " +
                " (select TASK_INSTANCE_ID from TASK_INSTANCE " +
                "  where TASK_INSTANCE_OWNER='" + OwnerType.PROCESS_INSTANCE +
                "'   and TASK_INSTANCE_OWNER_ID=?)";
        n = db.runUpdate(query, processInstanceId);
        count += n;

        // delete task instances and related
        query = "delete from INSTANCE_NOTE where INSTANCE_NOTE_OWNER='"+
                OwnerType.TASK_INSTANCE + "' and INSTANCE_NOTE_OWNER_ID in " +
                " (select TASK_INSTANCE_ID from TASK_INSTANCE " +
                "  where TASK_INSTANCE_OWNER='" + OwnerType.PROCESS_INSTANCE +
                "'   and TASK_INSTANCE_OWNER_ID=?)";
        n = db.runUpdate(query, processInstanceId);
        count += n;
        query = "delete from ATTACHMENT where ATTACHMENT_OWNER='"+
                OwnerType.TASK_INSTANCE + "' and ATTACHMENT_OWNER_ID in " +
                " (select TASK_INSTANCE_ID from TASK_INSTANCE " +
                "  where TASK_INSTANCE_OWNER='" + OwnerType.PROCESS_INSTANCE +
                "'   and TASK_INSTANCE_OWNER_ID=?)";
        n = db.runUpdate(query, processInstanceId);
        count += n;

        query = "delete from TASK_INSTANCE " +
                "  where TASK_INSTANCE_OWNER='" + OwnerType.PROCESS_INSTANCE +
                "'   and TASK_INSTANCE_OWNER_ID=?";
        n = db.runUpdate(query, processInstanceId);
        count += n;

        // finally, delete the process instance itself
        query = "delete from PROCESS_INSTANCE where PROCESS_INSTANCE_ID=?";
        n = db.runUpdate(query, processInstanceId);
        count += n;


        return count;
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
        sqlBuff.append(" from process_instance pi\n");
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

                sqlBuff.append("\n and exists (select vi.variable_inst_id from variable_instance vi")
                .append(" where vi.process_inst_id = pi.process_instance_id")
                .append(" and vi.variable_name = '" + varName + "'");

                if (isDate && variableTypeId != null) {
                    sqlBuff.append(" and vi.VARIABLE_TYPE_ID = " + variableTypeId); // date var type
                    if (db.isMySQL())
                        sqlBuff.append("\n and (select concat(substr(ivi.VARIABLE_VALUE, 5, 7), substr(ivi.VARIABLE_VALUE, 25))");
                    else
                        sqlBuff.append("\n and (select substr(ivi.VARIABLE_VALUE, 5, 7) || substr(ivi.VARIABLE_VALUE, 25)");
                    sqlBuff.append("\n     from variable_instance ivi  where ivi.variable_type_id = " + variableTypeId);
                    sqlBuff.append("\n     and ivi.variable_inst_id = vi.variable_inst_id");
                    sqlBuff.append("\n     and ivi.variable_name = '" + varName + "') = '"+ varValue + "') ");
                }
                else {
                    if (varValue != null && (varValue.trim().toLowerCase().startsWith("like") || varValue.trim().toLowerCase().startsWith("in")))
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
        sqlBuff.append("FROM process_instance pi\n");
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
                "STATUS_CD, START_DT, END_DT, COMPCODE, COMMENTS, SECONDARY_OWNER, SECONDARY_OWNER_ID\n" +
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
        pi.setStartDate(StringHelper.dateToString(rs.getTimestamp("START_DT")));
        pi.setEndDate(StringHelper.dateToString(rs.getTimestamp("END_DT")));
        pi.setCompletionCode(rs.getString("COMPCODE"));
        pi.setComment(rs.getString("COMMENTS"));
        pi.setSecondaryOwner(rs.getString("SECONDARY_OWNER"));
        if (pi.getSecondaryOwner() != null)
            pi.setSecondaryOwnerId(rs.getLong("SECONDARY_OWNER_ID"));
        populateNameVersionStatus(pi);
        return pi;
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
                " pi.STATUS_CD, pi.START_DT, pi.END_DT, pi.COMPCODE, pi.COMMENTS" +
                " from PROCESS_INSTANCE pi" +
                " where pi.OWNER = '" + ownerType + "' and pi.OWNER_ID = ?";
        ResultSet rs = db.runSelect(query, ownerId);
        while (rs.next()) {
            if (instanceList == null)
                instanceList = new ArrayList<ProcessInstance>();
            Long processId = rs.getLong("PROCESS_ID");
            String comment = rs.getString("COMMENTS");
            ProcessInstance pi = new ProcessInstance(processId, "");
            pi.setId(rs.getLong("PROCESS_INSTANCE_ID"));
            pi.setOwner(ownerType);
            pi.setOwnerId(ownerId);
            pi.setMasterRequestId(rs.getString("MASTER_REQUEST_ID"));
            pi.setStatusCode(rs.getInt("STATUS_CD"));
            pi.setStartDate(StringHelper.dateToString(rs.getTimestamp("START_DT")));
            pi.setEndDate(StringHelper.dateToString(rs.getTimestamp("END_DT")));
            pi.setCompletionCode(rs.getString("COMPCODE"));
            pi.setComment(comment);
            populateNameVersionStatus(pi);
            instanceList.add(pi);
        }

        return instanceList;
    }

    public ActivityList getActivityInstanceList(Query query) throws DataAccessException {
        try {
            Date start = query.getDateFilter("startDate");
            StringBuilder sql = new StringBuilder();
            db.openConnection();
            sql.append(buildActivityCountQuery(query, start));
            ResultSet rs = db.runSelect(sql.toString(), null);
            Long count;
            if (rs.next())
                count = new Long(rs.getLong(1));
            else
                count = new Long(-1);
            List<ActivityInstance> mdwActivityInstanceList = new ArrayList<>();
            ActivityList actList = new ActivityList(ActivityList.ACTIVITY_INSTANCES, mdwActivityInstanceList);
            if (count <= 0) {
                return actList;
            }

            sql = buildActivityQuery(query, start);

            rs = db.runSelect(sql.toString(), null);
            while (rs.next()) {
                ActivityInstance ai = new ActivityInstance();
                ai.setId(rs.getLong("aii"));
                ai.setDefinitionId("A" + rs.getLong("activity_id"));
                ai.setMasterRequestId(rs.getString("master_request_id"));
                ai.setStartDate(rs.getTimestamp("st"));
                ai.setProcessId(rs.getLong("process_id"));
                ai.setProcessName(rs.getString("process_name"));
                ai.setEndDate(rs.getTimestamp("ed"));
                ai.setResult(rs.getString("cc"));
                ai.setMessage(rs.getString("error"));
                ai.setStatus(WorkStatuses.getName(rs.getInt("status_cd")));
                ai.setProcessInstanceId(rs.getLong("pii"));

                mdwActivityInstanceList.add(ai);
            }
            actList.setRetrieveDate(new Date()); // TODO use db date
            actList.setCount(mdwActivityInstanceList.size());
            actList.setTotal(count);
            return actList;
        }
        catch (Exception e) {
            throw new DataAccessException(-1, "Error loading activity instance list", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public LinkedProcessInstance getProcessInstanceCallHierarchy(Long processInstanceId) throws DataAccessException {
        try {
            db.openConnection();
            ProcessInstance startingInstance = getProcessInstanceBase0(processInstanceId);
            LinkedProcessInstance startingLinked = new LinkedProcessInstance(startingInstance);
            LinkedProcessInstance top = startingLinked;
            // callers
            while (OwnerType.PROCESS_INSTANCE.equals(top.getProcessInstance().getOwner())) {
                ProcessInstance caller = getProcessInstanceBase0(top.getProcessInstance().getOwnerId());
                LinkedProcessInstance callerLinked = new LinkedProcessInstance(caller);
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

    private void addCalledHierarchy(LinkedProcessInstance caller) throws SQLException, DataAccessException {
        ProcessInstance callerProcInst = caller.getProcessInstance();
        List<ProcessInstance> calledInsts = getProcessInstancesForOwner(OwnerType.PROCESS_INSTANCE, callerProcInst.getId());
        if (calledInsts != null) {
            for (ProcessInstance calledInst : calledInsts) {
                LinkedProcessInstance child = new LinkedProcessInstance(calledInst);
                child.setParent(caller);
                caller.getChildren().add(child);
                addCalledHierarchy(child);
            }
        }
    }

    public boolean hasProcessInstances(Long processId) throws DataAccessException {
        try {
            db.openConnection();
            String query;
            if (db.isMySQL()) query = "select process_instance_id from PROCESS_INSTANCE where PROCESS_ID=? limit 0,1";
            else query = "select process_instance_id from PROCESS_INSTANCE where PROCESS_ID=? and ROWNUM = 1";
            ResultSet rs = db.runSelect(query, processId);
            return rs.next();
        } catch (Exception e) {
            throw new DataAccessException(0, e.getMessage(), e);
        } finally {
            db.closeConnection();
        }
    }

    public List<EventLog> getEventLogs(String eventName, String source,
            String ownerType, Long ownerId) throws DataAccessException {
        try {
            db.openConnection();
            StringBuffer query = new StringBuffer();
            query.append("select EVENT_LOG_ID, EVENT_NAME, EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID,");
            query.append("  EVENT_SOURCE, CREATE_DT, EVENT_CATEGORY, EVENT_SUB_CATEGORY,");
            query.append("  COMMENTS, CREATE_USR ");
            query.append("  FROM EVENT_LOG ");
            query.append("where ");
            Vector<Object> args = new Vector<Object>();
            if (eventName!=null) {
                if (args.size()>0) query.append(" and ");
                query.append("EVENT_NAME=?");
                args.add(eventName);
            }
            if (source!=null) {
                if (args.size()>0) query.append(" and ");
                query.append("EVENT_SOURCE=?");
                args.add(source);
            }
            if (ownerType!=null) {
                if (args.size()>0) query.append(" and ");
                query.append("EVENT_LOG_OWNER=?");
                args.add(ownerType);
            }
            if (ownerId!=null) {
                if (args.size()>0) query.append(" and ");
                query.append("EVENT_LOG_OWNER_ID=?");
                args.add(ownerId);
            }
            ResultSet rs = db.runSelect(query.toString(), args.toArray());
            List<EventLog> ret = new ArrayList<EventLog>();
            while (rs.next()) {
                EventLog el = new EventLog();
                el.setId(rs.getLong(1));
                el.setEventName(rs.getString(2));
                el.setOwnerType(rs.getString(3));
                el.setOwnerId(rs.getLong(4));
                el.setSource(rs.getString(5));
                el.setCreateDate(rs.getTimestamp(6).toString());
                el.setCategory(rs.getString(7));
                el.setSubCategory(rs.getString(8));
                el.setComment(rs.getString(9));
                el.setCreateUser(rs.getString(10));
                ret.add(el);
            }
            return ret;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to find task instance", e);
        } finally {
            db.closeConnection();
        }
    }

    protected StringBuilder buildActivityQuery(Query query, Date start) {
        StringBuilder sqlBuff = new StringBuilder();
        if (query.getMax() != Query.MAX_ALL)
            sqlBuff.append(db.pagingQueryPrefix());
        sqlBuff.append("select pi.process_instance_id as pii, pi.master_request_id, ")
        .append("pi.process_id, pi.comments as process_name, ai.activity_instance_id as aii, ai.activity_id, ")
        .append("ai.status_cd, ai.start_dt as st, ai.end_dt as ed, ai.compcode as cc, ai.status_message as error");

        buildProcessQueryCommon(sqlBuff, query, start);
        String orderBy = buildOrderBy(query);
        if (orderBy != null)
            sqlBuff.append("\n").append(orderBy);
        if (query.getMax() != Query.MAX_ALL)
            sqlBuff.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));
        return sqlBuff;
    }

    protected void buildProcessQueryCommon(StringBuilder sqlBuff, Query query, Date start) {
        sqlBuff.append(" FROM process_instance pi, activity_instance ai ");
        sqlBuff.append(" WHERE pi.process_instance_id = ai.process_instance_id  ");
        if (query.getFind() != null) {
            try {
                // numeric value means instance id
                long findInstId = Long.parseLong(query.getFind());
                sqlBuff.append(" and ai.activity_instance_id like '" + findInstId + "%'\n");
            }
            catch (NumberFormatException ex) {
                // otherwise master request id
                sqlBuff.append(" and pi.master_request_id like '" + query.getFind() + "%'\n");
            }
            sqlBuff.append(" and pi.STATUS_CD NOT IN (" +  WorkStatus.STATUS_COMPLETED.intValue() + "," + WorkStatus.STATUS_CANCELLED.intValue() + "," + WorkStatus.STATUS_PURGE.intValue() + ")");
            sqlBuff.append(" and ai.STATUS_CD IN (" +  WorkStatus.STATUS_FAILED.intValue() + "," + WorkStatus.STATUS_WAITING.intValue() + "," + WorkStatus.STATUS_IN_PROGRESS.intValue() + "," + WorkStatus.STATUS_HOLD.intValue() + ")");
        } else {
            // actInstId
            String actInstId = query.getFilter("instanceId");
            if (actInstId != null) {
                sqlBuff.append(" and ai.activity_instance_id  = ").append(actInstId).append("\n");
            }
        }
        if (start != null)
        {
            String startStr = new SimpleDateFormat("dd-MMM-yyyy").format(start);

            if (db.isMySQL())
                sqlBuff.append(" and ai.start_dt >= STR_TO_DATE('" + startStr + "','%d-%M-%Y')\n   ");
            else
                sqlBuff.append(" and ai.start_dt >= '" + startStr + "'\n   ");
        }
        // status
        String status = query.getFilter("status");
        if (status != null) {
            sqlBuff.append(" and ai.status_cd = ").append(WorkStatuses.getCode(status)).append("\n");
        }
    }

    protected String buildCountQuery(Map<String,String> pMap) {
        StringBuffer sqlBuff = new StringBuffer();
        sqlBuff.append("SELECT count(pi.process_instance_id) ");
        sqlBuff.append("FROM process_instance pi ");
        sqlBuff.append("WHERE pi.PROCESS_ID is not null "); // just to allow next condition to have "and"
        buildQueryCommon(sqlBuff, pMap, null);
        return sqlBuff.toString();
    }

    protected String buildActivityCountQuery(Query query, Date start) {
        StringBuilder sqlBuff = new StringBuilder();
        sqlBuff.append("SELECT count(pi.process_instance_id) ");
        buildProcessQueryCommon(sqlBuff, query, start);
        return sqlBuff.toString();
    }

    private String buildOrderBy(Query query) {
        StringBuilder sb = new StringBuilder();
        sb.append(" order by pi.process_instance_id");
        if (query.isDescending())
            sb.append(" desc");
        sb.append("\n");
        return sb.toString();
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
        if (pMap.containsKey("statusCodeList") && !StringHelper.isEmpty(pMap.get("statusCodeList"))){
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
