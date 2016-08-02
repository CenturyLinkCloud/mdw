/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.assembly;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.wst.common.project.facet.core.IActionDefinition;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.codegen.Generator;
import com.centurylink.mdw.plugin.codegen.JetAccess;
import com.centurylink.mdw.plugin.codegen.JetConfig;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.model.OsgiSettings;
import com.centurylink.mdw.plugin.project.model.VcsRepository;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

@SuppressWarnings("restriction")
public class ProjectInflator
{
  private WorkflowProject workflowProject;
  public WorkflowProject getProject() { return workflowProject; }

  private MdwSettings mdwSettings;
  public MdwSettings getSettings() { return mdwSettings; }

  private Shell shell;

  public ProjectInflator(WorkflowProject workflowProject, MdwSettings mdwSettings)
  {
    this.workflowProject = workflowProject;
    this.mdwSettings = mdwSettings;
  }

  public void inflate(Shell shell, IProgressMonitor monitor) throws IOException, CoreException, InvocationTargetException, InterruptedException
  {
    this.shell = shell;
    IProject earProject = workflowProject.getEarProject();
    createEarBase(earProject, monitor);
    monitor.worked(5);
    ProjectUpdater updater = new ProjectUpdater(workflowProject, mdwSettings);
    ProjectConfigurator configurator = new ProjectConfigurator(workflowProject, mdwSettings);

    updater.updateFrameworkJars(monitor);
    final IProject project = workflowProject.isCloudProject() ? workflowProject.getSourceProject() : workflowProject.getEarProject();
    updater.updateMappingTemplates(project.getFolder("deploy/config"), new SubProgressMonitor(monitor, 5));
    generateEarArtifacts(new SubProgressMonitor(monitor, 3));
    configurator.configureEarProject(new SubProgressMonitor(monitor, 5));
    generateSourceArtifacts(new SubProgressMonitor(monitor, 3));
    configurator.configureSourceProject(shell, new SubProgressMonitor(monitor, 5));
    configurator.createFrameworkSourceCodeAssociations(shell, monitor);
  }

  /**
   * Unjar the earBase archive into the ear project.
   * Download the framework jars and mappings.
   *
   * @param earProject
   * @param monitor
   */
  private void createEarBase(IProject earProject, IProgressMonitor monitor)
    throws CoreException, IOException
  {
    try
    {
      PluginUtil.unzipPluginResource("earBase.jar", null, earProject, "./", monitor);
    }
    catch (URISyntaxException ex)
    {
      PluginMessages.log(ex);
      throw new IOException(ex.getMessage());
    }
  }

  public void generateEarArtifacts(IProgressMonitor monitor) throws CoreException
  {
    Generator generator = new Generator(shell);
    IProject earProject = workflowProject.getEarProject();

    // TODO: why is this META-INF here?
    earProject.getFolder("META-INF").delete(true, monitor);

    // build.xml
    JetAccess jet = getJet("ear/build.xmljet", earProject, "build.xml", null);
    generator.createFile(jet, monitor);

    // release_build.xml
    jet = getJet("ear/release_build.xmljet", earProject, "release_build.xml", null);
    generator.createFile(jet, monitor);

    // .externalToolBuilders folder
    PluginUtil.createFolder(earProject, new Path(".externalToolBuilders"), monitor);

    // builder.launch
    jet = getJet("ear/externalToolBuilders/builder.launchjet", earProject, ".externalToolBuilders/" + workflowProject.getSourceProjectName() + "Builder.launch", null);
    generator.createFile(jet, monitor);

    // ApplicationProperties.xml
    if (workflowProject.checkRequiredVersion(5, 1, 8))
      jet = getJet("ear/deploy/config/ApplicationPropertiesEmpty.xmljet", earProject, "deploy/config/ApplicationProperties.xml", null);
    else
      jet = getJet("ear/deploy/config/ApplicationProperties.xmljet", earProject, "deploy/config/ApplicationProperties.xml", null);
    generator.createFile(jet, monitor);

    // ApplicationCache.xml
    jet = getJet("ear/deploy/config/ApplicationCache.xmljet", earProject, "deploy/config/ApplicationCache.xml", null);
    generator.createFile(jet, monitor);

    // deploy/env folder
    PluginUtil.createFolder(earProject, new Path("deploy/env"), monitor);

    // project.properties
    jet = getJet("ear/deploy/env/project.propertiesjet", earProject, "deploy/env/project.properties", null);
    generator.createFile(jet, monitor);

    // env.properties.dev
    jet = getJet("ear/deploy/env/env.properties.devjet", earProject, "deploy/env/env.properties.dev", null);
    generator.createFile(jet, monitor);

    // application.xml
    PluginUtil.createFolder(earProject, new Path("EarContent/META-INF"), monitor);
    jet = getJet("ear/EarContent/META-INF/application.xmljet", earProject, workflowProject.getEarContentFolder().getProjectRelativePath() + "/META-INF/application.xml", null);
    generator.createFile(jet, monitor);

    // startWebLogic.cmd.tmpl
    jet = getJet("ear/deploy/config/startWebLogic.cmd.tmpljet", earProject, "deploy/config/startWebLogic.cmd.tmpl", null);
    generator.createFile(jet, monitor);

    // designerConfig.xml
    jet = getJet("ear/deploy/config/designerConfig.xmljet", earProject, "deploy/config/designerConfig.xml", null);
    generator.createFile(jet, monitor);
  }

