/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.value;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;

public class DefaultViewerComparator extends ViewerComparator {
    private String property;
    private SortDirectionProvider sortDirectionProvider;
    private PropertyUtilsBean propUtilsBean = new PropertyUtilsBean();

    public DefaultViewerComparator(ColumnSpec colSpec,
            SortDirectionProvider sortDirectionProvider) {
        this.property = colSpec.property;
        this.sortDirectionProvider = sortDirectionProvider;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int compare(Viewer viewer, Object element1, Object element2) {
        try {
            Comparable c1 = (Comparable) propUtilsBean.getProperty(element1, property);
            Comparable c2 = (Comparable) propUtilsBean.getProperty(element2, property);
            if (sortDirectionProvider.getSortDirection() == SWT.UP) {
                if (c1 == null)
                    return -1;
                else if (c2 == null)
                    return 1;
                return c1.compareTo(c2);
            }
            else {
                if (c2 == null)
                    return -1;
                else if (c1 == null)
                    return 1;
                return c2.compareTo(c1);
            }
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
            return 0;
        }
    }

    public interface SortDirectionProvider {
        public int getSortDirection();
    }
}
