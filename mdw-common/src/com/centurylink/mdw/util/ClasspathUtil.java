/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Contains utility methods for validating the CLASSPATH of a runtime
 * environment and determining which version of a class will be loaded.
 */
public class ClasspathUtil {
    private static final String PATH_SEP = System.getProperty("path.separator");
    private static final String[] FRAMEWORK_JARS = { "mdw-common", "mdw-listeners", "mdw-schemas",
            "mdw-workflow", "mdw-services", "mdw-web", "mdw-taskmgr" };

    private ClasspathUtil() {
    } // cannot be instantiated

    /**
     * Returns the system classpath based on the Java runtime property.
     *
     * @return the classpath
     */
    public static String getSystemClasspath() {
        return System.getProperty("java.class.path");
    }

    /**
     * Returns the value of the environment variable CLASSPATH for the runtime
     * environment. Note that the system classpath is used in preference to this
     * if they disagree.
     *
     * @return the classpath from the environment variable
     */
    public static String getEnvVarClasspath() {
        return environmentVariable("CLASSPATH");
    }

    /**
     * Returns the PATH environment variable for the current runtime
     * environment.
     *
     * @return the PATH environment variable value
     */
    public static String getPath() {
        return environmentVariable("PATH");
    }

    /**
     * Parses the system classpath, returning a String array of values.
     *
     * @return classpath entries
     */
    public static String[] parseSystemClasspath() {
        StringTokenizer st = new StringTokenizer(getSystemClasspath(), PATH_SEP);
        String[] classlist = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            classlist[i] = st.nextToken();
            i++;
        }

