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
package com.centurylink.mdw.plugin.designer.wizards;

import java.io.File;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.TaskTemplate;
import com.centurylink.mdw.task.Attribute;
import com.centurylink.mdw.task.TaskTemplateDocument;

public class NewTaskTemplateWizard extends WorkflowAssetWizard {
    public static final String WIZARD_ID = "mdw.designer.new.taskTemplate";

    @SuppressWarnings("restriction")
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection,
                new TaskTemplate(org.eclipse.ui.internal.ide.IDEWorkbenchPlugin.getDefault()
                        .getDialogSettings().getSection("NewWizardAction")
                        .get(TaskTemplate.TASK_TYPE)));
    }

    @Override
    public boolean performFinish() {
        TaskTemplate taskTemplate = (TaskTemplate) getWorkflowAsset();
        try {
            TaskTemplateDocument doc;
            if (isImportFile()) {
                // load from selected file
                doc = TaskTemplateDocument.Factory.parse(new File(getImportFilePath()));
                com.centurylink.mdw.task.TaskTemplate template = doc.getTaskTemplate();
                // minimal validation (not checking if autoform/custom matches
                // selection)
                if (template.getLogicalId() == null)
                    throw new XmlException("Task template missing logicalId");
                else if (template.getCategory() == null)
                    throw new XmlException("Task template missing category");
                else if (template.getName() == null || template.getName().isEmpty())
                    throw new XmlException("Task template missing name");
            }
            else {
                doc = TaskTemplateDocument.Factory.newInstance();
                com.centurylink.mdw.task.TaskTemplate template = doc.addNewTaskTemplate();
                String taskName = taskTemplate.getName().substring(0,
                        taskTemplate.getName().length() - 5);
                template.setLogicalId(taskName.replace(' ', '_'));
                template.setCategory("GEN");
                template.setName(taskName);
                if ("AUTOFORM".equals(taskTemplate.getLanguage())) {
                    Attribute form = template.addNewAttribute();
                    form.setName("FormName");
                    form.setStringValue("Autoform");
                }
            }
            XmlOptions xmlOptions = new XmlOptions().setSaveAggressiveNamespaces();
            xmlOptions.setSavePrettyPrint().setSavePrettyPrintIndent(2);
            taskTemplate.setContent(doc.xmlText(xmlOptions));
            String templateName = taskTemplate.getName();
            taskTemplate.setTaskVO(new TaskVO(doc.getTaskTemplate()));
            taskTemplate.setName(templateName);
            taskTemplate.getTaskVO().setPackageName(taskTemplate.getPackage().getName());
            DesignerProxy designerProxy = taskTemplate.getProject().getDesignerProxy();
            designerProxy.createNewTaskTemplate(taskTemplate);
            if (designerProxy.getRunnerStatus().equals(RunnerStatus.SUCCESS)) {
                taskTemplate.openFile(new NullProgressMonitor());
                taskTemplate.addElementChangeListener(taskTemplate.getProject());
                taskTemplate.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, taskTemplate);
                taskTemplate.setVersion(1);
                taskTemplate.fireElementChangeEvent(ChangeType.VERSION_CHANGE, taskTemplate);
                DesignerPerspective.promptForShowPerspective(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow(), taskTemplate);
                return true;
            }
            else {
                return false;
            }
        }
        catch (Exception ex) {
            PluginMessages.uiError(getShell(), ex, "Create " + taskTemplate.getTitle(),
                    taskTemplate.getProject());
            return false;
        }
    }
}