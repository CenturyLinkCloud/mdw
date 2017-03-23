/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.TableModelUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ImportProjectSelectPage extends WizardPage {
    private TableEditor projectTableEditor;
    private Button selectAllButton;
    private Button deselectAllButton;

    public ImportProjectSelectPage() {
        setTitle("Select Projects to Import");
        setDescription("Choose which projects to import into your workspace.");
    }

    /**
     * Populate the table based on the imported data.
     */
    public void initialize() {
        List<DefaultRowImpl> tableRows = new ArrayList<DefaultRowImpl>();
        List<WorkflowProject> projectsToImport = new ArrayList<WorkflowProject>();
        for (WorkflowProject workflowProject : getProjectList()) {
            IProject existing = MdwPlugin.getWorkspaceRoot().getProject(workflowProject.getName());
            String[] rowData = new String[3];
            rowData[0] = "true";
            rowData[1] = workflowProject.getName();
            rowData[2] = "";
            if (existing != null && existing.exists()) {
                rowData[0] = "false";
                if (existing.isOpen())
                    rowData[2] = "Already exists in workspace";
                else
                    rowData[2] = "Closed project exists in workspace";
            }
            else if (workflowProject.getMdwDataSource().getJdbcUrl() == null) {
                rowData[0] = "false";
                rowData[2] = "Missing JDBC URL";
            }
            else {
                projectsToImport.add(workflowProject);
            }
            DefaultRowImpl row = projectTableEditor.new DefaultRowImpl(rowData);
            tableRows.add(row);
        }

        projectTableEditor.setValue(tableRows);
        setProjectsToImport(projectsToImport);
        handleFieldChanged();
    }

    @Override
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 4;
        gl.numColumns = ncol;
        composite.setLayout(gl);

        createProjectListControls(composite, ncol);
        createSpacer(composite, ncol);
        createSelectButtonControls(composite, ncol);

        setControl(composite);
    }

    private void createProjectListControls(Composite parent, int ncol) {
        projectTableEditor = new TableEditor(null, TableEditor.TYPE_TABLE);

        // colspecs
        List<ColumnSpec> projectColSpecs = new ArrayList<ColumnSpec>();
        ColumnSpec selectionColSpec = new ColumnSpec(PropertyEditor.TYPE_CHECKBOX,
                "Import/Overwrite", "import");
        selectionColSpec.width = 100;
        projectColSpecs.add(selectionColSpec);
        ColumnSpec projectColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Project", "project");
        projectColSpec.width = 250;
        projectColSpec.readOnly = true;
        projectColSpecs.add(projectColSpec);
        ColumnSpec noteColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Note", "note");
        noteColSpec.readOnly = true;
        noteColSpec.width = 150;
        projectColSpecs.add(noteColSpec);

        projectTableEditor.setColumnSpecs(projectColSpecs);
        projectTableEditor.setFillWidth(true);
        projectTableEditor.setModelUpdater(new TableModelUpdater() {
            public Object create() {
                return null;
            }

            @SuppressWarnings("rawtypes")
            public void updateModelValue(List tableValue) {
                List<WorkflowProject> selectedProjects = new ArrayList<WorkflowProject>();
                for (WorkflowProject workflowProject : getProjectList()) {
                    for (Object rowObj : tableValue) {
                        DefaultRowImpl row = (DefaultRowImpl) rowObj;
                        if (workflowProject.getName().equals(row.getColumnValue(1))) {
                            if (((Boolean) row.getColumnValue(0)).booleanValue())
                                selectedProjects.add(workflowProject);
                        }
                    }
                }
                setProjectsToImport(selectedProjects);
                handleFieldChanged();
            }
        });

        projectTableEditor.render(parent, false);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = ncol;
        projectTableEditor.getTable().setLayoutData(gd);
    }

    private void createSelectButtonControls(Composite parent, int ncol) {
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
        gd.widthHint = 75;
        selectAllButton.setLayoutData(gd);
        selectAllButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                for (Object rowObj : projectTableEditor.getTableValue()) {
                    DefaultRowImpl row = (DefaultRowImpl) rowObj;
                    row.setColumnValue(0, "true");
                }
                setProjectsToImport(getProjectList());
                projectTableEditor.getTableViewer().update(projectTableEditor.getTableValue(),
                        null);
                handleFieldChanged();
            }
        });

        deselectAllButton = new Button(buttonComposite, SWT.PUSH);
        deselectAllButton.setText("Deselect All");
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 70;
        deselectAllButton.setLayoutData(gd);
        deselectAllButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                for (Object rowObj : projectTableEditor.getTableValue()) {
                    DefaultRowImpl row = (DefaultRowImpl) rowObj;
                    row.setColumnValue(0, "false");
                }
                setProjectsToImport(new ArrayList<WorkflowProject>());
                projectTableEditor.getTableViewer().update(projectTableEditor.getTableValue(),
                        null);
                handleFieldChanged();
            }
        });
    }

    @Override
    public boolean isPageComplete() {
        return isPageValid();
    }

    boolean isPageValid() {
        return getProjectsToImport() != null && getProjectsToImport().size() > 0;
    }

    public IStatus[] getStatuses() {
        return null;
    }

    private List<WorkflowProject> getProjectsToImport() {
        return ((ImportProjectWizard) getWizard()).getProjectsToImport();
    }

    private void setProjectsToImport(List<WorkflowProject> projects) {
        ((ImportProjectWizard) getWizard()).setProjectsToImport(projects);
    }

    private List<WorkflowProject> getProjectList() {
        return ((ImportProjectWizard) getWizard()).getProjectList();
    }
}