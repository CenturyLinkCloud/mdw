/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.extensions;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.model.MdwVersion;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class ExtensionModule implements IFilter, Comparable<ExtensionModule> {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String version;

    /**
     * null means use MDW version
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String v) {
        this.version = v;
    }

    private String requiredMdwVersion = "5.1";

    /**
     * null means any version >= 5.1 (required for extension module support)
     */
    public String getRequiredMdwVersion() {
        return requiredMdwVersion;
    }

    public void setRequiredMdwVersion(String v) {
        this.requiredMdwVersion = v;
    }

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Handle an mdw plug-in project config file element when reading.
     */
    public void readConfigElement(String qName, Map<String, String> attrs,
            WorkflowProject project) {
        if (qName.equals(getId())) {
            if (!project.getExtensionModules().contains(this))
                project.getExtensionModules().add(this);
        }
    }

    /**
     * Create an mdw plug-in project config file element when writing.
     */
    public String writeConfigElement(WorkflowProject project) {
        StringBuffer buf = new StringBuffer("  <" + getId() + " type=\"extension\"");
        if (getVersion() != null)
            buf.append(" version=\"" + getVersion() + "\"");
        buf.append(" />\n");
        return buf.toString();
    }

    /**
     * Callback for adding this extension to the designated workflow project.
     * Allocated 100 monitor ticks.
     * 
     * @return true if add was successful and should continue (workflow project
     *         references should be added)
     */
    public abstract boolean addTo(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException, InterruptedException;

    /**
     * Callback for interacting with the UI as necessary for adding this
     * extension to the designated project.
     * 
     * @return true if add was successful and should continue (workflow project
     *         references should be added)
     */
    public boolean addUi(WorkflowProject project, Shell shell) throws ExtensionModuleException {
        return true; // default impl does nothing
    }

    /**
     * Callback for removing this extension from the designated workflow
     * project. Allocated 100 monitor ticks.
     * 
     * @return true if removal was successful and should continue (workflow
     *         project references should be removed)
     */
    public abstract boolean removeFrom(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException, InterruptedException;

    /**
     * Callback for interacting with the UI as necessary for removing this
     * extension to the designated project.
     * 
     * @return true if removal was successful and should continue (workflow
     *         project references should be removed)
     */
    public boolean removeUi(WorkflowProject project, Shell shell) throws ExtensionModuleException {
        return true; // default impl does nothing
    }

    /**
     * Callback for updating this extension for the designated workflow project.
     * Allocated 100 monitor ticks.
     * 
     * @return true if update was successful
     */
    public abstract boolean update(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException, InterruptedException;

    private Image icon;

    public Image getIcon() {
        if (icon == null) {
            icon = MdwPlugin.getImageDescriptor("icons/extension.gif").createImage();
        }
        return icon;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ExtensionModule))
            return false;

        return this.getId().equals(((ExtensionModule) other).getId());
    }

    public int compareTo(ExtensionModule other) {
        return this.getId().compareTo(other.getId());
    }

    /**
     * Object is an instance of WorkflowProject. Return false to suppress
     * extension. Default implementation checks for required MDW version (5.1 if
     * not specified).
     */
    public boolean select(Object object) {
        WorkflowProject workflowProject = (WorkflowProject) object;
        boolean meetsVersion = new MdwVersion(workflowProject.getMdwVersion())
                .checkRequiredVersion(requiredMdwVersion);
        boolean isEar = workflowProject.isEarProject();
        return meetsVersion && isEar;
    }

    // used for label in confirmation dialog
    public String toString() {
        return getName();
    }
}
