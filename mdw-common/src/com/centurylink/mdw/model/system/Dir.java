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
package com.centurylink.mdw.model.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Dir implements Jsonable {

    private String name;
    public String getName() { return name; }

    private String path;
    public String getPath() { return path; }

    private List<Dir> dirs = new ArrayList<>();
    public List<Dir> getDirs() { return dirs; }

    private List<FileInfo> files = new ArrayList<>();
    public List<FileInfo> getFiles() { return files; }

    private List<PathMatcher> excludeMatchers;

    private Instant modified;
    public Instant getModified() { return modified; }

    public Dir(File file) throws IOException {
        this(file, (List<PathMatcher>)null);
    }

    public Dir(File file, String[] excludePatterns) throws IOException {
        if (!file.isDirectory())
            throw new FileNotFoundException("Directory not found: " + file.getAbsolutePath());
        this.name = file.getName();
        this.path = file.getPath().replace('\\', '/');
        if (excludePatterns != null) {
            excludeMatchers = new ArrayList<>();
            for (String excludePattern : excludePatterns)
                excludeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + excludePattern));
        }
        this.modified = Instant.ofEpochMilli(file.lastModified());
        fill();
    }

    public Dir(File file, List<PathMatcher> excludeMatchers) throws IOException {
        this.name = file.getName();
        this.path = file.getPath().replace('\\', '/');
        this.excludeMatchers = excludeMatchers;
        this.modified = Instant.ofEpochMilli(file.lastModified());
        fill();
    }

    private void fill() throws IOException {
        for (File f : new File(path).listFiles()) {
            if (excludeMatchers == null || !matches(excludeMatchers, Paths.get(f.getPath()))) {
                if (f.isDirectory()) {
                    dirs.add(new Dir(f, excludeMatchers));
                }
                else {
                    files.add(new FileInfo(f));
                }
            }
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("path", path);
        if (!dirs.isEmpty()) {
            JSONArray dirsArr = new JSONArray();
            for (Dir d : this.dirs) {
                dirsArr.put(d.getJson());
            }
            json.put("dirs", dirsArr);
        }
        if (!files.isEmpty()) {
            JSONArray filesArr = new JSONArray();
            for (FileInfo f : this.files) {
                filesArr.put(f.getJson());
            }
            json.put("files", filesArr);
        }
        if (modified != null)
            json.put("modified", modified.toString());
        return json;
    }

    private boolean matches(List<PathMatcher> matchers, Path path) {
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path))
                return true;
        }
        return false;
    }

}
