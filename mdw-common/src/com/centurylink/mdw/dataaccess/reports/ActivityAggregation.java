package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.workflow.ActivityAggregate;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;

import java.sql.ResultSet;
import java.text.ParseException;
import java.util.*;

public class ActivityAggregation extends CommonDataAccess implements AggregateDataAccess {

    public List<ActivityAggregate> getTops(Query query) throws DataAccessException {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(act_unique_id) as ct, act_unique_id\n");
            if (db.isMySQL())
                sql.append("from (select CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as ACT_UNIQUE_ID from activity_instance ai, process_instance pi ");
            else
                sql.append("from (select pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as ACT_UNIQUE_ID from activity_instance ai, process_instance pi ");
            sql.append(getActivityWhereClause(query));
            sql.append(") a1\n");
            sql.append("group by act_unique_id\n");
            sql.append("order by ct desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            List<ActivityAggregate> list = new ArrayList<>();
            int idx = 0;
            int limit = query.getIntFilter("limit");
            while (rs.next() && (limit == -1 || idx < limit)) {
                ActivityAggregate actCount = new ActivityAggregate(rs.getLong("ct"));
                String actId = rs.getString("act_unique_id");
                actCount.setActivityId(actId);
                int colon = actId.lastIndexOf(":");
                actCount.setProcessId(new Long(actId.substring(0, colon)));
                actCount.setDefinitionId(actId.substring(colon + 1));
                list.add(actCount);
                idx++;
            }
            return list;
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

    public TreeMap<Date,List<ActivityAggregate>> getBreakdown(Query query) throws DataAccessException {
        try {
            // activity ids (processid:logicalId)
            String[] actIdsArr = query.getArrayFilter("activityIds");
            List<String> actIds = actIdsArr == null ? null : Arrays.asList(actIdsArr);
            // by status
            String[] statuses = query.getArrayFilter("statuses");
            List<Integer> statusCodes = null;
            if (statuses != null) {
                statusCodes = new ArrayList<Integer>();
                for (String status : statuses)
                    statusCodes.add(WorkStatuses.getCode(status));
            }
            if (actIds != null && statuses != null)
                throw new DataAccessException("Conflicting parameters: activityIds and statuses");

            StringBuilder sql = new StringBuilder();
            if (statusCodes != null)
                sql.append("select count(a.status_cd) as ct, a.st, a.status_cd\n");
            else if (actIds != null)
                sql.append("select count(a.act_unique_id) as ct, a.st, a.act_unique_id\n");
            else
                sql.append("select count(a.st) as ct, a.st\n");

            if (db.isMySQL())
                sql.append("from (select DATE_FORMAT(ai.start_dt,'%d-%M-%Y') as st");
            else
                sql.append("from (select to_char(ai.start_dt,'DD-Mon-yyyy') as st");
            if (statusCodes != null)
                sql.append(", ai.status_cd ");
            else if (actIds != null) {
                if (db.isMySQL())
                    sql.append(", CONCAT(pi.PROCESS_ID, ':A', ai.ACTIVITY_ID) as act_unique_id");
                else
                    sql.append(", pi.PROCESS_ID || ':A' || ai.ACTIVITY_ID as act_unique_id");
            }
            sql.append("  from activity_instance ai, process_instance pi\n   ");
            sql.append(getActivityWhereClause(query));
            if (statusCodes != null)
                sql.append("\n  and ai.status_cd ").append(getInCondition(statusCodes)).append(") a\n");
            else if (actIds != null)
                sql.append("\n ) a  where act_unique_id ").append(getInCondition(actIds));
            else
                sql.append("\n ) a");

            sql.append(" group by st");
            if (statusCodes != null)
                sql.append(", status_cd");
            else if (actIds != null)
                sql.append(", act_unique_id");
            if (db.isMySQL())
                sql.append("\norder by STR_TO_DATE(st, '%d-%M-%Y') desc\n");
            else
                sql.append("\norder by to_date(st, 'DD-Mon-yyyy') desc\n");

            db.openConnection();
            ResultSet rs = db.runSelect(sql.toString());
            TreeMap<Date,List<ActivityAggregate>> map = new TreeMap<>();
            while (rs.next()) {
                String startDateStr = rs.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                List<ActivityAggregate> actCounts = map.get(startDate);
                if (actCounts == null) {
                    actCounts = new ArrayList<>();
                    map.put(startDate, actCounts);
                }
                ActivityAggregate actCount = new ActivityAggregate(rs.getLong("ct"));
                if (statusCodes != null)
                    actCount.setName(WorkStatuses.getName(rs.getInt("status_cd")));
                else if (actIds != null) {
                    String actId = rs.getString("act_unique_id");
                    actCount.setActivityId(actId);
                    int colon = actId.lastIndexOf(":");
                    actCount.setProcessId(new Long(actId.substring(0, colon)));
                    actCount.setDefinitionId(actId.substring(colon + 1));
                }
                actCounts.add(actCount);
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
        Date start = query.getDateFilter("startDate");
        if (start == null)
            throw new DataAccessException("Parameter startDate is required");
        String startStr = getDateFormat().format(start);

        StringBuilder where = new StringBuilder();

        where.append(" where ai.process_instance_id=pi.PROCESS_INSTANCE_ID");

        if (db.isMySQL())
            where.append(" AND ai.start_dt >= STR_TO_DATE('" + startStr + "','%d-%M-%Y') ");
        else
            where.append(" AND ai.start_dt >= '" + startStr + "' ");

        where.append(" AND pi.STATUS_CD NOT IN (" +  WorkStatus.STATUS_COMPLETED.intValue() + "," + WorkStatus.STATUS_CANCELLED.intValue() + "," + WorkStatus.STATUS_PURGE.intValue() + ")");

        if (query.getArrayFilter("statuses") == null)
            where.append(" AND ai.STATUS_CD IN (" +  WorkStatus.STATUS_FAILED.intValue() + "," + WorkStatus.STATUS_WAITING.intValue() + "," + WorkStatus.STATUS_IN_PROGRESS.intValue() + "," + WorkStatus.STATUS_HOLD.intValue() + ")");

        return where.toString();
    }

}
