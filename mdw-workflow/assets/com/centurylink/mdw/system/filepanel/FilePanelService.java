package com.centurylink.mdw.system.filepanel;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.system.Dir;
import com.centurylink.mdw.model.system.FileInfo;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.file.Grep;
import com.centurylink.mdw.util.file.Grep.LineMatches;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
public class FilePanelService extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final Map<String,TailWatcher> tailWatchers = new HashMap<>();

    @Override
    @Path("/{filePath}")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {

        String[] segments = getSegments(path);
        if (segments.length == 5) {
            Query query = getQuery(path, headers);

            if (query.getFilter("path") != null) {
                try {
                    java.nio.file.Path p = Paths.get(query.getFilter("path"));
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
                    else if (query.getFilter("grep") != null) {
                        Grep grep = new Grep(p, query.getFilter("glob"));
                        int limit = query.getIntFilter("limit");
                        grep.setLimit(limit < 0 ? 250 : limit);
                        Map<java.nio.file.Path,List<LineMatches>> matches = grep.find(query.getFilter("grep"));
                        JSONObject json = new JSONObject();
                        json.put("limit", grep.getLimit());
                        json.put("root", p.toAbsolutePath().toString().replace('\\', '/'));
                        JSONArray results = new JSONArray();
                        json.put("results", results);
                        int lines = 0;
                        for (java.nio.file.Path resPath : matches.keySet()) {
                            JSONObject fileObj = new JSONObject();
                            results.put(fileObj);
                            fileObj.put("file", resPath.toString().replace('\\', '/'));
                            JSONArray matchesArr = new JSONArray();
                            fileObj.put("lineMatches", matchesArr);
                            for (LineMatches lineMatches : matches.get(resPath)) {
                                matchesArr.put(lineMatches.getJson());
                                lines++;
                            }
                        }
                        json.put("count", lines);
                        return json;
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
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                }
            }
            else {
                JSONObject json = new JSONObject();
                if (PropertyManager.getListProperty("mdw." + PropertyNames.FILEPANEL_ROOT_DIRS) == null) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR,
                            "Missing config: mdw." + PropertyNames.FILEPANEL_ROOT_DIRS);
                }
                JSONArray dirsArr = new JSONArray();
                for (Dir selfDir : getFilePanelDirs())
                    dirsArr.put(selfDir.getJson());
                json.put("dirs", dirsArr);
                return json;
            }
        }
        else {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        }
    }

    private void tail(Query query, int lastLine) throws IOException {
        boolean tailOn = query.getBooleanFilter("tail");
        String path = query.getFilter("path");
        synchronized(tailWatchers) {
            TailWatcher watcher = tailWatchers.get(path);
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

    private List<Dir> getFilePanelDirs() {
        List<Dir> dirs = new ArrayList<>();
        List<java.nio.file.Path> paths = getRootPaths();
        if (paths != null) {
            for (java.nio.file.Path path : paths) {
                File file = path.toFile();
                if (file.isDirectory()) {
                    try {
                        dirs.add(new Dir(file, getExcludes(), false));
                    }
                    catch (IOException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
                else {
                    String msg = "Warning: Configured FilePanel root is not a directory";
                    logger.warn(msg + ": " + file.getAbsolutePath());
                }
            }
        }
        return dirs;
    }

    private static List<java.nio.file.Path> rootPaths;
    private static List<java.nio.file.Path> getRootPaths() {
        if (rootPaths == null) {
            // per-server roots (or null key if not specified)
            rootPaths = new ArrayList<>();
            List<String> rootDirs = PropertyManager.getListProperty("mdw." + PropertyNames.FILEPANEL_ROOT_DIRS);
            if (rootDirs != null) {
                for (String dir : rootDirs) {
                    rootPaths.add(Paths.get(new File(dir).getPath()));
                }
            }
        }
        return rootPaths;
    }

    private static List<PathMatcher> excludes;
    public static List<PathMatcher> getExcludes() {
        if (excludes == null) {
            excludes = new ArrayList<>();
            List<String> excludePatterns = PropertyManager.getListProperty("mdw." + PropertyNames.FILEPANEL_EXCLUDE_PATTERNS);
            if (excludePatterns != null) {
                for (String excludePattern : excludePatterns) {
                    PathMatcher exclusion = FileSystems.getDefault().getPathMatcher("glob:" + excludePattern);
                    excludes.add(exclusion);
                }
            }
        }
        return excludes;
    }

    /**
     * Checks for matching exclude patterns.
     */
    private static boolean exclude(java.nio.file.Path path) {
        List<PathMatcher> exclusions = getExcludes();
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
    private static boolean include(java.nio.file.Path path) {
        List<java.nio.file.Path> roots = getRootPaths();
        if (roots != null) {
            for (java.nio.file.Path rootPath : roots) {
                if (path.startsWith(rootPath))
                    return true;
            }
        }
        return false;
    }

}
