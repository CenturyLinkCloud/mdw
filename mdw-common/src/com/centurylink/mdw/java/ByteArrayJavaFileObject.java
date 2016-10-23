/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class ByteArrayJavaFileObject extends SimpleJavaFileObject {

    private ByteArrayOutputStream baos;

    public ByteArrayJavaFileObject(String className, Kind kind) throws Exception {
        super(new URI(className), kind);
    }

    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public OutputStream openOutputStream() throws IOException {
        return baos = new ByteArrayOutputStream();
    }

    public byte[] getByteArray() {
        return baos.toByteArray();
    }
}
