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

import java.awt.EventQueue;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.console.MessageConsoleStream;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.MessageConsole;
import com.centurylink.mdw.plugin.MessageConsole.ConsoleRunnableEntity;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.runtime.LogSubscriberSocket;
import com.centurylink.mdw.designer.runtime.ProcessInstancePage;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

public class LogWatcher implements ConsoleRunnableEntity {
    public static final String MESSAGE_REG_EX = "\\[\\(.\\)([0-9.:]+) p([0-9]+)\\.([0-9]+) ([a-z])([^\\]]+)\\] (.*)";

    private Display display;

    public void setDisplay(Display display) {
        this.display = display;
    }

    private String masterRequestId;

    public void setMasterRequestId(String masterRequestId) {
        this.masterRequestId = masterRequestId;
    }

    private WorkflowProcess process;

    public void setProcess(WorkflowProcess process) {
        this.process = process;
    }

    protected WorkflowProject getProject() {
        return process.getProject();
    }

    private LogSubscriberSocket logListener;
    private MessageConsoleStream outputStream;
    private boolean watchProcess;
    private ImageDescriptor icon;
    private Pattern pattern;
    private Map<Long, ProcessInstancePage> processInstancePages;
    private Map<Long, ProcessInstanceVO> processInstances;
    private DesignerDataAccess dataAccess;

    private ServerSettings serverSettings;

    public ServerSettings getServerSettings() {
        return serverSettings;
    }

    public static LogWatcher instance;

    public static LogWatcher getInstance(Display display, ServerSettings serverSettings) {
        if (instance == null)
            instance = new LogWatcher(serverSettings);

        instance.setDisplay(display);

        return instance;
    }

    private LogWatcher(ServerSettings serverSettings) {
        this.serverSettings = serverSettings; // currently port is not used
        icon = MdwPlugin.getImageDescriptor("icons/display_prefs.gif");
    }

    public boolean isRunning() {
        return logListener != null;
    }

    public void startup(boolean watchProcess) {
        this.watchProcess = watchProcess;

        if (logListener != null)
            return; // already started

        pattern = Pattern.compile(MESSAGE_REG_EX, Pattern.DOTALL);
        processInstancePages = new HashMap<Long, ProcessInstancePage>();
        processInstances = new HashMap<Long, ProcessInstanceVO>();
        accumulated = new LinkedList<ProcessInstanceUpdater>();
        scheduleShutdown = false;

        MessageConsole console = MessageConsole.findConsole("Live View Log", icon, display);
        console.setRunnableEntity(this);
        console.setDefaultShowPref(true);
        console.setStatus(MessageConsole.STATUS_RUNNING);
        console.clearConsole();
        outputStream = console.newMessageStream();

        try {
            dataAccess = new DesignerDataAccess(
                    process.getProject().getDesignerProxy().getDesignerDataAccess());
            logListener = new LogSubscriberSocket(dataAccess, serverSettings.getLogWatcherPort(),
                    process.getProject().isOldNamespaces()) {
                protected void handleMessage(String message) {
                    handleLogMessage(message);
                }
            };
            logListener.start(true);
            new Thread(new Runnable() {
                public void run() {
                    processQueue();
                }
            }).start();
        }
        catch (Exception ex) {
            if (outputStream != null)
                ex.printStackTrace(new PrintStream(outputStream));

            if (logListener != null && !logListener.isClosed()) {
                logListener.shutdown();
                logListener = null;
            }
            PluginMessages.uiError(ex, "Monitor Logs", process.getProject());
        }
    }

    public void shutdown() {
        if (logListener != null) {
            logListener.shutdown();
            logListener = null;
        }

        if (outputStream != null) {
            try {
                outputStream.println("...Monitoring Terminated");
                outputStream.close();
                outputStream = null;
            }
            catch (IOException ex) {
                PluginMessages.log(ex);
            }
        }

        MessageConsole messageConsole = MessageConsole.findConsole("Live View Log", icon, display);
        messageConsole.setStatus(MessageConsole.STATUS_TERMINATED);
    }

    private Queue<ProcessInstanceUpdater> accumulated;
    private boolean scheduleShutdown = false;

