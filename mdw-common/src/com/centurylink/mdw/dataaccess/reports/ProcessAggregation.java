package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
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
            throws ParseException, DataAccessException, SQLException {
        String sql = "select process_id, " +
                "count(process_id) as ct\n" +
                "from PROCESS_INSTANCE\n" +
                getProcessWhereClause(query) + "\n" +
                "group by process_id\n" +
                "order by ct desc\n";
        ResultSet rs = db.runSelect(sql);
        List<ProcessAggregate> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        while (rs.next() && (limit == -1 || idx < limit)) {
            long ct = Math.round(rs.getDouble("ct"));
            ProcessAggregate processAggregate = new ProcessAggregate(ct);
            processAggregate.setCount(ct);
            processAggregate.setId(rs.getLong("process_id"));
            list.add(processAggregate);
            idx++;
        }
        return list;
    }

    private List<ProcessAggregate> getTopsByStatus(Query query)
            throws ParseException, DataAccessException, SQLException {
        String sql = "select status_cd, count(status_cd) as ct from PROCESS_INSTANCE\n" +
                getProcessWhereClause(query) + "\n" +
                "group by status_cd\n" +
                "order by ct desc\n";
        ResultSet rs = db.runSelect(sql);
        List<ProcessAggregate> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        while (rs.next() && (limit == -1 || idx < limit)) {
            long ct = Math.round(rs.getDouble("ct"));
            ProcessAggregate processAggregate = new ProcessAggregate(ct);
            processAggregate.setCount(ct);
            processAggregate.setId(rs.getInt("status_cd"));
            list.add(processAggregate);
            idx++;
        }
        return list;
    }

    private List<ProcessAggregate> getTopsByCompletionTime(Query query)
            throws ParseException, DataAccessException, SQLException {
        String sql = "select process_id, " +
                "avg(elapsed_ms) as elapsed, count(process_id) as ct\n" +
                "from PROCESS_INSTANCE" +
                ", INSTANCE_TIMING\n" +
                getProcessWhereClause(query) + "\n" +
                "group by process_id\n" +
                "order by elapsed desc\n";
        ResultSet rs = db.runSelect(sql);
        List<ProcessAggregate> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        while (rs.next() && (limit == -1 || idx < limit)) {
            Long elapsed = Math.round(rs.getDouble("elapsed"));
            ProcessAggregate processAggregate = new ProcessAggregate(elapsed);
            processAggregate.setCount(rs.getLong("ct"));
            processAggregate.setId(rs.getLong("process_id"));
            list.add(processAggregate);
            idx++;
        }
        return list;
    }

    public TreeMap<Date,List<ProcessAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");

        try {
            // process ids
            Long[] processIdsArr = query.getLongArrayFilter("processIds");
            List<Long> processIds = processIdsArr == null ? null : Arrays.asList(processIdsArr);
            // by status
            String[] statuses = query.getArrayFilter("statuses");
            List<Integer> statusCodes = null;
            if (statuses != null) {
                statusCodes = new ArrayList<>();
                for (String status : statuses)
                    statusCodes.add(WorkStatuses.getCode(status));
            }
            if (processIds != null && statuses != null)
                throw new DataAccessException("Conflicting parameters: processIds and statuses");

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
            else if (processIds != null)
                sql.append(", process_id ");
            if (by.equals("completionTime"))
                sql.append(", elapsed_ms");
            sql.append("  from PROCESS_INSTANCE");
            if (by.equals("completionTime"))
                sql.append(", INSTANCE_TIMING");
            sql.append("\n   ");
            sql.append(getProcessWhereClause(query));
            if (by.equals("status"))
                sql.append("\n   and status_cd ").append(getInCondition(statusCodes));
            else if (!by.equals("total"))
                sql.append("\n   and process_id ").append(getInCondition(processIds));
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
            ResultSet rs = db.runSelect(sql.toString(), null);
            TreeMap<Date,List<ProcessAggregate>> map = new TreeMap<>();
            Date prevStartDate = getStartDate(query);
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate;
                if (db.isMySQL())
                    startDate = getMySqlDateFormat().parse(startDateStr);
                else
                    startDate = getDateFormat().parse(startDateStr);
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
            while (endDate != null && endDate.getTime() - prevStartDate.getTime() > DAY_MS) {
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

    protected String getProcessWhereClause(Query query) throws ParseException, DataAccessException {
        String by = query.getFilter("by");
        Date start = getStartDate(query);

        StringBuilder where = new StringBuilder();
        if ("completionTime".equals(by))
            where.append("where owner_type = 'PROCESS_INSTANCE' and instance_id = process_instance_id\n");
        where.append(where.length() > 0 ? "and " : "where ");
        if (db.isMySQL())
            where.append("start_dt >= '").append(getMySqlDt(start)).append("' ");
        else
            where.append("start_dt >= '").append(getOracleDt(start)).append("' ");
        Date end = getEndDate(query);
        if (end != null) {
            if (db.isMySQL())
                where.append("and start_dt <= '").append(getMySqlDt(end)).append("' ");
            else
                where.append("and start_dt <= '").append(getOracleDt(end)).append("' ");
        }
        where.append("and owner not in ('MAIN_PROCESS_INSTANCE' ");
        if (query.getBooleanFilter("Master"))
            where.append(", 'PROCESS_INSTANCE' ");
        where.append(") ");
        String status = query.getFilter("Status");
        if (status != null)
            where.append("and STATUS_CD = ").append(WorkStatuses.getCode(status));
        return where.toString();
    }
}
