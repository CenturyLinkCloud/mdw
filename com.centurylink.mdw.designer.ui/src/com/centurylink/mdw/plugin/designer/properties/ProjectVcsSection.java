/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.SwitchButton;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

public class ProjectVcsSection extends PropertySection implements IFilter, ElementChangeListener
{
  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }

  private PropertyEditor gitRepoUrlEditor;
  private PropertyEditor gitBranchEditor;
  private PropertyEditor assetLocalPathEditor;
  private PropertyEditor gitSyncEditor;
  private PropertyEditor includeArchiveEditor;

  public void setSelection(WorkflowElement selection)
  {
    if (project != null)
      project.removeElementChangeListener(this);

    project = (WorkflowProject) selection;
    project.addElementChangeListener(this);

    // dispose controls to render dynamically
    if (gitRepoUrlEditor != null)
      gitRepoUrlEditor.dispose();
    if (gitBranchEditor != null)
      gitBranchEditor.dispose();
    if (assetLocalPathEditor != null)
      assetLocalPathEditor.dispose();
    if (gitSyncEditor != null)
      gitSyncEditor.dispose();
    if (includeArchiveEditor != null)
      includeArchiveEditor.dispose();

    // repository url text field
    gitRepoUrlEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    gitRepoUrlEditor.setLabel("Git Repository URL");
    gitRepoUrlEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        project.getMdwVcsRepository().setRepositoryUrlWithCredentials(((String)newValue).trim());
        project.getMdwVcsRepository().setEntrySource("projectSection");
        WorkflowProjectManager.updateProject(project);
        project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getMdwVcsRepository());
      }
    });
    gitRepoUrlEditor.render(composite);
    gitRepoUrlEditor.setValue(project.getMdwVcsRepository().getRepositoryUrlWithMaskedCredentials());
    gitRepoUrlEditor.setEditable(!project.isReadOnly());

    // git branch text field
    gitBranchEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    gitBranchEditor.setLabel("Git Branch");
    gitBranchEditor.setWidth(200);
    gitBranchEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        project.getMdwVcsRepository().setBranch(((String)newValue).trim());
        project.getMdwVcsRepository().setEntrySource("projectSection");
        WorkflowProjectManager.updateProject(project);
        project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getMdwVcsRepository());
      }
    });
    gitBranchEditor.render(composite);
    gitBranchEditor.setValue(project.getMdwVcsRepository().getBranch());
    gitBranchEditor.setEditable(!project.isReadOnly());

    // asset local path text field
    assetLocalPathEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    assetLocalPathEditor.setLabel("Asset Local Path");
    assetLocalPathEditor.setWidth(200);
    assetLocalPathEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        project.getMdwVcsRepository().setLocalPath(((String)newValue).trim());
        project.getMdwVcsRepository().setEntrySource("projectSection");
        WorkflowProjectManager.updateProject(project);
        project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getMdwVcsRepository());
      }
    });
    assetLocalPathEditor.render(composite);
    assetLocalPathEditor.setValue(project.getMdwVcsRepository().getLocalPath());
    assetLocalPathEditor.setEditable(!project.isReadOnly());

    if (project.isRemote())
    {
      if (project.isGitVcs())
      {
        // for git: sync switch
        gitSyncEditor = new PropertyEditor(project, PropertyEditor.TYPE_SWITCH);
        gitSyncEditor.setLabel("");
        //gitSyncEditor.setComment("(Unlock to enable asset editing)");
        gitSyncEditor.addValueChangeListener(new ValueChangeListener()
        {
          public void propertyValueChanged(Object newValue)
          {
            boolean unlocked = !Boolean.parseBoolean(newValue.toString());
            if (unlocked)
            {
              WorkflowProjectManager.getInstance().makeLocal(project);
              project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getMdwVcsRepository());
              MessageDialog.openInformation(getShell(), "Remote Project Unlocked", project.getName() + " has been unlocked.  Please close any open assets and refresh.");
            }
          }
        });
        gitSyncEditor.render(composite);
        SwitchButton switchBtn = (SwitchButton) gitSyncEditor.getWidget();
        switchBtn.setTextForSelect("Unlocked");
        switchBtn.setTextForUnselect("Synced");
        gitSyncEditor.setValue(true);
      }
      // include archive checkbox
      includeArchiveEditor = new PropertyEditor(project, PropertyEditor.TYPE_CHECKBOX);
      includeArchiveEditor.setLabel("Sync Asset Archive");
      includeArchiveEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          project.getMdwVcsRepository().setSyncAssetArchive(Boolean.parseBoolean(newValue.toString()));
          WorkflowProjectManager.updateProject(project);
          project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getMdwVcsRepository());
        }
      });
      includeArchiveEditor.render(composite);
      includeArchiveEditor.setValue(project.getMdwVcsRepository().isSyncAssetArchive());
      includeArchiveEditor.setEditable(!project.isReadOnly());
    }

    composite.layout(true);
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    project = (WorkflowProject) selection;
    // controls are rendered dynamically in setSelection()
  }

  public void elementChanged(ElementChangeEvent ece)
  {
    if (ece.getChangeType().equals(ChangeType.SETTINGS_CHANGE))
    {
      if (ece.getNewValue() instanceof VcsRepository)
      {
        VcsRepository repository = (VcsRepository) ece.getNewValue();
        if (!"projectSection".equals(repository.getEntrySource()))  // avoid overwriting
        {
          String newRepositoryUrl = repository.getRepositoryUrlWithMaskedCredentials();
          if (!gitRepoUrlEditor.getValue().equals(newRepositoryUrl))
            gitRepoUrlEditor.setValue(newRepositoryUrl);
          String newGitBranchValue = repository.getBranch();
          if (!gitBranchEditor.getValue().equals(newGitBranchValue))
            gitBranchEditor.setValue(newGitBranchValue);
          String newLocalPath = repository.getLocalPath();
          if (!assetLocalPathEditor.getValue().equals(newLocalPath))
            assetLocalPathEditor.setValue(newLocalPath);
          boolean newSyncAssets = repository.isSyncAssetArchive();
          if (includeArchiveEditor != null && !includeArchiveEditor.getValue().equals(String.valueOf(newSyncAssets)))
            includeArchiveEditor.setValue(newSyncAssets);
        }
      }
    }
  }

  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof WorkflowProject))
      return false;

    WorkflowProject workflowProject = (WorkflowProject) toTest;
    return workflowProject.getPersistType() == PersistType.Git;
  }

  @Override
  public void dispose()
  {
    super.dispose();
    if (project != null)
      project.removeElementChangeListener(this);
  }
}
