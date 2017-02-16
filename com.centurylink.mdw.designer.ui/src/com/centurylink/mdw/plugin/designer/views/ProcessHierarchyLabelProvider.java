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
