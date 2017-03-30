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
package com.centurylink.mdw.workflow.adapter.http;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.util.HttpConnection;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.workflow.adapter.PoolableAdapterBase;

/**
 * Provides a base implementation for SOAP and Rest adapters
 *
 * @author aa70413
 *
 */
public class HttpServiceAdapter extends PoolableAdapterBase {

    public static final String PROP_USER = "USER";
    public static final String PROP_PASS = "PASS";
    public static final String CONNECT_TIMEOUT = "ConnectTimeout";
    public static final String READ_TIMEOUT = "ReadTimeout";

    /**
     * <p>
     * Allows the overriding of this method without overriding invoke()
     * Default implementation is using the constructor
     * new HttpHelper((HttpURLConnection)connection, user, pass);
     * </p>
     * @return an HttpHelper instance
     * @throws PropertyException
     */
    public HttpHelper getHttpHelper(Object connection) throws PropertyException {
        String user = getAttributeValueSmart(PROP_USER);
        String pass = getAttributeValueSmart(PROP_PASS);
        HttpHelper helper = new HttpHelper((HttpConnection)connection);
        if (user != null) {
            helper.getConnection().setUser(user);
            helper.getConnection().setPassword(pass);
        }
        return helper;
    }

    @Override
    protected Response getResponse(Object connection, String responseString) throws IOException {
        Response response = super.getResponse(connection, responseString);
        if (connection instanceof HttpConnection) {
            HttpConnection httpConn = (HttpConnection) connection;
            if (httpConn.getResponse() != null) {
                response.setStatusCode(httpConn.getResponse().getCode());
                response.setStatusMessage(httpConn.getResponse().getMessage());
            }
        }
        return response;
    }


    @Override
    public void init(Properties parameters) {
    }

    @Override
    public void init() throws ConnectionException, AdapterException {

    }

    @Override
    public Object openConnection() throws ConnectionException, AdapterException {
        return null;
    }

    @Override
    public void closeConnection(Object connection) {
        // connection is closed by HttpHelper
    }

    /**
     * @see com.centurylink.mdw.adapter.PoolableAdapter#invoke(java.lang.Object, java.lang.String, int, java.util.Map)
     */
    @Override
    public String invoke(Object connection, String request, int timeout, Map<String, String> headers)
    throws AdapterException, ConnectionException {
        return null;
    }

    @Override
    public boolean ping(int timeout) {
         return false;
    }

    @Override
    protected boolean canBeSynchronous() {
        return true;
    }

    @Override
    protected boolean canBeAsynchronous() {
        return false;
    }

    /**
     *
     * @return connect timeout in milliseconds
     * @throws ActivityException
     */
    protected int getConnectTimeout() throws ActivityException {
        String connectTimeout = null;
        try {
            connectTimeout = getAttributeValueSmart(CONNECT_TIMEOUT);
            if (connectTimeout != null)
                return Integer.parseInt(connectTimeout);
            return -1;
        }
        catch (NumberFormatException ex) {
            throw new ActivityException("Invalid value for Connect Timeout attribute: " + connectTimeout);
        }
        catch (PropertyException ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    /**
     *
     * @return read timeout in seconds
     * @throws ActivityException
     */
    protected int getReadTimeout() throws ActivityException {
        String readTimeout = null;
        try {
            readTimeout = getAttributeValueSmart(READ_TIMEOUT);
            if (readTimeout != null)
                return Integer.parseInt(readTimeout);
            return -1;
        }
        catch (NumberFormatException ex) {
            throw new ActivityException("Invalid value for Request Timeout attribute: " + readTimeout);
        }
        catch (PropertyException ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

}
