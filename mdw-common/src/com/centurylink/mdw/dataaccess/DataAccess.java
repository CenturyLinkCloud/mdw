/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.SchemaTypeTranslator;
import com.centurylink.mdw.common.constant.ApplicationConstants;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.spring.SpringAppContext;
import com.centurylink.mdw.common.utilities.DesignatedHostSslVerifier;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.file.GitDiffs;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.dataaccess.file.RuntimeDataAccessVcs;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.dataaccess.file.WrappedBaselineData;
import com.centurylink.mdw.dataaccess.version4.ProcessImporterExporterV4;
import com.centurylink.mdw.dataaccess.version4.ProcessLoaderPersisterV4;
import com.centurylink.mdw.dataaccess.version4.RuntimeDataAccessV4;
import com.centurylink.mdw.dataaccess.version4.UserDataAccessV4;
import com.centurylink.mdw.dataaccess.version5.ProcessImporterExporterV5;
import com.centurylink.mdw.dataaccess.version5.ProcessLoaderPersisterV5;
import com.centurylink.mdw.dataaccess.version5.RuntimeDataAccessV5;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

public class DataAccess {

    public final static int schemaVersion3 = 3000;
    public final static int schemaVersion4 = 4001;
    public final static int schemaVersion5 = 5000;
    public final static int schemaVersion51 = 5001;
    public final static int schemaVersion52 = 5002;
    public final static int schemaVersion55 = 5005;
    public final static int currentSchemaVersion = schemaVersion55;
    public static int supportedSchemaVersion = currentSchemaVersion;
    public static boolean isPackageLevelAuthorization = false;

    public static RuntimeDataAccess getRuntimeDataAccess(int version, int supportedVersion, DatabaseAccess db) {
        if (version<=schemaVersion4) return new RuntimeDataAccessV4(db, version, supportedVersion);
        return new RuntimeDataAccessV5(db, version, supportedVersion);
    }

