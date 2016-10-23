/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.io.File;
import java.io.IOException;

import com.centurylink.mdw.util.file.FileHelper;

public class CodeUtil {

    public static final String HEADER_COMMENT = "/**\r\n * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.\r\n */\r\n";

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
