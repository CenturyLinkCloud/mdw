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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.java.CompilationException;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@RegisteredService(CacheService.class)
public class KotlinAccess implements CacheService {

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
    public static KotlinCompiledScript getScript(String name) {
        return getInstance().scripts.get(name);
    }
    /**
     * Must be public for access from Kotlin.
     */
    public static void putScript(String name, KotlinCompiledScript script) {
        getInstance().scripts.put(name, script);
    }

    /**
     * Loads lazily in development to speed startup time.
     */
    @Override
    public void refreshCache() throws Exception {
        clearCache();
        if (!ApplicationContext.isDevelopment())
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

        // TODO: optional script precompilation
        scripts = new HashMap<>();

        try {
            // compile .kt assets (not scripts)
            List<File> sources = getSources();
            if (!sources.isEmpty()) {
                List<String> args = new ArrayList<>();
                if (logger.isDebugEnabled())
                    logger.debug("Kotlin asset sources:");
                for (File source : sources) {
                    if (logger.isDebugEnabled())
                        logger.debug("  " + source);
                    args.add(source.toString());
                }
                args.add("-d");
                args.add(ApplicationContext.getTempDirectory() + "/" + KotlinClasspathKt.CLASSES_PATH);
                args.add("-no-stdlib");
                String classpath = new KotlinClasspath().getAsString();
                if (logger.isDebugEnabled()) {
                    logger.debug("Kotlin asset compilation classpath:");
                    logger.debug("  " + classpath);
                }
                args.add("-classpath");
                args.add(classpath);
                ExitCode exitCode = new K2JVMCompiler().exec(logger.getPrintStream(), args.toArray(new String[0]));
                if (exitCode.getCode() != 0) {
                    CompilationException ex = new CompilationException("Kotlin compiler error: " + exitCode);
                    logger.severeException(ex.getMessage(), ex);
                }
                else if (logger.isDebugEnabled()) {
                    logger.debug("Kotlin compilation result: " + exitCode);
                }
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
        Path omitPath = Paths.get(new File(assetRoot + "/" + KotlinClasspathKt.KOTLIN_PACKAGE.replace('.', '/')).getPath());

        PathMatcher ktMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.kt");
        List<File> files = new ArrayList<>();
        Files.walkFileTree(assetRoot,
            EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (ktMatcher.matches(path) && !path.startsWith(omitPath)) {
                        files.add(path.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        );
        return files;
    }

}
