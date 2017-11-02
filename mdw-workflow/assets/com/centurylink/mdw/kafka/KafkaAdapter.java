/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */

package com.centurylink.mdw.kafka;

import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;

/**
 * Dynamic Java workflow asset.
 */
@Tracked(LogLevel.TRACE)
public class KafkaAdapter extends PoolableAdapterBase implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /* (non-Javadoc)
     * @see com.centurylink.mdw.adapter.PoolableAdapter#init(java.util.Properties)
     */
    @Override
    public void init(Properties parameters) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.adapter.PoolableAdapter#init()
     */
    @Override
    public void init() throws ConnectionException, AdapterException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.adapter.PoolableAdapter#openConnection()
     */
    @Override
    public Object openConnection() throws ConnectionException, AdapterException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.adapter.PoolableAdapter#closeConnection(java.lang.Object)
     */
    @Override
    public void closeConnection(Object connection) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.adapter.PoolableAdapter#invoke(java.lang.Object, java.lang.String, int, java.util.Map)
     */
    @Override
    public String invoke(Object connection, String request, int timeout,
            Map<String, String> headers) throws AdapterException, ConnectionException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.adapter.PoolableAdapter#ping(int)
     */
    @Override
    public boolean ping(int timeout) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.workflow.adapter.PoolableAdapterBase#canBeSynchronous()
     */
    @Override
    protected boolean canBeSynchronous() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.workflow.adapter.PoolableAdapterBase#canBeAsynchronous()
     */
    @Override
    protected boolean canBeAsynchronous() {
        // TODO Auto-generated method stub
        return false;
    }


}
