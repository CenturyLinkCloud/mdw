/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.MiscConstants;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.AuthUtils;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.listener.ListenerHelper;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.event.BroadcastHelper;
import com.centurylink.mdw.services.process.EventServices;
import com.centurylink.mdw.services.task.TaskManagerAccess;

/**
 * The interface needs to be implemented if it is desired
 * that the class is loaded and executed when the server is started
 * The framework will load this class and execute the onStartup method
 * at start up.
 */

public class HTTPListenerHelper {

    private static String PARAM_NAME = "name";
    private static String PARAM_LANGUAGE = "language";
    private static String PARAM_VERSION = "version";
    private static String PARAM_REFRESH = "refresh";

    public void getResource(HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        RuleSetVO ruleset = null;
        StandardLogger logger = LoggerUtil.getStandardLogger();
        try {
            String resourceName = request.getParameter(PARAM_NAME);
            String language = request.getParameter(PARAM_LANGUAGE);
            String versionString = request.getParameter(PARAM_VERSION);
            String refresh = request.getParameter(PARAM_REFRESH);
            int version = 0;
            if (versionString!=null) version = ProcessVO.versionFromString(versionString);
            if (resourceName == null)
                throw new IllegalArgumentException("Missing request parameter: " + PARAM_NAME);
            if ("true".equalsIgnoreCase(refresh)) {
                (new RuleSetCache()).clearCache();
                logger.debug("RuleSetCache cleared");
            }
            FormServer handler = FormServer.getInstance();
            ruleset = handler.getResource(resourceName, language, version);
            if (ruleset == null)
                throw new DataAccessException("Cannot load web resource from database: " + resourceName);
            byte[] bytes = ruleset.getContent();
            response.setContentType(ruleset.getContentType());
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServletException(ex.getMessage(), ex);
        }
    }

