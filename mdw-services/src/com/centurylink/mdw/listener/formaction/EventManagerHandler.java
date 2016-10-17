/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.qwest.mbeng.MbengNode;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.event.EventInstanceVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.services.EventException;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.event.CertifiedMessageManager;
import com.centurylink.mdw.services.event.ScheduledEventQueue;

public class EventManagerHandler extends TablePaginator {

	private final static int EXTERNAL_EVENT_TAB = 0;
	private final static int SCHEDULED_JOB_TAB = 1;
	private final static int SCHEDULED_EVENT_TAB = 2;
	private final static int CERTIFIED_MESSAGE_TAB = 3;
	private final static int FLAG_TAB = 4;
	private final static int DOCUMENT_TAB = 5;
	private final static int EVENT_WAIT_TAB = 6;
	private final static int EVENT_LOG_TAB = 7;

	private final static String TABLE_DATAPATH = "EVENT_LIST";
	private final static String TABLE_METAPATH = "EVENT_LIST_META";
	private final static String TAB_INDEX = "TAB_INDEX";

	private int tab_index;

	public FormDataDocument handleAction(FormDataDocument datadoc, Map<String, String> params) {
		try {
			String subcmd = params.get(FormConstants.URLARG_ACTION);
			if (subcmd==null || subcmd.equals("InitialLoad")) {
				datadoc = new FormDataDocument();
				tab_index = 0;
				MbengNode tablenode = datadoc.setTable(null, TABLE_DATAPATH, false);
				int totalrows = this.getNumberOfRows(datadoc);
				int pagesize = this.getPageSize(datadoc);
				int nrows = pagesize<totalrows?pagesize:totalrows;
				this.fetchTablePage(datadoc, tablenode, 1, nrows, "CREATE_DT", true);
				datadoc.setAttribute(FormDataDocument.ATTR_FORM, "event_mgr");
				MbengNode metanode = datadoc.setSubform(null, TABLE_METAPATH);
				super.setMetaInfo(datadoc, metanode, 1, pagesize, totalrows, "-CREATE_DT");
			} else if (subcmd.equals("tabbing")) {
//				String tabsId = params.get(FormConstants.URLARG_TABS);
				String tabIndex = params.get(FormConstants.URLARG_TAB);
//				String tabIndexField = params.get(FormConstants.URLARG_DATA);
				datadoc.setValue(TAB_INDEX, tabIndex);
				tab_index = Integer.parseInt(tabIndex);
//				System.out.println("Tabs " + tabsId + " is changing to tab " + tabIndex);
				MbengNode tablenode = datadoc.setTable(null, TABLE_DATAPATH, true);
				int totalrows = this.getNumberOfRows(datadoc);
				int pagesize = this.getPageSize(datadoc);
				int nrows = pagesize<totalrows?pagesize:totalrows;
				this.fetchTablePage(datadoc, tablenode, 1, nrows, "CREATE_DT", true);
				MbengNode metanode = datadoc.setSubform(null, TABLE_METAPATH);
				super.setMetaInfo(datadoc, metanode, 1, pagesize, totalrows, "-CREATE_DT");
			} else if (subcmd.equals("view_message")) {
				String docid = params.get(FormConstants.URLARG_ROW);
				EventManager eventManager = ServiceLocator.getEventManager();
		        DocumentVO docvo = eventManager.getDocumentVO(new Long(docid));
		        datadoc.setValue("DOCUMENT_ID", docid);
		        datadoc.setValue("DOCUMENT", docvo.getContent());
				datadoc.setMetaValue(FormDataDocument.META_ACTION,
						FormConstants.ACTION_DIALOG + "?" + FormConstants.URLARG_FORMNAME + "=document_view_dialog");
			} else if (subcmd.equals("view_wait_instances")) {
				MbengNode metanode = datadoc.setSubform(null, TABLE_METAPATH);
				String row_str = datadoc.getValue(metanode, "selected");
				if (StringHelper.isEmpty(row_str) || row_str.indexOf(",")>0)
					throw new UserException("You must select a (single) row");
				int row_index = Integer.parseInt(row_str);
				MbengNode tablenode = datadoc.setTable(null, TABLE_DATAPATH, false);
				MbengNode row = datadoc.getRow(tablenode, row_index);
				if (row==null) throw new Exception("No row found for index " + row_str);
				EventManager eventManager = ServiceLocator.getEventManager();
		        String eventName = datadoc.getValue(row, "EVENT_NAME");
	        	List<String[]> eventlist = getEventWaitInstances(eventName, eventManager);
	        	StringBuffer sb = new StringBuffer();
				for (String[] event : eventlist) {
					sb.append(event[0]).append(": act inst ");
					sb.append(event[1]).append(", created ").append(event[3]);
					sb.append(", completion code ").append(event[2]);
					sb.append("\n");
				}
		        datadoc.setValue("DOCUMENT_ID", eventName);
		        datadoc.setValue("DOCUMENT", sb.toString());
				datadoc.setMetaValue(FormDataDocument.META_ACTION,
						FormConstants.ACTION_DIALOG + "?" + FormConstants.URLARG_FORMNAME + "=document_view_dialog");
			} else if (subcmd.equals("view_document")) {
				String documentId = params.get(FormConstants.URLARG_ROW);		// not used, get from selection
				String tableId =params.get("tableId");
				MbengNode metanode = datadoc.setSubform(null, TABLE_METAPATH);
				String row_str = datadoc.getValue(metanode, "selected");
				if (StringHelper.isEmpty(row_str) || row_str.indexOf(",")>0)
					throw new UserException("You must select a (single) row");
				int row_index = Integer.parseInt(row_str);
				MbengNode tablenode = datadoc.setTable(null, TABLE_DATAPATH, false);
				MbengNode row = datadoc.getRow(tablenode, row_index);
				if (row==null) throw new Exception("No row found for index " + row_str);
				documentId = datadoc.getValue(row, "DOCUMENT_ID");
				EventManager eventManager = ServiceLocator.getEventManager();
		        DocumentVO docvo = eventManager.getDocumentVO(new Long(documentId));
		        datadoc.setValue(row, "CONTENT", docvo.getContent());
				datadoc.setMetaValue(FormDataDocument.META_ACTION,
						FormConstants.ACTION_DIALOG + "?" + FormConstants.URLARG_FORMNAME + "="
						+ FormConstants.TABLE_ROW_DIALOG_PREFIX + tableId);
			} else if (subcmd.equals("view_process_instance")) {
				String idType = params.get("idtype");
				String procInstId;
				if ("activity_instance_id".equalsIgnoreCase(idType)) {
					procInstId = "111";		// TODO implement this
				} else procInstId = params.get(FormConstants.URLARG_ROW);
				datadoc.setMetaValue(FormDataDocument.META_ACTION,
						FormConstants.ACTION_WINDOW + "?" + FormConstants.URLARG_FORMNAME + "="
						+ "designer:PROCESS_INSTANCE&objid=" + procInstId);
			} else if (subcmd.equals("cancel_cm")) {
				super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
				changeCertifiedMessageStatus(datadoc, EventInstanceVO.STATUS_CERTIFIED_MESSAGE_CANCEL);
			} else if (subcmd.equals("hold_cm")) {
				super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
				changeCertifiedMessageStatus(datadoc, EventInstanceVO.STATUS_CERTIFIED_MESSAGE_HOLD);
			} else if (subcmd.equals("resume_cm")) {
				super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
				changeCertifiedMessageStatus(datadoc, EventInstanceVO.STATUS_CERTIFIED_MESSAGE);
			} else if (subcmd.equals("refresh")) {
				String tabIndex = datadoc.getValue(TAB_INDEX);
				tab_index = StringHelper.isEmpty(tabIndex)?0:Integer.parseInt(tabIndex);
				MbengNode tablenode = datadoc.setTable(null, TABLE_DATAPATH, true);
				int totalrows = this.getNumberOfRows(datadoc);
				int pagesize = this.getPageSize(datadoc);
				int nrows = pagesize<totalrows?pagesize:totalrows;
				this.fetchTablePage(datadoc, tablenode, 1, nrows, "CREATE_DT", true);
				MbengNode metanode = datadoc.setSubform(null, TABLE_METAPATH);
				super.setMetaInfo(datadoc, metanode, 1, pagesize, totalrows, "-CREATE_DT");
				ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
				queue.refreshCache();
			} else {
				String av = datadoc.getValue(TAB_INDEX);
				tab_index = av==null?0:Integer.parseInt(av);
				return super.handleAction(datadoc, params);
			}
		} catch (UserException e) {
			datadoc.addError(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			datadoc.addError(e.getMessage());
		}
		return datadoc;
	}

	@Override
	protected int getPageSize(FormDataDocument datadoc) {
		return 32;
	}

	private String getWhereClause(int tab_index)
			throws SQLException, DataAccessException {
		String whereClause;
    	if (tab_index==EXTERNAL_EVENT_TAB) {
        	StringBuffer buff = new StringBuffer();
         	buff.append("STATUS_CD in (");
         	buff.append(EventInstanceVO.STATUS_WAITING).append(",");
         	buff.append(EventInstanceVO.STATUS_ARRIVED).append(",");
         	buff.append(EventInstanceVO.STATUS_WAITING_MULTIPLE).append(",");
         	buff.append(EventInstanceVO.STATUS_CONSUMED);
         	buff.append(")");
         	whereClause = buff.toString();
    	} else if (tab_index==SCHEDULED_JOB_TAB) {
    		StringBuffer buff = new StringBuffer();
         	buff.append("STATUS_CD in (");
         	buff.append(EventInstanceVO.STATUS_SCHEDULED_JOB);
         	buff.append(")");
			whereClause = buff.toString();
		} else if (tab_index==SCHEDULED_EVENT_TAB) {
			StringBuffer buff = new StringBuffer();
         	buff.append("STATUS_CD in (");
         	buff.append(EventInstanceVO.STATUS_INTERNAL_EVENT);
         	buff.append(")");
			whereClause = buff.toString();
		} else if (tab_index==CERTIFIED_MESSAGE_TAB) {
			StringBuffer buff = new StringBuffer();
         	buff.append("STATUS_CD in (");
         	buff.append(EventInstanceVO.STATUS_CERTIFIED_MESSAGE).append(",");
         	buff.append(EventInstanceVO.STATUS_CERTIFIED_MESSAGE_DELIVERED).append(",");
         	buff.append(EventInstanceVO.STATUS_CERTIFIED_MESSAGE_CANCEL).append(",");
         	buff.append(EventInstanceVO.STATUS_CERTIFIED_MESSAGE_HOLD).append(",");
         	buff.append(EventInstanceVO.STATUS_CERTIFIED_MESSAGE_RECEIVED);
         	buff.append(")");
			whereClause = buff.toString();
		} else if (tab_index==FLAG_TAB) {
			StringBuffer buff = new StringBuffer();
         	buff.append("STATUS_CD in (");
         	buff.append(EventInstanceVO.STATUS_FLAG);
         	buff.append(")");
			whereClause = buff.toString();
    	} else whereClause = null;
    	return whereClause;
    }

	@Override
	protected int getNumberOfRows(FormDataDocument datadoc) {
		try {
			String tableName;
			if (tab_index==EVENT_WAIT_TAB) {
				tableName = "EVENT_WAIT_INSTANCE";
			} else if (tab_index==EVENT_LOG_TAB) {
				tableName = "EVENT_LOG";
			} else if (tab_index==DOCUMENT_TAB) {
				tableName = "DOCUMENT";
			} else {
				tableName = "EVENT_INSTANCE";
			}
			String whereClause = getWhereClause(tab_index);
			EventManager eventManager = ServiceLocator.getEventManager();
			return eventManager.getTableRowCount(tableName, whereClause);
		} catch (Exception e) {
			logger.severeException("Cannot find out total number of event instances", e);
			return 0;
		}
	}

	@Override
	protected void fetchTablePage(FormDataDocument datadoc,
			MbengNode tablenode, int start_row, int nrows, String sortOn,
			boolean descending) throws Exception {
		EventManager eventManager = ServiceLocator.getEventManager();
        if (tab_index==EVENT_WAIT_TAB) {
        	String[] fields = {"EVENT_WAIT_INSTANCE_ID", "EVENT_NAME", "EVENT_WAIT_INSTANCE_OWNER_ID",
        			"WAKE_UP_EVENT", "CREATE_DT"};
        	Class<?>[] types = {Long.class, String.class, Long.class, String.class, Date.class};
        	String whereClause = null;
        	List<String[]> eventlist = eventManager.getTableRowList("EVENT_WAIT_INSTANCE",
        			types, fields, whereClause, sortOn, descending, start_row, nrows);
			for (String[] event : eventlist) {
				MbengNode row = datadoc.addRow(tablenode);
				for (int j=0; j<types.length; j++) {
					datadoc.setCell(row, fields[j], event[j]);
				}
			}
        } else if (tab_index==EVENT_LOG_TAB) {
        	String[] fields = {"EVENT_LOG_ID", "EVENT_NAME", "EVENT_LOG_OWNER",
        			"EVENT_LOG_OWNER_ID", "CREATE_DT", "EVENT_CATEGORY",
        			"EVENT_SUB_CATEGORY", "CREATE_USR", "COMMENTS"};
        	Class<?>[] types = {Long.class, String.class, String.class, Long.class,
        			Date.class, String.class, String.class, String.class, String.class};
			String whereClause = getWhereClause(tab_index);
        	List<String[]> eventlist = eventManager.getTableRowList("EVENT_LOG",
        			types, fields, whereClause, sortOn, descending, start_row, nrows);
        	for (String[] event : eventlist) {
				MbengNode row = datadoc.addRow(tablenode);
				for (int j=0; j<types.length; j++) {
					datadoc.setCell(row, fields[j], event[j]);
				}
			}
        } else if (tab_index==DOCUMENT_TAB) {
        	String[] fields = {"DOCUMENT_ID", "CREATE_DT", "PROCESS_INST_ID",
        			"OWNER_TYPE", "OWNER_ID", "DOCUMENT_TYPE" /*, "CONTENT" - slower with this */};
        	Class<?>[] types = {Long.class, Date.class, Long.class, String.class,
        			Long.class, String.class /*, String.class */};
			String whereClause = null;
        	List<String[]> eventlist = eventManager.getTableRowList("DOCUMENT",
        			types, fields, whereClause, sortOn, descending, start_row, nrows);
        	for (String[] event : eventlist) {
				MbengNode row = datadoc.addRow(tablenode);
				for (int j=0; j<types.length; j++) {
					datadoc.setCell(row, fields[j], event[j]);
				}
			}
        } else {
        	String[] fields = {"EVENT_NAME", "DOCUMENT_ID", "STATUS_CD",
        			"CREATE_DT", "CONSUME_DT", "AUXDATA", "REFERENCE", "PRESERVE_INTERVAL"};
        	Class<?>[] types = {String.class, Long.class, Integer.class,
        			Date.class, Date.class, String.class, String.class, Integer.class};
			String whereClause = getWhereClause(tab_index);
        	List<String[]> eventlist = eventManager.getTableRowList("EVENT_INSTANCE",
        			types, fields, whereClause, sortOn, descending, start_row, nrows);
        	for (String[] event : eventlist) {
				MbengNode row = datadoc.addRow(tablenode);
				datadoc.setCell(row, "EVENT_NAME", event[0]);
				datadoc.setCell(row, "DOCUMENT_ID", event[1]);
				datadoc.setCell(row, "STATUS_CD", event[2]);
				datadoc.setCell(row, "status_name", EventInstanceVO.getStatusName(event[2]));
				datadoc.setCell(row, "CREATE_DT", event[3]);
				datadoc.setCell(row, "CONSUME_DT", event[4]);
				datadoc.setCell(row, "AUXDATA", event[5]);
				datadoc.setCell(row, "REFERENCE", event[6]);
				datadoc.setCell(row, "PRESERVE_INTERVAL", event[7]);
			}
        }
	}

	private List<String[]> getEventWaitInstances(String eventName, EventManager eventManager)
		throws DataAccessException {
        String[] fields = {"EVENT_WAIT_INSTANCE_ID", "EVENT_WAIT_INSTANCE_OWNER_ID",
    			"WAKE_UP_EVENT", "CREATE_DT"};
    	Class<?>[] types = {Long.class, Long.class, String.class, Date.class};
    	String whereClause = "EVENT_NAME='" + eventName + "'";
    	List<String[]> eventlist = eventManager.getTableRowList("EVENT_WAIT_INSTANCE",
    			types, fields, whereClause, "CREATE_DT", true, -1, -1);
    	return eventlist;
	}

	@Override
	protected void insertTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
		EventManager eventManager = ServiceLocator.getEventManager();
		Entity entity;
		String auditMsg;
		if (tab_index==EVENT_WAIT_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String actInstId = datadoc.getValue(row, "EVENT_WAIT_INSTANCE_OWNER_ID");
			String compCode = datadoc.getValue(row, "WAKE_UP_EVENT");
			eventManager.createEventWaitInstance(eventName, new Long(actInstId), compCode);
			entity = Entity.Event;
			auditMsg = "Create event wait instance " + eventName + " for act inst " + actInstId;
		} else if (tab_index==CERTIFIED_MESSAGE_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String docid = datadoc.getValue(row, "DOCUMENT_ID");
			String props = datadoc.getValue(row, "AUXDATA");
			String reference = datadoc.getValue(row, "REFERENCE");
			eventManager.createEventInstance(eventName, new Long(docid),
					EventInstanceVO.STATUS_CERTIFIED_MESSAGE, null, props, reference, 3600);
			(new CacheRegistration()).refreshCache(CertifiedMessageManager.class.getName());
			entity = Entity.Message;
			auditMsg = "Create certified message " + eventName + " for document " + docid;
		} else if (tab_index==FLAG_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String reference = datadoc.getValue(row, "REFERENCE");
			int preserveSeconds;
			try {
				preserveSeconds = Integer.parseInt(datadoc.getValue(row, "PRESERVE_INTERVAL"));
				if (preserveSeconds < 0) throw new UserException("Preserve Interval cannot be negative");
			} catch (Exception e) {
				throw new UserException("Preserve Interval must be specified as a number");
			}
			eventManager.createEventInstance(eventName, null, EventInstanceVO.STATUS_FLAG,
					null, null, reference, preserveSeconds);
			entity = Entity.Event;
			auditMsg = "Create event flag " + eventName;
		} else if (tab_index==SCHEDULED_JOB_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String schedule = datadoc.getValue(row, "AUXDATA");
//			String reference = datadoc.getValue(row, "REFERENCE");
			if (StringHelper.isEmpty(schedule))
					throw new UserException("You must specify cron expression");
			ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
			queue.scheduleCronJob(eventName, schedule);
			(new CacheRegistration()).refreshCache(ScheduledEventQueue.class.getName());
			entity = Entity.Other;
			auditMsg = "Create scheduled job " + eventName + " with schedule " + schedule;
		} else if (tab_index==SCHEDULED_EVENT_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String schedule = datadoc.getValue(row, "CONSUME_DT");
			String message = datadoc.getValue(row, "AUXDATA");
			String reference = datadoc.getValue(row, "REFERENCE");
			if (StringHelper.isEmpty(schedule))
				throw new UserException("You must specify data/time");
			ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
			queue.scheduleInternalEvent(eventName,
					StringHelper.stringToDate(schedule), message, reference);
			(new CacheRegistration()).refreshCache(ScheduledEventQueue.class.getName());
			entity = Entity.Event;
			auditMsg = "Create internal event " + eventName + " due on " + schedule;
		} else if (tab_index==EXTERNAL_EVENT_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String docid = datadoc.getValue(row, "DOCUMENT_ID");
			Long documentId = StringHelper.isEmpty(docid)?null:new Long(docid);
			String status_name = datadoc.getValue(row, "status_name");
			Integer status = EventInstanceVO.getStatusCodeFromName(status_name);
			if (status==null) throw new UserException("Unknown status " + status_name);
			eventManager.createEventInstance(eventName, documentId,
					status, null, null, null, 3600);
			entity = Entity.ExternalEvent;
			auditMsg = "Create event instance " + eventName + " with document " + docid;
		} else if (tab_index==DOCUMENT_TAB) {
			String owner_type = datadoc.getValue(row, "OWNER_TYPE");
			String owner_id = datadoc.getValue(row, "OWNER_ID");
			String process_inst_id = datadoc.getValue(row, "PROCESS_INST_ID");
			String document_type = datadoc.getValue(row, "DOCUMENT_TYPE");
			Long ownerId = StringHelper.isEmpty(owner_id)?new Long(0):new Long(owner_id);
			Long procInstId = StringHelper.isEmpty(process_inst_id)?new Long(0L):new Long(process_inst_id);
			String content = datadoc.getValue(row, "CONTENT");
			Long docid = eventManager.createDocument(document_type, procInstId,
					owner_type, ownerId, null, null, content);
			entity = Entity.Document;
			auditMsg = "Create document " + docid.toString();
		} else {
			throw new UserException("Cannot insert row to this table");
		}
        createAuditLog(eventManager, datadoc.getMetaValue(FormDataDocument.META_USER),
        		Action.Create, entity, 0L, auditMsg);
	}

