/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ProcessExplorerLabelProvider extends LabelProvider {
    public Image getImage(Object element) {
        if (element instanceof WorkflowProcess) {
            // TODO factor this logic into WorkflowProcess.getIcon()
            WorkflowProcess processVersion = (WorkflowProcess) element;
            if (processVersion.isTopLevel() && processVersion.getPackage() == null)
                return processVersion.getIconImage("processfolder.gif");
            else
                return processVersion.getIconImage();
        }
        else if (element instanceof WorkflowPackage) {
            // TODO factor this logic into WorkflowPackage.getIcon()
            WorkflowPackage packageVersion = (WorkflowPackage) element;
            if (packageVersion.isArchived() && packageVersion.isTopLevel())
                return packageVersion.getIconImage("packagefolder.gif");
            else
                return packageVersion.getIconImage();
        }
        else if (element instanceof WorkflowElement) {
            WorkflowElement workflowElement = (WorkflowElement) element;
            return workflowElement.getIconImage();
        }

        return null;
    }

    public String getText(Object element) {
        if (element instanceof WorkflowProcess) {
            // TODO factor this logic into WorkflowProcess.getLabel()
            WorkflowProcess processVersion = (WorkflowProcess) element;
            if (processVersion.isTopLevel()) {
                if (processVersion.getPackage() == null)
                    return processVersion.getName();
                else
                    return processVersion.getLabel();
            }
            else {
                return processVersion.getVersionLabel();
            }
        }
        else if (element instanceof WorkflowPackage) {
            // TODO factor this logic into WorkflowPackage.getLabel()
            WorkflowPackage packageVersion = (WorkflowPackage) element;

            if (packageVersion.isArchived()) {
                if (packageVersion.isTopLevel())
                    return packageVersion.getName();
                else
                    return packageVersion.getVersionLabel();
            }
            else {
                return packageVersion.getLabel();
            }
        }
        else if (element instanceof WorkflowElement) {
            WorkflowElement workflowElement = (WorkflowElement) element;
            return workflowElement.getLabel();
        }

        return null;
    }

    public void dispose() {
    }
}
