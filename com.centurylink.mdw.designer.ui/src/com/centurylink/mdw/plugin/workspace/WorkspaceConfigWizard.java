/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
