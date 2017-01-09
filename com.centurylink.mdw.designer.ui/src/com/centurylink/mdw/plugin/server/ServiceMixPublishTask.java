/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import com.centurylink.mdw.plugin.CodeTimer;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.GradleBuildFile;
import com.centurylink.mdw.plugin.project.model.MavenBuildFile;
import com.centurylink.mdw.plugin.project.model.OsgiBuildFile;
import com.centurylink.mdw.plugin.project.model.OsgiManifestDescriptor;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ServiceMixPublishTask extends PublishTaskDelegate
{
  @SuppressWarnings("rawtypes")
  @Override
  public PublishOperation[] getTasks(IServer server, int kind, List modules, List kindList)
  {
    if (modules == null)
      return null;

    ServiceMixServerBehavior serverBehave = null;
    if (server.getServerType().getId().startsWith(ServiceMixServer.ID_PREFIX))
      serverBehave = (ServiceMixServerBehavior) server.loadAdapter(ServiceMixServerBehavior.class, null);
    else if (server.getServerType().getId().startsWith(FuseServer.ID_PREFIX))
      serverBehave = (FuseServerBehavior) server.loadAdapter(FuseServerBehavior.class, null);

    if (serverBehave == null)
      return null;

    List<PublishOperation> tasks = new ArrayList<PublishOperation>();
    int size = modules.size();
    for (int i = 0; i < size; i++)
    {
      IModule[] module = (IModule[]) modules.get(i);
      Integer in = (Integer) kindList.get(i);
      tasks.add(new ServiceMixPublishOperation(serverBehave, kind, module, in.intValue()));
    }

    return tasks.toArray(new PublishOperation[tasks.size()]);
  }

  class ServiceMixPublishOperation extends PublishOperation
  {
    private ServiceMixServerBehavior server;
    private IModule[] module;
    private int kind;
    private int deltaKind;
    private PublishHelper publishHelper;

    public ServiceMixPublishOperation(ServiceMixServerBehavior server, int kind, IModule[] module, int deltaKind)
    {
      super("Publish to server", "Publish bundle to server/instance");
      this.server = server;
      this.module = module;
      this.kind = kind;
      this.deltaKind = deltaKind;
      publishHelper = new PublishHelper(null);
    }

    @Override
    public int getKind()
    {
      return REQUIRED;
    }

    @Override
    public int getOrder()
    {
      return 0;
    }

    @Override
    public void execute(IProgressMonitor monitor, IAdaptable info) throws CoreException
    {
      // parent web module
      if (module.length == 1)
      {
        PluginMessages.log("Publish: '" + module[0].getName() + "' ", IStatus.INFO);
        publishDir(module[0], monitor);
      }
    }

    private void publishDir(IModule module2, IProgressMonitor monitor) throws CoreException
    {
      try
      {
        CodeTimer timer = new CodeTimer("parseBnd() for " + module2.getProject());

        OsgiBuildFile buildFile = new GradleBuildFile(module2.getProject());
        if (!buildFile.exists())
          buildFile = new MavenBuildFile(module2.getProject());  // fall back to pom.xml
        if (!buildFile.exists())
        {
          PluginMessages.log("neither build.gradle nor pom.xml was found");
          return;  // can happen when project deleted from workspace
        }
        OsgiManifestDescriptor manifestDescriptor = buildFile.parse();

        timer.stopAndLog();

        boolean isWeb = module2.getModuleType().getId().equals("jst.web");

        ServerSettings serverSettings = server.getServerSettings();
        if (serverSettings.getServerLoc() == null)
          throw new FileNotFoundException("Server location is null.  Please populate server info.");
        if (!(new File(serverSettings.getServerLoc()).exists()))
          throw new FileNotFoundException("Server location not found: " + serverSettings.getServerLoc());
        File instanceDeployDir = new File(serverSettings.getServerLocWithFwdSlashes() + "/deploy/" + buildFile.getArtifactName());
        IPath publishPath = new Path(instanceDeployDir.toString());

        File metaInfDir = publishPath.append("/META-INF").toFile();

        // refresh output
        if (server.isRefreshOutputDirectoryBeforePublish())
        {
          IFolder outputFolder = module2.getProject().getFolder(buildFile.getOutputDirectory());
          outputFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }

        boolean hasDeltas = false;
        if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED)
        {
          if (instanceDeployDir.exists())
          {
            IStatus[] statuses = PublishHelper.deleteDirectory(instanceDeployDir, monitor);
            if (hasError(statuses))
              return;
          }
          if (deltaKind == ServerBehaviourDelegate.REMOVED)
          {
            server.setPublishState(IServer.PUBLISH_STATE_NONE);
            return;
          }
        }
        else if (!instanceDeployDir.exists())
        {
          if (!instanceDeployDir.mkdir())
          {
            PluginMessages.log("Error creating directory: " + instanceDeployDir);
            showError("Error creating directory: " + instanceDeployDir, "Server Deploy", server.getProject());
            return;
          }
        }

        if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL || !metaInfDir.exists())
        {
          IModuleResource[] mr = server.getResources(module);
          IStatus[] statuses = publishHelper.publishFull(mr, publishPath, monitor);
          if (hasError(statuses))
            return;
        }
        else
        {
          IModuleResourceDelta[] delta = server.getPublishedResourceDelta(module);

          int size = delta.length;
          if (size > 0)
            hasDeltas = true;
          for (int i = 0; i < size; i++)
          {
            IStatus[] statuses = publishHelper.publishDelta(delta[i], publishPath, monitor);
            if (hasError(statuses))
              return;
          }
        }

        // update the manifest
        if (buildFile instanceof MavenBuildFile && isWeb)
        {
          // special handling for MDW and other webapps for Maven (pre-5.5)
          // TODO: remove this fragile logic as soon as feasible
          if (module2.getProject().getName().startsWith("MDW"))
          {
            String osgiSrc = module2.getProject().getLocation().toFile().toString() + "/" + buildFile.getArtifactGenDir() + "/osgi/";
            String osgiWebInf = osgiSrc + buildFile.getArtifactName() + "/WEB-INF";
            // copy the built meta-inf
            File metaInfSrc = new File(osgiWebInf + "/classes/META-INF");
            if (metaInfSrc.exists())
            {
              File metaInfDest = new File(publishPath + "/META-INF");
              // TODO exclude faces-config.xml to avoid duplicate phase listener processing?
              // (doesn't seem to be happening anymore)
              PluginUtil.copyDirectory(metaInfSrc, metaInfDest, false);
            }
            // copy the built webapp libs (maven dependencies)
            File webLibSrc = new File(osgiWebInf + "/lib");
            if (webLibSrc.exists())
            {
              File webLibDest = new File(publishPath +"/WEB-INF/lib");
              PluginUtil.copyDirectory(webLibSrc, webLibDest, false);
            }
            // copy the osgi webapp descriptors if they exist
            File webInfOsgi = module2.getProject().getLocation().append("/web/WEB-INF/osgi").toFile();
            if (webInfOsgi.exists())
            {
              File webXmlSrc = new File(webInfOsgi + "/web.xml");
              File webXmlDest = publishPath.append("/WEB-INF/web.xml").toFile();
              if (webXmlSrc.lastModified() > webXmlDest.lastModified())
                PluginUtil.copyFile(webXmlSrc, webXmlDest);
            }
            File innerWebInf = publishPath.append("/WEB-INF/osgi/web.xml").toFile();
            if (innerWebInf.exists())
              innerWebInf.delete();
          }
          else
          {
            String mfPath = buildFile.getArtifactName() + "/WEB-INF/classes/META-INF/MANIFEST.MF";
            File mf = module2.getProject().getFile("target/" + mfPath).getLocation().toFile();
            File mfDest = publishPath.append("/META-INF/MANIFEST.MF").toFile();
            if (mf.exists() && mf.lastModified() > mfDest.lastModified())
              PluginUtil.copyFile(mf, mfDest);
            File innerWebInf = publishPath.append("/WEB-INF/classes/WEB-INF").toFile();
            if (innerWebInf.exists())
              PluginUtil.deleteDirectory(innerWebInf);
          }
        }
        else
        {
          if (isWeb) // gradle web
          {
            // grab the embedded libs from generated wars
            String archiveName =  buildFile.getArtifactName() + (isWeb ? ".war" : ".jar");
            File archive;
            if (buildFile.getArtifactGenDir().startsWith("..")) // relative path -- one level too high
              archive = new File(module2.getProject().getLocation().toFile().toString() + buildFile.getArtifactGenDir().substring(2) + "/" + archiveName);
            else
              archive = new File(module2.getProject().getLocation().toFile().toString() + "/" + buildFile.getArtifactGenDir() + "/" + archiveName);
            if (!archive.exists())
            {
              PluginMessages.log("Unable to locate web archive: " + archive);
              showError("Unable to locate web archive: " + archive, "Server Deploy", server.getProject());
              return;

            }
            copyWebInfLibArchiveEntriesToDir(archive, new File(publishPath.toString()));
          }

          // now for the manifest (everything but maven web)
          if (!metaInfDir.exists())
          {
            if (!metaInfDir.mkdirs())
            {
              PluginMessages.log("Error creating directory: " + metaInfDir);
              showError("Error creating directory: " + metaInfDir, "Server Deploy", server.getProject());
              return;
            }
          }
          FileOutputStream fos = null;
          try
          {
            File mfFile = publishPath.append("META-INF/MANIFEST.MF").toFile();
            if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL || !mfFile.exists() || buildFile.lastModified() > mfFile.lastModified() || mfFile.length() < 100 || hasDeltas)
            {
              String mdwVersion = buildFile.getMdwVersion();
              if (mdwVersion == null)
              {
                WorkflowProject wfp = WorkflowProjectManager.getInstance().getWorkflowProject(module2.getProject());
                if (wfp != null)
                  mdwVersion = wfp.getMdwVersion();
              }
              Manifest mf = manifestDescriptor.createManifest(buildFile.getVersion(), mdwVersion);
              fos = new FileOutputStream(mfFile);
              mf.write(fos);
            }
          }
          finally
          {
            if (fos != null)
              fos.close();
          }
        }

        server.setPublishState(IServer.PUBLISH_STATE_NONE);
      }
      catch (Exception ex)
      {
        PluginMessages.log(ex);
        showError(ex.toString(), "Server Publish", server.getProject());
      }
    }

    private boolean hasError(IStatus[] statuses)
    {
      for (IStatus status : statuses)
      {
        if (status.getSeverity() > IStatus.WARNING)
        {
          PluginMessages.log(status);
          showError(status.getMessage(), "Server Deploy", server.getProject());
          return true;
        }
      }
      return false;
    }

    private void showError(final String message, final String title, final WorkflowProject workflowProject)
    {
      server.setPublishState(IServer.PUBLISH_STATE_INCREMENTAL);
      MdwPlugin.getDisplay().asyncExec(new Runnable()
      {
        public void run()
        {
          PluginMessages.uiError(message, title, workflowProject);
        }
      });
    }
  }

  protected void copyWebInfLibArchiveEntriesToDir(File archive, File destDir) throws IOException
  {
    JarFile archiveFile = null;
    try
    {
      archiveFile = new JarFile(archive);
      for (Enumeration<?> entriesEnum = archiveFile.entries(); entriesEnum.hasMoreElements(); )
      {
        JarEntry jarEntry = (JarEntry) entriesEnum.nextElement();
        if (jarEntry.getName().startsWith("WEB-INF/lib/"))
        {
          File destFile = new File(destDir + "/" + jarEntry.getName());
          if (!destFile.exists())
          {
            if (!destDir.exists())
            {
              if (!destDir.mkdirs())
                throw new IOException("Unable to create web lib destination dir: " + destDir);
            }
            InputStream is = null;
            OutputStream os = null;
            try
            {
              is = archiveFile.getInputStream(jarEntry);
              byte[] buffer = new byte[1024];
              os = new FileOutputStream(destFile);
              while (true)
              {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1)
                  break;
                os.write(buffer, 0, bytesRead);
              }
            }
            finally
            {
              if (is != null)
                is.close();
              if (os != null)
                os.close();
            }
          }
        }
      }
    }
    finally
    {
      if (archiveFile != null)
        archiveFile.close();
    }
  }
}
