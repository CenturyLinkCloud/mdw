/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter.rest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

@Tracked(LogLevel.TRACE)
public class MultiRestServiceAdapter extends RestServiceAdapter {

    @Override
    public Object openConnection() {
       return null;
    }

    /**
     * Returns an HttpURLConnection based on the configured endpoint, which
     * includes the resource path. Override for HTTPS or other connection type.
     */
    public Object openConnection(String endpointUri) throws ConnectionException {
        try {
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
            URL url = new URL(endpointUri);
            return url.openConnection();
        }
        catch (Exception ex) {
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
    }

    protected List<Object> getConnectionObjs() throws ConnectionException, AdapterException {
        List<Object> connObjs = new ArrayList<Object>();
        for (String url : getEndpointUris())
            connObjs.add(openConnection(url));

        return connObjs;
    }


    /**
     * Returns the endpoint URLs for the RESTful service.  Supports property specifiers
     * via the syntax for {@link #AdapterActivityBase.getAttributeValueSmart(String)}.
     * @throws ActivityException
     */
    protected List<String> getEndpointUris() throws AdapterException {
        List<String> urlmap = new ArrayList<String>();
        try {
            String map = getAttributeValue(ENDPOINT_URI);
            List<String[]> urlmaparray;
            if (map==null)
                urlmaparray = new ArrayList<String[]>();
                else
                    urlmaparray = StringHelper.parseTable(map, ',', ';', 1);

            for (String[] entry : urlmaparray)
                urlmap.add(getValueSmart(entry[0], ""));
        }
        catch (Exception ex) {
            throw new AdapterException(-1, ex.getMessage(), ex);
        }
        return urlmap;
    }

    /**
     * Invokes the RESTful service by submitting an HTTP requests against the configured
     * endpoint URIs.  Override getRequestData() to provide the requestData value (usually a String).
     */
    @Override
    public String invoke(Object conn, String request, int timeout, Map<String, String> headers)
    throws ConnectionException, AdapterException {
        Exception savedException = null;
        int connectTimeout = -1;
        int readTimeout = -1;
        String httpMethod = null;
        StringBuffer sb = new StringBuffer();

        sb.append("<RESTResponseList>");

        for (Object connObj : getConnectionObjs()) {
            HttpHelper  httpHelper = null;
            try {
                sb.append("\r\n<RESTResponse uri=\"" + ((HttpURLConnection)connObj).getURL().toString() + "\">\r\n");

                httpHelper = getHttpHelper(connObj);

                connectTimeout = connectTimeout < 0 ? getConnectTimeout() : connectTimeout;
                if (connectTimeout > 0)
                    httpHelper.setConnectTimeout(connectTimeout);

                readTimeout = readTimeout < 0 ? getReadTimeout() : readTimeout;
                if (readTimeout > 0)
                    httpHelper.setReadTimeout(readTimeout);

                if (headers != null)
                    httpHelper.setHeaders(headers);

                if (httpMethod == null)
                    httpMethod = getHttpMethod();

                if (httpMethod.equals("GET"))
                    sb.append(httpHelper.get());
                else if (httpMethod.equals("POST"))
                    sb.append(httpHelper.post(request));
                else if (httpMethod.equals("PUT"))
                    sb.append(httpHelper.put(request));
                else if (httpMethod.equals("DELETE"))
                    sb.append(httpHelper.delete());
                else
                    throw new AdapterException("Unsupported HTTP Method: " + httpMethod);
            }
            catch (IOException ex) {
                if (httpHelper != null && httpHelper.getResponse() != null)
                    sb.append(httpHelper.getResponse());
                /**
                 * Plugs into automatic retrying
                 */
                logexception(ex.getMessage(), ex);
                savedException = ex;
            }
            catch (Exception ex) {
                int responseCode = -1;
                if (httpHelper != null)
                    responseCode = httpHelper.getResponseCode();
                throw new AdapterException(responseCode, ex.getMessage() , ex);
            }
            finally {
                sb.append("\r\n</RESTResponse>");
                if (httpHelper != null)
                    setResponseHeaders(httpHelper.getHeaders());
            }
        }
        sb.append("\r\n</RESTResponseList>");
        if (savedException != null) {
            logResponse(sb.toString());
            throw new ConnectionException(-1, savedException.getMessage(), savedException);
        }

        return sb.toString();
    }

}
