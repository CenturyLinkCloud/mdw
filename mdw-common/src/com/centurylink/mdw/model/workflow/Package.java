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
package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.MdwJavaException;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.pkg.PackageClassLoader;
import com.centurylink.mdw.spring.SpringAppContext;

import java.io.File;
import java.io.IOException;

public class Package implements Comparable<Package> {

    public static final String MDW = "com.centurylink.mdw";

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private MdwVersion version;
    public MdwVersion getVersion() {
        return this.version == null ? new MdwVersion(0) : this.version;
    }
    public void setVersion(MdwVersion version){
        this.version = version;
    }

    private File directory;
    public File getDirectory() { return directory; }
    public void setDirectory(File dir) { this.directory = dir; }

    public String getLabel() {
        return getName() + getVersion().getLabel();
    }

    public Package() { }

    public Package(PackageMeta meta, File pkgDir) {
        this.name = meta.getName();
        this.version = meta.getVersion();
        this.directory = pkgDir;
    }

    private PackageClassLoader packageClassLoader = null;
    public PackageClassLoader getClassLoader() {
        if (packageClassLoader == null)
            packageClassLoader = new PackageClassLoader(this);
        return packageClassLoader;
    }

    public GeneralActivity getActivityImplementor(String className)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, MdwJavaException {
        // try dynamic java first
        try {
            ClassLoader parentLoader = getClassLoader();
            return (GeneralActivity) CompiledJavaCache.getInstance(className, parentLoader, this);
        }
        catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        GeneralActivity injected = SpringAppContext.getInstance().getActivityImplementor(className, this);
        if (injected != null)
            return injected;
        if (getClassLoader().hasClass(className))
          return getClassLoader().loadClass(className).asSubclass(GeneralActivity.class).newInstance();
        return Package.class.getClassLoader().loadClass(className).asSubclass(GeneralActivity.class).newInstance();
    }

    public RequestHandler getRequestHandler(String className)
    throws ReflectiveOperationException, IOException, MdwJavaException {
        // try dynamic java first (preferred in case patch override is needed)
        try {
            ClassLoader parentLoader = getClassLoader();
            return (RequestHandler) CompiledJavaCache.getInstance(className, parentLoader, this);
        }
        catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        RequestHandler injected = SpringAppContext.getInstance().getRequestHandler(className, this);
        if (injected != null)
            return injected;
        if (getClassLoader().hasClass(className))
          return getClassLoader().loadClass(className).asSubclass(RequestHandler.class).newInstance();
        return Package.class.getClassLoader().loadClass(className).asSubclass(RequestHandler.class).newInstance();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public int compareTo(Package other) {
        // latest first
        if (this.name.equals(other.name))
            return this.version.compareTo(other.version);
        else
            return this.name.compareToIgnoreCase(other.name);
    }
}
