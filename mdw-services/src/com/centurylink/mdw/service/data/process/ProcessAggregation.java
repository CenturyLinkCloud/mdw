package com.centurylink.mdw.service.data.process;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.PreparedSelect;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.dataaccess.reports.AggregateDataAccess;
import com.centurylink.mdw.model.workflow.CompletionTimeUnit;
import com.centurylink.mdw.model.workflow.ProcessAggregate;
import com.centurylink.mdw.model.workflow.WorkStatuses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private List<ProcessAggregate> getTopsByThroughput(Query query)
            throws DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getProcessWhere(query);
        String sql = db.pagingQueryPrefix() +
                "select process_id, count(process_id) as ct\n" +
                "from PROCESS_INSTANCE\n" +
                preparedWhere.getWhere() + " " +
                "group by process_id\n" +
                "order by ct desc\n" +
                db.pagingQuerySuffix(query.getStart(), query.getMax());
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "ProcessAggregation.getTopsByThroughput()");
        return getTopAggregates(preparedSelect, query, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            ProcessAggregate processAggregate = new ProcessAggregate(ct);
            processAggregate.setCount(ct);
            processAggregate.setId(resultSet.getLong("process_id"));
            return processAggregate;
        });
    }

    private List<ProcessAggregate> getTopsByStatus(Query query)
            throws DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getProcessWhere(query);
        String sql = db.pagingQueryPrefix() +
                "select status_cd, count(status_cd) as ct from PROCESS_INSTANCE\n" +
                preparedWhere.getWhere() + " " +
                "group by status_cd\n" +
                "order by ct desc\n" +
                db.pagingQuerySuffix(query.getStart(), query.getMax());
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "ProcessAggregation.getTopsByStatus()");
        return getTopAggregates(preparedSelect, query, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            ProcessAggregate processAggregate = new ProcessAggregate(ct);
            processAggregate.setCount(ct);
            processAggregate.setId(resultSet.getInt("status_cd"));
            return processAggregate;
        });
    }

    private List<ProcessAggregate> getTopsByCompletionTime(Query query)
            throws DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getProcessWhere(query);
        String sql = db.pagingQueryPrefix() +
                "select process_id, avg(elapsed_ms) as elapsed, count(process_id) as ct\n" +
                "from PROCESS_INSTANCE" +
                ", INSTANCE_TIMING\n" +
                preparedWhere.getWhere() + " " +
                "group by process_id\n" +
                "order by elapsed desc\n" +
                db.pagingQuerySuffix(query.getStart(), query.getMax());
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "ProcessAggregation.getTopsByCompletionTime()");
        List<ProcessAggregate> aggregates = getTopAggregates(preparedSelect, query, resultSet -> {
            Long elapsed = Math.round(resultSet.getDouble("elapsed"));
            ProcessAggregate processAggregate = new ProcessAggregate(elapsed);
            processAggregate.setCount(resultSet.getLong("ct"));
            processAggregate.setId(resultSet.getLong("process_id"));
            return processAggregate;
        });
        CompletionTimeUnit units = getTimeUnit(query);
        if (units != CompletionTimeUnit.Milliseconds) {
            aggregates.forEach(aggregate -> {
                aggregate.setValue(units.convert(aggregate.getValue(), CompletionTimeUnit.Milliseconds));
            });
        }
        return aggregates;
    }

    public TreeMap<Instant,List<ProcessAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
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

            sql.append("from (select ").append(getSt("start_dt", query));

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

            sql.append("\norder by st\n");

            CompletionTimeUnit timeUnit = getTimeUnit(query);

            PreparedSelect select = new PreparedSelect(sql.toString(), params.toArray(), "Breakdown by " + by);
            return handleBreakdownResult(select, query, rs -> {
                ProcessAggregate processAggregate = new ProcessAggregate(rs.getLong("val"));
                if (by.equals("status")) {
                    int statusCode = rs.getInt("status_cd");
                    processAggregate.setName(WorkStatuses.getName(statusCode));
                    processAggregate.setId(statusCode);
                }
                else if (!by.equals("total")) {
                    processAggregate.setId(rs.getLong("process_id"));
                }
                if (by.equals("completionTime") && timeUnit != CompletionTimeUnit.Milliseconds) {
                    processAggregate.setValue(timeUnit.convert(processAggregate.getValue(), CompletionTimeUnit.Milliseconds));
                }

                return processAggregate;
            });
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    protected PreparedWhere getProcessWhere(Query query) throws DataAccessException {
        String by = query.getFilter("by");
        Instant start = getStart(query);

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if ("completionTime".equals(by)) {
            where.append("where owner_type = ? and instance_id = process_instance_id\n");
            params.add("PROCESS_INSTANCE");
        }
        where.append(where.length() > 0 ? "  and " : "where ");

        where.append("start_dt >= ?\n");
        params.add(getDbDt(start));

        Instant end = getEnd(query);
        if (end != null) {
            where.append("  and start_dt <= ?\n");
            params.add(getDbDt(end));
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

    public Map<Long,Long> getInstanceCounts(List<Long> processIds) throws DataAccessException {
        Map<Long,Long> instanceCounts = new HashMap<>();
        try {
            String sql = "select process_id, count(process_id) as count from PROCESS_INSTANCE \n" +
                    "where process_id in (" + processIds.stream().map(Object::toString).collect(Collectors.joining(",")) + ")\n" +
                    "group by process_id";
            db.openConnection();
            ResultSet rs = db.runSelect(sql);
            while (rs.next()) {
                instanceCounts.put(rs.getLong("process_id"), rs.getLong("count"));
            }
            return instanceCounts;
        }
        catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }
}
