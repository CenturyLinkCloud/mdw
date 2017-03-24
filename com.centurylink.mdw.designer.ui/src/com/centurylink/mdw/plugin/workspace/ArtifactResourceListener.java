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

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.editors.ProcessEditor;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.value.ArtifactEditorValueProvider;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.designer.display.Node;

public class ArtifactResourceListener implements IResourceChangeListener {
    private WorkflowElement element;

    public WorkflowElement getElement() {
        return element;
    }

    private ArtifactEditorValueProvider valueProvider;

    private IFile tempFile;

    public IFile getTempFile() {
        return tempFile;
    }

    public ArtifactResourceListener(WorkflowElement element,
            ArtifactEditorValueProvider valueProvider, IFile tempFile) {
        this.element = element;
        this.valueProvider = valueProvider;
        this.tempFile = tempFile;
    }

    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            IResourceDelta rootDelta = event.getDelta();
            IResourceDelta artifactDelta = rootDelta.findMember(tempFile.getFullPath());
            if (artifactDelta != null && artifactDelta.getKind() == IResourceDelta.CHANGED
                    && (artifactDelta.getFlags() & IResourceDelta.CONTENT) != 0) {
                // the file has been changed
                final Display display = Display.getCurrent();
                if (display != null) {
                    display.syncExec(new Runnable() {
                        public void run() {
                            byte[] newValue = PluginUtil.readFile(tempFile);
                            String attrVal = valueProvider.isBinary() ? encodeBase64(newValue)
                                    : new String(newValue);

                            if (getElement() instanceof Activity
                                    || getElement() instanceof WorkflowProcess) {
                                WorkflowProcess processVersion = null;
                                if (getElement() instanceof Activity) {
                                    Activity activity = (Activity) getElement();
                                    activity.setAttribute(valueProvider.getAttributeName(),
                                            attrVal);
                                    processVersion = activity.getProcess();
                                }
                                else {
                                    processVersion = (WorkflowProcess) getElement();
                                    processVersion.setAttribute(valueProvider.getAttributeName(),
                                            attrVal);
                                }
                                processVersion.fireDirtyStateChanged(true);
                                ProcessEditor processEditor = findProcessEditor(processVersion);
                                if (processEditor == null) {
                                    try {
                                        processEditor = openProcessEditor(processVersion);
                                        IEditorPart tempFileEditor = findTempFileEditor(tempFile);
                                        if (tempFileEditor != null)
                                            processEditor.addActiveScriptEditor(tempFileEditor);
                                    }
                                    catch (PartInitException ex) {
                                        PluginMessages.uiError(display.getActiveShell(), ex,
                                                "Open Process", processVersion.getProject());
                                        return;
                                    }
                                }
                                processVersion = processEditor.getProcess();
                                if (processVersion.isReadOnly()) {
                                    WorkflowProject workflowProject = getElement().getProject();
                                    PluginMessages.uiMessage(
                                            "Process for '" + getElement().getName()
                                                    + "' in workflow project '"
                                                    + workflowProject.getName() + "' is Read Only.",
                                            "Not Updated", workflowProject,
                                            PluginMessages.INFO_MESSAGE);
                                    return;
                                }

                                if (getElement() instanceof Activity) {
                                    Activity activity = (Activity) getElement();
                                    // the activity the attribute was set on
                                    // above may be a holdover from a
                                    // previously-opened process version
                                    for (Node node : processEditor.getProcessCanvasWrapper()
                                            .getFlowchartPage().getProcess().nodes) {
                                        if (activity.getLogicalId() != null
                                                && activity.getLogicalId()
                                                        .equals(node.getAttribute("LOGICAL_ID"))) {
                                            node.setAttribute(valueProvider.getAttributeName(),
                                                    attrVal);
                                            ActivityImpl actImpl = processVersion.getProject()
                                                    .getActivityImpl(
                                                            node.nodet.getImplementorClassName());
                                            element = new Activity(node, processVersion, actImpl);
                                        }
                                    }
                                    activity.fireDirtyStateChanged(true);
                                }
                                processEditor.dirtyStateChanged(true);

                                valueProvider.afterTempFileSaved();

                                // process editor is open
                                String message = valueProvider.getArtifactTypeDescription()
                                        + " temporary file has been saved locally; however, you must still save the process for the changes to be persisted.";
                                String toggleMessage = "Don't show me this message again.";
                                IPreferenceStore prefsStore = MdwPlugin.getDefault()
                                        .getPreferenceStore();
                                String prefsKey = "Mdw" + valueProvider.getArtifactTypeDescription()
                                        + "SuppressSaveNag";
                                if (!prefsStore.getBoolean(prefsKey)) {
                                    MessageDialogWithToggle dialog = MessageDialogWithToggle
                                            .openInformation(display.getActiveShell(),
                                                    "Artifact Save", message, toggleMessage, false,
                                                    null, null);
                                    prefsStore.setValue(prefsKey, dialog.getToggleState());
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private ProcessEditor findProcessEditor(WorkflowProcess processVersion) {
        return (ProcessEditor) MdwPlugin.getActivePage().findEditor(processVersion);
    }

    private ProcessEditor openProcessEditor(WorkflowProcess processVersion)
            throws PartInitException {
        return (ProcessEditor) MdwPlugin.getActivePage().openEditor(processVersion,
                "mdw.editors.process");
    }

    private IEditorPart findTempFileEditor(IFile tempFile) {
        return MdwPlugin.getActivePage().findEditor(new FileEditorInput(tempFile));
    }

    private String encodeBase64(byte[] inputBytes) {
        return new String(Base64.encodeBase64(inputBytes));
    }
}
