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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Extends java.util.Properties to make sure the keys are sorted in a
 * predictable order also to avoid setting date comment which causes Git conflicts.
 */
public class VersionProperties extends Properties {

    private File propFile;

    public VersionProperties(File propFile) throws IOException {
        super(null);
        load(new FileInputStream(propFile));
        this.propFile = propFile;
    }

    public VersionProperties(ByteArrayInputStream byteArrayInputStream) throws IOException {
        super(null);
        load(byteArrayInputStream);
    //    this.propFile = propFile;
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        if (comments != null) {
            super.store(out, comments);
        }
        else {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
            synchronized (this) {
                for (Enumeration<?> e = keys(); e.hasMoreElements();) {
                    String key = (String)e.nextElement();
                    String val = (String)get(key);
                    bw.write(key.replaceAll(" ", "\\\\ ").replaceAll("!", "\\\\!") + "=" + val);
                    bw.newLine();
                }
            }
            bw.flush();
        }
    }

    public void save() throws IOException {
        if (isEmpty()) {
            if (propFile.exists() && !propFile.delete())
                throw new IOException("Unable to delete file: " + propFile);
        }
        else {
            OutputStream out = null;
            try {
                out = new FileOutputStream(propFile);
                store(out, null);
            }
            catch (FileNotFoundException ex) {
                // maybe read-only
                propFile.setWritable(true);
                out = new FileOutputStream(propFile);
                store(out, null);
            }
            finally {
                if (out != null)
                    out.close();
            }
        }
    }
}
