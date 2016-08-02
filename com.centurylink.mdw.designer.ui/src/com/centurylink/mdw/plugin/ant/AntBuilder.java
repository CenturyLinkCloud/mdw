/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.ant;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Iterator;

import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.ProjectHelperRepository;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.MessageConsole;
import com.centurylink.mdw.plugin.PluginMessages;

/**
 *  Handles ant builds during wizard execution.
 */
public class AntBuilder
{
  private String buildFileLoc;
  public String getBuildFileLoc() { return buildFileLoc; }
  public void setBuildFileLoc(String loc) { buildFileLoc = loc; }

  private String target;
  public String getTarget() { return target; }
  public void setTarget(String t) { target = t; }

  private File buildFile;
  public File getBuildFile() { return buildFile; }

  private Project antProject;
  public Project getAntProject() { return antProject; }

  private IOConsoleOutputStream buildConsoleOutputStream;

  private String buildErrorMessages;
  public String getBuildErrorMessages() { return buildErrorMessages; }

  public AntBuilder(File buildFile)
  {
    this.buildFile = buildFile;
    System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");

    antProject = new Project();
    getAntProject().init();

    MessageConsole messageConsole = MessageConsole.findConsole("Server Config", MdwPlugin.getImageDescriptor("icons/ant.gif"), MdwPlugin.getDisplay());
    messageConsole.clearConsole();
    buildConsoleOutputStream = messageConsole.newOutputStream();

    getAntProject().addBuildListener(new DefaultLogger()
    {
      @Override
      public void messageLogged(BuildEvent event)
      {
        try
        {
          buildConsoleOutputStream.write(event.getMessage() + "\n");
        }
        catch (IOException ex)
        {
          PluginMessages.log(ex);
        }

        if (event.getPriority() < Project.MSG_WARN && !event.getMessage().startsWith("JAVA_HOME is set to") && !event.getMessage().startsWith("USER_NAME is set to"))
        {
          if (buildErrorMessages == null)
            buildErrorMessages = "";
          int lines = buildErrorMessages.split("\n").length;
          if (lines <= 2)  // two lines max
            buildErrorMessages += event.getMessage() + "\n";
          else if (lines == 3)
            buildErrorMessages += "\n[See output log more details]";
        }
      }
      @Override
      protected void printMessage(String message, PrintStream stream, int priority)
      {
        if (message != null && message.trim().startsWith("BUILD SUCCESSFUL"))
        {
          try
          {
            buildConsoleOutputStream.write(message);
            buildConsoleOutputStream.close();
          }
          catch (IOException ex)
          {
            PluginMessages.log(ex);
          }
        }
      }
    });
  }

  private Task task;
  public Task getTask() { return task; }

  /**
   * For programmatic execution
   */
  public AntBuilder(Task task)
  {
    this.task = task;
    antProject = new Project();
    antProject.setName("internal");
    antProject.init();
    task.setProject(antProject);
  }

  public void executeTask()
  {
    task.perform();
  }

  /**
   * Perform the ant build using the previously-specified options.
   *
   * @param monitor can be null
   */
  public void build(IProgressMonitor monitor)
  throws JavaModelException, MalformedURLException, IOException
  {
    fireBuildStarted(getAntProject());

    getAntProject().log("Ant Buildfile: " + buildFile);
    getAntProject().log("SAXParserFactory: " + SAXParserFactory.class.getName());

    ProjectHelper helper = null;
    for (Iterator<?> iter = ProjectHelperRepository.getInstance().getHelpers(); iter.hasNext(); )
    {
      ProjectHelper projHelper = (ProjectHelper) iter.next();
      if (projHelper.getClass().getName().startsWith("org.apache"))
        helper = projHelper;
    }

    getAntProject().addReference("ant.projectHelper", helper);
    helper.parse(getAntProject(), buildFile);

    AntBuildLogger logger = new AntBuildLogger(monitor);
    getAntProject().addBuildListener(logger);

    getAntProject().fireBuildStarted();
    getAntProject().executeTarget(getTarget());
    getAntProject().fireBuildFinished(null);

  }

  /*
   * We only have to do this because Project.fireBuildStarted is protected.
   * If it becomes public we should remove this and call the appropriate method.
   */
  @SuppressWarnings("rawtypes")
  private void fireBuildStarted(Project project)
  {
    BuildEvent event = new BuildEvent(project);
    for (Iterator i = project.getBuildListeners().iterator(); i.hasNext();)
    {
      BuildListener listener = (BuildListener) i.next();
      listener.buildStarted(event);
    }
  }
}
