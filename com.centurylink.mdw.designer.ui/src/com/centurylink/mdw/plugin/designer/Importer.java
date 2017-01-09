/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWPackage;
import com.centurylink.mdw.bpm.MDWProcess;
import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.PackageDocument;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.JsonUtil;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessExporter;
import com.centurylink.mdw.dataaccess.ProcessImporter;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.designer.DesignerCompatibility;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.designer.utils.ProcessWorker;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.plugin.CodeTimer;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class Importer
{
  private PluginDataAccess dataAccess;
  private Shell shell;

  /**
   * Null shell means non-interactive.
   */
  public Importer(PluginDataAccess dataAccess, Shell shell)
  {
    this.dataAccess = dataAccess;
    this.shell = shell;
  }

  private boolean isLocal()
  {
    return dataAccess.getDesignerDataAccess().isVcsPersist();
  }

  private PackageVO importedPackageVO;

  public WorkflowPackage importPackage(final WorkflowProject project, final String content, final ProgressMonitor progressMonitor)
  throws DataAccessException, RemoteException, ActionCancelledException, JSONException, XmlException
  {
    CodeTimer timer = new CodeTimer("importPackage()");
    int preexistingVersion = -1;
    importedPackageVO = null;

    progressMonitor.start("Importing Package into: '" + project.getLabel() + "'");
    progressMonitor.progress(5);

    progressMonitor.subTask("Parsing XML");

    boolean isJson = content.trim().startsWith("{");

    importedPackageVO = parsePackageContent(content);

    progressMonitor.subTask("Importing " + importedPackageVO.getLabel());

    progressMonitor.progress(10);

    final WorkflowPackage existing = project.getPackage(importedPackageVO.getPackageName());
    if (existing != null)
    {
      if (existing.getVersion() == importedPackageVO.getVersion())
      {
        final String msg = project.getName() + " already contains Package '" + importedPackageVO.getPackageName() + "' v" + importedPackageVO.getVersionString();
        if (shell != null)
        {
          shell.getDisplay().syncExec(new Runnable()
          {
            public void run()
            {
              if (!MessageDialog.openConfirm(shell, "Import Package", msg + ".\nImport this package?"))
                importedPackageVO = null;
            }
          });
        }
        else
        {
          PluginMessages.log(msg);
        }

        if (importedPackageVO != null)
        {
          // overwrite existing
          importedPackageVO.setPackageId(existing.getId());
          if (!isLocal())
            importedPackageVO.setVersion(0);
          preexistingVersion = existing.getVersion();
        }
      }
      else if (existing.getVersion() > importedPackageVO.getVersion())
      {
        final String msg = project.getName() + " already contains Package '" + importedPackageVO.getPackageName() + "' v" + existing.getVersionString() + ", whose version is greater than that of the imported package.  Cannot continue.";
        if (shell != null)
        {
          shell.getDisplay().syncExec(new Runnable()
          {
            public void run()
            {
              MessageDialog.openError(shell, "Import Package", msg);
              importedPackageVO = null;
            }
          });
        }
        else
        {
          PluginMessages.log(msg);
        }
      }

      if (importedPackageVO == null)
        return null;
    }

    if (shell != null && progressMonitor.isCanceled())
      throw new ActionCancelledException();

    progressMonitor.progress(10);

    progressMonitor.subTask("Checking elements");

    final List<WorkflowElement> conflicts = new ArrayList<WorkflowElement>();
    final List<WorkflowElement> conflictsWithDifferences = new ArrayList<WorkflowElement>();

    final List<ProcessVO> existingProcessVOs = new ArrayList<ProcessVO>();
    List<ProcessVO> processVOsToBeImported = new ArrayList<ProcessVO>();
    ProcessExporter exporter = null;
    for (ProcessVO importedProcessVO : importedPackageVO.getProcesses())
    {
      WorkflowProcess existingProcess = project.getProcess(importedProcessVO.getProcessName(), importedProcessVO.getVersionString());
      if (existingProcess != null)
      {
        conflicts.add(existingProcess);
        if (project.getDataAccess().getSupportedSchemaVersion() >= DataAccess.schemaVersion52
            && MdwPlugin.getSettings().isCompareConflictingAssetsDuringImport())
        {
          progressMonitor.subTask("Comparing processes (can be disabled in prefs)");
          // content comparison
          if (exporter == null)
            exporter = DataAccess.getProcessExporter(project.getDataAccess().getSchemaVersion(), project.isOldNamespaces() ? DesignerCompatibility.getInstance() : null);
          String existingProcessXml = project.getDataAccess().loadRuleSet(existingProcess.getId()).getRuleSet();
          String importedProcessXml = isJson ? importedProcessVO.getJson().toString(2) : exporter.exportProcess(importedProcessVO, project.getDataAccess().getSchemaVersion(), null);
          if (project.getDataAccess().getSupportedSchemaVersion() < DataAccess.schemaVersion55)
          {
            // may need to replace old namespace prefix in existing to avoid false positives in 5.2
            String oldNamespaceDecl = "xmlns:xs=\"http://mdw.qwest.com/XMLSchema\"";
            int oldNsIdx = existingProcessXml.indexOf(oldNamespaceDecl);
            if (oldNsIdx > 0)
            {
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
          existingProcessVOs.add(existingProcess.getProcessVO());
      }
      else
      {
        if (project.getDataAccess().getSupportedSchemaVersion() >= DataAccess.schemaVersion52)
          importedProcessVO.setInRuleSet(true);  // not optional
        processVOsToBeImported.add(importedProcessVO);
      }
      for (ProcessVO subProcVO : importedProcessVO.getSubProcesses())
      {
        WorkflowProcess existingSubProc = project.getProcess(subProcVO.getProcessName(), subProcVO.getVersionString());
        if (existingSubProc != null)
        {
          conflicts.add(existingSubProc);
          existingProcessVOs.add(existingSubProc.getProcessVO());
          if (!isLocal())
            existingProcessVOs.add(existingSubProc.getProcessVO());
        }
      }
    }

    if (shell != null && progressMonitor.isCanceled())
      throw new ActionCancelledException();

    progressMonitor.progress(10);

    final List<RuleSetVO> existingRuleSets = new ArrayList<RuleSetVO>();
    List<RuleSetVO> ruleSetsToBeImported = new ArrayList<RuleSetVO>();
    final List<RuleSetVO> emptyRuleSets = new ArrayList<RuleSetVO>();
    if (importedPackageVO.getRuleSets() != null)
    {
      for (RuleSetVO importedRuleSet : importedPackageVO.getRuleSets())
      {
        WorkflowAsset existingAsset = null;
        if (dataAccess.getSupportedSchemaVersion() >= DataAccess.schemaVersion55)
          existingAsset = project.getAsset(importedPackageVO.getName(), importedRuleSet.getName(), importedRuleSet.getLanguage(), importedRuleSet.getVersion());
        else
          existingAsset = project.getAsset(importedRuleSet.getName(), importedRuleSet.getLanguage(), importedRuleSet.getVersion());
        if (existingAsset != null)
        {
          conflicts.add(existingAsset);
          if (project.getDataAccess().getSupportedSchemaVersion() >= DataAccess.schemaVersion52
              && MdwPlugin.getSettings().isCompareConflictingAssetsDuringImport() && !existingAsset.isBinary())
          {
            progressMonitor.subTask("Comparing assets (can be disabled in prefs)");
            // content comparison
            existingAsset = project.getDesignerProxy().loadWorkflowAsset(existingAsset);
            String existingAssetStr = existingAsset.getRuleSetVO().getRuleSet().trim();
            String importedAssetStr = importedRuleSet.getRuleSet().trim();
            if (!existingAsset.isBinary())
            {
              existingAssetStr = existingAssetStr.replaceAll("\r", "");
              importedAssetStr = importedAssetStr.replaceAll("\r", "");
            }
            if (!existingAssetStr.equals(importedAssetStr))
              conflictsWithDifferences.add(existingAsset);
          }
          if (isLocal())
            ruleSetsToBeImported.add(importedRuleSet);
          else
            existingRuleSets.add(existingAsset.getRuleSetVO());
        }
        else if (importedRuleSet.getRuleSet().trim().isEmpty())
        {
          emptyRuleSets.add(importedRuleSet);
        }
        else
        {
          ruleSetsToBeImported.add(importedRuleSet);
        }
      }
    }

    if (MdwPlugin.getSettings().isCompareConflictingAssetsDuringImport()
        && existing != null && importedPackageVO.getTaskTemplates() != null && existing.getTaskTemplates() != null)
    {
      for (TaskVO importedTask : importedPackageVO.getTaskTemplates())
      {
        for (TaskTemplate taskTemplate : existing.getTaskTemplates())
        {
          if (taskTemplate.getName().equals(importedTask.getName()) && taskTemplate.getVersion() == importedTask.getVersion())
          {
            conflicts.add(taskTemplate);
            String existingTemplStr = taskTemplate.getTaskVO().toTemplate().xmlText();
            String importedTemplStr = importedTask.toTemplate().xmlText();
            if (!existingTemplStr.equals(importedTemplStr))
              conflictsWithDifferences.add(taskTemplate);
          }
        }
      }
    }

    if (progressMonitor.isCanceled())
      throw new ActionCancelledException();

    progressMonitor.progress(10);

    if (conflicts.size() > 0)
    {
      Collections.sort(conflicts, new Comparator<WorkflowElement>()
      {
        public int compare(WorkflowElement we1, WorkflowElement we2)
        {
          return we1.getLabel().compareToIgnoreCase(we2.getLabel());
        }
      });

      final String msg;
      if (isLocal())
        msg = "The following versions exist locally in '" + importedPackageVO.getPackageName() + "'.\nThese files in project '" + project.getName() + "' will be overwritten.\n";
      else
        msg = "The following versions from package '" + importedPackageVO.getPackageName() + "' will not be imported.\nThe same versions already exist in the '" + project.getName() + "' project.\n";

      if (shell != null)
      {
        shell.getDisplay().syncExec(new Runnable()
        {
          public void run()
          {
            String msg2 = msg;
            if (project.checkRequiredVersion(5, 2) && MdwPlugin.getSettings().isCompareConflictingAssetsDuringImport())
              msg2 += "(Asterisk * indicates content differs.)\n";
            int res = PluginMessages.uiList(shell, msg2, "Package Import", conflicts, conflictsWithDifferences);
            if (res == Dialog.CANCEL)
              importedPackageVO = null;
          }
        });
      }
      else
      {
        String msg2 = msg + " (";
        if (project.checkRequiredVersion(5, 2))
          msg2 += " -- * indicates content differs";
        msg2 += ").\n";
        for (WorkflowElement we : conflicts)
        {
          String flag = conflictsWithDifferences.contains(we) ? " *" : "";
          msg2 += "   " + we.getLabel() + flag + "\n";
        }
        PluginMessages.log(msg2);
      }
      if (importedPackageVO == null)
        return null;
    }

    if (emptyRuleSets.size() > 0)
    {
      final String msg = "The following assets from package '" + importedPackageVO.getPackageName() + "' will not be imported because they're empty.\n";
      if (shell != null)
      {
        shell.getDisplay().syncExec(new Runnable()
        {
          public void run()
          {
            int res = PluginMessages.uiList(shell, msg, "Package Import", emptyRuleSets);
            if (res == Dialog.CANCEL)
              importedPackageVO = null;
          }
        });
      }
      else
      {
        String msg2 = msg;
        for (RuleSetVO rs : emptyRuleSets)
          msg2 += "   " + rs.getLabel() + "\n";
        PluginMessages.log(msg2);
      }

      if (importedPackageVO == null)
        return null;
    }

    importedPackageVO.setProcesses(processVOsToBeImported);
    importedPackageVO.setRuleSets(ruleSetsToBeImported);

    // designer fix for backward compatibility
    ProcessWorker worker = new ProcessWorker();
    if (importedPackageVO.getProcesses() != null)
    {
      NodeMetaInfo syncedNodeMetaInfo = syncNodeMetaInfo(dataAccess.getDesignerDataModel().getNodeMetaInfo(), importedPackageVO);
      for (ProcessVO p : importedPackageVO.getProcesses())
      {
        worker.convert_to_designer(p);
        worker.convert_from_designer(p, syncedNodeMetaInfo);
      }
    }

    if (shell != null && progressMonitor.isCanceled())
      throw new ActionCancelledException();

    progressMonitor.progress(10);
    progressMonitor.subTask("Saving package");

    ProcessPersister.PersistType persistType = ProcessPersister.PersistType.IMPORT;
    if (isJson)
      persistType = ProcessPersister.PersistType.IMPORT_JSON;
    Long packageId = dataAccess.getDesignerDataAccess().savePackage(importedPackageVO, persistType);
    if (preexistingVersion > 0)
      importedPackageVO.setVersion(preexistingVersion);  // reset version for overwrite

    progressMonitor.progress(10);
    progressMonitor.subTask("Reloading processes");

    if (importedPackageVO.getProcesses() != null)
    {
      for (ProcessVO importedProcessVO : importedPackageVO.getProcesses())
      {
        ProcessVO reloaded = dataAccess.getDesignerDataAccess().getProcessDefinition(importedProcessVO.getProcessName(), importedProcessVO.getVersion());
        importedProcessVO.setProcessId(reloaded.getProcessId());
      }
      if (project.getDataAccess().getSupportedSchemaVersion() < DataAccess.schemaVersion52)
      {
        for (ProcessVO importedProcessVO : importedPackageVO.getProcesses())
          updateSubProcessIdAttributes(importedProcessVO);
      }
    }
    if (existingProcessVOs.size() > 0)
    {
      // add back existing processes
      importedPackageVO.getProcesses().addAll(existingProcessVOs);
      dataAccess.getDesignerDataAccess().savePackage(importedPackageVO);
    }

    progressMonitor.progress(10);

    progressMonitor.subTask("Reloading workflow assets");

    if (importedPackageVO.getRuleSets() != null)
    {
      for (RuleSetVO importedRuleSet : importedPackageVO.getRuleSets())
      {
        RuleSetVO reloaded;
        if (dataAccess.getSupportedSchemaVersion() >= DataAccess.schemaVersion55)
        {
          reloaded = dataAccess.getDesignerDataAccess().getRuleSet(importedPackageVO.getId(), importedRuleSet.getName());
          if (reloaded == null) // TODO: verify whether the above is even needed
            reloaded = dataAccess.getDesignerDataAccess().getRuleSet(importedRuleSet.getId());
        }
        else
        {
          reloaded = dataAccess.getDesignerDataAccess().getRuleSet(importedRuleSet.getName(), importedRuleSet.getLanguage(), importedRuleSet.getVersion());
        }

        importedRuleSet.setId(reloaded.getId());
      }
    }
    if (existingRuleSets.size() > 0)
    {
      importedPackageVO.getRuleSets().addAll(existingRuleSets);
      progressMonitor.subTask("Saving Package");
      dataAccess.getDesignerDataAccess().savePackage(importedPackageVO);
    }

    if (preexistingVersion > 0 && existingProcessVOs.size() == 0 && existingRuleSets.size() == 0)
    {
      progressMonitor.subTask("Saving Package");
      dataAccess.getDesignerDataAccess().savePackage(importedPackageVO);  // force associate processes
    }

    progressMonitor.progress(10);

    progressMonitor.subTask("Loading package");

    PackageVO newPackageVO = dataAccess.getDesignerDataAccess().loadPackage(packageId, false);
    WorkflowPackage importedPackage = new WorkflowPackage(project, newPackageVO);

    List<WorkflowProcess> processVersions = new ArrayList<WorkflowProcess>();
    for (ProcessVO processVO : newPackageVO.getProcesses())
    {
      WorkflowProcess processVersion = new WorkflowProcess(project, processVO);
      processVersion.setPackage(importedPackage);
      processVersions.add(processVersion);
    }
    Collections.sort(processVersions);
    importedPackage.setProcesses(processVersions);

    List<ExternalEvent> externalEvents = new ArrayList<ExternalEvent>();
    for (ExternalEventVO externalEventVO : newPackageVO.getExternalEvents())
    {
      ExternalEvent externalEvent = new ExternalEvent(externalEventVO, importedPackage);
      externalEvents.add(externalEvent);
    }
    Collections.sort(externalEvents);
    importedPackage.setExternalEvents(externalEvents);

    List<TaskTemplate> taskTemplates = new ArrayList<TaskTemplate>();
    if (newPackageVO.getTaskTemplates() != null)
    {
      for (TaskVO taskVO : newPackageVO.getTaskTemplates())
      {
        TaskTemplate taskTemplate = new TaskTemplate(taskVO, importedPackage);
        taskTemplates.add(taskTemplate);
      }
      Collections.sort(taskTemplates);
      importedPackage.setTaskTemplates(taskTemplates);
    }

    List<ActivityImpl> activityImpls = new ArrayList<ActivityImpl>();
    if (newPackageVO.getImplementors() != null)
    {
      for (ActivityImplementorVO activityImplVO : newPackageVO.getImplementors())
      {
        if (importedPackageVO.getImplementors() != null)
        {
          // attrs not included in reload -- take from original XML
          ActivityImplementorVO xmlImplVO = null;
          for (ActivityImplementorVO implVO : importedPackageVO.getImplementors())
          {
            if (activityImplVO.getImplementorClassName().equals(implVO.getImplementorClassName()))
              xmlImplVO = implVO;
          }
          if (xmlImplVO != null)
          {
            activityImplVO.setBaseClassName(xmlImplVO.getBaseClassName());
            activityImplVO.setIconName(xmlImplVO.getIconName());
            activityImplVO.setShowInToolbox(xmlImplVO.isShowInToolbox());
            activityImplVO.setLabel(xmlImplVO.getLabel());
            activityImplVO.setAttributeDescription(xmlImplVO.getAttributeDescription());
          }
        }
        ActivityImpl activityImpl = new ActivityImpl(activityImplVO, importedPackage);
        activityImpls.add(activityImpl);
      }
      Collections.sort(activityImpls);
      importedPackage.setActivityImpls(activityImpls);
    }

    if (newPackageVO.getRuleSets() != null)
    {
      List<WorkflowAsset> assets = new ArrayList<WorkflowAsset>();
      for (RuleSetVO ruleSet : newPackageVO.getRuleSets())
      {
        WorkflowAsset asset = WorkflowAssetFactory.createAsset(ruleSet, importedPackage);
        assets.add(asset);
      }
      Collections.sort(assets);
      importedPackage.setAssets(assets);
    }

    if (existing != null)
    {
      // deregister old assets
      if (existing.getAssets() != null)
      {
        for (WorkflowAsset oldAsset : existing.getAssets())
          WorkflowAssetFactory.deRegisterAsset(oldAsset);
      }
      project.removePackage(existing);
    }
    // register new assets
    if (importedPackage.getAssets() != null)
    {
      for (WorkflowAsset newAsset : importedPackage.getAssets())
        WorkflowAssetFactory.registerAsset(newAsset);
    }
    project.addPackage(importedPackage);

    progressMonitor.progress(10);

    dataAccess.auditLog(Action.Import, importedPackage);

    dataAccess.getPackages(true);
    dataAccess.getProcesses(true);
    dataAccess.getRuleSets(true);
    dataAccess.getActivityImplementors(true);
    project.findActivityImplementors(importedPackage);

    progressMonitor.progress(5);

    timer.stopAndLog();
    return importedPackage;
  }

  public WorkflowProcess importProcess(final WorkflowPackage targetPackage, final WorkflowProcess targetProcess, final String xml)
  throws DataAccessException, RemoteException, ActionCancelledException, XmlException, ValidationException
  {
    CodeTimer timer = new CodeTimer("importProcess()");
    ProcessVO importedProcessVO = null;

    int schemaVersion = dataAccess.getSchemaVersion();
    ProcessImporter importer = DataAccess.getProcessImporter(schemaVersion);
    importedProcessVO = importer.importProcess(xml);

    if (targetProcess != null && !importedProcessVO.getName().equals(targetProcess.getName()))
      throw new ValidationException("Process in XML (" + importedProcessVO.getName() + ") is not " + targetProcess.getName());

    for (WorkflowProcess existing : targetPackage.getProject().getAllProcesses())
    {
      if (existing.getName().equals(importedProcessVO.getName()))
      {
        if (existing.getVersion() == importedProcessVO.getVersion())
          throw new ValidationException(existing.getLabel() + " already exists in " + targetPackage.getProject().getLabel() + ".");
        if (existing.getVersion() > importedProcessVO.getVersion())
          throw new ValidationException(existing.getLabel() + " already exists in " + targetPackage.getProject().getLabel()
              + " with a version greater than the imported process (v" + importedProcessVO.getVersionString() + ").");
      }
    }

    // designer fix for backward compatibility
    ProcessWorker worker = new ProcessWorker();
    worker.convert_to_designer(importedProcessVO);
    worker.convert_from_designer(importedProcessVO, syncNodeMetaInfo(dataAccess.getDesignerDataModel().getNodeMetaInfo(), importedProcessVO));

    if (!importedProcessVO.isInRuleSet())
    {
      // fix pseudo variables
      for (VariableVO varVO : importedProcessVO.getVariables())
        varVO.setVariableId(null);
    }

    importedProcessVO.setPackageName(targetPackage.getName());
    WorkflowProcess alreadyInPackage = null;
    if (targetPackage.getProject().getProcess(importedProcessVO.getName()) == null)
      dataAccess.getDesignerDataAccess().createProcess(importedProcessVO);
    else
    {
      alreadyInPackage = targetPackage.getProcess(importedProcessVO.getName());
      dataAccess.getDesignerDataAccess().updateProcess(importedProcessVO, importedProcessVO.getVersion(), false);
    }

    ProcessVO reloaded = dataAccess.getDesignerDataAccess().getProcessDefinition(importedProcessVO.getProcessName(), importedProcessVO.getVersion());
    importedProcessVO.setProcessId(reloaded.getProcessId());
    if (targetPackage.getProject().getDataAccess().getSupportedSchemaVersion() < DataAccess.schemaVersion52)
      updateSubProcessIdAttributes(importedProcessVO);

    WorkflowProcess importedProcess = new WorkflowProcess(targetPackage.getProject(), importedProcessVO);
    dataAccess.getProcesses(false).add(importedProcess.getProcessVO());
    if (alreadyInPackage != null)
      targetPackage.removeProcess(alreadyInPackage);
    targetPackage.addProcess(importedProcess);
    importedProcess.setPackage(targetPackage);
    dataAccess.auditLog(Action.Import, importedProcess);

    timer.stopAndLog();
    return importedProcess;
  }

  private NodeMetaInfo syncNodeMetaInfo(NodeMetaInfo existingInfo, PackageVO importedPackageVO)
  {
    if (importedPackageVO.getImplementors() != null)
    {
      for (ActivityImplementorVO newActImpl : importedPackageVO.getImplementors())
      {
        if (newActImpl.getAttributeDescription() == null)
          newActImpl.setAttributeDescription("");  // so isLoaded() == true
        existingInfo.complement(newActImpl);
      }
    }
    return existingInfo;
  }

  private NodeMetaInfo syncNodeMetaInfo(NodeMetaInfo existingInfo, ProcessVO importedProcessVO)
  {
    if (importedProcessVO.getImplementors() != null)
    {
      for (ActivityImplementorVO newActImpl : importedProcessVO.getImplementors())
      {
        if (newActImpl.getAttributeDescription() == null)
          newActImpl.setAttributeDescription("");  // so isLoaded() == true
        existingInfo.complement(newActImpl);
      }
    }
    return existingInfo;
  }

  /**
   * Update processid attribute for calling processes within this package.
   * TODO: Get rid of this method, which is only needed for ancient (pre-4.5) runtimes.
   */
  private void updateSubProcessIdAttributes(ProcessVO processVO)
  throws DataAccessException, RemoteException, XmlException
  {
    // save calling processes to update subprocess activity attributes
    boolean toUpdate = false;
    if (processVO.getActivities() != null)
    {
      for (ActivityVO actVO : processVO.getActivities())
      {
        String procNameAttr = actVO.getAttribute("processname");
        String procVerAttr = actVO.getAttribute("processversion");
        if (procNameAttr != null && procVerAttr != null)
        {
          toUpdate = true;
        }
      }
      if (processVO.getSubProcesses() != null)
      {
        for (ProcessVO embedded : processVO.getSubProcesses())
        {
          for (ActivityVO actVO : embedded.getActivities())
          {
            String procNameAttr = actVO.getAttribute("processname");
            String procVerAttr = actVO.getAttribute("processversion");
            if (procNameAttr != null && procVerAttr != null)
            {
              toUpdate = true;
            }
          }
        }
      }
    }
    if (toUpdate)
    {
      ProcessVO procVO = dataAccess.getDesignerDataAccess().getProcess(processVO.getProcessId(), processVO);
      for (ActivityVO actVO : procVO.getActivities())
      {
        String procNameAttr = actVO.getAttribute("processname");
        String procVerAttr = actVO.getAttribute("processversion");
        if (procNameAttr != null && procVerAttr != null)
        {
          for (ProcessVO checkVO : importedPackageVO.getProcesses())
          {
            if (checkVO.getProcessName().equals(procNameAttr) && String.valueOf(checkVO.getVersion()).equals(procVerAttr))
              actVO.setAttribute("processid", checkVO.getProcessId().toString());
          }
        }
      }
      if (procVO.getSubProcesses() != null)
      {
        for (ProcessVO embedded : procVO.getSubProcesses())
        {
          for (ActivityVO actVO : embedded.getActivities())
          {
            String procNameAttr = actVO.getAttribute("processname");
            String procVerAttr = actVO.getAttribute("processversion");
            if (procNameAttr != null && procVerAttr != null)
            {
              for (ProcessVO checkVO : importedPackageVO.getProcesses())
              {
                if (checkVO.getProcessName().equals(procNameAttr) && String.valueOf(checkVO.getVersion()).equals(procVerAttr))
                  actVO.setAttribute("processid", checkVO.getProcessId().toString());
              }
            }
          }
        }
      }
      dataAccess.getDesignerDataAccess().updateProcess(procVO, 0, false);
    }
  }


  public PackageVO parsePackageContent(String packageContent) throws JSONException, DataAccessException
  {
    if (packageContent.trim().startsWith("{"))
    {
      Map<String,JSONObject> pkgJsonMap = JsonUtil.getJsonObjects(new JSONObject(packageContent).getJSONObject("packages"));
      String name = pkgJsonMap.keySet().iterator().next();
      PackageVO pkgVO = new PackageVO(pkgJsonMap.get(name));
      pkgVO.setName(name);
      return pkgVO;
    }
    else
    {
      int schemaVersion = dataAccess.getDesignerDataAccess().getDatabaseSchemaVersion();
      ProcessImporter importer = DataAccess.getProcessImporter(schemaVersion);
      return importer.importPackage(packageContent);
    }
  }

  public void importAttributes(final WorkflowElement element, final String xml, final ProgressMonitor progressMonitor, String attrPrefix)
  throws DataAccessException, RemoteException, ActionCancelledException, XmlException
  {
    progressMonitor.subTask("Parsing XML");

    int schemaVersion = dataAccess.getSchemaVersion();
    ProcessImporter importer = DataAccess.getProcessImporter(schemaVersion);
    progressMonitor.progress(40);

    progressMonitor.subTask("Saving attributes");

    ProcessDefinitionDocument procdef = ProcessDefinitionDocument.Factory.parse(xml, Compatibility.namespaceOptions());
    if (element instanceof WorkflowPackage && !((WorkflowPackage)element).getName().equals(procdef.getProcessDefinition().getPackageName()))
      throw new DataAccessException("Expected package: " + ((WorkflowPackage)element).getName() + " in attributes XML but found: " + procdef.getProcessDefinition().getPackageName());

    for (MDWProcess process : procdef.getProcessDefinition().getProcessList())
    {
      ProcessDefinitionDocument oneProcDef = ProcessDefinitionDocument.Factory.newInstance();
      MDWProcessDefinition oneProc = oneProcDef.addNewProcessDefinition();
      oneProc.getProcessList().add(process);
      ProcessVO importedProc = importer.importProcess(oneProcDef.xmlText());
      if (element instanceof WorkflowProcess && !((WorkflowProcess)element).getName().equals(importedProc.getProcessName()))
        throw new DataAccessException("Expected process: " + ((WorkflowProcess)element).getName() + " in attributes XML but found: " + importedProc.getName());
      ProcessVO proc = dataAccess.getLatestProcess(importedProc.getName());
      if (proc == null)
        throw new DataAccessException("Process not found: " + importedProc.getName());
      Map<String,String> existAttrs = dataAccess.getDesignerDataAccess().getAttributes(OwnerType.PROCESS, proc.getId());
      Map<String,String> importedAttrs = importedProc.getOverrideAttributes();
      Map<String,String> setAttrs = new HashMap<String,String>();
      if (existAttrs != null)
      {
        // retain existing attrs not related to this prefix
        for (String existName : existAttrs.keySet())
        {
          if (!WorkAttributeConstant.isAttrNameFor(existName, attrPrefix))
            setAttrs.put(existName, existAttrs.get(existName));
        }
      }
      if (importedAttrs != null)
      {
        for (String name : importedAttrs.keySet())
        {
          if (!WorkAttributeConstant.isAttrNameFor(name, attrPrefix))
            throw new DataAccessException("Expected attribute prefix: " + attrPrefix + " in attributes XML but found attribute: " + name);
          else
            setAttrs.put(name, importedAttrs.get(name));
        }
      }
      dataAccess.getDesignerDataAccess().setOverrideAttributes(attrPrefix, OwnerType.PROCESS, proc.getId(), setAttrs);
    }
  }

  public void importTaskTemplates(final WorkflowPackage pkg, final String xml, final ProgressMonitor progressMonitor) throws XmlException, RemoteException, DataAccessException
  {
    progressMonitor.subTask("Parsing XML");

    PackageDocument pkgDoc = PackageDocument.Factory.parse(xml);
    MDWPackage packageDef = pkgDoc.getPackage();
    if (!pkg.getName().equals(packageDef.getName()))
      throw new DataAccessException("Expected package: " + pkg.getName() + " in tasks XML but found: " + packageDef.getName());

    com.centurylink.mdw.task.TaskTemplatesDocument.TaskTemplates templates = packageDef.getTaskTemplates();
    PackageVO packageVO = new PackageVO();
    if (packageDef.getName() != null)
        packageVO.setPackageName(packageDef.getName());
    else packageVO.setPackageName("package");
    packageVO.setVersion(PackageVO.parseVersion(packageDef.getVersion()));
    List<TaskVO> packageTaskTemplates = new ArrayList<TaskVO>();
    for (com.centurylink.mdw.task.TaskTemplate template : templates.getTaskList())
    {
      TaskVO taskTemplateVO = new TaskVO(template);
      taskTemplateVO.setPackageName(packageVO.getPackageName());
      String v = template.getVersion();
      if (v != null && !v.equals("0"))
        taskTemplateVO.setVersion(RuleSetVO.parseVersion(v));
      String assetName = template.getAssetName();
      if (assetName != null && !assetName.isEmpty())
        taskTemplateVO.setName(assetName);
      packageTaskTemplates.add(taskTemplateVO);
    }
    packageVO.setTaskTemplates(packageTaskTemplates);
    if (!packageTaskTemplates.isEmpty())
        dataAccess.getDesignerDataAccess().savePackage(packageVO, ProcessPersister.PersistType.IMPORT);
    for (TaskVO taskVo : packageVO.getTaskTemplates())
    {
      TaskTemplate existing = null;
      for (TaskTemplate taskTemplate : pkg.getTaskTemplates())
      {
        if (taskTemplate.getLogicalId().equals(taskVo.getLogicalId()))
        {
          existing = taskTemplate;
          break;
        }
      }
      if (existing == null) {
        TaskTemplate newTemplate = new TaskTemplate(taskVo, pkg.getPackage());
        pkg.addTaskTemplate(newTemplate);
      }
      else {
        existing.setTaskVO(taskVo);
      }
    }
  }

}
