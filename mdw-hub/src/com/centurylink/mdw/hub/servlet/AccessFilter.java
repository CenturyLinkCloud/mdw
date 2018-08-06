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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.auth.AuthExcludePattern;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.hub.context.WebAppContext;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.system.Server;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.util.AuthUtils;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.yaml.YamlLoader;

@WebFilter(urlPatterns={"/*"})
public class AccessFilter implements Filter {

    private static String ACCESS_CONFIG_FILE = "access.yaml";
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static List<InetAddress> upstreamHosts; // null means not restricted
    private static String authMethod = "mdw";
    private static String authUserHeader;
    private static String forwardingHeader;  // x-forwarded-for
    private static String forwardedProtoHeader; // x-forwarded-proto
    private static String authTokenLoc;
    private static List<AuthExcludePattern> authExclusions = new ArrayList<AuthExcludePattern>();
    private static List<AuthExcludePattern> requireHttps = new ArrayList<AuthExcludePattern>();
    private static Map<String,String> responseHeaders;
    private static boolean devMode;
    private static boolean allowAnyAuthenticatedUser;
    private static int sessionTimeoutSecs = 0;
    private static boolean logResponseTimes;
    private static boolean logHeaders;
    private static boolean logParameters;
    private static boolean logCookies;
    private static List<InetAddress> internalHosts; // List of servers in cluster

