/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.data.monitor.CertifiedMessage;
import com.centurylink.mdw.model.data.monitor.ScheduledEvent;
import com.centurylink.mdw.model.value.event.InternalEventVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.services.event.CertifiedMessageManager;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.pooling.AdapterConnectionPool;
import com.centurylink.mdw.services.pooling.ConnectionPoolRegistration;
import com.centurylink.mdw.services.pooling.PooledAdapterConnection;

/**
 * New implementation of JMS Adapter which can be
 * configured through Designer and does not implement
 * ControlledAdapterActivity interface.
 *
 */
public class ConnectionPoolAdapter extends PoolableAdapterBase implements SuspendibleActivity {

    public static final String POOL_NAME = "Connection Pool";

    private Long requestLoggingId;
    private int exceptionCode;
	private AdapterConnectionPool pool;
	private boolean fromResume = false;

    @Override
    protected boolean canBeSynchronous() {
    	return true;
    }

    @Override
    protected boolean canBeAsynchronous() {
    	return true;
    }

    @Override
    protected boolean canBeCertified() {
    	return true;
    }

    /**
     * The method overrides the one in the super class to perform
     * JMS specific functions.
     */
    @Override
    public String invoke(Object conn, String requestData, int timeout, Map<String,String> meta_data)
        throws AdapterException, ConnectionException
    {
        String result;

        if (this.isCertified()) {
        	try {
        		Long docid;
        		if (requestLoggingId==null) {
        			DocumentReference docref = super.createDocument(XmlObject.class.getName(),
        					requestData, OwnerType.ACTIVITY_INSTANCE_REQUEST,
        					this.getActivityInstanceId(), null, null);
        			docid = docref.getDocumentId();
        		} else docid = requestLoggingId;
				CertifiedMessageManager queue = CertifiedMessageManager.getSingleton();
				Map<String,String> props = new HashMap<String,String>();
				props.put(CertifiedMessage.PROP_PROTOCOL, CertifiedMessage.PROTOCOL_POOL);
				props.put(CertifiedMessage.PROP_POOL_NAME, pool.getName());
				String v;
    			v = getProperty(AdapterActivity.PROP_TIMEOUT, pool, AdapterConnectionPool.PROP_TIMEOUT);
				if (!StringHelper.isEmpty(v)) props.put(CertifiedMessage.PROP_TIMEOUT, v);
    			v = getProperty(AdapterActivity.PROP_RETRY_INTERVAL, pool, AdapterConnectionPool.PROP_RETRY_INTERVAL);
				if (!StringHelper.isEmpty(v)) props.put(CertifiedMessage.PROP_RETRY_INTERVAL, v);
    			v = getProperty(AdapterActivity.PROP_MAX_TRIES, pool, AdapterConnectionPool.PROP_MAX_TRIES);
				if (!StringHelper.isEmpty(v)) props.put(CertifiedMessage.PROP_MAX_TRIES, v);
				if (meta_data!=null) props.putAll(meta_data);
				queue.sendTextMessage(props, (String)requestData, docid, logtag());
				result = null;
			} catch (Exception e) {
				throw new ConnectionException(ConnectionException.CONNECTION_DOWN, e.getMessage(), e);
			}
        } else if (this.isSynchronous()) {
        	PooledAdapterConnection connection = (PooledAdapterConnection)conn;
        	result = connection.invoke((String)requestData, timeout, meta_data);
        } else {
        	PooledAdapterConnection connection = (PooledAdapterConnection)conn;
        	connection.invoke(requestData, -1, meta_data);
        	result = null;
        }
        return result;
    }

    @Override
    public Object openConnection() throws ConnectionException,AdapterException {
		requestLoggingId = null;
		exceptionCode = 0;
    	if (this.isCertified()) {
    		return this;
    	} else if (fromResume) {
    		return pool.getReservedConnection(logtag(), this.getActivityInstanceId());
    	} else {
    		return pool.getConnection(logtag(), this.getActivityInstanceId());
    	}
    }

    @Override
    public void closeConnection(Object connection) {
    	if (!this.isCertified()) {
    		PooledAdapterConnection conn = (PooledAdapterConnection)connection;
    		conn.returnConnection(exceptionCode);
    	}
    }

    @Override
	protected Long logMessage(String message, boolean isResponse) {
    	Long docid = super.logMessage(message, isResponse);
    	if (!isResponse) requestLoggingId = docid;
    	return docid;
	}

