/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.io.File;
import java.io.IOException;

import com.centurylink.mdw.common.SchemaTypeTranslator;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.spring.SpringAppContext;
import com.centurylink.mdw.common.utilities.DesignatedHostSslVerifier;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.db.UserDataAccessDb;
import com.centurylink.mdw.dataaccess.file.GitDiffs;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.dataaccess.file.RuntimeDataAccessVcs;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.dataaccess.file.WrappedBaselineData;

public class DataAccess {

    public final static int schemaVersion55 = 5005;
    public final static int currentSchemaVersion = schemaVersion55;
    public static int supportedSchemaVersion = currentSchemaVersion;
    public static boolean isPackageLevelAuthorization = true;

    public static ProcessPersister getProcessPersister() throws DataAccessException {
        return getProcessPersister(currentSchemaVersion, supportedSchemaVersion, new DatabaseAccess(null),null);
    }

    /**
     * Not to be called from Designer due to property lookup.
     */
    public static ProcessPersister getProcessPersister(int version, int supportedVersion,
            DatabaseAccess db, SchemaTypeTranslator stt) throws DataAccessException {
        String fileBasedAssetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
        return getProcessPersister(version, supportedVersion, db, stt,fileBasedAssetLoc);
    }

    public static ProcessPersister getProcessPersister(int version, int supportedVersion,
            DatabaseAccess db, SchemaTypeTranslator stt, String fileBasedAssetLoc)
            throws DataAccessException {
        if (fileBasedAssetLoc != null)
            return (ProcessPersister) getVcsProcessLoader(new File(fileBasedAssetLoc), db);
        else
            throw new UnsupportedOperationException("Only VCS assets are supported");
    }

    /**
     * Not to be called from Designer due to property lookup.
     */
    public static ProcessLoader getProcessLoader(int version, int supportedVersion, DatabaseAccess db) throws DataAccessException {
        String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
        return getProcessLoader(version, supportedVersion, db, assetLoc);
    }

    public static ProcessLoader getProcessLoader(int version, int supportedVersion, DatabaseAccess db,
            String fileBasedAssetLoc) throws DataAccessException {
        if (fileBasedAssetLoc != null)
            return getVcsProcessLoader(new File(fileBasedAssetLoc), db);
        else
            throw new UnsupportedOperationException("Only VCS assets are supported");
    }

    public static ProcessLoader getProcessLoader(DatabaseAccess db) throws DataAccessException {
        return getProcessLoader(currentSchemaVersion, supportedSchemaVersion, db);
    }

    public static ProcessLoader getProcessLoader() throws DataAccessException {
        return getProcessLoader(currentSchemaVersion, supportedSchemaVersion, new DatabaseAccess(null));
    }

    /**
     * Not to be called from Designer due to property lookup.
     */
    public static RuntimeDataAccess getRuntimeDataAccess(DatabaseAccess db) throws DataAccessException {
        String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
        return getRuntimeDataAccess(db, assetLoc);
    }

    public static RuntimeDataAccess getRuntimeDataAccess(DatabaseAccess db, String fileBasedAssetLoc) throws DataAccessException {
        if (fileBasedAssetLoc != null)
            return getVcsRuntimeDataAccess(db, new File(fileBasedAssetLoc));
        else
            throw new UnsupportedOperationException("Only VCS assets are supported");
    }

    public static UserDataAccess getUserDataAccess(DatabaseAccess db) throws DataAccessException {
        return getUserDataAccess(currentSchemaVersion, supportedSchemaVersion, db);
    }

    public static UserDataAccess getUserDataAccess(int version, int supportedVersion, DatabaseAccess db) {
    	return new UserDataAccessDb(db, version, supportedVersion);
    }

    private static volatile ProcessLoader loaderPersisterVcs;
    private static final Object loaderPersisterLock = new Object();
    protected static ProcessLoader getVcsProcessLoader(File rootDir, DatabaseAccess db) throws DataAccessException {
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

                                if (logger.isDebugEnabled()) {
                                    // log actual diffs at debug level
                                    GitDiffs diffs = vcGit.getDiffs(branch, assetPath);
                                    if (!diffs.isEmpty()) {
                                        logger.severe("**** WARNING: Local Git repository is out-of-sync with respect to remote branch: " + branch
                                                + "\n(" + gitLocal.getAbsolutePath() + ")");
                                        logger.debug("Differences:\n============\n" + diffs);
                                    }
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
        return new int[] {schemaVersion55, schemaVersion55};
    }

}
