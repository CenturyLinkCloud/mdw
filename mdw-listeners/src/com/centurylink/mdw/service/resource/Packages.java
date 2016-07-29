/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessExporter;
import com.centurylink.mdw.dataaccess.file.GitDiffs.DiffType;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.Download;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.value.asset.PackageList;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.asset.AssetServicesImpl;

/**
 * This is an interim solution to be accessed for remote projects for client apps using VCS
 * assets who have not migrated to Git.  This service is slow and expensive.
 *
 * For asset package retrieval use Assets.java.
 */
public class Packages implements XmlService, JsonService {

    /**
     * Get all non-archived packages in XML format.
     */
    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        try {
            metaInfo.put(Listener.METAINFO_NO_PERSISTENCE, "false");
            List<PackageVO> packages = DataAccess.getProcessLoader().getPackageList(true, null);
            Map<String,PackageVO> topLevelPackages = new HashMap<String,PackageVO>();
            for (PackageVO pkg : packages) {
                PackageVO top = topLevelPackages.get(pkg.getName());
                if (top == null || top.getVersion() < pkg.getVersion())
                    topLevelPackages.put(pkg.getName(), pkg);
            }
            ProcessExporter exporter = DataAccess.getProcessExporter(DataAccess.currentSchemaVersion);
            return exporter.exportPackages(Arrays.asList(topLevelPackages.values().toArray(new PackageVO[0])), true);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getXml(parameters, metaInfo);
    }

    /**
     * Only works for VCS assets.
     * Returns a JSON download response with a URL pointing to the package zip file for download.
     * Parameters: archive (default = false), topLevel (default = false).  At least one parameter must be true.
     */
    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        boolean archive = "true".equals(parameters.get("archive"));
        boolean topLevel = "true".equals(parameters.get("topLevel"));
        boolean nonVersioned = "true".equals(parameters.get("nonVersioned"));
        if (!archive && !topLevel && !nonVersioned)
            throw new ServiceException("At least one of 'topLevel', 'nonVersioned' or 'archive' must be true");
        if (!ApplicationContext.isFileBasedAssetPersist())
            throw new ServiceException("Only applicable for VCS assets");

        try {
            File tempDir = new File(ApplicationContext.getTempDirectory());
            if (!tempDir.exists()) {
              if (!tempDir.mkdirs())
                  throw new IOException("Unable to create temporary directory: " + tempDir);
            }
            if (!tempDir.isDirectory())
                throw new IOException("Temp location is not a directory: " + tempDir);
            File pkgDir = new File(PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION));
            String rootName = topLevel ? "pkg" : "archive";
            File zipFile = new File(tempDir + "/" + rootName + StringHelper.filenameDateToString(new Date()) + ".zip");
            if (nonVersioned) {
                List<File> extraPackages = new ArrayList<File>();
                AssetServices assetServices = new AssetServicesImpl();
                PackageList packageList = assetServices.getPackages();
                for (PackageDir packageDir : packageList.getPackageDirs()) {
                    if (packageDir.getVcsDiffType() == DiffType.EXTRA)
                        extraPackages.add(packageDir);
                }
                if (archive) {
                    File archiveDir = new File(pkgDir + "/" + PackageDir.ARCHIVE_SUBDIR);
                    if (archiveDir.isDirectory())
                      extraPackages.add(archiveDir);
                }
                FileHelper.createZipFileWith(pkgDir, zipFile, extraPackages);
            }
            else {
                List<File> excludes = new ArrayList<File>();
                excludes.add(new File(".dmignore"));
                excludes.add(new File(".gitignore"));
                if (!topLevel || !archive) {
                    for (File file : pkgDir.listFiles()) {
                        if (!archive && file.getName().equals(PackageDir.ARCHIVE_SUBDIR))
                            excludes.add(file);
                        else if (!topLevel && !file.getName().equals(PackageDir.ARCHIVE_SUBDIR))
                            excludes.add(file);
                    }
                }
                FileHelper.createZipFile(pkgDir, zipFile, excludes);
            }
            String url = "http://" + ApplicationContext.getServerHostPort() + "/" + ApplicationContext.getMdwHubContextRoot() +
                    "/system/download?filepath=" + zipFile.getPath().replace('\\', '/');
            Download download = new Download(url);
            download.setFile(zipFile.getName());
            return download.getJson().toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}
