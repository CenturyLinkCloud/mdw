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