    @SuppressWarnings("unchecked")
    public void init(FilterConfig filterConfig) throws ServletException {

        devMode = ApplicationContext.isDevelopment();

        try {
            String accessYaml;
            if (ApplicationContext.isCloudFoundry())
                accessYaml = System.getenv("mdw_access");
            else
                accessYaml = new String(FileHelper.readConfig(ACCESS_CONFIG_FILE).getBytes());

            YamlLoader yamlLoader = new YamlLoader(accessYaml);
            Map<?,?> topMap = yamlLoader.getRequiredMap("", yamlLoader.getTop(), "");

            if (devMode)
                ApplicationContext.setDevUser(yamlLoader.get("devUser", topMap));

            ApplicationContext.setServiceUser(yamlLoader.get("serviceUser", topMap));

            // upstreamHosts
            List<?> upstreamHostsList = yamlLoader.getList("upstreamHosts", topMap);
            if (upstreamHostsList != null) {
                upstreamHosts = new ArrayList<InetAddress>();
                for (Object ip : upstreamHostsList)
                    upstreamHosts.add(InetAddress.getByName(ip.toString()));
            }

            // authMethod
            String authMethodVal = yamlLoader.get("authMethod", topMap);
            if (authMethodVal != null)
                authMethod = authMethodVal;
            WebAppContext.getMdw().setAuthMethod(authMethod);
            ApplicationContext.setAuthMethod(authMethod);

            // authUserHeader
            authUserHeader = yamlLoader.get("authUserHeader", topMap);

            // forwardingHeader
            forwardingHeader = yamlLoader.get("forwardingHeader", topMap);

            // forwardProto
            forwardedProtoHeader = yamlLoader.get("forwardedProtoHeader", topMap);

            // authTokenLoc
            authTokenLoc = yamlLoader.get("authTokenLoc", topMap);
            WebAppContext.getMdw().setAuthTokenLoc(authTokenLoc);

            // authExclusions
            List<?> authExclusionsList = yamlLoader.getList("authExclusions", topMap);
            if (authExclusionsList != null) {
                for (Object authExclude : authExclusionsList)
                    authExclusions.add(new AuthExcludePattern(authExclude.toString()));
            }

            // requireHttps
            List<?> requireHttpsList = yamlLoader.getList("requireHttps", topMap);
            if (requireHttpsList != null) {
                for (Object reqHttps : requireHttpsList)
                    requireHttps.add(new AuthExcludePattern(reqHttps.toString()));
            }

            // responseHeaders
            Map<?,?> responseHeadersMap = yamlLoader.getMap("responseHeaders", topMap);
            if (responseHeadersMap != null) {
                responseHeaders = new HashMap<String,String>();
                for (Object key : responseHeadersMap.keySet())
                    responseHeaders.put(key.toString(), (String)responseHeadersMap.get(key));
            }

            // sessionTimeout
            String sessionTimeoutStr = yamlLoader.get("sessionTimeout", topMap);
            if (sessionTimeoutStr != null)
                sessionTimeoutSecs = Integer.parseInt(sessionTimeoutStr);

            // allowAnyAuthenticadUser
            String allowAnyAuthUserStr = yamlLoader.get("allowAnyAuthenticatedUser", topMap);
            allowAnyAuthenticatedUser = "true".equalsIgnoreCase(allowAnyAuthUserStr);
            if (allowAnyAuthenticatedUser)
                WebAppContext.getMdw().setAllowAnyAuthenticatedUser(allowAnyAuthenticatedUser);

            Map<?,?> optionsMap = yamlLoader.getMap("loggingOptions", topMap);
            if (optionsMap != null) {
                // timedResponses
                String logRespTimesStr = yamlLoader.get("logResponseTimes", optionsMap);
                logResponseTimes = "true".equalsIgnoreCase(logRespTimesStr);
                // showHeaders
                String logHeadersStr = yamlLoader.get("logHeaders", optionsMap);
                logHeaders = "true".equalsIgnoreCase(logHeadersStr);
                // logParameters
                String logParamsStr = yamlLoader.get("logParameters", optionsMap);
                logParameters = "true".equalsIgnoreCase(logParamsStr);
                // logCookies
                String logCookiesStr = yamlLoader.get("logCookies", optionsMap);
                logCookies = "true".equalsIgnoreCase(logCookiesStr);
            }

            //Get MDW server list for IntraMessaging (service propagation)
            internalHosts =  new ArrayList<InetAddress>();
            for (Server server : ApplicationContext.getServerList().getServers()) {
                internalHosts.add(InetAddress.getByName(server.getHost()));
            }

            WebAppContext.getMdw().setCustomPaths(yamlLoader.getList("customPaths", topMap));

        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServletException(ex.getMessage(), ex);
        }
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
    throws IOException, ServletException {

        if (!(req instanceof HttpServletRequest))
            return;

        long start = System.currentTimeMillis();

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        HttpSession session = request.getSession();

        String path = (request.getServletPath().equals("/Services") ? "/services" : request.getServletPath()) + (request.getPathInfo() == null ? "" : request.getPathInfo());

        try {
            InetAddress remoteHost = null;
            String allowedHost = null;
            if (upstreamHosts != null && !devMode) {
                if (forwardingHeader != null) {
                    String forwardIps = request.getHeader(forwardingHeader);
                    if (forwardIps != null) {
                        for (String ip : forwardIps.split("[,\\s]+")) {
                            if (upstreamHosts.contains(InetAddress.getByName(ip))) {
                                allowedHost = ip;
                                break;
                            }
                        }
                    }
                }
                if (allowedHost == null) {
                    remoteHost = InetAddress.getByName(request.getRemoteHost());
                    if (upstreamHosts.contains(remoteHost))
                        allowedHost = remoteHost.toString();
                }
                if (allowedHost == null && !isAuthExclude(path)) {
                    throw new MdwSecurityException("Access not allowed from host: " + request.getRemoteHost());
                }
            }

            if (session.isNew()) {
                logger.mdwDebug("** - new HTTP session from: " + request.getRemoteHost());
                if (sessionTimeoutSecs > 0)
                    session.setMaxInactiveInterval(sessionTimeoutSecs);
            }

            if (logHeaders)
                logRequestHeaders(request);
            if (logParameters)
                logRequestParams(request);
            if (logCookies)
                logCookies(request);

            String authUser = null;
            if (authUserHeader != null && !ApplicationContext.isMdwAuth()) {
                authUser = request.getHeader(authUserHeader);
            }

            if (isHttpsRequired(path) && !isHttps(request)) {
                throw new MdwSecurityException("HTTPS required: " + path);
            }

            // check authentication
            AuthenticatedUser user = (AuthenticatedUser) session.getAttribute("authenticatedUser");
            if (user != null && path.equals("/services/com/centurylink/mdw/central/auth")) {
                // clear user from session (may be logging in as different user)
                user = null;
                session.invalidate();
            }
            if (user == null || user.getCuid() == null || (authUser != null && !user.getCuid().equals(authUser))) {
                user = null;
                String authHdr = request.getHeader(Listener.AUTHORIZATION_HEADER_NAME);
                if (authHdr == null && ApplicationContext.isMdwAuth() && "GET".equals(request.getMethod())) //JWT coming in on URL query string
                    authHdr = request.getParameter(Listener.AUTHORIZATION_HEADER_NAME);
                if (authHdr != null) {
                    Map<String,String> headers = new HashMap<String,String>();
                    headers.put(Listener.AUTHORIZATION_HEADER_NAME, authHdr);
                    if (AuthUtils.authenticate(AuthUtils.AUTHORIZATION_HEADER_AUTHENTICATION, headers)) {
                        authUser = headers.get(Listener.AUTHENTICATED_USER_HEADER);
                        if (authUser != null) {
                            User u = ServiceLocator.getUserServices().getUser(authUser);
                            if (u != null) {
                                user = new AuthenticatedUser(u, u.getAttributes());
                                session.setAttribute("authenticatedUser", user);
                            }
                            if (user == null) {
                                if (!allowAnyAuthenticatedUser && !(devMode && "/services/System/exit".equals(path)))
                                    throw new MdwSecurityException("User not authorized: " + authUser);
                            }
                        }
                    }
                }
                if (devMode) {
                    if (authUser == null) // otherwise honor the header to support auth even in dev mode
                        authUser = ApplicationContext.getDevUser();
                    ApplicationContext.setDevUser(authUser);
                    WebAppContext.getMdw().setHubUser(authUser);
                }
                if (user == null && authUser != null && authUser.length() > 0) {
                    // load the user
                    User u = ServiceLocator.getUserServices().getUser(authUser);
                    if (u != null) {
                        user = new AuthenticatedUser(u, u.getAttributes());
                        session.setAttribute("authenticatedUser", user);
                    }
                    if (user == null) {
                        if (!allowAnyAuthenticatedUser && !(devMode && "/services/System/exit".equals(path)))
                            throw new MdwSecurityException("User not authorized: " + authUser);
                    }
                }
                else if (user == null) {
                    // user not authenticated
                    if (remoteHost == null)
                        remoteHost = InetAddress.getByName(request.getRemoteHost());
                    boolean internalRequest = IsIntraRequest(remoteHost);
                    if (!isAuthExclude(path) && !internalRequest) {
                        if ("ct".equals(authMethod)) {
                            // redirect to login page is performed upstream (CT web agent)
                            throw new MdwSecurityException("Authentication required");
                        }
                        else {
                            if (path.startsWith("/services/SOAP/") || path.startsWith("/SOAP/"))
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            else if (path.startsWith("/services/") || path.startsWith("/REST/") || path.startsWith("/asset/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().println(new Status(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Required").getJson().toString(2));
                            }
                            else
                                response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/login");

                            return;
                        }
                    }
                    else if (internalRequest) {
                        User u = ServiceLocator.getUserServices().getUser(ApplicationContext.getServiceUser() == null ? "mdwapp" : ApplicationContext.getServiceUser());
                        if (u != null) {
                            user = new AuthenticatedUser(u, u.getAttributes());
                            session.setAttribute("authenticatedUser", user);
                        }
                        else
                            logger.warn("Cannot identify a valid service user for internal MDW request.  Please create mdwapp user or specify another user.");
                    }
                }
            }

            // This is to remove the JWT from QueryString
            if ((user != null || authUser != null)  && "GET".equals(request.getMethod()) && request.getParameter(Listener.AUTHORIZATION_HEADER_NAME) != null) {
                response.sendRedirect(ApplicationContext.getMdwHubUrl() + "/");
                return;
            }

            if (responseHeaders != null || logHeaders) {
                chain.doFilter(request, new ResponseWrapper(response));
            }
            else {
                chain.doFilter(request, response);
            }

            if (logResponseTimes)
                logger.info(" ** - response (" + path + "): " + (System.currentTimeMillis() - start) + " ms");
        }
        catch (Throwable t) {
            logger.severeException("Error accessing " + request.getRequestURL() + ": " + t.getMessage() , t);
            request.setAttribute("error", t);
            request.getRequestDispatcher("/error").forward(request, response);
        }
    }

