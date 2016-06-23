/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

/**
 * Serves up the HTML5 app cache manifest.  Keeps a running list of accessed taskDetail pages,
 * adding them to the cache as they're visited.
 *
 * TODO: custom page manual tasks which are cached so they're not redirected to something like the following:
 * http://x1070335.dhcp.intranet:8282/MDWHub/page.jsf?pageName=DonsTests/html5Page.xhtml
 * (would not be cached due to query parameters)
 * This means the custom page will look the same for every taskDetail.
 * Manifest must change when a new task instance is navigated to.
 */
public class CachingDispatchServlet extends HttpServlet {

    private static final String APP_CACHE = "mdw.appcache";
    private static final String VISITED_CACHED_PATHS = "com.centurylink.mdw.hub.visitedCachedPaths";
    private static StringBuffer cacheManifestStaticContent;
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    // cached pages
    // TODO: this could be driven from XML in taskview.xml
    private static final String TASK_DETAIL_PATH = "/taskDetail";
    private static final String TASK_DETAIL_FORWARD_PATH = "/tasks/taskDetail.jsf";
    private static final String TASK_DETAIL_PARAM = "taskInstanceId";
    private static final String CUSTOM_PAGE_PATH = "/page";
    private static final String CUSTOM_PAGE_FORWARD_PATH = "/page.jsf";
    private static final String CUSTOM_PAGE_PARAM = "pageName";

    private static Map<String,String> cachedDispatchPaths = new HashMap<String,String>();
    static {
        cachedDispatchPaths.put(TASK_DETAIL_PATH, TASK_DETAIL_FORWARD_PATH);
        cachedDispatchPaths.put(CUSTOM_PAGE_PATH, CUSTOM_PAGE_FORWARD_PATH);
    }
    private static Map<String,String> cachedDispatchParams = new HashMap<String,String>();
    static {
        cachedDispatchParams.put(TASK_DETAIL_PATH, TASK_DETAIL_PARAM);
        cachedDispatchParams.put(CUSTOM_PAGE_PATH, CUSTOM_PAGE_PARAM);
    }

    // non-cached
    private static final String MY_TASKS_PATH = "/taskList/myTasks";
    private static final String MY_TASKS_FORWARD_PATH = "/tasks/myTasks.jsf";
    private static final String WORKGROUP_TASKS_PATH = "/taskList/workgroupTasks";
    private static final String WORKGROUP_TASKS_FORWARD_PATH = "/tasks/workgroupTasks.jsf";

    private static Map<String,String> nonCachedDispatchPaths = new HashMap<String,String>();
    static {
        nonCachedDispatchPaths.put(MY_TASKS_PATH, MY_TASKS_FORWARD_PATH);
        nonCachedDispatchPaths.put(WORKGROUP_TASKS_PATH, WORKGROUP_TASKS_FORWARD_PATH);
        nonCachedDispatchPaths.put("/login", "/authentication/mdwLogin.jsf");
        nonCachedDispatchPaths.put("/loginError", "/authentication/mdwLoginError.jsf");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        cacheManifestStaticContent = new StringBuffer();
        InputStream is = config.getServletContext().getResourceAsStream("/WEB-INF/" + APP_CACHE);
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                cacheManifestStaticContent.append(line).append("\n");
            }
        }
        catch (IOException e) {
            logger.severeException(e.getMessage(), e);
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    logger.severeException(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.equals("/mdw.appcache")) {
            response.setContentType("text/cache-manifest");
            PrintWriter writer = response.getWriter();
            writer.println(cacheManifestStaticContent.toString());
            writer.println("# mdw.appcache for MDWHub " + ApplicationContext.getMdwVersion()  + " " + new Date());
        }
        else if (cachedDispatchPaths.containsKey(path)) {
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Cached dispatch (" + path + "): " + request.getRequestURL());
            String id = request.getPathInfo().substring(1);
            HttpSession session = request.getSession();
            Map<String,Set<String>> visitedPaths = getVisitedCachedPaths(session);
            if (visitedPaths== null)
                visitedPaths = new HashMap<String,Set<String>>();
            Set<String> visitedPathsForMe = visitedPaths.get(path);
            if (visitedPathsForMe == null) {
                visitedPathsForMe = new HashSet<String>();
                visitedPaths.put(path, visitedPathsForMe);
            }
            visitedPathsForMe.add(path + "/" + id);
            session.setAttribute(VISITED_CACHED_PATHS, visitedPaths);
            String forwardPath = cachedDispatchPaths.get(path) + "?" + cachedDispatchParams.get(path) + "=" + id;
            if (request.getQueryString() != null)
                forwardPath += "&" + request.getQueryString();

            request.getRequestDispatcher(forwardPath).forward(request, response);
        }
        else if (nonCachedDispatchPaths.containsKey(path)) {
            String forwardPath = nonCachedDispatchPaths.get(path);
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Non-cached dispatch (" + path + "): " + request.getRequestURL() + " -> " + forwardPath);
            if (request.getQueryString() != null)
                forwardPath += "?" + request.getQueryString();
            request.getRequestDispatcher(forwardPath).forward(request, response);
        }
        else if (nonCachedDispatchPaths.containsKey(path + request.getPathInfo())) {
            String forwardPath = nonCachedDispatchPaths.get(path + request.getPathInfo());
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Non-cached dispatch (" + path + ")(" + request.getPathInfo() + "): " + request.getRequestURL() + " -> " + forwardPath);
            if (request.getQueryString() != null)
                forwardPath += "?" + request.getQueryString();
            request.getRequestDispatcher(forwardPath).forward(request, response);
        }
    }

    /**
     * Map whose key is the path and whose value is a set of cached URLs.
     */
    @SuppressWarnings("unchecked")
    private Map<String,Set<String>> getVisitedCachedPaths(HttpSession session) {
        if (session == null)
            return null;
        return (Map<String,Set<String>>)session.getAttribute(VISITED_CACHED_PATHS);
    }
}