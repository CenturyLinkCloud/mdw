/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.project.facet.core.IActionConfig;
import org.eclipse.wst.common.project.facet.core.IActionConfigFactory;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.xml.sax.InputSource;

import com.centurylink.mdw.auth.Authenticator;
import com.centurylink.mdw.auth.ClearTrustAuthenticator;
import com.centurylink.mdw.auth.MdwSecurityException;
import com.centurylink.mdw.auth.OAuthAuthenticator;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.container.NamingProvider;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.designer.testing.TestResultsParser;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.designer.utils.Server;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.CodeTimer;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.NotificationChecker;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.User;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.PluginDataAccess;
import com.centurylink.mdw.plugin.designer.dialogs.FrameworkUpdateDialog;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.LegacyExpectedResults;
import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.PageletTab;
import com.centurylink.mdw.plugin.designer.views.ProcessExplorerContentProvider;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModule;
import com.centurylink.mdw.plugin.workspace.ArtifactResourceListener;
import com.centurylink.mdw.service.ApplicationSummaryDocument;
import com.centurylink.mdw.service.ApplicationSummaryDocument.ApplicationSummary;
import com.centurylink.mdw.workflow.EnvironmentDB;
import com.centurylink.mdw.workflow.ManagedNode;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

public class WorkflowProject extends WorkflowElement implements Comparable<WorkflowProject>, ElementChangeListener, IActionConfig
{
  public static final String MDW_QUALIFIER = "com.centurylink.mdw.plugin.project";
  public static final String MDW_DATASOURCE = "MDWDataSource";
  private static final String[] MDW4_SERVICES_LIBS
    = { "MDWAdapters.jar", "MDWDesignerServer.jar", "MDWServices.jar", "MDWWorkflowEngine.jar" };
  private static final String[] MDW50_SERVICES_LIBS
    = { "MDWAdapters.jar", "MDWServices.jar", "MDWWorkflowEngine.jar" };
  public static final String[] MDW51_SERVICES_LIBS
    = { "MDWServices.jar", "MDWWorkflowEngine.jar" };
  public static final String[] MDW_SERVICES_LIBS
    = { };
  public static final String[] MDW_WEBAPP_LIBS
    = { "MDWTaskManagerWeb.jar", "mdwweb.jar" };
  public static final String[] MDW_WARS
    = { "MDWTaskManagerWeb.war", "MDWWeb.war", "MDWDesignerWeb.war" };

  public static final String MDW_SUPPRESSED_ACTIVITY_IMPLEMENTORS = "MdwSuppressedActivityImplementors";
  public static final String MDW_REMOTE_JMX_PORT = "MdwRemoteJmxPort";
  public static final int MDW_DEFAULT_REMOTE_JMX_PORT = 8501;
  public static final String MDW_VISUALVM_ID = "MdwVisualVmId";
  public static final String BAM_PAGELET = "BAM.xml";

  public WorkflowProject()
  {
  }

  public WorkflowProject(WorkflowApplication workflowApp, WorkflowEnvironment workflowEnv)
  {
    String concat = workflowApp.getName();
    if (workflowEnv.getName().startsWith("MDW Cloud"))
      concat += " " + workflowEnv.getName().substring(4);
    else
      concat += " " + workflowEnv.getName();

    setSourceProjectName(concat.trim());
    setRemote(true);
    setProduction(workflowEnv.getName().equals("Production"));
    ManagedNode managedNode = workflowEnv.getManagedServerList().get(0);
    serverSettings = new ServerSettings(this);
    serverSettings.setHost(managedNode.getHost());
    serverSettings.setPort(managedNode.getPort().intValue());
    if (managedNode.getWebRoot() == null)
      setWebContextRoot(workflowApp.getWebContextRoot());
    else
      setWebContextRoot(managedNode.getWebRoot());
    EnvironmentDB envDb = workflowEnv.getEnvironmentDb();
    if (envDb != null)
    {
      mdwDataSource.setJdbcUrl(envDb.getJdbcUrl());
      mdwDataSource.setDbUser(envDb.getUser());
      if (envDb.getSchemaOwner() != null && envDb.getSchemaOwner().length() > 0)
        mdwDataSource.setSchemaOwner(envDb.getSchemaOwner());
      try
      {
        String password = CryptUtil.decrypt(envDb.getPassword());
        mdwDataSource.setDbPassword(password);
      }
      catch (GeneralSecurityException ex)
      {
        PluginMessages.uiError(ex, "Create Project", this);
      }
      setMdwDataSource(mdwDataSource);
    }
    com.centurylink.mdw.workflow.VcsRepository repository = workflowApp.getRepository();
    if (repository != null)
    {
      String provider = repository.getProvider();
      if (VcsRepository.PROVIDER_GIT.equals(provider))
      {
        mdwVcsRepository = new VcsRepository();
        mdwVcsRepository.setProvider(VcsRepository.PROVIDER_GIT);
        mdwVcsRepository.setRepositoryUrl(repository.getUrl());
        if (repository.getLocalPath() != null)  // non-default
           mdwVcsRepository.setLocalPath(repository.getLocalPath());
        mdwVcsRepository.setUser(repository.getUser());
        if (repository.getPassword() != null)
        {
          try
          {
            String password = CryptUtil.decrypt(repository.getPassword());
            mdwVcsRepository.setPassword(password);
          }
          catch (GeneralSecurityException ex)
          {
            PluginMessages.uiError(ex, "VCS Repository", this);
          }
        }
        setPersistType(PersistType.Git);
      }
    }
  }

  public Entity getActionEntity()
  {
    return Entity.Project;
  }

  private String earProjectName;
  public String getEarProjectName() { return earProjectName; }
  public void setEarProjectName(String earProjName) { this.earProjectName = earProjName; }

  private String sourceProjectName;
  public String getSourceProjectName() { return sourceProjectName; }
  public void setSourceProjectName(String srcProjName) { this.sourceProjectName = srcProjName; }
  public String getName() { return sourceProjectName; }

  public enum PersistType
  {
    Database,
    Git,
    None
  }
  private PersistType persistType = PersistType.Database;
  public PersistType getPersistType() { return persistType; }
  public void setPersistType(PersistType type) { this.persistType = type; }

  public boolean isFilePersist()
  {
    return persistType != PersistType.Database;
  }

  public boolean isShowDefaultPackage()
  {
    return !isFilePersist();
  }

  public boolean isRequireWorkflowPackage()
  {
    return checkRequiredVersion(5, 2);
  }

  public boolean isRequireAssetExtension()
  {
    if (checkRequiredVersion(5, 5))
      return true;
    if (checkRequiredVersion(5, 2))
      return !MdwPlugin.getSettings().isAllowAssetNamesWithoutExtensions();
    return false; // < 5.2
  }

  /**
   * Accessed from env templates during project creation.
   */
  public String getMdwWebProjectName()
  {
    if (isCloudProject())
      return "MDWWeb";
    else
      return getSourceProjectName() + "Web";
  }

  /**
   * Accessed from env templates during project creation.
   */
  public String getDesignerWebProjectName()
  {
    if (isCloudProject())
      return isOsgi() ? "MDWWeb" : "MDWDesignerWeb";
    else
      return getSourceProjectName() + "DesignerWeb";
  }

  /**
   * Accessed from env templates during project creation.
   */
  public String getTaskManagerWebProjectName()
  {
    if (isCustomTaskManager())
      return getSourceProjectName() + "TaskManager";
    else
      return "MDWTaskManagerWeb";
  }

  public enum SourceProjectType
  {
    POJO,
    EJB
  }
  private SourceProjectType sourceProjectType = SourceProjectType.POJO;
  public SourceProjectType getSourceProjectType() { return sourceProjectType; }
  public void setSourceProjectType(SourceProjectType type) { this.sourceProjectType = type; }
  public boolean isPojoSourceProject()
  {
    return sourceProjectType.equals(SourceProjectType.POJO);
  }
  public boolean isEjbSourceProject()
  {
    return sourceProjectType.equals(SourceProjectType.EJB);
  }

  @Override
  public String getLabel()
  {
    return getSourceProjectName();
  }

  public String getFullPathLabel()
  {
    return getName();
  }

  @Override
  public String getIcon()
  {
    if (isProduction())
    {
      if (isWarn())
        return "prod_project_warn.gif";
      else
        return "prod_project.gif";
    }
    else if (isRemote())
    {
      if (isWarn())
        return "remote_project_warn.gif";
      else
        return "remote_project.gif";
    }
    else if (isCloudProject())
    {
      return "cloud_project.gif";
    }
    else
    {
      return "wf_project.gif";
    }
  }

  private String webProjectName;
  public String getWebProjectName() { return webProjectName; }
  public void setWebProjectName(String wpName) { this.webProjectName = wpName; }

  public boolean isCustomTaskManager()
  {
    return hasExtension("customTaskManager");
  }

  private JdbcDataSource mdwDataSource = new JdbcDataSource();
  public JdbcDataSource getMdwDataSource() { return mdwDataSource; }
  public void setMdwDataSource(JdbcDataSource mds) { this.mdwDataSource = mds; }

  private VcsRepository mdwVcsRepository = new VcsRepository();
  public VcsRepository getMdwVcsRepository() { return mdwVcsRepository; }
  public void setMdwVcsRepository(VcsRepository repo) { this.mdwVcsRepository = repo; }

  private String appVersion;
  public String getAppVersion()
  {
    if (appVersion == null)
    {
      if (isRemote())
      {
        getMdwVersion();
      }
      else if (isEarProject())
      {
        IFile verFile = getEarProject().getFile(new Path("deploy/config/buildversion.properties"));
        if (verFile.exists())
          appVersion = new String(PluginUtil.readFile(verFile));
        else
          appVersion = "Unknown";
      }
    }
    return appVersion;
  }

  public boolean isGitVcs()
  {
    return isFilePersist() && mdwVcsRepository.getRepositoryUrl() != null;
  }

  private String mdwVersion;
  /**
   * Also populates appVersion to avoid two server calls if first one fails.
   * Does not attempt to populate if not initialized (ie not expanded in proc exp view).
   */
  public String getMdwVersion()
  {
    if (isRemote() && mdwVersion == null && designerProxy != null)
    {
      mdwVersion = "Unknown";
      appVersion = "Unknown";
      ApplicationSummary appSummary = getRemoteAppSummary(false);
      if (appSummary != null && appSummary.getMdwVersion() != null)
      {
        mdwVersion = appSummary.getMdwVersion();
        appVersion = appSummary.getVersion();

        try
        {
          // force parse (triggering exception for unparseable -- eg: @mdw.version@)
          MdwVersion vers = new MdwVersion(mdwVersion);
          vers.getMajorVersion();
          vers.getMinorVersion();
          vers.getBuildId();
        }
        catch (Exception ex)
        {
          mdwVersion = "Unknown";
          PluginMessages.log(ex);
        }
      }
      else
      {
        if (isFilePersist())
        {
          mdwVersion = "5.5"; // minimum supported version (TODO: future versions)
        }
        else
        {
          // server may not be running -- infer version from db structure
          String jdbcUrl = getMdwDataSource().getJdbcUrlWithCredentials();
          if (jdbcUrl != null)
          {
            try
            {
              // don't initialize designerProxy just to infer version from db
              RestfulServer server = new RestfulServer(jdbcUrl, getUser().getUsername(), getServiceUrl());
              DesignerDataAccess dao = new DesignerDataAccess(server, new ArrayList<Server>(), getUser().getUsername());
              int dbSchemaVersion = dao.getDatabaseSchemaVersion();
              int major = dbSchemaVersion / 1000;
              int minor = dbSchemaVersion % 1000;
              mdwVersion = major + "." + minor;
            }
            catch (Exception ex)
            {
              PluginMessages.log(ex);  // dataAccessFailed
            }
          }
        }
      }
    }
    return mdwVersion;
  }
  public void setMdwVersion(String version) { this.mdwVersion = version; }
  public int getMdwMajorVersion()
  {
    String mdwVersion = getMdwVersion();
    if (mdwVersion == null || mdwVersion.equals("Unknown"))
      mdwVersion = "0.0";
    return new MdwVersion(mdwVersion).getMajorVersion();
  }
  public int getMdwMinorVersion()
  {
    String mdwVersion = getMdwVersion();
    mdwVersion = mdwVersion.replaceAll("x", "0.0");
    if (mdwVersion == null || mdwVersion.equals("Unknown"))
      mdwVersion = "0.0.0";
    return new MdwVersion(mdwVersion).getMinorVersion();
  }
  public int getMdwBuildId()
  {
    mdwVersion = mdwVersion.replaceAll("x", "0.0");
    if (mdwVersion == null || mdwVersion.equals("Unknown"))
      mdwVersion = "0.0.0";
    return new MdwVersion(mdwVersion).getBuildId();
  }
  public int getMdwSchemaVersion()
  {
    return getMdwMajorVersion() * 1000;
  }
  public boolean checkRequiredVersion(int major, int minor)
  {
    return checkRequiredVersion(major, minor, 0);
  }
  public boolean checkRequiredVersion(int major, int minor, int build)
  {
    int projectMajor = getMdwMajorVersion();
    if (projectMajor > major)
      return true;
    if (projectMajor < major)
      return false;

    // major versions are equal
    int projectMinor = getMdwMinorVersion();
    if (projectMinor > minor)
      return true;
    if (projectMinor < minor)
      return false;

    // major and minor are equal
    return getMdwBuildId() >= build;
  }
  public boolean isMdw5()
  {
    return checkRequiredVersion(5, 0);
  }
  public boolean isPureMdw52()
  {
    if (!checkRequiredVersion(5, 2))
      return false;

    return getDesignerProxy().getDesignerDataAccess().getSupportedSchemaVersion() >= DataAccess.schemaVersion52;
  }
  public boolean isOldNamespaces()
  {
    return !checkRequiredVersion(5, 5);
  }

