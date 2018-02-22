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
package com.centurylink.mdw.cache.impl;

import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cli.Checkpoint;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;

public class AssetRefCache implements CacheService {

    private static final Object lock = new Object();
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile Map<Long,AssetRef> assetRefs;
    private static volatile boolean isEnabled = false;
    private static volatile Long offset = null;

    public void refreshCache() throws CachingException {
        synchronized (lock) {
            clearCache();
        }
    }

    @Override
    public void clearCache() {
        assetRefs = null;

    }

    public static AssetRef getAssetRef(String name) throws CachingException {
        if (assetRefs == null)
            load();

        AssetRef ref = null;
        for (AssetRef curRef : assetRefs.values()) {
            if (curRef.getName().equals(name))
                ref = curRef;
        }

        if (ref == null && isEnabled && offset != 0L) {
            ref = loadAssetRef(name);
            if (ref != null)
                assetRefs.put(ref.getDefinitionId(), ref);
        }
        return ref;
    }

    public static AssetRef getAssetRef(Long definitionId) throws CachingException {
        if (assetRefs == null)
            load();

        AssetRef ref = assetRefs.get(definitionId);

        if (ref == null && isEnabled && offset != 0L) {
            ref = loadAssetRef(definitionId);
            if (ref != null)
                assetRefs.put(definitionId, ref);
        }
        return ref;
    }

    public static AssetRef getAssetRef(AssetVersionSpec spec) throws CachingException {
        if (assetRefs == null)
            load();

        String name = spec.getPackageName() != null ? spec.getPackageName() : "";
        name += "/" + spec.getName() + " v";
        for (AssetRef curRef : assetRefs.values()) {
            if (curRef.getName().contains(name)) {
                Asset asset = new Asset();
                asset.setVersion(Asset.parseVersion(curRef.getName().substring((curRef.getName().lastIndexOf(" v")+1))));
                if (asset.meetsVersionSpec(spec.getVersion()))
                    return curRef;
            }
        }
        // If we haven't found it, pull all rows from ASSET_REF table (unless we already did)
        if (offset != 0L) {
            offset = 0L; // This will trigger loading the entire ASSET_REF table
            assetRefs = null;
            return getAssetRef(spec);
        }

        return null;
    }

    private static AssetRef loadAssetRef(String name) throws CachingException {
        // try to load from database in case Ref is older than default of 2 years
        AssetRef assetRef = null;
        try {
            LoaderPersisterVcs loader = (LoaderPersisterVcs)DataAccess.getProcessLoader();
            VersionControlGit vc = (VersionControlGit)loader.getVersionControl();
            DatabaseAccess db = new DatabaseAccess(null);
            try (DatabaseAccess dbAccess = db.open()){
                Checkpoint cp = new Checkpoint(loader.getStorageDir(), loader.getVersionControl(), vc.getCommit(), dbAccess.getConnection());
                assetRef = cp.retrieveRef(name);
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }

        if (assetRef != null && logger.isInfoEnabled())
            logger.info("AssetRef: '" + name + "' loaded from database.");

        return assetRef;
    }

    private static AssetRef loadAssetRef(Long definitionId) throws CachingException {
        // try to load from database in case Ref is older than default of 2 years
        AssetRef assetRef = null;
        try {
            LoaderPersisterVcs loader = (LoaderPersisterVcs)DataAccess.getProcessLoader();
            VersionControlGit vc = (VersionControlGit)loader.getVersionControl();
            DatabaseAccess db = new DatabaseAccess(null);
            try (DatabaseAccess dbAccess = db.open()){
                Checkpoint cp = new Checkpoint(loader.getStorageDir(), loader.getVersionControl(), vc.getCommit(), dbAccess.getConnection());
                assetRef = cp.retrieveRef(definitionId);
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }

        if (assetRef != null && logger.isInfoEnabled())
            logger.info("AssetRef: '" + assetRef.getName() + "' loaded from database.");

        return assetRef;
    }

    private static synchronized void load() {
     // load all Refs from database that were archived in last X days (default is 2 years)
        Map<Long,AssetRef> myAssetRefs = assetRefs;
        if (myAssetRefs == null) {
            myAssetRefs = assetRefs = new ConcurrentHashMap<Long,AssetRef>();

            try {
                if (DataAccess.getProcessLoader() instanceof LoaderPersisterVcs &&
                        ((LoaderPersisterVcs)DataAccess.getProcessLoader()).getVersionControl() instanceof VersionControlGit) {
                    isEnabled = true;
                    List<AssetRef> list = null;
                    LoaderPersisterVcs loader = (LoaderPersisterVcs)DataAccess.getProcessLoader();
                    VersionControlGit vc = (VersionControlGit)loader.getVersionControl();
                    DatabaseAccess db = new DatabaseAccess(null);
                    Date date = null;
                    if (offset == null)
                        offset = PropertyManager.getIntegerProperty("mdw.assetref.offset", 730) * 24 * 3600 * 1000L;
                    if (offset != 0L)  // If offset == 0, then load entire table
                        date = new Date(DatabaseAccess.getCurrentTime() - offset);
                    try (DatabaseAccess dbAccess = db.open()){
                        Checkpoint cp = new Checkpoint(loader.getStorageDir(), loader.getVersionControl(), vc.getCommit(), dbAccess.getConnection());
                        list = cp.retrieveAllRefs(date);
                    }
                    if (list != null) {
                        for (AssetRef ref : list) {
                            assetRefs.put(ref.getDefinitionId(), ref);
                        }
                    }
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
    }
}
