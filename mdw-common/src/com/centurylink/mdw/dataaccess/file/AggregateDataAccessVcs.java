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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.request.RequestCount;
import com.centurylink.mdw.model.task.TaskCount;
import com.centurylink.mdw.model.task.TaskStatuses;
import com.centurylink.mdw.model.workflow.ActivityCount;
import com.centurylink.mdw.model.workflow.ProcessCount;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;

/**
 * For MDW 6.2 we'll factor an interface from this.
 */
public class AggregateDataAccessVcs extends CommonDataAccess {

    public List<ProcessCount> getTopThroughputProcessInstances(Query query) throws DataAccessException {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(pi.process_id) as ct, pi.process_id\n");
            sql.append("from (select process_id from PROCESS_INSTANCE ");
            sql.append(getProcessWhereClause(query));
            sql.append(") pi\n");
            sql.append("group by process_id\n");
            sql.append("order by ct desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            List<ProcessCount> list = new ArrayList<ProcessCount>();
            int idx = 0;
            int limit = query.getIntFilter("limit");
            while (rs.next() && (limit == -1 || idx < limit)) {
                ProcessCount procCount = new ProcessCount(rs.getLong("ct"));
                procCount.setId(rs.getLong("process_id"));
                list.add(procCount);
                idx++;
            }
            return list;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Map<Date,List<ProcessCount>> getProcessInstanceBreakdown(Query query) throws DataAccessException {
        try {
            // process ids
            Long[] processIdsArr = query.getLongArrayFilter("processIds");
            List<Long> processIds = processIdsArr == null ? null : Arrays.asList(processIdsArr);
            // by status
            String[] statuses = query.getArrayFilter("statuses");
            List<Integer> statusCodes = null;
            if (statuses != null) {
                statusCodes = new ArrayList<Integer>();
                for (String status : statuses)
                    statusCodes.add(WorkStatuses.getCode(status));
            }
            if (processIds != null && statuses != null)
                throw new DataAccessException("Conflicting parameters: processIds and statuses");

            StringBuilder sql = new StringBuilder();
            if (statusCodes != null)
                sql.append("select count(pi.status_cd) as ct, pi.st, pi.status_cd\n");
            else if (processIds != null)
                sql.append("select count(pi.process_id) as ct, pi.st, pi.process_id\n");
            else
                sql.append("select count(pi.st) as ct, pi.st\n");

            if (db.isMySQL())
                sql.append("from (select DATE_FORMAT(start_dt,'%d-%M-%Y') as st");
            else
                sql.append("from (select to_char(start_dt,'DD-Mon-yyyy') as st");
            if (statusCodes != null)
                sql.append(", status_cd ");
            else if (processIds != null)
                sql.append(", process_id ");
            sql.append("  from PROCESS_INSTANCE\n   ");
            sql.append(getProcessWhereClause(query));
            if (statusCodes != null)
                sql.append("\n   and status_cd ").append(getInCondition(statusCodes));
            else if (processIds != null)
                sql.append("\n   and process_id ").append(getInCondition(processIds));
            sql.append(") pi\n");

            sql.append("group by st");
            if (statusCodes != null)
                sql.append(", status_cd");
            else if (processIds != null)
                sql.append(", process_id");
            if (db.isMySQL())
                sql.append("\norder by STR_TO_DATE(st, '%d-%M-%Y') desc\n");
            else
                sql.append("\norder by to_date(st, 'DD-Mon-yyyy') desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString(), null);
            Map<Date,List<ProcessCount>> map = new HashMap<Date,List<ProcessCount>>();
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                List<ProcessCount> processCounts = map.get(startDate);
                if (processCounts == null) {
                    processCounts = new ArrayList<ProcessCount>();
                    map.put(startDate, processCounts);
                }
                ProcessCount processCount = new ProcessCount(rs.getLong("ct"));
                if (statusCodes != null)
                    processCount.setStatus(WorkStatuses.getName(rs.getInt("status_cd")));
                else if (processIds != null)
                    processCount.setId(rs.getLong("process_id"));
                processCounts.add(processCount);
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    protected String getProcessWhereClause(Query query) throws ParseException, DataAccessException {
        Date start = query.getDateFilter("startDate");
        if (start == null)
            throw new DataAccessException("Parameter startDate is required");
        String startStr = getDateFormat().format(start);


        StringBuilder where = new StringBuilder();
        if (db.isMySQL())
            where.append("where start_dt >= STR_TO_DATE('" + startStr + "','%d-%M-%Y') ");
        else
            where.append("where start_dt >= '" + startStr + "' ");
        where.append("and owner not in ('MAIN_PROCESS_INSTANCE' ");
        if (query.getBooleanFilter("master"))
            where.append(", 'PROCESS_INSTANCE' ");
        where.append(") ");
        String status = query.getFilter("status");
        if (status != null)
            where.append("and STATUS_CD = " + WorkStatuses.getCode(status));
        return where.toString();
    }

    /**
     * Useful for inferring process name and version.
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

    public List<TaskCount> getTopTasks(Query query) throws DataAccessException {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(tii.task_id) as ct, tii.task_id\n");
            sql.append("from (select task_id from TASK_INSTANCE ti ");
            sql.append(getTaskWhereClause(query));
            sql.append(") tii\n");
            sql.append("group by task_id\n");
            sql.append("order by ct desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            List<TaskCount> list = new ArrayList<TaskCount>();
            int idx = 0;
            int limit = query.getIntFilter("limit");
            while (rs.next() && (limit == -1 || idx < limit)) {
                TaskCount taskCount = new TaskCount(rs.getLong("ct"));
                taskCount.setId(rs.getLong("task_id"));
                list.add(taskCount);
                idx++;
            }
            return list;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<TaskCount> getTopTaskWorkgroups(Query query) throws DataAccessException {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(tii.group_name) as ct, tii.group_name\n");
            sql.append("from (select ug.group_name from TASK_INST_GRP_MAPP tigm, TASK_INSTANCE ti, USER_GROUP ug\n   ");
            sql.append(getTaskWhereClause(query));
            sql.append("and tigm.task_instance_id = ti.task_instance_id\n   ");
            sql.append("and tigm.user_group_id = ug.user_group_id\n   ");
            sql.append(") tii\n");
            sql.append("group by group_name\n");
            sql.append("order by ct desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            List<TaskCount> list = new ArrayList<TaskCount>();
            int idx = 0;
            int limit = query.getIntFilter("limit");
            while (rs.next() && (limit == -1 || idx < limit)) {
                TaskCount taskCount = new TaskCount(rs.getLong("ct"));
                taskCount.setWorkgroup(rs.getString("group_name"));
                list.add(taskCount);
                idx++;
            }
            return list;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<TaskCount> getTopTaskAssignees(Query query) throws DataAccessException {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(tii.cuid) as ct, tii.cuid, tii.name\n");
            sql.append("from (select ui.cuid, ui.name from TASK_INSTANCE ti, USER_INFO ui\n   ");
            sql.append(getTaskWhereClause(query));
            sql.append("and ti.task_claim_user_id = ui.user_info_id\n   ");
            sql.append(") tii\n");
            sql.append("group by cuid, name\n");
            sql.append("order by ct desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            List<TaskCount> list = new ArrayList<TaskCount>();
            int idx = 0;
            int limit = query.getIntFilter("limit");
            while (rs.next() && (limit == -1 || idx < limit)) {
                TaskCount taskCount = new TaskCount(rs.getLong("ct"));
                taskCount.setUserId(rs.getString("cuid"));
                taskCount.setUserName(rs.getString("name"));
                list.add(taskCount);
                idx++;
            }
            return list;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Map<Date,List<TaskCount>> getTaskInstanceBreakdown(Query query) throws DataAccessException {
        try {
            List<String> params = new ArrayList<String>();
            // tasks
            List<Long> taskIds = null;
            Long[] taskIdsArr = query.getLongArrayFilter("taskIds");
            if (taskIdsArr != null) {
                taskIds = Arrays.asList(taskIdsArr);
                params.add("taskIds");
            }
            // workgroups
            List<String> workgroups = null;
            String[] workgroupsArr = query.getArrayFilter("workgroups");
            if (workgroupsArr != null) {
                workgroups = Arrays.asList(workgroupsArr);
                params.add("workgroups");
            }
            // assignees
            List<String> assignees = null;
            String[] assigneesArr = query.getArrayFilter("assignees");
            if (assigneesArr != null) {
                assignees = Arrays.asList(assigneesArr);
                params.add("assignees");
            }
            // assignees
            List<Integer> statusCodes = null;
            String[] statusesArr = query.getArrayFilter("statuses");
            if (statusesArr != null) {
                statusCodes = new ArrayList<Integer>();
                for (String status : statusesArr)
                    statusCodes.add(TaskStatuses.getCode(status));
                params.add("statuses");
            }
            if (params.size() > 1)
                throw new DataAccessException("Conflicting parameters: " + Arrays.toString(params.toArray()));

            StringBuilder sql = new StringBuilder();
            if (taskIds != null)
                sql.append("select count(tii.task_id) as ct, tii.st, tii.task_id\n");
            else if (workgroups != null)
                sql.append("select count(tii.group_name) as ct, tii.st, tii.group_name\n");
            else if (assignees != null)
                sql.append("select count(tii.cuid) as ct, tii.st, tii.cuid, tii.name\n");
            else if (statusCodes != null)
                sql.append("select count(tii.task_instance_status) as ct, tii.st, tii.task_instance_status\n");
            else
                sql.append("select count(tii.st) as ct, tii.st\n");

            if (db.isMySQL())
                sql.append("from (select DATE_FORMAT(ti.create_dt,'%d-%M-%Y') as st");
            else
                sql.append("from (select to_char(ti.create_dt,'DD-Mon-yyyy') as st");
            if (taskIds != null)
                sql.append(", ti.task_id ");
            else if (workgroups != null)
                sql.append(", ug.group_name ");
            else if (assignees != null)
                sql.append(", ui.cuid, ui.name ");
            else if (statusCodes != null)
                sql.append(", ti.task_instance_status ");
            sql.append("\n   from TASK_INSTANCE ti ");
            if (workgroups != null)
                sql.append(", TASK_INST_GRP_MAPP tigm, USER_GROUP ug ");
            else if (assignees != null)
                sql.append(", USER_INFO ui ");
            sql.append("\n   ");
            sql.append(getTaskWhereClause(query));
            if (taskIds != null)
                sql.append("and ti.task_id ").append(getInCondition(taskIds));
            else if (workgroups != null) {
                sql.append("and tigm.task_instance_id = ti.task_instance_id\n   ");
                sql.append("and tigm.user_group_id = ug.user_group_id\n   ");
                sql.append("and ug.group_name ").append(getInCondition(workgroups));
            }
            else if (assignees != null) {
                sql.append("and ti.task_claim_user_id = ui.user_info_id\n   ");
                sql.append("and ui.cuid ").append(getInCondition(assignees));
            }
            else if (statusCodes != null) {
                sql.append("and ti.task_instance_status ").append(getInCondition(statusCodes));
            }
            sql.append(") tii\n");
            sql.append("group by st");
            if (taskIds != null)
                sql.append(", task_id ");
            else if (workgroups != null)
                sql.append(", group_name ");
            else if (assignees != null)
                sql.append(", cuid, name ");
            else if (statusCodes != null)
                sql.append(", task_instance_status ");
            if (db.isMySQL())
                sql.append("\norder by STR_TO_DATE(st, '%d-%M-%Y') desc\n");
            else
                sql.append("\norder by to_date(st, 'DD-Mon-yyyy') desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            Map<Date,List<TaskCount>> map = new HashMap<Date,List<TaskCount>>();
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                List<TaskCount> taskCounts = map.get(startDate);
                if (taskCounts == null) {
                    taskCounts = new ArrayList<TaskCount>();
                    map.put(startDate, taskCounts);
                }
                TaskCount taskCount = new TaskCount(rs.getLong("ct"));
                if (taskIds != null) {
                    taskCount.setId(rs.getLong("task_id"));
                }
                else if (workgroups != null) {
                    taskCount.setWorkgroup(rs.getString("group_name"));
                }
                else if (assignees != null) {
                    taskCount.setUserId(rs.getString("cuid"));
                    taskCount.setUserName(rs.getString("name"));
                }
                else if (statusCodes != null) {
                    taskCount.setStatus(TaskStatuses.getName(rs.getInt("task_instance_status")));
                }
                taskCounts.add(taskCount);
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    protected String getTaskWhereClause(Query query) throws ParseException, DataAccessException {
        Date start = query.getDateFilter("startDate");
        if (start == null)
            throw new DataAccessException("Parameter startDate is required");
        String startStr = getDateFormat().format(start);
        StringBuilder where = new StringBuilder();
        if (db.isMySQL())
            where.append("where ti.create_dt >= STR_TO_DATE('" + startStr + "','%d-%M-%Y')\n   ");
        else
            where.append("where ti.create_dt >= '" + startStr + "'\n   ");

        return where.toString();
    }

    protected <T> String getInCondition(List<T> elements) {
        StringBuilder in = new StringBuilder();
        in.append("in (");
        if (elements.isEmpty()) {
            in.append("''");  // no match -- avoid malformed sql
        }
        else {
            for (int i = 0; i < elements.size(); i++) {
                T e = elements.get(i);
                String tic = e instanceof String ? "'" : "";
                in.append(tic).append(e).append(tic);
                if (i < elements.size() - 1)
                    in.append(",");
            }
        }
        in.append(") ");
        return in.toString();
    }

    /**
     * Useful for inferring process name and version.
     */
    public String getLatestTaskInstanceComments(Long taskId) throws DataAccessException {
        StringBuilder query = new StringBuilder();
        query.append("select task_instance_id, comments from TASK_INSTANCE\n");
        query.append("where task_instance_id = (select max(task_instance_id) from TASK_INSTANCE ");
        query.append("where task_id = ? and comments is not null)");

        try {
            db.openConnection();
            ResultSet rs = db.runSelect(query.toString(), taskId);
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

    public List<ActivityCount> getTopThroughputActivityInstances(Query query) throws DataAccessException {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(act_unique_id) as ct, act_unique_id\n");
            if (db.isMySQL())
                sql.append("from (select CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as ACT_UNIQUE_ID from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi ");
            else
                sql.append("from (select pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as ACT_UNIQUE_ID from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi ");
            sql.append(getActivityWhereClause(query));
            sql.append(") a1\n");
            sql.append("group by act_unique_id\n");
            sql.append("order by ct desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            List<ActivityCount> list = new ArrayList<ActivityCount>();
            int idx = 0;
            int limit = query.getIntFilter("limit");
            while (rs.next() && (limit == -1 || idx < limit)) {
                ActivityCount actCount = new ActivityCount(rs.getLong("ct"));
                String actId = rs.getString("act_unique_id");
                actCount.setActivityId(actId);
                int colon = actId.lastIndexOf(":");
                actCount.setProcessId(new Long(actId.substring(0, colon)));
                actCount.setDefinitionId(actId.substring(colon + 1));
                list.add(actCount);
                idx++;
            }
            return list;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    protected String getActivityWhereClause(Query query) throws ParseException, DataAccessException {
        Date start = query.getDateFilter("startDate");
        if (start == null)
            throw new DataAccessException("Parameter startDate is required");
        String startStr = getDateFormat().format(start);

        StringBuilder where = new StringBuilder();

        where.append(" where ai.process_instance_id=pi.PROCESS_INSTANCE_ID");

        if (db.isMySQL())
            where.append(" AND ai.start_dt >= STR_TO_DATE('" + startStr + "','%d-%M-%Y') ");
        else
            where.append(" AND ai.start_dt >= '" + startStr + "' ");

        where.append(" AND pi.STATUS_CD NOT IN (" +  WorkStatus.STATUS_COMPLETED.intValue() + "," + WorkStatus.STATUS_CANCELLED.intValue() + "," + WorkStatus.STATUS_PURGE.intValue() + ")");

        if (query.getArrayFilter("statuses") == null)
            where.append(" AND ai.STATUS_CD IN (" +  WorkStatus.STATUS_FAILED.intValue() + "," + WorkStatus.STATUS_WAITING.intValue() + "," + WorkStatus.STATUS_IN_PROGRESS.intValue() + "," + WorkStatus.STATUS_HOLD.intValue() + ")");

        return where.toString();
    }

    public Map<Date,List<ActivityCount>> getActivityInstanceBreakdown(Query query) throws DataAccessException {
        try {
            // activity ids (processid:logicalId)
            String[] actIdsArr = query.getArrayFilter("activityIds");
            List<String> actIds = actIdsArr == null ? null : Arrays.asList(actIdsArr);
            // by status
            String[] statuses = query.getArrayFilter("statuses");
            List<Integer> statusCodes = null;
            if (statuses != null) {
                statusCodes = new ArrayList<Integer>();
                for (String status : statuses)
                    statusCodes.add(WorkStatuses.getCode(status));
            }
            if (actIds != null && statuses != null)
                throw new DataAccessException("Conflicting parameters: activityIds and statuses");

            StringBuilder sql = new StringBuilder();
            if (statusCodes != null)
                sql.append("select count(a.status_cd) as ct, a.st, a.status_cd\n");
            else if (actIds != null)
                sql.append("select count(a.act_unique_id) as ct, a.st, a.act_unique_id\n");
            else
                sql.append("select count(a.st) as ct, a.st\n");

            if (db.isMySQL())
                sql.append("from (select DATE_FORMAT(ai.start_dt,'%d-%M-%Y') as st");
            else
                sql.append("from (select to_char(ai.start_dt,'DD-Mon-yyyy') as st");
            if (statusCodes != null)
                sql.append(", ai.status_cd ");
            else if (actIds != null) {
                if (db.isMySQL())
                    sql.append(", CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as act_unique_id");
                else
                    sql.append(", pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as act_unique_id");
            }
            sql.append("  from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi\n   ");
            sql.append(getActivityWhereClause(query));
            if (statusCodes != null)
                sql.append("\n  and ai.status_cd ").append(getInCondition(statusCodes)).append(") a\n");
            else if (actIds != null)
                sql.append("\n ) a  where act_unique_id ").append(getInCondition(actIds));
            else
                sql.append("\n ) a");

            sql.append(" group by st");
            if (statusCodes != null)
                sql.append(", status_cd");
            else if (actIds != null)
                sql.append(", act_unique_id");
            if (db.isMySQL())
                sql.append("\norder by STR_TO_DATE(st, '%d-%M-%Y') desc\n");
            else
                sql.append("\norder by to_date(st, 'DD-Mon-yyyy') desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            Map<Date,List<ActivityCount>> map = new HashMap<Date,List<ActivityCount>>();
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                List<ActivityCount> actCounts = map.get(startDate);
                if (actCounts == null) {
                    actCounts = new ArrayList<ActivityCount>();
                    map.put(startDate, actCounts);
                }
                ActivityCount actCount = new ActivityCount(rs.getLong("ct"));
                if (statusCodes != null)
                    actCount.setStatus(WorkStatuses.getName(rs.getInt("status_cd")));
                else if (actIds != null) {
                    String actId = rs.getString("act_unique_id");
                    actCount.setActivityId(actId);
                    int colon = actId.lastIndexOf(":");
                    actCount.setProcessId(new Long(actId.substring(0, colon)));
                    actCount.setDefinitionId(actId.substring(colon + 1));
                }
                actCounts.add(actCount);
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Map<Date,List<RequestCount>> getRequestBreakdown(Query query) throws DataAccessException {
        try {
            // request types
            List<String> ownerTypes = null;
            String[] requests = query.getArrayFilter("requests");
            if (requests != null) {
                ownerTypes = new ArrayList<String>();
                for (String request : requests) {
                    if ("Inbound Requests".equals(request))
                        ownerTypes.add(OwnerType.LISTENER_REQUEST);
                    else if ("Outbound Requests".equals(request))
                        ownerTypes.add(OwnerType.ADAPTER_REQUEST);
                }
            }

            StringBuilder sql = new StringBuilder();
            if (ownerTypes != null)
                sql.append("select count(d.owner_type) as ct, d.created, d.owner_type\n");
            else
                sql.append("select count(d.created) as ct, d.created\n");

            if (db.isMySQL())
                sql.append("from (select DATE_FORMAT(create_dt,'%d-%M-%Y') as created");
            else
                sql.append("from (select to_char(create_dt,'DD-Mon-yyyy') as created");
            if (ownerTypes != null)
                sql.append(", owner_type ");
            sql.append("  from DOCUMENT\n");
            sql.append(getRequestWhereClause(query));
            if (ownerTypes != null)
                sql.append("\n   and owner_type ").append(getInCondition(ownerTypes));
            sql.append(") d\n");

            sql.append("group by created");
            if (ownerTypes != null)
                sql.append(", owner_type");
            if (db.isMySQL())
                sql.append("\norder by STR_TO_DATE(created, '%d-%M-%Y') desc\n");
            else
                sql.append("\norder by to_date(created, 'DD-Mon-yyyy') desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            Map<Date,List<RequestCount>> map = new HashMap<Date,List<RequestCount>>();
            while (rs.next()) {
                String createDtStr = rs.getString("created");
                Date createDate = getDateFormat().parse(createDtStr);
                List<RequestCount> requestCounts = map.get(createDate);
                if (requestCounts == null) {
                    requestCounts = new ArrayList<RequestCount>();
                    map.put(createDate, requestCounts);
                }
                RequestCount requestCount = new RequestCount(rs.getLong("ct"));
                if (ownerTypes != null) {
                    String ownerType = rs.getString("owner_type");
                    if (OwnerType.LISTENER_REQUEST.equals(ownerType))
                        requestCount.setType("Inbound Requests");
                    else if (OwnerType.ADAPTER_REQUEST.equals(ownerType))
                        requestCount.setType("Outbound Requests");
                }
                requestCounts.add(requestCount);
            }
            return map;
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    protected String getRequestWhereClause(Query query) throws ParseException, DataAccessException {
        Date start = query.getDateFilter("startDate");
        if (start == null)
            throw new DataAccessException("Parameter startDate is required");
        String startStr = getDateFormat().format(start);

        StringBuilder where = new StringBuilder();
        if (db.isMySQL())
            where.append("where create_dt >= STR_TO_DATE('" + startStr + "','%d-%M-%Y') ");
        else
            where.append("where create_dt >= '" + startStr + "' ");
        return where.toString();
    }
}
