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
package com.centurylink.mdw.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.util.file.MdwIgnore;

/**
 * Contains utility methods for validating the CLASSPATH of a runtime
 * environment and determining which version of a class will be loaded.
 */
public class ClasspathUtil {

    private ClasspathUtil() {
    } // cannot be instantiated

    /**
     * Finds the location of the version of a particular class that will be used
     * by the Java runtime. Output goes to standard out.
     *
     * @param className
     * @return whether the class was located
     */
    public static String locate(String className, ClassLoader classLoader) {
        String resource = new String(className);

        // format the file name into a valid resource name
        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }
        resource = resource.replace('.', '/');
        resource = resource + ".class";

        // attempt to locate the file using the class loader
        URL classUrl = classLoader.getResource(resource);
        if (classUrl == null && classLoader == ClasspathUtil.class.getClassLoader()) {
            // Apparently clazz.getResource() works sometimes when clazz.getClassLoader().getResource() does not.
            // TODO: why?
            classUrl = ClasspathUtil.class.getResource(resource);
        }

        if (classUrl == null) {
            return "\nClass not found: [" + className + "]";
        }
        else {
            return classUrl.getFile();
        }
    }

    public static String locate(String className) {
        return locate(className, ClasspathUtil.class.getClassLoader());
    }

    public static File[] listJarFiles(File directory, boolean recursive, boolean honorMdwIgnore) throws IOException {
        if (recursive) {
            List<File> jarFiles = new ArrayList<File>();
            addJarFilesRecursive(jarFiles, directory, honorMdwIgnore);
            return jarFiles.toArray(new File[0]);
        }
        else {
            return listJarFiles(directory, honorMdwIgnore);
        }
    }

    private static void addJarFilesRecursive(List<File> jarFiles, File directory, boolean honorMdwIgnore)
            throws IOException {
        for (File jarFile : listJarFiles(directory, honorMdwIgnore)) {
            jarFiles.add(jarFile);
        }
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                MdwIgnore mdwIgnore = honorMdwIgnore ? new MdwIgnore(directory) : null;
                if (mdwIgnore == null || !mdwIgnore.isIgnore(file)) {
                    addJarFilesRecursive(jarFiles, file, honorMdwIgnore);
                }
            }
        }
    }

    public static File[] listJarFiles(File directory, boolean honorMdwIgnore) throws IOException {
        if (!directory.exists() || !directory.isDirectory())
            throw new IOException("Cannot find directory: " + directory);
        MdwIgnore mdwIgnore = honorMdwIgnore ? new MdwIgnore(directory) : null;
        File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name != null && name.toLowerCase().endsWith(".jar")
                        && (mdwIgnore == null || !mdwIgnore.isIgnore(new File(dir + "/" + name)));
            }
        });
        return files;
    }
}
