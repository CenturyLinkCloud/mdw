/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.dialogs.FileSaveDialog;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.TableModelUpdater;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ExportProjectPage extends WizardPage
{
  private TableEditor projectTableEditor;

  private Text fileNameTextField;
  private Button browseExportFileButton;
  private Button selectAllButton;
  private Button deselectAllButton;

  private String fileName;
  public String getFileName() {return fileName;}

  public ExportProjectPage()
  {
    setTitle("Export Project(s)");
    setDescription("Export one or more MDW projects to file.");
  }

  public void init(IStructuredSelection selection)
  {
    super.init(selection);
  }

  @Override
  public void drawWidgets(Composite parent)
  {
    // create the composite to hold the widgets
    Composite composite = new Composite(parent, SWT.NULL);

    // create the layout for this wizard page
    GridLayout gl = new GridLayout();
    int ncol = 4;
    gl.numColumns = ncol;
    composite.setLayout(gl);

    createProjectListControls(composite, ncol);
    createSelectButtonControls(composite, ncol);
    createSpacer(composite, ncol);
    createExportFileControls(composite, ncol);

    setControl(composite);

    fileNameTextField.forceFocus();
  }

  private void createProjectListControls(Composite parent, int ncol)
  {
    Label label = new Label(parent, SWT.NONE);
    label.setText("The following projects will be exported:");
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol;
    label.setLayoutData(gd);

    projectTableEditor = new TableEditor(null, TableEditor.TYPE_TABLE);

    // colspecs
    List<ColumnSpec> projectColSpecs = new ArrayList<ColumnSpec>();
    ColumnSpec selectionColSpec = new ColumnSpec(PropertyEditor.TYPE_CHECKBOX, "Export", "export");
    selectionColSpec.width = 45;
    projectColSpecs.add(selectionColSpec);
    ColumnSpec projectColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Project", "project");
    projectColSpec.width = 450;
    projectColSpec.readOnly = true;
    projectColSpecs.add(projectColSpec);

    projectTableEditor.setColumnSpecs(projectColSpecs);
    projectTableEditor.setModelUpdater(new TableModelUpdater()
    {
      public Object create()
      {
        return null;
      }

      @SuppressWarnings("rawtypes")
      public void updateModelValue(List tableValue)
      {
        List<WorkflowProject> selectedProjects = new ArrayList<WorkflowProject>();
        List<WorkflowProject> allProjects = WorkflowProjectManager.getInstance().getRemoteWorkflowProjects();
        for (WorkflowProject workflowProject : allProjects)
        {
          for (Object rowObj : tableValue)
          {
            DefaultRowImpl row = (DefaultRowImpl) rowObj;
            if (workflowProject.getName().equals(row.getColumnValue(1)))
            {
              if (((Boolean)row.getColumnValue(0)).booleanValue())
                selectedProjects.add(workflowProject);
            }
          }
        }
        setProjectsToExport(selectedProjects);
        handleFieldChanged();
      }
    });

    projectTableEditor.render(parent, false);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = ncol;
    projectTableEditor.getTable().setLayoutData(gd);

    List<DefaultRowImpl> tableRows = new ArrayList<DefaultRowImpl>();
    List<WorkflowProject> remoteProjects = WorkflowProjectManager.getInstance().getRemoteWorkflowProjects();
    for (WorkflowProject remoteProject : remoteProjects)
    {
      String[] rowData = new String[2];
      boolean selected = getProjectsToExport().contains(remoteProject);
      rowData[0] = String.valueOf(selected);
      rowData[1] = remoteProject.getName();
      DefaultRowImpl row = projectTableEditor.new DefaultRowImpl(rowData);
      tableRows.add(row);
    }
    projectTableEditor.setValue(tableRows);
  }

  private void createSelectButtonControls(Composite parent, int ncol)
  {
    Composite buttonComposite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.horizontalSpacing = 5;
    buttonComposite.setLayout(layout);
    GridData gd = new GridData(SWT.END, SWT.TOP, true, false);
    gd.horizontalSpan = ncol;
    buttonComposite.setLayoutData(gd);

    selectAllButton = new Button(buttonComposite, SWT.PUSH);
    selectAllButton.setText("Select All");
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 70;
    selectAllButton.setLayoutData(gd);
    selectAllButton.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          for (Object rowObj : projectTableEditor.getTableValue())
          {
            DefaultRowImpl row = (DefaultRowImpl) rowObj;
            row.setColumnValue(0, "true");
          }
          setProjectsToExport(WorkflowProjectManager.getInstance().getRemoteWorkflowProjects());
          projectTableEditor.getTableViewer().update(projectTableEditor.getTableValue(), null);
          handleFieldChanged();
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
          for (Object rowObj : projectTableEditor.getTableValue())
          {
            DefaultRowImpl row = (DefaultRowImpl) rowObj;
            row.setColumnValue(0, "false");
          }
          setProjectsToExport(new ArrayList<WorkflowProject>());
          projectTableEditor.getTableViewer().update(projectTableEditor.getTableValue(), null);
          handleFieldChanged();
        }
      });
  }

  private void createExportFileControls(Composite parent, int ncol)
  {
    Label label = new Label(parent, SWT.NONE);
    label.setText("Export XML File:");
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol;
    label.setLayoutData(gd);

    fileNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = ncol - 1;
    fileNameTextField.setLayoutData(gd);
    fileNameTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          fileName = fileNameTextField.getText().trim();
          handleFieldChanged();
        }
      });

    browseExportFileButton = new Button(parent, SWT.PUSH);
    browseExportFileButton.setText("Browse...");
    browseExportFileButton.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          String[] filters = new String[1];
          filters[0] = "*.xml";
          FileSaveDialog dlg = new FileSaveDialog(getShell());
          dlg.setFilterExtensions(filters);
          dlg.setFileName(createFileName());
          fileNameTextField.setText(dlg.open());
          if (!fileNameTextField.getText().toLowerCase().endsWith(".xml")) {
              fileNameTextField.setText(fileNameTextField.getText() + ".xml");
          }
        }
      });
  }

  @Override
  public boolean isPageComplete()
  {
    return isPageValid();
  }

  boolean isPageValid()
  {
    return (fileNameTextField.getText().trim().length() > 0
        && getProjectsToExport().size() > 0);
  }

  public IStatus[] getStatuses()
  {
    return null;
  }

  private String createFileName()
  {
    return "WorkflowProjectExport.xml";
  }

  public List<WorkflowProject> getProjectsToExport()
  {
    return ((ExportProjectWizard)getWizard()).getProjectsToExport();
  }

  public void setProjectsToExport(List<WorkflowProject> projectsToExport)
  {
    ((ExportProjectWizard)getWizard()).setProjectsToExport(projectsToExport);
  }
}