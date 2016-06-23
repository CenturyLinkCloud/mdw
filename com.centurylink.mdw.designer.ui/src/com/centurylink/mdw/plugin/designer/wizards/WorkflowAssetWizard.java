/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.DocumentTemplate;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public abstract class WorkflowAssetWizard extends Wizard implements INewWizard
{
  protected static final String BLANK_TEMPLATE = "Blank";

  private IWorkbench workbench;
  public IWorkbench getWorkbench() { return workbench; }

  private WorkflowAsset workflowAsset;
  public WorkflowAsset getWorkflowAsset() { return workflowAsset; }
  public void setWorkflowAsset(WorkflowAsset asset) { this.workflowAsset = asset; }

  private WorkflowAssetPage workflowAssetPage;
  public WorkflowAssetPage getWorkflowAssetPage() { return workflowAssetPage; }

  private boolean importFile;
  public boolean isImportFile() { return importFile; }
  public void setImportFile(boolean b) { this.importFile = b; }

  private String importFilePath;
  public String getImportFilePath() { return importFilePath; }
  public void setImportFilePath(String s) { this.importFilePath = s; }

  private String templateName;
  public String getTemplateName() { return templateName; }
  public void setTemplateName(String s) { this.templateName = s; }

  public void init(IWorkbench workbench, IStructuredSelection selection, WorkflowAsset asset)
  {
    setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
    setNeedsProgressMonitor(true);

    this.workflowAsset = asset;
    if (selection != null && selection.getFirstElement() instanceof WorkflowPackage)
    {
      WorkflowPackage packageVersion = (WorkflowPackage) selection.getFirstElement();
      workflowAsset.setPackage(packageVersion);
    }
    else if (selection != null && selection.getFirstElement() instanceof WorkflowProject)
    {
      WorkflowProject workflowProject = (WorkflowProject) selection.getFirstElement();
      if (workflowProject.isShowDefaultPackage())
        workflowAsset.setPackage(workflowProject.getDefaultPackage());
    }
    else if (selection != null && selection.getFirstElement() instanceof WorkflowElement)
    {
      WorkflowElement element = (WorkflowElement)selection.getFirstElement();
      workflowAsset.setPackage(element.getPackage());
    }
    else
    {
      WorkflowProject workflowProject = WorkflowProjectManager.getInstance().findWorkflowProject(selection);
      if (workflowProject != null)
        workflowAsset.setProject(workflowProject);
    }

    workflowAssetPage = new WorkflowAssetPage(workflowAsset);
  }

  @Override
  public void addPages()
  {
    addPage(workflowAssetPage);
  }

  @Override
  public boolean performFinish()
  {
    if (!workflowAsset.getProject().checkRequiredVersion(5, 0))
      workflowAsset.setPackage(workflowAsset.getProject().getDefaultPackage());

    if (isImportFile())
    {
      try
      {
        // load from selected file
        File file = new File(importFilePath);
        byte[] fileBytes = PluginUtil.readFile(file);

        if (workflowAsset.isBinary())
          workflowAsset.encodeAndSetContent(fileBytes);
        else
          workflowAsset.setContent(new String(fileBytes));
      }
      catch (IOException ex)
      {
        PluginMessages.uiError(getShell(), ex, "Create " + workflowAsset.getTitle(), workflowAsset.getProject());
      }
    }
    else
    {
      try
      {
        DocumentTemplate docTemplate = getNewDocTemplate();
        if (docTemplate != null)
        {
          byte[] templateContents = docTemplate.getContent();
          if (workflowAsset.isBinary())
            workflowAsset.encodeAndSetContent(templateContents);
          else if (workflowAsset.getLanguage().equals(RuleSetVO.FACELET) || workflowAsset.getLanguage().equals(RuleSetVO.HTML))
            workflowAsset.setContent(new String(templateContents));
          else
            workflowAsset.substituteAndSetContent(new String(templateContents));
        }
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(getShell(), ex, "Create " + workflowAsset.getTitle(), workflowAsset.getProject());
      }
    }

    DesignerProxy designerProxy = workflowAsset.getProject().getDesignerProxy();
    try
    {
      designerProxy.createNewWorkflowAsset(workflowAsset, true);
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(getShell(), ex, "New " + workflowAsset.getTitle(), workflowAsset.getProject());
      return false;
    }

    if (designerProxy.getRunnerStatus().equals(RunnerStatus.SUCCESS))
    {
      if (!workflowAsset.isBinary() || getNewDocTemplate() != null)
        workflowAsset.openFile(new NullProgressMonitor());

      workflowAsset.addElementChangeListener(workflowAsset.getProject());
      workflowAsset.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, workflowAsset);

      WorkflowAssetFactory.registerAsset(workflowAsset);

      DesignerPerspective.promptForShowPerspective(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), workflowAsset);

      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * Override to present a list of template options.  Default behavior is to list
   * the files discovered via getTemplateLocation().
   */
  public List<String> getTemplateOptions()
  {
    String templateLoc = getTemplateLocation();
    if (templateLoc == null)
    {
      return null;
    }
    else
    {
      List<String> templateOptions = new ArrayList<String>();
      try
      {
        URL templateDirUrl = PluginUtil.getLocalResourceUrl(templateLoc);
        File templateDir = new File(templateDirUrl.toURI());
        for (File templateFile : templateDir.listFiles())
        {
          if (templateFile.isFile())
          {
            String fileName = templateFile.getName();
            if (!templateFile.getName().startsWith("."))
            {
              int dot = fileName.lastIndexOf('.');
              if (dot > 0)
                fileName = fileName.substring(0, dot);
              templateOptions.add(fileName);
            }
          }
        }
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(getShell(), ex, "Load Templates", getWorkflowAsset().getProject());
      }
      return templateOptions;
    }
  }

  public String getTemplateLocation()
  {
    return null;
  }

  /**
   * Override to specify a new document template.
   */
  public DocumentTemplate getNewDocTemplate()
  {
    if (getTemplateLocation() == null)
    {
      return null;
    }
    else
    {
      String name = getTemplateName().replaceAll(" ", "");
      return new DocumentTemplate(name, getWorkflowAsset().getExtension(), getTemplateLocation());
    }
  }
}