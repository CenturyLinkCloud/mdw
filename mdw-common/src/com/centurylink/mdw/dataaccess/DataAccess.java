/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.io.File;
import java.io.IOException;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.db.UserDataAccessDb;
import com.centurylink.mdw.dataaccess.file.GitDiffs;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.dataaccess.file.RuntimeDataAccessVcs;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.dataaccess.file.WrappedBaselineData;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.DesignatedHostSslVerifier;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.ProgressMonitor;
import com.centurylink.mdw.util.timer.SystemOutProgressMonitor;

public class DataAccess {

    public final static int schemaVersion60 = 6000;
    public final static int currentSchemaVersion = schemaVersion60;
    public static int supportedSchemaVersion = currentSchemaVersion;
    public static boolean isPackageLevelAuthorization = true;

    public static ProcessPersister getProcessPersister() throws DataAccessException {
        return getProcessPersister(currentSchemaVersion, supportedSchemaVersion, new DatabaseAccess(null));
    }

    public static ProcessPersister getProcessPersister(int version, int supportedVersion, DatabaseAccess db)
    throws DataAccessException {
        File assetRoot = ApplicationContext.getAssetRoot();
        if (assetRoot == null)
            throw new IllegalStateException("Asset root not known");
        return (ProcessPersister) getVcsProcessLoader(assetRoot);
    }

    public static ProcessLoader getProcessLoader(int version, int supportedVersion, DatabaseAccess db)
    throws DataAccessException {
        File assetRoot = ApplicationContext.getAssetRoot();
        if (assetRoot == null)
            throw new IllegalStateException("Asset root not known");
        return getVcsProcessLoader(assetRoot);
    }

    public static ProcessLoader getProcessLoader(DatabaseAccess db) throws DataAccessException {
        return getProcessLoader(currentSchemaVersion, supportedSchemaVersion, db);
    }

    public static ProcessLoader getProcessLoader() throws DataAccessException {
        return getProcessLoader(currentSchemaVersion, supportedSchemaVersion, new DatabaseAccess(null));
    }

    public static RuntimeDataAccess getRuntimeDataAccess(DatabaseAccess db) throws DataAccessException {
        File assetRoot = ApplicationContext.getAssetRoot();
        if (assetRoot == null)
            throw new IllegalStateException("Asset root not known");
        return getVcsRuntimeDataAccess(db, assetRoot);
    }

    public static UserDataAccess getUserDataAccess(DatabaseAccess db) throws DataAccessException {
        return getUserDataAccess(currentSchemaVersion, supportedSchemaVersion, db);
    }

    public static UserDataAccess getUserDataAccess(int version, int supportedVersion, DatabaseAccess db) {
        return new UserDataAccessDb(db, version, supportedVersion);
    }

    private static volatile ProcessLoader loaderPersisterVcs;
    private static final Object loaderPersisterLock = new Object();
    protected static ProcessLoader getVcsProcessLoader(File rootDir) throws DataAccessException {
        ProcessLoader myLoaderPersisterVcs = loaderPersisterVcs;
        if (myLoaderPersisterVcs == null) {  // needs to be always the same version
            synchronized(loaderPersisterLock) {
                myLoaderPersisterVcs = loaderPersisterVcs;
                if (myLoaderPersisterVcs == null) {
                    if (!rootDir.exists() || rootDir.list().length == 0) { // initial environment startup scenario
                        String message;
                        if (rootDir.isDirectory())
                            message = "Asset location " + rootDir + " is empty.";
                        else
                            message = "Asset location " + rootDir + " does not exist.";
                        String warning = "\n****************************************\n" +
                                "** WARNING: " + message + "\n" +
                                "** Please import the MDW base and hub packages\n" +
                                "******************************************\n";
                        LoggerUtil.getStandardLogger().severe(warning);
                    }
                    VersionControl vc = getAssetVersionControl(rootDir);
                    loaderPersisterVcs = myLoaderPersisterVcs = new LoaderPersisterVcs("mdw", rootDir, vc, getBaselineData());
                }
            }
        }
        return myLoaderPersisterVcs;
    }


    protected static RuntimeDataAccess getVcsRuntimeDataAccess(DatabaseAccess db, File rootDir) throws DataAccessException {
        return new RuntimeDataAccessVcs(db, currentSchemaVersion, supportedSchemaVersion, getBaselineData());
    }

