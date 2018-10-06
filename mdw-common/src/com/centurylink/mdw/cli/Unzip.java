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
package com.centurylink.mdw.cli;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Bare min impl to support CLI without dependencies.
 * Suited for asset package imports in the way it handles overwriting.
 */
public class Unzip implements Operation {

    private static final int BUFFER_KB = 16;

    private File zipFile;
    private File destDir;
    private boolean overwrite;
    private Predicate<String> optionsCheck;

    public Unzip(File zipFile, File destDir) {
        this(zipFile, destDir, false);
    }

    public Unzip(File zipFile, File destDir, boolean overwrite) {
        this(zipFile, destDir, overwrite, null);
    }

    public Unzip(File zipFile, File destDir, boolean overwrite, Predicate<String> optionsCheck) {
        this.zipFile = zipFile;
        this.destDir = destDir;
        this.overwrite = overwrite;
        this.optionsCheck = optionsCheck;
    }

    public Unzip run(ProgressMonitor... progressMonitors) throws IOException {
        if (!destDir.exists() || !destDir.isDirectory())
            throw new IOException("Destination directory does not exist: " + destDir);
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            List<String> cleanedUpDirEntries = new ArrayList<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                boolean overwriteEntry = overwrite;
                int curlyStart = entryName.indexOf("{{");
                boolean hasOption = false;
                if (curlyStart >= 0 && entryName.length() > curlyStart + 2) {
                    int curlyEnd = entryName.indexOf("}}", curlyStart + 2);
                    if (curlyEnd >= curlyStart) {
                        hasOption = true;
                        String option = entryName.substring(curlyStart + 2, curlyEnd);
                        // if entryName consists only of option then its dir will have already been created
                        if (entryName.equals("{{" + option + "}}/") || optionsCheck == null || !optionsCheck.test(option)) {
                            continue;
                        }
                        else {
                            entryName = entryName.substring(0, curlyStart) + entryName.substring(curlyEnd + 3);
                            overwriteEntry = true; // allow options to overwrite previously-created templated files
                        }
                    }
                }
                String outpath = destDir + "/" + entryName;
                File outfile = new File(outpath);
                if (outfile.exists() && !overwriteEntry)
                    throw new IOException("Destination already exists: " + outfile.getAbsolutePath());
                if (entry.isDirectory()) {
                    Files.createDirectories(Paths.get(outfile.getPath()));
                }
                else {
                    if (!hasOption) {
                        // delete parent directory's files if any (only once per dir)
                        if (overwriteEntry && entryName.contains("/")) {
                            String parentEntry = entryName.substring(0, entryName.lastIndexOf('/') + 1);
                            if (!cleanedUpDirEntries.contains(parentEntry)) {
                                deleteFiles(outfile.getParentFile());
                                cleanedUpDirEntries.add(parentEntry);
                            }
                        }
                    }
                    InputStream is = null;
                    OutputStream os = null;
                    try {
                        is = zip.getInputStream(entry);
                        os = new FileOutputStream(outfile);
                        int read = 0;
                        byte[] bytes = new byte[BUFFER_KB * 1024];
                        while((read = is.read(bytes)) != -1)
                            os.write(bytes, 0, read);
                    }
                    finally {
                        if (is != null)
                            is.close();
                        if (os != null)
                          os.close();
                    }
                }
            }
        }

        return this;
    }


    private void deleteFiles(File dir) {
        for (File child : dir.listFiles()) {
            if (child.isFile())
                child.delete();
        }
    }
}
