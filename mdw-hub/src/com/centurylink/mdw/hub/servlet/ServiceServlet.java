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
package com.centurylink.mdw.hub.servlet;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.util.AuthUtils;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Base class for HTTP protocol service handling.
 */
public abstract class ServiceServlet extends HttpServlet {

    /**
     * Populates response headers with values added by event handling.
     */
    protected void populateResponseHeaders(Set<String> requestHeaderKeys, Map<String,String> metaInfo, HttpServletResponse response) {
        for (String key : metaInfo.keySet()) {
            if (!Listener.AUTHENTICATED_USER_HEADER.equals(key)
                    && !Listener.METAINFO_HTTP_STATUS_CODE.equals(key)
                    && !Listener.METAINFO_ACCEPT.equals(key)
                    && !Listener.METAINFO_CONTENT_TYPE.equals(key)
                    && !Listener.METAINFO_DOWNLOAD_FORMAT.equals(key)
                    && !Listener.METAINFO_MDW_REQUEST_ID.equals(key)
                    && !requestHeaderKeys.contains(key))
                response.setHeader(key, metaInfo.get(key));
        }

        // these always get populated if present
        if (metaInfo.get(Listener.METAINFO_REQUEST_ID) != null)
            response.setHeader(Listener.METAINFO_REQUEST_ID, metaInfo.get(Listener.METAINFO_REQUEST_ID));
        if (metaInfo.get(Listener.METAINFO_MDW_REQUEST_ID) != null && !metaInfo.get(Listener.METAINFO_MDW_REQUEST_ID).equals("0"))
            response.setHeader(Listener.METAINFO_MDW_REQUEST_ID, metaInfo.get(Listener.METAINFO_MDW_REQUEST_ID));
        if (metaInfo.get(Listener.METAINFO_CORRELATION_ID) != null)
            response.setHeader(Listener.METAINFO_CORRELATION_ID, metaInfo.get(Listener.METAINFO_CORRELATION_ID));
        if (metaInfo.get(Listener.METAINFO_DOCUMENT_ID) != null)
            response.setHeader(Listener.METAINFO_DOCUMENT_ID, metaInfo.get(Listener.METAINFO_DOCUMENT_ID));
    }

    protected Map<String,String> buildMetaInfo(HttpServletRequest request) {
        Map<String,String> metaInfo = new HashMap<String,String>();
        metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_REST);
        metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
        metaInfo.put(Listener.METAINFO_REQUEST_URL, request.getRequestURL().toString());
        String method = request.getMethod();
        String overrideMethod = request.getHeader("X-HTTP-Method-Override");
        if (overrideMethod != null)
            method = overrideMethod.toUpperCase();
        metaInfo.put(Listener.METAINFO_HTTP_METHOD, method);
        metaInfo.put(Listener.METAINFO_REMOTE_HOST, request.getRemoteHost());
        metaInfo.put(Listener.METAINFO_REMOTE_ADDR, request.getRemoteAddr());
        metaInfo.put(Listener.METAINFO_REMOTE_PORT, String.valueOf(request.getRemotePort()));

        if (request.getPathInfo() != null) {
            String requestPath = request.getPathInfo().substring(1);
            if (requestPath.startsWith("REST/"))
                requestPath = requestPath.substring(5);
            metaInfo.put(Listener.METAINFO_REQUEST_PATH, requestPath);
        }

        if (request.getQueryString() != null)
            metaInfo.put(Listener.METAINFO_REQUEST_QUERY_STRING, request.getQueryString());

        // default to incoming content-type (but overridden by accept header below)
        String contentType = request.getContentType();
        // only set content type for known types
        if (Listener.CONTENT_TYPE_JSON.equals(contentType) || Listener.CONTENT_TYPE_XML.equals(contentType)
                || Listener.CONTENT_TYPE_TEXT.equals(contentType)) {
            metaInfo.put(Listener.METAINFO_CONTENT_TYPE, contentType);
        }

