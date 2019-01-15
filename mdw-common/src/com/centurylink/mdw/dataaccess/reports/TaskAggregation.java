package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.task.TaskCount;
import com.centurylink.mdw.model.task.TaskStatuses;

import java.sql.ResultSet;
import java.text.ParseException;
import java.util.*;

public class TaskAggregation extends CommonDataAccess implements AggregateDataAccess {

    @Override
    public List getTops(Query query) throws DataAccessException {
        return null;
    }

    public List<TaskCount> getTopTasks(Query query) throws DataAccessException {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(tii.task_id) as ct, tii.task_id\n");
            sql.append("from (select task_id from task_instance ti ");
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
            sql.append("from (select ug.group_name from task_inst_grp_mapp tigm, task_instance ti, user_group ug\n   ");
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
            sql.append("from (select ui.cuid, ui.name from task_instance ti, user_info ui\n   ");
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

    public TreeMap<Date,List<TaskCount>> getBreakdown(Query query) throws DataAccessException {
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
            sql.append("\n   from task_instance ti ");
            if (workgroups != null)
                sql.append(", task_inst_grp_mapp tigm, user_group ug ");
            else if (assignees != null)
                sql.append(", user_info ui ");
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
            TreeMap<Date,List<TaskCount>> map = new TreeMap<>();
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                List<TaskCount> taskCounts = map.get(startDate);
                if (taskCounts == null) {
                    taskCounts = new ArrayList<>();
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



}
