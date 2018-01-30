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

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.system.Dir;
import com.centurylink.mdw.model.system.FileInfo;
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
                java.nio.file.Path p = Paths.get(query.getFilter("path"));
                try {
                    if (!include(p) || exclude(p))
                        throw new ServiceException(ServiceException.FORBIDDEN, "Forbidden");
                    File file = p.toFile();
                    if (query.getBooleanFilter("download")) {
                        if (!file.isFile())
                            throw new ServiceException(ServiceException.NOT_FOUND, "Not found: " + p);
                        headers.put(Listener.METAINFO_DOWNLOAD_FORMAT, Listener.DOWNLOAD_FORMAT_FILE);
                        headers.put(Listener.METAINFO_DOWNLOAD_FILE, file.getPath());
                        return null;
                    }
                    else if (query.getFilter("tail") != null) {
                        boolean tailOn = query.getBooleanFilter("tail");
                        String absPath = p.toAbsolutePath().toString();
                        TailWatcher watcher;
                        synchronized(tailWatchers) {
                            watcher = tailWatchers.get(absPath);
                            if (tailOn) {
                                if (watcher == null) {
                                    watcher = new TailWatcher(p, query.getIntFilter("lastLine"));
                                    tailWatchers.put(absPath, watcher);
                                    watcher.watch();
                                }
                                else {
                                    watcher.refCount++;
                                }
                            }
                            else {
                                watcher.refCount--;
                                if (watcher.refCount < 1) {
                                    watcher.done = true;
                                    tailWatchers.remove(absPath);
                                }
                            }
                        }
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
                            return fileView.getJson();
                        }
                        else if (file.isDirectory()) {
                            Dir dir = new Dir(file, getExcludes(), true);
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
                // root dirs
                List<Dir> dirs = getFilePanelDirs();
                JSONObject json = new JSONObject();
                JSONArray dirsArr = new JSONArray();
                for (Dir dir : dirs)
                    dirsArr.put(dir.getJson());
                json.put("dirs", dirsArr);
                return json;
            }
        }
        else {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        }
    }

    private List<Dir> getFilePanelDirs() throws ServiceException {
        List<Dir> dirs = new ArrayList<>();
        for (java.nio.file.Path path : getRootPaths()) {
            File file = path.toFile();
            if (file.isDirectory()) {
                try {
                    dirs.add(new Dir(file, getExcludes(), false));
                }
                catch (IOException ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
        return dirs;
    }

    private static List<java.nio.file.Path> rootPaths;
    private static List<java.nio.file.Path> getRootPaths() throws ServiceException {
        if (rootPaths == null) {
            String rootDirsProp = PropertyManager.getProperty(PropertyNames.FILEPANEL_ROOT_DIRS);
            if (rootDirsProp == null)
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Missing property: " + PropertyNames.FILEPANEL_ROOT_DIRS);
            rootPaths = new ArrayList<>();
            for (String dir : rootDirsProp.trim().split("\\s*,\\s*")) {
                rootPaths.add(Paths.get(new File(dir).getPath()));
            }
        }
        return rootPaths;
    }

    private static List<PathMatcher> excludes;
    public static List<PathMatcher> getExcludes() {
        if (excludes == null) {
            excludes = new ArrayList<>();
            String excludePatterns = PropertyManager.getProperty(PropertyNames.FILEPANEL_EXCLUDE_PATTERNS);
            if (excludePatterns != null) {
                for (String excludePattern : excludePatterns.trim().split("\\s*,\\s*")) {
                    excludes.add(FileSystems.getDefault().getPathMatcher("glob:" + excludePattern));
                }
            }
        }
        return excludes;
    }

    /**
     * Checks for matching exclude patterns.
     */
    private static boolean exclude(java.nio.file.Path path) {
        for (PathMatcher matcher : getExcludes()) {
            if (matcher.matches(path))
                return true;
        }
        return false;
    }

    /**
     * Checks whether subpath of a designated root dir.
     */
    private static boolean include(java.nio.file.Path path) throws ServiceException {
        for (java.nio.file.Path rootPath : getRootPaths()) {
            if (path.startsWith(rootPath))
                return true;
        }
        return false;
    }
}
