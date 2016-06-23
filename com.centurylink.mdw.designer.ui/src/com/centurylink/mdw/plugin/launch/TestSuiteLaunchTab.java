/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.FolderTreeDialog;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class TestSuiteLaunchTab extends AbstractLaunchConfigurationTab
{
  private WorkflowProject project;
  public WorkflowProject getProject() { return project; }
  protected void setProject(WorkflowProject project) { this.project = project; }

  private WorkflowPackage workflowPackage;
  public WorkflowPackage getPackage() { return workflowPackage; }

  private boolean legacyLaunch;
  public boolean isLegacyLaunch() { return legacyLaunch; }

  private String[] testCases = new String[0];
  protected String[] getTestCases() { return testCases; }
  protected void setTestCases(String[] cases) { this.testCases = cases; }

  private Combo workflowProjectCombo;
  private Combo workflowPackageCombo;
  private Text resultsPathText;
  private Button browseResultsPathButton;
  protected Table testCasesTable;
  protected TableViewer testCasesTableViewer;
  protected List<ColumnSpec> testCasesColumnSpecs;
  protected String[] testCasesColumnProps;
  private Button selectAllButton;
  private Button deselectAllButton;
  private Button verboseCheckBox;
  private Button stubbingCheckBox;
  private Button singleServerCheckBox;

  public void initializeFrom(ILaunchConfiguration launchConfig)
  {
    try
    {
      String wfProject = launchConfig.getAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PROJECT, "");
      project = WorkflowProjectManager.getInstance().getWorkflowProject(wfProject);
      if (project != null)
      {
        workflowProjectCombo.setText(project.getName());

        fillWorkflowPackageCombo();
        String wfPackage = launchConfig.getAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PACKAGE, "");
        if (!wfPackage.isEmpty())
        {
          workflowPackage = project.getPackage(wfPackage);
          if (workflowPackage != null)
            workflowPackageCombo.setText(workflowPackage.getName());
        }

        String resultsPath = launchConfig.getAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.RESULTS_PATH, WorkflowProject.DEFAULT_TEST_RESULTS_PATH);
        resultsPathText.setText(resultsPath);
      }

      legacyLaunch = launchConfig.getAttribute(AutomatedTestLaunchConfiguration.IS_LEGACY_LAUNCH, false);

      boolean isVerbose = launchConfig.getAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.VERBOSE, true);
      verboseCheckBox.setSelection(isVerbose);

      boolean isStubbing = launchConfig.getAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.STUBBING, false);
      stubbingCheckBox.setSelection(isStubbing);

      boolean singleServer = launchConfig.getAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.SINGLE_SERVER, false);
      singleServerCheckBox.setSelection(singleServer);

      List<String> cases = launchConfig.getAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.TEST_CASES, new ArrayList<String>());
      testCases = cases.toArray(new String[0]);

      refreshTestCasesTable();
    }
    catch (CoreException ex)
    {
      PluginMessages.uiError(ex, "Launch Init", project);
    }
  }

  protected void refreshTestCasesTable()
  {
    testCasesTableViewer.setInput(getAllTestCases());
    testCasesTableViewer.refresh();
  }

  /**
   * @return all project test cases to be displayed (or empty array if none)
   */
  protected String[] getAllTestCases()
  {
    List<String> displayedCases = null;
    if (project != null)
    {
      if (isLegacyLaunch())
      {
        displayedCases = project.getLegacyTestSuite().getTestCaseStringList();
      }
      else
      {
        if (workflowPackage != null)
          displayedCases = workflowPackage.getTestCaseStringList();
        else
          displayedCases = project.getTestCaseStringList();
      }
    }

    return displayedCases == null ? new String[0] : displayedCases.toArray(new String[0]);
  }

  protected abstract String getTestType();

  protected String getAttrPrefix()
  {
    return getTestType() + "_";
  }

  public void performApply(ILaunchConfigurationWorkingCopy launchConfig)
  {
    launchConfig.setAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PROJECT, workflowProjectCombo.getText());
    String pkg = workflowPackageCombo.getText();
    if (pkg.isEmpty())
      launchConfig.removeAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PACKAGE);
    else
      launchConfig.setAttribute(AutomatedTestLaunchConfiguration.WORKFLOW_PACKAGE, pkg);
    launchConfig.setAttribute(AutomatedTestLaunchConfiguration.IS_LEGACY_LAUNCH, isLegacyLaunch());
    launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.RESULTS_PATH, resultsPathText.getText());
    launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.VERBOSE, verboseCheckBox.getSelection());
    launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.STUBBING, stubbingCheckBox.getSelection());
    launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.SINGLE_SERVER, singleServerCheckBox.getSelection());
    List<String> cases = new ArrayList<String>();
    for (String testCase : testCases)
      cases.add(testCase);
    launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.TEST_CASES, cases);
  }

  protected void createWorkflowProjectSection(Composite parent)
  {
    List<WorkflowProject> workflowProjects = WorkflowProjectManager.getInstance().getWorkflowProjects();
    if (workflowProjects == null || workflowProjects.size() == 0)
      MessageDialog.openError(parent.getShell(), "Error", "No MDW Workflow projects found");

    new Label(parent, SWT.NONE).setText("Workflow Project");
    workflowProjectCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.verticalIndent = 3;
    workflowProjectCombo.setLayoutData(gd);
    workflowProjectCombo.removeAll();
    for (WorkflowProject project : workflowProjects)
    {
      workflowProjectCombo.add(project.getName());
    }
    workflowProjectCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        project = WorkflowProjectManager.getInstance().getWorkflowProject(workflowProjectCombo.getText());
        workflowPackage = null;
        fillWorkflowPackageCombo();
        resultsPathText.setText(project.getTestResultsPath(getAttrPrefix()));
        refreshTestCasesTable();
        setDirty(true);
        validatePage();
      }
    });
  }

  protected void createWorkflowPackageSection(Composite parent)
  {
    new Label(parent, SWT.NONE).setText("Workflow Package");
    workflowPackageCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 250;
    gd.verticalIndent = 3;
    workflowPackageCombo.setLayoutData(gd);
    workflowPackageCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        workflowPackage = project.getPackage(workflowPackageCombo.getText());
        legacyLaunch = false;
        refreshTestCasesTable();
        setDirty(true);
        validatePage();
      }
    });
  }

  private void fillWorkflowPackageCombo()
  {
    workflowPackageCombo.removeAll();
    if (project != null)
    {
      for (WorkflowPackage pkg : project.getTopLevelPackages())
        workflowPackageCombo.add(pkg.getName());
    }
  }

  protected Composite createLocationsSection(Composite parent)
  {
    Group locationsGroup = new Group(parent, SWT.NONE);
    locationsGroup.setText("Output");
    GridLayout gl = new GridLayout();
    gl.numColumns = 3;
    locationsGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    locationsGroup.setLayoutData(gd);

    new Label(locationsGroup, SWT.NONE).setText("Test Results ");
    resultsPathText = new Text(locationsGroup, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
    resultsPathText.setLayoutData(gd);
    resultsPathText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });

    browseResultsPathButton = new Button(locationsGroup, SWT.PUSH);
    browseResultsPathButton.setText("Browse...");
    browseResultsPathButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        FolderTreeDialog dlg = new FolderTreeDialog(getShell());
        dlg.setInput(project.getSourceProject());
        int res = dlg.open();
        if (res == Dialog.OK)
        {
          IFolder folder = (IFolder) dlg.getFirstResult();
          resultsPathText.setText(folder == null ? "" : folder.getFullPath().toString().substring(project.getSourceProjectName().length() + 2));
        }
      }
    });

    return locationsGroup;
  }

  protected Composite createServerSection(Composite parent)
  {
    Composite buttonComposite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 4;
    layout.horizontalSpacing = 5;
    buttonComposite.setLayout(layout);
    GridData gd1 = new GridData(SWT.BEGINNING, SWT.TOP, true, false);
    gd1.horizontalSpan = 2;
    buttonComposite.setLayoutData(gd1);

    verboseCheckBox = new Button(buttonComposite, SWT.CHECK);
    verboseCheckBox.setText("Verbose");
    verboseCheckBox.setLocation(120,200);
    verboseCheckBox.pack();
    verboseCheckBox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });

    stubbingCheckBox = new Button(buttonComposite, SWT.CHECK);
    stubbingCheckBox.setText("Stubbing");
    stubbingCheckBox.setLocation(120,200);
    stubbingCheckBox.pack();
    stubbingCheckBox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });

    singleServerCheckBox = new Button(buttonComposite, SWT.CHECK);
    singleServerCheckBox.setText("Pin to Server");
    singleServerCheckBox.setLocation(120,200);
    singleServerCheckBox.pack();
    singleServerCheckBox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setDirty(true);
        validatePage();
      }
    });

    return buttonComposite;
  }

  protected void createTestCasesSection(Composite parent)
  {
    createColumnSpecs();
    createTable(parent);
    createTableViewer();
    createSelectButtons(parent);
  }

  protected void createSelectButtons(Composite parent)
  {
    Composite buttonComposite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.horizontalSpacing = 5;
    buttonComposite.setLayout(layout);
    GridData gd = new GridData(SWT.BEGINNING, SWT.TOP, true, false);
    gd.horizontalSpan = 2;
    buttonComposite.setLayoutData(gd);

    selectAllButton = new Button(buttonComposite, SWT.PUSH);
    selectAllButton.setText("Select All");
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 75;
    selectAllButton.setLayoutData(gd);
    selectAllButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        testCases = getAllTestCases();
        testCasesTableViewer.refresh();
        setDirty(true);
        validatePage();
      }
    });

    deselectAllButton = new Button(buttonComposite, SWT.PUSH);
    deselectAllButton.setText("Deselect All");
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 70;
    deselectAllButton.setLayoutData(gd);
    deselectAllButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        testCases = new String[0];
        testCasesTableViewer.refresh();
        setDirty(true);
        validatePage();
      }
    });
  }

  protected void createColumnSpecs()
  {
    testCasesColumnSpecs = new ArrayList<ColumnSpec>();

    ColumnSpec selectionColSpec = new ColumnSpec(PropertyEditor.TYPE_CHECKBOX, "", "run");
    selectionColSpec.width = 28;
    testCasesColumnSpecs.add(selectionColSpec);

    ColumnSpec testCaseColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Test Case", "testCase");
    testCaseColSpec.width = 420;
    testCaseColSpec.readOnly = true;
    testCasesColumnSpecs.add(testCaseColSpec);

    testCasesColumnProps = new String[testCasesColumnSpecs.size()];
    for (int i = 0; i < testCasesColumnSpecs.size(); i++)
      testCasesColumnProps[i] = testCasesColumnSpecs.get(i).property;
  }

  protected void createTable(Composite parent)
  {
    int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION;

    testCasesTable = new Table(parent, style);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 2;
    gridData.verticalIndent = 3;
    testCasesTable.setLayoutData(gridData);
    testCasesTable.setLinesVisible(true);
    testCasesTable.setHeaderVisible(true);

    for (int i = 0; i < testCasesColumnSpecs.size(); i++)
    {
      ColumnSpec colSpec = testCasesColumnSpecs.get(i);
      int styles = SWT.LEFT;
      if (colSpec.readOnly)
        style = style | SWT.READ_ONLY;
      TableColumn column = new TableColumn(testCasesTable, styles, i);
      column.setText(colSpec.label);
      column.setWidth(colSpec.width);
      column.setResizable(colSpec.resizable);
    }
  }

  protected void createTableViewer()
  {
    testCasesTableViewer = new TableViewer(testCasesTable);
    testCasesTableViewer.setUseHashlookup(true);

    testCasesTableViewer.setColumnProperties(testCasesColumnProps);

    CellEditor[] editors = new CellEditor[testCasesColumnSpecs.size()];
    for (int i = 0; i < testCasesColumnSpecs.size(); i++)
    {
      ColumnSpec colSpec = testCasesColumnSpecs.get(i);
      CellEditor cellEditor = null;
      if (colSpec.type.equals(PropertyEditor.TYPE_TEXT))
      {
        cellEditor = new TextCellEditor(testCasesTable);
      }
      else if (colSpec.type.equals(PropertyEditor.TYPE_CHECKBOX))
      {
        cellEditor = new CheckboxCellEditor(testCasesTable);
      }
      editors[i] = cellEditor;
    }
    testCasesTableViewer.setCellEditors(editors);
    testCasesTableViewer.setCellModifier(new TestCaseCellModifier());
    testCasesTableViewer.setLabelProvider(new TestCaseLabelProvider());
    testCasesTableViewer.setContentProvider(new TestCaseContentProvider());
  }

  class TestCaseCellModifier implements ICellModifier
  {
    public boolean canModify(Object element, String property)
    {
      ColumnSpec colSpec = testCasesColumnSpecs.get(getColumnIndex(property));
      return !colSpec.readOnly;
    }

    public Object getValue(Object element, String property)
    {
      String testCasePath = (String) element;
      int colIndex = getColumnIndex(property);
      String colType = testCasesColumnSpecs.get(colIndex).type;
      if (colType.equals(PropertyEditor.TYPE_CHECKBOX))
      {
        boolean selected = false;
        for (String testCase : testCases)
        {
          if (testCase.equals(testCasePath))
            selected = true;
        }
        return new Boolean(selected);
      }
      else
      {
        return testCasePath;
      }
    }

    public void modify(Object element, String property, Object value)
    {
      TableItem item = (TableItem) element;
      String testCasePath = (String) item.getData();
      int colIndex = getColumnIndex(property);
      String colType = testCasesColumnSpecs.get(colIndex).type;
      if (colType.equals(PropertyEditor.TYPE_CHECKBOX))
      {
        boolean previouslySelected = false;
        for (String testCase : testCases)
        {
          if (testCase.equals(testCasePath))
          {
            previouslySelected = true;
            break;
          }
        }
        if (!previouslySelected)
        {
          String[] newTestCases = new String[testCases.length + 1];
          for (int i = 0; i < testCases.length; i++)
          {
            newTestCases[i] = testCases[i];
          }
          newTestCases[testCases.length] = testCasePath;
          testCases = newTestCases;
          Arrays.sort(testCases);
        }
        else
        {
          String[] newTestCases = new String[testCases.length - 1];
          int idx = 0;
          for (String testCase : testCases)
          {
            if (!testCase.equals(testCasePath))
            {
              newTestCases[idx] = testCase;
              idx++;
            }
          }
          testCases = newTestCases;
        }
        testCasesTableViewer.update(testCasePath, null);
        setDirty(true);
        validatePage();
      }
    }

    protected int getColumnIndex(String property)
    {
      for (int i = 0; i < testCasesColumnSpecs.size(); i++)
      {
        ColumnSpec colSpec = testCasesColumnSpecs.get(i);
        if (colSpec.property.equals(property))
        {
          return i;
        }
      }
      return -1;
    }
  }

  class TestCaseLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    private Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

    public Image getColumnImage(Object element, int columnIndex)
    {
      String testCasePath = (String) element;
      ColumnSpec colspec = testCasesColumnSpecs.get(columnIndex);
      if (colspec.type.equals(PropertyEditor.TYPE_CHECKBOX))
      {
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/unchecked.gif");
        for (String testCase : testCases)
        {
          if (testCase.equals(testCasePath))
          {
            descriptor = MdwPlugin.getImageDescriptor("icons/checked.gif");
            break;
          }
        }
        Image image = (Image) imageCache.get(descriptor);
        if (image == null)
        {
          image = descriptor.createImage();
          imageCache.put(descriptor, image);
        }
        return image;
      }
      else
      {
        return null;
      }
    }

    public String getColumnText(Object element, int columnIndex)
    {
      String testCasePath = (String) element;
      ColumnSpec colspec = testCasesColumnSpecs.get(columnIndex);
      if (colspec.type.equals(PropertyEditor.TYPE_TEXT))
      {
        return testCasePath;
      }
      else
      {
        return null;
      }
    }
  }

  class TestCaseContentProvider implements IStructuredContentProvider
  {
    public Object[] getElements(Object inputElement)
    {
      return (String[]) inputElement;
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  @Override
  public boolean canSave()
  {
    return getErrorMessage() == null;
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig)
  {
    return canSave();
  }

  protected void validatePage()
  {
    setErrorMessage(null);
    setMessage(null);

    if (project == null)
    {
      setErrorMessage("Please select a Workflow Project");
      updateLaunchConfigurationDialog();
      return;
    }
    String resultsPath = resultsPathText.getText();
    if (resultsPath.isEmpty() || project.getProjectFile(resultsPath).exists())
    {
      setErrorMessage("Please enter a valid Test Results path");
      updateLaunchConfigurationDialog();
      return;
    }

    updateLaunchConfigurationDialog();
  }

}
