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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.model.value.process.LinkedProcessInstance;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.views.ProcessHierarchyContentProvider.LinkedProcess;

public class ProcessHierarchyLabelProvider extends LabelProvider {
    private Image iconImage;

    public Image getIconImage() {
        if (iconImage == null) {
            ImageDescriptor imageDescriptor = MdwPlugin.getImageDescriptor("icons/process.gif");
            iconImage = imageDescriptor.createImage();
        }
        return iconImage;
    }

    public Image getImage(Object element) {
        return getIconImage();
    }

    public String getText(Object element) {
        if (element instanceof LinkedProcessInstance) {
            LinkedProcessInstance instance = (LinkedProcessInstance) element;
            ProcessInstanceVO procInst = instance.getProcessInstance();
            return procInst.getProcessName() + " v" + procInst.getProcessVersion() + " ("
                    + procInst.getId() + ")";
        }
        else if (element instanceof LinkedProcess) {
            return ((LinkedProcess) element).getProcess().getLabel();
        }
        else {
            return null;
        }
    }

    public void dispose() {
        if (iconImage != null && !iconImage.isDisposed())
            iconImage.dispose();
    }

}
