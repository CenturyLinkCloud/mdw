/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.ant.AntBuilder;
import com.centurylink.mdw.plugin.codegen.Generator;
import com.centurylink.mdw.plugin.codegen.JetAccess;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.ServerSettings;

@SuppressWarnings("restriction")
public class WebLogicServerConfigurator extends ServerConfigurator implements IRunnableWithProgress
{
  public WebLogicServerConfigurator(ServerSettings serverSettings)
  {
    super(serverSettings);
  }

  public void doConfigure(Shell shell)
  {
    setShell(shell);

    try
    {
      ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
      pmDialog.run(true, true, this);
    }
    catch (InvocationTargetException ex)
    {
      PluginMessages.uiError(shell, ex, "Configure Server", getServerSettings().getProject());
    }
    catch (InterruptedException ex)
    {
      PluginMessages.log(ex);
      MessageDialog.openWarning(shell, "Configure Server", "Configuration cancelled");
    }
  }

  private boolean deployOnly = false;
  public void doDeploy(Shell shell)
  {
    setShell(shell);
    deployOnly = true;
    try
    {
      ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
      pmDialog.run(true, true, this);
    }
    catch (InvocationTargetException ex)
    {
      PluginMessages.uiError(shell, ex, "Server Deploy", getServerSettings().getProject());
    }
    catch (InterruptedException ex)
    {
      PluginMessages.log(ex);
      MessageDialog.openWarning(shell, "Server Deploy", "Deployment cancelled");
    }
  }

  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
  {
    monitor.beginTask(deployOnly ? "Deploying Application" : "Configuring WebLogic Server", 150);
    monitor.worked(5);
    monitor.subTask("Generating build resources");

    String tempLoc = MdwPlugin.getSettings().getTempResourceLocation();

    AntBuilder antBuilder;
    try
    {
      if (getServerSettings().getProject().isCloudProject())
      {
        IProject proj = getServerSettings().getProject().getSourceProject();
        createCloudBase(proj, monitor);
        Generator generator = new Generator(getShell());

        // deploy/env folder
        PluginUtil.createFoldersAsNeeded(proj, proj.getFolder(tempLoc + "/deploy/env"), monitor);
        // project.properties
        JetAccess jet = getJet("ear/deploy/env/project.propertiesjet", proj, tempLoc + "/deploy/env/project.properties");
        generator.createFile(jet, monitor);
        // env.properties.dev
        jet = getJet("ear/deploy/env/env.properties.devjet", proj, tempLoc + "/deploy/env/env.properties.dev");
        generator.createFile(jet, monitor);
        // ApplicationProperties.xml
        jet = getJet("cloud/ApplicationProperties.xmljet", proj, tempLoc + "/deploy/config/ApplicationProperties.xml");
        generator.createFile(jet, monitor);
        // runConfigWLS.cmd
        jet = getJet("cloud/runConfigWLS.cmdjet", proj, tempLoc + "/deploy/config/runConfigWLS.cmd");
        generator.createFile(jet, monitor);
        // deployEar.py
        jet = getJet("cloud/deployEar.pyjet", proj, tempLoc + "/deploy/config/deployEar.py");
        generator.createFile(jet, monitor);
        // build.xml
        jet = getJet("cloud/build.xmljet", proj, tempLoc + "/build.xml");
        generator.createFile(jet, monitor);

        // startWebLogic.cmd
        File startWebLogic = new File(getServerSettings().getServerLoc() + "/" + (FILE_SEP.equals("/") ? "startWebLogic.sh" : "startWebLogic.cmd"));
        jet = getJet("cloud/startWebLogic.cmdjet", proj, tempLoc + "/" + startWebLogic.getName());
        generator.createFile(jet, monitor);
        File newStartWebLogic =  new File(proj.getFile(new Path(tempLoc + "/" + startWebLogic.getName())).getLocation().toString());
        PluginUtil.copyFile(startWebLogic, new File(startWebLogic.getAbsolutePath() + ".bak"));
        PluginUtil.copyFile(newStartWebLogic, startWebLogic);

        // perform the build
        monitor.setTaskName("Deploying app...");
        antBuilder = getAntBuilder(proj, tempLoc);
        if (deployOnly)
        {
          antBuilder.setTarget("deployEAR");
        }
        else
        {
          antBuilder.setTarget("setupAndDeploy");
          monitor.subTask("Executing Ant build (first time may take a while)");
        }

        antBuilder.getAntProject().setProperty("deploy.dir", tempLoc + "/deploy");
        antBuilder.getAntProject().setProperty("server.config.dir", "deploy/config");
        antBuilder.getAntProject().setProperty("app.lib.dir", "deploy/ear/APP-INF/lib");
        antBuilder.build(monitor);
      }
      else
      {
        IProject proj = getServerSettings().getProject().getEarProject();
        antBuilder = getAntBuilder(proj, null);
        if (deployOnly)
          antBuilder.setTarget("deployEAR");
        else
          antBuilder.setTarget("configureWLS");
        monitor.subTask("Executing Ant build");
        antBuilder.build(monitor);
      }

      getServerSettings().getProject().getSourceProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
      monitor.done();

      if (antBuilder.getBuildErrorMessages() != null)
      {
        final AntBuilder builder = antBuilder;
        MdwPlugin.getDisplay().asyncExec(new Runnable()
        {
          public void run()
          {
            MessageDialog.openError(getShell(), "Ant Build Failed", builder.getBuildErrorMessages().trim());
          }
        });
      }
    }
    catch (Exception ex)
    {
      throw new InvocationTargetException(ex);
    }
    finally
    {
      if (getServerSettings().getProject().isCloudProject())
      {
        String prefVal = MdwPlugin.getStringPref(PreferenceConstants.PREFS_DELETE_TEMP_FILES_AFTER_SERVER_CONFIG);
        if (prefVal == null || prefVal.length() == 0 || Boolean.valueOf(prefVal))
        {
          // cleanup
          try
          {
            getServerSettings().getProject().getSourceProject().getFolder(tempLoc + "/deploy").delete(true, monitor);
            getServerSettings().getProject().getSourceProject().getFile(tempLoc + "/startWebLogic.cmd").delete(true, monitor);
            getServerSettings().getProject().getSourceProject().getFile(tempLoc + "/build.xml").delete(true, monitor);
          }
          catch (CoreException ex)
          {
            PluginMessages.log(ex);
          }
        }
      }
    }
  }

