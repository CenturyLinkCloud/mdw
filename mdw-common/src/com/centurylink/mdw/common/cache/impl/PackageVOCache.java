/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.bpm.MDWPackage;
import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.PackageDocument;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.bpm.PropertyDocument.Property;
import com.centurylink.mdw.bpm.PropertyGroupDocument;
import com.centurylink.mdw.bpm.PropertyGroupDocument.PropertyGroup;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.cache.PreloadableCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;

public class PackageVOCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile List<PackageVO> packageList;
    private static final Object lock = new Object();

    public void initialize(Map<String,String> params) {}

    @Override
    public void loadCache() throws CachingException {
        synchronized(lock) {
            packageList = load();
        }
    }

    private static List<PackageVO> getPackageList() throws CachingException {
        List<PackageVO> packageListTemp = packageList;
        if (packageListTemp == null)
            synchronized(lock) {
                packageListTemp = packageList;
                if (packageListTemp == null)
                    packageList = packageListTemp = load();
            }
        return packageListTemp;
    }

    private static synchronized List<PackageVO> load() throws CachingException {
        try {
            CodeTimer timer = new CodeTimer("PackageVOCache.loadCache()", true);
            List<PackageVO> packageListTemp = DataAccess.getProcessLoader().getPackageList(false, null);
            for (PackageVO pkg : packageListTemp) {
                pkg.setAttributes(loadPackage(pkg).getAttributes());
                pkg.hashProperties();
            }
            Collections.sort(packageListTemp, new Comparator<PackageVO>() {
                public int compare(PackageVO p1, PackageVO p2) {
                    // latest first
                    if (p1.getName().equals(p2.getName()))
                        return p2.getVersion() - p1.getVersion();
                    else
                        return p1.getName().compareToIgnoreCase(p2.getName());
                }
            });
            timer.stopAndLogTiming("Load package list");
            return packageListTemp;
        }
        catch (DataAccessException ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }

    @Override
    public void clearCache() {}

    @Override
    public synchronized void refreshCache() throws CachingException {
        loadCache();
    }


    /**
     * Returns the design-time package for a specified process ID.
     * Returns the first match so does not support the same process in multiple packages.
     * Also, assumes the processId is not for an embedded subprocess.
     */
    public static PackageVO getProcessPackage(Long processId) {
        try {
            for (PackageVO packageVO : getPackageList()) {
              if (packageVO.containsProcess(processId))
                  return packageVO;
            }
            return PackageVO.getDefaultPackage();
        }
        catch (CachingException ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Returns the design-time package for a specified task ID.
     * Returns the first match so does not support the same template in multiple packages.
     */
    public static PackageVO getTaskTemplatePackage(Long taskId) {
        try {
            for (PackageVO packageVO : getPackageList()) {
              if (packageVO.containsTaskTemplate(taskId))
                  return packageVO;
            }
            return PackageVO.getDefaultPackage();
        }
        catch (CachingException ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Returns the design-time package for a specified ruleSetId
     * (Returns the first match so does not support the same asset in multiple packages).
     */
    public static PackageVO getRuleSetPackage(Long ruleSetId) throws CachingException {
        for (PackageVO packageVO : getPackageList()) {
            if (packageVO.containsRuleSet(ruleSetId))
                return packageVO;
        }
        return null;
    }

    public static PackageVO getPackageVO(String packageName) throws CachingException {
        for (PackageVO packageVO : getPackageList()) {
            if (packageVO.getPackageName().equals(packageName)) {
                return packageVO;
            }
        }
        return null;
    }

    public static PackageVO getJavaRuleSetPackage(String ruleSetName) throws CachingException {
        int lastDot = ruleSetName.lastIndexOf('.');
        if (lastDot == -1) {
            // default package
            return getDefaultPackageVO();
        }
        else {
            String packageName = ruleSetName.substring(0, lastDot);
            return getPackageVO(packageName);
        }
    }

    /**
     *  To get all the versions of packageVOs(including archive) based on package name
     * @param packageName
     * @return
     * @throws CachingException
     */
    public static List<PackageVO> getAllPackageVOs(String packageName) throws CachingException {
        List<PackageVO> allPackages = new ArrayList<PackageVO>();
        for (PackageVO packageVO : getPackageList()) {
            if (packageVO.getPackageName().equals(packageName)) {
                allPackages.add(packageVO);
            }
        }
        return allPackages;
    }

    public static PackageVO getPackage(Long packageId) {
        try {
            if (packageId == null || packageId.longValue() == 0)
                return PackageVO.getDefaultPackage();

            for (PackageVO packageVO : getPackageList()) {
                if (packageVO.getPackageId().equals(packageId)) {
                    return packageVO;
                }
            }
            return null;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    public static PackageVO getPackage(String packageName) {
        try {
            if (packageName == null || packageName.isEmpty() || packageName.equals(PackageVO.getDefaultPackage().getPackageName()))
                return PackageVO.getDefaultPackage();

            for (PackageVO packageVO : getPackageList()) {
                if (packageVO.getPackageName().equals(packageName)) {
                    return packageVO;
                }
            }
            return null;
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    public static PackageVO getDefaultPackageVO() throws CachingException {
        for (PackageVO packageVO : getPackageList()) {
            if (packageVO.getPackageName().equals(PackageVO.DEFAULT_PACKAGE_NAME))
              return packageVO;
        }
        return null;
    }

    private static PackageVO loadPackage(PackageVO packageVO) throws CachingException {
        try {
            ProcessLoader loader = DataAccess.getProcessLoader();
            // populate attributes from attribute table
            PackageVO loaded = loader.loadPackage(packageVO.getId(), false);
            // retrieve to avoid deadlock waiting for RuleSetCache
            RuleSetVO ruleSet = loader.getRuleSetForOwner(OwnerType.PACKAGE, loaded.getPackageId());
            if (ruleSet != null && ruleSet.getRuleSet() != null) {
                List<PropertyGroup> propertyGrp = null;
                if (ruleSet.getRuleSet().startsWith("<bpm:package") || ruleSet.getRuleSet().startsWith("<package")) {
                    PackageDocument pkgDoc = PackageDocument.Factory.parse(ruleSet.getRuleSet());
                    MDWPackage pkg = pkgDoc.getPackage();
                    if (pkg.getApplicationProperties() != null && pkg.getApplicationProperties().getPropertyGroupList() != null) {
                        propertyGrp = pkg.getApplicationProperties().getPropertyGroupList();
                    }
                }
                else {
                    ProcessDefinitionDocument processDefDoc = ProcessDefinitionDocument.Factory.parse(ruleSet.getRuleSet(), Compatibility.namespaceOptions());
                    MDWProcessDefinition procDef = processDefDoc.getProcessDefinition();
                    if (procDef != null && procDef.getApplicationProperties() != null && procDef.getApplicationProperties().getPropertyGroupList() != null) {
                        propertyGrp = processDefDoc.getProcessDefinition().getApplicationProperties().getPropertyGroupList();
                    }
                }
                if (propertyGrp != null) {
                    for (PropertyGroupDocument.PropertyGroup propGroup : propertyGrp) {
                        if (propGroup.getName() != null && propGroup.getName().equals(ApplicationContext.getRuntimeEnvironment()) && propGroup.getPropertyList() != null) {
                            for (Property prop : propGroup.getPropertyList()) {
                                AttributeVO attr = new AttributeVO(prop.getName(), prop.getStringValue());
                                loaded.getAttributes().add(attr);
                            }
                        }
                    }
                }
            }
            return loaded;
        }
        catch (Exception ex) {
            throw new CachingException(-1, ex.getMessage(), ex);
        }
    }
}
