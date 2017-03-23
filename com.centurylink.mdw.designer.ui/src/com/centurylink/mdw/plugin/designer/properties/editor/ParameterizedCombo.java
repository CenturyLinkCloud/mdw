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
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.convert.ParameterizedValueConverter;
import com.centurylink.mdw.plugin.designer.properties.convert.ParameterizedValueConverter.ComboParameter;
import com.centurylink.mdw.plugin.designer.wizards.WorkflowAssetPage;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.swt.widgets.CTreeCombo;
import com.centurylink.swt.widgets.CTreeComboItem;

public class ParameterizedCombo extends Composer {
    private WorkflowElement workflowElement;
    private WorkflowProject workflowProject;
    private Combo mainCombo;
    private Label paramLabel;
    private Text paramText;
    private CTreeCombo paramCombo; // assuming ruleset
    private Link paramLink;
    private WorkflowAsset paramAsset;
    private boolean suppressFire = false;

    private ParameterizedValueConverter valueConverter;

    public ParameterizedCombo(Composite parent, WorkflowElement workflowElement,
            ParameterizedValueConverter valueConverter, int style, int width, boolean readOnly) {
        super(parent, style, width, readOnly);
        this.workflowElement = workflowElement;
        this.workflowProject = workflowElement.getProject();
        this.valueConverter = valueConverter;
        createControls();
    }

