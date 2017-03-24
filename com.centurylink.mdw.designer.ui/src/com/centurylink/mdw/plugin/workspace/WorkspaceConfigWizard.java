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
package com.centurylink.mdw.plugin.workspace;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.wizard.Wizard;

import com.centurylink.mdw.plugin.PluginMessages;

public class WorkspaceConfigWizard extends Wizard {
    private WorkspaceConfigPage workspaceConfigPage;

    // model
    private WorkspaceConfig workspaceConfig;

    public WorkspaceConfig getWorkspaceConfig() {
        return workspaceConfig;
    }

    public WorkspaceConfigWizard(WorkspaceConfig workspaceConfig) {
        this.workspaceConfig = workspaceConfig;
        setWindowTitle("MDW Workspace Configuration");
    }

    @Override
    public void addPages() {
        workspaceConfigPage = (WorkspaceConfigPage) new WorkspaceConfigPage();
        addPage(workspaceConfigPage);
    }

    @SuppressWarnings("restriction")
    public boolean performFinish() {
        WorkspaceConfigurator configurator = new WorkspaceConfigurator(getWorkspaceConfig(),
                getShell());
        try {
            getContainer().run(false, true,
                    new org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter(configurator));
            return configurator.isAccepted();
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(getShell(), ex, "Workspace Setup");
            return false;
        }
        catch (InterruptedException ex) {
            throw new OperationCanceledException(ex.getMessage());
        }
    }
}
