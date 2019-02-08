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
package com.centurylink.mdw.workflow.adapter.rest;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.adapter.HeaderAwareAdapter;
import com.centurylink.mdw.auth.AuthTokenProvider;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.event.AdapterStubRequest;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.ServicePaths;
import com.centurylink.mdw.util.HttpAltConnection;
import com.centurylink.mdw.util.HttpConnection;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.http.BasicAuthProvider;
import com.centurylink.mdw.workflow.adapter.http.HttpServiceAdapter;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Tracked(LogLevel.TRACE)
public class RestServiceAdapter extends HttpServiceAdapter implements HeaderAwareAdapter {

    public static final String HTTP_METHOD = "HttpMethod";
    public static final String ENDPOINT_URI = "EndpointUri";  // includes the resource path
    public static final String HEADERS_VARIABLE = "HeadersVariable";
    public static final String AUTH_PROVIDER = "AuthProvider";
    public static final String AUTH_APP_ID = "AuthAppId";
    public static final String AUTH_USER = "AuthUser";
    public static final String AUTH_PASSWORD = "AuthPassword";

    @Override
    public Object openConnection() throws ConnectionException {
        return openConnection(null, 0);
    }

    public Object openConnection(String proxyHost, int proxyPort) throws ConnectionException {
        return openConnection(proxyHost, proxyPort, null);
    }

