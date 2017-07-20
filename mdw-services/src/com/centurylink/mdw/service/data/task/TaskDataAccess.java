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
package com.centurylink.mdw.service.data.task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.MiscConstants;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.monitor.ServiceLevelAgreement;
import com.centurylink.mdw.model.monitor.ServiceLevelAgreementInstance;
import com.centurylink.mdw.model.note.InstanceNote;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.util.CollectionUtil;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Task-related data access.
 */
public class TaskDataAccess extends CommonDataAccess {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static String TASK_INSTANCE_SELECT_SHALLOW =
        " ti.TASK_INSTANCE_ID," +
        " ti.TASK_ID," +
        " ti.TASK_INSTANCE_STATUS, " +
        " ti.TASK_INSTANCE_OWNER," +
        " ti.TASK_INSTANCE_OWNER_ID," +
        " ti.TASK_INST_SECONDARY_OWNER," +
        " ti.TASK_INST_SECONDARY_OWNER_ID," +
        " ti.TASK_CLAIM_USER_ID," +
        " ti.TASK_START_DT," +
        " ti.TASK_END_DT," +
        " ti.COMMENTS," +
        " ti.TASK_INSTANCE_STATE," +
        " ti.TASK_INSTANCE_REFERRED_AS, " +
        " ti.DUE_DATE," +
        " ti.PRIORITY," +
        " ti.MASTER_REQUEST_ID";

    private static String TASK_INSTANCE_SELECT =
        "distinct " + TASK_INSTANCE_SELECT_SHALLOW + ", " +
        " ti.TASK_INSTANCE_OWNER_ID as PROCESS_INSTANCE_ID," +
        " ui.CUID, ui.NAME as USER_NAME";

    private static String TASK_INSTANCE_FROM =
        "TASK_INSTANCE ti, USER_INFO ui ";

    private static boolean hasInstanceGroupMappings;

    public TaskDataAccess(DatabaseAccess db) {
        super(db, DataAccess.currentSchemaVersion, DataAccess.supportedSchemaVersion);
    }

    protected Long getNextId(String sequenceName) throws SQLException {
        String query = "select " + sequenceName + ".NEXTVAL from dual";
        ResultSet rs = db.runSelect(query, null);
        rs.next();
        return new Long(rs.getString(1));
    }

    /**
     * This is preserved for backward compatibility on early versions of MDW 4
     */
    public ServiceLevelAgreement getServiceLevelAgreement(Long taskId)
    throws DataAccessException {
        try {
            db.openConnection();
            String query = "select SLA_ID, SLA_HR, SLA_START_DT, SLA_END_DT from SLA sla " +
                "where sla.SLA_OWNER_ID = ? and sla.SLA_OWNER = '" + OwnerType.TASK + "'" +
                "   and (sla.SLA_START_DT is null or sysdate > sla.SLA_START_DT)" +
                "   and (sla.SLA_END_DT is null or sla.SLA_END_DT > sysdate )";
            ResultSet rs = db.runSelect(query, taskId);
            if (rs.next()) {
                ServiceLevelAgreement sla = new ServiceLevelAgreement();
                sla.setId(rs.getLong(1));
                sla.setSLAInHours(rs.getFloat(2));
                sla.setSlaStartDate(rs.getTimestamp(3));
                sla.setSlaEndDate(rs.getTimestamp(4));
                if (rs.next()) throw new SQLException("Non-unique SLA for task " + taskId);
                return sla;
            } else return null;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to get SLA for task", e);
        } finally {
            db.closeConnection();
        }
    }

    public ServiceLevelAgreementInstance getSLAInstance(Long taskId)
            throws DataAccessException {
        try {
            db.openConnection();
            String query = "select SLA_INSTANCE_ID,SLA_INST_OWNER,SLA_INST_OWNER_ID," +
                "  SLA_ESTM_COMP_DT, SLA_BRK_REPTD_IND " +
                "from SLA_INSTANCE where SLA_INST_OWNER='" +
                OwnerType.TASK_INSTANCE + "' and SLA_INST_OWNER_ID=?";
            ResultSet rs = db.runSelect(query, taskId);
            if (rs.next()) {
                ServiceLevelAgreementInstance slaInst = new ServiceLevelAgreementInstance();
                slaInst.setId(rs.getLong(1));
                slaInst.setOwnerType(rs.getString(2));
                slaInst.setOwnerId(rs.getLong(3));
                slaInst.setEstimatedCompletionDate(rs.getTimestamp(4));
                slaInst.setSLABreakReportedInd(rs.getInt(5));
                return slaInst;
            } else return null;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to get SLA instance", e);
        } finally {
            db.closeConnection();
        }
    }

