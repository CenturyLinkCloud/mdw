/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

/**
 * Dynamic section for pagelet-driven tabs (see PageletTab).
 */
public class PageletSection extends PropertySection {
    private WorkflowElement element;

    public WorkflowElement getElement() {
        return element;
    }

    private PropertyEditorList propertyEditors;
    private PropertyEditor savePropertyEditor;
    private PropertyEditor offlinePropertyEditor;
    private PropertyEditor helpPropertyEditor;

    private List<String> attributeNames;

    private String tabId;

    public PageletSection(String tabId) {
        this.tabId = tabId;
    }

    public void setSelection(WorkflowElement selection) {
        element = (WorkflowElement) selection;

        if (propertyEditors != null) {
            for (PropertyEditor propertyEditor : propertyEditors) {
                propertyEditor.dispose();
            }
        }
        if (savePropertyEditor != null)
            savePropertyEditor.dispose();
        if (offlinePropertyEditor != null)
            offlinePropertyEditor.dispose();
        if (helpPropertyEditor != null)
            helpPropertyEditor.dispose();

        final PageletTab tab = element.getProject().getPageletTab(tabId);
        if (tab != null) {
            if (element.overrideAttributesApplied()) {
                attributeNames = new ArrayList<String>();
                propertyEditors = new PropertyEditorList(element, tab.getXml());
                for (PropertyEditor propertyEditor : propertyEditors) {
                    attributeNames.add(propertyEditor.getName());
                    propertyEditor.setFireDirtyStateChange(false);
                    propertyEditor.render(composite);
                    propertyEditor.setValue(element.getAttribute(propertyEditor.getName()));
                    if (!propertyEditor.getType().equals(PropertyEditor.TYPE_LINK)) {
                        // pagelet-driven attributes are always considered
                        // override attributes
                        boolean editable = element.isUserAuthorized(UserRoleVO.PROCESS_EXECUTION)
                                && !propertyEditor.isReadOnly();
                        propertyEditor.setEditable(editable);
                        propertyEditor.setFireDirtyStateChange(false);
                    }
                    if (!propertyEditor.isReadOnly()) {
                        propertyEditor.addValueChangeListener(new ValueChangeListener() {
                            public void propertyValueChanged(Object newValue) {
                                setDirty(true);
                            }
                        });
                    }
                }

                // help link
                helpPropertyEditor = new PropertyEditor(element, PropertyEditor.TYPE_LINK);
                helpPropertyEditor.setLabel("Override Attributes Help");
                helpPropertyEditor.render(composite);
                helpPropertyEditor.setValue("/MDWHub/doc/override-attributes.html");

                // save button
                savePropertyEditor = createSaveButton();
            }
            else {
                // offline message
                offlinePropertyEditor = new PropertyEditor(element, PropertyEditor.TYPE_INFO);
                offlinePropertyEditor.render(composite);
                offlinePropertyEditor
                        .setValue("Attributes unavailable. Reload process with server online.");

                // help link
                helpPropertyEditor = new PropertyEditor(element, PropertyEditor.TYPE_LINK);
                helpPropertyEditor.setLabel("Override Attributes Help");
                helpPropertyEditor.render(composite);
                helpPropertyEditor.setValue("/MDWHub/doc/todo.html");
            }
        }

        composite.layout(true);
    }

    public String getOverrideAttributePrefix() {
        if (attributeNames != null && attributeNames.size() > 0)
            return WorkAttributeConstant.getOverrideAttributePrefix(attributeNames.get(0));
        else
            return null;
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        // widget creation is deferred until setSelection()
    }
}