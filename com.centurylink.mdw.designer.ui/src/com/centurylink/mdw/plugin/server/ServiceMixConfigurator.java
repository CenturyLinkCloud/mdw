/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.xml.sax.SAXException;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.preferences.model.ServerConsoleSettings;
import com.centurylink.mdw.plugin.preferences.model.ServerConsoleSettings.ClientShell;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ServiceMixConfigurator extends ServerConfigurator
{
  public ServiceMixConfigurator(ServerSettings serverSettings)
  {
    super(serverSettings);
  }

  public void doConfigure(Shell shell)
  {
    // presently configuration is external/manual
  }

  public boolean hasClientShell() { return true; }

  @SuppressWarnings("restriction")
  public void doDeploy(Shell shell)
  {
    org.eclipse.wst.server.core.internal.Server matchingServer = null;
    for (IServer server : ServerCore.getServers())
    {
      if (server.getRuntime() != null && server.getRuntime().getRuntimeType() != null
          && server.getRuntime().getRuntimeType().getId().startsWith("com.centurylink.server.runtime.servicemix"))
      {
        org.eclipse.wst.server.core.internal.Server smixServer = (org.eclipse.wst.server.core.internal.Server)server;
        if (smixServer.getAllModules().size() > 0)
        {
          String wfProjectName = smixServer.getAllModules().get(0)[0].getName();
          WorkflowProject serverWfp = WorkflowProjectManager.getInstance().getWorkflowProject(wfProjectName);
          if (serverWfp != null && serverWfp.equals(getServerSettings().getProject()))
            matchingServer = smixServer;
        }
      }
    }

    if (matchingServer != null)
    {
      matchingServer.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());
    }
    else
    {
      MessageDialog.openError(shell, "Server Deploy", "Could not find a ServiceMix server with a module matching " + getServerSettings().getProject());
    }
  }

  public void doClientShell(Shell shell)
  {
    try
    {
      // check for shell settings mismatch
      File karafShellProps = new File(getServerSettings().getServerLoc() + FILE_SEP + "etc" + FILE_SEP + "org.apache.karaf.shell.cfg");
      if (karafShellProps.exists())
      {
        Properties shellProps = new Properties();
        shellProps.load(new FileInputStream(karafShellProps));
        String sshPort = shellProps.getProperty("sshPort");
        if (sshPort != null)
        {
          if (!sshPort.equals(String.valueOf(getServerSettings().getCommandPort())))
          {
            if (!MessageDialog.openConfirm(shell, "SSH Port Mismatch", "The SSH port configured in the MDW Server Settings (" + getServerSettings().getCommandPort() + ") "
                + " appears to be different from the SSH port specified in the sshPort property in " + karafShellProps + " (" + sshPort + ").  Continue Client Shell Launch?"))
            {
              return;
            }
          }
        }
      }
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
    }
    try
    {
      Runtime rt = Runtime.getRuntime();
      File smixBinDir = new File(getServerSettings().getHome() + FILE_SEP + "bin");
      String[] env = getEnvironment(false);
      for (int i = 0; i < env.length; i++)
      {
        if (env[i].toUpperCase().startsWith("COMSPEC="))
          env[i] = "COMSPEC="; // comspec causes command-line interpretation problems
      }

      ServerConsoleSettings consoleSettings = MdwPlugin.getSettings().getServerConsoleSettings();
      if (ClientShell.Putty == consoleSettings.getClientShell())
      {
        // putty
        File exe = consoleSettings.getClientShellExe();
        if (exe != null)
        {
          if (exe.exists() && exe.isFile())
            rt.exec(exe + " -ssh -P " + getServerSettings().getCommandPort() + " -l " + getServerSettings().getUser() + " -pw " + getServerSettings().getPassword() + " " + getServerSettings().getHost(), env, exe.getParentFile());
          else
            MessageDialog.openError(shell, "Client Shell", "Putty executable file not found: '" + exe + "'");
        }
        else
        {
          MessageDialog.openError(shell, "Client Shell", "Please specify the Putty executable location in your MDW Server Console preferences");
        }
      }
      else
      {
        // karaf client shell
        rt.exec("cmd /c start client.bat -a " + getServerSettings().getCommandPort() + " -u " + getServerSettings().getUser() + " -p " + getServerSettings().getPassword(), env, smixBinDir);
      }
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Client Shell", getServerSettings().getProject());
    }
  }

  public String launchNewServerCreation(Shell shell)
  {
    // should never happen
    throw new UnsupportedOperationException("Not supported for ServiceMix");
  }

  public void parseServerAdditionalInfo()
  throws IOException, SAXException, ParserConfigurationException
  {
    // nothing to show
  }

  @Override
  public String[] getEnvironment(boolean debug)
  {
    String[] env = new String[debug ? 4 : 2];
    env[0] = "JAVA_HOME=" + getServerSettings().getJdkHome();
    env[1] = "JAVA_OPTS=" + (getServerSettings().getJavaOptions() == null ? "" : getServerSettings().getJavaOptions());
    if (debug)
    {
      env[2] = "KARAF_DEBUG=" +  debug;
      env[3] = "JAVA_DEBUG_OPTS=" + getDebugOptions();
    }
    return (String[])PluginUtil.appendArrays(getCurrentEnv(), env);
  }

  @Override
  public String getCommandDir()
  {
    return getServerSettings().getServerLoc() + FILE_SEP + "bin";
  }

  @Override
  public String getStartCommand()
  {
    String cmd = (FILE_SEP.equals("/") ? "servicemix.sh" : "servicemix.bat");
    if (!new File(getCommandDir() + FILE_SEP + cmd).exists())
      cmd = (FILE_SEP.equals("/") ? "karaf.sh" : "karaf.bat");

    return getCommandDir() + FILE_SEP + cmd;
  }

  @Override
  public String getStopCommand()
  {
    String cmd = (FILE_SEP.equals("/") ? "karaf.sh stop" : "karaf.bat stop");
    return getCommandDir() + FILE_SEP + cmd;
  }

}
