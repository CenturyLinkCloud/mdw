/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.version5;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.version4.RuntimeDataAccessV4;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;

public class RuntimeDataAccessV5 extends RuntimeDataAccessV4 {

    private List<VariableTypeVO> variableTypes;

    public RuntimeDataAccessV5(DatabaseAccess db, int databaseVersion, int supportedVersion) {
        super(db, databaseVersion, supportedVersion);
    }

    public RuntimeDataAccessV5(DatabaseAccess db, int databaseVersion, int supportedVersion, List<VariableTypeVO> variableTypes) {
        this(db, databaseVersion, supportedVersion);
        this.variableTypes = variableTypes;
    }

    protected String getVariableType(Long id) {
        if (variableTypes == null) {
            return VariableTypeCache.getTypeName(id);
        }
        else {
            for (VariableTypeVO variableType : variableTypes) {
                if (variableType.getVariableTypeId().longValue() == id.longValue())
                    return variableType.getVariableType();
            }
            // If didn't find the type, look in cache
            return VariableTypeCache.getTypeName(id);
        }
    }

    /**
     * return the external message associated with the activity
     * instance. The activity must be either an adapter/event-wait or a start
     * activity. When eventInstId is not null, this is a start activity.
     * When it is null, then this is a adaptor activity, and the activity ID
     * and activity instance ID must be present.
     *
     * Different from V4: instead of getting data from EXTERNAL_EVENT_INSTANCE, DOCUMENT
     * and ADAPTER_INSTANCE table, get only from DOCUMENT table
     */
    @Override
    public ExternalMessageVO getExternalMessage(Long activityId, Long activityInstId,
            Long eventInstId) throws DataAccessException {
        try {
            db.openConnection();
            String query, request=null, response=null;
            if (eventInstId==null) {    // adapter/event-wait activity
                query =  "select CONTENT from DOCUMENT where OWNER_TYPE='"
                    + OwnerType.ADAPTOR_REQUEST
                    + "' and OWNER_ID=?";
                ResultSet rs = db.runSelect(query, activityInstId);
                if (rs.next()) request = rs.getString(1);
                query =  "select CONTENT from DOCUMENT where OWNER_TYPE='"
                    + OwnerType.ADAPTOR_RESPONSE
                    + "' and OWNER_ID=?";
                rs = db.runSelect(query, activityInstId);
                if (rs.next()) response = rs.getString(1);

            } else {        // start activity
                query = "select CONTENT from DOCUMENT where OWNER_TYPE='"
                    + OwnerType.LISTENER_REQUEST
                    + "' and DOCUMENT_ID=?";
                ResultSet rs = db.runSelect(query, eventInstId);
                if (rs.next()) request = rs.getString(1);
                query = "select CONTENT from DOCUMENT where OWNER_TYPE='"
                    + OwnerType.LISTENER_RESPONSE
                    + "' and OWNER_ID=?";
                rs = db.runSelect(query, eventInstId);
                if (rs.next()) response = rs.getString(1);
            }
            return new ExternalMessageVO(request, response);
        } catch (Exception e) {
            throw new DataAccessException(0,"error to load external message", e);
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Difference from V4: added COMPCODE field
     */
    @Override
    public ProcessInstanceVO getProcessInstanceAll(Long procInstId)
    throws DataAccessException {
	    try {
	        db.openConnection();
	        ProcessInstanceVO procInstInfo = this.getProcessInstanceBase0(procInstId);
	        List<ActivityInstanceVO> actInstList = new ArrayList<ActivityInstanceVO>();
	        String query = "select ACTIVITY_INSTANCE_ID,STATUS_CD,START_DT,END_DT," +
	        	"    STATUS_MESSAGE,ACTIVITY_ID,COMPCODE" +
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
	            actInst.setCompletionCode(rs.getString(7));
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
	        List<VariableInstanceInfo> variableDataList = new ArrayList<VariableInstanceInfo>();
	        if (this.getDatabaseVersion()>=DataAccess.schemaVersion52) {
	        	query = "select VARIABLE_INST_ID, VARIABLE_ID, VARIABLE_VALUE, VARIABLE_NAME, VARIABLE_TYPE_ID " +
	    			"from VARIABLE_INSTANCE where PROCESS_INST_ID=? order by lower(VARIABLE_NAME)";
	        } else {
	        	query = "select vi.VARIABLE_INST_ID, vi.VARIABLE_ID, vi.VARIABLE_VALUE, v.VARIABLE_NAME," +
	                "  vt.VARIABLE_TYPE_NAME" +
	                " from VARIABLE_INSTANCE vi, VARIABLE v, VARIABLE_TYPE vt" +
	                " where vi.PROCESS_INST_ID = ? and vi.VARIABLE_ID = v.VARIABLE_ID" +
	                "   and v.VARIABLE_TYPE_ID = vt.VARIABLE_TYPE_ID" +
	                " order by v.VARIABLE_NAME";
	        }
	        rs = db.runSelect(query, procInstId);
	        boolean hasOldVariableInstances = false;
	        while (rs.next()) {
	            VariableInstanceInfo data = new VariableInstanceInfo();
	            data.setInstanceId(new Long(rs.getLong(1)));
	            data.setVariableId(new Long(rs.getLong(2)));
	            data.setStringValue(rs.getString(3));
	            data.setName(rs.getString(4));
	            if (data.getName()==null) hasOldVariableInstances = true;
		        if (this.getDatabaseVersion()>=DataAccess.schemaVersion52) {
		        	data.setType(getVariableType(rs.getLong(5)));
		        } else {
		        	data.setType(rs.getString(5));
		        }
	            variableDataList.add(data);
	        }
	        if (hasOldVariableInstances&&getDatabaseVersion()>=DataAccess.schemaVersion52
	        			&& this.getSupportedVersion()<DataAccess.schemaVersion52) {
	        	// backward compatibility code to load variabe names for variable instances created earlier
	        	query = "select vi.VARIABLE_INST_ID, v.VARIABLE_NAME, v.VARIABLE_TYPE_ID " +
	        		" from VARIABLE_INSTANCE vi, VARIABLE v " +
                    " where vi.PROCESS_INST_ID = ? and vi.VARIABLE_ID = v.VARIABLE_ID";
	        	rs = db.runSelect(query, procInstId);
	        	while (rs.next()) {
	        		String varname = rs.getString(2);
	        		Long varinstId = rs.getLong(1);
	        		for (VariableInstanceInfo vi : variableDataList) {
	        			if (vi.getInstanceId().equals(varinstId)) {
	        				if (vi.getName()==null) vi.setName(varname);
	        				if (vi.getType()== null) vi.setType(getVariableType(rs.getLong(3)));
	        				break;
	        			}
	        		}
	        	}
	        }
	        procInstInfo.setVariables(variableDataList);
	        return procInstInfo;
	    } catch (Exception e) {
	        throw new DataAccessException(0,"failed to load process instance runtime info", e);
	    } finally {
	        db.closeConnection();
	    }
	}

    /**
     * Different from V4: add completion code field
     */
    @Override
    protected ProcessInstanceVO getProcessInstanceBase0(Long procInstId) throws SQLException, DataAccessException {
    	String query;
    	if (getSupportedVersion()>=DataAccess.schemaVersion52) {
    		query = "select pi.PROCESS_ID,pi.OWNER,pi.OWNER_ID,pi.MASTER_REQUEST_ID," +
            " pi.STATUS_CD,pi.START_DT,pi.END_DT,r.RULE_SET_NAME,pi.COMPCODE,pi.COMMENTS,r.VERSION_NO" +
            " from PROCESS_INSTANCE pi, RULE_SET r" +
            " where pi.PROCESS_INSTANCE_ID=? and pi.PROCESS_ID=r.RULE_SET_ID";
    	} else {
    		query = "select pi.PROCESS_ID,pi.OWNER,pi.OWNER_ID,pi.MASTER_REQUEST_ID," +
            	" pi.STATUS_CD,pi.START_DT,pi.END_DT,w.WORK_NAME,pi.COMPCODE,pi.COMMENTS" +
            	" from PROCESS_INSTANCE pi, WORK w" +
            	" where pi.PROCESS_INSTANCE_ID=? and pi.PROCESS_ID=w.WORK_ID";
    	}
        ResultSet rs = db.runSelect(query, procInstId);
        if (!rs.next()) throw new SQLException("failed to load process instance " + procInstId);
        ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong(1), rs.getString(8));
    	pi.setOwner(rs.getString(2));
    	pi.setOwnerId(rs.getLong(3));
    	pi.setMasterRequestId(rs.getString(4));
    	pi.setStatusCode(rs.getInt(5));
    	pi.setStartDate(StringHelper.dateToString(rs.getTimestamp(6)));
    	pi.setId(procInstId);
        pi.setCompletionCode(rs.getString(9));
        pi.setComment(rs.getString(10));
        pi.setEndDate(StringHelper.dateToString(rs.getTimestamp(7)));
        if (getSupportedVersion()>=DataAccess.schemaVersion52)
            pi.setProcessVersion(RuleSetVO.formatVersion(rs.getInt("VERSION_NO")));
        return pi;
    }

    /**
     * Difference from V4: uses RULE_SET table instead of WORK for process name
     */
    @Override
    protected String buildQuery(Map<String,String> pMap, int startIndex, int endIndex, String orderBy) {
    	if (getSupportedVersion()<DataAccess.schemaVersion52)
    		return super.buildQuery(pMap, startIndex, endIndex, orderBy);
        StringBuffer sqlBuff = new StringBuffer();
        if (startIndex!=QueryRequest.ALL_ROWS) sqlBuff.append(db.pagingQueryPrefix());
        sqlBuff.append("SELECT ");
        if (startIndex!=QueryRequest.ALL_ROWS) sqlBuff.append("/*+ NO_USE_NL(pi r) */ ");
        sqlBuff.append("pi.PROCESS_INSTANCE_ID,pi.MASTER_REQUEST_ID,pi.STATUS_CD,pi.START_DT,");
        sqlBuff.append("pi.END_DT,pi.OWNER,pi.OWNER_ID,pi.PROCESS_ID,r.RULE_SET_NAME,pi.COMMENTS ");
        sqlBuff.append("FROM process_instance pi, rule_set r ");
        sqlBuff.append("WHERE pi.PROCESS_ID=r.RULE_SET_ID ");
        if (!OwnerType.MAIN_PROCESS_INSTANCE.equals(pMap.get("owner"))) {
        	sqlBuff.append(" and pi.OWNER!='" + OwnerType.MAIN_PROCESS_INSTANCE +"' ");
        }
        buildQueryCommon(sqlBuff, pMap, orderBy);
        if (startIndex!=QueryRequest.ALL_ROWS) sqlBuff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        return sqlBuff.toString();
    }

    @Override
    protected String buildQuery(Map<String,String> criteria, List<String> variables, Map<String,String> variableCriteria, int startIndex, int endIndex, String orderBy) {
    	if (getSupportedVersion()<DataAccess.schemaVersion52)
    		return super.buildQuery(criteria, variables, variableCriteria, startIndex, endIndex, orderBy);
    	StringBuffer sqlBuff = new StringBuffer();
        if (startIndex!=QueryRequest.ALL_ROWS) sqlBuff.append(db.pagingQueryPrefix());
        sqlBuff.append("SELECT pis.PROCESS_INSTANCE_ID, pis.MASTER_REQUEST_ID, pis.STATUS_CD, pis.START_DT, pis.END_DT, pis.OWNER, pis.OWNER_ID, pis.PROCESS_ID, pis.PROCESS_NAME, pis.COMMENTS");
        if (variables != null && variables.size() > 0) {
        	for (String varName : variables) {
        		sqlBuff.append(", ").append(varName.startsWith("DATE:") ? varName.substring(5) : varName);
        	}
        }
        sqlBuff.append("\n    FROM (\n");
        sqlBuff.append("  SELECT pi.*, r.rule_set_name as process_name");
        sqlBuff.append(buildVariablesSelect(variables));
        sqlBuff.append(buildVariablesClause(criteria, variables, variableCriteria));
        sqlBuff.append(") pis\n");
        if (orderBy != null)
        	sqlBuff.append("\n").append(orderBy);
        if (startIndex!=QueryRequest.ALL_ROWS)
        	sqlBuff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        return sqlBuff.toString();
    }

    @Override
    protected String buildProcessNameClause(String processName) {
        return " AND pi.PROCESS_ID in (select RULE_SET_ID from RULE_SET where RULE_SET_NAME = '" + processName + "')";
    }

    @Override
    public String buildVariablesSelect(List<String> variables) {
    	if (getSupportedVersion()<DataAccess.schemaVersion52)
    		return super.buildVariablesSelect(variables);
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

    @Override
    protected String buildVariablesClause(Map<String,String> criteria, List<String> variables, Map<String,String> variableCriteria) {
    	if (getSupportedVersion()<DataAccess.schemaVersion52)
    		return super.buildVariablesClause(criteria, variables, variableCriteria);
    	StringBuffer sqlBuff = new StringBuffer();
        if (variableCriteria != null && !variableCriteria.isEmpty()) {
        	int i = variableCriteria.keySet().size();
            for (String varName : variableCriteria.keySet()) {
                sqlBuff.append(" \nFROM process_instance pi, variable_instance vi, rule_set r\n");
                sqlBuff.append("  WHERE pi.process_instance_id = vi.process_inst_id\n");
                sqlBuff.append("  AND pi.process_id = r.rule_set_id\n");
                buildQueryCommon(sqlBuff, criteria, null);
                String varValue = variableCriteria.get(varName);
                boolean isDate = varName.startsWith("DATE:");
                Long variableTypeId = null;
                if (isDate) {
                    varName = varName.substring(5);
                    variableTypeId = VariableTypeCache.getTypeId("java.util.Date");
                }
                sqlBuff.append(" and vi.VARIABLE_NAME = '" + varName + "' ");
                if (isDate && variableTypeId != null) {
                    sqlBuff.append(" and vi.VARIABLE_TYPE_ID = "+variableTypeId); // date var type
                    // inline view to avoid parse errors (TODO better way?)
                    if (db.isMySQL())
                        sqlBuff.append("\n and (select str_to_date(concat(substr(ivi.VARIABLE_VALUE, 5, 7), substr(ivi.VARIABLE_VALUE, 25)), '%M %D %Y')");
                    else
                        sqlBuff.append("\n and (select to_date(substr(ivi.VARIABLE_VALUE, 5, 7) || substr(ivi.VARIABLE_VALUE, 25), 'MON DD YYYY')");
                    sqlBuff.append("\n     from variable_instance ivi  where ivi.variable_type_id = "+variableTypeId);
                    sqlBuff.append("\n     and ivi.variable_inst_id = vi.variable_inst_id");
                    if (db.isMySQL()) varValue = dateConditionToMySQL(varValue);
                    sqlBuff.append("\n     and ivi.variable_name = '" + varName + "') "+ varValue + " ");
                }
                else {
                    sqlBuff.append(" and vi.VARIABLE_VALUE " + varValue + " ");
                }
                if (--i > 0)
                  sqlBuff.append("\nintersect\n").append("  SELECT pi.*, r.rule_set_name as process_name\n").append(buildVariablesSelect(variables));
            }
        } else {
            sqlBuff.append("\n  FROM process_instance pi, rule_set r\n");
            sqlBuff.append("WHERE pi.PROCESS_ID = r.rule_set_id ");
            buildQueryCommon(sqlBuff, criteria, null);
        }
        return sqlBuff.toString();
    }

    public LinkedProcessInstance getProcessInstanceCallHierarchy(Long processInstanceId) throws DataAccessException {
        try {
            db.openConnection();
            ProcessInstanceVO startingInstance = getProcessInstanceBase0(processInstanceId);
            LinkedProcessInstance startingLinked = new LinkedProcessInstance(startingInstance);
            LinkedProcessInstance top = startingLinked;
            // callers
            while (OwnerType.PROCESS_INSTANCE.equals(top.getProcessInstance().getOwner())) {
                ProcessInstanceVO caller = getProcessInstanceBase0(top.getProcessInstance().getOwnerId());
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
        ProcessInstanceVO callerProcInst = caller.getProcessInstance();
        List<ProcessInstanceVO> calledInsts = getProcessInstancesForOwner(OwnerType.PROCESS_INSTANCE, callerProcInst.getId());
        if (calledInsts != null) {
            for (ProcessInstanceVO calledInst : calledInsts) {
                LinkedProcessInstance child = new LinkedProcessInstance(calledInst);
                child.setParent(caller);
                caller.getChildren().add(child);
                addCalledHierarchy(child);
            }
        }
    }

    /**
     * Only for V5+
     */
    protected List<ProcessInstanceVO> getProcessInstancesForOwner(String ownerType, Long ownerId) throws SQLException, DataAccessException {
        List<ProcessInstanceVO> instanceList = null;
        String query;
        if (getSupportedVersion() >= DataAccess.schemaVersion52) {
            query = "select pi.PROCESS_INSTANCE_ID, pi.PROCESS_ID, pi.MASTER_REQUEST_ID," +
            " pi.STATUS_CD, pi.START_DT, pi.END_DT, r.RULE_SET_NAME as NAME, pi.COMPCODE, pi.COMMENTS, r.VERSION_NO" +
            " from PROCESS_INSTANCE pi, RULE_SET r" +
            " where pi.OWNER = '" + ownerType + "' and pi.OWNER_ID = ? and pi.PROCESS_ID = r.RULE_SET_ID";
        } else {
            query = "select pi.PROCESS_INSTANCE_ID, pi.PROCESS_ID, pi.MASTER_REQUEST_ID," +
                " pi.STATUS_CD, pi.START_DT, pi.END_DT, w.WORK_NAME as NAME, pi.COMPCODE, pi.COMMENTS, p.VERSION_NO" +
                " from PROCESS_INSTANCE pi, WORK w, PROCESS p" +
                " where pi.OWNER = '" + ownerType + "' and pi.OWNER_ID = ? and pi.PROCESS_ID = w.WORK_ID and p.PROCESS_ID = w.WORK_ID";
        }
        ResultSet rs = db.runSelect(query, ownerId);
        while (rs.next()) {
            if (instanceList == null)
                instanceList = new ArrayList<ProcessInstanceVO>();
            ProcessInstanceVO pi = new ProcessInstanceVO(rs.getLong("PROCESS_ID"), rs.getString("NAME"));
            pi.setId(rs.getLong("PROCESS_INSTANCE_ID"));
            pi.setProcessVersion(RuleSetVO.formatVersion(rs.getInt("VERSION_NO")));
            pi.setOwner(ownerType);
            pi.setOwnerId(ownerId);
            pi.setMasterRequestId(rs.getString("MASTER_REQUEST_ID"));
            pi.setStatusCode(rs.getInt("STATUS_CD"));
            pi.setStartDate(StringHelper.dateToString(rs.getTimestamp("START_DT")));
            pi.setEndDate(StringHelper.dateToString(rs.getTimestamp("END_DT")));
            pi.setCompletionCode(rs.getString("COMPCODE"));
            pi.setComment(rs.getString("COMMENTS"));
            instanceList.add(pi);
        }

        return instanceList;
    }
}
