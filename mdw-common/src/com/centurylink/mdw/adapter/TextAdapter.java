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
package com.centurylink.mdw.adapter;

import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.Response;

/**
 * Interface for text-based adapter activities.
 */
public interface TextAdapter {

    /**
     * Initialization.
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

    Response doInvoke(Object connection,
            String request, int timeout, Map<String,String> headers)
    throws AdapterException,ConnectionException;

    /**
     * Handle success response.
     * @param response
     * @throws ActivityException
     * @throws ConnectionException
     * @throws AdapterException
     */
    void onSuccess(String response)
    throws ActivityException, ConnectionException, AdapterException;

    /**
     * Handle failure response.
     * @param errorCause
     * @return completion code
     * @throws AdapterException
     * @throws ConnectionException
     */
    String onFailure(Throwable errorCause)
    throws AdapterException,ConnectionException;

}