    protected void createControls() {
        int layoutCols = 6;

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = layoutCols;
        gridLayout.marginWidth = 0;
// gridLayout.horizontalSpacing = 5;
        setLayout(gridLayout);

        // main combo
        int style = SWT.DROP_DOWN | SWT.BORDER | getStyle();
        if (isReadOnly())
            style = style | SWT.READ_ONLY;
        mainCombo = new Combo(this, style);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = getWidth();
        mainCombo.setLayoutData(gd);
        mainCombo.removeAll();
        for (String valueOption : valueConverter.getOptions())
            mainCombo.add(valueOption);

        mainCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String newVal = mainCombo.getText().trim();

                if (paramText != null && !paramText.isDisposed()) {
                    paramText.dispose();
                    paramText = null;
                }
                if (paramCombo != null && !paramCombo.isDisposed()) {
                    paramCombo.dispose();
                    paramCombo = null;
                }
                if (paramLink != null && !paramLink.isDisposed()) {
                    paramLink.dispose();
                    paramLink = null;
                }

                ComboParameter param = valueConverter.getOptionParam(newVal);
                boolean hasParam = param != null;
                if (hasParam) {
                    boolean hasTypes = param.getTypes() != null;

                    paramLabel.setText(param.getName() + ":");
                    if (hasTypes) {
                        createParamCombo();
                        fillParamCombo(param.getTypes());
                    }
                    else {
                        createParamText();
                    }
                }
                else {
                    paramLabel.setText("");
                    paramAsset = null;
                }
                paramLabel.setVisible(hasParam);

                getParent().layout(true);

                if (!suppressFire)
                    fireModify(e);
            }
        });

        createSpacer(5);

        // param
        paramLabel = createLabel("Parameter:");
    }

    private void createParamText() {
        paramText = createText(100);
        paramText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                fireModify(e);
            }
        });
    }

    private void createParamCombo() {
        paramCombo = new CTreeCombo(this, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.heightHint = 16;
        paramCombo.setLayoutData(gd);
        paramCombo.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                CTreeComboItem[] selItems = paramCombo.getSelection();
                if (selItems.length == 1) {
                    CTreeComboItem selItem = selItems[0];
                    if (selItem.getItemCount() != 0) {
                        // ignore package selection
                        paramCombo.setSelection(new CTreeComboItem[0]);
                    }
                    else {
                        try {
                            Thread.sleep(200);
                        }
                        catch (InterruptedException ex) {
                        }

                        // set the definition doc
                        WorkflowPackage pkg = workflowProject
                                .getPackage(selItem.getParentItem().getText());
                        if (pkg == null) {
                            paramAsset = null;
                        }
                        else {
                            paramAsset = pkg.getAsset(selItem.getText());
                        }

                        paramLink.setText(getParamLinkLabel());
                        fireModify(new ModifyEvent(event));

                        paramCombo.dropDown(false);
                    }
                }
            }
        });
        paramCombo.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                if (paramCombo.getSelection().length == 0) {
                    // something was typed in the combo instead of selected
                    paramAsset = assetFromAttr(paramCombo.getText().trim());
                    paramLink.setText(getParamLinkLabel());

                    String oldVal = workflowElement.getAttribute(paramLabel.getText());
                    String newVal;
                    if (paramCombo.getText().startsWith("$"))
                        newVal = paramCombo.getText().trim();
                    else
                        newVal = paramAsset == null ? paramCombo.getText()
                                : attrFromAsset(paramAsset, false);

                    boolean changed = oldVal == null ? newVal != null && newVal.length() > 0
                            : !oldVal.equals(newVal);
                    if (changed && !suppressFire)
                        fireModify(new ModifyEvent(event));
                }
            }
        });

        paramLink = new Link(this, SWT.SINGLE);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 75;
        paramLink.setLayoutData(gd);
        paramLink.setText(getParamLinkLabel());
        paramLink.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (paramAsset == null) {
                    String[] paramTypes = valueConverter.getOptionParam(getValue()).getTypes();
                    paramAsset = createParamAsset(paramTypes);
                    fillParamCombo(paramTypes);
                    if (paramAsset != null) {
                        paramCombo.setText(paramAsset.getName());
                        fireModify(new ModifyEvent(new Event()));
                    }
                }
                else {
                    openParamAsset();
                }
            }
        });
    }

    @Override
    public void setInput(Object input) {
        suppressFire = true;
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;
        mainCombo.setText(values.get(0));
        if (values.size() > 1)
            setParamValue(values.get(1));
        suppressFire = false;
    }

    public void setParamValue(String value) {
        if (paramText != null && !paramText.isDisposed()) {
            paramText.setText(value);
        }

        if (paramCombo != null && !paramCombo.isDisposed()) {
            paramAsset = assetFromAttr(value);

            if (paramAsset != null) {
                CTreeComboItem selItem = null;
                for (CTreeComboItem pkgItem : paramCombo.getItems()) {
                    if (pkgItem.getText().equals(paramAsset.getPackage().getName())) {
                        for (CTreeComboItem assetItem : pkgItem.getItems()) {
                            if (assetItem.getText().equals(paramAsset.getName())) {
                                selItem = assetItem;
                                break;
                            }
                        }
                        break;
                    }
                }
                paramCombo.setSelection(
                        selItem == null ? new CTreeComboItem[0] : new CTreeComboItem[] { selItem });
            }
            else {
                paramCombo.setText(value == null ? "" : value);
            }

            paramLink.setText(getParamLinkLabel());
            paramLink.setVisible(paramAsset != null || !workflowElement.isReadOnly());
        }
    }

    @Override
    public void setEditable(boolean editable) {
        mainCombo.setEnabled(editable);
        if (paramText != null && !paramText.isDisposed())
            paramText.setEditable(editable);
        if (paramCombo != null && !paramCombo.isDisposed())
            paramCombo.setEditable(editable);
    }

    public String getValue() {
        return mainCombo.getText().trim();
    }

    public String getParamValue() {
        if (paramText != null && !paramText.isDisposed())
            return paramText.getText();
        else if (paramCombo != null && !paramCombo.isDisposed())
            return paramAsset == null ? paramCombo.getText() : attrFromAsset(paramAsset, true);
        else
            return null;
    }

    public List<String> getValues() {
        List<String> values = new ArrayList<String>();
        values.add(getValue());
        String paramVal = getParamValue();
        if (paramVal != null)
            values.add(paramVal);
        return values;
    }

    private void fillParamCombo(String[] types) {
        paramCombo.removeAll();

        Comparator<WorkflowElement> comparator = new Comparator<WorkflowElement>() {
            public int compare(WorkflowElement we1, WorkflowElement we2) {
                return we1.getName().compareTo(we2.getName());
            }
        };

        List<WorkflowAsset> assets = workflowProject.getAssetList(Arrays.asList(types));
        Map<WorkflowPackage, List<WorkflowAsset>> packageAssets = new TreeMap<WorkflowPackage, List<WorkflowAsset>>();

        for (WorkflowAsset asset : assets) {
            List<WorkflowAsset> assetsForPkg = packageAssets.get(asset.getPackage());
            if (assetsForPkg == null) {
                assetsForPkg = new ArrayList<WorkflowAsset>();
                packageAssets.put(asset.getPackage(), assetsForPkg);
            }
            assetsForPkg.add(asset);
        }

        for (WorkflowPackage pkg : packageAssets.keySet()) {
            CTreeComboItem packageItem = new CTreeComboItem(paramCombo, SWT.NONE);
            packageItem.setText(pkg.getName());
            packageItem.setImage(pkg.getIconImage());
            List<WorkflowAsset> assetsForPkg = packageAssets.get(pkg);
            Collections.sort(assetsForPkg, comparator);
            for (WorkflowAsset assetForPkg : assetsForPkg) {
                CTreeComboItem assetItem = new CTreeComboItem(packageItem, SWT.NONE);
                assetItem.setText(assetForPkg.getName());
                assetItem.setImage(assetForPkg.getIconImage());
            }
        }
    }

    private String getParamLinkLabel() {
        if (paramAsset == null) {
            if (paramCombo.getText().trim().length() == 0)
                return "<A>New...</A>";
            else
                return "";
        }
        else {
            boolean lockedToUser = paramAsset.isLockedToUser();
            if (lockedToUser)
                return " <A>Edit " + paramAsset.getVersionLabel() + "</A>";
            else
                return " <A>View " + paramAsset.getVersionLabel() + "</A>";
        }
    }

    public WorkflowAsset assetFromAttr(String attrValue) {
        if (attrValue == null || attrValue.isEmpty() || attrValue.startsWith("$")) {
            return null;
        }

        int slashIdx = attrValue.indexOf('/');
        if (slashIdx <= 0) {
            // prefer asset in same package
            WorkflowAsset pkgDoc = workflowElement.getPackage().getAsset(attrValue);
            if (pkgDoc != null)
                return pkgDoc;
            else
                return workflowProject.getAsset(attrValue);
        }
        else {
            String pkgName = attrValue.substring(0, slashIdx);
            WorkflowPackage pkg = workflowProject.getPackage(pkgName);
            if (pkg == null)
                return null;
            else
                return pkg.getAsset(attrValue.substring(slashIdx + 1));
        }
    }

    private String attrFromAsset(WorkflowAsset asset, boolean includePackage) {
        if (asset == null)
            return null;

        if (asset.isInDefaultPackage() || !includePackage)
            return asset.getName();
        else
            return asset.getPackage().getName() + "/" + asset.getName();
    }

    @SuppressWarnings("restriction")
    private WorkflowAsset createParamAsset(String[] paramTypes) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        org.eclipse.ui.internal.dialogs.NewWizard wizard = new org.eclipse.ui.internal.dialogs.NewWizard();
        wizard.setCategoryId("mdw.designer.asset");
        wizard.init(workbench,
                new StructuredSelection(new Object[] { workflowElement.getPackage() }));

        IDialogSettings workbenchSettings = org.eclipse.ui.internal.ide.IDEWorkbenchPlugin
                .getDefault().getDialogSettings();
        IDialogSettings wizardSettings = workbenchSettings.getSection("NewWizardAction");
        if (wizardSettings == null)
            wizardSettings = workbenchSettings.addNewSection("NewWizardAction");
        wizardSettings.put("NewWizardSelectionPage.STORE_SELECTED_ID", getWizardId(paramTypes));
        wizard.setDialogSettings(wizardSettings);
        wizard.setForcePreviousAndNextButtons(true);

        WizardDialog dialog = new WizardDialog(null, wizard);
        dialog.create();
        dialog.open();

        IWizardPage wizardPage = dialog.getCurrentPage();
        if (wizardPage instanceof WorkflowAssetPage)
            return ((WorkflowAssetPage) wizardPage).getWorkflowAsset();
        else
            return null;
    }

    private String getWizardId(String[] paramTypes) {
        for (String assetType : paramTypes) {
            String id = WorkflowAssetFactory.getWizardId(assetType);
            if (id != null)
                return id;
        }
        return null;
    }

    private void openParamAsset() {
        paramAsset.openFile(new NullProgressMonitor());
    }
}
