/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.test;

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
