/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.VersionControl;

public class VcsArchiver {

    private static final String ARCHIVE = "Archive";

    private File assetDir;
    private File tempDir;
    private VersionControl versionControl;

    private ProgressMonitor progressMonitor;
    public ProgressMonitor getProgressMonitor() { return progressMonitor; }

    private File archiveDir;
    private List<File> tempPkgDirs;

    public VcsArchiver(File assetDir, File tempDir, VersionControl versionControl, ProgressMonitor progressMonitor) {
        this.assetDir = assetDir;
        this.tempDir = tempDir;
        this.versionControl = versionControl;
        this.progressMonitor = progressMonitor;
    }

    public void backup() throws DataAccessException, IOException {
        backup(null);
    }
    /**
     * Save current package(s) to temp dir.  Uses 40% of progressMonitor.
     *
     * @param Asset: com.centurylink.mdw.demo.intro/readme.md (entire package will be archived).
     * Package: com.centurylink.mdw.demo.
     * All Packages: null.
     *
     */
    public void backup(String path) throws DataAccessException, IOException {

        // isolate the package name
        if (path != null) {
            int slash = path.lastIndexOf('/');
            if (slash > 0)
                path = path.substring(0, slash);
        }

        LoaderPersisterVcs oldLoader = new LoaderPersisterVcs("mdw", assetDir, versionControl, new MdwBaselineData());
        List<PackageDir> oldPkgDirs = oldLoader.getPackageDirs(false);
        progressMonitor.subTask("Backing up existing package(s) in: " + assetDir);
        progressMonitor.progress(10);
        tempDir = new File(tempDir + "/AssetBackup_" + StringHelper.filenameDateToString(new Date()));
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs())
                throw new IOException("Unable to create temp directory: " + tempDir.getAbsolutePath());
        }
        progressMonitor.progress(10);
        progressMonitor.subTask("Copying packages to temp: " + tempDir.getAbsolutePath());
        tempPkgDirs = new ArrayList<File>(oldPkgDirs.size());
        for (PackageDir oldPkgDir : oldPkgDirs) {
            if (path == null || path.equals(oldPkgDir.getPackage().getName())) {
                File tempPkgDir = new File(tempDir + "/" + oldPkgDir.getPackageName() + " v" + oldPkgDir.getPackageVersion());
                progressMonitor.subTask("  -- " + tempPkgDir.getName());
                oldLoader.copyPkg(oldPkgDir, tempPkgDir);
                tempPkgDirs.add(tempPkgDir);
            }
        }
        progressMonitor.progress(20);
    }

    /**
     * Move replaced package(s) to archive.  Uses 40% of progressMonitor.
     */
    public void archive() throws DataAccessException, IOException {
        archiveDir = new File(assetDir + "/" + ARCHIVE);
        if (!archiveDir.exists())
            if (!archiveDir.mkdirs())
                throw new IOException("Unable to create archive directory: " + archiveDir.getAbsolutePath());
        progressMonitor.progress(10);
        progressMonitor.subTask("Adding packages to archive: " + archiveDir.getAbsolutePath());
        LoaderPersisterVcs newLoader = new LoaderPersisterVcs("mdw", assetDir, versionControl, new MdwBaselineData());
        List<PackageDir> newPkgDirs = newLoader.getPackageDirs(false);
        for (File tempPkgDir : tempPkgDirs) {
            boolean found = false;
            for (PackageDir newPkgDir : newPkgDirs) {
                String newPkgLabel = newPkgDir.getPackageName() + " v" + newPkgDir.getPackageVersion();
                if (tempPkgDir.getName().equals(newPkgLabel)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                progressMonitor.subTask("  -- " + tempPkgDir.getName());
                File archiveDest = new File(archiveDir + "/" + tempPkgDir.getName());
                if (archiveDest.exists())
                    newLoader.deletePkg(archiveDest);
                newLoader.copyPkg(tempPkgDir, archiveDest);
            }
        }
        progressMonitor.progress(20);
        progressMonitor.subTask("Removing temp: " + tempDir);
        newLoader.delete(tempDir);
        progressMonitor.progress(10);
    }
}
