package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.model.request.RequestAggregate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

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
//            else if (by.equals("status"))
//                return getTopsByStatus(query);
//            else if (by.equals("completionTime"))
//                return getTopsByCompletionTime(query);
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
        ResultSet rs = db.runSelect(sql, psWhere.getParams());
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


    public TreeMap<Date,List<RequestAggregate>> getBreakdown(Query query) throws DataAccessException {
        try {
            // request types
            List<String> ownerTypes = null;
            String[] requests = query.getArrayFilter("requests");
            if (requests != null) {
                ownerTypes = new ArrayList<>();
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
            if (ownerTypes != null) {
                sql.append("\n   and owner_type ").append(getInCondition(ownerTypes));
            }
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
            TreeMap<Date,List<RequestAggregate>> map = new TreeMap<>();
            while (rs.next()) {
                String createDtStr = rs.getString("created");
                Date createDate = getDateFormat().parse(createDtStr);
                List<RequestAggregate> requestAggregates = map.get(createDate);
                if (requestAggregates == null) {
                    requestAggregates = new ArrayList<>();
                    map.put(createDate, requestAggregates);
                }
                RequestAggregate requestAggregate = new RequestAggregate(rs.getLong("ct"));
                requestAggregates.add(requestAggregate);
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
            where.append("and path != ?\n");
            params.add("AppSummary");
        }

        String ownerType = null;
        String direction = query.getFilter("direction");
        boolean isMaster = query.getBooleanFilter("Master");
        if ("inbound".equals(direction) || isMaster) {
            ownerType = OwnerType.LISTENER_REQUEST;
        }
        else if ("outbound".equals(direction)) {
            ownerType = OwnerType.ADAPTER_REQUEST;
        }
        if (ownerType == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing parameter: direction");
        where.append("and doc.owner_type = ?\n");
        params.add(ownerType);

        if ("completionTime".equals(by)) {
            where.append("and it.owner_type = ? and instance_id = document_id\n");
            params.add(ownerType);
        }
        where.append("and create_dt >= ? ");
        params.add(getDt(start));

        Date end = getEndDate(query);
        if (end != null) {
            where.append("and create_dt <= ? ");
            params.add(getDt(end));
        }

        return new PreparedWhere(where.toString(), params.toArray());
    }
}