    public void getResource(String pathInfo, boolean refresh, HttpServletResponse response)
        throws ServletException
    {
        RuleSetVO ruleset = null;
        StandardLogger logger = LoggerUtil.getStandardLogger();
        try {
            // infer standard resource name and language
            String filepath = pathInfo.substring("/resource/".length());
            int dotloc = filepath.lastIndexOf(".");
            if (dotloc<0) throw new ServletException("Unknown getResource request, pathInfo=" + pathInfo);
            String language = RuleSetVO.getFormat(filepath);
            if (refresh) {
                (new RuleSetCache()).clearCache();
                logger.debug("RuleSetCache cleared");
            }
            // load from cache, files (local override), database, and internal source
            FormServer handler = FormServer.getInstance();
            String resourceName = filepath.substring(0,dotloc);
            ruleset = handler.getResource(resourceName, language, 0);
            if (ruleset == null)
                throw new DataAccessException("Cannot load web resource from database: " + filepath);
            byte[] bytes = ruleset.getContent();
            response.setContentType(ruleset.getContentType());
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        } catch (DataAccessException ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServletException(ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServletException(ex.getMessage(), ex);
        }
    }

    public void doLogin(HttpServletRequest request, HttpServletResponse response)
    throws ServletException {
        String user = request.getParameter("user");
        String pass = request.getParameter("pass");
        if (user!=null && pass!=null) {
            try {
                AuthUtils.ldapAuthenticate(user, pass);
                String originalPath = request.getParameter(MiscConstants.originalPath);
                boolean allowUnauthorized = "true".equalsIgnoreCase(request.getParameter(MiscConstants.AllowAnyAuthenticatedUser));
                AuthenticatedUser authuser = TaskManagerAccess.getInstance().getUserAuthorization(user, allowUnauthorized);
                HttpSession session = request.getSession(true);
                session.setAttribute(MiscConstants.authenticatedUser, authuser);
                String url = "http://" + request.getServerName() + ":" +
                request.getServerPort() + originalPath;
                response.sendRedirect(url);
            }
            catch (Exception e) {
                String msg = e.getMessage();
                display_authentication_page(request, msg, response);
            }
        } else {
            display_authentication_page(request, null, response);
        }
    }

    private void display_authentication_page(HttpServletRequest request,
            String errmsg, HttpServletResponse response) throws ServletException {
        response.setContentType("text/html");
        StringBuffer html = new StringBuffer();
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>Authenticate</title>\n");
        html.append("</head>\n");
        html.append("<body bgcolor='#c0c0c0'>\n");
        html.append("<h1>MDW Authentication</h1>\n");
        if (errmsg!=null) {
            html.append("<font color='red'><b>" + errmsg + "</b></font>\n");
        }
        String originalPath = request.getParameter(MiscConstants.originalPath);
        String allowUnauthorized = request.getParameter(MiscConstants.AllowAnyAuthenticatedUser);
        html.append("<form action='" + request.getContextPath() + "/MDWHTTPListener/login' method='post'>\n");
        html.append("<input type='hidden' name='" + MiscConstants.originalPath + "' value='")
                .append(StringHelper.escapeXml(originalPath)).append("'>\n");
        if (allowUnauthorized!=null)
            html.append("<input type='hidden' name='" + MiscConstants.AllowAnyAuthenticatedUser + "' value='")
            .append(allowUnauthorized).append("'>\n");
        html.append("<center>\n");
        html.append("<table border='1'>\n");
        html.append("<tr><td>User Name</td><td><input type='text' name='user'/></td></tr>\n");
        html.append("<tr><td>Password</td><td><input type='password' name='pass'/></td></tr>\n");
        html.append("<tr><td colspan='2' align='center'><input type='submit' value='Log In'></td></tr>\n");
        html.append("</table>\n");
        html.append("</center>\n");
        html.append("</form>\n");
        html.append("<p>\n");
        html.append("<i>This authentication page is for development environment only,\n");
        html.append("ClearTrust filter will kick in instead in production/test environments.</i>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        response.setContentType("text/html");
        try {
            response.getOutputStream().print(html.toString());
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Handle a JSON request from client.
     * The response is a JSON document as well
     * @param pRequest
     * @param pResponse
     */
    public void handleJsonService(HttpServletRequest pRequest, HttpServletResponse pResponse) {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        InputStream in = null;
        try {
             int n = pRequest.getContentLength();
             in = pRequest.getInputStream();
             byte[] bytes = new byte[n];
             in.read(bytes);
             String content = new String(bytes);
             if (logger.isDebugEnabled()) logger.debug("JSON REQUEST: " + content);
             FormServer handler = FormServer.getInstance();
             content = handler.handleJsonAjax(content, pRequest);
             if (logger.isDebugEnabled()) logger.debug("JSON RESPONSE: " + content);
            pResponse.getWriter().write(content);
        } catch (Exception e) {
            logger.severeException(e.getMessage(), e);
        } finally {
            if (in!=null) try {in.close(); } catch (Exception e) {};
        }
    }

    /**
     * Handle an XML request from client.
     * The response does not have to be XML - determined by contract with client, and
     * can be XML, HTML, JSON, or plain text.
     * @param pRequest
     * @param pResponse
     * @throws JMSException
     */
    public void handleXmlService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuffer requestBuffer = new StringBuffer(request.getContentLength());
        String line = null;
        while ((line = reader.readLine()) != null) {
            requestBuffer.append(line).append('\n');
        }

        // read the POST request contents
        String requestString = requestBuffer.toString();
        String responseString;

        if (request.getHeader("MDWInternalMessageId")!=null) {
            String internalMessageId = request.getHeader("MDWInternalMessageId");
            try {
                if (!EventServices.getInstance().sendInternalMessageCheck(ThreadPoolProvider.WORKER_LISTENER, internalMessageId, "HTTPListener", requestString))
                    throw new JMSException("No thread available to send internal message");
                responseString = "OK";
            }
            catch (JMSException e) {
                logger.severeException("Failed to send internal message ", e);
                responseString = "ERROR: Failed to send internal message";
            }
        } else if (request.getHeader("MDWCertifiedMessageId")!=null) {
            boolean consumed;
            String certifiedMessageId = request.getHeader("MDWCertifiedMessageId");
            try {
                EventManager eventManager = ServiceLocator.getEventManager();
                consumed = eventManager.consumeCertifiedMessage(certifiedMessageId);
                if (consumed) {
                    if (logger.isMdwDebugEnabled()) {
                        logger.mdwDebug("RESTful Service POST Request:\n" + requestString);
                    }
                    ListenerHelper helper = new ListenerHelper();
                    helper.processEvent(requestString, buildMetaInfo(request));
                    if (logger.isMdwDebugEnabled()) {
                        logger.mdwDebug("RESTful Service acknowledges:\n" + certifiedMessageId);
                    }
                } // else consumed previously
                responseString = certifiedMessageId;
            } catch (DataAccessException e) {
                logger.severeException("Failed to check if certified message is received", e);
                responseString = "ERROR: Failure in checking if certified message has been received";
            }
        } else if (request.getHeader("MDWBroadcast")!=null) {
            try {
                BroadcastHelper broadcastHelper = new BroadcastHelper();
                broadcastHelper.processBroadcastMessage(requestString);
                logger.info("Received and processed broadcast: " + requestString);
            } catch (Exception e) {
                logger.severeException("Failed to process broadcast", e);
            }
            responseString = "OK";
        } else {
            if (logger.isMdwDebugEnabled()) {
                logger.mdwDebug("RESTful Service POST Request:\n" + requestString);
            }
            ListenerHelper helper = new ListenerHelper();
            responseString = helper.processEvent(requestString, buildMetaInfo(request));
            if (logger.isMdwDebugEnabled()) {
                logger.mdwDebug("RESTful Service POST Response:\n" + responseString);
            }
        }
        response.getOutputStream().print(responseString);
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

    public void checkClass(HttpServletRequest request, HttpServletResponse response, Class<?> webAppClass)
    throws ServletException {
        String classToBase = request.getParameter("classToBase");
        String classToCheck = request.getParameter("classToCheck");
        String errmsg = null;
        String classLocation = null;
        ArrayList<String> checkClassLoaderStack = null;
        ArrayList<String> baseClassLoaderStack = null;
        if (classToCheck!=null) {
            try {
                String resource = classToCheck;
                if (!resource.startsWith("/")) resource = "/" + resource;
                resource = resource.replace('.', '/');
                resource = resource + ".class";
                Class<?> baseClass;
                if (classToBase==null || classToBase.trim().length()==0) baseClass = webAppClass;
                else baseClass = Class.forName(classToBase, false, webAppClass.getClassLoader());
                ClassLoader classloader = baseClass.getClassLoader();
                baseClassLoaderStack = new ArrayList<String>();
                while (classloader!=null) {
                    String clinfo = classloader.toString();
                    baseClassLoaderStack.add(clinfo);
                    classloader = classloader.getParent();
                }
                URL classUrl = baseClass.getResource(resource);
                if (classUrl == null) {
                    errmsg = "Class to check is not found";
                } else {
                    classLocation = classUrl.getFile();
                    Class<?> checkClass = Class.forName(classToCheck, false, baseClass.getClassLoader());
                    classloader = checkClass.getClassLoader();
                    checkClassLoaderStack = new ArrayList<String>();
                    while (classloader!=null) {
                        String clinfo = classloader.toString();
                        checkClassLoaderStack.add(clinfo);
                        classloader = classloader.getParent();
                    }
                }

            } catch (Exception e) {
                errmsg = e.toString();
            }
        }
        response.setContentType("text/html");
        StringBuffer html = new StringBuffer();
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>Authenticate</title>\n");
        html.append("</head>\n");
        html.append("<body bgcolor='#c0d0c0'>\n");
        html.append("<h1>Class and Class Loader Information</h1>\n");
        html.append("<form action='" + request.getContextPath() + "/MDWHTTPListener/checkclass' method='post'>\n");
        html.append("<table border='1'>\n");
        html.append("<tr><td>Class to check:</td><td><input type='text' size='64' name='classToCheck'");
        if (classToCheck!=null) html.append(" value='").append(classToCheck).append("'");
        html.append("/></td></tr>\n");
        html.append("<tr><td>Class location</td><td>")
            .append(classLocation==null?"&nbsp;":classLocation).append("</td></tr>\n");
        html.append("<tr><td>Class loader</td><td>");
        if (checkClassLoaderStack!=null && checkClassLoaderStack.size()>0) {
            html.append("<ul>");
            for (String clinfo : checkClassLoaderStack) {
                html.append("<li>").append(clinfo).append("</li>");
            }
            html.append("</ul>");
        } else {
            html.append("&nbsp;");
        }
        html.append("</td></tr>\n");
        if (errmsg!=null) {
            html.append("<tr><td>Error</td><td><font color='red'><b>" + errmsg + "</b></font></td></tr>\n");
        }
        html.append("<tr><td colspan='2' align='center'><input type='submit' value='Submit'/></td></tr>\n");
        html.append("</table>\n");
        html.append("<h2>Base Class</h2>\n");
        html.append("This is the class whose class loader is used to find the checked class above.\n");
        html.append("<table border='1'>\n");
        html.append("<tr><td>Base Class:</td><td><input type='text' size='64' name='classToBase'");
        if (classToBase==null||classToBase.length()==0)
            html.append(" value='").append(webAppClass.getName()).append("'");
        else html.append(" value='").append(classToBase).append("'");
        html.append("/></td></tr>\n");

        html.append("<tr><td>Class loader</td><td>");
        if (baseClassLoaderStack!=null && baseClassLoaderStack.size()>0) {
            html.append("<ul>");
            for (String clinfo : baseClassLoaderStack) {
                html.append("<li>").append(clinfo).append("</li>");
            }
            html.append("</ul>");
        } else {
            html.append("&nbsp;");
        }
        html.append("</td></tr>\n");
        html.append("</table>\n");
        html.append("</form>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        response.setContentType("text/html");
        try {
            response.getOutputStream().print(html.toString());
        } catch (IOException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

}