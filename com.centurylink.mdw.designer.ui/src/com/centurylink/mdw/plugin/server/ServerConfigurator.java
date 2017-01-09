/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.ant.AntBuilder;
import com.centurylink.mdw.plugin.codegen.JetAccess;
import com.centurylink.mdw.plugin.codegen.JetConfig;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.ServerSettings;

public abstract class ServerConfigurator
{
  private ServerSettings serverSettings;
  public ServerSettings getServerSettings() { return serverSettings; }

  private Shell shell;
  public Shell getShell() { return shell; }
  public void setShell(Shell shell) { this.shell = shell; }

  public static final class Factory
  {
    /**
     * Only WebLogic needs workflowProject.
     */
    public static ServerConfigurator create(ServerSettings serverSettings)
    {
      if (serverSettings.isWebLogic())
        return new WebLogicServerConfigurator(serverSettings);
      else if (serverSettings.isJBoss())
        return new JBossServerConfigurator(serverSettings);
      else if (serverSettings.isServiceMix())
        return new ServiceMixConfigurator(serverSettings);
      else if (serverSettings.isFuse())
        return new FuseConfigurator(serverSettings);
      else if (serverSettings.isTomcat())
        return new TomcatConfigurator(serverSettings);
      else
        throw new IllegalArgumentException("Unsupported container: " + serverSettings.getContainerName());
    }
  }

  protected ServerConfigurator(ServerSettings serverSettings)
  {
    this.serverSettings = serverSettings;
  }

  public abstract void doConfigure(Shell shell);
  public abstract void doDeploy(Shell shell);
  public abstract String launchNewServerCreation(Shell shell);
  public abstract void parseServerAdditionalInfo() throws IOException, SAXException, ParserConfigurationException;

  protected AntBuilder getAntBuilder(IProject project, String buildFilePath)
  {
    String buildFileLoc = buildFilePath == null ? "build.xml" : buildFilePath + "/build.xml";
    File buildFile = project.getFile(buildFileLoc).getRawLocation().toFile();
    AntBuilder antBuilder = new AntBuilder(buildFile);
    antBuilder.getAntProject().setBaseDir(project.getLocation().toFile());
    antBuilder.getAntProject().setProperty("env", "dev");
    return antBuilder;
  }

  public static final String FILE_SEP = System.getProperty("file.separator");
  public static final String PATH_SEP = System.getProperty("path.separator");

  /**
   * Gets a string array containing the current name/value pairs for the runtime environment.
   */
  protected String[] getCurrentEnv()
  {
    String fileSep = System.getProperty("file.separator");
    String envCmd = (fileSep.equals("/") ? "env" : "cmd /c set");

    List<String> currEnvList = new ArrayList<String>();
    Runtime rt = Runtime.getRuntime();
    try
    {
      Process p = rt.exec(envCmd);
      BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String inLine = "";
      while ((inLine = br.readLine()) != null)
      {
        currEnvList.add(inLine);
      }
      br.close();
      return (String[]) currEnvList.toArray(new String[] {});
    }
    catch (IOException ex)
    {
      PluginMessages.log(ex);
      return null;
    }
  }

  public abstract String[] getEnvironment(boolean debug);
  public abstract String getCommandDir();
  public abstract String getStartCommand();
  public abstract String getStopCommand();

  public boolean hasClientShell() { return false; }

  public void doClientShell(Shell shell)
  {
    // default does nothing
  }

  public String getConfigDir()
  {
    if (getServerSettings().getProject().isCloudProject())
      return getServerSettings().getProject().getSourceProject().getFolder(new Path("deploy/config")).getLocation().toOSString();
    else
    {
      File mdwDir = new File(serverSettings.getServerLoc() + FILE_SEP + "mdw");

      if (mdwDir.exists() && mdwDir.isDirectory())
        return mdwDir.toString() + FILE_SEP + "config";
      else
        return serverSettings.getServerLoc() + FILE_SEP + "Qwest" + FILE_SEP + "config";  // compatibility
    }
  }

  /**
   * @return additional classpath locations
   */
  protected String getClasspathAdditions()
  {
    return getConfigDir() + PATH_SEP;
  }

  /**
   * Add the MDW stuff to the classpath.
   *
   * @param env before the fix
   * @return the env array after the fix
   */
  protected String[] fixEnvClasspath(String[] env)
  {
    // append the mdw stuff to the classpath
    String[] fixedEnv = null;
    String newClasspath = null;
    int envCpIdx = 0;
    for (int i = 0; i < env.length; i++)
    {
      String entry = env[i];
      if (entry.length() >= 10)
      {
        String begin = entry.substring(0, 10);
        if (begin.equalsIgnoreCase("classpath="))
        {
          if (entry.indexOf(getClasspathAdditions()) > 0)
          {
            newClasspath = entry;
          }
          else
          {
            newClasspath = entry + PATH_SEP + getClasspathAdditions();
          }
          envCpIdx = i;
          break;
        }
      }
    }

    if (newClasspath == null)
    {
      fixedEnv = new String[env.length + 2];
      for (int i = 0; i < env.length; i++)
        fixedEnv[i] = env[i];
      fixedEnv[env.length] = "CLASSPATH=" + getClasspathAdditions();
      fixedEnv[env.length + 1] = "PATCH_CLASSPATH=" + getClasspathAdditions();
    }
    else
    {
      fixedEnv = new String[env.length + 1];
      for (int i = 0; i < env.length; i++)
        fixedEnv[i] = env[i];
      fixedEnv[envCpIdx] = newClasspath;
      fixedEnv[env.length] = "PATCH_CLASSPATH=" + newClasspath.substring(10);
    }

    return fixedEnv;
  }

  protected String getDebugOptions()
  {
    return "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,"
        + (getServerSettings().isSuspend() ? "suspend=y," : "suspend=n,")
        + "address=" + getServerSettings().getDebugPort();
  }

  protected void createCloudBase(IProject project, IProgressMonitor monitor)
  throws CoreException, IOException
  {
    try
    {
      String tempLoc = MdwPlugin.getSettings().getTempResourceLocation();
      PluginUtil.unzipPluginResource("earBase.jar", null, project, tempLoc, monitor);
      ProjectUpdater updater = new ProjectUpdater(getServerSettings().getProject(), MdwPlugin.getSettings());
      updater.updateMappingTemplates(project.getFolder(new Path(tempLoc + "/deploy/config")), monitor);
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
      throw new IOException(ex.getMessage());
    }
  }

  protected JetAccess getJet(String jetFile, IProject targetProject, String targetPath)
  {
    JetConfig jetConfig = new JetConfig();
    jetConfig.setModel(getServerSettings().getProject());
    jetConfig.setSettings(MdwPlugin.getSettings());
    jetConfig.setPluginId(MdwPlugin.getPluginId());
    jetConfig.setTargetFolder(targetProject.getName());
    jetConfig.setTargetFile(targetPath);
    jetConfig.setTemplateRelativeUri("templates/" + jetFile);
    return new JetAccess(jetConfig);
  }
}
