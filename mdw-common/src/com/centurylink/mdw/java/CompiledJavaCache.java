/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.java;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.ws.rs.Path;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cloud.CloudClasspath;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.Compatibility.SubstitutionResult;
import com.centurylink.mdw.common.cache.AssetCache;
import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.DynamicJavaServiceRegistry;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;

/**
 * Compiles and caches java asset classes, and provides a class-loading mechanism to allow these assets
 * to reference Jar classes as well.
 */
public class CompiledJavaCache implements PreloadableCache, AssetCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static Map<ClassCacheKey,Class<?>> compiledCache = new HashMap<ClassCacheKey,Class<?>>();

    public CompiledJavaCache() {

    }

    public CompiledJavaCache(Map<String,String> params) {
        initialize(params);
    }

    private static String[] preCompiled;

    public void initialize(Map<String,String> params) {
        if (params != null) {
            String preCompString = params.get("PreCompiled");
            if (preCompString != null && preCompString.trim().length() > 0) {
                List<String> preCompList = new ArrayList<String>();
                preCompiled = preCompString.split("\\\n");
                for (int i = 0; i < preCompiled.length; i++) {
                    String preLoad = preCompiled[i].trim();
                    if (!preLoad.isEmpty())
                      preCompList.add(preLoad);
                }
                preCompiled = preCompList.toArray(new String[]{});
            }
        }
    }

    public String getFormat() {
        return RuleSetVO.JAVA;
    }

    /**
     * To load all the dynamic java Registered services classes
     * @param string
     */
    private void loadDynamicJavaRegisteredServices() {
        Map<PackageVO,Map<String,String>> packagedJava = new HashMap<PackageVO,Map<String,String>>();
        try {
            for (RuleSetVO javaRuleSet : RuleSetCache.getRuleSets(RuleSetVO.JAVA)) {
                // process RegisteredService-annotated classes
                if (javaRuleSet.getRuleSet().indexOf("@RegisteredService") > 0 ||
                        javaRuleSet.getRuleSet().indexOf("@Path") > 0 || javaRuleSet.getRuleSet().indexOf("@Api") > 0) {
                    String className = JavaNaming.getValidClassName(javaRuleSet.getName());
                    PackageVO javaAssetPackage = PackageVOCache.getRuleSetPackage(javaRuleSet.getId());
                    if (javaAssetPackage == null) {
                        logger.severe("Omitting unpackaged Registered Service from compilation: " + javaRuleSet.getLabel());
                    }
                    else {
                        String qName = JavaNaming.getValidPackageName(javaAssetPackage.getPackageName()) + "." + className;
                        Map<String,String> javaSources = packagedJava.get(javaAssetPackage);
                        if (javaSources == null) {
                            javaSources = new HashMap<String,String>();
                            packagedJava.put(javaAssetPackage, javaSources);
                        }
                        javaSources.put(qName, javaRuleSet.getRuleSet());
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }

        if (!packagedJava.isEmpty()) {
            for (PackageVO pkg : packagedJava.keySet()) {
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
                                    logger.info("Dynamic Java JAX-RS JSON Service: " + clazz);
                                    DynamicJavaServiceRegistry.addRegisteredService(JsonService.class.getName(), clazz.getName(), resourcePath);
                                }
                                else if (XmlService.class.isAssignableFrom(clazz)) {
                                    logger.info("Dynamic Java JAX-RS XML Service: " + clazz);
                                    DynamicJavaServiceRegistry.addRegisteredService(XmlService.class.getName(), clazz.getName(), resourcePath);
                                }
                            }
                        }
                        else {
                            for (int i = 0; i < registeredService.value().length; i++) {
                                String serviceName = registeredService.value()[i].getName();
                                logger.info("Dynamic Java @RegisteredService: " + serviceName + " Class: " + clazz);
                                DynamicJavaServiceRegistry.addRegisteredService(serviceName, clazz.getName());
                            }
                        }
                    }
                }
                catch (Exception ex) {
                  // let other packages continue to process
                  logger.severeException("Failed to process Dynamic Java Managed Beans for package " + pkg.getLabel() + ": " + ex.getMessage(), ex);
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

    public static Class<?> getClass(PackageVO currentPackage, String className, String javaCode)
    throws ClassNotFoundException, IOException, MdwJavaException {
        return getClass(null, currentPackage, className, javaCode);
    }

    public static Class<?> getClass(ClassLoader parentLoader, PackageVO currentPackage, String className, String javaCode)
    throws ClassNotFoundException, IOException, MdwJavaException {
        return getClass(parentLoader, currentPackage, className, javaCode, true);
    }

    public static Class<?> getClass(ClassLoader parentLoader, PackageVO currentPackage, String className, String javaCode, boolean cache)
    throws ClassNotFoundException, IOException, MdwJavaException {
        ClassCacheKey key = new ClassCacheKey(className, parentLoader);
        Class<?> clazz = cache ? compiledCache.get(key) : null;
        if (clazz == null) {
            try {
                if (Compatibility.hasCodeSubstitutions())
                    javaCode = doCompatibilityCodeSubstitutions(className, javaCode);
                clazz = compileJavaCode(parentLoader, currentPackage, className, javaCode, cache);
                if (cache) {
                    key.compiled = new Date();
                    synchronized (compiledCache) {
                        compiledCache.remove(key); // needs to be removed since it may have been added by the classloader
                        compiledCache.put(key, clazz);
                    }
                }
            }
            catch (ClassNotFoundException ex) {
                throw ex;
            }
            catch (IOException ex) {
                throw ex;
            }
            catch (MdwJavaException ex) {
                throw ex;
            }
            catch (Throwable t) {
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
    public static List<Class<?>> compileClasses(ClassLoader parentLoader, PackageVO currentPackage, Map<String,String> javaSources, boolean cache)
    throws ClassNotFoundException, IOException, MdwJavaException {
        try {
            for (String className : javaSources.keySet()) {
                if (Compatibility.hasCodeSubstitutions())
                    javaSources.put(className, doCompatibilityCodeSubstitutions(className, javaSources.get(className)));
            }
            List<Class<?>> classes = compileJava(parentLoader, currentPackage, javaSources, cache);
            if (cache) {
                for (Class<?> clazz : classes) {
                    ClassCacheKey key = new ClassCacheKey(clazz.getName(), parentLoader);
                    key.compiled = new Date();
                    synchronized (compiledCache) {
                        compiledCache.remove(key); // needs to be removed since it may have been added by the classloader
                        compiledCache.put(key, clazz);
                    }
                }
            }
            return classes;
        }
        catch (ClassNotFoundException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw ex;
        }
        catch (MdwJavaException ex) {
            throw ex;
        }
        catch (Throwable t) {
            logger.severeException(t.getMessage(), t);
            // don't let compilation errors prevent startup
            return null;
        }
    }

    public static Class<?> getResourceClass(String className, ClassLoader parentLoader, PackageVO currentPackage)
    throws ClassNotFoundException, IOException, MdwJavaException {
        if (className.indexOf('$') > 0) {
            // inner class -- check previously loaded
            synchronized (compiledCache) {
                for (ClassCacheKey key : compiledCache.keySet()) {
                    if (key.equals(new ClassCacheKey(className, parentLoader))) {
                        Class<?> clazz = compiledCache.get(key);
                        try {
                            return clazz.getClassLoader().loadClass(className);
                        }
                        catch (ClassNotFoundException cnfe) {
                            // try next loaded class
                        }
                    }
                }
            }
        }
        RuleSetVO javaRuleSet = RuleSetCache.getRuleSet(className, RuleSetVO.JAVA);
        if (javaRuleSet == null)
            throw new ClassNotFoundException(className);

        try {
            if (currentPackage == null) {
                // use the java asset package
                int lastDot = className.lastIndexOf('.');
                if (lastDot == -1)  {
                    // default package
                    currentPackage = PackageVOCache.getDefaultPackageVO();
                }
                else {
                    String packageName = className.substring(0, lastDot);
                    currentPackage = PackageVOCache.getPackageVO(packageName);
                }
            }

            return getClass(parentLoader, currentPackage, className, javaRuleSet.getRuleSet());
        }
        catch (CachingException ex) {
            throw new MdwJavaException(ex.getMessage(), ex);
        }
    }

    public static Object getInstance(String resourceClassName, ClassLoader parentLoader, PackageVO currentPackage) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException, MdwJavaException  {
        return getResourceClass(resourceClassName, parentLoader, currentPackage).newInstance();
    }

    private static Class<?> compileJavaCode(ClassLoader parentLoader, final PackageVO currentPackage, String className, String javaCode, boolean cache)
    throws ClassNotFoundException, IOException, MdwJavaException {
        if (parentLoader == null)
            parentLoader = CompiledJavaCache.class.getClassLoader();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
            throw new MdwJavaException("No Java compiler available");

        final JavaFileObject jfo = new StringJavaFileObject(className, javaCode);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        JavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        MdwJavaFileManager<JavaFileManager> mdwFileManager = new MdwJavaFileManager<JavaFileManager>(standardFileManager);

        // compiler options
        List<String> options = new ArrayList<String>();

        // compiler classpath
        String pathSep = System.getProperty("path.separator");
        String classpath = getJavaCompilerClasspath(parentLoader, currentPackage);
        classpath += pathSep + getTempDir();  // include java source artifacts

        String info = "Compiling Dynamic Java class: " + className;
        if (logger.isDebugEnabled()) {
            String extra = "parent ClassLoader=" + parentLoader;
            if (currentPackage != null)
                extra += ", workflow package: " + currentPackage.getLabel();
            logger.debug(info + " (" + extra + ")");
        }
        else {
            logger.info(info);
        }

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("Dynamic Java Compiler Classpath: " + classpath);
        }

        options.addAll(Arrays.asList("-g", "-classpath", classpath));
        String extraOptions = PropertyManager.getProperty(PropertyNames.MDW_DYNAMIC_JAVA_COMPILE_OPTIONS);
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

        return new DynamicJavaClassLoader(parentLoader, currentPackage, cache).loadClass(className);
    }

    private static List<Class<?>> compileJava(ClassLoader parentLoader, final PackageVO currentPackage, Map<String,String> javaSources, boolean cache)
    throws ClassNotFoundException, IOException, MdwJavaException {
        if (parentLoader == null)
            parentLoader = CompiledJavaCache.class.getClassLoader();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
            throw new MdwJavaException("No Java compiler available");

        String classNames = "";
        List<JavaFileObject> jfos = new ArrayList<JavaFileObject>();
        for (String className : javaSources.keySet()) {
            jfos.add(new StringJavaFileObject(className, javaSources.get(className)));
            classNames += className + ", ";
        }
        classNames = classNames.substring(0, classNames.length() - 2);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        JavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        MdwJavaFileManager<JavaFileManager> mdwFileManager = new MdwJavaFileManager<JavaFileManager>(standardFileManager);

        // compiler options
        List<String> options = new ArrayList<String>();

        // compiler classpath
        String pathSep = System.getProperty("path.separator");
        String classpath = getJavaCompilerClasspath(parentLoader, currentPackage);
        classpath += pathSep + getTempDir();  // include java source artifacts

        String info = "Compiling Dynamic Java classes: " + classNames;
        if (logger.isDebugEnabled()) {
            String extra = "parent ClassLoader=" + parentLoader;
            if (currentPackage != null)
                extra += ", workflow package: " + currentPackage.getLabel();
            logger.debug(info + " (" + extra + ")");
        }
        else {
            logger.info(info);
        }

        if (logger.isMdwDebugEnabled()) {
            logger.mdwDebug("Dynamic Java Compiler Classpath: " + classpath);
        }

        options.addAll(Arrays.asList("-g", "-classpath", classpath));
        String extraOptions = PropertyManager.getProperty(PropertyNames.MDW_DYNAMIC_JAVA_COMPILE_OPTIONS);
        if (extraOptions != null)
            options.addAll(Arrays.asList(extraOptions.split(" ")));

        CompilationTask compileTask = compiler.getTask(null, mdwFileManager, diagnostics, options, null, jfos);
        boolean hasErrors = false;
        List<String> erroredClasses = new ArrayList<String>();
        List<String> compilableClasses = null;
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
                compilableClasses = new ArrayList<String>();
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

        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (String className : javaSources.keySet()) {
            if (!erroredClasses.contains(className))
                classes.add(new DynamicJavaClassLoader(parentLoader, currentPackage, cache).loadClass(className));
        }
        return classes;
    }

    public void clearCache() {
        synchronized (compiledCache) {
            compiledCache.clear();
        }
        synchronized(compilerClasspaths) {
            compilerClasspaths.clear();
        }
        clearDynamicJavaRegisteredServices();
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
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }

    public synchronized void refreshCache() throws CachingException {
        clearCache();
        loadCache();
    }

    private static volatile Map<CompilerClasspathKey,String> compilerClasspaths = Collections.synchronizedMap(new HashMap<CompilerClasspathKey,String>());
    public static String getJavaCompilerClasspath(ClassLoader parentLoader, PackageVO packageVO) throws IOException {
        CompilerClasspathKey key = new CompilerClasspathKey(parentLoader);
        String classpath = compilerClasspaths.get(key);
        if (classpath == null) {
            synchronized(compilerClasspaths) {
                classpath = compilerClasspaths.get(key);
                if (classpath == null) {
                    CloudClasspath cloudClsPath = new CloudClasspath(packageVO == null ? null : packageVO.getCloudClassLoader());
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
    private static void initializeJavaSourceArtifacts() throws DataAccessException, IOException, CachingException {
        logger.info("Initializing Java source assets...");
        long before = System.currentTimeMillis();

        for (RuleSetVO javaSource : RuleSetCache.getRuleSets(RuleSetVO.JAVA)) {
            PackageVO pkg = PackageVOCache.getRuleSetPackage(javaSource.getId());
            String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getPackageName());
            String className = JavaNaming.getValidClassName(javaSource.getName());
            File dir = createNeededDirs(packageName);
            File file = new File(dir + "/" + className + ".java");
            if (file.exists())
                file.delete();

            String javaCode = javaSource.getRuleSet();
            if (javaCode != null) {
                javaCode = doCompatibilityCodeSubstitutions(packageName + "." + className, javaCode);
                FileWriter writer = new FileWriter(file);
                writer.write(javaCode);
                writer.close();
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
                    RuleSetVO javaRuleSet = RuleSetCache.getRuleSet(preCompClass, RuleSetVO.JAVA);
                    PackageVO pkg = PackageVOCache.getRuleSetPackage(javaRuleSet.getId());
                    String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getPackageName());
                    String className = (pkg == null ? "" : packageName + ".") + JavaNaming.getValidClassName(javaRuleSet.getName());
                    getClass(null, pkg, className, javaRuleSet.getRuleSet());
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

    static class ClassCacheKey {
        private String className;
        private ClassLoader parentLoader;
        public ClassLoader getParentLoader() { return parentLoader; }
        private Date compiled;
        public Date getCompiled() { return compiled; }

        public ClassCacheKey(String className, ClassLoader parentLoader) {
            this.className = className;
            this.parentLoader = parentLoader;
        }

        public String toString() {
            return className + " w/parentLoader: " + parentLoader;
        }

        public boolean equals(Object other) {
            if (!(other instanceof ClassCacheKey))
                return false;
            return toString().equals(other.toString());
        }

        public int hashCode() {
            return toString().hashCode();
        }
    }

    static class CompilerClasspathKey {
        private ClassLoader parentLoader;

        CompilerClasspathKey(ClassLoader parentLoader) {
            this.parentLoader = parentLoader;
        }

        public String toString() {
            return "parentLoader: " + parentLoader;
        }

        public boolean equals(Object other) {
            if (!(other instanceof CompilerClasspathKey))
                return false;
            return toString().equals(other.toString());
        }

        public int hashCode() {
            return toString().hashCode();
        }
    }

    static class DynamicJavaClassLoader extends ClassLoader {

        private PackageVO packageVO;
        private boolean cache;

        DynamicJavaClassLoader(ClassLoader parentLoader, PackageVO packageVO, boolean cache) {
            super(parentLoader);
            this.packageVO = packageVO;
            this.cache = cache;
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("findClass(): " + name);
            Class<?> cl = null;
            // check the cache first
            ClassCacheKey key = new ClassCacheKey(name, this.getParent());
            cl = compiledCache.get(key);
            if (cl == null) {
                JavaFileObject jfo = MdwJavaFileManager.getJavaFileObject(name);
                if (jfo != null) {
                    if (packageVO != null && packageVO.getName() != null) {
                        Package pkg = getPackage(packageVO.getName());
                        if (pkg == null)
                            definePackage(packageVO.getName(), null, null, null, "MDW", packageVO.getVersionString(), "CenturyLink", null);
                    }
                    byte[] bytes = ((ByteArrayJavaFileObject)jfo).getByteArray();
                    cl = defineClass(name, bytes, 0, bytes.length);
                }
            }
            // try the package classloader
            if (cl == null && packageVO != null) {
                ClassLoader packageCl = packageVO.getClassLoader();
                if (packageCl != null) {
                    try {
                        cl = packageCl.loadClass(name);
                    }
                    catch (ClassNotFoundException ex) {
                        // more meaningful CNFE is thrown below
                    }
                }
            }
            // try the cloud classloader
            if (cl == null && packageVO != null && packageVO.getCloudClassLoader().hasClass(name) /* prevent infinite loop */ )
                cl = packageVO.getCloudClassLoader().loadClass(name);

            if (cl == null) {
                // don't log: can happen when trying multiple bundles to resolve a class
                throw new ClassNotFoundException(cnfeMsg(name));
            }
            if (cache) {
                synchronized (compiledCache) {
                    compiledCache.put(key, cl);  // compiled date is only set at compile time
                }
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
            if (!logger.isMdwDebugEnabled()) {
                return super.loadClass(name);
            }
            else {
                Class<?> loaded = super.loadClass(name);
                logger.mdwDebug("Loaded class: '" + name + "' from DynamicJavaClassLoader with parent: " + getParent());
                if (logger.isTraceEnabled())
                    logger.traceException("Stack trace: ", new Exception("ClassLoader stack trace"));
                return loaded;
            }
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
     * @param parentClassLoader
     * @param className
     * @throws CachingException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws MdwJavaException
     */
    public static Class<?> getClassFromAssetName(ClassLoader parentClassLoader, String className) throws CachingException, MdwJavaException, ClassNotFoundException, IOException {
        Class<?> clazz = null;
        PackageVO javaAssetPackage = PackageVOCache.getJavaRuleSetPackage(className);
        RuleSetVO javaRuleSet = RuleSetCache.getRuleSet(className, RuleSetVO.JAVA);
        if (parentClassLoader == null) {
            parentClassLoader = javaAssetPackage.getClassLoader();
        }
        clazz = getClass(parentClassLoader, javaAssetPackage, className, javaRuleSet.getRuleSet());
        return clazz;
    }
}

