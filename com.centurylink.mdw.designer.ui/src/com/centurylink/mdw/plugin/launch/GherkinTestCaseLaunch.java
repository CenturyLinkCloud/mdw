/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.testing.LogMessageMonitor;
import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.designer.testing.TestCaseRun;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Unfortunately Cucumber test cases cannot be run in parallel:
 * https://github.com/cucumber/cucumber-jvm/issues/630
 */
public class GherkinTestCaseLaunch extends TestCaseRun
{
  private static final String CUCUMBER_LAUNCH_TYPE = "com.centurylink.mdw.plugin.launch.CucumberTestCase";

  private static final Object lock = new Object();

  private WorkflowProject workflowProject;
  private ILaunchConfiguration launchConfig;

  public GherkinTestCaseLaunch(TestCase testcase, int run, String masterRequestId,
      DesignerDataAccess dao, LogMessageMonitor monitor, Map<String, ProcessVO> processCache,
      boolean isLoadTest, boolean oneThreadPerCase, boolean oldNamespaces, WorkflowProject workflowProject)
      throws DataAccessException
  {
    super(testcase, run, masterRequestId, dao, monitor, processCache, isLoadTest, oneThreadPerCase, oldNamespaces);
    this.workflowProject = workflowProject;
  }

  @Override
  public void run()
  {
    synchronized(lock)
    {
      try
      {
        launchConfig = getLaunchConfiguration();

        IDebugEventSetListener listener = new IDebugEventSetListener()
        {
          public void handleDebugEvents(DebugEvent[] events)
          {
            for (DebugEvent event : events)
            {
              if (event.getSource() instanceof IProcess)
              {
                IProcess process = (IProcess) event.getSource();
                if (event.getKind() == DebugEvent.CREATE)
                {
                  process.getStreamsProxy().getOutputStreamMonitor().addListener(new IStreamListener()
                  {
                    public void streamAppended(String text, IStreamMonitor monitor)
                    {
                      log.print(text);
                      if (text.equals("===== execute case " + getTestCase().getCaseName() + "\r\n"))
                          getTestCase().setStatus(TestCase.STATUS_RUNNING);
                          getTestCase().setStartDate(new Date());
                    }
                  });
                  process.getStreamsProxy().getErrorStreamMonitor().addListener(new IStreamListener()
                  {
                    public void streamAppended(String text, IStreamMonitor monitor)
                    {
                      log.print(text);
                    }
                  });
                }
                else if (event.getKind() == DebugEvent.TERMINATE)
                {
                  if (process.getLaunch().getLaunchConfiguration().equals(launchConfig) && process.isTerminated() && true)
                  {
                    getTestCase().setEndDate(new Date());
                    try
                    {
                      if (process.getExitValue() == 0)
                      {
                        getTestCase().setStatus(TestCase.STATUS_PASS);
                      }
                      else
                      {
                        String exitMsg = "Cucumber exit code: " + process.getExitValue();
                        log.println(exitMsg);
                        setMessage(exitMsg);  // TODO why not displayed?
                        getTestCase().setStatus(TestCase.STATUS_FAIL);
                      }
                      if (log != System.out)
                        log.close();
                    }
                    catch (DebugException ex)
                    {
                      PluginMessages.log(ex);
                      ex.printStackTrace(log);
                      getTestCase().setStatus(TestCase.STATUS_ERROR);
                      if (log != System.out)
                        log.close();
                    }
                  }
                }
              }
            }
          }
        };

        DebugPlugin.getDefault().addDebugEventListener(listener);
        DebugUITools.launch(launchConfig, ILaunchManager.RUN_MODE);

        // don't return until execution complete
        while (getTestCase().getStatus().equals(TestCase.STATUS_RUNNING) || getTestCase().getStatus().equals(TestCase.STATUS_WAITING))
          Thread.sleep(500);
      }
      catch (Throwable ex)
      {
        PluginMessages.log(ex);
        ex.printStackTrace(log);
        getTestCase().setStatus(TestCase.STATUS_ERROR);
        getTestCase().setEndDate(new Date());
        if (log != System.out)
          log.close();
      }
    }
  }

  /**
   * Actually returns a working copy since we don't save this launch config.
   */
  public ILaunchConfiguration getLaunchConfiguration() throws CoreException
  {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(CUCUMBER_LAUNCH_TYPE);
    ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, "mdwCucumberLaunch_" + getTestCase().getCaseName());

