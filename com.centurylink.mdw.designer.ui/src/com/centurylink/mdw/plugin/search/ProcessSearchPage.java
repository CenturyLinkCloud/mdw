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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;

public class ProcessSearchPage extends SearchPage {
    private Button contActivityButton;
    private Combo activityImplsCombo;
    private Button masterReqIdButton;
    private Button instanceByIdButton;
    private Button instanceByInstanceIdButton;

    private ActivityImplementorVO activityImpl;

    public String getEntityTitle() {
        return "Process";
    }

    public String getSearchPatternLabel() {
        return "Process Search Pattern (* = All):";
    }

    @Override
    protected Group createSearchTypeControls(Composite parent) {
        Group radioGroup = super.createSearchTypeControls(parent);

        // process containing activity impl
        contActivityButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        contActivityButton.setText("Process Containing Activity: ");
        contActivityButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (contActivityButton.getSelection()) {
                    setSearchType(SearchQuery.SearchType.CONTAINING_ENTITY);
                    determineScope();
                    activityImplsCombo.removeAll();
                    activityImplsCombo.add("");
                    if (getScopedProjects() == null || getScopedProjects().size() > 1) {
                        activityImplsCombo.add("Select a project first...");
                        activityImplsCombo.select(1);
                    }
                    else {
                        List<ActivityImplementorVO> impls = getScopedProjects().get(0)
                                .getDesignerProxy().getPluginDataAccess()
                                .getActivityImplementors(false);
                        Collections.sort(impls, new Comparator<ActivityImplementorVO>() {
                            public int compare(ActivityImplementorVO ai1,
                                    ActivityImplementorVO ai2) {
                                if (ai1.getLabel() == null)
                                    return -1;
                                else if (ai2.getLabel() == null)
                                    return 1;
                                else
                                    return ai1.getLabel().compareToIgnoreCase(ai2.getLabel());
                            }
                        });
                        for (ActivityImplementorVO impl : impls) {
                            if (impl.getLabel() != null)
                                activityImplsCombo.add(impl.getLabel());
                        }
                    }

                    activityImplsCombo.setEnabled(true);
                    checkEnablement();
                }
                else {
                    activityImplsCombo.select(0);
                    activityImplsCombo.setEnabled(false);
                }
            }
        });
        activityImplsCombo = new Combo(radioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        activityImplsCombo
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1));
        activityImplsCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                activityImpl = null;
                String label = activityImplsCombo.getText();
                if (label.length() > 0) {
                    for (ActivityImplementorVO impl : getScopedProjects().get(0).getDesignerProxy()
                            .getPluginDataAccess().getActivityImplementors(false)) {
                        if (label.equals(impl.getLabel())) {
                            activityImpl = impl;
                            break;
                        }
                    }
                }
                checkEnablement();
            }
        });
        activityImplsCombo.setEnabled(false);

        // master request id
        masterReqIdButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        masterReqIdButton.setText("Process Instance by Master Request ID");
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 225;
        masterReqIdButton.setLayoutData(gd);
        masterReqIdButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (masterReqIdButton.getSelection()) {
                    setSearchType(SearchQuery.SearchType.INSTANCE_BY_MRI);
                    checkEnablement();
                }
            }
        });
        searchByNameButton.setSelection(false);
        masterReqIdButton.setSelection(true);
        setSearchType(SearchQuery.SearchType.INSTANCE_BY_MRI);

        // instance by process id
        instanceByIdButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        instanceByIdButton.setText("Process Instances by Process ID");
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 200;
        instanceByIdButton.setLayoutData(gd);
        instanceByIdButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (instanceByIdButton.getSelection()) {
                    setSearchType(SearchQuery.SearchType.INSTANCE_BY_ENTITY_ID);
                    checkEnablement();
                }
            }
        });

        // instance by instance id
        instanceByInstanceIdButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        instanceByInstanceIdButton.setText("Process Instance by Instance ID");
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        instanceByInstanceIdButton.setLayoutData(gd);
        instanceByInstanceIdButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (instanceByInstanceIdButton.getSelection()) {
                    setSearchType(SearchQuery.SearchType.INSTANCE_BY_ID);
                    checkEnablement();
                }
            }
        });

        return radioGroup;
    }

    public SearchQuery createSearchQuery() {
        ProcessSearchQuery query = new ProcessSearchQuery(getScopedProjects(), getSearchType(),
                getSearchPattern(), isCaseSensitive(), getShell());
        query.setContainedEntityId(activityImpl == null ? null : activityImpl.getImplementorId());
        query.setContainedEntityName(
                activityImpl == null ? null : activityImpl.getImplementorClassName());
        query.setSelectedPackage(getSelectedPackage());
        return query;
    }

    @Override
    protected void checkEnablement() {
        if (getSearchType().equals(SearchQuery.SearchType.CONTAINING_ENTITY)) {
            String activityImpl = activityImplsCombo.getText();
            if (activityImpl.length() == 0 || activityImpl.equals("Select a project first..."))
                setEnabled(false);
            else
                super.checkEnablement();
        }
        else {
            super.checkEnablement();
        }
    }
}
