/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;

public class RuleSetCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<RuleSetKey,RuleSetVO> ruleSetMap;
    private static List<RuleSetVO> allRuleSets;  // initially unpopulated
    private static List<RuleSetVO> latestRuleSets;

    public RuleSetCache() {

    }

    private List<String> preLoaded;

    public RuleSetCache(Map<String,String> params) {
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
        return getRuleSetMap().size();
    }

    public void clearCache() {
        allRuleSets = null;
        latestRuleSets = null;
        ruleSetMap = null;
    }

    public static boolean isLoaded() {
        return ruleSetMap != null || latestRuleSets != null || allRuleSets != null;
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

    public static synchronized Map<RuleSetKey,RuleSetVO> getRuleSetMap() {
        if (ruleSetMap == null) {
            ruleSetMap = Collections.synchronizedMap(new TreeMap<RuleSetKey,RuleSetVO>());
        }
        return ruleSetMap;
    }

    public static RuleSetVO getRuleSet(Long id) {
        return getRuleSet(new RuleSetKey(id));
    }

    public static RuleSetVO getRuleSet(String name) {
        return getRuleSet(new RuleSetKey(name));
    }

    public static RuleSetVO getRuleSet(String name, String language) {
        return getRuleSet(new RuleSetKey(name, language));
    }

    public static RuleSetVO getRuleSet(String name, String[] languages) {
        for (String language : languages) {
            RuleSetVO ruleSetVO = getRuleSet(new RuleSetKey(name, language));
            if (ruleSetVO != null)
                return ruleSetVO;
        }
        return null;  // not found
    }

    public static RuleSetVO getRuleSet(String name, String language, int version) {
        return getRuleSet(new RuleSetKey(name, language, version));
    }

    /**
     * Either a specific version number can be specified, or a Smart Version can be specified which designates an allowable range.
     * @see RuleSetVO.meetsVersionSpec().
     */
    public static RuleSetVO getRuleSet(AssetVersionSpec spec) {
        RuleSetVO match = null, rulesetVO = null;
        try {
            // Get ruleset from package based on asset version spec
            if (spec.getPackageName() != null) {
                // get all the versions of packages based on package name
                List<PackageVO> allPackgeVOs = PackageVOCache.getAllPackageVOs(spec.getPackageName());
                for (PackageVO pkgVO : allPackgeVOs) {
                    for (RuleSetVO ruleSet : pkgVO.getRuleSets()) {
                        if (spec.getName().equals(ruleSet.getName())) {
                            if (ruleSet.meetsVersionSpec(spec.getVersion()) && (match == null || ruleSet.getVersion() > match.getVersion()))
                                match = ruleSet;
                        }
                    }
                }
                if (match != null) {
                    if (!match.isLoaded())
                        rulesetVO = DataAccess.getProcessLoader().getRuleSet(match.getId());
                    else
                        rulesetVO = match;
                }
                return rulesetVO;
            }

            for (RuleSetVO ruleSet : getAllRuleSets()) {
                if (spec.getName().equals(ruleSet.getName())) {
                    if (ruleSet.meetsVersionSpec(spec.getVersion()) && (match == null || ruleSet.getVersion() > match.getVersion()))
                        match = ruleSet;
                }
            }
            if (match != null && !match.isLoaded()) {
                rulesetVO = DataAccess.getProcessLoader().getRuleSet(match.getId());
            }
        } catch (Exception ex) {
            logger.severeException("Failed to load asset: "+spec.toString()+ " : "+ex.getMessage(), ex);
        }
        return rulesetVO;
    }

    /**
     * Get the ruleset based on version spec whose name and custom attributes match the parameters.
     */
    public static RuleSetVO getRuleSet(AssetVersionSpec spec, Map<String,String> attributeValues) {
        RuleSetVO match = null;
        try {
            for (RuleSetVO ruleSet : getAllRuleSets()) {
                if (spec.getName().equals(ruleSet.getName())) {
                    if (ruleSet.meetsVersionSpec(spec.getVersion()) && (match == null || ruleSet.getVersion() > match.getVersion())) {
                        boolean attrsMatch = true;
                        for (String attrName : attributeValues.keySet()) {
                            String attrValue = attributeValues.get(attrName);
                            String rsValue = ruleSet.getAttribute(attrName);
                            if (rsValue == null || !rsValue.equals(attrValue)) {
                                attrsMatch = false;
                                break;
                            }
                        }
                        if (attrsMatch && (match == null || match.getVersion() < ruleSet.getVersion())) {
                            if (!ruleSet.isLoaded()) {
                                RuleSetVO loaded = getRuleSet(ruleSet.getId());
                                ruleSet.setRuleSet(loaded.getRuleSet());
                            }
                            match = ruleSet;
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
     * RuleSet names like 'myPackage/myRuleSet' designate package-specific versions.
     * (or myPackage.MyCustomJavaClass for dynamic Java or Script).
     */
    public static RuleSetVO getRuleSet(RuleSetKey key) {
        int delimIdx = getDelimIndex(key);
        if (delimIdx > 0) {
            // look in package rule sets
            String pkgName = key.getName().substring(0, delimIdx);
            String ruleSetName = key.getName().substring(delimIdx + 1);
            try {
                PackageVO pkgVO = PackageVOCache.getPackageVO(pkgName);
                if (pkgVO != null) {
                    for (RuleSetVO ruleSet : pkgVO.getRuleSets()) {
                        if (ruleSet.getName().equals(ruleSetName))
                            return getRuleSet(new RuleSetKey(ruleSet.getId()));
                        if (RuleSetVO.JAVA.equals(key.getLanguage()) && ruleSet.getName().equals(ruleSetName + ".java"))
                            return getRuleSet(new RuleSetKey(ruleSet.getId()));
                        if (RuleSetVO.GROOVY.equals(key.getLanguage()) && ruleSet.getName().equals(ruleSetName + ".groovy"))
                            return getRuleSet(new RuleSetKey(ruleSet.getId()));
                    }
                }
            }
            catch (CachingException ex) {
                logger.severeException(ex.getMessage(), ex);
            }
            if (ApplicationContext.isFileBasedAssetPersist()) {
                // try to load (for compatibility case)
                RuleSetVO ruleSet = loadRuleSet(key);
                if (ruleSet != null) {
                    getRuleSetMap();
                    synchronized(ruleSetMap) {
                        ruleSetMap.put(new RuleSetKey(ruleSet), ruleSet);
                    }
                    return ruleSet;
                }
            }
            return null;  // not found
        }

        getRuleSetMap();
        synchronized(ruleSetMap) {
            for (RuleSetKey mapKey : ruleSetMap.keySet()) {
                if (key.equals(mapKey))
                    return ruleSetMap.get(mapKey);
            }
        }

        RuleSetVO ruleSet = null;
        ruleSet = loadRuleSet(key);
        if (ruleSet != null) {
            synchronized(ruleSetMap) {
                ruleSetMap.put(new RuleSetKey(ruleSet), ruleSet);
            }
        }
        return ruleSet;
    }

    private static int getDelimIndex(RuleSetKey key) {
        int delimIdx = -1;
        String name = key.getName();
        if (name != null) {
            if (RuleSetVO.JAVA.equals(key.getLanguage())) {
                delimIdx = name.endsWith(".java") ? name.substring(0, name.length() - 6).lastIndexOf('.') : name.lastIndexOf('.');
            }
            else if (RuleSetVO.GROOVY.equals(key.getLanguage())) {
                delimIdx = name.endsWith(".groovy") ? name.substring(0, 8).lastIndexOf('.') : name.lastIndexOf('.');
            }
            else {
              delimIdx = key.getName().indexOf('/');
            }
        }
        return delimIdx;
    }

    public static List<RuleSetVO> getRuleSets(String language) {
        List<RuleSetVO> ruleSets = new ArrayList<RuleSetVO>();
        try {
            for (RuleSetVO ruleSet : getLatestRuleSets()) {
                if (ruleSet.getLanguage() != null && ruleSet.getLanguage().equals(language)) {
                    RuleSetVO fullRuleSet = getRuleSet(ruleSet.getId());
                    ruleSets.add(fullRuleSet);
                }
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return ruleSets;
    }

    public static synchronized List<RuleSetVO> getLatestRuleSets() throws DataAccessException, CachingException {
        if (latestRuleSets == null) {
            latestRuleSets = new ArrayList<RuleSetVO>();
            for (RuleSetVO ruleSet : getAllRuleSets()) {
                PackageVO ruleSetPkg = PackageVOCache.getRuleSetPackage(ruleSet.getId());
                // relies on getAllRuleSets() being ordered by version descending
                boolean already = false;
                for (RuleSetVO rs : latestRuleSets) {
                    PackageVO rsPkg = PackageVOCache.getRuleSetPackage(rs.getId());
                    if (rs.getName().equals(ruleSet.getName())) {
                        // potential match
                        boolean languageMatch = false;
                        if (rs.getLanguage() == null)
                          languageMatch = ruleSet.getLanguage() == null;
                        else
                          languageMatch = rs.getLanguage().equals(ruleSet.getLanguage());
                        boolean packageMatch = false;
                        if (rsPkg == null)
                          packageMatch = ruleSetPkg == null;
                        else if (ruleSetPkg == null)
                          packageMatch = true;  // earlier version in default package should be ignored
                        else
                          packageMatch = rsPkg.getPackageName().equals(ruleSetPkg.getPackageName());

                        if (languageMatch && packageMatch) {
                            already = true;
                            break;
                        }
                    }
                }
                if (!already)
                    latestRuleSets.add(ruleSet);
            }
        }
        return latestRuleSets;
    }

    /**
     * Get the latest ruleset whose name and custom attributes match the parameters.
     * RuleSet names like 'myPackage/myRuleSet' designate package-specific versions;
     * however, package is ignored when selecting by custom attributes.
     */
    public static RuleSetVO getLatestRuleSet(String name, String language, Map<String,String> attributeValues) {
        String ruleSetName = name;
        if (name.indexOf('/') > 0)
          ruleSetName = name.substring(name.indexOf('/') + 1);
        RuleSetVO ruleSet = null;
        try {
            for (RuleSetVO potentialMatch : getAllRuleSets()) {
                if (potentialMatch.getName().equals(ruleSetName) && potentialMatch.getLanguage().equals(language)) {
                    boolean attrsMatch = true;
                    for (String attrName : attributeValues.keySet()) {
                        String attrValue = attributeValues.get(attrName);
                        String rsValue = potentialMatch.getAttribute(attrName);
                        if (rsValue == null || !rsValue.equals(attrValue)) {
                            attrsMatch = false;
                            break;
                        }
                    }
                    if (attrsMatch && (ruleSet == null || ruleSet.getVersion() < potentialMatch.getVersion())) {
                        if (!potentialMatch.isLoaded()) {
                            RuleSetVO loaded = getRuleSet(potentialMatch.getId());
                            potentialMatch.setRuleSet(loaded.getRuleSet());
                        }
                        ruleSet = potentialMatch;
                    }
                }
            }
        }
        catch (DataAccessException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return ruleSet;
    }

    public static Set<String> getRuleNames(String ruleNameRegex) {
        Set<String> matchedRuleNames = new HashSet<String>();
        try {
            for(RuleSetVO ruleSet : getAllRuleSets()){
                if(ruleSet.getName().matches(ruleNameRegex)){
                    matchedRuleNames.add(ruleSet.getName());
                }
            }
        }
        catch (DataAccessException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        return matchedRuleNames;
    }

    private static RuleSetVO loadRuleSet(RuleSetKey key) {
        try {
            for (RuleSetVO ruleSet : getAllRuleSets()) {
                if (key.equals(new RuleSetKey(ruleSet))) {
                    return DataAccess.getProcessLoader().getRuleSet(ruleSet.getId());
                }
            }
            if (ApplicationContext.isFileBasedAssetPersist()) {
                if (DataAccess.isUseCompatibilityDatasource()) {
                    // compatibility check for in-flight db assets
                    // Note: this retrieves by asset name only (ignoring package) -- requires unique asset names
                    ProcessLoader dbLoader = DataAccess.getDbProcessLoader();
                    String ruleSetName;
                    int delimIdx = getDelimIndex(key);
                    if (delimIdx > 0)
                        ruleSetName = key.getName().substring(delimIdx + 1);
                    else
                        ruleSetName = key.getName();
                    return dbLoader.getRuleSet(ruleSetName, key.getLanguage(), key.getVersion());
                }
            }

            return null;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    public static synchronized List<RuleSetVO> getAllRuleSets() throws DataAccessException {
        if (allRuleSets == null) {
            allRuleSets = DataAccess.getProcessLoader().getRuleSets();
            try {
                for (RuleSetVO ruleSet : allRuleSets) {
                    PackageVO pkg = PackageVOCache.getRuleSetPackage(ruleSet.getId());
                    if (pkg != null)
                      ruleSet.setPackageName(pkg.getName());
                }
            }
            catch (CachingException ex) {
                throw new DataAccessException(ex.getMessage(), ex);
            }
        }

        return allRuleSets;
    }
}

class RuleSetKey implements Comparable<RuleSetKey> {
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

    public RuleSetKey(Long id) {
        this.id = id;
    }
    public RuleSetKey(String name, String language, int version) {
        this.name = name;
        this.language = language;
        this.version = version;
    }
    public RuleSetKey(String name, String language) {
        this.name = name;
        this.language = language;
    }
    public RuleSetKey(String name) {
        this.name = name;
    }
    public RuleSetKey(RuleSetVO ruleSet) {
        this.id = ruleSet.getId();
        this.name = ruleSet.getName();
        this.language = ruleSet.getLanguage();
        this.version = ruleSet.getVersion();
    }

    public boolean equals(Object other) {
        RuleSetKey otherKey = (RuleSetKey) other;
        if (id != null)
          return id.equals(otherKey.getId());  // if id is specified, base equality only on that

        return compareTo(otherKey) == 0;
    }

    public int compareTo(RuleSetKey otherKey) {
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

