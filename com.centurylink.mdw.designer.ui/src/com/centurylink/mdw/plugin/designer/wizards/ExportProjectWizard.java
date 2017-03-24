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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlOptions;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument;
import com.centurylink.mdw.workflow.ConfigManagerProjectsDocument.ConfigManagerProjects;
import com.centurylink.mdw.workflow.EnvironmentDB;
import com.centurylink.mdw.workflow.ManagedNode;
import com.centurylink.mdw.workflow.WorkflowApplication;
import com.centurylink.mdw.workflow.WorkflowEnvironment;

public class ExportProjectWizard extends Wizard implements IExportWizard {
    private ExportProjectPage exportProjectPage;

    private IWorkbench workbench;

    public IWorkbench getWorkbench() {
        return workbench;
    }

    private List<WorkflowProject> projectsToExport;

    public List<WorkflowProject> getProjectsToExport() {
        return projectsToExport;
    }

    public void setProjectsToExport(List<WorkflowProject> projects) {
        this.projectsToExport = projects;
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
        projectsToExport = new ArrayList<WorkflowProject>();
        if (selection != null) {
            for (Object element : selection.toArray()) {
                if (element instanceof WorkflowProject && ((WorkflowProject) element).isRemote())
                    projectsToExport.add((WorkflowProject) element);
            }
        }
    }

    @Override
    public boolean performFinish() {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                ConfigManagerProjectsDocument doc = ConfigManagerProjectsDocument.Factory
                        .newInstance();
                ConfigManagerProjects configMgrProjects = doc.addNewConfigManagerProjects();

                try {
                    for (WorkflowProject workflowProject : projectsToExport) {
                        WorkflowApplication workflowApp = configMgrProjects.addNewWorkflowApp();
                        workflowApp.setName("");
                        workflowApp.setMalAppName("");
                        workflowApp.setEcomAcronym("");
                        workflowApp.setWebContextRoot(workflowProject.getWebContextRoot());
                        WorkflowEnvironment workflowEnv = workflowApp.addNewEnvironment();
                        workflowEnv.setName(workflowProject.getName());
                        workflowEnv.setOwner(workflowProject.getAuthor());
                        ManagedNode managedNode = workflowEnv.addNewManagedServer();
                        managedNode.setHost(workflowProject.getServerSettings().getHost());
                        managedNode.setPort(new BigInteger(
                                String.valueOf(workflowProject.getServerSettings().getPort())));
                        EnvironmentDB envDb = workflowEnv.addNewEnvironmentDb();
                        envDb.setJdbcUrl(workflowProject.getMdwDataSource().getJdbcUrl());
                        envDb.setUser(workflowProject.getMdwDataSource().getDbUser());
                        envDb.setPassword(CryptUtil
                                .encrypt(workflowProject.getMdwDataSource().getDbPassword()));
                    }
                    String xml = doc.xmlText(
                            new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));
                    String filepath = exportProjectPage.getFileName();
                    writeTextFile(filepath, xml);
                }
                catch (Exception ex) {
                    PluginMessages.uiError(getShell(), ex, "Export Projects");
                }
            }
        });
        return true;
    }

    @Override
    public void addPages() {
        exportProjectPage = new ExportProjectPage();
        addPage(exportProjectPage);
    }

    public void writeTextFile(String fullPathFilename, String xml) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fullPathFilename));
        writer.write(xml);
        writer.close();
    }
}
