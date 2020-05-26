package com.centurylink.mdw.service.data.event;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.event.EventLog;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class EventDataAccess extends CommonDataAccess  {

    public List<EventLog> getEventLogs(String eventName, String source,
            String ownerType, Long ownerId) throws DataAccessException {
        try {
            db.openConnection();
            StringBuffer query = new StringBuffer();
            query.append("select EVENT_LOG_ID, EVENT_NAME, EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID,");
            query.append("  EVENT_SOURCE, CREATE_DT, EVENT_CATEGORY, EVENT_SUB_CATEGORY,");
            query.append("  COMMENTS, CREATE_USR ");
            query.append("  FROM EVENT_LOG ");
            query.append("where ");
            Vector<Object> args = new Vector<Object>();
            if (eventName!=null) {
                if (args.size()>0) query.append(" and ");
                query.append("EVENT_NAME=?");
                args.add(eventName);
            }
            if (source!=null) {
                if (args.size()>0) query.append(" and ");
                query.append("EVENT_SOURCE=?");
                args.add(source);
            }
            if (ownerType!=null) {
                if (args.size()>0) query.append(" and ");
                query.append("EVENT_LOG_OWNER=?");
                args.add(ownerType);
            }
            if (ownerId!=null) {
                if (args.size()>0) query.append(" and ");
                query.append("EVENT_LOG_OWNER_ID=?");
                args.add(ownerId);
            }
            ResultSet rs = db.runSelect(query.toString(), args.toArray());
            List<EventLog> ret = new ArrayList<EventLog>();
            while (rs.next()) {
                EventLog el = new EventLog();
                el.setId(rs.getLong(1));
                el.setEventName(rs.getString(2));
                el.setOwnerType(rs.getString(3));
                el.setOwnerId(rs.getLong(4));
                el.setSource(rs.getString(5));
                el.setCreateDate(rs.getTimestamp(6).toString());
                el.setCategory(rs.getString(7));
                el.setSubCategory(rs.getString(8));
                el.setComment(rs.getString(9));
                el.setCreateUser(rs.getString(10));
                ret.add(el);
            }
            return ret;
        } catch (Exception e) {
            throw new DataAccessException(0,"failed to find task instance", e);
        } finally {
            db.closeConnection();
        }
    }
}
