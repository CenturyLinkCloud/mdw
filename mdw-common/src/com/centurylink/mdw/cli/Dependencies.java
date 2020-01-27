/*
 * Copyright (C) 2020 CenturyLink, Inc.
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
package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameters;
import com.centurylink.mdw.model.PackageDependency;
import com.centurylink.mdw.model.PackageMeta;
import com.centurylink.mdw.model.system.BadVersionException;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.util.file.Packages;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Parameters(commandNames = "dependencies", commandDescription = "Check asset package dependencies", separators="=")
public class Dependencies extends Setup {

    private List<PackageDependency> unmetDependencies = new ArrayList<>();
    public List<PackageDependency> getUnmetDependencies() { return unmetDependencies; }

    @Override
    public Operation run(ProgressMonitor... progressMonitors) throws IOException {

        Packages packages = getPackageDirs();
        List<String> packageNames = packages.getPackageNames();

        for (File pkgDir : packages.getPackageDirs()) {
            boolean pkgPrinted = false;
            File metaFile = getPackageMeta(pkgDir);
            PackageMeta pkgMeta = new PackageMeta(Files.readAllBytes(metaFile.toPath()));
            if (isDebug()) {
                logPackage(pkgMeta.getName());
                pkgPrinted = true;
            }
            List<String> depStrings = pkgMeta.getDependencies();
            if (depStrings != null) {
                for (String depString : depStrings) {
                    try {
                        PackageDependency dependency = new PackageDependency(depString);
                        boolean depPrinted = false;
                        if (isDebug()) {
                            getOut().println("  " + dependency);
                            depPrinted = true;
                        }
                        if (!packageNames.contains(dependency.getPackage())) {
                            if (!pkgPrinted) {
                                logPackage(pkgMeta.getName());
                                pkgPrinted = true;
                            }
                            unmet(dependency.getPackage(), dependency.getVersion(), "  ** Missing package: " + dependency.getPackage() + " **");
                            continue;
                        }
                        PackageMeta installedPkgMeta = new PackageMeta(Files.readAllBytes(getPackageMeta(dependency.getPackage()).toPath()));
                        MdwVersion installedVer = new MdwVersion(installedPkgMeta.getVersion());
                        if (installedVer.compareTo(dependency.getVersion()) < 0) {
                            if (!pkgPrinted) {
                                logPackage(pkgMeta.getName());
                                pkgPrinted = true;
                            }
                            if (!depPrinted) {
                                getOut().println("  " + dependency.getPackage());
                            }
                            unmet(dependency.getPackage(), dependency.getVersion(), "  ** Unmatched version: " + (installedVer + " < " + dependency.getVersion()) + " **");
                        }
                    }
                    catch (BadVersionException ex) {
                        getOut().println("Bad dependency: " + ex.getMessage());
                    }
                }
            }
        }

        if (!unmetDependencies.isEmpty()) {
            StringBuilder unmet = new StringBuilder();
            for (PackageDependency pkgDep : unmetDependencies) {
                if (unmet.length() > 0)
                    unmet.append(", ");
                unmet.append(pkgDep.toString());
            }
            throw new IOException("Unmet package dependencies: " + unmet);
        }
        else {
            getOut().println("Package dependency check completed without errors");
        }

        return this;
    }

    private void logPackage(String packageName) {
        getOut().println("Dependencies for package: " + packageName);
    }

    private void unmet(String packageName, MdwVersion version, String message) {
        getOut().println(message);  // getErr() comes out-of-sequence in Studio
        PackageDependency alreadyUnmet = getUnmet(packageName);
        if (alreadyUnmet == null || alreadyUnmet.getVersion().compareTo(version) < 0) {
            unmetDependencies.add(new PackageDependency(packageName, version));
        }
    }

    private PackageDependency getUnmet(String packageName) {
        for (PackageDependency pkgDep : getUnmetDependencies()) {
            if (pkgDep.getPackage().equals(packageName))
                return pkgDep;
        }
        return null;
    }
}
