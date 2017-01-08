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
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.ResourceWrapper;
import com.centurylink.mdw.plugin.designer.model.CucumberTest;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Shortcut for standalone (non-MDW) Cucumber feature test.
 */
public class CucumberLaunchShortcut implements ILaunchShortcut
{
  public static final String GROUP_ID = "com.centurylink.mdw.plugin.launch.group.cucumber.test";
  public static final String TYPE_ID = "com.centurylink.mdw.plugin.launch.Cucumber";

  public void launch(ISelection sel, String mode)
  {
    StructuredSelection selection = (StructuredSelection) sel;
    Object firstElement = selection.getFirstElement();
    ResourceWrapper resourceWrapper = new ResourceWrapper(firstElement);
    try
    {
      List<CucumberTest> tests = new ArrayList<CucumberTest>();
      IFile file = resourceWrapper.getFile();
      if (file != null)
      {
        // launch gherkin feature test(s)
        for (Object obj : selection.toArray())
        {
          ResourceWrapper fileWrapper = new ResourceWrapper(obj);
          tests.add(new CucumberTest(fileWrapper.getOwningProject(), fileWrapper.getFile()));
        }
        performLaunch(tests, new ResourceWrapper(file.getParent()).getFolder(), mode);
      }
      else
      {
        // package folder
        IFolder folder = resourceWrapper.getFolder();
        if (folder != null)
        {
          CucumberTest.findTests(folder, tests);
          performLaunch(tests, folder, mode);
        }
        else
        {
          IProject proj = resourceWrapper.getProject();
          tests.addAll(CucumberTest.getTests(proj));
          performLaunch(tests, null, mode);
        }
      }
    }
    catch (Exception ex)
    {
      WorkflowProject proj = firstElement instanceof WorkflowElement ? ((WorkflowElement)firstElement).getProject() : null;
      PluginMessages.uiError(ex, "Test Exec", proj);
    }
  }

  public void launch(IEditorPart editor, String mode)
  {
    // TODO Auto-generated method stub

  }

  private void performLaunch(List<CucumberTest> tests, IFolder folder, String mode) throws CoreException
  {
    IProject project = tests.get(0).getProject();

    String launchName;
    if (folder == null)
      launchName = project.getName();
    else
      launchName = folder.getName();

    List<String> testCases = new ArrayList<String>();
    for (CucumberTest test : tests)
    {
      String testPath = test.getPath();
      if (folder != null)
        testPath = testPath.substring(folder.getProjectRelativePath().toString().length() + 1);
      testCases.add(testPath);
    }

    ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(project, folder, launchName, testCases);
    ILaunchConfiguration config = null;
    ILaunchConfigurationType configType = workingCopy.getType();
    ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations(configType);
    for (ILaunchConfiguration launchConfig : configs)
    {
      String projectAttr = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
      if (!project.getName().equals(projectAttr))
        continue;
      String folderMatch = (folder == null) ? "" : folder.getProjectRelativePath().toString();
      String folderAttr = launchConfig.getAttribute(CucumberLaunchConfiguration.FOLDER, "");
      if (!folderMatch.equals(folderAttr))
        continue;
      config = launchConfig;
    }

    if (config == null)
    {
      // no existing found - create a new one
      config = workingCopy.doSave();
    }
    else
    {
      workingCopy = config.getWorkingCopy();
      workingCopy.setAttribute(CucumberLaunchConfiguration.FEATURES, testCases);
      config = workingCopy.doSave();
    }
    IStructuredSelection selection = new StructuredSelection(config);
    DebugUITools.openLaunchConfigurationDialogOnGroup(getShell(), selection, GROUP_ID);
  }

  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IProject project, IFolder folder, String launchName, List<String> testCases) throws CoreException
  {
    ILaunchConfigurationType configType = getLaunchManager().getLaunchConfigurationType(TYPE_ID);
    ILaunchConfigurationWorkingCopy wc = configType.newInstance(project, getLaunchManager().generateLaunchConfigurationName(launchName));
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
    if (folder != null)
      wc.setAttribute(CucumberLaunchConfiguration.FOLDER, folder.getProjectRelativePath().toString());
    wc.setAttribute(CucumberLaunchConfiguration.FEATURES, testCases);

    wc.setAttribute(CucumberLaunchConfiguration.GLUE, CucumberLaunchConfiguration.DEFAULT_GLUE);
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, CucumberLaunchConfiguration.DEFAULT_ARGS);

    return wc;
  }

  private ILaunchManager getLaunchManager()
  {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  private Shell getShell()
  {
    return MdwPlugin.getActiveWorkbenchWindow().getShell();
  }
}
