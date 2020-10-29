package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameters;
import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.workflow.PackageDependency;
import com.centurylink.mdw.model.workflow.PackageMeta;
import com.centurylink.mdw.model.system.BadVersionException;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.file.Packages;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Parameters(commandNames = "dependencies", commandDescription = "Check asset package dependencies", separators="=")
public class Dependencies extends Setup {

    private final List<PackageDependency> unmetDependencies = new ArrayList<>();
    public List<PackageDependency> getUnmetDependencies() { return unmetDependencies; }

    @Override
    public Operation run(ProgressMonitor... progressMonitors) throws IOException {

        Packages packages = getPackageDirs();
        List<String> packageNames = packages.getPackageNames();

        for (File pkgDir : packages.getPackageDirs()) {
            boolean pkgPrinted = false;
            File metaFile = getPackageMeta(pkgDir);
            String meta = new String(Files.readAllBytes(metaFile.toPath()));
            PackageMeta pkgMeta = new PackageMeta(Yamlable.fromString(meta));
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
                        String metaContent = new String(Files.readAllBytes(getPackageMeta(dependency.getPackage()).toPath()));
                        PackageMeta installedPkgMeta = new PackageMeta(Yamlable.fromString(metaContent));
                        MdwVersion installedVer = installedPkgMeta.getVersion();
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
