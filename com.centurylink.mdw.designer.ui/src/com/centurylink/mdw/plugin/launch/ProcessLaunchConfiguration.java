/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.VariableValue;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessLaunchConfiguration extends WorkflowLaunchConfiguration {
    public static final String PROCESS_NAME = "processName";
    public static final String PROCESS_VERSION = "processVersion";
    public static final String MASTER_REQUEST_ID = "masterRequestId";
    public static final String OWNER = "owner";
    public static final String OWNER_ID = "ownerId";
    public static final String SYNCHRONOUS = "synchronous";
    public static final String RESPONSE_VAR_NAME = "responseVarName";
    public static final String SHOW_LOGS = "showLogsInConsole";
    public static final String LOG_WATCHER_PORT = "logWatcherPort";
    public static final String LIVE_VIEW = "processInstanceLiveView";
    public static final String VARIABLE_VALUES = "variableValues";
    public static final String LAUNCH_VIA_EXTERNAL_EVENT = "launchViaExternalEvent";
    public static final String NOTIFY_PROCESS = "nofifyProcess";

    public void launch(ILaunchConfiguration launchConfig, String mode, ILaunch launch,
            IProgressMonitor monitor) throws CoreException {
        String wfProjectName = launchConfig.getAttribute(WORKFLOW_PROJECT, "");
        WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                .getWorkflowProject(wfProjectName);
        if (workflowProject == null) {
            showError("Can't locate workflow project: '" + wfProjectName + "'.", "Process Launch",
                    null);
            return;
        }

        if (mode.equals(ILaunchManager.DEBUG_MODE)) {
            if (!connectForDebug(workflowProject, launchConfig))
                return;
        }

        setWriteToConsole(
                launchConfig.getAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true));

        boolean viaExternalEvent = launchConfig.getAttribute(LAUNCH_VIA_EXTERNAL_EVENT, false);
        if (viaExternalEvent) {
            String request = launchConfig.getAttribute(EXTERNAL_EVENT_REQUEST, "");
            fireExternalEvent(workflowProject, request, null);
            return;
        }

        boolean notifyProcess = launchConfig.getAttribute(NOTIFY_PROCESS, false);
        if (notifyProcess) {
            String eventName = launchConfig.getAttribute(NOTIFY_PROCESS_EVENT, "");
            String message = launchConfig.getAttribute(NOTIFY_PROCESS_REQUEST, "");
            notifyProcess(workflowProject, eventName, message, null);
            return;
        }

        String processName = launchConfig.getAttribute(PROCESS_NAME, "");
        String processVersion = launchConfig.getAttribute(PROCESS_VERSION, "");
        WorkflowProcess process = workflowProject.getProcess(processName, processVersion);
        if (process == null) {
            // handle condition: obsolete version no longer in project list, but
            // not yet in archive
            ProcessVO procVO = workflowProject.getDesignerProxy().getProcessVO(processName,
                    processVersion);
            if (procVO == null) {
                showError("Can't locate process '" + processName + " v" + processVersion + "' in "
                        + wfProjectName + ".", "Process Launch", workflowProject);
                return;
            }
            else {
                process = new WorkflowProcess(workflowProject, procVO);
            }
        }

        String masterRequestId = launchConfig.getAttribute(MASTER_REQUEST_ID, "");
        if (masterRequestId.length() == 0) {
            showError("Missing masterRequestId.", "Process Launch", workflowProject);
            return;
        }

        String owner = launchConfig.getAttribute(OWNER, "");
        String ownerId = launchConfig.getAttribute(OWNER_ID, "");
        boolean synchronous = launchConfig.getAttribute(SYNCHRONOUS, false);
        String responseVarName = launchConfig.getAttribute(RESPONSE_VAR_NAME, "");
        Map<String, String> variableValues = launchConfig.getAttribute(VARIABLE_VALUES,
                new HashMap<String, String>());

        boolean showLogs = launchConfig.getAttribute(SHOW_LOGS, false);
        int logWatchPort = launchConfig.getAttribute(LOG_WATCHER_PORT, 7181);
        boolean liveView = launchConfig.getAttribute(LIVE_VIEW, false);

        launchProcess(process, masterRequestId, owner, new Long(ownerId), synchronous,
                responseVarName, variableValues, null, showLogs, logWatchPort, liveView);
    }

    private boolean watch;

    protected void launchProcess(WorkflowProcess process, String masterRequestId, String owner,
            Long ownerId, boolean synchronous, String responseVarName,
            Map<String, String> parameters, Long activityId, boolean showLogs, int logWatchPort,
            boolean liveView) {
        List<VariableValue> variableValues = new ArrayList<VariableValue>();
        for (String varName : parameters.keySet()) {
            VariableVO variableVO = process.getVariable(varName);
            if (parameters.get(varName).length() > 0) {
                VariableTypeVO varType = process.getProject().getDataAccess()
                        .getVariableType(variableVO.getVariableType());
                variableValues.add(new VariableValue(variableVO, varType, parameters.get(varName)));
            }
        }
        DesignerProxy designerProxy = process.getProject().getDesignerProxy();
        try {
            if (showLogs || liveView) {
                watch = true;

                LogWatcher logWatcher = designerProxy.getLogWatcher(MdwPlugin.getDisplay());
                if (logWatcher.isRunning()) {
                    MdwPlugin.getDisplay().syncExec(new Runnable() {
                        public void run() {
                            String message = "Live View is already monitoring an existing instance.  Disconnect to monitor new instance?";
                            watch = MessageDialog.openConfirm(
                                    MdwPlugin.getDisplay().getActiveShell(), "Live View", message);
                        }
                    });
                }

                if (watch) {
                    logWatcher.shutdown();
                    logWatcher.setMasterRequestId(masterRequestId);
                    logWatcher.setProcess(process);
                    logWatcher.startup(liveView);
                }
            }
            if (synchronous) {
                String response = designerProxy.launchSynchronousProcess(process, masterRequestId,
                        owner, ownerId, variableValues, responseVarName);
                if (isWriteToConsole())
                    writeToConsole("Process Launch Response", response + "\n");
            }
            else {
                MDWStatusMessage statusMsg = designerProxy.launchProcess(process, masterRequestId,
                        owner, ownerId, variableValues, activityId);
                if (isWriteToConsole())
                    writeToConsole("Process Launch Response", statusMsg.getStatusMessage() + "\n");
            }
        }
        catch (Exception ex) {
            showError(ex, "Launch Process", process.getProject());
        }
    }

    private boolean connectForDebug(WorkflowProject workflowProject,
            ILaunchConfiguration launchConfig) {
        try {
            ILaunchConfigurationWorkingCopy workingCopy = createDebugLaunchConfig(workflowProject,
                    launchConfig);
            ILaunchConfiguration config = findExistingDebugLaunchConfig(workingCopy);
            if (config == null) {
                // no existing found - create a new one
                config = workingCopy.doSave();
            }

            final ILaunchConfiguration debugLaunchConfig = config;

            MdwPlugin.getDisplay().syncExec(new Runnable() {
                public void run() {
                    DebugUITools.launch(debugLaunchConfig, ILaunchManager.DEBUG_MODE);
                }
            });
            return true;
        }
        catch (Exception ex) {
            showError(ex, "Debug Process", workflowProject);
            return false;
        }
    }

    private ILaunchConfigurationWorkingCopy createDebugLaunchConfig(WorkflowProject workflowProject,
            ILaunchConfiguration launchConfig) throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();

        ILaunchConfigurationType configType = launchManager
                .getLaunchConfigurationType("com.centurylink.mdw.plugin.launch.JavaDebug");
        ILaunchConfigurationWorkingCopy wc = configType.newInstance(null,
                launchManager.generateLaunchConfigurationName(workflowProject.getLabel()));

        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                        workflowProject.getSourceProject().getName()));
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
                launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
                        "org.eclipse.jdt.launching.socketAttachConnector"));
        Map<String, String> argDefaults = new HashMap<String, String>();
        argDefaults.put("hostname", workflowProject.isRemote()
                ? workflowProject.getServerSettings().getHome() : "localhost");
        argDefaults.put("port", ProcessLaunchConfiguration.DEFAULT_DEBUG_PORT);
        argDefaults.put("timeout", "20000");
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, launchConfig
                .getAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, argDefaults));
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, false);

        return wc;
    }

    private ILaunchConfiguration findExistingDebugLaunchConfig(
            ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType configType = workingCopy.getType();
        ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(configType);
        String wcDebugProj = workingCopy
                .getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
        String wcVmConnector = workingCopy
                .getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, "");
        Map<?, ?> wcConnectAttrMap = workingCopy.getAttribute(
                IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, new HashMap<String, String>());

        for (ILaunchConfiguration launchConfig : configs) {
            String debugProj = launchConfig
                    .getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
            if (!debugProj.equals(wcDebugProj))
                continue;
            String vmConnector = launchConfig
                    .getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, "");
            if (!vmConnector.equals(wcVmConnector))
                continue;
            Map<?, ?> connectAttrMap = launchConfig.getAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP,
                    new HashMap<String, String>());
            boolean mapMatches = true;
            for (Object key : connectAttrMap.keySet()) {
                if (!"timeout".equals(key)
                        && !connectAttrMap.get(key).equals(wcConnectAttrMap.get(key))) {
                    mapMatches = false;
                    continue;
                }
            }
            if (!mapMatches)
                continue;

            return launchConfig;
        }
        return null;
    }

}
