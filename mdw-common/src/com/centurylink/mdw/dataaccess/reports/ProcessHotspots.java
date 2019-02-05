package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.model.report.Hotspot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ProcessHotspots extends ProcessInsights {

    public List<Hotspot> getHotspots(Query query) throws SQLException, ParseException, ServiceException {

        PreparedWhere where = getWhere(query);
        String sql = "select avg(it.elapsed_ms) as avg_time, ai.activity_id\n" +
                "from ACTIVITY_INSTANCE ai, PROCESS_INSTANCE pi, INSTANCE_TIMING it\n" +
                where.getWhere() + "\ngroup by ai.activity_id\norder by avg_time desc";

        try {
            db.openConnection();
            List<Hotspot> hotspots = new ArrayList<>();
            ResultSet rs = db.runSelect("Process hotspots ", sql, where.getParams());
            while (rs.next()) {
                hotspots.add(new Hotspot(rs.getString("activity_id"), rs.getLong("avg_time")));
            }
            return hotspots;
        }
        finally {
            db.closeConnection();
        }
    }

    private PreparedWhere getWhere(Query query) throws ServiceException {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        where.append("where ai.process_instance_id = pi.process_instance_id\n");

        where.append("and process_id = ?\n");
        params.add(getProcessId(query));

        where.append("and owner_type = ?\n");
        params.add("ACTIVITY_INSTANCE");

        where.append("and ai.activity_instance_id = it.instance_id\n");

        where.append("and ai.start_dt >= ?");
        params.add(getDt(getStartDate(query)));

        return new PreparedWhere(where.toString(), params.toArray());
    }

}
