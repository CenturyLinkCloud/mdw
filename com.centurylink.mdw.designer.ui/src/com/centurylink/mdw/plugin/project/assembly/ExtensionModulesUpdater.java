/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.assembly;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModule;
import com.centurylink.mdw.plugin.project.extensions.ExtensionModuleException;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ExtensionModulesUpdater {
    private WorkflowProject project;

    public ExtensionModulesUpdater(WorkflowProject project) {
        assert (!project.isRemote());
        this.project = project;
    }

    private List<ExtensionModule> adds;

    public List<ExtensionModule> getAdds() {
        return adds;
    }

    public void setAdds(List<ExtensionModule> adds) {
        this.adds = adds;
    }

    public boolean hasAdds() {
        return adds != null && adds.size() > 0;
    }

    private List<ExtensionModule> removes;

    public List<ExtensionModule> getRemoves() {
        return removes;
    }

    public void setRemoves(List<ExtensionModule> removes) {
        this.removes = removes;
    }

    public boolean hasRemoves() {
        return removes != null && removes.size() > 0;
    }

    public void doUpdate(Shell shell) {
        if (project.getExtensionModules() != null) {
            for (ExtensionModule module : project.getExtensionModules()) {
                doUpdate(module, shell);
            }
        }
    }

    public void doChanges(Shell shell) {
        // do the removes
        if (hasRemoves()) {
            String confirm = "Remove the following extensions from project '" + project.getName()
                    + "'?";
            if (PluginMessages.uiList(shell, confirm, "Remove Extension Modules",
                    removes) == Dialog.OK) {
                for (ExtensionModule remove : removes) {
                    try {
                        if (remove.removeUi(project, shell))
                            doRemove(remove, shell);
                    }
                    catch (ExtensionModuleException ex) {
                        PluginMessages.uiError(shell, ex, "Remove Extension");
                    }
                }
            }
        }

        // do the adds
        if (hasAdds()) {
            for (ExtensionModule add : adds) {
                try {
                    if (add.addUi(project, shell))
                        doAdd(add, shell);
                }
                catch (ExtensionModuleException ex) {
                    PluginMessages.uiError(shell, ex, "Add Extension");
                }
            }
        }
    }

    private void doAdd(final ExtensionModule module, final Shell shell) {
        try {
            ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
            pmDialog.run(true, true, new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Adding Extension Module: " + module.getName(), 105);
                    monitor.worked(5);
                    try {
                        if (module.addTo(project, monitor))
                            project.addExtension(module);
                        WorkflowProjectManager.getInstance().save(project);
                        monitor.done();
                    }
                    catch (InterruptedException ex) {
                        throw ex;
                    }
                    catch (Exception ex) {
                        PluginMessages.log(ex);
                        throw new InvocationTargetException(ex);
                    }
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(shell, ex, "Add Extension Module", project);
        }
        catch (InterruptedException ex) {
            PluginMessages.log(ex);
            MessageDialog.openWarning(shell, "Add Extension Module", "Operation cancelled");
        }
    }

    private void doRemove(final ExtensionModule module, final Shell shell) {
        try {
            ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
            pmDialog.run(true, true, new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Removing Extension Module: " + module.getName(), 105);
                    monitor.worked(5);
                    try {
                        if (module.removeFrom(project, monitor))
                            project.removeExtension(module);
                        WorkflowProjectManager.getInstance().save(project);
                        monitor.done();
                    }
                    catch (InterruptedException ex) {
                        throw ex;
                    }
                    catch (Exception ex) {
                        PluginMessages.log(ex);
                        throw new InvocationTargetException(ex);
                    }
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(shell, ex, "Remove Extension Module", project);
        }
        catch (InterruptedException ex) {
            PluginMessages.log(ex);
            MessageDialog.openWarning(shell, "Remove Extension Module", "Operation cancelled");
        }
    }

    private void doUpdate(final ExtensionModule module, final Shell shell) {
        try {
            ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(shell);
            pmDialog.run(true, true, new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Updating Extension Module: " + module.getName(), 105);
                    monitor.worked(5);
                    try {
                        module.update(project, monitor);
                        monitor.done();
                    }
                    catch (InterruptedException ex) {
                        throw ex;
                    }
                    catch (Exception ex) {
                        PluginMessages.log(ex);
                        throw new InvocationTargetException(ex);
                    }
                }
            });
        }
        catch (InvocationTargetException ex) {
            PluginMessages.uiError(shell, ex, "Update Extension Module", project);
        }
        catch (InterruptedException ex) {
            PluginMessages.log(ex);
            MessageDialog.openWarning(shell, "Update Extension Module", "Operation cancelled");
        }
    }

}
