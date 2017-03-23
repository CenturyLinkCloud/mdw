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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ElementChangeListener;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.convert.DateConverter;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;
import com.centurylink.mdw.plugin.designer.storage.DocumentStorage;
import com.centurylink.mdw.plugin.designer.storage.StorageEditorInput;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessSection extends PropertySection implements IFilter, ElementChangeListener {
    private WorkflowProcess process;

    public WorkflowProcess getProcess() {
        return process;
    }

    private PropertyEditor idPropertyEditor;
    private PropertyEditor namePropertyEditor;
    private PropertyEditor descriptionPropertyEditor;
    private PropertyEditor createDatePropertyEditor;
    private PropertyEditor definitionLinkEditor;

    public void setSelection(WorkflowElement selection) {
        if (process != null)
            process.removeElementChangeListener(this);

        process = (WorkflowProcess) selection;
        process.addElementChangeListener(this);

        idPropertyEditor.setElement(process);
        idPropertyEditor.setValue(process.getIdLabel());

        namePropertyEditor.setElement(process);
        namePropertyEditor.setValue(process.getName());

        descriptionPropertyEditor.setElement(process);
        descriptionPropertyEditor.setValue(process.getDescription());
        descriptionPropertyEditor.setEditable(!process.isReadOnly());

        createDatePropertyEditor.setElement(process);
        createDatePropertyEditor.setValue(process.getCreateDate());

        // avoid triggering dirty state change when reloading combo
        List<DirtyStateListener> dsListeners = new ArrayList<DirtyStateListener>();
        dsListeners.addAll(process.getDirtyStateListeners());
        for (DirtyStateListener dsListener : dsListeners)
            process.removeDirtyStateListener(dsListener);
        List<String> options = new ArrayList<String>();
        options.add("");
        if (process.isInRuleSet())
            options.addAll(getDesignerDataModel().getWorkgroupNames());
        // re-enable dirty state firing
        for (DirtyStateListener dsListener : dsListeners)
            process.addDirtyStateListener(dsListener);

        definitionLinkEditor.setElement(process);
        if (process.hasInstanceInfo())
            definitionLinkEditor.setLabel("Open Process Definition");
        else if (process.isInRuleSet())
            definitionLinkEditor.setLabel("View Definition XML");
        definitionLinkEditor.setVisible(process.hasInstanceInfo() || process.isInRuleSet());
    }

    public void drawWidgets(Composite composite, WorkflowElement selection) {
        process = (WorkflowProcess) selection;

        // id text field
        idPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
        idPropertyEditor.setLabel("ID");
        idPropertyEditor.setWidth(150);
        idPropertyEditor.setReadOnly(true);
        idPropertyEditor.render(composite);

        // name text field
        namePropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
        namePropertyEditor.setLabel("Name");
        namePropertyEditor.setReadOnly(true);
        namePropertyEditor.render(composite);

        // description text area
        descriptionPropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
        descriptionPropertyEditor.setLabel("Description");
        descriptionPropertyEditor.setWidth(475);
        descriptionPropertyEditor.setHeight(100);
        descriptionPropertyEditor.setMultiLine(true);
        descriptionPropertyEditor.setTextLimit(1000);
        descriptionPropertyEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                process.setDescription((String) newValue);
            }
        });
        descriptionPropertyEditor.render(composite);

        // create date read-only text field
        createDatePropertyEditor = new PropertyEditor(process, PropertyEditor.TYPE_TEXT);
        createDatePropertyEditor.setLabel("Created");
        createDatePropertyEditor.setWidth(150);
        createDatePropertyEditor.setValueConverter(new DateConverter());
        createDatePropertyEditor.setReadOnly(true);
        createDatePropertyEditor.render(composite);

        // definition link
        definitionLinkEditor = new PropertyEditor(process, PropertyEditor.TYPE_LINK);
        definitionLinkEditor.setLabel("Definition");
        definitionLinkEditor.addValueChangeListener(new ValueChangeListener() {
            public void propertyValueChanged(Object newValue) {
                openDefinition();
            }
        });
        definitionLinkEditor.render(composite);

    }

    private void openDefinition() {
        if (process.hasInstanceInfo())
            openProcessDefinition(new WorkflowProcess(process));
        else if (process.isInRuleSet())
            openDefinitionXml(process);
    }

    private void openProcessDefinition(WorkflowProcess processVersion) {
        // create a new instance for a new editor
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(processVersion, "mdw.editors.process");
        }
        catch (PartInitException ex) {
            PluginMessages.uiError(getShell(), ex, "Open Process", processVersion.getProject());
        }
    }

    private void openDefinitionXml(final WorkflowProcess processVersion) {
        BusyIndicator.showWhile(MdwPlugin.getDisplay(), new Runnable() {
            public void run() {
                try {
                    WorkflowProject project = processVersion.getProject();
                    IStorage storage = new DocumentStorage(project, processVersion.getLabel(),
                            project.getDataAccess().loadRuleSet(processVersion.getId())
                                    .getRuleSet());
                    IStorageEditorInput input = new StorageEditorInput(storage);
                    IWorkbenchPage page = MdwPlugin.getActivePage();
                    if (page != null)
                        page.openEditor(input, "org.eclipse.ui.DefaultTextEditor");
                }
                catch (Exception ex) {
                    PluginMessages.uiError(ex, "View Definition XML", processVersion.getProject());
                }
            }
        });
    }

    public boolean select(Object toTest) {
        if (toTest == null || !(toTest instanceof WorkflowProcess))
            return false;

        return true;
    }

    public void elementChanged(ElementChangeEvent ece) {
        if (ece.getElement().equals(process)) {
            if (ece.getChangeType().equals(ChangeType.RENAME)) {
                if (!namePropertyEditor.getValue().equals(ece.getNewValue()))
                    namePropertyEditor.setValue(ece.getNewValue().toString());
                notifyLabelChange();
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (process != null)
            process.removeElementChangeListener(this);
    }

}