  public boolean isSnapshotMdwVersion()
  {
    String version = getMdwVersion();
    if (version != null)
      return version.indexOf("SNAPSHOT") > 0;
    return false;
  }

  private boolean remote;
  public boolean isRemote() { return remote; }
  public void setRemote(boolean remote) { this.remote = remote; }

  private boolean cloudProject;
  public boolean isCloudProject() { return cloudProject; }
  public void setCloudProject(boolean cloud) { this.cloudProject = cloud; }

  public boolean isCloudOnly()
  {
    return isCloudProject() && isWar();
  }

  /**
   * As opposed to Dynamic Java.
   */
  public boolean isLocalJavaSupported()
  {
    return !isCloudOnly() && !isRemote();
  }

  private Boolean remoteOsgi;
  public boolean isOsgi()
  {
    if (isRemote())
    {
      if (remoteOsgi == null)
      {
        boolean osgiCheck = false;
        ApplicationSummary appSummary = getRemoteAppSummary(false);
        if (appSummary != null)
          osgiCheck = NamingProvider.OSGI.equals(appSummary.getContainer());
        remoteOsgi = new Boolean(osgiCheck);
      }
      return remoteOsgi;
    }
    else
    {
      return getServerSettings().isOsgi();
    }
  }

  public boolean isWar()
  {
    if (isRemote())
    {
      boolean war = false;
      ApplicationSummary appSummary = getRemoteAppSummary(false);
      if (appSummary != null)
        war = NamingProvider.TOMCAT.equals(appSummary.getContainer());
      return war;
    }
    else
    {
      return getServerSettings().isWar();
    }
  }

  // this is set by <webProject name="mdw-hub" deployDir="deploy" />
  private String deployDir;
  public String getDeployDir() { return deployDir; }
  public void setDeployDir(String deployDir) { this.deployDir = deployDir; }

  /**
   * Only for cloud projects.
   */
  public IFolder getDeployFolder()
  {
    String dir = deployDir == null ? "deploy" : deployDir;

    if (isCloudProject())
    {
      if (getWebProject() != null)
        return getWebProject().getFolder(new Path(dir));
      else
        return getSourceProject().getFolder(new Path(dir));
    }
    else
    {
      return null;
    }
  }

  public boolean isEarProject()
  {
    return !isRemote() && !isCloudProject() && !isWar();
  }

  private boolean production;
  public boolean isProduction() { return production; }
  public void setProduction(boolean prod)
  {
    boolean changed = this.production != prod;
    this.production = prod;
    if (changed)
      fireElementChangeEvent(ChangeType.LABEL_CHANGE, getSourceProjectName());
  }

  private ApplicationSummary remoteAppSummary;
  public ApplicationSummary getRemoteAppSummary(boolean errorDialogOnFailure)
  {
    if (remoteAppSummary == null)
      retrieveRemoteAppSummary(errorDialogOnFailure);

    return remoteAppSummary;
  }
  public void clearRemoteAppSummary()
  {
    remoteAppSummary = null;
    mdwVersion = null;
    remoteOsgi = null;
    getMdwVcsRepository().clear();

  }
  public boolean isRemoteAppInfoAvailable()
  {
    return getRemoteAppSummary(false) != null;
  }
  public void retrieveRemoteAppSummary(final boolean errorDialogOnFailure)
  {
    Runnable runnable = new Runnable()
    {
      public void run()
      {
        // bypass designer proxy to delay lazy loading if project hasn't been opened
        String url = getServiceUrl() + "/Services/GetAppSummary";
        try
        {
          HttpHelper httpHelper = new HttpHelper(new URL(url));
          httpHelper.setConnectTimeout(MdwPlugin.getSettings().getHttpConnectTimeout());
          httpHelper.setReadTimeout(MdwPlugin.getSettings().getHttpReadTimeout());
          String response = httpHelper.get();
          ApplicationSummaryDocument appSummaryDocument = ApplicationSummaryDocument.Factory.parse(response, Compatibility.namespaceOptions());
          remoteAppSummary = appSummaryDocument.getApplicationSummary();
        }
        catch (IOException ex)
        {
          PluginMessages.log(ex);
          if (errorDialogOnFailure)
            MessageDialog.openError(MdwPlugin.getShell(), "Authentication", "Server appears to be offline: " + ex.getMessage());
        }
        catch (Exception ex)
        {
          PluginMessages.log(ex);
          if (errorDialogOnFailure)
            PluginMessages.uiError(ex, "Remote App Summary", WorkflowProject.this);
        }
      }
    };

    if (MdwPlugin.isUiThread())
      BusyIndicator.showWhile(MdwPlugin.getDisplay(), runnable);
    else
      runnable.run();
  }