        return classlist;
    }

    /**
     * Parses the system path, returning a String array of values.
     *
     * @return path entries
     */
    public static String[] parseSystemPath() {
        if (getPath() == null)
            return new String[0];

        StringTokenizer st = new StringTokenizer(getPath(), PATH_SEP);
        String[] pathlist = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            pathlist[i] = st.nextToken();
            i++;
        }

        return pathlist;
    }

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

    /**
     * Returns the present working directory of the runtime environment.
     *
     * @return value of user.dir
     */
    public static String getRuntimeWorkingDir() {
        return System.getProperty("user.dir");
    }

    /**
     * Evaluates an environment variable from the runtime environment.
     *
     * @param var
     *            the variable to evaluate IN UPPER CASE
     * @return the runtime value
     */
    static String environmentVariable(String var) {
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            if (key.toUpperCase().equals(var))
                return env.get(key);
        }
        return null;
    }

    /**
     * @return the classpath for the deployed java ee app
     */
    public static String getCompilerClasspath(ClassLoader parentLoader) throws IOException {
        StringBuffer cpBuf = new StringBuffer(getSystemClasspath());

        // TODO handle Tomcat
        // WebLogic
        String cpUtilLoc = ClasspathUtil.locate(ClasspathUtil.class.getName());
        int jarBangIdx = cpUtilLoc.indexOf(".jar!");
        if (jarBangIdx > 0) {
            // deployed as EAR
            // app-inf/lib
            String jarFilePath = cpUtilLoc.substring(0, jarBangIdx + 4);
            File jarFile;
            if (jarFilePath.startsWith("file:/")) {
                try {
                    jarFile = new File(new URI(jarFilePath));
                }
                catch (URISyntaxException ex) {
                    throw new IOException(ex.getMessage(), ex);
                }
            }
            else {
                jarFile = new File(jarFilePath);
            }
            if (!jarFile.exists() || !jarFile.isFile())
                throw new FileNotFoundException(jarFilePath);
            File jarDir = jarFile.getParentFile();
            if (!jarDir.exists() || !jarDir.isDirectory())
                throw new FileNotFoundException(jarDir.getAbsolutePath());
            // append all the jar files to the classpath
            for (File file : listJarFiles(jarDir))
                cpBuf.append(PATH_SEP).append(file.getAbsolutePath());
            // parent dir should contain EJB jars
            for (File file : listJarFiles(jarDir.getParentFile().getParentFile()))
                cpBuf.append(PATH_SEP).append(file.getAbsolutePath());
            if (parentLoader != null) {
                // ejb jars
                URL ejbUrl = parentLoader
                        .getResource("com/qwest/mdw/services/process/BaseActivity.class");
                if (ejbUrl != null) {
                    String ejbLoc = ejbUrl.getFile();
                    jarBangIdx = ejbLoc.indexOf(".jar!");
                    if (jarBangIdx > 0) {
                        jarFilePath = ejbLoc.substring(0, jarBangIdx + 4);
                        jarFile = new File(jarFilePath);
                        if (jarFile.exists() && jarFile.isFile()) {
                            jarDir = jarFile.getParentFile();
                            if (jarDir.exists() && jarDir.isDirectory()) {
                                for (File file : listJarFiles(jarDir))
                                    cpBuf.append(PATH_SEP).append(file.getAbsolutePath());
                            }
                        }
                    }
                }

                // task manager webapp jars
                URL webUrl = parentLoader
                        .getResource("com/qwest/mdw/taskmgr/ui/tasks/FullTaskInstance.class");
                if (webUrl != null) {
                    String webLoc = webUrl.getFile();
                    jarBangIdx = webLoc.indexOf(".jar!");
                    if (jarBangIdx > 0) {
                        jarFilePath = webLoc.substring(0, jarBangIdx + 4);
                        jarFile = new File(jarFilePath);
                        if (jarFile.exists() && jarFile.isFile()) {
                            jarDir = jarFile.getParentFile();
                            if (jarDir.exists() && jarDir.isDirectory()) {
                                for (File file : listJarFiles(jarDir))
                                    cpBuf.append(PATH_SEP).append(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
        else {
            // local deployment (ie from eclipse)
            int end = cpUtilLoc.lastIndexOf(ClasspathUtil.class.getName().replace('.', '/'));
            String commonCpDir = cpUtilLoc.substring(1, end);
            cpBuf.append(PATH_SEP).append(commonCpDir);
            int buildClassesIdx = commonCpDir.lastIndexOf("/build/classes");
            if (buildClassesIdx > 0) {
                String rootCpDir = commonCpDir.substring(0, buildClassesIdx);
                for (String frameworkProj : FRAMEWORK_JARS)
                    cpBuf.append(PATH_SEP)
                            .append(rootCpDir + "/../" + frameworkProj + "/build/classes");
                for (File file : listJarFiles(
                        new File(rootCpDir + "/../MDWFramework/EarContent/APP-INF/lib")))
                    cpBuf.append(PATH_SEP).append(file.getAbsolutePath());
                for (File file : listJarFiles(new File(rootCpDir + "/../MDWWeb/web/WEB-INF/lib")))
                    cpBuf.append(PATH_SEP).append(file.getAbsolutePath());
                for (File file : listJarFiles(
                        new File(rootCpDir + "/../MDWTaskManagerWeb/web/WEB-INF/lib")))
                    cpBuf.append(PATH_SEP).append(file.getAbsolutePath());
            }
        }

        return cpBuf.toString();
    }

    public static File[] listJarFiles(File directory, boolean recursive) throws IOException {
        if (recursive) {
            List<File> jarFiles = new ArrayList<File>();
            addJarFilesRecursive(jarFiles, directory);
            return jarFiles.toArray(new File[0]);
        }
        else {
            return listJarFiles(directory);
        }
    }

    private static void addJarFilesRecursive(List<File> jarFiles, File directory)
            throws IOException {
        for (File jarFile : listJarFiles(directory))
            jarFiles.add(jarFile);
        for (File file : directory.listFiles()) {
            if (file.isDirectory())
                addJarFilesRecursive(jarFiles, file);
        }
    }

    public static File[] listJarFiles(File directory) throws IOException {
        if (!directory.exists() || !directory.isDirectory())
            throw new IOException("Cannot find directory: " + directory);

        File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name != null && name.toLowerCase().endsWith(".jar");
            }
        });
        return files;
    }
}