    List<String> resPaths = new ArrayList<String>();
    resPaths.add("/" + workflowProject.getName());
    workingCopy.setAttribute(CucumberLaunchConfiguration.ATTR_MAPPED_RESOURCE_PATHS, resPaths);
    List<String> resTypes = new ArrayList<String>();
    resTypes.add(CucumberLaunchConfiguration.RESOURCE_TYPE_PROJECT);
    workingCopy.setAttribute(CucumberLaunchConfiguration.ATTR_MAPPED_RESOURCE_TYPES, resTypes);
    workingCopy.setAttribute(CucumberLaunchConfiguration.ATTR_USE_START_ON_FIRST_THREAD, true);
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, workflowProject.getName());

    // TODO user-specified arguments (especially glue)
    String args = CucumberLaunchConfiguration.DEFAULT_ARGS;
    // glue
    if (workflowProject.isFilePersist())
    {
      for (WorkflowPackage gluePackage : getGluePackages())
        args += " --glue \"" + gluePackage.getFolder().getProjectRelativePath() + "\"";
    }
    // legacy glue
    File oldGlueFile = new File(workflowProject.getOldTestCasesDir() + "/steps.groovy");
    if (oldGlueFile.exists())
      args += " --glue \"" + oldGlueFile.toString().replace('\\', '/') +  "\"";

    // feature
    if (getTestCase().isLegacy())
    {
      String oldTestSuiteLoc = workflowProject.getOldTestCasesDir().toString().replace('\\', '/');
      args += " \"" + oldTestSuiteLoc + "/" + getTestCase().getCaseName() +  "\""; // + "/commands.feature";
    }
    else
    {
      args += " \"" + workflowProject.getAssetFolder().getProjectRelativePath() + "/" + getTestCase().getPrefix().replace('.', '/') +
          "/" + getTestCase().getName() + RuleSetVO.getFileExtension(RuleSetVO.FEATURE) +  "\"";
    }

    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, args);

    String vmArgs = "-Dmdw.test.case=" + getTestCase().getCaseName();

    if (getTestCase().isLegacy())
      vmArgs += " -Dmdw.test.cases.dir=\"" + workflowProject.getOldTestCasesDir().toString().replace('\\', '/') + "\"";
    else
      vmArgs += " -Dmdw.test.case.file=\"" + getTestCase().getCaseFile().toString().replace('\\', '/') + "\"";

    vmArgs += " -Dmdw.test.case.user=" + workflowProject.getUser().getUsername();
    vmArgs += " -Dmdw.test.server.url=" + workflowProject.getServiceUrl();

    if (stubbing)
    {
      vmArgs += " -Dmdw.test.server.stub=true";
      vmArgs += " -Dmdw.test.server.stubPort=" + workflowProject.getServerSettings().getStubServerPort();
    }

    if (workflowProject.isOldNamespaces())
      vmArgs += " -Dmdw.test.old.namespaces=true";

    if (singleServer)
      vmArgs += " -Dmdw.test.pin.to.server=true";

    if (createReplace)
      vmArgs += " -Dmdw.test.create.replace=true";

    vmArgs += " -Dmdw.test.results.dir=\"" + getTestCase().getResultDirectory().toString().replace('\\', '/') +  "\"";

    if (workflowProject.isFilePersist())
      vmArgs += " -Dmdw.test.workflow.dir=\"" + workflowProject.getAssetDir().toString().replace('\\', '/') +  "\"";
    else
      vmArgs += " -Dmdw.test.jdbc.url=" + workflowProject.getMdwDataSource().getJdbcUrlWithCredentials();

    if (verbose)
      vmArgs += " -Dmdw.test.verbose=true";

    if (this.getMasterRequestId() != null)
      vmArgs += " -Dmdw.test.masterRequestId=" + this.getMasterRequestId();

    // TODO: temporary debug
    //vmArgs += " -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8020";

    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);

    return workingCopy; //.doSave();
  }

  @Override
  public void stop()
  {
    try
    {
      for (ILaunch launch : DebugPlugin.getDefault().getLaunchManager().getLaunches())
      {
        if (launch.getLaunchConfiguration() instanceof CucumberLaunchConfiguration)
          launch.terminate();
      }
      super.stop();
    }
    catch (DebugException ex)
    {
      PluginMessages.log(ex);
    }
  }

  private List<WorkflowPackage> getGluePackages()
  {
    List<WorkflowPackage> gluePackages = new ArrayList<WorkflowPackage>();

    for (WorkflowPackage pkg : workflowProject.getTopLevelPackages())
    {
      if (pkg.getName().endsWith(".testing"))
        gluePackages.add(pkg);
    }
    return gluePackages;
  }

}
