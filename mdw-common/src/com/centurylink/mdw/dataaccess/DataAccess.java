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
package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.git.GitDiffs;
import com.centurylink.mdw.git.VersionControlGit;
import com.centurylink.mdw.util.DesignatedHostSslVerifier;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DataAccess {

    public final static int currentSchemaVersion = 6001;
    public static int supportedSchemaVersion = currentSchemaVersion;

    static {
        File assetRoot = ApplicationContext.getAssetRoot();
        if (!assetRoot.exists() || assetRoot.list().length == 0) {
            // initial environment startup scenario
            String message;
            if (assetRoot.isDirectory())
                message = "Asset location " + assetRoot + " is empty.";
            else
                message = "Asset location " + assetRoot + " does not exist.";
            String warning = "\n****************************************\n" +
                    "** WARNING: " + message + "\n" +
                    "** Please import the MDW base and hub packages\n" +
                    "******************************************\n";
            LoggerUtil.getStandardLogger().error(warning);
        }
        try {
            getAssetVersionControl(assetRoot);
        } catch (IOException ex) {
            LoggerUtil.getStandardLogger().error(ex.getMessage(), ex);
        }
    }

    private static VersionControlGit assetVersionControl;
    public synchronized static VersionControlGit getAssetVersionControl(File rootDir) throws IOException {
        if (assetVersionControl == null) {
            boolean fetch = PropertyManager.getBooleanProperty(PropertyNames.MDW_GIT_FETCH, !ApplicationContext.isDevelopment());
            assetVersionControl = new VersionControlGit(fetch);
            String gitLocalPath = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
            if (gitLocalPath == null) // use the asset dir as placeholder
                gitLocalPath = rootDir.getAbsolutePath();

            assetVersionControl.connect(null, "mdw", null, new File(gitLocalPath));
            // check up-to-date
            StandardLogger logger = LoggerUtil.getStandardLogger();
            try {
                String url = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
                String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
                String tag = branch == null ? PropertyManager.getProperty(PropertyNames.MDW_GIT_TAG) : null;
                if (url != null && (branch != null || tag != null)) {
                    String user = PropertyManager.getProperty(PropertyNames.MDW_GIT_USER);
                    String password = PropertyManager.getProperty(PropertyNames.MDW_GIT_PASSWORD);
                    if (user != null) {
                        VersionControlGit vcGit = (VersionControlGit) assetVersionControl;
                        File gitLocal = new File(gitLocalPath);
                        vcGit.connect(url, user, password, gitLocal);

                        String gitTrustedHost = PropertyManager.getProperty(PropertyNames.MDW_GIT_TRUSTED_HOST);
                        if (gitTrustedHost != null)
                            DesignatedHostSslVerifier.setupSslVerification(gitTrustedHost);

                        String assetPath = vcGit.getRelativePath(rootDir.toPath());
                        logger.info("Loading assets from path: " + assetPath);

                        if (!gitLocal.isDirectory()) {
                            if (!gitLocal.mkdirs())
                                throw new DataAccessException("Git loc " + gitLocalPath + " does not exist and cannot be created.");
                        }
                        if (!vcGit.localRepoExists()) {
                            logger.error("**** WARNING: Git location " + gitLocalPath + " does not contain a repository.  Cloning: " + url);
                            vcGit.cloneNoCheckout();
                            if (PropertyManager.getBooleanProperty(PropertyNames.MDW_GIT_AUTO_CHECKOUT, true)) {
                                if (branch != null) {
                                    logger.info("Performing branch checkout...");
                                    vcGit.hardCheckout(branch);
                                }
                                else {  // tag != null
                                    logger.info("Performing tag checkout...");
                                    vcGit.hardTagCheckout(tag);
                                }
                            }
                            else
                                logger.warn("Git Auto Checkout is set to false!  No assets will be pulled from Git...");
                        }

                        // sanity checks
                        if (branch != null) {
                            String gitBranch = vcGit.getBranch();
                            if (!branch.equals(gitBranch)) {
                                String warning = "\n****************************************\n" +
                                        "** WARNING: Git branch: " + gitBranch + " does not match " + PropertyNames.MDW_GIT_BRANCH + ": " + branch + "\n" +
                                        "** Please perform an Import to sync with branch " + branch + "\n" +
                                        "******************************************\n";
                                LoggerUtil.getStandardLogger().error(warning);
                            } else {
                                String localCommit = vcGit.getCommit();
                                if (localCommit != null) {
                                    String remoteCommit = vcGit.getRemoteCommit(branch);
                                    if (!localCommit.equals(remoteCommit))
                                        LoggerUtil.getStandardLogger().error("**** WARNING: Git commit: " + localCommit + " does not match remote HEAD commit: " + remoteCommit);
                                }

                                // log actual diffs at debug level
                                GitDiffs diffs = vcGit.getDiffs(branch, assetPath);
                                if (!diffs.isEmpty()) {
                                    logger.warn("**** WARNING: Local Git repository is out-of-sync with respect to branch: " + branch
                                            + "\n(" + gitLocal.getAbsolutePath() + ")");
                                    logger.info("Differences:\n============\n" + diffs);
                                }
                            }
                        }
                        else {   // Tag != null
                            String localCommit = vcGit.getCommit();
                            String tagCommit = vcGit.getCommitForTag(tag);
                            if (localCommit != null && tagCommit != null && !localCommit.equals(tagCommit)) {
                                logger.info("Current commit " + localCommit + " does not match commit for tag " + tag + ". Performing tag checkout...");
                                vcGit.hardTagCheckout(tag);
                            }
                            else if (localCommit == null)
                                logger.warn("Could not find local commit!");
                            else if (tagCommit == null) {
                                logger.warn("Git Tag named " + tag + " was NOT found!");
                            }
                        }
                    }
                    else {
                        logger.error("**** WARNING: Not verifying Git asset sync due to missing property " + PropertyNames.MDW_GIT_USER + " (use anonymous for public repos)");
                    }
                }
            }
            catch (Exception ex) {
                logger.error("Error during Git up-to-date check.", ex);
            }

            // allow initial startup with no asset root
            if (!rootDir.exists()) {
                if (!rootDir.mkdirs())
                    throw new FileNotFoundException("Asset root: " + rootDir + " does not exist and cannot be created.");
            }
        }
        return assetVersionControl;
    }
}
