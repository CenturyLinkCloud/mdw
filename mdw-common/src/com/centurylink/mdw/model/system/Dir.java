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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Dir implements Jsonable {

    private File root;
    public File getRoot() { return root; }

    private File file;
    public File getFile() { return file; }

    private List<Dir> dirs = new ArrayList<>();
    public List<Dir> getDirs() { return dirs; }

    private List<FileInfo> files = new ArrayList<>();
    public List<FileInfo> getFiles() { return files; }

    private List<PathMatcher> excludeMatchers;

    public Dir(File root, File file) throws IOException {
        this(root, file, (List<PathMatcher>)null);
    }

    public Dir(File root, File file, String[] excludePatterns) throws IOException {
        this.root = root;
        this.file = file;
        if (excludePatterns != null) {
            excludeMatchers = new ArrayList<>();
            for (String excludePattern : excludePatterns)
                excludeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + excludePattern));
        }
        fill();
    }

    public Dir(File root, File file, List<PathMatcher> excludeMatchers) throws IOException {
        this.root = root;
        this.file = file;
        this.excludeMatchers = excludeMatchers;
        fill();
    }

    private void fill() throws IOException {
        for (File f : getFile().listFiles()) {
            if (excludeMatchers == null || !matches(excludeMatchers, Paths.get(f.getPath()))) {
                if (f.isDirectory()) {
                    dirs.add(new Dir(root, f, excludeMatchers));
                }
                else {
                    files.add(new FileInfo(f.getName(), isBinary(f)));
                }
            }
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        String path;
        if (file.equals(root))
            path = file.getPath().replace('\\', '/');
        else
            path = file.getName();
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
        return json;
    }

    private boolean matches(List<PathMatcher> matchers, Path path) {
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path))
                return true;
        }
        return false;
    }

    /**
     * TODO: validate
     */
    public static boolean isBinary(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        int size = in.available();
        if (size > 1024)
            size = 1024;
        byte[] data = new byte[size];
        in.read(data);
        in.close();

        int ascii = 0;
        int other = 0;

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            if (b < 0x09)
                return true;

            if (b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D )
                ascii++;
            else if (b >= 0x20 &&  b <= 0x7E)
                ascii++;
            else
                other++;
        }

        if (other == 0)
            return false;

        return 100 * other / (ascii + other) > 95;
    }

}
