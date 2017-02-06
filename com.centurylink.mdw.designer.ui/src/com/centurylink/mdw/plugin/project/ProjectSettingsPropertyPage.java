/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.properties.editor.SwitchButton;
import com.centurylink.mdw.plugin.project.assembly.ExtensionModulesUpdater;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

/**
 * Property page for workflow project settings.
 */
public class ProjectSettingsPropertyPage extends ProjectPropertyPage
{
  private Text sourceProjectTextField;
  private Text webProjectTextField;
  private Combo mdwVersionComboBox;
  private Text databaseJdbcUrlTextField;
  private Text gitRepositoryUrlTextField;
  private Text gitBranchTextField;
  private Text assetLocalPathTextField;
  private SwitchButton syncSwitch;
  private Button includeArchiveCheckbox;
  private Text filesToIgnoreTextField;
  private Text hostTextField;
  private Text portTextField;
  private Text contextRootTextField;
  private Button updateServerCacheCheckbox;

  private String originalJdbcUrl;
  private String originalGitRepositoryUrl;
  private String originalGitBranch;
  private String originalAssetLocalPath;
  private String originalSchemaOwner;
  private String originalMdwVersion;
  private String originalFilesToIgnore;
  private String originalHost;
  private int originalPort;
  private String originalContextRoot;
  private boolean originalSync;
  private boolean originalIncludeArchive;
  private boolean originalRefreshServerCache;

  @Override
  protected Control createContents(Composite parent)
  {
    noDefaultAndApplyButton();
    initializeWorkflowProject();

    Composite composite = createComposite(parent);

    createRelatedProjectControls(composite);
    addSeparator(composite);
    createMdwVersionControls(composite);
    addSeparator(composite);
    if (getProject().getPersistType() == PersistType.Git)
    {
      createGitRepositoryControls(composite);
    }

    if (getProject().getPersistType() == PersistType.None)
    {
      // do nothing
    }
    else
    {
      createJdbcUrlControls(composite);
    }
    addSeparator(composite);
    if (getProject().isRemote())
      createHostPortContextRootControls(composite);
    else
      createUpdateSettingsControls(composite);

    return composite;
  }

