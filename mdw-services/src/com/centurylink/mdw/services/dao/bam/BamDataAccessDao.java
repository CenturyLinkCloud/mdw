/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.bam;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.bam.Attribute;
import com.centurylink.mdw.model.data.bam.Component;
import com.centurylink.mdw.model.data.bam.ComponentRelation;
import com.centurylink.mdw.model.data.bam.Event;
import com.centurylink.mdw.model.data.bam.MasterRequest;

public class BamDataAccessDao implements BamDataAccess {

	private final static String MASTER_REQUEST_SEQ_NAME = "BAM_MASTER_REQUEST_ID_SEQ";
	private final static String COMPONENT_SEQ_NAME = "BAM_MASTER_REQUEST_ID_SEQ";
	private final static String EVENT_SEQ_NAME = "BAM_EVENT_ID_SEQ";

	// TODO master request and component table need to add column status
	// TODO BAM_EVENT - need to add COMPONENT_ROWID column - use NOTE currently
	// TODO add MASTER_REQUEST_ROWID column to BAM_COMPONENT_RELATION for performance reason

	@Override
    public List<MasterRequest> loadMasterRequests(DatabaseAccess db, Date from, Date to) throws SQLException {
		String query = "select MASTER_REQUEST_ROWID, MASTER_REQUEST_ID," +
				"SOURCE_SYSTEM, REQUEST_TIME, RECORD_TIME " +
				"from BAM_MASTER_REQUEST " +
				"where REQUEST_TIME>=? and REQUEST_TIME<=? " +
				"order by REQUEST_TIME desc";
		List<MasterRequest> results = new ArrayList<MasterRequest>();
		Object[] args = new Object[2];
		Calendar cal = Calendar.getInstance();
		cal.setTime(from);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		from.setTime(cal.getTimeInMillis());
		cal.setTime(to);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		to.setTime(cal.getTimeInMillis());
		args[0] = from;
		args[1] = to;
		ResultSet rs = db.runSelect(query, args);
		while (rs.next()) {
			MasterRequest masterRequest = new MasterRequest();
			masterRequest.setMasterRequestId(rs.getString(2));
			masterRequest.setRowId(rs.getLong(1));
			masterRequest.setRealm(rs.getString(3));
			masterRequest.setCreateTime(rs.getTimestamp(4));
			masterRequest.setRecordTime(rs.getTimestamp(5));
			results.add(masterRequest);
		}
		return results;
	}

	@Override
    public List<MasterRequest> loadMasterRequests(DatabaseAccess db, String queryRest) throws SQLException {
		String query = "select m.MASTER_REQUEST_ROWID, m.MASTER_REQUEST_ID," +
				"m.SOURCE_SYSTEM, m.REQUEST_TIME, m.RECORD_TIME " + queryRest;
		List<MasterRequest> results = new ArrayList<MasterRequest>();
		ResultSet rs = db.runSelect(query, null);
		while (rs.next()) {
			MasterRequest masterRequest = new MasterRequest();
			masterRequest.setMasterRequestId(rs.getString(2));
			masterRequest.setRowId(rs.getLong(1));
			masterRequest.setRealm(rs.getString(3));
			masterRequest.setCreateTime(rs.getTimestamp(4));
			masterRequest.setRecordTime(rs.getTimestamp(5));
			results.add(masterRequest);
		}
		return results;
	}

    @Override
    public MasterRequest loadMasterRequest(DatabaseAccess db, String masterRequestId, String realm,
            int loadLevel) throws SQLException {
        // TODO Auto-generated method stub
        return loadMasterRequest(db,  masterRequestId,  realm, loadLevel,  false);
    }