    public boolean isAuthExclude(String path) {
        if (authExclusions != null) {
            for (AuthExcludePattern authExclude : authExclusions) {
                if (authExclude.match(path))
                    return true;
            }
        }
        return false;
    }

    public boolean isHttpsRequired(String path) {
        if (requireHttps != null) {
            for (AuthExcludePattern reqHttps : requireHttps) {
                if (reqHttps.match(path))
                    return true;
            }
        }
        return false;
    }

    private boolean isHttps(HttpServletRequest request) {
        if (request.getProtocol().toLowerCase().equals("https")) {
            return true;
        }
        else if (forwardedProtoHeader != null) {
            String forwardedProto = request.getHeader(forwardedProtoHeader);
            if (forwardedProto != null && forwardedProto.toLowerCase().equals("https"))
                return true;
        }
        return false;
    }

    private boolean IsIntraRequest(InetAddress remoteHost) {
        if (internalHosts.contains(remoteHost))
            return true;
        else
            return false;
    }

    public void destroy() {
    }

    private void logRequestHeaders(HttpServletRequest request) {
        logger.info("** - requestHeaders (" + request.getRequestURL() + "):");
        Enumeration<?> en = request.getHeaderNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement().toString();
            if (name.equalsIgnoreCase(Listener.AUTHORIZATION_HEADER_NAME) || name.equalsIgnoreCase("Password"))
                logger.info("   " + name + ": *************");
            else
                logger.info("   " + name + ": " + request.getHeader(name));
        }
    }

    private void logRequestParams(HttpServletRequest request) {
        logger.info("** - requestParameters (" + request.getRequestURL() + "):");
        Enumeration<?> en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement().toString();
            logger.info("   " + name + ": " + request.getParameter(name));
        }
    }

    private void logCookies(HttpServletRequest request) {
        logger.info("** - cookies (" + request.getRequestURL() + "):");
        if (request.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : request.getCookies()) {
                logger.info("   " + cookie.getName() + ": " + cookie.getValue());
            }
        }
    }

    class ResponseWrapper extends HttpServletResponseWrapper {

        private static final String HEADER_PRE = "  ** - responseHeader: ";

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
            if (responseHeaders != null) {
                for (String key : responseHeaders.keySet())
                    addHeader(key, responseHeaders.get(key));
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if (logHeaders)
                logger.info(HEADER_PRE + name + ": " + value);
            super.addHeader(name, value);
        }

        @Override
        public void addDateHeader(String name, long value) {
            if (logHeaders)
                logger.info(HEADER_PRE + name + ": " + new Date(value));
            super.addDateHeader(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
            if (logHeaders)
                logger.info(HEADER_PRE + name + ": " + value);
            super.addIntHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if (logHeaders)
                logger.info(HEADER_PRE + name + ": " + value);
            super.setHeader(name, value);
        }

        @Override
        public void setDateHeader(String name, long value) {
            if (logHeaders)
                logger.info(HEADER_PRE + name + ": " + new Date(value));
            super.setDateHeader(name, value);
        }

        @Override
        public void setIntHeader(String name, int value) {
            if (logHeaders)
                logger.info(HEADER_PRE + name + "=" + value);
            super.setIntHeader(name, value);
        }
    }
}