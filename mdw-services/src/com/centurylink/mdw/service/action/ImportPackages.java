/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessExporter;
import com.centurylink.mdw.dataaccess.ProcessImporter;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.service.Content;
import com.centurylink.mdw.services.ServiceLocator;

/**
 * Interim solution for importing packages into remote projects for VCS assets without Git.
 *
 * Due to XMLBeans defect https://issues.apache.org/jira/browse/XMLBEANS-357,
 * The XML import services strips out newline entities in attribute values.
 * Therefore, this service is no longer used by Designer, which now uses the
 * FileUploadServlet to import from a zip file.
 */
@Deprecated
public class ImportPackages implements XmlService {

    private static final String USER = "User";
    private static final String OVERWRITE = "Overwrite";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    /**
     */
    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo)
    throws ServiceException {
        try {
            if (!ApplicationContext.isFileBasedAssetPersist())
                throw new ServiceException("ImportPackages only valid for file-based asset persistence");
            String user = (String)parameters.get(USER);
            if (user == null)
                throw new ServiceException("Missing '" + USER + "' in " + getClass().getSimpleName() + " request");
            boolean overwrite = false;
            String owParam = (String)parameters.get(OVERWRITE);
            if (owParam != null)
                overwrite = Boolean.parseBoolean(owParam);

            Content content = (Content) parameters.get(CONTENT);
            if (content == null)
                throw new ServiceException("Missing '" + CONTENT + "' in " + getClass().getSimpleName() + " request");

            String packagesXml = content.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));

            VersionControl versionControl = new VersionControlGit();
            File rootDir = new File(PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION));
            versionControl.connect(null, null, null, rootDir);
            LoaderPersisterVcs loaderPersister = new LoaderPersisterVcs("mdw", rootDir, versionControl, new MdwBaselineData(), null);

            long before = System.currentTimeMillis();

            ProcessImporter importer = DataAccess.getProcessImporter(DataAccess.currentSchemaVersion);
            for (PackageVO pkg : importer.importPackages(packagesXml)) {
               importPackage(loaderPersister, pkg, user, overwrite);
            }
            long afterImport = System.currentTimeMillis();
            logger.info("Time taken for package import: " + ((afterImport - before)/1000) + " s");

            // success response
            StatusMessage statusMessage = new StatusMessage();
            statusMessage.setCode(0);
            return statusMessage.getXml();
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo)
    throws ServiceException {
        return getXml(parameters, metaInfo);
    }

    /**
     * Duplicated from com.centurylink.mdw.designer.Importer.java.
     */
    public void importPackage(LoaderPersisterVcs loaderPersister, PackageVO importedPackageVO, String user, boolean overwrite)
    throws DataAccessException, RemoteException, ActionCancelledException, XmlException {
        int preexistingVersion = -1;

        System.out.println("Parsing XML...");

        PackageVO existing = null;
        try {
            existing = loaderPersister.getPackage(importedPackageVO.getPackageName());
        }
        catch (DataAccessException ex) {
            if (!ex.getMessage().startsWith("Package does not exist:"))
                throw ex;
        }
        if (existing != null) {
            if (existing.getVersion() == importedPackageVO.getVersion()) {
                  String msg = "Target already contains Package '" + importedPackageVO.getPackageName() + "' v" + importedPackageVO.getVersionString();
                  if (!overwrite)
                      throw new ActionCancelledException(msg + " and overwrite argument NOT specified.");
                  else
                      logger.info(msg + " -- will overwrite existing package.");

                  // overwrite existing
                  importedPackageVO.setPackageId(existing.getId());
                  preexistingVersion = existing.getVersion();
            }
            else if (existing.getVersion() > importedPackageVO.getVersion()) {
                String msg = "Target already contains Package '" + importedPackageVO.getPackageName() + "' v" + existing.getVersionString() + ", whose version is greater than that of the imported package.  Cannot continue.";
                throw new ActionCancelledException(msg);
            }
        }

        final List<RuleSetVO> conflicts = new ArrayList<RuleSetVO>();
        final List<RuleSetVO> conflictsWithDifferences = new ArrayList<RuleSetVO>();

        List<ProcessVO> existingProcessVOs = new ArrayList<ProcessVO>();
        List<ProcessVO> processVOsToBeImported = new ArrayList<ProcessVO>();
        ProcessExporter exporter = null;
        for (ProcessVO importedProcessVO : importedPackageVO.getProcesses()) {
            ProcessVO existingProcess = null;
            try {
                  existingProcess = loaderPersister.getProcessBase(importedProcessVO.getProcessName(), importedProcessVO.getVersion());
            }
            catch (DataAccessException ex) {
                // trap process does not exist
                if (!ex.getMessage().startsWith("Process does not exist;"))
                    throw ex;
            }
            if (existingProcess != null) {
                conflicts.add(existingProcess);
                // content comparison
                 exporter = DataAccess.getProcessExporter(DataAccess.currentSchemaVersion, null);
                String existingProcessXml = loaderPersister.getRuleSet(existingProcess.getId()).getRuleSet();
                String importedProcessXml = exporter.exportProcess(importedProcessVO, DataAccess.currentSchemaVersion, null);
                // avoid false positives
                existingProcessXml = existingProcessXml.replaceAll("\\s*<bpm:Attribute Name=\"REFERENCED_ACTIVITIES\".*/>", "");
                existingProcessXml = existingProcessXml.replaceAll("\\s*<bpm:Attribute Name=\"REFERENCED_PROCESSES\".*/>", "");
                existingProcessXml = existingProcessXml.replaceFirst(" packageVersion=\"0.0\"", "");
                existingProcessXml = existingProcessXml.replaceAll("\\s*<bpm:Attribute Name=\"processid\".*/>", "");
                if (!existingProcessXml.equals(importedProcessXml))
                    conflictsWithDifferences.add(existingProcess);
                processVOsToBeImported.add(importedProcessVO);

                existingProcessVOs.add(existingProcess);
            }
            else {
                importedProcessVO.setInRuleSet(true);  // not optional
                processVOsToBeImported.add(importedProcessVO);
            }

            for (ProcessVO subProcVO : importedProcessVO.getSubProcesses()) {
                ProcessVO existingSubProc = null;
                try {
                    existingSubProc = loaderPersister.getProcessBase(subProcVO.getProcessName(), subProcVO.getVersion());
                }
                catch (DataAccessException ex) {
                    // trap process does not exist
                    if (!ex.getMessage().startsWith("Process does not exist;"))
                        throw ex;
                }
                if (existingSubProc != null) {
                    conflicts.add(existingSubProc);
                    existingProcessVOs.add(existingSubProc);
                }
            }
        }

        List<RuleSetVO> existingRuleSets = new ArrayList<RuleSetVO>();
        List<RuleSetVO> ruleSetsToBeImported = new ArrayList<RuleSetVO>();
        List<RuleSetVO> emptyRuleSets = new ArrayList<RuleSetVO>();
        if (importedPackageVO.getRuleSets() != null) {
            System.out.println("Comparing assets...");
            for (RuleSetVO importedRuleSet : importedPackageVO.getRuleSets()) {
                RuleSetVO existingAsset = null;
                // supports same-named assets in different packages
                if (existing != null) {
                    RuleSetVO latestAsset = loaderPersister.getRuleSet(existing.getPackageId(), importedRuleSet.getName());
                    if (latestAsset != null && latestAsset.getVersion() >= importedRuleSet.getVersion())
                        existingAsset = latestAsset;
                }

                if (existingAsset != null) {
                    conflicts.add(existingAsset);
                    // content comparison
                    existingAsset = loaderPersister.getRuleSet(existingAsset.getId());
                    String existingAssetStr = existingAsset.getRuleSet().trim();
                    String importedAssetStr = importedRuleSet.getRuleSet().trim();
                    if (!existingAsset.isBinary()) {
                        existingAssetStr = existingAssetStr.replaceAll("\r", "");
                        importedAssetStr = importedAssetStr.replaceAll("\r", "");
                    }
                    if (!existingAssetStr.equals(importedAssetStr))
                        conflictsWithDifferences.add(existingAsset);
                    ruleSetsToBeImported.add(importedRuleSet);

                    existingRuleSets.add(existingAsset);
                }
                else if (importedRuleSet.getRuleSet().trim().isEmpty()) {
                    emptyRuleSets.add(importedRuleSet);
                }
                else {
                    ruleSetsToBeImported.add(importedRuleSet);
                }
            }
        }

        if (conflicts.size() > 0) {
            Collections.sort(conflicts, new Comparator<RuleSetVO>() {
                public int compare(RuleSetVO rs1, RuleSetVO rs2) {
                    return rs1.getLabel().compareToIgnoreCase(rs2.getLabel());
                }
            });
            String msg = "The following versions exist locally in '" + importedPackageVO.getPackageName() + "' and will be overwritten: -- * indicates content differs).";
            logger.info("Importer: " + msg);
            for (RuleSetVO rs : conflicts) {
                String flag = conflictsWithDifferences.contains(rs) ? " *" : "";
                logger.info("   " + rs.getLabel() + flag);
            }
        }

        if (emptyRuleSets.size() > 0) {
            logger.info("The following assets from package '" + importedPackageVO.getPackageName() + "' will not be imported because they're empty:");
            for (RuleSetVO rs : emptyRuleSets)
                logger.info("   " + rs.getLabel());
        }

        importedPackageVO.setProcesses(processVOsToBeImported);
        importedPackageVO.setRuleSets(ruleSetsToBeImported);

        Long packageId = loaderPersister.persistPackage(importedPackageVO, ProcessPersister.PersistType.IMPORT);
        Action action = importedPackageVO.getVersion() == 0 ? Action.Create : Action.Change;
        auditLog(user, action, Entity.Package, packageId, importedPackageVO.getLabel());
        if (preexistingVersion > 0)
            importedPackageVO.setVersion(preexistingVersion);  // reset version for overwrite

        auditLog(user, Action.Import, Entity.Package, packageId, importedPackageVO.getLabel());
    }

    private void auditLog(String user, Action action, Entity entity, Long entityId, String comments) throws DataAccessException {
        UserActionVO userAction = new UserActionVO(user, action, entity, entityId, comments);
        userAction.setSource("Import Packages Service");
        ServiceLocator.getUserServices().auditLog(userAction);
    }

}
