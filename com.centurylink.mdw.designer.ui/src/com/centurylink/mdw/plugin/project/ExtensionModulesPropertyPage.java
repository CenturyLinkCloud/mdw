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
package com.centurylink.mdw.plugin.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.centurylink.mdw.plugin.project.assembly.ExtensionModulesUpdater;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModule;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModulesTable;

public class ExtensionModulesPropertyPage extends ProjectPropertyPage {
    private ExtensionModulesTable extensionsTable;
    private List<ExtensionModule> existingModules;

    @Override
    protected Control createContents(Composite parent) {
        noDefaultAndApplyButton();
        initializeWorkflowProject();

        Composite composite = createComposite(parent);

        extensionsTable = new ExtensionModulesTable(getProject());
        extensionsTable.create();
        extensionsTable.getTableEditor().render(composite, false);

        existingModules = getProject().getExtensionModules();
        List<ExtensionModule> selectedModules = new ArrayList<ExtensionModule>();
        selectedModules.addAll(existingModules);
        extensionsTable.setSelectedModules(selectedModules);

        return composite;
    }

    private List<ExtensionModule> adds;
    private List<ExtensionModule> removes;

    @Override
    public boolean performOk() {
        List<ExtensionModule> selectedModules = extensionsTable.getSelectedModules();

        // check for deltas
        adds = new ArrayList<ExtensionModule>();
        for (ExtensionModule selected : selectedModules) {
            if (!existingModules.contains(selected))
                adds.add(selected);
        }

        removes = new ArrayList<ExtensionModule>();
        for (ExtensionModule existing : existingModules) {
            if (!selectedModules.contains(existing))
                removes.add(existing);
        }

        ExtensionModulesUpdater changer = new ExtensionModulesUpdater(getProject());
        changer.setAdds(adds);
        changer.setRemoves(removes);
        changer.doChanges(getShell());

        return true;
    }

}
