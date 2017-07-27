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
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskState;
import com.centurylink.mdw.model.task.TaskStatus;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Task-related data access.
 */
public class TaskDataAccess extends CommonDataAccess {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static String TASK_INSTANCE_SELECT_SHALLOW =
        " ti.TASK_INSTANCE_ID, " +
        " ti.TASK_ID, " +
        " ti.TASK_INSTANCE_STATUS, " +
        " ti.TASK_INSTANCE_OWNER, " +
        " ti.TASK_INSTANCE_OWNER_ID, " +
        " ti.TASK_INST_SECONDARY_OWNER, " +
        " ti.TASK_INST_SECONDARY_OWNER_ID, " +
        " ti.TASK_CLAIM_USER_ID, " +
        " ti.TASK_START_DT, " +
        " ti.TASK_END_DT, " +
        " ti.COMMENTS, " +
        " ti.TASK_INSTANCE_STATE, " +
        " ti.TASK_INSTANCE_REFERRED_AS, " +
        " ti.DUE_DATE, " +
        " ti.PRIORITY, " +
        " ti.MASTER_REQUEST_ID";

    private static String TASK_INSTANCE_SELECT;
    private static boolean hasTaskTitleColumn;

    private static String TASK_INSTANCE_FROM =
        "TASK_INSTANCE ti, USER_INFO ui ";

    private String getTaskInstanceSelect() throws SQLException {
        return getTaskInstanceSelect(false);
    }
    private String getTaskInstanceSelect(boolean deep) throws SQLException {
        if (TASK_INSTANCE_SELECT == null) {
            // need to check if task_title supported
            String q;
            if (db.isMySQL())
                q = "SHOW COLUMNS FROM `task_instance` LIKE 'task_title'";
            else
                q = "select column_name from all_tab_columns where table_name='TASK_INSTANCE' AND column_name='TASK_TITLE'";
            if (db.runSelect(q, null).next()) {
                hasTaskTitleColumn = true;
                TASK_INSTANCE_SELECT_SHALLOW += ", TI.TASK_TITLE";
            }
            TASK_INSTANCE_SELECT = "distinct " + TASK_INSTANCE_SELECT_SHALLOW + ", " +
                    " ti.TASK_INSTANCE_OWNER_ID as PROCESS_INSTANCE_ID, ui.CUID, ui.NAME as USER_NAME";
        }
        return deep ? TASK_INSTANCE_SELECT : TASK_INSTANCE_SELECT_SHALLOW;
    }

    public TaskDataAccess() {
        this(new DatabaseAccess(null));
    }

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
     * Returns with state=Invalid for task instance exists whose definition no longer exists (can happen
     * if previous task definitions were not archived properly).
     */
    private TaskInstance getTaskInstanceSub(ResultSet rs, boolean isVOversion) throws SQLException {

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
        if (hasTaskTitleColumn)
            task.setTitle(rs.getString("TASK_TITLE"));
        if (isVOversion) {
            task.setAssigneeCuid(rs.getString("CUID"));
            if (taskVO != null)
              task.setDescription(taskVO.getComment());
        }
        return task;
    }

    public TaskInstance getTaskInstance(Long taskInstId) throws DataAccessException {
        try {
            db.openConnection();
            StringBuilder query = new StringBuilder();
            query.append("select ").append(getTaskInstanceSelect()).append(", GROUP_NAME\n");
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
            String query = "select " + getTaskInstanceSelect() +
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

    public TaskInstance getTaskInstanceByActivityInstanceId(Long activityInstanceId)
        throws DataAccessException {
        try {
            db.openConnection();
            StringBuffer sql = new StringBuffer("select ");
            sql.append(getTaskInstanceSelect());
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
            Long id = db.isMySQL() ? null : this.getNextId("MDW_COMMON_INST_ID_SEQ");
            String query = "insert into TASK_INSTANCE " +
                "(TASK_INSTANCE_ID, TASK_ID, TASK_INSTANCE_STATUS, " +
                " TASK_INSTANCE_OWNER, TASK_INSTANCE_OWNER_ID, TASK_CLAIM_USER_ID, COMMENTS, " +
                " TASK_START_DT, TASK_END_DT, TASK_INSTANCE_STATE, " +
                " TASK_INST_SECONDARY_OWNER, TASK_INST_SECONDARY_OWNER_ID, " +
                " TASK_INSTANCE_REFERRED_AS, DUE_DATE, PRIORITY, MASTER_REQUEST_ID, " +
                " CREATE_DT, CREATE_USR";
            if (taskInst.getTitle() != null)
                query += ", TASK_TITLE";
            query += ") values (?, ?, ?, ?, ?, ?, ?, " + now() + ", ?, ?, ?, ?, ?, ?, ?, ?, " + now() + ", 'mdw'";
            if (taskInst.getTitle() != null)
                query += ", ?";
            query += ")";
            Object[] args = taskInst.getTitle() == null ? new Object[15] : new Object[16];
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
            if (taskInst.getTitle() != null)
              args[15] = taskInst.getTitle();
            if (db.isMySQL())
                id = db.runInsertReturnId(query, args);
            else
                db.runUpdate(query, args);
            db.commit();
            return id;
        } catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to create task instance", e);
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
            query.append(getTaskInstanceSelect());
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
        try {
            StringBuffer buff = new StringBuffer();
            db.openConnection();
            buff.append("select ").append(getTaskInstanceSelect(true));
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

    public List<TaskStatus> getAllTaskStatuses() throws DataAccessException {
        return DataAccess.getBaselineData().getAllTaskStatuses();
    }

    public List<TaskState> getAllTaskStates() throws DataAccessException {
        return DataAccess.getBaselineData().getAllTaskStates();
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

    public TaskList getTaskInstances(Query query) throws DataAccessException {
        long start = System.currentTimeMillis();
        try {
            StringBuilder sql = new StringBuilder();
            if (query.getMax() != -1)
                sql.append(db.pagingQueryPrefix());

            db.openConnection();
            sql.append("select ").append(getTaskInstanceSelect(true)).append("\n");
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

            if (!StringHelper.isEmpty(where)) {
                sql.append(where);
                countSql.append(where);
            }

            String orderBy = buildTaskInstanceOrderBy(query);
            if (!StringHelper.isEmpty(orderBy))
                sql.append(orderBy);

            Long total = 0L;
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
                TaskInstance taskInst = getTaskInstanceSub(rs, true);
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