	@Override
    public MasterRequest loadMasterRequest(DatabaseAccess db,
			String masterRequestId, String realm, int loadLevel, boolean lock) throws SQLException {
		String query = "select MASTER_REQUEST_ROWID, REQUEST_TIME, RECORD_TIME " +
			"from BAM_MASTER_REQUEST " +
			"where MASTER_REQUEST_ID=? and SOURCE_SYSTEM=? ";
		if (lock)
		    query += " for update \n";
		List<MasterRequest> results = new ArrayList<MasterRequest>();
		Object[] args = new Object[2];
		args[0] = masterRequestId;
		args[1] = realm;
		ResultSet rs = db.runSelect(query, args);
		MasterRequest masterRequest;
		if (rs.next()) {
			masterRequest = new MasterRequest();
			masterRequest.setMasterRequestId(masterRequestId);
			masterRequest.setRowId(rs.getLong(1));
			masterRequest.setRealm(realm);
			masterRequest.setCreateTime(rs.getTimestamp(2));
			masterRequest.setRecordTime(rs.getTimestamp(3));
			results.add(masterRequest);
		} else return null;
		if (loadLevel>=2) {
			List<Attribute> attrlist = loadAttributes(db, masterRequest.getRowId(), null);
			masterRequest.setAttributes(new ArrayList<Attribute>());
			if (attrlist!=null) {
				for (Attribute a : attrlist) {
					Component c = a.getComponentRowId()==null?null:masterRequest.findComponent(a.getComponentRowId());
					if (c==null) masterRequest.getAttributes().add(a);
					else c.getAttributes().add(a);
				}
			}
		}
		if (loadLevel>=3) {
			List<Event> eventlist = this.loadEvents(db, masterRequest.getRowId());
			masterRequest.setEvents(new ArrayList<Event>());
			for (Event e : eventlist) {
				Component c = e.getComponentRowId()==null?null:masterRequest.findComponent(e.getComponentRowId());
				if (c==null) {
					masterRequest.getEvents().add(e);
					if (e.getEventName().equalsIgnoreCase(Event.EVENT_NAME_SUBMIT))
						masterRequest.setCurrentStatus("PENDING");
					else if (e.getEventName().equalsIgnoreCase(Event.EVENT_NAME_STATUS))
						masterRequest.setCurrentStatus(e.getEventData());
				} else {
					c.getEvents().add(e);
					if (e.getEventName().equalsIgnoreCase(Event.EVENT_NAME_STATUS))
						c.setCurrentStatus(e.getEventData());
				}
			}
		}
		return masterRequest;
	}

	@Override
    public Component loadComponent(DatabaseAccess db, MasterRequest masterRequest,
			String componentId) throws SQLException {
		String query = "select COMPONENT_ROWID,COMPONENT_TYPE " +
			"from BAM_COMPONENT where MASTER_REQUEST_ROWID=? and COMPONENT_ID=?";
		Object[] args = new Object[2];
		args[0] = masterRequest.getRowId();
		args[1] = componentId;
		ResultSet rs = db.runSelect(query, args);
		if (rs.next()) {
			Component component = new Component();
			component.setComponentId(componentId);
			component.setRowId(rs.getLong(1));
			component.setComponentType(rs.getString(2));
			return component;
		} else return null;
	}

	@Override
    public List<Attribute> loadAttributes(DatabaseAccess db,
			Long masterRequestRowId, Long componentRowId) throws SQLException
	{
		String query;
		ResultSet rs;
		if (componentRowId==null) {
			// load all attributes including those for components
			query = "select ATTRIBUTE_NAME,ATTRIBUTE_VALUE,COMPONENT_ROWID, EVENT_ROWID from BAM_ATTRIBUTE " +
				"where MASTER_REQUEST_ROWID=?";
			rs = db.runSelect(query, masterRequestRowId);
		} else if (componentRowId.longValue()==0L) {
			// load attributes of master request proper
			query = "select ATTRIBUTE_NAME,ATTRIBUTE_VALUE,COMPONENT_ROWID, EVENT_ROWID from BAM_ATTRIBUTE " +
			"where MASTER_REQUEST_ROWID=? and COMPONENT_ROWID is null";
			rs = db.runSelect(query, masterRequestRowId);
		} else {
			// load attributes for a component
			query = "select ATTRIBUTE_NAME,ATTRIBUTE_VALUE,COMPONENT_ROWID, EVENT_ROWID from BAM_ATTRIBUTE " +
				"where MASTER_REQUEST_ROWID=? and COMPONENT_ROWID=?";
			Object[] args = new Object[2];
			args[0] = masterRequestRowId;
			args[1] = componentRowId;
			rs = db.runSelect(query, args);
		}
		List<Attribute> attributes = null;
		while (rs.next()) {
			if (attributes==null) attributes = new ArrayList<Attribute>();
			Attribute attr = new Attribute();
			attr.setAttributeName(rs.getString(1));
			attr.setAttributeValue(rs.getString(2));
			attr.setComponentRowId(rs.getLong(3));
	        attr.setEventRowId(rs.getLong(4));
			attributes.add(attr);
		}
		return attributes;
	}

