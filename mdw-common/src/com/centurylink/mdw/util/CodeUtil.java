/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.util;

import java.io.File;
import java.io.IOException;

import com.centurylink.mdw.util.file.FileHelper;

public class CodeUtil {

    public static final String HEADER_COMMENT = "/*\r\n * Copyright (C) 2017 CenturyLink, Inc.\r\n *\r\n * Licensed under the Apache License, Version 2.0 (the \"License\");\r\n * you may not use this file except in compliance with the License.\r\n * You may obtain a copy of the License at\r\n *\r\n *      http://www.apache.org/licenses/LICENSE-2.0\r\n *\r\n * Unless required by applicable law or agreed to in writing, software\r\n * distributed under the License is distributed on an \"AS IS\" BASIS,\r\n * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\r\n * See the License for the specific language governing permissions and\r\n * limitations under the License.\r\n */\r\n";

    /**
     * @param args h c:/workspaces/mdw (for adding standard header comments to *.java)
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("arguments: h fileSystemRoot");
        }
        else {
            String rootPath = args[1];
            File rootDir = new File(rootPath);
            if (!rootDir.exists() || !rootDir.isDirectory()) {
                System.err.println("Cannot find root directory: " + rootDir);
            }
            else {
                try {
                    CodeUtil codeUtil = new CodeUtil();
                    codeUtil.count = 0;
                    if ("h".equals(args[0])) {
                        System.out.println("Replacing Headers Comments...");
                        codeUtil.addHeaderComments(rootDir);
                        System.out.println("Replaced Header Comment in " + codeUtil.count + " files");
                    }
                    else if ("a".equals(args[0])) {
                        System.out.println("Removing Author Tags...");
                        codeUtil.removeAuthorTags(rootDir);
                        System.out.println("Removed Author Tags in " + codeUtil.count + " files");
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private int count;

    public void addHeaderComments(File rootDir) throws IOException {
        for (File file : rootDir.listFiles()) {
            if (file.isDirectory()) {
                addHeaderComments(file);
            }
            else if (file.getName().endsWith(".java")) {
                String contents = FileHelper.readFromFile(file.getAbsolutePath());
                if (!contents.startsWith(HEADER_COMMENT + "\npackage ") && !contents.startsWith(HEADER_COMMENT + "\r\npackage ")) {
                    int idx = contents.startsWith("package ") ? 0 : contents.indexOf("\npackage ");
                    if (idx >= 0) {
                        contents = HEADER_COMMENT + contents.substring(idx == 0 ? 0 : idx+1);
                        FileHelper.writeToFile(file.getAbsolutePath(), contents, false);
                        System.out.println(file);
                        count++;
                    }
                }
            }
        }
    }

    public void removeAuthorTags(File rootDir) throws IOException {
        for (File file : rootDir.listFiles()) {
            if (file.isDirectory()) {
                removeAuthorTags(file);
            }
            else if (file.getName().endsWith(".java")) {
                String contents = FileHelper.readFromFile(file.getAbsolutePath());
                int idx = contents.indexOf("\r\n * @author ");
                if (idx > 0) {
                    int lineEnd = contents.indexOf("\n", idx + 2);
                    contents = contents.substring(0, idx + 2) + contents.substring(lineEnd + 1);
                    FileHelper.writeToFile(file.getAbsolutePath(), contents, false);
                    System.out.println(file);
                    count++;
                }
            }
        }
    }
}
