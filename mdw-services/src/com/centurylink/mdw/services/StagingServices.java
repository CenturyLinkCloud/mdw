package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.file.GitBranch;
import com.centurylink.mdw.dataaccess.file.GitProgressMonitor;

public interface StagingServices {

    String STAGE = "stage_";

    /**
     * Return Git branch if it exists on remote.  Otherwise null.
     */
    GitBranch getStagingBranch(String cuid) throws ServiceException;

    /**
     * Prepare user staging branch.  Clones if necessary, creates branch if necessary; pulls if local repo present.
     */
    GitBranch prepareStagingBranch(String cuid, GitProgressMonitor progressMonitor) throws ServiceException;

}
