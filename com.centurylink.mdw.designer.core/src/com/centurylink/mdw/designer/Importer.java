/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessExporter;
import com.centurylink.mdw.dataaccess.ProcessImporter;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.designer.utils.ProcessWorker;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;

/**
 * Command-line importer for XML-formatted workflow asset packages.
 * TODO: Much duplication from Designer plug-in importer.  Needs to be refactored but will require extensive testing.
 */
public class Importer {

    public static void main(String[] args) {

        if (args.length == 1 && args[0].equals("-h")) {
            System.out.println("Example Usage: ");
            System.out.println("java com.centurylink.mdw.designer.Importer appcuid apppassword "
                    + "jdbc:oracle:thin:mdwdemo/mdwdemo@mdwdevdb.dev.qintra.com:1594:mdwdev (or /path/to/root/storage) "
                    + "http://archiva.corp.intranet/archiva/repository/mdw/com/centurylink/mdw/assets/camel/5.5.11/com.centurylink.mdw.camel-5.5.11.xml "
                    + "overwrite=true");
            System.exit(0);
        }
        if (args.length != 4 && args.length != 5) {
            System.err.println("arguments: <user> <password> <jdbcUrl|fileBasedRootDir> <xmlFile|xmlFileUrl> <overwrite=(true|FALSE)>");
            System.err.println("(-h for example usage)");
            System.exit(-1);
        }

        String user = args[0];
        String password = args[1];
        String arg2 = args[2];  // either jdbcUrl or local file path
        String xmlFile = args[3];
        boolean overwrite = (args.length == 5 && args[4].equalsIgnoreCase("overwrite=true")) ?  true : false;

        try {
            DesignerDataAccess.getAuthenticator().authenticate(user, password);
            boolean local = !arg2.startsWith("jdbc:");
            RestfulServer restfulServer = new RestfulServer(local ? "jdbc://dummy" : arg2, user, "http://dummy");
            DesignerDataAccess dataAccess = null;

            if (local) {
                VersionControl versionControl = new VersionControlGit();
                versionControl.connect(null, null, null, new File(arg2));
                restfulServer.setVersionControl(versionControl);
                restfulServer.setRootDirectory(new File(arg2));
                dataAccess = new DesignerDataAccess(restfulServer, null, user, false);
            }
            else {
                dataAccess = new DesignerDataAccess(restfulServer, null, user, true);
                UserVO userVO = dataAccess.getUser(user);
                if (userVO == null)
                    throw new DataAccessException("User: '" + user + "' not found.");
                if (!userVO.hasRole(UserGroupVO.COMMON_GROUP, UserRoleVO.PROCESS_DESIGN))
                    throw new DataAccessException("User: '" + user + "' not authorized for " + UserRoleVO.PROCESS_DESIGN + ".");
            }

            String xml;
            if (xmlFile.startsWith("http://") || xmlFile.startsWith("https://")) {
                xml = new HttpHelper(new URL(xmlFile)).get();
            }
            else {
                xml= readFile(xmlFile);
            }
            System.out.println("Importing with arguments: " + user + " ******* " + arg2 + " " + xmlFile);
            Importer importer = new Importer(dataAccess);
            long before = System.currentTimeMillis();
            importer.importPackage(xml, overwrite);
            long afterImport = System.currentTimeMillis();
            System.out.println("Time taken for import: " + ((afterImport - before)/1000) + " s");
            System.out.println("Please restart your server or refresh the MDW asset cache.");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    static String readFile(String filepath) throws IOException {
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = new FileInputStream(filepath);
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuffer contents = new StringBuffer();
            String line = null;
            while((line = reader.readLine()) != null) {
                contents.append(line);
                contents.append("\n");
            }
            return contents.toString();
        }
        finally {
            if (reader != null)
                reader.close();
            if (is != null)
                is.close();
        }
    }

    private DesignerDataAccess dataAccess;
    private PackageVO importedPackageVO;

    Importer(DesignerDataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    private boolean isLocal() {
        return dataAccess.isVcsPersist();
    }

    public void importPackage(String xml, boolean overwrite)
    throws DataAccessException, RemoteException, ActionCancelledException, XmlException {
        int preexistingVersion = -1;

        System.out.println("Parsing XML...");

        importedPackageVO = parsePackageXml(xml);

        PackageVO existing = null;
        try {
            existing = dataAccess.getPackage(importedPackageVO.getPackageName());
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
                      System.out.println(msg + " -- will overwrite existing package.");

                  // overwrite existing
                  importedPackageVO.setPackageId(existing.getId());
                  if (!isLocal())
                      importedPackageVO.setVersion(0);
                  preexistingVersion = existing.getVersion();
            }
            else if (existing.getVersion() > importedPackageVO.getVersion()) {
                String msg = "Target already contains Package '" + importedPackageVO.getPackageName() + "' v" + existing.getVersionString() + ", whose version is greater than that of the imported package.  Cannot continue.";
                throw new ActionCancelledException(msg);
            }
        }

        System.out.println("Checking elements...");

        final List<RuleSetVO> conflicts = new ArrayList<RuleSetVO>();
        final List<RuleSetVO> conflictsWithDifferences = new ArrayList<RuleSetVO>();

        List<ProcessVO> existingProcessVOs = new ArrayList<ProcessVO>();
        List<ProcessVO> processVOsToBeImported = new ArrayList<ProcessVO>();
        ProcessExporter exporter = null;
        System.out.println("Comparing processes...");
        for (ProcessVO importedProcessVO : importedPackageVO.getProcesses()) {
            ProcessVO existingProcess = null;
            try {
                  existingProcess = dataAccess.getProcessDefinition(importedProcessVO.getProcessName(), importedProcessVO.getVersion());
            }
            catch (DataAccessException ex) {
                // trap process does not exist
                if (!ex.getMessage().startsWith("Process does not exist;"))
                    throw ex;
            }
            if (existingProcess != null) {
                conflicts.add(existingProcess);
                if (dataAccess.getSupportedSchemaVersion() >= DataAccess.schemaVersion52) {
                    // content comparison
                    if (exporter == null) {
                        boolean isOldNamespaces = dataAccess.getDatabaseSchemaVersion() < DataAccess.schemaVersion55;
                        exporter = DataAccess.getProcessExporter(dataAccess.getDatabaseSchemaVersion(), isOldNamespaces ? DesignerCompatibility.getInstance() : null);
                    }
                    String existingProcessXml = dataAccess.getRuleSet(existingProcess.getId()).getRuleSet();
                    String importedProcessXml = exporter.exportProcess(importedProcessVO, dataAccess.getDatabaseSchemaVersion(), null);
                    if (dataAccess.getSupportedSchemaVersion() < DataAccess.schemaVersion55) {
                        // may need to replace old namespace prefix in existing to avoid false positives in 5.2
                        String oldNamespaceDecl = "xmlns:xs=\"http://mdw.qwest.com/XMLSchema\"";
                        int oldNsIdx = existingProcessXml.indexOf(oldNamespaceDecl);
                        if (oldNsIdx > 0) {
                            String newNamespaceDecl = "xmlns:bpm=\"http://mdw.qwest.com/XMLSchema\"";
                            existingProcessXml = existingProcessXml.substring(0, oldNsIdx) + newNamespaceDecl + importedProcessXml.substring(oldNsIdx + oldNamespaceDecl.length() + 2);
                            existingProcessXml = existingProcessXml.replaceAll("<xs:", "<bpm:");
                            existingProcessXml = existingProcessXml.replaceAll("</xs:", "</bpm:");
                        }
                    }
                    // avoid false positives
                    existingProcessXml = existingProcessXml.replaceAll("\\s*<bpm:Attribute Name=\"REFERENCED_ACTIVITIES\".*/>", "");
                    existingProcessXml = existingProcessXml.replaceAll("\\s*<bpm:Attribute Name=\"REFERENCED_PROCESSES\".*/>", "");
                    existingProcessXml = existingProcessXml.replaceFirst(" packageVersion=\"0.0\"", "");
                    existingProcessXml = existingProcessXml.replaceAll("\\s*<bpm:Attribute Name=\"processid\".*/>", "");
                    if (!existingProcessXml.equals(importedProcessXml))
                        conflictsWithDifferences.add(existingProcess);
                }
                if (isLocal())
                    processVOsToBeImported.add(importedProcessVO);
                else
                    existingProcessVOs.add(existingProcess);
            }
            else {
                if (dataAccess.getSupportedSchemaVersion() >= DataAccess.schemaVersion52)
                    importedProcessVO.setInRuleSet(true);  // not optional
                processVOsToBeImported.add(importedProcessVO);
            }
            for (ProcessVO subProcVO : importedProcessVO.getSubProcesses()) {
                ProcessVO existingSubProc = null;
                try {
                    existingSubProc = dataAccess.getProcessDefinition(subProcVO.getProcessName(), subProcVO.getVersion());
                }
                catch (DataAccessException ex) {
                    // trap process does not exist
                    if (!ex.getMessage().startsWith("Process does not exist;"))
                        throw ex;
                }
                if (existingSubProc != null) {
                    conflicts.add(existingSubProc);
                    existingProcessVOs.add(existingSubProc);
                    if (!isLocal())
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
                if (dataAccess.getSupportedSchemaVersion() >= DataAccess.schemaVersion55) {
                    // supports same-named assets in different packages
                    if (existing != null) {
                        RuleSetVO latestAsset = dataAccess.getRuleSet(existing.getPackageId(), importedRuleSet.getName());
                        if (latestAsset != null && latestAsset.getVersion() >= importedRuleSet.getVersion())
                            existingAsset = latestAsset;
                    }
                }
                else {
                    existingAsset = dataAccess.getRuleSet(importedRuleSet.getName(), importedRuleSet.getLanguage(), importedRuleSet.getVersion());
                }

                if (existingAsset != null) {
                    conflicts.add(existingAsset);
                    if (dataAccess.getSupportedSchemaVersion() >= DataAccess.schemaVersion52) {
                        // content comparison
                        existingAsset = dataAccess.getRuleSet(existingAsset.getId());
                        String existingAssetStr = existingAsset.getRuleSet().trim();
                        String importedAssetStr = importedRuleSet.getRuleSet().trim();
                        if (!existingAsset.isBinary()) {
                            existingAssetStr = existingAssetStr.replaceAll("\r", "");
                            importedAssetStr = importedAssetStr.replaceAll("\r", "");
                        }
                        if (!existingAssetStr.equals(importedAssetStr))
                            conflictsWithDifferences.add(existingAsset);
                    }
                    if (isLocal())
                        ruleSetsToBeImported.add(importedRuleSet);
                    else
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
            String msg;
            if (isLocal())
                msg = "The following versions exist locally in '" + importedPackageVO.getPackageName() + "' and will be overwritten:";
            else
                msg = "The following versions from package '" + importedPackageVO.getPackageName() + "' will not be imported:\n(The same or later versions already exist in the target environment";

            if (dataAccess.getDatabaseSchemaVersion() >= DataAccess.schemaVersion52)
                msg += " -- * indicates content differs";
            msg += ").";
            System.out.println(msg);
            for (RuleSetVO rs : conflicts) {
                String flag = conflictsWithDifferences.contains(rs) ? " *" : "";
                System.out.println("   " + rs.getLabel() + flag);
            }
        }

        if (emptyRuleSets.size() > 0) {
            System.out.println("The following assets from package '" + importedPackageVO.getPackageName() + "' will not be imported because they're empty:");
            for (RuleSetVO rs : emptyRuleSets)
                System.out.println("   " + rs.getLabel());
        }

        importedPackageVO.setProcesses(processVOsToBeImported);
        importedPackageVO.setRuleSets(ruleSetsToBeImported);

        // designer fix for backward compatibility
        ProcessWorker worker = new ProcessWorker();
        if (importedPackageVO.getProcesses() != null) {
            NodeMetaInfo nodeMetaInfo = new NodeMetaInfo();
            nodeMetaInfo.init(dataAccess.getActivityImplementors(), dataAccess.getDatabaseSchemaVersion());
            NodeMetaInfo syncedNodeMetaInfo = syncNodeMetaInfo(nodeMetaInfo, importedPackageVO);
            for (ProcessVO p : importedPackageVO.getProcesses()) {
                worker.convert_to_designer(p);
                worker.convert_from_designer(p, syncedNodeMetaInfo);
            }
        }

        System.out.println("Saving package...");

        if (isLocal())
            dataAccess.savePackageNoAudit(importedPackageVO, ProcessPersister.PersistType.IMPORT);
        else
            dataAccess.savePackage(importedPackageVO, ProcessPersister.PersistType.IMPORT);
        if (preexistingVersion > 0)
            importedPackageVO.setVersion(preexistingVersion);  // reset version for overwrite

        if (importedPackageVO.getProcesses() != null) {
            System.out.println("Reloading processes...");
            for (ProcessVO importedProcessVO : importedPackageVO.getProcesses()) {
                ProcessVO reloaded = dataAccess.getProcessDefinition(importedProcessVO.getProcessName(), importedProcessVO.getVersion());
                importedProcessVO.setProcessId(reloaded.getProcessId());
            }
            if (dataAccess.getSupportedSchemaVersion() < DataAccess.schemaVersion52) {
                for (ProcessVO importedProcessVO : importedPackageVO.getProcesses())
                    updateSubProcessIdAttributes(importedProcessVO);
            }
        }
        if (existingProcessVOs.size() > 0) {
            // add back existing processes
            importedPackageVO.getProcesses().addAll(existingProcessVOs);
            dataAccess.savePackage(importedPackageVO);
        }

        if (importedPackageVO.getRuleSets() != null) {
            System.out.println("Reloading workflow assets");
            for (RuleSetVO importedRuleSet : importedPackageVO.getRuleSets()) {
                RuleSetVO reloaded;
                if (dataAccess.getSupportedSchemaVersion() >= DataAccess.schemaVersion55) {
                    reloaded = dataAccess.getRuleSet(importedPackageVO.getId(), importedRuleSet.getName());
                    if (reloaded == null) // TODO: verify whether the above is even needed
                        reloaded = dataAccess.getRuleSet(importedRuleSet.getId());
                }
                else {
                    reloaded = dataAccess.getRuleSet(importedRuleSet.getName(), importedRuleSet.getLanguage(), importedRuleSet.getVersion());
                }

                importedRuleSet.setId(reloaded.getId());
            }
        }

        if (existingRuleSets.size() > 0) {
            importedPackageVO.getRuleSets().addAll(existingRuleSets);
            System.out.println("Saving Package...");
            if (isLocal())
                dataAccess.savePackageNoAudit(importedPackageVO);
            else
                dataAccess.savePackage(importedPackageVO);
        }

        if (preexistingVersion > 0 && existingProcessVOs.size() == 0 && existingRuleSets.size() == 0) {
            System.out.println("Saving Package...");
            if (isLocal())
                dataAccess.savePackageNoAudit(importedPackageVO);  // force associate processes
            else
                dataAccess.savePackage(importedPackageVO);  // force associate processes
        }
    }

    private PackageVO parsePackageXml(String xml) throws DataAccessException {
        int schemaVersion = dataAccess.getDatabaseSchemaVersion();
        ProcessImporter importer = DataAccess.getProcessImporter(schemaVersion);
        return importer.importPackage(xml);
    }

    private NodeMetaInfo syncNodeMetaInfo(NodeMetaInfo existingInfo, PackageVO importedPackageVO) {
        for (ActivityImplementorVO newActImpl : importedPackageVO.getImplementors()) {
            if (newActImpl.getAttributeDescription() == null)
                newActImpl.setAttributeDescription("");  // so isLoaded() == true
            existingInfo.complement(newActImpl);
        }
        return existingInfo;
    }

    /**
     * Update processid attribute for calling processes within this package.
     * TODO: Get rid of this method, which is only needed for ancient (pre-4.5) runtimes.
     */
    private void updateSubProcessIdAttributes(ProcessVO processVO) throws DataAccessException, RemoteException, XmlException {
        boolean toUpdate = false;
        if (processVO.getActivities() != null) {
            for (ActivityVO actVO : processVO.getActivities()) {
                String procNameAttr = actVO.getAttribute("processname");
                String procVerAttr = actVO.getAttribute("processversion");
                if (procNameAttr != null && procVerAttr != null) {
                    toUpdate = true;
                }
            }
            if (processVO.getSubProcesses() != null) {
                for (ProcessVO embedded : processVO.getSubProcesses()) {
                    for (ActivityVO actVO : embedded.getActivities()) {
                        String procNameAttr = actVO.getAttribute("processname");
                        String procVerAttr = actVO.getAttribute("processversion");
                        if (procNameAttr != null && procVerAttr != null) {
                            toUpdate = true;
                        }
                    }
                }
            }
        }
        if (toUpdate) {
            ProcessVO procVO = dataAccess.getProcess(processVO.getProcessId(), processVO);
            for (ActivityVO actVO : procVO.getActivities()) {
                String procNameAttr = actVO.getAttribute("processname");
                String procVerAttr = actVO.getAttribute("processversion");
                if (procNameAttr != null && procVerAttr != null) {
                    for (ProcessVO checkVO : importedPackageVO.getProcesses()) {
                        if (checkVO.getProcessName().equals(procNameAttr) && String.valueOf(checkVO.getVersion()).equals(procVerAttr))
                            actVO.setAttribute("processid", checkVO.getProcessId().toString());
                    }
                }
            }
            if (procVO.getSubProcesses() != null) {
                for (ProcessVO embedded : procVO.getSubProcesses()) {
                    for (ActivityVO actVO : embedded.getActivities()) {
                        String procNameAttr = actVO.getAttribute("processname");
                        String procVerAttr = actVO.getAttribute("processversion");
                        if (procNameAttr != null && procVerAttr != null) {
                            for (ProcessVO checkVO : importedPackageVO.getProcesses()) {
                                if (checkVO.getProcessName().equals(procNameAttr) && String.valueOf(checkVO.getVersion()).equals(procVerAttr))
                                    actVO.setAttribute("processid", checkVO.getProcessId().toString());
                            }
                        }
                    }
                }
            }
            dataAccess.updateProcess(procVO, 0, false);
        }
    }
}
