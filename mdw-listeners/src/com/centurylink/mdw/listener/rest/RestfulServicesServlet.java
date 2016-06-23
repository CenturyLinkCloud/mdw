/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.rest;

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

import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.Resource;
import com.centurylink.mdw.service.ResourceRequestDocument;
import com.centurylink.mdw.service.ResourceRequestDocument.ResourceRequest;

public class RestfulServicesServlet  extends HttpServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestfulServicesServlet.doGet()", true);

        if (logger.isDebugEnabled()) {
            logger.debug("RESTful Service GET Request:\n" + request.getRequestURI() + (request.getQueryString() == null ? "" : ("?" + request.getQueryString())));
        }

        if (request.getPathInfo() == null) {
            // forward to html documentation
            RequestDispatcher requestDispatcher = request.getRequestDispatcher("/doc/webServices.html");
            requestDispatcher.forward(request, response);
            return;
        }

        ResourceRequestDocument resourceRequestDocument = ResourceRequestDocument.Factory.newInstance();
        ResourceRequest resourceRequest = resourceRequestDocument.addNewResourceRequest();
        Resource resource = resourceRequest.addNewResource();
        resource.setName(request.getPathInfo().substring(1));
        for (Iterator<?> keysIter = request.getParameterMap().keySet().iterator(); keysIter.hasNext(); ) {
            String key = (String) keysIter.next();
            String value = request.getParameter(key);
            Parameter parameter = resource.addNewParameter();
            parameter.setName(key);
            parameter.setStringValue(value);
        }
        String requestString = resourceRequestDocument.xmlText(getXmlOptions());
        ListenerHelper helper = new ListenerHelper();
        Map<String,String> metaInfo = buildMetaInfo(request);
        metaInfo.put(Listener.METAINFO_REQUEST_CATEGORY, Listener.REQUEST_CATEGORY_READ);
        String responseString = helper.processEvent(requestString, metaInfo);
        if (logger.isMdwDebugEnabled()) {
           logger.mdwDebug("RESTful Service GET Response:\n" + responseString);
        }

        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
          response.setContentType("text/xml");
        else
          response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

        response.getOutputStream().print(responseString);
        timer.stopAndLogTiming("");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestfulServicesServlet.doPost()", true);

        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuffer requestBuffer = new StringBuffer(request.getContentLength());
        String line = null;
        while ((line = reader.readLine()) != null) {
            requestBuffer.append(line).append('\n');
        }

        // read the POST request contents
        String requestString = requestBuffer.toString();
        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("RESTful Service POST Request:\n" + requestString);
        }

        ListenerHelper helper = new ListenerHelper();
        Map<String,String> metaInfo = buildMetaInfo(request);
        metaInfo.put(Listener.METAINFO_REQUEST_CATEGORY, Listener.REQUEST_CATEGORY_CREATE);
        String responseString = helper.processEvent(requestString, metaInfo);
        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("RESTful Service POST Response:\n" + responseString);
        }

        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
            response.setContentType("text/xml");
        else
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

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
        String responseString = helper.processEvent(requestString, metaInfo);
        if (logger.isMdwDebugEnabled()) {
           logger.mdwDebug("RESTful Service PUT Response:\n" + responseString);
        }

        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
            response.setContentType("text/xml");
        else
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

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

        ResourceRequestDocument resourceRequestDocument = ResourceRequestDocument.Factory.newInstance();
        ResourceRequest resourceRequest = resourceRequestDocument.addNewResourceRequest();
        Resource resource = resourceRequest.addNewResource();
        resource.setName(request.getPathInfo().substring(1));
        for (Iterator<?> keysIter = request.getParameterMap().keySet().iterator(); keysIter.hasNext(); ) {
            String key = (String) keysIter.next();
            String value = request.getParameter(key);
            Parameter parameter = resource.addNewParameter();
            parameter.setName(key);
            parameter.setStringValue(value);
        }
        String requestString = resourceRequestDocument.xmlText(getXmlOptions());
        ListenerHelper helper = new ListenerHelper();
        Map<String,String> metaInfo = buildMetaInfo(request);
        metaInfo.put(Listener.METAINFO_REQUEST_CATEGORY, Listener.REQUEST_CATEGORY_DELETE);
        String responseString = helper.processEvent(requestString, metaInfo);
        if (logger.isMdwDebugEnabled()) {
           logger.mdwDebug("RESTful Service DELETE Response:\n" + responseString);
        }

        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
            response.setContentType("text/xml");
        else
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

        response.getOutputStream().print(responseString);
        timer.stopAndLogTiming("");
    }

    private Map<String,String> buildMetaInfo(HttpServletRequest request)
    {
        Map<String,String> metaInfo = new HashMap<String,String>();
        metaInfo.put(Listener.METAINFO_PROTOCOL, Listener.METAINFO_PROTOCOL_RESTFUL_WEBSERVICE);
        metaInfo.put(Listener.METAINFO_SERVICE_CLASS, this.getClass().getName());
        metaInfo.put("RequestURL", request.getRequestURL().toString());
        metaInfo.put("HttpMethod", request.getMethod());
        if (request.getPathInfo() != null)
            metaInfo.put("Resource", request.getPathInfo().substring(1));
        if (request.getMethod().equalsIgnoreCase("GET")) {
            Enumeration<?> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String key = (String) paramNames.nextElement();
                metaInfo.put(key, request.getParameter(key).toString());
            }
        }
        Enumeration<?> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            metaInfo.put(headerName, request.getHeader(headerName));
        }
        return metaInfo;
    }

    private XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

}
