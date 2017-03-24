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
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabDescriptor;

/**
 * Wraps a standard Eclipse property tab.
 */
@SuppressWarnings("restriction")
public class PropertyTab extends TabDescriptor {
    public PropertyTab(IConfigurationElement configurationElement) {
        super(configurationElement);
    }

    private boolean dirty;

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Avoid discouraged access warnings.
     */
    public String getLabel() {
        return super.getLabel();
    }

    @Override
    public String getText() {
        return dirty ? getLabel() + " *" : getLabel();
    }

    public String getOverrideAttributePrefix() {
        // mdw.properties.tabs.simul.override
        int override = getId().indexOf(".override");
        String trimmed = getId().substring(0, override);
        return trimmed.substring(trimmed.lastIndexOf('.') + 1).toUpperCase(); // by
                                                                              // convention
    }

}
