/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.workflow.adapter;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.activity.types.SuspendibleActivity;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.pooling.AdapterConnectionPool;
import com.centurylink.mdw.services.pooling.ConnectionPoolRegistration;
import com.centurylink.mdw.services.pooling.PooledAdapterConnection;
import com.centurylink.mdw.util.StringHelper;

/**
 * New implementation of JMS Adapter which can be
 * configured through Designer and does not implement
 * ControlledAdapterActivity interface.
 *
 */
public class ConnectionPoolAdapter extends PoolableAdapterBase implements SuspendibleActivity {

    public static final String POOL_NAME = "Connection Pool";

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

        if (this.isSynchronous()) {
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
        exceptionCode = 0;
        if (fromResume) {
            return pool.getReservedConnection(logtag(), this.getActivityInstanceId());
        } else {
            return pool.getConnection(logtag(), this.getActivityInstanceId());
        }
    }

    @Override
    public void closeConnection(Object connection) {
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
            ActivityInstance actinst = this.getActivityInstance();
            InternalEvent message = InternalEvent.createActivityNotifyMessage(actinst,
                    EventType.RESUME, getMasterRequestId(),
                    COMPCODE_AUTO_RETRY);
            loginfo("suspend the activity - " + originalCause.getMessage());
            eventQueue.scheduleInternalEvent(ScheduledEvent.INTERNAL_EVENT_PREFIX+this.getActivityInstanceId(),
                    null, message.toString(), "pool:"+pool.getName());
        } else {
            InternalEvent message = InternalEvent.createActivityStartMessage(getActivityId(),
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

    public boolean resume(InternalEvent eventMessageDoc)
            throws ActivityException {
        fromResume = true;
        this.execute();        // openConnection actually gets the already assigned connection
        return !needSuspend();
    }

    public boolean resumeWaiting(InternalEvent eventMessageDoc)
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
