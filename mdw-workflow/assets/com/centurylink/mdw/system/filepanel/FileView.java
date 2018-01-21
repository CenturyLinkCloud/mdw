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

    private int lineIndex;
    private StringBuilder lineBuffer;
    private int bufferSize;   // buffer size (TODO: options)
    private int bufferLength;  // actual number of lines in buffer
    private int bufferStart;

    private int lineCount;

    public FileView(FileInfo info, Query query) throws IOException {
        this.info = info;
        this.query = query;
        lineIndex = query.getIntFilter("lineIndex");
        if (lineIndex == -1)
            lineIndex = 0;
        bufferSize = query.getIntFilter("bufferSize");
        if (bufferSize == -1)
            bufferSize = 1000;

        lineBuffer = new StringBuilder();  // TODO: presize?
        bufferLength = 0;

        Path path = Paths.get(info.getPath());
        if (info.isBinary()) {
            lineBuffer.append("Binary file: " + info.getName());
        }
        else {
//            // old-fashioned
//            File file = path.toFile();
//            int count = 0;
//            try (LineNumberReader reader = new LineNumberReader(new FileReader(file))) {
//                while (reader.readLine() != null) {}
//                count = reader.getLineNumber();
//            }
//            info.setLineCount(count);
//
//            try (LineNumberReader reader = new LineNumberReader(new FileReader(file))) {
//                int firstLine = getBufferFirstLine() + 1;
//                int lastLine = getBufferLastLine() + 1;
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    if (reader.getLineNumber() >= firstLine
//                         && reader.getLineNumber() <= lastLine) {
//                        lineBuffer.append(applyMask(line)).append("\n");
//                    }
//                }
//                info.setLineCount(reader.getLineNumber());
//            }


            // streams
//            try (Stream<String> stream = Files.lines(path)) {
//                info.setLineCount((int)stream.count());
//                bufferStart = getBufferFirstLine();
//                bufferEnd = getBufferLastLine();
//                int limit = bufferEnd - bufferStart;
//                try (Stream<String> stream2 = Files.lines(path)) {
//                    stream2.skip(bufferStart).limit(limit).forEachOrdered(line -> {
//                        lineBuffer.append(applyMask(line)).append("\n");
//                        bufferLength++;
//                    });
//                }
//            }
//            catch (UncheckedIOException ex) {
//                throw ex.getCause();
//            }

            // one-pass
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

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        JSONObject infoJson = info.getJson();
        infoJson.put("isFile", true);
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
