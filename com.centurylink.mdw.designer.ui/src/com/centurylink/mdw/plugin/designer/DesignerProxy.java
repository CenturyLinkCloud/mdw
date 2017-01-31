/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;

import javax.swing.UIManager;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;

import com.centurylink.mdw.auth.AuthenticationException;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.SelfSerializable;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.translator.impl.JavaObjectTranslator;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessOfflineException;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.VersionControlDummy;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.dataaccess.version4.DBMappingUtil;
import com.centurylink.mdw.designer.DataUnavailableException;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.MainFrame;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.pages.DesignerCanvas;
import com.centurylink.mdw.designer.pages.DesignerPage.PersistType;
import com.centurylink.mdw.designer.pages.FlowchartPage;
import com.centurylink.mdw.designer.runtime.ProcessInstanceLoader;
import com.centurylink.mdw.designer.runtime.ProcessInstancePage;
import com.centurylink.mdw.designer.runtime.ProcessInstanceTreeModel;
import com.centurylink.mdw.designer.runtime.RunTimeDesignerCanvas;
import com.centurylink.mdw.designer.testing.GroovyTestCaseRun;
import com.centurylink.mdw.designer.testing.LogMessageMonitor;
import com.centurylink.mdw.designer.testing.StubServer;
import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.designer.testing.TestCaseRun;
import com.centurylink.mdw.designer.testing.TestDataFilter;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.designer.utils.ProcessValidator;
import com.centurylink.mdw.designer.utils.ProcessWorker;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.model.Download;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskInstanceVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.plugin.CodeTimer;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.User;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerResult;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.dialogs.LoginDialog;
import com.centurylink.mdw.plugin.designer.dialogs.SwtDialogProvider;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.plugin.designer.model.VariableValue;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.editor.AssetLocator;
import com.centurylink.mdw.plugin.launch.GherkinTestCaseLaunch;
import com.centurylink.mdw.plugin.launch.LogWatcher;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.JdbcDataSource;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.swing.support.AwtEnvironment;
import com.centurylink.mdw.service.ApplicationSummaryDocument;
import com.centurylink.mdw.service.ApplicationSummaryDocument.ApplicationSummary;
import com.centurylink.mdw.service.DbInfo;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.service.Resource;

/**
 * Provides access to Designer functionality for the plug-in.  This is useful when
 * dealing with UI wrapper objects or when long-running operations require a progress
 * monitor provided by the DesignerRunner class.
 */
public class DesignerProxy
{
  private PluginDataAccess dataAccess;
  public PluginDataAccess getPluginDataAccess() { return dataAccess; }

  private WorkflowProject project;

  private RestfulServer restfulServer;
  public RestfulServer getRestfulServer() { return restfulServer; }
  public void setRestfulServerWebUrl(String url)
  {
    restfulServer.setMdwWebUrl(url);
  }

  private VersionControlGit versionControl;

  private CacheRefresh cacheRefresh;
  public CacheRefresh getCacheRefresh() { return cacheRefresh; }

  private ServerGit serverGit;

  private AwtEnvironment awtEnvironment;
  public AwtEnvironment getAwtEnvironment() { return awtEnvironment; }

  private DesignerRunner designerRunner;
  public RunnerResult getRunnerResult()
  {
    return designerRunner.getResult();
  }
  public RunnerStatus getRunnerStatus()
  {
    if (designerRunner == null)
      return null;
    return designerRunner.getStatus();
  }

  public DesignerProxy(WorkflowProject workflowProject)
  {
    this.project = workflowProject;
  }

  private MainFrame mainFrame;

  public DesignerDataAccess getDesignerDataAccess()
  {
    return dataAccess.getDesignerDataAccess();
  }

  public NodeMetaInfo getNodeMetaInfo()
  {
    return dataAccess.getDesignerDataModel().getNodeMetaInfo();
  }

  public IconFactory getIconFactory()
  {
    return mainFrame.getIconFactory();
  }

  public void initialize(ProgressMonitor progressMonitor) throws Exception
  {
    mainFrame = new MainFrame("Not Displayed");
    mainFrame.setOptionPane(new SwtDialogProvider(MdwPlugin.getDisplay()));

    CodeTimer timer = new CodeTimer("initialize()");
    Map<String,String> connProps = new HashMap<String,String>();

    try
    {
      User user = project.getUser();
      if (user == null)
        handleLazyUserAuth();
      if (project.getPersistType() == WorkflowProject.PersistType.Git)
      {
        String jdbcUrl = project.getMdwDataSource().getJdbcUrlWithCredentials();
        int schemaVersion = project.getMdwMajorVersion() * 1000 + project.getMdwMinorVersion() * 100;
        restfulServer = new RestfulServer(jdbcUrl, project.getUser().getUsername(), project.getServiceUrl(), schemaVersion);
        VcsRepository gitRepo = project.getMdwVcsRepository();
        versionControl = new VersionControlGit();
        String gitUser = null;
        String gitPassword = null;
        if (MdwPlugin.getSettings().isUseDiscoveredVcsCredentials())
        {
          gitUser = gitRepo.getUser();
          gitPassword = gitRepo.getPassword();
        }
        versionControl.connect(gitRepo.getRepositoryUrl(), gitUser, gitPassword, project.getProjectDir());
        restfulServer.setVersionControl(versionControl);
        restfulServer.setRootDirectory(project.getAssetDir());
        if (project.isRemote())
        {
          File assetDir = project.getAssetDir();
          boolean isGit = gitRepo.getRepositoryUrl() != null;
          String pkgDownloadServicePath = null;
          try
          {
            if (isGit)
            {
              serverGit = new ServerGit(project, restfulServer);
              // update branch from Git
              if (progressMonitor != null)
                progressMonitor.subTask("Retrieving Git status");
              Platform.getBundle("org.eclipse.egit.ui").start(); // avoid Eclipse default Authenticator -- otherwise login fails
              ApplicationSummaryDocument appSummaryDoc = restfulServer.getAppSummary();
              ApplicationSummary appSummary = appSummaryDoc.getApplicationSummary();
              if (appSummary.getRepository() == null)
                throw new DataAccessOfflineException("Unable to confirm Git status on server (missing repository)");
              String branch = appSummary.getRepository().getBranch();
              if (branch == null || branch.isEmpty())
                throw new DataAccessOfflineException("Unable to confirm Git status on server (missing branch)");
              String oldBranch = gitRepo.getBranch();
              if (!branch.equals(oldBranch))
                gitRepo.setBranch(branch);
              if (progressMonitor != null)
                progressMonitor.subTask("Updating from branch: " + branch);
              versionControl.hardReset();  // Reset any existing files to avoid conflicts and other issues with pulling/changing branch
              versionControl.checkout(branch); // in case changed
              versionControl.pull(branch);
              String serverCommit = appSummary.getRepository().getCommit();
              String localCommit = versionControl.getCommit();
              if (localCommit == null || !localCommit.equals(serverCommit))
              {
                project.setWarn(true);
                PluginMessages.log("Server commit: " + serverCommit + " does not match Git repository for branch "
                    + branch + ": " + versionControl.getCommit() + ".", IStatus.WARNING);
              }
              WorkflowProjectManager.getInstance().save(project); // save the discovered branch
              if (progressMonitor != null)
                progressMonitor.progress(10);
              if (project.checkRequiredVersion(5, 5, 34))
                pkgDownloadServicePath = "Packages?format=json&nonVersioned=true";
            }
            else
            {
              // non-git -- delete existing asset dir
              if (assetDir.exists())
                PluginUtil.deleteDirectory(assetDir);
              if (!assetDir.mkdirs())
                throw new DiscoveryException("Unable to create asset directory: " + assetDir);
              pkgDownloadServicePath = "Packages?format=json&topLevel=true";
            }

            if (pkgDownloadServicePath != null && progressMonitor != null)
            {
              if (gitRepo.isSyncAssetArchive())
                pkgDownloadServicePath += "&archive=true";
              String json = restfulServer.invokeResourceService(pkgDownloadServicePath);

              Download download = new Download(new JSONObject(json));
              if (!StringHelper.isEmpty(download.getUrl()))
              {
                URL url = new URL(download.getUrl());
                IFolder tempFolder = project.getTempFolder();
                IFile tempFile = tempFolder.getFile(download.getFile());
                IProgressMonitor subMonitor = new SubProgressMonitor(((SwtProgressMonitor)progressMonitor).getWrappedMonitor(), 5);
                try
                {
                  PluginUtil.downloadIntoProject(project.getSourceProject(), url, tempFolder, tempFile, "Download Packages", subMonitor);
                  PluginUtil.unzipProjectResource(project.getSourceProject(), tempFile, null, project.getAssetFolder(), subMonitor);
                }
                catch (FileNotFoundException ex)
                {
                  if (isGit)
                    throw new DataUnavailableException("Extra/Archived packages not retrieved: " + ex.getMessage(), ex);
                  else
                    throw ex;
                }
              }
            }
          }
          catch (ZipException ze)
          {
            throw ze;
          }
          catch (IOException ex)
          {
            throw new DataAccessOfflineException("Server appears to be offline: " + ex.getMessage(), ex);
          }
        }
      }
      else if (project.getPersistType() == WorkflowProject.PersistType.None)
      {
        restfulServer = new RestfulServer(null, project.getUser().getUsername(), project.getServiceUrl());
        VersionControl versionControl = new VersionControlDummy();
        versionControl.connect(null, null, null, project.getProjectDir());
        restfulServer.setVersionControl(versionControl);
        restfulServer.setRootDirectory(project.getAssetDir());
      }
      else
      {
        String jdbcUrl = project.getMdwDataSource().getJdbcUrlWithCredentials();
        if (jdbcUrl == null)
          throw new DataAccessException("Please specify a valid JDBC URL in your MDW Project Settings");

        if (project.getMdwDataSource().getSchemaOwner() == null)
          DBMappingUtil.setSchemaOwner("");  // don't qualify queries
        else
          DBMappingUtil.setSchemaOwner(project.getMdwDataSource().getSchemaOwner());

        restfulServer = new RestfulServer(jdbcUrl, project.getUser().getUsername(), project.getServiceUrl());
        connProps.put("defaultRowPrefetch", String.valueOf(MdwPlugin.getSettings().getJdbcFetchSize()));
      }
      cacheRefresh = new CacheRefresh(project, restfulServer);

      boolean oldNamespaces = project.isOldNamespaces();
      boolean remoteRetrieve = project.isFilePersist() && project.checkRequiredVersion(5, 5, 19);
      restfulServer.setConnectTimeout(MdwPlugin.getSettings().getHttpConnectTimeout());
      restfulServer.setReadTimeout(MdwPlugin.getSettings().getHttpReadTimeout());
      mainFrame.startSession(project.getUser().getUsername(), restfulServer, progressMonitor, connProps, oldNamespaces, remoteRetrieve);
      restfulServer.setDataModel(mainFrame.getDataModel());
      mainFrame.dao.setCurrentServer(restfulServer);
      dataAccess = new PluginDataAccess(project, mainFrame.getDataModel(), mainFrame.dao);
      dataAccess.organizeRuleSets();  // they've already been retrieved
      // static supportedSchemaVersion has just been set, so save it at instance level
      dataAccess.setSupportedSchemaVersion(DataAccess.supportedSchemaVersion);

      if (project.getPersistType() == WorkflowProject.PersistType.Git && !project.isRemote())
      {
        try
        {
          mainFrame.dao.checkServerOnline();
        }
        catch (DataAccessOfflineException offlineEx)
        {
          if (MdwPlugin.getSettings().isLogConnectErrors())
            PluginMessages.log(offlineEx);
        }
      }


      dataAccess.getVariableTypes(true);

      try
      {
        // override mainframe's settings for look-and-feel
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch (Exception ex)
      {
        PluginMessages.log(ex);
      }

      System.setProperty("awt.useSystemAAFontSettings", "on");
      System.setProperty("swing.aatext", "true");
    }
    finally
    {
      timer.stopAndLog();
    }
  }

  public FlowchartPage newFlowchartPage()
  {
    return FlowchartPage.newPage(mainFrame);
  }

  public ProcessInstancePage newProcessInstancePage()
  {
    return ProcessInstancePage.newPage(mainFrame);
  }

  public ProcessVO getProcessVO(String processName, String version)
  {
    return dataAccess.getProcess(processName, version);
  }

  public ProcessVO getLatestProcessVO(String processName)
  {
    return dataAccess.getLatestProcess(processName);
  }

  public ProcessVO getProcessVO(Long processId)
  {
    ProcessVO processVO = dataAccess.getProcess(processId);
    if (processVO != null)
      return processVO;

    // may be private process -- try loading
    try
    {
      processVO = dataAccess.getDesignerDataAccess().getProcessDefinition(processId);
      dataAccess.getDesignerDataModel().addPrivateProcess(processVO);
      return processVO;
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Load Process", project);
    }

    return null; // not found
  }

  public DesignerCanvas loadProcess(WorkflowProcess processVersion, FlowchartPage flowchartPage)
  {
    CodeTimer timer = new CodeTimer("loadProcess()");
    Graph graph = flowchartPage.loadProcess(processVersion.getId(), null);
    flowchartPage.setProcess(graph);
    if (project.isRemote() && project.isFilePersist()) {
      if (!project.isGitVcs() || project.getMdwVcsRepository().isGitProjectSync())
        graph.setReadonly(true);
    }
    processVersion.setProcessVO(graph.getProcessVO());
    processVersion.setReadOnly(graph.isReadonly());
    timer.stopAndLog();
    return flowchartPage.canvas;
  }

  /**
   * Performs a deep load and populates the DesignerDataModel.
   */
  public ProcessVO loadProcess(String name, int version) throws RemoteException, DataAccessException
  {
    Graph graph = dataAccess.getDesignerDataModel().findProcessGraph(name, version);
    if (graph != null)
      return graph.getProcessVO();

    ProcessVO procDef = dataAccess.getDesignerDataModel().findProcessDefinition(name, version);
    ProcessVO loaded = getDesignerDataAccess().getProcess(procDef.getProcessId(), procDef);
    new ProcessWorker().convert_to_designer(loaded);
    dataAccess.getDesignerDataModel().addProcessGraph(new Graph(loaded, dataAccess.getDesignerDataModel().getNodeMetaInfo(), getIconFactory()));
    return loaded;
  }


  public void registerExternalEventHandler(ExternalEvent externalEvent)
  {
    try
    {
      dataAccess.getDesignerDataAccess().createExternalEvent(externalEvent.getExternalEventVO());
      if (!externalEvent.isInDefaultPackage())
        dataAccess.getDesignerDataAccess().addExternalEventToPackage(externalEvent.getExternalEventVO(), externalEvent.getPackage().getPackageVO());

      dataAccess.getDesignerDataModel().addExternalEvent(externalEvent.getExternalEventVO());
      externalEvent.getPackage().addExternalEvent(externalEvent);
      Collections.sort(externalEvent.getPackage().getExternalEvents());
      cacheRefresh.fireRefresh(externalEvent.isDynamicJava());
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Create External Event Handler", project);
    }
  }

  public void saveExternalEvent(ExternalEvent externalEvent)
  {
    try
    {
      dataAccess.getDesignerDataAccess().updateExternalEvent(externalEvent.getExternalEventVO());
      cacheRefresh.refreshSingle("ExternalEventCache", true);
    }
    catch (Exception ex)
    {
      if (ex.getCause() != null && ex.getCause().getMessage() != null && ex.getCause().getMessage().startsWith(("ORA-00001")))
        PluginMessages.uiMessage("Message Pattern already exists.  External Event cannot be saved.", "Save External Event", PluginMessages.INFO_MESSAGE);
      else
        PluginMessages.uiError(ex, "Create External Event Handler", project);
    }
  }

  public void deleteExternalEvent(final ExternalEvent externalEvent)
  {
    String progressMsg = "Deleting Event Handler '" + externalEvent.getName() + "'";
    String errorMsg = "Delete Event Handler";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws DataAccessException, RemoteException
      {
        if (externalEvent.isInDefaultPackage())
        {
          externalEvent.getProject().getDefaultPackage().removeExternalEvent(externalEvent);
        }
        else
        {
          dataAccess.getDesignerDataAccess().removeExternalEventFromPackage(externalEvent.getExternalEventVO(), externalEvent.getPackage().getPackageVO());
          externalEvent.getPackage().removeExternalEvent(externalEvent);
        }
        dataAccess.getDesignerDataAccess().removeExternalEvent(externalEvent.getExternalEventVO());
      }
    };
    designerRunner.run();
  }