        Enumeration<?> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            if (!Listener.AUTHENTICATED_USER_HEADER.equals(headerName)) // do not allow this to be injected
                metaInfo.put(headerName, request.getHeader(headerName));
            if (Listener.METAINFO_ACCEPT.equalsIgnoreCase(headerName)) {
                String accept = request.getHeader(Listener.METAINFO_ACCEPT);
                if (Listener.CONTENT_TYPE_JSON.equals(accept) || Listener.CONTENT_TYPE_XML.equals(accept)
                        || Listener.CONTENT_TYPE_TEXT.equals(accept)) {
                    contentType = request.getHeader(headerName);
                }
            }
        }

        // getting parameters for POST consumes request reader
        if (!"POST".equalsIgnoreCase(request.getMethod())
                && !"PUT".equalsIgnoreCase(request.getMethod())
                && !"PATCH".equalsIgnoreCase(request.getMethod())) {
            Enumeration<?> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String key = (String) paramNames.nextElement();
                if (!Listener.AUTHENTICATED_USER_HEADER.equals(key)) // do not allow this to be injected
                    metaInfo.put(key, request.getParameter(key).toString());
            }
        }

        return metaInfo;
    }

    protected void authenticate(HttpServletRequest request, Map<String,String> headers, String payload) throws ServiceException {
        headers.remove(Listener.AUTHENTICATED_USER_HEADER); // only we should populate this
        if (headers.containsKey(Listener.AUTHORIZATION_HEADER_NAME) || headers.containsKey(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase())) {
            // perform http basic auth, which populates the auth user header
            AuthUtils.authenticate(AuthUtils.HTTP_BASIC_AUTHENTICATION, headers);
        }
        else {
            // check for user authenticated in session
            AuthenticatedUser user = (AuthenticatedUser)request.getSession().getAttribute("authenticatedUser");
            if (user != null)
                headers.put(Listener.AUTHENTICATED_USER_HEADER, user.getCuid());
        }

        if (headers.get(Listener.AUTHENTICATED_USER_HEADER) == null) {
            if (ApplicationContext.isDevelopment()) {
                // auth failed or not provided but allow dev override
                if (ApplicationContext.getDevUser() != null) {
                    headers.put(Listener.AUTHENTICATED_USER_HEADER, ApplicationContext.getDevUser());
                    if (String.valueOf(HttpServletResponse.SC_UNAUTHORIZED).equals(headers.get(Listener.METAINFO_HTTP_STATUS_CODE)))
                        headers.remove(Listener.METAINFO_HTTP_STATUS_CODE);
                }
            }
            else if (ApplicationContext.isServiceApiOpen()) {
              // auth failed or not provided but allow service user override
              if (ApplicationContext.getServiceUser() != null) {
                  headers.put(Listener.AUTHENTICATED_USER_HEADER, ApplicationContext.getServiceUser());
                  if (String.valueOf(HttpServletResponse.SC_UNAUTHORIZED).equals(headers.get(Listener.METAINFO_HTTP_STATUS_CODE)))
                      headers.remove(Listener.METAINFO_HTTP_STATUS_CODE);
              }
            }
            else {
                if ("GET".equalsIgnoreCase(request.getMethod())) {
                    // allow GET access to app summary and assets (for discovery)
                    // TODO more general approach (also "/Services/exit)
                    String[] allowed = new String[] { "/Services/AppSummary",
                            "/Services/GetAppSummary", "/services/AppSummary",
                            "/Services/System/sysInfo", "/services/Assets" };
                    for (String allow : allowed) {
                        if (request.getRequestURI().equals("/" + ApplicationContext.getMdwHubContextRoot() + allow))
                            return;
                    }
                }
                else if (request.getRequestURI().startsWith("/" + ApplicationContext.getMdwHubContextRoot() + "/services/com/centurylink/mdw/slack")) {
                    // validates Slack token unless request is coming from our AppFog prod instance
                    StandardLogger logger = LoggerUtil.getStandardLogger();
                    if (logger.isMdwDebugEnabled()) {
                            logger.mdwDebug("Slack request\nRequest remote host: " + request.getRemoteHost()); //+ "\nINET remote host: " + InetAddress.getByName(request.getRemoteHost()).getHostName());
                            for (String key : headers.keySet())
                                logger.mdwDebug(key + ": " + headers.get(key) + "\n");
                    }

                    if (AuthUtils.authenticate(AuthUtils.SLACK_TOKEN, headers, payload))
                        return;
                    else {
                        List<InetAddress> appFogProd = null;
                        InetAddress remote = null;
                        try {
                            remote = InetAddress.getByName(request.getRemoteHost());
                            appFogProd = Arrays.asList(InetAddress.getAllByName("mdw.useast.appfog.ctl.io"));
                        }
                        catch (UnknownHostException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if (appFogProd != null && appFogProd.contains(remote)) {
                            headers.put(Listener.AUTHENTICATED_USER_HEADER, "mdwapp");
                            return;
                        }
                    }
                }
                else if (headers.containsKey(Listener.X_HUB_SIGNATURE) || headers.containsKey(Listener.X_HUB_SIGNATURE.toLowerCase())) {
                    // perform http GitHub auth, which populates the auth user header
                    if (AuthUtils.authenticate(AuthUtils.GIT_HUB_SECRET_KEY, headers, payload))
                        return;
                }
                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
                throw new ServiceException(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failure");
            }
        }
    }

    protected String createErrorResponseMessage(HttpServletRequest request,
            Map<String,String> metaInfo, ServiceException ex) throws IOException {
        return new Status(ex.getCode(), ex.getMessage()).getJson().toString(2);
    }

    /**
     * We cannot rely on request.getRemoteAddr().
     * Caller must use the loopback host.
     */
    protected boolean isFromLocalhost(HttpServletRequest request) throws MalformedURLException {
        String host = new URL(request.getRequestURL().toString()).getHost();
        return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("0:0:0:0:0:0:0:1");
    }
}
