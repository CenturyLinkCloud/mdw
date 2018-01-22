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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class FileInfo implements Jsonable {

    private String name;
    public String getName() { return name; }

    private String path;
    public String getPath() { return path; }

    private boolean binary;
    public boolean isBinary() { return binary; }

    private Instant modified;
    public Instant getModified() { return modified; }

    private long size;
    public long getSize() { return size; }

    private int lineCount = -1; // unknown
    public int getLineCount() { return lineCount; }
    public void setLineCount(int count) { this.lineCount = count; }

    /**
     * Details include permissions and ownership.
     */
    private boolean details;
    public boolean isDetails() { return details; }

    private String permissions;
    public String getPermissions() { return permissions; }

    private String owner;
    public String getOwner() { return owner; }

    private String group;
    public String getGroup() { return group; }

    public FileInfo(File file) throws IOException {
        this(file, false);
    }

    public FileInfo(File file, boolean details) throws IOException {
        if (!file.isFile())
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        this.name = file.getName();
        this.path = file.getPath().replace('\\', '/');
        this.binary = isBinary(file);
        this.modified = Instant.ofEpochMilli(file.lastModified());
        this.size = file.length();
        if (details)
            addDetails();
    }

    private void addDetails() throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Path path = Paths.get(this.path);
            PosixFileAttributes attrs = Files.getFileAttributeView(path, PosixFileAttributeView.class).readAttributes();
            permissions = String.format("%s%n", PosixFilePermissions.toString(attrs.permissions()));
            UserPrincipal ownerPrincipal = attrs.owner();
            owner = ownerPrincipal.toString();
            UserPrincipal groupPrincipal = attrs.group();
            group = groupPrincipal.toString();
        }
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("path", path);
        if (binary)
            json.put("binary", true);
        if (modified != null)
            json.put("modified", modified.toString());
        if (size > 0)
            json.put("size", size);
        if (lineCount >= 0)
            json.put("lineCount", lineCount);
        if (permissions != null)
            json.put("permissions", permissions);
        if (owner != null)
            json.put("owner", owner);
        if (group != null)
            json.put("group", group);
        return json;
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
