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
package com.centurylink.mdw.hub.context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.system.SysInfo;
import com.centurylink.mdw.model.system.SysInfoCategory;
import com.centurylink.mdw.services.asset.CustomPageLookup;
import com.centurylink.mdw.util.log.LoggerUtil;

public class WebAppContext {

    private static Mdw mdw;
    public static Mdw getMdw() throws IOException {
        if (mdw == null) {

            String hubRoot = ApplicationContext.getMdwHubContextRoot();
            if (hubRoot.length() > 0 && !hubRoot.startsWith("/"))
                hubRoot = "/" + hubRoot;
            String servicesRoot = PropertyManager.getProperty("mdw.hub.services.url");
            if (servicesRoot == null)
                servicesRoot = hubRoot;

            File assetRoot = null;
            String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
            if (assetLoc != null)
                assetRoot = new File(assetLoc);

            String overridePackage = PropertyManager.getProperty(PropertyNames.MDW_HUB_OVERRIDE_PACKAGE);
            if (overridePackage == null)
                overridePackage = "mdw-hub";

            initMdwBuildVersion();
            mdw = new Mdw(mdwVersion, mdwBuild, hubRoot, servicesRoot, assetRoot, overridePackage);

            boolean isDev = "dev".equals(System.getProperty("mdw.runtime.env"));
            if (isDev) {
                String hubUser = ApplicationContext.getDevUser();
                mdw.setHubUser(hubUser);
            }

            mdw.setWebSocketUrl(ApplicationContext.getWebSocketUrl());
            mdw.setAuthMethod(ApplicationContext.getAuthMethod());
            mdw.setDocsRoot(ApplicationContext.getDocsUrl());

            mdw.setCentralRoot(ApplicationContext.getMdwCentralUrl());
            String appId = PropertyManager.getProperty(PropertyNames.MDW_APP_ID);
            mdw.setAppId(appId);

            String discoveryUrl = PropertyManager.getProperty(PropertyNames.DISCOVERY_URL);
            if (discoveryUrl == null)
                discoveryUrl = "https://mdw-dev.useast.appfog.ctl.io/mdw";
            mdw.setDiscoveryUrl(discoveryUrl);

            mdw.setGitBranch(PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH));
            mdw.setGitTag(PropertyManager.getProperty(PropertyNames.MDW_GIT_TAG));

            try {
                JSONArray routes = CustomPageLookup.getUiRoutes();
                if (routes != null)
                    mdw.setCustomRoutes(routes.toString());
            }
            catch (ReflectiveOperationException ex) {
                LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            }
        }
        return mdw;
    }

    /**
     * Recursively list override files.  Assumes dir path == ext == type.
     */
    public static List<File> listOverrideFiles(String type) throws IOException {
        List<File> files = new ArrayList<File>();
        if (getMdw().getOverrideRoot().isDirectory()) {
            File dir = new File(getMdw().getOverrideRoot() + "/" + type);
            if (dir.isDirectory())
                addFiles(files, dir, type);
        }
        return files;
    }

    private static void addFiles(List<File> list, File dir, String ext) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory())
                addFiles(list, file, ext);
            else if (file.getName().endsWith("." + ext))
                list.add(file);
        }
    }

    private static String mdwVersion;
    private static String mdwBuild;

    private static void initMdwBuildVersion() throws IOException {
        if (mdwVersion == null) {
            mdwVersion = ApplicationContext.getMdwVersion();
            mdwBuild = ApplicationContext.getMdwBuildTimestamp();
        }
    }

    public static String addContextInfo(String str, HttpServletRequest request) {
        try {
            JSONArray inArr = new JSONArray(str);
            JSONArray addedArr = new JSONArray();
            addedArr.put(getContainerInfo(request.getSession().getServletContext()).getJson());
            addedArr.put(getRequestInfo(request).getJson());
            addedArr.put(getSessionInfo(request.getSession()).getJson());
            for (int i = 0; i < inArr.length(); i++)
                addedArr.put(inArr.get(i));
            return addedArr.toString(2);
        }
        catch (Exception ex) {
            LoggerUtil.getStandardLogger().severeException(ex.getMessage(), ex);
            return str;
        }
    }

    /**
     * Request info for System REST service.
     */
    public static SysInfoCategory getRequestInfo(HttpServletRequest request) {
        List<SysInfo> requestInfos = new ArrayList<SysInfo>();
        requestInfos.add(new SysInfo("Method", request.getMethod()));
        requestInfos.add(new SysInfo("URL", request.getRequestURL().toString()));
        requestInfos.add(new SysInfo("Protocol", request.getProtocol()));
        requestInfos.add(new SysInfo("Servlet path", request.getServletPath()));
        requestInfos.add(new SysInfo("Context path", request.getContextPath()));
        requestInfos.add(new SysInfo("Path info", request.getPathInfo()));
        requestInfos.add(new SysInfo("Path translated", request.getPathTranslated()));
        requestInfos.add(new SysInfo("Query string", request.getQueryString()));
        requestInfos.add(new SysInfo("Content length: ", String.valueOf(request.getContentLength())));
        requestInfos.add(new SysInfo("Content type: ", request.getContentType()));
        requestInfos.add(new SysInfo("Server name", request.getServerName()));
        requestInfos.add(new SysInfo("Server port", String.valueOf(request.getServerPort())));
        requestInfos.add(new SysInfo("Remote user", request.getRemoteUser()));
        requestInfos.add(new SysInfo("Remote address", request.getRemoteAddr()));
        requestInfos.add(new SysInfo("Remote host", request.getRemoteHost()));
        requestInfos.add(new SysInfo("Authorization type", request.getAuthType()));
        requestInfos.add(new SysInfo("Locale", String.valueOf(request.getLocale())));

        SysInfo paramInfo = new SysInfo("Parameters");
        Enumeration<?> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String)paramNames.nextElement();
            paramInfo.addSysInfo(new SysInfo(paramName, request.getParameter(paramName)));
        }
        requestInfos.add(paramInfo);

        SysInfo headerInfo = new SysInfo("Headers");
        Enumeration<?> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String)headerNames.nextElement();
            headerInfo.addSysInfo(new SysInfo(headerName, request.getHeader(headerName)));
        }
        requestInfos.add(headerInfo);

        SysInfo attrInfo = new SysInfo("Attributes");
        Enumeration<?> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String)attrNames.nextElement();
            attrInfo.addSysInfo(new SysInfo(attrName, String.valueOf(request.getAttribute(attrName))));
        }
        requestInfos.add(attrInfo);

        return new SysInfoCategory("Request Details", requestInfos);
    }

    public static SysInfoCategory getSessionInfo(HttpSession session) {
        List<SysInfo> sessionInfos = new ArrayList<SysInfo>();
        sessionInfos.add(new SysInfo("ID", session.getId()));
        sessionInfos.add(new SysInfo("New", String.valueOf(session.isNew())));
        sessionInfos.add(new SysInfo("Created", String.valueOf(new Date(session.getCreationTime()))));
        sessionInfos.add(new SysInfo("Last accessed", String.valueOf(new Date(session.getLastAccessedTime()))));
        sessionInfos.add(new SysInfo("Max inactive", String.valueOf(session.getMaxInactiveInterval())));

        SysInfo attrInfo = new SysInfo("Attributes");
        Enumeration<?> attrNames = session.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String)attrNames.nextElement();
            attrInfo.addSysInfo(new SysInfo(attrName, String.valueOf(session.getAttribute(attrName))));
        }
        sessionInfos.add(attrInfo);
        return new SysInfoCategory("Session Details", sessionInfos);
    }

    public static SysInfoCategory getContainerInfo(ServletContext context) {
        List<SysInfo> containerInfos = new ArrayList<SysInfo>();
        containerInfos.add(new SysInfo("Root path", context.getRealPath("/")));
        containerInfos.add(new SysInfo("Servlet container", context.getServerInfo()));
        containerInfos.add(new SysInfo("Servlet version", context.getMajorVersion() + "." + context.getMinorVersion()));
        containerInfos.add(new SysInfo("Servlet context", context.getServletContextName()));
        SysInfo attrInfo = new SysInfo("Attributes");
        Enumeration<?> attrNames = context.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String)attrNames.nextElement();
            if (!"org.apache.tomcat.util.scan.MergedWebXml".equals(attrName) && !"org.apache.catalina.jsp_classpath".equals(attrName))
            attrInfo.addSysInfo(new SysInfo(attrName, String.valueOf(context.getAttribute(attrName))));
        }
        containerInfos.add(attrInfo);
        return new SysInfoCategory("Container Details", containerInfos);
    }

}
