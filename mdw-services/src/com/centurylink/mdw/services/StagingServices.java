package com.centurylink.mdw.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.file.GitProgressMonitor;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.PackageList;
import com.centurylink.mdw.model.asset.StagingArea;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public interface StagingServices {

    String STAGING = "staging_";
    String STAGED_ASSET = "Staged";

    /**
     * All user stagingAreas that exist on the server.
     */
    List<StagingArea> getStagingAreas() throws ServiceException;

    /**
     * Return user staging area if Git branch exists on remote.  Otherwise null.
     */
    StagingArea getUserStagingArea(String cuid) throws ServiceException;

    /**
     * Prepare user staging branch.  Clones if necessary, creates branch if necessary; pulls if local repo present.
     */
    StagingArea prepareStagingArea(String cuid, GitProgressMonitor progressMonitor) throws ServiceException;

    File getStagingDir();
    File getStagingDir(String cuid);
    File getStagingAssetsDir(String cuid) throws ServiceException;
    VersionControlGit getStagingVersionControl(String cuid) throws ServiceException;
    String getVcAssetPath() throws ServiceException;

    PackageList getPackages(String cuid, boolean withVcsInfo) throws ServiceException;
    void createPackage(String cuid, String packageName) throws ServiceException;
    void createAsset(String cuid, String assetPath, String template) throws ServiceException;

    AssetInfo getStagedAsset(String cuid, String assetPath) throws ServiceException;
    SortedMap<String, List<AssetInfo>> getStagedAssets(String cuid) throws ServiceException;

    void stageAssets(String cuid, List<String> assets) throws ServiceException;
    void unStageAssets(String cuid, List<String> assets) throws ServiceException;
    void deleteAsset(String cuid, String assetPath) throws ServiceException;

    void promoteAssets(String cuid, String comment) throws ServiceException;
    void rollbackAssets(String cuid, Map<String,String> assetVersions) throws ServiceException;

    AssetServices getAssetServices(String cuid) throws ServiceException;
}