package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.PreparedSelect;
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
        PreparedWhere preparedWhere = getRequestWhere(query);
        String sql = db.pagingQueryPrefix() +
                "select path, count(path) as ct\n" +
                "from DOCUMENT doc\n" +
                preparedWhere.getWhere() +
                "group by path\n" +
                "order by ct desc\n" +
                db.pagingQuerySuffix(query.getStart(), query.getMax());
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "RequestAggregation.getTopsByThroughput()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            RequestAggregate requestAggregate = new RequestAggregate(ct);
            requestAggregate.setCount(ct);
            requestAggregate.setPath(resultSet.getString("doc.path"));
            return requestAggregate;
        });
    }

    private List<RequestAggregate> getTopsByStatus(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getRequestWhere(query);
        String sql = db.pagingQueryPrefix() +
                "select status_code, count(status_code) as ct from DOCUMENT\n" +
                preparedWhere.getWhere() +
                "group by status_code\n" +
                "order by ct desc\n" +
                db.pagingQuerySuffix(query.getStart(), query.getMax());
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "RequestAggregation.getTopsByStatus()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            RequestAggregate requestAggregate = new RequestAggregate(ct);
            requestAggregate.setCount(ct);
            Integer statusCode = resultSet.getInt("status_code");
            requestAggregate.setId(statusCode);
            requestAggregate.setStatus(statusCode);
            return requestAggregate;
        });
    }

    private List<RequestAggregate> getTopsByCompletionTime(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getRequestWhere(query);
        String sql = db.pagingQueryPrefix() +
                "select path, avg(elapsed_ms) as elapsed, count(path) as ct\n" +
                "from DOCUMENT" +
                ", INSTANCE_TIMING it\n" +
                preparedWhere.getWhere() +
                "group by path\n" +
                "order by elapsed desc\n" +
                db.pagingQuerySuffix(query.getStart(), query.getMax());
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "RequestAggregation.getTopsByCompletionTime()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            Long elapsed = Math.round(resultSet.getDouble("elapsed"));
            RequestAggregate requestAggregate = new RequestAggregate(elapsed);
            requestAggregate.setCount(resultSet.getLong("ct"));
            requestAggregate.setPath(resultSet.getString("path"));
            return requestAggregate;
        });
    }

    public TreeMap<Date,List<RequestAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");
        try {
            PreparedWhere preparedWhere = getRequestWhere(query);
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
            else if (!by.equals("total"))
                sql.append(", path ");
            if (by.equals("completionTime"))
                sql.append(", elapsed_ms");
            sql.append("\n  from DOCUMENT");
            if (by.equals("completionTime"))
                sql.append(", INSTANCE_TIMING it");
            sql.append("\n  ");
            sql.append(preparedWhere.getWhere()).append(" ");
            List<Object> params = new ArrayList<>(Arrays.asList(preparedWhere.getParams()));
            if (by.equals("status")) {
                String[] statuses = query.getArrayFilter("statusCodes");
                List<Integer> statusCodes = null;
                if (statuses != null) {
                    statusCodes = new ArrayList<>();
                    for (String status : statuses)
                        statusCodes.add(Integer.parseInt(status));
                }
                PreparedWhere inCondition = getInCondition(statusCodes);
                sql.append("   and status_code ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            else if (!by.equals("total")) {
                String[] requestPathsArr = query.getArrayFilter("requestPaths");
                List<String> requestPaths = requestPathsArr == null ? null : Arrays.asList(requestPathsArr);
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

    protected PreparedWhere getRequestWhere(Query query)
            throws ParseException, DataAccessException, ServiceException {
        String by = query.getFilter("by");
        Date start = getStartDate(query);

        StringBuilder where = new StringBuilder("where path is not null\n");
        List<Object> params = new ArrayList<>();

        boolean includeHealthCheck = query.getBooleanFilter("Health Check");
        if (!includeHealthCheck) {
            where.append("  and path != ?");
            params.add("AppSummary");
            where.append(" and path != ?\n");
            params.add("Get->AppSummary");
        }

        String ownerType;
        String direction = query.getFilter("direction");
        if ("out".equals(direction)) {
            if (by.equals("completionTime"))
                ownerType = OwnerType.ADAPTER;
            else
                ownerType = OwnerType.ADAPTER_RESPONSE;
        }
        else {
            ownerType = OwnerType.LISTENER_RESPONSE;
        }

        if ("completionTime".equals(by)) {
            if ("out".equals(direction))
                where.append("  and instance_id = owner_id and it.owner_type = ?\n");
            else
                where.append("  and instance_id = document_id and it.owner_type = ?\n");
            params.add(ownerType);
        }
        else {
            where.append("  and owner_type = ?\n");
            params.add(ownerType);
        }

        where.append("  and create_dt >= ? ");
        params.add(getDt(start));

        Date end = getEndDate(query);
        if (end != null) {
            where.append(" and create_dt <= ? ");
            params.add(getDt(end));
        }
        where.append("\n");

        String status = query.getFilter("Status");
        if (status != null) {
            int spaceHyphen = status.indexOf(" -");
            if (spaceHyphen > 0)
                status = status.substring(0,spaceHyphen);
            where.append("  and status_code = ?\n");
            params.add(Integer.parseInt(status));
        }

        return new PreparedWhere(where.toString(), params.toArray());
    }
}
