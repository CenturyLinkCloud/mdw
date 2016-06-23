/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jdt.ui.wizards.NewContainerWizardPage;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

/**
 * Base page class for MDW plug-in wizard pages.
 */
public abstract class WizardPage extends NewClassWizardPage
{
  public abstract void drawWidgets(Composite parent);

  /**
   * Override if using workflow project controls
   */
  public WorkflowElement getElement() { return null; }

  // workflowProjects
  private List<WorkflowProject> workflowProjects;
  public List<WorkflowProject> getProjects() { return workflowProjects; }
  public void setProjects(List<WorkflowProject> projects) { this.workflowProjects = projects; }

  // workflow project
  private WorkflowProject workflowProject;
  public WorkflowProject getProject() { return workflowProject; }
  public void setProject(WorkflowProject wp) { workflowProject = wp; }

  // stock widgets
  protected Combo workflowProjectCombo;
  protected Combo workflowPackageCombo;
  protected Text authorTextField;

  /**
   * Constructor.
   */
  public WizardPage()
  {
  }

  /**
   * Called by IOC to populate the model.
   */
  public void setConfig(Object config)
  {
    this.workflowProject = (WorkflowProject) config;
  }

  public void init(IStructuredSelection selection)
  {
    super.init(selection);
  }

  public void initValues()
  {
    // override to initialize widget values on page display
  }

  /**
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent)
  {
    drawWidgets(parent);
  }

  /**
   * sets the completed field on the wizard class when all the information
   * on the page is entered
   */
  public boolean isPageComplete()
  {
    // default
    return true;
  }

  /**
   * @see Listener#handleEvent(Event)
   */
  public void handleEvent(Event event)
  {
    setPageComplete(isPageComplete());
    getWizard().getContainer().updateButtons();
  }

  protected void createWorkflowProjectControls(Composite parent, int ncol)
  {
    createWorkflowProjectControls(parent, ncol, false);
  }


