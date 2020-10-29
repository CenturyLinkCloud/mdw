package com.centurylink.mdw.java;

import com.centurylink.mdw.annotations.Monitor;
import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.file.Packages;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.pkg.PackageClasspath;
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
public class CompiledJavaCache implements PreloadableCache {

    public final static boolean classicLoading = PropertyManager.getBooleanProperty(PropertyNames.MDW_CONTAINER_CLASSIC_CLASSLOADING, false);
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final Map<String,Class<?>> compiledCache = new ConcurrentHashMap<>();
    private static final Map<String,Boolean> classesNotFound = new ConcurrentHashMap<>();

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

    /**
     * To load all the dynamic java Registered services classes
     */
    private void loadDynamicRegisteredServices() {
        Map<Package,Map<String,String>> packagedJava = new HashMap<>();
        try {
            for (Asset javaAsset : AssetCache.getAssets("java")) {
                // process RegisteredService-annotated classes
                if (javaAsset.getText().indexOf("@RegisteredService") > 0 ||
                        javaAsset.getText().indexOf("@Monitor") > 0 ||
                        javaAsset.getText().indexOf("@Path") > 0 ||
                        javaAsset.getText().indexOf("@Api") > 0 ||
                        javaAsset.getText().indexOf("@ScheduledJob") > 0) {
                    String className = JavaNaming.getValidClassName(javaAsset.getName());
                    Package javaAssetPackage = PackageCache.getPackage(javaAsset.getPackageName());
                    if (javaAssetPackage == null) {
                        logger.error("Omitting unpackaged Registered Service from compilation: " + javaAsset.getLabel());
                    }
                    else {
                        String qName = JavaNaming.getValidPackageName(javaAssetPackage.getName()) + "." + className;
                        Map<String,String> javaSources = packagedJava.get(javaAssetPackage);
                        if (javaSources == null) {
                            javaSources = new HashMap<>();
                            packagedJava.put(javaAssetPackage, javaSources);
                        }
                        javaSources.put(qName, javaAsset.getText());
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
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
                                String resourcePath = pathAnnotation.value();
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
                  logger.error("Failed to process Dynamic Java services in package " + pkg.getLabel() + ": " + ex.getMessage(), ex);
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
                compileJavaCode(parentLoader, currentPackage, className, javaCode);
                clazz = DynamicJavaClassLoader.getInstance(parentLoader, currentPackage, cache).loadClass(className);
            }
            catch (ClassNotFoundException | IOException | MdwJavaException ex) {
                throw ex;
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
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
            return compileJava(parentLoader, currentPackage, javaSources, cache);
        }
        catch (ClassNotFoundException | MdwJavaException | IOException ex) {
            throw ex;
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
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
        Asset javaAsset = AssetCache.getJavaAsset(className);
        if (javaAsset == null)
            throw new ClassNotFoundException(className);

        try {
            if (currentPackage == null) {
                // use the java asset package
                int lastDot = className.lastIndexOf('.');
                String packageName = className.substring(0, lastDot);
                currentPackage = PackageCache.getPackage(packageName);
            }

            return getClass(parentLoader, currentPackage, className, javaAsset.getText());
        }
        catch (CachingException ex) {
            throw new MdwJavaException(ex.getMessage(), ex);
        }
    }

    public static Object getInstance(String resourceClassName, ClassLoader parentLoader, Package currentPackage) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException, MdwJavaException  {
        Class<?> resourceClass = getResourceClass(resourceClassName, parentLoader, currentPackage);
        return resourceClass == null ? null : resourceClass.newInstance();
    }

    private static void compileJavaCode(ClassLoader parentLoader, final Package currentPackage, String className, String javaCode)
    throws IOException, MdwJavaException {
        if (parentLoader == null)
            parentLoader = CompiledJavaCache.class.getClassLoader();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
            throw new MdwJavaException("No Java compiler available.  JDK must precede JRE on system PATH.");

        final JavaFileObject jfo = new StringJavaFileObject(className, javaCode);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        JavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        MdwJavaFileManager<JavaFileManager> mdwFileManager = new MdwJavaFileManager<>(standardFileManager);

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

        // compiler options
        List<String> options = new ArrayList<>(Arrays.asList("-g", "-classpath", classpath));
        String extraOptions = PropertyManager.getProperty(PropertyNames.MDW_JAVA_COMPILER_OPTIONS);
        if (extraOptions != null)
            options.addAll(Arrays.asList(extraOptions.split(" ")));

        CompilationTask compileTask = compiler.getTask(null, mdwFileManager, diagnostics, options, null, Arrays.asList(jfo));
        boolean hasErrors = false;
        if (!compileTask.call()) {
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                String msg = "\nJava Compilation " + diagnostic.getKind() + ":" + diagnostic.getSource()
                  + "(" + diagnostic.getLineNumber() + "," + diagnostic.getColumnNumber() + ")\n"
                  + "   " + diagnostic.getMessage(null) + "\n";
                logger.error(msg);
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

        // compiler options
        List<String> options = new ArrayList<>(Arrays.asList("-g", "-classpath", classpath));
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
                logger.error(msg);
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
                logger.error("Compilation errors in Dynamic Java. See compiler output in log for details.");
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

    public void loadCache() throws CachingException {

        try {
            logger.info("Loading Java cache...");
            long before = System.currentTimeMillis();
            initializeJavaSourceArtifacts();
            preCompileJavaSourceArtifacts();
            loadDynamicRegisteredServices();
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

    private static final Map<String,String> compilerClasspaths = new ConcurrentHashMap<>();
    public static String getJavaCompilerClasspath(Package pkg) throws IOException {
        String key;
        if (pkg == null || pkg.getName() == null)
            key= Packages.MDW_BASE;
        else
            key = pkg.getName();

        String classpath = compilerClasspaths.get(key);
        if (classpath == null) {
            synchronized(compilerClasspaths) {
                classpath = compilerClasspaths.get(key);
                if (classpath == null && pkg != null) {
                    PackageClasspath packageClasspath = new PackageClasspath(pkg.getClassLoader());
                    packageClasspath.read();
                    classpath = packageClasspath.toString();
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

        for (Asset javaSource : AssetCache.getAssets("java")) {
            Package pkg = PackageCache.getPackage(javaSource.getPackageName());
            String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getName());
            String className = JavaNaming.getValidClassName(javaSource.getName());
            File dir = createNeededDirs(packageName);
            File file = new File(dir + "/" + className + ".java");
            if (file.exists())
                file.delete();

            String javaCode = javaSource.getText();
            if (javaCode != null) {
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
                    Asset javaAsset = AssetCache.getJavaAsset(preCompClass);
                    Package pkg = PackageCache.getPackage(javaAsset.getPackageName());
                    String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getName());
                    String className = (pkg == null ? "" : packageName + ".") + JavaNaming.getValidClassName(javaAsset.getName());
                    getClass(null, pkg, className, javaAsset.getText());
                }
                catch (Exception ex) {
                    // let other classes continue to process
                    logger.error(ex.getMessage(), ex);
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

        private final Package pkg;
        private final boolean cache;

        private static final Map<String,Object> parallelLockMap = new ConcurrentHashMap<>();
        private static final Map<String,DynamicJavaClassLoader> instances = new ConcurrentHashMap<>();

        static {
            ClassLoader.registerAsParallelCapable();
        }

        DynamicJavaClassLoader(ClassLoader parentLoader, Package pkg, boolean cache) {
            super(DynamicJavaClassLoader.class.getClassLoader());
            this.pkg = pkg;
            this.cache = cache;
        }

        public static DynamicJavaClassLoader getInstance(ClassLoader parentLoader, Package pkg, boolean cache) {
            String pkgName = pkg.getName();
            if (instances.containsKey(pkgName))
                return instances.get(pkgName);
            else {
                synchronized (instances) {
                    Map<String,DynamicJavaClassLoader> temp = instances;
                    if (temp.containsKey(pkgName))
                        return temp.get(pkgName);
                    else {
                        DynamicJavaClassLoader loader = new DynamicJavaClassLoader(parentLoader, pkg, cache);
                        instances.put(pkgName, loader);
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
                        try {
                            Asset javaAsset = AssetCache.getJavaAsset(name);
                            if (javaAsset != null) {
                                String javaCode = javaAsset.getText();
                                compileJavaCode(getParent(), pkg, name, javaCode);
                                jfo = MdwJavaFileManager.getJavaFileObject(name);
                            }
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                    if (jfo != null) {
                        if (pkg != null && pkg.getName() != null) {
                            java.lang.Package pkg = getPackage(this.pkg.getName());
                            if (pkg == null)
                                definePackage(this.pkg.getName(), null, null, null, "MDW",
                                        this.pkg.getVersion().toString(), "CenturyLink", null);
                        }
                        byte[] bytes = ((ByteArrayJavaFileObject) jfo).getByteArray();
                        cl = defineClass(name, bytes, 0, bytes.length);
                    }
                } else  // Found it in cache
                    return cl;

                // try the package classloader to find class in assets
                if (cl == null && pkg != null && pkg.getClassLoader().hasClass(name) /* prevent infinite loop */)
                    cl = pkg.getClassLoader().directFindClass(name);
            }
            if (cl == null) {
                if (!classicLoading)
                    classesNotFound.putIfAbsent(name, true);
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
            if (pkg != null)
                msg += "\nand Workflow Package: " + pkg.getLabel() + " (ClassLoader: " + pkg.getClass().getClassLoader() + ")";
            return msg;
        }

        public String toString() {
            String str = getClass().getName() + " with parent " + getParent();
            if (pkg != null)
                str += "\nand Workflow Package: " + pkg.getLabel() + " (ClassLoader: " + pkg.getClass().getClassLoader() + ")";
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
                    logger.trace("Stack trace: ", new Exception("ClassLoader stack trace"));
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

    /**
     * @param parentClassLoader parentClassLoader
     * @param className className
     */
    public static Class<?> getClassFromAssetName(ClassLoader parentClassLoader, String className)
            throws CachingException, MdwJavaException, ClassNotFoundException, IOException {
        Class<?> clazz;
        int lastDot = className.lastIndexOf('.');
        String packageName = className.substring(0, lastDot);
        Package pkg = PackageCache.getPackage(packageName);
        Asset javaAsset = AssetCache.getJavaAsset(className);
        if (parentClassLoader == null) {
            parentClassLoader = pkg.getClass().getClassLoader();
        }
        clazz = getClass(parentClassLoader, pkg, className, javaAsset.getText());
        return clazz;
    }
}

