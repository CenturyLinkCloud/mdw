/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.container.plugins.CommonThreadPool;
import com.centurylink.mdw.container.plugins.CommonThreadPool.ManagedThread;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.services.UserException;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public class ThreadPoolManagerHandler extends TablePaginator {

	private final static String POOL_INFO = "ThreadPoolInformation";
	private final static String POOL_PAUSED = "PoolPaused";
	private final static String CONNECTIONS_TABLE = "PoolConnections";
	private final static String SERVER_LIST_NODE = "ServerList";
	private final static String MANAGED_SERVER = "ManagedServer";
	private final static String WAITING_LIST = "WaitingList";
	private final static String WAITING_REQUEST = "WaitingRequest";
	private final static String MAX_REQUESTS = "MaxRequests";
	private final static String TOTAL_REQUESTS = "TotalRequests";

	public FormDataDocument handleAction(FormDataDocument datadoc, Map<String, String> params) {
		try {
			String subcmd = params.get(FormConstants.URLARG_ACTION);
			if (subcmd==null || subcmd.equals("init")) {
				fetchServerList(datadoc);
				showPool(datadoc);
			} else if (subcmd.equals("resume")) {
				CommonThreadPool pool = this.getThreadPool();
				pool.resume();
				refreshGui(datadoc);
				datadoc.setValue(POOL_PAUSED, "false");
			} else if (subcmd.equals("pause")) {
				CommonThreadPool pool = this.getThreadPool();
				pool.pause();
				refreshGui(datadoc);
				datadoc.setValue(POOL_PAUSED, "true");
			} else if (subcmd.equals("release_conn")) {
				ManagedThread thread = this.getSelectedConnection(datadoc);
				thread.interrupt();
				refreshGui(datadoc);
			} else if (subcmd.equals("clear_conn")) {
				CommonThreadPool pool = this.getThreadPool();
				pool.getThreadPoolExecutor().purge();
				refreshGui(datadoc);
			} else if (subcmd.equals("grab_conn")) {
				CommonThreadPool pool = this.getThreadPool();
				Runnable command = new Runnable() {
					public void run() {
						try {
							System.out.println("====GOOD DAY!=====");
							Thread.sleep(60000);	// 60 seconds
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				pool.execute(ThreadPoolProvider.WORKER_DEFAULT, "ThreadPoolManager", command);
				refreshGui(datadoc);
			} else if (subcmd.equals("refresh_conn")) {
				fetchStatistics(datadoc);
				this.fetchConnectionTableData(datadoc);
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
			logger.severeException("ThreadPoolManagerHandler exception", e);
			datadoc.addError(e.getMessage());
		}
		return datadoc;
	}

	private void refreshGui(FormDataDocument datadoc) throws Exception {
		Thread.sleep(2000);	// so that update is done
		fetchStatistics(datadoc);
		this.fetchConnectionTableData(datadoc);
	}

	private CommonThreadPool getThreadPool() throws UserException {
		ThreadPoolProvider poolProvider = ApplicationContext.getThreadPoolProvider();
		if (!(poolProvider instanceof CommonThreadPool))
			throw new UserException("You are not using MDW Thread Pool");
		return (CommonThreadPool)poolProvider;
	}

	private ManagedThread getSelectedConnection(FormDataDocument datadoc)
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
		CommonThreadPool pool = this.getThreadPool();
		for (ManagedThread conn: pool.getThreadList()) {
			if (conn.getName().equals(idstr)) return conn;
		}
		throw new UserException("No connection is found with the ID");
	}

	private void fetchStatistics(FormDataDocument datadoc)
	throws MbengException, UserException {
		CommonThreadPool pool = this.getThreadPool();
		datadoc.setValue(POOL_INFO, pool.currentStatus());
	}

	private void fetchConnectionTableData(FormDataDocument datadoc)
	throws MbengException, UserException {
		MbengNode connectionsNode = datadoc.setTable(null, CONNECTIONS_TABLE, true);
		CommonThreadPool pool = this.getThreadPool();
		List<ManagedThread> connections = pool.getThreadList();
		for (ManagedThread one : connections) {
			MbengNode row = datadoc.addRow(connectionsNode);
			datadoc.setValue(row, "Id", one.getName());
			datadoc.setValue(row, "Assignee", one.getAssignee());
			if (one.getAssignTime()!=null)
				datadoc.setValue(row, "AssignTime", StringHelper.dateToString(one.getAssignTime()));
		}

		MbengNode waitingListNode = datadoc.setTable(null, WAITING_LIST, true);
		Iterator<Runnable> waitingRequests = pool.getThreadPoolExecutor().getQueue().iterator();
		while (waitingRequests.hasNext()) {
			Runnable one = waitingRequests.next();
			MbengNode row = datadoc.addRow(waitingListNode);
			datadoc.setValue(row, WAITING_REQUEST, one.toString());
		}

		datadoc.setValue(MAX_REQUESTS, Long.toString(pool.getThreadPoolExecutor().getMaximumPoolSize()));
		datadoc.setValue(TOTAL_REQUESTS, Long.toString(pool.getThreadPoolExecutor().getTaskCount()));
	}

	private void showPool(FormDataDocument datadoc)
	throws MbengException, UserException {
		fetchStatistics(datadoc);
		fetchConnectionTableData(datadoc);
		CommonThreadPool pool = this.getThreadPool();
		datadoc.setValue(POOL_PAUSED, pool.isPaused()?"true":"false");
	}

	@Override
	protected void insertTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		System.err.println("Should never reach here");
	}

	@Override
	protected void deleteTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		System.err.println("Should never reach here");
	}

	@Override
	protected void updateTableRow(FormDataDocument datadoc, String tablepath,
			MbengNode row) throws Exception {
		System.err.println("Should never reach here");
	}

	@Override
	protected void fetchTablePage(FormDataDocument datadoc,
			MbengNode tablenode, int start_row, int nrows, String sortOn,
			boolean descending) throws Exception {
		fetchConnectionTableData(datadoc);
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

}