    private synchronized void handleLogMessage(String message) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.matches()) {
            String time = matcher.group(1);
            final Long procId = new Long(matcher.group(2));
            final Long procInstId = new Long(matcher.group(3));
            String subtype = matcher.group(4);
            String id = matcher.group(5);
            String msg = matcher.group(6);

            try {
                ProcessInstanceVO processInstanceInfo = processInstances.get(procInstId);
                if (processInstanceInfo == null) {
                    processInstanceInfo = new DesignerDataAccess(dataAccess)
                            .getProcessInstanceBase(procInstId, null);

                    // only interested in one masterRequestId
                    if (processInstanceInfo.getMasterRequestId().equals(masterRequestId))
                        processInstances.put(procInstId, processInstanceInfo);
                }

                if (!processInstanceInfo.getMasterRequestId().equals(masterRequestId))
                    return;

                // log the message
                outputStream.println(message);

                if (watchProcess) {
                    openInstance(processInstanceInfo);

                    synchronized (this) {
                        ProcessInstancePage processInstancePage = processInstancePages
                                .get(processInstanceInfo.getId());
                        accumulated.offer(new ProcessInstanceUpdater(procId, procInstId,
                                processInstancePage, subtype, time, id, msg));
                    }
                }

                if (msg.startsWith(WorkStatus.LOGMSG_PROC_COMPLETE)
                        && procId.equals(process.getId()))
                    scheduleShutdown = true;
            }
            catch (Exception ex) {
                PluginMessages.log(ex);
                shutdown();
            }
        }
    }

    private void processQueue() {
        while (isRunning()) {
            ProcessInstanceUpdater pid = null;
            final ProcessInstancePage pip;
            // boolean isEmbedded = false;
            synchronized (this) {
                pid = accumulated.peek();
                // isEmbedded = pid == null ? false :
                // isEmbeddedProcess(processInstanceInfos.get(pid.getProcessInstanceId()));
                pip = pid == null ? null : processInstancePages.get(pid.getProcessInstanceId());
                if (pid != null) {
                    if (pip != null)
                        accumulated.remove();
                }
            }
            if (pid != null && pip != null) {
                pid.setProcessInstancePage(pip);
                pid.handleMessage();

                if (scheduleShutdown && accumulated.isEmpty())
                    shutdown();

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        pip.canvas.repaint();
                    }
                });
            }
            try {
                Thread.sleep(250);
            }
            catch (InterruptedException ex) {
            }
        }

        refresh();
    }

    private synchronized void refresh() {
        // refresh the process instance(s)
        if (!display.isDisposed()) {
            display.asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage page = MdwPlugin.getActivePage();
                    if (page != null) {
                        for (ProcessInstanceVO pii : processInstances.values()) {
                            WorkflowProcess pv = getProject().getProcess(pii.getProcessId());
                            WorkflowProcess instance = new WorkflowProcess(pv);
                            instance.setProcessInstance(pii);
                            ProcessEditor editor = (ProcessEditor) page.findEditor(instance);
                            if (editor != null) {
                                editor.refresh();
                            }
                        }
                    }
                }
            });
        }
    }

    private synchronized void openInstance(final ProcessInstanceVO processInstanceInfo) {
        final WorkflowProcess newProcess = new WorkflowProcess(process);
        if (!processInstanceInfo.getProcessId().equals(process.getId())) {
            // must be a subprocess
            ProcessVO subprocVO = new ProcessVO();
            subprocVO.setProcessId(processInstanceInfo.getProcessId());
            subprocVO.setProcessName(processInstanceInfo.getProcessName());
            newProcess.setProcessVO(subprocVO);
        }
        final boolean isEmbedded = isEmbeddedProcess(processInstanceInfo);

        final ProcessInstanceVO pageInfo = isEmbedded
                ? processInstances.get(processInstanceInfo.getOwnerId()) : processInstanceInfo;
        newProcess.setProcessInstance(pageInfo);

        display.syncExec(new Runnable() {
            public void run() {
                ProcessInstancePage procInstPage = null;
                try {
                    IWorkbenchPage page = MdwPlugin.getActivePage();
                    ProcessEditor editor = null;
                    if (processInstancePages.get(pageInfo.getId()) == null) {
                        newProcess.setDesignerDataAccess(dataAccess);
                        editor = (ProcessEditor) page.openEditor(newProcess, "mdw.editors.process");
                        procInstPage = editor.getProcessCanvasWrapper().getProcessInstancePage();
                        if (procInstPage != null) {
                            processInstancePages.put(pageInfo.getId(), procInstPage);
                        }
                    }
                    else if (isEmbedded
                            && processInstancePages.get(processInstanceInfo.getId()) == null) {
                        newProcess.getProject().getDesignerProxy().loadProcessInstance(newProcess,
                                processInstancePages.get(pageInfo.getId()));
                        processInstancePages.put(processInstanceInfo.getId(),
                                processInstancePages.get(pageInfo.getId()));
                    }
                    else {
                        editor = (ProcessEditor) page.findEditor(newProcess);
                        if (editor != null)
                            page.activate(editor);
                    }
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                    processInstancePages.remove(processInstanceInfo.getId());
                }
            }
        });
    }

    private boolean isEmbeddedProcess(ProcessInstanceVO processInstance) {
        if (processInstance.isNewEmbedded())
            return true;
        if (!processInstance.getOwner().equals(OwnerType.PROCESS_INSTANCE))
            return false;
        ProcessInstanceVO parentInstance = processInstances.get(processInstance.getOwnerId());
        if (parentInstance == null)
            return false; // possible when the instance is a subprocess of an
                          // embedded

        WorkflowProcess parentProcDef = process.getProject()
                .getProcess(parentInstance.getProcessId());

        if (parentProcDef.getProcessVO().getSubProcesses() != null) {
            for (ProcessVO childproc : parentProcDef.getProcessVO().getSubProcesses()) {
                if (childproc.getProcessId().equals(processInstance.getProcessId()))
                    return true;
            }
        }
        return false;
    }
}
