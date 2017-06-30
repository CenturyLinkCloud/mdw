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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Fetch implements Operation {

    private URL from;
    private String data;
    public String getData() { return data; }

    public Fetch(URL from) {
        this.from = from;
    }

    public Fetch run(ProgressMonitor... progressMonitors) throws IOException {
        try (InputStream urlIn = new BufferedInputStream(from.openStream());
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len = urlIn.read(buffer);
            while (len >= 0) {
                out.write(buffer, 0, len);
                len = urlIn.read(buffer);
            }
            data = out.toString();
        }
        return this;
    }

}