	@Override
	protected void deleteTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
		EventManager eventManager = ServiceLocator.getEventManager();
        Entity entity;
		String auditMsg;
        if (tab_index==EVENT_WAIT_TAB) {
			String id = datadoc.getValue(row, "EVENT_WAIT_INSTANCE_ID");
			eventManager.deleteTableRow("EVENT_WAIT_INSTANCE", "EVENT_WAIT_INSTANCE_ID", id);
			entity = Entity.Event;
			auditMsg = "Delete event wait instance " + id;
		} else if (tab_index==CERTIFIED_MESSAGE_TAB) {
			String status_str = datadoc.getValue(row, "STATUS_CD");
			if (status_str.equals(EventInstanceVO.STATUS_CERTIFIED_MESSAGE.toString()))
					throw new UserException("Cannot delete active messages - you can cancel it first");
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			eventManager.deleteTableRow("EVENT_INSTANCE", "EVENT_NAME", eventName);
			entity = Entity.Message;
			auditMsg = "Delete certified message " + eventName;
		} else if (tab_index==SCHEDULED_JOB_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
			queue.unscheduleEvent(eventName);	// this broadcast
			entity = Entity.Other;
			auditMsg = "Delete scheduled job " + eventName;
		} else if (tab_index==SCHEDULED_EVENT_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
			queue.unscheduleEvent(eventName);	// this broadcast
			entity = Entity.Event;
			auditMsg = "Delete internal event " + eventName;
		} else if (tab_index==FLAG_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			eventManager.deleteTableRow("EVENT_INSTANCE", "EVENT_NAME", eventName);
			entity = Entity.Event;
			auditMsg = "Delete event flag " + eventName;
		} else if (tab_index==EXTERNAL_EVENT_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
        	List<String[]> eventlist = getEventWaitInstances(eventName, eventManager);
        	if (eventlist.size()>0) throw new UserException("There are waiting instances");
			eventManager.deleteTableRow("EVENT_INSTANCE", "EVENT_NAME", eventName);
			entity = Entity.ExternalEvent;
			auditMsg = "Delete event instance " + eventName;
		} else if (tab_index==DOCUMENT_TAB) {
			String documentId = datadoc.getValue(row, "DOCUMENT_ID");
	        eventManager.deleteTableRow("DOCUMENT", "DOCUMENT_ID", documentId);
	        entity = Entity.Document;
			auditMsg = "Delete document " + documentId;
		} else {
			throw new UserException("Cannot delete row to this table");
		}
        createAuditLog(eventManager, datadoc.getMetaValue(FormDataDocument.META_USER),
        		Action.Delete, entity, 0L, auditMsg);
	}

	@Override
	protected void updateTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
		EventManager eventManager = ServiceLocator.getEventManager();
		Entity entity;
		String auditMsg;
		if (tab_index==FLAG_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String reference = datadoc.getValue(row, "REFERENCE");
			int preserveSeconds;
			try {
				preserveSeconds = Integer.parseInt(datadoc.getValue(row, "PRESERVE_INTERVAL"));
				if (preserveSeconds < 0) throw new Exception("Preserve Interval cannot be negative");
			} catch (Exception e) {
				throw new Exception("Preserve Interval must be specified as a number");
			}
			eventManager.updateEventInstance(eventName, null, EventInstanceVO.STATUS_FLAG,
					null, null, reference, preserveSeconds);
			entity = Entity.Event;
			auditMsg = "Update event flag " + eventName;
		} else if (tab_index==SCHEDULED_JOB_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String schedule = datadoc.getValue(row, "AUXDATA");
			if (StringHelper.isEmpty(schedule))
				throw new Exception("You must specify cron expression");
			ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
			queue.rescheduleCronJob(eventName, schedule);
			(new CacheRegistration()).refreshCache(ScheduledEventQueue.class.getName());
			entity = Entity.Event;
			auditMsg = "Update scheduled job " + eventName + " with schedule " + schedule;
		} else if (tab_index==SCHEDULED_EVENT_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String schedule = datadoc.getValue(row, "CONSUME_DT");
			String message = datadoc.getValue(row, "AUXDATA");
			if (StringHelper.isEmpty(schedule))
				throw new Exception("You must specify data/time");
			ScheduledEventQueue queue = ScheduledEventQueue.getSingleton();
			queue.rescheduleInternalEvent(eventName, StringHelper.stringToDate(schedule), message);
			(new CacheRegistration()).refreshCache(ScheduledEventQueue.class.getName());
			entity = Entity.Event;
			auditMsg = "Update internal event " + eventName;
		} else if (tab_index==CERTIFIED_MESSAGE_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String docid = datadoc.getValue(row, "DOCUMENT_ID");
			String properties = datadoc.getValue(row, "AUXDATA");
			String reference = datadoc.getValue(row, "REFERENCE");
			eventManager.updateEventInstance(eventName, new Long(docid),
					null, null, properties, reference, 0);
			(new CacheRegistration()).refreshCache(CertifiedMessageManager.class.getName());
			entity = Entity.Event;
			auditMsg = "Update certified message " + eventName;
		} else if (tab_index==EXTERNAL_EVENT_TAB) {
			String eventName = datadoc.getValue(row, "EVENT_NAME");
			String docid = datadoc.getValue(row, "DOCUMENT_ID");
			Long documentId = StringHelper.isEmpty(docid)?null:new Long(docid);
			String status_name = datadoc.getValue(row, "status_name");
			Integer status = EventInstanceVO.getStatusCodeFromName(status_name);
			if (status==null) throw new UserException("Unknown status " + status_name);
			eventManager.updateEventInstance(eventName, documentId,
					status, null, null, null, 0);
			entity = Entity.Event;
			auditMsg = "Update event instance " + eventName;
		} else if (tab_index==DOCUMENT_TAB) {
			String owner_type = datadoc.getValue(row, "OWNER_TYPE");
			String owner_id = datadoc.getValue(row, "OWNER_ID");
			String process_inst_id = datadoc.getValue(row, "PROCESS_INST_ID");
			String document_type = datadoc.getValue(row, "DOCUMENT_TYPE");
			Long ownerId = StringHelper.isEmpty(owner_id)?new Long(0):new Long(owner_id);
			Long procInstId = StringHelper.isEmpty(process_inst_id)?new Long(0L):new Long(process_inst_id);
			String content = datadoc.getValue(row, "CONTENT");
			String docid_str = datadoc.getValue(row, "DOCUMENT_ID");
			Long docid = new Long(docid_str);
			eventManager.updateDocumentContent(docid, content, null);
			eventManager.updateDocumentInfo(docid, procInstId,
					document_type, owner_type, ownerId, null, null);
			entity = Entity.Document;
			auditMsg = "Update document " + docid_str;
		} else {
			throw new Exception("Cannot update row to this table");
		}
        createAuditLog(eventManager, datadoc.getMetaValue(FormDataDocument.META_USER),
        		Action.Change, entity, 0L, auditMsg);
	}

	private void changeCertifiedMessageStatus(FormDataDocument datadoc,
			Integer newStatus) throws Exception {
		MbengNode metanode = datadoc.setSubform(null, TABLE_METAPATH);
		String row_str = datadoc.getValue(metanode, "selected");
		if (StringHelper.isEmpty(row_str) || row_str.indexOf(",")>0)
			throw new UserException("You must select a (single) row");
		int row_index = Integer.parseInt(row_str);
		MbengNode tablenode = datadoc.setTable(null, TABLE_DATAPATH, false);
		MbengNode row = datadoc.getRow(tablenode, row_index);
		if (row==null) throw new Exception("No row found for index " + row_str);
		String status_str = datadoc.getValue(row, "STATUS_CD");
		if (newStatus.equals(EventInstanceVO.STATUS_CERTIFIED_MESSAGE)) {
			if (status_str.equals(EventInstanceVO.STATUS_CERTIFIED_MESSAGE.toString()))
				throw new UserException("The message is already active");
		} else if (newStatus.equals(EventInstanceVO.STATUS_CERTIFIED_MESSAGE_CANCEL)) {
			if (status_str.equals(EventInstanceVO.STATUS_CERTIFIED_MESSAGE_CANCEL.toString()))
				throw new UserException("The message is already cancelled");
		} else if (newStatus.equals(EventInstanceVO.STATUS_CERTIFIED_MESSAGE_HOLD)) {
			if (status_str.equals(EventInstanceVO.STATUS_CERTIFIED_MESSAGE_HOLD.toString()))
				throw new UserException("The message is already in hold status");
		} else throw new Exception("Should never hit this situation");
		EventManager eventManager = ServiceLocator.getEventManager();
        String msgid = datadoc.getValue(row, "EVENT_NAME");
        eventManager.updateCertifiedMessageStatus(msgid, newStatus);
        datadoc.setValue(row, "STATUS_CD", newStatus.toString());
        String status_name = EventInstanceVO.getStatusName(newStatus);
        datadoc.setValue(row, "status_name", status_name);
        createAuditLog(eventManager, datadoc.getMetaValue(FormDataDocument.META_USER),
        		Action.Change, Entity.Message, 0L, "CM " + msgid + " status changed to " + status_name);
		(new CacheRegistration()).refreshCache(CertifiedMessageManager.class.getName());
	}

	private void createAuditLog(EventManager eventManager, String user, Action action,
			Entity entity, Long entityId, String description)
		throws EventException, DataAccessException {
		UserActionVO userAction = new UserActionVO(user, action, entity, entityId, description);
        userAction.setSource("EventManagerHandler");
        eventManager.createAuditLog(userAction);
	}

}
