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
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Query query = new Query(path, headers);
            if (query.getFilter("path") != null) {
                String host = query.getFilter("server");
                // TODO: check and forward to appropriate server
                java.nio.file.Path p = Paths.get(query.getFilter("path"));
                try {
                    if (!include(host, p) || exclude(host, p))
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
                            Dir dir = new Dir(file, getExcludes(host), true);
                            JSONObject json = new JSONObject();
                            json.put("info", dir.getJson());
                            return json;
                        }
                        else {
                            throw new ServiceException(ServiceException.NOT_FOUND, "Not found: " + p);
                        }
                    }
                }
                catch (IOException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
                }
            }
            else {
                JSONObject json = new JSONObject();
                // non-host-specific dirs
                List<Dir> dirs = getFilePanelDirs(null);
                if (dirs != null) {
                    JSONArray dirsArr = new JSONArray();
                    for (Dir dir : dirs)
                        dirsArr.put(dir.getJson());
                    json.put("dirs", dirsArr);
                }
                // host-specific dirs
                List<String> hosts = getHosts();
                if (!hosts.isEmpty()) {
                    JSONArray hostsArr = new JSONArray();
                    for (String host : getHosts()) {
                        // TODO: collect dirs from host services
                        JSONObject hostJson = new JSONObject();
                        hostJson.put("name", host);
                        List<Dir> hostDirs = getFilePanelDirs(host);
                        if (hostDirs != null) {
                            if (dirs != null) {
                                throw new ServiceException(ServiceException.INTERNAL_ERROR,
                                        "FilePanel root.dirs may be specified either globally or per-server; not both.");
                            }
                            JSONArray hostDirsArr = new JSONArray();
                            for (Dir hostDir : hostDirs)
                                hostDirsArr.put(hostDir.getJson());
                            hostJson.put("dirs", hostDirsArr);
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

    private static List<String> hosts;
    public static List<String> getHosts() {
        if (hosts == null) {
            hosts = new ArrayList<>();
            for (Server server : ApplicationContext.getServerList()) {
                if (server.getConfig().containsKey("filepanel"))
                    hosts.add(server.getHost());
            }
        }
        return hosts;
    }

    private List<Dir> getFilePanelDirs(String host) throws ServiceException {
        List<Dir> dirs = null;
        List<java.nio.file.Path> paths = getRootPaths(host);
        if (paths != null) {
            dirs = new ArrayList<>();
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
