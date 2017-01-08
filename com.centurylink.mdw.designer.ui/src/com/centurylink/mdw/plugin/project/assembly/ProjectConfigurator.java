/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.assembly;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.launching.AbstractVMInstallType;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jst.j2ee.internal.common.classpath.J2EEComponentClasspathUpdater;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.componentcore.internal.ComponentResource;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.codegen.Generator;
import com.centurylink.mdw.plugin.codegen.JetAccess;
import com.centurylink.mdw.plugin.codegen.JetConfig;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.designer.model.JavaSource;
import com.centurylink.mdw.plugin.designer.model.Script;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.DotClasspathUpdater;
import com.centurylink.mdw.plugin.project.model.DotProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.plugin.workspace.TempFileRemover;

@SuppressWarnings("restriction")
public class ProjectConfigurator
{
  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }

  private MdwSettings settings;
  public MdwSettings getSettings() { return settings; }

  public ProjectConfigurator(WorkflowProject project, MdwSettings settings)
  {
    this.project = project;
    this.settings = settings;
  }

  public void configureEarProject(IProgressMonitor monitor) throws CoreException
  {
    IProject earProject = project.getEarProject();
    // TODO does nothing
    J2EEComponentClasspathUpdater.getInstance().queueUpdateEAR(earProject);
  }

  /**
   * @param shell
   * @param monitor
   * @throws CoreException
   */
  public void configureSourceProject(Shell shell, IProgressMonitor monitor) throws CoreException
  {
    IProject sourceProject = project.getSourceProject();
    updateSourceProjectManifest(monitor, sourceProject);
    updateSourceProjectBeaLibs(monitor, sourceProject);
    updateSourceProjectClasspath(monitor, project);
    J2EEComponentClasspathUpdater.getInstance().queueUpdateModule(sourceProject);
  }

  public void configureOsgiProject(Shell shell, IProgressMonitor monitor) throws CoreException
  {
    IProject sourceProject = project.getSourceProject();
    if (project.getOsgiSettings().isGradleBuild())
    {
      if (isGradleCapable())
      {
        addGradleNature(monitor);
        addGradleClasspath(monitor);
      }
    }
    else
    {
      if (isMavenCapable())
        addMavenNature(monitor);
    }

    J2EEComponentClasspathUpdater.getInstance().queueUpdateModule(sourceProject);
  }

  /**
   * JST source code associations are stored here:
   * [Workspace]/.metadata/.plugins/org.eclipse.jst.j2ee/classpath.decorations.xml
   */
  public void createFrameworkSourceCodeAssociations(Shell shell, IProgressMonitor monitor) throws CoreException
  {
    if (monitor == null)
      monitor = new NullProgressMonitor();

    // source code attachments
    String[] changedAttributes = { CPListElement.SOURCEATTACHMENT };

    if (project.isEarProject())
    {
      String appInfLibDir = project.getEarContentFolder().getFullPath().toString() + "/APP-INF/lib/";

      IPath containerPath = new Path("org.eclipse.jst.j2ee.internal.module.container");
      for (File libFile : project.getAppInfLibFiles())
      {
        String libFileName = libFile.getName();
        if (libFileName.startsWith("MDW") && !libFileName.equals("MDWDesignerResources.jar")
            && libFileName.endsWith(".jar") && !libFileName.endsWith("_src.jar"))
        {
          String srcJarFileName = libFileName.replaceFirst("\\.jar", "_src.jar");
          IClasspathEntry cpEntry = JavaCore.newLibraryEntry(new Path(appInfLibDir + libFileName), new Path(appInfLibDir + srcJarFileName), null);
          BuildPathSupport.modifyClasspathEntry(shell, cpEntry, changedAttributes, project.getSourceJavaProject(), containerPath, false, monitor);
        }
      }
      String earContent = project.getEarContentFolder().getFullPath().toString() + "/";
      for (File servicesLibFile : project.getServicesLibFiles())
      {
        String srcJarFileName = servicesLibFile.getName().replaceFirst("\\.jar", "_src.jar");
        IClasspathEntry cpEntry = JavaCore.newLibraryEntry(new Path(earContent + servicesLibFile.getName()), new Path(earContent + srcJarFileName), null);
        BuildPathSupport.modifyClasspathEntry(shell, cpEntry, changedAttributes, project.getSourceJavaProject(), containerPath, false, monitor);
      }
    }
    else if (project.isCloudProject())
    {
      // handled by maven
    }
    else
    {
      IFolder libFolder = project.getSourceProject().getFolder("lib");
      createSourceCodeAssociations(shell, libFolder, monitor);
    }
  }

  private void createSourceCodeAssociations(Shell shell, IFolder libFolder, IProgressMonitor monitor) throws CoreException
  {
    if (project.isOsgi())
    {
      // handled by Maven
    }
    else
    {
      String[] changedAttributes = { CPListElement.SOURCEATTACHMENT };

      if (libFolder.exists())
      {
        String libDir = libFolder.getFullPath().toString() + "/";

        for (File libFile : new File(libFolder.getLocation().toString()).listFiles())
        {
          String libFileName = libFile.getName();
          if (libFileName.startsWith("MDW") && !libFileName.equals("MDWDesignerResources.jar")
              && libFileName.endsWith(".jar") && !libFileName.endsWith("_src.jar"))
          {
            String srcJarFileName = libFileName.replaceFirst("\\.jar", "_src.jar");
            IClasspathEntry cpEntry = JavaCore.newLibraryEntry(new Path(libDir + libFileName), new Path(libDir + srcJarFileName), null);
            BuildPathSupport.modifyClasspathEntry(shell, cpEntry, changedAttributes, project.getSourceJavaProject(), null, false, monitor);
          }
          else if (libFileName.equals("mdwweb.jar"))
          {
            IClasspathEntry cpEntry = JavaCore.newLibraryEntry(new Path(libDir + libFileName), new Path(libDir + "MDWWeb_src.jar"), null);
            BuildPathSupport.modifyClasspathEntry(shell, cpEntry, changedAttributes, project.getSourceJavaProject(), null, false, monitor);
          }
        }
      }
    }
  }

  public void createWebProjectSourceCodeAssociations(Shell shell, IProgressMonitor monitor) throws CoreException
  {
    IPath containerPath = new Path("org.eclipse.jst.j2ee.internal.web.container");
    String[] changedAttributes = { CPListElement.SOURCEATTACHMENT };

    for (File webappLibFile : project.getWebappLibFiles())
    {
      String webInfLib = project.getWebContentFolder().getRawLocation().toString() + "/WEB-INF/lib/";

      String srcJarFileName = null;
      if (webappLibFile.getName().equals("mdwweb.jar"))
        srcJarFileName = "MDWWeb_src.jar";
      else
        srcJarFileName = webappLibFile.getName().replaceFirst("\\.jar", "_src.jar");

      IClasspathEntry cpEntry = JavaCore.newLibraryEntry(new Path(webInfLib + webappLibFile.getName()), new Path(webInfLib + srcJarFileName), null);
      BuildPathSupport.modifyClasspathEntry(shell, cpEntry, changedAttributes, project.getWebJavaProject(), containerPath, false, monitor);
    }
  }

  private void updateSourceProjectBeaLibs(IProgressMonitor monitor, IProject sourceProject)
  {
    IFile libsFile = sourceProject.getFile(".settings/com.bea.workshop.wls.core.systemlibs.xml");
    PluginUtil.writeFile(libsFile, BEA_SYSTEM_LIBS, monitor);
  }

  private void updateSourceProjectClasspath(IProgressMonitor monitor, WorkflowProject workflowProject)
  {
    List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
    try
    {
      for (IClasspathEntry existingEntry : workflowProject.getSourceJavaProject().getRawClasspath())
      {
        classpathEntries.add(existingEntry);
      }
      for (File file : workflowProject.getServicesLibFiles())
      {
        String jarFileName = file.getName();
        int dotIdx = jarFileName.indexOf('.');
        String sourceFileName = jarFileName.substring(0, dotIdx) + "_src" + jarFileName.substring(dotIdx);
        IPath jarFilePath = workflowProject.getEarContentFolder().getFile(jarFileName).getFullPath();
        IPath sourceFilePath = workflowProject.getEarContentFolder().getFile(sourceFileName).getFullPath();
        classpathEntries.add(JavaCore.newLibraryEntry(jarFilePath, sourceFilePath, null));
      }
      workflowProject.getSourceJavaProject().setRawClasspath(classpathEntries.toArray(new IClasspathEntry[0]), monitor);
    }
    catch (JavaModelException ex)
    {
      PluginMessages.log(ex);
    }
  }

  public void setGroovy(IProgressMonitor monitor)
  {
    if (isGroovyCapable())
    {
      try
      {
        setJava(monitor);

        if (!hasGroovyNature())
          addGroovyNature();

        if (MdwPlugin.getSettings().isLoadScriptLibsOnEdit()
            && !project.areScriptLibrariesSaved() /* only do this once per project session */)
        {
          loadGroovyScriptLibraries(monitor);
          project.setScriptLibrariesSaved(true);
        }
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Groovy Support", project);
      }
    }
  }

  public boolean isGroovyCapable()
  {
    return !MdwPlugin.isRcp() && MdwPlugin.workspaceHasGroovySupport();
  }

  public boolean hasGroovyNature() throws CoreException
  {
    if (MdwPlugin.isRcp())
      return false;

    return project.getSourceProject().getNature("org.eclipse.jdt.groovy.core.groovyNature") != null;
  }

  public void addGroovyNature() throws Exception
  {
    IJavaProject javaProject = project.getSourceJavaProject();
    if (javaProject == null)
      throw new IllegalStateException("Workflow project: " + project.getLabel() + " does not contain a Java source project");

    Class<?> groovyRuntimeClass = Class.forName("org.codehaus.groovy.eclipse.core.model.GroovyRuntime");
    Method addGroovyRuntime = groovyRuntimeClass.getMethod("addGroovyRuntime", new Class[]{IProject.class});
    addGroovyRuntime.invoke(null, new Object[]{project.getSourceProject()});
  }

  public void loadGroovyScriptLibraries(IProgressMonitor monitor)
  {
    // locally save all the groovy libraries
    for (WorkflowAsset asset : project.getTopLevelWorkflowAssets())
    {
      if (asset instanceof Script && ((Script)asset).isGroovy())
      {
        if (!asset.isLoaded())
          asset.load();

        try
        {
          IFolder folder = asset.getTempFolder();
          if (!folder.exists())
            PluginUtil.createFoldersAsNeeded(project.getSourceProject(), folder, monitor);
          IFile tempFile = asset.getTempFile(folder);

          if (tempFile.exists())
            new TempFileRemover(folder, tempFile).remove(monitor);

          tempFile.create(new ByteArrayInputStream(asset.getFileContent()), true, monitor);

          PluginMessages.log("Created groovy lib file: " + tempFile.getFullPath().toString());
        }
        catch (CoreException ex)
        {
          PluginMessages.uiError(ex, "Load Groovy Libraries", project);
        }
      }
    }
  }

  public void loadJavaLibraries(IProgressMonitor monitor)
  {
    // locally save all the Java libraries
    for (WorkflowAsset asset : project.getTopLevelWorkflowAssets())
    {
      if (asset instanceof JavaSource)
      {
        if (!asset.isLoaded())
          asset.load();

        try
        {
          IFolder folder = asset.getTempFolder();
          if (!folder.exists())
            PluginUtil.createFoldersAsNeeded(project.getSourceProject(), folder, monitor);
          IFile tempFile = asset.getTempFile(folder);

          if (tempFile.exists())
            new TempFileRemover(folder, tempFile).remove(monitor);

          tempFile.create(new ByteArrayInputStream(asset.getFileContent()), true, monitor);

          PluginMessages.log("Created Java lib file: " + tempFile.getFullPath().toString());
        }
        catch (CoreException ex)
        {
          PluginMessages.uiError(ex, "Load Java Libraries", project);
        }
      }
    }
  }

  public void setJava(IProgressMonitor monitor)
  {
    if (isJavaCapable())
    {
      try
      {
        if (!hasJavaNature())
          addJavaNature();

        if (project.isOsgi() || project.isRemote())
        {
          if (project.getOsgiSettings().isGradleBuild())
            setGradle(monitor);
          else
            setMaven(monitor);
        }

        if (!project.isFilePersist())
        {
          // TODO in many cases might be added by Maven
          String srcLoc = MdwPlugin.getSettings().getTempResourceLocation();
          addJavaProjectSourceFolder(srcLoc, "build/" + srcLoc, monitor);
        }
        if (!project.isRemote())
        {
          project.getSourceProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
          if (project.isFilePersist())
            removeDeploymentAssemblyResourceMappings(project.getAssetFolder());
          else
            removeDeploymentAssemblyResourceMappings(project.getTempFolder());
        }

        if (MdwPlugin.getSettings().isLoadScriptLibsOnEdit()
            && !project.areJavaLibrariesSaved() /* only do this once per project session */)
        {
          loadJavaLibraries(monitor);
          project.setJavaLibrariesSaved(true);
        }
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Java Support", project);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public boolean addDeploymentAssemblyResourceMappings(IFolder folder)
  {
    StructureEdit moduleCore = null;
    try {
        moduleCore = StructureEdit.getStructureEditForWrite(project.getSourceProject());
        if (moduleCore != null)
        {
          ComponentResource found = null;
          for (Object o : moduleCore.getComponent().getResources())
          {
            ComponentResource resource = (ComponentResource)o;
            if (("/" + folder.getProjectRelativePath()).equals(resource.getSourcePath().toString()))
              found = resource;
          }
          if (found == null)
          {
            ComponentResource componentResource = moduleCore.createWorkbenchModuleResource(folder);
            componentResource.setRuntimePath(new Path("/"));
            moduleCore.getComponent().getResources().add(componentResource);
          }
        }
    }
    finally {
        if (moduleCore != null) {
            moduleCore.saveIfNecessary(new NullProgressMonitor());
            moduleCore.dispose();
        }
    }
    return true;
  }


  public boolean removeDeploymentAssemblyResourceMappings(IFolder folder)
  {
    StructureEdit moduleCore = null;
    try {
        moduleCore = StructureEdit.getStructureEditForWrite(project.getSourceProject());
        if (moduleCore != null && moduleCore.getComponent() != null && moduleCore.getComponent().getResources() != null)
        {
          ComponentResource toRemove = null;
          for (Object o : moduleCore.getComponent().getResources())
          {
            ComponentResource resource = (ComponentResource)o;
            if (("/" + folder.getProjectRelativePath()).equals(resource.getSourcePath().toString()))
              toRemove = resource;
          }
          if (toRemove != null)
            moduleCore.getComponent().getResources().remove(toRemove);
        }
    }
    finally {
        if (moduleCore != null) {
            moduleCore.saveIfNecessary(new NullProgressMonitor());
            moduleCore.dispose();
        }
    }
    return true;
  }

  public boolean isJavaCapable()
  {
    return !MdwPlugin.isRcp();
  }

  public boolean hasJavaNature()
  {
    if (!project.isRemote() && !project.isCloudProject())
      return true;
    else
      return JavaProject.hasJavaNature(project.getSourceProject());
  }

  public void addJavaNature() throws CoreException
  {
    IProjectDescription description = project.getSourceProject().getDescription();
    String[] prevNatures = description.getNatureIds();
    String[] newNatures = new String[prevNatures.length + 1];
    System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
    newNatures[prevNatures.length] = JavaCore.NATURE_ID;
    description.setNatureIds(newNatures);
    project.getSourceProject().setDescription(description, null);
  }

  public void addJavaProjectSourceFolder(String sourceFolder, IProgressMonitor monitor)
  throws CoreException, JavaModelException
  {
    addJavaProjectSourceFolder(sourceFolder, null, monitor);
  }

  public void addJavaProjectSourceFolder(String sourceFolder, String outputFolder, IProgressMonitor monitor)
  throws CoreException, JavaModelException
  {
    if (!isJavaCapable())
      return;

    IFolder folder = project.getSourceProject().getFolder(sourceFolder);
    if (!folder.exists())
      folder.create(true, true, monitor);

    // projects with newly-added Java Nature contain root source entry, which must be removed
    IClasspathEntry rootEntry = JavaCore.newSourceEntry(project.getSourceProject().getFullPath());
    IPath outputPath = outputFolder == null ? null : project.getSourceProject().getFolder(outputFolder).getFullPath();
    IClasspathEntry newEntry = JavaCore.newSourceEntry(folder.getFullPath(), ClasspathEntry.EXCLUDE_NONE, outputPath);

    boolean includesContainer = false;
    List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
    IClasspathEntry toRemove = null;
    for (IClasspathEntry existingEntry : project.getSourceJavaProject().getRawClasspath())
    {
      if (newEntry.equals(existingEntry))
        return;  // already on classpath
      else if (newEntry.getPath().equals(existingEntry.getPath()) && newEntry.getEntryKind() == existingEntry.getEntryKind())
        toRemove = existingEntry;  // to be replaced with new entry

      if (!rootEntry.equals(existingEntry))
        classpathEntries.add(existingEntry);

      if (existingEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && String.valueOf(existingEntry.getPath()).indexOf("JRE_CONTAINER") >= 0)
        includesContainer = true;
    }
    if (toRemove != null)
      classpathEntries.remove(toRemove);
    classpathEntries.add(newEntry);
    if (!includesContainer)
    {
      IClasspathEntry jre = getJreContainerClasspathEntry(project.getJavaVersion());
      if (jre == null)
        jre = JavaRuntime.getDefaultJREContainerEntry(); // fallback to any available
      classpathEntries.add(jre);
    }

    project.getSourceJavaProject().setRawClasspath(classpathEntries.toArray(new IClasspathEntry[0]), monitor);
    J2EEComponentClasspathUpdater.getInstance().queueUpdate(project.getSourceProject());
  }

  private IClasspathEntry getJreContainerClasspathEntry(String desiredVersion)
  {
    for (IVMInstallType installType : JavaRuntime.getVMInstallTypes())
    {
      if (installType instanceof AbstractVMInstallType)
      {
        AbstractVMInstallType install = (AbstractVMInstallType) installType;
        for (IVMInstall vmInstall : install.getVMInstalls())
        {
          if (vmInstall instanceof IVMInstall2)
          {
            IVMInstall2 vmInstall2 = (IVMInstall2)vmInstall;
            if (vmInstall2.getJavaVersion() != null && vmInstall2.getJavaVersion().startsWith(desiredVersion))
            {
              IPath containerPath = new Path(JavaRuntime.JRE_CONTAINER);
              IPath vmPath = containerPath.append(vmInstall.getVMInstallType().getId()).append(vmInstall.getName());
              return JavaCore.newContainerEntry(vmPath);
            }
          }
        }
      }
    }
    return null;
  }

  public void removeJavaProjectSourceFolder(String sourceFolder, IProgressMonitor monitor)
  throws CoreException, JavaModelException
  {
    if (!isJavaCapable())
      return;

    IPath sourcePath = project.getSourceProject().getFolder(sourceFolder).getFullPath();
    List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
    IClasspathEntry toRemove = null;
    for (IClasspathEntry existingEntry : project.getSourceJavaProject().getRawClasspath())
    {
      if (sourcePath.equals(existingEntry.getPath()))
        toRemove = existingEntry;
      else
        classpathEntries.add(existingEntry);
    }
    if (toRemove != null)
    {
      project.getSourceJavaProject().setRawClasspath(classpathEntries.toArray(new IClasspathEntry[0]), monitor);
      J2EEComponentClasspathUpdater.getInstance().queueUpdate(project.getSourceProject());
    }
  }

  public void setMaven(IProgressMonitor monitor)
  {
    if (isMavenCapable())
    {
      try
      {
        if (!hasMavenNature())
          addMavenNature(monitor);
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Maven Support", project);
      }
    }
  }

  public boolean isMavenCapable()
  {
    return !MdwPlugin.isRcp() && MdwPlugin.workspaceHasMavenSupport();
  }

  public boolean hasMavenNature() throws CoreException
  {
    return project.getSourceProject().hasNature(IMavenConstants.NATURE_ID);
  }

  public void setGradle(IProgressMonitor monitor)
  {
    if (isGradleCapable())
    {
      try
      {
        if (!hasGradleNature())
          addGradleNature(monitor);
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(ex, "Gradle Support", project);
      }
    }
  }

  public boolean isGradleCapable()
  {
    return !MdwPlugin.isRcp() && MdwPlugin.workspaceHasGradleSupport();
  }

  public boolean hasGradleNature() throws CoreException
  {
    return project.getSourceProject().hasNature("org.springsource.ide.eclipse.gradle.core.nature");
  }

  @SuppressWarnings("deprecation")
  public void addMavenNature(IProgressMonitor monitor) throws CoreException
  {
    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setResolveWorkspaceProjects(false);
    configuration.setActiveProfiles("");

    IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
    configurationManager.enableMavenNature(project.getSourceProject(), configuration, monitor);
    configurationManager.updateProjectConfiguration(project.getSourceProject(), monitor);
  }

  public void addGradleNature(IProgressMonitor monitor) throws CoreException
  {
    try
    {
      DotProjectUpdater dotProj = new DotProjectUpdater(project.getSourceProject());
      dotProj.addNature("org.springsource.ide.eclipse.gradle.core.nature");
      dotProj.save(monitor);
    }
    catch (Exception ex)
    {
      throw new CoreException(new Status(Status.ERROR, MdwPlugin.getPluginId(), Status.ERROR, "ERROR: " + ex, ex));
    }
  }

  public void addGradleClasspath(IProgressMonitor monitor) throws CoreException
  {
    try
    {
      DotClasspathUpdater dotCp = new DotClasspathUpdater(project.getSourceProject());
      dotCp.addContainerClasspathEntry("org.springsource.ide.eclipse.gradle.classpathcontainer");
      dotCp.save(monitor);
    }
    catch (Exception ex)
    {
      throw new CoreException(new Status(Status.ERROR, MdwPlugin.getPluginId(), Status.ERROR, "ERROR: " + ex, ex));
    }
  }

  public boolean hasFrameworkJars() throws CoreException
  {
    if (!project.isRemote())
      return true;

    return hasMavenNature() && project.getSourceProject().getFile("pom.xml").exists();
  }

  public boolean hasWebAppJars()
  {
    if (project.isCustomTaskManager())
      return true;

    return project.getSourceProject().getFolder(new Path("web/WEB-INF/lib")).getFile("mdwweb.jar").exists();
  }

  /**
   * For remote and cloud projects.
   */
  public void addFrameworkJarsToClasspath(final IProgressMonitor monitor) throws CoreException
  {
    if (project.isRemote())
    {
      // code generation runs on the UI thread
      Generator generator = new Generator(MdwPlugin.getShell());
      IProject sourceProject = project.getSourceProject();

      // pom.xml
      JetAccess jet = getJet("osgi/remote_pom.xmljet", sourceProject, "pom.xml");
      generator.createFile(jet, monitor);
      addMavenNature(monitor);  // force maven refresh
    }
    else if (project.isOsgi() || project.isWar())
    {
      // pom.xml is generated with project
    }
    else if (project.isCloudProject())
    {
      IFolder earFolder = project.getSourceProject().getFolder(new Path("deploy/ear"));
      addJarsToClasspath(project.getJavaProject(), earFolder, monitor);
      IFolder earAppInfLib = earFolder.getFolder(new Path("APP-INF/lib"));
      addJarsToClasspath(project.getJavaProject(), earAppInfLib, monitor);
    }

    IFolder libFolder = project.getSourceProject().getFolder("lib");
    if (libFolder.exists())
      addJarsToClasspath(project.getJavaProject(), libFolder, monitor);

    IFolder webLibFolder = project.getSourceProject().getFolder(new Path("web/WEB-INF/lib"));
    if (webLibFolder.exists())
      addJarsToClasspath(project.getJavaProject(), webLibFolder, monitor);

    J2EEComponentClasspathUpdater.getInstance().queueUpdateModule(project.getSourceProject());
  }

  public void setJavaBuildOutputPath(String relativePath, IProgressMonitor monitor) throws JavaModelException
  {
    if (getProject().isCloudProject())
    {
      // set the build output path
      IPath fullPath = getProject().getSourceProject().getFolder(new Path(relativePath)).getFullPath();
      getProject().getSourceJavaProject().setOutputLocation(fullPath, monitor);
    }
  }

  private void addJarsToClasspath(IJavaProject javaProject, IFolder libFolder, IProgressMonitor monitor) throws JavaModelException
  {
    List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
    for (IClasspathEntry existingEntry : javaProject.getRawClasspath())
      classpathEntries.add(existingEntry);

    File libDir = new File(libFolder.getRawLocation().toOSString());
    if (libDir.exists() && libDir.isDirectory())
    {
      File[] jarFiles = libDir.listFiles(new FilenameFilter()
        {
          public boolean accept(File dir, String name)
          {
            return name.endsWith(".jar") && !name.endsWith("_src.jar");
          }
        });

      for (File jarFile : jarFiles)
      {
        IPath path = libFolder.getFile(jarFile.getName()).getFullPath();
        IClasspathEntry newEntry = JavaCore.newLibraryEntry(path, null, null);
        boolean already = false;
        for (IClasspathEntry existing : javaProject.getRawClasspath())
        {
          if (existing.getPath().equals(newEntry.getPath()))
          {
            already = true;
            break;
          }
        }
        if (!already)
          classpathEntries.add(newEntry);
      }

      javaProject.setRawClasspath(classpathEntries.toArray(new IClasspathEntry[0]), monitor);
      J2EEComponentClasspathUpdater.getInstance().queueUpdateModule(javaProject.getProject());
    }
  }

  private void updateSourceProjectManifest(IProgressMonitor monitor, IProject sourceProject)
  {
    // update the manifest of the source project
    IFile manifestFile = J2EEProjectUtilities.getManifestFile(sourceProject);
    String manifest = "Manifest-Version: 1.0\nClass-Path:";
    for (File appInfLibFile : project.getAppInfLibFiles())
    {
      String fileName = appInfLibFile.getName();
      if (fileName.endsWith(".jar") && !fileName.endsWith("_src.jar"))
      {
        manifest += " APP-INF/lib/" + fileName + " \n";
      }
    }

    for (File servicesJarFile : project.getServicesLibFiles())
    {
      manifest += " " + servicesJarFile.getName() + " \n";
    }

    PluginUtil.writeFile(manifestFile, manifest, monitor);
  }

  public static String getJavaProjectClasspathEntry(String projectName) throws CoreException
  {
    IJavaProject javaProject = WorkflowProjectManager.getJavaProject(projectName);
    IRuntimeClasspathEntry projectEntry = JavaRuntime.newDefaultProjectClasspathEntry(javaProject);
    return projectEntry.getMemento();
  }

  public static String getJarFileClasspathEntry(IPath path) throws CoreException
  {
    IRuntimeClasspathEntry entry = JavaRuntime.newVariableRuntimeClasspathEntry(path);
    return entry.getMemento();
  }

  private static final String BEA_SYSTEM_LIBS
    = "<system-libraries>\n"
    + "  <library path=\"server/lib/api.jar\" javadoc=\"http://java.sun.com/j2ee/1.4/docs/api\"/>\n"
    + "  <library path=\"server/lib/wls-api.jar\" javadoc=\"http://edocs.bea.com/wls/docs100/javadocs\"/>\n"
    + "  <library path=\"%MODULES_DIR%/com.bea.core.xml.xmlbeans_2.3.1.0.jar\"/>\n"
    + "</system-libraries>\n";

  protected JetAccess getJet(String jetFile, IProject targetProject, String targetPath)
  {
    JetConfig jetConfig = new JetConfig();
    jetConfig.setModel(project);
    jetConfig.setSettings(MdwPlugin.getSettings());
    jetConfig.setPluginId(MdwPlugin.getPluginId());
    jetConfig.setTargetFolder(targetProject.getName());
    jetConfig.setTargetFile(targetPath);
    jetConfig.setTemplateRelativeUri("templates/" + jetFile);
    return new JetAccess(jetConfig);
  }

  public void initializeFrameworkJars()
  {
    final Shell shell = MdwPlugin.getShell();
    ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
    try
    {
      pmDialog.run(false, false, new IRunnableWithProgress()
      {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
          monitor.beginTask("Adding Framework Libraries", 100);
          monitor.worked(5);
          ProjectUpdater updater = new ProjectUpdater(getProject(), getSettings());
          updater.updateFrameworkJars(monitor);
          try
          {
            SubProgressMonitor submon = new SubProgressMonitor(monitor, 75);
            addFrameworkJarsToClasspath(submon);
            createFrameworkSourceCodeAssociations(shell, submon);
            monitor.done();
          }
          catch (CoreException ex)
          {
            throw new InvocationTargetException(ex);
          }
        }
      });
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Framework Jars", getProject());
    }
  }

  public void initializeWebAppJars()
  {
    if (getProject().isRemote())
      return;
    final Shell shell = MdwPlugin.getShell();
    ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
    try
    {
      pmDialog.run(true, false, new IRunnableWithProgress()
      {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
          monitor.beginTask("Adding WebApp Libraries", 100);
          monitor.worked(5);
          try
          {
            ProjectUpdater updater = new ProjectUpdater(getProject(), MdwPlugin.getSettings());
            updater.updateWebProjectJars(monitor);
            createFrameworkSourceCodeAssociations(shell, monitor);
          }
          catch (CoreException ex)
          {
            PluginMessages.log(ex);
          }
        }
      });
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
    }
  }
}