  public void createActivityImpl(ActivityImpl activityImpl)
  {
    try
    {
      dataAccess.getDesignerDataAccess().createActivityImplementor(activityImpl.getActivityImplVO(), false, null);
      if (!activityImpl.isInDefaultPackage())
        dataAccess.getDesignerDataAccess().addActivityImplToPackage(activityImpl.getActivityImplVO(), activityImpl.getPackage().getPackageVO());

      dataAccess.getDesignerDataModel().addImplementor(activityImpl.getActivityImplVO());
      activityImpl.getPackage().addActivityImpl(activityImpl);
      Collections.sort(activityImpl.getPackage().getActivityImpls());
      cacheRefresh.fireRefresh(activityImpl.isDynamicJava());
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Create Activity Implementorr", project);
    }
  }


  public void saveActivityImpl(ActivityImpl activityImpl)
  {
    try
    {
      dataAccess.getDesignerDataAccess().updateActivityImplementor(activityImpl.getActivityImplVO(), false, null);
      cacheRefresh.fireRefresh(false);
    }
    catch (Exception ex)
    {
      if (ex.getCause() != null && ex.getCause().getMessage() != null && ex.getCause().getMessage().startsWith(("ORA-00001")))
        PluginMessages.uiMessage("ActivityImplementor already exists and cannot be saved.", "Save Activity Implementor", PluginMessages.INFO_MESSAGE);
      else
        PluginMessages.uiError(ex, "Save Activity Implementor", project);
    }
  }

