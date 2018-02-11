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
package com.centurylink.mdw.cloud;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Used for Tomcat (and generally in cloud mode) to include Jar assets and library jars
 * on the classpath.
 */
public class CloudClassLoader extends ClassLoader {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private List<File> classpath;
    public List<File> getClasspath() { return classpath; }

    private Package mdwPackage;

    private File assetRoot;

    private static List<Asset> cachedJarAssets = null;
    private List<Asset> jarAssets = null;
    public synchronized List<Asset> getJarAssets() {
        List<Asset> newJarAssets = AssetCache.getJarAssets();

        if (cachedJarAssets != newJarAssets) {
            cachedJarAssets = newJarAssets;
            jarAssets = null;
        }

        if (jarAssets == null) {
            jarAssets = new ArrayList<Asset>();
            jarAssets.addAll(newJarAssets);
            // same-package jars go first
            if (this.mdwPackage.getName() != null) {
                Collections.sort(jarAssets, new Comparator<Asset>() {
                    public int compare(Asset rs1, Asset rs2) {
                        String pkgName = mdwPackage.getName();
                        if (pkgName.equals(rs1.getPackageName()) && !pkgName.equals(rs2.getPackageName()))
                            return -1;
                        else if (pkgName.equals(rs2.getPackageName()) && !pkgName.equals(rs1.getPackageName()))
                            return 1;
                        else
                            return 0;
                    }
                });
            }
        }

        return jarAssets;
    }

