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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestResults;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.LegacyExpectedResults;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;

public class AutomatedTestLabelProvider extends LabelProvider {
    private Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

    public Image getImage(Object element) {
        WorkflowElement workflowElement = (WorkflowElement) element;
        return workflowElement.getIconImage();
    }

    public String getText(Object element) {
        if (element instanceof AutomatedTestSuite) {
            AutomatedTestSuite testSuite = (AutomatedTestSuite) element;
            return testSuite.getProject().getName();
        }
        else if (element instanceof WorkflowPackage) {
            return ((WorkflowPackage) element).getName();
        }
        else if (element instanceof Folder) {
            return ((Folder) element).getName();
        }
        else if (element instanceof AutomatedTestCase) {
            AutomatedTestCase testCase = (AutomatedTestCase) element;
            return testCase.getLabel();
        }
        else if (element instanceof AutomatedTestResults) {
            AutomatedTestResults expectedResults = (AutomatedTestResults) element;
            return expectedResults.getLabel();
        }
        else if (element instanceof LegacyExpectedResults) {
            LegacyExpectedResults expectedResult = (LegacyExpectedResults) element;
            return expectedResult.getName();
        }

        return null;
    }

    public void dispose() {
        for (Image image : imageCache.values()) {
            image.dispose();
        }
        imageCache.clear();
    }

}