	/**
	 * Load all events for the master request, including those for components
	 * @param db
	 * @param masterRequestRowId
	 * @return
	 * @throws SQLException
	 */
	private List<Event> loadEvents(DatabaseAccess db, Long masterRequestRowId) throws SQLException {
		String query = "select EVENT_ROWID,EVENT_NAME,SOURCE_SYSTEM,EVENT_ID,EVENT_DATA," +
				"NOTE,EVENT_TIME,RECORD_TIME,EVENT_CATEGORY,EVENT_SUBCATEGORY " +
				"from BAM_EVENT " +
				"where MASTER_REQUEST_ROWID=? " +
				"order by EVENT_TIME, EVENT_ROWID";
		ResultSet rs = db.runSelect(query, masterRequestRowId);
		List<Event> eventlist = new ArrayList<Event>();
		while (rs.next()) {
			Event event = new Event();
			event.setRowId(rs.getLong(1));
			event.setEventName(rs.getString(2));
			event.setSourceSystem(rs.getString(3));
			event.setEventId(rs.getString(4));
			event.setEventData(rs.getString(5));
			String compRowId = rs.getString(6);
			event.setComponentRowId(compRowId==null?null:new Long(compRowId));
			event.setEventTime(rs.getTimestamp(7));
			event.setEventCategory(rs.getString(9));
			event.setSubCategory(rs.getString(10));
			eventlist.add(event);
		}
		return eventlist;
	}

	@Override
    public void persistComponents(DatabaseAccess db,
			Long masterRequestRowId, Long eventRowId, List<Component> components)
			throws SQLException {
		String query = "insert into BAM_COMPONENT " +
				"(MASTER_REQUEST_ROWID,COMPONENT_ROWID,COMPONENT_ID,COMPONENT_TYPE)" +
				" values (?,?,?,?)";
		Object[] args = new Object[4];
		args[0] = masterRequestRowId;
		for (Component component : components) {
			Long componentRowId = getNewRowId(db, COMPONENT_SEQ_NAME);
			component.setRowId(componentRowId);
			args[1] = componentRowId;
			args[2] = component.getComponentId();
			args[3] = component.getComponentType();
			db.runUpdate(query, args);
			if (component.getAttributes()!=null) {
				persistAttributes(db, masterRequestRowId, eventRowId, componentRowId, component.getAttributes());
			}
		}
	}

	@Override
    public void persistComponentRelations(DatabaseAccess db,
			Long masterRequestRowId, List<ComponentRelation> componentRelations)
			throws SQLException {
		String query = "insert into BAM_COMPONENT_RELATION " +
				"(COMPONENT_A_ROWID,COMPONENT_B_ROWID,RELATION_TYPE)" +
				" values (?,?,?)";
		Object[] args = new Object[3];
		for (ComponentRelation componentRelation : componentRelations) {
			args[1] = componentRelation.getComponentARowid();
			args[2] = componentRelation.getComponentBRowid();
			args[3] = componentRelation.getRelationType();
			db.runUpdate(query, args);
		}
	}