    public CloudClassLoader(Package pkg) {
        super(pkg.getClassLoader());
        mdwPackage = pkg;

        classpath = new ArrayList<File>();

        String cp = pkg.getProperty(PropertyNames.MDW_JAVA_RUNTIME_CLASSPATH);
        if (cp != null) {
            String[] cps = cp.trim().split(File.pathSeparator);
            for (int i = 0; i < cps.length; i++) {
                classpath.add(new File(cps[i]));
            }
        }

        String libdir = pkg.getProperty(PropertyNames.MDW_JAVA_LIBRARY_PATH);
        if (libdir != null) {
            File dir = new File(libdir);
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    if (f.getName().endsWith(".jar"))
                      classpath.add(f);
                }
            }
        }

        String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
        if (assetLoc != null)
            assetRoot = new File(assetLoc);
    }

    /**
     * TODO: allow packages to isolate their classes
     */
    private static Map<String,Class<?>> sharedClassCache = new ConcurrentHashMap<String,Class<?>>();

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] b = null;
        try {
            Asset javaAsset = AssetCache.getAsset(name, Asset.JAVA);
            // try dynamic java first
            if (javaAsset != null)
                return CompiledJavaCache.getClass(getParent(), mdwPackage, name, javaAsset.getStringContent());
            // try shared cache
            Class<?> found;
            found = sharedClassCache.get(name);
            if (found != null)
                return found;
            // next try dynamic jar assets
            String path = name.replace('.', '/') + ".class";
            b = findInJarAssets(path);
            // lastly try the file system
            if (b == null)
                b = findInFileSystem(path);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(),  ex);
        }

        if (b == null)
            throw new ClassNotFoundException(name);

        if (logger.isMdwDebugEnabled())
            logger.mdwDebug("Class " + name + " loaded by Cloud classloader for package: " + mdwPackage.getLabel());

        String pkgName = mdwPackage.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0)
            pkgName = name.substring(0, lastDot);

        java.lang.Package pkg = getPackage(pkgName);
        if (pkg == null)
            definePackage(pkgName, null, null, null, "MDW", mdwPackage.getVersionString(), "CenturyLink", null);
        Class<?> clz = defineClass(name, b, 0, b.length);
        Class<?> found = sharedClassCache.get(name);
        if (found != null)
            clz = found;
        else
            sharedClassCache.put(name, clz);

        return clz;
    }

    private byte[] findInFileSystem(String path) throws IOException {
        byte[] b = null;
        for (int i = 0; b == null && i < classpath.size(); i++) {
            File file = classpath.get(i);
            if (file.isDirectory()) {
                b = findInDirectory(file, path);
            }
            else {
                String filepath = file.getPath();
                if (filepath.endsWith(".jar")) {
                    b = findInJarFile(file, path);
                }
            }
        }
        return b;
    }

    private byte[] findInDirectory(File dir, String path) throws IOException {
        byte[] b = null;
        File f = new File(dir.getPath() + "/" + path);
        if (f.exists()) {
            FileInputStream fi = null;
            try {
                fi = new FileInputStream(f);
                b = new byte[fi.available()];
                fi.read(b);
                fi.close();
            }
            finally {
                if (fi != null)
                    fi.close();
            }
        }
        return b;
    }

    private byte[] findInJarFile(File jar, String path) throws IOException {
        byte[] b = null;
        InputStream is = null;
        JarFile jf = null;
        try {
            jf = new JarFile(jar);
            ZipEntry ze = jf.getEntry(path);
            if (ze != null) {
                int k, n, m;
                is = jf.getInputStream(ze);
                n = is.available();
                m = 0;
                k = 1;
                b = new byte[n];
                while (m < n && k > 0) {
                    k = is.read(b, m, n-m);
                    if (k > 0)
                        m += k;
                }
                if (m < n) {
                    String msg = "Package class loader: expect " + n + ", read " + m;
                    throw new IOException(msg);
                }
            }
        }
        finally {
            if (is != null)
                is.close();
            if (jf != null)
                jf.close();
        }
        return b;
    }

    private byte[] findInJarAssets(String path) throws IOException {
        // prefer assets in the same package
        byte[] b = null;
        if (assetRoot != null) {
            for (Asset jarAsset : getJarAssets()) {
                File jarFile = jarAsset.getRawFile();
                if (jarFile == null)
                    jarFile = new File(assetRoot + "/" + jarAsset.getPackageName().replace('.', '/') + "/" + jarAsset.getName());
                b = findInJarFile(jarFile, path);
                if (b != null)
                    return b;
            }
        }
        return b;
    }

    private Map<String,Boolean> classesFound = new ConcurrentHashMap<String,Boolean>();

    /**
     * Looks for classes in Jar assets or on the prop-specified classpath.
     * Results are cached, so Jar/CP changes require app redeployment.
     */
    public boolean hasClass(String name) {
        Boolean found = classesFound.get(name);
        if (found != null)
            return found;

        byte[] b = null;
        try {
            // try dynamic jar assets
            String path = name.replace('.', '/') + ".class";
            b = findInJarAssets(path);
            // try the file system
            if (b == null)
                b = findInFileSystem(path);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(),  ex);
        }
        found = b != null;
        classesFound.put(name, found);
        return found;
    }

    /**
     * This is used by XMLBeans for loading the type system.
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] b = null;
        try {
            Asset resource = AssetCache.getAsset(mdwPackage.getName() + "/" + name);
            if (resource != null)
                b = resource.getRawContent();
            if (b == null)
                b = findInJarAssets(name);
            if (b == null)
                b = findInFileSystem(name);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(),  ex);
        }

        if (b == null)
            return super.getResourceAsStream(name);
        else
            return new ByteArrayInputStream(b);
    }

    @Override
    public URL getResource(String name) {
        URL result = null;
        for (int i = 0; result == null && i < classpath.size(); i++) {
            File one = classpath.get(i);
            if (one.isDirectory()) {
                File searchResource = new File(one.getPath() + "/" + name);
                if ( searchResource.exists() ) {
                    try {
                        result = searchResource.toURI().toURL();
                    } catch (MalformedURLException mfe) {
                        result = null;
                    }
                }
            }
        }
        if (result == null) {
            if (assetRoot != null) {
                String sep = File.separator.equals("/") ? "" : "/";
                for (Asset jarAsset : getJarAssets()) {
                    File jarFile = jarAsset.getRawFile();
                    if (jarFile == null)
                        jarFile = new File(assetRoot + "/" + jarAsset.getPackageName().replace('.', '/') + "/" + jarAsset.getName());
                    try (JarFile jf = new JarFile(jarFile)) {
                        ZipEntry entry = jf.getEntry(name);
                        if (entry == null && name.startsWith("/"))
                            entry = jf.getEntry(name.substring(1));
                        if (entry != null) {
                            return new URL("jar:file:" + sep + jarFile.getAbsolutePath().replace('\\', '/') + "!/" + name);
                        }
                    }
                    catch (Exception ex) {
                        logger.severeException("Error loading resource: " + name, ex);
                    }
                }
            }

            result = super.findResource(name);
        }
        return result;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Vector<URL> resUrls = null;

        if (assetRoot != null) {
            String sep = File.separator.equals("/") ? "" : "/";
            for (Asset jarAsset : getJarAssets()) {
                File jarFile = jarAsset.getRawFile();
                if (jarFile == null)
                    jarFile = new File(assetRoot + "/" + jarAsset.getPackageName().replace('.', '/') + "/" + jarAsset.getName());
                try (JarFile jf = new JarFile(jarFile)) {
                    if (jf.getEntry(name) != null) {
                        if (resUrls == null)
                            resUrls = new Vector<>();
                        resUrls.addElement(new URL("jar:file:" + sep + jarFile.getAbsolutePath().replace('\\', '/') + "!/" + name));
                    }
                }
                catch (Exception ex) {
                    logger.severeException("Error loading resource: " + name, ex);
                }
            }
        }
        if (resUrls != null) {
            Enumeration<URL> superEnum = super.getResources(name);
            while (superEnum.hasMoreElements()) {
                resUrls.addElement(superEnum.nextElement());
            }
            return resUrls.elements();
        }

        return super.getResources(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!logger.isMdwDebugEnabled()) {
            return super.loadClass(name);
        }
        else {
            Class<?> loaded = super.loadClass(name);
            logger.mdwDebug("Loaded class: '" + name + "' from CloudClassLoader with parent: " + getParent());
            if (logger.isTraceEnabled())
                logger.traceException("Stack trace: ", new Exception("ClassLoader stack trace"));
            return loaded;
        }
    }

    public String toString() {
        return this.getClass() + (mdwPackage == null ? "null" : (":" + mdwPackage.getName()));
    }

}