    /**
     * Returns an HttpConnection based on the configured endpoint, which
     * includes the resource path. Override for HTTPS or other connection type.
     */
    public Object openConnection(String proxyHost, int proxyPort, Proxy.Type proxyType) throws ConnectionException {
        try {
            String endpointUri = getEndpointUri();
            if (endpointUri == null)
                throw new ConnectionException("Missing endpoint URI");
            Map<String,String> params = getRequestParameters();
            if (params != null && !params.isEmpty()) {
                StringBuffer query = new StringBuffer();
                query.append(endpointUri.indexOf('?') < 0 ? '?' : '&');
                int count = 0;
                for (String name : params.keySet()) {
                    if (count > 0)
                        query.append('&');
                    String encoding = getUrlEncoding();
                    query.append(encoding == null ? name : URLEncoder.encode(name, encoding)).append("=");
                    query.append(encoding == null ? params.get(name) : URLEncoder.encode(params.get(name), encoding));
                    count++;
                }
                endpointUri += query;
            }
            logdebug("REST adapter endpoint: " + endpointUri);
            URL url = new URL(endpointUri);
            HttpConnection httpConnection;
            if ("PATCH".equals(getHttpMethod()))
                httpConnection = new HttpAltConnection(url);
            else
                httpConnection = new HttpConnection(url);

            if (proxyHost != null)
                httpConnection.setProxyHost(proxyHost);
            if (proxyPort > 0)
                httpConnection.setProxyPort(proxyPort);
            if (proxyType != null)
                httpConnection.setProxyType(proxyType);

            httpConnection.open();
            return httpConnection;
        }
        catch (Exception ex) {
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
    }

    public HttpHelper getHttpHelper(Object connection) throws ActivityException {
        Object authProvider = getAuthProvider();
        if (authProvider instanceof BasicAuthProvider)
            return ((BasicAuthProvider)authProvider).getHttpHelper((HttpConnection)connection);
        else
            return new HttpHelper((HttpConnection)connection);
    }

    private Object authProvider;
    public Object getAuthProvider() throws ActivityException {
        if (authProvider == null) {
            String providerClass = getAttributeValueSmart(AUTH_PROVIDER);
            if (providerClass != null) {
                if (providerClass.equals(BasicAuthProvider.class.getName())) {
                    String user = getAttribute(AUTH_USER);
                    String password = getAttribute(AUTH_PASSWORD);
                    authProvider = new BasicAuthProvider(user, password);
                }
                else {
                    try {
                        Class<?> clazz = getPackage().getCloudClassLoader().loadClass(providerClass);
                        if (AuthTokenProvider.class.isAssignableFrom(clazz)) {
                            authProvider = clazz.newInstance();
                        }
                        else {
                            throw new ActivityException("Provider must implement AuthTokenProvider: " + clazz);
                        }
                    }
                    catch (ReflectiveOperationException ex) {
                        throw new ActivityException(ex.getMessage(), ex);
                    }
                }
            }
        }
        return authProvider;
    }

    /**
     * Override to specify URL request parameters.
     */
    protected Map<String,String> getRequestParameters() {
        return null;
    }

    protected String getUrlEncoding() {
        return "UTF-8";
    }

    /**
     * Returns the endpoint URL for the RESTful service.  Supports property specifiers
     * via the syntax for {@link #getAttributeValueSmart(String)}.
     */
    protected String getEndpointUri() throws PropertyException {
        return getAttributeValueSmart(ENDPOINT_URI);
    }

    /**
     * Invokes the RESTful service by submitting an HTTP request against the configured
     * endpoint URI.  Override getRequestData() to provide the requestData value (usually a String).
     */
    @Override
    public String invoke(Object inConn, String request, int timeout, Map<String, String> headers)
    throws ConnectionException, AdapterException {
        HttpHelper httpHelper = null;
        Object conn = inConn;
        try {
            httpHelper = getHttpHelper(conn);

            if (headers != null)
                httpHelper.setHeaders(headers);

            String response = performHttpAction(httpHelper, request);

            // Check if received a 401 if using an AuthTokenProvider, refresh token in case it expired and try again
            if (response != null && getAuthProvider() instanceof AuthTokenProvider && super.getResponse(conn, response).getStatusCode() == Status.UNAUTHORIZED.getCode()) {
                String user = getAttribute(AUTH_USER);
                String password = getAttribute(AUTH_PASSWORD);
                URL endpoint = new URL(getEndpointUri());
                AuthTokenProvider authProvider = (AuthTokenProvider)getAuthProvider();
                authProvider.invalidateToken(endpoint, user);
                String token = new String(authProvider.getToken(endpoint, user, password));
                headers.put("Authorization", "Bearer " + token);
                conn = openConnection();
                httpHelper = getHttpHelper(conn);
                httpHelper.setHeaders(headers);
                response = performHttpAction(httpHelper, request);
            }

            if (response != null) {
                // HTTP code threshold for retries
                int codeThreshold = DEFAULT_RETRY_HTTP_CODE;
                String retryCodes = getAttributeValueSmart(RETRY_HTTP_CODES);
                if (retryCodes != null) {
                    try {
                        codeThreshold = Integer.parseInt(retryCodes);
                    }
                    catch (NumberFormatException ex) {} // Use default in this case
                }
                // HTTP code threshold for error
                int errorCodeThreshold = DEFAULT_ERROR_HTTP_CODE;
                String errorCodes = getAttributeValueSmart(ERROR_HTTP_CODES);
                if (errorCodes != null) {
                    try {
                        errorCodeThreshold = Integer.parseInt(errorCodes);
                    }
                    catch (NumberFormatException ex) {} // Use default in this case
                }
                Response httpResponse = super.getResponse(conn, response);
                if (httpResponse.getStatusCode() >= codeThreshold)
                    throw new IOException("Server returned HTTP response code: " + httpResponse.getStatusCode());  // Retryable
                else if (httpResponse.getStatusCode() >= errorCodeThreshold)
                    throw new AdapterException("Server returned HTTP response code: " + httpResponse.getStatusCode());   // Non-Retryable
            }
            return response;
        }
        catch (IOException ex) {
            if (httpHelper != null && httpHelper.getResponse() != null) {
                Response response = new Response(httpHelper.getResponse());
                response.setStatusCode(httpHelper.getResponseCode());
                response.setStatusMessage(httpHelper.getResponseMessage());
                response.setPath(ServicePaths.getOutboundResponsePath(String.valueOf(httpHelper.getConnection().getUrl()),
                        httpHelper.getConnection().getMethod()));
                logResponse(response);
            }
            /**
             * Plugs into automatic retrying
             */
            logexception(ex.getMessage(), ex);
            throw new ConnectionException(-1, ex.getMessage(), ex);
        }
        catch (Exception ex) {
            int responseCode = -1;
            if (httpHelper != null) {
                responseCode = httpHelper.getResponseCode();
                if (httpHelper.getResponse() != null) {
                    Response response = new Response(httpHelper.getResponse());
                    response.setStatusCode(responseCode);
                    response.setStatusMessage(httpHelper.getResponseMessage());
                    response.setPath(ServicePaths.getOutboundResponsePath(String.valueOf(httpHelper.getConnection().getUrl()),
                            httpHelper.getConnection().getMethod()));
                    logResponse(response);
                }
            }
            throw new AdapterException(responseCode, ex.getMessage() , ex);
        }
        finally {
            if (httpHelper != null) {
                setResponseHeaders(httpHelper.getHeaders());
                // We have to override the response from original connection if token was expired
                if (!httpHelper.getConnection().equals(inConn))
                    ((HttpConnection)inConn).setResponse(httpHelper.getConnection().getResponse());
            }
        }
    }

    protected String performHttpAction(HttpHelper httpHelper, String request) throws ActivityException, IOException, AdapterException {
        int connectTimeout = getConnectTimeout();
        if (connectTimeout > 0)
            httpHelper.setConnectTimeout(connectTimeout);

        int readTimeout = getReadTimeout();
        if (readTimeout > 0)
            httpHelper.setReadTimeout(readTimeout);

        String httpMethod = getHttpMethod();
        String response = null;
        if (httpMethod.equals("GET"))
            response = httpHelper.get();
        else if (httpMethod.equals("POST"))
            response = httpHelper.post(request);
        else if (httpMethod.equals("PUT"))
            response = httpHelper.put(request);
        else if (httpMethod.equals("DELETE"))
            response = httpHelper.delete();
        else if (httpMethod.equals("PATCH"))
            response = httpHelper.patch(request);
        else
            throw new AdapterException("Unsupported HTTP Method: " + httpMethod);

        return response;
    }

    /**
     * The method overrides the one from the super class and does the following:
     * <ul>
     *   <li>For HTTP GET and DELETE requests, it returns an empty string</li>
     *   <li>Otherwise it gets the value of the variable with the name specified in the
     *      attribute REQUEST_VARIABLE. The value is typically an XML document or a string</li>
     *   <li>It invokes the variable translator to convert the value into a string
     *      and then returns the string value.</li>
     * </ul>
     * For HTTP methods other than GET and DELETE this will throw an exception if the
     * request data variable is not bound, or the value is not a DocumentReference or String.
     */
    @Override
    protected String getRequestData() throws ActivityException {
        String httpMethod = getHttpMethod();
        if (httpMethod.equals("GET") || httpMethod.equals("DELETE"))
            return "";

        String request = super.getRequestData();
        if (request == null)
            throw new ActivityException("Request data attribute is missing for HTTP method: " + httpMethod);

        return request;
    }

    protected String getHttpMethod() throws ActivityException {
        try {
            String httpMethod = getAttributeValueSmart(HTTP_METHOD);
            if (httpMethod == null)
                throw new ActivityException("RESTful adapter required attribute missing: " + HTTP_METHOD);
            return httpMethod;
        }
        catch (PropertyException ex) {
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    /**
     * Override to specify HTTP request headers.
     */
    public Map<String,String> getRequestHeaders() {

        // If we already set the headers when logging request metadata, use them
        if (super.getRequestHeaders() != null)
            return super.getRequestHeaders();

        try {
            Map<String,String> headers = null;
            String headersVar = getAttributeValueSmart(HEADERS_VARIABLE);
            if (headersVar != null) {
                Process processVO = getProcessDefinition();
                Variable variableVO = processVO.getVariable(headersVar);
                if (variableVO == null)
                    throw new ActivityException("Headers variable '" + headersVar + "' is not defined for process " + processVO.getLabel());
                if (!variableVO.getType().startsWith("java.util.Map"))
                    throw new ActivityException("Headers variable '" + headersVar + "' must be of type java.util.Map");
                Object headersObj = getVariableValue(headersVar);
                if (headersObj != null) {
                    headers = new HashMap<>();
                    for (Object key : ((Map<?,?>)headersObj).keySet()) {
                        headers.put(key.toString(), ((Map<?,?>)headersObj).get(key).toString());
                    }
                }
            }
            Object authProvider = getAuthProvider();
            if (authProvider instanceof AuthTokenProvider) {
                if (headers == null)
                    headers = new HashMap<>();
                URL endpoint = new URL(getEndpointUri());
                String user = getAttribute(AUTH_USER);
                String password = getAttribute(AUTH_PASSWORD);
                String appId = getAttribute(AUTH_APP_ID);
                if (appId != null && appId.length() > 0) {
                    Map<String,String> options = new HashMap<>();
                    options.put("appId", appId);
                    ((AuthTokenProvider) authProvider).setOptions(options);
                }
                String token = new String(((AuthTokenProvider)authProvider).getToken(endpoint, user, password));
                headers.put("Authorization", "Bearer " + token);
            }
            // Set the headers so that we don't try and set them next time this method gets called
            super.setRequestHeaders(headers);
            return headers;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    public Map<String,String> getResponseHeaders() { return super.getResponseHeaders(); }
    protected void setResponseHeaders(Map<String,String> headers) { super.setResponseHeaders(headers); }

    @Override
    protected AdapterStubRequest getStubRequest(String requestContent) throws AdapterException {
        AdapterStubRequest stubRequest = super.getStubRequest(requestContent);
        try {
            stubRequest.setUrl(getEndpointUri());
            stubRequest.setMethod(getHttpMethod());
            stubRequest.setHeaders(getRequestHeaders());
        }
        catch (Exception ex) {
            throw new AdapterException(500, ex.getMessage(), ex, false);
        }
        return stubRequest;
    }

    @Override
    public Object invoke(Object pConnection, Object requestData, Map<String, String> requestHeaders)
    throws AdapterException, ConnectionException {
        return invoke(pConnection,(String)requestData, 0, requestHeaders );
    }

    /**
     * Specific stacktrace error message for RestServiceAdapter to include the endpoint
     */
    @Override
    public String getAdapterInvocationErrorMessage() {

        try {
            return "Adapter Invocation Exception - endpoint = "+getEndpointUri()+ " : Adapter Interface = "+getClass().getName();
        }
        catch (PropertyException e) {
            return "Adapter Invocation Exception";
        }
    }

    @Override
    protected JSONObject getRequestMeta() throws Exception {
        JSONObject meta = super.getRequestMeta();
        meta.put(Listener.METAINFO_HTTP_METHOD, getHttpMethod());
        meta.put(Listener.METAINFO_REQUEST_URL, getEndpointUri());

        return meta;
    }

    @Override
    protected JSONObject getResponseMeta() throws Exception {
        JSONObject meta = super.getResponseMeta();
        meta.put(Listener.METAINFO_HTTP_METHOD, getHttpMethod());
        meta.put(Listener.METAINFO_REQUEST_URL, getEndpointUri());

        return meta;
    }

    protected Long logRequest(Request request) {
        request.setPath(getEndpointUri());
        return super.logRequest(request);
    }
}