  protected void createWorkflowProjectControls(Composite parent, int ncol, final boolean handleFieldChanged)
  {
    workflowProjects = WorkflowProjectManager.getInstance().getWorkflowProjects();
    if (workflowProjects == null || workflowProjects.size() == 0)
      MessageDialog.openError(parent.getShell(), "Error", "No MDW Workflow projects found");

    if (getElement() != null && getElement().getProject() != null)
    {
      workflowProject = getElement().getProject();
    }
    else if (workflowProjects.size() > 0)
    {
      for (WorkflowProject project : workflowProjects)
      {
        if (project.isLoaded())
        {
          workflowProject = project;
          break;
        }
      }
      if (workflowProject == null)
        workflowProject = workflowProjects.get(0); // last resort
      getElement().setProject(workflowProject);
    }

    new Label(parent, SWT.NONE).setText("Workflow Project:");
    workflowProjectCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    gd.widthHint = 200;
    workflowProjectCombo.setLayoutData(gd);

    workflowProjectCombo.removeAll();
    for (WorkflowProject project : workflowProjects)
    {
      workflowProjectCombo.add(project.getSourceProjectName());
    }

    workflowProjectCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        workflowProject = WorkflowProjectManager.getInstance().getWorkflowProject(workflowProjectCombo.getText());
        getElement().setProject(workflowProject);
        if (workflowPackageCombo != null)
        {
          workflowPackageCombo.removeAll();
          for (WorkflowPackage packageVersion : workflowProject.getProject().getTopLevelUserVisiblePackages())
            workflowPackageCombo.add(packageVersion.getName());
          workflowPackageCombo.select(0);
        }
        if (handleFieldChanged)
          handleFieldChanged();
      }
    });
    if (getElement().getProject() != null)
      workflowProjectCombo.setText(getElement().getProject().getName());
  }

  protected void createWorkflowPackageControls(Composite parent, int ncol)
  {
    createWorkflowPackageControls(parent, ncol, false);
  }

  protected void createWorkflowPackageControls(Composite parent, int ncol, final boolean handleFieldChanged)
  {
    new Label(parent, SWT.NONE).setText("Workflow Package:");
    workflowPackageCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    gd.widthHint = 200;
    workflowPackageCombo.setLayoutData(gd);
    workflowPackageCombo.removeAll();
    if (getElement().getProject() != null)
    {
      for (WorkflowPackage packageVersion : getElement().getProject().getTopLevelUserVisiblePackages())
        workflowPackageCombo.add(packageVersion.getName());
    }

    workflowPackageCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setPackage(workflowPackageCombo.getText().trim());
        if (handleFieldChanged)
          handleFieldChanged();
      }
    });
    if (getElement().getPackage() != null)
      workflowPackageCombo.setText(getElement().getPackage().getName());
    else
      workflowPackageCombo.select(0);
    getElement().setPackage(getElement().getProject().getTopLevelUserVisiblePackages().get(workflowPackageCombo.getSelectionIndex()));
  }

  public void setPackage(String packageName)
  {
    if (packageName == null || packageName.length() == 0)
      getElement().setPackage(null);
    else
      getElement().setPackage(getElement().getProject().getPackage(packageName));
  }

  public void setPackage(WorkflowPackage packageVersion)
  {
    getElement().setPackage(packageVersion);
  }

  protected void createContainerControls(Composite parent, int columns)
  {
    super.createContainerControls(parent, columns);
    determinePackageFragmentRoot();
  }

  public void determinePackageFragmentRoot()
  {
    determinePackageFragmentRoot(getProject());
  }

  public void determinePackageFragmentRoot(WorkflowProject workflowProject)
  {
    IPackageFragmentRoot oldPackageFragmentRoot = getPackageFragmentRoot();

    if (workflowProject != null && workflowProject.isLocalJavaSupported())
    {
      try
      {
        IPackageFragmentRoot tempRoot = null;
        IPackageFragmentRoot srcRoot = null;
        IJavaProject javaProject = workflowProject == null ? null : workflowProject.getSourceJavaProject();
        if (javaProject != null)
        {
          for (IPackageFragmentRoot pfr : javaProject.getPackageFragmentRoots())
          {
            if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE)
            {
              if (pfr.getElementName().equals(MdwPlugin.getSettings().getTempResourceLocation()))
              {
                tempRoot = pfr;
              }
              else
              {
                srcRoot = pfr;
                break;
              }
            }
          }
          if (srcRoot == null && tempRoot == null)
            srcRoot = javaProject.getPackageFragmentRoot(javaProject.getResource());
          setPackageFragmentRoot(srcRoot == null ? tempRoot : srcRoot, true);
        }
      }
      catch (JavaModelException ex)
      {
        PluginMessages.log(ex);
      }
    }
    else
    {
      setPackageFragmentRoot(getPackageFragmentRoot(), true);
    }
    if (oldPackageFragmentRoot == null || !oldPackageFragmentRoot.equals(getPackageFragmentRoot()))
      setPackageFragment(null, true);
  }

  /**
   * Generates a section label on the wizard page.
   *
   * @param parent
   * @param ncol
   */
  public void createSectionLabel(Composite parent, String text, int ncol)
  {
    createSpacer(parent, ncol);
    Label serviceLabel = new Label(parent, SWT.NONE);
    serviceLabel.setText(text);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol;
    serviceLabel.setLayoutData(gd);
  }

  /**
   * Generates a spacer on the wizard page.
   *
   * @param parent
   * @param ncol
   */
  public void createSpacer(Composite parent, int ncol)
  {
    Label spacerLabel = new Label(parent, ncol);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol;
    gd.heightHint = 3;
    spacerLabel.setLayoutData(gd);
  }

  /**
   * Generates a separation line on the wizard page.
   *
   * @param parent
   * @param ncol
   */
  public void createSepLine(Composite parent, int ncol)
  {
     Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
     GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
     gridData.horizontalSpan = ncol;
     line.setLayoutData(gridData);
  }

  /**
   * updates the wizard status bar
   */
  private void doStatusUpdate()
  {
    saveDataToModel();

    // show statuses related to pages
    IStatus[] statuses = null;
    if (getStatuses() == null)
    {
      statuses = new IStatus[] { new Status(IStatus.OK, "not_used", 0, getDescription(), null) };
    }
    else
    {
      statuses = getStatuses();
    }

    // most severe status will be displayed and the ok button enabled/disabled
    updateStatus(statuses);
  }

  /**
   * Checks a string value for validity.
   *
   * @param s the string to check
   * @return whether the string is valid
   */
  public boolean checkString(String s)
  {
    return (s != null) && (s.length() > 0);
  }

  /**
   * Checks a string value for validity, ensuring no whitespace is present
   * @param s the string
   * @return whether the string is valid
   */
  public boolean checkStringNoWhitespace(String s)
  {
    return (checkString(s) && !containsWhitespace(s));
  }

  public boolean checkStringDisallowChars(String s, String badChars)
  {
    if (!checkString(s))
      return false;
    for (int i = 0; i < s.length(); i++)
    {
      for (int j = 0; j < badChars.length(); j++)
      {
        if (s.charAt(i) == badChars.charAt(j))
          return false;
      }
    }
    return true;
  }

  /**
   * Check a string for whitespace characters.
   * @param s the string
   * @return whether whitespace chars exist
   */
  public boolean containsWhitespace(String s)
  {
    if (s == null)
      return false;
    if (s.indexOf(' ') >= 0)
      return true;
    if (s.indexOf('\t') >= 0)
      return true;
    if (s.indexOf('\n') >= 0)
      return true;
    if (s.indexOf('\r') >= 0)
      return true;

    return false;
  }

  /**
   * Checks an int value for validity.
   *
   * @param i the int to check
   * @return whether the int is valid
   */
  public boolean checkInt(int i)
  {
    return i > 0;
  }

  /**
   * Checks a long value for validity.
   *
   * @param l the long to check
   * @return whether the int is valid
   */
  public boolean checkLong(long l)
  {
    return l > 0;
  }

  /**
   * Checks a URL value for validity.
   *
   * @param s the URL to check
   * @return whether the URL is valid
   */
  public boolean checkUrl(URL url)
  {
    return (url != null) && (url.toString().length() > 0);
  }

  public boolean checkUrl(String url)
  {
    if (url == null)
      return false;
    try
    {
      return checkUrl(new URL(url.trim()));
    }
    catch (MalformedURLException ex)
    {
      return false;
    }
  }

  /**
   * Checks a URI value for validity.
   *
   * @param s the URI to check
   * @return whether the URI is valid
   */
  public boolean checkUri(URI uri)
  {
    return (uri != null) && (uri.toString().length() > 0);
  }

  public boolean checkDir(String dir)
  {
    if (dir == null || dir.length() == 0)
      return true;  // avoid premature error messages
    File checkDir = null;
    try
    {
      checkDir = new File(dir);
    }
    catch (NullPointerException ex)
    {
      return false;
    }
    return checkDir.isDirectory();
  }

  public boolean checkFile(String file)
  {
    if (file == null)
      return false;
    File checkFile = null;
    try
    {
      checkFile = new File(file);
    }
    catch (NullPointerException ex)
    {
      return false;
    }
    return checkFile.exists();
  }
  /**
   * workaround to overcome eclipse wizard behavior for container control
   */
  public int getMaxFieldWidth()
  {
    return 200;
  }

  public String getPluginId()
  {
    return MdwPlugin.getPluginId();
  }

  /**
   * Returns a dialog persistent setting.
   *
   * @param key lookup key
   * @return the value
   */
  public String getDialogSetting(String key)
  {
    return getDialogSettings().get(key);
  }

  /**
   * Returns a dialog persistent setting as a boolean.
   *
   * @param key lookup key
   * @return the value
   */
  public boolean getDialogSettingBoolean(String key)
  {
    String stringVal = getDialogSetting(key);
    return (stringVal != null && stringVal.equalsIgnoreCase("true"));
  }

  /**
   * Returns a dialog persistent setting as an int.
   *
   * @param key lookup key
   * @return the value
   */
  public int getDialogSettingInt(String key)
  {
    String stringVal = getDialogSetting(key);
    try
    {
      return Integer.parseInt(stringVal);
    }
    catch (NumberFormatException ex)
    {
      return -1;
    }
  }

  /**
   * creates composite control and sets the default layout data
   *
   * @param parent the parent of the new composite
   * @param numColumns the number of columns for the new composite
   * @return the newly-created composite
   */
  protected Composite createComposite(Composite parent, int ncol)
  {
    Composite composite = new Composite(parent, SWT.NULL);

    //GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = ncol;
    composite.setLayout(layout);

    //GridData
    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    composite.setLayoutData(data);
    return composite;
  }


  protected void createAuthorControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Author:");
    authorTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 120;
    gd.horizontalSpan = ncol - 1;
    authorTextField.setLayoutData(gd);
    authorTextField.setText(getProject().getAuthor());
    authorTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getProject().setAuthor(authorTextField.getText().trim());
        handleFieldChanged();
      }
    });
  }

  /**
   * @see NewContainerWizardPage#handleFieldChanged
   */
  protected void handleFieldChanged()
  {
    doStatusUpdate();
  }

  protected void handleFieldChanged(String fieldName)
  {
    super.handleFieldChanged(fieldName);
    handleFieldChanged();
  }

  /**
   * Updates the status line and the OK button according to the given status
   *
   * @param status status to apply
   */
  @SuppressWarnings("restriction")
  protected void updateStatus(IStatus status)
  {
    setPageComplete(!status.matches(IStatus.ERROR));
    org.eclipse.jdt.internal.ui.dialogs.StatusUtil.applyToStatusLine(this, status);
  }

  /**
   * Updates the status line and the OK button according to the status evaluate from an array of
   * status. The most severe error is taken. In case that two status with the same severity exists,
   * the status with lower index is taken.
   *
   * @param status  the array of status
   */
  @SuppressWarnings("restriction")
  protected void updateStatus(IStatus[] status)
  {
    updateStatus(org.eclipse.jdt.internal.ui.dialogs.StatusUtil.getMostSevere(status));
  }

  public void saveDataToModel()
  {
  }

  public IStatus[] getStatuses()
  {
    return null;
  }

  /**
   * Override to prefer non-temp package root.
   */
  @SuppressWarnings("restriction")
  @Override
  protected void initContainerPage(IJavaElement elem)
  {
    IPackageFragmentRoot tempRoot = null;  // only as fallback
    IPackageFragmentRoot initRoot = null;
    if (elem != null)
    {
      initRoot = org.eclipse.jdt.internal.corext.util.JavaModelUtil.getPackageFragmentRoot(elem);
      try
      {
        if (initRoot == null || initRoot.getKind() != IPackageFragmentRoot.K_SOURCE)
        {
          IJavaProject jproject = elem.getJavaProject();
          if (jproject != null)
          {
            initRoot = null;
            if (jproject.exists())
            {
              IPackageFragmentRoot[] roots = jproject.getPackageFragmentRoots();
              for (int i = 0; i < roots.length; i++)
              {
                if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE)
                {
                  if (roots[i].getElementName().equals(MdwPlugin.getSettings().getTempResourceLocation()))
                  {
                    tempRoot = roots[i];
                  }
                  else
                  {
                    initRoot = roots[i];
                    break;
                  }
                }
              }
            }
            if (initRoot == null && tempRoot == null)
            {
              initRoot = jproject.getPackageFragmentRoot(jproject.getResource());
            }
          }
        }
      }
      catch (JavaModelException e)
      {
        org.eclipse.jdt.internal.ui.JavaPlugin.log(e);
      }
    }
    setPackageFragmentRoot(initRoot == null ? tempRoot : initRoot, true);
  }

}