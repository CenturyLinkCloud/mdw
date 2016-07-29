/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.socket;

import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.soccom.SoccomClient;
import com.centurylink.mdw.soccom.SoccomException;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;


/**
 * Socket adapter using soccom extended socket protocol
 */
@Tracked(LogLevel.TRACE)
public class SocketAdapter extends PoolableAdapterBase {

	public static final String PROP_HOSTPORT = "hostport";

	public static final int DEFAULT_TIMEOUT = 120;

	private String hostport;
	private SoccomClient connection;

	@Override
	protected boolean canBeSynchronous() {
		return true;
	}

	@Override
	protected boolean canBeAsynchronous() {
		return false;
	}

	/**
	 * This method must be implemented for PoolableAdapter
	 */
	@Override
	public void init() throws ConnectionException, AdapterException {
		hostport = getAttribute(PROP_HOSTPORT, null, true);
	}

	/**
	 * This method must be implemented for PoolableAdapter
	 */
	@Override
	public void init(Properties parameters) {
		hostport = parameters.getProperty(PROP_HOSTPORT);
	}

	/**
	 * This method must be implemented for PoolableAdapter
	 */
	@Override
	public Object openConnection()
	throws ConnectionException, AdapterException {
        try {
        	int k = hostport.indexOf(':');
        	if (k<0) throw new AdapterException("Invalid host:port specification - " + hostport);
        	connection = new SoccomClient(hostport.substring(0,k), hostport.substring(k+1));
			return connection;
        } catch (SoccomException e) {
        	if (e.getErrorCode()==SoccomException.CONNECT)
                throw new ConnectionException(ConnectionException.CONNECTION_DOWN, e.getMessage(), e);
        	else throw new AdapterException(e.getErrorCode(), e.getMessage(), e);
        }
	}

	/**
	 * This method must be implemented for PoolableAdapter
	 */
	@Override
	public String invoke(Object connection, String request, int timeout, Map<String,String> metainfo)
	throws ConnectionException, AdapterException {
		String response;
		try{
        	if (timeout<=0) timeout = DEFAULT_TIMEOUT;
        	SoccomClient client = (SoccomClient)connection;
        	client.putreq(request);
        	response = client.getresp(timeout);
        } catch (SoccomException ex){
        	if (ex.getErrorCode()==SoccomException.POLL_TIMEOUT)
                throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        	else throw new AdapterException(-1, ex.getMessage(), ex);
		}
        return response;
	}

	/**
	 * This method must be implemented for PoolableAdapter
	 */
	@Override
	public void closeConnection(Object connection) {
		if (connection!=null) {
			((SoccomClient)connection).close();
			connection = null;
		}
	}

	/**
	 * This method must be implemented for PoolableAdapter
	 */
	@Override
	public boolean ping(int timeout) {
		return false;	// this needs to be overridden for specific application connector
	}


}
