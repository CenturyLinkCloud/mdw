/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cache.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class AssetCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<AssetKey,Asset> assetMap;
    private static List<Asset> allAssets;  // initially unpopulated
    private static List<Asset> latestAssets;

    public AssetCache() {

    }

    private List<String> preLoaded;

    public AssetCache(Map<String,String> params) {
        initialize(params);
    }

    public void initialize(Map<String,String> params) {
        if (params != null) {
            String preLoadString = params.get("PreInitialized");
            if (preLoadString != null && preLoadString.trim().length() > 0) {
                String[] preLoadedArr = preLoadString.split("\\\n");
                for (String preLoad : preLoadedArr) {
                    if (preLoad.trim().length() > 0) {
                        if (preLoaded == null)
                            preLoaded = new ArrayList<String>();
                        preLoaded.add(preLoad.trim());
                    }
                }
            }
        }
    }

    public int getCacheSize() {
        return getAssetMap().size();
    }

    public void clearCache() {
        allAssets = null;
        latestAssets = null;
        assetMap = null;
    }

    public static boolean isLoaded() {
        return assetMap != null || latestAssets != null || allAssets != null;
    }

    public void loadCache() throws CachingException {
        // load is performed lazily unless preloads are specified
        if (preLoaded != null && preLoaded.size() > 0) {
            try {
                for (String preLoad : preLoaded) {
                    if (preLoad.length() > 0) {
                        Class<?> preLoadExecCls = Class.forName(preLoad);
                        Method initMethod = preLoadExecCls.getMethod("initialize");
                        initMethod.invoke(null, (Object[])null);
                   }
                }
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
    }

    public synchronized void refreshCache() throws CachingException {
        clearCache();
        loadCache();
    }

    public static synchronized Map<AssetKey,Asset> getAssetMap() {
        if (assetMap == null) {
            assetMap = Collections.synchronizedMap(new TreeMap<AssetKey,Asset>());
        }
        return assetMap;
    }

    public static Asset getAsset(Long id) {
        return getAsset(new AssetKey(id));
    }

    public static Asset getAsset(String name) {
        return getAsset(new AssetKey(name));
    }

    public static Asset getAsset(String name, String language) {
        return getAsset(new AssetKey(name, language));
    }

    public static Asset getAsset(String name, String[] languages) {
        for (String language : languages) {
            Asset asset = getAsset(new AssetKey(name, language));
            if (asset != null)
                return asset;
        }
        return null;  // not found
    }

    public static Asset getAsset(String name, String language, int version) {
        return getAsset(new AssetKey(name, language, version));
    }

    /**
     * Either a specific version number can be specified, or a Smart Version can be specified which designates an allowable range.
     * @see AssetVO.meetsVersionSpec().
     */
    public static Asset getAsset(AssetVersionSpec spec) {
        Asset match = null, assetVO = null;
        try {
            // Get asset from package based on asset version spec
            if (spec.getPackageName() != null) {
                // get all the versions of packages based on package name
                List<Package> allPackgeVOs = PackageCache.getAllPackageVOs(spec.getPackageName());
                for (Package pkgVO : allPackgeVOs) {
                    for (Asset asset : pkgVO.getAssets()) {
                        if (spec.getName().equals(asset.getName())) {
                            if (asset.meetsVersionSpec(spec.getVersion()) && (match == null || asset.getVersion() > match.getVersion()))
                                match = asset;
                        }
                    }
                }
                if (match != null) {
                    if (!match.isLoaded())
                        assetVO = DataAccess.getProcessLoader().getAsset(match.getId());
                    else
                        assetVO = match;
                }
                return assetVO;
            }

            for (Asset asset : getAllAssets()) {
                if (spec.getName().equals(asset.getName())) {
                    if (asset.meetsVersionSpec(spec.getVersion()) && (match == null || asset.getVersion() > match.getVersion()))
                        match = asset;
                }
            }
            if (match != null && !match.isLoaded()) {
                assetVO = DataAccess.getProcessLoader().getAsset(match.getId());
            }
        } catch (Exception ex) {
            logger.severeException("Failed to load asset: "+spec.toString()+ " : "+ex.getMessage(), ex);
        }
        return assetVO;
    }

    /**
     * Get the asset based on version spec whose name and custom attributes match the parameters.
     */
    public static Asset getAsset(AssetVersionSpec spec, Map<String,String> attributeValues) {
        Asset match = null;
        try {
            for (Asset asset : getAllAssets()) {
                if (spec.getName().equals(asset.getName())) {
                    if (asset.meetsVersionSpec(spec.getVersion()) && (match == null || asset.getVersion() > match.getVersion())) {
                        boolean attrsMatch = true;
                        for (String attrName : attributeValues.keySet()) {
                            String attrValue = attributeValues.get(attrName);
                            String rsValue = asset.getAttribute(attrName);
                            if (rsValue == null || !rsValue.equals(attrValue)) {
                                attrsMatch = false;
                                break;
                            }
                        }
                        if (attrsMatch && (match == null || match.getVersion() < asset.getVersion())) {
                            if (!asset.isLoaded()) {
                                Asset loaded = getAsset(asset.getId());
                                asset.setStringContent(loaded.getStringContent());
                            }
                            match = asset;
                        }
                    }
                }
            }
        } catch (DataAccessException ex) {
            logger.severeException("Failed to load asset: "+spec.toString()+ " : "+ex.getMessage(), ex);
        }
        return match;
    }


    /**
     * Asset names like 'myPackage/myAsset.ext' designate package-specific versions.
     * (or myPackage.MyCustomJavaClass for dynamic Java or Script).
     */
    public static Asset getAsset(AssetKey key) {
        int delimIdx = getDelimIndex(key);
        if (delimIdx > 0) {
            // look in package rule sets
            String pkgName = key.getName().substring(0, delimIdx);
            String assetName = key.getName().substring(delimIdx + 1);
            try {
                Package pkgVO = PackageCache.getPackageVO(pkgName);
                if (pkgVO != null) {
                    for (Asset asset : pkgVO.getAssets()) {
                        if (asset.getName().equals(assetName))
                            return getAsset(new AssetKey(asset.getId()));
                        if (Asset.JAVA.equals(key.getLanguage()) && asset.getName().equals(assetName + ".java"))
                            return getAsset(new AssetKey(asset.getId()));
                        if (Asset.GROOVY.equals(key.getLanguage()) && asset.getName().equals(assetName + ".groovy"))
                            return getAsset(new AssetKey(asset.getId()));
                    }
                }
            }
            catch (CachingException ex) {
                logger.severeException(ex.getMessage(), ex);
            }
            // try to load (for archived compatibility case)
            Asset asset = loadAsset(key);
            if (asset != null) {
                getAssetMap();
                synchronized(assetMap) {
                    assetMap.put(new AssetKey(asset), asset);
                }
                return asset;
            }
            return null;  // not found
        }

        getAssetMap();
        synchronized(assetMap) {
            for (AssetKey mapKey : assetMap.keySet()) {
                if (key.equals(mapKey))
                    return assetMap.get(mapKey);
            }
        }

        Asset asset = null;
        asset = loadAsset(key);
        if (asset != null) {
            synchronized(assetMap) {
                assetMap.put(new AssetKey(asset), asset);
            }
        }
        return asset;
    }

    private static int getDelimIndex(AssetKey key) {
        int delimIdx = -1;
        String name = key.getName();
        if (name != null) {
            if (Asset.JAVA.equals(key.getLanguage())) {
                delimIdx = name.endsWith(".java") ? name.substring(0, name.length() - 6).lastIndexOf('.') : name.lastIndexOf('.');
            }
            else if (Asset.GROOVY.equals(key.getLanguage())) {
                delimIdx = name.endsWith(".groovy") ? name.substring(0, 8).lastIndexOf('.') : name.lastIndexOf('.');
            }
            else {
              delimIdx = key.getName().indexOf('/');
            }
        }
        return delimIdx;
    }

    public static List<Asset> getAssets(String language) {
        List<Asset> assets = new ArrayList<Asset>();
        try {
            for (Asset asset : getLatestAssets()) {
                if (asset.getLanguage() != null && asset.getLanguage().equals(language)) {
                    Asset fullAsset = getAsset(asset.getId());
                    assets.add(fullAsset);
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return assets;
    }

    public static synchronized List<Asset> getLatestAssets() throws DataAccessException, CachingException {
        if (latestAssets == null) {
            latestAssets = new ArrayList<Asset>();
            for (Asset asset : getAllAssets()) {
                Package assetPkg = PackageCache.getAssetPackage(asset.getId());
                // relies on getAllAssets() being ordered by version descending
                boolean already = false;
                for (Asset rs : latestAssets) {
                    Package rsPkg = PackageCache.getAssetPackage(rs.getId());
                    if (rs.getName().equals(asset.getName())) {
                        // potential match
                        boolean languageMatch = false;
                        if (rs.getLanguage() == null)
                          languageMatch = asset.getLanguage() == null;
                        else
                          languageMatch = rs.getLanguage().equals(asset.getLanguage());
                        boolean packageMatch = false;
                        if (rsPkg == null)
                          packageMatch = assetPkg == null;
                        else if (assetPkg == null)
                          packageMatch = true;  // earlier version in default package should be ignored
                        else
                          packageMatch = rsPkg.getPackageName().equals(assetPkg.getPackageName());

                        if (languageMatch && packageMatch) {
                            already = true;
                            break;
                        }
                    }
                }
                if (!already)
                    latestAssets.add(asset);
            }
        }
        return latestAssets;
    }

    /**
     * Get the latest asset whose name and custom attributes match the parameters.
     * Asset names like 'my.pkg/myAsset.ext' designate package-specific versions;
     * however, package is ignored when selecting by custom attributes.
     */
    public static Asset getLatestAssets(String name, String language, Map<String,String> attributeValues) {
        String assetName = name;
        if (name.indexOf('/') > 0)
          assetName = name.substring(name.indexOf('/') + 1);
        Asset asset = null;
        try {
            for (Asset potentialMatch : getAllAssets()) {
                if (potentialMatch.getName().equals(assetName) && potentialMatch.getLanguage().equals(language)) {
                    boolean attrsMatch = true;
                    for (String attrName : attributeValues.keySet()) {
                        String attrValue = attributeValues.get(attrName);
                        String rsValue = potentialMatch.getAttribute(attrName);
                        if (rsValue == null || !rsValue.equals(attrValue)) {
                            attrsMatch = false;
                            break;
                        }
                    }
                    if (attrsMatch && (asset == null || asset.getVersion() < potentialMatch.getVersion())) {
                        if (!potentialMatch.isLoaded()) {
                            Asset loaded = getAsset(potentialMatch.getId());
                            potentialMatch.setStringContent(loaded.getStringContent());
                        }
                        asset = potentialMatch;
                    }
                }
            }
        }
        catch (DataAccessException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return asset;
    }

    public static Set<String> getRuleNames(String ruleNameRegex) {
        Set<String> matchedRuleNames = new HashSet<String>();
        try {
            for(Asset asset : getAllAssets()){
                if(asset.getName().matches(ruleNameRegex)){
                    matchedRuleNames.add(asset.getName());
                }
            }
        }
        catch (DataAccessException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return matchedRuleNames;
    }

    private static Asset loadAsset(AssetKey key) {
        try {
            for (Asset asset : getAllAssets()) {
                if (key.equals(new AssetKey(asset))) {
                    return DataAccess.getProcessLoader().getAsset(asset.getId());
                }
            }
            return null;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    public static synchronized List<Asset> getAllAssets() throws DataAccessException {
        if (allAssets == null) {
            allAssets = DataAccess.getProcessLoader().getAssets();
            try {
                for (Asset asset : allAssets) {
                    Package pkg = PackageCache.getAssetPackage(asset.getId());
                    if (pkg != null)
                      asset.setPackageName(pkg.getName());
                }
            }
            catch (CachingException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }

        return allAssets;
    }
}

class AssetKey implements Comparable<AssetKey> {
    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String language;
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    private int version;
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public AssetKey(Long id) {
        this.id = id;
    }
    public AssetKey(String name, String language, int version) {
        this.name = name;
        this.language = language;
        this.version = version;
    }
    public AssetKey(String name, String language) {
        this.name = name;
        this.language = language;
    }
    public AssetKey(String name) {
        this.name = name;
    }
    public AssetKey(Asset asset) {
        this.id = asset.getId();
        this.name = asset.getName();
        this.language = asset.getLanguage();
        this.version = asset.getVersion();
    }

    public boolean equals(Object other) {
        AssetKey otherKey = (AssetKey) other;
        if (id != null)
          return id.equals(otherKey.getId());  // if id is specified, base equality only on that

        return compareTo(otherKey) == 0;
    }

    public int compareTo(AssetKey otherKey) {
        if (id != null)
            return -id.compareTo(otherKey.getId());  // id is specified so compare based on that (reverse order)

        if (name == null && otherKey.getName() != null)
            return -1;
        if (otherKey.getName() == null && name != null)
            return 1;
        if (!name.equals(otherKey.getName()))
            return name.compareTo(otherKey.getName());

        if (otherKey.getLanguage() == null)
          otherKey.setLanguage(language);  // language not specified

        if (language == null && otherKey.getLanguage() != null)
            return -1;
        if (otherKey.getLanguage() == null && language != null)
            return 1;
        if (!language.equals(otherKey.getLanguage()))
            return language.compareTo(otherKey.getLanguage());

        if (version == 0 && (id == null || name == null))
            return 0; // search for latest version
        if (otherKey.getVersion() == 0 && (otherKey.getId() == null || otherKey.getName() == null))
            return 0; // search for latest version

        // name and language are the same, sort by version descending
        return otherKey.getVersion() - version;
    }

    public String toString() {
        return "id: '" + id + "'  name: '" + name + "'  language: '" + language + "'  version: " + version;
    }
}

