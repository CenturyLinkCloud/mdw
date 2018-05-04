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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
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

    private String absolutePath;
    public String getAbsolutePath() { return absolutePath; }

    private List<Dir> dirs = new ArrayList<>();
    public List<Dir> getDirs() { return dirs; }

    private List<FileInfo> files = new ArrayList<>();
    public List<FileInfo> getFiles() { return files; }

    private List<PathMatcher> excludeMatchers;

    private Instant modified;
    public Instant getModified() { return modified; }

    private int numLinks;
    public int getNumLinks() { return numLinks; }

    /**
     * Details include permissions, ownership and size (but not on Windows).
     */
    private boolean details;
    public boolean isDetails() { return details; }

    private String permissions;
    public String getPermissions() { return permissions; }

    private String owner;
    public String getOwner() { return owner; }

    private String group;
    public String getGroup() { return group; }

    private long size;
    public long getSize() { return size; }

    public Dir(File file, List<PathMatcher> excludeMatchers, boolean details) throws IOException {
        if (!file.isDirectory())
            throw new FileNotFoundException("Directory not found: " + file.getAbsolutePath());
        this.name = file.getName();
        this.path = file.getPath().replace('\\', '/');
        this.excludeMatchers = excludeMatchers;
        this.modified = Instant.ofEpochMilli(file.lastModified());
        this.details = details;
        if (details)
            addDetails();
        fill();
    }

    /**
     * Non-recursive constructor.
     */
    public Dir(File file) throws IOException {
        if (!file.isDirectory())
            throw new FileNotFoundException("Directory not found: " + file.getAbsolutePath());
        this.name = file.getName();
        this.path = file.getPath().replace('\\', '/');
        this.modified = Instant.ofEpochMilli(file.lastModified());
    }

    private void fill() throws IOException {
        for (File f : new File(path).listFiles()) {
            if (excludeMatchers == null || !matches(excludeMatchers, Paths.get(f.getPath()))) {
                if (f.isDirectory()) {
                    if (details) {
                        Dir dir = new Dir(f);
                        dir.addDetails();
                        dirs.add(dir);
                    }
                    else {
                        // dir tree (recursive)
                        dirs.add(new Dir(f, excludeMatchers, details));
                    }
                }
                else if (f.canRead()) {
                    files.add(new FileInfo(f, details));
                }
            }
        }
    }

    private void addDetails() throws IOException {
        Path path = Paths.get(this.path);
        this.absolutePath = path.toAbsolutePath().toString().replace('\\', '/');
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            PosixFileAttributes attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class).readAttributes();
            permissions = PosixFilePermissions.toString(attrs.permissions());
            UserPrincipal ownerPrincipal = attrs.owner();
            owner = ownerPrincipal.toString();
            UserPrincipal groupPrincipal = attrs.group();
            group = groupPrincipal.toString();
            size = path.toFile().length();
            numLinks = path.toFile().list().length + 2;
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("path", path);
        if (absolutePath != null) {
            json.put("absolutePath", absolutePath);
        }
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
        if (size > 0)
            json.put("size", size);
        if (permissions != null)
            json.put("permissions", permissions);
        if (owner != null)
            json.put("owner", owner);
        if (group != null)
            json.put("group", group);
        if (numLinks > 0)
            json.put("numLinks", numLinks);
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
