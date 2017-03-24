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
package com.centurylink.mdw.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper {

    private static final int ZIP_BUFFER_KB = 16;

    public static void writeZip(File directory, OutputStream outputStream, List<File> excludes) throws IOException {

        byte[] buffer = new byte[ZIP_BUFFER_KB * 1024];
        ZipOutputStream zos = null;
        try {

            zos = new ZipOutputStream(outputStream);

            for (File file : new FileLister(directory).list()) {
                boolean exclude = false;
                if (excludes != null) {
                    for (File ex : excludes) {
                        if (file.getPath().startsWith(ex.getPath() + System.getProperty("file.separator")) || file.getPath().equals(ex.getPath())) {
                            exclude = true;
                            break;
                        }
                    }
                }
                if (!exclude) {
                    String name = file.getPath().substring(directory.getPath().length() + 1).replace('\\', '/');
                    if (file.isDirectory())
                        name += '/';
                    ZipEntry ze = new ZipEntry(name);
                    zos.putNextEntry(ze);

                    if (file.isFile()) {
                        FileInputStream in = new FileInputStream(file);

                        int len;
                        while ((len = in.read(buffer)) > 0)
                            zos.write(buffer, 0, len);

                        in.close();
                    }
                }
            }
        }
        finally {
            if (zos != null) {
                zos.closeEntry();
                zos.close();
            }
        }
    }

    public static void writeZipWith(File directory, OutputStream outputStream, List<File> includes) throws IOException {

        byte[] buffer = new byte[ZIP_BUFFER_KB * 1024];
        ZipOutputStream zos = null;
        try {

            zos = new ZipOutputStream(outputStream);

            for (File file : new FileLister(directory).list()) {
                boolean include = false;
                if (includes != null) {
                    for (File in : includes) {
                        if (file.getPath().startsWith(in.getPath() + System.getProperty("file.separator")) || file.getPath().equals(in.getPath())) {
                            include = true;
                            break;
                        }
                    }
                }
                if (include) {
                    String name = file.getPath().substring(directory.getPath().length() + 1).replace('\\', '/');
                    if (file.isDirectory())
                        name += '/';
                    ZipEntry ze = new ZipEntry(name);
                    zos.putNextEntry(ze);

                    if (file.isFile()) {
                        FileInputStream in = new FileInputStream(file);

                        int len;
                        while ((len = in.read(buffer)) > 0)
                            zos.write(buffer, 0, len);

                        in.close();
                    }
                }
            }
        }
        finally {
            if (zos != null) {
                zos.closeEntry();
                zos.close();
            }
        }
    }
}
