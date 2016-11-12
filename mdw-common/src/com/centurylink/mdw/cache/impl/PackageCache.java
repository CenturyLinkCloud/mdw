/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cache.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.CodeTimer;

public class PackageCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile List<Package> packageList;
    private static final Object lock = new Object();

    public void initialize(Map<String,String> params) {}

    @Override
    public void loadCache() throws CachingException {
        synchronized(lock) {
            packageList = load();
        }
    }

    private static List<Package> getPackageList() throws CachingException {
        List<Package> packageListTemp = packageList;
        if (packageListTemp == null)
            synchronized(lock) {
                packageListTemp = packageList;
                if (packageListTemp == null)
                    packageList = packageListTemp = load();
            }
        return packageListTemp;
    }

    public static List<Package> getPackages() throws CachingException {
        return getPackageList();
    }

    private static synchronized List<Package> load() throws CachingException {
        try {
            CodeTimer timer = new CodeTimer("PackageCache.loadCache()", true);
            List<Package> packageListTemp = DataAccess.getProcessLoader().getPackageList(false, null);
            for (Package pkg : packageListTemp) {
                pkg.setAttributes(loadPackage(pkg).getAttributes());
                pkg.hashProperties();
            }
            Collections.sort(packageListTemp, new Comparator<Package>() {
                public int compare(Package p1, Package p2) {
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
            throw new CachingException(ex.getMessage(), ex);
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
    public static Package getProcessPackage(Long processId) {
        try {
            if (processId != null) {
                for (Package pkg : getPackageList()) {
                  if (pkg.containsProcess(processId))
                      return pkg;
                }
            }
            return Package.getDefaultPackage();
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
    public static Package getTaskTemplatePackage(Long taskId) {
        try {
            for (Package pkg : getPackageList()) {
              if (pkg.containsTaskTemplate(taskId))
                  return pkg;
            }
            return Package.getDefaultPackage();
        }
        catch (CachingException ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Returns the design-time package for a specified assetId
     * (Returns the first match so does not support the same asset in multiple packages).
     */
    public static Package getAssetPackage(Long assetId) throws CachingException {
        for (Package pkg : getPackageList()) {
            if (pkg.containsAsset(assetId))
                return pkg;
        }
        return null;
    }

    public static Package getJavaAssetPackage(String assetName) throws CachingException {
        int lastDot = assetName.lastIndexOf('.');
        if (lastDot == -1) {
            // default package
            return getDefaultPackage();
        }
        else {
            String packageName = assetName.substring(0, lastDot);
            return getPackage(packageName);
        }
    }

    /**
     *  To get all the versions of packageVOs(including archive) based on package name
     * @param packageName
     * @return
     * @throws CachingException
     */
    public static List<Package> getAllPackages(String packageName) throws CachingException {
        List<Package> allPackages = new ArrayList<Package>();
        for (Package packageVO : getPackageList()) {
            if (packageVO.getPackageName().equals(packageName)) {
                allPackages.add(packageVO);
            }
        }
        return allPackages;
    }

    public static Package getPackage(Long packageId) {
        try {
            if (packageId == null || packageId.longValue() == 0)
                return Package.getDefaultPackage();

            for (Package packageVO : getPackageList()) {
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

    public static Package getPackage(String packageName) throws CachingException {
        for (Package packageVO : getPackageList()) {
            if (packageVO.getPackageName().equals(packageName)) {
                return packageVO;
            }
        }
        return null;
    }

    public static Package getDefaultPackage() throws CachingException {
        for (Package pkg : getPackageList()) {
            if (pkg.getPackageName() == null)
              return pkg;
        }
        return null;
    }

    private static Package loadPackage(Package pkg) throws CachingException {
        try {
            ProcessLoader loader = DataAccess.getProcessLoader();
            // populate attributes from attribute table
            Package loaded = loader.loadPackage(pkg.getId(), false);
            // retrieve to avoid deadlock waiting for AssetVOCache
            Asset assetVO = loader.getAssetForOwner(OwnerType.PACKAGE, loaded.getPackageId());
            if (assetVO != null && assetVO.getStringContent() != null) {
                if (assetVO.getStringContent().trim().startsWith("{")) {
                    Package metaPkg = new Package(new JSONObject(assetVO.getStringContent()));
                    List<Attribute> envAttrs = metaPkg.getAttributes(ApplicationContext.getRuntimeEnvironment());
                    if (envAttrs != null) {
                        envAttrs.addAll(metaPkg.getAttributes(null)); // non-env-specific
                        loaded.setAttributes(envAttrs);
                    }
                    loaded.setGroup(metaPkg.getGroup());
                }
            }
            return loaded;
        }
        catch (Exception ex) {
            throw new CachingException(ex.getMessage(), ex);
        }
    }
}