    public static RuntimeDataAccess getRuntimeDataAccess(int version, int supportedVersion, DatabaseAccess db, List<VariableTypeVO> variableTypes) {
        if (version<=schemaVersion4) return new RuntimeDataAccessV4(db, version, supportedVersion);
        return new RuntimeDataAccessV5(db, version, supportedVersion, variableTypes);
    }

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
        if (version <= schemaVersion4)
            return new ProcessLoaderPersisterV4(db, version, supportedVersion, stt);
        else
            return new ProcessLoaderPersisterV5(db, version, supportedVersion, stt);
    }

    public static ProcessPersister getDbProcessPersister() throws DataAccessException {
        return getDbProcessPersister(null);
    }

    public static ProcessPersister getDbProcessPersister(String source) throws DataAccessException {
        return new ProcessLoaderPersisterV5(new DatabaseAccess(source), currentSchemaVersion, supportedSchemaVersion, null);
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

        if (version <= schemaVersion4)
            return new ProcessLoaderPersisterV4(db, version, supportedVersion, null);
        return new ProcessLoaderPersisterV5(db, version, supportedVersion, null);
    }

    public static ProcessLoader getProcessLoader(DatabaseAccess db) throws DataAccessException {
        return getProcessLoader(currentSchemaVersion, supportedSchemaVersion, db);
    }

    public static ProcessLoader getProcessLoader() throws DataAccessException {
        return getProcessLoader(currentSchemaVersion, supportedSchemaVersion, new DatabaseAccess(null));
    }

    public static ProcessLoader getDbProcessLoader() throws DataAccessException {
        return getDbProcessLoader(null);
    }

    public static ProcessLoader getDbProcessLoader(String source) throws DataAccessException {
        return new ProcessLoaderPersisterV5(new DatabaseAccess(source), currentSchemaVersion, supportedSchemaVersion, null);
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

    	return getRuntimeDataAccess(currentSchemaVersion, supportedSchemaVersion, db);
    }

    public static ProcessImporter getProcessImporter(int version) {
        if (version<=schemaVersion4) return new ProcessImporterExporterV4();
        return new ProcessImporterExporterV5();
    }

    public static ProcessExporter getProcessExporter(int version) {
        if (version<=schemaVersion4) return new ProcessImporterExporterV4();
        return new ProcessImporterExporterV5();
    }

    public static ProcessExporter getProcessExporter(int version, SchemaTypeTranslator stt) {
        if (version<=schemaVersion4) return new ProcessImporterExporterV4(stt);
        return new ProcessImporterExporterV5(stt);
    }

    public static UserDataAccess getUserDataAccess(DatabaseAccess db) throws DataAccessException {
        return getUserDataAccess(currentSchemaVersion, supportedSchemaVersion, db);
    }

    public static UserDataAccess getUserDataAccess(int version, int supportedVersion, DatabaseAccess db) {
    	return new UserDataAccessV4(db, version, supportedVersion);
    }

    private static volatile ProcessLoader loaderPersisterVcs;
    private static final Object loaderPersisterLock = new Object();
    protected static ProcessLoader getVcsProcessLoader(File rootDir, DatabaseAccess db) throws DataAccessException {
        ProcessLoader myLoaderPersisterVcs = loaderPersisterVcs;
        if (myLoaderPersisterVcs == null) {  // needs to be always the same version
            synchronized(loaderPersisterLock) {
                myLoaderPersisterVcs = loaderPersisterVcs;
                if (myLoaderPersisterVcs == null) {
                    String compatDs = isUseCompatibilityDatasource(db) ? ApplicationConstants.MDW_FRAMEWORK_DATA_SOURCE_NAME : null;
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
                    loaderPersisterVcs = myLoaderPersisterVcs = new LoaderPersisterVcs("mdw", rootDir, vc, getBaselineData(), compatDs);
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
     * For VCS assets.  Not to be called from Designer.
     */
    public static boolean isUseCompatibilityDatasource() throws DataAccessException {
        assert ApplicationContext.isFileBasedAssetPersist();
        return isUseCompatibilityDatasource(null);
    }

    /**
     * For VCS assets.  Only check the first time.
     */
    private static final Object useCompatibilityDatasourceLock = new Object();
    private static volatile Boolean useCompatDataSource = null;
    public static boolean isUseCompatibilityDatasource(DatabaseAccess db) throws DataAccessException {
        Boolean useCompatDataSourceTemp = useCompatDataSource;
        if (useCompatDataSourceTemp == null) {
            synchronized(useCompatibilityDatasourceLock) {
                useCompatDataSourceTemp = useCompatDataSource;
                if (useCompatDataSourceTemp == null) {
                    if (db == null)
                        db = new DatabaseAccess(null);
                    if (db.isMySQL() || db.isMariaDB())
                        useCompatDataSource = useCompatDataSourceTemp = false;
                    else {
                        try {
                            db.openConnection();
                            String query = "select table_name from all_tables where table_name = 'RULE_SET'";
                            useCompatDataSource = useCompatDataSourceTemp = db.runSelect(query, null).next();
                        } catch (SQLException e) {
                            throw new DataAccessException(e.getMessage(), e);
                        } finally {
                            db.closeConnection();
                        }
                    }
                }
            }
        }
        return useCompatDataSourceTemp;
    }

    public static int[] getDatabaseSchemaVersion(DatabaseAccess db) throws DataAccessException {
        int version;
        int supportedVersion=-1;
        try {
            db.openConnection();
            isPackageLevelAuthorization = false;
            String query = "select ATTRIBUTE_VALUE from ATTRIBUTE where ATTRIBUTE_OWNER='SYSTEM' and ATTRIBUTE_NAME='"
            	+ PropertyNames.MDW_DB_VERSION + "'";
            ResultSet rs = db.runSelect(query, null);
            if (rs.next()) {
            	version = Integer.parseInt(rs.getString(1));
            	query = "select ATTRIBUTE_VALUE from ATTRIBUTE where ATTRIBUTE_OWNER='SYSTEM' and ATTRIBUTE_NAME='"
                	+ PropertyNames.MDW_DB_VERSION_SUPPORTED + "'";
            	rs = db.runSelect(query, null);
            	if (rs.next()) supportedVersion = Integer.parseInt(rs.getString(1));
            } else {
            	query = "select table_name from all_tables " +
            		"where table_name in  ('DOCUMENT','PACKAGE_RULESETS')";
                rs.close();
            	rs = db.runSelect(query, null);
            	version = schemaVersion3;
            	while (rs.next()) {
            		String table = rs.getString(1);
            		if (table.equalsIgnoreCase("PACKAGE_RULESETS")) version = schemaVersion5;
            		else if (version<schemaVersion4 && table.equalsIgnoreCase("DOCUMENT"))
            			version = schemaVersion4;
            	}
            	query = "select table_name from all_tables where table_name = 'RESOURCE_TYPE'";
            	rs = db.runSelect(query, null);
            	if (rs.next()) {
            	    query = "select variable_type_id from variable_type where variable_type_name = 'com.centurylink.mdw.model.StringDocument'";
            	    rs = db.runSelect(query, null);
            	    if (rs.next()) {
            	        version = schemaVersion55;
            	        query = "select column_name from all_tab_columns where table_name='PACKAGE' AND column_name='GROUP_NAME'";
            	        rs = db.runSelect(query, null);
                        if (rs.next())
                            isPackageLevelAuthorization = true;
                        else
                            isPackageLevelAuthorization = false;
            	    }
            	    else
            	        version = schemaVersion52;
            	}
            }
            if (supportedVersion<0) {
            	if (version>=schemaVersion52) supportedVersion = version;
            	else supportedVersion = schemaVersion4;
            }
            if (supportedVersion >= schemaVersion55 && db.isMySQL())
                isPackageLevelAuthorization = true;
            return new int[]{version,supportedVersion};
        } catch (SQLException e) {
            throw new DataAccessException(123, "Cannot connect to database", e);
        } finally {
            db.closeConnection();
        }
    }

}
