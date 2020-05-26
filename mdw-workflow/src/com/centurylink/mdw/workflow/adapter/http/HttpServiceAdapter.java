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

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.adapter.AdapterException;
import com.centurylink.mdw.adapter.ConnectionException;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.service.data.ServicePaths;
import com.centurylink.mdw.util.HttpConnection;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.workflow.adapter.TextAdapterActivity;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;

import static com.centurylink.mdw.constant.PropertyNames.MDW_ADAPTER_CONNECTION_TIMEOUT;
import static com.centurylink.mdw.constant.PropertyNames.MDW_ADAPTER_READ_TIMEOUT;

/**
 * Provides a base implementation for SOAP and Rest adapters
 */
public class HttpServiceAdapter extends TextAdapterActivity {

    public static final String PROP_USER = "USER";
    public static final String PROP_PASS = "PASS";
    public static final String CONNECT_TIMEOUT = "ConnectTimeout";
    public static final String READ_TIMEOUT = "ReadTimeout";
    public static final String RETRY_HTTP_CODES = "RetryHttpCodes";
    public static final int DEFAULT_RETRY_HTTP_CODE = Status.INTERNAL_ERROR.getCode();
    public static final String ERROR_HTTP_CODES = "ErrorHttpCodes";
    public static final int DEFAULT_ERROR_HTTP_CODE = Status.MOVED_PERMANENTLY.getCode();  // 301

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
    public HttpHelper getHttpHelper(Object connection) throws ActivityException {
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
                if (response.getStatusCode() > 0 && StringUtils.isBlank(response.getStatusMessage())) {
                    response.setStatusMessage(StatusResponse.getMessage(response.getStatusCode()));
                }
                response.setPath(ServicePaths.getOutboundResponsePath(httpConn.getUrl(), httpConn.getMethod()));
            }
        }
        return response;
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
     * @see com.centurylink.mdw.adapter.TextAdapter#invoke(java.lang.Object, java.lang.String, int, java.util.Map)
     */
    @Override
    public String invoke(Object connection, String request, int timeout, Map<String, String> headers)
            throws AdapterException, ConnectionException {
        return null;
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
        Integer connectTimeout = null;
        try {
           connectTimeout = getAttribute(CONNECT_TIMEOUT, PropertyManager.getIntegerProperty(MDW_ADAPTER_CONNECTION_TIMEOUT, 20000));
            return connectTimeout;

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
        Integer readTimeout = null;
                try {
            readTimeout = getAttribute(READ_TIMEOUT, PropertyManager.getIntegerProperty(MDW_ADAPTER_READ_TIMEOUT, 10000));
            return readTimeout;
        }
        catch (PropertyException ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }
}
