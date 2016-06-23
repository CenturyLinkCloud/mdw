/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;

import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.impl.PropertyManagerDatabase;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;

public class PropertyManagerHandler extends TablePaginator {

	@Override
	public FormDataDocument handleAction(FormDataDocument datadoc, Map<String, String> params) {
		try {
			String subcmd = params.get(FormConstants.URLARG_ACTION);
			if (subcmd==null) subcmd = params.get("subcmd");
			if (subcmd==null || subcmd.equals("InitialLoad")) {
				datadoc = new FormDataDocument();
				loadProperties(datadoc);
				datadoc.setAttribute(FormDataDocument.ATTR_FORM, "property_mgr");
			} else {
				return super.handleAction(datadoc, params);
			}
		} catch (Exception e) {
			if (!(e instanceof UserException)) e.printStackTrace();
			datadoc.addError(e.getMessage());
		}
		return datadoc;
	}

//	private void save_properties(FormDataDocument datadoc, EJBContext ejb) throws DataAccessException {
////		System.out.println("datadoc: " + datadoc.format());
//		Properties props = PropertyManager.getInstance().getAllProperties();
//		List<String> changes = new ArrayList<String>();
//		collect_changes(datadoc.getRootNode(), null, props, changes);
//		ProcessPersister persister =
//			DataAccess.getProcessPersister(DataAccess.currentSchemaVersion,new DatabaseAccess(null,ejb));
//		for (String pn : changes) {
//	        persister.setAttribute(OwnerType.SYSTEM, 0L, pn, (String)props.get(pn));
//		}
//	}
//
//	private void collect_changes(MbengNode parent, String prefix, Properties props,
//			List<String> changes) {
//		MbengNode child;
//		String pn;
//		for (child=parent.getFirstChild(); child!=null; child=child.getNextSibling()) {
//			pn = child.getName();
//			if (prefix!=null && pn.charAt(0)=='_') pn = prefix + "/" + pn.substring(1);
//			else if (prefix!=null) pn = prefix + "." + pn;
//			if (props.containsKey(pn)) {
//				if (!props.get(pn).equals(child.getValue())) {
//					props.put(pn, child.getValue());	// note this refreshes current server
//					changes.add(pn);
//				}
//			}
//			collect_changes(child, pn, props, changes);
//		}
//	}

	private void loadProperties(FormDataDocument datadoc) throws MbengException {
		PropertyManager propmgr = PropertyManager.getInstance();
		MbengNode table = datadoc.setTable(null, "PROPERTIES", true);
		Properties props = propmgr.getAllProperties();
		for (Object key : props.keySet()) {
			String pn = (String)key;
			String pv = props.getProperty(pn);
			MbengNode row = datadoc.addRow(table);
			datadoc.setCell(row, "PropertyName", pn);
			datadoc.setCell(row, "PropertyValue", pv);
			String source;
			if (propmgr instanceof PropertyManagerDatabase) {
				source = ((PropertyManagerDatabase)propmgr).getPropertySource(pn);
			} else {
				source = propmgr.getClass().getName();
			}
			datadoc.setCell(row, "Source", source);
		}
	}

	@Override
	protected void fetchTablePage(FormDataDocument datadoc,
			MbengNode tablenode, int start_row, int nrows, String sortOn,
			boolean descending) throws Exception {
		this.loadProperties(datadoc);
		if (sortOn!=null) super.sortTable(datadoc, tablenode, descending?("-"+sortOn):sortOn);
	}

    private void setPropertyGlobally(String name, String value)
    	throws MDWException, JSONException {
    	JSONObject json = new JSONObject();
		json.put("ACTION", "REFRESH_PROPERTY");
		json.put("NAME", name);
		json.put("VALUE", value==null?"":value);
		InternalMessenger messenger = MessengerFactory.newInternalMessenger();
		messenger.broadcastMessage(json.toString());
    }

	private void updateProperty(String pn, String pv) throws Exception {
		EventManager eventMgr = ServiceLocator.getEventManager();
		eventMgr.setAttribute(OwnerType.SYSTEM, 0L, pn, pv);	// always save to database?
		PropertyManager propmgr = PropertyManager.getInstance();
		propmgr.setStringProperty(pn, pv);
		if (propmgr instanceof PropertyManagerDatabase)
			((PropertyManagerDatabase)propmgr).putPropertySource(pn, PropertyManagerDatabase.DATABASE);
		setPropertyGlobally(pn, pv);
	}

	@Override
	protected void insertTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		String pn = datadoc.getValue(row, "PropertyName");
		String pv = datadoc.getValue(row, "PropertyValue");
		updateProperty(pn, pv);
	}

	@Override
	protected void deleteTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		String source = datadoc.getValue(row, "Source");
		if (!PropertyManagerDatabase.DATABASE.equals(source))
			throw new Exception("You can only delete database properties");
		String pn = datadoc.getValue(row, "PropertyName");
		updateProperty(pn, null);
	}

	@Override
	protected void updateTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		String source = datadoc.getValue(row, "Source");
		if (PropertyManagerDatabase.ENV_OVERRIDE_PROPERTIES_FILE_NAME.equals(source))
			throw new Exception("Cannot update local override properties");
		String pn = datadoc.getValue(row, "PropertyName");
		String pv = datadoc.getValue(row, "PropertyValue");
		updateProperty(pn, pv);
	}

}
