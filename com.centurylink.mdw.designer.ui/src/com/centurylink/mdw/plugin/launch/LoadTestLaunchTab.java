/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;

public class LoadTestLaunchTab extends TestSuiteLaunchTab {
    private Spinner runIntervalSpinner;

    private Image image = MdwPlugin.getImageDescriptor("icons/load_test.gif").createImage();
    private Map<String, Integer> testCaseCounts = new HashMap<String, Integer>();

    @Override
    public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
        workingCopy.setAttribute(AutomatedTestLaunchConfiguration.IS_LOAD_TEST, new Boolean(true));
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
        createRunsSection(composite);
        createServerSection(composite);
        createTestCasesSection(composite);
    }

    protected void createTestCasesSection(Composite parent) {
        createColumnSpecs();
        createTable(parent);
        createTableViewer();
        createSelectButtons(parent);
    }

    public String getName() {
        return "Load Test";
    }

    public Image getImage() {
        return image;
    }

    public void initializeFrom(ILaunchConfiguration launchConfig) {
        super.initializeFrom(launchConfig);

        try {
            int threadInterval = launchConfig.getAttribute(
                    getAttrPrefix() + AutomatedTestLaunchConfiguration.RUN_INTERVAL, 2);
            runIntervalSpinner.setSelection(threadInterval);

            Map<String, String> testCaseCountsStrMap = launchConfig.getAttribute(
                    getAttrPrefix() + AutomatedTestLaunchConfiguration.TESTCASE_COUNTS_MAP,
                    new HashMap<String, String>());
            for (Object key : testCaseCountsStrMap.keySet())
                testCaseCounts.put(key.toString(),
                        Integer.parseInt(testCaseCountsStrMap.get(key).toString()));

            testCasesTableViewer.refresh();

            if (launchConfig.getAttribute(AutomatedTestLaunchConfiguration.IS_LOAD_TEST, false))
                validatePage();
        }
        catch (CoreException ex) {
            PluginMessages.uiError(ex, "Launch Init", getProject());
        }
    }

    public void performApply(ILaunchConfigurationWorkingCopy launchConfig) {
        super.performApply(launchConfig);
        launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.RUN_INTERVAL,
                runIntervalSpinner.getSelection());
        HashMap<String, String> testCaseCountsStrMap = new HashMap<String, String>();
        for (String key : testCaseCounts.keySet()) {
            Integer intValue = testCaseCounts.get(key);
            if (intValue == null)
                testCaseCountsStrMap.remove(key);
            else
                testCaseCountsStrMap.put(key, intValue + "");
        }
        launchConfig.setAttribute(
                getAttrPrefix() + AutomatedTestLaunchConfiguration.TESTCASE_COUNTS_MAP,
                testCaseCountsStrMap);
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy launchConfig) {
        launchConfig.setAttribute(getAttrPrefix() + AutomatedTestLaunchConfiguration.RUN_INTERVAL,
                2);
    }

    private void createRunsSection(Composite parent) {
        Group runsGroup = new Group(parent, SWT.NONE);
        runsGroup.setText("Runs");
        GridLayout gl = new GridLayout();
        gl.numColumns = 5;
        runsGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        runsGroup.setLayoutData(gd);

        new Label(runsGroup, SWT.NONE).setText("Interval (Seconds)");
        runIntervalSpinner = new Spinner(runsGroup, SWT.BORDER);
        runIntervalSpinner.setMinimum(1);
        runIntervalSpinner.setMaximum(20);
        runIntervalSpinner.setIncrement(1);
        runIntervalSpinner.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                setDirty(true);
                validatePage();
            }
        });
    }

    protected void createColumnSpecs() {
        super.createColumnSpecs();

        ColumnSpec testCaseRunColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Count", "count");
        testCaseRunColSpec.width = 50;
        testCaseRunColSpec.readOnly = false;
        testCasesColumnSpecs.add(testCaseRunColSpec);

        testCasesColumnProps = new String[testCasesColumnSpecs.size()];
        for (int i = 0; i < testCasesColumnSpecs.size(); i++) {
            testCasesColumnProps[i] = testCasesColumnSpecs.get(i).property;
        }
    }

    protected void createTableViewer() {
        testCasesTableViewer = new TableViewer(testCasesTable);
        testCasesTableViewer.setUseHashlookup(true);

        testCasesTableViewer.setColumnProperties(testCasesColumnProps);

        CellEditor[] editors = new CellEditor[testCasesColumnSpecs.size()];
        for (int i = 0; i < testCasesColumnSpecs.size(); i++) {
            ColumnSpec colSpec = testCasesColumnSpecs.get(i);
            CellEditor cellEditor = null;
            if (colSpec.type.equals(PropertyEditor.TYPE_TEXT)) {
                if (i == 1)
                    cellEditor = new TextCellEditor(testCasesTable);
                else {
                    // Text with digits only for 3rd column
                    cellEditor = new TextCellEditor(testCasesTable);
                    ((Text) cellEditor.getControl()).addVerifyListener(new VerifyListener() {
                        public void verifyText(VerifyEvent e) {
                            e.doit = "0123456789".indexOf(e.text) >= 0;
                        }
                    });
                }
            }
            else if (colSpec.type.equals(PropertyEditor.TYPE_CHECKBOX)) {
                cellEditor = new CheckboxCellEditor(testCasesTable);
            }
            editors[i] = cellEditor;
        }
        testCasesTableViewer.setCellEditors(editors);
        testCasesTableViewer.setCellModifier(new TestCaseCellModifier());
        testCasesTableViewer.setLabelProvider(new TestCaseLabelProvider());
        testCasesTableViewer.setContentProvider(new TestCaseContentProvider());
    }

    class TestCaseLabelProvider extends TestSuiteLaunchTab.TestCaseLabelProvider {
        public String getColumnText(Object element, int columnIndex) {
            if (columnIndex == 2) {
                Integer count = testCaseCounts.get((String) element);
                return count == null ? null : count.toString();
            }
            else {
                return super.getColumnText(element, columnIndex);
            }
        }
    }

    class TestCaseCellModifier extends TestSuiteLaunchTab.TestCaseCellModifier {
        public Object getValue(Object element, String property) {
            String testCaseName = (String) element;
            int colIndex = getColumnIndex(property);
            if (colIndex == 2) {
                Integer count = testCaseCounts.get(testCaseName);
                return (count == null ? "" : count.toString());
            }
            else
                return super.getValue(element, property);
        }

        public void modify(Object element, String property, Object value) {
            TableItem item = (TableItem) element;
            String testCaseName = (String) item.getData();
            int colIndex = getColumnIndex(property);

            if (colIndex == 2) {
                if (value == null || value.toString().length() == 0 || value == "")
                    testCaseCounts.remove(testCaseName);
                else
                    testCaseCounts.put(testCaseName, Integer.parseInt(value.toString()));
                testCasesTableViewer.update(testCaseName, null);
                setDirty(true);
                validatePage();
            }
            else {
                super.modify(element, property, value);
            }
        }
    }

    @Override
    protected void validatePage() {
        super.validatePage();

        if (getProject() == null || (!getProject().checkRequiredVersion(5, 2)
                && !getProject().checkRequiredVersion(4, 5, 20))) {
            setErrorMessage(
                    "Load Testing requires a Workflow Project with an MDW Version of at least 5.2 or 4.5.20");
        }

        for (String testCase : getTestCases()) {
            Integer ctStr = testCaseCounts.get(testCase);
            if (ctStr == null || ctStr == 0)
                setErrorMessage("All selected Load Test Cases require a non-zero Count.");
        }

        updateLaunchConfigurationDialog();
    }

    public void dispose() {
        super.dispose();
        image.dispose();
    }

    protected String getTestType() {
        return AutomatedTestCase.LOAD_TEST;
    }
}
