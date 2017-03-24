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
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.project.ProjectPersist;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProjectConnectionsSettings extends PropertySection
        implements IFilter, ElementChangeListener {
    private WorkflowProject project;

    public WorkflowProject getProject() {
        return project;
    }

    private PropertyEditor javaOptionsTextField;
    private PropertyEditor debugModeCheckbox;
    private PropertyEditor debugPortTextField;
    private PropertyEditor debugSuspendCheckbox;
    private PropertyEditor separatorOne;
    private PropertyEditor logWatchHostTextField;
    private PropertyEditor logWatchPortTextField;
    private PropertyEditor logWatchTimeoutSpinner;
    private PropertyEditor separatorTwo;
    private PropertyEditor stubServerHostTextField;
    private PropertyEditor stubServerPortTextField;
    private PropertyEditor stubServerTimeoutSpinner;

    public void setSelection(WorkflowElement selection) {
        if (project != null)
            project.removeElementChangeListener(this);

        project = (WorkflowProject) selection;
        project.addElementChangeListener(this);

        if (javaOptionsTextField != null)
            javaOptionsTextField.dispose();
        if (debugModeCheckbox != null)
            debugModeCheckbox.dispose();
        if (debugPortTextField != null)
            debugPortTextField.dispose();
        if (debugSuspendCheckbox != null)
            debugSuspendCheckbox.dispose();
        if (separatorOne != null)
            separatorOne.dispose();
        if (logWatchHostTextField != null)
            logWatchHostTextField.dispose();
        if (logWatchPortTextField != null)
            logWatchPortTextField.dispose();
        if (logWatchTimeoutSpinner != null)
            logWatchTimeoutSpinner.dispose();
        if (separatorTwo != null)
            separatorTwo.dispose();
        if (stubServerHostTextField != null)
            stubServerHostTextField.dispose();
        if (stubServerPortTextField != null)
            stubServerPortTextField.dispose();
        if (stubServerTimeoutSpinner != null)
            stubServerTimeoutSpinner.dispose();

        if (!project.isRemote() && !project.isOsgi()) {
            javaOptionsTextField = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
            javaOptionsTextField.setLabel("Java Options");
            javaOptionsTextField.setWidth(400);
            javaOptionsTextField.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    String javaOptions = newValue == null ? null : String.valueOf(newValue);
                    if (javaOptions != null && javaOptions.trim().length() == 0)
                        javaOptions = null;
                    project.getServerSettings().setJavaOptions(javaOptions);
                    saveProjectPrefs();
                }
            });
            javaOptionsTextField.render(composite);
            javaOptionsTextField.setValue(project.getServerSettings().getJavaOptions());
            javaOptionsTextField.setEditable(!project.isReadOnly());

            debugModeCheckbox = new PropertyEditor(project, PropertyEditor.TYPE_CHECKBOX);
            debugModeCheckbox.setLabel("Server Runner Debug");
            debugModeCheckbox.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    Boolean value = (Boolean) newValue;
                    project.getServerSettings().setDebug(value);
                    enableDebugControls(value);
                    saveProjectPrefs();
                }
            });
            debugModeCheckbox.render(composite);
            debugModeCheckbox.setValue(project.getServerSettings().isDebug());
            debugModeCheckbox.setEditable(!project.isReadOnly());

            debugPortTextField = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
            debugPortTextField.setLabel("        Debug Port");
            debugPortTextField.setWidth(100);
            debugPortTextField.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    int port = Integer.parseInt(String.valueOf(newValue));
                    project.getServerSettings().setDebugPort(port);
                    saveProjectPrefs();
                }
            });
            debugPortTextField.render(composite);
            debugPortTextField.setValue(project.getServerSettings().getDebugPort());
            debugPortTextField.setEditable(!project.isReadOnly());

            debugSuspendCheckbox = new PropertyEditor(project, PropertyEditor.TYPE_CHECKBOX);
            debugSuspendCheckbox.setLabel("        Suspend");
            debugSuspendCheckbox.addValueChangeListener(new ValueChangeListener() {
                public void propertyValueChanged(Object newValue) {
                    Boolean value = (Boolean) newValue;
                    project.getServerSettings().setSuspend(value);
                    saveProjectPrefs();
                }
            });
            debugSuspendCheckbox.render(composite);
            debugSuspendCheckbox.setValue(project.getServerSettings().isSuspend());
            debugSuspendCheckbox.setEditable(!project.isReadOnly());

            separatorOne = new PropertyEditor(project, PropertyEditor.TYPE_SEPARATOR);
            separatorOne.render(composite);
        }

        logWatchHostTextField = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
        logWatchHostTextField.setLabel("Log Watcher Host");
        logWatchHostTextField.setWidth(225);
        logWatchHostTextField.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                project.getServerSettings().setLogWatcherHost(String.valueOf(newValue));
                saveProjectPrefs();
            }
        });
        logWatchHostTextField.render(composite);
        logWatchHostTextField.setValue(project.getServerSettings().getLogWatcherHost());
        logWatchHostTextField.setEditable(false);

        logWatchPortTextField = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
        logWatchPortTextField.setLabel("        Port");
        logWatchPortTextField.setWidth(100);
        logWatchPortTextField.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                int port = Integer.parseInt(String.valueOf(newValue));
                project.getServerSettings().setLogWatcherPort(port);
                saveProjectPrefs();
            }
        });
        logWatchPortTextField.render(composite);
        logWatchPortTextField.setValue(project.getServerSettings().getLogWatcherPort());
        logWatchPortTextField.setEditable(!project.isReadOnly());

        logWatchTimeoutSpinner = new PropertyEditor(project, PropertyEditor.TYPE_SPINNER);
        logWatchTimeoutSpinner.setLabel("        Timeout");
        logWatchTimeoutSpinner.setWidth(50);
        logWatchTimeoutSpinner.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                int timeout = Integer.parseInt(String.valueOf(newValue));
                project.getServerSettings().setLogWatcherTimeout(timeout);
                saveProjectPrefs();
            }
        });
        logWatchTimeoutSpinner.render(composite);
        logWatchTimeoutSpinner.setValue(project.getServerSettings().getLogWatcherTimeout());
        logWatchTimeoutSpinner.setEditable(!project.isReadOnly());

        separatorTwo = new PropertyEditor(project, PropertyEditor.TYPE_SEPARATOR);
        separatorTwo.render(composite);

        stubServerHostTextField = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
        stubServerHostTextField.setLabel("Stub Server Host");
        stubServerHostTextField.setWidth(225);
        stubServerHostTextField.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                project.getServerSettings().setStubServerHost(String.valueOf(newValue));
                saveProjectPrefs();
            }
        });
        stubServerHostTextField.render(composite);
        stubServerHostTextField.setValue(project.getServerSettings().getStubServerHost());
        stubServerHostTextField.setEditable(false);

        stubServerPortTextField = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
        stubServerPortTextField.setLabel("        Port");
        stubServerPortTextField.setWidth(100);
        stubServerPortTextField.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                int port = Integer.parseInt(String.valueOf(newValue));
                project.getServerSettings().setStubServerPort(port);
                saveProjectPrefs();
            }
        });
        stubServerPortTextField.render(composite);
        stubServerPortTextField.setValue(project.getServerSettings().getStubServerPort());
        stubServerPortTextField.setEditable(!project.isReadOnly());

        stubServerTimeoutSpinner = new PropertyEditor(project, PropertyEditor.TYPE_SPINNER);
        stubServerTimeoutSpinner.setLabel("        Timeout");
        stubServerTimeoutSpinner.setWidth(50);
        stubServerTimeoutSpinner.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                int timeout = Integer.parseInt(String.valueOf(newValue));
                project.getServerSettings().setStubServerTimeout(timeout);
                saveProjectPrefs();
            }
        });
        stubServerTimeoutSpinner.render(composite);
        stubServerTimeoutSpinner.setValue(project.getServerSettings().getStubServerTimeout());
        stubServerTimeoutSpinner.setEditable(!project.isReadOnly());

        composite.layout(true);
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        project = (WorkflowProject) selection;

        // controls are rendered dynamically in setSelection()
    }

    public boolean select(Object toTest) {
        return (toTest != null && (toTest instanceof WorkflowProject));
    }

    public void elementChanged(ElementChangeEvent ece) {
        if (ece.getChangeType().equals(ChangeType.SETTINGS_CHANGE)) {
            if (ece.getNewValue() instanceof ServerSettings) {
                ServerSettings serverSettings = (ServerSettings) ece.getNewValue();
                if (javaOptionsTextField != null
                        && !javaOptionsTextField.getValue().equals(serverSettings.getJavaOptions()))
                    javaOptionsTextField.setValue(serverSettings.getJavaOptions());
                if (debugModeCheckbox != null && !debugModeCheckbox.getValue()
                        .equals(String.valueOf(serverSettings.isDebug())))
                    debugModeCheckbox.setValue(serverSettings.isDebug());
                if (debugPortTextField != null && !debugPortTextField.getValue()
                        .equals(String.valueOf(serverSettings.getDebugPort())))
                    debugPortTextField.setValue(serverSettings.getDebugPort());
                if (debugSuspendCheckbox != null && !debugSuspendCheckbox.getValue()
                        .equals(String.valueOf(serverSettings.isSuspend())))
                    debugSuspendCheckbox.setValue(serverSettings.isSuspend());
                if (!logWatchHostTextField.getValue().equals(serverSettings.getLogWatcherHost()))
                    logWatchHostTextField.setValue(serverSettings.getLogWatcherHost());
                if (!logWatchPortTextField.getValue()
                        .equals(String.valueOf(serverSettings.getLogWatcherPort())))
                    logWatchPortTextField.setValue(serverSettings.getLogWatcherPort());
                if (!String.valueOf(serverSettings.getLogWatcherTimeout())
                        .equals(logWatchTimeoutSpinner.getValue()))
                    logWatchTimeoutSpinner.setValue(serverSettings.getLogWatcherTimeout());
                if (!stubServerHostTextField.getValue().equals(serverSettings.getStubServerHost()))
                    stubServerHostTextField.setValue(serverSettings.getStubServerHost());
                if (!stubServerPortTextField.getValue()
                        .equals(String.valueOf(serverSettings.getStubServerPort())))
                    stubServerPortTextField.setValue(serverSettings.getStubServerPort());
                if (!String.valueOf(serverSettings.getStubServerTimeout())
                        .equals(stubServerTimeoutSpinner.getValue()))
                    stubServerTimeoutSpinner.setValue(serverSettings.getStubServerTimeout());
            }
        }
    }

    private void enableDebugControls(boolean enabled) {
        if (!enabled) {
            debugPortTextField.setValue("");
            debugSuspendCheckbox.setValue(false);
        }
        else {
            if (project.getServerSettings().getDebugPort() == 0)
                debugPortTextField.setValue(8500);
            else
                debugPortTextField.setValue(project.getServerSettings().getDebugPort());
        }
        debugPortTextField.setEditable(enabled);
        debugSuspendCheckbox.setEditable(enabled);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (project != null)
            project.removeElementChangeListener(this);
    }

    private void saveProjectPrefs() {
        new ProjectPersist(project).saveProjectPrefs(project.getSourceProject());
    }
}
