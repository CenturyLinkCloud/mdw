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

import org.eclipse.jface.viewers.IFilter;

import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditorList;

public class CcRecipientsSection extends DesignSection implements IFilter {
    @Override
    public boolean selectForSection(PropertyEditor propertyEditor) {
        return PropertyEditor.SECTION_CC_RECIPIENTS.equals(propertyEditor.getSection());
    }

    @Override
    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof Activity))
            return false;

        Activity activity = (Activity) toTest;
        if (!activity.getProject().isMdw5())
            return false;

        if (activity.getProject().checkRequiredVersion(6))
            return false; // combined with Recipients

        if (activity.isForProcessInstance())
            return false;

        if (activity.isNotification())
            return true;

        if (activity.isManualTask()) {
            PropertyEditorList propEditorList = new PropertyEditorList(activity);
            for (PropertyEditor propertyEditor : propEditorList) {
                // return true if any widgets specify CC RECIPIENTS section
                if (PropertyEditor.SECTION_CC_RECIPIENTS.equals(propertyEditor.getSection()))
                    return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldUseExtraSpace() {
        return true;
    }
}
