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
package com.centurylink.mdw.plugin.designer;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import org.apache.xmlbeans.XmlException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class DesignerRunner {
    private Shell shell;
    private ProgressMonitorDialog progressMonitorDialog;
    private String progressMessage;
    private String errorMessage;
    private WorkflowProject workflowProject;
    private RunnerResult result;

    public RunnerResult getResult() {
        return result;
    }

    public RunnerStatus getStatus() {
        if (result == null)
            return null;
        return result.getStatus();
    }

    public DesignerRunner(String progressMessage, String errorMessage,
            WorkflowProject workflowProject) {
        this.progressMessage = progressMessage;
        this.errorMessage = errorMessage;
        this.workflowProject = workflowProject;
    }

    public abstract void perform()
            throws ValidationException, DataAccessException, RemoteException, XmlException;

    public RunnerResult run() {
        return run(true);
    }

    public RunnerResult run(boolean forked) {
        result = new RunnerResult();

        shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
        progressMonitorDialog = new MdwProgressMonitorDialog(shell);
        try {
            shell.setFocus(); // this is absolutely needed
            progressMonitorDialog.run(forked, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(progressMessage, IProgressMonitor.UNKNOWN);
                    try {
                        perform();
                        result.setStatus(RunnerStatus.SUCCESS);
                    }
                    catch (Exception ex) {
                        result.setStatus(RunnerStatus.FAILURE);
                        throw new InvocationTargetException(ex);
                    }
                }
            });
            return result;
        }
        catch (Exception ex) {
            int messageLevel = PluginMessages.isDataIntegrityException(ex)
                    ? PluginMessages.DATA_INTEGRITY_MESSAGE : PluginMessages.ERROR_MESSAGE;
            PluginMessages.uiMessage(shell, ex, errorMessage, workflowProject, messageLevel);
            if (ex.getCause() instanceof ValidationException) {
                if (DesignerProxy.INCOMPATIBLE_INSTANCES.equals(ex.getCause().getMessage())
                        || (ex.getCause().getMessage() != null && ex.getCause().getMessage()
                                .contains(DesignerProxy.ALREADY_EXISTS)))
                    result.setStatus(RunnerStatus.DISALLOW);
                else
                    result.setStatus(RunnerStatus.INVALID);
            }

            return result;
        }
    }

    public enum RunnerStatus {
        INITIAL, SUCCESS, FAILURE, INVALID, DISALLOW
    }

    public class RunnerResult {
        private RunnerStatus status;

        public RunnerStatus getStatus() {
            return status;
        }

        public void setStatus(RunnerStatus status) {
            this.status = status;
        }

        private Object[] returnValues;

        public Object[] getReturnValues() {
            return returnValues;
        }

        public void setReturnValues(Object[] vals) {
            this.returnValues = vals;
        }

        public RunnerResult() {
            status = RunnerStatus.INITIAL;
        }
    }
}
