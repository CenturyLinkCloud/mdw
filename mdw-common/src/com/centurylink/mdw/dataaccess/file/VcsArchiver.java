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
package com.centurylink.mdw.dataaccess.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.ProgressMonitor;

public class VcsArchiver {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final String ARCHIVE = "Archive";

    private File assetDir;
    private File tempDir;
    private File tempArchiveDir;
    private VersionControl versionControl;

    private ProgressMonitor progressMonitor;
    public ProgressMonitor getProgressMonitor() { return progressMonitor; }

    private File archiveDir;
    private List<File> tempPkgDirs;
    private List<File> tempPkgArchiveDirs;

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

        // copy all packages from the asset dir to the temp dir
        LoaderPersisterVcs oldLoader = new LoaderPersisterVcs("mdw", assetDir, versionControl, new MdwBaselineData());
        List<PackageDir> oldPkgDirs = oldLoader.getPackageDirs(true);
        progressMonitor.subTask("Backing up existing package(s) in: " + assetDir);
        progressMonitor.progress(10);
        tempDir = new File(tempDir + "/AssetBackup_" + StringHelper.filenameDateToString(new Date()));
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs())
                throw new IOException("Unable to create temp directory: " + tempDir.getAbsolutePath());
        }
        tempArchiveDir = new File(tempDir + "/Archive");
        if (!tempArchiveDir.exists()) {
            if (!tempArchiveDir.mkdirs())
                throw new IOException("Unable to create temp directory: " + tempArchiveDir.getAbsolutePath());
        }
        progressMonitor.progress(10);
        progressMonitor.subTask("Copying packages to temps: " + tempDir.getAbsolutePath() + ", " + tempArchiveDir.getAbsolutePath());
        tempPkgDirs = new ArrayList<File>(oldPkgDirs.size());
        tempPkgArchiveDirs = new ArrayList<File>(oldPkgDirs.size());
        for (PackageDir oldPkgDir : oldPkgDirs) {
            if (path == null || path.equals(oldPkgDir.getPackageName())) {
                File myTempDir = oldPkgDir.isArchive() ? tempArchiveDir : tempDir;
                File tempPkgDir = new File(myTempDir + "/" + oldPkgDir.getPackageName() + " v" + oldPkgDir.getPackageVersion());
                progressMonitor.subTask("  -- " + tempPkgDir.getName());
                oldLoader.copyPkg(oldPkgDir, tempPkgDir);
                if (oldPkgDir.isArchive())
                    tempPkgArchiveDirs.add(tempPkgDir);
                else
                    tempPkgDirs.add(tempPkgDir);
            }
        }
        progressMonitor.progress(20);
    }

    /**
     * Move replaced package(s) to archive from temp (those not found in updated asset folder).
     * Uses 40% of progressMonitor.
     */
    public void archive() throws DataAccessException, IOException {
        archive(false);
    }

    /**
     * Move replaced package(s) to archive from temp (those not found in updated asset folder).
     * Uses 40% of progressMonitor.
     */
    public void archive(boolean deleteBackups) throws DataAccessException, IOException {
        LoaderPersisterVcs newLoader = new LoaderPersisterVcs("mdw", assetDir, versionControl, new MdwBaselineData());
        archiveDir = new File(assetDir + "/" + ARCHIVE);
        if (!archiveDir.exists()) {
            if (tempPkgArchiveDirs != null && tempPkgArchiveDirs.size() > 0) {  // The Archive directory got deleted - Restore it here
                progressMonitor.subTask("Restoring Archive assets: " + archiveDir.getAbsolutePath());
                for (File tempPkgDir : tempPkgArchiveDirs) {
                    File archiveDest = new File(archiveDir + "/" + tempPkgDir.getName());
                    if (archiveDest.exists())
                        newLoader.deletePkg(archiveDest);
                    newLoader.copyPkg(tempPkgDir, archiveDest);
                }
            }
            else if (!archiveDir.mkdirs())
                throw new IOException("Unable to create archive directory: " + archiveDir.getAbsolutePath());
        }
        List<PackageDir> newPkgDirs = newLoader.getPackageDirs(false);
        if (!"true".equals(PropertyManager.getProperty("mdw.suppress.asset.version.check"))) {
            AssetFile flaggedAsset = null;
            PackageDir newPkg = null;
            // Check for user errors - i.e. older version asset present in newer pkg
            progressMonitor.subTask("Checking for asset version inconsistencies");
            versionControl.clear();
            for (File prevPkg : tempPkgDirs) {
                String simplePkgName = prevPkg.getName().substring(0, prevPkg.getName().indexOf(" v"));
                newPkg = null;
                for (PackageDir item : newPkgDirs) {
                    if (simplePkgName.equals(item.getPackageName())) {
                        newPkg = item;
                        break;
                    }
                }
                if (newPkg != null) {
                    for (File prevFile : prevPkg.listFiles()) {
                        if (prevFile.isFile()) {
                            AssetFile newFile = newPkg.getAssetFile(new File(newPkg + "/" + prevFile.getName()));
                            if (newFile != null && newFile.getRevision().getVersion() > 0) {
                                AssetRevision prevRev = versionControl.getRevision(prevFile);
                                if (prevRev != null && prevRev.getVersion() > newFile.getRevision().getVersion()) {
                                    flaggedAsset = newFile;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (flaggedAsset != null)
                    break;
            }
            if (flaggedAsset != null) {  // Need to restore previous pkgs and error out on asset import
                progressMonitor.subTask("Found asset version inconsistencies....restoring from backup");
                for (File prevPkg : tempPkgDirs) {
                    File restoreDest = new File(assetDir + "/" + prevPkg.getName().substring(0, prevPkg.getName().indexOf(" v")).replace('.', '/'));
                    if (restoreDest.exists())
                        newLoader.deletePkg(restoreDest);
                    newLoader.copyPkg(prevPkg, restoreDest);
                }
                if (deleteBackups) {
                    progressMonitor.subTask("Removing temp backups: " + tempDir + ", " + tempArchiveDir);
                    try {
                        newLoader.delete(tempDir);
                    }
                    catch (Throwable ex) {
                        logger.severeException(ex.getMessage(), ex);
                    }
                }
                throw new IOException("Failed -- Import would overide asset with older version: " + flaggedAsset.getLogicalFile());
            }
            progressMonitor.progress(10);
        }
        progressMonitor.subTask("Adding packages to archive: " + archiveDir.getAbsolutePath());
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
        if (deleteBackups) {
            progressMonitor.subTask("Removing temp backups: " + tempDir + ", " + tempArchiveDir);
            try {
                newLoader.delete(tempDir);
            }
            catch (Throwable ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        progressMonitor.progress(10);
    }
}
