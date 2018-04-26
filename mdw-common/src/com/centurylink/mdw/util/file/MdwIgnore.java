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
package com.centurylink.mdw.util.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Currently only supports a straight dir/file list (no wildcards or subpaths).
 */
public class MdwIgnore {

    public static final String MDW_IGNORE = ".mdwignore";

    List<File> excludes = new ArrayList<>();

    public MdwIgnore(File dir) throws IOException {
        File ignoreFile = new File(dir + "/" + MDW_IGNORE);
        if (ignoreFile.exists()) {
            String list = new String(Files.readAllBytes(Paths.get(ignoreFile.getPath()))).trim();
            for (String line : list.split("\n")) {
                line = line.trim();
                if (!line.startsWith("#"))
                    excludes.add(new File(dir + "/" + line));
            }
        }
    }

    public boolean isIgnore(File file) {
        return excludes.contains(file);
    }

}