  private void createRelatedProjectControls(Composite parent)
  {
    addHeading(parent, "Project");

    Composite composite = createComposite(parent, 2);

    new Label(composite, SWT.NONE).setText("Source Project:");
    String sourceProject = getProject().getSourceProjectName();
    int style = (sourceProject == null || sourceProject.length() ==0) ? SWT.SINGLE| SWT.BORDER : SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY;
    sourceProjectTextField = new Text(composite, style);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 175;
    sourceProjectTextField.setLayoutData(gd);
    sourceProjectTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getProject().setSourceProjectName(sourceProjectTextField.getText().trim());
      }
    });
    sourceProjectTextField.setText(sourceProject);

    if (getProject().isCustomTaskManager())
    {
      new Label(composite, SWT.NONE).setText("Web Project:");

      webProjectTextField = new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
      gd = new GridData(GridData.BEGINNING);
      gd.widthHint = 175;
      webProjectTextField.setLayoutData(gd);
      webProjectTextField.setText(getProject().getWebProjectName());
    }
  }

  private void createMdwVersionControls(Composite parent)
  {
    addHeading(parent, "Version");

    Composite composite = createComposite(parent, 2);

    // mdw version
    new Label(composite, SWT.NONE).setText("MDW Version:");
    if (getProject().isRemote())
    {
      String ver = getProject().getMdwVersion();
      new Label(composite, SWT.NONE).setText(ver == null ? "" : ver);
    }
    else
    {
      mdwVersionComboBox = new Combo(composite, SWT.DROP_DOWN);
      mdwVersionComboBox.removeAll();
      List<String> mdwVersions = MdwPlugin.getSettings().getMdwVersions();
      for (int i = 0; i < mdwVersions.size(); i++)
        mdwVersionComboBox.add(mdwVersions.get(i));

      mdwVersionComboBox.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          String mdwVersion = mdwVersionComboBox.getText();
          getProject().setMdwVersion(mdwVersion);
        }
      });

      mdwVersionComboBox.setText(getProject().getMdwVersion());
    }
    originalMdwVersion = getProject().getMdwVersion();

    // app version
    if (!getProject().isCloudProject() && !"Unknown".equals(getProject().getAppVersion()))
    {
      new Label(composite, SWT.NONE).setText(getProject().getName() + ":");
      String appVer = getProject().getAppVersion();
      new Label(composite, SWT.NONE).setText(appVer == null ? "" : appVer);
    }
  }

  private void createJdbcUrlControls(Composite parent)
  {
    addHeading(parent, "Database");

    Composite composite = createComposite(parent, 2);

    new Label(composite, SWT.NONE).setText("Database JDBC URL:");
    new Label(composite, SWT.NONE);

    databaseJdbcUrlTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 450;
    gd.horizontalSpan = 2;
    databaseJdbcUrlTextField.setLayoutData(gd);
    databaseJdbcUrlTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        boolean success = getProject().getMdwDataSource().setJdbcUrlWithCredentials(databaseJdbcUrlTextField.getText().trim());
        setValid(success);
        setErrorMessage(success ? null : "Invalid JDBC URL");
      }
    });
    String jdbcUrl = getProject().getMdwDataSource().getJdbcUrlWithMaskedCredentials();
    if (jdbcUrl != null)
    {
      databaseJdbcUrlTextField.setText(jdbcUrl);
      originalJdbcUrl = jdbcUrl;
    }
  }

  private void createGitRepositoryControls(Composite parent)
  {
    addHeading(parent, "Git Repository");

    Composite composite = createComposite(parent, 2);

    new Label(composite, SWT.NONE).setText("Repository URL:");
    new Label(composite, SWT.NONE);

    gitRepositoryUrlTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 450;
    gd.horizontalSpan = 2;
    gitRepositoryUrlTextField.setLayoutData(gd);
    gitRepositoryUrlTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        boolean success = getProject().getMdwVcsRepository().setRepositoryUrlWithCredentials(gitRepositoryUrlTextField.getText().trim());
        setValid(success);
        setErrorMessage(success ? null : "Invalid Repository URL");
      }
    });
    String repoUrl = getProject().getMdwVcsRepository().getRepositoryUrlWithMaskedCredentials();
    if (repoUrl != null)
    {
      gitRepositoryUrlTextField.setText(repoUrl);
      originalGitRepositoryUrl = repoUrl;
    }

    // branch
    new Label(composite, SWT.NONE).setText("Branch:");
    gitBranchTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gitBranchTextField.setLayoutData(gd);
    gitBranchTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getProject().getMdwVcsRepository().setBranch(gitBranchTextField.getText().trim());
      }
    });
    String branch = getProject().getMdwVcsRepository().getBranch();
    if (branch != null)
      gitBranchTextField.setText(branch);
    originalGitBranch = branch;

    // local path
    new Label(composite, SWT.NONE).setText("Local Asset Path:");
    assetLocalPathTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 300;
    assetLocalPathTextField.setLayoutData(gd);
    assetLocalPathTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getProject().getMdwVcsRepository().setLocalPath(assetLocalPathTextField.getText().trim());
      }
    });
    String localPath = getProject().getMdwVcsRepository().getLocalPath();
    if (localPath != null)
      assetLocalPathTextField.setText(localPath);
    originalAssetLocalPath = localPath;

    if (getProject().isRemote())
    {
      if (getProject().isGitVcs())
      {
        // git: sync project
        syncSwitch = new SwitchButton(composite, SWT.NONE);
        syncSwitch.setTextForSelect("Synced");
        syncSwitch.setTextForUnselect("Unlocked");
        syncSwitch.setSelection(false);
        originalSync = true;
      }

      // non-git: include archive checkbox
      includeArchiveCheckbox = new Button(composite, SWT.CHECK | SWT.LEFT);
      includeArchiveCheckbox.setText("Include asset archive when synchronizing");
      gd = new GridData(GridData.BEGINNING);
      gd.horizontalSpan = 2;
      includeArchiveCheckbox.setLayoutData(gd);
      includeArchiveCheckbox.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          getProject().getMdwVcsRepository().setSyncAssetArchive(includeArchiveCheckbox.getSelection());
        }
      });
      boolean includeArchive = getProject().getMdwVcsRepository().isSyncAssetArchive();
      includeArchiveCheckbox.setSelection(includeArchive);
      originalIncludeArchive = includeArchive;
    }
  }

  private void createHostPortContextRootControls(Composite parent)
  {
    addHeading(parent, "Server");

    Composite composite = createComposite(parent, 2);

    new Label(composite, SWT.NONE).setText("MDW Host:");

    // host
    hostTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 225;
    hostTextField.setLayoutData(gd);
    hostTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getProject().getServerSettings().setHost(hostTextField.getText().trim());
      }
    });
    String host = getProject().getServerSettings().getHost();
    if (host != null)
      hostTextField.setText(host);
    originalHost = host;

    new Label(composite, SWT.NONE).setText("MDW Port:");
    portTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 100;
    portTextField.setLayoutData(gd);
    portTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getProject().getServerSettings().setPort(Integer.parseInt(portTextField.getText().trim()));
      }
    });
    int port = getProject().getServerSettings().getPort();
    if (port != 0)
      portTextField.setText(String.valueOf(port));
    originalPort = port;

    new Label(composite, SWT.NONE).setText("Web Context Root:");
    contextRootTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 175;
    contextRootTextField.setLayoutData(gd);
    contextRootTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getProject().setWebContextRoot(contextRootTextField.getText().trim());
      }
    });
    String contextRoot = getProject().getWebContextRoot();
    if (contextRoot != null)
      contextRootTextField.setText(contextRoot);
    originalContextRoot = contextRoot;

    // refresh server cache on save
    updateServerCacheCheckbox = new Button(composite, SWT.CHECK | SWT.LEFT);
    updateServerCacheCheckbox.setText("Update Server Cache");
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 2;
    updateServerCacheCheckbox.setLayoutData(gd);
    updateServerCacheCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        getProject().setUpdateServerCache(updateServerCacheCheckbox.getSelection());
      }
    });
    boolean refreshServerCache = getProject().isUpdateServerCache();
    updateServerCacheCheckbox.setSelection(refreshServerCache);
    originalRefreshServerCache = refreshServerCache;
  }

  private void createUpdateSettingsControls(Composite parent)
  {
    addHeading(parent, "Update");

    Composite composite = createComposite(parent, 2);

    // refresh server cache on save
    updateServerCacheCheckbox = new Button(composite, SWT.CHECK | SWT.LEFT);
    updateServerCacheCheckbox.setText("Update Server Cache");
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 2;
    updateServerCacheCheckbox.setLayoutData(gd);
    updateServerCacheCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        getProject().setUpdateServerCache(updateServerCacheCheckbox.getSelection());
      }
    });
    boolean refreshServerCache = getProject().isUpdateServerCache();
    updateServerCacheCheckbox.setSelection(refreshServerCache);
    originalRefreshServerCache = refreshServerCache;

    new Label(composite, SWT.NONE).setText("Files to Ignore During MDW Update:");
    new Label(composite, SWT.NONE);

    filesToIgnoreTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 450;
    gd.horizontalSpan = 2;
    filesToIgnoreTextField.setLayoutData(gd);
    filesToIgnoreTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getProject().setFilesToIgnoreDuringUpdate(filesToIgnoreTextField.getText().trim());
      }
    });
    String filesToIgnore = getProject().getFilesToIgnoreDuringUpdate();
    if (filesToIgnore != null)
      filesToIgnoreTextField.setText(filesToIgnore);
    originalFilesToIgnore = filesToIgnore;

    new Label(composite, SWT.NONE).setText("(Comma-separated list of files to exclude)");
  }

  @Override
  public boolean performCancel()
  {
    if (originalMdwVersion != null)
      getProject().setMdwVersion(originalMdwVersion);
    if (getProject().getPersistType() == PersistType.Git)
    {
      if (originalGitRepositoryUrl != null)
        getProject().getMdwVcsRepository().setRepositoryUrlWithCredentials(originalGitRepositoryUrl);
      if (originalGitBranch != null)
        getProject().getMdwVcsRepository().setBranch(originalGitBranch);
      if (originalAssetLocalPath != null)
        getProject().getMdwVcsRepository().setLocalPath(originalAssetLocalPath);
      getProject().getMdwVcsRepository().setSyncAssetArchive(originalIncludeArchive);
    }
    if (originalJdbcUrl != null)
      getProject().getMdwDataSource().setJdbcUrlWithCredentials(originalJdbcUrl);
    getProject().getMdwDataSource().setSchemaOwner(originalSchemaOwner);
    if (getProject().isRemote())
    {
      getProject().getServerSettings().setHost(originalHost);
      getProject().getServerSettings().setPort(originalPort);
      getProject().setWebContextRoot(originalContextRoot);
    }
    else
    {
      getProject().setFilesToIgnoreDuringUpdate(originalFilesToIgnore);
    }
    getProject().setUpdateServerCache(originalRefreshServerCache);
    return super.performCancel();
  }

  public boolean performOk()
  {
    IProject project = (IProject) getElement();
    try
    {
      WorkflowProjectManager.getInstance().save(getProject(), project);
    }
    catch (CoreException ex)
    {
      PluginMessages.uiError(getShell(), ex, "Project Settings", getProject());
      return false;
    }

    if (getProject().getPersistType() == PersistType.Git)
    {
      if (!gitRepositoryUrlTextField.getText().trim().equals(originalGitRepositoryUrl)
          || !gitBranchTextField.getText().trim().equals(originalGitBranch)
          || !assetLocalPathTextField.getText().trim().equals(originalAssetLocalPath)
          || (includeArchiveCheckbox != null && includeArchiveCheckbox.getSelection() != originalIncludeArchive))
      {
        getProject().getMdwVcsRepository().setEntrySource("projectSettingsPropertyPage");
        getProject().fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, getProject().getMdwVcsRepository());
      }
    }
    if (!databaseJdbcUrlTextField.getText().trim().equals(originalJdbcUrl))
    {
      getProject().getMdwDataSource().setEntrySource("projectSettingsPropertyPage");
      getProject().fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, getProject().getMdwDataSource());
    }
    if (mdwVersionComboBox != null && !mdwVersionComboBox.getText().equals(originalMdwVersion))
    {
      getProject().fireElementChangeEvent(ChangeType.VERSION_CHANGE, getProject().getMdwVersion());
      if (MessageDialog.openQuestion(getShell(), "Update Framework Libraries", "The MDW version has changed.  Would you like to download updated framework libraries to match the new selection?"))
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
          return false;
        }
      }
      if (getProject().isOsgi())
        MessageDialog.openInformation(getShell(), "MDW Version Changed", "The MDW version has been updated in the plug-in settings file.  Please update any MDW dependencies in your pom.xml build file.");
    }
    if (getProject().isRemote())
    {
      if (!hostTextField.getText().trim().equals(originalHost)
          || !portTextField.getText().trim().equals(String.valueOf(originalPort)))
      {
        getProject().fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, getProject().getServerSettings());
      }
      if (!contextRootTextField.getText().trim().equals(originalContextRoot))
      {
        getProject().fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, getProject().getWebContextRoot());
      }
      if (syncSwitch != null && syncSwitch.getSelection() != originalSync)
      {
        WorkflowProjectManager.getInstance().makeLocal(getProject());
        getProject().fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, getProject().getMdwVcsRepository());
        MessageDialog.openInformation(getShell(), "Remote Project Unlocked", getProject().getName() + " has been unlocked.  Please close any open assets and refresh.");
      }
    }
    if (updateServerCacheCheckbox.getSelection() != originalRefreshServerCache)
      getProject().fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, getProject().isUpdateServerCache());

    return true;
  }
}
