package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.request.RequestCount;

import java.sql.ResultSet;
import java.text.ParseException;
import java.util.*;

public class RequestAggregation extends CommonDataAccess implements AggregateDataAccess {

    @Override
    public List<RequestCount> getTops(Query query) throws DataAccessException {
        return null;
    }

    public TreeMap<Date,List<RequestCount>> getBreakdown(Query query) throws DataAccessException {
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
            sql.append("  from document\n");
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
            TreeMap<Date,List<RequestCount>> map = new TreeMap<>();
            while (rs.next()) {
                String createDtStr = rs.getString("created");
                Date createDate = getDateFormat().parse(createDtStr);
                List<RequestCount> requestCounts = map.get(createDate);
                if (requestCounts == null) {
                    requestCounts = new ArrayList<>();
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
