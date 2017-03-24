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

import java.rmi.RemoteException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.plugin.CodeTimer;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class CacheRefresh {
    private WorkflowProject workflowProject;
    private RestfulServer restfulServer;
    private DesignerRunner designerRunner;
    private MDWStatusMessageDocument refreshStatusMessage;

    public CacheRefresh(WorkflowProject project, RestfulServer server) {
        this.workflowProject = project;
        this.restfulServer = server;
    }

    public void doRefresh(boolean silent) {
        doRefresh(silent, true);
    }

    public void doRefresh(boolean silent, final boolean includeDynamicJava) {
        try {
            if (silent) {
                if (workflowProject.isUpdateServerCache()) {
                    // run as background job
                    new Job("MDW Cache Refresh: " + restfulServer.getMdwWebUrl()) {
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                CodeTimer timer = new CodeTimer("CacheRefresh.doRefresh()");
                                refreshStatusMessage = restfulServer.refreshCache(
                                        workflowProject.isRemote(),
                                        workflowProject.isOldNamespaces(), includeDynamicJava);
                                timer.stopAndLog();
                            }
                            catch (Exception ex) {
                                if (MdwPlugin.getSettings().isLogConnectErrors())
                                    PluginMessages.log(ex);
                            }
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
            }
            else {
                String progressMsg = "Refreshing Caches and Properties for '"
                        + workflowProject.getLabel() + "'";
                String errorMsg = "Refresh Cache";

                designerRunner = new DesignerRunner(progressMsg, errorMsg, workflowProject) {
                    public void perform()
                            throws ValidationException, DataAccessException, RemoteException {
                        try {
                            CodeTimer timer = new CodeTimer("CacheRefresh.doRefresh()");
                            refreshStatusMessage = restfulServer.refreshCache(
                                    workflowProject.isRemote(), workflowProject.isOldNamespaces(),
                                    includeDynamicJava);
                            timer.stopAndLog();
                        }
                        catch (Exception ex) {
                            PluginMessages.uiError(ex, ex.getMessage(), "Refresh Caches",
                                    workflowProject);
                        }
                    }
                };
                designerRunner.run();

                if (refreshStatusMessage == null) {
                    PluginMessages.uiError(
                            workflowProject.getName()
                                    + " Refresh Caches and Properties failed.  Make sure baseline event handlers are registered.",
                            "Refresh Caches", workflowProject);
                }
                else if (refreshStatusMessage.getMDWStatusMessage().getStatusCode() != 0) {
                    String message = refreshStatusMessage.getMDWStatusMessage().getStatusMessage();
                    PluginMessages.uiMessage(
                            workflowProject.getName() + " Refresh Caches and Properties:\n"
                                    + message,
                            "Refresh Caches", workflowProject, PluginMessages.INFO_MESSAGE);
                }
            }
        }
        catch (Exception ex) {
            if (!silent)
                PluginMessages.uiError(ex, ex.getMessage(), "Refresh Caches", workflowProject);
        }
    }

    public void fireRefresh() {
        fireRefresh(true);
    }

    /**
     * Don't wait for response
     */
    public void fireRefresh(final boolean includeDynamicJava) {
        if (workflowProject.isUpdateServerCache()) {
            new Thread(new Runnable() {
                public void run() {
                    CodeTimer timer = new CodeTimer("fireProcessCacheRefresh()");
                    try {
                        restfulServer.refreshCache(workflowProject.isRemote(),
                                workflowProject.isOldNamespaces(), includeDynamicJava);
                    }
                    catch (Exception ex) {
                        if (MdwPlugin.getSettings().isLogConnectErrors())
                            PluginMessages.log(ex.toString());
                        if ("true".equals(
                                System.getProperty("mdw.designer.show.connect.stack.trace")))
                            PluginMessages.log(ex);
                    }
                    timer.stopAndLog();
                }
            }).start();
        }
    }

    public void refreshSingle(String cacheName, boolean silent) {
        try {
            MDWStatusMessageDocument statusMsgDoc = restfulServer.refreshCache("SingleCache",
                    cacheName, workflowProject.isRemote(), workflowProject.isOldNamespaces(),
                    false);
            if (!silent) {
                String message = statusMsgDoc.getMDWStatusMessage().getStatusMessage();
                PluginMessages.uiMessage(workflowProject.getName() + " Refresh Caches:\n" + message,
                        "Refresh Caches", workflowProject, PluginMessages.INFO_MESSAGE);
            }
        }
        catch (Exception ex) {
            if (!silent || MdwPlugin.getSettings().isLogConnectErrors())
                PluginMessages.uiError(ex, ex.getMessage(), "Refresh Caches", workflowProject);
        }
    }
}
