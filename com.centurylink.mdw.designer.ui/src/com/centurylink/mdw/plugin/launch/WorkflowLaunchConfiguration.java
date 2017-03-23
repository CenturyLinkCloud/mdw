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
package com.centurylink.mdw.plugin.launch;

import java.io.IOException;
import java.util.Map;

import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;

import com.centurylink.mdw.designer.DesignerCompatibility;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;

public abstract class WorkflowLaunchConfiguration extends LaunchConfigurationDelegate {
    public static final String WORKFLOW_PROJECT = "workflowProject";
    public static final String EXTERNAL_EVENT_REQUEST = "externalEventRequest";
    public static final String NOTIFY_PROCESS_EVENT = "notifyProcessEvent";
    public static final String NOTIFY_PROCESS_REQUEST = "notifyProcessRequest";
    public static final String DEFAULT_DEBUG_PORT = "8500";

    private boolean writeToConsole = true;

    public boolean isWriteToConsole() {
        return writeToConsole;
    }

    public void setWriteToConsole(boolean write) {
        writeToConsole = write;
    }

    public void fireExternalEvent(WorkflowProject workflowProject, String request,
            Map<String, String> headers) {
        try {
            String response = workflowProject.getDesignerProxy().sendExternalEvent(request,
                    headers);
            if (writeToConsole)
                writeToConsole("External Event Response", response + "\n");
        }
        catch (Exception ex) {
            showError(ex, "Send External Event", workflowProject);
        }
    }

    public void notifyProcess(WorkflowProject workflowProject, String eventName, String message,
            Map<String, String> headers) {
        try {
            ActionRequestDocument msgdoc = ActionRequestDocument.Factory.newInstance();
            ActionRequest actionRequest = msgdoc.addNewActionRequest();
            com.centurylink.mdw.service.Action act = actionRequest.addNewAction();
            act.setName("RegressionTest");
            Parameter param = act.addNewParameter();
            param.setName("SubAction");
            param.setStringValue("NotifyProcess");
            param = act.addNewParameter();
            param.setName("EventName");
            param.setStringValue(eventName);
            param = act.addNewParameter();
            param.setName("Message");
            param.setStringValue(message);
            String request;
            if (workflowProject.isOldNamespaces())
                request = DesignerCompatibility.getInstance().getOldActionRequest(msgdoc);
            else
                request = msgdoc.xmlText();
            String response = workflowProject.getDesignerProxy().notifyProcess(request, headers);
            if (writeToConsole)
                writeToConsole("Notify Process Response", response + "\n");
        }
        catch (Exception ex) {
            showError(ex, "Send External Event", workflowProject);
        }
    }

    protected void showError(final String message, final String title,
            final WorkflowProject workflowProject) {
        MdwPlugin.getDisplay().asyncExec(new Runnable() {
            public void run() {
                PluginMessages.uiError(message, title, workflowProject);
            }
        });
    }

    protected void showError(final Exception ex, final String title,
            final WorkflowProject workflowProject) {
        MdwPlugin.getDisplay().asyncExec(new Runnable() {
            public void run() {
                PluginMessages.uiError(ex, title, workflowProject);
            }
        });
    }

    private org.eclipse.ui.console.MessageConsole console;

    protected void writeToConsole(String name, String toWrite) throws IOException {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMgr = plugin.getConsoleManager();
        IConsole[] existing = conMgr.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (name.equals(existing[i].getName()))
                console = (org.eclipse.ui.console.MessageConsole) existing[i];
        }

        if (console == null) {
            console = new org.eclipse.ui.console.MessageConsole(name,
                    MdwPlugin.getImageDescriptor("icons/extevent.gif"));
            conMgr.addConsoles(new IConsole[] { console });
        }

        console.newMessageStream().write(toWrite);

        MdwPlugin.getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage page = MdwPlugin.getActivePage();
                if (page != null) {
                    try {
                        IConsoleView view = (IConsoleView) page
                                .showView(IConsoleConstants.ID_CONSOLE_VIEW);
                        if (view != null)
                            view.display(console);
                    }
                    catch (PartInitException ex) {
                        PluginMessages.log(ex);
                    }
                }
            }
        });
    }

}