  public void generateSourceArtifacts(IProgressMonitor monitor) throws CoreException
  {
    Generator generator = new Generator(shell);
    IProject sourceProject = workflowProject.getSourceProject();

    // build.xml
    JetAccess jet = getJet("source/build.xmljet", sourceProject, "build.xml", null);
    generator.createFile(jet, monitor);

    // base .*ignore
    jet = getJet("source/.ignorejet", sourceProject, ".cvsignore", null);
    generator.createFile(jet, monitor);
    jet = getJet("source/.ignorejet", sourceProject, ".dmignore", null);
    generator.createFile(jet, monitor);
    jet = getJet("source/.ignorejet", sourceProject, ".svnignore", null);
    generator.createFile(jet, monitor);
    jet = getJet("source/.ignorejet", sourceProject, ".gitignore", null);
    generator.createFile(jet, monitor);

    // META-INF .*ignore
    jet = getJet("source/src/META-INF/.ignorejet", sourceProject, "src/META-INF/.cvsignore", null);
    generator.createFile(jet, monitor);
    jet = getJet("source/src/META-INF/.ignorejet", sourceProject, "src/META-INF/.dmignore", null);
    generator.createFile(jet, monitor);
    jet = getJet("source/src/META-INF/.ignorejet", sourceProject, "src/META-INF/.svnignore", null);
    generator.createFile(jet, monitor);
    jet = getJet("source/src/META-INF/.ignorejet", sourceProject, "src/META-INF/.gitignore", null);
    generator.createFile(jet, monitor);

    if (workflowProject.isEjbSourceProject() && workflowProject.getServerSettings().isJBoss())
    {
      jet = getJet("source/src/META-INF/jboss.xmljet", sourceProject, "src/META-INF/jboss.xml", null);
      generator.createFile(jet, monitor);
    }

    // baseline ejb impl
    if (workflowProject.isEjbSourceProject())
    {
      String ejbPath = "src/" + workflowProject.getDefaultSourceCodePackagePath() + "/services";
      PluginUtil.createFoldersAsNeeded(sourceProject, sourceProject.getFolder(new Path(ejbPath)), monitor);
      jet = getJet("source/src/ejbs/WorkflowManager.javajet", sourceProject, ejbPath + "/" + workflowProject.getSourceProjectName() + "Manager.java", null);
      generator.createFile(jet, monitor);
      jet = getJet("source/src/ejbs/WorkflowManagerBean.javajet", sourceProject, ejbPath + "/" + workflowProject.getSourceProjectName() + "ManagerBean.java", null);
      generator.createFile(jet, monitor);
    }
    else
    {
      String srcPath = "src/" + workflowProject.getDefaultSourceCodePackagePath() + "/hello";
      PluginUtil.createFoldersAsNeeded(sourceProject, sourceProject.getFolder(new Path(srcPath)), monitor);
      jet = getJet("source/src/pojos/Hello.javajet", sourceProject, srcPath + "/" + workflowProject.getSourceProjectName() + "Hello.java", null);
      generator.createFile(jet, monitor);
    }
  }