  public void saveActivityImpls(final List<ActivityImpl> activityImpls)
  {
    String progressMsg = "Saving Activity Implementor(s)";
    String errorMsg = "Save Implementors";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws DataAccessException, RemoteException
      {
        for (ActivityImpl activityImpl : activityImpls)
        {
          if (activityImpl.getActivityImplVO().isShowInToolbox())
            dataAccess.getDesignerDataAccess().updateActivityImplementor(activityImpl.getActivityImplVO(), false, null);
        }
        cacheRefresh.fireRefresh(false);
      }
    };
    designerRunner.run();
  }

  public void deleteActivityImpl(final ActivityImpl activityImpl, final boolean includeActivities)
  {
    String progressMsg = "Deleting Activity Implementor '" + activityImpl.getName() + "'";
    String errorMsg = "Delete Activity Implementor";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws DataAccessException, RemoteException, ValidationException
      {
        for (PackageVO packageVO : dataAccess.getPackages(false))
        {
          if (packageVO.containsActivityImpl(activityImpl.getId()))
          {
            dataAccess.getDesignerDataAccess().removeActivityImplFromPackage(activityImpl.getActivityImplVO(), packageVO);
            project.getPackage(packageVO.getName()).removeActivityImpl(activityImpl);
          }
        }
        try
        {
          dataAccess.getDesignerDataAccess().removeActivityImplementor(activityImpl.getActivityImplVO(), includeActivities);
          dataAccess.getDesignerDataModel().removeImplementor(activityImpl.getActivityImplVO());
        }
        catch (DataAccessException ex)
        {
          if (ex.getCause() != null && ex.getCause().getMessage() != null && ex.getCause().getMessage().startsWith("ORA-02292"))
          {
            PluginMessages.log(ex);
            throw new ValidationException("It appears that activity implementor '" + activityImpl.getName() + "' is used by one or more processes.  Please delete the processes before proceeding.");
          }
          else
          {
            throw ex;
          }
        }
      }
    };
    designerRunner.run();
  }

  /**
   * Returns the default implementor as a generic representation.
   */
  public ActivityImpl getGenericActivityImpl(String implClass)
  {
    ActivityImplementorVO implVO = dataAccess.getDesignerDataModel().getNodeMetaInfo().getDefaultActivity();
    if (implVO == null)
    {
      // even base impl cannot be loaded
      implVO = new ActivityImplementorVO();
      implVO.setLabel(implClass);
      implVO.setAttributeDescription("<PAGELET/>");
    }
    implVO.setImplementorClassName(implClass);
    ActivityImpl impl = new ActivityImpl(implVO, project.getDefaultPackage());
    impl.setProject(project);
    return impl;
  }

  public void saveWorkflowAssetWithProgress(final WorkflowAsset asset, final boolean keepLocked)
  {
    String progressMsg = "Saving '" + asset.getName() + "'";
    String errorMsg = "Save Workflow Asset";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
         saveWorkflowAsset(asset, keepLocked);
      }
    };
    designerRunner.run();
  }

  public RunnerResult createNewWorkflowAsset(final WorkflowAsset asset, final boolean lockToUser)
  throws ValidationException, DataAccessException, RemoteException
  {
    String progressMsg = "Saving '" + asset.getName() + "'";
    String errorMsg = "Save " + asset.getTitle();

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        if (!asset.getProject().isFilePersist())
        {
          // check for existing
          RuleSetVO existing = dataAccess.getDesignerDataAccess().getRuleSet(asset.getPackage() == null ? 0 : asset.getPackage().getId(), asset.getName());
          if (existing != null && existing.getVersion() == asset.getVersion())
            throw new ValidationException(asset.getFullPathLabel() + ALREADY_EXISTS
                 + "Please perform the following steps to recover:\n"
                 + "     - Close all open asset editors\n"
                 + "     - Refresh Process Explorer view and open the latest version asset\n"
                 + "     - Recover changes from backup under " + asset.getTempFolder().getFullPath() + "\n"
                 + "     - Save the asset again, incrementing the version");

        }

        asset.setId(new Long(-1));
        // custom attributes initialized to empty
        asset.setAttributes(null);

        asset.setCreateUser(project.getUser().getUsername());
        asset.setLockingUser(project.getUser().getUsername());

        saveWorkflowAsset(asset, lockToUser);
        dataAccess.getDesignerDataModel().addRuleSet(asset.getRuleSetVO());
        // update the tree
        if (!asset.isInDefaultPackage())
        {
          asset.getPackage().addAsset(asset);
          savePackage(asset.getPackage());
        }
        else
        {
          asset.getProject().getDefaultPackage().addAsset(asset);
        }
      }
    };
    return designerRunner.run();
  }

  /**
   * Only for MDW 5.5 with VCS assets.
   */
  public RunnerResult createNewTaskTemplate(final TaskTemplate taskTemplate)
  throws ValidationException, DataAccessException, RemoteException
  {
    String progressMsg = "Saving '" + taskTemplate.getName() + "'";
    String errorMsg = "Save " + taskTemplate.getTitle();

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        if (!taskTemplate.getProject().isFilePersist())
          throw new ValidationException("New Task Template only for VCS Assets");
        taskTemplate.getTaskVO().setVersion(1);
        dataAccess.getDesignerDataAccess().createTaskTemplate(taskTemplate.getTaskVO());
        // update the tree
        taskTemplate.getPackage().addTaskTemplate(taskTemplate);
        savePackage(taskTemplate.getPackage());
      }
    };
    return designerRunner.run();
  }

  public void saveWorkflowAsset(WorkflowAsset asset, boolean keepLocked)
  throws DataAccessException, RemoteException, ValidationException
  {
    if (!asset.isBinary())
    {
      if (asset.getContent() == null || asset.getContent().length() == 0)
        asset.setContent(asset.getDefaultContent());
    }

    if (!keepLocked)
      asset.setLockingUser(null);

    boolean isNew = asset.getId() == null || asset.getId() == -1;
    Long id = dataAccess.getDesignerDataAccess().saveRuleSet(asset.getRuleSetVO());
    asset.getRuleSetVO().setId(id);
    asset.setModifyDate(new Date()); // roughly the same as db time hopefully

    WorkflowPackage pkg = asset.getPackage();
    if (pkg != null && !pkg.isDefaultPackage())
    {
      if (project.isFilePersist())
      {
        // for raw assets, saveWorkflowAsset() is only called when version incremented, so pkg needs saving
        pkg.setVersion(pkg.getVersion() + 1);
        pkg.setExported(false); // exported not relevant for vcs processes since pkg will be archived anyway
        savePackage(pkg, true);
      }
      else
      {
        // TODO: savePackage(pkg);
      }
    }

    if (!isNew && asset.getProject().isGitVcs() && asset.getProject().isRemote())
    {
      IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
      boolean pushToGit = prefsStore.getBoolean(PreferenceConstants.PREFS_PUSH_TO_GIT_REMOTE_WHEN_SAVING);
      if (pushToGit)
      {
        try
        {
          commitAndPush(asset);
          serverGit.pullAsset(asset);
        }
        catch (Exception ex)
        {
          throw new DataAccessException(ex.getMessage(), ex);
        }
      }
    }

    cacheRefresh.fireRefresh(RuleSetVO.JAVA.equals(asset.getLanguage()));
  }

  private void commitAndPush(WorkflowAsset asset) throws Exception
  {
    String assetPath = asset.getVcsAssetPath();
    if (!versionControl.isTracked(assetPath))
      versionControl.add(assetPath);
    String msg = asset.getRevisionComment();
    List<String> paths = asset.getPackage().getVcsAssetMetaPaths();
    paths.add(assetPath);
    versionControl.commit(paths, msg);
    versionControl.push();

  }

  private void commitAndPush(WorkflowProcess process) throws Exception
  {
    String assetPath = process.getVcsAssetPath();
    if (!versionControl.isTracked(assetPath))
      versionControl.add(assetPath);
    // TODO: capture asset comments as when saving other types of assets
    String msg = process.getLabel() + " saved by " + project.getUser().getUsername();
    List<String> paths = process.getPackage().getVcsAssetMetaPaths();
    paths.add(assetPath);
    versionControl.commit(paths, msg);
    versionControl.push();
  }

  public void deleteWorkflowAsset(final WorkflowAsset asset)
  {
    String progressMsg = "Deleting '" + asset.getName() + "'";
    String errorMsg = "Delete " + asset.getTitle();

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws DataAccessException, RemoteException
      {
        dataAccess.getDesignerDataAccess().deleteRuleSet(asset.getRuleSetVO());
        if (asset.isInDefaultPackage())
          asset.getProject().getDefaultPackage().removeAsset(asset);
        else
          asset.getPackage().removeAsset(asset);

        dataAccess.getDesignerDataModel().removeRuleSet(asset.getRuleSetVO(), false);
      }
    };
    designerRunner.run();
  }

  public void deleteTaskTemplate(final TaskTemplate taskTemplate)
  {
    String progressMsg = "Deleting '" + taskTemplate.getName() + "'";
    String errorMsg = "Delete " + taskTemplate.getTitle();

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws DataAccessException, RemoteException
      {
        if (!taskTemplate.getProject().isFilePersist())
          throw new DataAccessException("Delete Task Template only for VCS Assets");
        dataAccess.getDesignerDataAccess().deleteTaskTemplate(taskTemplate.getTaskVO());
        taskTemplate.getPackage().removeTaskTemplate(taskTemplate);
      }
    };
    designerRunner.run();
  }

  public boolean saveOverrideAttributes(String prefix, String ownerType, Long ownerId, String subType, String subId, Map<String,String> attributes)
  {
    try
    {
      try
      {
        dataAccess.getDesignerDataAccess().setOverrideAttributes(prefix, ownerType, ownerId, subType, subId, attributes);
      }
      catch (DataAccessOfflineException ex)
      {
        PluginMessages.log(ex);
        MessageDialog.openWarning(MdwPlugin.getShell(), "Save Attributes", "Server appears to be offline: " + ex.getMessage());
        return false;
      }
      cacheRefresh.fireRefresh(false);
      return true;
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Save Attributes", project);
      return false;
    }
  }

  public boolean saveAllOverrideAttributes(WorkflowProcess process)
  {
    try
    {
      try
      {
        dataAccess.getDesignerDataAccess().setOverrideAttributes(process.getProcessVO());
      }
      catch (DataAccessOfflineException ex)
      {
        PluginMessages.log(ex);
        MessageDialog.openWarning(MdwPlugin.getShell(), "Save Attributes", "Server appears to be offline: " + ex.getMessage());
        return false;
      }
      cacheRefresh.fireRefresh(false);
      return false;
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Save Attributes", project);
      return false;
    }
  }

  public void createNewProcess(final WorkflowProcess processVersion)
  {
    String progressMsg = "Creating '" + processVersion.getName() + "'";
    String errorMsg = "Create Process";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        if (dataAccess.getDesignerDataModel().getNodeMetaInfo().getStartActivity() == null)
          throw new ValidationException("Please import the MDW Baseline Package corresponding to your MDW framework version.");

        Graph process = new Graph(processVersion.getName(), ProcessVisibilityConstant.PUBLIC, dataAccess.getDesignerDataModel().getNodeMetaInfo(), getIconFactory());
        process.setDescription(processVersion.getDescription());
        process.getProcessVO().setInRuleSet(processVersion.isInRuleSet());
        if (project.isFilePersist())
          process.getProcessVO().setRawFile(new File(project.getAssetDir() + "/" + processVersion.getPackage().getName() + "/" + processVersion.getName() + ".proc"));
        if (!processVersion.isInDefaultPackage())
          process.getProcessVO().setPackageName(processVersion.getPackage().getName());
        final FlowchartPage flowchartPage = FlowchartPage.newPage(mainFrame);
        flowchartPage.setProcess(process);

        saveProcess(processVersion, flowchartPage, PersistType.CREATE, 0, false, false);
        toggleProcessLock(processVersion, true);

        if (processVersion.isInRuleSet())
          dataAccess.getAllRuleSets(false).add(processVersion.getProcessVO());

        // update the process tree
        if (!processVersion.isInDefaultPackage())
        {
          processVersion.getPackage().addProcess(processVersion);
          savePackage(processVersion.getPackage());
        }
        else
        {
          processVersion.getProject().getDefaultPackage().addProcess(processVersion);
        }
        // add to the mainFrame process list
        dataAccess.getDesignerDataModel().addProcess(processVersion.getProcessVO());
        // dataAccess.getProcesses(true);
      }
    };
    designerRunner.run();
  }

  public void saveProcessAs(final WorkflowProcess processVersion, final WorkflowPackage targetPackage, final String newName, final boolean validate)
  {
    String progressMsg = "Saving '" + newName + "'";
    String errorMsg = "Save Process";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        ProcessVO origProcVO = processVersion.getProcessVO();
        ProcessVO newProcVO = new ProcessVO(-1L, newName, origProcVO.getProcessDescription(), null);
        newProcVO.set(origProcVO.getAttributes(), origProcVO.getVariables(), origProcVO.getTransitions(), origProcVO.getSubProcesses(), origProcVO.getActivities());
        newProcVO.setVersion(1);
        newProcVO.setInRuleSet(origProcVO.isInRuleSet());
        WorkflowProcess newProcess = new WorkflowProcess(targetPackage.getProject(), newProcVO);
        newProcess.setPackage(targetPackage);

        Graph process = new Graph(newProcVO, dataAccess.getDesignerDataModel().getNodeMetaInfo(), getIconFactory());
        process.dirtyLevel = Graph.NEW;
        // mainFrame.procmenu.dataChanged(new DataChangeEvent(DataChangeEvent.DEFINITION_GRAPH_ADD, process, false));
        FlowchartPage flowchartPage = FlowchartPage.newPage(mainFrame);
        flowchartPage.setProcess(process);

        saveProcess(newProcess, flowchartPage, PersistType.CREATE, 0, false, false);
        toggleProcessLock(newProcess, true);
        newProcess.getProcessVO().setVersion(1);  // why?

        dataAccess.getProcesses(false).add(newProcess.getProcessVO());

        targetPackage.addProcess(newProcess);
        newProcess.setPackage(targetPackage);

        if (!newProcess.isInDefaultPackage())
          savePackage(newProcess.getPackage());
      }
    };
    designerRunner.run();
  }

  public void createNewPackage(final WorkflowPackage newPackage, final boolean isJson)
  {
    String progressMsg = "Creating '" + newPackage.getName() + "'";
    String errorMsg = "Create Package";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws DataAccessException, RemoteException, ValidationException
      {
        com.centurylink.mdw.dataaccess.ProcessPersister.PersistType persistType;
        if (isJson)
            persistType = com.centurylink.mdw.dataaccess.ProcessPersister.PersistType.CREATE_JSON;
        else
            persistType = com.centurylink.mdw.dataaccess.ProcessPersister.PersistType.NEW_VERSION;
        Long id = dataAccess.getDesignerDataAccess().savePackage(newPackage.getPackageVO(), persistType);
        newPackage.getPackageVO().setId(id);
        // update the process tree
        newPackage.getProject().addPackage(newPackage);
        dataAccess.getPackages(false).add(newPackage.getPackageVO());
        // update the tree
        newPackage.getProject().fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newPackage);
        // update other listeners
        newPackage.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newPackage);
      }
    };
    designerRunner.run();
  }

  /**
   * Deletes all versions of a package.
   */
  public void deletePackage(final WorkflowPackage packageToDelete)
  {
    if (packageToDelete.isDefaultPackage())
      return;

    String progressMsg = "Deleting '" + packageToDelete.getLabel();
    String errorMsg = "Delete Package";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws DataAccessException, RemoteException
      {
        dataAccess.getDesignerDataAccess().deletePackage(packageToDelete.getPackageVO());
        WorkflowProject workflowProject = packageToDelete.getProject();
        if (workflowProject.isShowDefaultPackage())
        {
          WorkflowPackage defaultPackage = workflowProject.getDefaultPackage();
          for (WorkflowProcess processVersion : packageToDelete.getProcesses())
            defaultPackage.addProcess(processVersion);
          for (ActivityImpl activityImpl : packageToDelete.getActivityImpls())
            defaultPackage.addActivityImpl(activityImpl);
          for (ExternalEvent externalEvent : packageToDelete.getExternalEvents())
            defaultPackage.addExternalEvent(externalEvent);
          for (TaskTemplate taskTemplate : packageToDelete.getTaskTemplates())
            defaultPackage.addTaskTemplate(taskTemplate);
        }
        workflowProject.removePackage(packageToDelete);
        dataAccess.getPackages(true);
      }
    };
    designerRunner.run();
  }

  public void renamePackage(final WorkflowPackage packageToRename, final String newName)
  {
    if (packageToRename.isDefaultPackage())
      return;

    final WorkflowProject workflowProject = packageToRename.getProject();
    if (workflowProject.packageNameExists(newName))
    {
      Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
      MessageDialog.openError(shell, "Can't Rename", "Package name already exists: '" + newName + "'");
      return;
    }

    String progressMsg = "Renaming package to '" + newName + "'";
    String errorMsg = "Rename Package";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, workflowProject)
    {
      public void perform() throws DataAccessException, RemoteException
      {
        packageToRename.getPackageVO().setId(dataAccess.getDesignerDataAccess().renamePackage(packageToRename.getId(), newName, 1));
      }
    };
    designerRunner.run();
    packageToRename.setName(newName);
  }

  public void renameWorkflowAsset(final WorkflowAsset asset, final String newName)
  {
    if (asset.getPackage().workflowAssetNameExists(newName))
    {
      Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
      MessageDialog.openError(shell, "Can't Rename", "Name already exists: '" + newName + "'");
      return;
    }

    String progressMsg = "Renaming to '" + newName + "'";
    String errorMsg = "Rename Workflow Asset";

    if (!asset.isLoaded())
      asset.load();

    designerRunner = new DesignerRunner(progressMsg, errorMsg, asset.getProject())
    {
      public void perform() throws DataAccessException, RemoteException
      {
        asset.setVersion(1);
        dataAccess.getDesignerDataModel().removeRuleSet(asset.getRuleSetVO(), false);
        dataAccess.getDesignerDataAccess().renameRuleSet(asset.getRuleSetVO(), newName);
        if (asset instanceof AutomatedTestCase)
        {
          if (project.isFilePersist())
            ((AutomatedTestCase)asset).setTestCase(new TestCase(asset.getPackage().getName(), asset.getRawFile()));
          else
            ((AutomatedTestCase)asset).setTestCase(new TestCase(asset.getPackage().getName(), asset.getRuleSetVO()));
        }
        asset.setModifyDate(new Date()); // roughly the same as db time hopefully
        cacheRefresh.fireRefresh(RuleSetVO.JAVA.equals(asset.getLanguage()));
        dataAccess.getDesignerDataModel().addRuleSet(asset.getRuleSetVO());
      }
    };
    designerRunner.run();
  }

  public List<WorkflowProcess> getProcessesUsingActivityImpl(Long activityImplId, String implClass) throws DataAccessException
  {
    List<WorkflowProcess> processVersions = new ArrayList<WorkflowProcess>();
    for (ProcessVO processVO : dataAccess.getDesignerDataAccess().getProcessListForImplementor(activityImplId, implClass))
    {
      WorkflowProcess processVersion = project.getProcess(processVO.getProcessId());
      if (processVersion == null)  // might not be loaded in tree
        processVersion = new WorkflowProcess(project, processVO);
      processVersions.add(processVersion);
    }
    return processVersions;
  }

  public void renameProcess(final WorkflowProcess processVersion, final String newName)
  {
    if (dataAccess.processNameExists(newName))
    {
      Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
      MessageDialog.openError(shell, "Can't Rename", "Process name already exists: '" + newName + "'");
      return;
    }

    String version = "v" + processVersion.getVersionString();
    String progressMsg = "Renaming to '" + newName + "' " + version;
    String errorMsg = "Rename Process";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException, XmlException
      {
        try
        {
          if (dataAccess.getDesignerDataAccess().hasProcessInstances(processVersion.getId()))
            throw new DataAccessException("Process " + processVersion.getLabel() + " has instances and cannot be renamed.\nPlease save as a new version.");
        }
        catch (DataAccessOfflineException ex)
        {
          final StringBuffer confirm = new StringBuffer();
          MdwPlugin.getDisplay().syncExec(new Runnable()
          {
            public void run()
            {
              String msg = "Cannot connect to server to check for instances.  Are you sure you want to rename?";
              confirm.append(MessageDialog.openConfirm(MdwPlugin.getShell(), "Rename Process", msg));
            }
          });
          if (!Boolean.valueOf(confirm.toString()))
            return;
        }

        dataAccess.removeProcess(processVersion.getProcessVO());

        if (processVersion.isInRuleSet() && !project.isFilePersist())
        {
          ProcessVO procVO = dataAccess.getDesignerDataAccess().getProcessDefinition(processVersion.getName(), processVersion.getVersion());
          procVO = dataAccess.getDesignerDataAccess().getProcess(procVO.getProcessId(), procVO);
          procVO.setName(newName);
          procVO.setVersion(1);
          new ProcessWorker().convert_to_designer(procVO);
          dataAccess.getDesignerDataAccess().updateProcess(procVO, 0, false);
          processVersion.setProcessVO(procVO);
        }
        else
        {
          processVersion.setName(newName);
          processVersion.getProcessVO().setVersion(1);
          processVersion.getProcessVO().setId(dataAccess.getDesignerDataAccess().renameProcess(processVersion.getId(), newName, 1));
        }
        dataAccess.getDesignerDataModel().addProcess(processVersion.getProcessVO());
      }
    };
    designerRunner.run();
  }

  public void deleteProcess(final WorkflowProcess processVersion, final boolean includeInstances)
  {
    String version = "v" + processVersion.getVersionString();
    String progressMsg = "Deleting '" + processVersion.getName() + "' " + version;
    String errorMsg = "Delete Process";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws DataAccessException, RemoteException, ValidationException
      {
        if (!includeInstances)
        {
          try
          {
            if (dataAccess.getDesignerDataAccess().hasProcessInstances(processVersion.getId()))
              throw new DataAccessException("Process " + processVersion.getLabel() + " has instances which also must be deleted.");
          }
          catch (DataAccessOfflineException ex)
          {
            final StringBuffer confirm = new StringBuffer();
            MdwPlugin.getDisplay().syncExec(new Runnable()
            {
              public void run()
              {
                String msg = "Cannot connect to server to check for instances.  Are you sure you want to delete?";
                confirm.append(MessageDialog.openConfirm(MdwPlugin.getShell(), "Delete Process", msg));
              }
            });
            if (!Boolean.valueOf(confirm.toString()))
              return;
          }
        }
        dataAccess.getDesignerDataAccess().removeProcess(processVersion.getProcessVO(), includeInstances);
        dataAccess.removeProcess(processVersion.getProcessVO());
        if (processVersion.isInDefaultPackage())
        {
          processVersion.getProject().getDefaultPackage().removeProcess(processVersion);
        }
        else
        {
          processVersion.getPackage().removeProcess(processVersion);
          if (!project.isFilePersist())
            savePackage(processVersion.getPackage());
        }
      }
    };
    designerRunner.run();
  }

  public void savePackage(WorkflowPackage packageVersion) throws DataAccessException, RemoteException, ValidationException
  {
    savePackage(packageVersion, false);
  }

  public void savePackage(WorkflowPackage packageVersion, boolean newVersion) throws DataAccessException, RemoteException, ValidationException
  {
    if (packageVersion.isDefaultPackage())
      return;

    CodeTimer timer = new CodeTimer("savePackage()");

    packageVersion.syncProcessVos();
    if (packageVersion.getPackageVO().getImplementors() != null)
    {
      // remove non-existent activity impls
      List<ActivityImplementorVO> toRemove = new ArrayList<ActivityImplementorVO>();
      for (ActivityImplementorVO activityImpl : packageVersion.getPackageVO().getImplementors())
      {
        if (dataAccess.getActivityImplementor(activityImpl.getImplementorClassName()) == null)
          toRemove.add(activityImpl);
      }
      for (ActivityImplementorVO remove : toRemove)
        packageVersion.getPackageVO().getImplementors().remove(remove);
    }

    WorkflowPackage prevPkg = new WorkflowPackage(packageVersion.getProject(), new PackageVO());
    prevPkg.getPackageVO().setId(packageVersion.getId());
    prevPkg.setName(packageVersion.getName());
    prevPkg.getPackageVO().setVersion(packageVersion.getVersion());
    prevPkg.setTags(packageVersion.getTags());
    prevPkg.setModifyDate(packageVersion.getModifyDate());
    if (packageVersion.getPackageVO() != null && packageVersion.getPackageVO().getAttributes() != null)
    {
      for(AttributeVO attr: packageVersion.getPackageVO().getAttributes())
      {
        prevPkg.setAttribute(attr.getAttributeName(), attr.getAttributeValue());
      }
    }

    Long pkgId = prevPkg.getId();
    if (!packageVersion.isLatest() || packageVersion.isArchived())  // do not create new version
      pkgId = dataAccess.getDesignerDataAccess().savePackage(packageVersion.getPackageVO(), ProcessPersister.PersistType.UPDATE);
    else
      pkgId = dataAccess.getDesignerDataAccess().savePackage(packageVersion.getPackageVO());
    if (prevPkg.getId() == null || newVersion || (pkgId.longValue() != prevPkg.getId().longValue()))
    {
      packageVersion.getPackageVO().setPackageId(pkgId);
      prevPkg.setArchived(true);
      dataAccess.getPackages(false).add(packageVersion.getPackageVO());
      packageVersion.getProject().newPackageVersion(packageVersion, prevPkg);
      // update the tree
      packageVersion.getProject().fireElementChangeEvent(packageVersion, ChangeType.VERSION_CHANGE, packageVersion.getVersionString());
      // update other listeners
      packageVersion.fireElementChangeEvent(ChangeType.VERSION_CHANGE, packageVersion.getVersionString());
    }

    timer.stopAndLog();
  }

  public void setPackage(WorkflowPackage workflowPackage) throws DataAccessException, RemoteException, ValidationException
  {
    if (workflowPackage.isDefaultPackage())
      return;

    CodeTimer timer = new CodeTimer("setPackage()");

    // version tags DO not carry over to incremented packages
    // packageVersion.removeAttribute(WorkAttributeConstant.VERSION_TAG);

    Long pkgId = dataAccess.getDesignerDataAccess().savePackage(workflowPackage.getPackageVO());
    workflowPackage.getPackageVO().setPackageId(pkgId);
    workflowPackage.getProject().fireElementChangeEvent(workflowPackage, ChangeType.VERSION_CHANGE, workflowPackage.getVersionString());
    workflowPackage.fireElementChangeEvent(ChangeType.VERSION_CHANGE, workflowPackage.getVersionString());
    timer.stopAndLog();
  }

  public void tagPackage(WorkflowPackage packageVersion, String tag) throws DataAccessException, RemoteException, ValidationException
  {
    if (!packageVersion.isExported())
    {
      // set exported flag
      packageVersion.setExported(true);
      dataAccess.setAttribute(OwnerType.PACKAGE, packageVersion.getId(), "EXPORTED_IND", "1");
      packageVersion.fireElementChangeEvent(ChangeType.STATUS_CHANGE, WorkflowPackage.STATUS_EXPORTED);
    }
    String existingTags = packageVersion.getAttribute(WorkAttributeConstant.VERSION_TAG);
    String newTags = (existingTags == null || existingTags.isEmpty()) ? tag : existingTags + "," + tag;
    packageVersion.setAttribute(WorkAttributeConstant.VERSION_TAG, newTags);
    dataAccess.setAttribute(OwnerType.PACKAGE, packageVersion.getId(), WorkAttributeConstant.VERSION_TAG, newTags);
    packageVersion.fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, packageVersion.getAttributeVO(WorkAttributeConstant.VERSION_TAG));
  }

  public void copyForeignProcess(final String name, final String version, final WorkflowProject sourceProject, final WorkflowPackage targetPackage)
  {
    String progressMsg = "Copying foreign process '" + name + "'";
    String errorMsg = "Copy Foreign Process";

    final Importer importer = new Importer(dataAccess, MdwPlugin.getShell());

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException, XmlException
      {
        Exporter exporter = new Exporter(sourceProject.getDesignerProxy().getDesignerDataAccess());
        String xml = exporter.exportProcess(name, version, false);
        try
        {
          WorkflowProcess newProc = importer.importProcess(targetPackage, null, xml);
          toggleProcessLock(newProc, true);
          if (!newProc.isInDefaultPackage())
            savePackage(newProc.getPackage());
        }
        catch (ActionCancelledException ex)
        {
          PluginMessages.log(ex);
        }
      }
    };
    designerRunner.run();
  }

  public String exportProcess(WorkflowProcess processVersion) throws DataAccessException, RemoteException, XmlException
  {
    return getDesignerDataAccess().exportProcess(processVersion.getId(), processVersion.getProject().isOldNamespaces());
  }

  public void saveProcessWithProgress(final WorkflowProcess process, final FlowchartPage flowchartPage, final PersistType persistType,
      final int version, final boolean validate, final boolean keepLocked)
  {
    String progressMsg = "Saving '" + process.getName() + "'";
    String errorMsg = "Save Process";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        WorkflowProcess oldProcess = null;
        if (persistType == PersistType.NEW_VERSION)
        {
          RuleSetVO existing = null;
          if (process.isInRuleSet())
            existing = dataAccess.getDesignerDataAccess().getRuleSet(process.getName(), RuleSetVO.PROCESS, version);
          if (existing != null)
            throw new DataAccessException("\n" + process.getLabel() + ALREADY_EXISTS);

          process.getProcessVO().setProcessId(-1L);
          // preserve the old version for the process tree
          ProcessVO oldProcessVoStub = new ProcessVO();
          oldProcessVoStub.setProcessId(process.getId());
          oldProcessVoStub.setProcessName(process.getName());
          oldProcessVoStub.setVersion(process.getProcessVO().getVersion());
          oldProcess = new WorkflowProcess(process.getProject(), oldProcessVoStub);
          oldProcess.setPackage(process.getPackage());
          oldProcess.setTopLevelVersion(process.getTopLevelVersion());
          if (project.isFilePersist())  // temp attribute to avoid API change
            process.setAttribute("previousProcessVersion", oldProcess.getVersionString());
        }
        saveProcess(process, flowchartPage, persistType, version, validate, keepLocked);
        if (process.getTopLevelVersion() != null)
          process.getTopLevelVersion().setProcessVO(process.getProcessVO());
        if (persistType == PersistType.NEW_VERSION && process.isInRuleSet())
        {
          RuleSetVO procRs = dataAccess.getRuleSet(process.getId());
          if (procRs == null)
            dataAccess.getAllRuleSets(false).add(process.getProcessVO());
        }
      }
    };
    designerRunner.run();
  }

  public RunnerResult forceUpdateProcessWithProgress(final WorkflowProcess processVersion, final FlowchartPage flowchartPage, final boolean validate, final boolean keepLocked)
  {
    String progressMsg = "Updating '" + processVersion.getName() + "'";
    String errorMsg = "Update Process";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        forceUpdateProcess(processVersion, flowchartPage, validate, keepLocked);
        if (processVersion.getTopLevelVersion() != null)
          processVersion.getTopLevelVersion().setProcessVO(processVersion.getProcessVO());
      }
    };
    return designerRunner.run();
  }

  /**
   * Relies on classic designer page to update the processVO with the reloaded data.
   */
  public void saveProcess(WorkflowProcess process, FlowchartPage flowchartPage, PersistType persistType, int version, boolean validate, boolean lock)
  throws ValidationException, DataAccessException, RemoteException
  {
    CodeTimer timer = new CodeTimer("saveProcess()");

    Graph graph = flowchartPage.getProcess();
    graph.save_temp_vars();

    // refresh variable type cache since Designer Classic uses a single, static cache
    dataAccess.loadVariableTypes(); // calls VariableTypeCache.refreshCache()

    if (validate)
      new ProcessValidator(process.getProcessVO()).validate(getNodeMetaInfo());

    if (!process.getProject().checkRequiredVersion(5, 5))
      flowchartPage.setProcessVersions(flowchartPage.getProcess(), !process.getProject().checkRequiredVersion(5, 2));  // not sure why this was ever needed

    Map<String,String> overrideAttributes = null;
    if (process.getProcessVO() != null && process.getProject().isFilePersist() && process.overrideAttributesApplied())
      overrideAttributes = process.getProcessVO().getOverrideAttributes();

    Graph reloaded = null;
    try
    {
      reloaded = flowchartPage.saveProcess(graph, mainFrame, persistType, version, lock);
    }
    catch (ValidationException ex)
    {
      graph.setDirtyLevel(Graph.DIRTY);
      if (ex.getMessage() != null && ex.getMessage().contains("ORA-02292"))
        throw new ValidationException("There are instances associated with this process version.\n       Please increment the version when saving.");
      else
        throw ex;
    }

    process.setProcessVO(reloaded.getProcessVO());

    flowchartPage.setProcess(reloaded);

    WorkflowPackage pkg = process.getPackage();
    if (pkg != null && !pkg.isDefaultPackage() && persistType == PersistType.NEW_VERSION)
    {
      if (project.isFilePersist())
      {
        pkg.setVersion(pkg.getVersion() + 1);
        pkg.setExported(false); // exported not relevant for vcs processes since pkg will be archived anyway
        savePackage(pkg, true);
      }
      else
      {
        savePackage(pkg);
      }
    }

    if (process.getProject().isFilePersist() && version != 0 && persistType == PersistType.NEW_VERSION && overrideAttributes != null && !overrideAttributes.isEmpty()
        && MdwPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREFS_CARRY_FORWARD_OVERRIDE_ATTRS))
    {
      if (!process.overrideAttributesApplied())
        throw new DataAccessException("Override attributes not applied");
      reloaded.getProcessVO().applyOverrideAttributes(overrideAttributes);
      getDesignerDataAccess().setOverrideAttributes(reloaded.getProcessVO());
    }

    if (version != 0 && process.getProject().isGitVcs() && process.getProject().isRemote())
    {
      IPreferenceStore prefsStore = MdwPlugin.getDefault().getPreferenceStore();
      boolean pushToGit = prefsStore.getBoolean(PreferenceConstants.PREFS_PUSH_TO_GIT_REMOTE_WHEN_SAVING);
      if (pushToGit)
      {
        try
        {
          commitAndPush(process);
          serverGit.pullProcess(process);
        }
        catch (Exception ex)
        {
          throw new DataAccessException(ex.getMessage(), ex);
        }
      }
    }

    cacheRefresh.fireRefresh(reloaded.getProcessVO().hasDynamicJavaActivity());

    timer.stopAndLog();
  }

  public static final String INCOMPATIBLE_INSTANCES = "Activity or transition instances exist which make it impossible to update this process.  Please increment the version when saving.";
  public static final String ALREADY_EXISTS = " already exists (created in another session).\nChanges NOT persisted.\n";

  /**
   * Relies on classic designer page to update the processVO with the reloaded data.
   */
  public void forceUpdateProcess(WorkflowProcess processVersion, FlowchartPage flowchartPage, boolean validate, boolean lock)
  throws ValidationException, DataAccessException, RemoteException
  {
    CodeTimer timer = new CodeTimer("forceUpdateProcess()");

    Graph graph = flowchartPage.getProcess();
    graph.save_temp_vars();
    if (validate)
      new ProcessValidator(processVersion.getProcessVO()).validate(getNodeMetaInfo());
    if (!processVersion.getProject().checkRequiredVersion(5, 5))
      flowchartPage.setProcessVersions(flowchartPage.getProcess(), !processVersion.getProject().checkRequiredVersion(5, 2));  // not sure why this was ever needed
    Graph reloaded = null;
    try
    {
      reloaded = flowchartPage.saveProcess(graph, mainFrame, PersistType.UPDATE, 0, lock);
    }
    catch (ValidationException ex)
    {
      if (ex.getMessage() != null && ex.getMessage().contains("ORA-02292"))
        throw new ValidationException(INCOMPATIBLE_INSTANCES);
      else
        throw ex;
    }

    flowchartPage.setProcess(reloaded);
    processVersion.setProcessVO(reloaded.getProcessVO());
    cacheRefresh.fireRefresh(reloaded.getProcessVO().hasDynamicJavaActivity());

    timer.stopAndLog();
  }

  public boolean hasInstances(WorkflowProcess processVersion) throws DataAccessException, RemoteException
  {
    return dataAccess.getDesignerDataAccess().hasProcessInstances(processVersion.getId());
  }

  public ProcessList getProcessInstanceList(WorkflowProcess processVersion, Map<String,String> criteria, Map<String,String> variables, int pageIndex, int pageSize, String orderBy)
  {
    CodeTimer timer = new CodeTimer("getProcessInstanceList()");
    try
    {
      Long id = processVersion.getId();
      if (id == 0)
      {
        // cross-version query by name
        String name;
        if (project.isFilePersist())
          name = processVersion.getPackage().getName() + "/" + processVersion.getName();  // qualified name
        else
          name = processVersion.getName();

        return dataAccess.getDesignerDataAccess().getProcessInstanceList(name, criteria, variables, pageIndex, pageSize, orderBy);
      }
      else
      {
        return dataAccess.getDesignerDataAccess().getProcessInstanceList(id, criteria, variables, pageIndex, pageSize, orderBy);
      }
    }
    catch (DataAccessOfflineException ex)
    {
      PluginMessages.log(ex);
      String msg = "Server appears to be offline: " + ex.getMessage();
      MessageDialog.openWarning(MdwPlugin.getShell(), "Retrieve Process Instances", msg);
      return new ProcessList(ProcessList.PROCESS_INSTANCES, new ArrayList<ProcessInstanceVO>());
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Retrieve Process Instances", project);
      return new ProcessList(ProcessList.PROCESS_INSTANCES, new ArrayList<ProcessInstanceVO>());
    }
    finally
    {
      timer.stopAndLog();
    }
  }

  public List<ProcessInstanceVO> getProcessInstances(Map<String,String> criteria) throws DataAccessException, RemoteException
  {
    CodeTimer timer = new CodeTimer("getProcessInstances()");
    try
    {
      return dataAccess.getDesignerDataAccess().getProcessInstanceList(criteria, 1, 50, new ProcessVO(), "order by process_instance_id desc").getItems();
    }
    finally
    {
      timer.stopAndLog();
    }
  }

  public RunTimeDesignerCanvas loadProcessInstance(WorkflowProcess processVersion, ProcessInstancePage processInstancePage)
  {
    CodeTimer timer = new CodeTimer("loadProcessInstance()");

    ProcessInstanceVO processInstanceInfo = processVersion.getProcessInstance();

    ProcessInstanceLoader instanceLoadThread =
      new ProcessInstanceLoader(processInstanceInfo, processInstancePage);

    String errorMessage = null;

    try
    {
      Graph processGraph = dataAccess.getDesignerDataModel().findProcessGraph(processVersion.getId(), null);
      if (processGraph == null)
        processGraph = processInstancePage.loadProcess(processInstanceInfo.getProcessId(), null);
      processVersion.setProcessVO(processGraph.getProcessVO());

      DesignerDataAccess dao = processVersion.getDesignerDataAccess() == null ? processInstancePage.frame.dao : processVersion.getDesignerDataAccess();
      Graph procInstGraph = instanceLoadThread.loadCompletionMap(processGraph.getProcessVO(), processInstanceInfo, dao);
      ProcessInstanceTreeModel model = instanceLoadThread.createOrUpdateModel(null);
      model.getCurrentProcess().setGraph(procInstGraph);
      processInstancePage.setData(model, processInstancePage);

      Map<Long,List<TaskInstanceVO>> taskInstances = new HashMap<Long,List<TaskInstanceVO>>();

      // embedded subprocesses
      List<ProcessInstanceVO> embeddedSubs = new ArrayList<ProcessInstanceVO>();
      if (procInstGraph.subgraphs != null)
      {
        for (SubGraph instSubGraph : procInstGraph.subgraphs)
        {
          if (instSubGraph.getInstances() != null)
          {
            if (instSubGraph.nodes != null)
            {
              List<Long> taskActivityIds = new ArrayList<Long>();
              for (Node node : instSubGraph.nodes)
              {
                if (node.isTaskActivity())
                {
                  for (ProcessInstanceVO embeddedProcInst : instSubGraph.getInstances())
                  {
                    if (!embeddedProcInst.getActivityInstances(node.getActivityId()).isEmpty())
                      taskActivityIds.add(node.getActivityId());
                  }
                }
              }
              if (!taskActivityIds.isEmpty())
              {
                for (ProcessInstanceVO embeddedProcInst : instSubGraph.getInstances())
                {
                  ProcessVO embeddedProc = instSubGraph.getGraph().getProcessVO();
                  Map<Long,List<TaskInstanceVO>> embeddedTaskInsts = dao.getTaskInstances(embeddedProc, embeddedProcInst, taskActivityIds);
                  for (Long actId : embeddedTaskInsts.keySet())
                  {
                    if (taskInstances.get(actId) == null)
                      taskInstances.put(actId, new ArrayList<TaskInstanceVO>());
                    taskInstances.get(actId).addAll(embeddedTaskInsts.get(actId));
                  }
                }
              }
            }
            embeddedSubs.addAll(instSubGraph.getInstances());
          }
        }
      }
      processVersion.setEmbeddedSubProcessInstances(embeddedSubs);

      // manual task instances
      if (processGraph.nodes != null)
      {
        List<Long> taskActivityIds = new ArrayList<Long>();
        for (Node node : processGraph.nodes)
        {
          if (node.isTaskActivity() && !processInstanceInfo.getActivityInstances(node.getActivityId()).isEmpty())
            taskActivityIds.add(node.getActivityId());
        }
        if (!taskActivityIds.isEmpty())
        {
          Map<Long,List<TaskInstanceVO>> taskInsts = dao.getTaskInstances(processVersion.getProcessVO(), processInstanceInfo, taskActivityIds);
          for (Long actId : taskInsts.keySet())
          {
            if (taskInstances.get(actId) == null)
              taskInstances.put(actId, new ArrayList<TaskInstanceVO>());
            taskInstances.get(actId).addAll(taskInsts.get(actId));
          }
        }
      }

      processVersion.setTaskInstances(taskInstances);
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
      errorMessage = PluginMessages.getUserMessage(ex);
    }
    if (errorMessage == null)
      errorMessage = instanceLoadThread.getErrorMessage();

    if (errorMessage != null)
    {
      PluginMessages.uiError(errorMessage, "Load Process Instance");
    }

    timer.stopAndLog();
    return processInstancePage.canvas;
  }

  public WorkflowAsset loadWorkflowAsset(WorkflowAsset asset)
  {
    try
    {
      RuleSetVO ruleSetVO = dataAccess.getDesignerDataAccess().getRuleSet(asset.getId());
      asset.setName(ruleSetVO.getName());
      asset.setLanguage(ruleSetVO.getLanguage());
      asset.setContent(ruleSetVO.getRuleSet());
      asset.setModifyDate(ruleSetVO.getModifyDate());
      asset.setLockingUser(ruleSetVO.getModifyingUser());
      asset.setVersion(ruleSetVO.getVersion());
      asset.setComment(ruleSetVO.getComment());
      if (!(asset instanceof TaskTemplate)) // don't overwrite task attributes
        asset.setAttributes(ruleSetVO.getAttributes());
      return asset;
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Load Definition Doc", project);
      return null;
    }
  }

  public List<ProcessInstanceVO> getSubProcessInstances(WorkflowProcess parentProcess, Activity activity)
  {
    try
    {
      Long parentProcessInstanceId = parentProcess.getProcessInstance().getId();
      if (activity.isHeterogeneousSubProcInvoke())
      {
        List<ProcessInstanceVO> insts = new ArrayList<ProcessInstanceVO>();
        String procMapStr = activity.getAttribute(WorkAttributeConstant.PROCESS_MAP);
        if (procMapStr != null && !procMapStr.isEmpty())
        {
          List<String[]> procMap = StringHelper.parseTable(procMapStr, ',', ';', 3);
          for (String[] row : procMap)
          {
            ProcessVO subProcessVO = new ProcessVO();
            AssetVersionSpec spec = new AssetVersionSpec(row[1], row[2] == null ? "0" : row[2]);
            AssetLocator locator = new AssetLocator(activity, AssetLocator.Type.Process);
            WorkflowProcess found = locator.getProcessVersion(spec);
            if (found != null)
            {
              subProcessVO.setProcessId(found.getId());
              subProcessVO.setProcessName(found.getName());
              insts.addAll(dataAccess.getDesignerDataAccess().getChildProcessInstance(parentProcessInstanceId, subProcessVO, parentProcess.getProcessVO()));
            }
            else
            {
              PluginMessages.log(new Exception("SubProcess not found: " + row[1] + " v" + row[2]));
            }
          }
        }
        return insts;
      }
      else if (activity.isManualTask())
      {
        List<ProcessInstanceVO> insts = new ArrayList<ProcessInstanceVO>();
        String procMapStr = activity.getAttribute(TaskAttributeConstant.SERVICE_PROCESSES);
        if (procMapStr != null && !procMapStr.isEmpty())
        {
          Map<String,String> pMap = new HashMap<String,String>();
          pMap.put("owner", OwnerType.TASK_INSTANCE);
          StringBuffer sb = new StringBuffer();
          sb.append("(");
          if (activity.getTaskInstances() != null)
          {
            for (TaskInstanceVO taskInst : activity.getTaskInstances())
            {
              if (sb.length() > 1)
                sb.append(",");
              sb.append(taskInst.getTaskInstanceId().toString());
            }
          }
          sb.append(")");
          pMap.put("ownerIdList", sb.toString());
          insts = dataAccess.getDesignerDataAccess().getProcessInstanceList(pMap, 0, QueryRequest.ALL_ROWS, parentProcess.getProcessVO(), null).getItems();
        }
        return insts;
      }
      else
      {
        ProcessVO subProcessVO = new ProcessVO();
        String subProcName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
        String subProcVer = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
        AssetVersionSpec spec = new AssetVersionSpec(subProcName, subProcVer == null ? "0" : subProcVer);
        AssetLocator locator = new AssetLocator(activity, AssetLocator.Type.Process);
        WorkflowProcess subProc = locator.getProcessVersion(spec);
        subProcessVO.setProcessId((subProc == null || subProc.getId() == null) ? 0L : subProc.getId());
        subProcessVO.setProcessName(activity.getAttribute(WorkAttributeConstant.PROCESS_NAME));

        // handle alias subprocs
        String subprocAliasProcessId = activity.getAttribute(WorkAttributeConstant.ALIAS_PROCESS_ID);
        if (subprocAliasProcessId != null)
          subProcessVO = this.getProcessVO(new Long(subprocAliasProcessId));

        return dataAccess.getDesignerDataAccess().getChildProcessInstance(parentProcessInstanceId, subProcessVO, parentProcess.getProcessVO());
      }
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Load SubProcess Instances (P=" + parentProcess.getId() + ",A=" + activity.getId() + ")", project);
      return null;
    }
  }

  public List<ProcessInstanceVO> getSubProcessInstances(ProcessInstanceVO parentEmbeddedProcessInstance, Activity activity)
  {
    try
    {
      Long parentProcessInstanceId = parentEmbeddedProcessInstance.getId();
      ProcessVO subProcessVO = new ProcessVO();
      String subProcName = activity.getAttribute(WorkAttributeConstant.PROCESS_NAME);
      String subProcVer = activity.getAttribute(WorkAttributeConstant.PROCESS_VERSION);
      WorkflowProcess subProc = new AssetLocator(activity, AssetLocator.Type.Process).getProcessVersion(new AssetVersionSpec(subProcName, subProcVer));
      subProcessVO.setProcessId(subProc == null ? 0L : subProc.getId());
      subProcessVO.setProcessName(subProcName);

      return dataAccess.getDesignerDataAccess().getChildProcessInstance(parentProcessInstanceId, subProcessVO, new ProcessVO());
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Load SubProcess Instances", project);
      return null;
    }
  }

  public DocumentVO getDocument(DocumentReference docRef)
  {
    try
    {
      return getDesignerDataAccess().getDocument(docRef.getDocumentId());
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
      return null;
    }
  }

  public void moveProcessToPackage(final String processName, final String version, final WorkflowPackage targetPackage)
  {
    String progressMsg = "Moving '" + processName + " v " + version + "' to package '" + targetPackage.getName() + "'";
    String errorMsg = "Move Process";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        WorkflowProject workflowProject = targetPackage.getProject();
        WorkflowProcess processVersion = workflowProject.getProcess(processName, version);

        if (processVersion.isInDefaultPackage())
        {
          if (processVersion.isInRuleSet())
            dataAccess.getDesignerDataAccess().addRuleSetToPackage(processVersion.getProcessVO(), targetPackage.getPackageVO());
          else
            dataAccess.getDesignerDataAccess().addProcessToPackage(processVersion.getProcessVO(), targetPackage.getPackageVO());
          WorkflowPackage defaultPackage = workflowProject.getDefaultPackage();
          defaultPackage.removeProcess(processVersion);
        }
        else
        {
          if (processVersion.isInRuleSet())
          {
            // id can change from repackaging for VCS assets
            Long newId = dataAccess.getDesignerDataAccess().addRuleSetToPackage(processVersion.getProcessVO(), targetPackage.getPackageVO());
            dataAccess.getDesignerDataAccess().removeRuleSetFromPackage(processVersion.getProcessVO(), processVersion.getPackage().getPackageVO());
            processVersion.getProcessVO().setId(newId);
            processVersion.getPackage().removeProcess(processVersion);
          }
          else
          {
            dataAccess.getDesignerDataAccess().addProcessToPackage(processVersion.getProcessVO(), targetPackage.getPackageVO());
            dataAccess.getDesignerDataAccess().removeProcessFromPackage(processVersion.getProcessVO(), processVersion.getPackage().getPackageVO());
            processVersion.getPackage().removeProcess(processVersion);
          }
        }

        targetPackage.addProcess(processVersion);
      }
    };
    designerRunner.run();
  }

  public void removeProcessFromPackage(final String processName, final String version, final WorkflowPackage packageVersion)
  {
    String progressMsg = "Removing '" + processName + " v " + version + "' from package '" + packageVersion.getName() + "'";
    String errorMsg = "Remove Process from Package";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        WorkflowProcess processVersion = project.getProcess(processName, version);

        if (processVersion.isInRuleSet())
          dataAccess.getDesignerDataAccess().removeRuleSetFromPackage(processVersion.getProcessVO(), processVersion.getPackage().getPackageVO());
        else
          dataAccess.getDesignerDataAccess().removeProcessFromPackage(processVersion.getProcessVO(), packageVersion.getPackageVO());

        packageVersion.removeProcess(processVersion);
        WorkflowPackage defaultPackage = project.getDefaultPackage();
        defaultPackage.addProcess(processVersion);
      }
    };
    designerRunner.run();
  }

  public void copyProcess(final String originalName, final String originalVersion, final String newName, final WorkflowPackage targetPackage)
  {

    String progressMsg = "Creating process '" + newName + "'\nin package '" + targetPackage.getName() + "'";
    String errorMsg = "Create Process";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        int dotIdx = originalVersion.indexOf('.');
        int major = Integer.parseInt(originalVersion.substring(0, dotIdx));
        int minor = Integer.parseInt(originalVersion.substring(dotIdx + 1));
        ProcessVO origProcVO = dataAccess.getDesignerDataAccess().getProcessDefinition(originalName, major*1000 + minor);
        origProcVO = dataAccess.getDesignerDataAccess().getProcess(origProcVO.getProcessId(), origProcVO);
        new ProcessWorker().convert_to_designer(origProcVO);

        ProcessVO newProcVO = new ProcessVO(-1L, newName, origProcVO.getProcessDescription(), null);
        newProcVO.set(origProcVO.getAttributes(), origProcVO.getVariables(), origProcVO.getTransitions(), origProcVO.getSubProcesses(), origProcVO.getActivities());
        newProcVO.setVersion(1);
        newProcVO.setInRuleSet(origProcVO.isInRuleSet());
        WorkflowProcess newProcess = new WorkflowProcess(targetPackage.getProject(), newProcVO);
        newProcess.setPackage(targetPackage);

        Graph process = new Graph(newProcVO, dataAccess.getDesignerDataModel().getNodeMetaInfo(), getIconFactory());
        process.dirtyLevel = Graph.NEW;
        // mainFrame.procmenu.dataChanged(new DataChangeEvent(DataChangeEvent.DEFINITION_GRAPH_ADD, process, false));
        FlowchartPage flowchartPage = FlowchartPage.newPage(mainFrame);
        flowchartPage.setProcess(process);

        saveProcess(newProcess, flowchartPage, PersistType.CREATE, 0, false, false);
        toggleProcessLock(newProcess, true);
        newProcess.getProcessVO().setVersion(1);  // why?

        dataAccess.getProcesses(false).add(newProcess.getProcessVO());

        targetPackage.addProcess(newProcess);
        newProcess.setPackage(targetPackage);

        if (!newProcess.isInDefaultPackage())
          savePackage(newProcess.getPackage());
      }
    };
    designerRunner.run();
  }

  public void moveExternalEventToPackage(final Long externalEventId, final WorkflowPackage targetPackage)
  {
    String progressMsg = "Moving external event id '" + externalEventId + "' to package '" + targetPackage.getName() + "'";
    String errorMsg = "Move External Event";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        WorkflowProject workflowProject = targetPackage.getProject();
        ExternalEvent externalEvent = workflowProject.getExternalEvent(externalEventId);

        if (externalEvent.isInDefaultPackage())
        {
          dataAccess.getDesignerDataAccess().addExternalEventToPackage(externalEvent.getExternalEventVO(), targetPackage.getPackageVO());
          WorkflowPackage defaultPackage = workflowProject.getDefaultPackage();
          defaultPackage.removeExternalEvent(externalEvent);
        }
        else
        {
          // id can change from repackaging for VCS assets
          Long newId = dataAccess.getDesignerDataAccess().addExternalEventToPackage(externalEvent.getExternalEventVO(), targetPackage.getPackageVO());
          dataAccess.getDesignerDataAccess().removeExternalEventFromPackage(externalEvent.getExternalEventVO(), externalEvent.getPackage().getPackageVO());
          externalEvent.getExternalEventVO().setId(newId);
          externalEvent.getPackage().removeExternalEvent(externalEvent);
        }

        targetPackage.addExternalEvent(externalEvent);
      }
    };
    designerRunner.run();
  }

  public void removeExternalEventFromPackage(final Long externalEventId, final WorkflowPackage packageVersion)
  {
    String progressMsg = "Removing external event id '" + externalEventId + "' from package '" + packageVersion.getName() + "'";
    String errorMsg = "Remove External Event from Package";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        ExternalEvent externalEvent = project.getExternalEvent(externalEventId);
        dataAccess.getDesignerDataAccess().removeExternalEventFromPackage(externalEvent.getExternalEventVO(), packageVersion.getPackageVO());
        packageVersion.removeExternalEvent(externalEvent);
        WorkflowPackage defaultPackage = project.getDefaultPackage();
        defaultPackage.addExternalEvent(externalEvent);
      }
    };
    designerRunner.run();
  }

  public void addActivityImplToPackage(final Long activityImplId, final WorkflowPackage targetPackage)
  {
    String progressMsg = "Adding activity implementor id '" + activityImplId + "' to package '" + targetPackage.getName() + "'";
    String errorMsg = "Package Activity Implementor";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        WorkflowProject workflowProject = targetPackage.getProject();
        ActivityImpl activityImpl = workflowProject.getActivityImpl(activityImplId);
        dataAccess.getDesignerDataAccess().addActivityImplToPackage(activityImpl.getActivityImplVO(), targetPackage.getPackageVO());
        targetPackage.addActivityImpl(activityImpl);
      }
    };
    designerRunner.run();
  }

  public void removeActivityImplFromPackage(final Long activityImplId, final WorkflowPackage packageVersion)
  {
    String progressMsg = "Removing activity implementor id '" + activityImplId + "' from package '" + (packageVersion == null ? "(default package)" : packageVersion.getName()) + "'";
    String errorMsg = "Remove Activity Impl from Package";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        ActivityImpl activityImpl = project.getActivityImpl(activityImplId);
        WorkflowPackage defaultPackage = project.getDefaultPackage();
        if (packageVersion == null) // default
        {
          defaultPackage.removeActivityImpl(activityImpl);
        }
        else
        {
          dataAccess.getDesignerDataAccess().removeActivityImplFromPackage(activityImpl.getActivityImplVO(), packageVersion.getPackageVO());
          packageVersion.removeActivityImpl(activityImpl);
          boolean inOtherPackage = false;
          for (WorkflowPackage pkg : project.getTopLevelUserVisiblePackages())
          {
            if (pkg.getPackageVO().containsActivityImpl(activityImplId))
            {
              inOtherPackage = true;
              break;
            }
          }
          if (!inOtherPackage)
          {
            defaultPackage.addActivityImpl(activityImpl);
          }
        }
      }
    };
    designerRunner.run();
  }

  public void moveWorkflowAssetToPackage(final Long assetId, final WorkflowPackage targetPackage)
  {
    String progressMsg = "Moving Asset ID: " + assetId + " to package '" + targetPackage.getName() + "'";
    String errorMsg = "Move Workflow Asset";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        WorkflowProject workflowProject = targetPackage.getProject();
        WorkflowAsset asset = workflowProject.getAsset(assetId);

        if (asset.isInDefaultPackage())
        {
          dataAccess.getDesignerDataAccess().addRuleSetToPackage(asset.getRuleSetVO(), targetPackage.getPackageVO());
          WorkflowPackage defaultPackage = workflowProject.getDefaultPackage();
          defaultPackage.removeAsset(asset);
        }
        else
        {
          // id can change from repackaging for VCS assets
          Long newId = dataAccess.getDesignerDataAccess().addRuleSetToPackage(asset.getRuleSetVO(), targetPackage.getPackageVO());
          dataAccess.getDesignerDataAccess().removeRuleSetFromPackage(asset.getRuleSetVO(), asset.getPackage().getPackageVO());
          asset.setId(newId);
          asset.getPackage().removeAsset(asset);
        }

        targetPackage.addAsset(asset);
      }
    };
    designerRunner.run();
  }

  public void removeWorkflowAssetFromPackage(final Long assetId, final WorkflowPackage packageVersion)
  {
    String progressMsg = "Removing Asset ID: " + assetId + " from package '" + packageVersion.getName() + "'";
    String errorMsg = "Remove from Package";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        WorkflowAsset asset = project.getAsset(assetId);
        dataAccess.getDesignerDataAccess().removeRuleSetFromPackage(asset.getRuleSetVO(), packageVersion.getPackageVO());
        packageVersion.removeAsset(asset);
        WorkflowPackage defaultPackage = project.getDefaultPackage();
        defaultPackage.addAsset(asset);
      }
    };
    designerRunner.run();
  }

  public void updateProcessesAndAssetsToLatest(final WorkflowPackage packageVersion)
  {
    String progressMsg = "Updating package '" + packageVersion.getLabel() + "' to contain latest processes and workflow assets.";
    String errorMsg = "Update Package";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        List<WorkflowProcess> pkgProcesses = new ArrayList<WorkflowProcess>();
        pkgProcesses.addAll(packageVersion.getProcesses());
        for (WorkflowProcess inProc : pkgProcesses)
        {
          WorkflowProcess latestProc = inProc.getAllProcessVersions().get(0);
          if (!inProc.equals(latestProc))
          {
            if (inProc.isInRuleSet())
            {
              dataAccess.getDesignerDataAccess().removeRuleSetFromPackage(inProc.getProcessVO(), packageVersion.getPackageVO());
              packageVersion.removeProcess(inProc);
              dataAccess.getDesignerDataAccess().addRuleSetToPackage(latestProc.getProcessVO(), packageVersion.getPackageVO());
              packageVersion.addProcess(latestProc);
            }
            else
            {
              dataAccess.getDesignerDataAccess().removeProcessFromPackage(inProc.getProcessVO(), packageVersion.getPackageVO());
              packageVersion.removeProcess(inProc);
              dataAccess.getDesignerDataAccess().addProcessToPackage(latestProc.getProcessVO(), packageVersion.getPackageVO());
              packageVersion.addProcess(latestProc);
            }
          }
        }
        List<WorkflowAsset> pkgAssets = new ArrayList<WorkflowAsset>();
        pkgAssets.addAll(packageVersion.getAssets());
        for (WorkflowAsset inAsset : pkgAssets)
        {
          WorkflowAsset latestAsset = inAsset.getAllVersions().get(0);
          if (!inAsset.equals(latestAsset))
          {
            dataAccess.getDesignerDataAccess().removeRuleSetFromPackage(inAsset.getRuleSetVO(), packageVersion.getPackageVO());
            packageVersion.removeAsset(inAsset);
            dataAccess.getDesignerDataAccess().addRuleSetToPackage(latestAsset.getRuleSetVO(), packageVersion.getPackageVO());
            packageVersion.addAsset(latestAsset);
          }
        }
      }
    };
    designerRunner.run();
  }

  /**
   * Replace obsolete implementors, and other assets (see help doc upgradeAssetsDuringImport.html).
   */
  public void upgradeAssets(WorkflowPackage packageVersion) throws DataAccessException, IOException, RemoteException
  {
    boolean packageUpdated = false;

    PackageVO packageVO = packageVersion.getPackageVO();
    List<ProcessVO> processVOs = packageVO.getProcesses();

    // update activity implementors
    List<ProcessVO> newProcs = new ArrayList<ProcessVO>();
    for (ProcessVO processVO : processVOs)
    {
      boolean processUpdated = false;

      ProcessVO newProc = dataAccess.getDesignerDataAccess().getProcess(processVO.getProcessId(), processVO);
      List<ActivityVO> activities = newProc.getActivities();

      if (activities != null)
      {
        for (ActivityVO activityVO : activities)
        {
          if (new ActivityUpgrader(activityVO).doUpgrade())
            processUpdated = true;
        }
        if (newProc.getSubProcesses() != null)
        {
          for (ProcessVO subproc : newProc.getSubProcesses())
          {
            if (subproc.getActivities() != null)
            {
              for (ActivityVO subprocActivity : subproc.getActivities())
              {
                if (new ActivityUpgrader(subprocActivity).doUpgrade())
                  processUpdated = true;
              }
            }
          }
        }
      }

      // update variable types
      List<VariableVO> variables = newProc.getVariables();
      if (variables != null)
      {
        for (VariableVO variableVO : variables)
        {
          String variableType = variableVO.getVariableType();
          String updatedVariableType = Compatibility.getVariableType(variableType);
          if (!updatedVariableType.equals(variableType))
          {
            variableVO.setVariableType(updatedVariableType);
            processUpdated = true;
          }
        }
      }

      if (processUpdated)
      {
        int processVersion = newProc.getVersion();
        processVersion++;
        newProc.setVersion(processVersion);
        packageUpdated = true;
      }
      newProcs.add(newProc);
    }

    // Set old activity implementors in the package to hidden
    List<ActivityImplementorVO> activityImplementorVOs = packageVO.getImplementors();
    for (ActivityImplementorVO activityImplementorVO : activityImplementorVOs)
    {
      String activityImplClassName = activityImplementorVO.getImplementorClassName();
      if (Compatibility.isOldImplementor(activityImplClassName))
      {
        activityImplementorVO.setHidden(true);
        packageUpdated = true;
      }
    }

    if (packageUpdated)
    {
      // update with new assets for saving
      packageVO.setProcesses(newProcs);
      List<RuleSetVO> newRuleSets = new ArrayList<RuleSetVO>();
      for (RuleSetVO ruleSet : packageVO.getRuleSets())
        newRuleSets.add(getDesignerDataAccess().getRuleSet(ruleSet.getId()));
      packageVO.setRuleSets(newRuleSets);

      int version = packageVersion.getVersion();
      version++;
      packageVersion.setVersion(version);
      packageVersion.setExported(false);  // avoid forcing version increment on save
      packageVersion.syncProcesses();
      getDesignerDataAccess().savePackage(packageVO, ProcessPersister.PersistType.IMPORT);
    }
  }

  public List<WorkflowProcess> findCallingProcesses(WorkflowProcess subproc) throws DataAccessException, RemoteException
  {
    List<WorkflowProcess> callers = new ArrayList<WorkflowProcess>();
    List<ProcessVO> callingProcVos = dataAccess.getDesignerDataAccess().findCallingProcesses(subproc.getProcessVO());

    for (ProcessVO procVo : callingProcVos)
    {
      WorkflowProcess processVersion = project.getProcess(procVo.getProcessId());
      if (processVersion == null)  // might not be loaded in tree
        processVersion = new WorkflowProcess(project, procVo);
      callers.add(processVersion);

    }
    return callers;
  }

  public List<WorkflowProcess> findCalledProcesses(WorkflowProcess mainproc) throws DataAccessException, RemoteException
  {
    List<WorkflowProcess> called = new ArrayList<WorkflowProcess>();
    List<ProcessVO> calledProcVos = dataAccess.getDesignerDataAccess().findCalledProcesses(mainproc.getProcessVO());

    for (ProcessVO procVo : calledProcVos)
    {
      WorkflowProcess processVersion = project.getProcess(procVo.getProcessId());
      if (processVersion == null)  // might not be loaded in tree
        processVersion = new WorkflowProcess(project, procVo);
      called.add(processVersion);

    }
    return called;
  }

  public LinkedProcessInstance getProcessInstanceCallHierarchy(ProcessInstanceVO procInst) throws DataAccessException, RemoteException
  {
    return dataAccess.getDesignerDataAccess().getProcessInstanceCallHierarchy(procInst);
  }

  public void updateVariableValue(final WorkflowProcess processVersion, final VariableInstanceInfo varInstInfo, final String newValue)
  {
    String progressMsg = "Updating variable value for '" + varInstInfo.getName() + "'";
    String errorMsg = "Update Variable Value";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        VariableTranslator vt = getVariableTranslator(varInstInfo.getType());
        boolean isDoc = vt instanceof DocumentReferenceTranslator;
        boolean isJavaObj = Object.class.getName().equals(varInstInfo.getType());
        if (project.isFilePersist() || vt == null || varInstInfo.getInstanceId() == null || isJavaObj)
        {
          // vcs assets or unknown translator or new instance or java object (go to server)
          try
          {
            DocumentReference docRef = varInstInfo.getStringValue() != null && varInstInfo.getStringValue().startsWith("DOCUMENT:") ? new DocumentReference(varInstInfo.getStringValue()) : null;
            varInstInfo.setStringValue(String.valueOf(processVersion.getProcessInstance().getId()));
            dataAccess.getDesignerDataAccess().updateVariableInstanceThruServer(varInstInfo, newValue, isDoc);
            if (docRef != null)
              varInstInfo.setStringValue(docRef.toString()); // restore doc ref value
            else
              varInstInfo.setStringValue(newValue);
          }
          catch (DataAccessException ex)
          {
            if (ex.getCause() instanceof java.net.ConnectException)
            {
              PluginMessages.log(ex);
              throw new DataAccessException("Updating this variable requires that your server is running and accessible.");
            }
            else
            {
              throw ex;
            }
          }
        }
        else
        {
          dataAccess.getDesignerDataAccess().updateVariableInstanceInDb(varInstInfo, newValue, isDoc);
        }
      }
    };
    designerRunner.run();
  }

  /**
   * Must be called from the UI thread.
   */
  public VariableTypeVO getVariableInfo(final VariableInstanceInfo varInstInfo)
  throws DataAccessException, RemoteException, ConnectException
  {
    String varType = varInstInfo.getType();
    VariableTranslator varTrans = getVariableTranslator(varType);
    final VariableTypeVO varTypeVO = new VariableTypeVO(0L, varType, varTrans == null ? null : varTrans.getClass().getName());
    if (varInstInfo.getStringValue() != null && varTypeVO.isJavaObjectType())
    {
      BusyIndicator.showWhile(MdwPlugin.getShell().getDisplay(), new Runnable()
      {
        public void run()
        {
          DocumentReference docRef = new DocumentReference(varInstInfo.getStringValue());
          try
          {
            Object obj = new JavaObjectTranslator().realToObject(getDocument(docRef).getContent());
            varTypeVO.setVariableType(obj.getClass().getName());
            varTypeVO.setUpdateable(obj instanceof SelfSerializable);
          }
          catch (TranslationException ex)
          {
            if (MdwPlugin.getSettings().isLogConnectErrors())
                PluginMessages.log(ex);
            try
            {
              String resp = getRestfulServer().invokeResourceService("DocumentValue?format=xml&id=" + docRef.getDocumentId() + "&type=" + varTypeVO.getVariableType());
              Resource res = Resource.Factory.parse(resp, Compatibility.namespaceOptions());
              for (Parameter param : res.getParameterList()) {
                if ("className".equals(param.getName()))
                  varTypeVO.setVariableType(param.getStringValue());
                else if ("isUpdateable".equals(param.getName()))
                  varTypeVO.setUpdateable(Boolean.parseBoolean(param.getStringValue()));
              }
            }
            catch (Exception ex2)
            {
              throw new RuntimeException(ex2.getMessage(), ex2);
            }
          }
        }
      });
    }
    return varTypeVO;
  }

  /**
   * Must be called from the UI thread.
   */
  public String getVariableValue(final Shell shell, final VariableInstanceInfo varInstInfo, final boolean tryServer)
  throws DataAccessException, RemoteException, ConnectException
  {
    final StringBuffer valueHolder = new StringBuffer();
    BusyIndicator.showWhile(shell.getDisplay(), new Runnable()
    {
      public void run()
      {
        String value = varInstInfo.getStringValue();
        if (value != null)
        {
          boolean isDoc = isDocumentVariable(varInstInfo.getType(), value);
          if (isDoc)
          {
            DocumentReference docRef = new DocumentReference(value);
            value = getDocument(docRef).getContent();
            boolean isJavaObj = Object.class.getName().equals(varInstInfo.getType());
            if (isJavaObj)
            {
              try
              {
                value = new JavaObjectTranslator().realToObject(value).toString();
              }
              catch (TranslationException ex)
              {
                if (MdwPlugin.getSettings().isLogConnectErrors())
                  PluginMessages.log(ex);
                if (tryServer)
                {
                  try
                  {
                    value = getRestfulServer().invokeResourceService("DocumentValue?id=" + docRef.getDocumentId() + "&type=" + varInstInfo.getType() + "&format=text");
                  }
                  catch (Exception ex2)
                  {
                    throw new RuntimeException(ex2.getMessage(), ex2);
                  }
                }
                else
                {
                  value = "<Double-click to retrieve>";
                }
              }
            }
          }
          valueHolder.append(value);
        }
      }
    });
    return valueHolder.toString();
  }

  private boolean isDocumentVariable(String type, String value)
  {
    VariableTranslator translator;
    VariableTypeVO vo = dataAccess.getVariableType(type);
    if (vo == null)
      return false;
    try
    {
      Class<?> cl = Class.forName(vo.getTranslatorClass());
      translator = (VariableTranslator)cl.newInstance();
      return (translator instanceof DocumentReferenceTranslator);
    }
    catch (Exception e)
    {
      if (value == null)
        return false;
      return value.startsWith("DOCUMENT:");
    }
  }

  private VariableTranslator getVariableTranslator(String varType)
  {
    VariableTranslator vt = null;
    VariableTypeVO vo = dataAccess.getVariableType(varType);
    if (vo != null)
    {
      try
      {
        Class<? extends VariableTranslator> cl = Class.forName(vo.getTranslatorClass()).asSubclass(VariableTranslator.class);
        vt = (VariableTranslator) cl.newInstance();
      }
      catch (Exception ex)
      {
        // can't create translator
        if (MdwPlugin.getSettings().isLogConnectErrors())
          PluginMessages.log(ex);
      }
    }
    return vt;
  }

  public void deleteProcessInstances(final List<ProcessInstanceVO> processInstances)
  {
    String progressMsg = "Deleting process instances";
    String errorMsg = "Delete Process Instance";

    designerRunner = new DesignerRunner(progressMsg, errorMsg, project)
    {
      public void perform() throws ValidationException, DataAccessException, RemoteException
      {
        dataAccess.getDesignerDataAccess().deleteProcessInstances(processInstances);
      }
    };
    designerRunner.run();
  }

  public ExternalMessageVO getExternalMessage(Activity activity, ActivityInstanceVO activityInstance)
  {
    try
    {
      Long eventDocId = null;
      if (activity.isStart())
      {
        ProcessInstanceVO processInst = activity.getProcessInstance();
        if (processInst.getOwner().equals(OwnerType.DOCUMENT))
          eventDocId = processInst.getOwnerId();
      }
      return dataAccess.getDesignerDataAccess().getExternalMessage(activity.getId(), activityInstance.getId(), eventDocId);
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Retrieve External Message", project);
      return null;
    }
  }

  public String retryActivityInstance(Activity activity, ActivityInstanceVO activityInstance)
  {
    try
    {
      String statusMsg = restfulServer.retryActivityInstance(activity.getId(), activityInstance.getId(), activity.getProject().isOldNamespaces());
      getPluginDataAccess().auditLog(Action.Retry, Entity.ActivityInstance, activityInstance.getId(), activity.getLabel());
      return statusMsg;
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
      return ex.getMessage();
    }
  }

  public String skipActivityInstance(Activity activity, ActivityInstanceVO activityInstance, String completionCode)
  {
    try
    {
      String statusMsg = restfulServer.skipActivityInstance(activity.getId(), activityInstance.getId(), completionCode, activity.getProject().isOldNamespaces());
      getPluginDataAccess().auditLog(Action.Proceed, Entity.ActivityInstance, activityInstance.getId(), completionCode);
      return statusMsg;
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
      return ex.getMessage();
    }
  }

  public TestCaseRun prepareTestCase(AutomatedTestCase testCase, int runNum, File resultDir, boolean createReplace, boolean verbose, PrintStream log, LogMessageMonitor monitor, boolean singleServer, boolean stubbing)
  throws RemoteException
  {
    if ((testCase.isGroovy() || testCase.isGherkin()) && !MdwPlugin.workspaceHasGroovySupport())
    {
      String msg = "Please install Groovy support to execute test case: " + testCase.getName();
      throw new IllegalStateException(msg);
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmssSSS");
    try
    {
      if (!testCase.isLegacy() && !testCase.getProject().isFilePersist()) {
        // non-VCS asset-based -- load ruleSet content
        testCase.load();
        testCase.getRuleSetVO().setPackageName(testCase.getPackage().getName());
      }
      testCase.getTestCase().prepare();

      Map<String,ProcessVO> procCache = new HashMap<String,ProcessVO>();

      String masterRequestId = testCase.getMasterRequestId();

      if (masterRequestId == null)
        masterRequestId = project.getUser().getUsername() + "-" + sdf.format(new Date());
      else if (!testCase.isGroovy() && !testCase.isGherkin() && masterRequestId.equals(TestDataFilter.AnyNumberToken))
        masterRequestId = Long.toString(System.currentTimeMillis());
      if (testCase.isGherkin())
      {
        if (masterRequestId.indexOf("${masterRequestId}") != -1)
            masterRequestId = masterRequestId.replace("${masterRequestId}", project.getUser().getUsername() + "-" + sdf.format(new Date()));
      }
      testCase.setMasterRequestId(masterRequestId);
      TestCaseRun run;

      if (testCase.isGherkin())
        run = new GherkinTestCaseLaunch(testCase.getTestCase(), runNum, masterRequestId, new DesignerDataAccess(dataAccess.getDesignerDataAccess()), monitor, procCache, testCase.isLoadTest(), true, testCase.getProject().isOldNamespaces(), project);
      else if (testCase.isGroovy())
      {
        List<String> classpathList = new ArrayList<String>();
        IClasspathEntry[] iClassPathEntries = project.getJavaProject().getResolvedClasspath(true);
        for (IClasspathEntry iClassPathEntry: iClassPathEntries){
           StringBuffer projectPath = new StringBuffer(StringUtils.substringBeforeLast(project.getProjectDirWithFwdSlashes(), "/"));
           String classPath= iClassPathEntry.getPath().toString();
           if(iClassPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE || iClassPathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT)
             classpathList.add(projectPath.append("/").append(classPath.split("/")[1]).append("/build/classes").toString());
           else
             classpathList.add(classPath);
        }
        run = new GroovyTestCaseRun(testCase.getTestCase(), runNum, masterRequestId, new DesignerDataAccess(dataAccess.getDesignerDataAccess()), monitor, procCache, testCase.isLoadTest(), true, testCase.getProject().isOldNamespaces(), classpathList);
      }
      else
        run = new TestCaseRun(testCase.getTestCase(), runNum, masterRequestId, new DesignerDataAccess(dataAccess.getDesignerDataAccess()), monitor, procCache, testCase.isLoadTest(), true, testCase.getProject().isOldNamespaces());
      run.prepareTest(createReplace, resultDir, verbose, singleServer, stubbing, log);
      return run;
    }
    catch (Exception ex)
    {
      throw new RemoteException(ex.getMessage(), ex);
    }
  }

  public ApplicationSummary retrieveAppSummary(boolean failSilently)
  {
    try
    {
      return restfulServer.getAppSummary().getApplicationSummary();
    }
    catch (Exception ex)
    {
      if (!failSilently)
        PluginMessages.uiError(ex, ex.toString(), "Remote App Summary", project);

      return null;
    }
  }

  public MDWStatusMessage launchProcess(WorkflowProcess processVersion, String masterRequestId, String owner, Long ownerId, List<VariableValue> variableValues, Long activityId)
  throws RemoteException, DataAccessException, XmlException
  {
    Map<VariableVO,String> variables = new HashMap<VariableVO,String>();
    for (VariableValue variableValue : variableValues)
    {
      variables.put(variableValue.getVariableVO(), variableValue.getValue());
    }

    MDWStatusMessageDocument statusMessageDoc = restfulServer.launchProcess(processVersion.getId(), masterRequestId, owner, ownerId, variables, activityId, project.isOldNamespaces());
    if (statusMessageDoc.getMDWStatusMessage().getStatusCode() != 0)
      throw new RemoteException("Error launching process: " + statusMessageDoc.getMDWStatusMessage().getStatusMessage());

    // audit log in separate dao since launch is multi-threaded
    UserActionVO userAction = new UserActionVO(project.getUser().getUsername(), Action.Run, processVersion.getActionEntity(), processVersion.getId(), processVersion.getLabel());
    userAction.setSource("Eclipse/RCP Designer");
    try
    {
      new DesignerDataAccess(dataAccess.getDesignerDataAccess()).auditLog(userAction);
    }
    catch (Exception ex)
    {
      throw new DataAccessException(-1, ex.getMessage(), ex);
    }
    return statusMessageDoc.getMDWStatusMessage();
  }

  public String launchSynchronousProcess(WorkflowProcess processVersion, String masterRequestId, String owner, Long ownerId, List<VariableValue> variableValues, String responseVarName)
  throws RemoteException, DataAccessException, XmlException
  {
    Map<VariableVO,String> variables = new HashMap<VariableVO,String>();
    for (VariableValue variableValue : variableValues)
    {
      variables.put(variableValue.getVariableVO(), variableValue.getValue());
    }
    boolean oldFormat = !processVersion.getProject().checkRequiredVersion(5, 5);
    String ret = restfulServer.launchSynchronousProcess(processVersion.getId(), masterRequestId, owner, ownerId, variables, responseVarName, oldFormat);
    dataAccess.auditLog(Action.Run, processVersion);
    return ret;
  }

  public String sendExternalEvent(String request, Map<String,String> headers) throws DataAccessException, RemoteException
  {
    String ret = dataAccess.getDesignerDataAccess().sendMessage("RestfulWebService", request, headers);
    dataAccess.auditLog(Action.Send, Entity.Event, new Long(0), "External Event");
    return ret;
  }

  public String notifyProcess(String request, Map<String,String> headers) throws DataAccessException, RemoteException
  {
    String ret = dataAccess.getDesignerDataAccess().sendMessage("RestfulWebService", request, headers);
    dataAccess.auditLog(Action.Send, Entity.Event, new Long(0), "Notify Process");
    return ret;
  }

  public String toggleProcessLock(WorkflowProcess processVersion, boolean lock) throws DataAccessException, RemoteException
  {
    processVersion.setReadOnly(!lock);
    processVersion.setLockingUser(lock ? project.getUser().getUsername() : null);
    processVersion.setLockedDate(lock ? new Date() : null);
    return dataAccess.getDesignerDataAccess().lockUnlockProcess(processVersion.getId(), project.getUser().getUsername(), lock);
  }

  public String toggleWorkflowAssetLock(WorkflowAsset asset, boolean lock) throws DataAccessException, RemoteException
  {
    return dataAccess.getDesignerDataAccess().lockUnlockRuleSet(asset.getId(), project.getUser().getUsername(), lock);
  }

  public void remoteImportVcs() throws DataAccessException
  {
    try
    {
      VcsRepository repo = project.getMdwVcsRepository();
      String user = repo.getUser();
      String password = repo.getPassword();
      if (user == null || password == null)
        MessageDialog.openError(MdwPlugin.getShell(), "VCS Import", "Please set repository credentials for " + project.getLabel());
      String encryptedPassword = CryptUtil.encrypt(password);
      dataAccess.getDesignerDataAccess().importFromVcs(user, encryptedPassword, repo.getBranch());
    }
    catch (Exception ex)
    {
      throw new DataAccessException(ex.getMessage(), ex);
    }
  }

  public String checkForServerDbMismatch()
  {
    ApplicationSummary appSummary = retrieveAppSummary(true);
    if (appSummary == null)
      return "Server appears to be offline: " + restfulServer.getMdwWebUrl();
    else
    {
      if (project.isFilePersist())
        return null;  // not relevant
      else
        return checkForDbMismatch(appSummary);
    }
  }

  public String checkForDbMismatch(ApplicationSummary appSummary)
  {
    DbInfo dbInfo = appSummary.getDbInfo();
    if (dbInfo != null)
    {
      JdbcDataSource ds = project.getMdwDataSource();
      if (!ds.getJdbcUrl().equalsIgnoreCase(dbInfo.getJdbcUrl())) {
        if (!ds.isMariaDb() || !ds.getMariaDbUrlAsMySql().equals(dbInfo.getJdbcUrl())) // server may report as mysql
            return "Project JDBC URL: " + ds.getJdbcUrl() + "\nServer JDBC URL: " + dbInfo.getJdbcUrl();
      }
      else if (!ds.getDbUser().equalsIgnoreCase(dbInfo.getUser()))
        return "Project DB User: " + ds.getDbUser() + "\nServer DB User: " + dbInfo.getUser();
    }
    return null;
  }

  public List<TaskInstanceVO> getMyTasks()
  {
    // TODO: use new REST services to retrieve task list
    return new ArrayList<TaskInstanceVO>();
  }

  public void toggleStubServer()
  {
    if (StubServer.isRunning())
    {
      StubServer.stop();
    }
    else
    {
      try
      {
        StubServer.start(restfulServer, project.getServerSettings().getStubServerPort(), new UiStubber(), project.isOldNamespaces());
      }
      catch (IOException ex)
      {
        PluginMessages.uiError(ex,  "Stub Server", project);
      }
    }
  }

  public boolean isStubServerRunning()
  {
    return StubServer.isRunning();
  }

  private static LogWatcher logWatcher;  // TODO allow one log watcher per project
  public LogWatcher getLogWatcher(Display display)
  {
    if (logWatcher == null)
      logWatcher = LogWatcher.getInstance(display, project.getServerSettings());
    return logWatcher;
  }
  public void toggleLogWatcher(Display display, boolean watchProcess)
  {
    LogWatcher logWatcher = getLogWatcher(display);
    if (logWatcher.isRunning())
    {
      logWatcher.shutdown();
    }
    else
    {
      logWatcher.startup(watchProcess);
    }
  }
  public static boolean isLogWatcherRunning()
  {
    // TODO allow one log watcher per project
    if (LogWatcher.instance == null)
      return false;
    else
      return LogWatcher.instance.isRunning();
  }

  public static boolean isArchiveEditAllowed()
  {
    return DesignerDataAccess.isArchiveEditAllowed();
  }

  public boolean isServerOnline()
  {
    return retrieveAppSummary(true) != null;
  }

  private void handleLazyUserAuth() throws AuthenticationException
  {
      if (project.isFilePersist() && !project.isRemote())
      {
        // authentication not needed
        project.setUser(new User(System.getProperty("user.name")));
      }
      else
      {
        // user authentication
        Boolean authenticated = project.isAuthenticated();
        if (authenticated == null)
          throw new AuthenticationException("Not authorized for project: " + project.getName());
        if (!authenticated)
        {
          LoginDialog loginDialog = new LoginDialog(MdwPlugin.getShell(), project);
          int res = loginDialog.open();
          if (res == Dialog.CANCEL || !project.isAuthenticated())
            throw new AuthenticationException("Not authorized for project: " + project.getName());
        }
      }
  }
}
