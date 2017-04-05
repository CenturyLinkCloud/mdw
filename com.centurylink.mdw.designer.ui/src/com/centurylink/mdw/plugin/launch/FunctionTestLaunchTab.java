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
package com.centurylink.mdw.plugin.launch;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;

public class FunctionTestLaunchTab extends TestSuiteLaunchTab {
    private Spinner threadCountSpinner;
    private Spinner threadIntervalSpinner;
    private Button createReplaceCheckBox;

    private boolean debug;

    private Image image = MdwPlugin.getImageDescriptor("icons/auto_test.gif").createImage();

    public FunctionTestLaunchTab(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
        workingCopy.setAttribute(AutomatedTestLaunchConfiguration.IS_LOAD_TEST, new Boolean(false));
        super.activated(workingCopy);
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);

        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 2;
        composite.setLayout(topLayout);

        createWorkflowProjectSection(composite);
        createWorkflowPackageSection(composite);
        createLocationsSection(composite);
        createThreadsSection(composite);
        createServerSection(composite);
        createTestCasesSection(composite);
    }

    public String getName() {
        return "Function Test";
    }

    public Image getImage() {
        return image;
    }

    public void initializeFrom(ILaunchConfiguration launchConfig) {
        super.initializeFrom(launchConfig);

        try {
            int threadCount = launchConfig.getAttribute(
                    getAttrPrefix() + AutomatedTestLaunchConfiguration.THREAD_COUNT, 5);
            threadCountSpinner.setSelection(threadCount);
            int threadInterval = launchConfig.getAttribute(
                    getAttrPrefix() + AutomatedTestLaunchConfiguration.THREAD_INTERVAL, 2);
            threadIntervalSpinner.setSelection(threadInterval);

            if (getProject().isFilePersist()) {
                boolean createReplace = launchConfig.getAttribute(
                        getAttrPrefix() + AutomatedTestLaunchConfiguration.CREATE_REPLACE_RESULTS,
                        false);
                createReplaceCheckBox.setSelection(createReplace);
            }
            else {
                createReplaceCheckBox.setSelection(false);
                createReplaceCheckBox.setEnabled(false);
            }
        }
        catch (CoreException ex) {
            PluginMessages.uiError(ex, "Launch Init", getProject());
        }
        validatePage();
    }

    public void performApply(ILaunchConfigurationWorkingCopy launchConfig) {
        super.performApply(launchConfig);
        launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.THREAD_COUNT,
                threadCountSpinner.getSelection());
        launchConfig.setAttribute(
                getAttrPrefix() + AutomatedTestLaunchConfiguration.THREAD_INTERVAL,
                threadIntervalSpinner.getSelection());
        launchConfig.setAttribute(
                getAttrPrefix() + AutomatedTestLaunchConfiguration.CREATE_REPLACE_RESULTS,
                createReplaceCheckBox.getSelection());
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy launchConfig) {
        launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.THREAD_COUNT,
                5);
        launchConfig.setAttribute(
                getAttrPrefix() + AutomatedTestLaunchConfiguration.THREAD_INTERVAL, 2);
    }

    private void createThreadsSection(Composite parent) {
        Group threadsGroup = new Group(parent, SWT.NONE);
        threadsGroup.setText("Threads");
        GridLayout gl = new GridLayout();
        gl.numColumns = 5;
        threadsGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        threadsGroup.setLayoutData(gd);

        new Label(threadsGroup, SWT.NONE).setText("Thread Count");
        threadCountSpinner = new Spinner(threadsGroup, SWT.BORDER);
        threadCountSpinner.setMinimum(1);
        threadCountSpinner.setMaximum(20);
        threadCountSpinner.setIncrement(1);
        threadCountSpinner.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setDirty(true);
                validatePage();
            }
        });

        Label spacer = new Label(threadsGroup, SWT.NONE);
        gd = new GridData(GridData.CENTER);
        gd.widthHint = 20;
        spacer.setLayoutData(gd);

        new Label(threadsGroup, SWT.NONE).setText("Interval (Seconds)");
        threadIntervalSpinner = new Spinner(threadsGroup, SWT.BORDER);
        threadIntervalSpinner.setMinimum(1);
        threadIntervalSpinner.setMaximum(20);
        threadIntervalSpinner.setIncrement(1);
        threadIntervalSpinner.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setDirty(true);
                validatePage();
            }
        });
    }

    protected Composite createServerSection(Composite parent) {
        Composite composite = super.createServerSection(parent);

        createReplaceCheckBox = new Button(composite, SWT.CHECK);
        createReplaceCheckBox.setText("Create/Replace Results");
        createReplaceCheckBox.setLocation(120, 200);
        createReplaceCheckBox.pack();
        createReplaceCheckBox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setDirty(true);
                validatePage();
            }
        });

        return composite;
    }

    public void dispose() {
        super.dispose();
        image.dispose();
    }

    protected void validatePage() {
        super.validatePage();

        if (getErrorMessage() == null) {
            List<String> selectedCases = Arrays.asList(getTestCases());
            int count = selectedCases.size();
            int gherkinCount = 0;
            // warn if mixed launch
            for (AutomatedTestCase autoTestCase : getProject().getTestCases()) {
                if (selectedCases.contains(autoTestCase.getPath())) {
                    if (autoTestCase.isGherkin())
                        gherkinCount++;
                }
            }
            if (gherkinCount > 0 && count > gherkinCount) {
                setErrorMessage("Mixed Gherkin/non-Gherkin test selections not currently supported.");
            }
            else if (debug && count > 1) {
                setErrorMessage("Currently only one test case can be debugged.  Select Run mode for multiple cases.");
            }
            else if (!selectedCases.isEmpty() && createReplaceCheckBox.getSelection()) {
                setWarningMessage("Any existing test results assets will be overwritten.");
            }
            else {
                setErrorMessage(null);
                setWarningMessage(null);
            }
            updateLaunchConfigurationDialog();
        }
    }

    protected String getTestType() {
        return AutomatedTestCase.FUNCTION_TEST;
    }
}
