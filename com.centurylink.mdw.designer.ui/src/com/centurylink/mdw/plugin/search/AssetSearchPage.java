/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class AssetSearchPage extends SearchPage {
    private Combo resourceTypeCombo;
    private Button containingTextButton;
    private Text containedTextText;

    private String resourceType;
    private String containedText;

    public String getEntityTitle() {
        return "Asset";
    }

    public String getSearchPatternLabel() {
        return "Asset Search Pattern (* = All):";
    }

    public SearchQuery createSearchQuery() {
        AssetSearchQuery query = new AssetSearchQuery(getScopedProjects(), getSearchType(),
                getSearchPattern(), isCaseSensitive(), getShell());
        query.setResourceType(resourceType);
        query.setContainedText(containedText);
        query.setSelectedPackage(getSelectedPackage());
        return query;
    }

    @Override
    protected void createSearchPatternControls(Composite parent) {
        super.createSearchPatternControls(parent);

        Label label = new Label(parent, SWT.NONE);
        label.setText("Asset Type: ");
        label.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));

        resourceTypeCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        resourceTypeCombo
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        resourceTypeCombo.removeAll();
        List<String> types = new ArrayList<String>();
        types.addAll(WorkflowAsset.getResourceTypes());
        types.add("External Event Handler");
        types.add(RuleSetVO.IMAGE_GIF);
        types.add(RuleSetVO.IMAGE_JPEG);
        types.add(RuleSetVO.IMAGE_PNG);
        Collections.sort(types, new Comparator<String>() {
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        for (String resourceType : types)
            resourceTypeCombo.add(resourceType);
        resourceTypeCombo.select(0);
        resourceType = resourceTypeCombo.getText();

        resourceTypeCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                resourceType = resourceTypeCombo.getText();
            }
        });
    }

    @Override
    protected Group createSearchTypeControls(Composite parent) {
        Group radioGroup = super.createSearchTypeControls(parent);

        new Label(radioGroup, SWT.NONE); // spacer
        new Label(radioGroup, SWT.NONE); // spacer

        // search by name, containing text
        containingTextButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        containingTextButton.setText(getEntityTitle() + " by Name, Containing Text: ");
        containingTextButton.setSelection(false);
        containingTextButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (containingTextButton.getSelection()) {
                    setSearchType(SearchQuery.SearchType.CONTAINING_TEXT);
                    if (containedText == null || containedText.length() == 0)
                        setEnabled(false);
                    else
                        checkEnablement();
                    containedTextText.setEnabled(true);
                }
                else {
                    containedTextText.setText("");
                    containedTextText.setEnabled(false);
                    checkEnablement();
                }
            }
        });

        containedTextText = new Text(radioGroup, SWT.SINGLE | SWT.BORDER);
        containedTextText
                .setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        containedTextText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                containedText = containedTextText.getText().trim();
                if (containedText.length() == 0 && containingTextButton.getSelection())
                    setEnabled(false);
                else
                    checkEnablement();
            }
        });
        containedTextText.setEnabled(false);

        return radioGroup;
    }

    @Override
    protected void checkEnablement() {
        if (getSearchType().equals(SearchQuery.SearchType.CONTAINING_TEXT)) {
            if (containedTextText.getText().trim().length() == 0)
                setEnabled(false);
            else
                super.checkEnablement();
        }
        else {
            super.checkEnablement();
        }
    }

}
