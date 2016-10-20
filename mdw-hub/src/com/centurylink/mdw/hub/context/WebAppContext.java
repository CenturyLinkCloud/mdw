/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.AuthConstants;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.system.SysInfo;
import com.centurylink.mdw.model.value.system.SysInfoCategory;

public class WebAppContext {

    private static Mdw mdw;
    public static Mdw getMdw() throws IOException {
        if (mdw == null) {

            String hubRoot = "/mdw";  // TODO overridable
            String servicesRoot = PropertyManager.getProperty("mdw.admin.services.url");
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

            boolean isDev = "dev".equals(System.getProperty("runtimeEnv"));
            if (isDev) {
                String hubUser = PropertyManager.getProperty(PropertyNames.MDW_DEV_USER);
                if (hubUser == null)
                    hubUser = PropertyManager.getProperty("mdw.hub.user"); // compatibility
                mdw.setHubUser(hubUser);
            }

            if (isDev || AuthConstants.getOAuthTokenLocation() != null || AuthConstants.isMdwLdapAuth())
                mdw.setLoginPage("/login");
            else
                mdw.setLoginPage("/authentication/login.jsf");
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
            attrInfo.addSysInfo(new SysInfo(attrName, String.valueOf(context.getAttribute(attrName))));
        }
        containerInfos.add(attrInfo);
        return new SysInfoCategory("Container Details", containerInfos);
    }

}
