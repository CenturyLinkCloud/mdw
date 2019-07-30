package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.file.GitBranch;
import com.centurylink.mdw.dataaccess.file.GitProgressMonitor;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.StagingServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class StagingServicesImpl implements StagingServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     * Returns the main version control (ie: asset location).
     */
    private VersionControlGit getMainVersionControl() throws ServiceException {
        try {
            AssetServices assetServices = ServiceLocator.getAssetServices();
            return (VersionControlGit) assetServices.getVersionControl();
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    private File getStagingDir(String cuid) {
        String tempDir = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
        return new File(tempDir + "/git/staging/" + STAGE + cuid);
    }

    /**
     * Cloned specifically for user staging.
     */
    private VersionControlGit getStagingVersionControl(String cuid) throws ServiceException {
            VersionControlGit stagingVersionControl = new VersionControlGit();
            String gitUrl = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
            String gitUser = PropertyManager.getProperty(PropertyNames.MDW_GIT_USER);
            String gitPassword = PropertyManager.getProperty(PropertyNames.MDW_GIT_PASSWORD);
            if (gitUrl == null || gitUser == null || gitPassword == null)
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Missing mdw.git configuration");
            try {
                stagingVersionControl.connect(gitUrl, gitUser, gitPassword, getStagingDir(cuid));
                return stagingVersionControl;
            }
            catch (IOException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
            }
    }

    public GitBranch getStagingBranch(String cuid) throws ServiceException {
        return getStagingBranch(cuid, getMainVersionControl());
    }

    private GitBranch getStagingBranch(String cuid, VersionControlGit vcGit) throws ServiceException {
        try {
            List<GitBranch> branches = vcGit.getRemoteBranches();
            Optional<GitBranch> opt = branches.stream().filter(b -> b.getName().equals(STAGE + cuid)).findFirst();
            return opt.orElse(null);
        }
        catch (ServiceException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }
    }

    public GitBranch prepareStagingBranch(String cuid, GitProgressMonitor progressMonitor) throws ServiceException {
        String stagingBranchName = STAGE + cuid;
        VersionControlGit stagingVc = getStagingVersionControl(cuid);
        try {
            if (stagingVc.localRepoExists()) {
                GitBranch stagingBranch = getStagingBranch(cuid, stagingVc);
                new Thread(() -> {
                    try {
                        stagingVc.pull(stagingVc.getBranch(), progressMonitor);
                        if (stagingBranch == null)
                            stagingVc.createBranch(stagingBranchName, stagingVc.getBranch(),progressMonitor);
                        if (!stagingVc.getBranch().equals(stagingBranchName))
                            stagingVc.checkout(stagingBranchName);
                        if (progressMonitor != null)
                            progressMonitor.done();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        if (progressMonitor != null)
                            progressMonitor.error(ex);
                    }
                }).start();
            } else {
                String mainBranch = getMainVersionControl().getBranch();
                new Thread(() -> {
                    try {
                        stagingVc.clone(mainBranch, progressMonitor);
                        GitBranch stagingBranch = getStagingBranch(cuid, stagingVc);
                        if (stagingBranch == null) {
                            stagingVc.createBranch(stagingBranchName, mainBranch, progressMonitor);
                            stagingVc.checkout(stagingBranchName);
                            if (progressMonitor != null)
                                progressMonitor.done();
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        if (progressMonitor != null)
                            progressMonitor.error(ex);
                    }
                }).start();
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }

        return null;
    }
}
