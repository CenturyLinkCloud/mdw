/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.value;

import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.dialogs.MdwInputDialog;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.GradleBuildFile;
import com.centurylink.mdw.plugin.project.model.OsgiBuildFile;
import com.centurylink.mdw.plugin.project.model.MavenBuildFile;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.common.utilities.FileHelper;

public abstract class ArtifactEditorValueProvider {
    private WorkflowElement element;

    public WorkflowElement getElement() {
        return element;
    }

    public WorkflowProject getProject() {
        return element.getProject();
    }

    public ArtifactEditorValueProvider(WorkflowElement element) {
        this.element = element;
    }

    public IFolder getTempFolder() {
        return getProject().getSourceProject()
                .getFolder(MdwPlugin.getSettings().getTempResourceLocation());
    }

    public String getTempFileName() {
        String ext = WorkflowElement.getArtifactFileExtensions().get(getLanguage());
        if (element instanceof Activity) {
            String id = ((Activity) element).getLogicalId();
            return FileHelper.stripDisallowedFilenameChars(element.getName()) + "_" + id + ext;
        }
        else {
            return FileHelper.stripDisallowedFilenameChars(element.getLabel() + ext);
        }
    }

    public boolean isBinary() {
        return false;
    }

    /**
     * Override to perform actions before the temp file is opened.
     * 
     * @return false if opening should be aborted
     */
    public boolean beforeTempFileOpened() {
        return true;
    }

    /**
     * Override to show views, etc. after temp file is opened.
     */
    public void afterTempFileOpened(IEditorPart tempFileEditor) {
    }

    public void afterTempFileSaved() {
    }

    public String getBundleSymbolicName() {
        if (getProject().isRemote() || !getProject().isOsgi())
            return null;

        try {
            OsgiBuildFile buildFile = new GradleBuildFile(getProject().getSourceProject());
            if (!buildFile.exists())
                buildFile = new MavenBuildFile(getProject().getSourceProject()); // fall
                                                                                 // back
                                                                                 // to
                                                                                 // pom.xml
            if (buildFile.exists()) {
                return buildFile.parseSymbolicName();
            }
            else {
                PluginMessages.log(
                        "Neither build.gradle nor pom.xml was found for determining bundle symbolic name");
            }
        }
        catch (Exception ex) {
            PluginMessages.log(ex);
            String msg = "Error parsing pom.xml. Please enter your OSGi Bundle Symbolic Name.\nThis is needed so that the classloader will be able to find your dynamic java.";
            MdwInputDialog dlg = new MdwInputDialog(MdwPlugin.getShell(), msg, false);
            dlg.setWidth(300);
            if (dlg.open() == Dialog.OK)
                return dlg.getInput();
        }

        return null;
    }

    protected byte[] decodeBase64(String inputString) {
        return Base64.decodeBase64(inputString.getBytes());
    }

    public abstract String getAttributeName();

    public abstract byte[] getArtifactContent();

    public abstract String getArtifactTypeDescription();

    public abstract List<String> getLanguageOptions();

    public abstract String getDefaultLanguage();

    public abstract String getLanguage();

    public abstract void languageChanged(String newLanguage);

    public abstract String getEditLinkLabel();

}
