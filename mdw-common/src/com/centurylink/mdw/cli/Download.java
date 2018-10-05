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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Bare min impl to support CLI without dependencies.
 */
public class Download implements Operation {

    private static final int BUFFER_KB = 16;

    private URL from;
    public URL getFrom() { return from; }

    private File to;
    public File getTo() { return to; }

    private long size; // if known, in bytes
    public long getSize() { return size; }

    private String message;

    public Download(URL from, File to) {
        this(from, to, null);
    }

    public Download(URL from, File to, String message) {
        this(from, to, 0, message);
    }

    public Download(URL from, File to, long size) {
        this(from, to, size, null);
    }

    public Download(URL from, File to, long size, String message) {
        this.from = from;
        this.to = to;
        this.size = size;
        this.message = message;
    }

    public Download run(ProgressMonitor... progressMonitors) throws IOException {
        if (message != null) {
            for (ProgressMonitor progressMonitor : progressMonitors)
                progressMonitor.message(message);
        }

        URLConnection connection = from.openConnection();
        if (size == 0 && connection.getContentLength() > 0)
          size = connection.getContentLength();
        try (InputStream urlIn = new BufferedInputStream(connection.getInputStream());
                OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(to))) {
            if (size == 0)
              size = urlIn.available();
            long sofar = 0;
            byte[] buffer = new byte[BUFFER_KB * 1024];

            for (ProgressMonitor progressMonitor : progressMonitors)
                progressMonitor.progress(0);

            while (true) {
                if (size > 0 && progressMonitors.length > 0 && sofar < size) {
                    int prog = (int) Math.floor((sofar * 100)/size);
                    for (ProgressMonitor progressMonitor : progressMonitors)
                        progressMonitor.progress(prog);
                }

                int bytesRead = urlIn.read(buffer);
                if (bytesRead == -1)
                    break;
                fileOut.write(buffer, 0, bytesRead);
                sofar += bytesRead;
            }
            for (ProgressMonitor progressMonitor : progressMonitors)
                progressMonitor.progress(100);
        }

        return this;
    }
}
