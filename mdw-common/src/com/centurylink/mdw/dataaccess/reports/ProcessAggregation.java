package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.PreparedSelect;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.model.workflow.ProcessAggregate;
import com.centurylink.mdw.model.workflow.WorkStatuses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class ProcessAggregation extends AggregateDataAccess<ProcessAggregate> {

    public List<ProcessAggregate> getTops(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");
        try {
            db.openConnection();
            if (by.equals("throughput"))
                return getTopsByThroughput(query);
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

    private List<ProcessAggregate> getTopsByThroughput(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getProcessWhere(query);
        String sql = "select process_id, count(process_id) as ct\n" +
                "from PROCESS_INSTANCE\n" +
                preparedWhere.getWhere() + " " +
                "group by process_id\n" +
                "order by ct desc\n";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "ProcessAggregation.getTopsByThroughput()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            ProcessAggregate processAggregate = new ProcessAggregate(ct);
            processAggregate.setCount(ct);
            processAggregate.setId(resultSet.getLong("process_id"));
            return processAggregate;
        });
    }

    private List<ProcessAggregate> getTopsByStatus(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getProcessWhere(query);
        String sql = "select status_cd, count(status_cd) as ct from PROCESS_INSTANCE\n" +
                preparedWhere.getWhere() + " " +
                "group by status_cd\n" +
                "order by ct desc\n";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "ProcessAggregation.getTopsByStatus()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            ProcessAggregate processAggregate = new ProcessAggregate(ct);
            processAggregate.setCount(ct);
            processAggregate.setId(resultSet.getInt("status_cd"));
            return processAggregate;
        });
    }

    private List<ProcessAggregate> getTopsByCompletionTime(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getProcessWhere(query);
        String sql = "select process_id, avg(elapsed_ms) as elapsed, count(process_id) as ct\n" +
                "from PROCESS_INSTANCE" +
                ", INSTANCE_TIMING\n" +
                preparedWhere.getWhere() + " " +
                "group by process_id\n" +
                "order by elapsed desc\n";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "ProcessAggregation.getTopsByCompletionTime()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            Long elapsed = Math.round(resultSet.getDouble("elapsed"));
            ProcessAggregate processAggregate = new ProcessAggregate(elapsed);
            processAggregate.setCount(resultSet.getLong("ct"));
            processAggregate.setId(resultSet.getLong("process_id"));
            return processAggregate;
        });
    }

    public TreeMap<Date,List<ProcessAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");
        try {
            PreparedWhere preparedWhere = getProcessWhere(query);
            StringBuilder sql = new StringBuilder();
            if (by.equals("status"))
                sql.append("select count(pi.status_cd) as val, pi.st, pi.status_cd\n");
            else if (by.equals("throughput"))
                sql.append("select count(pi.process_id) as val, pi.st, pi.process_id\n");
            else if (by.equals("completionTime"))
                sql.append("select avg(pi.elapsed_ms) as val, pi.st, pi.process_id\n");
            else if (by.equals("total"))
                sql.append("select count(pi.st) as val, pi.st\n");

            if (db.isMySQL())
                sql.append("from (select date(start_dt) as st");
            else
                sql.append("from (select to_char(start_dt,'DD-Mon-yyyy') as st");
            if (by.equals("status"))
                sql.append(", status_cd ");
            else if (!by.equals("total"))
                sql.append(", process_id ");
            if (by.equals("completionTime"))
                sql.append(", elapsed_ms");
            sql.append("  from PROCESS_INSTANCE");
            if (by.equals("completionTime"))
                sql.append(", INSTANCE_TIMING");
            sql.append("\n  ");
            sql.append(preparedWhere.getWhere()).append(" ");
            List<Object> params = new ArrayList<>(Arrays.asList(preparedWhere.getParams()));
            if (by.equals("status")) {
                String[] statuses = query.getArrayFilter("statuses");
                List<Integer> statusCodes = null;
                if (statuses != null) {
                    statusCodes = new ArrayList<>();
                    for (String status : statuses)
                        statusCodes.add(WorkStatuses.getCode(status));
                }
                PreparedWhere inCondition = getInCondition(statusCodes);
                sql.append("   and status_cd ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            else if (!by.equals("total")) {
                Long[] processIdsArr = query.getLongArrayFilter("processIds");
                List<Long> processIds = processIdsArr == null ? null : Arrays.asList(processIdsArr);
                PreparedWhere inCondition = getInCondition(processIds);
                sql.append("   and process_id ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            sql.append(") pi\n");

            sql.append("group by st");
            if (by.equals("status"))
                sql.append(", status_cd");
            else if (!by.equals("total"))
                sql.append(", process_id");
            if (db.isMySQL())
                sql.append("\norder by st\n");
            else
                sql.append("\norder by to_date(st, 'DD-Mon-yyyy')\n");

            db.openConnection();
            ResultSet rs = db.runSelect("Breakdown by " + by, sql.toString(), params.toArray());
            TreeMap<Date,List<ProcessAggregate>> map = new TreeMap<>();
            Date prevStartDate = getStartDate(query);
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                // fill in gaps
                while (startDate.getTime() - prevStartDate.getTime() > DAY_MS) {
                    prevStartDate = new Date(prevStartDate.getTime() + DAY_MS);
                    map.put(getRoundDate(prevStartDate), new ArrayList<>());
                }
                List<ProcessAggregate> processAggregates = map.get(startDate);
                if (processAggregates == null) {
                    processAggregates = new ArrayList<>();
                    map.put(startDate, processAggregates);
                }
                ProcessAggregate processAggregate = new ProcessAggregate(rs.getLong("val"));
                if (by.equals("status")) {
                    int statusCode = rs.getInt("status_cd");
                    processAggregate.setName(WorkStatuses.getName(statusCode));
                    processAggregate.setId(statusCode);
                }
                else if (!by.equals("total")) {
                    processAggregate.setId(rs.getLong("process_id"));
                }
                processAggregates.add(processAggregate);
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

    protected PreparedWhere getProcessWhere(Query query) throws ParseException, DataAccessException {
        String by = query.getFilter("by");
        Date start = getStartDate(query);

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if ("completionTime".equals(by)) {
            where.append("where owner_type = ? and instance_id = process_instance_id\n");
            params.add("PROCESS_INSTANCE");
        }
        where.append(where.length() > 0 ? "  and " : "where ");

        where.append("start_dt >= ?\n");
        params.add(getDt(start));

        Date end = getEndDate(query);
        if (end != null) {
            where.append("  and start_dt <= ?\n");
            params.add(getDt(end));
        }

        where.append("  and owner not in (?");
        params.add("MAIN_PROCESS_INSTANCE");
        if (query.getBooleanFilter("Master")) {
            where.append(", ? ");
            params.add("PROCESS_INSTANCE");
        }
        where.append(")\n");

        String status = query.getFilter("Status");
        if (status != null) {
            where.append("  and status_cd = ?\n");
            params.add(WorkStatuses.getCode(status));
        }

        return new PreparedWhere(where.toString(), params.toArray());
    }
}
