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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.DocumentTemplate;
import com.centurylink.mdw.plugin.designer.model.Template;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class NewTemplateWizard extends WorkflowAssetWizard {
    public static final String WIZARD_ID = "mdw.designer.new.template";

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection, new Template());
    }

    @Override
    public String getTemplateLocation() {
        String languageSel = getWorkflowAssetPage().getLanguageCombo().getText();
        if (languageSel.equalsIgnoreCase(RuleSetVO.FACELET))
            return "/templates/facelet";
        else
            return null;
    }

    @Override
    public List<String> getTemplateOptions() {
        String languageSel = getWorkflowAssetPage().getLanguageCombo().getText();
        if (languageSel.equalsIgnoreCase(RuleSetVO.FACELET)) {
            return super.getTemplateOptions();
        }
        else {
            List<String> options = new ArrayList<String>();
            options.add(BLANK_TEMPLATE);
            return options;
        }
    }

    @Override
    public DocumentTemplate getNewDocTemplate() {
        if (getWorkflowAsset().getLanguage().equals(RuleSetVO.FACELET))
            return super.getNewDocTemplate();
        else
            return null;
    }
}