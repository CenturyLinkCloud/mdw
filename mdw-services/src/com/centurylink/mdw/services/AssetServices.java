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
package com.centurylink.mdw.services;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.AssetPackageList;
import com.centurylink.mdw.model.asset.PackageAssets;
import com.centurylink.mdw.model.asset.PackageList;

/**
 * Services for interacting with design-time workflow assets.
 */
public interface AssetServices {

    public File getAssetRoot();

    public VersionControl getVersionControl() throws IOException;

    /**
     * Returns the list of workflow packages.  Does not include archived packages.
     * Includes Git information if available.
     */
    public PackageList getPackages(boolean withVcsInfo) throws ServiceException;

    /**
     * Returns packages, with their assets filtered by query criteria.
     */
    public AssetPackageList getAssetPackageList(Query query) throws ServiceException;

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
    public Map<String,List<AssetInfo>> getAssetsOfType(String format) throws ServiceException;

    /**
     * Includes Git info if available.
     * @param assetPath - myPackage/myAsset.ext
     */
    public AssetInfo getAsset(String assetPath) throws ServiceException;

    public void createPackage(String packageName) throws ServiceException;
    public void createAsset(String assetPath) throws ServiceException;
    public void createAsset(String assetPath, byte[] content) throws ServiceException;
}
