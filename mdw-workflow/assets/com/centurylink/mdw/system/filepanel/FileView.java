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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.system.FileInfo;

/**
 * Window into a file's current scroll buffer.
 */
public class FileView implements Jsonable {

    private FileInfo info;
    public FileInfo getInfo() { return info; }

    private Query query;
    public Query getQuery() { return query; }

    private Path path;

    private int lineIndex;
    private StringBuilder lineBuffer;
    private int bufferSize;   // buffer size (TODO: options)
    private int bufferLength;  // actual number of lines in buffer
    private int bufferStart;

    private String search;

    private int lineCount;

    public FileView(FileInfo info, Query query) throws IOException {
        this.info = info;
        this.query = query;

        if (info.isBinary()) {
            lineBuffer.append("Binary file: " + info.getName());
        }
        else {
            path = Paths.get(info.getPath());
            lineIndex = query.getIntFilter("lineIndex");
            if (lineIndex == -1)
                lineIndex = 0;
            bufferSize = query.getIntFilter("bufferSize");
            if (bufferSize == -1)
                bufferSize = 1000;

            lineBuffer = new StringBuilder();  // TODO: presize?
            bufferLength = 0;

            search = query.getFilter("search");
            if (search != null) {
                // find lineIndex of first match
                lineIndex = searchIndex = search(lineIndex, query.getBooleanFilter("backward"));
            }

            // one-pass forward
            try (Stream<String> stream = Files.lines(path)) {
                if (lineIndex > 0) {
                    bufferStart = lineIndex - bufferSize/2;
                    if (bufferStart < 0)
                        bufferStart = 0;
                }
                lineCount = bufferStart;
                stream.skip(bufferStart).forEachOrdered(line -> {
                    if (bufferLength < bufferSize) {
                        lineBuffer.append(applyMask(line)).append("\n");
                        bufferLength++;
                    }
                    lineCount++;
                });
                info.setLineCount(lineCount);
            }
            catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        }
    }


    /**
     * Search pass locates line index of first match.
     */
    private int search(int start, boolean backward) throws IOException {
        search = search.toLowerCase();
        try (Stream<String> stream = Files.lines(path)) {
            if (backward) {
                int idx = searchTo(start - 1, true);
                if (idx < 0) {
                    idx = searchFrom(start, true);
                }
                return idx;
            }
            else {
                int idx = searchFrom(start);
                if (idx < 0) {
                    // wrap search
                    idx = searchTo(start - 1);
                }
                return idx;
            }
        }
        catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    private int searchIndex;
    private int lastIndex;
    private int searchFrom(int start) throws IOException {
        return searchFrom(start, false);
    }
    private int searchFrom(int start, boolean findLast) throws IOException {
        searchIndex = lastIndex = -1;
        try (Stream<String> stream = Files.lines(path)) {
            Stream<String> s = stream.skip(start).filter(line -> {
                searchIndex++;
                boolean found = line.toLowerCase().indexOf(search) >= 0;
                if (found && findLast)
                    lastIndex = searchIndex;
                return found;
            });
            if (findLast) {
                s.forEachOrdered(l -> {});
                return lastIndex;
            }
            else {
                return s.findFirst().isPresent() ? searchIndex + start : -1;
            }
        }
    }

    private int searchTo(int end) throws IOException {
        return searchTo(end, false);
    }
    private int searchTo(int end, boolean findLast) throws IOException {
        searchIndex = lastIndex = -1;
        try (Stream<String> stream = Files.lines(path)) {
            Stream<String> s = stream.limit(lineIndex).filter(line -> {
                searchIndex++;
                boolean found = line.toLowerCase().indexOf(search) >= 0;
                if (found && findLast)
                    lastIndex = searchIndex;
                return found;
            });
            if (findLast) {
                s.forEachOrdered(l -> {});
                return lastIndex;
            }
            return s.findFirst().isPresent() ? searchIndex : -1;
        }
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        JSONObject infoJson = info.getJson();
        infoJson.put("isFile", true);
        if (search != null)
            infoJson.put("searchIndex", searchIndex);
        json.put("info", infoJson);
        JSONObject bufferJson = new JSONObject();
        bufferJson.put("lines", lineBuffer.toString());
        bufferJson.put("length", bufferLength);
        bufferJson.put("start", bufferStart);
        json.put("buffer", bufferJson);
        return json;
    }

    private String applyMask(String line) {
        for (String masked : getMaskedLines()) {
            if (line.startsWith(masked)) {
                int lineLen = line.length();
                line = line.substring(0, masked.length());
                for (int i = 0; i < lineLen - masked.length(); i++)
                  line += "*";
            }
        }
        return line;
    }

    private static List<String> maskedLines;
    private static List<String> getMaskedLines() {
        if (maskedLines == null) {
            maskedLines = new ArrayList<>();
            maskedLines.add("mdw.database.password=");
            maskedLines.add("LDAP-AppPassword=");
            String maskedLinesProp = PropertyManager.getProperty(PropertyNames.FILEPANEL_MASKED_LINES);
            if (maskedLinesProp != null) {
                for (String maskedLine : maskedLinesProp.trim().split("\\s*,\\s*")) {
                    maskedLines.add(maskedLine);
                }
            }
        }
        return maskedLines;
    }


}
