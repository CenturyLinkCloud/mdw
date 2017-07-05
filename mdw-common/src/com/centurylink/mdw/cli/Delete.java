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
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;

/**
 * Recursively delete.
 */
public class Delete {

    private File file;
    private boolean includeSubpackages;

    public Delete(File file) {
        this(file, false);
    }

    public Delete(File file, boolean includeSubpackages) {
        this.file = file;
        this.includeSubpackages = includeSubpackages;
    }

    public void run() throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (includeSubpackages || !new File(child + "/.mdw/package.json").isFile())
                    new Delete(child, includeSubpackages).run();
            }
        }
        if (!file.delete())
            throw new IOException("Failed to delete: " + file.getAbsolutePath());
    }
}