  public void generateWarCloudArtifacts(IProgressMonitor monitor) throws CoreException
  {
    Generator generator = new Generator(shell);
    IProject sourceProject = workflowProject.getSourceProject();

    if (workflowProject.isFilePersist())
      PluginUtil.createFoldersAsNeeded(sourceProject, workflowProject.getAssetFolder(), monitor);

    // pom.xml
    JetAccess jet;
    if (workflowProject.isFilePersist())
      jet = getJet("cloud/cloud_fs_pom.xmljet", sourceProject, "pom.xml", null);
    else
      jet = getJet("osgi/remote_pom.xmljet", sourceProject, "pom.xml", null);
    generator.createFile(jet, monitor);
  }

  public void generateOsgiArtifacts(IProgressMonitor monitor) throws CoreException
  {
    Generator generator = new Generator(shell);
    IProject sourceProject = workflowProject.getSourceProject();

    OsgiSettings osgiSettings = workflowProject.getOsgiSettings();

    // bundle-context.xml
    String springPath = osgiSettings.getResourceDir() + "/META-INF/spring";
    JetAccess jet = getJet("osgi/bundle-context.xmljet", sourceProject, springPath + "/bundle-context.xml", null);
    PluginUtil.createFoldersAsNeeded(sourceProject, sourceProject.getFolder(new Path(springPath)), monitor);
    generator.createFile(jet, monitor);

    if (osgiSettings.isGradleBuild())
    {
      // build.gradle
      jet = getJet("osgi/build.gradlejet", sourceProject, "build.gradle", null);
      generator.createFile(jet, monitor);
    }
    else
    {
      // pom.xml
      String template = workflowProject.checkRequiredVersion(5, 5) ? "osgi/pom.xmljet" : "osgi/52/pom.xmljet";
      jet = getJet(template, sourceProject, "pom.xml", null);
      generator.createFile(jet, monitor);
    }

    // BundleActivator.java
    String srcPath = osgiSettings.getSourceDir() + "/" + workflowProject.getDefaultSourceCodePackagePath() + "/bundle";
    PluginUtil.createFoldersAsNeeded(sourceProject, sourceProject.getFolder(new Path(srcPath)), monitor);
    String template = workflowProject.checkRequiredVersion(5, 5) ? "osgi/BundleActivator.javajet" : "osgi/52/BundleActivator.javajet";
    jet = getJet(template, sourceProject, srcPath + "/WorkflowBundleActivator.java", null);
    generator.createFile(jet, monitor);

    if (!workflowProject.checkRequiredVersion(5, 5))
    {
      // env.properties.dev
      String depEnvPath = osgiSettings.getResourceDir() + "/deploy/env";
      PluginUtil.createFoldersAsNeeded(sourceProject, sourceProject.getFolder(depEnvPath), monitor);
      jet = getJet("ear/deploy/env/env.properties.devjet", sourceProject, depEnvPath + "/env.properties.dev", null);
      generator.createFile(jet, monitor);
    }
    if (workflowProject.isFilePersist())
      PluginUtil.createFoldersAsNeeded(sourceProject, workflowProject.getAssetFolder(), monitor);
  }

  public void generateWebArtifacts(IProgressMonitor monitor) throws CoreException
  {
    Generator generator = new Generator(shell);
    IProject webProject = workflowProject.getWebProject();

    // build.xml
    JetAccess jet = getJet("web/build.xmljet", webProject, "build.xml", null);
    generator.createFile(jet, monitor);

    jet = getJet("web/readme.txtjet", webProject, "readme.txt", null);
    generator.createFile(jet, monitor);
  }

