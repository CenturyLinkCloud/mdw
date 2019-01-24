package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.PreparedSelect;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.model.task.TaskAggregate;
import com.centurylink.mdw.model.task.TaskStatuses;
import com.centurylink.mdw.model.workflow.WorkStatuses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class TaskAggregation extends AggregateDataAccess<TaskAggregate> {

    @Override
    public List<TaskAggregate> getTops(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");
        try {
            db.openConnection();
            if (by.equals("throughput"))
                return getTopsByThroughput(query);
            else if (by.equals("workgroup"))
                return getTopsByWorkgroup(query);
            else if (by.equals("assignee"))
                return getTopsByAssignee(query);
            else if (by.equals("status"))
                return getTopsByStatus(query);
            else if (by.equals("completionTime"))
                return getTopsByCompletionTime(query);
            else
                throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported filter: by=" + by);
        }
        catch (SQLException | ParseException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private List<TaskAggregate> getTopsByThroughput(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getTaskWhere(query);
        String sql = "select task_id, count(task_id) as ct\n" +
                "from TASK_INSTANCE\n" +
                preparedWhere.getWhere() + " " +
                "group by task_id\n" +
                "order by ct desc\n";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "TaskAggregation.getTopsByThroughput()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            TaskAggregate taskAggregate = new TaskAggregate(ct);
            taskAggregate.setCount(ct);
            taskAggregate.setId(resultSet.getLong("task_id"));
            return taskAggregate;
        });
    }

    public List<TaskAggregate> getTopsByWorkgroup(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getTaskWhere(query);
        String sql = "select count(tii.group_name) as ct, tii.group_name\n" +
                "from (select ug.group_name from TASK_INST_GRP_MAPP tigm, TASK_INSTANCE ti, USER_GROUP ug\n   " +
                preparedWhere.getWhere() +
                "and tigm.task_instance_id = ti.task_instance_id\n   " +
                "and tigm.user_group_id = ug.user_group_id\n   " +
                ") tii\n" +
                "group by group_name\n" +
                "order by ct desc";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "TaskAggregation.getTopsByWorkgroup()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            TaskAggregate taskAggregate = new TaskAggregate(ct);
            taskAggregate.setCount(ct);
            String workgroup = resultSet.getString("group_name");
            taskAggregate.setName(workgroup);
            taskAggregate.setWorkgroup(workgroup);
            return taskAggregate;
        });
    }

    public List<TaskAggregate> getTopsByAssignee(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getTaskWhere(query);
        String sql = "select count(tii.cuid) as ct, tii.cuid, tii.name\n" +
                "from (select ui.cuid, ui.name from TASK_INSTANCE ti, USER_INFO ui\n   " +
                preparedWhere.getWhere() +
                "and ti.task_claim_user_id = ui.user_info_id\n   " +
                ") tii\n" +
                "group by cuid, name\n" +
                "order by ct desc";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "TaskAggregation.getTopsByAssignee()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            TaskAggregate taskAggregate = new TaskAggregate(ct);
            taskAggregate.setCount(ct);
            taskAggregate.setUserId(resultSet.getString("cuid"));
            taskAggregate.setName(resultSet.getString("name"));
            return taskAggregate;
        });
    }

    private List<TaskAggregate> getTopsByStatus(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getTaskWhere(query);
        String sql = "select task_instance_status, count(task_instance_status) as ct from TASK_INSTANCE\n" +
                preparedWhere.getWhere() + " " +
                "group by task_instance_status\n" +
                "order by ct desc\n";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "TaskAggregation.getTopsByStatus()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            TaskAggregate taskAggregate = new TaskAggregate(ct);
            taskAggregate.setCount(ct);
            int statusCode = resultSet.getInt("task_instance_status");
            taskAggregate.setId(statusCode);
            taskAggregate.setName(TaskStatuses.getName(statusCode));
            return taskAggregate;
        });
    }

    private List<TaskAggregate> getTopsByCompletionTime(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getTaskWhere(query);
        String sql = "select task_id, avg(elapsed_ms) as elapsed, count(task_id) as ct\n" +
                "from TASK_INSTANCE, INSTANCE_TIMING\n" +
                preparedWhere.getWhere() + " " +
                "group by task_id\n" +
                "order by elapsed desc\n";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "TaskAggregation.getTopsByCompletionTime()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            Long elapsed = Math.round(resultSet.getDouble("elapsed") / 1000);
            TaskAggregate taskAggregate = new TaskAggregate(elapsed);
            taskAggregate.setCount(resultSet.getLong("ct"));
            taskAggregate.setId(resultSet.getLong("task_id"));
            return taskAggregate;
        });
    }

    public TreeMap<Date,List<TaskAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");

        try {
            PreparedWhere preparedWhere = getTaskWhere(query);
            // task ids
            Long[] taskIdsArr = query.getLongArrayFilter("taskIds");
            List<Long> taskIds = taskIdsArr == null ? null : Arrays.asList(taskIdsArr);

            StringBuilder sql = new StringBuilder();
            if (by.equals("status"))
                sql.append("select count(ti.task_instance_status) as val, ti.st, ti.task_instance_status\n");
            else if (by.equals("throughput"))
                sql.append("select count(ti.task_id) as val, ti.st, ti.task_id\n");
            else if (by.equals("completionTime"))
                sql.append("select avg(ti.elapsed_ms) as val, ti.st, ti.task_id\n");
            else if (by.equals("workgroup"))
                sql.append("select count(ti.group_name) as val, ti.st, ti.group_name\n");
            else if (by.equals("assignee"))
                sql.append("select count(ti.cuid) as val, ti.st, ti.cuid, ti.name\n");
            else if (by.equals("total"))
                sql.append("select count(ti.st) as val, ti.st\n");

            if (db.isMySQL())
                sql.append("from (select date(task_start_dt) as st");
            else
                sql.append("from (select to_char(task_start_dt,'DD-Mon-yyyy') as st");
            if (by.equals("status"))
                sql.append(", task_instance_status ");
            else if (taskIds != null)
                sql.append(", task_id ");
            if (by.equals("completionTime"))
                sql.append(", elapsed_ms");
            if (by.equals("workgroup"))
                sql.append(", ug.group_name from TASK_INSTANCE ti");
            else if (by.equals("assignee"))
                sql.append(", cuid, name from TASK_INSTANCE ti");
            else
                sql.append(" from TASK_INSTANCE");
            if (by.equals("completionTime"))
                sql.append(", INSTANCE_TIMING");
            else if (by.equals("workgroup"))
                sql.append(", TASK_INST_GRP_MAPP tigm, USER_GROUP ug ");
            else if (by.equals("assignee"))
                sql.append(", USER_INFO ui ");
            sql.append("\n  ");
            sql.append(preparedWhere.getWhere()).append(" ");
            List<Object> params = new ArrayList<>(Arrays.asList(preparedWhere.getParams()));
            if (by.equals("status")) {
                // by status
                String[] statuses = query.getArrayFilter("statuses");
                List<Integer> statusCodes = null;
                if (statuses != null) {
                    statusCodes = new ArrayList<>();
                    for (String status : statuses)
                        statusCodes.add(TaskStatuses.getCode(status));
                }
                PreparedWhere inCondition = getInCondition(statusCodes);
                sql.append("   and task_instance_status ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            else if (by.equals("workgroup")) {
                String[] workgroups = query.getArrayFilter("workgroups");
                PreparedWhere inCondition = getInCondition(Arrays.asList(workgroups));
                sql.append("  and tigm.task_instance_id = ti.task_instance_id\n");
                sql.append("  and tigm.user_group_id = ug.user_group_id\n");
                sql.append("  and ug.group_name ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            else if (by.equals("assignee")) {
                String[] assignees = query.getArrayFilter("assignees");
                PreparedWhere inCondition = getInCondition(Arrays.asList(assignees));
                sql.append("  and ti.task_claim_user_id = ui.user_info_id\n");
                sql.append("  and ui.cuid ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            else if (!by.equals("total")) {
                PreparedWhere inCondition = getInCondition(taskIds);
                sql.append("   and task_id ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            sql.append(") ti\n");

            sql.append("group by st");
            if (by.equals("status"))
                sql.append(", task_instance_status");
            else if (by.equals("throughput") || by.equals("completionTime"))
                sql.append(", task_id");
            else if (by.equals("workgroup"))
                sql.append(", group_name");
            else if (by.equals("assignee"))
                sql.append(", cuid, name ");
            if (db.isMySQL())
                sql.append("\norder by st\n");
            else
                sql.append("\norder by to_date(st, 'DD-Mon-yyyy')\n");

            db.openConnection();
            ResultSet rs = db.runSelect("Breakdown by " + by, sql.toString(), params.toArray());
            TreeMap<Date,List<TaskAggregate>> map = new TreeMap<>();
            Date prevStartDate = getStartDate(query);
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                // fill in gaps
                while (startDate.getTime() - prevStartDate.getTime() > DAY_MS) {
                    prevStartDate = new Date(prevStartDate.getTime() + DAY_MS);
                    map.put(getRoundDate(prevStartDate), new ArrayList<>());
                }
                List<TaskAggregate> taskAggregates = map.get(startDate);
                if (taskAggregates == null) {
                    taskAggregates = new ArrayList<>();
                    map.put(startDate, taskAggregates);
                }
                TaskAggregate taskAggregate = new TaskAggregate(rs.getLong("val"));
                if (by.equals("throughput")) {
                    taskAggregate.setId(rs.getLong("task_id"));
                }
                else if (by.equals("status")) {
                    int statusCode = rs.getInt("task_instance_status");
                    taskAggregate.setName(WorkStatuses.getName(statusCode));
                    taskAggregate.setId(statusCode);
                }
                else if (by.equals("completionTime")) {
                    taskAggregate.setId(rs.getLong("task_id"));
                    taskAggregate.setValue(Math.round((float)taskAggregate.getValue() / 1000));
                }
                else if (by.equals("workgroup")) {
                    String workgroup = rs.getString("group_name");
                    taskAggregate.setName(workgroup);
                    taskAggregate.setWorkgroup(workgroup);
                }
                else if (by.equals("assignee")) {
                    taskAggregate.setUserId(rs.getString("cuid"));
                    taskAggregate.setName(rs.getString("name"));
                }
                taskAggregates.add(taskAggregate);
                prevStartDate = startDate;
            }
            // missing start date
            Date roundStartDate = getRoundDate(getStartDate(query));
            if (map.get(roundStartDate) == null)
                map.put(roundStartDate, new ArrayList<>());
            // gaps at end
            Date endDate = getEndDate(query);
            while ((endDate != null) && ((endDate.getTime() - prevStartDate.getTime()) > DAY_MS)) {
                prevStartDate = new Date(prevStartDate.getTime() + DAY_MS);
                map.put(getRoundDate(prevStartDate), new ArrayList<>());
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

    protected PreparedWhere getTaskWhere(Query query) throws ParseException, DataAccessException {
        String by = query.getFilter("by");
        Date start = getStartDate(query);

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if ("completionTime".equals(by)) {
            where.append("where owner_type = ? and instance_id = task_instance_id\n");
            params.add("TASK_INSTANCE");
        }
        where.append(where.length() > 0 ? "  and " : "where ");

        where.append("task_start_dt >= ?\n");
        params.add(getDt(start));

        Date end = getEndDate(query);
        if (end != null) {
            where.append("  and task_start_dt <= ?\n");
            params.add(getDt(end));
        }

        String status = query.getFilter("Status");
        if (status != null) {
            where.append("  and task_instance_status = ?\n");
            params.add(WorkStatuses.getCode(status));
        }

        return new PreparedWhere(where.toString(), params.toArray());
    }
}
