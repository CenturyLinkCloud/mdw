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
