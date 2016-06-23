/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class StringJavaFileObject extends SimpleJavaFileObject {
    
    private String className;
    private ByteArrayOutputStream byteCodeStream;
    public String getClassName() { return className; }
    
    private String javaCode;

    public StringJavaFileObject(String className) {
        super(URI.create("string:///" + className.replace('.', '/').replaceAll(" ", "") + Kind.SOURCE.extension), Kind.SOURCE);
        this.className = className.replaceAll(" ", "");
    }

    public StringJavaFileObject(String className, String javaCode) {
        this(className);
        this.javaCode = javaCode;
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return javaCode;
    }
    
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(byteCodeStream.toByteArray());
    }

    public OutputStream openOutputStream() throws IOException {
        return byteCodeStream = new ByteArrayOutputStream();
    }

    public byte[] getByteArray() {
        return javaCode.getBytes();
    }
}