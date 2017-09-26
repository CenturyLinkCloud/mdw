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
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.util.HttpConnection;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
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
    public static final String RETRY_HTTP_CODES = "RetryHttpCodes";
    public static final int DEFAULT_HTTP_CODE = Status.INTERNAL_ERROR.getCode();

    private Map<String,String> requestHeaders = null;
    protected Map<String,String> getRequestHeaders() { return requestHeaders; }
    protected void setRequestHeaders(Map<String,String> headers) { requestHeaders = headers; }

    /**
     * <p>
     * Allows the overriding of this method without overriding invoke()
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
                if (response.getStatusCode() > 0 && StringHelper.isEmpty(response.getStatusMessage())) {
                    switch (response.getStatusCode()) {
                        case 200: response.setStatusMessage(Status.OK.getMessage());  break;
                        case 201: response.setStatusMessage(Status.CREATED.getMessage());  break;
                        case 202: response.setStatusMessage(Status.ACCEPTED.getMessage());  break;
                        case 204: response.setStatusMessage(Status.NO_CONTENT.getMessage());  break;
                        case 205: response.setStatusMessage(Status.RESET_CONTENT.getMessage());  break;
                        case 206: response.setStatusMessage(Status.PARTIAL_CONTENT.getMessage());  break;
                        case 301: response.setStatusMessage(Status.MOVED_PERMANENTLY.getMessage());  break;
                        case 302: response.setStatusMessage(Status.FOUND.getMessage());  break;
                        case 303: response.setStatusMessage(Status.SEE_OTHER.getMessage());  break;
                        case 304: response.setStatusMessage(Status.NOT_MODIFIED.getMessage());  break;
                        case 305: response.setStatusMessage(Status.USE_PROXY.getMessage());  break;
                        case 307: response.setStatusMessage(Status.TEMPORARY_REDIRECT.getMessage());  break;
                        case 400: response.setStatusMessage(Status.BAD_REQUEST.getMessage());  break;
                        case 401: response.setStatusMessage(Status.UNAUTHORIZED.getMessage());  break;
                        case 402: response.setStatusMessage(Status.PAYMENT_REQUIRED.getMessage());  break;
                        case 403: response.setStatusMessage(Status.FORBIDDEN.getMessage());  break;
                        case 404: response.setStatusMessage(Status.NOT_FOUND.getMessage());  break;
                        case 405: response.setStatusMessage(Status.METHOD_NOT_ALLOWED.getMessage());  break;
                        case 406: response.setStatusMessage(Status.NOT_ACCEPTABLE.getMessage());  break;
                        case 407: response.setStatusMessage(Status.PROXY_AUTHENTICATION_REQUIRED.getMessage());  break;
                        case 408: response.setStatusMessage(Status.REQUEST_TIMEOUT.getMessage());  break;
                        case 409: response.setStatusMessage(Status.CONFLICT.getMessage());  break;
                        case 410: response.setStatusMessage(Status.GONE.getMessage());  break;
                        case 411: response.setStatusMessage(Status.LENGTH_REQUIRED.getMessage());  break;
                        case 412: response.setStatusMessage(Status.PRECONDITION_FAILED.getMessage());  break;
                        case 413: response.setStatusMessage(Status.REQUEST_ENTITY_TOO_LARGE.getMessage());  break;
                        case 414: response.setStatusMessage(Status.REQUEST_URI_TOO_LONG.getMessage());  break;
                        case 415: response.setStatusMessage(Status.UNSUPPORTED_MEDIA_TYPE.getMessage());  break;
                        case 416: response.setStatusMessage(Status.REQUESTED_RANGE_NOT_SATISFIABLE.getMessage());  break;
                        case 417: response.setStatusMessage(Status.EXPECTATION_FAILED.getMessage());  break;
                        case 500: response.setStatusMessage(Status.INTERNAL_SERVER_ERROR.getMessage());  break;
                        case 501: response.setStatusMessage(Status.NOT_IMPLEMENTED.getMessage());  break;
                        case 502: response.setStatusMessage(Status.BAD_GATEWAY.getMessage());  break;
                        case 503: response.setStatusMessage(Status.SERVICE_UNAVAILABLE.getMessage());  break;
                        case 504: response.setStatusMessage(Status.GATEWAY_TIMEOUT.getMessage());  break;
                        case 505: response.setStatusMessage(Status.HTTP_VERSION_NOT_SUPPORTED.getMessage());  break;
                        default: response.setStatusMessage("Unrecognized HTTP code"); break;
                    }
                }
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
