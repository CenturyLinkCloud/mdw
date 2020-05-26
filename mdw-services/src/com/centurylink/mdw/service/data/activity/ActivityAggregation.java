package com.centurylink.mdw.service.data.activity;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.PreparedSelect;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.dataaccess.reports.AggregateDataAccess;
import com.centurylink.mdw.model.workflow.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class ActivityAggregation extends AggregateDataAccess<ActivityAggregate> {

    public static Integer[] STUCK = new Integer[] {
            WorkStatus.STATUS_IN_PROGRESS,
            WorkStatus.STATUS_FAILED,
            WorkStatus.STATUS_WAITING
    };

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
            else if (by.equals("completionTime"))
                return getTopsByCompletionTime(query);
            else
                throw new ServiceException(ServiceException.BAD_REQUEST, "Unsupported filter: by=" + by);
        }
        catch (SQLException | IOException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        finally {
            db.closeConnection();
        }
    }

    private List<ActivityAggregate> getTopsByStuck(Query query)
            throws DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getActivityWhere(query);
        String sql = "select count(act_unique_id) as ct, act_unique_id\n" +
                getUniqueIdFrom() +
                preparedWhere.getWhere() +
                ") a1\n" +
                "group by act_unique_id\n" +
                "order by ct desc\n";

        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),"ActivityAggregation.getTopsByStuck()");
        return getTopAggregates(preparedSelect, query, resultSet -> {
            ActivityAggregate activityAggregate = new ActivityAggregate(resultSet.getLong("ct"));
            String actId = resultSet.getString("act_unique_id");
            activityAggregate.setActivityId(actId);
            int colon = actId.lastIndexOf(":");
            activityAggregate.setProcessId(new Long(actId.substring(0, colon)));
            activityAggregate.setDefinitionId(actId.substring(colon + 1));
            return activityAggregate;
        });
    }

    private List<ActivityAggregate> getTopsByStatus(Query query)
            throws DataAccessException, SQLException, ServiceException {
        PreparedWhere preparedWhere = getActivityWhere(query);
        String sql = "select status_cd, count(status_cd) as ct " +
                "from ACTIVITY_INSTANCE ai\n" +
                preparedWhere.getWhere() +
                "group by status_cd\n" +
                "order by ct desc\n";
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),"ActivityAggregation.getTopsByStatus()");
        return getTopAggregates(preparedSelect, query, resultSet -> {
            long ct = Math.round(resultSet.getDouble("ct"));
            ActivityAggregate activityAggregate = new ActivityAggregate(ct);
            activityAggregate.setCount(ct);
            activityAggregate.setId(resultSet.getInt("status_cd"));
            return activityAggregate;
        });
    }

    private List<ActivityAggregate> getTopsByCompletionTime(Query query)
            throws IOException, SQLException, ServiceException, DataAccessException {
        PreparedWhere preparedWhere = getActivityWhere(query);
        String excluded = getExcludedClause(query);

        String sql = db.pagingQueryPrefix() +
                "select avg(elapsed_ms) as elapsed, count(act_unique_id) as ct, act_unique_id\n" +
                getUniqueIdFrom() +
                preparedWhere.getWhere() +
                ") a1, INSTANCE_TIMING\n" +
                "where owner_type = 'ACTIVITY_INSTANCE' and instance_id = activity_instance_id\n" +
                (excluded == null ? "" : (" and " + excluded + "\n")) +
                "group by act_unique_id\n" +
                "order by elapsed desc\n" +
                db.pagingQuerySuffix(query.getStart(), query.getMax());
        PreparedSelect preparedSelect = new PreparedSelect(sql, preparedWhere.getParams(),
                "ActivityAggregation.getTopsByCompletionTime()");
        List<ActivityAggregate> aggregates = getTopAggregates(preparedSelect, query, resultSet -> {
            ActivityAggregate activityAggregate = new ActivityAggregate(resultSet.getLong("ct"));
            Long elapsed = Math.round(resultSet.getDouble("elapsed"));
            activityAggregate.setValue(elapsed);
            activityAggregate.setCount(resultSet.getLong("ct"));
            String actId = resultSet.getString("act_unique_id");
            activityAggregate.setActivityId(actId);
            int colon = actId.lastIndexOf(":");
            activityAggregate.setProcessId(new Long(actId.substring(0, colon)));
            activityAggregate.setDefinitionId(actId.substring(colon + 1));
            return activityAggregate;
        });
        CompletionTimeUnit units = getTimeUnit(query);
        if (units != CompletionTimeUnit.Milliseconds) {
            aggregates.forEach(aggregate -> {
                aggregate.setValue(units.convert(aggregate.getValue(), CompletionTimeUnit.Milliseconds));
            });
        }
        return aggregates;
    }

    private String getUniqueIdFrom() {
        if (db.isMySQL())
            return "from (select CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as ACT_UNIQUE_ID, ai.activity_instance_id from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi ";
        else
            return "from (select pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as ACT_UNIQUE_ID, ai.activity_instance_id from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi ";
    }

    public TreeMap<Instant,List<ActivityAggregate>> getBreakdown(Query query) throws DataAccessException, ServiceException {
        String by = query.getFilter("by");
        if (by == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing required filter: 'by'");

        try {
            PreparedWhere preparedWhere = getActivityWhere(query);
            StringBuilder sql = new StringBuilder();
            if (by.equals("status"))
                sql.append("select count(a.status_cd) as val, a.st, a.status_cd\n");
            else if (by.equals("throughput"))
                sql.append("select count(a.act_unique_id) as val, a.st, a.act_unique_id\n");
            else if (by.equals("completionTime"))
                sql.append("select avg(elapsed_ms) as val, a.st, a.act_unique_id\n");
            else if (by.equals("total"))
                sql.append("select count(a.st) as val, a.st\n");

            sql.append("from (select ").append(getSt("ai.start_dt", query));

            if (by.equals("status"))
                sql.append(", ai.status_cd ");
            else if (by.equals("throughput") || by.equals("completionTime")) {
                if (db.isMySQL())
                    sql.append(", CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as act_unique_id");
                else
                    sql.append(", pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as act_unique_id");

                if (by.equals("completionTime"))
                    sql.append(", ai.activity_instance_id");
            }

            sql.append("  from ACTIVITY_INSTANCE ai ");
            if (by.equals("throughput") || by.equals("completionTime"))
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
            else if (by.equals("throughput") || by.equals("completionTime")) {
                sql.append("\n ) a");
                if (by.equals("completionTime"))
                    sql.append(", INSTANCE_TIMING");
                // activity ids (processid:logicalId)
                String[] actIdsArr = query.getArrayFilter("activityIds");
                List<String> actIds = actIdsArr == null ? null : Arrays.asList(actIdsArr);
                PreparedWhere inCondition = getInCondition(actIds);
                sql.append("\nwhere act_unique_id ").append(inCondition.getWhere());
                params.addAll(Arrays.asList(inCondition.getParams()));
                if (by.equals("completionTime"))
                    sql.append("\nand owner_type = 'ACTIVITY_INSTANCE' and instance_id = activity_instance_id  ");
            }
            else if (by.equals("total")) {
                sql.append("\n ) a");
            }

            sql.append("\ngroup by st");
            if (by.equals("status"))
                sql.append(", status_cd");
            else if (by.equals("throughput") || by.equals("completionTime"))
                sql.append(", act_unique_id");

            sql.append("\norder by st\n");

            CompletionTimeUnit timeUnit = getTimeUnit(query);

            PreparedSelect select = new PreparedSelect(sql.toString(), params.toArray(), "Breakdown by " + by);
            return handleBreakdownResult(select, query, rs -> {
                ActivityAggregate activityAggregate = new ActivityAggregate(rs.getLong("val"));
                if (by.equals("status")) {
                    int statusCode = rs.getInt("status_cd");
                    activityAggregate.setName(WorkStatuses.getName(statusCode));
                    activityAggregate.setId(statusCode);
                }
                else if (by.equals("throughput") || by.equals("completionTime")) {
                    String actId = rs.getString("act_unique_id");
                    activityAggregate.setActivityId(actId);
                    int colon = actId.lastIndexOf(":");
                    activityAggregate.setProcessId(new Long(actId.substring(0, colon)));
                    activityAggregate.setDefinitionId(actId.substring(colon + 1));
                    if (by.equals("completionTime") && timeUnit != CompletionTimeUnit.Milliseconds) {
                        activityAggregate.setValue(timeUnit.convert(activityAggregate.getValue(), CompletionTimeUnit.Milliseconds));
                    }
                }
                return activityAggregate;
            });
        }
        catch (DataAccessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    protected PreparedWhere getActivityWhere(Query query) throws DataAccessException {
        String by = query.getFilter("by");
        Instant start = getStart(query);

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (by.equals("throughput") || by.equals("completionTime"))
            where.append(" where ai.process_instance_id = pi.process_instance_id ");
        where.append(where.length() > 0 ? "  and " : "where ");
        where.append("ai.start_dt >= ? ");
        params.add(getDbDt(start));
        Instant end = getEnd(query);
        if (end != null) {
            where.append("  and ai.start_dt <= ?");
            params.add(getDbDt(end));
        }

        Integer[] statuses = new Integer[0];
        if (by.equals("throughput")) {
            statuses = STUCK;
        }
        String status = query.getFilter("Status");
        if (status != null) {
            where.append("  and ai.status_cd = ?\n");
            params.add(WorkStatuses.getCode(status));
        }
        else if (by.equals("completionTime")) {
            statuses = new Integer[]{WorkStatus.STATUS_COMPLETED};
        }
        if (statuses.length > 0) {
            where.append(" and ai.status_cd in (");
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0)
                    where.append(",");
                where.append("?");
                params.add(statuses[i]);
            }
            where.append(")");
        }

        return new PreparedWhere(where.toString(), params.toArray());
    }

    private String getExcludedClause(Query query) throws IOException {
        boolean excludeLongRunning = query.getBooleanFilter("Exclude Long-Running");
        if (excludeLongRunning) {
            List<String> excluded = new ArrayList<>();
            Map<String,ActivityImplementor> implementors = ImplementorCache.getImplementors();
            for (String implementorClass : implementors.keySet()) {
                ActivityImplementor implementor = implementors.get(implementorClass);
                if (implementor.isLongRunning()) {
                    for (Activity activity : ActivityCache.getActivities(implementorClass, true)) {
                        String uniqueId = activity.getProcessId() + ":A" + activity.getId();
                        if (!excluded.contains(uniqueId))
                            excluded.add(uniqueId);
                    }
                }
            }
            if (!excluded.isEmpty()) {
                return "act_unique_id not in ('" + String.join("','", excluded) + "')";
            }
        }
        return null;
    }
}
