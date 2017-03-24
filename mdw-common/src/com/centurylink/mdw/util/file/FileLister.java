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
package com.centurylink.mdw.util.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileLister {

    File dir;
    List<File> fileList;

    public FileLister(File dir) {
        this.dir = dir;
    }

    public List<File> list() {
        fileList = new ArrayList<File>();
        add(dir);
        return fileList;
    }

    private void add(File node) {
        if (!node.getName().equals(".metadata")) { // annoying Dimensions files
            if (node.isDirectory()) {
                if (!node.equals(dir))
                  fileList.add(node);
                for (File sub : node.listFiles())
                    add(sub);
            }
            else {
                fileList.add(node);
            }
        }
    }
}