	private String getProperty(String attrName, AdapterConnectionPool pool, String poolPropName) {
		String v = this.getAttributeValue(attrName);
		if (StringHelper.isEmpty(v)) v = pool.getProperty(poolPropName);
		return v;
	}

	@Override
	protected int getMaxTries() {
    	String v = getProperty(PROP_MAX_TRIES, pool, AdapterConnectionPool.PROP_MAX_TRIES);
    	return StringHelper.getInteger(v, 12);
	}

	@Override
	protected int getRetryInterval() {
		String v = getProperty(PROP_RETRY_INTERVAL, pool, AdapterConnectionPool.PROP_RETRY_INTERVAL);
        return StringHelper.getInteger(v, 600);
	}

	@Override
	public String onFailure(Throwable errorCause)
	throws AdapterException,ConnectionException {
		this.exceptionCode = super.getErrorCode(errorCause);
		return super.onFailure(errorCause);
	}

	/**
	 * This method is not used, as connection pools do not use this generic adapter.
	 */
	@Override
	public void init(Properties parameters) {
	}

	@Override
	public void init() throws ConnectionException, AdapterException {
		String pool_name = this.getAttributeValue(POOL_NAME);
		if (pool_name==null) throw new AdapterException("Pool name not specified");
		pool = ConnectionPoolRegistration.getPool(pool_name);
    	if (pool==null) throw new AdapterException("Connection pool not defined: " + pool_name);
	}

	/**
	 * This method is not used, as connection pools do not use this generic adapter.
	 */
	@Override
	public boolean ping(int timeout) {
		return false;
	}

	@Override
    protected final void handleConnectionException(int errorCode, Throwable originalCause)
    throws ActivityException {
    	ScheduledEventQueue eventQueue = ScheduledEventQueue.getSingleton();
    	if (errorCode==ConnectionException.POOL_EXHAUSTED || errorCode==ConnectionException.POOL_DISABLED) {
    		ActivityInstanceVO actinst = this.getActivityInstance();
    		InternalEventVO message = InternalEventVO.createActivityNotifyMessage(actinst,
					EventType.RESUME, getMasterRequestId(),
					COMPCODE_AUTO_RETRY);
        	loginfo("suspend the activity - " + originalCause.getMessage());
        	eventQueue.scheduleInternalEvent(ScheduledEvent.INTERNAL_EVENT_PREFIX+this.getActivityInstanceId(),
        			null, message.toString(), "pool:"+pool.getName());
    	} else {
    		InternalEventVO message = InternalEventVO.createActivityStartMessage(getActivityId(),
    				getProcessInstanceId(), getWorkTransitionInstanceId(), getMasterRequestId(),
    				COMPCODE_AUTO_RETRY);
    		int retry_interval = this.getRetryInterval();
    		Date scheduledTime = new Date(DatabaseAccess.getCurrentTime()+retry_interval*1000);
    		loginfo("The activity failed, set to retry at " + StringHelper.dateToString(scheduledTime));
    		eventQueue.scheduleInternalEvent(ScheduledEvent.INTERNAL_EVENT_PREFIX+this.getActivityInstanceId(),
    				scheduledTime, message.toString(), "pool:"+pool.getName());
    		this.setReturnCode(COMPCODE_AUTO_RETRY);
    		// the above is to prevent engine from making transitions (typically to exception handler)
    		throw new ActivityException(errorCode, originalCause.getMessage(), originalCause);
    	}
	}

	public boolean needSuspend() {
		return exceptionCode == ConnectionException.POOL_EXHAUSTED
			|| exceptionCode==ConnectionException.POOL_DISABLED;
	}

	public boolean resume(InternalEventVO eventMessageDoc)
			throws ActivityException {
		fromResume = true;
		this.execute();		// openConnection actually gets the already assigned connection
		return !needSuspend();
	}

	public boolean resumeWaiting(InternalEventVO eventMessageDoc)
			throws ActivityException {
		// not currently used (the activity is never put in hold status)
		return true;
	}

	/**
	 * Override to look for connection pool setting when the attribute
	 * is not set in this activity
	 */
	@Override
    protected int getTimeoutForResponse() {
    	String timeout_s=null;
    	int timeout;
    	try {
    		timeout_s = this.getProperty(AdapterActivity.PROP_TIMEOUT, pool, AdapterConnectionPool.PROP_TIMEOUT);
			timeout = timeout_s==null?-1:Integer.parseInt(timeout_s);
		} catch (NumberFormatException e) {
			logger.severeException("Cannot parse timeout value " + timeout_s, e);
			timeout = -1;
		}
		return timeout;
    }

}
