/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AssetLocator
{
  public enum Type {
    Process,
    TaskTemplate,
    Asset
  }

  private WorkflowElement element; // the attribute owner
  public WorkflowElement getElement() { return element; }

  public WorkflowProject getProject()
  {
    return element.getProject();
  }

  private Type assetType;

  public boolean isProcess() { return assetType == Type.Process; }
  public boolean isTaskTemplate() { return assetType == Type.TaskTemplate; }

  public AssetLocator(WorkflowElement workflowElement, Type assetType)
  {
    this.element = workflowElement;
    this.assetType = assetType;
  }

  public WorkflowElement assetFromAttr(String attrValue)
  {
    if (attrValue == null || attrValue.isEmpty() || attrValue.startsWith("$") || attrValue.startsWith("#"))
    {
      return null;
    }

    if (isProcess())
    {
      int slashIdx = attrValue.indexOf('/');
      if (slashIdx <= 0)
      {
        // prefer process in same package
        WorkflowProcess proc = getElement().getPackage() == null ? null : getProcessVersion(attrValue, getElement().getPackage().getProcesses());
        if (proc != null)
          return proc;
        else
          return getProcessVersion(attrValue, getProject().getAllProcessVersions());
      }
      else
      {
        String pkgName = attrValue.substring(0, slashIdx);
        WorkflowPackage pkg = getProject().getPackage(pkgName);
        if (pkg == null)
          return null;
        else
          return getProcessVersion(attrValue.substring(slashIdx + 1), pkg.getProcesses());
      }
    }
    else if (isTaskTemplate())
    {
      // no need to handle unqualified attrs
      int slashIdx = attrValue.indexOf('/');
      String pkgName = slashIdx <= 0 ? null : attrValue.substring(0, slashIdx);
      WorkflowPackage pkg = pkgName == null ? null : getProject().getPackage(pkgName);
      if (pkg == null)
        return null;
      else
        return getTaskTemplateVersion(attrValue.substring(slashIdx + 1), pkg.getTaskTemplates());
    }
    else
    {
      int slashIdx = attrValue.indexOf('/');
      if (slashIdx <= 0)
      {
        // prefer asset in same package
        WorkflowAsset pkgDoc = getElement().getPackage() == null ? null : getWorkflowAssetVersion(attrValue, getElement().getPackage().getAssets());
        if (pkgDoc != null)
          return pkgDoc;
        else
          return getWorkflowAssetVersion(attrValue, getProject().getAllWorkflowAssets());
      }
      else
      {
        String pkgName = attrValue.substring(0, slashIdx);
        WorkflowPackage pkg = getProject().getPackage(pkgName);
        if (pkg == null)
          return null;
        else
          return getWorkflowAssetVersion(attrValue.substring(slashIdx + 1), pkg.getAssets());
      }
    }
  }

  public String attrFromAsset(WorkflowElement asset)
  {
    boolean includePackage = true;
    if (isProcess() && !getProject().checkRequiredVersion(5, 5))
      includePackage = false;

    if (asset.isInDefaultPackage() || !includePackage)
      return asset.getName();
    else
      return asset.getPackage().getName() + "/" + asset.getName();
  }

  public WorkflowElement getAssetVersion(String text, WorkflowPackage pkg)
  {
    if (isProcess())
      return getProcessVersion(text, pkg.getProcesses());
    else if (isTaskTemplate())
      return getTaskTemplateVersion(text, pkg.getTaskTemplates());
    else
      return getWorkflowAssetVersion(text, pkg.getAssets());
  }

  private WorkflowAsset getWorkflowAssetVersion(String versionSpec, List<WorkflowAsset> assets)
  {
    AssetVersionSpec spec = AssetVersionSpec.parse(versionSpec);
    boolean isDefaultSmart = isDefaultSmartFormat(spec.getVersion());
    WorkflowAsset match = null;
    for (WorkflowAsset asset : assets)
    {
      if (asset.getName().matches(spec.getName()))
      {
        if (asset.getVersionString().equals(spec.getVersion()))
        {
          match = asset;
          break;
        }
        else if ((AssetVersionSpec.VERSION_LATEST.equals(spec.getVersion()) || (isDefaultSmart && asset.meetsVersionSpec(spec.getVersion())))
            && (match == null || asset.getVersion() > match.getVersion()))
        {
          match = asset;
        }
      }
    }
    return match;
  }

  private TaskTemplate getTaskTemplateVersion(String versionSpec, List<TaskTemplate> taskTemplates)
  {
    AssetVersionSpec spec = AssetVersionSpec.parse(versionSpec);
    boolean isDefaultSmart = isDefaultSmartFormat(spec.getVersion());
    TaskTemplate match = null;
    for (TaskTemplate taskTemplate : taskTemplates)
    {
      if (taskTemplate.getName().matches(spec.getName()))
      {
        if (taskTemplate.getVersionString().equals(spec.getVersion()))
        {
          match = taskTemplate;
          break;
        }
        else if ((AssetVersionSpec.VERSION_LATEST.equals(spec.getVersion()) || (isDefaultSmart && taskTemplate.meetsVersionSpec(spec.getVersion())))
            && (match == null || taskTemplate.getVersion() > match.getVersion()))
        {
          match = taskTemplate;
        }
      }
    }
    return match;
  }

  public WorkflowProcess getProcessVersion(AssetVersionSpec spec)
  {
    if (spec.getPackageName() != null)
      return getProcessVersion(spec.getName() + " v" + spec.getVersion(), getProject().getAllProcessesByPackage(spec.getPackageName()));
    return getProcessVersion(spec.getName() + " v" + spec.getVersion(), getProject().getAllProcesses());
  }

  public TaskTemplate getTaskTemplateVersion(AssetVersionSpec spec)
  {
    WorkflowPackage pkg = getProject().getPackage(spec.getPackageName());
    return getTaskTemplateVersion(spec.getName() + " v" + spec.getVersion(), pkg.getTaskTemplates());
  }

  public WorkflowAsset getWorkflowAssetVersion(AssetVersionSpec spec)
  {
    return getWorkflowAssetVersion(spec.getName() + " v" + spec.getVersion(), getProject().getAllWorkflowAssets());
  }

  private WorkflowProcess getProcessVersion(String versionSpec, List<WorkflowProcess> processes)
  {
    AssetVersionSpec spec = AssetVersionSpec.parse(versionSpec);
    boolean isDefaultSmart = isDefaultSmartFormat(spec.getVersion());
    WorkflowProcess match = null;
    for (WorkflowProcess process : processes)
    {
      if (process.getName().equals(spec.getName()))
      {
        if (process.getVersionString().equals(spec.getVersion()))
        {
          match = process;
          break;
        }
        else if ((AssetVersionSpec.VERSION_LATEST.equals(spec.getVersion()) || (isDefaultSmart && process.meetsVersionSpec(spec.getVersion())))
            && (match == null || process.getVersion() > match.getVersion()))
        {
          match = process;
        }
      }
    }
    return match;
  }

  private boolean isDefaultSmartFormat(String versionSpec)
  {
    return versionSpec.startsWith("[") && versionSpec.indexOf(",") > 0 && versionSpec.endsWith(")");
  }

  public void openAsset(AssetVersionSpec spec)
  {
    IWorkbenchPage page = MdwPlugin.getActivePage();
    try
    {
      if (isProcess())
      {
        page.openEditor(getProcessVersion(spec), "mdw.editors.process");
      }
      else
      {
        getWorkflowAssetVersion(spec).openFile(new NullProgressMonitor());
      }
    }
    catch (PartInitException ex)
    {
      PluginMessages.uiError(ex, "Open Asset", getProject());
    }
  }

}
