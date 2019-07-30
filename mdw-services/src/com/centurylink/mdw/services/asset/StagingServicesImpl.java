package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.file.GitBranch;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.StagingServices;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class StagingServicesImpl implements StagingServices {

    private static final String STAGE = "stage_";

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

    private VersionControlGit stagingVersionControl;
    /**
     * Cloned specifically for user staging.
     */
    private VersionControlGit getStagingVersionControl() throws ServiceException {
        if (stagingVersionControl == null) {
            stagingVersionControl = new VersionControlGit();
            String url = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
            String user = PropertyManager.getProperty(PropertyNames.MDW_GIT_USER);
            String password = PropertyManager.getProperty(PropertyNames.MDW_GIT_PASSWORD);
            if (url == null || user == null || password == null)
                throw new ServiceException(ServiceException.INTERNAL_ERROR, "Missing mdw.git configuration");
            String tempDir = PropertyManager.getProperty(PropertyNames.MDW_TEMP_DIR);
            try {
                stagingVersionControl.connect(url, user, password, new File(tempDir + "/git/staging"));
            }
            catch (IOException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
            }
        }
        return stagingVersionControl;
    }

    public GitBranch getStagingBranch(String cuid) throws ServiceException {
        try {
            VersionControlGit vcGit = getMainVersionControl();
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

    public GitBranch createStagingBranch(String cuid) throws ServiceException {
        if (getStagingBranch(cuid) != null)
            throw new ServiceException(ServiceException.CONFLICT, "Staging branch exists: " + STAGE + cuid);
        VersionControlGit stagingVc = getStagingVersionControl();
        try {
            if (!stagingVc.localRepoExists()) {
                stagingVc.cloneBranch(STAGE + cuid);
            }
        }
        catch (Exception ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex);
        }

        return null;
    }
}
