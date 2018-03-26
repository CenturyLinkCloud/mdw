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
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Recursively copy.
 */
public class Copy implements Operation {
    private File from;
    private File to;
    private boolean includeSubpackages;

    public Copy(File from, File to) {
        this(from, to, false);
    }

    public Copy(File from, File to, boolean includeSubpackages) {
        this.from = from;
        this.to = to;
        this.includeSubpackages = includeSubpackages;
    }

    public Copy run(ProgressMonitor... progressMonitors) throws IOException {
        Files.copy(Paths.get(from.getPath()), Paths.get(to.getPath()));
        if (from.isDirectory()) {
            for (File childFrom : from.listFiles()) {
                if (includeSubpackages || !new File(childFrom + "/.mdw/package.json").isFile() || !new File(childFrom + "/.mdw/package.yaml").isFile()) {
                    File childTo = new File(to + "/" + childFrom.getName());
                    new Copy(childFrom, childTo, includeSubpackages).run();
                }
            }
        }
        return this;
    }
}
