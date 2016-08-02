/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.FolderTreeDialog;
import com.centurylink.mdw.plugin.designer.model.CucumberTest;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;

public class CucumberLaunchTab extends TestSuiteLaunchTab
{
  private Image image = MdwPlugin.getImageDescriptor("icons/cukes.gif").createImage();

  private IProject project;
  private IFolder folder;
  private String glue;

  private Combo projectCombo;
  private Text featureLocText;
  private Button browseFeatureButton;
  private Text glueText;
  private Button browseGlueButton;

  public void createControl(Composite parent)
  {
    Composite composite = new Composite(parent, SWT.NONE);
    setControl(composite);

    GridLayout topLayout = new GridLayout();
    topLayout.numColumns = 2;
    composite.setLayout(topLayout);

    createProjectSection(composite);
    createLocationsSection(composite);
    createTestCasesSection(composite);
  }

  protected void createProjectSection(Composite parent)
  {
    List<IJavaProject> javaProjects = null;
    try
    {
      javaProjects = WorkflowProjectManager.getJavaProjects();
    }
    catch (JavaModelException ex)
    {
      PluginMessages.log(ex);
    }
    if (javaProjects == null || javaProjects.size() == 0)
      MessageDialog.openError(parent.getShell(), "Error", "No Java projects found");

    new Label(parent, SWT.NONE).setText("Project: ");
    projectCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.verticalIndent = 3;
    gd.widthHint = 200;
    projectCombo.setLayoutData(gd);
    projectCombo.removeAll();
    for (IJavaProject javaProject : javaProjects)
      projectCombo.add(javaProject.getProject().getName());

    projectCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        project = WorkflowProjectManager.getProject(projectCombo.getText());
        folder = null;
        featureLocText.setText("");
        resetTests();
        refreshTestCasesTable();
        setDirty(true);
        validatePage();
      }
    });
  }

  @Override
  protected Composite createLocationsSection(Composite parent)
  {
    Group locationsGroup = new Group(parent, SWT.NONE);
    locationsGroup.setText("Locations");
    GridLayout gl = new GridLayout();
    gl.numColumns = 3;
    locationsGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    locationsGroup.setLayoutData(gd);

    new Label(locationsGroup, SWT.NONE).setText("Features: ");
    featureLocText = new Text(locationsGroup, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
    featureLocText.setLayoutData(gd);
    featureLocText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        if (featureLocText.getText().isEmpty())
          folder = null;
        else
          folder = project.getFolder(featureLocText.getText());
        resetTests();
        refreshTestCasesTable();
        setDirty(true);
        validatePage();
      }
    });

    browseFeatureButton = new Button(locationsGroup, SWT.PUSH);
    browseFeatureButton.setText("Browse...");
    browseFeatureButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        FolderTreeDialog dlg = new FolderTreeDialog(getShell());
        dlg.setInput(project);
        int res = dlg.open();
        if (res == Dialog.OK)
        {
          folder = (IFolder) dlg.getFirstResult();
          featureLocText.setText(folder.getProjectRelativePath().toString());
        }
      }
    });

    new Label(locationsGroup, SWT.NONE).setText("Glue: ");
    glueText = new Text(locationsGroup, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
    glueText.setLayoutData(gd);
    glueText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        glue = glueText.getText();
        setDirty(true);
        validatePage();
      }
    });

    browseGlueButton = new Button(locationsGroup, SWT.PUSH);
    browseGlueButton.setText("Browse...");
    browseGlueButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        FolderTreeDialog dlg = new FolderTreeDialog(getShell());
        dlg.setInput(project);
        int res = dlg.open();
        if (res == Dialog.OK)
        {
          IFolder glueFolder = (IFolder) dlg.getFirstResult();
          glueText.setText(glueFolder.getProjectRelativePath().toString());
        }
      }
    });

    return locationsGroup;
  }

  private void resetTests()
  {
    try
    {
      if (project == null || !project.exists())
      {
        setTestCases(new String[0]);
      }
      else if (folder == null)
      {
        List<String> testPaths = new ArrayList<String>();
        for (CucumberTest test : CucumberTest.getTests(project))
          testPaths.add(test.getPath());
        setTestCases(testPaths.toArray(new String[0]));
      }
      else
      {
        if (folder.exists())
        {
          List<String> testPaths = new ArrayList<String>();
          for (CucumberTest test : CucumberTest.getTests(folder))
            testPaths.add(test.getPath().substring(folder.getProjectRelativePath().toString().length() + 1));
          setTestCases(testPaths.toArray(new String[0]));
        }
        else
        {
          setTestCases(new String[0]);
        }
      }
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
      setTestCases(new String[0]);
    }
  }

  public String getName()
  {
    return "Cucumber Test";
  }

  public Image getImage()
  {
    return image;
  }

  public void initializeFrom(ILaunchConfiguration launchConfig)
  {
    try
    {
      String defaultProj = projectCombo.getItem(0);
      String projAttr = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, defaultProj);
      project = WorkflowProjectManager.getProject(projAttr);
      if (project == null)
      {
        projectCombo.setText("");
      }
      else
      {
        projectCombo.setText(project.getName());
        String folderAttr = launchConfig.getAttribute(CucumberLaunchConfiguration.FOLDER, "");
        if (!folderAttr.isEmpty())
        {
          folder = project.getFolder(folderAttr);
          if (folder == null || !folder.exists())
            featureLocText.setText("");
          else
            featureLocText.setText(folderAttr);
        }
        else
        {
          featureLocText.setText("");
        }
      }

      List<String> features = launchConfig.getAttribute(CucumberLaunchConfiguration.FEATURES, new ArrayList<String>());
      setTestCases(features.toArray(new String[0]));

      glue = launchConfig.getAttribute(CucumberLaunchConfiguration.GLUE, CucumberLaunchConfiguration.DEFAULT_GLUE);
      glueText.setText(glue);

      refreshTestCasesTable();
    }
    catch (CoreException ex)
    {
      PluginMessages.uiError(ex, "Launch Init", getProject());
    }
    validatePage();
  }

  /**
   * @return all project test cases to be displayed (or empty array if none)
   */
  protected String[] getAllTestCases()
  {
    List<String> displayedCases = new ArrayList<String>();
    try
    {
      if (project != null)
      {
        List<CucumberTest> tests;
        if (folder == null)
          tests = CucumberTest.getTests(project);
        else
          tests = CucumberTest.getTests(folder);

        for (CucumberTest test : tests)
        {
          String testPath = test.getPath();
          if (folder != null)
            testPath = testPath.substring(folder.getProjectRelativePath().toString().length() + 1);
          displayedCases.add(testPath);
        }
      }
    }
    catch (CoreException ex)
    {
      PluginMessages.log(ex);
    }

    return displayedCases.toArray(new String[0]);
  }

  public void performApply(ILaunchConfigurationWorkingCopy launchConfig)
  {
    launchConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
    if (folder == null)
      launchConfig.removeAttribute(CucumberLaunchConfiguration.FOLDER);
    else
      launchConfig.setAttribute(CucumberLaunchConfiguration.FOLDER, folder.getProjectRelativePath().toString());

    List<String> features = new ArrayList<String>();
    for (String feature : getTestCases())
      features.add(feature);
    launchConfig.setAttribute(CucumberLaunchConfiguration.FEATURES, features);

    launchConfig.setAttribute(CucumberLaunchConfiguration.GLUE, glue);

    List<String> resPaths = new ArrayList<String>();
    resPaths.add("/" + project.getName());
    launchConfig.setAttribute(CucumberLaunchConfiguration.ATTR_MAPPED_RESOURCE_PATHS, resPaths);
    List<String> resTypes = new ArrayList<String>();
    resTypes.add(CucumberLaunchConfiguration.RESOURCE_TYPE_PROJECT);
    launchConfig.setAttribute(CucumberLaunchConfiguration.ATTR_MAPPED_RESOURCE_TYPES, resTypes);
    launchConfig.setAttribute(CucumberLaunchConfiguration.ATTR_USE_START_ON_FIRST_THREAD, true);

    DebugPlugin.getDefault().addDebugEventListener(new CucumberLaunchListener(launchConfig));
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy launchConfig)
  {
  }

  public void dispose()
  {
    super.dispose();
    image.dispose();
  }

  protected void validatePage()
  {
    setErrorMessage(null);
    setMessage(null);

    if (project == null)
    {
      setErrorMessage("Test cases must reside in a workspace Java project");
      updateLaunchConfigurationDialog();
      return;
    }
    if (folder != null && !folder.getProjectRelativePath().toString().isEmpty() && !folder.exists())
    {
      setErrorMessage("Please select a valid project folder");
      updateLaunchConfigurationDialog();
      return;
    }
    if (glue == null || glue.isEmpty())
    {
      setErrorMessage("Please enter a path for glue code");
      updateLaunchConfigurationDialog();
      return;
    }

    updateLaunchConfigurationDialog();
  }

  protected String getTestType()
  {
    return "cucumber";
  }
}
