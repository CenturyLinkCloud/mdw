/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.version4;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.RuntimeDataAccess;
import com.centurylink.mdw.model.data.event.EventLog;
import com.centurylink.mdw.model.value.activity.ActivityList;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.task.TaskActionVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;

public class RuntimeDataAccessV4 extends CommonDataAccess implements RuntimeDataAccess {

    public RuntimeDataAccessV4(DatabaseAccess db, int databaseVersion, int supportedVersion) {
        super(db, databaseVersion, supportedVersion);
    }

    public ProcessInstanceVO getProcessInstanceBase(Long procInstId) throws DataAccessException {
        try {
            db.openConnection();
            return getProcessInstanceBase0(procInstId);
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to process instance", e);
        } finally {
            db.closeConnection();
        }
    }

    protected ProcessInstanceVO getProcessInstanceBase0(Long procInstId) throws SQLException, DataAccessException {
        String query = "select pi.PROCESS_ID,pi.OWNER,pi.OWNER_ID,pi.MASTER_REQUEST_ID," +
            " pi.STATUS_CD,pi.START_DT,pi.END_DT,w.WORK_NAME" +
            " from PROCESS_INSTANCE pi, WORK w" +
            " where pi.PROCESS_INSTANCE_ID=? and pi.PROCESS_ID=w.WORK_ID";
        ResultSet rs = db.runSelect(query, procInstId);
        if (!rs.next()) throw new SQLException("failed to load process instance");
        ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong(1), rs.getString(8));
        pi.setOwner(rs.getString(2));
        pi.setOwnerId(rs.getLong(3));
        pi.setMasterRequestId(rs.getString(4));
        pi.setStatusCode(rs.getInt(5));
        pi.setStartDate(StringHelper.dateToString(rs.getTimestamp(6)));
        pi.setId(procInstId);
        pi.setEndDate(StringHelper.dateToString(rs.getTimestamp(7)));
        return pi;
    }

    public ProcessInstanceVO getProcessInstanceForSecondary(String pSecOwner, Long pSecOwnerId)
            throws DataAccessException {
        try {
            db.openConnection();
            String query = "select PROCESS_INSTANCE_ID,PROCESS_ID,OWNER,OWNER_ID,MASTER_REQUEST_ID," +
                    " STATUS_CD,START_DT,END_DT" +
                    " from PROCESS_INSTANCE where SECONDARY_OWNER=? and SECONDARY_OWNER_ID=?";
            Object[] args = new Object[2];
            args[0] = pSecOwner;
            args[1] = pSecOwnerId;
            ResultSet rs = db.runSelect(query, args);
            if (!rs.next()) throw new DataAccessException("failed to load process instance");
            String processName = null;
            ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong(2), processName);
            pi.setOwner(rs.getString(3));
            pi.setOwnerId(rs.getLong(4));
            pi.setMasterRequestId(rs.getString(5));
            pi.setStatusCode(rs.getInt(6));
            pi.setStartDate(StringHelper.dateToString(rs.getTimestamp(7)));
            pi.setId(rs.getLong(1));
            pi.setEndDate(StringHelper.dateToString(rs.getTimestamp(8)));
            return pi;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to load variable types", e);
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Make available the SECONDARY_OWNER_ID and SECONDARY_OWNER details for a process
     */
    public ProcessInstanceVO getProcessInstanceForCalling(Long procInstId)
            throws DataAccessException {
        try {
            db.openConnection();
            String query = "select PROCESS_INSTANCE_ID,PROCESS_ID,OWNER,OWNER_ID,MASTER_REQUEST_ID,"
                    + "STATUS_CD,SECONDARY_OWNER,SECONDARY_OWNER_ID,COMPCODE,COMMENTS"
                    + " from PROCESS_INSTANCE where PROCESS_INSTANCE_ID=?";
            ResultSet rs = db.runSelect(query, procInstId);
            if (!rs.next())
                throw new DataAccessException("failed to load process instance");
            ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong(2), null);
            pi.setId(rs.getLong(1));
            pi.setOwner(rs.getString(3));
            pi.setOwnerId(rs.getLong(4));
            pi.setMasterRequestId(rs.getString(5));
            pi.setStatusCode(rs.getInt(6));
            pi.setSecondaryOwner(rs.getString(7));
            pi.setSecondaryOwnerId(rs.getLong(8));
            pi.setCompletionCode(rs.getString(9));
            pi.setComment(rs.getString(10));
            return pi;
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to load process ", e);
        }
        finally {
            db.closeConnection();
        }
    }

    /**
     * return the external message associated with the activity
     * instance. The activity must be either an adapter or a start
     * activity. When eventInstId is not null, this is a start activity.
     * When it is null, then this is a adaptor activity, and the activity ID
     * and activity instance ID must be present.
     */
    public ExternalMessageVO getExternalMessage(Long activityId, Long activityInstId,
            Long eventInstId) throws DataAccessException {
        try {
            db.openConnection();
            String query;
            if (eventInstId==null) {    // adapter activity
                query =  "select REQUEST_DATA, RESPONSE_DATA from ADAPTER_INSTANCE"
                    + " where ADAPTER_ID=? and ADAPTER_INSTANCE_OWNER_ID=?";
                Object[] args = new Object[2];
                args[0] = activityId;
                args[1] = activityInstId;
                ResultSet rs = db.runSelect(query, args);
                if (rs.next()) {
                    return new ExternalMessageVO(rs.getString(1), rs.getString(2));
                } else return null;
            } else {        // start activity
                query = "select EXTERNAL_EVENT_DATA from EXTERNAL_EVENT_INSTANCE"
                    + " where EXTERNAL_EVENT_INSTANCE_ID=?";
                ResultSet rs = db.runSelect(query, eventInstId);
                if (!rs.next()) return null;
                String request = rs.getString(1);
                query = "select CONTENT from DOCUMENT where OWNER_TYPE='"
                    + OwnerType.EXTERNAL_EVENT_INSTANCE_RESPONSE
                    + "' and OWNER_ID=?";
                rs = db.runSelect(query, eventInstId);
                String resp=null;
                if (rs.next()) resp = rs.getString(1);
                return new ExternalMessageVO(request, resp);
            }
        } catch (Exception e) {
            throw new DataAccessException(0,"error to load external message", e);
        } finally {
            db.closeConnection();
        }
    }

    public String getExternalEventDetails(Long externalEventId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "select external_event_data from external_event_instance where external_event_instance_id = ?";
            ResultSet rs = db.runSelect(query, externalEventId);
            if (rs.next()) {
                return rs.getString(1);
            }
            else {
                return null;
            }

        }
        catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
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

    protected String buildProcessNameClause(String processName) {
        return " AND pi.PROCESS_ID in (select WORK_ID from WORK where WORK_NAME = '" + processName + "')";
    }

    protected String buildQuery(Map<String,String> pMap, int startIndex, int endIndex, String orderBy) {
        StringBuffer sqlBuff = new StringBuffer();
        if (startIndex!=QueryRequest.ALL_ROWS) sqlBuff.append(db.pagingQueryPrefix());
        sqlBuff.append("SELECT ");
        if (startIndex!=QueryRequest.ALL_ROWS) sqlBuff.append("/*+ NO_USE_NL(pi r) */ ");
        sqlBuff.append("pi.PROCESS_INSTANCE_ID,pi.MASTER_REQUEST_ID,pi.STATUS_CD,pi.START_DT,");
        sqlBuff.append("pi.END_DT,pi.OWNER,pi.OWNER_ID,pi.PROCESS_ID,w.WORK_NAME,pi.COMMENTS ");
        sqlBuff.append("FROM process_instance pi, work w ");
        sqlBuff.append("WHERE pi.PROCESS_ID=w.WORK_ID ");
        if (!OwnerType.MAIN_PROCESS_INSTANCE.equals(pMap.get("owner"))) {
        	sqlBuff.append(" and pi.OWNER!='" + OwnerType.MAIN_PROCESS_INSTANCE +"' ");
        }
        buildQueryCommon(sqlBuff, pMap, orderBy);
        if (startIndex!=QueryRequest.ALL_ROWS) sqlBuff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        return sqlBuff.toString();
    }

    protected String buildQuery(Map<String,String> criteria, List<String> variables, Map<String,String> variableCriteria, int startIndex, int endIndex, String orderBy) {
        StringBuffer sqlBuff = new StringBuffer();
        if (startIndex!=QueryRequest.ALL_ROWS) sqlBuff.append(db.pagingQueryPrefix());
        sqlBuff.append("SELECT pis.PROCESS_INSTANCE_ID, pis.MASTER_REQUEST_ID, pis.STATUS_CD, pis.START_DT, pis.END_DT, pis.OWNER, pis.OWNER_ID, pis.PROCESS_ID, pis.PROCESS_NAME");
        if (variables != null && variables.size() > 0) {
        	for (String varName : variables) {
        		sqlBuff.append(", ").append(varName.startsWith("DATE:") ? varName.substring(5) : varName);
        	}
        }
        sqlBuff.append("\n    FROM (\n");
        sqlBuff.append("  SELECT pi.*, w.work_name as process_name");
        sqlBuff.append(buildVariablesSelect(variables));
        sqlBuff.append(buildVariablesClause(criteria, variables, variableCriteria));
        sqlBuff.append(") pis\n");
        if (orderBy != null)
        	sqlBuff.append("\n").append(orderBy);
        if (startIndex!=QueryRequest.ALL_ROWS)
        	sqlBuff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        return sqlBuff.toString();
    }

    protected String buildCountQuery(Map<String,String> pMap) {
        StringBuffer sqlBuff = new StringBuffer();
        sqlBuff.append("SELECT count(pi.process_instance_id) ");
        sqlBuff.append("FROM process_instance pi ");
        sqlBuff.append("WHERE pi.PROCESS_ID is not null ");	// just to allow next condition to have "and"
        buildQueryCommon(sqlBuff, pMap, null);
        return sqlBuff.toString();
    }

    protected String buildCountQuery(Map<String,String> criteria, Map<String,String> variablesCriteria) {
        if (variablesCriteria == null || variablesCriteria.isEmpty())
            return buildCountQuery(criteria);

        StringBuffer sqlBuff = new StringBuffer();
        sqlBuff.append("SELECT COUNT(pis.process_instance_id)\n");
        sqlBuff.append("FROM (\n");
        if (getSupportedVersion() < DataAccess.schemaVersion52)
            sqlBuff.append("  SELECT pi.*, work_name as process_name\n");
        else
            sqlBuff.append("  SELECT pi.*, r.RULE_SET_NAME as process_name\n");
        sqlBuff.append(buildVariablesClause(criteria, null, variablesCriteria));
        sqlBuff.append(") pis");
        return sqlBuff.toString();
    }

    public String buildVariablesSelect(List<String> variables) {
        StringBuffer buff = new StringBuffer();
        if (variables != null && variables.size() > 0) {
            for (String varName : variables) {
                String name = varName.startsWith("DATE:") ? varName.substring(5) : varName;
                buff.append(",\n");
                buff.append("    (select vi.VARIABLE_VALUE from VARIABLE v, VARIABLE_INSTANCE vi "
                        + " where pi.PROCESS_INSTANCE_ID = vi.PROCESS_INST_ID "
                        + " and v.variable_name = '" + name + "' "
                        + " and vi.variable_id = v.variable_id) " + name);
            }
        }
        return buff.toString();
    }

    protected String buildVariablesClause(Map<String,String> criteria, List<String>variables, Map<String,String> variableCriteria) {
        StringBuffer sqlBuff = new StringBuffer();
        if (variableCriteria != null && !variableCriteria.isEmpty()) {
            int i = variableCriteria.keySet().size();
            for (String varName : variableCriteria.keySet()) {
                sqlBuff.append(" \nFROM process_instance pi, variable v, variable_instance vi, work w\n");
                sqlBuff.append("  WHERE pi.process_instance_id = vi.process_inst_id\n");
                sqlBuff.append("  AND pi.process_id = w.work_id\n");
                sqlBuff.append("  AND vi.variable_id = v.variable_id\n");
                buildQueryCommon(sqlBuff, criteria, null);
                String varValue = variableCriteria.get(varName);
                boolean isDate = varName.startsWith("DATE:");
                if (isDate) {
                    varName = varName.substring(5);
                }
                sqlBuff.append(" and v.VARIABLE_NAME = '" + varName + "' ");
                if (isDate) {
                    sqlBuff.append(" and v.VARIABLE_TYPE_ID = 5 "); // date var type
                    // inline view to avoid parse errors (TODO better way?)
                    sqlBuff.append("\n and (select to_date(substr(ivi.VARIABLE_VALUE, 5, 7) || substr(ivi.VARIABLE_VALUE, 25), 'MON DD YYYY')"
                      + "\n     from variable iv, variable_instance ivi"
                      + "\n     where iv.variable_type_id = 5"
                      + "\n     and ivi.variable_id = iv.variable_id"
                      + "\n     and ivi.variable_inst_id = vi.variable_inst_id"
                      + "\n     and iv.variable_name = '" + varName + "') " + varValue + " ");
                }
                else {
                    sqlBuff.append(" and vi.VARIABLE_VALUE " + varValue + " ");
                }
                if (--i > 0)
                  sqlBuff.append("\nintersect\n").append("  SELECT pi.*, w.work_name as process_name\n").append(buildVariablesSelect(variables));
            }
        }
        else {
            sqlBuff.append("\n  FROM process_instance pi, work w\n");
            sqlBuff.append("WHERE pi.PROCESS_ID = w.work_id ");
            buildQueryCommon(sqlBuff, criteria, null);
        }
        return sqlBuff.toString();
    }

    public ProcessList getProcessInstanceList(Map<String,String> criteria, int pageIndex, int pageSize, String orderBy)
    throws DataAccessException {
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
            int startIndex = pageSize==QueryRequest.ALL_ROWS ? QueryRequest.ALL_ROWS : (pageIndex - 1) * pageSize;
            int endIndex = startIndex + pageSize;
            query = buildQuery(criteria, startIndex, endIndex, orderBy);

            rs = db.runSelect(query, null);
            List<ProcessInstanceVO> mdwProcessInstanceList = new ArrayList<ProcessInstanceVO>();
            while (rs.next()) {
            	ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong(8), rs.getString(9));
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
            processList.setRetrieveDate(new Date()); // TODO use db time
            processList.setCount(mdwProcessInstanceList.size());
            processList.setTotal(count);
            return processList;
        } catch (Exception e) {
            throw new DataAccessException(0,"error to load child process instance list", e);
        } finally {
            db.closeConnection();
        }
    }

    public ProcessList getProcessInstanceList(
            Map<String,String> criteria, Map<String,String> variables,
            int pageIndex, int pageSize, String orderBy) throws DataAccessException {
        return getProcessInstanceList(criteria, null, variables, pageIndex, pageSize, orderBy);
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

            QueryRequest req = new QueryRequest();
            req.setRestrictions(criteria);
            if (orderBy == null)
                orderBy = " ORDER BY PROCESS_INSTANCE_ID DESC\n";
            int startIndex = pageSize==QueryRequest.ALL_ROWS ? QueryRequest.ALL_ROWS : (pageIndex - 1) * pageSize;
            int endIndex = startIndex + pageSize;
            query = buildQuery(criteria, variableNames, variables, startIndex, endIndex, orderBy);

            rs = db.runSelect(query, null);
            List<ProcessInstanceVO> mdwProcessInstanceList = new ArrayList<ProcessInstanceVO>();
            while (rs.next()) {
            	ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong(8), rs.getString(9));
            	pi.setOwner(rs.getString(6));
            	pi.setOwnerId(rs.getLong(7));
            	pi.setMasterRequestId(rs.getString(2));
            	pi.setStatusCode(rs.getInt(3));
            	pi.setStartDate(StringHelper.dateToString(rs.getTimestamp(4)));
            	pi.setId(rs.getLong(1));
                pi.setComment(rs.getString(10));
                pi.setEndDate(StringHelper.dateToString(rs.getTimestamp(5)));
                if (variableNames != null && variableNames.size() > 0) {
                    List<VariableInstanceInfo> vars = new ArrayList<VariableInstanceInfo>();
                    for (String varName : variableNames) {
                        String name = varName.startsWith("DATE:") ? varName.substring(5) : varName;
                        String varVal = rs.getString(name.toUpperCase());
                        VariableInstanceInfo varInstInfo = new VariableInstanceInfo();
                        varInstInfo.setName(name);
                        varInstInfo.setStringValue(varVal);
                        vars.add(varInstInfo);
                    }
                    pi.setVariables(vars);
                }
                mdwProcessInstanceList.add(pi);
            }
            ProcessList procList = new ProcessList(ProcessList.PROCESS_INSTANCES, mdwProcessInstanceList);
            procList.setRetrieveDate(new Date()); // TODO use db date
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

    public List<ProcessInstanceVO> getProcessInstanceList(String owner, String secondaryOwner,
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
            List<ProcessInstanceVO> mdwProcessInstanceList = new ArrayList<ProcessInstanceVO>();
            while (rs.next()) {
                ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong(2), rs.getString(8));
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

    public  List<TaskInstanceVO> getTaskInstancesForProcessInstance(Long processInstId) throws DataAccessException {
        return getTaskInstances(processInstId, null);
    }

    public List<TaskInstanceVO> getTaskInstances(Long processInstId, Long taskId) throws DataAccessException {
        try {
            db.openConnection();
            String query;
            if (db.isMySQL()) {
            	query = "select ti.TASK_INSTANCE_ID,ti.TASK_ID,ti.TASK_INSTANCE_STATUS," +
	                "ui.CUID,ti.TASK_START_DT,ti.TASK_END_DT,ti.TASK_INSTANCE_STATE," +
	                "ti.COMMENTS,ti.OWNER_APP_NAME,ti.ASSOCIATED_TASK_INST_ID,t.TASK_NAME," +
	                "ti.TASK_INST_SECONDARY_OWNER, ti.TASK_INST_SECONDARY_OWNER_ID" +
	                " from TASK_INSTANCE ti left join USER_INFO ui" +
	                " on ui.USER_INFO_ID = ti.TASK_CLAIM_USER_ID, " +
	                " TASK t" +
	                " where ti.TASK_INSTANCE_OWNER=? and" +
	                " ti.TASK_INSTANCE_OWNER_ID=? and" +
	                " ti.TASK_ID = t.TASK_ID";
            } else {
            	query = "select ti.TASK_INSTANCE_ID,ti.TASK_ID,ti.TASK_INSTANCE_STATUS," +
                    "ui.CUID,ti.TASK_START_DT,ti.TASK_END_DT,ti.TASK_INSTANCE_STATE," +
                    "ti.COMMENTS,ti.OWNER_APP_NAME,ti.ASSOCIATED_TASK_INST_ID,t.TASK_NAME," +
                    "ti.TASK_INST_SECONDARY_OWNER, ti.TASK_INST_SECONDARY_OWNER_ID" +
                    " from TASK_INSTANCE ti, USER_INFO ui, TASK t" +
                    " where ti.TASK_INSTANCE_OWNER=? and" +
                    " ti.TASK_INSTANCE_OWNER_ID=? and" +
                    " ti.TASK_ID = t.TASK_ID and" +
                    " ui.USER_INFO_ID(+) = ti.TASK_CLAIM_USER_ID";
            }
            Object[] args;
            if (taskId!=null) {
                query += " and ti.TASK_ID=?";
                args = new Object[3];
                args[2] = taskId;
            } else args = new Object[2];
            args[0] = OwnerType.PROCESS_INSTANCE;
            args[1] = processInstId;
            ResultSet rs = db.runSelect(query, args);
            List<TaskInstanceVO> ret = new ArrayList<TaskInstanceVO>();
            while (rs.next()) {
                Long taskInstId = rs.getLong(1);
                String pTaskName = rs.getString(11);
                taskId = rs.getLong(2);
                String pMasterRequestId = null;
                Date pStartDate = rs.getTimestamp(5);
                Date pEndDate = rs.getTimestamp(6);
                Date pDueDate = null;
                Integer pStatusCd = new Integer(rs.getInt(3));
                Integer pStateCd = new Integer(rs.getInt(7));
                String pComments = rs.getString(8);
                String pClaimUserCuid = rs.getString(4);
                String pTaskMessage = null;
                String pActivityName = null;
                String pCategoryCd = null;
                String pOwnerAppName = rs.getString(9);     // remote application name
                Long pAssTaskInstId = new Long(rs.getLong(10));
                TaskInstanceVO vo = new TaskInstanceVO(taskInstId, taskId, pTaskName, pMasterRequestId,
                        pStartDate, pEndDate, pDueDate, pStatusCd, pStateCd,
                        pComments, pClaimUserCuid, pTaskMessage, pActivityName,
                        pCategoryCd, pOwnerAppName, pAssTaskInstId);
                String secondaryOwner = rs.getString(12);
                if (!StringHelper.isEmpty(secondaryOwner)) {
                    vo.setSecondaryOwnerType(secondaryOwner);
                    vo.setSecondaryOwnerId(new Long(rs.getLong(13)));
                }
                ret.add(vo);
            }
            if (getSupportedVersion() > DataAccess.schemaVersion5) {
                for (TaskInstanceVO vo : ret) {
                    vo.setWorkgroups(getTaskInstanceWorkgroups(vo));
                }
            }
            return ret;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to load task instance list", e);
        } finally {
            db.closeConnection();
        }
    }

    public  List<TaskInstanceVO> getTaskInstancesForMasterRequestId(String masterRequestId)
            throws DataAccessException {
        try {
            db.openConnection();
            String query = "select ti.TASK_INSTANCE_ID,ti.TASK_ID,ti.TASK_INSTANCE_STATUS," +
                "   ti.TASK_START_DT,ti.TASK_END_DT,ti.TASK_INSTANCE_STATE," +
                "   ti.COMMENTS,ti.OWNER_APP_NAME,ti.ASSOCIATED_TASK_INST_ID," +
                "   t.TASK_NAME, si.SLA_ESTM_COMP_DT, ui.CUID, tc.TASK_CATEGORY_CD," +
                "   (select ai.STATUS_MESSAGE " +
                "    from ACTIVITY_INSTANCE ai , PROCESS_INSTANCE pi2" +
                "    where ti.TASK_INSTANCE_OWNER_ID = pi2.PROCESS_INSTANCE_ID" +
                "      and pi2.SECONDARY_OWNER ='ACTIVITY_INSTANCE'" +
                "      and ai.ACTIVITY_INSTANCE_ID = pi2.SECONDARY_OWNER_ID)," +
                "   (select w.WORK_NAME from ACTIVITY_INSTANCE ai, WORK w, PROCESS_INSTANCE pi2" +
                "    where ti.TASK_INSTANCE_OWNER_ID = pi2.PROCESS_INSTANCE_ID" +
                "      and pi2.SECONDARY_OWNER ='ACTIVITY_INSTANCE'" +
                "      and ai.ACTIVITY_INSTANCE_ID = pi2.SECONDARY_OWNER_ID" +
                "      and ai.ACTIVITY_ID = w.WORK_ID)," +
                "   pi1.MASTER_REQUEST_ID" +
                " from TASK_INSTANCE ti, TASK t, SLA_INSTANCE si, USER_INFO ui, " +
                "   TASK_CATEGORY tc, PROCESS_INSTANCE pi1" +
                " where ti.TASK_ID = t.TASK_ID and" +
                "   t.TASK_CATEGORY_ID = tc.TASK_CATEGORY_ID and" +
                "   si.SLA_INST_OWNER_ID(+)= ti.TASK_INSTANCE_ID and" +
                "   si.SLA_INST_OWNER(+) = 'TASK_INSTANCE' and" +
                "   ui.USER_INFO_ID(+) = ti.TASK_CLAIM_USER_ID and" +
                "   ti.TASK_INSTANCE_OWNER_ID in" +
                "     (select PROCESS_INSTANCE_ID from PROCESS_INSTANCE" +
                "      where MASTER_REQUEST_ID = ?) and" +
                "   ti.TASK_INSTANCE_OWNER_ID = pi1.PROCESS_INSTANCE_ID";
            ResultSet rs = db.runSelect(query, masterRequestId);
            List<TaskInstanceVO> ret = new ArrayList<TaskInstanceVO>();
            while (rs.next()) {
                String taskMessage = rs.getString(14);
                if (taskMessage == null) taskMessage = rs.getString(7);
                TaskInstanceVO vo = new TaskInstanceVO(rs.getLong(1),       // task inst ID
                        rs.getLong(2),          // task ID
                        rs.getString(10),       // task name
                        rs.getString(16),       // master request ID
                        rs.getTimestamp(4),     // start date
                        rs.getTimestamp(5),     // end date
                        rs.getTimestamp(11),    // estimated date from SLA instance
                        rs.getInt(3),           // task status code
                        rs.getInt(6),           // task state code
                        rs.getString(7),        // task comment
                        rs.getString(12),       // claim user cuid
                        taskMessage,            // task message
                        rs.getString(15),       // activity name
                        rs.getString(13),       // category code
                        rs.getString(8),        // owner application name (for remote task link)
                        rs.getLong(9));         // associated task instance ID
                ret.add(vo);
            }
            for (TaskInstanceVO vo : ret) {
                vo.setWorkgroups(getTaskInstanceWorkgroups(vo));
            }
            return ret;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to load task instance list", e);
        } finally {
            db.closeConnection();
        }
    }

    private List<String> getTaskInstanceWorkgroups(TaskInstanceVO vo) throws DataAccessException {
        try {
            List<String> workgroups = new ArrayList<String>();
            String query = "select group_name from task_inst_grp_mapp tigm, user_group ug where tigm.user_group_id = ug.user_group_id and tigm.task_instance_id = ?";
            ResultSet rs = db.runSelect(query, vo.getTaskInstanceId());
            while (rs.next()) {
                workgroups.add(rs.getString("group_name"));
            }
            if (workgroups.isEmpty()) {
                query = "select attribute_value from attribute where attribute_name = '" + TaskAttributeConstant.GROUPS + "' and attribute_owner = '" + OwnerType.TASK + "' and attribute_owner_id = ?";
                rs = db.runSelect(query, vo.getTaskId());
                if (rs.next()) {
                    String groupsAttr = rs.getString("attribute_value");
                    workgroups = Arrays.asList(groupsAttr.split(","));
                }
            }
            if (workgroups.isEmpty() && getSupportedVersion() < DataAccess.schemaVersion52) {
                query = "select group_name from task_usr_grp_mapp tugm, user_group ug where tugm.user_group_id = ug.user_group_id and tugm.task_id = ?";
                rs = db.runSelect(query, vo.getTaskId());
                while (rs.next()) {
                    workgroups.add(rs.getString("group_name"));
                }
            }
            return workgroups;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
    }


    public List<Long> findTaskInstance(Long taskId, String masterRequestId)
            throws DataAccessException {
        try {
            db.openConnection();
            String query = "select ti.TASK_INSTANCE_ID" +
                " from TASK_INSTANCE ti, PROCESS_INSTANCE pi" +
                " where ti.TASK_INSTANCE_OWNER_ID = pi.PROCESS_INSTANCE_ID and" +
                "   pi.MASTER_REQUEST_ID = ? and" +
                "   ti.TASK_ID = ?" +
                " order by ti.TASK_INSTANCE_ID desc";
            Object[] args = new Object[2];
            args[0] = masterRequestId;
            args[1] = taskId;
            ResultSet rs = db.runSelect(query, args);
            List<Long> ret = new ArrayList<Long>();
            while (rs.next()) {
                ret.add(rs.getLong(1));
            }
            return ret;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to find task instance", e);
        } finally {
            db.closeConnection();
        }
    }

    public ProcessInstanceVO getProcessInstanceAll(Long procInstId)
        throws DataAccessException {
        try {
            db.openConnection();
            ProcessInstanceVO procInstInfo = this.getProcessInstanceBase0(procInstId);
            List<ActivityInstanceVO> actInstList = new ArrayList<ActivityInstanceVO>();
            String query = "select ACTIVITY_INSTANCE_ID,STATUS_CD,START_DT,END_DT,STATUS_MESSAGE,ACTIVITY_ID" +
                " from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID=?" +
                " order by ACTIVITY_INSTANCE_ID desc";
            ResultSet rs = db.runSelect(query, procInstId);
            ActivityInstanceVO actInst;
            while (rs.next()) {
                actInst = new ActivityInstanceVO();
                actInst.setId(new Long(rs.getLong(1)));
                actInst.setStatusCode(rs.getInt(2));
                actInst.setStartDate(StringHelper.dateToString(rs.getTimestamp(3)));
                actInst.setEndDate(StringHelper.dateToString(rs.getTimestamp(4)));
                actInst.setStatusMessage(rs.getString(5));
                actInst.setDefinitionId(new Long(rs.getLong(6)));
                actInstList.add(actInst);
            }
            procInstInfo.setActivities(actInstList);
            List<WorkTransitionInstanceVO> workTransInstanceList
                = new ArrayList<WorkTransitionInstanceVO>();
            query = "select WORK_TRANS_INST_ID,STATUS_CD,START_DT,END_DT,WORK_TRANS_ID" +
                " from WORK_TRANSITION_INSTANCE" +
                " where PROCESS_INST_ID=? order by WORK_TRANS_INST_ID desc";
            rs = db.runSelect(query, procInstId);
            WorkTransitionInstanceVO workTransInstance;
            while (rs.next()) {
                workTransInstance = new WorkTransitionInstanceVO();
                workTransInstance.setTransitionInstanceID(new Long(rs.getLong(1)));
                workTransInstance.setProcessInstanceID(procInstId);
                workTransInstance.setStatusCode(rs.getInt(2));
                workTransInstance.setStartDate(StringHelper.dateToString(rs.getTimestamp(3)));
                workTransInstance.setEndDate(StringHelper.dateToString(rs.getTimestamp(4)));
                workTransInstance.setTransitionID(new Long(rs.getLong(5)));
                workTransInstanceList.add(workTransInstance);
            }
            procInstInfo.setTransitions(workTransInstanceList);
            query = "select PROCESS_INSTANCE_ID, PROCESS_ID, STATUS_CD, START_DT, " +
                " END_DT, SECONDARY_OWNER " +
                " from PROCESS_INSTANCE where OWNER_ID = ?" +
                " order by PROCESS_INSTANCE_ID desc";
            rs = db.runSelect(query, procInstId);
            while (rs.next()) {
                String secondaryOwner = rs.getString(6);
                if (OwnerType.ACTIVITY_INSTANCE.equals(secondaryOwner)) continue;
                // MDW 3 subprocesses
                actInst = new ActivityInstanceVO();
                actInst.setId(new Long(rs.getLong(1)));
                actInst.setDefinitionId(new Long(rs.getLong(2)));
                actInst.setStatusCode(rs.getInt(3));
                actInst.setStartDate(StringHelper.dateToString(rs.getTimestamp(4)));
                actInst.setEndDate(StringHelper.dateToString(rs.getTimestamp(5)));
                actInst.setStatusMessage(OwnerType.PROCESS);
                actInstList.add(actInst);
            }
            List<VariableInstanceInfo> variableDataList = new ArrayList<VariableInstanceInfo>();
            query = "select vi.VARIABLE_INST_ID, vi.VARIABLE_ID, vi.VARIABLE_VALUE, v.VARIABLE_NAME," +
                    "  vt.VARIABLE_TYPE_NAME" +
                    " from VARIABLE_INSTANCE vi, VARIABLE v, VARIABLE_TYPE vt" +
                    " where vi.PROCESS_INST_ID = ? and vi.VARIABLE_ID = v.VARIABLE_ID" +
                    "   and v.VARIABLE_TYPE_ID = vt.VARIABLE_TYPE_ID" +
                    " order by v.VARIABLE_NAME";
            rs = db.runSelect(query, procInstId);
            while (rs.next()) {
                VariableInstanceInfo data = new VariableInstanceInfo();
                data.setInstanceId(new Long(rs.getLong(1)));
                data.setVariableId(new Long(rs.getLong(2)));
                data.setStringValue(rs.getString(3));
                data.setName(rs.getString(4));
                data.setType(rs.getString(5));
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
        if (getSupportedVersion()<DataAccess.schemaVersion52) {
        	query = "delete from SLA_INSTANCE where SLA_INST_OWNER='"+
        		OwnerType.PROCESS_INSTANCE + "' and SLA_INST_OWNER_ID=?";
        	n = db.runUpdate(query, processInstanceId);
        	count += n;
        }

        if (getSupportedVersion()>=DataAccess.schemaVersion51) {
        	query = "delete from TASK_INST_INDEX where TASK_INSTANCE_ID in " +
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
		}

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

        if (getSupportedVersion()<DataAccess.schemaVersion52) {
        	query = "delete from SLA_INSTANCE where SLA_INST_OWNER='"+
        		OwnerType.TASK_INSTANCE + "' and SLA_INST_OWNER_ID in " +
                " (select TASK_INSTANCE_ID from TASK_INSTANCE " +
                "  where TASK_INSTANCE_OWNER='" + OwnerType.PROCESS_INSTANCE +
                "'   and TASK_INSTANCE_OWNER_ID=?)";
        	n = db.runUpdate(query, processInstanceId);
        	count += n;
        }
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
            String query;
            ResultSet rs;
            if (getSupportedVersion()<DataAccess.schemaVersion52) {
                // included embedded process below
            	query = "select PROCESS_INSTANCE_ID from PROCESS_INSTANCE where PROCESS_ID=?" +
                    " or PROCESS_ID in (select p.PROCESS_ID from PROCESS p, WORK_TRANSITION t " +
                    "where p.PROCESS_ID=t.TO_WORK_ID and t.PROCESS_ID=?)";
                Object[] args = new Object[2];
                args[0] = processId;
                args[1] = processId;
                rs = db.runSelect(query, args);
            } else {
            	query = "select PROCESS_INSTANCE_ID from PROCESS_INSTANCE where PROCESS_ID=?";
            	rs = db.runSelect(query, processId);
            }
            List<String> procInstIdList = new ArrayList<String>();
            while (rs.next()) {
                procInstIdList.add(rs.getString(1));
            }
            for (String procInstId : procInstIdList) {
                this.deleteOneProcessInstance(new Long(procInstId));
                db.commit();	// commit for each deletion
            }
//            db.commit();
            return procInstIdList.size();
        } catch(Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to delete process instance", e);
        } finally {
            db.closeConnection();
        }
    }

    public TaskInstanceVO getTaskInstance(Long taskInstId) throws DataAccessException {
    	try {
            db.openConnection();
            String query = "select TASK_ID,TASK_INSTANCE_STATUS," +
	            "TASK_CLAIM_USER_ID,TASK_START_DT,TASK_END_DT,TASK_INSTANCE_STATE," +
	            "COMMENTS,OWNER_APP_NAME,ASSOCIATED_TASK_INST_ID," +
	            "TASK_INSTANCE_OWNER,TASK_INSTANCE_OWNER_ID," +
	            "TASK_INST_SECONDARY_OWNER,TASK_INST_SECONDARY_OWNER_ID" +
	            " from TASK_INSTANCE" +
	            " where TASK_INSTANCE_ID=?";
	        ResultSet rs = db.runSelect(query, taskInstId);
	        if (rs.next()) {
	            Long taskId = new Long(rs.getLong(1));
	            String pTaskName = null;
	            String pOrderId = null;
	            Date pStartDate = rs.getTimestamp(4);
	            Date pEndDate = rs.getTimestamp(5);
	            Date pDueDate = null;
	            Integer pStatusCd = new Integer(rs.getInt(2));
	            Integer pStateCd = new Integer(rs.getInt(6));
	            String pComments = rs.getString(7);
	            String pClaimUserCuid = null;
	            String pTaskMessage = null;
	            String pActivityName = null;
	            String pCategoryCd = null;
	            String pOwnerAppName = rs.getString(8);
	            Long pAssTaskInstId = new Long(rs.getLong(9));
	            TaskInstanceVO vo = new TaskInstanceVO(taskInstId, taskId, pTaskName, pOrderId,
	                    pStartDate, pEndDate, pDueDate, pStatusCd, pStateCd,
	                    pComments, pClaimUserCuid, pTaskMessage, pActivityName,
	                    pCategoryCd, pOwnerAppName, pAssTaskInstId);
	            vo.setOwnerType(rs.getString(10));
	            vo.setOwnerId(new Long(rs.getLong(11)));
	            vo.setSecondaryOwnerType(rs.getString(12));
	            vo.setSecondaryOwnerId(new Long(rs.getLong(13)));
	            return vo;
	        } else return null;
    	} catch(Exception e) {
    		db.rollback();
    		throw new DataAccessException(0, "failed to delete process instance", e);
    	} finally {
    		db.closeConnection();
    	}
    }

    private boolean isEmbeddedProcessInstance(ProcessInstanceVO pi) throws SQLException {
        String query = "select ATTRIBUTE_VALUE from ATTRIBUTE " +
            "where ATTRIBUTE_OWNER=? and ATTRIBUTE_OWNER_ID=? " +
            "and ATTRIBUTE_NAME=?";
        Object[] args = new Object[3];
        args[0] = OwnerType.PROCESS;
        args[1] = pi.getProcessId();
        args[2] = WorkAttributeConstant.PROCESS_VISIBILITY;
        ResultSet rs = db.runSelect(query, args);
        if (rs.next()) {
            String visibility = rs.getString(1);
            return visibility.equals(ProcessVisibilityConstant.EMBEDDED);
        } else return false;
    }

    public ProcessInstanceVO getCauseForTaskInstance(Long pTaskInstanceId)
            throws DataAccessException {
        try {
            TaskInstanceVO ti = getTaskInstance(pTaskInstanceId);
            // getTaskInstance open/close connection as well
            db.openConnection();
            if (ti == null) return null;
            ProcessInstanceVO pi = this.getProcessInstanceBase0(ti.getOwnerId());
            if (isEmbeddedProcessInstance(pi)) {
                pi = this.getProcessInstanceBase0(new Long(pi.getOwnerId()));
            }
            return pi;
        } catch(Exception e) {
            throw new DataAccessException(0, "failed to get cause for task instance", e);
        } finally {
            db.closeConnection();
        }
    }

    public DocumentVO getDocument(Long documentId) throws DataAccessException
    {
        try {
            db.openConnection();
            return super.getDocument(documentId, false);
        } catch(Exception e) {
            throw new DataAccessException(0, "failed to get document content", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<DocumentVO> findDocuments(Long procInstId, String type, String searchKey1, String searchKey2,
        String ownerType, Long ownerId, Date createDateStart, Date createDateEnd, String orderByClause) throws DataAccessException {
        try {
            db.openConnection();
            return findDocuments0(procInstId, type, searchKey1, searchKey2,
                    ownerType, ownerId, createDateStart, createDateEnd, orderByClause);
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to retrieve document VOs", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public void updateVariableInstance(VariableInstanceInfo var) throws DataAccessException {
        try {
        	db.openConnection();
        	String query = "update VARIABLE_INSTANCE set VARIABLE_VALUE=?, MOD_DT=" + now() + " where VARIABLE_INST_ID=?";
        	Object[] args = new Object[2];
        	args[0] = var.getStringValue();
        	args[1] = var.getInstanceId();
	        db.runUpdate(query, args);
	        db.commit();
	    } catch(Exception e) {
	        db.rollback();
	        throw new DataAccessException(0, "failed to update variable instance", e);
	    } finally {
	        db.closeConnection();
	    }
    }

    public void updateDocumentContent(Long documentId, String content) throws DataAccessException {
        try {
            db.openConnection();
            String query = "update DOCUMENT set CONTENT=?, MODIFY_DT=" + now() + " where DOCUMENT_ID=?";
            Object[] args = new Object[2];
            args[0] = content;
            args[1] = documentId;
            db.runUpdate(query, args);
            db.commit();
        } catch(Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to update document content", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<EventLog> getEventLogs(String pEventName, String pEventSource,
    	    String pEventOwner, Long pEventOwnerId) throws DataAccessException {
    	try {
            db.openConnection();
            StringBuffer query = new StringBuffer();
            query.append("select EVENT_LOG_ID, EVENT_NAME, EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID,");
            query.append("  EVENT_SOURCE, CREATE_DT, EVENT_CATEGORY, EVENT_SUB_CATEGORY,");
        	query.append("  COMMENTS, CREATE_USR ");
        	query.append("  FROM EVENT_LOG ");
            query.append("where ");
            Vector<Object> args = new Vector<Object>();
            if (pEventName!=null) {
            	if (args.size()>0) query.append(" and ");
            	query.append("EVENT_NAME=?");
            	args.add(pEventName);
            }
            if (pEventSource!=null) {
            	if (args.size()>0) query.append(" and ");
            	query.append("EVENT_SOURCE=?");
            	args.add(pEventSource);
            }
            if (pEventOwner!=null) {
            	if (args.size()>0) query.append(" and ");
            	query.append("EVENT_LOG_OWNER=?");
            	args.add(pEventOwner);
            }
            if (pEventOwnerId!=null) {
            	if (args.size()>0) query.append(" and ");
            	query.append("EVENT_LOG_OWNER_ID=?");
            	args.add(pEventOwnerId);
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

    public List<TaskActionVO> getUserTaskActions(String[] groups, Date startDate) throws DataAccessException {

        boolean compatible = getSupportedVersion() < DataAccess.schemaVersion52;
        boolean post51 = getDatabaseVersion() >= DataAccess.schemaVersion51;

        StringBuffer grps = new StringBuffer("(");
        for (int i = 0; i < groups.length; i++) {
            grps.append("'" + groups[i].replaceAll("'", "''") + "'");
            if (i < groups.length - 1)
              grps.append(",");
        }
        grps.append(")");

        StringBuffer sql = new StringBuffer("select sysdate, el.*\n"
            + "from event_log el, task_instance ti" + (post51 ? ", task_inst_grp_mapp tigm\n" :"\n")
            + "where el.create_dt > ?\n"
            + "and el.event_log_owner = 'TaskInstance'\n"
            + "and el.event_log_owner_id = ti.task_instance_id\n");
        if (post51)
            sql.append("and tigm.task_instance_id = ti.task_instance_id\n");
        sql.append("and (\n");
        if (compatible) {
            sql.append("(ti.task_id in\n"
              + "  (select distinct t.task_id\n"
              + "   from task t, task_usr_grp_mapp tugm, user_group ug\n"
              + "   where t.task_id = tugm.task_id\n"
              + "   and ug.user_group_id = tugm.user_group_id\n"
              + "   and ug.group_name in " + grps + "))\n");
            if (post51) {
                sql.append("or ");
            }
        }
        if (post51) {
            sql.append("(ti.task_instance_id = tigm.task_instance_id\n"
              + "and tigm.user_group_id in\n"
              + "  (select ug.user_group_id from user_group ug\n"
              + "   where ug.group_name in " + grps + "))\n");
        }
        sql.append("\n)\n");
        sql.append("order by el.event_log_id, el.event_name, el.comments\n");

        try {
            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString(), new Object[] { startDate });
            List<TaskActionVO> ret = new ArrayList<TaskActionVO>();
            while (rs.next()) {
                TaskActionVO taVO = new TaskActionVO();
                taVO.setId(rs.getLong("event_log_id"));
                String action = rs.getString("event_name");
                taVO.setAction(UserActionVO.getAction(action));
                if (taVO.getAction() == UserActionVO.Action.Other)
                    taVO.setExtendedAction(action);
                taVO.setAction(UserActionVO.getAction(rs.getString("event_name")));
                taVO.setEntity(UserActionVO.getEntity(rs.getString("event_log_owner")));
                taVO.setSource(rs.getString("event_source"));
                taVO.setEntityId(rs.getLong("event_log_owner_id"));
                taVO.setUser(rs.getString("create_usr"));
                taVO.setDate(rs.getTimestamp("create_dt"));
                taVO.setDescription(rs.getString("comments"));
                taVO.setRetrieveDate(rs.getTimestamp("sysdate"));
                if (Action.Assign == taVO.getAction()) {
                    taVO.setDestination(rs.getString("mod_usr"));
                }
                ret.add(taVO);
            }
            return ret;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public LinkedProcessInstance getProcessInstanceCallHierarchy(Long processInstanceId) throws DataAccessException {
        throw new UnsupportedOperationException("Not supported for MDW 4.x");
    }

    public ActivityList getActivityInstanceList(Query query) throws DataAccessException {
        throw new UnsupportedOperationException("Only supported for VCS Assets");
    }
}
