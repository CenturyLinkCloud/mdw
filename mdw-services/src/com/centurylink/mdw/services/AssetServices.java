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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.asset.ArchiveDir;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.AssetPackageList;
import com.centurylink.mdw.model.asset.PackageAssets;
import com.centurylink.mdw.model.asset.PackageList;
import com.centurylink.mdw.services.asset.Renderer;

/**
 * Services for interacting with design-time workflow assets.
 */
public interface AssetServices {

    File getAssetRoot();

    File getArchiveDir();
    void deleteArchive() throws ServiceException;

    VersionControl getVersionControl() throws IOException;

    /**
     * Returns the list of workflow packages.  Does not include archived packages.
     * Includes Git information if available.
     */
    PackageList getPackages(boolean withVcsInfo) throws ServiceException;

    /**
     * Returns packages, with their assets filtered by query criteria.
     */
    AssetPackageList getAssetPackageList(Query query) throws ServiceException;

    /**
     * Return the PackageDir for a name.  Does not contain assets or VCS info.
     */
    PackageDir getPackage(String name) throws ServiceException;

    /**
     * Returns the assets of a workflow package latest version.
     */
    PackageAssets getAssets(String packageName) throws ServiceException;
    PackageAssets getAssets(String packageName, boolean withVcsInfo) throws ServiceException;

    /**
     * Returns all assets for a given file extension (mapped to their package name).
     * Assets do not contain VCS info.
     */
    Map<String,List<AssetInfo>> getAssetsOfType(String format) throws ServiceException;
    /**
     * Returns all assets for a list of file extensions (mapped to their package name).
     * Assets do not contain VCS info.
     */
    Map<String,List<AssetInfo>> getAssetsOfTypes(String[] formats) throws ServiceException;
    /**
     * Returns all assets with the specified name (without VCS info).
     */
    Map<String,List<AssetInfo>> findAssets(Predicate<File> predicate) throws ServiceException;

    /**
     * @param assetPath - myPackage/myAsset.ext
     * @param withVcsInfo - include Git info if available
     * @throws ServiceException if asset not found
     */
    AssetInfo getAsset(String assetPath, boolean withVcsInfo) throws ServiceException;

    /**
     * Without VCS info.
     * @param assetPath - myPackage/myAsset.ext
     */
    AssetInfo getAsset(String assetPath) throws ServiceException;
    AssetInfo getImplAsset(String className) throws ServiceException;

    void createPackage(String packageName) throws ServiceException;
    void deletePackage(String packageName) throws ServiceException;

    void createAsset(String assetPath) throws ServiceException;
    void createAsset(String assetPath, byte[] content) throws ServiceException;
    void deleteAsset(String assetPath) throws ServiceException;

    List<ArchiveDir> getArchiveDirs() throws ServiceException;

    List<String> getExtraPackageNames() throws ServiceException;

    Renderer getRenderer(String assetPath, String renderTo) throws ServiceException;

    List<PackageDir> getPackageDirs() throws IOException;

    List<PackageDir> findPackageDirs(Predicate<File> predicate) throws IOException;
}
