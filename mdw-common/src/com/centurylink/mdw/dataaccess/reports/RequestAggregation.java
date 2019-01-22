package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.model.request.RequestAggregate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class RequestAggregation extends AggregateDataAccess<RequestAggregate> {

    @Override
    public List<RequestAggregate> getTops(Query query) throws DataAccessException, ServiceException {
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

    private List<RequestAggregate> getTopsByThroughput(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere psWhere = getRequestWhereClause(query);
        String sql = "select path, " +
                "count(path) as ct\n" +
                "from DOCUMENT doc\n" +
                psWhere.getWhere() + "\n" +
                "group by path\n" +
                "order by ct desc\n";
        ResultSet rs = db.runSelect("getTopsByThroughput()", sql, psWhere.getParams());
        List<RequestAggregate> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        while (rs.next() && (limit == -1 || idx < limit)) {
            long ct = Math.round(rs.getDouble("ct"));
            RequestAggregate requestAggregate = new RequestAggregate(ct);
            requestAggregate.setCount(ct);
            requestAggregate.setPath(rs.getString("doc.path"));
            list.add(requestAggregate);
            idx++;
        }
        return list;
    }

    private List<RequestAggregate> getTopsByStatus(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getRequestWhereClause(query);
        String sql = "select status_code, count(status_code) as ct from DOCUMENT\n" +
                preparedWhere.getWhere() + " " +
                "group by status_code\n" +
                "order by ct desc\n";
        ResultSet rs = db.runSelect("getTopsByStatus()", sql, preparedWhere.getParams());
        List<RequestAggregate> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        while (rs.next() && (limit == -1 || idx < limit)) {
            long ct = Math.round(rs.getDouble("ct"));
            RequestAggregate requestAggregate = new RequestAggregate(ct);
            requestAggregate.setCount(ct);
            requestAggregate.setId(rs.getInt("status_cd"));
            list.add(requestAggregate);
            idx++;
        }
        return list;
    }

    private List<RequestAggregate> getTopsByCompletionTime(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getRequestWhereClause(query);
        String sql = "select path, " +
                "avg(elapsed_ms) as elapsed, count(path) as ct\n" +
                "from DOCUMENT" +
                ", INSTANCE_TIMING\n" +
                preparedWhere.getWhere() + " " +
                "group by path\n" +
                "order by elapsed desc\n";
        ResultSet rs = db.runSelect("getTopsByCompletionTime()", sql, preparedWhere.getParams());
        List<RequestAggregate> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        while (rs.next() && (limit == -1 || idx < limit)) {
            Long elapsed = Math.round(rs.getDouble("elapsed"));
            RequestAggregate requestAggregate = new RequestAggregate(elapsed);
            requestAggregate.setCount(rs.getLong("ct"));
            requestAggregate.setPath(rs.getString("path"));
            list.add(requestAggregate);
            idx++;
        }
        return list;
    }

    public TreeMap<Date,List<RequestAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");

        try {
            PreparedWhere preparedWhere = getRequestWhereClause(query);
            // request paths
            String[] requestPathsArr = query.getArrayFilter("requestPaths");
            List<String> requestPaths = requestPathsArr == null ? null : Arrays.asList(requestPathsArr);
            // by status
            String[] statuses = query.getArrayFilter("statuses");
            List<Integer> statusCodes = null;
            if (statuses != null) {
                statusCodes = new ArrayList<>();
                for (String status : statuses)
                    statusCodes.add(Integer.parseInt(status));
            }
            if (requestPaths != null && statuses != null)
                throw new DataAccessException("Conflicting parameters: paths and statuses");

            StringBuilder sql = new StringBuilder();
            if (by.equals("status"))
                sql.append("select count(req.status_code) as val, req.st, req.status_code\n");
            else if (by.equals("throughput"))
                sql.append("select count(req.path) as val, req.st, req.path\n");
            else if (by.equals("completionTime"))
                sql.append("select avg(req.elapsed_ms) as val, req.st, req.path\n");
            else if (by.equals("total"))
                sql.append("select count(req.st) as val, req.st\n");

            if (db.isMySQL())
                sql.append("from (select date(create_dt) as st");
            else
                sql.append("from (select to_char(create_dt,'DD-Mon-yyyy') as st");
            if (by.equals("status"))
                sql.append(", status_code ");
            else if (requestPaths != null)
                sql.append(", path ");
            if (by.equals("completionTime"))
                sql.append(", elapsed_ms");
            sql.append("\n  from DOCUMENT");
            if (by.equals("completionTime"))
                sql.append(", INSTANCE_TIMING");
            sql.append("\n  ");
            sql.append(preparedWhere.getWhere()).append(" ");
            List<Object> params = new ArrayList<>(Arrays.asList(preparedWhere.getParams()));
            if (by.equals("status")) {
                PreparedWhere inCondition = getInCondition(statusCodes);
                sql.append("   and status_code ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            else if (!by.equals("total")) {
                PreparedWhere inCondition = getInCondition(requestPaths);
                sql.append("   and path ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            sql.append(") req\n");

            sql.append("group by st");
            if (by.equals("status"))
                sql.append(", status_code");
            else if (!by.equals("total"))
                sql.append(", path");
            if (db.isMySQL())
                sql.append("\norder by st\n");
            else
                sql.append("\norder by to_date(st, 'DD-Mon-yyyy')\n");

            db.openConnection();
            ResultSet rs = db.runSelect("Breakdown by " + by, sql.toString(), params.toArray());
            TreeMap<Date,List<RequestAggregate>> map = new TreeMap<>();
            Date prevStartDate = getStartDate(query);
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                // fill in gaps
                while (startDate.getTime() - prevStartDate.getTime() > DAY_MS) {
                    prevStartDate = new Date(prevStartDate.getTime() + DAY_MS);
                    map.put(getRoundDate(prevStartDate), new ArrayList<>());
                }
                List<RequestAggregate> requestAggregates = map.get(startDate);
                if (requestAggregates == null) {
                    requestAggregates = new ArrayList<>();
                    map.put(startDate, requestAggregates);
                }
                RequestAggregate requestAggregate = new RequestAggregate(rs.getLong("val"));
                if (by.equals("status")) {
                    int statusCode = rs.getInt("status_code");
                    requestAggregate.setStatus(statusCode);
                    requestAggregate.setId(statusCode);
                }
                else if (!by.equals("total")) {
                    requestAggregate.setPath(rs.getString("path"));
                }
                requestAggregates.add(requestAggregate);
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

    protected PreparedWhere getRequestWhereClause(Query query)
            throws ParseException, DataAccessException, ServiceException {
        String by = query.getFilter("by");
        Date start = getStartDate(query);

        StringBuilder where = new StringBuilder("where path is not null\n");
        List<Object> params = new ArrayList<>();

        boolean includeHealthCheck = query.getBooleanFilter("HealthCheck");
        if (!includeHealthCheck) {
            where.append("  and path != ?\n");
            params.add("AppSummary");
        }

        String ownerType = null;
        String direction = query.getFilter("direction");
        boolean isMaster = query.getBooleanFilter("Master");
        if ("inbound".equals(direction) || isMaster) {
            if (by.equals("status") || by.equals("completionTime"))
                ownerType = OwnerType.LISTENER_RESPONSE;
            else
                ownerType = OwnerType.LISTENER_REQUEST;
        }
        else if ("outbound".equals(direction)) {
            if (by.equals("status"))
                ownerType = OwnerType.ADAPTER_RESPONSE;
            else
                ownerType = OwnerType.ADAPTER_REQUEST;
        }
        if (ownerType == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing parameter: direction");
        where.append("  and owner_type = ?\n");
        params.add(ownerType);

        if ("completionTime".equals(by)) {
            where.append("  and it.owner_type = ? and instance_id = document_id\n");
            params.add(ownerType);
        }
        where.append("  and create_dt >= ? ");
        params.add(getDt(start));

        Date end = getEndDate(query);
        if (end != null) {
            where.append("  and create_dt <= ? ");
            params.add(getDt(end));
        }

        return new PreparedWhere(where.toString(), params.toArray());
    }
}