    public void removeSLAInstance(Long taskId)
        throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from SLA_INSTANCE where SLA_INST_OWNER='" +
                OwnerType.TASK_INSTANCE + "' and SLA_INST_OWNER_ID=?";
            db.runUpdate(query, taskId);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to remove SLA instance", e);
        } finally {
            db.closeConnection();
        }
    }

    private TaskInstance getTaskInstanceSub(ResultSet rs, boolean isVOversion) throws SQLException {
        return getTaskInstanceSub(rs, isVOversion, null, null);
    }

    /**
     * Returns with state=Invalid for task instance exists whose definition no longer exists (can happen esp. in VCS assets
     * if previous task definitions were not archived properly).
     */
    private TaskInstance getTaskInstanceSub(ResultSet rs, boolean isVOversion, List<String> variables, List<String> indexNames) throws SQLException {

        TaskInstance task = new TaskInstance();
        task.setTaskInstanceId(rs.getLong("TASK_INSTANCE_ID"));
        task.setTaskId(rs.getLong("TASK_ID"));
        task.setStatusCode(rs.getInt("TASK_INSTANCE_STATUS"));
        task.setOwnerType(rs.getString("TASK_INSTANCE_OWNER"));
        task.setOwnerId(rs.getLong("TASK_INSTANCE_OWNER_ID"));
        task.setSecondaryOwnerType(rs.getString("TASK_INST_SECONDARY_OWNER"));
        task.setSecondaryOwnerId(rs.getLong("TASK_INST_SECONDARY_OWNER_ID"));
        task.setAssigneeId(rs.getLong("TASK_CLAIM_USER_ID"));
        task.setStartDate(StringHelper.dateToString(rs.getTimestamp("TASK_START_DT")));
        task.setEndDate(StringHelper.dateToString(rs.getTimestamp("TASK_END_DT")));
        task.setComments(rs.getString("COMMENTS"));
        task.setStateCode(rs.getInt("TASK_INSTANCE_STATE"));
        task.setDueDate(rs.getTimestamp("DUE_DATE"));
        task.setPriority(rs.getInt("PRIORITY"));
        task.setMasterRequestId(rs.getString("MASTER_REQUEST_ID"));
        TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(task.getTaskId());
        if (taskVO == null) {
            String ref = rs.getString("TASK_INSTANCE_REFERRED_AS");
            logger.warn("ERROR: Task instance ID " + task.getTaskInstanceId() + " missing task definition (" + ref + ").");
            task.setTaskName(ref);
            task.setInvalid(true);
            return task;
        }
        task.setCategoryCode(taskVO.getTaskCategory());
        task.setTaskName(taskVO.getTaskName());
        if (task.getTaskName() == null) {
            task.setTaskName(taskVO.getTaskName());
        }
        if (isVOversion) {
            task.setAssigneeCuid(rs.getString("CUID"));
            if (taskVO != null)
              task.setDescription(taskVO.getComment());
        }
        Map<String,Object> varMap = null;
        if (indexNames != null && indexNames.size() > 0) {
            varMap = new HashMap<String,Object>();
            for (String indexName : indexNames) {
                String value = rs.getString(indexName.toUpperCase());
                if (indexName.equals("MASTER_REQUEST_ID"))
                    task.setMasterRequestId(value);
                else
                    varMap.put(indexName, value);
            }
        }
        if (task.getVariables() == null && variables != null && variables.size() > 0) {
            if (varMap == null)
             varMap = new HashMap<String,Object>();
            for (String varName : variables) {
                String name = varName;
                if (varName.startsWith("DATE:"))
                    name = varName.substring(5);
                String varVal = rs.getString(name.toUpperCase());
                varMap.put(name, varVal);
            }
        }
        task.setVariables(varMap);
        return task;
    }

    public TaskInstance getTaskInstance(Long taskInstId) throws DataAccessException {
        try {
            db.openConnection();
            StringBuilder query = new StringBuilder();
            query.append("select ").append(TASK_INSTANCE_SELECT_SHALLOW).append(", GROUP_NAME\n");
            query.append("from TASK_INSTANCE ti\n");
            query.append("left join TASK_INST_GRP_MAPP tigm on ti.TASK_INSTANCE_ID = tigm.TASK_INSTANCE_ID\n");
            query.append("left join USER_GROUP ug on tigm.USER_GROUP_ID = ug.USER_GROUP_ID\n");
            query.append("where ti.TASK_INSTANCE_ID = ?");
            ResultSet rs = db.runSelect(query.toString(), taskInstId);
            TaskInstance ti = null;
            while (rs.next()) {
                if (ti == null)
                    ti = getTaskInstanceSub(rs, false);
                String groupName = rs.getString("GROUP_NAME");
                if (groupName != null && !groupName.isEmpty()) {
                    if (ti.getGroups() == null)
                        ti.setGroups(new ArrayList<String>());
                    ti.getGroups().add(groupName);
                }
            }
            return ti;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task instance", e);
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Returns shallow TaskInstanceVOs.
     */
    public List<TaskInstance> getSubTaskInstances(Long masterTaskInstId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "select " + TASK_INSTANCE_SELECT_SHALLOW +
                " from TASK_INSTANCE ti where TASK_INST_SECONDARY_OWNER_ID=?";
            ResultSet rs = db.runSelect(query, masterTaskInstId);
            List<TaskInstance> taskInsts = new ArrayList<TaskInstance>();
            while (rs.next()) {
                taskInsts.add(getTaskInstanceSub(rs, false));
            }
            return taskInsts;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task instances", e);
        } finally {
            db.closeConnection();
        }
    }

    // this is only used for classic task in local task manager
    public TaskInstance getTaskInstanceByActivityInstanceId(Long activityInstanceId)
        throws DataAccessException {
        try {
            db.openConnection();
            StringBuffer sql = new StringBuffer("select ");
            sql.append(TASK_INSTANCE_SELECT_SHALLOW);
            sql.append(" from TASK_INSTANCE ti, WORK_TRANSITION_INSTANCE wti ");
            sql.append(" where ti.TASK_INST_SECONDARY_OWNER=?");
            sql.append("   and ti.TASK_INST_SECONDARY_OWNER_ID=wti.WORK_TRANS_INST_ID");
            sql.append("   and wti.DEST_INST_ID=?");
            Object[] args = new Object[2];
            args[0] = OwnerType.WORK_TRANSITION_INSTANCE;
            args[1] = activityInstanceId;
            ResultSet rs = db.runSelect(sql.toString(), args);
            if (rs.next()) {
                return getTaskInstanceSub(rs, false);
            } else return null;
        } catch (Exception e) {
             throw new DataAccessException(0, "failed to get task instance", e);
        } finally {
            db.closeConnection();
        }
    }

    public Long createTaskInstance(TaskInstance taskInst, Date dueDate)
        throws DataAccessException {
        try {
            db.openConnection();
            Long id = db.isMySQL()?null:this.getNextId("MDW_COMMON_INST_ID_SEQ");
            String query = "insert into TASK_INSTANCE " +
                "(TASK_INSTANCE_ID, TASK_ID, TASK_INSTANCE_STATUS, " +
                " TASK_INSTANCE_OWNER, TASK_INSTANCE_OWNER_ID, TASK_CLAIM_USER_ID, COMMENTS, " +
                " TASK_START_DT, TASK_END_DT, TASK_INSTANCE_STATE, " +
                " TASK_INST_SECONDARY_OWNER, TASK_INST_SECONDARY_OWNER_ID, " +
                " TASK_INSTANCE_REFERRED_AS, DUE_DATE, PRIORITY, MASTER_REQUEST_ID, " +
                " CREATE_DT, CREATE_USR) " +
                "values (?, ?, ?, ?, ?, ?, ?, " + now() + ", ?, ?, ?, ?, ?, ?, ?, ?, " + now() + ",'TaskManager')";
            Object[] args = new Object[15];
            args[0] = id;
            args[1] = taskInst.getTaskId();
            args[2] = taskInst.getStatusCode();
            args[3] = taskInst.getOwnerType();
            args[4] = taskInst.getOwnerId();
            args[5] = null;
            String comments = taskInst.getComments();
            if (comments != null && comments.length() > 1000)
                comments = comments.substring(0, 999);
            args[6] = comments;
            args[7] = null;
            args[8] = taskInst.getStateCode();
            args[9] = taskInst.getSecondaryOwnerType();
            args[10] = taskInst.getSecondaryOwnerId();
            args[11] = taskInst.getTaskName();
            args[12] = dueDate;
            args[13] = taskInst.getPriority() == null ? 0 : taskInst.getPriority();
            args[14] = taskInst.getMasterRequestId();
            if (db.isMySQL()) id = db.runInsertReturnId(query, args);
            else db.runUpdate(query, args);
            db.commit();
            return id;
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to create task instance", e);
        } finally {
            db.closeConnection();
        }
    }

    public void updateTaskInstance(Long taskInstId, Map<String,Object> changes, boolean setEndDate)
        throws DataAccessException {
        try {
            db.openConnection();
            StringBuffer sb = new StringBuffer();
            sb.append("update TASK_INSTANCE set ");
            Set<String> keys = changes.keySet();
            int n = keys.size();
            Object[] args = new Object[n+1];
            int i = 0;
            for (String key : keys) {
                if (i>0) sb.append(", ");
                sb.append(key).append("=?");
                args[i] = changes.get(key);
                i++;
            }
            if (setEndDate) sb.append(", TASK_END_DT="+now()+"");
            sb.append(" where TASK_INSTANCE_ID=?");
            args[n] = taskInstId;
            String query = sb.toString();
            db.runUpdate(query, args);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to update task instance: " + taskInstId, e);
        } finally {
            db.closeConnection();
        }
    }

    public void cancelTaskInstance(TaskInstance taskInst)
        throws DataAccessException {
        try {
            db.openConnection();
            String query = "update TASK_INSTANCE" +
                " set TASK_INSTANCE_STATE=?, TASK_INSTANCE_STATUS=?, TASK_END_DT="+now()+
                " where TASK_INSTANCE_ID=?";
            Object[] args = new Object[3];
            args[0] = taskInst.getStateCode();
            args[1] = taskInst.getStatusCode();
            args[2] = taskInst.getTaskInstanceId();
            db.runUpdate(query, args);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to cancel task instance: " + taskInst.getId(), e);
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Creates the where clause based on the passed in params
     * @param criteria
     * @param pPersistable Class Impl
     * @param pTableAlias
     */
    private String buildWhereClause(Map<String,String> criteria) throws DataAccessException {
        StringBuffer buff = new StringBuffer();
        if(criteria.isEmpty()){
            return "";
        }
        for (String key : criteria.keySet()) {
            String value = criteria.get(key);
            if (value==null) value = " is null ";
            if (key.equals("taskName") && value != null) {
                buff.append(" and substr(task_instance_referred_as, instr(task_instance_referred_as, '/') + 1) " + value);
            } else if (key.equals("categoryId") && value != null) {
                String catTasksClause = buildCategoryTasksClause(Integer.parseInt(value.replaceAll("'", "").replaceAll("=", "").trim()));
                if (catTasksClause != null)
                    buff.append(" and " + catTasksClause);
            } else if (key.equals("estimatedCompletionDate")) {
                if (db.isMySQL())
                    value = dateConditionToMySQL(value);
                buff.append(" and ti.due_date " + value);
            } else if (key.equals("orderId") || key.equals("masterRequestId")){
                buff.append(" and ti.master_request_id " + value);
            } else if (key.equals("id")) {
                buff.append(" and ti.task_instance_id " + value);
            } else if (key.equals("definitionId")) {
                buff.append(" and ti.task_id " + value);
            } else if (key.equals("statusCode")) {
                buff.append(" and ti.task_instance_status " + value);
            } else if (key.equals("stateCode")) {
                buff.append(" and ti.task_instance_state " + value);
            } else if (key.equals("owner")) {
                buff.append(" and ti.task_instance_owner " + value);
            } else if (key.equals("ownerId")) {
                buff.append(" and ti.task_instance_owner_id " + value);
            } else if (key.equals("secondaryOwner")) {
                buff.append(" and ti.task_inst_secondary_owner " + value);
            } else if (key.equals("secondaryOwnerId")) {
                buff.append(" and ti.task_inst_secondary_owner " + value);
            } else if (key.equals("taskClaimUserId")) {
                buff.append(" and ti.task_claim_user_id " + value);
            } else if (key.equals("startDate")) {
                if (db.isMySQL()) value = dateConditionToMySQL(value);
                buff.append(" and ti.task_start_dt " + value);
            } else if (key.equals("endDate")) {
                if (db.isMySQL()) value = dateConditionToMySQL(value);
                buff.append(" and ti.task_end_dt " + value);
            } else if (key.equals("comment")) {
                buff.append(" and ti.comments " + value);
            } else {
                buff.append(" and ti.").append(key).append(" ").append(value);
            }
        }
        return buff.toString();
    }

    private String buildVariablesWhereClause(Map<String,String> variablesCriteria) {
        StringBuffer buff = new StringBuffer();
        if (variablesCriteria != null && !variablesCriteria.isEmpty()) {
            int i = variablesCriteria.size();
            buff.append("\nand ti.task_instance_owner_id in (");
            for (String varName : variablesCriteria.keySet()) {
                buff.append("select pi.process_instance_id ");
                buff.append("from process_instance pi, variable_instance vi ");
                buff.append("where pi.process_instance_id = vi.process_inst_id ");
                String varValue = variablesCriteria.get(varName);
                boolean isDate = varName.startsWith("DATE:");
                if (isDate) {
                    varName = varName.substring(5);
                }
                buff.append("and vi.variable_name = '" + varName + "' ");
                if (isDate) {
                    buff.append("and v.variable_type_id = 5 "); // date var type
                    // inline view to avoid sql parse errors
                    buff.append("\n and (select to_date(substr(ivi.VARIABLE_VALUE, 5, 7) || substr(ivi.VARIABLE_VALUE, 25), 'MON DD YYYY')"
                      + " from variable iv, variable_instance ivi"
                      + " where iv.variable_type_id = 5"
                      + " and ivi.variable_id = iv.variable_id"
                      + " and ivi.variable_inst_id = vi.variable_inst_id"
                      + " and iv.variable_name = '" + varName + "') " + varValue + " ");
                }
                else {
                    buff.append("and vi.variable_value " + varValue + " ");
                }
                if (--i > 0)
                  buff.append("\nintersect\n");
            }
            buff.append(")\n");
        }
        return buff.toString();
    }

    private String buildIndexCriteriaWhereClause(Map<String,String> indexCriteria) {
        StringBuffer buff = new StringBuffer();
        if (indexCriteria != null && !indexCriteria.isEmpty()) {
            for (String keyName : indexCriteria.keySet()) {
                buff.append("\nand tidx.index_key = '").append(keyName).append("' ");
                buff.append("and tidx.index_value ").append(indexCriteria.get(keyName));
            }
            buff.append("\n");
        }
        return buff.toString();
    }

    /**
     * Creates the order by clause based on the passed in column
     * @param orderBy
     * @param ascending
     * @param tableAlias
     */
    private String buildOrderByClause(String orderBy, boolean ascending, List<String> variables) throws DataAccessException{
        if (orderBy == null || orderBy.length() == 0)
            return "";

        StringBuffer buff = new StringBuffer();
        buff.append(" order by ");

        if (orderBy.equals("TASK_CATEGORY_CD")) {
            buff.append("cat.category");
        }
        else if (orderBy.equals("SLA_ESTM_COMP_DT")) {
            buff.append("DUE_DATE");
        }
        else if (orderBy.equals("TASK_INSTANCE_STATE")) {
            if (db.isOracle()) {
                buff.append("decode(ti.task_instance_state");
                for (TaskState state : getAllTaskStates()) {
                    if (state.getId() != 1 && state.getId() != 4)
                        buff.append(", " + state.getId() + ", '" + state.getDescription() + "'");
                }

                buff.append(", NULL)");
            }
            else {  // mySQL
                buff.append("case ");
                for (TaskState state : getAllTaskStates())
                    if (state.getId() != 1 && state.getId() != 4)
                        buff.append("when ti.task_instance_state = " + state.getId() + " THEN '" + state.getDescription() + "' ");
                    else
                        buff.append("when ti.task_instance_state = " + state.getId() + " THEN NULL ");

                buff.append("END");
            }
        }
        else if (orderBy.equals("TASK_INSTANCE_STATUS")) {
            if (db.isOracle()) {
                buff.append("decode(ti.task_instance_status");
                for (TaskStatus status : getAllTaskStatuses())
                    buff.append(", " + status.getId() + ", '" + status.getDescription() + "'");

                buff.append(", 'ZZ')");
            }
            else { // mySQL
                buff.append("case ");
                for (TaskStatus status : getAllTaskStatuses())
                    buff.append("when ti.task_instance_status = " + status.getId() + " THEN '" + status.getDescription() + "' ");

                buff.append("END");
            }
        }
        else if (variables != null && variables.contains("DATE:" + orderBy))
            buff.append("to_date(substr(" + orderBy + ", 5, 7) || substr(" + orderBy + ", 25), 'MON DD YYYY')");
        else
            buff.append(orderBy);

        if (ascending) {
            buff.append(" asc");
        } else {
            buff.append(" desc");
        }
        return buff.toString();
    }

    /**
     * Builds the Task Instance Query with the passed in params and optional variables
     * @param criteria
     * @param variables
     * @param variablesCriteria
     * @param userGroups
     * @param orderBy
     * @param ascendingOrder
     * @return query string
     */
    private String buildTaskInstanceQuery(Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria,
            List<String> indexNames, Map<String,String> indexCriteria, List<String> searchColumns, Object searchKey, String[] userGroups, String orderBy, boolean ascendingOrder)
    throws DataAccessException {
        boolean inclProcessInst = !CollectionUtil.isEmpty(variables) || !CollectionUtil.isEmpty(variablesCriteria);

        StringBuffer buff = new StringBuffer();
        buff.append("select ").append(TASK_INSTANCE_SELECT);
        if (inclProcessInst)
            buff.append(", pi.master_request_id as pi_master_request_id");
        buff.append(getIndexSelectString(indexNames));
        buff.append(getVariablesSelectString(variables));
        if ("TASK_CATEGORY_CD".equals(orderBy))
            buff.append(", cat.category");
        if (db.isMySQL()) {
            buff.append("\nfrom TASK_INSTANCE ti left join USER_INFO ui ");
            buff.append("on ui.user_info_id = ti.task_claim_user_id ");
            if ("TASK_CATEGORY_CD".equals(orderBy)) {
                buff.append("left join (");
                boolean catCount = false;
                for (TaskTemplate category : TaskTemplateCache.getTaskTemplates()) {
                    if (catCount)
                        buff.append(" UNION ALL ");
                    else
                        catCount = true;

                    buff.append("select '" + category.getTaskId() + "' AS taskid, '" + category.getTaskCategory() + "' AS category");
                }
                if (catCount)
                    buff.append(") as cat on ti.task_id = cat.taskid");
            }
            if (inclProcessInst)
                buff.append("left join process_instance pi on pi.process_instance_id = ti.task_instance_owner_id ");
            if (searchKey != null || indexCriteria != null)
                buff.append("left join task_inst_index tidx on tidx.TASK_INSTANCE_ID = ti.TASK_INSTANCE_ID ");
        } else {
            buff.append("\nfrom ").append(TASK_INSTANCE_FROM);
            if ("TASK_CATEGORY_CD".equals(orderBy)) {
                buff.append(", (");
                boolean catCount = false;
                for (TaskTemplate category : TaskTemplateCache.getTaskTemplates()) {
                    if (catCount)
                        buff.append(" UNION ALL ");
                    else
                        catCount = true;

                    buff.append("select '" + category.getTaskId() + "' AS taskid, '" + category.getTaskCategory() + "' AS category from dual");
                }
                if (catCount)
                    buff.append(") cat");
            }
        }
        if (userGroups != null && hasInstanceGroupMappings && !containsSiteAdmin(userGroups))
            buff.append(", task_inst_grp_mapp tigm");
        if (inclProcessInst && !db.isMySQL())
            buff.append(", process_instance pi");
        if (!db.isMySQL() && (searchKey != null || indexCriteria != null))
            buff.append(", task_inst_index tidx");
        if (db.isMySQL()) {
            buff.append("\nwhere 1=1 ");
        } else {
            buff.append("\nwhere ui.user_info_id(+) = ti.task_claim_user_id ");
        }
        if (inclProcessInst && !db.isMySQL())
            buff.append("and pi.process_instance_id(+) = ti.task_instance_owner_id ");
        else if (!db.isMySQL() && "TASK_CATEGORY_ID".equals(orderBy))
            buff.append("and ti.task_id = cat.taskid(+) ");
        if (!db.isMySQL() && (searchKey != null || indexCriteria != null)) {
            buff.append("and tidx.task_instance_id(+) = ti.task_instance_id ");
        }

        String addWhereClause = buildWhereClause(criteria);
        if(!StringHelper.isEmpty(addWhereClause)){
            buff.append(addWhereClause);
        }
        //TaskList Search
        if (searchKey != null && searchColumns != null && !searchColumns.isEmpty()) {
            String serachClause = buildTaskSearchWhereClause(searchKey, searchColumns);
            if (!StringHelper.isEmpty(serachClause)) {
                buff.append(serachClause);
            }
        }

        if (userGroups != null && !containsSiteAdmin(userGroups))
          buff.append(buildUserGroupsTaskClause(userGroups));

        String variablesWhereClause = buildVariablesWhereClause(variablesCriteria);
        if (!StringHelper.isEmpty(variablesWhereClause)) {
            buff.append(variablesWhereClause);
        }
        String indexCriteriaWhereClause = buildIndexCriteriaWhereClause(indexCriteria);
        if (!StringHelper.isEmpty(indexCriteriaWhereClause)) {
            buff.append(indexCriteriaWhereClause);
        }

        String orderByClause = buildOrderByClause(orderBy, ascendingOrder, variables);
        if(!StringHelper.isEmpty(orderByClause)){
            buff.append(orderByClause);
        }
        return buff.toString();
    }

    /**
     * To build search where clause for TaskList search
     * @param searchKey
     * @param visibleDbColumns
     * @return
     */
    private String buildTaskSearchWhereClause(Object searchKey, List<String> visibleDbColumns) {
        StringBuffer searchClause = new StringBuffer();
        String searchValue = searchKey.toString().toUpperCase();
        boolean isSearchKeyNumeric = false;
        boolean isStateOrStatus = false;
        try {
            Long.parseLong(searchKey.toString());
            isSearchKeyNumeric = true;
        }
        catch (NumberFormatException nfe) {
            isSearchKeyNumeric = false;
        }
        searchClause.append(" and (");
        if (!isSearchKeyNumeric) {
            if (visibleDbColumns.contains("TASK_INSTANCE_STATE")) {
                Integer stateCode = TaskState.getStatusForNameContains(searchValue);
                if (stateCode != null) {
                    searchClause.append("ti.task_instance_state =").append(stateCode).append(")");
                    isStateOrStatus = true;
                }
            }
            if (visibleDbColumns.contains("TASK_INSTANCE_STATUS")) {
                Integer statusCode = TaskStatus.getStatusCodeForNameContains(searchValue);
                if (statusCode != null) {
                    searchClause.append("ti.task_instance_status =").append(statusCode).append(")");
                    isStateOrStatus = true;
                }
            }
        }

        if (isStateOrStatus) {
            return searchClause.toString();
        }

        for (String column : visibleDbColumns) {
            if (isSearchKeyNumeric) {
                if ("TASK_INSTANCE_ID".equals(column)) {
                    searchClause.append("ti.task_instance_id like ").append("'%").append(searchKey).append("%'").append(" OR ");
                }
                else if ("MASTER_REQUEST_ID".equals(column)) {
                    searchClause.append("UPPER(ti.MASTER_REQUEST_ID) like ").append("'%").append(searchKey).append("%'").append(" OR ");
                }
                else if ("TASK_INSTANCE_OWNER_ID".equals(column)) {
                    searchClause.append("UPPER(ti.TASK_INSTANCE_OWNER_ID) like ").append("'%").append(searchKey).append("%'").append(" OR ");
                }
            }
            else {
                if ("TASK_CATEGORY_CD".equals(column) && !(searchValue.length() > 3)) {
                    searchClause.append("t.task_category_id in (select tc.task_category_id from task_category tc where UPPER(tc.task_category_cd) like ")
                            .append("'%").append(searchValue).append("%')").append(" OR ");
                }
                else if ("TASK_NAME".equals(column)) {
                    searchClause.append("UPPER(t.task_name) like ").append("'%").append(searchValue).append("%'").append(" OR ");
                    //searchClause.append("UPPER(ti.TASK_INSTANCE_REFERRED_AS) like ").append("'%").append(searchValue).append("%'").append(" OR ");
                }
                else if ("MASTER_REQUEST_ID".equals(column)) {
                    searchClause.append("UPPER(ti.MASTER_REQUEST_ID) like ").append("'%").append(searchValue).append("%'").append(" OR ");
                }
                else if ("USER_NAME".equals(column)) {
                    searchClause.append("UPPER(ui.NAME) like ").append("'%").append(searchValue).append("%'").append(" OR ");
                }
                else if ("TASK_START_DT".equals(column)) {
                    // TODO : based on requirement
                }
                else if ("SLA_ESTM_COMP_DT".equals(column)) {
                    // TODO : based on requirement
                }
            }
        }
        searchClause.append("UPPER(tidx.index_value) like ").append("'%").append(searchValue).append("%')");
        //searchClause.replace(searchClause.length() - 3, searchClause.length(), ")"); // To remove last appended " OR"
        return searchClause.toString();
    }

    /**
     * Determine tasks to include in the query based on appropriate user groups.
     */
    private String buildUserGroupsTaskClause(String[] userGroups) throws DataAccessException {
        StringBuffer buff = new StringBuffer();
        if (userGroups != null) {
            // determine group ids according to instance-level mappings
            List<Long> groupIds = getGroupIds(userGroups);
            String instGroupIdsClause = null;
            if (!groupIds.isEmpty() && hasInstanceGroupMappings) {
                StringBuffer instSb = new StringBuffer();
                instSb.append("ti.task_instance_id = tigm.task_instance_id ");
                instSb.append("and tigm.user_group_id in (");

                for (int i = 0; i < groupIds.size(); i++) {
                    instSb.append(groupIds.get(i));
                    if (i < groupIds.size() - 1) {
                        instSb.append(", ");
                    }
                }
                instSb.append(") ");
                instGroupIdsClause = instSb.toString();
            }

            if (instGroupIdsClause != null)
                buff.append(instGroupIdsClause);
        }
        return buff.length() == 0 ? "" : " and " + buff.toString();
    }

    private String buildCategoryTasksClause(int categoryId) throws DataAccessException {
        StringBuffer clause = new StringBuffer("task_id in (");
        List<TaskTemplate> tasks = TaskTemplateCache.getTaskTemplatesForCategory(categoryId);
        if (tasks.isEmpty())
            clause.append("0)");  // no matching tasks
        else {
            for (int i = 0; i < tasks.size(); i++) {
                clause.append(tasks.get(i).getTaskId());
                if (i < tasks.size() - 1)
                    clause.append(",");
            }
            clause.append(")");
        }
        return clause.toString();
    }

    private boolean containsSiteAdmin(String[] workgroups) {
        for (String groupName : workgroups) {
            if (Workgroup.SITE_ADMIN_GROUP.equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    private List<Long> getGroupIds(String[] groups) throws DataAccessException {
        try {
            List<Long> groupIds = new ArrayList<Long>();
            for (String group : groups) {
                if (!group.equals(Workgroup.COMMON_GROUP)) {
                  Long id = UserGroupCache.getWorkgroup(group).getId();
                  if (!groupIds.contains(id))
                      groupIds.add(id);
                }
            }
            return groupIds;
        }
        catch (Exception ex) {
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
    }

    /**
     * Returns all the task instances that are mapped to the user group
     * that match with the passed in params, including optional variables
     * (used for workgroupTasks query when All link is selected)
     * @param criteria
     * @param variables
     * @param variablesCriteria
     * @param userGroups
     * @param orderBy
     * @param ascendingOrder
     * @return list of task instance vos
     */
    public List<TaskInstance> queryTaskInstances(Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria,
            List<String> indexNames, Map<String,String> indexCriteria, List<String> searchColumns, Object searchKey, String[] userGroups, String orderBy, boolean ascendingOrder)
    throws DataAccessException {
        String query = buildTaskInstanceQuery(criteria, variables, variablesCriteria, indexNames, indexCriteria, searchColumns, searchKey, userGroups, orderBy, ascendingOrder);
        if(logger.isMdwDebugEnabled()){
            logger.mdwDebug("queryTaskInstances() Query --> "+query) ;
        }
        try {
            db.openConnection();
            List<TaskInstance> taskInstances = new ArrayList<TaskInstance>();
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                TaskInstance taskInst = getTaskInstanceSub(rs, true, variables, indexNames);
                if (taskInst != null)
                    taskInstances.add(taskInst);
            }
            return taskInstances;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to query task instances", e);
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Retrieve task instances with specified variable values
     * (Used for myTasks query)
     * @param criteria selection criteria
     * @param variables variables to include
     * @param variablesCriteria
     * @return list of task instance VOs
     */
    public List<TaskInstance> queryTaskInstances(Map<String,String> criteria, List<String> variables, Map<String,String> variablesCriteria,
            List<String> indexNames, Map<String,String> indexCriteria)
    throws DataAccessException {

        String query = buildTaskInstanceQuery(criteria, variables, variablesCriteria, indexNames, indexCriteria, null, null, null, null, false);

        if(logger.isMdwDebugEnabled()){
            logger.mdwDebug("queryTaskInstances() Query-->"+query) ;
        }
        try {
            db.openConnection();
            List<TaskInstance> taskInstances = new ArrayList<TaskInstance>();
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                TaskInstance taskInst = getTaskInstanceSub(rs, true, variables, indexNames);
                if (taskInst != null)
                    taskInstances.add(taskInst);
            }
            return taskInstances;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to query task instances", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<TaskInstance> getTaskInstancesForProcessInstance(Long procInstId)
    throws DataAccessException {
        return getTaskInstancesForProcessInstance(procInstId, false);
    }

    public List<TaskInstance> getTaskInstancesForProcessInstance(Long procInstId, boolean includeInstanceGroups)
    throws DataAccessException {
        try {
            db.openConnection();
            List<TaskInstance> taskInstances = new ArrayList<TaskInstance>();
            StringBuffer query = new StringBuffer();
            query.append("select ");
            query.append(TASK_INSTANCE_SELECT_SHALLOW);
            query.append(" from TASK_INSTANCE ti");
            query.append(" where ti.TASK_INSTANCE_OWNER='PROCESS_INSTANCE' and ti.TASK_INSTANCE_OWNER_ID = ?");
            ResultSet rs = db.runSelect(query.toString(), procInstId);
            while (rs.next()) {
                TaskInstance taskInst = getTaskInstanceSub(rs, false);
                Long assigneeId = taskInst.getAssigneeId();
                if (assigneeId != null && assigneeId.longValue() != 0) {
                    User user = UserGroupCache.getUser(assigneeId);
                    if (user != null)
                      taskInst.setAssigneeCuid(user.getCuid());
                }
                taskInstances.add(taskInst);
            }
            if (includeInstanceGroups) {
                for (TaskInstance taskInstance: taskInstances) {
                    getTaskInstanceGroups(taskInstance);
                }
            }
            return taskInstances;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to query task instances", e);
        } finally {
            db.closeConnection();
        }
    }

    /**
     * Returns all the task instances that are clained by the user
     * @param pTaskInstId
     * @param pOmitOwner for the process instance
     * @return the taskInst and associated data
     * @throws TaskDataException
     * @throws DataAccessException
     *
     */
    public TaskInstance getTaskInstanceAllInfo(Long pTaskInstId)
        throws DataAccessException {

        StringBuffer buff = new StringBuffer();
        buff.append("select ").append(TASK_INSTANCE_SELECT);
        if (db.isMySQL()) {
            buff.append(" from TASK_INSTANCE ti left join USER_INFO ui on ui.user_info_id = ti.task_claim_user_id ");
        } else {
            buff.append(" from ").append(TASK_INSTANCE_FROM);
        }

        buff.append(" where ti.task_instance_id = ? ");
        if (!db.isMySQL()) buff.append("and ui.user_info_id(+) = ti.task_claim_user_id ");

        String query = buff.toString();
        if(logger.isMdwDebugEnabled()){
            logger.mdwDebug("getTaskInstanceAllInfo() Query-->"+query) ;
        }
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(query, pTaskInstId);
            if (rs.next()) {
                TaskInstance taskInst = getTaskInstanceSub(rs, true);
                if (rs.next()) throw new SQLException("Non unique result");
                TaskTemplate taskVO = TaskTemplateCache.getTaskTemplate(taskInst.getTaskId());
                if (taskVO != null) {
                    getTaskInstanceAdditionalInfoGeneral(taskInst);
                } else {
                    // for invalid instances, just load the groups to allow actioning
                    getTaskInstanceGroups(taskInst);
                }
                return taskInst;
            } else return null;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task instance", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<TaskAction> getAllTaskActions()
                throws DataAccessException {
        try {
            db.openConnection();
            List<TaskAction> actions = new ArrayList<TaskAction>();
            String query = "select TASK_ACTION_ID,TASK_ACTION_NAME,COMMENTS from TASK_ACTION";
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                TaskAction one = new TaskAction();
                one.setTaskActionId(rs.getLong(1));
                one.setTaskActionName(rs.getString(2));
                actions.add(one);
            }
            return actions;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task actions", e);
        } finally {
            db.closeConnection();
        }
    }

    public TaskAction createTaskAction(String action, String description)
        throws DataAccessException {
        try {
            db.openConnection();
            Long id = db.isMySQL()?null:this.getNextId("MDW_COMMON_ID_SEQ");
            String query = "insert into TASK_ACTION " +
                "(TASK_ACTION_ID,TASK_ACTION_NAME,CREATE_DT,CREATE_USR,COMMENTS) " +
                "values (?,?,sysdate,'MDW',?)";
            Object[] args = new Object[3];
            args[0] = id;
            args[1] = action;
            args[2] = description;
            if (db.isMySQL()) id = db.runInsertReturnId(query, args);
            else db.runUpdate(query, args);
            db.commit();
            TaskAction ret = new TaskAction();
            ret.setTaskActionId(id);
            ret.setTaskActionName(action);
            return ret;
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to create task action", e);
        } finally {
            db.closeConnection();
        }
    }

    public TaskAction getTaskAction(String action)
        throws DataAccessException {
        TaskAction one = new TaskAction();
        one.setTaskActionId(0L);
        one.setTaskActionName(action);
        return one;
    }

    public List<TaskStatus> getAllTaskStatuses() throws DataAccessException {
        return DataAccess.getBaselineData().getAllTaskStatuses();
    }

    public List<TaskState> getAllTaskStates() throws DataAccessException {
        return DataAccess.getBaselineData().getAllTaskStates();
    }

    public class TaskInstanceReportItem {
        public String entityName;
        public int state;
        public int count;
    }

    public List<TaskInstanceReportItem> getTaskInstanceReport(String query)
            throws DataAccessException {

        try {
            db.openConnection();
            List<TaskInstanceReportItem> reports = new ArrayList<TaskInstanceReportItem>();
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                TaskInstanceReportItem report = new TaskInstanceReportItem();
                report.entityName = rs.getString(1);
                report.state = rs.getInt(2);
                report.count = rs.getInt(3);
                reports.add(report);
            }
            return reports;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task instance report", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<Attribute> getTaskAttributes(Long taskId)
        throws DataAccessException {
        if (taskId==null) return new ArrayList<Attribute>(0); // jxxu - this can happen, not sure why
        try {
            db.openConnection();
            return super.getAttributes1(OwnerType.TASK, taskId);
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task attributes", e);
        } finally {
            db.closeConnection();
        }
    }

    public void updateAttribute(Long attributeId, String attributeName, String attributeValue)
    throws DataAccessException {
        try {
            db.openConnection();
            String query;
            if (attributeValue==null) {
                query = "delete ATTRIBUTE where ATTRIBUTE_ID=?";
                db.runUpdate(query, attributeId);
            } else {
                query = "update ATTRIBUTE set ATTRIBUTE_NAME=?, ATTRIBUTE_VALUE=? where ATTRIBUTE_ID=?";
                Object[] args = new Object[3];
                args[0] = attributeName;
                args[1] = attributeValue;
                args[2] = attributeId;
                db.runUpdate(query, args);
            }
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to update task attribute", e);
        } finally {
            db.closeConnection();
        }
    }

    public void setTaskAttribute(Long taskId, String attrname, String attrvalue)
            throws DataAccessException {
        try {
            db.openConnection();
            setAttribute0(OwnerType.TASK, taskId, attrname, attrvalue);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to set task attribute", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<InstanceNote> getInstanceNotes(String ownerType, Long ownerId)
        throws DataAccessException {
        try {
            db.openConnection();
            List<InstanceNote> notes = new ArrayList<InstanceNote>();
            String query = "select INSTANCE_NOTE_ID,INSTANCE_NOTE_NAME,INSTANCE_NOTE_DETAILS," +
                " CREATE_DT,CREATE_USR,MOD_DT,MOD_USR " +
                "from INSTANCE_NOTE " +
                "where INSTANCE_NOTE_OWNER='" + ownerType + "' and INSTANCE_NOTE_OWNER_ID=?";
            ResultSet rs = db.runSelect(query, ownerId);
            while (rs.next()) {
                InstanceNote note = new InstanceNote();
                note.setId(rs.getLong(1));
                note.setOwnerType(ownerType);
                note.setOwnerId(ownerId);
                note.setNoteName(rs.getString(2));
                note.setNoteDetails(rs.getString(3));
                note.setCreatedDate(rs.getTimestamp(4));
                note.setCreatedBy(rs.getString(5));
                note.setModifiedDate(rs.getTimestamp(6));
                note.setModifiedBy(rs.getString(7));
                notes.add(note);
            }
            return notes;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task instance notes", e);
        } finally {
            db.closeConnection();
        }
    }

    public Long createInstanceNote(String pOwner, Long pOwnerId,
            String pNoteName, String pNoteDetails, String pCreatedBy)
        throws DataAccessException {
        try {
            db.openConnection();
            Long id = db.isMySQL()?null:this.getNextId("INSTANCE_NOTE_ID_SEQ");
            String query = "insert into INSTANCE_NOTE " +
                "(INSTANCE_NOTE_ID,INSTANCE_NOTE_OWNER,INSTANCE_NOTE_OWNER_ID," +
                " INSTANCE_NOTE_NAME,INSTANCE_NOTE_DETAILS," +
                " CREATE_DT,CREATE_USR) " +
                "values (?,?,?,?,?,"+now()+",?)";
            Object[] args = new Object[6];
            args[0] = id;
            args[1] = pOwner;
            args[2] = pOwnerId;
            args[3] = pNoteName;
            args[4] = pNoteDetails;
            args[5] = pCreatedBy;
            if (db.isMySQL()) id = db.runInsertReturnId(query, args);
            else db.runUpdate(query, args);
            db.commit();
            return id;
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to create task instance note", e);
        } finally {
            db.closeConnection();
        }
    }

    public void deleteInstanceNote(Long instanceNoteId)
        throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete INSTANCE_NOTE where INSTANCE_NOTE_ID=?";
            db.runUpdate(query, instanceNoteId);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to delete task instance note", e);
        } finally {
            db.closeConnection();
        }
    }

    public void updateInstanceNote(Long pInstNoteId, String pNoteName, String pNoteDetails, String pCuid)
        throws DataAccessException {
        try {
            db.openConnection();
            String query = "update INSTANCE_NOTE " +
            "set INSTANCE_NOTE_NAME=?,INSTANCE_NOTE_DETAILS=?,MOD_DT="+now()+",MOD_USR=? " +
            "where INSTANCE_NOTE_ID=?";
            Object[] args = new Object[4];
            args[0] = pNoteName;
            args[1] = pNoteDetails;
            args[2] = pCuid;
            args[3] = pInstNoteId;
            db.runUpdate(query, args);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to update instance note", e);
        } finally {
            db.closeConnection();
        }
    }

    public void updateInstanceNote(String owner, Long ownerId, String name, String details, String cuid)
            throws DataAccessException {
            int found = 0;
            try {
                db.openConnection();
                String query = "update INSTANCE_NOTE " +
                "set INSTANCE_NOTE_NAME=?,INSTANCE_NOTE_DETAILS=?,MOD_DT="+now()+",MOD_USR=? " +
                "where INSTANCE_NOTE_OWNER=? and INSTANCE_NOTE_OWNER_ID=? and INSTANCE_NOTE_NAME=?";
                Object[] args = new Object[6];
                args[0] = name;
                args[1] = details;
                args[2] = cuid;
                args[3] = owner;
                args[4] = ownerId;
                args[5] = name;
                found = db.runUpdate(query, args);
                db.commit();
            } catch (Exception e) {
                db.rollback();
                throw new DataAccessException(0,"failed to update instance note", e);
            } finally {
                db.closeConnection();
            }
            if (found == 0)
                throw new DataAccessException("Note '" + name + "' not found for owner " + owner + " with ID " + ownerId);
        }

    public List<Attachment> getTaskInstanceAttachments(String attachmentLocation, Long taskInstanceId)
        throws DataAccessException {
        try {
            db.openConnection();
            List<Attachment> attachments = new ArrayList<Attachment>();
            String query = "select ATTACHMENT_ID,ATTACHMENT_OWNER,ATTACHMENT_OWNER_ID,ATTACHMENT_NAME,ATTACHMENT_LOCATION," +
                    "ATTACHMENT_CONTENT_TYPE," +
                " CREATE_DT,CREATE_USR,MOD_DT,MOD_USR " +
                "from ATTACHMENT " +
                "where ATTACHMENT_LOCATION=? and ATTACHMENT_STATUS=?";
            Object[] args = new Object[2];
            if (attachmentLocation.startsWith(MiscConstants.ATTACHMENT_LOCATION_PREFIX)) {
                args[0] = MiscConstants.ATTACHMENT_LOCATION_PREFIX+taskInstanceId;
            } else {
                args[0] = attachmentLocation + taskInstanceId + "/";
            }
            args[1] = Attachment.STATUS_ATTACHED;
            ResultSet rs = db.runSelect(query, args);
            while (rs.next()) {
                Attachment attachment = new Attachment();
                attachment.setId(rs.getLong(1));
                attachment.setOwnerType(rs.getString(2));
                attachment.setOwnerId(rs.getLong(3));
                attachment.setAttachmentName(rs.getString(4));
                attachment.setAttachmentLocation(rs.getString(5));
                attachment.setAttachmentContentType(rs.getString(6));
                attachment.setCreatedDate(rs.getTimestamp(7));
                attachment.setCreatedBy(rs.getString(8));
                attachment.setModifiedDate(rs.getTimestamp(9));
                attachment.setModifiedBy(rs.getString(10));
                attachments.add(attachment);
            }
            return attachments;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to get task instance attachments", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<Attachment> getAttachments(String attachmentName,String attachmentLocation)
            throws DataAccessException {
         try {
             db.openConnection();
             int argumentSize = 2;
             if (! StringHelper.isEmpty(attachmentName)) {
                 argumentSize = 3;
             }
             List<Attachment> attachments = new ArrayList<Attachment>();
             String query = "select ATTACHMENT_ID,ATTACHMENT_OWNER,ATTACHMENT_OWNER_ID,ATTACHMENT_NAME," +
                  "ATTACHMENT_CONTENT_TYPE," +
                 " CREATE_DT,CREATE_USR,MOD_DT,MOD_USR " +
                 "from ATTACHMENT " +
                 "where ATTACHMENT_STATUS=? and ATTACHMENT_LOCATION=?";
             Object[] args = new Object[argumentSize];
             args[0] = Attachment.STATUS_ATTACHED;
             if (! attachmentLocation.startsWith(MiscConstants.ATTACHMENT_LOCATION_PREFIX)
                     && ! attachmentLocation.endsWith("/")) {
                 attachmentLocation += "/";
             }
             args[1] = attachmentLocation;
             if (! StringHelper.isEmpty(attachmentName)){
                 query = query + " and ATTACHMENT_NAME=?";
                 args[2] = attachmentName;
             }
             ResultSet rs = db.runSelect(query, args);
             while (rs.next()) {
                 Attachment attachment = new Attachment();
                 attachment.setId(rs.getLong(1));
                 attachment.setOwnerType(rs.getString(2));
                 attachment.setOwnerId(rs.getLong(3));
                 attachment.setAttachmentName(rs.getString(4));
                 attachment.setAttachmentLocation((String)args[1]);
                 attachment.setAttachmentContentType(rs.getString(5));
                 attachment.setCreatedDate(rs.getTimestamp(6));
                 attachment.setCreatedBy(rs.getString(7));
                 attachment.setModifiedDate(rs.getTimestamp(8));
                 attachment.setModifiedBy(rs.getString(9));
                 attachments.add(attachment);
             }
             return attachments;
         } catch (Exception e) {
             throw new DataAccessException(0, "failed to get task instance attachments", e);
         } finally {
             db.closeConnection();
         }

    }




    public Long createAttachment(String pOwner, Long pOwnerId, Integer status,
            String attachmentName, String attachmentLocation, String contentType, String pCreatedBy)
        throws DataAccessException {
        try {
            db.openConnection();
            Long id = db.isMySQL()?null:this.getNextId("ATTACHMENT_ID_SEQ");
            String query = "insert into ATTACHMENT " +
                "(ATTACHMENT_ID,ATTACHMENT_OWNER,ATTACHMENT_OWNER_ID," +
                " ATTACHMENT_NAME,ATTACHMENT_LOCATION,ATTACHMENT_CONTENT_TYPE,ATTACHMENT_STATUS," +
                " CREATE_DT,CREATE_USR) " +
                "values (?,?,?,?,?,?,?,"+now()+",?)";
            Object[] args = new Object[8];
            args[0] = id;
            args[1] = pOwner;
            args[2] = pOwnerId;
            args[3] = attachmentName;
            args[4] = attachmentLocation;
            args[5] = contentType;
            args[6] = status;
            args[7] = pCreatedBy;
            if (db.isMySQL()) id = db.runInsertReturnId(query, args);
            else db.runUpdate(query, args);
            db.commit();
            return id;
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to create task instance attachment", e);
        } finally {
            db.closeConnection();
        }
    }

    public void deleteAttachment(Long AttachmentId)
        throws DataAccessException {
        try {
            db.openConnection();
            String query = "update ATTACHMENT set ATTACHMENT_STATUS=? where ATTACHMENT_ID=?";
            Object[] args = new Object[2];
            args[0] = Attachment.STATUS_DETTACHED;
            args[1] = AttachmentId;
            db.runUpdate(query, args);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to delete task instance attachment", e);
        } finally {
            db.closeConnection();
        }
    }

    public Attachment getAttachment(Long pAttachmentId)
            throws DataAccessException {
            try {
                db.openConnection();
                Attachment attachment = null;
                String query = " SELECT ATTACHMENT_OWNER,ATTACHMENT_OWNER_ID," +
                        "ATTACHMENT_NAME,ATTACHMENT_LOCATION,ATTACHMENT_CONTENT_TYPE " +
                        "FROM ATTACHMENT where ATTACHMENT_ID=?";
                Object[] args = new Object[1];
                args[0] = pAttachmentId;
                ResultSet rs = db.runSelect(query, args);
                if (rs.next()) {
                    attachment = new Attachment();
                    attachment.setOwnerType(rs.getString(1));
                    attachment.setOwnerId(rs.getLong(2));
                    attachment.setAttachmentName(rs.getString(3));
                    attachment.setAttachmentLocation(rs.getString(4));
                    attachment.setAttachmentContentType(rs.getString(5));
                }
                return attachment;
            } catch (Exception e) {
                throw new DataAccessException(0,"failed to retrieve attachment for AttachmentId "+pAttachmentId, e);
            } finally {
                db.closeConnection();
            }
        }

    public void updateAttachment(Long id, String attachmentLocation, String pCuid)
        throws DataAccessException {
        try {
            db.openConnection();
            String query = "update ATTACHMENT " +
                "set ATTACHMENT_LOCATION=?,MOD_DT="+now()+",MOD_USR=? " +
                "where ATTACHMENT_ID=?";
            Object[] args = new Object[3];
            args[0] = attachmentLocation;
            args[1] = pCuid;
            args[2] = id;
            db.runUpdate(query, args);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to updat instance attachment", e);
        } finally {
            db.closeConnection();
        }
    }

    public String getVariablesSelectString(List<String> variables) {
        StringBuffer buff = new StringBuffer();
        if (variables != null && variables.size() > 0) {
            for (String varName : variables) {
                String name = varName;
                if (varName.startsWith("DATE:")) {
                    name = varName.substring(5);
                }

                buff.append(",\n");
                buff.append("(select vi.variable_value from variable_instance vi "
                        + " where pi.process_instance_id= vi.process_inst_id "
                        + " and vi.variable_name = '" + name + "' ");
               if (db.isMySQL())
                   buff.append(" limit 1) ");
               else
                   buff.append(" and rownum < 2) ");
               buff.append(name);
            }
        }
        return buff.toString();
    }

    public String getIndexSelectString(List<String> indexNames) {
        StringBuffer buff = new StringBuffer();
        if (indexNames != null) {
            for (String name : indexNames) {
                buff.append(",\n");
                buff.append("(select tii.index_value from task_inst_index tii where tii.task_instance_id = ti.task_instance_id and tii.index_key= '").append(name)
                    .append("'");
                if (db.isMySQL())
                    buff.append(" limit 1) ");
                else
                    buff.append(" and rownum < 2) ");
                buff.append(name);
            }
        }
        return buff.toString();
    }

    // new task instance group mapping
    public void setTaskInstanceGroups(Long taskInstId, String[] groups)
        throws DataAccessException {
        try {
            db.openConnection();
            // get group IDs
            StringBuffer sb = new StringBuffer();
            sb.append("select USER_GROUP_ID from USER_GROUP where GROUP_NAME in (");
            for (int i=0; i<groups.length; i++) {
                if (i>0) sb.append(",");
                sb.append("'").append(groups[i]).append("'");
            }
            sb.append(")");
            ResultSet rs = db.runSelect(sb.toString(), null);
            List<Long> groupIds = new ArrayList<Long>();
            while (rs.next()) {
                groupIds.add(rs.getLong(1));
            }
            // delete existing groups
            String query = "delete from TASK_INST_GRP_MAPP where TASK_INSTANCE_ID=?";
            db.runUpdate(query, taskInstId);
            // insert groups
            query = "insert into TASK_INST_GRP_MAPP " +
                "(TASK_INSTANCE_ID,USER_GROUP_ID,CREATE_DT) values (?,?,"+now()+")";
            db.prepareStatement(query);
            Object[] args = new Object[2];
            args[0] = taskInstId;
            for (Long group : groupIds) {
                args[1] = group;
                db.runUpdateWithPreparedStatement(args);
            }
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to associate task instance groups", e);
        } finally {
            db.closeConnection();
        }
    }

    // new task instance indices
    public void setTaskInstanceIndices(Long taskInstId, Map<String,String> indices)
        throws DataAccessException {
        try {
            db.openConnection();
            // delete existing indices
            String query = "delete from TASK_INST_INDEX where TASK_INSTANCE_ID=?";
            // insert new ones
            db.runUpdate(query, taskInstId);
            query = "insert into TASK_INST_INDEX " +
                "(TASK_INSTANCE_ID,INDEX_KEY,INDEX_VALUE,CREATE_DT) values (?,?,?,"+now()+")";
            db.prepareStatement(query);
            Object[] args = new Object[3];
            args[0] = taskInstId;
            for (String key : indices.keySet()) {
                args[1] = key;
                args[2] = indices.get(key);
                if (!StringHelper.isEmpty((String)args[2])) db.runUpdateWithPreparedStatement(args);
            }
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to add task instance indices", e);
        } finally {
            db.closeConnection();
        }
    }

    public void setTaskInstancePriority(Long taskInstanceId, Integer priority)
    throws DataAccessException {
        try {
            db.openConnection();
            Object[] args = new Object[2];
            args[0] = priority;
            args[1] = taskInstanceId;
            String query = "update TASK_INSTANCE set PRIORITY=? where TASK_INSTANCE_ID=?";
            db.runUpdate(query, args);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0,"failed to update task instance priority", e);
        } finally {
            db.closeConnection();
        }
    }

    private void buildValueSpec(StringBuffer buff, String value) {
        if (value==null) buff.append(" is null");
        else if (value.startsWith("~")) {
            buff.append(" like '").append(value.substring(1)).append("'");
        } else if (value.startsWith(">=")) {
            if (db.isMySQL()) buff.append(">='").append(value.substring(1)).append("'");
            else buff.append(">=to_date('").append(value.substring(1)).append("','yyyy-mm-dd')");
        } else if (value.startsWith("<=")) {
            if (db.isMySQL()) buff.append("<='").append(value.substring(1)).append("'");
            else buff.append("<=to_date('").append(value.substring(1)).append("','yyyy-mm-dd')");
        } else buff.append("='").append(value).append("'");
    }

 // Final query should look like:
//  select distinct ti.task_instance_id,ti.task_instance_referred_as, si.SLA_ESTM_COMP_DT, ui.CUID
//  from task_instance ti, SLA_INSTANCE si, USER_INFO ui, task_inst_grp_mapp tg, user_group g, task_inst_index ix1, task_inst_index ix2
//  where si.SLA_INST_OWNER_ID(+)= ti.TASK_INSTANCE_ID
//  and si.SLA_INST_OWNER(+) = 'TASK_INSTANCE'
//  and ui.USER_INFO_ID(+) = ti.TASK_CLAIM_USER_ID
//  and ti.task_instance_id=tg.task_instance_id
//  and tg.user_group_id=g.user_group_id
//  and g.group_name in ('CSO Users', 'Fallout')
//  and ix1.index_key='State' and ix1.index_value='CO' and ix1.task_instance_id=ti.task_instance_id
//  and ix2.index_key='City' and ix2.index_value in ('Superior','Boulder') and ix2.task_instance_id=ti.task_instance_id
    public String buildFromWhereClause(Map<String,String> criteria) {
         boolean hasGroups = false;
         int numberOfIndices = 0;
         int k;
         for (String key : criteria.keySet()) {
             if (key.startsWith("ix_")) {
                 numberOfIndices++;
             } else if (key.equals("groups")) {
                 hasGroups = true;
             }
         }
         StringBuffer buff = new StringBuffer();
         if (db.isMySQL()) {
             buff.append("from TASK_INSTANCE ti left join USER_INFO ui ");
             buff.append("on ui.USER_INFO_ID = ti.TASK_CLAIM_USER_ID ");
         } else {
             buff.append("from TASK_INSTANCE ti, USER_INFO ui");
         }
         if (hasGroups) buff.append(", TASK_INST_GRP_MAPP tg, USER_GROUP g");
         for (k=1; k<=numberOfIndices; k++) buff.append(", TASK_INST_INDEX ix").append(k);
         buff.append("\n");
         buff.append("where ");
         if (db.isMySQL()) {
             buff.append("1=1 ");    // just to avoid checking if need "and"
         } else {
             buff.append("ui.USER_INFO_ID(+) = ti.TASK_CLAIM_USER_ID\n");
         }
         k = 0;
         for (String key : criteria.keySet()) {
             String value = criteria.get(key);
             if (key.startsWith("ti_")) {
                 buff.append("and ").append("ti.").append(key.substring(3));
                 buildValueSpec(buff, value);
                 buff.append("\n");
             } else if (key.startsWith("ix_")) {
                 k++;
                 buff.append("and ix").append(k).append(".TASK_INSTANCE_ID=ti.TASK_INSTANCE_ID ");
                 buff.append("and ix").append(k).append(".INDEX_KEY='").append(key.substring(3));
                 buff.append("' and ix").append(k).append(".INDEX_VALUE");
                 buildValueSpec(buff, value);
                 buff.append("\n");
             } else if (key.equals("cuid")) {
                 buff.append("AND ui.CUID='").append(value).append("'\n");
             } else if (key.equals("groups")) {
                 buff.append("and ti.TASK_INSTANCE_ID=tg.TASK_INSTANCE_ID ");
                 buff.append("and tg.USER_GROUP_ID=g.USER_GROUP_ID ");
                 buff.append("and g.GROUP_NAME in (");
                 String[] groups = value.split(",");
                 for (int i=0; i<groups.length; i++) {
                     int s = groups[i].indexOf(':');
                     String groupName = s>0?groups[i].substring(0,s):groups[i];
                     if (i>0) buff.append(",");
                     buff.append("'").append(groupName).append("'");
                 }
                 buff.append(")\n");
             }
         }
         return buff.toString();
    }

    public int countTaskInstancesNew(String fromWhereClause)
    throws DataAccessException {
        StringBuffer buff = new StringBuffer();

        buff.append("select count(distinct ti.TASK_INSTANCE_ID)\n");
        buff.append(fromWhereClause);
        String query = buff.toString();
        if(logger.isMdwDebugEnabled()){
            logger.mdwDebug("queryTaskInstances() Query-->"+query) ;
        }
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(query, null);
            if (rs.next()) {
                return rs.getInt(1);
            } else throw new Exception("Failed to get count result");
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to count task instances", e);
        } finally {
            db.closeConnection();
        }
    }

    public List<TaskInstance> queryTaskInstancesNew(String fromWhereClause,
            int startIndex, int endIndex, String sortOn, boolean loadIndices)
    throws DataAccessException {
        StringBuffer buff = new StringBuffer();
        buff.append(db.pagingQueryPrefix());
        buff.append("select distinct ").append(TASK_INSTANCE_SELECT_SHALLOW);
        // note there are two additional parameters, indices 16 & 17
        buff.append(",ui.CUID\n");
        buff.append(fromWhereClause);
        if (sortOn!=null) {
            boolean desc = false;
            if (sortOn.startsWith("-")) {
                desc = true;
                sortOn = sortOn.substring(1);
            }
            if (sortOn.startsWith("ti_")) {
                buff.append(" order by ti.").append(sortOn.substring(3));
            } else if (sortOn.startsWith("si_")) {
                // TODO should not sort on si.SLA_ESTM_COMP_DT, instead on ti.TASK_END_DT
                buff.append(" order by si.").append(sortOn.substring(3));
            } else {
                buff.append(" order by ti.TASK_INSTANCE_ID");
            }
            // does not select and sort on indices
            if (desc) buff.append(" desc\n");
            else buff.append("\n");
        } else buff.append(" order by ti.TASK_INSTANCE_ID\n");
        buff.append(db.pagingQuerySuffix(startIndex, endIndex-startIndex));
        String query = buff.toString();
        if(logger.isMdwDebugEnabled()){
            logger.mdwDebug("queryTaskInstances() Query-->"+query) ;
        }
        try {
            db.openConnection();
            List<TaskInstance> taskInstances = new ArrayList<TaskInstance>();
            ResultSet rs = db.runSelect(query, null);
            while (rs.next()) {
                TaskInstance taskInst = getTaskInstanceSub(rs, false);
                taskInst.setAssigneeCuid(rs.getString("CUID"));
                taskInstances.add(taskInst);
            }
            if (loadIndices && taskInstances.size()>0) {
                Map<Long,TaskInstance> map = new HashMap<Long,TaskInstance>();
                Map<String,Object> indices;
                buff.setLength(0);
                buff.append("select task_instance_id,index_key,index_value ");
                buff.append("from task_inst_index ");
                buff.append("where task_instance_id in (");
                TaskInstance taskInst;
                Long taskInstId;
                String indexKey, indexValue;
                for (int i=0; i<taskInstances.size(); i++) {
                    if (i>0) buff.append(",");
                    taskInst = taskInstances.get(i);
                    taskInstId = taskInst.getTaskInstanceId();
                    buff.append(taskInstId);
                    map.put(taskInstId, taskInst);
                }
                buff.append(")");
                query = buff.toString();
                rs = db.runSelect(query, null);
                while (rs.next()) {
                    taskInstId = rs.getLong(1);
                    indexKey = rs.getString(2);
                    indexValue = rs.getString(3);
                    taskInst = map.get(taskInstId);
                    indices = taskInst.getVariables();
                    if (indices==null) {
                        indices = new HashMap<String,Object>();
                        taskInst.setVariables(indices);
                    }
                    indices.put(indexKey, indexValue);
                }
            }
            return taskInstances;
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to query task instances", e);
        } finally {
            db.closeConnection();
        }
    }

    private void getTaskInstanceGroups(TaskInstance taskInst) throws SQLException {
        List<String> groups = new ArrayList<String>();
        StringBuffer buff = new StringBuffer();
        buff.append("select g.GROUP_NAME ");
        buff.append("from TASK_INST_GRP_MAPP tigm, USER_GROUP g ");
        buff.append("where tigm.TASK_INSTANCE_ID=? and tigm.USER_GROUP_ID=g.USER_GROUP_ID");
        String query = buff.toString();
        ResultSet rs = db.runSelect(query, taskInst.getTaskInstanceId());
        while (rs.next()) {
            groups.add(rs.getString(1));
        }
        taskInst.setGroups(groups);
    }

    public void getTaskInstanceAdditionalInfoGeneral(TaskInstance taskInst)
        throws DataAccessException {
        StringBuffer buff = new StringBuffer();
        if (db.isMySQL()) {
            buff.append("select ui.CUID\n");
            buff.append("from TASK_INSTANCE ti left join USER_INFO ui\n");
            buff.append("on ui.USER_INFO_ID = ti.TASK_CLAIM_USER_ID " );
            buff.append("where ti.TASK_INSTANCE_ID = ?");
        } else {
            buff.append("select ui.CUID\n");
            buff.append("from TASK_INSTANCE ti, USER_INFO ui\n");
            buff.append("where ui.USER_INFO_ID(+) = ti.TASK_CLAIM_USER_ID AND " );
            buff.append("ti.TASK_INSTANCE_ID = ?");
        }
        String query = buff.toString();
        if(logger.isMdwDebugEnabled()){
            logger.mdwDebug("getTaskInstanceAllInfo() Query-->"+query) ;
        }
        try {
            db.openConnection();
            ResultSet rs = db.runSelect(query, taskInst.getTaskInstanceId());
            if (rs.next()) {
                taskInst.setAssigneeCuid(rs.getString(1));
            }
            // load indices
            Map<String,Object> indices = new HashMap<String,Object>();
            taskInst.setVariables(indices);;
            buff.setLength(0);
            buff.append("select INDEX_KEY,INDEX_VALUE ");
            buff.append("from TASK_INST_INDEX ");
            buff.append("where TASK_INSTANCE_ID=?");
            String indexKey, indexValue;
            query = buff.toString();
            rs = db.runSelect(query, taskInst.getTaskInstanceId());
            while (rs.next()) {
                indexKey = rs.getString(1);
                indexValue = rs.getString(2);
                indices.put(indexKey, indexValue);
                if (indexKey.equals("MASTER_REQUEST_ID"))
                    taskInst.setMasterRequestId(indexValue);
            }
            // load groups
            getTaskInstanceGroups(taskInst);
        } catch (Exception e) {
            throw new DataAccessException(0, "failed to query task instances", e);
        } finally {
            db.closeConnection();
        }
    }

    public Map<String,String> getTaskInstIndices(Long taskInstanceId) throws DataAccessException {
        try {
            Map<String, String> indices = new HashMap<String, String>();
            db.openConnection();
            String sql = "select tii.index_key,tii.index_value from task_inst_index tii where tii.task_instance_id = ?";
            ResultSet rs = db.runSelect(sql, taskInstanceId);
            while (rs.next()) {
                indices.put(rs.getString(1), rs.getString(2));
            }
            return indices;
        }
        catch (Exception ex) {
            logger.severeException("Failed to get Task Instance Indices", ex);
            // throw new DataAccessException(-1, "Failed to get Task Instance
            // Indices", ex);
        }
        finally {
            db.closeConnection();
        }
        return null;
    }

    /**
     * Only for 5.5+ VCS Assets.
     */
    public TaskList getTaskInstances(Query query) throws DataAccessException {
        long start = System.currentTimeMillis();
        StringBuilder sql = new StringBuilder();
        if (query.getMax() != -1)
            sql.append(db.pagingQueryPrefix());

        sql.append("select ").append(TASK_INSTANCE_SELECT).append("\n");
        StringBuilder countSql = new StringBuilder();
        countSql.append("select count(ti.task_instance_id)\n");

        if (db.isMySQL()) {
            sql.append("from task_instance ti left join user_info ui on ui.user_info_id = ti.task_claim_user_id\n");
            countSql.append("from task_instance ti left join user_info ui on ui.user_info_id = ti.task_claim_user_id\n");
        }
        else {
            sql.append("from task_instance ti, user_info ui\n");
            countSql.append("from task_instance ti, user_info ui\n");
        }

        String[] workgroups = query.getArrayFilter("workgroups");
        if (workgroups != null && workgroups.length > 0 && !containsSiteAdmin(workgroups)) {
            String tigm = ", task_inst_grp_mapp tigm ";
            sql.append(tigm);
            countSql.append(tigm);
        }
        sql.append("\n");

        String where;
        if (query.getFind() != null) {
            try {
                // numeric value means instance id
                long findInstId = Long.parseLong(query.getFind());
                where = "where ti.task_instance_id like '" + findInstId + "%'\n";
            }
            catch (NumberFormatException ex) {
                // otherwise master request id
                where = "where ti.master_request_id like '" + query.getFind() + "%'\n";
            }
        }
        else {
            where = buildTaskInstanceWhere(query);
        }

        if(!StringHelper.isEmpty(where)) {
            sql.append(where);
            countSql.append(where);
        }

        String orderBy = buildTaskInstanceOrderBy(query);
        if(!StringHelper.isEmpty(orderBy))
            sql.append(orderBy);

        try {
            Long total = 0L;
            db.openConnection();
            ResultSet rs = db.runSelect(countSql.toString(), null);
            if (rs.next())
                total = rs.getLong(1);

            if (query.getMax() != -1)
              sql.append(db.pagingQuerySuffix(query.getStart(), query.getMax()));

            if(logger.isDebugEnabled())
                logger.mdwDebug("queryTaskInstances() Query-->"+query) ;

            List<TaskInstance> taskInstances = new ArrayList<TaskInstance>();
            rs = db.runSelect(sql.toString(), null);
            while (rs.next()) {
                TaskInstance taskInst = getTaskInstanceSub(rs, true, null, null);
                if (taskInst != null) {
                    if (taskInst.getAssigneeCuid() != null) {
                        try {
                            User user = UserGroupCache.getUser(taskInst.getAssigneeCuid());
                            if (user == null)
                                throw new CachingException("Unable to lookup assignee: " + taskInst.getAssigneeCuid());
                            taskInst.setAssignee(user.getName());
                        }
                        catch (CachingException ex) {
                            logger.severeException("Cannot find assignee: " + taskInst.getAssigneeCuid(), ex);
                        }
                    }
                    taskInstances.add(taskInst);
                }
            }
            TaskList taskList = new TaskList(TaskList.TASKS, taskInstances);
            taskList.setTotal(total);
            taskList.setRetrieveDate(DatabaseAccess.getDbDate());
            return taskList;
        }
        catch (SQLException e) {
            throw new DataAccessException(500, "Failed to query task instances", e);
        }
        finally {
            db.closeConnection();
            if (logger.isMdwDebugEnabled()) {
              long elapsed = System.currentTimeMillis() - start;
              logger.mdwDebug("queryTaskInstances() Elapsed-->" + elapsed + " ms");
            }
        }

    }

    private String buildTaskInstanceWhere(Query query) throws DataAccessException {

        StringBuilder where = new StringBuilder();
        if (db.isMySQL())
            where.append("where 1=1\n");
        else
            where.append("where ui.user_info_id(+) = ti.task_claim_user_id\n");

        // taskId
        String taskId = query.getFilter("taskId");
        if (taskId != null) {
            where.append(" and ti.task_id = ").append(taskId).append("\n");
        }
        // workgroups
        String[] workgroups = query.getArrayFilter("workgroups");
        if (workgroups != null && workgroups.length > 0 && !containsSiteAdmin(workgroups)) {
            where.append(" and tigm.task_instance_id = ti.task_instance_id").append("\n");
            where.append(" and tigm.user_group_id in (");
            for (int i = 0; i < workgroups.length; i++) {
                try {
                    if (!Workgroup.COMMON_GROUP.equals(workgroups[i])) {
                        Workgroup group = UserGroupCache.getWorkgroup(workgroups[i]);
                        if (group == null)
                            throw new CachingException("Cannot find workgroup: " + workgroups[i]);
                        where.append(group.getId());
                        if (i < workgroups.length - 1)
                          where.append(",");
                    }
                }
                catch (CachingException ex) {
                    // just log this
                    logger.severeException("Failed to lookup workgroup: " + workgroups[i], ex);
                }
            }
            where.append(")\n");
        }
        // instanceId or masterRequestId
        long instanceId = query.getLongFilter("instanceId");
        if (instanceId > 0) {
            where.append("and ti.task_instance_id = " + instanceId + "\n");
            return where.toString(); // ignore other criteria
        }
        String masterRequestId = query.getFilter("masterRequestId");
        if (masterRequestId != null) {
            where.append("and ti.master_request_id = '" + masterRequestId + "'\n");
            return where.toString(); // ignore other criteria
        }

        // assignee
        String assignee = query.getFilter("assignee");
        if (assignee != null && !assignee.isEmpty()) {
            if (assignee.equals("[Unassigned]"))
                where.append(" and ti.task_claim_user_id is null\n");
            else
                where.append(" and ui.cuid = '" + assignee + "'\n");
        }
        // startDate
        try {
            Date startDate = query.getDateFilter("startDate");
            if (startDate != null) {
                String start = getDateFormat().format(startDate);
                if (db.isMySQL())
                    where.append(" and ti.task_start_dt >= STR_TO_DATE('").append(start).append("','%d-%M-%Y')\n");
                else
                    where.append(" and ti.task_start_dt >= '").append(start).append("'\n");
            }
        }
        catch (ParseException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        // status
        String status = query.getFilter("status");
        if (status != null) {
            if (status.equals(TaskStatus.STATUSNAME_ACTIVE)) {
                where.append(" and ti.task_instance_status not in (")
                  .append(TaskStatus.STATUS_COMPLETED)
                  .append(",").append(TaskStatus.STATUS_CANCELLED)
                  .append(")\n");
            }
            else if (status.equals(TaskStatus.STATUSNAME_CLOSED)) {
                where.append(" and ti.task_instance_status in (")
                  .append(TaskStatus.STATUS_COMPLETED)
                  .append(",").append(TaskStatus.STATUS_CANCELLED)
                  .append(")\n");
            }
            else {
                Long statusCode = getTaskStatusCode(status);
                if (statusCode == null)
                    throw new DataAccessException("Unable to find code for status: " + status);
                where.append(" and ti.task_instance_status = ").append(statusCode).append("\n");
            }
        }
        // state
        String advisory = query.getFilter("advisory");
        if (advisory != null) {
            if (advisory.equals(TaskState.STATE_NOT_INVALID)) {
                where.append(" and ti.task_instance_state != " + TaskState.STATE_INVALID).append("\n");
            }
            else {
                Long stateCode = getTaskStateCode(advisory);
                if (stateCode == null)
                    throw new DataAccessException("Unable to find code for task advisory: " + advisory);
                where.append(" and ti.task_instance_state = ").append(stateCode).append("\n");
            }
        }
        // category
        String category = query.getFilter("category");
        if (category != null) {
            Long categoryCode = getCategoryId(category);
            if (categoryCode == null)
                throw new DataAccessException("Unable to find code for category: " + category);
            String catTasksClause = buildCategoryTasksClause((int)categoryCode.longValue());
            if (catTasksClause != null)
                where.append(" and ").append(catTasksClause).append("\n");
        }
        // owner and ownerId
        String owner = query.getFilter("owner");
        long ownerId = query.getLongFilter("ownerId");
        if (owner != null)
            where.append(" and ti.task_instance_owner = '").append(owner).append("'\n");
        if (ownerId > 0)
            where.append(" and ti.task_instance_owner_id = ").append(ownerId).append("\n");
        if (owner == null && ownerId <= 0) {
            Long[] processInstanceIds = query.getLongArrayFilter("processInstanceIds");
            if (processInstanceIds != null && processInstanceIds.length > 0) {
                where.append(" and ti.task_instance_owner = '").append(OwnerType.PROCESS_INSTANCE ).append("'\n");
                where.append(" and ti.task_instance_owner_id in (");
                for (int i = 0; i < processInstanceIds.length; i++) {
                    where.append(processInstanceIds[i]);
                    if (i < processInstanceIds.length - 1)
                        where.append(", ");
                }
                where.append(")\n");
            }
        }

        // if sort by due date, exclude those tasks without
        if ("dueDate".equals(query.getSort()))
            where.append(" and ti.due_date is not null\n");

        // TODO: Indexes, Due Date?, End Date?

        return where.toString();
    }

    private String buildTaskInstanceOrderBy(Query query) throws DataAccessException {
        StringBuilder sb = new StringBuilder();
        String sort = query.getSort();
        if ("dueDate".equals(sort))
            sb.append(" order by ti.due_date");
        else
            sb.append(" order by ti.task_instance_id");
        if (query.isDescending())
            sb.append(" desc");
        sb.append(", ti.task_id\n");
        return sb.toString();
    }

    protected Long getTaskStatusCode(String statusName) throws DataAccessException {
        if (statusName != null) {
            for (TaskStatus taskStatus : getAllTaskStatuses()) {
                if (statusName.equals(taskStatus.getName())) {
                    return taskStatus.getCode();
                }
            }
        }
        return null;
    }

    protected Long getTaskStateCode(String stateName) throws DataAccessException {
        if (stateName != null) {
            for (TaskState taskState : getAllTaskStates()) {
                if (stateName.equals(taskState.getName())) {
                    return taskState.getCode();
                }
            }
        }
        return null;
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

    private static DateFormat dateFormat;
    protected static DateFormat getDateFormat() {
        if (dateFormat == null)
            dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        return dateFormat;
    }

    public String getCategoryCode(int categoryId) throws DataAccessException {
        return DataAccess.getBaselineData().getTaskCategoryCodes().get(categoryId);
    }

    protected Long getCategoryId(String categoryName) throws DataAccessException {
        if (categoryName != null) {
            for (TaskCategory taskCategory : DataAccess.getBaselineData().getTaskCategories().values()) {
                if (categoryName.equals(taskCategory.getName())) {
                    return taskCategory.getId();
                }
            }
        }
        return null;
    }


}
