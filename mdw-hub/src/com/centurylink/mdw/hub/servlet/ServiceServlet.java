/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.util.AuthUtils;

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
                    && !requestHeaderKeys.contains(key))
                response.setHeader(key, metaInfo.get(key));
        }

        // these always get populated if present
        if (metaInfo.get(Listener.METAINFO_REQUEST_ID) != null)
            response.setHeader(Listener.METAINFO_REQUEST_ID, metaInfo.get(Listener.METAINFO_REQUEST_ID));
        if (metaInfo.get(Listener.METAINFO_MDW_REQUEST_ID) != null)
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
        metaInfo.put(Listener.METAINFO_HTTP_METHOD, request.getMethod());
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

    protected void authenticate(HttpServletRequest request, Map<String,String> headers) throws ServiceException {
        headers.remove(Listener.AUTHENTICATED_USER_HEADER); // only we should populate this
        if (headers.containsKey(Listener.AUTHORIZATION_HEADER_NAME) || headers.containsKey(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase())) {
            // perform http basic auth
            if (!AuthUtils.authenticate(headers, AuthUtils.HTTP_BASIC_AUTHENTICATION)) {
                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
                throw new ServiceException(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failure");
            }
        }
        else {
            // check for user authenticated in session
            AuthenticatedUser user = (AuthenticatedUser)request.getSession().getAttribute("authenticatedUser");
            if (user != null)
                headers.put(Listener.AUTHENTICATED_USER_HEADER, user.getCuid());
        }

        if (ApplicationContext.isDevelopment() && headers.get(Listener.AUTHENTICATED_USER_HEADER) == null) {
            // auth failed or not provided but allow dev override
            if (ApplicationContext.getDevUser() != null) {
                headers.put(Listener.AUTHENTICATED_USER_HEADER, ApplicationContext.getDevUser());
                if (String.valueOf(HttpServletResponse.SC_UNAUTHORIZED).equals(headers.get(Listener.METAINFO_HTTP_STATUS_CODE)))
                    headers.remove(Listener.METAINFO_HTTP_STATUS_CODE);
            }
        }
        else if (ApplicationContext.isServiceApiOpen() && headers.get(Listener.AUTHENTICATED_USER_HEADER) == null) {
          // auth failed or not provided but allow service user override
          if (ApplicationContext.getServiceUser() != null) {
              headers.put(Listener.AUTHENTICATED_USER_HEADER, ApplicationContext.getServiceUser());
              if (String.valueOf(HttpServletResponse.SC_UNAUTHORIZED).equals(headers.get(Listener.METAINFO_HTTP_STATUS_CODE)))
                  headers.remove(Listener.METAINFO_HTTP_STATUS_CODE);
          }
        }
    }

    protected String createErrorResponseMessage(HttpServletRequest request,
            Map<String, String> metaInfo, ServiceException ex) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuffer requestBuffer = new StringBuffer(request.getContentLength());
        String line;
        while ((line = reader.readLine()) != null)
            requestBuffer.append(line).append('\n');
        return new ListenerHelper().createErrorResponse(requestBuffer.toString(), metaInfo, ex);
    }
}
