package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.report.Insight;
import com.centurylink.mdw.model.report.Timepoint;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import static com.centurylink.mdw.dataaccess.reports.AggregateDataAccess.getRoundDate;

public class RequestInsights extends CommonDataAccess {

    public List<Insight> getInsights(Query query) throws SQLException, ParseException, ServiceException {
        PreparedWhere where = getWhere(query, false);
        String sql = "select count(req.status_code) as ct, req.st, req.status_code\n" +
                "from (select date(create_dt) as st, status_code \n" +
                "  from DOCUMENT\n" + where.getWhere() + ") req\n" +
                "group by st, status_code\n" +
                "order by st";

        try {
            db.openConnection();
            List<Insight> insights = new ArrayList<>();
            Date prevStart = getStartDate(query);
            ResultSet rs = db.runSelect("Request insights ", sql, where.getParams());
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

                String status = String.valueOf(rs.getInt("status_code"));
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
        String sql = "select avg(req.elapsed_ms) as ms, req.st\n" +
                "from (select date(create_dt) as st, elapsed_ms\n" +
                "  from DOCUMENT, INSTANCE_TIMING\n" + where.getWhere() + ") req\n" +
                "group by st\n" +
                "order by st";

        try {
            db.openConnection();
            List<Timepoint> timepoints = new ArrayList<>();
            ResultSet rs = db.runSelect("Request trend ", sql, where.getParams());
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

        where.append("  where path = ?\n");
        params.add(getPath(query));

        String ownerType = OwnerType.LISTENER_RESPONSE;
        if ("out".equals(query.getFilter("direction"))) {
            if (isTrend)
                ownerType = OwnerType.ADAPTER;
            else
                ownerType = OwnerType.ADAPTER_RESPONSE;
        }

        where.append("  and DOCUMENT.owner_type = ?\n");
        params.add(ownerType);

        where.append("  and create_dt >= ?\n");
        params.add(getDt(getStartDate(query)));

        if (isTrend) {
            String trend = query.getFilter("trend");
            if ("completionTime".equals(trend)) {
                where.append("  and instance_id = document_id");
            }
        }

        return new PreparedWhere(where.toString(), params.toArray());
    }

    private String getPath(Query query) throws ServiceException {
        String path = query.getFilter("path");
        if (path == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing query param: path");
        return path;
    }

    private Date getStartDate(Query query) throws ServiceException {
        Query.Timespan span = query.getTimespanFilter("span");
        if (span == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid span: " + query.getFilter("span"));
        return new Date(System.currentTimeMillis() - span.millis() + DatabaseAccess.getDbTimeDiff());
    }
}
