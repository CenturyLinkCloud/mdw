/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabItem;
import org.eclipse.ui.views.properties.tabbed.ITabSelectionListener;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.PluginDataAccess;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.Transition;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class PropertySection
        extends org.eclipse.ui.views.properties.tabbed.AbstractPropertySection {
    private WorkflowElement selection;

    public WorkflowElement getSelection() {
        return selection;
    }

    protected Composite composite;
    protected TabbedPropertySheetPage tabbedPropertySheetPage;

    private PropertyEditor savePropertyEditor; // for override attributes

    private ITabDescriptor selectedTab;
    private boolean warnedDirty;

    public void createControls(Composite parent,
            org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage tabbedPropertySheetPage) {
        super.createControls(parent, tabbedPropertySheetPage);
        this.tabbedPropertySheetPage = (TabbedPropertySheetPage) tabbedPropertySheetPage;
        selection = (WorkflowElement) this.tabbedPropertySheetPage.getCurrentSelection();

        this.tabbedPropertySheetPage.addTabSelectionListener(new ITabSelectionListener() {
            public void tabSelected(ITabDescriptor tabDescriptor) {
                selectedTab = tabDescriptor;
            }
        });

        // create the composite to hold the widgets
        composite = createComposite(parent);
        drawWidgets(composite, selection);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
                MdwPlugin.getPluginId() + ".properties_help");
    }

    public int getTopMargin() {
        return 6;
    }

    private Composite createComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        // create the grid layout
        GridLayout gl = new GridLayout();
        gl.numColumns = PropertyEditor.COLUMNS;
        gl.marginTop = getTopMargin();
        gl.marginLeft = 3;
        composite.setLayout(gl);
        return composite;
    }

    public void setInput(IWorkbenchPart part, ISelection selection) {
        if (!(selection instanceof WorkflowElement))
            return;

        super.setInput(part, selection);
        if (selection != null && !selection.equals(this.selection)
                && this.selection.isOverrideAttributeDirty(getOverrideAttributePrefix())
                && !warnedDirty)
            warnDirty();
        this.selection = (WorkflowElement) selection;
        setSelection(this.selection);

        flagDirtyTabs();
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        GridData gridData = new GridData(GridData.BEGINNING);
        gridData.heightHint = 1;
        Label label = new Label(composite, SWT.NONE);
        label.setLayoutData(gridData);
    }

    /**
     * For override attributes.
     */
    protected PropertyEditor createSaveButton() {
        savePropertyEditor = new PropertyEditor(selection, PropertyEditor.TYPE_BUTTON);
        savePropertyEditor.setLabel("Save");
        savePropertyEditor.setWidth(65);
        savePropertyEditor.setComment(" ");
        savePropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                saveOverrideAttributes();
            }
        });
        savePropertyEditor.render(composite);
        savePropertyEditor
                .setEnabled(selection.isOverrideAttributeDirty(getOverrideAttributePrefix()));
        return savePropertyEditor;
    }

    public void setSelection(WorkflowElement element) {
    }

    @Override
    public boolean shouldUseExtraSpace() {
        return true;
    }

    public void notifyLabelChange() {
        if (!tabbedPropertySheetPage.getControl().isDisposed()
                && Thread.currentThread().equals(MdwPlugin.getDisplay().getThread()))
            tabbedPropertySheetPage.labelProviderChanged(null);
    }

    protected PluginDataAccess getDataAccess() {
        return selection.getProject().getDataAccess();
    }

    protected DesignerDataModel getDesignerDataModel() {
        return getDataAccess().getDesignerDataModel();
    }

    protected DesignerProxy getDesignerProxy() {
        return selection.getProject().getDesignerProxy();
    }

    protected boolean showScriptSection(Activity activity) {
        if (activity.getActivityImpl().getAttrDescriptionXml() == null)
            return false;

        PropertyEditorList propEditorList = new PropertyEditorList(activity);
        for (PropertyEditor propertyEditor : propEditorList) {
            if (propertyEditor.getType().equals(PropertyEditor.TYPE_SCRIPT)) {
                activity.setScriptLanguages(propertyEditor.getScriptLanguages());
                if (activity.isTransformActivity())
                    return false;
                return !"EXPRESSION".equals(propertyEditor.getScriptType());
            }
        }
        return false;
    }

    protected Shell getShell() {
        return getPart().getSite().getShell();
    }

    /**
     * Return non-null for override attributes.
     */
    public String getOverrideAttributePrefix() {
        return null;
    }

    protected void saveOverrideAttributes() {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                String subType = null;
                if (selection instanceof Activity)
                    subType = OwnerType.ACTIVITY;
                else if (selection instanceof Transition)
                    subType = OwnerType.WORK_TRANSITION;
                boolean saved = getDesignerProxy().saveOverrideAttributes(
                        getOverrideAttributePrefix(), OwnerType.PROCESS, selection.getProcessId(),
                        subType, subType == null ? null : String.valueOf(selection.getId()),
                        getOverrideAttributes());
                if (saved)
                    setDirty(false);
            }
        });
    }

    protected Map<String, String> getOverrideAttributes() {
        Map<String, String> overrideAttrs = new HashMap<String, String>();
        for (AttributeVO attribute : selection.getAttributes()) {
            if (WorkAttributeConstant.isAttrNameFor(attribute.getAttributeName(),
                    getOverrideAttributePrefix()) && attribute.getAttributeValue() != null)
                overrideAttrs.put(attribute.getAttributeName(), attribute.getAttributeValue());
        }
        return overrideAttrs;
    }

    protected void setDirty(boolean dirty) {
        selection.setOverrideAttributeDirty(getOverrideAttributePrefix(), dirty);
        if (savePropertyEditor != null)
            savePropertyEditor.setEnabled(dirty);
        flagDirtyTabs();
    }

    protected boolean isDirty() {
        return selection.isOverrideAttributeDirty(getOverrideAttributePrefix());
    }

    @Override
    public void aboutToBeHidden() {
        if (isDirty() && !warnedDirty)
            warnDirty();
    }

    protected void warnDirty() {
        String tabLabel = null;
        if (selectedTab instanceof PageletTab)
            tabLabel = ((PageletTab) selectedTab).getLabel();
        else if (selectedTab instanceof PropertyTab)
            tabLabel = ((PropertyTab) selectedTab).getLabel();
        String msg = "Remember to save "
                + (tabLabel == null ? "attributes." : tabLabel + " attributes.");
        MessageDialog.openWarning(getShell(), "Attributes", msg);
        warnedDirty = true;
    }

    /**
     * This is for dynamic pagelet tabs (override attributes).
     */
    @SuppressWarnings("restriction")
    protected void flagDirtyTabs() {
        org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyComposite composite = (org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyComposite) tabbedPropertySheetPage
                .getControl();
        if (composite != null && !composite.isDisposed()) {
            org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyList tabs = composite
                    .getList();
            if (tabs != null && !tabs.isDisposed()) {
                for (int i = 0; i < tabs.getNumberOfElements(); i++) {
                    Object elem = tabs.getElementAt(i);
                    if (elem instanceof org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyList.ListElement) {
                        org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyList.ListElement tabElem = (org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyList.ListElement) elem;
                        ITabItem tabItem = tabElem.getTabItem();
                        // by convention, Pagelet tabs and tab ids ending in
                        // ".override" are for override attributes
                        if (tabItem instanceof PageletTab) {
                            PageletTab pageletTab = (PageletTab) tabItem;
                            boolean wasDirty = pageletTab.isDirty();
                            boolean isDirty = selection.isOverrideAttributeDirty(
                                    pageletTab.getOverrideAttributePrefix());
                            if (wasDirty != isDirty) {
                                pageletTab.setDirty(isDirty);
                                tabElem.redraw();
                            }
                        }
                        else if (tabItem instanceof PropertyTab
                                && tabItem.toString().endsWith(".override")) {
                            PropertyTab propertyTab = (PropertyTab) tabItem;
                            boolean wasDirty = propertyTab.isDirty();
                            boolean isDirty = selection.isOverrideAttributeDirty(
                                    propertyTab.getOverrideAttributePrefix());
                            if (wasDirty != isDirty) {
                                propertyTab.setDirty(isDirty);
                                tabElem.redraw();
                            }
                        }
                    }
                }
            }
        }
    }
}
