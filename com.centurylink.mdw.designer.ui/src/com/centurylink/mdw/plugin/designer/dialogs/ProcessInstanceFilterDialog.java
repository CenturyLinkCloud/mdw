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
package com.centurylink.mdw.plugin.designer.dialogs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.ecl.datepicker.DatePicker;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.model.ProcessInstanceFilter;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.VariableValue;
import com.centurylink.mdw.plugin.variables.VariableValuesTableContainer;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;

public class ProcessInstanceFilterDialog extends TrayDialog {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private ProcessInstanceFilter filter;

    public ProcessInstanceFilter getFilter() {
        return filter;
    }

    public void setFilter(ProcessInstanceFilter pif) {
        this.filter = pif;
    }

    private Combo versionCombo;
    private Text instanceIdText;
    private Text masterRequestIdText;
    private Combo ownerCombo;
    private Text ownerIdText;
    private Combo statusCombo;
    private DatePicker startFromDatePicker;
    private DatePicker startToDatePicker;
    private Button startDateClearButton;
    private DatePicker endFromDatePicker;
    private DatePicker endToDatePicker;
    private Button endDateClearButton;
    private Spinner pageSizeSpinner;

    private VariableValuesTableContainer tableContainer;
    private List<VariableValue> variableValues;

    public int RESET_ID = 2;

    private static Map<String, String> ownerTypes = new LinkedHashMap<String, String>();
    static {
        ownerTypes.put("Designer", "Designer");
        ownerTypes.put("Document", "DOCUMENT");
        ownerTypes.put("External Event Instance", "EXTERNAL_EVENT_INSTANCE");
        ownerTypes.put("Order", "ORDER");
        ownerTypes.put("Process Instance", "PROCESS_INSTANCE");
        ownerTypes.put("Process Launcher", "Process Launcher");
        ownerTypes.put("Tester", "TESTER");
    }

    private String getOwnerType(String owner) {
        for (String ownerType : ownerTypes.keySet()) {
            if (owner.equals(ownerTypes.get(ownerType)))
                return ownerType;
        }
        return null;
    }

    private static Map<String, Integer> statuses = new TreeMap<String, Integer>();
    static {
        for (Integer code : WorkStatuses.getWorkStatuses().keySet())
            statuses.put(WorkStatuses.getWorkStatuses().get(code), code);
    }

    private String getStatus(Integer code) {
        return WorkStatuses.getWorkStatuses().get(code);
    }