  private JetAccess getJet(String jetFile, IProject targetProject, String targetPath, IPackageFragment pkg)
  {
    // prepare config for creating files
    JetConfig jetConfig = new JetConfig();
    jetConfig.setModel(workflowProject);
    jetConfig.setSettings(mdwSettings);
    jetConfig.setPluginId(MdwPlugin.getPluginId());

    if (pkg == null)
      jetConfig.setPackageName("");
    else
      jetConfig.setPackageName(pkg.getElementName());
    jetConfig.setTargetFolder(targetProject.getName());
    jetConfig.setTargetFile(targetPath);
    jetConfig.setTemplateRelativeUri("templates/" + jetFile);

    return new JetAccess(jetConfig);
  }

  public void inflateRemoteProject(final IRunnableContext container)
  {
    // get a project handle
    final IProject newProjectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(workflowProject.getName());

    // get a project descriptor
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());

    // create the new project operation
    IRunnableWithProgress op = new IRunnableWithProgress()
    {
      public void run(IProgressMonitor monitor) throws InvocationTargetException
      {
        CreateProjectOperation op = new CreateProjectOperation(description, "MDW Remote Project");
        try
        {
          PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op, monitor, WorkspaceUndoUtil.getUIInfoAdapter(shell));
        }
        catch (ExecutionException ex)
        {
          throw new InvocationTargetException(ex);
        }
      }
    };

    // run the new project creation operation
    try
    {
      container.run(true, true, op);
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(shell, ex, "Create Remote Project", workflowProject);
    }
  }

  public void inflateCloudProject(final IRunnableContext container)
  {
    getProject().setCloudProject(true);

    // get a project handle
    final IProject newProjectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(workflowProject.getName());

    // get a project descriptor
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());

    // create the new project operation
    IRunnableWithProgress op = new IRunnableWithProgress()
    {
      public void run(IProgressMonitor monitor) throws InvocationTargetException
      {
        Repository newRepo = null;
        try
        {
          if (workflowProject.getPersistType() == PersistType.Git)
          {
            File localDir = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile() + "/" + workflowProject.getName());
            if (workflowProject.getMdwVcsRepository().hasRemoteRepository())
            {
              monitor.subTask("Cloning Git repository");
              VcsRepository gitRepo = workflowProject.getMdwVcsRepository();
              Git.cloneRepository().setURI(gitRepo.getRepositoryUrlWithCredentials()).setDirectory(localDir).call();
            }
            else
            {
              newRepo = new FileRepository(new File(localDir + "/.git"));
              newRepo.create();
            }

            // .gitignore
            Generator generator = new Generator(shell);
            JetAccess jet = getJet("source/.ignorejet", getProject().getSourceProject(), ".gitignore", null);
            generator.createFile(jet, monitor);
          }

          CreateProjectOperation op = new CreateProjectOperation(description, "MDW Cloud Project");
          PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op, monitor, WorkspaceUndoUtil.getUIInfoAdapter(shell));
        }
        catch (Exception ex)
        {
          throw new InvocationTargetException(ex);
        }
        finally
        {
          if (newRepo != null)
            newRepo.close();
        }
      }
    };

    // run the new project creation operation
    try
    {
      container.run(false, false, op);
      ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(MdwPlugin.getShell());
      pmDialog.run(true, false, new IRunnableWithProgress()
      {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
          monitor.beginTask("Inflating Workflow Project", 250);
          monitor.worked(5);
          // configure as Java project
          ProjectConfigurator projConf = new ProjectConfigurator(getProject(), MdwPlugin.getSettings());
          projConf.setJava(new SubProgressMonitor(monitor, 3));

          ProjectUpdater updater = new ProjectUpdater(getProject(), MdwPlugin.getSettings());
          updater.updateMappingFiles(new SubProgressMonitor(monitor, 3));  // bootstrap versions of the property files
          updater.updateFrameworkJars(new SubProgressMonitor(monitor, 150));
          updater.updateWebProjectJars(new SubProgressMonitor(monitor, 50));

          try
          {
            if (getProject().isOsgi())
              projConf.addJavaProjectSourceFolder(getProject().getOsgiSettings().getSourceDir(), new SubProgressMonitor(monitor, 3));
            else if (!getProject().isWar())
              projConf.addJavaProjectSourceFolder("src", monitor);
            projConf.setJavaBuildOutputPath("build/classes", new SubProgressMonitor(monitor, 5));
            projConf.addFrameworkJarsToClasspath(monitor);

            // add the workflow facet
            getProject().setSkipFacetPostInstallUpdates(true);  // already did framework updates
            IFacetedProject facetedProject = ProjectFacetsManager.create(getProject().getSourceProject(), true, new SubProgressMonitor(monitor, 3));
            IProjectFacetVersion javaFacetVersion = ProjectFacetsManager.getProjectFacet("java").getDefaultVersion();
            if (Float.parseFloat(javaFacetVersion.getVersionString()) < 1.6)
              javaFacetVersion = ProjectFacetsManager.getProjectFacet("java").getVersion("1.6");
            if (workflowProject.isCloudOnly())
              javaFacetVersion = ProjectFacetsManager.getProjectFacet("java").getVersion("1.7");
            facetedProject.installProjectFacet(javaFacetVersion, null, new SubProgressMonitor(monitor, 3));
            IProjectFacetVersion mdwFacet = ProjectFacetsManager.getProjectFacet("mdw.workflow").getDefaultVersion();
            facetedProject.installProjectFacet(mdwFacet, getProject(), new SubProgressMonitor(monitor, 3));
            if (workflowProject.isOsgi())
            {
              IProjectFacet utilFacet = ProjectFacetsManager.getProjectFacet("jst.utility");
              IProjectFacetVersion facetVer = utilFacet.getDefaultVersion();
              IActionDefinition def = facetVer.getActionDefinition(null, IFacetedProject.Action.Type.INSTALL);
              Object cfg = def.createConfigObject();
              facetedProject.installProjectFacet(ProjectFacetsManager.getProjectFacet("jst.utility").getDefaultVersion(), cfg, new SubProgressMonitor(monitor, 3));
            }
            else if (workflowProject.isWar())
            {
              // add the facet to the xml file
              IFile facetsFile = workflowProject.getSourceProject().getFile(".settings/org.eclipse.wst.common.project.facet.core.xml");
              if (facetsFile.exists())
              {
                String content = new String(PluginUtil.readFile(facetsFile));
                int insert = content.indexOf("</faceted-project>");
                content = content.substring(0, insert) + "  <installed facet=\"jst.web\" version=\"3.0\"/>\n" + content.substring(insert);
                PluginUtil.writeFile(facetsFile, content, new SubProgressMonitor(monitor, 3));
              }
            }

            final ProjectConfigurator configurator = new ProjectConfigurator(getProject(), MdwPlugin.getSettings());
            if (!workflowProject.isOsgi() && !workflowProject.isWar())
            {
              MdwPlugin.getDisplay().syncExec(new Runnable()
              {
                public void run()
                {
                  try
                  {
                    configurator.createFrameworkSourceCodeAssociations(MdwPlugin.getShell(), new NullProgressMonitor());
                  }
                  catch (CoreException ex)
                  {
                    PluginMessages.log(ex);
                  }
                }
              });
            }

            if (workflowProject.isOsgi())
            {
              generateOsgiArtifacts(new SubProgressMonitor(monitor, 10));
              configurator.configureOsgiProject(shell, new SubProgressMonitor(monitor, 5));
            }
            else if (workflowProject.isWar())
            {
              generateWarCloudArtifacts(new SubProgressMonitor(monitor, 10));
              configurator.addMavenNature(new SubProgressMonitor(monitor, 5));  // force maven refresh
            }
          }
          catch (Exception ex)
          {
            throw new InvocationTargetException(ex);
          }
        }
      });
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Create Cloud Project", workflowProject);
    }
  }
}
