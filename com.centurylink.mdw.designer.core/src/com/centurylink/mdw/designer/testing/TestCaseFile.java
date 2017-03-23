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
package com.centurylink.mdw.designer.testing;

import java.io.FileInputStream;
import java.io.IOException;

public class TestCaseFile extends java.io.File {

    public TestCaseFile(String pathname) {
        super(pathname);
    }

    public String getText() throws IOException {
        return text();
    }

    public String text() throws IOException {
        return new String(read());
    }

    private byte[] read() throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(this);
            byte[] bytes = new byte[(int) this.length()];
            fis.read(bytes);
            return bytes;
        }
        finally {
            if (fis != null)
                fis.close();
        }
    }
}
