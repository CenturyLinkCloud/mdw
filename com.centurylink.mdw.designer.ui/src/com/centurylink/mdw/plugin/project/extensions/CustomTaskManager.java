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
package com.centurylink.mdw.plugin.project.extensions;

import java.io.IOException;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetProjectCreationDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.servlet.ui.project.facet.WebProjectWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties.FacetDataModelMap;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.preferences.model.MdwSettings;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.plugin.project.assembly.ProjectInflator;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class CustomTaskManager extends ExtensionModule {
    @Override
    public void readConfigElement(String qName, Map<String, String> attrs,
            WorkflowProject project) {
        if (qName.equals("webProject")) {
            project.setWebProjectName(attrs.get("name"));
            String deployDir = attrs.get("deployDir");
            if (deployDir != null)
                project.setDeployDir(deployDir);
            if (!project.getExtensionModules().contains(this))
                project.getExtensionModules().add(this);
        }
    }

    @Override
    public String writeConfigElement(WorkflowProject project) {
        return "  <webProject name=\"" + project.getWebProjectName() + "\""
                + (project.getDeployDir() == null ? ""
                        : " deployDir=\"" + project.getDeployDir() + "\"")
                + " />\n";
    }

    @Override
    public boolean addUi(WorkflowProject project, Shell shell) {
        return launchWebProjectWizard(shell, project);
    }

    @Override
    public boolean addTo(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException {
        monitor.worked(5);

        try {
            updateApplicationXml(project, project.getWebProjectName() + ".war",
                    project.getWebProjectName(), new SubProgressMonitor(monitor, 5));

            MdwSettings mdwSettings = MdwPlugin.getSettings();
            ProjectInflator inflator = new ProjectInflator(project, mdwSettings);
            inflator.generateWebArtifacts(new SubProgressMonitor(monitor, 5));
            ProjectUpdater updater = new ProjectUpdater(project, mdwSettings);
            ProjectConfigurator configurator = new ProjectConfigurator(project, mdwSettings);
            updater.updateWebProjectJars(new SubProgressMonitor(monitor, 30));
            configurator.createWebProjectSourceCodeAssociations(null, monitor);
            monitor.worked(5);
        }
        catch (Exception ex) {
            throw new ExtensionModuleException(ex.getMessage(), ex);
        }

        return true;
    }

    @Override
    public boolean removeFrom(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException {
        monitor.worked(5);

        try {
            updateApplicationXml(project, "MDWTaskManagerWeb.war",
                    project.getSourceProjectName() + "TaskManager",
                    new SubProgressMonitor(monitor, 10));

            monitor.worked(20);

            IProjectDescription projDesc = project.getEarProject().getDescription();
            IProject[] oldRefs = projDesc.getReferencedProjects();
            int toRemove = -1;
            for (int i = 0; i < oldRefs.length; i++) {
                if (oldRefs[i].getName().equals(project.getWebProjectName()))
                    toRemove = i;
            }
            if (toRemove != -1) {
                IProject[] newRefs = new IProject[oldRefs.length - 1];
                for (int i = 0; i < newRefs.length; i++) {
                    if (i < toRemove)
                        newRefs[i] = oldRefs[i];
                    else
                        newRefs[i] = oldRefs[i + 1];
                }
            }
        }
        catch (Exception ex) {
            throw new ExtensionModuleException(ex.getMessage(), ex);
        }
        monitor.worked(25);
        return true;
    }

    @Override
    public boolean removeUi(WorkflowProject project, Shell shell) throws ExtensionModuleException {
        String message = "Custom Task Manager project references will be removed.  (Please delete web project '"
                + project.getWebProjectName() + "' by hand.)";
        return MessageDialog.openConfirm(shell, "Remove Custom Task Manager", message);
    }

    private boolean launchWebProjectWizard(Shell shell, WorkflowProject workflowProject) {
        WebProjectWizard webProjectWizard = new WebProjectWizard();
        IDataModel dataModel = webProjectWizard.getDataModel();
        workflowProject.setWebProjectName(workflowProject.getSourceProjectName() + "TaskManager");
        dataModel.setStringProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME,
                workflowProject.getWebProjectName());
        dataModel.setBooleanProperty(IJ2EEFacetProjectCreationDataModelProperties.ADD_TO_EAR, true);
        dataModel.setStringProperty(IJ2EEFacetProjectCreationDataModelProperties.EAR_PROJECT_NAME,
                workflowProject.getEarProjectName());
        WizardDialog dialog = new WizardDialog(shell, webProjectWizard);

        FacetDataModelMap facetDmMap = (FacetDataModelMap) webProjectWizard.getDataModel()
                .getProperty(IFacetProjectCreationDataModelProperties.FACET_DM_MAP);
        // web facet data model props
        IDataModel webFacetDataModel = (IDataModel) facetDmMap.get("jst.web");
        webFacetDataModel
                .setStringProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER, "web");

        if (dialog.open() != Dialog.OK)
            return false;

        // in case user changed the web project name
        String webProjectName = (String) webProjectWizard.getDataModel()
                .getProperty(IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME);
        workflowProject.setWebProjectName(webProjectName);
        return true;
    }

    private void updateApplicationXml(WorkflowProject project, String webUri, String contextRoot,
            IProgressMonitor monitor) throws XmlException, CoreException, IOException {
        monitor.subTask("Update application deployment descriptor");

        String namespace = "http://java.sun.com/xml/ns/j2ee";
        QName id = new QName("id");
        String errorMsg = "Update failed for EAR content META-INF/application.xml.";
        XmlCursor xmlCursor = null;
        try {
            IFile appXmlFile = project.getEarContentFolder().getFile("META-INF/application.xml");
            XmlObject xmlBean = XmlObject.Factory.parse(appXmlFile.getContents());
            xmlCursor = xmlBean.newCursor();
            xmlCursor.toChild(0); // document
            if (!xmlCursor.toChild(namespace, "module")) {
                namespace = "http://java.sun.com/xml/ns/javaee";
                xmlCursor.toChild(namespace, "module");
            }
            boolean found = true;
            while (found && !"MDWTaskManagerWeb".equals(xmlCursor.getAttributeText(id)))
                found = xmlCursor.toNextSibling(namespace, "module");

            if (!found)
                throw new XmlException(
                        errorMsg + "\nNo 'module' element found with id='MDWTaskManagerWeb'");

            if (!xmlCursor.toChild(namespace, "web"))
                throw new XmlException(
                        errorMsg + "\nMissing 'web' subelement under module 'MDWTaskManagerWeb'");

            if (!xmlCursor.toChild(namespace, "web-uri"))
                throw new XmlException(errorMsg
                        + "\nMissing 'web-uri' subelement under module 'MDWTaskManagerWeb'");

            xmlCursor.setTextValue(webUri);

            xmlCursor.toParent();
            if (!xmlCursor.toChild(namespace, "context-root"))
                throw new XmlException(errorMsg
                        + "\nMissing 'context-root' subelement under module 'MDWTaskManagerWeb'");

            xmlCursor.setTextValue(contextRoot);

            XmlOptions xmlOptions = new XmlOptions();
            xmlOptions.setUseDefaultNamespace();
            PluginUtil.writeFile(appXmlFile, xmlBean.xmlText(xmlOptions), monitor);
        }
        finally {
            if (xmlCursor != null)
                xmlCursor.dispose();
        }
    }

    @Override
    public boolean update(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException {
        monitor.worked(5);

        try {
            ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
            updater.updateWebProjectJars(new SubProgressMonitor(monitor, 90));
            monitor.worked(5);
        }
        catch (Exception ex) {
            throw new ExtensionModuleException(ex.getMessage(), ex);
        }

        return true;
    }

    @Override
    public boolean select(Object object) {
        WorkflowProject workflowProject = (WorkflowProject) object;
        return !workflowProject.isCloudProject();
    }
}
