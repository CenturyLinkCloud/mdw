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
package com.centurylink.mdw.plugin.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class SearchPage extends DialogPage implements ISearchPage {
    private ISearchPageContainer searchPageContainer;

    public ISearchPageContainer getContainer() {
        return searchPageContainer;
    }

    public void setContainer(ISearchPageContainer container) {
        searchPageContainer = container;
    }

    private Text searchPatternText;
    private Button caseSensitiveCheckbox;
    protected Button searchByNameButton;
    private Button searchByIdButton;

    private String searchPattern;

    public String getSearchPattern() {
        return searchPattern;
    }

    private boolean caseSensitive;

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    private SearchQuery.SearchType searchType = SearchQuery.SearchType.ENTITY_BY_NAME;

    public SearchQuery.SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchQuery.SearchType type) {
        this.searchType = type;
    }

    private List<WorkflowProject> scopedProjects;

    public List<WorkflowProject> getScopedProjects() {
        return scopedProjects;
    }

    public void setScopedProjects(List<WorkflowProject> projects) {
        this.scopedProjects = projects;
    }

    private WorkflowPackage selectedPackage;

    public WorkflowPackage getSelectedPackage() {
        return selectedPackage;
    }

    public void setSelectedPackage(WorkflowPackage selPkg) {
        this.selectedPackage = selPkg;
    }

    public abstract String getEntityTitle();

    public abstract String getSearchPatternLabel();

    public abstract SearchQuery createSearchQuery();

    @SuppressWarnings("restriction")
    public void createControl(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this page
        GridLayout gl = new GridLayout();
        gl.numColumns = 3;
        composite.setLayout(gl);

        createSearchPatternControls(composite);
        createSearchTypeControls(composite);

        setControl(composite);

        if (searchPageContainer instanceof org.eclipse.search.internal.ui.SearchDialog) {
            org.eclipse.search.internal.ui.SearchDialog searchDialog = (org.eclipse.search.internal.ui.SearchDialog) searchPageContainer;
            searchDialog.addPageChangedListener(new IPageChangedListener() {
                public void pageChanged(PageChangedEvent event) {
                    checkEnablement();
                }
            });
        }

        checkEnablement();

        searchPatternText.forceFocus();
    }

    protected void createSearchPatternControls(Composite parent) {
        // search pattern
        Label label = new Label(parent, SWT.NONE);
        label.setText(getSearchPatternLabel());
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 3;
        label.setLayoutData(gd);

        searchPatternText = new Text(parent, SWT.SINGLE | SWT.BORDER);
        searchPatternText
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        searchPatternText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                searchPattern = searchPatternText.getText().trim();
                checkEnablement();
            }
        });

        // case sensitive
        caseSensitiveCheckbox = new Button(parent, SWT.CHECK);
        caseSensitiveCheckbox.setText("&Case Sensitive");
        caseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                caseSensitive = caseSensitiveCheckbox.getSelection();
            }
        });
        caseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    }

    protected Group createSearchTypeControls(Composite parent) {
        Group radioGroup = new Group(parent, SWT.NONE);
        radioGroup.setText("Search For");
        GridLayout gl = new GridLayout();
        gl.numColumns = 4;
        radioGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 3;
        radioGroup.setLayoutData(gd);

        // search by name
        searchByNameButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        searchByNameButton.setText(getEntityTitle() + " by Name");
        searchByNameButton.setSelection(true);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 225;
        searchByNameButton.setLayoutData(gd);
        searchByNameButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (searchByNameButton.getSelection()) {
                    searchType = SearchQuery.SearchType.ENTITY_BY_NAME;
                    checkEnablement();
                }
            }
        });

        // search by id
        searchByIdButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        searchByIdButton.setText(getEntityTitle() + " by ID");
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        searchByIdButton.setLayoutData(gd);
        searchByIdButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (searchByIdButton.getSelection()) {
                    searchType = SearchQuery.SearchType.ENTITY_BY_ID;
                    checkEnablement();
                }
            }
        });

        return radioGroup;
    }

    @Override
    public String getTitle() {
        return getEntityTitle() + " Search";
    }

    public boolean performAction() {
        determineScope();
        return search(scopedProjects);
    }

    protected void determineScope() {
        if (searchPattern == null)
            searchPattern = "";

        if (scopedProjects == null) {
            // determine scoped projects from selection
            scopedProjects = new ArrayList<WorkflowProject>();

            if (searchPageContainer
                    .getSelectedScope() == ISearchPageContainer.SELECTED_PROJECTS_SCOPE) {
                for (String projectName : searchPageContainer.getSelectedProjectNames()) {
                    IProject project = MdwPlugin.getWorkspaceRoot().getProject(projectName);
                    WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                            .getWorkflowProject(project);
                    if (workflowProject != null && !scopedProjects.contains(workflowProject))
                        scopedProjects.add(workflowProject);
                }
            }
            else if (searchPageContainer
                    .getSelectedScope() == ISearchPageContainer.SELECTION_SCOPE) {
                if (searchPageContainer.getSelection() instanceof WorkflowProject)
                    scopedProjects.add((WorkflowProject) searchPageContainer.getSelection());
                if (searchPageContainer.getSelection() instanceof WorkflowPackage)
                    selectedPackage = (WorkflowPackage) searchPageContainer.getSelection();
            }
            else if (searchPageContainer
                    .getSelectedScope() == ISearchPageContainer.WORKING_SET_SCOPE) {
                for (IWorkingSet workingSet : searchPageContainer.getSelectedWorkingSets()) {
                    for (IAdaptable element : workingSet.getElements()) {
                        if (element instanceof IProject || element instanceof IJavaProject) {
                            IProject project = element instanceof IJavaProject
                                    ? ((IJavaProject) element).getProject() : (IProject) element;
                            WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                                    .getWorkflowProject(project);
                            if (workflowProject != null
                                    && !scopedProjects.contains(workflowProject))
                                scopedProjects.add(workflowProject);
                        }
                    }
                }
            }
            else {
                scopedProjects.addAll(WorkflowProjectManager.getInstance().getWorkflowProjects());
            }
        }
    }

    public boolean search(List<WorkflowProject> scopedProjects) {
        try {
            SearchQuery searchQuery = createSearchQuery();

            ProgressMonitorDialog context = new MdwProgressMonitorDialog(getShell());
            NewSearchUI.runQueryInForeground(context, searchQuery);

            // this shouldn't be necessary according to the Eclipse API docs
            NewSearchUI.activateSearchResultView();
            ISearchResultViewPart part = NewSearchUI.getSearchResultView();
            part.updateLabel();
            SearchResultsPage page = (SearchResultsPage) part.getActivePage();
            page.setSearchQuery(searchQuery);
            page.setInput(searchQuery.getSearchResult(), null);
            return true;
        }
        catch (OperationCanceledException ex) {
            setMessage("Search cancelled", IMessageProvider.INFORMATION);
            return false;
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, getEntityTitle() + " Search");
            return false;
        }
    }

    protected void checkEnablement() {
        if (searchPattern == null || searchPattern.length() == 0) {
            setEnabled(false);
        }
        else if (searchType.equals(SearchQuery.SearchType.ENTITY_BY_ID)
                || searchType.equals(SearchQuery.SearchType.INSTANCE_BY_ID)
                || searchType.equals(SearchQuery.SearchType.INSTANCE_BY_ENTITY_ID)) {
            try {
                Long.parseLong(searchPattern);
                setEnabled(true);
            }
            catch (NumberFormatException ex) {
                setMessage("Invalid ID");
                setEnabled(false);
            }
        }
        else if (searchType.equals(SearchQuery.SearchType.INSTANCE_BY_MRI)
                && searchPattern.equals("*")) {
            setEnabled(false);
        }
        else {
            setEnabled(true);
        }
    }

    protected void setEnabled(boolean enabled) {
        searchPageContainer.setPerformActionEnabled(enabled);
    }
}
