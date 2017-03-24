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
package com.centurylink.mdw.plugin.project;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;

import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModule;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModulesTable;

public class ExtensionModulesWizardPage extends WizardPage implements IFacetWizardPage {
    public static final String PAGE_TITLE = "MDW Extension Modules";

    private ExtensionModulesTable extensionsTable;

    /**
     * Constructor.
     */
    public ExtensionModulesWizardPage() {
        setTitle(PAGE_TITLE);
        setDescription("Optional add-ins that extend MDW with extra capabilities");
    }

    public void initValues() {
        extensionsTable.setSelectedModules(new ArrayList<ExtensionModule>());
    }

    /**
     * draw the widgets using a grid layout
     * 
     * @param parent
     *            - the parent composite
     */
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 4;
        gl.numColumns = ncol;
        composite.setLayout(gl);

        extensionsTable = new ExtensionModulesTable(getProject());
        extensionsTable.create();
        extensionsTable.getTableEditor().render(composite, false);
        extensionsTable.setSelectedModules(new ArrayList<ExtensionModule>());
        extensionsTable.getTableEditor().addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                getProject().setExtensionModules(extensionsTable.getSelectedModules());
            }
        });

        setControl(composite);
    }

    /**
     * no wrong answer
     */
    public IStatus[] getStatuses() {
        return null;
    }

    /**
     * nothing is required
     */
    public boolean isPageComplete() {
        return true;
    }

    public void setWizardContext(IWizardContext context) {
    }

    @Override
    public IWizardPage getNextPage() {
        return null;
    }

    public void transferStateToConfig() {
    }
}