    private static VersionControl assetVersionControl;
    public synchronized static VersionControl getAssetVersionControl(File rootDir) throws DataAccessException {
        if (assetVersionControl == null) {
            assetVersionControl = new VersionControlGit();
            String gitLocalPath = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
            if (gitLocalPath == null) // use the asset dir as placeholder
                gitLocalPath = rootDir.getAbsolutePath();
            try {
                assetVersionControl.connect(null, "mdw", null, new File(gitLocalPath));
                // check up-to-date
                StandardLogger logger = LoggerUtil.getStandardLogger();
                try {
                    String url = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
                    String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
                    if (url != null && branch != null) {
                        String user = PropertyManager.getProperty(PropertyNames.MDW_GIT_USER);
                        String password = PropertyManager.getProperty(PropertyNames.MDW_GIT_PASSWORD);
                        if (user != null && password != null) {
                            VersionControlGit vcGit = new VersionControlGit();
                            File gitLocal = new File(gitLocalPath);
                            vcGit.connect(url, user, password, gitLocal);

                            String gitTrustedHost = PropertyManager.getProperty(PropertyNames.MDW_GIT_TRUSTED_HOST);
                            if (gitTrustedHost != null)
                                DesignatedHostSslVerifier.setupSslVerification(gitTrustedHost);

                            if (!gitLocal.isDirectory()) {
                                if (!gitLocal.mkdirs())
                                    throw new DataAccessException("Git loc " + gitLocalPath + " does not exist and cannot be created.");
                            }
                            if (!vcGit.localRepoExists()) {
                                logger.severe("**** WARNING: Git location " + gitLocalPath + " does not contain a repository.  Cloning with no checkout...");
                                vcGit.cloneNoCheckout();
                            }

                            String assetPath = vcGit.getRelativePath(rootDir);
                            logger.info("Loading assets from path: " + assetPath);

                            // sanity checks
                            String gitBranch = vcGit.getBranch();
                            if (!branch.equals(gitBranch)) {
                                String warning = "\n****************************************\n" +
                                        "** WARNING: Git branch: " + gitBranch + " does not match " + PropertyNames.MDW_GIT_BRANCH + ": " + branch + "\n" +
                                        "** Please perform an Import to sync with branch " + branch + "\n" +
                                        "******************************************\n";
                                LoggerUtil.getStandardLogger().severe(warning);
                            }
                            else {
                                String localCommit = vcGit.getCommit();
                                if (localCommit != null) {
                                    String remoteCommit = vcGit.getRemoteCommit(branch);
                                    if (!localCommit.equals(remoteCommit))
                                        LoggerUtil.getStandardLogger().severe("**** WARNING: Git commit: " + localCommit + " does not match remote HEAD commit: " + remoteCommit);
                                }

                                // log actual diffs at debug level
                                GitDiffs diffs = vcGit.getDiffs(branch, assetPath);
                                if (!diffs.isEmpty()) {
                                    logger.warn("**** WARNING: Local Git repository is out-of-sync with respect to remote branch: " + branch
                                            + "\n(" + gitLocal.getAbsolutePath() + ")");
                                    logger.info("Differences:\n============\n" + diffs);
                                }

                                String strGitAutoPull = PropertyManager.getProperty(PropertyNames.MDW_GIT_AUTO_PULL);
                                boolean gitAutoPull = strGitAutoPull == null ? false : Boolean.parseBoolean(strGitAutoPull);
                                if (gitAutoPull) {
                                    // force checkout all assets
                                    File tempDir = new File(PropertyNames.MDW_TEMP_DIR);
                                    ProgressMonitor progressMonitor = new SystemOutProgressMonitor();
                                    VcsArchiver archiver = new VcsArchiver(ApplicationContext.getAssetRoot(), tempDir, vcGit, progressMonitor);
                                    logger.severe("**** Performing Git Auto-Pull (Overwrites existing assets): " + vcGit + " (branch: " + branch + ")");
                                    archiver.backup();
                                    vcGit.sparseCheckout(branch, assetPath);
                                    archiver.archive();
                                }
                            }
                        }
                        else {
                            logger.severe("**** WARNING: Not verifying local Git repository due to missing credentials.");
                        }
                    }
                }
                catch (Exception ex) {
                    logger.severeException("Error during Git up-to-date check.", ex);
                }

                // allow initial startup with no asset root
                if (!rootDir.exists()) {
                    if (!rootDir.mkdirs())
                        throw new DataAccessException("Asset root: " + rootDir + " does not exist and cannot be created.");
                }
            }
            catch (IOException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }
        return assetVersionControl;
    }

    public static BaselineData getBaselineData() throws DataAccessException {
        try {
            return new WrappedBaselineData(new MdwBaselineData()) {
                protected BaselineData getOverrideBaselineData() {
                    return SpringAppContext.getInstance().getBaselineData();
                }
            };
        }
        catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * TODO differentiate version 6
     */
    public static int[] getDatabaseSchemaVersion(DatabaseAccess db) throws DataAccessException {
        return new int[] {schemaVersion60, schemaVersion60};
    }

}
