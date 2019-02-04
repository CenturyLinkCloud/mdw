package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.report.Insight;
import com.centurylink.mdw.model.report.Timepoint;
import com.centurylink.mdw.model.workflow.WorkStatuses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import static com.centurylink.mdw.dataaccess.reports.AggregateDataAccess.getRoundDate;

public class ProcessInsights extends CommonDataAccess {

    public List<Insight> getInsights(Query query) throws SQLException, ParseException, ServiceException {
        PreparedWhere where = getWhere(query, false);
        String sql = "select count(pi.status_cd) as ct, pi.st, pi.status_cd\n" +
                "from (select date(start_dt) as st, status_cd\n" +
                "  from PROCESS_INSTANCE\n" + where.getWhere() + ") pi\n" +
                "group by st, status_cd\n" +
                "order by st";

        try {
            db.openConnection();
            List<Insight> insights = new ArrayList<>();
            Date prevStart = getStartDate(query);
            ResultSet rs = db.runSelect("Process insights ", sql, where.getParams());
            while (rs.next()) {
                String startStr = rs.getString("st");
                Date start = getDateFormat().parse(startStr);

                // fill in gaps
                while (start.getTime() - prevStart.getTime() > Query.Timespan.Day.millis()) {
                    prevStart = new Date(prevStart.getTime() + Query.Timespan.Day.millis());
                    insights.add(new Insight(getRoundDate(prevStart).toInstant(), new LinkedHashMap<>()));
                }

                Insight insight = insights.stream().filter(in -> in.getTime().equals(start.toInstant())).findAny().orElse(null);
                if (insight == null) {
                    LinkedHashMap<String,Integer> map = new LinkedHashMap<>();
                    insight = new Insight(getRoundDate(start).toInstant(), map);
                    insights.add(insight);
                }

                String status = WorkStatuses.getName(rs.getInt("status_cd"));
                insight.getElements().put(status, rs.getInt("ct"));

                prevStart = start;
            }
            // gaps at end
            Date endDate = new Date(System.currentTimeMillis() + DatabaseAccess.getDbTimeDiff());
            while ((endDate.getTime() - prevStart.getTime()) > Query.Timespan.Day.millis()) {
                prevStart = new Date(prevStart.getTime() + Query.Timespan.Day.millis());
                insights.add(new Insight(getRoundDate(prevStart).toInstant(), new LinkedHashMap<>()));
            }

            return insights;
        }
        finally {
            db.closeConnection();
        }
    }

    public List<Timepoint> getTrend(Query query) throws SQLException, ParseException, ServiceException {
        PreparedWhere where = getWhere(query, true);
        String sql = "select avg(pi.elapsed_ms) as ms, pi.st\n" +
                "from (select date(start_dt) as st, elapsed_ms\n" +
                "  from PROCESS_INSTANCE, INSTANCE_TIMING\n" + where.getWhere() + ") pi\n" +
                "group by st\n" +
                "order by st";

        try {
            db.openConnection();
            List<Timepoint> timepoints = new ArrayList<>();
            ResultSet rs = db.runSelect("Process trend ", sql, where.getParams());
            while (rs.next()) {
                String startStr = rs.getString("st");
                Date start = getDateFormat().parse(startStr);

                timepoints.add(new Timepoint(getRoundDate(start).toInstant(), Math.round(rs.getDouble("ms"))));
            }
            return timepoints;
        }
        finally {
            db.closeConnection();
        }
    }

    private PreparedWhere getWhere(Query query, boolean isTrend) throws ServiceException {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        where.append("  where start_dt >= ?\n");
        params.add(getDt(getStartDate(query)));

        where.append("  and process_id = ?\n");
        params.add(getProcessId(query));

        if (isTrend) {
            String trend = query.getFilter("trend");
            if ("completionTime".equals(trend)) {
                where.append("  and owner_type = ?\n");
                params.add("PROCESS_INSTANCE");
                where.append("  and instance_id = process_instance_id");
            }
        }

        return new PreparedWhere(where.toString(), params.toArray());
    }

    private Long getProcessId(Query query) throws ServiceException {
        long processId = query.getLongFilter("processId");
        if (processId == -1)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing query param: processId");
        return processId;
    }

    private Date getStartDate(Query query) throws ServiceException {
        Query.Timespan span = query.getTimespanFilter("span");
        if (span == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid span: " + query.getFilter("span"));
        return new Date(System.currentTimeMillis() - span.millis() + DatabaseAccess.getDbTimeDiff());
    }
}
