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
package com.centurylink.mdw.cache.asset;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cli.Dependencies;
import com.centurylink.mdw.file.PackageFinder;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.PackageMeta;
import com.centurylink.mdw.file.Packages;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PackageCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile List<Package> packageList;

    public void initialize(Map<String,String> params) {}

    @Override
    public void loadCache() throws CachingException {
        List<Package> packageListTemp = packageList;
        if (packageListTemp == null)
            synchronized(PackageCache.class) {
                packageListTemp = packageList;
                if (packageListTemp == null)
                    packageList = load();
            }
    }

    @Override
    public void clearCache() {}

    @Override
    public synchronized void refreshCache() throws CachingException {
        synchronized(PackageCache.class) {
            packageList = load();
        }
    }

    public static List<Package> getPackages() throws CachingException {
        List<Package> packageListTemp = packageList;
        if (packageListTemp == null)
            synchronized(PackageCache.class) {
                packageListTemp = packageList;
                if (packageListTemp == null)
                    packageList = packageListTemp = load();
            }
        return packageListTemp;
    }

    public static Package getPackage(String packageName) throws CachingException {
        for (Package pkg : getPackages()) {
            if (pkg.getName().equals(packageName)) {
                return pkg;
            }
        }
        return null;
    }

    public static Package getMdwBasePackage() throws CachingException {
        return getPackage(Packages.MDW_BASE);
    }

    /**
     * Validates package dependencies using the CLI command.
     */
    private static void validatePackageDependencies() {
        Dependencies dependencies = new Dependencies();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        dependencies.setAssetLoc(ApplicationContext.getAssetRoot().toString());
        dependencies.setOut(ps);
        dependencies.setErr(ps);
        try {
            dependencies.run();
        }
        catch (Exception ex) {
            logger.error("Package dependency check error(s)", ex);
        }
        finally {
            String output = baos.toString();
            if (!output.isEmpty())
                logger.info(output);
        }
    }

    /**
     * Checks if the framework asset packages and current MDW build versions are the same.
     * Otherwise logs a warning message.
     */
    private static void validateMdwPackageVersions(List<Package> packages) {
        final String exceptions = ".*\\b(demo|hub|central)\\b.*";
        String mdwVersion = ApplicationContext.getMdwVersion();

        List<Package> filteredPackages = packages.stream()
                .filter(e -> !mdwVersion.equals(e.getVersion().toString()) && e.getName().startsWith("com.centurylink.mdw"))
                .collect(Collectors.toList());
        List<Package> mismatches = filteredPackages.stream()
                .filter(p2 -> !(p2.getName().matches(exceptions)))
                .collect(Collectors.toList());

        if (!mismatches.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("\n****************************************\n"
                    + "** WARNING: These asset packages do not match MDW runtime version " + mdwVersion + "\n");
            for (Package mismatch : mismatches) {
                message.append("**   " + mismatch.getLabel() + "\n");
            }
            message.append("******************************************\n");
            logger.warn(message.toString());
        }
    }

    private static synchronized List<Package> load() throws CachingException {
        List<Package> packages = new ArrayList<>();
        try {
            Path rootPath = ApplicationContext.getAssetRoot().toPath();
            Map<Path,PackageMeta> packageMetas = new PackageFinder(rootPath).findPackages();
            for (Path pkgPath : packageMetas.keySet()) {
                PackageMeta pkgMeta = packageMetas.get(pkgPath);
                packages.add(new Package(pkgMeta, pkgPath.toFile()));
            }
        }
        catch (IOException ex) {
            throw new CachingException(ex.getMessage(), ex);
        }

        if (!packages.isEmpty()) {
            Collections.sort(packages);
            validateMdwPackageVersions(packages);
            validatePackageDependencies();
        }

        return packages;
    }
}
