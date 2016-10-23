/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.adapter;

import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;

/**
 * This class is an interface for defining poolable adapters
 * which can be used as activities as well as by connection pools.
 * A connection pool manages a set of connections, represented
 * by instances of the class PooledConnection, and each connection
 * manages an instance of PoolableAdapter.
 *
 *
 */
public interface PoolableAdapter {

    /**
     * Initialization when used by a connection pool
     * @param parameters
     */
    void init(Properties parameters);

    /**
     * Initialization when used by an adapter
     */
    void init()
    throws ConnectionException, AdapterException;

    /**
     * Open connection
     * @return An object representing an open connection. It is
     *         typical to return the adapter activity instance itself,
     *         although any other object (but null) can be returned,
     *         which will be parsed in as a parameter to invoke() and closeConnection().
     * @throws ConnectionException retriable connection failure
     * @throws AdapterException non-retriable connection failure
     *
     */
    Object openConnection()
    throws ConnectionException, AdapterException;


    /**
     * Close a connection opened.
     * @param conneciton the object returned by openConnection()
     *         and is typically the adapter activity instance itself.
     */
    void closeConnection(Object connection);


    /**
     * send a message to external system, and if the underlying
     * protocol is synchronous (WebService, EJB, etc), the
     * response is returned.
     *
     * To support auto-retry (may or may not be certified message),
     * the implementation should throws AdapterException when
     * an error is detected and is non-retriable, and throws
     * ConnectionException if hitting a retriable error.
     *
     * When it is used to support certified message, the method
     * should throw exceptions unless a correct acknowledgment is received.
     *
     * @param conneciton the object returned by openConnection()
     *         and is typically the adapter activity instance itself.
     * @param request
     * @param timeout time out in seconds. Ignored if no response is expected.
     * @param headers protocol request data, e.g. correlation ID
     * @return response if the underlying protocol is synchronous
     *    or null otherwise
     * @throws ConnectionException retriable exception
     * @throws AdapterException non-retriable exception
     */
    String invoke(Object connection,
            String request, int timeout, Map<String,String> headers)
    throws AdapterException,ConnectionException;

    String doInvoke(Object connection,
            String request, int timeout, Map<String,String> headers)
    throws AdapterException,ConnectionException;

    /**
     * This method is used for auto-shutdown and restart in a connection pool,
     * so need only to be implemented appropriately if you intend to use auto-restart feature.
     * The method is only used by connection pools, not by activities.
     *
     * @param timeout timeout in seconds. The value is obtained from
     *         the property PingTimeout in the pool configuration.
     *         If PingTimeout is not specified, the value of the property "timeout" is used.
     *         If "timeout" is not specified either, then -1 is passed in.
     * @return true if the connection is up, and false if it is still down.
     */
    boolean ping(int timeout);

    /**
     * This method is only used by activities, not by connection pools.
     * @param response
     * @throws ActivityException
     * @throws ConnectionException
     * @throws AdapterException
     */
    void onSuccess(String response)
    throws ActivityException, ConnectionException, AdapterException;

    /**
     * This method is only used by activities, not by connection pools.
     * @param errorCause
     * @return completion code
     * @throws AdapterException
     * @throws ConnectionException
     */
    String onFailure(Throwable errorCause)
    throws AdapterException,ConnectionException;

//    /**
//     * returns portion of the pagelet description for implementors
//     * @return
//     */
//    String getDescriptorInserts();

}
