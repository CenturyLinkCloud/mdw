/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class PropertiesLabelProvider extends LabelProvider {
    public Image getImage(Object element) {
        if (element instanceof WorkflowElement) {
            WorkflowElement workflowElement = (WorkflowElement) element;
            return workflowElement.getIconImage();
        }

        return null;
    }

    public String getText(Object element) {
        if (element instanceof WorkflowElement) {
            WorkflowElement workflowElement = (WorkflowElement) element;
            return workflowElement.getFullPathLabel().replace('\n', ' ').replaceAll("\\r", "");
        }

        return null;
    }
}
