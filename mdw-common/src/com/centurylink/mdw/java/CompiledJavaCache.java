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

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.app.Compatibility.SubstitutionResult;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.ExcludableCache;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.cloud.CloudClasspath;
import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.util.file.Packages;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import javax.ws.rs.Path;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compiles and caches java asset classes, and provides a class-loading mechanism to allow these assets
 * to reference Jar classes as well.
 */
public class CompiledJavaCache implements PreloadableCache, ExcludableCache {

    public final static boolean classicLoading = PropertyManager.getBooleanProperty(PropertyNames.MDW_CONTAINER_CLASSIC_CLASSLOADING, false);
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Map<String,Class<?>> compiledCache = new ConcurrentHashMap<>();
    private static Map<String,Boolean> classesNotFound = new ConcurrentHashMap<>();

    public CompiledJavaCache() {

    }

    private static String[] preCompiled;

    public void initialize(Map<String,String> params) {
        if (params != null) {
            String preCompString = params.get("PreCompiled");
            if (preCompString != null && preCompString.trim().length() > 0) {
                List<String> preCompList = new ArrayList<>();
                preCompiled = preCompString.split("\n");
                for (String s : preCompiled) {
                    String preLoad = s.trim();
                    if (!preLoad.isEmpty())
                        preCompList.add(preLoad);
                }
                preCompiled = preCompList.toArray(new String[]{});
            }
        }
    }

    public String getFormat() {
        return Asset.JAVA;
    }

