package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.file.GitProgressMonitor;
import com.centurylink.mdw.model.asset.Stage;

import java.util.List;

public interface StagingServices {

    String STAGE = "stage_";

    /**
     * All user stages that exist on the server.
     */
    List<Stage> getStages() throws ServiceException;

    /**
     * Return user stage if Git branch exists on remote.  Otherwise null.
     */
    Stage getUserStage(String cuid) throws ServiceException;

    /**
     * Prepare user staging branch.  Clones if necessary, creates branch if necessary; pulls if local repo present.
     */
    Stage prepareUserStage(String cuid, GitProgressMonitor progressMonitor) throws ServiceException;

}
