/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.pooling;

import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.adapter.PoolableAdapter;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;

/**
 * An instance of this class represents a connection in a connection pool.
 * 
 * From the connection to the external systems point of view,
 * the pooled connections can implement one of the two styles:
 *  a) one-connection-for-all: keep the same connection for all usage.
 *      For this style, the connection is opened when the pooled connection is created,
 *      and the connection is closed when the pooled connection is destroyed.
 *  b) one-connection-per-call: create a connection on each call. In this style,
 *      the invoke() method here invokes openConnection() and closeConnection()
 *      of the adapter around its invoke() method.
 * 
 *
 */
public class PooledAdapterConnection extends PooledConnection {
    
    private AdapterConnectionPool pool;
    private PoolableAdapter adapter;
    private Object connection;        // object returned by openConnection, typically the same as adapter
    private boolean one_connection_for_all;
    
    /**
     * @throws Exception
     */
    public PooledAdapterConnection(AdapterConnectionPool pool,
            PoolableAdapter adapter, Properties props) throws Exception {
        this.adapter = adapter;
        this.pool = pool;
        this.one_connection_for_all = "true".equalsIgnoreCase(
                props.getProperty(AdapterConnectionPool.PROP_ONE_CONNECTION_FOR_ALL));
        adapter.init(props);
        if (one_connection_for_all) connection = adapter.openConnection();
    }

    public void destroy() {
        if (one_connection_for_all) adapter.closeConnection(connection);
    }
    
    /**
     * This is used in the client code using the connection.
     * You must invoke this method when the call is finished,
     * to return the connection to the pool.
     * It is recommended to put this call in the "finally" 
     * arm of a "try" construct that get the connection,
     * to ensure the connection is always returned.
     */
    public final void returnConnection(int exceptionCode) {
        pool.returnToPool(this, exceptionCode);
    }
    
    /**
     * 
     * @param message
     * @param timeout
     * @param metainfo per-call meta data such as correlation ID
     * @return
     * @throws ConnectionException when there is a retriable error
     * @throws AdapterException when there is a non-retriable error
     */
    public String invoke(String message, int timeout, Map<String,String> metainfo) 
    throws ConnectionException, AdapterException {
        if (one_connection_for_all) {
            return adapter.doInvoke(connection, message, timeout, metainfo);
        } else {
            Object connection = null;
            try {
                connection = adapter.openConnection();
                String response = adapter.doInvoke(connection, message, timeout, metainfo);
                return response;
            } finally {
                if (connection!=null) adapter.closeConnection(connection);
            }
        }
    }
    
    /**
     * This method is used for auto-shutdown and restart.
     * @return true if the connection is up, and false if it is still down.
     */
    protected boolean ping() {
        if (one_connection_for_all) {
            return adapter.ping(pool.getPingTimeout());
        } else {
            Object connection = null;
            try {
                connection = adapter.openConnection();
                return adapter.ping(pool.getPingTimeout());
            } catch (Exception e) {
                return false;
            } finally {
                if (connection!=null) adapter.closeConnection(connection);
            }
        }
    }
    
}