	@Override
    public Long persistEvent(DatabaseAccess db,
			Long masterRequestRowId, Long componentRowId, Event event)
			throws SQLException {
		String query = "insert into BAM_EVENT " +
				"(EVENT_ROWID,MASTER_REQUEST_ROWID,NOTE," +
				"EVENT_NAME,EVENT_CATEGORY,EVENT_SUBCATEGORY," +
				"SOURCE_SYSTEM,EVENT_ID,EVENT_DATA,EVENT_TIME,RECORD_TIME)" +
				" values (?,?,?,?,?,?,?,?,?,?," +  (db.isMySQL()?"now()":"sysdate") + ")";
		Object[] args = new Object[10];
		Long eventRowId = getNewRowId(db, EVENT_SEQ_NAME);
		args[0] = eventRowId;
		args[1] = masterRequestRowId;
		args[2] = componentRowId;
		args[3] = event.getEventName();
		args[4] = event.getEventCategory();
		args[5] = event.getSubCategory();
		args[6] = event.getSourceSystem();
		args[7] = event.getEventId();
		args[8] = event.getEventData();
		args[9] = event.getEventTime();
        if (db.isMySQL()) eventRowId = db.runInsertReturnId(query, args);
        else db.runUpdate(query, args);
		return eventRowId;
	}



	private Long getNewRowId(DatabaseAccess db, String sequenceName) throws SQLException {
		if (db.isMySQL())
		    return null;
	    String query = "select " + sequenceName + ".nextval from DUAL";
		ResultSet rs = db.runSelect(query, null);
		rs.next();
		return rs.getLong(1);
	}

	@Override
    public void persistMasterRequest(DatabaseAccess db, MasterRequest masterRequest)
			throws SQLException {
		String query = "insert into BAM_MASTER_REQUEST (MASTER_REQUEST_ROWID, MASTER_REQUEST_ID,"
			+ "SOURCE_SYSTEM, REQUEST_TIME, RECORD_TIME) values (?,?,?,?, " + (db.isMySQL()?"now()":"sysdate") + ")";
		Long masterRequestRowId = getNewRowId(db, MASTER_REQUEST_SEQ_NAME);
		Object[] args = new Object[4];
		masterRequest.setRowId(masterRequestRowId);
		args[0] = masterRequestRowId;
		args[1] = masterRequest.getMasterRequestId();
		args[2] = masterRequest.getRealm();
		args[3] = masterRequest.getCreateTime();
		db.runUpdate(query, args);
	}

	@Override
    public void persistAttributes(DatabaseAccess db,
			Long masterRequestRowId, Long eventRowId, Long componentRowId, List<Attribute> attributes)
			throws SQLException {
		String query = "insert into BAM_ATTRIBUTE " +
				"(MASTER_REQUEST_ROWID, EVENT_ROWID, COMPONENT_ROWID,ATTRIBUTE_NAME,ATTRIBUTE_VALUE)" +
				" values (?,?,?,?,?)";
		Object[] args = new Object[5];
		args[0] = masterRequestRowId;
	    args[1] = eventRowId;
		args[2] = componentRowId;
		for (Attribute attr : attributes) {
			args[3] = attr.getAttributeName();
			args[4] = attr.getAttributeValue();
			db.runUpdate(query, args);
		}
	}

	@Override
    public void deleteComponentRelations(DatabaseAccess db, List<Component> componentList)
		throws SQLException {
		StringBuffer sb = new StringBuffer();
		for (Component comp : componentList) {
			if (sb.length()==0) sb.append("(");
			else sb.append(",");
			sb.append("'").append(comp.getRowId().toString()).append("'");
		}
		sb.append(")");
		String complist = sb.toString();
		String query = "delete BAM_COMPONENT_RELATION where COMPONENT_A_ROWID in " + complist +
			" or COMPONENT_B_ROWID in " + complist;
		db.runSelect(query, null);
	}

	@Override
    public void updateAttributes(DatabaseAccess db,
			Long masterRequestRowId, Long componentRowId, List<Attribute> attributes)
			throws SQLException {
		String query = "update BAM_ATTRIBUTE set ATTRIBUTE_VALUE=? " +
			"where MASTER_REQUEST_ROWID=? and COMPONENT_ROWID=? and EVENT_ROWID = ? and ATTRIBUTE_NAME=?";
		Object[] args = new Object[5];
		args[1] = masterRequestRowId;
		args[2] = componentRowId;
		for (Attribute attr : attributes) {
			args[3] = attr.getEventRowId();
	        args[4] = attr.getAttributeName();
			args[0] = attr.getAttributeValue();
			db.runUpdate(query, args);
		}
	}



}
