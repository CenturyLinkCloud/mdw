/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.file;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.dataaccess.BaselineData;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.version5.RuntimeDataAccessV5;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.activity.ActivityInstance;
import com.centurylink.mdw.model.value.activity.ActivityList;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.process.ProcessVO;

/**
 * Used for VCS-based assets in the runtime container (not Designer).
 * Accesses the db, but takes into account the fact that the design-time tables don't exist.
 * Also handles compatibility for pre-VCS processes that still reside in the db.
 */
public class RuntimeDataAccessVcs extends RuntimeDataAccessV5 {

    public RuntimeDataAccessVcs(DatabaseAccess db, int databaseVersion, int supportedVersion, BaselineData baselineData) {
        super(db, databaseVersion, supportedVersion, baselineData.getVariableTypes());
    }

    @Override
    public ProcessList getProcessInstanceList(Map<String,String> criteria, int pageIndex, int pageSize, String orderBy) throws DataAccessException {
        ProcessList procList = super.getProcessInstanceList(criteria, pageIndex, pageSize, orderBy);
        for (ProcessInstanceVO process : procList.getItems())
            populateNameVersionStatus(process);
        return procList;
    }

    @Override
    public ProcessList getProcessInstanceList(Map<String,String> criteria, Map<String,String> variables, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException {
        ProcessList procList = super.getProcessInstanceList(criteria, variables, pageIndex, pageSize, orderBy);
        for (ProcessInstanceVO process : procList.getItems())
            populateNameVersionStatus(process);
        return procList;
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

    @Override
    protected String buildQuery(Map<String,String> criteria, List<String> variables, Map<String,String> variableCriteria, int startIndex, int endIndex, String orderBy) {
        StringBuffer sqlBuff = new StringBuffer();
        if (startIndex != QueryRequest.ALL_ROWS)
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
        if (startIndex != QueryRequest.ALL_ROWS)
            sqlBuff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        return sqlBuff.toString();
    }

    @Override
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
                        sqlBuff.append("\n and (select str_to_date(concat(substr(ivi.VARIABLE_VALUE, 5, 7), substr(ivi.VARIABLE_VALUE, 25)), '%M %D %Y')");
                    else
                        sqlBuff.append("\n and (select to_date(substr(ivi.VARIABLE_VALUE, 5, 7) || substr(ivi.VARIABLE_VALUE, 25), 'MON DD YYYY')");
                    sqlBuff.append("\n     from variable_instance ivi  where ivi.variable_type_id = " + variableTypeId);
                    sqlBuff.append("\n     and ivi.variable_inst_id = vi.variable_inst_id");
                    if (db.isMySQL())
                        varValue = dateConditionToMySQL(varValue);
                    sqlBuff.append("\n     and ivi.variable_name = '" + varName + "') "+ varValue + ") ");
                }
                else {
                    sqlBuff.append(" and vi.VARIABLE_VALUE " + varValue + ") ");
                }
            }
        }
        return sqlBuff.toString();
    }

    @Override
    protected String buildQuery(Map<String,String> criteria, int startIndex, int endIndex, String orderBy) {
        StringBuffer sqlBuff = new StringBuffer();
        if (startIndex != QueryRequest.ALL_ROWS)
            sqlBuff.append(db.pagingQueryPrefix());
        sqlBuff.append("SELECT ");
        if (startIndex != QueryRequest.ALL_ROWS)
            sqlBuff.append("/*+ NO_USE_NL(pi r) */ ");
        sqlBuff.append("pi.PROCESS_INSTANCE_ID, pi.MASTER_REQUEST_ID, pi.STATUS_CD, pi.START_DT, ");
        sqlBuff.append("pi.END_DT, pi.OWNER, pi.OWNER_ID, pi.PROCESS_ID, '' as NAME, pi.COMMENTS\n");
        sqlBuff.append("FROM process_instance pi\n");
        sqlBuff.append("where 1=1 ");
        if (!OwnerType.MAIN_PROCESS_INSTANCE.equals(criteria.get("owner")))
            sqlBuff.append(" and pi.OWNER!='" + OwnerType.MAIN_PROCESS_INSTANCE +"' ");
        buildQueryCommon(sqlBuff, criteria, orderBy);
        if (startIndex != QueryRequest.ALL_ROWS)
            sqlBuff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        return sqlBuff.toString();
    }

    @Override
    protected String buildProcessNameClause(String qualifiedName) {
        int slash = qualifiedName.indexOf('/');
        String pkg = qualifiedName.substring(0, slash);
        String proc = qualifiedName.substring(slash + 1);
        return " AND pi.COMMENTS like '" + pkg + " v%/" + proc + " v%'";
    }

    @Override
    protected ProcessInstanceVO getProcessInstanceBase0(Long processInstanceId) throws SQLException, DataAccessException {
        String query = "select PROCESS_ID, OWNER, OWNER_ID, MASTER_REQUEST_ID, " +
                "STATUS_CD, START_DT, END_DT, COMPCODE, COMMENTS\n" +
                "from PROCESS_INSTANCE where PROCESS_INSTANCE_ID = ?";
        ResultSet rs = db.runSelect(query, processInstanceId);
        if (!rs.next())
            throw new SQLException("Cannot find process instance ID: " + processInstanceId);
        ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong("PROCESS_ID"), "");
        pi.setId(processInstanceId);
        pi.setOwner(rs.getString("OWNER"));
        pi.setOwnerId(rs.getLong("OWNER_ID"));
        pi.setMasterRequestId(rs.getString("MASTER_REQUEST_ID"));
        pi.setStatusCode(rs.getInt("STATUS_CD"));
        pi.setStartDate(StringHelper.dateToString(rs.getTimestamp("START_DT")));
        pi.setEndDate(StringHelper.dateToString(rs.getTimestamp("END_DT")));
        pi.setCompletionCode(rs.getString("COMPCODE"));
        pi.setComment(rs.getString("COMMENTS"));
        populateNameVersionStatus(pi);
        return pi;
    }

    /**
     * For VCS-defined processes, relies on comment having been set.
     * Also sets status name from code.
     */
    protected void populateNameVersionStatus(ProcessInstanceVO processInstance) throws DataAccessException {
        if (processInstance.getComment() == null) {
            // try retrieving process def from db for compatibility
            try {
                ProcessVO process = DataAccess.getDbProcessLoader().getProcessBase(processInstance.getProcessId());
                processInstance.setProcessName(process.getName());
                processInstance.setProcessVersion(process.getVersionString());
            }
            catch (DataAccessException ex) {
                // Most likely due to old VCS instances without comment.
                // This is a transient condition until folks upgrade to MDW 5.5.12+ and age off previous instances.
                LoggerUtil.getStandardLogger().debugException("No compatibility definition for process instance " + processInstance.getId(), ex);
            }
        }
        else {
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

    @Override
    protected List<ProcessInstanceVO> getProcessInstancesForOwner(String ownerType, Long ownerId) throws SQLException, DataAccessException {
        List<ProcessInstanceVO> instanceList = null;
        String query = "select pi.PROCESS_INSTANCE_ID, pi.PROCESS_ID, pi.MASTER_REQUEST_ID," +
                " pi.STATUS_CD, pi.START_DT, pi.END_DT, pi.COMPCODE, pi.COMMENTS" +
                " from PROCESS_INSTANCE pi" +
                " where pi.OWNER = '" + ownerType + "' and pi.OWNER_ID = ?";
        ResultSet rs = db.runSelect(query, ownerId);
        while (rs.next()) {
            if (instanceList == null)
                instanceList = new ArrayList<ProcessInstanceVO>();
            Long processId = rs.getLong("PROCESS_ID");
            String comment = rs.getString("COMMENTS");
            ProcessInstanceVO pi = new ProcessInstanceVO(processId, "");
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
            List<ActivityInstance> mdwActivityInstanceList = new ArrayList<ActivityInstance>();
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
                ai.setActivityInstanceId(rs.getLong("aii"));

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


}
