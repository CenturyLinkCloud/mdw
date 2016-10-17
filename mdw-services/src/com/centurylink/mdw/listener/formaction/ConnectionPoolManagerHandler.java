/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.impl.PropertyManagerDatabase;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.UserException;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.centurylink.mdw.services.pooling.AdapterConnectionPool;
import com.centurylink.mdw.services.pooling.ConnectionPoolRegistration;
import com.centurylink.mdw.services.pooling.MDWConnectionPool;
import com.centurylink.mdw.services.pooling.PooledAdapterConnection;
import com.centurylink.mdw.services.pooling.PooledConnection;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public class ConnectionPoolManagerHandler extends TablePaginator {

	private final static String CONNECTION_POOLS_TABLE = "ConnectionPools";
	private final static String PROPERTIES_TABLE = "PoolProperties";
	private final static String CONNECTIONS_TABLE = "PoolConnections";
	private final static String POOL_NAME = "PoolName";
	private final static String POOL_ENABLED = "PoolEnabled";
	private final static String SERVER_LIST_NODE = "ServerList";
	private final static String MANAGED_SERVER = "ManagedServer";
	private final static String WAITING_LIST = "WaitingList";
	private final static String WAITING_REQUEST = "WaitingRequest";
	private final static String MAX_REQUESTS = "MaxRequests";
	private final static String TOTAL_REQUESTS = "TotalRequests";

	private final boolean START_IN_MEMORY_ONLY = true;

	public FormDataDocument handleAction(FormDataDocument datadoc, Map<String, String> params) {
		try {
			String subcmd = params.get(FormConstants.URLARG_ACTION);
			if (subcmd==null || subcmd.equals("init")) {
				datadoc = new FormDataDocument();
				fetchPoolTableData(datadoc);
				fetchServerList(datadoc);
			} else if (subcmd.equals("view")) {
				showPool(datadoc, this.getSelectedPool(datadoc));
			} else if (subcmd.equals("start")) {
				String poolName = datadoc.getValue(POOL_NAME);
				MDWConnectionPool pool = getCurrentPool(poolName);
				if (pool instanceof AdapterConnectionPool) {
					if (START_IN_MEMORY_ONLY) {
						((AdapterConnectionPool)pool).broadcastPoolStatus(AdapterConnectionPool.STATUS_MANUAL_UP);
						Thread.sleep(2000);	// wait for broadcast to take efect
						datadoc.setValue(POOL_ENABLED, pool.isStarted()?"true":"false");
					} else {
						((AdapterConnectionPool)pool).setEnabled(true);
						// the followings are for refreshing GUI
						fetchPropertyTableData(datadoc, pool);
						datadoc.setValue(POOL_ENABLED, "true");
					}
				} else {
					throw new UserException("Cannot manually enable a database connection pool");
				}
			} else if (subcmd.equals("stop")) {
				String poolName = datadoc.getValue(POOL_NAME);
				MDWConnectionPool pool = getCurrentPool(poolName);
				if (pool instanceof AdapterConnectionPool) {
					if (START_IN_MEMORY_ONLY) {
						((AdapterConnectionPool)pool).broadcastPoolStatus(AdapterConnectionPool.STATUS_MANUAL_DOWN);
						Thread.sleep(2000);	// wait for broadcast to take efect
						datadoc.setValue(POOL_ENABLED, pool.isStarted()?"true":"false");
					} else {
						((AdapterConnectionPool)pool).setEnabled(false);
						// the followings are for refreshing GUI
						fetchPropertyTableData(datadoc, pool);
						datadoc.setValue(POOL_ENABLED, "false");
					}
				} else {
					throw new UserException("Cannot manually disable a database connection pool");
				}
			} else if (subcmd.equals("release_conn")) {
				PooledConnection conn = this.getSelectedConnection(datadoc);
				if (conn.getAssignee()!=null) {
				    ((PooledAdapterConnection)conn).returnConnection(0);
				}
				String poolName = datadoc.getValue(POOL_NAME);
				MDWConnectionPool pool = getCurrentPool(poolName);
				this.fetchConnectionTableData(datadoc, pool);
			} else if (subcmd.equals("clear_conn")) {
				String poolName = datadoc.getValue(POOL_NAME);
				MDWConnectionPool pool = getCurrentPool(poolName);
				pool.destroyIdleConnections();
				this.fetchConnectionTableData(datadoc, pool);
			} else if (subcmd.equals("grab_conn")) {
				String poolName = datadoc.getValue(POOL_NAME);
				MDWConnectionPool pool = getCurrentPool(poolName);
				((AdapterConnectionPool)pool).getConnection("ConnectionPoolManager", null);
				this.fetchConnectionTableData(datadoc, pool);
			} else if (subcmd.equals("refresh_conn")) {
				String poolName = datadoc.getValue(POOL_NAME);
				MDWConnectionPool pool = getCurrentPool(poolName);
				this.fetchConnectionTableData(datadoc, pool);
			} else if (subcmd.equals("paging") ||
					subcmd.equals("insertrow") ||
					subcmd.equals("deleterow") ||
					subcmd.equals("updaterow")) {
				return super.handleAction(datadoc, params);
			} else {
				throw new Exception("Unknown action " + subcmd);
			}
		} catch (UserException e) {
			datadoc.addError(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			datadoc.addError(e.getMessage());
		}
		return datadoc;
	}


	private String getSelectedPool(FormDataDocument datadoc)
	throws MbengException, UserException {
		MbengNode metanode = datadoc.setSubform(null, CONNECTION_POOLS_TABLE + "_META");
		String row_str = datadoc.getValue(metanode, "selected");
		if (StringHelper.isEmpty(row_str) || row_str.indexOf(",")>0)
			throw new UserException("You must select a (single) row");
		int row_index = Integer.parseInt(row_str);
		MbengNode tablenode = datadoc.setTable(null, CONNECTION_POOLS_TABLE, false);
		MbengNode row = datadoc.getRow(tablenode, row_index);
		if (row==null) throw new UserException("No row found for index " + row_str);
		String poolName = datadoc.getValue(row, "PoolName");
		return poolName;
	}

	private PooledConnection getSelectedConnection(FormDataDocument datadoc)
	throws MbengException, UserException {
		MbengNode metanode = datadoc.setSubform(null, CONNECTIONS_TABLE + "_META");
		String row_str = datadoc.getValue(metanode, "selected");
		if (StringHelper.isEmpty(row_str) || row_str.indexOf(",")>0)
			throw new UserException("You must select a (single) row");
		int row_index = Integer.parseInt(row_str);
		MbengNode tablenode = datadoc.setTable(null, CONNECTIONS_TABLE, false);
		MbengNode row = datadoc.getRow(tablenode, row_index);
		if (row==null) throw new UserException("No row found for index " + row_str);
		String idstr = datadoc.getValue(row, "Id");
		int id = Integer.parseInt(idstr);
		String poolName = datadoc.getValue(POOL_NAME);
		MDWConnectionPool pool = getCurrentPool(poolName);
		for (PooledConnection conn: pool.getConnectionList()) {
			if (conn.getId()==id) return conn;
		}
		throw new UserException("No connection is found with the ID");
	}

	private void fetchPoolTableData(FormDataDocument datadoc) throws MbengException {
		MbengNode tablenode = datadoc.setTable(null, CONNECTION_POOLS_TABLE, true);
		Set<String> poolnames = ConnectionPoolRegistration.getPoolNames();
		for (String one : poolnames) {
			MbengNode row = datadoc.addRow(tablenode);
			datadoc.setValue(row, POOL_NAME, one);
		}
	}

	private MDWConnectionPool getCurrentPool(String poolName) {
		return ConnectionPoolRegistration.getPool(poolName);
	}

	private void fetchPropertyTableData(FormDataDocument datadoc, MDWConnectionPool pool)
	throws MbengException {
		MbengNode propertiesNode = datadoc.setTable(null, PROPERTIES_TABLE, true);

		Properties props = ((AdapterConnectionPool)pool).getProperties();
		for (Object pn : props.keySet()) {
			MbengNode row = datadoc.addRow(propertiesNode);
			datadoc.setValue(row, "PropertyName", (String)pn);
			datadoc.setValue(row, "PropertyValue", props.getProperty((String)pn));
		}
	}

	private void fetchConnectionTableData(FormDataDocument datadoc, MDWConnectionPool pool)
	throws MbengException {
		MbengNode connectionsNode = datadoc.setTable(null, CONNECTIONS_TABLE, true);
		List<PooledConnection> connections = pool.getConnectionList();
		for (PooledConnection one : connections) {
			MbengNode row = datadoc.addRow(connectionsNode);
			datadoc.setValue(row, "Id", Integer.toString(one.getId()));
			datadoc.setValue(row, "Assignee", one.getAssignee());
			if (one.getAssignTime()!=null)
				datadoc.setValue(row, "AssignTime", StringHelper.dateToString(one.getAssignTime()));
		}

		MbengNode waitingListNode = datadoc.setTable(null, WAITING_LIST, true);
		if (pool instanceof AdapterConnectionPool) {
			List<String> waitingRequests = ((AdapterConnectionPool)pool).getWaitingRequests();
			for (String one : waitingRequests) {
				MbengNode row = datadoc.addRow(waitingListNode);
				datadoc.setValue(row, WAITING_REQUEST, one);
			}
		}

		datadoc.setValue(MAX_REQUESTS, Integer.toString(pool.getMaxConnectionRequests()));
		datadoc.setValue(TOTAL_REQUESTS, Integer.toString(pool.getTotalConnectionRequests()));
	}

	private void showPool(FormDataDocument datadoc, String poolName)
	throws MbengException, UserException {
		MDWConnectionPool pool= getCurrentPool(poolName);
		datadoc.setValue(POOL_NAME, poolName);
		datadoc.setValue(POOL_ENABLED, pool.isStarted()?"true":"false");
		fetchPropertyTableData(datadoc, pool);
		fetchConnectionTableData(datadoc, pool);
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

	private void updatePoolProperty(String poolName, String pn, String pv) throws Exception {
		String attrname = PropertyNames.MDW_CONNECTION_POOL + "." + poolName + "." + pn;
		if (pv!=null && pv.length()==0) pv = null;
		EventManager eventMgr = ServiceLocator.getEventManager();
		eventMgr.setAttribute(OwnerType.SYSTEM, 0L, attrname, pv);	// always save to database?
		PropertyManager propmgr = PropertyManager.getInstance();
		propmgr.setStringProperty(attrname, pv);
		if (propmgr instanceof PropertyManagerDatabase)
			((PropertyManagerDatabase)propmgr).putPropertySource(attrname, PropertyManagerDatabase.DATABASE);
		setPropertyGlobally(attrname, pv);
		AdapterConnectionPool pool = ConnectionPoolRegistration.getPool(poolName);
		if (pv!=null) pool.getProperties().put(pn, pv);
		else pool.getProperties().remove(pn);
	}

	private void deletePoolProperties(String poolName, Properties props) throws Exception {
		String attrname;
		EventManager eventMgr = ServiceLocator.getEventManager();
		for (Object key : props.keySet()) {
			attrname = PropertyNames.MDW_CONNECTION_POOL + "." + poolName + "." + (String)key;
			eventMgr.setAttribute(OwnerType.SYSTEM, 0L, attrname, "");
		}
		// property manager will be refreshed by ConnectionPoolRegistration.refresh()
	}

	private void insertPoolProperties(String poolName, Properties props) throws Exception {
		String attrname;
		EventManager eventMgr = ServiceLocator.getEventManager();
		for (Object key : props.keySet()) {
			attrname = PropertyNames.MDW_CONNECTION_POOL + "." + poolName + "." + (String)key;
			eventMgr.setAttribute(OwnerType.SYSTEM, 0L, attrname, props.getProperty((String)key));
		}
		// property manager will be refreshed by ConnectionPoolRegistration.refresh()
	}

	@Override
	protected void insertTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
		if (tablepath.equals(CONNECTION_POOLS_TABLE)) {
			String poolName = datadoc.getValue(row, "PoolName");
			AdapterConnectionPool pool = ConnectionPoolRegistration.addPool(poolName);
			pool.getProperties().put(AdapterConnectionPool.PROP_ADAPTER, "- enter adapter class -");
			pool.getProperties().put(AdapterConnectionPool.PROP_POOL_SIZE, "1");
			pool.getProperties().put(AdapterConnectionPool.PROP_DISABLED, "true");
			insertPoolProperties(poolName, pool.getProperties());
			this.showPool(datadoc, poolName);
			Thread.sleep(2000);		// wait till database update committed
			CacheRegistration.broadcastRefresh(PropertyManager.class.getName() + ","
					+ ConnectionPoolRegistration.class.getName(),
					MessengerFactory.newInternalMessenger());
		} else if (tablepath.equals(PROPERTIES_TABLE)) {
			String poolName = datadoc.getValue(POOL_NAME);
			String propName = datadoc.getValue(row, "PropertyName");
			String propValue = datadoc.getValue(row, "PropertyValue");
			updatePoolProperty(poolName, propName, propValue);
		} else if (tablepath.equals(CONNECTIONS_TABLE)) {
	        System.err.println("Should never reach here");
		}
	}

	@Override
	protected void deleteTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
		if (tablepath.equals(CONNECTION_POOLS_TABLE)) {
			String poolName = datadoc.getValue(row, "PoolName");
			AdapterConnectionPool pool = ConnectionPoolRegistration.getPool(poolName);
			if (pool.isStarted()) throw new Exception("You must stop the pool before deleting it");
			ConnectionPoolRegistration.removePool(poolName);
			this.deletePoolProperties(poolName, pool.getProperties());
			String showingPoolName = datadoc.getValue(POOL_NAME);
			if (poolName.equals(showingPoolName)) {
				datadoc.setValue(POOL_NAME, null);
				datadoc.setTable(null, PROPERTIES_TABLE, true);
				datadoc.setTable(null, CONNECTIONS_TABLE, true);
			}
			Thread.sleep(2000);		// wait till database update committed
			CacheRegistration.broadcastRefresh(PropertyManager.class.getName()
					+ "," + ConnectionPoolRegistration.class.getName(),
					MessengerFactory.newInternalMessenger());
		} else if (tablepath.equals(PROPERTIES_TABLE)) {
			String poolName = datadoc.getValue(POOL_NAME);
			String propName = datadoc.getValue(row, "PropertyName");
			updatePoolProperty(poolName, propName, null);
		} else if (tablepath.equals(CONNECTIONS_TABLE)) {
	        System.err.println("Should never reach here");
		}
	}

	@Override
	protected void updateTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		super.verifyRole(datadoc, UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.PROCESS_EXECUTION);
		if (tablepath.equals(CONNECTION_POOLS_TABLE)) {
	        System.err.println("Should never reach here");
		} else if (tablepath.equals(PROPERTIES_TABLE)) {
			String poolName = datadoc.getValue(POOL_NAME);
			String propName = datadoc.getValue(row, "PropertyName");
			String propValue = datadoc.getValue(row, "PropertyValue");
			updatePoolProperty(poolName, propName, propValue);
		} else if (tablepath.equals(CONNECTIONS_TABLE)) {
	        System.err.println("Should never reach here");
		}
	}

	@Override
	protected void fetchTablePage(FormDataDocument datadoc,
			MbengNode tablenode, int start_row, int nrows, String sortOn,
			boolean descending) throws Exception {
		if (tablenode.getName().equals(CONNECTION_POOLS_TABLE)) {
			fetchPoolTableData(datadoc);
		} else if (tablenode.getName().equals(PROPERTIES_TABLE)) {
			String poolName = datadoc.getValue(POOL_NAME);
			fetchPropertyTableData(datadoc, getCurrentPool(poolName));
		} else if (tablenode.getName().equals(CONNECTIONS_TABLE)) {
			String poolName = datadoc.getValue(POOL_NAME);
			fetchConnectionTableData(datadoc, getCurrentPool(poolName));
		}
	}

	private void fetchServerList(FormDataDocument datadoc)
			throws MbengException, PropertyException {
		List<String> servers = ApplicationContext.getCompleteServerList();
		MbengNode serverListNode = datadoc.setTable(null, SERVER_LIST_NODE, true);
		for (String server : servers) {
			datadoc.addEntry(serverListNode, server.replace(":", " port "));
		}
		//datadoc.addEntry(serverListNode, "127.0.0.1 port 7001");
		String host = ApplicationContext.getServerHost();
		int port = ApplicationContext.getServerPort();
		datadoc.setValue(MANAGED_SERVER, host + ":" + port);
	}


	// TODO see below
	// * test in cluster environment
	// * load testing

}
