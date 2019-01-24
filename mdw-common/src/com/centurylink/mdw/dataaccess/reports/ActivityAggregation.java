package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.PreparedSelect;
import com.centurylink.mdw.dataaccess.PreparedWhere;
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
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getActivityWhere(query);
        String sql = "select count(act_unique_id) as ct, act_unique_id\n" +
                getUniqueIdFrom() +
                preparedWhere.getWhere() +
                ") a1\n" +
                "group by act_unique_id\n" +
                "order by ct desc\n";

        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),"ActivityAggregation.getTopsByStuck()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            ActivityAggregate activityAggregate = new ActivityAggregate(resultSet.getLong("ct"));
            String actId = resultSet.getString("act_unique_id");
            activityAggregate.setActivityId(actId);
            int colon = actId.lastIndexOf(":");
            activityAggregate.setProcessId(new Long(actId.substring(0, colon)));
            activityAggregate.setDefinitionId(actId.substring(colon + 1));
            return activityAggregate;
        });
    }

    private String getUniqueIdFrom() {
        if (db.isMySQL())
            return "from (select CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as ACT_UNIQUE_ID from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi ";
        else
            return "from (select pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as ACT_UNIQUE_ID from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi ";
    }

    private List<ActivityAggregate> getTopsByStatus(Query query)
            throws ParseException, DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getActivityWhere(query);
        String sql = "select status_cd, count(status_cd) as ct " +
                "from ACTIVITY_INSTANCE ai\n" +
                preparedWhere.getWhere() +
                "group by status_cd\n" +
                "order by ct desc\n";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),"ActivityAggregation.getTopsByStatus()");
        return getTopAggregates(query, preparedSelect, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            ActivityAggregate activityAggregate = new ActivityAggregate(ct);
            activityAggregate.setCount(ct);
            activityAggregate.setId(resultSet.getInt("status_cd"));
            return activityAggregate;
        });
    }

    public TreeMap<Date,List<ActivityAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");

        try {
            PreparedWhere preparedWhere = getActivityWhere(query);
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
            sql.append("  from ACTIVITY_INSTANCE ai ");
            if (by.equals("throughput"))
                sql.append(", PROCESS_INSTANCE pi");
            sql.append("\n");
            sql.append(preparedWhere.getWhere());
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
                sql.append("   and ai.status_cd ").append(inCondition.getWhere()).append(") a\n");
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            else if (by.equals("throughput")) {
                // activity ids (processid:logicalId)
                String[] actIdsArr = query.getArrayFilter("activityIds");
                List<String> actIds = actIdsArr == null ? null : Arrays.asList(actIdsArr);
                PreparedWhere inCondition = getInCondition(actIds);
                sql.append("\n ) a  where act_unique_id ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
            }
            else if (by.equals("total")) {
                sql.append("\n ) a");
            }

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
            ResultSet rs = db.runSelect("Breakdown by " + by, sql.toString(), params.toArray());
            TreeMap<Date,List<ActivityAggregate>> map = new TreeMap<>();
            Date prevStartDate = getStartDate(query);
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
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

    protected PreparedWhere getActivityWhere(Query query) throws ParseException, DataAccessException {
        String by = query.getFilter("by");
        Date start = getStartDate(query);

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (by.equals("throughput"))
            where.append(" where ai.process_instance_id = pi.process_instance_id ");
        where.append(where.length() > 0 ? "  and " : "where ");
        where.append("ai.start_dt >= ? ");
        params.add(getDt(start));
        Date end = getEndDate(query);
        if (end != null) {
            where.append("  and ai.start_dt <= ?");
            params.add(getDt(end));
        }

        String status = query.getFilter("Status");
        if (status != null) {
            where.append("  and ai.status_cd = ?\n");
            params.add(WorkStatuses.getCode(status));
        }

        where.append(" and ai.status_cd in (?,?,?)");
        params.add(WorkStatus.STATUS_IN_PROGRESS);
        params.add(WorkStatus.STATUS_FAILED);
        params.add(WorkStatus.STATUS_WAITING);

        return new PreparedWhere(where.toString(), params.toArray());
    }

}
