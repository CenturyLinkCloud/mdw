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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

@WebServlet(urlPatterns={"/services/*", "/Services/*", "/REST/*"}, loadOnStartup=1)
public class RestServlet extends ServiceServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doGet()", true);

        if (request.getPathInfo() == null) {
            // redirect to html documentation
            response.sendRedirect("/" + ApplicationContext.getMdwHubContextRoot() + "/doc/webServices.html");
            return;
        }
        else if (request.getPathInfo().startsWith("/SOAP")) {
            // forward to SOAP servlet
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(request.getPathInfo());
            requestDispatcher.forward(request, response);
            return;
        }
        else if (ApplicationContext.isDevelopment() && isFromLocalhost(request)) {
            // this is only allowed from localhost and in dev
            if ("/System/exit".equals(request.getPathInfo())) {
                response.setStatus(200);
                new Thread(new Runnable() {
                    public void run() {
                        System.exit(0);
                    }
                }).start();
                return;
            }
        }

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);

            String downloadFormat = metaInfo.get(Listener.METAINFO_DOWNLOAD_FORMAT);
            if (downloadFormat != null) {
                response.setContentType(Listener.CONTENT_TYPE_DOWNLOAD);
                String fileName = request.getPathInfo().substring(1).replace('/', '-') + "." + downloadFormat;
                response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
                if (downloadFormat.equals(Listener.DOWNLOAD_FORMAT_JSON)
                        || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_XML)
                        || downloadFormat.equals(Listener.DOWNLOAD_FORMAT_TEXT)) {
                    response.getOutputStream().write(responseString.getBytes());
                }
                else {
                    // for binary content string response will have been Base64
                    // encoded
                    response.getOutputStream()
                            .write(Base64.decodeBase64(responseString.getBytes()));
                }
            }
            else {
                if ("/System/sysInfo".equals(request.getPathInfo())
                        && "application/json".equals(metaInfo.get(Listener.METAINFO_CONTENT_TYPE))) {
                    responseString = WebAppContext.addContextInfo(responseString, request);
                }
                response.getOutputStream().print(responseString);
            }
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            response.sendError(ex.getCode(), createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doPost()", true);

        if (request.getPathInfo() != null && request.getPathInfo().startsWith("/SOAP")) {
            // forward to SOAP servlet
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(request.getPathInfo());
            requestDispatcher.forward(request, response);
            return;
        }

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);
            response.getOutputStream().print(responseString);
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            response.sendError(ex.getCode(), createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doPut()", true);

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);
            response.getOutputStream().print(responseString);
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            response.sendError(ex.getCode(), createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }


    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doDelete()", true);

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);
            response.getOutputStream().print(responseString);
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            response.sendError(ex.getCode(), createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        CodeTimer timer = new CodeTimer("RestServlet.doPatch()", true);

        Map<String,String> metaInfo = buildMetaInfo(request);
        try {
            String responseString = handleRequest(request, response, metaInfo);
            response.getOutputStream().print(responseString);
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            response.sendError(ex.getCode(), createErrorResponseMessage(request, metaInfo, ex));
        }
        finally {
            timer.stopAndLogTiming("");
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getHeader("X-HTTP-Method-Override"))) {
            doPatch(request, response);
        }
        else {
            super.service(request, response);
        }
    }

    protected String handleRequest(HttpServletRequest request, HttpServletResponse response, Map<String,String> metaInfo)
            throws ServiceException, IOException {

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug(getClass().getSimpleName() + " " + request.getMethod() + ":\n   "
              + request.getRequestURI() + (request.getQueryString() == null ? "" : ("?" + request.getQueryString())));
        }

        String requestString = null;
        // DELETE can have a body in some containers
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            BufferedReader reader = request.getReader();
            StringBuffer requestBuffer = new StringBuffer(request.getContentLength() < 0 ? 0 : request.getContentLength());
            String line;
            while ((line = reader.readLine()) != null)
                requestBuffer.append(line).append('\n');

            // log request
            requestString = requestBuffer.toString();
            if (logger.isMdwDebugEnabled()) {
                logger.mdwDebug(getClass().getSimpleName() + " " + request.getMethod() + " Request:\n" + requestString);
            }
        }

        authenticate(request, metaInfo);
        Set<String> reqHeaderKeys = new HashSet<String>(metaInfo.keySet());
        String responseString = new ListenerHelper().processEvent(requestString, metaInfo);
        populateResponseHeaders(reqHeaderKeys, metaInfo, response);
        if (metaInfo.get(Listener.METAINFO_CONTENT_TYPE) == null)
            response.setContentType("application/json");
        else
            response.setContentType(metaInfo.get(Listener.METAINFO_CONTENT_TYPE));

        if (metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE) != null && !metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE).equals("0"))
            response.setStatus(Integer.parseInt(metaInfo.get(Listener.METAINFO_HTTP_STATUS_CODE)));

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug(getClass().getSimpleName() + " " + request.getMethod() + " Response:\n" + responseString);
        }

        return responseString;
    }

}
