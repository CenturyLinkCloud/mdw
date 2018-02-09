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

        lineBuffer = new StringBuilder();  // TODO: presize?

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

            bufferLength = 0;

            search = query.getFilter("search");
            if (search != null) {
                // find index of first match (search from next line after current lineIndex)
                int searchIndex = query.getIntFilter("searchIndex");
                if (searchIndex == -1)
                    searchIndex = 0;
                this.searchIndex = search(searchIndex, query.getBooleanFilter("backward"));
                if (this.searchIndex >= 0)
                    this.lineIndex = this.searchIndex;
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
     * For tail mode.
     */
    public FileView(FileInfo info, int lastLine) throws IOException {
        this.info = info;
        path = Paths.get(info.getPath());
        bufferStart = lastLine;
        lineBuffer = new StringBuilder();
        try (Stream<String> stream = Files.lines(path)) {
            // repeat last line in case it changed
            stream.skip(lastLine).forEachOrdered(line -> {
                lineBuffer.append(applyMask(line)).append("\n");
                bufferLength++;
                lineCount++;
            });
            info.setLineCount(lastLine + lineCount);
        }
        catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    /**
     * Search pass locates line index of first match.
     */
    private int search(int startLine, boolean backward) throws IOException {
        search = search.toLowerCase();
        try (Stream<String> stream = Files.lines(path)) {
            if (backward) {
                int idx = -1;
                if (startLine > 0)
                    idx = searchTo(startLine - 1, true);
                if (idx < 0)
                    idx = searchFrom(startLine, true);
                return idx;
            }
            else {
                int idx = searchFrom(startLine);
                if (idx < 0) {
                    // wrap search
                    idx = searchTo(startLine - 1);
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
    private int searchFrom(int startLine) throws IOException {
        return searchFrom(startLine, false);
    }
    private int searchFrom(int startLine, boolean findLast) throws IOException {
        searchIndex = lastIndex = -1;
        try (Stream<String> stream = Files.lines(path)) {
            Stream<String> s = stream.skip(startLine).filter(line -> {
                searchIndex++;
                boolean found = line.toLowerCase().indexOf(search) >= 0;
                if (found && findLast)
                    lastIndex = searchIndex + startLine;
                return found;
            });
            if (findLast) {
                s.forEachOrdered(l -> {});
                return lastIndex;
            }
            else {
                return s.findFirst().isPresent() ? searchIndex + startLine : -1;
            }
        }
    }

    private int searchTo(int endLine) throws IOException {
        return searchTo(endLine, false);
    }
    private int searchTo(int endLine, boolean findLast) throws IOException {
        searchIndex = lastIndex = -1;
        try (Stream<String> stream = Files.lines(path)) {
            Stream<String> s = stream.limit(endLine).filter(line -> {
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
            if (line.trim().startsWith(masked)) {
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
            maskedLines.add("password:");
            // only global configuration supported at present
            String maskedLinesProp = PropertyManager.getProperty("mdw." + PropertyNames.FILEPANEL_MASKED_LINES);
            if (maskedLinesProp != null) {
                for (String maskedLine : maskedLinesProp.trim().split("\\s*,\\s*")) {
                    maskedLines.add(maskedLine);
                }
            }
        }
        return maskedLines;
    }


}
