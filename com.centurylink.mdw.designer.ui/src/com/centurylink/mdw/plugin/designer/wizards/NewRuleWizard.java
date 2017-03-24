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
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.DocumentTemplate;
import com.centurylink.mdw.plugin.designer.model.Rule;

public class NewRuleWizard extends WorkflowAssetWizard {
    public static final String WIZARD_ID = "mdw.designer.new.rule";

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection, new Rule());
    }

    @Override
    public DocumentTemplate getNewDocTemplate() {
        if (((Rule) getWorkflowAsset()).isExcel())
            return new DocumentTemplate("Empty", getWorkflowAsset().getExtension(),
                    "templates/excel");
        else if (((Rule) getWorkflowAsset()).isExcel2007())
            return new DocumentTemplate("Empty", getWorkflowAsset().getExtension(),
                    "templates/excel");
        else
            return new DocumentTemplate("Empty", getWorkflowAsset().getExtension(),
                    "templates/drools");
    }
}