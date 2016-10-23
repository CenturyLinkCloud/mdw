/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cloud;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.util.ClasspathUtil;

// TODO add the jars in PropertyNames.MDW_JAR_LIBRARY_PATH
public class CloudClasspath {
    private static final String PATH_SEP = System.getProperty("path.separator");
    private static final String FILE_SEP = System.getProperty("file.separator");
    private CloudClassLoader cloudClassLoader;
    private String systemClasspath;
    private String cloudClasspath;
    private List<File> tomcatBaseLibJars = new ArrayList<File>();
    private List<File> tomcatHomeLibJars = new ArrayList<File>();
    private List<File> tomcatWebAppJars = new ArrayList<File>();
    private List<File> jarAssetFiles = new ArrayList<File>();
    private File tomcatMdwWebInfClasses;
    private String configPath;


    public CloudClasspath(CloudClassLoader cloudClassLoader) {
        super();
        this.cloudClassLoader = cloudClassLoader;
    }

    // cloud compiler class path
    public void read() throws IOException {
        systemClasspath = System.getProperty("java.class.path");
        if (systemClasspath == null)
            systemClasspath = "";
        if (cloudClassLoader != null) {
            if (cloudClassLoader.getClasspath() != null) {
                cloudClasspath = "";
                for (int i = 0; i < cloudClassLoader.getClasspath().size(); i++) {
                    cloudClasspath += cloudClassLoader.getClasspath().get(i);
                    if (i < cloudClassLoader.getClasspath().size() - 1)
                        cloudClasspath += PATH_SEP;
                }
            }
            List<Asset> jarAssets = cloudClassLoader.getJarAssets();
            if (jarAssets != null) {
                if (ApplicationContext.isFileBasedAssetPersist()) {
                    String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
                    jarAssetFiles.addAll(Arrays.asList(ClasspathUtil.listJarFiles(new File(assetLoc), true)));
                }
                else {
                    // write them to the temp dir (unfortunately needed for Groovy asset dependencies)
                    File tempLibDir = new File(ApplicationContext.getTempDirectory() + "/lib");
                    if (!tempLibDir.exists()) {
                        if (!tempLibDir.mkdirs())
                            throw new IOException("Unable to create directory: " + tempLibDir);
                        for (Asset jarAsset : jarAssets) {
                            String name = jarAsset.getName().endsWith(".jar") ? jarAsset.getName() : jarAsset.getName() + ".jar";
                            File destFile = new File(tempLibDir + "/" + jarAsset.getPackageName() + "/" + name);
                            jarAssetFiles.add(destFile);
                        }
                    }
                }
            }
        }

        if (ApplicationContext.isWar()) {
            configPath = System.getProperty("mdw.config.location");
            String catalinaBase = System.getProperty("catalina.base");
            File tomcatWebAppDir = new File(catalinaBase + FILE_SEP + "webapps");
            if (!tomcatWebAppDir.exists()) {
                // for Cloud EWS
                File configDir = new File(configPath);
                if (configDir.exists())
                    tomcatWebAppDir = new File(configDir.getParentFile() + FILE_SEP + "apps");
            }
            if (tomcatWebAppDir.isDirectory()) {
                tomcatWebAppJars.addAll(Arrays.asList(ClasspathUtil.listJarFiles(tomcatWebAppDir, true)));
                tomcatMdwWebInfClasses = new File(tomcatWebAppDir + FILE_SEP + "mdw" + FILE_SEP + "WEB-INF" + FILE_SEP + "classes");
            }
            // tomcat base
            File tomcatBaseLib = new File(catalinaBase + FILE_SEP + "lib");
            if (tomcatBaseLib.isDirectory())
                tomcatBaseLibJars.addAll(Arrays.asList(ClasspathUtil.listJarFiles(tomcatBaseLib, false)));
            // tomcat home
            String catalinaHome = System.getProperty("catalina.home");
            File tomcatHomeLib = new File(catalinaHome + FILE_SEP + "lib");
            if (tomcatHomeLib.isDirectory())
                tomcatHomeLibJars.addAll(Arrays.asList(ClasspathUtil.listJarFiles(tomcatHomeLib, false)));
        }
    }

    @Override
    public String toString() {
        StringBuffer classpath = new StringBuffer(systemClasspath);
        // user-specified classpath entries come first
        String compilerClasspath = PropertyManager.getProperty(PropertyNames.MDW_COMPILER_CLASSPATH);
        if (compilerClasspath != null)
            classpath.append(PATH_SEP).append(compilerClasspath);
        for (File jarFile : tomcatBaseLibJars) {
            classpath.append(PATH_SEP).append(jarFile.getAbsolutePath());
        }
        for (File jarFile : tomcatWebAppJars) {
            classpath.append(PATH_SEP).append(jarFile.getAbsolutePath());
        }
        if (tomcatMdwWebInfClasses != null) {
            classpath.append(PATH_SEP).append(tomcatMdwWebInfClasses);
        }
        for (File jarFile : tomcatHomeLibJars) {
            classpath.append(PATH_SEP).append(jarFile.getAbsolutePath());
        }
        if (cloudClasspath != null) {
            classpath.append(PATH_SEP).append(cloudClasspath);
        }
        for (File jarAssetFile : jarAssetFiles) {
            classpath.append(PATH_SEP).append(jarAssetFile.getAbsolutePath());
        }
        if (configPath != null) {
            classpath.append(PATH_SEP).append(configPath);
        }
        return classpath.toString();
    }

}
