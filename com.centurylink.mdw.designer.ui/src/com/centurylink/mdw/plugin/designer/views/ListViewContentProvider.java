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

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ListViewContentProvider implements IStructuredContentProvider {
    public ListViewContentProvider() {
    }

    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof Object[])
            return (Object[]) inputElement;
        else if (inputElement instanceof List)
            return ((List<?>) inputElement).toArray();
        else
            return null;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

    public long getCount() {
        // TODO
        return 0;
    }

    public int getPageIndex() {
        // TODO
        return 0;
    }
}
