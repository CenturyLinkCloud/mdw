/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.AuthUtils;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.Resource;
import com.centurylink.mdw.service.ResourceRequestDocument;
import com.centurylink.mdw.service.ResourceRequestDocument.ResourceRequest;

public class RestServlet extends HttpServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestfulServicesServlet.doGet()", true);

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("RESTful Service GET Request:\n" + request.getRequestURI() + (request.getQueryString() == null ? "" : ("?" + request.getQueryString())));
        }

        if (request.getPathInfo() == null) {
            // redirect to html documentation
            response.sendRedirect("/" + ApplicationContext.getMdwHubContextRoot() + "/doc/webServices.html");
            return;
        }
        if (request.getPathInfo().startsWith("/SOAP")) {
            // forward to SOAP servlet
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(request.getPathInfo());
            requestDispatcher.forward(request, response);
            return;
        }

        ResourceRequestDocument resourceRequestDocument = ResourceRequestDocument.Factory.newInstance();
        ResourceRequest resourceRequest = resourceRequestDocument.addNewResourceRequest();
        Resource resource = resourceRequest.addNewResource();
        String path = request.getPathInfo().substring(1);
        int slash = path.indexOf('/');
        if (slash > 0)
            resource.setName(path.substring(0, slash));
        else
            resource.setName(path);
        for (Iterator<?> keysIter = request.getParameterMap().keySet().iterator(); keysIter.hasNext(); ) {
            String key = (String) keysIter.next();
            String value = request.getParameter(key);
            if (!Listener.AUTHENTICATED_USER_HEADER.equals(key)) { // do not allow this to be injected
              Parameter parameter = resource.addNewParameter();
              parameter.setName(key);
              parameter.setStringValue(value);
            }
        }
        String requestString = resourceRequestDocument.xmlText(getXmlOptions());
        ListenerHelper helper = new ListenerHelper();
        Map<String,String> metaInfo = buildMetaInfo(request);
        metaInfo.put(Listener.METAINFO_REQUEST_CATEGORY, Listener.REQUEST_CATEGORY_READ);

        String responseString = handleAuthentication(request.getSession(), requestString, metaInfo, helper);
        if (responseString == null) // valid or no auth requested
            responseString = helper.processEvent(requestString, metaInfo);

        if (metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE) != null)
            response.setStatus(Integer.parseInt(metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE)));

        String downloadFormat = metaInfo.get(Listener.METAINFO_DOWNLOAD_FORMAT);
        if (downloadFormat != null) {
            response.setContentType(Listener.CONTENT_TYPE_DOWNLOAD);
            String fileName = path.substring(0).replace('/', '-') + "." + downloadFormat;
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
            if (downloadFormat.equals(Listener.DOWNLOAD_FORMAT_JSON) || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_XML) || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_TEXT)) {
                response.getOutputStream().write(responseString.getBytes());
            }
            else {
                // for binary content string response will have been Base64 encoded
                response.getOutputStream().write(Base64.decodeBase64(responseString.getBytes()));
            }
        }
        else {
            if (logger.isMdwDebugEnabled()) {
                logger.mdwDebug("RESTful Service GET Response:\n" + responseString);
            }
            if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
                response.setContentType("text/xml");
            else
                response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

            if ("System/sysInfo".equals(path) && "application/json".equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE))) {
                responseString = WebAppContext.addContextInfo(responseString, request);
            }

            response.getOutputStream().print(responseString);
        }

        timer.stopAndLogTiming("");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestfulServicesServlet.doPost()", true);

        if (request.getPathInfo() != null && request.getPathInfo().startsWith("/SOAP")) {
            // forward to SOAP servlet
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(request.getPathInfo());
            requestDispatcher.forward(request, response);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuffer requestBuffer = new StringBuffer(request.getContentLength());
        String line = null;
        while ((line = reader.readLine()) != null) {
            requestBuffer.append(line).append('\n');
        }

        // read the POST request contents
        String requestString = requestBuffer.toString();
        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("REST Service POST Request:\n" + requestString);
        }

        ListenerHelper helper = new ListenerHelper();
        Map<String,String> metaInfo = buildMetaInfo(request);
        String responseString = handleAuthentication(request.getSession(), requestString, metaInfo, helper);
        if (responseString == null) // valid or no auth requested
            responseString = helper.processEvent(requestString, metaInfo);

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("REST Service POST Response:\n" + responseString);
        }

        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
            response.setContentType("text/xml");
        else
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

        if (metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE) != null)
            response.setStatus(Integer.parseInt(metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE)));

        response.getOutputStream().print(responseString);
        timer.stopAndLogTiming("");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestfulServicesServlet.doPut()", true);

        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuffer requestBuffer = new StringBuffer(request.getContentLength());
        String line = null;
        while ((line = reader.readLine()) != null) {
            requestBuffer.append(line).append('\n');
        }

        // read the PUT request contents
        String requestString = requestBuffer.toString();
        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("RESTful Service PUT Request:\n" + requestString);
        }

        ListenerHelper helper = new ListenerHelper();
        Map<String,String> metaInfo = buildMetaInfo(request);
        metaInfo.put(Listener.METAINFO_REQUEST_CATEGORY, Listener.REQUEST_CATEGORY_UPDATE);

        String responseString = handleAuthentication(request.getSession(), requestString, metaInfo, helper);
        if (responseString == null) // valid or no auth requested
            responseString = helper.processEvent(requestString, metaInfo);

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("RESTful Service PUT Response:\n" + responseString);
        }

        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
            response.setContentType("text/xml");
        else
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

        if (metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE) != null)
          response.setStatus(Integer.parseInt(metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE)));

        response.getOutputStream().print(responseString);
        timer.stopAndLogTiming("");
    }


    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestfulServicesServlet.doDelete()", true);

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("RESTful Service DELETE Request:\n" + request.getRequestURI());
         }

        if (request.getPathInfo() == null)
            return;

        ListenerHelper helper = new ListenerHelper();
        Map<String,String> metaInfo = buildMetaInfo(request);
        metaInfo.put(Listener.METAINFO_REQUEST_CATEGORY, Listener.REQUEST_CATEGORY_DELETE);

        String responseString = handleAuthentication(request.getSession(), "{}", metaInfo, helper);
        if (responseString == null) // valid or no auth requested
            responseString = helper.processEvent("{}", metaInfo);
        if (logger.isMdwDebugEnabled()) {
           logger.mdwDebug("RESTful Service DELETE Response:\n" + responseString);
        }

        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
            response.setContentType("text/xml");
        else
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

        if (metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE) != null)
          response.setStatus(Integer.parseInt(metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE)));

        response.getOutputStream().print(responseString);
        timer.stopAndLogTiming("");
    }

    private Map<String,String> buildMetaInfo(HttpServletRequest request) {
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
        if (request.getMethod().equalsIgnoreCase("GET")) {
            Enumeration<?> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String key = (String) paramNames.nextElement();
                if (!Listener.AUTHENTICATED_USER_HEADER.equals(key)) // do not allow this to be injected
                    metaInfo.put(key, request.getParameter(key).toString());
            }
        }
        if (request.getQueryString() != null)
            metaInfo.put(Listener.METAINFO_REQUEST_QUERY_STRING, request.getQueryString());
        Enumeration<?> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            if (!Listener.AUTHENTICATED_USER_HEADER.equals(headerName)) // do not allow this to be injected
                metaInfo.put(headerName, request.getHeader(headerName));
        }
        return metaInfo;
    }

    private XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

    private String handleAuthentication(HttpSession session, String request, Map<String,String> headers, ListenerHelper helper) {
        String error = null;
        headers.remove(Listener.AUTHENTICATED_USER_HEADER); // only we should populate this
        if (headers.containsKey(Listener.AUTHORIZATION_HEADER_NAME) || headers.containsKey(Listener.AUTHORIZATION_HEADER_NAME.toLowerCase())) {
            // perform http basic auth
            if (!AuthUtils.authenticate(headers, AuthUtils.HTTP_BASIC_AUTHENTICATION)) {
                error = helper.createAuthenticationErrorResponse(request, headers);
                headers.put(Listener.METAINFO_HTTP_STATUS_CODE, String.valueOf(HttpServletResponse.SC_UNAUTHORIZED));
            }
        }
        else {
            // check for user authenticated in session
            AuthenticatedUser user = (AuthenticatedUser)session.getAttribute("authenticatedUser");
            if (user != null)
                headers.put(Listener.AUTHENTICATED_USER_HEADER, user.getCuid());
        }

        if (ApplicationContext.isDevelopment() && headers.get(Listener.AUTHENTICATED_USER_HEADER) == null) {
            // auth failed or not provided but allow dev override
            if (ApplicationContext.getDevUser() != null) {
                headers.put(Listener.AUTHENTICATED_USER_HEADER, ApplicationContext.getDevUser());
                if (String.valueOf(HttpServletResponse.SC_UNAUTHORIZED).equals(headers.get(Listener.METAINFO_HTTP_STATUS_CODE)))
                    headers.remove(Listener.METAINFO_HTTP_STATUS_CODE);
                error = null;
            }
        }
        else if (ApplicationContext.isServiceApiOpen() && headers.get(Listener.AUTHENTICATED_USER_HEADER) == null) {
          // auth failed or not provided but allow service user override
          if (ApplicationContext.getServiceUser() != null) {
              headers.put(Listener.AUTHENTICATED_USER_HEADER, ApplicationContext.getServiceUser());
              if (String.valueOf(HttpServletResponse.SC_UNAUTHORIZED).equals(headers.get(Listener.METAINFO_HTTP_STATUS_CODE)))
                  headers.remove(Listener.METAINFO_HTTP_STATUS_CODE);
              error = null;
          }
        }
        return error;
    }


}
