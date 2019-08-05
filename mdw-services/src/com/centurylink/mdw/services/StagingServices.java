package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.file.GitProgressMonitor;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.Stage;

import java.io.File;
import java.util.List;
import java.util.SortedMap;

public interface StagingServices {

    String STAGE = "stage_";
    String STAGED_ASSET = "Staged";

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

    File getStagingDir();

    File getStagingDir(String cuid);

    SortedMap<String,List<AssetInfo>> getStagedAssets(String cuid) throws ServiceException;
    void stageAssets(String cuid, List<String> assets) throws ServiceException;
    void unStageAssets(String cuid, List<String> assets) throws ServiceException;

}
