/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.system.filepanel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.system.Dir;
import com.centurylink.mdw.model.system.FileInfo;
import com.centurylink.mdw.model.system.Server;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@Path("/")
public class FilePanelService extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Map<String,TailWatcher> tailWatchers = new HashMap<>();

    @Override
    @Path("/{filePath}")
    public JSONObject get(String path, Map<String,String> headers)
            throws ServiceException, JSONException {

        String[] segments = getSegments(path);
        if (segments.length == 5) {
            Query query = getQuery(path, headers);
            URL requestUrl = getRequestUrl(headers);
            boolean forwarded = query.getBooleanFilter("forwarded");
            String hostParam = query.getFilter("host");
            // we match against request host and server host
            String requestHost = requestUrl.getHost();
            String serverHost = ApplicationContext.getServerHost();

            if (query.getFilter("path") != null) {
                try {
                    if (!forwarded && hostParam != null && !hostParam.equals(requestHost) && !hostParam.equals(serverHost)) {
                        // forward to appropriate server
                        HttpHelper helper = new HttpHelper(getForwardUrl(requestUrl, hostParam));
                        return new JSONObject(helper.get());
                    }
                    else {
                        java.nio.file.Path p = Paths.get(query.getFilter("path"));
                        if (!include(hostParam, p) || exclude(hostParam, p))
                            throw new ServiceException(ServiceException.FORBIDDEN, "Forbidden");
                        File file = p.toFile();
                        if (query.getBooleanFilter("download")) {
                            if (!file.isFile())
                                throw new ServiceException(ServiceException.NOT_FOUND, "Not found: " + p);
                            headers.put(Listener.METAINFO_DOWNLOAD_FORMAT, Listener.DOWNLOAD_FORMAT_FILE);
                            headers.put(Listener.METAINFO_DOWNLOAD_FILE, file.getPath());
                            return null;
                        }
                        else if (query.getFilter("grep") != null) {
                            // TODO grep
                            return new JSONObject();
                        }
                        else {
                            if (file.isFile()) {
                                // file view
                                FileInfo fileInfo = new FileInfo(file);
                                FileView fileView = new FileView(fileInfo, query);
                                if (query.getFilter("tail") != null) {
                                    tail(query, fileInfo.getLineCount() - 1);
                                }
                                return fileView.getJson();
                            }
                            else if (file.isDirectory()) {
                                Dir dir = new Dir(file, getExcludes(hostParam), true);
                                JSONObject json = new JSONObject();
                                json.put("info", dir.getJson());
                                return json;
                            }
                            else {
                                throw new ServiceException(ServiceException.NOT_FOUND, "Not found: " + p);
                            }
                        }
                    }
                }
                catch (IOException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                }
            }
            else {
                JSONObject json = new JSONObject();
                Set<String> hosts = getHosts();
                if (hosts.isEmpty() || forwarded) {
                    // non-host-specific dirs or forwarded request
                    if (!forwarded && PropertyManager.getListProperty("mdw." + PropertyNames.FILEPANEL_ROOT_DIRS) == null) {
                        throw new ServiceException(ServiceException.INTERNAL_ERROR,
                                "Missing config: mdw." + PropertyNames.FILEPANEL_ROOT_DIRS);
                    }
                    JSONArray dirsArr = new JSONArray();
                    for (Dir selfDir : getFilePanelDirs(hostParam))
                        dirsArr.put(selfDir.getJson());
                    json.put("dirs", dirsArr);
                }
                else {
                    // primary request for host-specific dirs
                    if (PropertyManager.getListProperty("mdw." + PropertyNames.FILEPANEL_ROOT_DIRS) != null) {
                        throw new ServiceException(ServiceException.INTERNAL_ERROR,
                                "FilePanel root.dirs may be specified either globally or per-server; not both.");
                    }
                    JSONArray hostsArr = new JSONArray();
                    for (String host : getHosts()) {
                        JSONObject hostJson = new JSONObject();
                        hostJson.put("name", host);
                        if (requestHost.equals(host) || serverHost.equals(host)) {
                            // no need to forward
                            List<Dir> hostDirs = getFilePanelDirs(host);
                            if (hostDirs != null) {
                                JSONArray hostDirsArr = new JSONArray();
                                for (Dir hostDir : hostDirs)
                                    hostDirsArr.put(hostDir.getJson());
                                hostJson.put("dirs", hostDirsArr);
                            }
                        }
                        else {
                            try {
                                HttpHelper httpHelper = new HttpHelper(getForwardUrl(requestUrl, host));
                                JSONObject hostDirsJson = new JSONObject(httpHelper.get());
                                hostJson.put("dirs", hostDirsJson.getJSONArray("dirs"));
                            }
                            catch (IOException ex) {
                                // don't let this blow up FilePanel altogether
                                logger.severeException("Error retrieving from: " + getServer(host), ex);;
                            }
                        }
                        hostsArr.put(hostJson);
                    }
                    json.put("hosts", hostsArr);
                }
                return json;
            }
        }
        else {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        }
    }

    private URL getForwardUrl(URL requestUrl, String host) throws MalformedURLException {
        return new URL(requestUrl.getProtocol() + "://" + getServer(host) + requestUrl.getPath()
        + (requestUrl.getQuery() == null ? "?" : "?" + requestUrl.getQuery() + "&") + "forwarded=true&host=" + host);
    }

    private void tail(Query query, int lastLine) throws IOException {
        boolean tailOn = query.getBooleanFilter("tail");
        TailWatcher watcher = null;
        String path = query.getFilter("path");
        synchronized(tailWatchers) {
            watcher = tailWatchers.get(path);
            if (tailOn) {
                if (watcher == null) {
                    watcher = new TailWatcher(Paths.get(path), lastLine);
                    tailWatchers.put(path, watcher);
                    watcher.watch();
                }
                watcher.refCount++;
            }
            else if (watcher != null) {
                watcher.refCount--;
                if (watcher.refCount < 1) {
                    watcher.stop();
                    tailWatchers.remove(path);
                }
            }
        }
    }

    // one server per host (despite possibly running on multiple ports)
    private static Map<String,Server> hostServers;
    public static Map<String,Server> getHostServers() {
        if (hostServers == null) {
            hostServers = new HashMap<>();
            for (Server server : ApplicationContext.getServerList()) {
                if (server.getConfig().containsKey("filepanel")) {
                    if (!hostServers.containsKey(server.getHost()))
                        hostServers.put(server.getHost(), server);
                }
            }
        }
        return hostServers;
    }

    public static Set<String> getHosts() {
        return getHostServers().keySet();
    }

    public static Server getServer(String host) {
        return getHostServers().get(host);
    }

    private List<Dir> getFilePanelDirs(String host) throws ServiceException {
        List<Dir> dirs = new ArrayList<>();
        List<java.nio.file.Path> paths = getRootPaths(host);
        if (paths != null) {
            for (java.nio.file.Path path : paths) {
                File file = path.toFile();
                if (file.isDirectory()) {
                    try {
                        dirs.add(new Dir(file, getExcludes(host), false));
                    }
                    catch (IOException ex) {
                        logger.severeException(ex.getMessage(), ex);
                    }
                }
                else {
                    String msg = "Warning: Configured FilePanel root is not a directory";
                    if (host != null)
                        msg += " (on host " + host + ")";
                    logger.warn(msg + ": " + file.getAbsolutePath());
                }
            }
        }
        return dirs;
    }

    private static Map<String,List<java.nio.file.Path>> rootPaths;
    private static List<java.nio.file.Path> getRootPaths(String host) throws ServiceException {
        if (rootPaths == null) {
            // per-server roots (or null key if not specified)
            rootPaths = new HashMap<>();
            List<String> rootDirs;
            if (host == null) {
                rootDirs = PropertyManager.getListProperty("mdw." + PropertyNames.FILEPANEL_ROOT_DIRS);
            }
            else {
                rootDirs = PropertyManager.getListProperty("mdw.servers." + host + "." + PropertyNames.FILEPANEL_ROOT_DIRS);
            }
            if (rootDirs != null) {
                for (String dir : rootDirs) {
                    java.nio.file.Path path = Paths.get(new File(dir).getPath());
                    List<java.nio.file.Path> paths = rootPaths.get(host);
                    if (paths == null) {
                        paths = new ArrayList<>();
                        rootPaths.put(host, paths);
                    }
                    paths.add(path);
                }
            }
        }
        return rootPaths.get(host);
    }

    private static Map<String,List<PathMatcher>> excludes;
    public static List<PathMatcher> getExcludes(String host) {
        if (excludes == null) {
            excludes = new HashMap<>();
            List<String> excludePatterns;
            if (host == null) {
                excludePatterns = PropertyManager.getListProperty("mdw." + PropertyNames.FILEPANEL_EXCLUDE_PATTERNS);
            }
            else {
                excludePatterns = PropertyManager.getListProperty("mdw.servers." + host + "." + PropertyNames.FILEPANEL_EXCLUDE_PATTERNS);
            }
            if (excludePatterns != null) {
                for (String excludePattern : excludePatterns) {
                    PathMatcher exclusion = FileSystems.getDefault().getPathMatcher("glob:" + excludePattern);
                    List<PathMatcher> exclusions = excludes.get(host);
                    if (exclusions == null) {
                        exclusions = new ArrayList<>();
                        excludes.put(host, exclusions);
                    }
                    exclusions.add(exclusion);
                }
            }
        }
        return excludes.get(host);
    }

    /**
     * Checks for matching exclude patterns.
     */
    private static boolean exclude(String host, java.nio.file.Path path) {
        List<PathMatcher> exclusions = getExcludes(host);
        if (exclusions == null && host != null) {
            exclusions = getExcludes(null); // check global config
        }
        if (exclusions != null) {
            for (PathMatcher matcher : exclusions) {
                if (matcher.matches(path))
                    return true;
            }
        }
        return false;
    }

    /**
     * Checks whether subpath of a designated root dir.
     * Root dirs must be configured per host or globally (not both).
     */
    private static boolean include(String host, java.nio.file.Path path) throws ServiceException {
        List<java.nio.file.Path> roots = getRootPaths(host);
        if (roots != null) {
            for (java.nio.file.Path rootPath : roots) {
                if (path.startsWith(rootPath))
                    return true;
            }
        }
        return false;
    }

}
