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
    private int bufferLines;

    public FileView(FileInfo info, Query query) throws IOException {
        this.info = info;
        this.query = query;
        lineIndex = query.getIntFilter("lineIndex");
        if (lineIndex == -1)
            lineIndex = 0;
        bufferLines = query.getIntFilter("bufferLines");
        if (bufferLines == -1)
            bufferLines = 1000;

        lineBuffer = new StringBuilder();  // TODO: presize?

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
            try (Stream<String> stream = Files.lines(path)) {
                info.setLineCount((int)stream.count());
                int firstLine = getBufferFirstLine();
                int lastLine = getBufferLastLine();
                try (Stream<String> stream2 = Files.lines(path)) {
                    stream2.skip(firstLine).limit(lastLine - firstLine + 1).forEachOrdered(line -> {
                        lineBuffer.append(applyMask(line)).append("\n");
                    });
                }
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
        json.put("lines", lineBuffer.toString());
        return json;
    }

    public int getBufferFirstLine()
    {
      int firstLine = lineIndex - bufferLines/2;

      if (lineIndex + bufferLines/2 > info.getLineCount() - 1)
        firstLine = info.getLineCount() - bufferLines - 1;
      if (firstLine < 0)
        firstLine = 0;

      return firstLine;
    }

    public int getBufferLastLine()
    {
      int lastLine = getBufferFirstLine() + bufferLines;
      if (lastLine > info.getLineCount() - 1)
        lastLine = info.getLineCount() - 1;
      if (lastLine < 0)
        lastLine = 0;

      return lastLine;
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
