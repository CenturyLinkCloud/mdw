package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.workflow.ActivityAggregate;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class ActivityAggregation extends AggregateDataAccess<ActivityAggregate> {

    public List<ActivityAggregate> getTops(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");

        try {
            db.openConnection();
            if (by.equals("throughput"))
                return getTopsByStuck(query);
            else if (by.equals("status"))
                return getTopsByStatus(query);
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

    private List<ActivityAggregate> getTopsByStuck(Query query)
            throws ParseException, DataAccessException, SQLException {

        String sql = "select count(act_unique_id) as ct, act_unique_id\n" +
                getUniqueIdFrom() +
                getActivityWhereClause(query) +
                ") a1\n" +
                "group by act_unique_id\n" +
                "order by ct desc\n";
        ResultSet rs = db.runSelect(sql);
        List<ActivityAggregate> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        while (rs.next() && (limit == -1 || idx < limit)) {
            ActivityAggregate activityAggregate = new ActivityAggregate(rs.getLong("ct"));
            String actId = rs.getString("act_unique_id");
            activityAggregate.setActivityId(actId);
            int colon = actId.lastIndexOf(":");
            activityAggregate.setProcessId(new Long(actId.substring(0, colon)));
            activityAggregate.setDefinitionId(actId.substring(colon + 1));
            list.add(activityAggregate);
            idx++;
        }
        return list;
    }

    private String getUniqueIdFrom() {
        if (db.isMySQL())
            return "from (select CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as ACT_UNIQUE_ID from activity_instance ai, process_instance pi ";
        else
            return "from (select pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as ACT_UNIQUE_ID from activity_instance ai, process_instance pi ";
    }

    private List<ActivityAggregate> getTopsByStatus(Query query)
            throws ParseException, DataAccessException, SQLException {
        String sql = "select status_cd, count(status_cd) as ct " +
                "from activity_instance ai\n" +
                getActivityWhereClause(query) + "\n" +
                "group by status_cd\n" +
                "order by ct desc\n";
        ResultSet rs = db.runSelect(sql);
        List<ActivityAggregate> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        while (rs.next() && (limit == -1 || idx < limit)) {
            long ct = Math.round(rs.getDouble("ct"));
            ActivityAggregate activityAggregate = new ActivityAggregate(ct);
            activityAggregate.setCount(ct);
            activityAggregate.setId(rs.getInt("status_cd"));
            list.add(activityAggregate);
            idx++;
        }
        return list;
    }

    public TreeMap<Date,List<ActivityAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");

        try {
            // activity ids (processid:logicalId)
            String[] actIdsArr = query.getArrayFilter("activityIds");
            List<String> actIds = actIdsArr == null ? null : Arrays.asList(actIdsArr);
            // by status
            String[] statuses = query.getArrayFilter("statuses");
            List<Integer> statusCodes = null;
            if (statuses != null) {
                statusCodes = new ArrayList<>();
                for (String status : statuses)
                    statusCodes.add(WorkStatuses.getCode(status));
            }
            if (actIds != null && statuses != null)
                throw new DataAccessException("Conflicting parameters: activityIds and statuses");

            StringBuilder sql = new StringBuilder();
            if (by.equals("status"))
                sql.append("select count(a.status_cd) as ct, a.st, a.status_cd\n");
            else if (by.equals("throughput"))
                sql.append("select count(a.act_unique_id) as ct, a.st, a.act_unique_id\n");
            else if (by.equals("total"))
                sql.append("select count(a.st) as ct, a.st\n");

            if (db.isMySQL())
                sql.append("from (select date(ai.start_dt) as st");
            else
                sql.append("from (select to_char(ai.start_dt,'DD-Mon-yyyy') as st");
            if (by.equals("status"))
                sql.append(", ai.status_cd ");
            else if (by.equals("throughput")) {
                if (db.isMySQL())
                    sql.append(", CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as act_unique_id");
                else
                    sql.append(", pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as act_unique_id");
            }
            sql.append("  from activity_instance ai ");
            if (by.equals("throughput"))
                sql.append(", process_instance pi");
            sql.append("\n");
            sql.append(getActivityWhereClause(query));
            if (by.equals("status"))
                sql.append("\n  and ai.status_cd ").append(getInCondition(statusCodes)).append(") a\n");
            else if (by.equals("throughput"))
                sql.append("\n ) a  where act_unique_id ").append(getInCondition(actIds));
            else if (by.equals("total"))
                sql.append("\n ) a");

            sql.append(" group by st");
            if (by.equals("status"))
                sql.append(", status_cd");
            else if (by.equals("throughput"))
                sql.append(", act_unique_id");
            if (db.isMySQL())
                sql.append("\norder by st");
            else
                sql.append("\norder by to_date(st, 'DD-Mon-yyyy')");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            TreeMap<Date,List<ActivityAggregate>> map = new TreeMap<>();
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
                List<ActivityAggregate> activityAggregates = map.get(startDate);
                if (activityAggregates == null) {
                    activityAggregates = new ArrayList<>();
                    map.put(startDate, activityAggregates);
                }
                ActivityAggregate activityAggregate = new ActivityAggregate(rs.getLong("ct"));
                if (by.equals("status")) {
                    int statusCode = rs.getInt("status_cd");
                    activityAggregate.setName(WorkStatuses.getName(statusCode));
                    activityAggregate.setId(statusCode);
                }
                else if (by.equals("throughput")) {
                    String actId = rs.getString("act_unique_id");
                    activityAggregate.setActivityId(actId);
                    int colon = actId.lastIndexOf(":");
                    activityAggregate.setProcessId(new Long(actId.substring(0, colon)));
                    activityAggregate.setDefinitionId(actId.substring(colon + 1));
                }
                else if (!by.equals("total")) {
                }

                activityAggregates.add(activityAggregate);
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

    protected String getActivityWhereClause(Query query) throws ParseException, DataAccessException {
        String by = query.getFilter("by");
        Date start = getStartDate(query);

        StringBuilder where = new StringBuilder();

        if (by.equals("throughput"))
            where.append(" where ai.process_instance_id = pi.process_instance_id ");
        where.append(where.length() > 0 ? "and " : "where ");
        if (db.isMySQL())
            where.append("ai.start_dt >= '").append(getMySqlDt(start)).append("' ");
        else
            where.append("ai.start_dt >= '").append(getOracleDt(start)).append("' ");
        Date end = getEndDate(query);
        if (end != null) {
            if (db.isMySQL())
                where.append("and ai.start_dt <= '").append(getMySqlDt(end)).append("' ");
            else
                where.append("and ai.start_dt <= '").append(getOracleDt(end)).append("' ");
        }

        where.append(" and ai.status_cd in (").append(WorkStatus.STATUS_IN_PROGRESS).append(",")
                .append(WorkStatus.STATUS_FAILED).append(",").append(WorkStatus.STATUS_WAITING).append(")");

        String status = query.getFilter("Status");
        if (status != null)
            where.append("and ai.STATUS_CD = ").append(WorkStatuses.getCode(status));

        return where.toString();
    }

}
