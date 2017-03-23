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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.actions.WorkflowElementActionHandler;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProjectMonitoringSection extends PropertySection implements IFilter {
    private WorkflowProject project;

    public WorkflowProject getProject() {
        return project;
    }

    private PropertyEditor jmxPortEditor;
    private PropertyEditor visualVmIdEditor;
    private PropertyEditor visualVmLaunchEditor;
    private PropertyEditor jConsoleLaunchEditor;
    private PropertyEditor filePanelLaunchEditor;

    public void setSelection(WorkflowElement selection) {
        project = (WorkflowProject) selection;

        visualVmLaunchEditor.setElement(project);
        jConsoleLaunchEditor.setElement(project);
        filePanelLaunchEditor.setElement(project);

        jmxPortEditor.setElement(project);
        jmxPortEditor.setValue(project.getJmxPort());
        jmxPortEditor.setEditable(!project.isReadOnly());

        visualVmIdEditor.setElement(project);
        visualVmIdEditor.setValue(project.getVisualVmId());
        visualVmIdEditor.setEditable(!project.isReadOnly());

        if (project.isRemote()) {
            visualVmIdEditor.setVisible(false);
            jmxPortEditor.setVisible(true);
            jmxPortEditor.setFocus();
        }
        else {
            jmxPortEditor.setVisible(false);
            visualVmIdEditor.setVisible(true);
            visualVmIdEditor.setFocus();
        }
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        project = (WorkflowProject) selection;

        // launch visualvm link
        visualVmLaunchEditor = new PropertyEditor(project, PropertyEditor.TYPE_LINK);
        visualVmLaunchEditor.setLabel("Java VisualVM");
        visualVmLaunchEditor.setHeight(20);
        visualVmLaunchEditor.setFont(new FontData("Tahoma", 8, SWT.BOLD));
        visualVmLaunchEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                launchVisualVm();
            }
        });
        visualVmLaunchEditor.render(composite);
        visualVmLaunchEditor.getLabelWidget().setText("Monitoring Tools:");

        // launch jconsole link
        jConsoleLaunchEditor = new PropertyEditor(project, PropertyEditor.TYPE_LINK);
        jConsoleLaunchEditor.setLabel(" <A>JConsole</A>  (for older VMs)");
        jConsoleLaunchEditor.setHeight(20);
        jConsoleLaunchEditor.setFont(new FontData("Tahoma", 8, SWT.BOLD));
        jConsoleLaunchEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                launchJConsole();
            }
        });
        jConsoleLaunchEditor.render(composite);

        // launch filepanel link
        filePanelLaunchEditor = new PropertyEditor(project, PropertyEditor.TYPE_LINK);
        filePanelLaunchEditor.setLabel("FilePanel");
        filePanelLaunchEditor.setHeight(20);
        filePanelLaunchEditor.setFont(new FontData("Tahoma", 8, SWT.BOLD));
        filePanelLaunchEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                launchFilePanel();
            }
        });
        filePanelLaunchEditor.render(composite);

        // jmx port text field
        jmxPortEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
        jmxPortEditor.setLabel("Remote JMX Port");
        jmxPortEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                project.setPersistentProperty(WorkflowProject.MDW_REMOTE_JMX_PORT,
                        newValue == null ? null : newValue.toString());
            }
        });
        jmxPortEditor.render(composite);

        // visual vm id text field
        visualVmIdEditor = new PropertyEditor(project, PropertyEditor.TYPE_TEXT);
        visualVmIdEditor.setLabel("VisualVM ID");
        visualVmIdEditor.setComment("(Should match server \"-Dvisualvm.id\" VM argument)");
        visualVmIdEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                project.setPersistentProperty(WorkflowProject.MDW_VISUALVM_ID,
                        newValue == null ? null : newValue.toString());
            }
        });
        visualVmIdEditor.render(composite);
    }

    public boolean select(Object toTest) {
        return (toTest != null && (toTest instanceof WorkflowProject));
    }

    private void launchVisualVm() {
        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        actionHandler.launchSwing(project, "Java VisualVM");
    }

    private void launchJConsole() {
        WorkflowElementActionHandler actionHandler = new WorkflowElementActionHandler();
        actionHandler.launchSwing(project, "JConsole");
    }

    private void launchFilePanel() {
        ServerSettings ss = project.getServerSettings();
        String path;
        if (project.checkRequiredVersion(5, 5))
            path = project.getMdwHubContextRoot() + "/system/filepanel/index.jsf";
        else
            path = project.getWebContextRoot() + "/filepanel/index.jsf";

        Program.launch("http://" + ss.getHost() + ":" + ss.getPort() + "/" + path);
    }
}