/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.kotlin;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.java.CompilationException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.file.MdwIgnore;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@RegisteredService(CacheService.class)
public class KotlinAccess implements CacheService, PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static KotlinAccess instance;
    public static synchronized KotlinAccess getInstance() {
        if (instance == null) {
            instance = new KotlinAccess();
            instance.load();
        }

        return instance;
    }

    private KotlinScriptEngine scriptEngine;
    public KotlinScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    private Map<String,KotlinCompiledScript> scripts;
    public Map<String,KotlinCompiledScript> getScripts() { return scripts; }

    public static KotlinCompiledScript getScript(String name) {
        return getInstance().scripts.get(name);
    }
    /**
     * Must be public for access from Kotlin.
     */
    public static void putScript(String name, KotlinCompiledScript script) {
        getInstance().scripts.put(name, script);
    }

    @Override
    public void initialize(Map<String,String> params) throws CachingException {
        // TODO: params indicate scripts to precompile
    }

    @Override
    public void loadCache() {
        refreshCache();
    }

    @Override
    public void refreshCache() {
        clearCache();
        getInstance();
    }

    @Override
    public void clearCache() {
        instance = null;
    }

    private synchronized void load() {
        long start = System.currentTimeMillis();

        scriptEngine = (KotlinScriptEngine) new KotlinScriptEngineFactory().getScriptEngine();
        if (logger.isDebugEnabled()) {
            long finish = System.currentTimeMillis();
            logger.debug("KotlinAccess.getScriptEngine(): " + (finish - start) + " ms");
            start = finish;
        }

        scripts = new HashMap<>();

        try {
            // compile .kt assets (not scripts)
            List<File> sources = getSources();
            File classesDir = new File(ApplicationContext.getTempDirectory() + "/" + KotlinClasspathKt.CLASSES_PATH);
            // conditional compilation only applies in dev mode
            if (!sources.isEmpty() && needsCompile(sources, classesDir)) {
                List<String> args = new ArrayList<>();
                logger.info("Compiling Kotlin...");
                if (logger.isDebugEnabled())
                    logger.debug("Kotlin asset sources:");
                for (File source : sources) {
                    if (logger.isDebugEnabled())
                        logger.debug("  " + source);
                    args.add(source.toString());
                }
                args.add("-d");
                args.add(classesDir.getPath());
                args.add("-no-stdlib");
                String classpath = new KotlinClasspath().getAsString();
                if (logger.isDebugEnabled()) {
                    logger.debug("Kotlin asset compilation classpath:");
                    logger.debug("  " + classpath);
                }
                args.add("-classpath");
                args.add(classpath);
                ExitCode exitCode = new K2JVMCompiler().exec(System.out, args.toArray(new String[0]));
                if (exitCode.getCode() != 0) {
                    CompilationException ex = new CompilationException("Kotlin compiler error: " + exitCode);
                    logger.severeException(ex.getMessage(), ex);
                }
                else if (logger.isDebugEnabled()) {
                    logger.debug("Kotlin compilation result: " + exitCode);
                }
            }
            else {
                logger.info("Skipping Kotlin compilation as " + classesDir.getPath() + " is up-to-date");
            }

            instance = this;
        }
        catch (Exception ex) {
            throw new StartupException("Kotlin initialization failure: " + ex.getMessage(), ex);
        }
        finally {
            if (logger.isDebugEnabled()) {
                logger.debug("Compile Kotlin source assets: " + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }

    private List<File> getSources() throws IOException {
        Path assetRoot = Paths.get(ApplicationContext.getAssetRoot().getPath().replace('\\', '/'));
        PathMatcher ktMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.kt");
        List<File> files = new ArrayList<>();
        Files.walkFileTree(assetRoot,
            EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    File file = path.toFile();
                    MdwIgnore mdwIgnore = new MdwIgnore(file);
                    if (ktMatcher.matches(path) && !new File(file.getParentFile() + "/buildKt.gradle").exists()) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        );
        return files;
    }


    private File newestClassFile;

    /**
     * Currently does an unsophisticated brute check of last modified source and asset jars versus
     * the last compiled class file.
     * TODO: https://github.com/CenturyLinkCloud/mdw/issues/392
     */
    protected synchronized boolean needsCompile(List<File> sources, File outputDir) throws IOException {
        if (!outputDir.isDirectory())
            return true;

        newestClassFile = null;
        Files.walkFileTree(Paths.get(outputDir.getPath()),
            EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (path.toString().endsWith(".class")) {
                        File classFile = path.toFile();
                        if (newestClassFile == null || classFile.lastModified() > newestClassFile.lastModified())
                            newestClassFile = classFile;
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        );
        if (newestClassFile == null)
            return true;  // nothing compiled

        File newestSourceFile = null;
        for (File source : sources) {
            if (newestSourceFile == null || source.lastModified() > newestSourceFile.lastModified())
                newestSourceFile = source;
        }
        for (Asset jar : AssetCache.getJarAssets()) {
            if (newestSourceFile == null || jar.file().lastModified() > newestSourceFile.lastModified())
                newestSourceFile = jar.file();
        }

        boolean needsCompile = newestSourceFile.lastModified() >= newestClassFile.lastModified();
        if (logger.isDebugEnabled()) {
            if (needsCompile)
                logger.debug("Kt needs compile due to " + newestSourceFile + " >= " + newestClassFile);
            else
                logger.debug("Kt no compile due to " + newestSourceFile + " < " + newestClassFile);
        }
        return needsCompile;
    }
}
