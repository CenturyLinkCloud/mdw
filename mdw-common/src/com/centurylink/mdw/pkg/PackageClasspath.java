package com.centurylink.mdw.pkg;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.util.ClasspathUtil;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PackageClasspath {
    private static final String PATH_SEP = System.getProperty("path.separator");
    private static final String FILE_SEP = System.getProperty("file.separator");
    private PackageClassLoader packageClassLoader;
    private String systemClasspath;
    private String packageClasspath;
    private List<File> webappJars = new ArrayList<>();
    private List<File> jarAssetFiles = new ArrayList<>();
    private File webInfClasses;
    private String configPath;


    public PackageClasspath(PackageClassLoader packageClassLoader) {
        this.packageClassLoader = packageClassLoader;
    }

    public void read() throws IOException {
        systemClasspath = System.getProperty("java.class.path");
        if (systemClasspath == null)
            systemClasspath = "";
        if (packageClassLoader != null) {
            if (packageClassLoader.getClasspath() != null) {
                packageClasspath = "";
                for (int i = 0; i < packageClassLoader.getClasspath().size(); i++) {
                    packageClasspath += packageClassLoader.getClasspath().get(i);
                    if (i < packageClassLoader.getClasspath().size() - 1)
                        packageClasspath += PATH_SEP;
                }
            }

            packageClassLoader.getJarAssets(); // This is to pre-load the JARs for performance

            jarAssetFiles.addAll(Arrays.asList(ClasspathUtil.listJarFiles(ApplicationContext.getAssetRoot(), true, true)));
            // same-package jars go first
            String pkgPath = ApplicationContext.getAssetRoot().getCanonicalPath() + FILE_SEP + packageClassLoader.getPackageName().replace(".", FILE_SEP);
            jarAssetFiles.sort((o1, o2) -> {
                try {
                    if (pkgPath.equals(o1.getParentFile().getCanonicalPath()) && !pkgPath.equals(o2.getParentFile().getCanonicalPath()))
                        return -1;
                    else if (!pkgPath.equals(o1.getParentFile().getCanonicalPath()) && pkgPath.equals(o2.getParentFile().getCanonicalPath()))
                        return 1;
                    else
                        return 0;
                }
                catch (Exception ignored) {}
                return 0;
            });
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
                File configDir = new File(configPath);
                if (configDir.exists())
                    tomcatWebAppDir = new File(configDir.getParentFile() + FILE_SEP + "apps");
            }
            if (tomcatWebAppDir.isDirectory()) {
                String mdwWarName = PropertyManager.getProperty(PropertyNames.MDW_WAR_NAME);
                if (StringUtils.isBlank(mdwWarName))
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
        if (packageClasspath != null) {
            for (String file : packageClasspath.split(PATH_SEP)) {
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
        StringBuilder classpath = new StringBuilder(systemClasspath);
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
        if (packageClasspath != null) {
            classpath.append(PATH_SEP).append(packageClasspath);
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