    /**
     * To load all the dynamic java Registered services classes
     */
    private void loadDynamicJavaRegisteredServices() {
        Map<Package,Map<String,String>> packagedJava = new HashMap<>();
        try {
            for (Asset javaAsset : AssetCache.getAssets(Asset.JAVA)) {
                // process RegisteredService-annotated classes
                if (javaAsset.getStringContent().indexOf("@RegisteredService") > 0 ||
                        javaAsset.getStringContent().indexOf("@Monitor") > 0 ||
                        javaAsset.getStringContent().indexOf("@Path") > 0 ||
                        javaAsset.getStringContent().indexOf("@Api") > 0 ||
                        javaAsset.getStringContent().indexOf("@ScheduledJob") > 0) {
                    String className = JavaNaming.getValidClassName(javaAsset.getName());
                    Package javaAssetPackage = PackageCache.getAssetPackage(javaAsset.getId());
                    if (javaAssetPackage == null) {
                        logger.severe("Omitting unpackaged Registered Service from compilation: " + javaAsset.getLabel());
                    }
                    else {
                        String qName = JavaNaming.getValidPackageName(javaAssetPackage.getName()) + "." + className;
                        Map<String,String> javaSources = packagedJava.get(javaAssetPackage);
                        if (javaSources == null) {
                            javaSources = new HashMap<>();
                            packagedJava.put(javaAssetPackage, javaSources);
                        }
                        javaSources.put(qName, javaAsset.getStringContent());
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }

        if (!packagedJava.isEmpty()) {
            for (Package pkg : packagedJava.keySet()) {
                try {
                    List<Class<?>> classes = CompiledJavaCache.compileClasses(getClass().getClassLoader(), pkg, packagedJava.get(pkg), true);
                    for (Class<?> clazz : classes) {
                        RegisteredService registeredService = clazz.getAnnotation(RegisteredService.class);
                        if (registeredService == null) {
                            // jax-rs services, and swagger Apis
                            Path pathAnnotation = clazz.getAnnotation(Path.class);
                            if (pathAnnotation != null) {
                                String resourcePath = pathAnnotation.value() == null ? clazz.getPackage().getName() + "/" + clazz.getSimpleName() : pathAnnotation.value();
                                if (JsonService.class.isAssignableFrom(clazz)) {
                                    logger.info("JAX-RS JSON Service: " + resourcePath + " --> '" + clazz + "'");
                                    DynamicJavaServiceRegistry.addRegisteredService(JsonService.class.getName(), clazz.getName(), resourcePath);
                                }
                                if (XmlService.class.isAssignableFrom(clazz)) {
                                    logger.info("JAX-RS XML Service: " + resourcePath + " --> '" + clazz + "'");
                                    DynamicJavaServiceRegistry.addRegisteredService(XmlService.class.getName(), clazz.getName(), resourcePath);
                                }
                            }
                            // Monitor annotation
                            Monitor monitorAnnotation = clazz.getAnnotation(Monitor.class);
                            if (monitorAnnotation != null) {
                                logger.info("Monitor Service: " + monitorAnnotation.value() + " --> '" + clazz + "'");
                                String monitorCategory = monitorAnnotation.category().getName();
                                MonitorRegistry.getInstance().addDynamicService(monitorCategory, clazz.getName());
                            }
                            // ScheduledJob annotation
                            ScheduledJob scheduledJobAnnotation = clazz.getAnnotation(ScheduledJob.class);
                            if (scheduledJobAnnotation != null) {
                                logger.info("Scheduled Job: " + scheduledJobAnnotation.value() + " --> '" + clazz + "'");
                                DynamicJavaServiceRegistry.addRegisteredService(com.centurylink.mdw.model.monitor.ScheduledJob.class.getName(), clazz.getName());
                            }
                        }
                        else {
                            for (int i = 0; i < registeredService.value().length; i++) {
                                String serviceName = registeredService.value()[i].getName();
                                logger.info("@RegisteredService: " + serviceName + " Class: " + clazz);
                                DynamicJavaServiceRegistry.addRegisteredService(serviceName, clazz.getName());
                            }
                        }
                    }
                }
                catch (Exception ex) {
                  // let other packages continue to process
                  logger.severeException("Failed to process Dynamic Java services in package " + pkg.getLabel() + ": " + ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * To clear dynamic java registered services
     */
    private void clearDynamicJavaRegisteredServices() {
        DynamicJavaServiceRegistry.clearRegisteredServices();
    }

    public static Class<?> getClass(Package currentPackage, String className, String javaCode)
    throws ClassNotFoundException, IOException, MdwJavaException {
        return getClass(null, currentPackage, className, javaCode);
    }

    public static Class<?> getClass(ClassLoader parentLoader, Package currentPackage, String className, String javaCode)
    throws ClassNotFoundException, IOException, MdwJavaException {
        return getClass(parentLoader, currentPackage, className, javaCode, true);
    }

    public static Class<?> getClass(ClassLoader parentLoader, Package currentPackage, String className, String javaCode, boolean cache)
    throws ClassNotFoundException, IOException, MdwJavaException {
        Class<?> clazz = cache ? compiledCache.get(className) : null;
        if (clazz == null) {
            try {
                if (Compatibility.hasCodeSubstitutions())
                    javaCode = doCompatibilityCodeSubstitutions(className, javaCode);
                compileJavaCode(parentLoader, currentPackage, className, javaCode, cache);
                clazz = DynamicJavaClassLoader.getInstance(parentLoader, currentPackage, cache).loadClass(className);
            }
            catch (ClassNotFoundException | IOException | MdwJavaException ex) {
                throw ex;
            } catch (Throwable t) {
                logger.severeException(t.getMessage(), t);
                // don't let compilation errors prevent startup
            }
        }
        return clazz;
    }

    /**
     * @param parentLoader parent classloader
     * @param currentPackage the package for compilation context
     * @param javaSources map of class name to source text
     * @param cache true to add compiled classes to cache
     * @return list of compiled classes
     */
    public static List<Class<?>> compileClasses(ClassLoader parentLoader, Package currentPackage, Map<String,String> javaSources, boolean cache)
    throws ClassNotFoundException, IOException, MdwJavaException {
        try {
            for (String className : javaSources.keySet()) {
                if (Compatibility.hasCodeSubstitutions())
                    javaSources.put(className, doCompatibilityCodeSubstitutions(className, javaSources.get(className)));
            }
            List<Class<?>> classes = compileJava(parentLoader, currentPackage, javaSources, cache);
            return classes;
        }
        catch (ClassNotFoundException | MdwJavaException | IOException ex) {
            throw ex;
        } catch (Throwable t) {
            logger.severeException(t.getMessage(), t);
            // don't let compilation errors prevent startup
            return null;
        }
    }

    public static Class<?> getResourceClass(String className, ClassLoader parentLoader, Package currentPackage)
    throws ClassNotFoundException, IOException, MdwJavaException {
        if (className.indexOf('$') > 0) {
            // inner class -- check previously loaded
            Class<?> clazz = compiledCache.get(className);
            if (clazz != null)
                return clazz;
        }
        Asset javaAsset = AssetCache.getAsset(className, Asset.JAVA);
        if (javaAsset == null)
            throw new ClassNotFoundException(className);

        try {
            if (currentPackage == null) {
                // use the java asset package
                int lastDot = className.lastIndexOf('.');
                if (lastDot == -1)  {
                    // default package
                    currentPackage = PackageCache.getDefaultPackage();
                }
                else {
                    String packageName = className.substring(0, lastDot);
                    currentPackage = PackageCache.getPackage(packageName);
                }
            }

            return getClass(parentLoader, currentPackage, className, javaAsset.getStringContent());
        }
        catch (CachingException ex) {
            throw new MdwJavaException(ex.getMessage(), ex);
        }
    }

    public static Object getInstance(String resourceClassName, ClassLoader parentLoader, Package currentPackage) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException, MdwJavaException  {
        Class resourceClass = getResourceClass(resourceClassName, parentLoader, currentPackage);
        return resourceClass == null ? null : resourceClass.newInstance();
    }

    private static void compileJavaCode(ClassLoader parentLoader, final Package currentPackage, String className, String javaCode, boolean cache)
    throws IOException, MdwJavaException {
        if (parentLoader == null)
            parentLoader = CompiledJavaCache.class.getClassLoader();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
            throw new MdwJavaException("No Java compiler available.  JDK must precede JRE on system PATH.");

        final JavaFileObject jfo = new StringJavaFileObject(className, javaCode);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        JavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        MdwJavaFileManager<JavaFileManager> mdwFileManager = new MdwJavaFileManager<JavaFileManager>(standardFileManager);

        // compiler options
        List<String> options = new ArrayList<>();

        // compiler classpath
        String pathSep = System.getProperty("path.separator");
        String classpath = getJavaCompilerClasspath(currentPackage);
        classpath += pathSep + getTempDir();  // include java source artifacts

        String debug = "Compiling Dynamic Java class: " + className;
        if (logger.isMdwDebugEnabled()) {
            String extra = "parent ClassLoader=" + parentLoader;
            if (currentPackage != null)
                extra += ", workflow package: " + currentPackage.getLabel();
            logger.debug(debug + " (" + extra + ")");
        }
        else if (logger.isDebugEnabled()) {
            logger.debug(debug);
        }

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("Dynamic Java Compiler Classpath: " + classpath);
        }

        options.addAll(Arrays.asList("-g", "-classpath", classpath));
        String extraOptions = PropertyManager.getProperty(PropertyNames.MDW_JAVA_COMPILER_OPTIONS);
        if (extraOptions != null)
            options.addAll(Arrays.asList(extraOptions.split(" ")));

        CompilationTask compileTask = compiler.getTask(null, mdwFileManager, diagnostics, options, null, Arrays.asList(jfo));
        // TODO jaxb processors
        // compileTask.setProcessors(processors);
        boolean hasErrors = false;
        if (!compileTask.call()) {
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                String msg = "\nJava Compilation " + diagnostic.getKind() + ":" + diagnostic.getSource()
                  + "(" + diagnostic.getLineNumber() + "," + diagnostic.getColumnNumber() + ")\n"
                  + "   " + diagnostic.getMessage(null) + "\n";
                logger.severe(msg);
                if (!hasErrors && diagnostic.getKind().equals(Diagnostic.Kind.ERROR))
                    hasErrors = true;
            }
            if (hasErrors) {
                logger.debug("Dynamic Java Compiler Classpath: " + classpath);
                throw new CompilationException("Compilation errors in Dynamic Java. See compiler output in log for details.");
            }
        }
    }

    private static List<Class<?>> compileJava(ClassLoader parentLoader, final Package currentPackage, Map<String,String> javaSources, boolean cache)
    throws ClassNotFoundException, IOException, MdwJavaException {
        if (parentLoader == null)
            parentLoader = CompiledJavaCache.class.getClassLoader();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
            throw new MdwJavaException("No Java compiler available");

        String classNames = "";
        List<JavaFileObject> jfos = new ArrayList<>();
        for (String className : javaSources.keySet()) {
            jfos.add(new StringJavaFileObject(className, javaSources.get(className)));
            classNames += className + ", ";
        }
        classNames = classNames.substring(0, classNames.length() - 2);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        JavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        MdwJavaFileManager<JavaFileManager> mdwFileManager = new MdwJavaFileManager<>(standardFileManager);

        // compiler options
        List<String> options = new ArrayList<>();

        // compiler classpath
        String pathSep = System.getProperty("path.separator");
        String classpath = getJavaCompilerClasspath(currentPackage);
        classpath += pathSep + getTempDir();  // include java source artifacts

        String debug = "Compiling Dynamic Java classes: " + classNames;
        if (logger.isMdwDebugEnabled()) {
            String extra = "parent ClassLoader=" + parentLoader;
            if (currentPackage != null)
                extra += ", workflow package: " + currentPackage.getLabel();
            logger.debug(debug + " (" + extra + ")");
        }
        else if (logger.isDebugEnabled()) {
            logger.info(debug);
        }

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("Dynamic Java Compiler Classpath: " + classpath);
        }

        options.addAll(Arrays.asList("-g", "-classpath", classpath));
        String extraOptions = PropertyManager.getProperty(PropertyNames.MDW_JAVA_COMPILER_OPTIONS);
        if (extraOptions != null)
            options.addAll(Arrays.asList(extraOptions.split(" ")));

        CompilationTask compileTask = compiler.getTask(null, mdwFileManager, diagnostics, options, null, jfos);
        boolean hasErrors = false;
        List<String> erroredClasses = new ArrayList<>();
        List<String> compilableClasses;
        if (!compileTask.call()) {
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                String msg = "\nJava Compilation " + diagnostic.getKind() + ":" + diagnostic.getSource()
                  + "(" + diagnostic.getLineNumber() + "," + diagnostic.getColumnNumber() + ")\n"
                  + "   " + diagnostic.getMessage(null) + "\n";
                logger.severe(msg);
                if (diagnostic.getKind().equals(Diagnostic.Kind.ERROR)) {
                    hasErrors = true;
                    if (diagnostic.getSource() instanceof StringJavaFileObject) {
                        StringJavaFileObject source = (StringJavaFileObject)diagnostic.getSource();
                        if (!erroredClasses.contains(source.getClassName()))
                            erroredClasses.add(source.getClassName());
                    }
                }
            }
            if (hasErrors) {
                logger.debug("Dynamic Java Compiler Classpath: " + classpath);
                logger.severe("Compilation errors in Dynamic Java. See compiler output in log for details.");
                compilableClasses = new ArrayList<>();
                for (String className : javaSources.keySet()) {
                    if (!erroredClasses.contains(className))
                        compilableClasses.add(className);
                }
                // recompile only compilable classes (TODO: a better way)
                jfos.clear();
                for (String className : compilableClasses)
                    jfos.add(new StringJavaFileObject(className, javaSources.get(className)));
                if (compilableClasses.size() > 0) {
                    compileTask = compiler.getTask(null, mdwFileManager, diagnostics, options, null, jfos);
                    compileTask.call();
                }
            }
        }

        List<Class<?>> classes = new ArrayList<>();
        for (String className : javaSources.keySet()) {
            if (!erroredClasses.contains(className))
                classes.add(DynamicJavaClassLoader.getInstance(parentLoader, currentPackage, cache).loadClass(className));
        }
        return classes;
    }

    public void clearCache() {
        compiledCache.clear();
        compilerClasspaths.clear();
        classesNotFound.clear();
        MdwJavaFileManager.clearJfoCache();
        clearDynamicJavaRegisteredServices();
        DynamicJavaClassLoader.clearLoaderInstances();
        // garbage collection helps ensure dynamicism of loaded classes
        Runtime.getRuntime().gc();
    }

    public int getCacheSize() {
        return compiledCache.size();
    }

    public void loadCache() throws CachingException {

        try {
            logger.info("Loading Java cache...");
            long before = System.currentTimeMillis();
            initializeJavaSourceArtifacts();
            preCompileJavaSourceArtifacts();
            loadDynamicJavaRegisteredServices();
            if (logger.isDebugEnabled())
                logger.debug("Time to load Java cache: " + (System.currentTimeMillis() - before) + " ms");
        }
        catch (Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }

    public synchronized void refreshCache() throws CachingException {
        clearCache();
        loadCache();
    }

    private static volatile Map<String,String> compilerClasspaths = new ConcurrentHashMap<>();
    public static String getJavaCompilerClasspath(Package packageVO) throws IOException {
        String key;
        if (packageVO == null || packageVO.getName() == null)
            key= Packages.MDW_BASE;
        else
            key = packageVO.getName();

        String classpath = compilerClasspaths.get(key);
        if (classpath == null) {
            synchronized(compilerClasspaths) {
                classpath = compilerClasspaths.get(key);
                if (classpath == null && packageVO != null) {
                    CloudClasspath cloudClsPath = new CloudClasspath(packageVO.getCloudClassLoader());
                    cloudClsPath.read();
                    classpath = cloudClsPath.toString();
                    compilerClasspaths.put(key, classpath);
                }
            }
        }
        return classpath;
    }

    /**
     * Writes the java-language assets into the temporary directory.
     * This is only needed for compilation dependencies.
     */
    private static void initializeJavaSourceArtifacts() throws IOException, CachingException {
        logger.info("Initializing Java source assets...");
        long before = System.currentTimeMillis();

        for (Asset javaSource : AssetCache.getAssets(Asset.JAVA)) {
            Package pkg = PackageCache.getAssetPackage(javaSource.getId());
            String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getName());
            String className = JavaNaming.getValidClassName(javaSource.getName());
            File dir = createNeededDirs(packageName);
            File file = new File(dir + "/" + className + ".java");
            if (file.exists())
                file.delete();

            String javaCode = javaSource.getStringContent();
            if (javaCode != null) {
                javaCode = doCompatibilityCodeSubstitutions(packageName + "." + className, javaCode);
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(javaCode);
                }
            }
        }
        if (logger.isDebugEnabled())
            logger.debug("Time to initialize Java source assets: " + (System.currentTimeMillis() - before) + " ms");
    }

    /**
     *  Precompile designated Java Source artifacts.
     */
    private static void preCompileJavaSourceArtifacts() {
        if (preCompiled != null) {
            for (String preCompClass : preCompiled) {
                logger.info("Precompiling dynamic Java asset class: " + preCompClass);
                try {
                    Asset javaAsset = AssetCache.getAsset(preCompClass, Asset.JAVA);
                    Package pkg = PackageCache.getAssetPackage(javaAsset.getId());
                    String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getName());
                    String className = (pkg == null ? "" : packageName + ".") + JavaNaming.getValidClassName(javaAsset.getName());
                    getClass(null, pkg, className, javaAsset.getStringContent());
                }
                catch (Exception ex) {
                    // let other classes continue to process
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
    }

    private static File createNeededDirs(String packageName) {
        File rootDir = new File(getTempDir());
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        String path = getTempDir();
        File dir = new File(path);
        if (packageName != null) {
            StringTokenizer st = new StringTokenizer(packageName, ".");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                path += "/" + token;
                dir = new File(path);
                if (!dir.exists())
                    dir.mkdir();
            }
        }
        return dir;
    }

    private static String getTempDir() {
        return ApplicationContext.getTempDirectory();
    }

    public static class DynamicJavaClassLoader extends ClassLoader {

        private Package packageVO;
        private boolean cache;

        private static Map<String, Object> parallelLockMap = new ConcurrentHashMap<>();
        private static Map<String, DynamicJavaClassLoader> instances = new ConcurrentHashMap<>();

        static {
            ClassLoader.registerAsParallelCapable();
        }

        DynamicJavaClassLoader(ClassLoader parentLoader, Package packageVO, boolean cache) {
            super(DynamicJavaClassLoader.class.getClassLoader()); //packageVO == null ? DynamicJavaClassLoader.class.getClassLoader() : packageVO.getCloudClassLoader());
            this.packageVO = packageVO;
            this.cache = cache;
        }

        public static DynamicJavaClassLoader getInstance(ClassLoader parentLoader, Package packageVO, boolean cache) {
            String key = packageVO.getName() + ""; // To handle the default package which has null name
            if (instances.containsKey(key))
                return instances.get(key);
            else {
                synchronized (instances) {
                    Map<String, DynamicJavaClassLoader> temp = instances;
                    if (temp.containsKey(key))
                        return temp.get(key);
                    else {
                        DynamicJavaClassLoader loader = new DynamicJavaClassLoader(parentLoader, packageVO, cache);
                        instances.put(key, loader);
                        return loader;
                    }
                }
            }
        }

        public static void clearLoaderInstances() {
            instances.clear();
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("findClass(): " + name);
            Class<?> cl = null;
            if (classicLoading || classesNotFound.get(name) == null) {
                // check the cache first
                cl = compiledCache.get(name);
                if (cl == null) {
                    JavaFileObject jfo = MdwJavaFileManager.getJavaFileObject(name);
                    if (jfo == null) {
                        Asset javaAsset = AssetCache.getAsset(name, Asset.JAVA);
                        if (javaAsset != null) {
                            try {
                                String javaCode = javaAsset.getStringContent();
                                if (Compatibility.hasCodeSubstitutions())
                                    javaCode = doCompatibilityCodeSubstitutions(name, javaCode);
                                compileJavaCode(getParent(), packageVO, name, javaCode, cache);
                                jfo = MdwJavaFileManager.getJavaFileObject(name);
                            } catch (Exception ex) {
                                logger.severeException(ex.getMessage(), ex);
                            }
                        }
                    }
                    if (jfo != null) {
                        if (packageVO != null && packageVO.getName() != null) {
                            java.lang.Package pkg = getPackage(packageVO.getName());
                            if (pkg == null)
                                definePackage(packageVO.getName(), null, null, null, "MDW", packageVO.getVersionString(), "CenturyLink", null);
                        }
                        byte[] bytes = ((ByteArrayJavaFileObject) jfo).getByteArray();
                        cl = defineClass(name, bytes, 0, bytes.length);
                    }
                } else  // Found it in cache
                    return cl;

                // try the cloud classloader to find class in assets
                if (cl == null && packageVO != null && packageVO.getCloudClassLoader().hasClass(name) /* prevent infinite loop */)
                    cl = packageVO.getCloudClassLoader().directFindClass(name);
            }
            if (cl == null) {
                if (!classicLoading) classesNotFound.putIfAbsent(name, true);
                // don't log: can happen when trying multiple bundles to resolve a class
                throw new ClassNotFoundException(cnfeMsg(name));
            } else if (cache) {
                Class<?> tempCl = compiledCache.putIfAbsent(name, cl);
                if (tempCl != null)
                    cl = tempCl;
            }
            return cl;
        }

        private String cnfeMsg(String className) {
            String msg = className + " with Parent ClassLoader: " + getParent();
            if (packageVO != null)
                msg += "\nand Workflow Package: " + packageVO.getLabel() + " (ClassLoader: " + packageVO.getClassLoader() + ")";
            return msg;
        }

        public String toString() {
            String str = getClass().getName() + " with parent " + getParent();
            if (packageVO != null)
                str += "\nand Workflow Package: " + packageVO.getLabel() + " (ClassLoader: " + packageVO.getClassLoader() + ")";
            return str;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> loaded;
            try {
                synchronized (getStaticClassLoadingLock(name)) {  // Only allow 1 class loader instance to load same class
                    // Look in assets first (overrides classes provided by built-in JARs), unless classicLoading is true
                    loaded = classicLoading ? super.loadClass(name) : findClass(name);
                }
            } catch (ClassNotFoundException | LinkageError e) {
                if (classicLoading)
                    throw e;
                else
                    loaded = super.loadClass(name);  // Not in assets, so look in parent loaders.
            }
            if (logger.isMdwDebugEnabled()) {
                logger.mdwDebug("Loaded class: '" + name + "' from DynamicJavaClassLoader with parent: " + getParent());
                if (logger.isTraceEnabled())
                    logger.traceException("Stack trace: ", new Exception("ClassLoader stack trace"));
            }
            return loaded;
        }

        /**
         * Prevents multiple instances of dynamicJavaClassLoader from trying to load
         * the same class at the same time
         */
        private Object getStaticClassLoadingLock(String className) {
            Object lock;
            Object newLock = new Object();
            lock = parallelLockMap.putIfAbsent(className, newLock);
            if (lock == null) {
                lock = newLock;
            }
            return lock;
        }
    }

    protected static String doCompatibilityCodeSubstitutions(String label, String in) throws IOException {
        SubstitutionResult substitutionResult = Compatibility.getInstance().performCodeSubstitutions(in);
        if (!substitutionResult.isEmpty()) {
            logger.warn("Compatibility substitutions applied for Java asset " + label + " (details logged at debug level).");
            if (logger.isDebugEnabled())
                logger.debug("Compatibility substitutions for " + label + ":\n" + substitutionResult.getDetails());
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Substitution output for " + label + ":\n" + substitutionResult.getOutput());
            return substitutionResult.getOutput();
        }
        return in;
    }

    /**
     * @param parentClassLoader parentClassLoader
     * @param className className
     * @throws CachingException CachingException
     * @throws IOException IOException
     * @throws ClassNotFoundException ClassNotFoundException
     * @throws MdwJavaException MdwJavaException
     */
    public static Class<?> getClassFromAssetName(ClassLoader parentClassLoader, String className) throws CachingException, MdwJavaException, ClassNotFoundException, IOException {
        Class<?> clazz;
        Package javaAssetPackage = PackageCache.getJavaAssetPackage(className);
        Asset javaAsset = AssetCache.getAsset(className, Asset.JAVA);
        if (parentClassLoader == null) {
            parentClassLoader = javaAssetPackage.getClassLoader();
        }
        clazz = getClass(parentClassLoader, javaAssetPackage, className, javaAsset.getStringContent());
        return clazz;
    }
}