  private String author = System.getProperty("user.name");
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }

  private ServerSettings serverSettings = new ServerSettings(this);
  public ServerSettings getServerSettings() { return serverSettings; }
  public void setServerSettings(ServerSettings settings) { serverSettings = settings; }
  public void clearServerSettings() { this.serverSettings = new ServerSettings(this); }

  private boolean updateServerCache = true;
  public boolean isUpdateServerCache() { return updateServerCache; }
  public void setUpdateServerCache(boolean refresh) { this.updateServerCache = refresh; }

  private OsgiSettings osgiSettings = new OsgiSettings();
  public OsgiSettings getOsgiSettings() { return osgiSettings; }
  public void setOsgiSettings(OsgiSettings settings) { this.osgiSettings = settings; }

  private DesignerProxy designerProxy;
  public synchronized DesignerProxy getDesignerProxy()
  {
    // lazily initialize
    if (designerProxy == null)
    {
      BusyIndicator.showWhile(Display.getCurrent(), new Runnable()
      {
        public void run()
        {
          designerProxy = new DesignerProxy(WorkflowProject.this);
          try
          {
            designerProxy.initialize(null);
          }
          catch (Exception ex)
          {
            designerProxy = null;
            PluginMessages.uiError(ex, "Initialize Project");
          }
        }
      });
    }
    return designerProxy;
  }

  public synchronized void initialize(ProgressMonitor progressMonitor) throws Exception
  {
    designerProxy = new DesignerProxy(this);
    try
    {
      if (isFilePersist())
        refreshProject();
      designerProxy.initialize(progressMonitor);
    }
    catch (Exception ex)
    {
      designerProxy = null;
      throw ex;
    }
  }

  public boolean isInitialized()
  {
    return designerProxy != null;
  }

  public PluginDataAccess getDataAccess()
  {
    if (getDesignerProxy() == null)
      return null;
    return getDesignerProxy().getPluginDataAccess();
  }

  public DesignerDataModel getDesignerDataModel()
  {
    return getDataAccess().getDesignerDataModel();
  }

  public DesignerDataAccess getDesignerDataAccess()
  {
    if (getDesignerProxy() == null)
      return null;
    return getDesignerProxy().getDesignerDataAccess();
  }

  private List<WorkflowPackage> topLevelPackages;
  private List<WorkflowPackage> topLevelUserVisiblePackages;
  private List<WorkflowProcess> unpackagedProcesses;
  private List<WorkflowAsset> unpackagedWorkflowAssets;
  private Folder archivedPackageFolder;
  private Folder archivedUserVisiblePackagesFolder;
  private AutomatedTestSuite legacyTestSuite;
  private WorkflowPackage archivedDefaultPackage;
  private List<PageletTab> pageletTabs;
  public List<PageletTab> getPageletTabs() { return pageletTabs; }
  public PageletTab getPageletTab(String id)
  {
    if (pageletTabs != null)
    {
      for (PageletTab pageletTab : pageletTabs)
      {
        if (pageletTab.getId().equals(id))
          return pageletTab;
      }
    }
    return null;
  }


  public String getTitle()
  {
    return "Workflow Project";
  }

  public Long getId()
  {
    return convertToLong(getName());
  }

  public boolean isReadOnly()
  {
    return isRemote() && isFilePersist();
  }
  public boolean isUserAllowedToEdit()
  {
    return isRemote();
  }

  public boolean hasInstanceInfo()
  {
    return false;
  }

  public WorkflowProject getProject() { return this; }

  private String filesToIgnoreDuringUpdate;
  public String getFilesToIgnoreDuringUpdate() { return filesToIgnoreDuringUpdate; }
  public void setFilesToIgnoreDuringUpdate(String fti) { filesToIgnoreDuringUpdate = fti; }
  public List<String> getFilesToIgnore()
  {
    if (filesToIgnoreDuringUpdate == null)
      return null;

    List<String> filesToIgnore = new ArrayList<String>();
    StringTokenizer st = new StringTokenizer(filesToIgnoreDuringUpdate, ", ");
    while (st.hasMoreTokens())
    {
      filesToIgnore.add(st.nextToken());
    }
    return filesToIgnore;
  }

  public static final String DEFAULT_FILES_TO_IGNORE = "application.xml,build.xml,env.properties.tmpl,startWebLogic.cmd";
  public void setDefaultFilesToIgnoreDuringUpdate()
  {
    filesToIgnoreDuringUpdate = DEFAULT_FILES_TO_IGNORE;
    if (isOsgi())
      filesToIgnoreDuringUpdate = "mdw.properties";
    else if (isWar())
      filesToIgnoreDuringUpdate = "com.centurylink.mdw.cfg";
  }

  public static final class Factory implements IActionConfigFactory
  {
    public Object create()
    {
      return new WorkflowProject();
    }
  }

  public IProject getEarProject()
  {
    return WorkflowProjectManager.getInstance().getWorkflowFacetedProject(earProjectName);
  }

  public File getProjectDir()
  {
    if (isRemote() || isCloudProject())
      return getSourceProject().getLocation().toFile();
    else
      return getEarProject().getLocation().toFile();
  }

  public String getProjectDirWithFwdSlashes()
  {
    return getProjectDir().toString().replace('\\', '/');
  }

  public IFolder getProjectFolder(String path)
  {
    if (isRemote() || isCloudProject())
      return getSourceProject().getFolder(path);
    else
      return getEarProject().getFolder(path);
  }

  public IFile getProjectFile(String path)
  {
    if (isRemote() || isCloudProject())
      return getSourceProject().getFile(path);
    else
      return getEarProject().getFile(path);
  }

  public File getAssetDir()
  {
    assert isFilePersist();
    return new File(getProjectDir() + "/" + getMdwVcsRepository().getLocalPath());
  }

  public IFolder getAssetFolder()
  {
    assert isFilePersist();
    return getSourceProject().getFolder("/" + getMdwVcsRepository().getLocalPath());
  }

  public IFolder getAssetArchiveFolder()
  {
    assert isFilePersist();
    return getSourceProject().getFolder("/" + getMdwVcsRepository().getLocalPath() + "/Archive");
  }

  public void refreshProject()
  {
    assert isFilePersist();
    try
    {
      getSourceProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }
  }

  public IProject getSourceProject()
  {
    if (isRemote() || isCloudProject())
      return MdwPlugin.getWorkspaceRoot().getProject(getSourceProjectName());
    else
      return getSourceJavaProject().getProject();
  }

  public IJavaProject getSourceJavaProject()
  {
    return (IJavaProject) WorkflowProjectManager.getJavaProject(sourceProjectName);
  }

  public IJavaProject getJavaProject()
  {
    return WorkflowProjectManager.getJavaProject(sourceProjectName);
  }

  public IProject getWebProject()
  {
    if (getWebJavaProject() == null)
      return null;

    return getWebJavaProject().getProject();
  }

  public IJavaProject getWebJavaProject()
  {
    if (webProjectName == null || webProjectName.length() == 0)
      return null;

    return WorkflowProjectManager.getJavaProject(webProjectName);
  }

  public IFolder getTempFolder()
  {
    return getSourceProject().getFolder(MdwPlugin.getSettings().getTempResourceLocation());
  }

  public File getTempDir()
  {
    return getSourceProject().getFolder(MdwPlugin.getSettings().getTempResourceLocation()).getRawLocation().toFile();
  }

  public IFolder getEarContentFolder()
  {
    if (isCloudProject())
    {
      return getSourceProject().getFolder(new Path("deploy/ear"));
    }
    else  // ear project
    {
      IVirtualComponent earComponent = ComponentCore.createComponent(getEarProject());
      if (earComponent == null)
        return null;
      IVirtualFolder earRootFolder = earComponent.getRootFolder();
      return (IFolder) earRootFolder.getUnderlyingFolder();
    }
  }

  public File[] getAppInfLibFiles()
  {
    IFolder appInfLibFolder = getEarContentFolder().getFolder("APP-INF/lib");
    return new File(appInfLibFolder.getLocation().toString()).listFiles();
  }

  public File[] getServicesLibFiles()
  {
    List<File> matchingFiles = new ArrayList<File>();
    File[] allFiles = new File(getEarContentFolder().getLocation().toString()).listFiles();
    for (File file : allFiles)
    {
      String[] servicesLibs = MDW_SERVICES_LIBS;
      if (!checkRequiredVersion(5, 2))
        servicesLibs = MDW51_SERVICES_LIBS;
      if (!checkRequiredVersion(5, 1))
        servicesLibs = MDW50_SERVICES_LIBS;
      if (!checkRequiredVersion(5, 0))
        servicesLibs = MDW4_SERVICES_LIBS;

      if (Arrays.binarySearch(servicesLibs, file.getName()) >= 0)
      {
        matchingFiles.add(file);
      }
    }

    return matchingFiles.toArray(new File[0]);
  }

  public File[] getWebappLibFiles()
  {
    List<File> matchingFiles = new ArrayList<File>();
    File[] allFiles = new File(getWebContentFolder().getLocation().toString() + "/WEB-INF/lib").listFiles();
    for (File file : allFiles)
    {
      if (Arrays.binarySearch(MDW_WEBAPP_LIBS, file.getName()) >= 0)
      {
        matchingFiles.add(file);
      }
    }

    return matchingFiles.toArray(new File[0]);
  }

  private String webContextRoot;
  public void setWebContextRoot(String webContextRoot) { this.webContextRoot = webContextRoot; }
  public String getWebContextRoot()
  {
    if (webContextRoot != null)
      return webContextRoot;  // this is the case for remote projects

    if (isCloudOnly())
      return "mdw";
    else
      return getContextRoot("MDWWeb");
  }

  public String getWebToolsUserAccessUrl()
  {
    if (isRemote())
    {
      return getRemoteAppSummary(true).getMdwWebUrl();
    }
    else
    {
      return getServiceUrl();
    }
  }

  public String getServiceUrl()
  {
    return getServerSettings().getUrlBase() + "/" + getWebContextRoot();
  }

  public String getTaskManagerContextRoot()
  {
    return getContextRoot("MDWTaskManagerWeb");
  }

  public String getMdwHubContextRoot()
  {
    if (isCloudOnly())
      return "mdw";
    else
      return getContextRoot("MDWHub");
  }

  private String getContextRoot(String webAppId)
  {
    String ctxRoot = webAppId;
    try
    {
      IFolder earContentFolder = getEarContentFolder();
      if (earContentFolder != null)
      {
        IFile appXmlFile = earContentFolder.getFile("META-INF/application.xml");
        if (appXmlFile.exists())
        {
          InputStream inStream = appXmlFile.getContents();
          InputSource src = new InputSource(inStream);
          SAXParserFactory parserFactory = SAXParserFactory.newInstance();
          SAXParser parser = parserFactory.newSAXParser();
          AppXmlContextRootFinder ctxRootFinder = new AppXmlContextRootFinder(webAppId);
          parser.parse(src, ctxRootFinder);
          inStream.close();
          ctxRoot = ctxRootFinder.getContextRoot();
        }
      }
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
    }

    return ctxRoot;
  }

  public String getDesignerWebContextRoot()
  {
    return getContextRoot("MDWDesignerWeb");
  }

  public String getTaskManagerUrl()
  {
    if (isRemote())
    {
      return getRemoteAppSummary(true).getTaskManagerUrl();
    }
    else
    {
      return getServerSettings().getUrlBase() + "/" + getTaskManagerContextRoot();
    }
  }

  public String getMdwHubUrl()
  {
    if (isRemote())
    {
      return getRemoteAppSummary(true).getMdwHubUrl();
    }
    else
    {
      return getServerSettings().getUrlBase() + "/" + getMdwHubContextRoot();
    }
  }

  public String getDesignerWebUrl()
  {
    if (isRemote())
    {
      return getRemoteAppSummary(true).getDesignerUrl();
    }
    else
    {
      return getServerSettings().getUrlBase() + "/" + getDesignerWebContextRoot();
    }
  }

  public String getMyTasksPath()
  {
    if (checkRequiredVersion(5,5))
      return TaskAttributeConstant.MY_TASKS_PATH;
    else
      return TaskAttributeConstant.MY_TASKS_PATH_COMPATIBILITY;
  }

  public String getTaskManagerMyTasksPath()
  {
    return TaskAttributeConstant.MY_TASKS_PATH_COMPATIBILITY;
  }

  public String getTaskInstancePath(Long taskInstanceId)
  {
    return getTaskInstancePath(taskInstanceId, false);
  }

  public String getTaskInstancePath(Long taskInstanceId, boolean isAssigned)
  {
    boolean is55 = checkRequiredVersion(5, 5);
    String base =  is55 ? TaskAttributeConstant.TASK_DETAIL_PATH : TaskAttributeConstant.TASK_DETAIL_COMPATIBILITY_PATH;
    return "/" + base + taskInstanceId + (is55 ? "?" : "&" ) + TaskAttributeConstant.WORKGROUP_TASKS_TAB;
  }

  public String getSystemInfoPath()
  {
    return "/system/systemInformation.jsf";
  }

  public IFolder getWebContentFolder()
  {
    IVirtualComponent webComponent = ComponentCore.createComponent(getWebProject());
    IVirtualFolder webRootFolder = webComponent.getRootFolder();
    return (IFolder) webRootFolder.getUnderlyingFolder();
  }

  public String getDefaultSourceCodePackage()
  {
    return "com.centurylink." + sourceProjectName.toLowerCase().replace(' ', '.');
  }

  public String getDefaultSourceCodePackagePath()
  {
    return getDefaultSourceCodePackage().replace('.', '/');
  }

  public boolean isFrameworkProject()
  {
    return "mdw-workflow".equals(getSourceProjectName()) || (isEarProject() && "MDWFramework".equals(getEarProjectName()));
  }

  public WorkflowPackage getDefaultPackage()
  {
    if (isFilePersist())
      return null;
    else
      return getPackage(PackageVO.DEFAULT_PACKAGE_NAME);
  }

  public List<WorkflowPackage> getTopLevelPackages()
  {
    if (topLevelPackages == null)
      findTopLevelPackages(null);
    return topLevelPackages;
  }

  public List<WorkflowPackage> getTopLevelUserVisiblePackages()
  {
    return getTopLevelUserVisiblePackages(null);
  }

  public List<WorkflowPackage> getTopLevelUserVisiblePackages(ProgressMonitor progressMonitor)
  {
    if (topLevelUserVisiblePackages == null)
      findTopLevelPackages(progressMonitor);
    return topLevelUserVisiblePackages;
  }

  public boolean isLoaded()
  {
    return topLevelPackages != null;
  }

  /**
   * Finds the list of top level packages (including the default if supported),
   * populated with the appropriate processes, etc.
   */
  private void findTopLevelPackages(ProgressMonitor progressMonitor)
  {
    CodeTimer timer = new CodeTimer("findTopLevelPackages()");
    topLevelPackages = new ArrayList<WorkflowPackage>();
    topLevelUserVisiblePackages = new ArrayList<WorkflowPackage>();
    activityImpls.clear();
    for (PackageVO packageVO : getTopLevelPackageVOs(progressMonitor))
    {
      WorkflowPackage topLevelPackage = new WorkflowPackage(this, packageVO);
      topLevelPackage.setProcesses(findProcesses(topLevelPackage));
      topLevelPackage.setExternalEvents(findExternalEvents(topLevelPackage));
      topLevelPackage.setActivityImpls(findActivityImplementors(topLevelPackage));
      topLevelPackage.setAssets(findWorkflowAssets(topLevelPackage));
      topLevelPackage.setTaskTemplates(findTaskTemplates(topLevelPackage));
      topLevelPackages.add(topLevelPackage);
      if (topLevelPackage.isVisible())
        topLevelUserVisiblePackages.add(topLevelPackage);
      // register as a listener so that i can pass on element change events
      topLevelPackage.addElementChangeListener(this);
      for (WorkflowProcess process : topLevelPackage.getProcesses())
        process.addElementChangeListener(this);
    }
    Collections.sort(topLevelPackages);
    Collections.sort(topLevelUserVisiblePackages);
    File resultsFile = getFunctionTestResultsFile();
    if (resultsFile.exists())
    {
      // update test case statuses
      List<TestCase> testCases = new ArrayList<TestCase>();
      for (WorkflowPackage pkg : topLevelPackages)
      {
        for (WorkflowAsset asset : pkg.getAssets())
        {
          if (asset instanceof AutomatedTestCase)
            testCases.add(((AutomatedTestCase)asset).getTestCase());
        }
      }
      if (!testCases.isEmpty())
      {
        try
        {
          TestResultsParser parser = new TestResultsParser(resultsFile, testCases);
          if (resultsFile.getName().endsWith(".xml"))
            parser.parseXml();
          else
            parser.parseJson(getAssetDir());
        }
        catch (Exception ex)
        {
          PluginMessages.uiError(ex, "Parse Test Results", this);
        }
      }
    }
    timer.stopAndLog();
  }

  /**
   * Returns the top-level package VOs (includes default if supported).
   */
  private Collection<PackageVO> getTopLevelPackageVOs(ProgressMonitor progressMonitor)
  {
    Map<String,PackageVO> latestVersions = new HashMap<String,PackageVO>();
    for (PackageVO packageVO : getDataAccess().getPackages(true, progressMonitor))
    {
      if (!packageVO.isArchived() && (isShowDefaultPackage() || !packageVO.isDefaultPackage()))
      {
        PackageVO latestVersion = latestVersions.get(packageVO.getPackageName());
        if (latestVersion == null || packageVO.getVersion() > latestVersion.getVersion())
          latestVersions.put(packageVO.getPackageName(), packageVO);
      }
    }
    return latestVersions.values();
  }

  /**
   * Returns the process versions belonging to a package version.
   */
  private List<WorkflowProcess> findProcesses(WorkflowPackage aPackage)
  {
    List<WorkflowProcess> processVersions = new ArrayList<WorkflowProcess>();
    for (ProcessVO processVO : getDataAccess().getAllProcesses(false))
    {
      if (aPackage.getPackageVO().containsProcess(processVO.getProcessId()))
      {
        WorkflowProcess processVersion = new WorkflowProcess(this, processVO);
        if (!processVersions.contains(processVersion))  // vcs assets would add same-versioned archived processes
        {
          processVersion.setPackage(aPackage);
          processVersions.add(processVersion);
        }
      }
    }
    Collections.sort(processVersions);
    return processVersions;
  }

  public WorkflowPackage findPackage(WorkflowProcess processVersion)
  {
    for (PackageVO packageVO : getDataAccess().getPackages(false))
    {
      if (packageVO.containsProcess(processVersion.getId()))
        return new WorkflowPackage(this, packageVO);
    }
    return null;
  }

  public WorkflowPackage findPackage(RuleSetVO ruleSet)
  {
    for (PackageVO packageVO : getDataAccess().getPackages(false))
    {
      if (packageVO.containsRuleSet(ruleSet.getId()))
        return new WorkflowPackage(this, packageVO);
    }
    return getDefaultPackage();
  }

  /**
   * Returns the external events belonging to a package version.
   */
  private List<ExternalEvent> findExternalEvents(WorkflowPackage aPackage)
  {
    if (aPackage.isDefaultPackage())
    {
      List<ExternalEvent> unpackagedExternalEvents = new ArrayList<ExternalEvent>();
      for (ExternalEventVO externalEventVO : getDataAccess().getExternalEvents(false))
      {
        if (!isPackaged(externalEventVO))
        {
          ExternalEvent externalEvent = new ExternalEvent(externalEventVO, aPackage);
          externalEvent.addElementChangeListener(this);
          unpackagedExternalEvents.add(externalEvent);
        }
      }
      Collections.sort(unpackagedExternalEvents);
      return unpackagedExternalEvents;
    }
    else
    {
      List<ExternalEvent> packagedExternalEvents = new ArrayList<ExternalEvent>();
      for (ExternalEventVO externalEventVO : getDataAccess().getExternalEvents(false))
      {
        if (aPackage.getPackageVO().containsExternalEvent(externalEventVO.getId()))
        {
          ExternalEvent externalEvent = new ExternalEvent(externalEventVO, aPackage);
          if (!packagedExternalEvents.contains(externalEvent))
          {
            externalEvent.addElementChangeListener(this);
            packagedExternalEvents.add(externalEvent);
          }
        }
      }
      Collections.sort(packagedExternalEvents);
      return packagedExternalEvents;
    }
  }

  /**
   * Returns the task templates belonging to a package version.
   */
  private List<TaskTemplate> findTaskTemplates(WorkflowPackage aPackage)
  {
    if (aPackage.isDefaultPackage())
    {
      List<TaskTemplate> unpackagedTaskTemplates = new ArrayList<TaskTemplate>();
      for (TaskVO taskVO : getDataAccess().getTaskTemplates(false))
      {
        if (!isPackaged(taskVO))
        {
          TaskTemplate taskTemplate = new TaskTemplate(taskVO, aPackage);
          taskTemplate.addElementChangeListener(this);
          unpackagedTaskTemplates.add(taskTemplate);
        }
      }
      Collections.sort(unpackagedTaskTemplates);
      return unpackagedTaskTemplates;
    }
    else
    {
      List<TaskTemplate> packagedTaskTemplates = new ArrayList<TaskTemplate>();
      for (TaskVO taskVO : getDataAccess().getTaskTemplates(false))
      {
        if (aPackage.getPackageVO().containsTaskTemplate(taskVO.getTaskId()))
        {
          TaskTemplate taskTemplate = new TaskTemplate(taskVO, aPackage);
          if (!packagedTaskTemplates.contains(taskTemplate))
          {
            taskTemplate.addElementChangeListener(this);
            packagedTaskTemplates.add(taskTemplate);
          }
        }
      }
      Collections.sort(packagedTaskTemplates);
      return packagedTaskTemplates;
    }
  }

  public void reloadActivityImplementors()
  {
    getDataAccess().getActivityImplementors(true);
    activityImpls.clear();
    for (WorkflowPackage pkg : getTopLevelUserVisiblePackages())
      pkg.setActivityImpls(findActivityImplementors(pkg));
  }

  private Map<String,ActivityImpl> activityImpls = new HashMap<String,ActivityImpl>();
  public ActivityImpl getActivityImpl(String className)
  {
    ActivityImpl impl = activityImpls.get(className);
    if (impl == null)
      impl = getDesignerProxy().getGenericActivityImpl(className);

    return impl;
  }
  public void addActivityImpl(ActivityImpl activityImpl)
  {
    activityImpls.put(activityImpl.getImplClassName(), activityImpl);
  }
  public void setActivityImplClass(String className, ActivityImpl activityImpl)
  {
    activityImpls.put(className, activityImpl);
  }

  public ActivityImpl getActivityImpl(Long activityImplId)
  {
    for (WorkflowPackage packageVersion : getTopLevelUserVisiblePackages())
    {
      for (ActivityImpl activityImpl : packageVersion.getActivityImpls())
      {
        if (activityImpl.getId().equals(activityImplId))
          return activityImpl;
      }
    }
    return null;
  }

  /**
   * Returns the activity implementors events belonging to a package version.
   */
  public List<ActivityImpl> findActivityImplementors(WorkflowPackage aPackage)
  {
    List<ActivityImpl> impls = new ArrayList<ActivityImpl>();
    List<ActivityImplementorVO> implVOs = getDataAccess().getActivityImplementors(false);
    for (ActivityImplementorVO implVO : implVOs)
    {
      if ((aPackage.isDefaultPackage() && !isPackaged(implVO))
          || (!aPackage.isDefaultPackage() && aPackage.getPackageVO().containsActivityImpl(implVO.getImplementorClassName())))
      {
        ActivityImpl activityImpl = activityImpls.get(implVO.getImplementorClassName());
        if (activityImpl == null)
        {
          activityImpl = new ActivityImpl(implVO, aPackage);
          activityImpl.addElementChangeListener(this);
          activityImpls.put(implVO.getImplementorClassName(), activityImpl);
        }
        if (!impls.contains(activityImpl))
          impls.add(activityImpl);
      }
    }
    Collections.sort(impls);
    return impls;
  }

  /**
   * Returns the workflow assets belonging to a package version.
   */
  private List<WorkflowAsset> findWorkflowAssets(WorkflowPackage aPackage)
  {
    List<WorkflowAsset> assets = new ArrayList<WorkflowAsset>();
    if (!aPackage.isDefaultPackage())
    {
      for (RuleSetVO ruleSetVO : getDataAccess().getRuleSets(false))
      {
        if (aPackage.getPackageVO().containsRuleSet(ruleSetVO.getId()))
        {
          WorkflowAsset asset = WorkflowAssetFactory.createAsset(ruleSetVO, aPackage);
          if (asset != null)
          {
            asset.addElementChangeListener(this);
            if (!asset.isArchived())
              WorkflowAssetFactory.registerAsset(asset);
            assets.add(asset);
            if (RuleSetVO.PAGELET.equals(asset.getLanguage()) && !BAM_PAGELET.equals(asset.getName())) // BAM's special
            {
              if (pageletTabs == null)
                pageletTabs = new ArrayList<PageletTab>();
              String name = asset.getName();
              if (name.indexOf('.') >= 0)
                name = name.substring(0, name.lastIndexOf('.'));
              PageletTab pageletTab = new PageletTab(name, designerProxy.loadWorkflowAsset(asset).getContent());
              // check if already added from a later package version
              boolean already = false;
              for (PageletTab existing : pageletTabs)
              {
                if (existing.getId().equals(pageletTab.getId()))
                {
                  already = true;
                  break;
                }
              }
              if (!already)
                pageletTabs.add(pageletTab);
            }
          }
        }
      }
      Collections.sort(assets);
    }
    return assets;
  }

  public void addPackage(WorkflowPackage newPackage)
  {
    getTopLevelUserVisiblePackages().add(newPackage);
    getTopLevelPackages().add(newPackage);
    Collections.sort(topLevelUserVisiblePackages);
  }

  public void removePackage(WorkflowPackage toRemove)
  {
    getTopLevelUserVisiblePackages().remove(toRemove);
    getTopLevelPackages().remove(toRemove);
  }

  public WorkflowPackage getPackage(String packageName)
  {
    for (WorkflowPackage topLevelPackage : getTopLevelUserVisiblePackages())
    {
      if (topLevelPackage.getName().equals(packageName))
        return topLevelPackage;
    }
    return null;
  }

  public boolean packageNameExists(String packageName)
  {
    for (WorkflowPackage aPackage : getTopLevelPackages())
    {
      if (aPackage.getName().equals(packageName))
        return true;
    }
    return false;
  }

  public WorkflowPackage getProcessPackage(Long processId)
  {
    for (WorkflowProcess pv : getAllProcessVersions())
    {
      if (pv.getId().equals(processId))
        return pv.getPackage();
    }
    return getDefaultPackage();
  }

  public boolean workflowAssetNameExists(String name)
  {
    for (RuleSetVO ruleSetVO : getDataAccess().getAllRuleSets(false))
    {
      if (ruleSetVO.getName().equals(name))
        return true;
    }
    return false;
  }

  private List<WorkflowProcess> getUnpackagedProcesses()
  {
    if (unpackagedProcesses == null)
      unpackagedProcesses = findUnpackagedProcesses();
    return unpackagedProcesses;
  }

  public List<WorkflowAsset> getUnpackagedWorkflowAssets()
  {
    // should be called after findArchivedPackages()
    if (archivedDefaultPackage == null)
      findArchivedPackages(null, false);

    if (unpackagedWorkflowAssets == null)
    {
      unpackagedWorkflowAssets = new ArrayList<WorkflowAsset>();
      for (Folder folder : findUnpackagedAssetFolders())
      {
        for (WorkflowElement element : folder.getChildren())
          unpackagedWorkflowAssets.add((WorkflowAsset)element);
      }
    }

    return unpackagedWorkflowAssets;
  }

  public void removeWorkflowAsset(WorkflowAsset asset)
  {
    for (WorkflowPackage pkg : getTopLevelUserVisiblePackages())
      pkg.getAssets().remove(asset);
    getUnpackagedWorkflowAssets().remove(asset);
  }

  /**
   * Gets the top-level unpackaged version for all processes.
   * Side effect: if a version is later than than in any package, it's added to the default package.
   * The results are populated with their unpackaged descendant process versions.
   */
  private List<WorkflowProcess> findUnpackagedProcesses()
  {
    List<WorkflowProcess> unpackagedProcesses = new ArrayList<WorkflowProcess>();

    for (ProcessVO processVO : getDataAccess().getProcesses(false))
    {
      ProcessVO topUnpackaged = null;
      ProcessVO toCheck = processVO;

      // if process version is later than that in any package,
      // it belongs in the top-level default package
      boolean toCheckIsLatest = true;
      for (WorkflowPackage pkg : getTopLevelPackages())
      {
        if (pkg.isDefaultPackage())
          continue;
        WorkflowProcess packaged = pkg.getProcess(processVO.getProcessName());
        if (packaged != null && packaged.getVersion() >= toCheck.getVersion())
        {
          toCheckIsLatest = false;
          break;
        }
      }
      if (toCheckIsLatest)
      {
        WorkflowProcess pv = new WorkflowProcess(this, toCheck);
        getDefaultPackage().addProcess(pv);
        pv.addElementChangeListener(this);
        toCheck = toCheck.getPrevVersion();
      }

      // add the hierarchy of old unpackaged processes
      while (toCheck != null)
      {
        if (!isPackaged(toCheck))
        {
          topUnpackaged = toCheck;
          break;
        }
        toCheck = toCheck.getPrevVersion();
      }
      if (topUnpackaged != null)
      {
        WorkflowProcess processVersion = new WorkflowProcess(this, topUnpackaged);
        processVersion.setDescendantProcessVersions(getUnpackagedDescendantProcesses(processVersion));
        unpackagedProcesses.add(processVersion);
      }
    }
    Collections.sort(unpackagedProcesses);

    return unpackagedProcesses;
  }

  /**
   * Returns the folders for unpackaged archived workflow assets.
   * Side-effect: If an unpackaged version is later than any package, add to default package.
   */
  private List<Folder> findUnpackagedAssetFolders()
  {
    CodeTimer timer = new CodeTimer("findUnpackagedAssetFolders()");

    Map<String,Folder> upAssetFolders = new HashMap<String,Folder>();

    Map<String,List<RuleSetVO>> nonLatestUnpackaged = new HashMap<String,List<RuleSetVO>>();

    for (RuleSetVO ruleSetVO : getDataAccess().getRuleSets(false))
    {
      if (!isPackaged(ruleSetVO))
      {
        // if version is later than that in any non-archived package,
        // it belongs in the top-level default package
        boolean laterThanPackaged = true;
        for (WorkflowPackage pkg : getTopLevelPackages())
        {
          WorkflowAsset packaged = pkg.getAsset(ruleSetVO.getName());
          if (packaged != null && packaged.getVersion() >= ruleSetVO.getVersion())
          {
            laterThanPackaged = false;
            break;
          }
        }

        boolean latestUnpackaged = true;
        if (laterThanPackaged)
        {
          WorkflowAsset defPkgAsset = getDefaultPackage().getAsset(ruleSetVO.getName());
          if (defPkgAsset != null)
          {
            if (defPkgAsset.getVersion() > ruleSetVO.getVersion())
            {
              latestUnpackaged = false;
            }
            else if (defPkgAsset.getVersion() < ruleSetVO.getVersion())
            {
              defPkgAsset.removeElementChangeListener(this);
              getDefaultPackage().removeAsset(defPkgAsset);
              WorkflowAssetFactory.deRegisterAsset(defPkgAsset);
              List<RuleSetVO> list = nonLatestUnpackaged.get(ruleSetVO.getName());
              if (list == null)
              {
                list = new ArrayList<RuleSetVO>();
                nonLatestUnpackaged.put(ruleSetVO.getName(), list);
              }
              list.add(defPkgAsset.getRuleSetVO());
            }
          }
        }

        if (laterThanPackaged && latestUnpackaged)
        {
          WorkflowAsset asset = WorkflowAssetFactory.createAsset(ruleSetVO, getDefaultPackage());
          if (asset != null)
          {
            asset.addElementChangeListener(this);
            getDefaultPackage().addAsset(asset);
            WorkflowAssetFactory.registerAsset(asset);
          }
        }
        else
        {
          List<RuleSetVO> list = nonLatestUnpackaged.get(ruleSetVO.getName());
          if (list == null)
          {
            list = new ArrayList<RuleSetVO>();
            nonLatestUnpackaged.put(ruleSetVO.getName(), list);
          }
          list.add(ruleSetVO);
        }
      }
    }

    for (String rsName : nonLatestUnpackaged.keySet())
    {
      List<RuleSetVO> list = nonLatestUnpackaged.get(rsName);
      Collections.sort(list, new Comparator<RuleSetVO>()
      {
        public int compare(RuleSetVO rs1, RuleSetVO rs2)
        {
          return rs2.getVersion() - rs1.getVersion();
        }
      });

      for (RuleSetVO rs : list)
      {
        WorkflowAsset asset = WorkflowAssetFactory.createAsset(rs, archivedDefaultPackage);
        if (asset != null)
        {
          Folder folder = upAssetFolders.get(asset.getName());
          if (folder == null)
          {
            folder = WorkflowAssetFactory.createAssetFolder(asset);
            upAssetFolders.put(asset.getName(), folder);
          }
          else
          {
            folder.getChildren().add(asset);
          }
        }
      }
    }

    List<Folder> folders = new ArrayList<Folder>();
    folders.addAll(upAssetFolders.values());
    Collections.sort(folders);

    timer.stopAndLog();

    return folders;
  }

  private boolean isPackaged(ProcessVO processVO)
  {
    for (PackageVO packageVO : getDataAccess().getPackages(false))
    {
      if (packageVO.containsProcess(processVO.getProcessId()))
        return true;
    }
    return false;
  }

  /**
   * Returns the descendant process versions for a top-level process
   * version (includes the top-level version itself as a descendant).
   */
  private List<WorkflowProcess> getUnpackagedDescendantProcesses(WorkflowProcess topLevelProcessVersion)
  {
    List<WorkflowProcess> processVersions = new ArrayList<WorkflowProcess>();
    // clone top-level process for first child
    WorkflowProcess processVersion = new WorkflowProcess(topLevelProcessVersion);
    // walk down the chain
    while (processVersion != null)
    {
      if (processVersion.getPackage() == null)
      {
        processVersion.setTopLevelVersion(topLevelProcessVersion);
        processVersions.add(processVersion);
      }
      processVersion = processVersion.getPreviousVersion();
    }
    // no need to sort
    return processVersions;
  }

  private boolean isPackaged(ExternalEventVO externalEventVO)
  {
    for (PackageVO packageVO : getDataAccess().getPackages(false))
    {
      if (packageVO.containsExternalEvent(externalEventVO.getId()))
        return true;
    }
    return false;
  }

  private boolean isPackaged(TaskVO taskVO)
  {
    for (PackageVO packageVO : getDataAccess().getPackages(false))
    {
      if (packageVO.containsTaskTemplate(taskVO.getTaskId()))
        return true;
    }
    return false;
  }

  private boolean isPackaged(ActivityImplementorVO activityImplVO)
  {
    for (PackageVO packageVO : getDataAccess().getPackages(false))
    {
      if (packageVO.containsActivityImpl(activityImplVO.getImplementorClassName()))
        return true;
    }
    return false;
  }

  private boolean isPackaged(RuleSetVO ruleSetVO)
  {
    for (PackageVO packageVO : getDataAccess().getPackages(false))
    {
      if (packageVO.containsRuleSet(ruleSetVO.getId()))
        return true;
    }
    return false;
  }

  public Folder getArchivedPackageFolder()
  {
    if (archivedPackageFolder == null)
      archivedPackageFolder = findArchivedPackages(null, false);
    return archivedPackageFolder;
  }

  public Folder getArchivedUserVisiblePackagesFolder(ProgressMonitor progressMonitor)
  {
    if (archivedUserVisiblePackagesFolder == null)
      archivedUserVisiblePackagesFolder = findArchivedPackages(progressMonitor, true);
    return archivedUserVisiblePackagesFolder;
  }

  public Folder getArchivedUserVisiblePackagesFolder()
  {
    return getArchivedUserVisiblePackagesFolder(null);
  }

  /**
   * Returns all top-level archived (non-latest) packages, populated with
   * their descendants which include the appropriate process versions.
   * Uses 10% of progressMonitor.
   */
  private Folder findArchivedPackages(ProgressMonitor progressMonitor, boolean visiblePkgsOnly)
  {
    CodeTimer timer = new CodeTimer("findAuthorizedArchivedPackages()");

    if (progressMonitor != null)
      progressMonitor.subTask("Organizing packages");

    Folder archivedPackageFolder = new Folder("Archive", this);
    archivedPackageFolder.setIcon("archive.gif");
    List<WorkflowPackage> topLevelArchivedPackageList = new ArrayList<WorkflowPackage>();
    List<WorkflowPackage> allArchivedPackages = new ArrayList<WorkflowPackage>();
    for (PackageVO packageVO : getDataAccess().getPackages(false))
    {
      if (isShowDefaultPackage() || !packageVO.isDefaultPackage())
      {
        WorkflowPackage potentialArchived = new WorkflowPackage(this, packageVO);
        if (!getTopLevelPackages().contains(potentialArchived))
        {
          potentialArchived.setArchived(true);
          potentialArchived.setProcesses(findProcesses(potentialArchived));
          potentialArchived.setAssets(findWorkflowAssets(potentialArchived));
          potentialArchived.setActivityImpls(findActivityImplementors(potentialArchived));
          potentialArchived.setExternalEvents(findExternalEvents(potentialArchived));
          potentialArchived.setTaskTemplates(findTaskTemplates(potentialArchived));
          allArchivedPackages.add(potentialArchived);
        }
      }
    }
    // organize the package version hierarchy
    Map<String,WorkflowPackage> topLevelArchivedPackages = new HashMap<String,WorkflowPackage>();
    for (WorkflowPackage archivedPackage : allArchivedPackages)
    {
      WorkflowPackage topLevel = topLevelArchivedPackages.get(archivedPackage.getName());
      if (!visiblePkgsOnly || (visiblePkgsOnly && archivedPackage.isVisible()))
        if (topLevel == null || archivedPackage.getPackageVO().getVersion() > topLevel.getPackageVO().getVersion())
          topLevelArchivedPackages.put(archivedPackage.getName(), archivedPackage);
    }
    for (WorkflowPackage topLevelArchivedPackage : topLevelArchivedPackages.values())
    {
      for (WorkflowPackage archivedPackage : allArchivedPackages)
      {
        if (topLevelArchivedPackage.getName().equals(archivedPackage.getName()))
        {
          if (!visiblePkgsOnly || (visiblePkgsOnly && archivedPackage.isVisible()))
          {
            if (topLevelArchivedPackage.equals(archivedPackage))
            {
              // clone top-level process for first child
              WorkflowPackage firstChild = new WorkflowPackage(topLevelArchivedPackage);
              firstChild.setArchived(true);
              firstChild.setTopLevelVersion(topLevelArchivedPackage);
              firstChild.setProcesses(topLevelArchivedPackage.getProcesses());
              firstChild.setAssets(topLevelArchivedPackage.getAssets());
              firstChild.setActivityImpls(topLevelArchivedPackage.getActivityImpls());
              firstChild.setExternalEvents(topLevelArchivedPackage.getExternalEvents());
              firstChild.setTaskTemplates(topLevelArchivedPackage.getTaskTemplates());
              topLevelArchivedPackage.addDescendantPackageVersion(firstChild);
            }
            else
            {
              archivedPackage.setTopLevelVersion(topLevelArchivedPackage);
              topLevelArchivedPackage.addDescendantPackageVersion(archivedPackage);
            }
          }
        }
      }
      topLevelArchivedPackageList.add(topLevelArchivedPackage);
      topLevelArchivedPackage.setArchivedFolder(archivedPackageFolder);
    }
    Collections.sort(topLevelArchivedPackageList);

    if (isShowDefaultPackage())
    {
      // add unpackaged (default package)
      PackageVO defaultPackageVO = new PackageVO();
      defaultPackageVO.setPackageId(new Long(0));
      defaultPackageVO.setPackageName(PackageVO.DEFAULT_PACKAGE_NAME);
      archivedDefaultPackage = new WorkflowPackage(this, defaultPackageVO);
      archivedDefaultPackage.setProcesses(findUnpackagedProcesses());
      archivedDefaultPackage.setChildFolders(findUnpackagedAssetFolders());
      archivedDefaultPackage.setArchived(true);
      topLevelArchivedPackageList.add(0, archivedDefaultPackage);
    }

    List<WorkflowElement> children = new ArrayList<WorkflowElement>();
    children.addAll(topLevelArchivedPackageList);
    archivedPackageFolder.setChildren(children);

    if (progressMonitor != null)
      progressMonitor.progress(10);

    timer.stopAndLog();
    return archivedPackageFolder;
  }

  /**
   * Returns all process versions regardless of packaging and archive status.
   */
  public List<WorkflowProcess> getAllProcesses()
  {
    List<WorkflowProcess> allProcesses = new ArrayList<WorkflowProcess>();
    for (WorkflowPackage topLevelPackage : getTopLevelUserVisiblePackages())
      allProcesses.addAll(topLevelPackage.getProcesses());
    if (isShowDefaultPackage())
    {
      for (WorkflowProcess pv : getUnpackagedProcesses())
      {
        if (!allProcesses.contains(pv))
          allProcesses.add(pv);
      }
    }
    for (WorkflowElement child : getArchivedUserVisiblePackagesFolder().getChildren())
    {
      WorkflowPackage topLevelArchivedPackage = (WorkflowPackage) child;
      if (topLevelArchivedPackage.getDescendantPackageVersions() != null)
      {
        for (WorkflowPackage archivedPackage : topLevelArchivedPackage.getDescendantPackageVersions())
        {
          for (WorkflowProcess pv : archivedPackage.getProcesses())
          {
            if (!allProcesses.contains(pv))
              allProcesses.add(pv);
          }
        }
      }
    }
    return allProcesses;
  }

  // TO get all the processes that are in specified package (including archived ones)
  public List<WorkflowProcess> getAllProcessesByPackage(String packageName)
  {
    List<WorkflowProcess> allProcesses = new ArrayList<WorkflowProcess>();
    for (WorkflowPackage topLevelPackage : getTopLevelUserVisiblePackages())
    {
      if (packageName.equals(topLevelPackage.getName()))
      {
        allProcesses.addAll(topLevelPackage.getProcesses());
        break;
      }
    }
    for (WorkflowElement child : getArchivedUserVisiblePackagesFolder().getChildren())
    {
      WorkflowPackage topLevelArchivedPackage = (WorkflowPackage) child;
      if (packageName.equals(topLevelArchivedPackage.getName()))
      {
        if (topLevelArchivedPackage.getDescendantPackageVersions() != null)
        {
          for (WorkflowPackage archivedPackage : topLevelArchivedPackage.getDescendantPackageVersions())
          {
            for (WorkflowProcess pv : archivedPackage.getProcesses())
            {
              if (!allProcesses.contains(pv))
                allProcesses.add(pv);
            }
          }
        }
        break;
      }
    }
    return allProcesses;
  }

  private List<WorkflowAsset> assetsNotForCurrentUser;

  /**
   * Returns all assets regardless of user access, packaging and archive status.
   */
  public List<WorkflowAsset> getAllWorkflowAssets()
  {
    List<WorkflowAsset> allAssets = new ArrayList<WorkflowAsset>();

    allAssets.addAll(getTopLevelWorkflowAssets());
    if (isShowDefaultPackage())
      allAssets.addAll(getUnpackagedWorkflowAssets());

    Map<String,WorkflowAsset> latestPackaged = new HashMap<String,WorkflowAsset>();
    for (WorkflowAsset asset : allAssets)
    {
      String key = (asset.isInDefaultPackage() ? "" : asset.getPackage().getName() + "/") + asset.getName() + asset.getVersionLabel();
      latestPackaged.put(key, asset);
    }

    // get archived def docs if they don't match any non-archived version
    for (WorkflowElement child : getArchivedPackageFolder().getChildren())
    {
      WorkflowPackage topLevelArchivedPackage = (WorkflowPackage) child;
      if (topLevelArchivedPackage.getDescendantPackageVersions() != null)
      {
        for (WorkflowPackage archivedPackage : topLevelArchivedPackage.getDescendantPackageVersions())
        {
          for (WorkflowAsset archivedAsset : archivedPackage.getAssets())
          {
            String key = (archivedAsset.isInDefaultPackage() ? "" : archivedAsset.getPackage().getName() + "/") + archivedAsset.getName() + archivedAsset.getVersionLabel();
            WorkflowAsset curPkgMax = latestPackaged.get(key);
            if (curPkgMax == null || archivedAsset.getPackage().getVersion() > curPkgMax.getPackage().getVersion())
              latestPackaged.put(key, archivedAsset);
          }
        }
      }
    }

    // reset list to include those added above
    allAssets = new ArrayList<WorkflowAsset>(latestPackaged.values());

    // account for assets not visible to user (cached separately)
    if (assetsNotForCurrentUser == null)
    {
      assetsNotForCurrentUser = new ArrayList<WorkflowAsset>();
      for (RuleSetVO rs : getDataAccess().getAllRuleSets(false))
      {
        WorkflowAsset asset = WorkflowAssetFactory.createAsset(rs, findPackage(rs));
        if (asset != null && !allAssets.contains(asset))
          assetsNotForCurrentUser.add(asset);
      }
    }
    allAssets.addAll(assetsNotForCurrentUser);

    return allAssets;
  }

  /**
   * Returns all top level assets regardless of packaging.
   */
  public List<WorkflowAsset> getTopLevelWorkflowAssets()
  {
    List<WorkflowAsset> allAssets = new ArrayList<WorkflowAsset>();

    for (WorkflowPackage topLevelPackage : getTopLevelUserVisiblePackages())
    {
      allAssets.addAll(topLevelPackage.getAssets());
    }

    return allAssets;
  }

  /**
   * Returns all external events regardless of packaging and archive status.
   */
  /**
   * Returns all event handlers, regardless of packaging.
   */
  public List<ExternalEvent> getAllExternalEvents()
  {
    List<ExternalEvent> allHandlers = new ArrayList<ExternalEvent>();
    for (WorkflowPackage pkg : getTopLevelPackages())
    {
      for (ExternalEvent handler : pkg.getExternalEvents())
      {
        if (!allHandlers.contains(handler))
          allHandlers.add(handler);
      }
    }
    for (WorkflowElement topLevelArchived : getArchivedPackageFolder().getChildren())
    {
      if (topLevelArchived instanceof WorkflowPackage)
      {
        WorkflowPackage topLevelPkg = (WorkflowPackage) topLevelArchived;
        if (topLevelPkg.hasDescendantPackageVersions())
        {
          for (WorkflowPackage pkg : topLevelPkg.getDescendantPackageVersions())
          {
            for (ExternalEvent handler : pkg.getExternalEvents())
            {
              if (!allHandlers.contains(handler))
                allHandlers.add(handler);
            }
          }
        }
      }
    }
    Collections.sort(allHandlers);
    return allHandlers;
  }

  /**
   * Returns all processes, regardless of packaging or version.
   */
  public List<WorkflowProcess> getAllProcessVersions()
  {
    List<WorkflowProcess> allProcessVersions = new ArrayList<WorkflowProcess>();
    for (WorkflowProcess processVersion : getAllProcesses())
    {
      if (processVersion.hasDescendantProcessVersions())
      {
        for (WorkflowProcess descendant : processVersion.getDescendantProcessVersions())
          allProcessVersions.add(descendant);
      }
      else
      {
        allProcessVersions.add(processVersion);
      }
    }
    Collections.sort(allProcessVersions);
    return allProcessVersions;
  }

  public WorkflowProcess getProcess(String name, int version)
  {
    return getProcess(name, WorkflowProcess.getVersionString(version));
  }

  public WorkflowProcess getProcess(String name, String version)
  {
    for (WorkflowProcess processVersion : getAllProcessVersions())
    {
      if (processVersion.getName().equals(name) && processVersion.getVersionString().equals(version))
        return processVersion;
    }
    return null;
  }

  public WorkflowProcess getProcess(String name)
  {
    WorkflowProcess latest = null;
    for (WorkflowProcess processVersion : getAllProcessVersions())
    {
      if (processVersion.getName().equals(name) && (latest == null || latest.getVersion() < processVersion.getVersion()))
        latest = processVersion;
    }
    return latest;
  }

  public WorkflowProcess getProcess(Long processId)
  {
    for (WorkflowProcess processVersion : getAllProcessVersions())
    {
      if (processVersion.getId().equals(processId))
        return processVersion;
    }
    return null;
  }

  public ExternalEvent getExternalEvent(Long externalEventId)
  {
    for (WorkflowPackage packageVersion : getTopLevelUserVisiblePackages())
    {
      for (ExternalEvent externalEvent : packageVersion.getExternalEvents())
      {
        if (externalEvent.getId().equals(externalEventId))
          return externalEvent;
      }
    }
    return null;
  }

  public boolean externalEventMessagePatternExists(String messagePattern)
  {
    for (ExternalEventVO externalEvent : getDataAccess().getExternalEvents(false))
    {
      if (externalEvent.getEventName().equals(messagePattern))
        return true;
    }
    return false;
  }

  public boolean activityImplLabelExists(String label)
  {
    for (ActivityImplementorVO activityImplVO : getDataAccess().getActivityImplementors(false))
    {
      if (activityImplVO.getLabel().equals(label))
        return true;
    }
    return false;
  }

  public boolean activityImplClassExists(String className)
  {
    for (ActivityImplementorVO activityImplVO : getDataAccess().getActivityImplementors(false))
    {
      if (activityImplVO.getImplementorClassName().equals(className))
        return true;
    }
    return false;
  }

  /**
   * Assumes package is not default and language is not null.
   */
  public WorkflowAsset getAsset(String pkg, String name, String language, int version)
  {
    for (WorkflowAsset asset : getAllWorkflowAssets())
    {
      if (!asset.isInDefaultPackage() && pkg.equals(asset.getPackage().getName())
          && language.equals(asset.getLanguage()) && name.equals(asset.getName()) && version == asset.getVersion())
        return asset;
    }
    return null;
  }

  public WorkflowAsset getAsset(String name, String language, int version)
  {
    if (language == null)
      language = "";  // language can be empty for pre-5.0 MDW versions
    for (WorkflowAsset asset : getAllWorkflowAssets())
    {
      if (asset.getName().equals(name) && asset.getLanguage().equals(language) && asset.getVersion() == version)
        return asset;
    }
    return null;
  }

  public WorkflowAsset getAsset(String name)
  {
    int slash = name.indexOf('/');
    if (slash > 0)
      return getAsset(name.substring(0, slash), name.substring(slash + 1));

    WorkflowAsset latest = null;
    for (WorkflowAsset asset : getAllWorkflowAssets())
    {
      if (asset.getName().equals(name) && (latest == null || latest.getVersion() < asset.getVersion()))
        latest = asset;
    }
    return latest;
  }

  public WorkflowAsset getAsset(String pkg, String name)
  {
    if (pkg == null || pkg.length() == 0)
      return getAsset(name);

    WorkflowAsset latest = null;
    for (WorkflowAsset asset : getAllWorkflowAssets())
    {
      if (!asset.isInDefaultPackage() && pkg.equals(asset.getPackage().getName()) && name.equals(asset.getName())
          && (latest == null || latest.getVersion() < asset.getVersion()))
        latest = asset;
    }
    return latest;
  }

  public WorkflowAsset getAsset(Long assetId)
  {
    for (WorkflowAsset asset : getAllWorkflowAssets())
    {
      if (asset.getId().equals(assetId))
        return asset;
    }
    return null;
  }

  /**
   * Combines asset-defined tests with old tests.
   */
  public List<AutomatedTestCase> getTestCases()
  {
    List<AutomatedTestCase> cases = new ArrayList<AutomatedTestCase>();
    if (isFilePersist())
    {
      for (WorkflowPackage pkg : getTopLevelPackages())
      {
        for (AutomatedTestCase testCase : pkg.getTestCases())
          cases.add(testCase);
      }
    }
    for (AutomatedTestCase legacyCase : getLegacyTestSuite().getTestCases())
      cases.add(legacyCase);

    return cases;
  }

  public List<String> getTestCaseStringList()
  {
    List<String> lst = new ArrayList<String>();
    for (AutomatedTestCase testCase : getTestCases())
      lst.add(testCase.getPath());
    return lst;
  }

  public void fireTestCaseStatusChange(AutomatedTestCase testCase, String newStatus)
  {
    fireElementChangeEvent(testCase, ChangeType.STATUS_CHANGE, newStatus);
  }

  public static final String DEFAULT_TEST_RESULTS_PATH = "testResults";
  public static final String LOAD_TEST_RESULTS_FILE = "mdw-load-test-results.xml";
  private static final String PREF_TEST_RESULTS_PATH = "MdwTestResultsPath";
  private Map<String,String> testResultsPaths = new HashMap<String,String>();
  public String getTestResultsPath(String type)
  {
    String resultsPath = testResultsPaths.get(type);
    if (resultsPath == null)
    {
      String pref = getPersistentProperty(type + "_"+ PREF_TEST_RESULTS_PATH);
      if (pref == null)
        resultsPath = DEFAULT_TEST_RESULTS_PATH;
      else
        resultsPath = pref;
      testResultsPaths.put(type, resultsPath);
    }
    return resultsPath;
  }
  public void setTestResultsPath(String type, String resultsPath)
  {
    setPersistentProperty(type + "_" + PREF_TEST_RESULTS_PATH, resultsPath);
    testResultsPaths.put(type, resultsPath);
  }
  public File getTestResultsDir(String type)
  {
    return new File(getProjectDir() + "/" + getTestResultsPath(type));
  }
  public File getFunctionTestResultsDir()
  {
    return getTestResultsDir(AutomatedTestCase.FUNCTION_TEST);
  }
  public File getLoadTestResultsDir()
  {
    return getTestResultsDir(AutomatedTestCase.LOAD_TEST);
  }
  public File getFunctionTestResultsFile()
  {
    File resultsDir = getFunctionTestResultsDir();
    String summaryFile = "mdw-function-test-results.json";
    // fall back to old XML results
    if (!new File(resultsDir + "/" + summaryFile).exists() && new File(resultsDir + "/mdw-function-test-results.xml").exists())
        summaryFile = "mdw-function-test-results.xml";
    return new File(resultsDir + "/" + summaryFile);
  }
  public File getLoadTestResultsFile()
  {
    return new File(getLoadTestResultsDir() + "/" + LOAD_TEST_RESULTS_FILE);
  }
  public File getOldTestCasesDir()
  {
    return new File(getProjectDir() + "/testCases");
  }
  public IFolder getOldTestCasesFolder()
  {
    return getProjectFolder("/testCases");
  }

  public void refreshLegacyTestSuite()
  {
    legacyTestSuite = null;
    AutomatedTestSuite testSuite = getLegacyTestSuite();
    testSuite.fireElementChangeEvent(testSuite, ChangeType.SETTINGS_CHANGE, null);
    try
    {
      getOldTestCasesFolder().getParent().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }
  }

  /**
   * Legacy tests.
   */
  public AutomatedTestSuite getLegacyTestSuite()
  {
    if (legacyTestSuite == null)
    {
      legacyTestSuite = findLegacyTests();
      this.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, legacyTestSuite);
    }

    return legacyTestSuite;
  }

  public AutomatedTestSuite findLegacyTests()
  {
    AutomatedTestSuite testSuite = new AutomatedTestSuite(this);

    if (testSuite.readLegacyCases())
    {
      for (AutomatedTestCase testCase : testSuite.getTestCases())
      {
        testCase.addElementChangeListener(this);
        for (LegacyExpectedResults expectedResult : testCase.getLegacyExpectedResults())
          expectedResult.addElementChangeListener(this);
        for (com.centurylink.mdw.plugin.designer.model.File file : testCase.getFiles())
          file.addElementChangeListener(this);
      }
    }

    testSuite.setLabel("Legacy Tests");
    testSuite.setIcon("folder.gif");
    return testSuite;
  }

  public String toString()
  {
    String dbSchemaInfo = "";
    if (getDesignerProxy() != null && getDesignerProxy().getDesignerDataAccess() != null)
    {
      dbSchemaInfo = "databaseSchemaVersion: " + getDesignerProxy().getDesignerDataAccess().getDatabaseSchemaVersion() + "\n"
        + "databaseSupportedSchemaVerison: " + getDesignerProxy().getDesignerDataAccess().getSupportedSchemaVersion() + "\n\n";
    }

    String proj = "WorkflowProject:\n---------------\n"
      + "remote: " + this.isRemote() + "\n"
      + "cloud: " + this.isCloudProject() + "\n"
      + "sourceProjectName: " + this.sourceProjectName + "\n"
      + "mdwVersion: " + this.getMdwVersion() + "\n\n";

    if (isFilePersist())
    {
      proj += getMdwVcsRepository() + "\n\n"
           + getServerSettings();
    }
    else
    {
      proj += getMdwDataSource() + "\n\n"
           + dbSchemaInfo
           + getServerSettings() + "\n\n";
    }

    return proj;
  }

  public int compareTo(WorkflowProject other)
  {
    return this.getSourceProjectName().compareToIgnoreCase(other.getSourceProjectName());
  }

  public boolean equals(Object other)
  {
    if (!(other instanceof WorkflowProject))
      return false;

    return this.getSourceProjectName().equals(((WorkflowProject)other).getSourceProjectName());
  }

  public IPath getMetaDataLoc() throws CoreException
  {
    return MdwPlugin.getMetaDataLoc(getSourceProjectName());
  }

  public void fireElementChangeEvent(ChangeType changeType, Object newValue)
  {
    if (changeType.equals(ChangeType.SETTINGS_CHANGE))
    {
      // force just-in-time reinitialization
      if (newValue instanceof JdbcDataSource)
      {
        clear();
      }
      else if (newValue instanceof ServerSettings)
      {
        remoteAppSummary = null;
        if (isRemote())
          mdwVersion = null;
        if (designerProxy != null)
          designerProxy.setRestfulServerWebUrl(getServiceUrl());
      }
    }
    super.fireElementChangeEvent(this, changeType, newValue);
  }

  public void elementChanged(ElementChangeEvent ece)
  {
    // pass it on
    for (ElementChangeListener listener : getElementChangeListeners())
      listener.elementChanged(ece);
  }

  @Override
  public void removeElementChangeListener(ElementChangeListener listener)
  {
    super.removeElementChangeListener(listener);

    if (listener instanceof ProcessExplorerContentProvider)
    {
      // deregister from the elements that i'm a proxy listener for
      if (topLevelPackages != null)
      {
        for (WorkflowPackage topLevelPackage : topLevelPackages)
        {
          topLevelPackage.removeElementChangeListener(this);
          for (WorkflowProcess process : topLevelPackage.getProcesses())
            process.removeElementChangeListener(this);
          for (ExternalEvent extEvent : topLevelPackage.getExternalEvents())
            extEvent.removeElementChangeListener(this);
          for (WorkflowAsset asset : topLevelPackage.getAssets())
            asset.removeElementChangeListener(this);
        }
      }
    }
  }

  private NotificationChecker noticeChecker;

  public void initNoticeChecks()
  {
    setNoticeChecks(getNoticeChecks());
    if (noticeChecker != null)
      noticeChecker.setInterval(getNoticeCheckInterval());
  }

  public void shutdownNoticeChecks()
  {
    if (noticeChecker != null)
      noticeChecker.shutdown();
    noticeChecker = null;
  }

  public List<String> getNoticeChecks()
  {
    List<String> selectedNotices = new ArrayList<String>();
    String prefs = getPersistentProperty(NotificationChecker.NOTIF_CHECK_PREFS_KEY);
    if (prefs != null)
    {
      for (String s : prefs.split(","))
        selectedNotices.add(s);
    }
    return selectedNotices;
  }

  public void setNoticeChecks(final List<String> selectedNotices)
  {
    if (selectedNotices == null || selectedNotices.size() == 0)
    {
      if (noticeChecker != null)
        noticeChecker.shutdown();
      noticeChecker = null;
      setPersistentProperty(NotificationChecker.NOTIF_CHECK_PREFS_KEY, null);
    }
    else
    {
      if (noticeChecker == null)
      {
        Display.getDefault().asyncExec(new Runnable()
        {
          public void run()
          {
            noticeChecker = new NotificationChecker(Display.getCurrent(), WorkflowProject.this);
            noticeChecker.setNoticeTypes(selectedNotices);
            noticeChecker.setInterval(getNoticeCheckInterval());
            noticeChecker.startup();
          }
        });
      }
      else
      {
        noticeChecker.setNoticeTypes(selectedNotices);
      }

      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < selectedNotices.size(); i++)
      {
        String selectedNotice = selectedNotices.get(i);
        sb.append(selectedNotice);
        if (i < selectedNotices.size() - 1)
          sb.append(',');
      }

      setPersistentProperty(NotificationChecker.NOTIF_CHECK_PREFS_KEY, sb.toString());
    }
  }

  public int getNoticeCheckInterval()
  {
    String s = getPersistentProperty(NotificationChecker.NOTIF_CHECK_INTERVAL_KEY);
    if (s == null)
      s = "10"; // default to 10 minutes
    return Integer.parseInt(s);
  }

  public void setNoticeCheckInterval(int interval)
  {
    if (noticeChecker != null)
    {
      // respond immediately
      noticeChecker.shutdown();
      noticeChecker.setInterval(interval);
      noticeChecker.startup();
    }

    setPersistentProperty(NotificationChecker.NOTIF_CHECK_INTERVAL_KEY, String.valueOf(interval));
  }

  public boolean supportsExtensions() { return checkRequiredVersion(5, 1); }
  private List<ExtensionModule> extensionModules = new ArrayList<ExtensionModule>();
  public List<ExtensionModule> getExtensionModules() { return extensionModules; }
  public void setExtensionModules(List<ExtensionModule> modules) { this.extensionModules = modules; }

  public boolean hasExtension(String id)
  {
    for (ExtensionModule exMod : extensionModules)
    {
      if (exMod.getId().equals(id))
        return true;
    }
    return false;
  }
  public void addExtension(ExtensionModule extensionModule)
  {
    if (!extensionModules.contains(extensionModule))
      extensionModules.add(extensionModule);
  }
  public void removeExtension(ExtensionModule extensionModule)
  {
    if (extensionModules.contains(extensionModule))
      extensionModules.remove(extensionModule);
  }

  private Map<IFile,ArtifactResourceListener> artifactResourceListeners;

  public void addArtifactResourceListener(ArtifactResourceListener listener)
  {
    if (artifactResourceListeners == null)
    {
      artifactResourceListeners = new HashMap<IFile,ArtifactResourceListener>();
    }
    else if (artifactResourceListeners.containsKey(listener.getTempFile()))
    {
      ArtifactResourceListener oldListener = artifactResourceListeners.get(listener.getTempFile());
      removeArtifactResourceListener(oldListener); // avoid duplicate listening
    }

    ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
    artifactResourceListeners.put(listener.getTempFile(), listener);
  }

  public void removeArtifactResourceListener(ArtifactResourceListener listener)
  {
    if (artifactResourceListeners != null)
    {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
      artifactResourceListeners.remove(listener.getTempFile());
    }
  }

  /**
   * Forces refresh from the database or file system.
   */
  public void clear()
  {
    if (isLoaded())
    {
      List<WorkflowAsset> assets = archivedPackageFolder == null ? getTopLevelWorkflowAssets() : getAllWorkflowAssets();
      for (WorkflowAsset asset : assets)
        WorkflowAssetFactory.deRegisterAsset(asset);
      List<IFile> listenersToRemove = new ArrayList<IFile>();
      if (artifactResourceListeners != null)
      {
        for (IFile file : artifactResourceListeners.keySet())
          listenersToRemove.add(file);
      }
      for (IFile file : listenersToRemove)
        removeArtifactResourceListener(artifactResourceListeners.get(file));
    }
    if (designerProxy != null && designerProxy.isStubServerRunning())
      designerProxy.toggleStubServer();

    designerProxy = null;
    topLevelPackages = null;
    topLevelUserVisiblePackages = null;
    archivedPackageFolder = null;
    archivedUserVisiblePackagesFolder = null;
    legacyTestSuite = null;
    artifactResourceListeners = null;
    assetsNotForCurrentUser = null;
    shutdownNoticeChecks();
    scriptLibrariesSaved = false;
    pageletTabs = null;
    warn = false;
  }

  private boolean scriptLibrariesSaved = false;
  public boolean areScriptLibrariesSaved() { return scriptLibrariesSaved; }
  public void setScriptLibrariesSaved(boolean b) { scriptLibrariesSaved = b; }

  private boolean javaLibrariesSaved = false;
  public boolean areJavaLibrariesSaved() { return javaLibrariesSaved; }
  public void setJavaLibrariesSaved(boolean b) { javaLibrariesSaved = b; }

  public int getJmxPort()
  {
    int port = MDW_DEFAULT_REMOTE_JMX_PORT;
    String portPref = getPersistentProperty(MDW_REMOTE_JMX_PORT);
    if (portPref != null)
    {
      try
      {
        port = Integer.parseInt(portPref);
      }
      catch (NumberFormatException ex)
      {
        // revert to default
      }
    }
    return port;
  }

  public long getVisualVmId()
  {
    long vvmId = getId();
    String idPref = getPersistentProperty(WorkflowProject.MDW_VISUALVM_ID);
    if (idPref != null)
    {
      try
      {
        vvmId = Long.parseLong(idPref);
      }
      catch (NumberFormatException ex)
      {
        // revert to workflow project id
      }
    }
    return vvmId;
  }

  public void setPersistentProperty(String name, String value)
  {
    try
    {
      QualifiedName qName = new QualifiedName(MdwPlugin.getPluginId(), name);
      if (value != null && value.trim().length() == 0)
        getSourceProject().setPersistentProperty(qName, null);
      else
        getSourceProject().setPersistentProperty(qName, value);
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }
  }

  public String getPersistentProperty(String name)
  {
    try
    {
      QualifiedName qName = new QualifiedName(MdwPlugin.getPluginId(), name);
      return getSourceProject().getPersistentProperty(qName);
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
      return null;
    }
  }

  /**
   * methods specified by the IFacetWizardPage interface (called when adding workflow project facet)
   */
  public void setProjectName(String projectName)
  {
    if (isEarProject())
      setEarProjectName(projectName);
    else
      setSourceProjectName(projectName.endsWith("Ear") ? projectName.substring(0, projectName.length() - 3) : projectName);
  }

  public void setVersion(IProjectFacetVersion projectFacetVersion)
  {
  }

  public IStatus validate()
  {
    if (getMdwVersion() == null || getServerSettings().getHost() == null || getServerSettings().getPort() == 0 || getMdwDataSource().getJdbcUrl() == null)
    {
      return new Status(IStatus.ERROR, MdwPlugin.getPluginId(), 0, "Further configuration required.", null);
    }
    else
    {
      return Status.OK_STATUS;
    }
  }

  // transient flag for wizards that update the mdw.workflow facet
  private boolean skipFacetPostInstallUpdates = false;
  public boolean isSkipFacetPostInstallUpdates() { return skipFacetPostInstallUpdates; }
  public void setSkipFacetPostInstallUpdates(boolean skip) {this.skipFacetPostInstallUpdates = skip; }


  // user authorization methods

  public boolean isUserAuthorizedInAnyGroup(String role)
  {
    if (isFilePersist() && UserRoleVO.PROCESS_DESIGN.equals(role))
    {
      if (isRemote())
      {
        if (!isGitVcs() || getMdwVcsRepository().isGitProjectSync())
          return false; // only unlocked remote projects can be edited
      }
      else
      {
        return true;
      }
    }
    if (designerProxy == null) // avoid db lookup
      return false;
    return getDesignerDataModel().userHasRoleInAnyGroup(role);
  }

  public boolean isUserAuthorizedForSystemAdmin()
  {
    if (designerProxy == null) // avoid db lookup
      return false;
    return getDesignerDataModel().belongsToGroup(UserGroupVO.SITE_ADMIN_GROUP);
  }

  public List<WorkflowAsset> getAssetList(List<String> assetTypes)
  {
    List<WorkflowAsset> assets = new ArrayList<WorkflowAsset>();
    for (WorkflowAsset asset : getTopLevelWorkflowAssets())
    {
      if (assetTypes == null || assetTypes.contains(asset.getLanguage()))
      {
        if (!assets.contains(asset))
          assets.add(asset);
      }
    }
    return assets;
  }

  public List<WorkflowAsset> getAssetList(String assetType)
  {
    List<WorkflowAsset> assets = new ArrayList<WorkflowAsset>();
    for (WorkflowAsset asset : getTopLevelWorkflowAssets())
    {
      if (assetType == null || assetType.equals(asset.getLanguage()))
      {
        if (!assets.contains(asset))
          assets.add(asset);
      }
    }
    return assets;
  }

  public void viewSource(String className)
  {
    if (setJava())
    {
      // dynamic java is preferred
      int lastDot = className.lastIndexOf('.');
      WorkflowAsset dynamicJava = null;
      if (lastDot == -1)
      {
        dynamicJava = getAsset(className);
        if (dynamicJava == null)
          dynamicJava = getAsset(className + ".java");
      }
      else
      {
        String pkg = className.substring(0, lastDot);
        String cls = className.substring(lastDot + 1);
        dynamicJava = getAsset(pkg, cls);
        if (dynamicJava == null)
          dynamicJava = getAsset(pkg, cls + ".java");
      }
      if (dynamicJava != null)
      {
        dynamicJava.openFile(new NullProgressMonitor());
        return;
      }

      IJavaProject wfSourceJavaProject = getSourceJavaProject();
      try
      {
        Path sourcePath =  new Path(className.replace('.', '/') + ".java");
        IJavaElement javaElement = wfSourceJavaProject.findElement(sourcePath);
        if (javaElement == null)
        {
          PluginMessages.uiMessage("Unable to find source code for '" + className + "'.", "View Source", this, PluginMessages.INFO_MESSAGE);
          return;
        }
        JavaUI.openInEditor(javaElement);
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Open Java Source", this);
      }
    }
  }

  public boolean setJava()
  {
    ProjectConfigurator projConf = new ProjectConfigurator(this, MdwPlugin.getSettings());
    if (projConf.isJavaCapable())
    {
      IProgressMonitor monitor = new NullProgressMonitor();
      projConf.setJava(monitor);
      try
      {
        if (!projConf.hasFrameworkJars())
        {
          FrameworkUpdateDialog updateDlg = new FrameworkUpdateDialog(MdwPlugin.getShell(), MdwPlugin.getSettings(), getProject());
          if (updateDlg.open() == Dialog.OK)
          {
            String origVer = getProject().getMdwVersion();
            getProject().setMdwVersion(updateDlg.getMdwVersion());  // for downloading
            projConf.initializeFrameworkJars();
            projConf.initializeWebAppJars();
            getProject().setMdwVersion(origVer);
          }
        }
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Open Java Source", this);
      }
      return true;
    }
    return false;
  }

  public String getJavaVersion()
  {
    if (isCloudOnly() || (getServerSettings() != null && getServerSettings().isFuse()))
      return "1.7";
    else
      return "1.6";
  }

  public void newPackageVersion(WorkflowPackage newPkg, WorkflowPackage prevPkg)
  {
    if (isFilePersist())
    {
      // refresh prevPkg since it's fast
      try
      {
        PackageVO pkg = getDesignerDataAccess().loadPackage(prevPkg.getId(), false);
        if (pkg == null) // when incrementing by saving non-process assets since this currently does not archive pkgs
          return;
        prevPkg.setPackageVO(pkg);
        prevPkg.setProcesses(findProcesses(prevPkg));
        prevPkg.setExternalEvents(findExternalEvents(prevPkg));
        prevPkg.setActivityImpls(findActivityImplementors(prevPkg));
        prevPkg.setAssets(findWorkflowAssets(prevPkg));
        prevPkg.setTaskTemplates(findTaskTemplates(prevPkg));
        // may have been called in response to vcs save new process version
        prevPkg.syncProcesses();
      }
      catch (Exception ex)
      {
        PluginMessages.log(ex);
      }
    }
    // for non-vcs, this is not complete since it does not include package contents
    for (WorkflowElement we : getArchivedUserVisiblePackagesFolder().getChildren())
    {
      WorkflowPackage pv = (WorkflowPackage) we;
      if (pv.getName().equals(prevPkg.getName()))
      {
        List<WorkflowPackage> descendants = pv.getDescendantPackageVersions();
        if (descendants == null)
        {
          descendants = new ArrayList<WorkflowPackage>();
          pv.setDescendantPackageVersions(descendants);
        }
        descendants.add(prevPkg);
        prevPkg.setTopLevelVersion(pv);
      }
    }
  }

  public String getVcsAssetPath()
  {
    assert isFilePersist();
    return getMdwVcsRepository().getLocalPath();
  }

  private List<String> suppressedActivityImplementors;
  public List<String> getSuppressedActivityImplementors() throws IOException
  {
    if (suppressedActivityImplementors == null)
    {
      suppressedActivityImplementors = new ArrayList<String>();
      String compressed = getPersistentProperty(MDW_SUPPRESSED_ACTIVITY_IMPLEMENTORS);
      String suppressedImplsProp = compressed == null ? null : StringHelper.uncompress(compressed);
      if (suppressedImplsProp == null)
      {
        if (checkRequiredVersion(5,5))
        {
          // suppress the old baseline package impls by default
          WorkflowPackage baseline52 = getPackage("MDW Baseline");
          if (baseline52 != null) {
            for (ActivityImpl impl : baseline52.getActivityImpls())
              suppressedActivityImplementors.add(impl.getImplClassName());
          }
        }
      }
      else
      {
        for (String impl : suppressedImplsProp.split(","))
          suppressedActivityImplementors.add(impl);
      }
    }
    return suppressedActivityImplementors;
  }

  public void setSuppressedActivityImplementors(List<String> suppressedImpls) throws IOException
  {
    if (suppressedImpls == null || suppressedImpls.isEmpty())
    {
      suppressedActivityImplementors = new ArrayList<String>();
      setPersistentProperty(MDW_SUPPRESSED_ACTIVITY_IMPLEMENTORS, null);
    }
    else
    {
      suppressedActivityImplementors = suppressedImpls;
      String impls = "";
      for (int i = 0; i < suppressedImpls.size(); i++)
      {
        impls += suppressedImpls.get(i);
        if (i < suppressedImpls.size() - 1)
          impls += ',';
      }
      setPersistentProperty(MDW_SUPPRESSED_ACTIVITY_IMPLEMENTORS, StringHelper.compress(impls));
    }
  }

  public List<TaskTemplate> getTopLevelTaskTemplates()
  {
    List<TaskTemplate> allTaskTemplates = new ArrayList<TaskTemplate>();
    for (WorkflowPackage topLevelPackage : getTopLevelUserVisiblePackages())
    {
      allTaskTemplates.addAll(topLevelPackage.getTaskTemplates());
    }
    return allTaskTemplates;
  }

  public void authenticate(String user, String password, boolean secureStore) throws MdwSecurityException
  {
    WorkflowProjectManager.getInstance().authenticate(getAuthenticator(), user, password, secureStore);
  }

  /**
   * TODO: Consider authenticating per-project (currently auth is attempted only once per
   * Authenticator implementation class).  Per-project would be inconvenient for users who
   * do not want to save their credentials in the Eclipse secure store.
   * @return null means authenticator could not be created
   */
  public Boolean isAuthenticated()
  {
    Authenticator authenticator = getAuthenticator();
    if (authenticator == null) // maybe could not connect to server for remote proj
      return null;
    user = WorkflowProjectManager.getInstance().getAuthenticatedUser(authenticator);
    return user != null;
  }

  public Authenticator getAuthenticator()
  {
    if (isGitVcs() && isRemote())
    {
      ApplicationSummary appSummary = getRemoteAppSummary(true);
      if (appSummary == null)
        return null;
      String oauthTokenUrl = appSummary.getOAuthTokenUrl();
      if (oauthTokenUrl != null)
        return new OAuthAuthenticator(oauthTokenUrl);
    }

    // TODO: Straight LDAP
    // https://wiki.eclipse.org/Security:_KeyStore_support_for_Eclipse
    return new ClearTrustAuthenticator();
  }

  // the current user credentials
  private User user;
  public User getUser() { return user; }
  public void setUser(User u) { user = u; }

  public WorkflowPackage getPackage(IFolder folder)
  {
    for (WorkflowPackage pkg : getTopLevelPackages())
    {
      if (pkg.getFolder().equals(folder))
        return pkg;
    }
    return null;
  }

  private boolean warn;
  public void setWarn(boolean warn) { this.warn = warn; }
  public boolean isWarn() { return warn; }
}
