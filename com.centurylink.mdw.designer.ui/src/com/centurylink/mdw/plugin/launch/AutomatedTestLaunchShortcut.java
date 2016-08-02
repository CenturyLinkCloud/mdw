/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.ResourceWrapper;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AutomatedTestLaunchShortcut implements ILaunchShortcut
{
  public static final String GROUP_ID = "com.centurylink.mdw.plugin.launch.group.automated.test";
  public static final String TYPE_ID = "com.centurylink.mdw.plugin.launch.AutomatedTest";

  public void launch(ISelection sel, String mode)
  {
    StructuredSelection selection = (StructuredSelection) sel;
    Object firstElement = selection.getFirstElement();
    WorkflowProject project = null;
    try
    {
      if (firstElement instanceof WorkflowElement)
      {
        WorkflowElement element = (WorkflowElement) firstElement;
        project = element.getProject();
        boolean prevEnablement = disableBuildBeforeLaunch();
        if (selection.size() > 1 || element instanceof AutomatedTestCase)
        {
          List<AutomatedTestCase> testCases = new ArrayList<AutomatedTestCase>();
          for (Object obj : selection.toArray())
            testCases.add((AutomatedTestCase)obj);
          performLaunch(testCases, mode);
        }
        else
        {
          performLaunch(element, mode);
        }
        setBuildBeforeLaunch(prevEnablement);
      }
      else
      {
        ResourceWrapper resourceWrapper = new ResourceWrapper(firstElement);
        IFile firstFile = resourceWrapper.getFile();
        if (firstFile != null)
        {
          project = WorkflowProjectManager.getInstance().getWorkflowProject(firstFile.getProject());
          // test case file(s)
          List<AutomatedTestCase> testCases = new ArrayList<AutomatedTestCase>();
          for (Object obj : selection.toArray())
          {
            IFile file = (IFile) obj;
            WorkflowPackage pkg = project.getPackage((IFolder)file.getParent());
            testCases.add((AutomatedTestCase)pkg.getAsset(file));
          }
          performLaunch(testCases, mode);
        }
        else
        {
          IFolder folder = resourceWrapper.getFolder();
          if (folder != null)
          {
            project = WorkflowProjectManager.getInstance().getWorkflowProject(folder.getProject());
            performLaunch(project.getPackage(folder), mode);
          }
          else
          {
            IProject proj = resourceWrapper.getProject();
            project = WorkflowProjectManager.getInstance().getWorkflowProject(proj);
            performLaunch(project, mode);
          }
        }
      }
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Test Exec", project);
    }
  }

  public void launch(IEditorPart editor, String mode)
  {
    if (editor.getEditorInput() instanceof IFileEditorInput)
    {
      IFileEditorInput fileEditorInput = (IFileEditorInput) editor.getEditorInput();
      IFile file = fileEditorInput.getFile();
      WorkflowProject project = WorkflowProjectManager.getInstance().getWorkflowProject(file.getProject());
      WorkflowPackage pkg = project.getPackage((IFolder)file.getParent());
      try
      {
        performLaunch(pkg.getAsset(file), mode);
      }
      catch (CoreException ex)
      {
        PluginMessages.uiError(ex, "Test Exec", project);
      }
    }
  }

  private void performLaunch(WorkflowElement element, String mode) throws CoreException
  {
    ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(element);
    ILaunchConfiguration launchConfig = findExistingLaunchConfiguration(workingCopy);
    if (launchConfig == null)
    {
      // no existing found - create a new one
      launchConfig = workingCopy.doSave();
    }
    else
    {
      // update test_cases to match selected workflow element
      String prefix;
      if (launchConfig.getAttribute(AutomatedTestLaunchConfiguration.IS_LOAD_TEST, false))
        prefix = AutomatedTestCase.LOAD_TEST;
      else
        prefix = AutomatedTestCase.FUNCTION_TEST;
      List<String> testCases = workingCopy.getAttribute(prefix + "_" + AutomatedTestLaunchConfiguration.TEST_CASES, new ArrayList<String>());
      workingCopy = launchConfig.getWorkingCopy();
      workingCopy.setAttribute(prefix + "_" + AutomatedTestLaunchConfiguration.TEST_CASES, testCases);
      launchConfig = workingCopy.doSave();
    }

    IStructuredSelection selection = new StructuredSelection(launchConfig);
    DebugUITools.openLaunchConfigurationDialogOnGroup(getShell(), selection, GROUP_ID);
  }

  /**
   * All test cases must be in the same package or legacy suite.
   */
  private void performLaunch(List<AutomatedTestCase> cases, String mode) throws CoreException
  {
    AutomatedTestCase firstCase = cases.get(0);
    WorkflowProject workflowProject = firstCase.getProject();
    WorkflowPackage workflowPackage = firstCase.getPackage();
    boolean isLegacyLaunch = firstCase.isLegacy();

    String testName;
    if (isLegacyLaunch)
      testName = workflowProject.getName() + " Legacy";
    else
      testName = workflowPackage.getName();
    List<String> testCases = new ArrayList<String>();
    for (AutomatedTestCase testCase : cases)
      testCases.add(testCase.getPath());

    ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(workflowProject, workflowPackage, isLegacyLaunch, testName, testCases);
    ILaunchConfiguration config = findExistingLaunchConfiguration(workingCopy);
    if (config == null)
    {
      // no existing found - create a new one
      config = workingCopy.doSave();
    }
    else
    {
      workingCopy = config.getWorkingCopy();
      String prefix;
      if (workingCopy.getAttribute(AutomatedTestLaunchConfiguration.IS_LOAD_TEST, false))
        prefix = AutomatedTestCase.LOAD_TEST;
      else
        prefix = AutomatedTestCase.FUNCTION_TEST;

      workingCopy.setAttribute(prefix + "_" + AutomatedTestLaunchConfiguration.TEST_CASES, testCases);
      config = workingCopy.doSave();
    }
    IStructuredSelection selection = new StructuredSelection(config);
    DebugUITools.openLaunchConfigurationDialogOnGroup(getShell(), selection, GROUP_ID);
  }

  private Shell getShell()
  {
    return MdwPlugin.getActiveWorkbenchWindow().getShell();
  }

  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(WorkflowElement element) throws CoreException
  {
    String testName = null;
    List<String> testCases = new ArrayList<String>();

    WorkflowProject workflowProject = element.getProject();
    WorkflowPackage workflowPackage = null;
    boolean isLegacyLaunch = false;

    if (element instanceof WorkflowProject)
    {
      testName = workflowProject.getName();
      testCases = workflowProject.getTestCaseStringList();
    }
    else if (element instanceof WorkflowPackage)
    {
      workflowPackage = (WorkflowPackage) element;
      testName = workflowPackage.getName();
      testCases = workflowPackage.getTestCaseStringList();
    }
    else if (element instanceof AutomatedTestCase)
    {
      AutomatedTestCase testCase = (AutomatedTestCase) element;
      if (testCase.isLegacy())
      {
        testName = workflowProject.getName() + " Legacy";
        isLegacyLaunch = true;
      }
      else
      {
        workflowPackage = testCase.getPackage();
        testName = workflowPackage.getName();
      }
      testCases = new ArrayList<String>();
      testCases.add(testCase.getPath());
    }
    else if (element instanceof AutomatedTestSuite)
    {
      testName = workflowProject.getName() + " Legacy";
      isLegacyLaunch = true;
      testCases = ((AutomatedTestSuite)element).getTestCaseStringList();
    }

    return createLaunchConfiguration(workflowProject, workflowPackage, isLegacyLaunch, testName, testCases);
  }

  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(WorkflowProject workflowProject, WorkflowPackage workflowPackage,
      boolean isLegacyLaunch, String testName, List<String> testCases) throws CoreException
  {
    ILaunchConfigurationType configType = getLaunchManager().getLaunchConfigurationType(TYPE_ID);
    ILaunchConfigurationWorkingCopy wc = configType.newInstance(workflowProject.getSourceProject(),
        getLaunchManager().generateLaunchConfigurationName(testName));

    wc.setAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PROJECT, workflowProject.getName());
    if (workflowPackage == null || workflowPackage.equals(workflowProject.getDefaultPackage()))
      wc.removeAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PACKAGE);
    else
      wc.setAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PACKAGE, workflowPackage.getName());
    wc.setAttribute(AutomatedTestLaunchConfiguration.IS_LEGACY_LAUNCH, isLegacyLaunch);
    wc.setAttribute(AutomatedTestCase.FUNCTION_TEST + "_" +
        AutomatedTestLaunchConfiguration.RESULTS_PATH, workflowProject.getTestResultsPath(AutomatedTestCase.FUNCTION_TEST));
    wc.setAttribute(AutomatedTestCase.LOAD_TEST + "_" +
        AutomatedTestLaunchConfiguration.RESULTS_PATH, workflowProject.getTestResultsPath(AutomatedTestCase.LOAD_TEST));
    wc.setAttribute(AutomatedTestCase.FUNCTION_TEST + "_" + AutomatedTestLaunchConfiguration.TEST_CASES, testCases);
    wc.setAttribute(AutomatedTestCase.LOAD_TEST + "_" + AutomatedTestLaunchConfiguration.TEST_CASES, testCases);
    return wc;
  }

  private ILaunchConfiguration findExistingLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException
  {
    String wcWorkflowProject = workingCopy.getAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PROJECT, "");
    String wcWorkflowPackage = workingCopy.getAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PACKAGE, "");
    boolean wcIsLegacyLaunch = workingCopy.getAttribute(AutomatedTestLaunchConfiguration.IS_LEGACY_LAUNCH, false);

    ILaunchConfigurationType configType = workingCopy.getType();
    ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations(configType);
    for (ILaunchConfiguration launchConfig : configs)
    {
      String workflowProject = launchConfig.getAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PROJECT, "");
      if (!wcWorkflowProject.equals(workflowProject))
        continue;
      String workflowPackage = launchConfig.getAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PACKAGE, "");
      if (!wcWorkflowPackage.equals(workflowPackage))
        continue;
      boolean isLegacyLaunch = launchConfig.getAttribute(AutomatedTestLaunchConfiguration.IS_LEGACY_LAUNCH, false);
      if (wcIsLegacyLaunch != isLegacyLaunch)
        continue;

      return launchConfig;

    }
    return null;
  }

  private ILaunchManager getLaunchManager()
  {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  /**
   * Returns previous pref value.
   */
  protected boolean disableBuildBeforeLaunch()
  {
    boolean buildBeforeLaunchPref = MdwPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH);
    MdwPlugin.getDefault().getPreferenceStore().setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, false);
    return buildBeforeLaunchPref;
  }

  protected void setBuildBeforeLaunch(boolean buildBeforeLaunchPref)
  {
    MdwPlugin.getDefault().getPreferenceStore().setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, buildBeforeLaunchPref);
  }
}
