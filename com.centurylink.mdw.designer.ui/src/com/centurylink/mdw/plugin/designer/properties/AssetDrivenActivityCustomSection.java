/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.AssetLocator;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.properties.editor.WorkflowAssetEditor;

public class AssetDrivenActivityCustomSection extends DesignSection implements IFilter {
    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    private CustomAttributeVO customAttrVO;
    private Map<String, String> valueMap;

    private PropertyEditorList propertyEditors;
    private Label warningLabel;
    private Label commentLabel;
    private PropertyEditor clearBtnPropertyEditor;

    public void setSelection(WorkflowElement selection) {
        activity = (Activity) selection;
        customAttrVO = determineCustomAttr(activity);

        if (propertyEditors != null) {
            for (PropertyEditor propertyEditor : propertyEditors)
                propertyEditor.dispose();
        }
        if (warningLabel != null)
            warningLabel.dispose();
        if (commentLabel != null)
            commentLabel.dispose();
        if (clearBtnPropertyEditor != null)
            clearBtnPropertyEditor.dispose();

        if (customAttrVO == null || StringHelper.isEmpty(customAttrVO.getDefinition())) {
            warningLabel = new Label(composite, SWT.NONE);
            warningLabel.setText("No custom attributes defined for workflow asset type"
                    + (customAttrVO == null ? "." : ": " + customAttrVO.getCategorizer()));
        }
        else {
            commentLabel = new Label(composite, SWT.NONE);
            GridData gd = new GridData(SWT.LEFT);
            gd.horizontalSpan = PropertyEditor.COLUMNS;
            commentLabel.setLayoutData(gd);
            commentLabel.setText(
                    "Enter the custom attribute values that must be matched at runtime for the asset to be in effect.");

            valueMap = new HashMap<String, String>();
            String attr = activity.getAttribute("CustomAttributes");
            if (attr != null)
                valueMap = StringHelper.parseMap(attr);

            // TODO: update is broken
            // setAttribute() must be called on property update (must not update
            // activity directly)
            propertyEditors = new PropertyEditorList(activity, customAttrVO.getDefinition());
            for (PropertyEditor propertyEditor : propertyEditors) {
                propertyEditor.setReadOnly(true);
                propertyEditor.render(composite);
                propertyEditor.setValue(valueMap.get(propertyEditor.getName()));
            }

            // clear button
            clearBtnPropertyEditor = new PropertyEditor(activity, PropertyEditor.TYPE_BUTTON);
            clearBtnPropertyEditor.setLabel("Clear");
            clearBtnPropertyEditor.setComment("Remove Criteria:");
            clearBtnPropertyEditor.setWidth(65);
            clearBtnPropertyEditor.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    clear();
                }
            });
            clearBtnPropertyEditor.render(composite);
        }

        composite.layout(true);
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        // widget creation is deferred until setSelection()
    }

    private CustomAttributeVO determineCustomAttr(Activity activity) {
        PropertyEditorList propEditorList = new PropertyEditorList(activity);
        for (PropertyEditor propertyEditor : propEditorList) {
            if (propertyEditor instanceof WorkflowAssetEditor) {
                WorkflowAssetEditor assetEditor = (WorkflowAssetEditor) propertyEditor;
                propertyEditor.setValue(activity);
                AssetLocator locator = new AssetLocator(assetEditor.getElement(),
                        assetEditor.getLocatorType());
                WorkflowAsset asset = (WorkflowAsset) locator
                        .assetFromAttr(activity.getAttribute(assetEditor.getAttributeName()));
                if (asset != null) {
                    // language definitively determined by selected asset
                    return activity.getProject().getDataAccess()
                            .getAssetCustomAttribute(asset.getLanguage());
                }
                else {
                    // guess language based on presence of custom attributes
                    for (String language : assetEditor.getAssetTypes()) {
                        CustomAttributeVO customAttr = activity.getProject().getDataAccess()
                                .getAssetCustomAttribute(language);
                        if (customAttr != null && !StringHelper.isEmpty(customAttr.getDefinition()))
                            return customAttr;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;

        if (activity.isManualTask()) // manual tasks have asset-driven widgets
                                     // but no custom section
            return false;

        if (activity.isSubProcessInvoke()) // so do subprocess launch activities
            return false;

        if (activity.isForProcessInstance())
            return false;

        PropertyEditorList propEditorList = new PropertyEditorList((Activity) toTest);
        for (PropertyEditor propertyEditor : propEditorList) {
            // return true if any widgets are considered asset-driven
            if (propertyEditor instanceof WorkflowAssetEditor)
                return true;
        }

        return false;
    }

    public String getAttribute(String name) {
        return valueMap.get(name);
    }

    public void setAttribute(String name, String value) {
        valueMap.put(name, value);
        StringBuffer customAttrs = new StringBuffer();
        int i = 0;
        for (String key : valueMap.keySet()) {
            customAttrs.append(key).append("=");
            customAttrs.append(valueMap.get(key));
            if (i < valueMap.keySet().size() - 1)
                customAttrs.append(";");
        }
        activity.setAttribute("CustomAttributes", customAttrs.toString());
    }

    private void clear() {
        activity.removeAttribute("CustomAttributes");
        for (PropertyEditor propertyEditor : propertyEditors) {
            propertyEditor.setValue((String) null);
        }
        activity.fireDirtyStateChanged(true);
    }
}