  @SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
  public String launchNewServerCreation(Shell shell)
  {
    String serverHome = getServerSettings().getHome();
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType programType = manager.getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);

    try
    {
      ILaunchConfiguration cfg = programType.newInstance(null, "WebLogic Domain Config Wizard");
      ILaunchConfigurationWorkingCopy wc = cfg.getWorkingCopy();
      wc.setAttribute(IExternalToolConstants.ATTR_LOCATION, serverHome + "/common/bin/config.cmd");
      wc.setAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, serverHome + "/common/bin");
      Map envVars = new HashMap();
      envVars.put("MW_HOME", serverHome.substring(0, serverHome.lastIndexOf(System.getProperty("file.separator"))));
      wc.setAttribute("org.eclipse.debug.core.environmentVariables", envVars);
      cfg = wc.doSave();
      ILaunch launch = cfg.launch(ILaunchManager.RUN_MODE, null, false, false);
      String errors = "";
      for (IProcess process : launch.getProcesses())
        errors += process.getStreamsProxy().getErrorStreamMonitor().getContents() + "\n";
      cfg.delete();

      if (errors.trim().length() > 0)
        throw new IOException(errors.trim());

      return null;
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(shell, ex, "Create Domain", getServerSettings().getProject());
      return null;
    }
  }

  /**
   * Handles WebLogic 8.1 or 9.x/10.x.
   */
  public void parseServerAdditionalInfo()
  throws IOException, SAXException, ParserConfigurationException
  {
    File configxml = null;
    getServerSettings().setDomainName(null);
    getServerSettings().setServerName(null);
    String domainLoc = getServerSettings().getServerLoc();
    configxml = new File(domainLoc + "/config.xml");
    if (!configxml.exists())
      configxml = new File(domainLoc + "/config/config.xml");

    InputStream inStream = new FileInputStream(configxml);
    InputSource src = new InputSource(inStream);
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    SAXParser parser = parserFactory.newSAXParser();
    parser.parse(src, new DefaultHandler()
    {
      // attributes for WebLogic Server config.xml format
      int domainLevel = -1;
      boolean domainElem = false;
      boolean domainNameElem = false;
      int serverLevel = -1;
      boolean serverElem = false;
      boolean serverNameElem = false;

      int level = 0;

      public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException
      {
        if (qName.equals("Domain"))
        {
          getServerSettings().setDomainName(attrs.getValue("Name"));
        }
        if (qName.equals("Server"))
        {
          getServerSettings().setServerName(attrs.getValue("Name"));
        }
        if (qName.equals("domain"))
        {
          domainElem = true;
          domainLevel = level;
        }
        if (qName.equals("server"))
        {
          serverElem = true;
          serverLevel = level;
        }
        if (qName.equals("name"))
        {
          if (domainElem && domainLevel >= 0 && level == domainLevel + 1)
            domainNameElem = true;
          if (serverElem && serverLevel >= 0 && level == serverLevel + 1)
            serverNameElem = true;
        }
        level++;
      }

      public void characters(char[] ch, int start, int length)
        throws SAXException
      {
        if (domainNameElem)
        {
          String domainName = "";
          for (int i = start; i < start + length; i++)
            domainName += ch[i];

          getServerSettings().setDomainName(domainName);
        }
        if (serverNameElem)
        {
          String serverName = "";
          for (int i = start; i < start + length; i++)
            serverName += ch[i];

          getServerSettings().setServerName(serverName);
        }
      }

      public void endElement(String uri, String localName, String qName)
        throws SAXException
      {
        if (qName.equals("name"))
        {
          if (domainElem && domainLevel >= 0 && level == domainLevel + 2)
            domainNameElem = false;
          if (serverElem && serverLevel >= 0 && level == serverLevel + 2)
            serverNameElem = false;
        }
        if (qName.equals("domain"))
        {
          domainElem = false;
        }
        if (qName.equals("server"))
        {
          serverElem = false;
        }
        level--;
      }

    });

    inStream.close();
  }

  public String getCommandDir()
  {
    if (new File(getServerSettings().getServerLoc() + FILE_SEP + "bin").exists())
      return getServerSettings().getServerLoc() + FILE_SEP + "bin";
    else
      return getServerSettings().getServerLoc();
  }

  public String getStartCommand()
  {
    // starts with nodebug option (debug is set via JAVA_OPTIONS)
    String cmd = (FILE_SEP.equals("/") ? "startWebLogic.sh nodebug" : "startWebLogic.cmd nodebug");
    return getCommandDir() + FILE_SEP + cmd;
  }

  public String getStopCommand()
  {
    String cmd = (FILE_SEP.equals("/") ? "stopWebLogic.sh" : "stopWebLogic.cmd");
    return getCommandDir() + FILE_SEP + cmd;
  }

  /**
   * @return a fixed-up copy of the runtime environment
   */
  public String[] getEnvironment(boolean debug)
  {
    String javaOptions = getJavaOptions();
    if (debug && getServerSettings().isDebug())
      javaOptions += getDebugOptions();
    String[] addlEnv = { "JAVA_OPTIONS=" + javaOptions, "MW_HOME=" + new File(getServerSettings().getHome()).getParent(), "USER_MEM_ARGS=" + getMemoryArgs() };
    String[] env = (String[]) PluginUtil.appendArrays(getCurrentEnv(), addlEnv);
    return fixEnvClasspath(env);
  }

  /**
   * TODO Prefs
   */
  protected String getJavaOptions()
  {
    String opts = "-DruntimeEnv=dev "
      + "-Dcom.qwest.appsec.CTECOMFilterConfigFilePath=" + getConfigDir() + FILE_SEP + "CTECOMFilter.config "
      + "-Dcom.qwest.appsec.CTAPPFilterConfigFilePath=" + getConfigDir() + FILE_SEP + "CTAPPFilter.config "
      + "-Djava.util.logging.config.file=" + getConfigDir() + FILE_SEP + "logging.properties "
      + "-Dcom.sun.management.jmxremote -Dvisualvm.id=" + getServerSettings().getProject().getId() + " "
      + (getServerSettings().getJavaOptions() == null ? "" : getServerSettings().getJavaOptions());

    if (!getServerSettings().getProject().checkRequiredVersion(5, 1))
      opts += "-Dcfg.uri=file:///" + getConfigDir() + "/busconnector.xml ";
    return opts;
  }

  protected String getMemoryArgs()
  {
    return "-Xms256m -Xmx512m -XX:MaxPermSize=256m";  // TODO Prefs
  }
}
