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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
        writeZipWith(directory, outputStream, includes, true);
    }

    public static void writeZipWith(File directory, OutputStream outputStream, List<File> includes, boolean includeSubs) throws IOException {

        byte[] buffer = new byte[ZIP_BUFFER_KB * 1024];
        ZipOutputStream zos = null;
        try {

            zos = new ZipOutputStream(outputStream);

            for (File file : new FileLister(directory).list()) {
                boolean include = false;
                if (includes != null) {
                    for (File in : includes) {
                        if (includeSubs) {
                            if (file.getPath().startsWith(in.getPath() + System.getProperty("file.separator")) || file.getPath().equals(in.getPath())) {
                                include = true;
                                break;
                            }
                        }
                        else {
                            if (file.isDirectory() && (file.getPath().startsWith(in.getPath() + System.getProperty("file.separator") + ".mdw") || file.getPath().equals(in.getPath()))) {
                                    include = true;
                                    break;
                            }
                            else if (file.isFile() && (file.getPath().equals(in.getPath() + System.getProperty("file.separator") + file.getName()) || file.getPath().equals(in.getPath() + System.getProperty("file.separator") + ".mdw" + System.getProperty("file.separator") + file.getName()))) {
                                include = true;
                                break;
                            }
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

    public static void unzip(File zipFile, File destDir) throws IOException {
        unzip(zipFile, destDir, null, null, false);
    }

    public static void unzip(File zipFile, File destDir, String baseLoc, List<String> excludes, boolean overwrite) throws IOException {
        if (!destDir.exists() || !destDir.isDirectory())
            throw new IOException("Destination directory does not exist: " + destDir);

        ZipFile zip = new ZipFile(zipFile);
        try {
            if (baseLoc != null && !baseLoc.endsWith("/"))
                baseLoc += "/";

            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if ((baseLoc == null || (entry.getName().startsWith(baseLoc) && !entry.getName().equals(baseLoc)))
                        && (excludes == null || !excludes.contains(entry.getName()))) {
                    // write the file
                    String outpath = destDir + "/";
                    if (baseLoc == null)
                        outpath += entry.getName();
                    else
                        outpath += entry.getName().substring(baseLoc.length());
                    File outfile = new File(outpath);
                    if (outfile.exists() && !overwrite)
                        throw new IOException("Output file already exists: " + outfile);
                    if (entry.isDirectory()) {
                        if (outfile.exists())
                            FileHelper.deleteRecursive(outfile);
                        if (!outfile.mkdirs())
                            throw new IOException("Unable to create directory: " + outfile);
                    }
                    else {
                        InputStream is = null;
                        OutputStream os = null;
                        try {
                            is = zip.getInputStream(entry);
                            os = new FileOutputStream(outfile);
                            int read = 0;
                            byte[] bytes = new byte[1024];
                            while((read = is.read(bytes)) != -1)
                                os.write(bytes, 0, read);
                        }
                        finally {
                            if (is != null)
                                is.close();
                            if (os != null)
                              os.close();
                        }
                    }
                }
            }
        }
        finally {
            zip.close();
        }
    }

    public static void zip(File directory, File zipFile) throws IOException {
        zip(directory, zipFile, null);
    }

    public static void zip(File directory, File zipFile, List<File> excludes) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(zipFile);
            ZipHelper.writeZip(directory, fos, excludes);
        }
        finally {
            if (fos != null)
                fos.close();
        }
    }

    public static void zipWith(File directory, File zipFile, List<File> includes) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(zipFile);
            ZipHelper.writeZipWith(directory, fos, includes, false);
        }
        finally {
            if (fos != null)
                fos.close();
        }
    }

    public static void unzip(URL fromUrl, File destDir) throws IOException {
        InputStream urlIn = null;
        OutputStream tempOut = null;
        File tempZip = null;;
        try {
            urlIn = new BufferedInputStream(fromUrl.openStream());
            tempZip = File.createTempFile("mdw", ".zip", null);
            tempOut = new BufferedOutputStream(new FileOutputStream(tempZip));
            byte[] buffer = new byte[ZIP_BUFFER_KB * 1024];
            int len = urlIn.read(buffer);
            while (len >= 0) {
                tempOut.write(buffer, 0, len);
                len = urlIn.read(buffer);
            }
        }
        finally {
            urlIn.close();
            tempOut.close();
        }

        unzip(tempZip, destDir);
        tempZip.delete();
    }

}
