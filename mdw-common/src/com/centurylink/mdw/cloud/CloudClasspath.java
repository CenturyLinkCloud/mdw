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
import com.centurylink.mdw.util.StringHelper;

// TODO add the jars in PropertyNames.MDW_JAR_LIBRARY_PATH
public class CloudClasspath {
    private static final String PATH_SEP = System.getProperty("path.separator");
    private static final String FILE_SEP = System.getProperty("file.separator");
    private CloudClassLoader cloudClassLoader;
    private String systemClasspath;
    private String cloudClasspath;
    private List<File> webappJars = new ArrayList<File>();
    private List<File> jarAssetFiles = new ArrayList<File>();
    private File webInfClasses;
    private String configPath;


    public CloudClasspath(CloudClassLoader cloudClassLoader) {
        super();
        this.cloudClassLoader = cloudClassLoader;
    }

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
                jarAssetFiles.addAll(Arrays.asList(ClasspathUtil.listJarFiles(ApplicationContext.getAssetRoot(), true, true)));
            }
        }

        if (ApplicationContext.isSpringBoot()) {
            File bootInfLib = new File(ApplicationContext.getDeployPath() + "/BOOT-INF/lib");
            if (bootInfLib.exists()) {
                webappJars.addAll(Arrays.asList(ClasspathUtil.listJarFiles(bootInfLib, true, false)));
            }
            webInfClasses = new File(ApplicationContext.getDeployPath() + "/BOOT-INF/classes");
        }
        else {
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
                String mdwWarName = PropertyManager.getProperty(PropertyNames.MDW_WAR_NAME);
                if (StringHelper.isEmpty(mdwWarName))
                    mdwWarName = ApplicationContext.getMdwHubContextRoot();
                File mdwWebInfDir = new File(tomcatWebAppDir + FILE_SEP + mdwWarName + FILE_SEP + "WEB-INF");
                webappJars.addAll(Arrays.asList(ClasspathUtil.listJarFiles(mdwWebInfDir, true, false)));
                webInfClasses = new File(mdwWebInfDir + FILE_SEP + "classes");
            }
        }
    }

    public List<File> getFiles() {
        List<File> files = new ArrayList<>();
        String compilerClasspath = PropertyManager.getProperty(PropertyNames.MDW_JAVA_COMPILER_CLASSPATH);
        if (compilerClasspath != null) {
            for (String file : compilerClasspath.split(PATH_SEP)) {
                files.add(new File(file));
            }
        }
        files.addAll(webappJars);
        if (webInfClasses != null) {
            files.add(webInfClasses);
        }
        if (cloudClasspath != null) {
            for (String file : cloudClasspath.split(PATH_SEP)) {
                files.add(new File(file));
            }
        }
        files.addAll(jarAssetFiles);
        if (configPath != null) {
            for (String file : configPath.split(PATH_SEP)) {
                files.add(new File(file));
            }
        }
        return files;
    }

    @Override
    public String toString() {
        StringBuffer classpath = new StringBuffer(systemClasspath);
        // user-specified classpath entries come first
        String compilerClasspath = PropertyManager.getProperty(PropertyNames.MDW_JAVA_COMPILER_CLASSPATH);
        if (compilerClasspath != null)
            classpath.append(PATH_SEP).append(compilerClasspath);
        for (File jarFile : webappJars) {
            classpath.append(PATH_SEP).append(jarFile.getAbsolutePath());
        }
        if (webInfClasses != null) {
            classpath.append(PATH_SEP).append(webInfClasses);
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
