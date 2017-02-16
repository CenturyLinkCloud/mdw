/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.centurylink.mdw.plugin.PluginMessages;

public class ConvertProjectAction implements IObjectActionDelegate {
    private static final String FACETS_PROP_PAGE_ID = "org.eclipse.wst.common.project.facet.ui.FacetsPropertyPage";

    private Shell shell;
    private IStructuredSelection selection;

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof IStructuredSelection)
            this.selection = (IStructuredSelection) selection;
        else
            this.selection = null;
    }

    @SuppressWarnings("restriction")
    public void run(IAction action) {
        if (selection != null) {
            Object element = selection.getFirstElement();
            try {
                IProject project = (element instanceof IJavaProject)
                        ? ((IJavaProject) element).getProject() : (IProject) element;
                IFacetedProject facetedProject = ProjectFacetsManager.create(project);
                if (facetedProject == null) {
                    org.eclipse.wst.common.project.facet.ui.internal.ConvertProjectToFacetedFormRunnable
                            .runInProgressDialog(shell, project);
                    facetedProject = ProjectFacetsManager.create(project);
                }
                IFacetedProjectWorkingCopy workingCopy = org.eclipse.wst.common.project.facet.ui.internal.SharedWorkingCopyManager
                        .getWorkingCopy(facetedProject);
                workingCopy.addProjectFacet(
                        ProjectFacetsManager.getProjectFacet("mdw.workflow").getDefaultVersion());
                if (!facetedProject.hasProjectFacet(ProjectFacetsManager.getProjectFacet("jst.ear"))
                        && !facetedProject
                                .hasProjectFacet(ProjectFacetsManager.getProjectFacet("jst.web"))
                        && !facetedProject.hasProjectFacet(
                                ProjectFacetsManager.getProjectFacet("jst.utility"))) {
                    workingCopy.addProjectFacet(ProjectFacetsManager.getProjectFacet("jst.utility")
                            .getDefaultVersion());
                }

                PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(shell, element,
                        FACETS_PROP_PAGE_ID, null, null, PreferencesUtil.OPTION_NONE);
                if (dialog != null) {
                    dialog.open();
                }
            }
            catch (Exception ex) {
                PluginMessages.uiError(shell, ex, "Convert Workflow Project");
            }
        }
    }

}