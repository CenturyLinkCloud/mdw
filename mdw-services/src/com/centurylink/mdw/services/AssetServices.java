/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.value.asset.Asset;
import com.centurylink.mdw.model.value.asset.PackageAssets;
import com.centurylink.mdw.model.value.asset.PackageList;

/**
 * Services for interacting with design-time workflow assets.
 */
public interface AssetServices {

    public File getAssetRoot();

    public VersionControl getVersionControl() throws IOException;
    public void clearVersionControl();

    /**
     * Retrieves the contents of a workflow asset.
     * @param asset - Qualified name (including workflow package: 'MyPackage/MyText.txt').
     *                File extension is required, and must designate a format supported by MDW.
     * @return byte array with the contents
     */
    public byte[] getAssetContent(String asset) throws ServiceException;


    /**
     * Saves a new version of a workflow asset.
     * @param asset - Qualified name (including workflow package: 'MyPackage/MyText.txt').
     *                File extension is required, and must designate a format supported by MDW.
     * @param content - Contents of the asset.
     * @param user - The workstation ID of the user making the change.
     * @param comment - Explanation for asset history.
     */
    public void saveAsset(String asset, Object content, String user, String comment) throws ServiceException;

    /**
     * Returns the list of workflow packages.  Does not include archived packages.
     * Includes Git information if available.
     */
    public PackageList getPackages() throws ServiceException;


    /**
     * Return the PackageDir for a name.  Does not contain assets or VCS info.
     */
    public PackageDir getPackage(String name) throws ServiceException;

    /**
     * Returns the assets of a workflow package latest version.
     * Includes Git information if available.
     */
    public PackageAssets getAssets(String packageName) throws ServiceException;

    /**
     * Returns all assets for a given file extension (mapped to their package name).
     * Assets do not contain VCS info.
     */
    public Map<String,List<Asset>> getAssetsOfType(String format) throws ServiceException;

    /**
     * Includes Git info if available.
     * @param assetPath - myPackage/myAsset.ext
     */
    public Asset getAsset(String assetPath) throws ServiceException;

}