    public ProcessInstanceFilterDialog(Shell shell, WorkflowProcess processVersion,
            ProcessInstanceFilter filter) {
        super(shell);
        this.process = processVersion;
        this.filter = filter;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 6;
        composite.setLayout(layout);

        composite.getShell().setText("Process Instance Filters");

        TabFolder tabFolder = new TabFolder(composite, SWT.NONE);

        createProcessTabItem(tabFolder);

        if (process != null && process.getVariables() != null)
            createVariablesTabItem(tabFolder);

        tabFolder.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if ("Variables".equals(((TabItem) e.item).getText())) {
                    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                        public void run() {
                            // populate process variables
                            variableValues = new ArrayList<VariableValue>();
                            String verString = versionCombo.getText().trim();
                            int version = 0;
                            if (verString.length() > 0)
                                version = RuleSetVO.parseVersion(verString);
                            try {
                                ProcessVO proc = process.getProject().getDesignerProxy()
                                        .loadProcess(process.getName(), version);
                                for (VariableVO variableVO : proc.getVariables()) {
                                    String varName = variableVO.getVariableName();
                                    VariableTypeVO varType = process.getProject().getDataAccess()
                                            .getVariableType(variableVO.getVariableType());
                                    variableValues.add(new VariableValue(variableVO, varType,
                                            filter.getVariableValues().get(varName)));
                                }
                            }
                            catch (Exception ex) {
                                PluginMessages.uiError(ex, "Get Process Variables",
                                        process.getProject());
                            }
                        }
                    });
                    tableContainer.setInput(variableValues);
                }
            }
        });

        return composite;
    }

    private TabItem createProcessTabItem(TabFolder tabFolder) {
        TabItem processTabItem = new TabItem(tabFolder, SWT.NULL);
        processTabItem.setText("Process");

        Composite composite = new Composite(tabFolder, SWT.NONE);
        GridLayout gl = new GridLayout();
        gl.numColumns = 5;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // process label
        new Label(composite, SWT.NONE).setText("Process:");
        Label processLabel = new Label(composite, SWT.NONE);
        if (process != null)
            processLabel.setText(process.getName());
        GridData gd = new GridData(SWT.BEGINNING);
        gd.horizontalSpan = 2;
        processLabel.setLayoutData(gd);

        // process version
        new Label(composite, SWT.NONE).setText("Version:");
        versionCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        versionCombo.removeAll();
        versionCombo.add("");
        for (WorkflowProcess pv : process.getAllProcessVersions())
            versionCombo.add(pv.getVersionString());
        if (process.getId() == 0)
            versionCombo.setText("");
        else
            versionCombo.setText(process.getVersionString());
        versionCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                filter.setProcess(versionCombo.getText().trim());
            }
        });

        Label vSpacer = new Label(composite, SWT.NONE);
        gd = new GridData(SWT.BEGINNING);
        gd.heightHint = 1;
        gd.horizontalSpan = 5;
        vSpacer.setLayoutData(gd);

        // instance id
        new Label(composite, SWT.NONE).setText("Instance ID:");
        instanceIdText = new Text(composite, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.BEGINNING);
        gd.widthHint = 100;
        instanceIdText.setLayoutData(gd);
        if (filter.getProcessInstanceId() != null)
            instanceIdText.setText(filter.getProcessInstanceId().toString());
        instanceIdText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                try {
                    if (instanceIdText.getText().trim().length() == 0)
                        filter.setProcessInstanceId(null);
                    else
                        filter.setProcessInstanceId(
                                Long.parseLong(instanceIdText.getText().trim()));
                }
                catch (NumberFormatException ex) {
                    if (filter.getProcessInstanceId() == null)
                        instanceIdText.setText("");
                    else
                        instanceIdText.setText(filter.getProcessInstanceId().toString());
                }
            }
        });

        new Label(composite, SWT.NONE).setText("");

        // master request id
        new Label(composite, SWT.NONE).setText("Master Request ID:");
        masterRequestIdText = new Text(composite, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.BEGINNING);
        gd.widthHint = 100;
        masterRequestIdText.setLayoutData(gd);
        if (filter.getMasterRequestId() != null)
            masterRequestIdText.setText(filter.getMasterRequestId());
        masterRequestIdText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (masterRequestIdText.getText().trim().length() == 0)
                    filter.setMasterRequestId(null);
                else
                    filter.setMasterRequestId(masterRequestIdText.getText().trim());
            }
        });

        // owner
        new Label(composite, SWT.NONE).setText("Owner Type:");
        ownerCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        ownerCombo.removeAll();
        ownerCombo.add("");
        for (String ownerType : ownerTypes.keySet())
            ownerCombo.add(ownerType);
        if (filter.getOwner() != null)
            ownerCombo.setText(getOwnerType(filter.getOwner()));
        ownerCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (ownerCombo.getText().trim().length() == 0)
                    filter.setOwner(null);
                else
                    filter.setOwner(ownerTypes.get(ownerCombo.getText().trim()));
            }
        });

        Label spacer = new Label(composite, SWT.NONE);
        gd = new GridData(SWT.BEGINNING);
        gd.widthHint = 20;
        spacer.setLayoutData(gd);

        // owner id
        new Label(composite, SWT.NONE).setText("Owner ID:");
        ownerIdText = new Text(composite, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(SWT.BEGINNING);
        gd.widthHint = 100;
        ownerIdText.setLayoutData(gd);
        if (filter.getOwnerId() != null)
            ownerIdText.setText(filter.getOwnerId().toString());
        ownerIdText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                try {
                    if (ownerIdText.getText().trim().length() == 0)
                        filter.setOwnerId(null);
                    else
                        filter.setOwnerId(Long.parseLong(ownerIdText.getText().trim()));
                }
                catch (NumberFormatException ex) {
                    if (filter.getOwnerId() == null)
                        ownerIdText.setText("");
                    else
                        ownerIdText.setText(filter.getOwnerId().toString());
                }
            }
        });

        // status
        new Label(composite, SWT.NONE).setText("Status:");
        statusCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        statusCombo.removeAll();
        statusCombo.add("");
        for (String status : statuses.keySet())
            statusCombo.add(status);
        gd = new GridData(SWT.BEGINNING);
        gd.horizontalSpan = 4;
        statusCombo.setLayoutData(gd);
        if (filter.getStatusCode() != null)
            statusCombo.setText(getStatus(filter.getStatusCode()));
        statusCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (statusCombo.getText().trim().length() == 0)
                    filter.setStatusCode(null);
                else
                    filter.setStatusCode(statuses.get(statusCombo.getText().trim()));
            }
        });

        // start date range
        new Label(composite, SWT.NONE).setText("Start Date:");

        Group startDateGroup = new Group(composite, SWT.NONE);
        gl = new GridLayout();
        gl.numColumns = 7;
        startDateGroup.setLayout(gl);
        gd = new GridData(SWT.BEGINNING);
        gd.horizontalSpan = 4;
        startDateGroup.setLayoutData(gd);

        // start date from
        new Label(startDateGroup, SWT.NONE).setText("From:");
        startFromDatePicker = new DatePicker(startDateGroup, SWT.BORDER);
        Calendar startFrom = null;
        if (filter.getStartDateFrom() != null) {
            startFrom = Calendar.getInstance();
            startFrom.setTime(filter.getStartDateFrom());
        }
        startFromDatePicker.setSelection(startFrom);
        startFromDatePicker.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (startFromDatePicker.getSelection() == null)
                    filter.setStartDateFrom(null);
                else
                    filter.setStartDateFrom(startFromDatePicker.getSelection().getTime());
            }
        });

        spacer = new Label(startDateGroup, SWT.NONE);
        gd = new GridData(SWT.BEGINNING);
        gd.widthHint = 10;
        spacer.setLayoutData(gd);

        // start date to
        new Label(startDateGroup, SWT.NONE).setText("To:");
        startToDatePicker = new DatePicker(startDateGroup, SWT.BORDER);
        Calendar startTo = null;
        if (filter.getStartDateTo() != null) {
            startTo = Calendar.getInstance();
            startTo.setTime(filter.getStartDateTo());
        }
        startToDatePicker.setSelection(startTo);
        startToDatePicker.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (startToDatePicker.getSelection() == null)
                    filter.setStartDateTo(null);
                else
                    filter.setStartDateTo(startToDatePicker.getSelection().getTime());
            }
        });

        spacer = new Label(startDateGroup, SWT.NONE);
        gd = new GridData(SWT.BEGINNING);
        gd.widthHint = 10;
        spacer.setLayoutData(gd);

        // start date clear
        startDateClearButton = new Button(startDateGroup, SWT.PUSH);
        startDateClearButton
                .setImage(MdwPlugin.getImageDescriptor("icons/clear.gif").createImage());
        startDateClearButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                startToDatePicker.setSelection(null);
                startFromDatePicker.setSelection(null);
            }
        });

        // end date range
        new Label(composite, SWT.NONE).setText("End Date:");

        Group endDateGroup = new Group(composite, SWT.NONE);
        gl = new GridLayout();
        gl.numColumns = 7;
        endDateGroup.setLayout(gl);
        gd = new GridData(SWT.BEGINNING);
        gd.horizontalSpan = 4;
        endDateGroup.setLayoutData(gd);

        // end date from
        new Label(endDateGroup, SWT.NONE).setText("From:");
        endFromDatePicker = new DatePicker(endDateGroup, SWT.BORDER);
        Calendar endFrom = null;
        if (filter.getEndDateFrom() != null) {
            endFrom = Calendar.getInstance();
            endFrom.setTime(filter.getEndDateFrom());
        }
        endFromDatePicker.setSelection(endFrom);
        endFromDatePicker.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (endFromDatePicker.getSelection() == null)
                    filter.setEndDateFrom(null);
                else
                    filter.setEndDateFrom(endFromDatePicker.getSelection().getTime());

                if (filter.getEndDateFrom() != null && endToDatePicker.getSelection() == null) {
                    Calendar endTo = endFromDatePicker.getSelection();
                    endTo.add(Calendar.DATE, 7);
                    endToDatePicker.setSelection(endTo);
                }
            }
        });

        spacer = new Label(endDateGroup, SWT.NONE);
        gd = new GridData(SWT.BEGINNING);
        gd.widthHint = 10;
        spacer.setLayoutData(gd);

        // end date to
        new Label(endDateGroup, SWT.NONE).setText("To:");
        endToDatePicker = new DatePicker(endDateGroup, SWT.BORDER);
        Calendar endTo = null;
        if (filter.getEndDateTo() != null) {
            endTo = Calendar.getInstance();
            endTo.setTime(filter.getEndDateTo());
        }
        endToDatePicker.setSelection(endTo);
        endToDatePicker.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (endToDatePicker.getSelection() == null)
                    filter.setEndDateTo(null);
                else
                    filter.setEndDateTo(endToDatePicker.getSelection().getTime());
            }
        });

        spacer = new Label(endDateGroup, SWT.NONE);
        gd = new GridData(SWT.BEGINNING);
        gd.widthHint = 10;
        spacer.setLayoutData(gd);

        // end date clear
        endDateClearButton = new Button(endDateGroup, SWT.PUSH);
        endDateClearButton.setImage(MdwPlugin.getImageDescriptor("icons/clear.gif").createImage());
        endDateClearButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                endToDatePicker.setSelection(null);
                endFromDatePicker.setSelection(null);
            }
        });

        vSpacer = new Label(composite, SWT.NONE);
        gd = new GridData(SWT.BEGINNING);
        gd.heightHint = 1;
        gd.horizontalSpan = 5;
        vSpacer.setLayoutData(gd);

        // max rows
        new Label(composite, SWT.NONE).setText("Page Size:");
        pageSizeSpinner = new Spinner(composite, SWT.BORDER);
        pageSizeSpinner.setMinimum(10);
        pageSizeSpinner.setMaximum(100);
        pageSizeSpinner.setIncrement(10);
        if (filter.getPageSize() != null)
            pageSizeSpinner.setSelection(filter.getPageSize().intValue());
        pageSizeSpinner.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                filter.setPageSize(pageSizeSpinner.getSelection());
            }
        });

        processTabItem.setControl(composite);
        return processTabItem;
    }

    private TabItem createVariablesTabItem(TabFolder tabFolder) {
        TabItem variablesTabItem = new TabItem(tabFolder, SWT.NULL);
        variablesTabItem.setText("Variables");

        Composite composite = new Composite(tabFolder, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // variables table
        tableContainer = new VariableValuesTableContainer();
        tableContainer.create(composite);
        tableContainer.addDirtyStateListener(new DirtyStateListener() {
            public void dirtyStateChanged(boolean dirty) {
                filter.getVariableValues().clear();
                if (variableValues != null) {
                    for (VariableValue variableValue : variableValues)
                        filter.getVariableValues().put(variableValue.getName(),
                                variableValue.getValue());
                }
            }
        });
        variablesTabItem.setControl(composite);

        variableValues = new ArrayList<VariableValue>();
        tableContainer.setInput(variableValues);

        return variablesTabItem;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        Button resetButton = createButton(parent, RESET_ID, "Reset", false);
        resetButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                resetFields();
            }
        });
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private void resetFields() {
        instanceIdText.setText("");
        masterRequestIdText.setText("");
        ownerCombo.setText("");
        ownerIdText.setText("");
        statusCombo.setText("");
        startToDatePicker.setSelection(null);
        startFromDatePicker.setSelection(null);
        endToDatePicker.setSelection(null);
        endFromDatePicker.setSelection(null);
        pageSizeSpinner.setSelection(20);
        filter.setPageSize(20);
        filter.getVariableValues().clear();
        for (VariableValue varVal : variableValues)
            varVal.setValue("");
        tableContainer.setInput(variableValues);
    }
}