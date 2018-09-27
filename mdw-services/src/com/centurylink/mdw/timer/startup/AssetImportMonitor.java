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
package com.centurylink.mdw.timer.startup;

import java.io.File;
import java.sql.ResultSet;
import java.util.Date;

import com.centurylink.mdw.common.service.SystemMessages;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.model.system.SystemMessage.Level;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.cli.Import;

/**
 * Checks for asset imports performed on other instances
 */
public class AssetImportMonitor implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static boolean _terminating;
    private static AssetImportMonitor monitor = null;
    private static Thread thread = null;

    /**
     * Invoked when the server starts up.
     */
    public void onStartup() throws StartupException {
        if (monitor == null) {
            monitor = this;
            thread = new Thread() {
                @Override
                public void run() {
                    this.setName("AssetImportMonitor-thread");
                    monitor.start();
                }
            };
            thread.start();
        }
    }

    public void onShutdown() {
        _terminating = true;
        thread.interrupt();
    }

    public void start() {
        Bulletin bulletin = null;
        try {
            Long interval = PropertyManager.getLongProperty(PropertyNames.MDW_ASSET_SYNC_INTERVAL, 60000); //Defaults to checking every 60 seconds
            boolean gitHardReset = PropertyManager.getBooleanProperty(PropertyNames.MDW_ASSET_SYNC_GITRESET, false);
            File assetDir = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION) == null ? null : new File(PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION));
            String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
            File gitRoot = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH) == null ? null : new File(PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH));
            AssetServices assetServices = ServiceLocator.getAssetServices();
            VersionControlGit vcs = (VersionControlGit)assetServices.getVersionControl();
            if (vcs == null || vcs.getCommit() == null || assetDir == null || gitRoot == null || branch == null) {
                _terminating = true;
                String message = "Failed to start Asset Import Monitor due to: ";
                message += assetDir == null ? "Missing mdw.asset.location property" : "";
                message += gitRoot == null ? message.length() > 0 ? ", missing mdw.git.local.path property" : "Missing mdw.git.local.path property" : "";
                message += branch == null ? message.length() > 0 ? ", missing mdw.git.branch property" : "Missing mdw.git.branch property" : "";
                message += (vcs == null || vcs.getCommit() == null) ? message.length() > 0 ? ", missing Git repository" : "missing Git repository" : "";
                logger.warn(message);
            }
            else {
                _terminating = false;
            }

            while (!_terminating) {
                try {
                    Thread.sleep(interval);

                    // Check if it needs to trigger an asset import to sync up this instance's assets
                    // Exclude if head commit in local repo is newer than commit from VALUE DB table (means new local commit - asset saved)
                    try (DbAccess dbAccess = new DbAccess()) {
                        String select = "select value, mod_dt from value where name='CommitID' and owner_type='AssetImport' and owner_id='0'";
                        String latestCommit = null;
                        Date lastImportTime = null;
                        try (ResultSet rs = dbAccess.runSelect(select)) {
                            if (rs.next()) {
                                latestCommit = rs.getString("value");
                                lastImportTime = rs.getDate("mod_dt");
                            }
                        }
                        // Proceed if latest commit from VALUE table doesn't match current local Git commit (Potential import done in other instance)
                        if (latestCommit != null && !vcs.getCommit().equals(latestCommit)) {
                            Date currentCommitTime = new Date(vcs.getCommitTime(vcs.getCommit()));
                            // Check if import was done after the current local commit was created - Otherwise, means new local commit - asset saved
                            if (currentCommitTime.getTime() < lastImportTime.getTime()) {
                                logger.info("Detected Asset Import in cluster.  Performing Asset Import...");
                                bulletin = SystemMessages.bulletinOn("Asset import in progress...");
                                Import importer = new Import(gitRoot, vcs, branch, gitHardReset, dbAccess.getConnection());
                                importer.setAssetLoc(vcs.getRelativePath(assetDir));
                                importer.importMDWGit();
                                SystemMessages.bulletinOff(bulletin, "Asset import completed");
                                bulletin = null;
                                CacheRegistration.getInstance().refreshCaches(null);
                            }
                        }
                    }

                }
                catch (InterruptedException e) {
                    if (!_terminating) throw e;
                    logger.info(this.getClass().getName() + " stopping.");
                }
            }
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            SystemMessages.bulletinOff(bulletin, Level.Error, "Asset import failed: " + ((e.getMessage() == null && e.getCause() != null) ? e.getCause().getMessage() : e.getMessage()));
        }
        finally {
            if (!_terminating) this.start();  // Restart if a failure occurred, besides instance is shutting down
        }
    }
}
