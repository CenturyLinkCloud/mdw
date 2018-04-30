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

import com.centurylink.mdw.cli.Checkpoint;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import com.centurylink.mdw.util.timer.SystemOutProgressMonitor;

/**
 * Checks for asset imports performed on other instances
 */
public class AssetImportMonitor implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Long interval;
    private static boolean _terminating;
    private static AssetImportMonitor monitor;
    private static Thread thread;

    /**
     * Invoked when the server starts up.
     */
    public void onStartup() throws StartupException {
        interval = PropertyManager.getLongProperty(PropertyNames.MDW_ASSET_SYNC_INTERVAL, 60000); //Defaults to checking every 60 seconds
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



    public void onShutdown() {
        _terminating = true;
        thread.interrupt();
    }

    public void start() {
        try {
            VcsArchiver archiver = null;
            File assetDir = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION) == null ? null : new File(PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION));
            File tempDir = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR) == null ? null: new File(PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR));
            String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
            AssetServices assetServices = ServiceLocator.getAssetServices();
            VersionControlGit vcs = (VersionControlGit)assetServices.getVersionControl();
            if (vcs == null || vcs.getCommit() == null || assetDir == null || tempDir == null || branch == null) {
                _terminating = true;
                String message = "Failed to start Asset Import Monitor due to: ";
                message += assetDir == null ? "Missing mdw.asset.location property" : "";
                message += tempDir == null ? message.length() > 0 ? ", missing mdw.temp.dir property" : "Missing mdw.temp.dir property" : "";
                message += branch == null ? message.length() > 0 ? ", missing mdw.git.branch property" : "Missing mdw.git.branch property" : "";
                message += (vcs == null || vcs.getCommit() == null) ? message.length() > 0 ? ", missing Git repository" : "missing Git repository" : "";
                logger.warn(message);
            }
            else {
                _terminating = false;
                archiver = new VcsArchiver(assetDir, tempDir, vcs, new SystemOutProgressMonitor());
            }

            while (!_terminating) {
                try {
                    // Check if it needs to trigger an asset import to sync up this instance's assets
                    try (DbAccess dbAccess = new DbAccess()) {
                        Checkpoint cp = new Checkpoint(assetDir, vcs, vcs.getCommit(),
                                dbAccess.getConnection());
                        if (!vcs.getCommit().equals(cp.getLatestRefCommit())) {
                            if (VcsArchiver.setInProgress()) {
                            logger.info("Detected Asset Import in cluster.  Performing Asset Import...");
                            archiver.backup();
                    //        vcs.hardCheckout(branch);
                            archiver.archive(true);
                            CacheRegistration.getInstance().refreshCaches(null);
                            }
                        }
                    }
                    Thread.sleep(interval);
                }
                catch (InterruptedException e) {
                    if (!_terminating) throw e;
                    logger.info(this.getClass().getName() + " stopping.");
                }
            }
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
        }
        finally {
            if (!_terminating) this.start();  // Restart if a failure occurred, besides instance is shutting down
        }
    }
}
