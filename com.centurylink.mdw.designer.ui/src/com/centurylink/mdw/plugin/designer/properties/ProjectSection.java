/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.internal.dialogs.PropertyDialog;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.assembly.ExtensionModulesUpdater;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.JdbcDataSource;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

@SuppressWarnings("restriction")
public class ProjectSection extends PropertySection implements ElementChangeListener
{
  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }

  private MdwSettings mdwSettings;

  private PropertyEditor sourceProjectEditor;
  private PropertyEditor jdbcUrlEditor;
  private PropertyEditor hostEditor;
  private PropertyEditor portEditor;
  private PropertyEditor webContextRootEditor;
  private PropertyEditor updateServerCacheEditor;
  private PropertyEditor localProjectEditor;
  private PropertyEditor localProjectInfoEditor;
  private PropertyEditor mdwVersionEditor;
  private PropertyEditor appVersionEditor;

  public void setSelection(WorkflowElement selection)
  {
    if (project != null)
      project.removeElementChangeListener(this);

    project = (WorkflowProject) selection;
    project.addElementChangeListener(this);

    // dispose controls to render dynamically
    if (sourceProjectEditor != null)
      sourceProjectEditor.dispose();
    if (jdbcUrlEditor != null)
      jdbcUrlEditor.dispose();
    if (hostEditor != null)
      hostEditor.dispose();
    if (portEditor != null)
      portEditor.dispose();
    if (webContextRootEditor != null)
      webContextRootEditor.dispose();
    if (updateServerCacheEditor != null)
      updateServerCacheEditor.dispose();
    if (localProjectEditor != null)
      localProjectEditor.dispose();
    if (localProjectInfoEditor != null)
      localProjectInfoEditor.dispose();
    if (mdwVersionEditor != null)
      mdwVersionEditor.dispose();
    if (appVersionEditor != null)
      appVersionEditor.dispose();

    // source project text field
    sourceProjectEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    sourceProjectEditor.setLabel("Source Project");
    sourceProjectEditor.setWidth(200);
    sourceProjectEditor.render(composite);
    sourceProjectEditor.setValue(project.getSourceProjectName());
    sourceProjectEditor.setEditable(false);

    // jdbc url text field
    jdbcUrlEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    jdbcUrlEditor.setLabel("JDBC URL");
    jdbcUrlEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          project.getMdwDataSource().setJdbcUrlWithCredentials(((String)newValue).trim());
          project.getMdwDataSource().setEntrySource("projectSection");
          WorkflowProjectManager.updateProject(project);
          project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getMdwDataSource());
        }
      });
    jdbcUrlEditor.render(composite);
    jdbcUrlEditor.setValue(project.getMdwDataSource().getJdbcUrlWithMaskedCredentials());
    jdbcUrlEditor.setEditable(!project.isReadOnly());

    // host text field
    hostEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    hostEditor.setLabel("Server Host");
    hostEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        project.getServerSettings().setHost((String)newValue);
        WorkflowProjectManager.updateProject(project);
        project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getServerSettings());
      }
    });
    hostEditor.render(composite);
    hostEditor.setValue(project.getServerSettings().getHost());
    hostEditor.setEditable(!project.isReadOnly());

    // port text field
    portEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    portEditor.setLabel("Server Port");
    portEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        project.getServerSettings().setPort(Integer.parseInt(((String)newValue).trim()));
        WorkflowProjectManager.updateProject(project);

        project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getServerSettings());
      }
    });
    portEditor.render(composite);
    portEditor.setValue(project.getServerSettings().getPort());
    portEditor.setEditable(!project.isReadOnly());

    // web context root text field
    webContextRootEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    webContextRootEditor.setLabel("Web Context Root");
    webContextRootEditor.setWidth(200);
    if (project.isRemote())
    {
      webContextRootEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          project.setWebContextRoot(((String)newValue).trim());
          WorkflowProjectManager.updateProject(project);
          project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.getServerSettings());
        }
      });
    }
    webContextRootEditor.render(composite);
    webContextRootEditor.setValue(project.getWebContextRoot());
    webContextRootEditor.setEditable(!project.isReadOnly() && project.isRemote());

    // refresh server cache checkbox
    updateServerCacheEditor = new PropertyEditor(project, PropertyEditor.TYPE_CHECKBOX);
    updateServerCacheEditor.setLabel("Update Server Cache");
    updateServerCacheEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        project.setUpdateServerCache(Boolean.parseBoolean(newValue.toString()));
        WorkflowProjectManager.updateProject(project);
        project.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, project.isUpdateServerCache());
      }
    });
    updateServerCacheEditor.render(composite);
    updateServerCacheEditor.setValue(project.isUpdateServerCache());
    updateServerCacheEditor.setEditable(!project.isReadOnly());

    // mdw version combo
    if (project.isRemote())
    {
      mdwVersionEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
    }
    else
    {
      mdwVersionEditor = new PropertyEditor(project, PropertyEditor.TYPE_COMBO);
      List<String> versionOptions = mdwSettings.getMdwVersions();
      if (!versionOptions.contains(project.getMdwVersion()))
        versionOptions.add(project.getMdwVersion());
      mdwVersionEditor.setValueOptions(versionOptions);
      mdwVersionEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          if (!project.isRemote() && !newValue.equals(""))
          {
            project.setMdwVersion((String)newValue);
            WorkflowProjectManager.updateProject(project);
            if (MessageDialog.openQuestion(getShell(), "Update Framework Libraries", "Download updated framework libraries to match MDW Version selection?"))
            {
              ProjectUpdater updater = new ProjectUpdater(getProject(), MdwPlugin.getSettings());
              try
              {
                updater.updateFrameworkJars(null);
                ExtensionModulesUpdater modulesUpdater = new ExtensionModulesUpdater(getProject());
                modulesUpdater.doUpdate(getShell());
              }
              catch (Exception ex)
              {
                PluginMessages.uiError(getShell(), ex, "Update Framework Libraries", getProject());
              }
            }
            if (getProject().isOsgi())
              MessageDialog.openInformation(getShell(), "MDW Version Changed", "The MDW version has been updated in the plug-in settings file.  Please update any MDW dependencies in your pom.xml build file.");
          }
        }
      });
    }
    mdwVersionEditor.setLabel("MDW Version");
    mdwVersionEditor.setWidth(100);
    mdwVersionEditor.render(composite);
    mdwVersionEditor.setValue(project.getMdwVersion());
    mdwVersionEditor.setEditable(!project.isReadOnly() && !project.isRemote());

    // app version
    if (!project.isCloudProject() && !"Unknown".equals(project.getAppVersion()))
    {
      appVersionEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
      appVersionEditor.setLabel("App Version");
      appVersionEditor.setWidth(200);
      appVersionEditor.render(composite);
      appVersionEditor.setValue(project.getAppVersion());
      appVersionEditor.setEditable(false);
    }

    if (!project.isRemote())
    {
      // local project text field
      localProjectEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
      localProjectEditor.setLabel("Workspace Project");
      localProjectEditor.setWidth(200);
      localProjectEditor.setReadOnly(true);
      localProjectEditor.render(composite);
      if (project.isCloudProject())
        localProjectEditor.setValue(project.getSourceProjectName());
      else
        localProjectEditor.setValue(project.getEarProjectName());

      if (!project.isOsgi())
      {
        // local project info field
        localProjectInfoEditor = new PropertyEditor(project, PropertyEditor.TYPE_LINK);
        localProjectInfoEditor.setLabel("Workspace Project Settings");
        localProjectInfoEditor.addValueChangeListener(new ValueChangeListener()
        {
          public void propertyValueChanged(Object newValue)
          {
            final IProject proj = project.isCloudProject() ? project.getSourceProject() : project.getEarProject();
            PropertyDialog dialog = PropertyDialog.createDialogOn(getShell(), "mdw.workflow.mdwServerConnectionsPropertyPage", proj);
            if (dialog != null)
              dialog.open();
          }
        });
        localProjectInfoEditor.render(composite);
      }
    }

    composite.layout(true);
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    project = (WorkflowProject) selection;

    mdwSettings = MdwPlugin.getSettings();

    // controls are rendered dynamically in setSelection()
  }

  public void elementChanged(ElementChangeEvent ece)
  {
    if (ece.getChangeType().equals(ChangeType.SETTINGS_CHANGE))
    {
      if (ece.getNewValue() instanceof JdbcDataSource)
      {
        JdbcDataSource dataSource = (JdbcDataSource) ece.getNewValue();
        if (!"projectSection".equals(dataSource.getEntrySource()))  // avoid overwriting
        {
          String newJdbcUrl = dataSource.getJdbcUrlWithMaskedCredentials();
          jdbcUrlEditor.setValue(newJdbcUrl);
        }
      }
      else if (ece.getNewValue() instanceof ServerSettings)
      {
        ServerSettings serverSettings = (ServerSettings) ece.getNewValue();
        if (!hostEditor.getValue().equals(serverSettings.getHost()))
          hostEditor.setValue(serverSettings.getHost());
        if (!portEditor.getValue().equals(String.valueOf(serverSettings.getPort())))
          portEditor.setValue(serverSettings.getPort());
      }
      else if (ece.getNewValue() instanceof String)
      {
        if (!webContextRootEditor.getValue().equals(ece.getNewValue()))
          webContextRootEditor.setValue(ece.getNewValue().toString());
      }
      else if (ece.getNewValue() instanceof Boolean)
      {
        if (!updateServerCacheEditor.getValue().equalsIgnoreCase(ece.getNewValue().toString()))
          updateServerCacheEditor.setValue(ece.getNewValue().toString());
      }
    }
    else if (ece.getChangeType().equals(ChangeType.VERSION_CHANGE)
        && project.equals(ece.getElement()))
    {
      if (!mdwVersionEditor.getValue().equals(ece.getNewValue()))
        mdwVersionEditor.setValue(ece.getNewValue().toString());
    }
  }

  @Override
  public void dispose()
  {
    super.dispose();
    if (project != null)
      project.removeElementChangeListener(this);
  }
}
