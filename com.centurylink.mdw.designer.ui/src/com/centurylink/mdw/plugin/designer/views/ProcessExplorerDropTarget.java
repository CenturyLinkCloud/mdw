/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.lang.reflect.Constructor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.dialogs.CopyDialog;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ProcessExplorerDropTarget extends ViewerDropAdapter {
    public ProcessExplorerDropTarget(TreeViewer viewer) {
        super(viewer);
    }

    /**
     * Workflow element string format (elements delimited by '#'): Process:
     * "Process~MyWorkflowProject^MyPackage v1.0^MyProcess v1.0" ExternalEvent:
     * "ExternalEvent~MyWorkflowProject^MyPackage v1.0^extEvtId" ActivityImpl:
     * "ActivityImpl~MyWorkflowProject^MyPackage v1.0^activityImplId"
     * WorkflowAsset: "WorkflowAsset~MyWorkflowProject^MyPackage v1.0^assetId"
     */
    public boolean performDrop(Object data) {
        return drop((String) data, getCurrentTarget(), getCurrentOperation());
    }

    public boolean drop(String data, Object target, int operation) {
        String elementInfo = (String) data;
        String[] elements = elementInfo.split("#");

        if (target instanceof WorkflowElement && ((WorkflowElement) target).getPackage() != null) {
            WorkflowPackage targetPackage = ((WorkflowElement) target).getPackage();
            if (checkForDuplicates(targetPackage, elements))
                return false;

            for (String element : elements) {
                if (element.startsWith("Process~")) {
                    if (!handleDropProcess(element.substring(8), targetPackage, operation))
                        return false;
                }
                else if (element.startsWith("ExternalEvent~")) {
                    if (!handleDropExternalEvent(element.substring(14), targetPackage, operation))
                        return false;
                }
                else if (element.startsWith("ActivityImpl~")) {
                    if (!handleDropActivityImpl(element.substring(14), targetPackage, operation))
                        return false;
                }
                else if (element.startsWith("WorkflowAsset~")) {
                    if (!handleDropWorkflowAsset(element.substring(14), targetPackage, operation))
                        return false;
                }
                else {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    public boolean validateDrop(Object target, int op, TransferData type) {
        if (!TextTransfer.getInstance().isSupportedType(type))
            return false;

        String elementInfo = null;
        try {
            Object toDrop = TextTransfer.getInstance().nativeToJava(type);
            if (!(toDrop instanceof String))
                return false;
            elementInfo = (String) toDrop;
        }
        catch (SWTException ex) {
            return false;
        }

        return isValidDrop(elementInfo, target, getCurrentOperation());
    }

    public boolean isValidDrop(String elementInfo, Object target, int operation) {
        if (target instanceof WorkflowElement && ((WorkflowElement) target).getPackage() != null) {
            WorkflowPackage targetPackage = ((WorkflowElement) target).getPackage();

            // not allowed for archived or read-only packages
            if (targetPackage.isArchived() || targetPackage.isReadOnly())
                return false;

            String[] elements = elementInfo.split("#");
            for (String element : elements) {
                String itemInfo = element.substring(element.indexOf('~') + 1);
                String workflowProjectName = getWorkflowProjectName(itemInfo);
                String packageName = getPackageName(itemInfo);
                String packageVersion = getPackageVersion(itemInfo);

                if (operation == DND.DROP_MOVE) {
                    // only allow moves for elements in the same project
                    if (!targetPackage.getProject().getName().equals(workflowProjectName))
                        return false;

                    // don't allow drops into same package
                    if (targetPackage.getName().equals(packageName)
                            && targetPackage.getVersionString().equals(packageVersion))
                        return false;
                    // default package and empty package are the same
                    if (targetPackage.isDefaultPackage() && packageName.equals(""))
                        return false;
                }
                else if (operation == DND.DROP_COPY) {
                    // only allow copies for elements in the same project
                    // (exception: single process)
                    if (!targetPackage.getProject().getName().equals(workflowProjectName)) {
                        if (!element.startsWith("Process") || elements.length != 1)
                            return false;
                    }

                    if (!element.startsWith("Process") && !element.startsWith("ExternalEvent")
                            && !element.startsWith("ActivityImpl")
                            && !element.startsWith("WorkflowAsset"))
                        return false;
                }
            }

            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Returns true if duplicates found.
     */
    private boolean checkForDuplicates(WorkflowPackage targetPackage, String[] elements) {
        for (String element : elements) {
            if (element.startsWith("Process~")) {
                String processName = element.substring(8);
                for (WorkflowProcess process : targetPackage.getProcesses()) {
                    if (process.getName().equals(processName)) {
                        String msg = "Package '" + targetPackage.getName()
                                + "' already contains version " + process.getVersionLabel()
                                + " of process '" + process.getName() + "'";
                        MessageDialog.openError(getViewer().getControl().getShell(),
                                "Duplicate Process", msg);
                        return true;
                    }
                }
            }
            else if (element.startsWith("ExternalEvent~")) {
                String externalEventName = element.substring(14);
                for (ExternalEvent externalEvent : targetPackage.getExternalEvents()) {
                    if (externalEvent.getName().equals(externalEventName)) {
                        String msg = "Package '" + targetPackage.getName() + "' already contains "
                                + " an external event named '" + externalEvent.getName() + "'";
                        MessageDialog.openError(getViewer().getControl().getShell(),
                                "Duplicate External Event", msg);
                        return true;
                    }
                }
            }
            else if (element.startsWith("ActivityImpl~")) {
                // TODO: dups okay?
            }
            else if (element.startsWith("WorkflowAsset~")) {
                String assetName = element.substring(14);
                for (WorkflowAsset asset : targetPackage.getAssets()) {
                    if (asset.getName().equals(assetName)) {
                        String msg = "Package '" + targetPackage.getName() + "' already contains "
                                + " an definition document named '" + asset.getName() + "'";
                        MessageDialog.openError(getViewer().getControl().getShell(),
                                "Duplicate Definition Doc", msg);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param processInfo
     * @param targetPackage
     * @param operation
     * @return
     */
    private boolean handleDropProcess(String processInfo, WorkflowPackage targetPackage,
            int operation) {
        String processName = getProcessName(processInfo);
        String version = getProcessVersion(processInfo);
        WorkflowProject targetProject = targetPackage.getProject();
        WorkflowPackage sourcePackage = targetProject.getPackage(getPackageName(processInfo));
        DesignerProxy designerProxy = targetProject.getDesignerProxy();

        if (operation == DND.DROP_COPY) {
            String workflowProjectName = getWorkflowProjectName(processInfo);
            WorkflowProcess existing = targetProject.getProcess(processName, version);

            if (!targetPackage.getProject().getName().equals(workflowProjectName)) {
                // copying from remote project
                if (existing != null) {
                    MessageDialog.openError(getViewer().getControl().getShell(),
                            "Copy Foreign Process",
                            "' " + processName + " v" + version
                                    + "' already exists in workflow project '"
                                    + targetPackage.getProject().getLabel() + "'.");
                    return false;
                }

                WorkflowProject sourceProject = WorkflowProjectManager.getInstance()
                        .getWorkflowProject(workflowProjectName);

                designerProxy.copyForeignProcess(processName, version, sourceProject,
                        targetPackage);
                WorkflowProcess newCopy = targetProject.getProcess(processName, "0.1");
                if (newCopy != null) {
                    newCopy.addElementChangeListener(targetProject);
                    newCopy.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newCopy);
                }

                ((TreeViewer) getViewer()).refresh(targetPackage, true);
                return true;
            }
            else {
                // is there a dirty editor open?
                if (existing != null) {
                    IEditorPart editor = findOpenEditor(existing);
                    if (editor != null && editor.isDirty()) {
                        MessageDialog.openWarning(getViewer().getControl().getShell(),
                                "Copy Process", "' " + processName + " v" + version
                                        + "' is currently open in an editor.\nPlease save or close before copying.");
                        return false;
                    }
                }
                CopyDialog copyDialog = new CopyDialog(getViewer().getControl().getShell(),
                        existing, processName, version, targetPackage);
                if (copyDialog.open() == Dialog.OK) {
                    String newName = copyDialog.getNewName();
                    designerProxy.copyProcess(processName, version, newName, targetPackage);
                    WorkflowProcess newCopy = targetProject.getProcess(newName, "0.1");
                    if (newCopy != null) {
                        newCopy.addElementChangeListener(targetProject);
                        newCopy.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newCopy);
                    }
                    ((TreeViewer) getViewer()).refresh(targetPackage, true);
                    return true;
                }
            }

            return false;
        }
        else if (operation == DND.DROP_MOVE) {
            // don't allow multiple versions of the same process in a package
            if (!targetPackage.isDefaultPackage()) {
                if (targetPackage.getProcess(processName) != null) {
                    MessageDialog.openError(MdwPlugin.getShell(), "Process Exists",
                            "Process '" + processName + "' already exists in package '"
                                    + targetPackage.getName() + "'.");
                    return false;
                }
            }

            if (targetPackage.isDefaultPackage()) {
                designerProxy.removeProcessFromPackage(processName, version, sourcePackage);
            }
            else {
                designerProxy.moveProcessToPackage(processName, version, targetPackage);
            }
            ((TreeViewer) getViewer()).refresh(targetPackage.getProject(), true);
            if (sourcePackage != null)
                ((TreeViewer) getViewer()).refresh(sourcePackage.getProject(), true);
            return true;
        }
        else {
            return false;
        }
    }

    private boolean handleDropExternalEvent(String externalEventInfo, WorkflowPackage targetPackage,
            int operation) {
        Long externalEventId = getExternalEventId(externalEventInfo);
        WorkflowProject workflowProject = targetPackage.getProject();
        WorkflowPackage sourcePackage = workflowProject
                .getPackage(getPackageName(externalEventInfo));
        DesignerProxy designerProxy = workflowProject.getDesignerProxy();

        if (operation == DND.DROP_COPY) {
            ExternalEvent existing = workflowProject.getExternalEvent(externalEventId);
            CopyDialog copyDialog = new CopyDialog(getViewer().getControl().getShell(), existing,
                    existing.getName(), null,targetPackage);
            if (copyDialog.open() == Dialog.OK) {
                String newPattern = copyDialog.getNewName();
                ExternalEvent newCopy = new ExternalEvent(existing);
                newCopy.setName(newPattern);
                newCopy.setPackage(targetPackage);
                designerProxy.registerExternalEventHandler(newCopy);

                if (newCopy != null) {
                    newCopy.addElementChangeListener(workflowProject);
                    newCopy.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newCopy);
                }
                ((TreeViewer) getViewer()).refresh(targetPackage, true);
                return true;
            }
            return false;
        }
        else if (operation == DND.DROP_MOVE) {
            if (targetPackage.isDefaultPackage()) {
                designerProxy.removeExternalEventFromPackage(externalEventId, sourcePackage);
            }
            else {
                designerProxy.moveExternalEventToPackage(externalEventId, targetPackage);
            }
            ((TreeViewer) getViewer()).refresh(targetPackage.getProject(), true);
            return true;
        }
        else {
            return false;
        }
    }

    private boolean handleDropActivityImpl(String activityImplInfo, WorkflowPackage targetPackage,
            int operation) {
        Long activityImplId = getActivityImplId(activityImplInfo);
        WorkflowProject workflowProject = targetPackage.getProject();
        WorkflowPackage sourcePackage = workflowProject
                .getPackage(getPackageName(activityImplInfo));
        DesignerProxy designerProxy = workflowProject.getDesignerProxy();

        if (operation == DND.DROP_COPY) {
            if (!targetPackage.isDefaultPackage()) {
                designerProxy.addActivityImplToPackage(activityImplId, targetPackage);
                ((TreeViewer) getViewer()).refresh(targetPackage, true);
                return true;
            }
            return false;
        }
        else if (operation == DND.DROP_MOVE) {
            if (targetPackage.isDefaultPackage()) {
                designerProxy.removeActivityImplFromPackage(activityImplId, sourcePackage);
            }
            else {
                designerProxy.addActivityImplToPackage(activityImplId, targetPackage);
                if (RunnerStatus.SUCCESS.equals(designerProxy.getRunnerStatus()))
                    designerProxy.removeActivityImplFromPackage(activityImplId, sourcePackage);
            }
            ((TreeViewer) getViewer()).refresh(targetPackage.getProject(), true);
            return true;
        }
        else {
            return false;
        }
    }

    private boolean handleDropWorkflowAsset(String assetInfo, WorkflowPackage targetPackage,
            int operation) {
        if (!targetPackage.getProject().checkRequiredVersion(5, 0)) {
            String msg = "Packaging of Definition Documents (such as scripts and pages) is only supported for MDWFramework 5.0 and later.";
            MessageDialog.openError(getViewer().getControl().getShell(), "Package Def Doc", msg);
            return false;
        }

        Long assetId = getWorkflowAssetId(assetInfo);
        WorkflowProject workflowProject = targetPackage.getProject();
        WorkflowPackage sourcePackage = workflowProject.getPackage(getPackageName(assetInfo));
        DesignerProxy designerProxy = workflowProject.getDesignerProxy();

        if (operation == DND.DROP_COPY) {
            WorkflowAsset existing = workflowProject.getAsset(assetId);
            CopyDialog copyDialog = new CopyDialog(getViewer().getControl().getShell(), existing,
                    existing.getName(), String.valueOf(existing.getVersion()), targetPackage);
            if (copyDialog.open() == Dialog.OK) {
                String newName = copyDialog.getNewName();
                if (!existing.isLoaded())
                    existing.load();
                WorkflowAsset newCopy;
                try {
                    // try and use reflection to make it the correct type
                    Constructor<? extends WorkflowAsset> ctor = existing.getClass()
                            .getConstructor(new Class<?>[] { existing.getClass() });
                    newCopy = ctor.newInstance(new Object[] { existing });
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                    newCopy = new WorkflowAsset(existing);
                }
                newCopy.setName(newName);
                newCopy.setVersion(1);
                newCopy.setPackage(targetPackage);
                try {
                    designerProxy.createNewWorkflowAsset(newCopy, true);
                }
                catch (Exception ex) {
                    PluginMessages.uiError(ex, "Copy " + existing.getTitle(),
                            existing.getProject());
                }
                if (newCopy != null) {
                    newCopy.addElementChangeListener(workflowProject);
                    newCopy.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newCopy);
                    WorkflowAssetFactory.registerAsset(newCopy);
                }
                ((TreeViewer) getViewer()).refresh(targetPackage, true);
                return true;
            }
            return false;
        }
        else if (operation == DND.DROP_MOVE) {
            // don't allow multiple versions of the same asset in a package
            if (!targetPackage.isDefaultPackage()) {
                WorkflowAsset asset = workflowProject.getAsset(assetId);
                if (targetPackage.getAsset(asset.getName()) != null) {
                    MessageDialog.openError(MdwPlugin.getShell(), "Asset Exists",
                            "Asset '" + asset.getName() + "' already exists in package '"
                                    + targetPackage.getName() + "'.");
                    return false;
                }
            }

            if (targetPackage.isDefaultPackage()) {
                designerProxy.removeWorkflowAssetFromPackage(assetId, sourcePackage);
            }
            else {
                designerProxy.moveWorkflowAssetToPackage(assetId, targetPackage);
            }
            ((TreeViewer) getViewer()).refresh(targetPackage.getProject(), true);
            WorkflowAssetFactory.registerAsset(workflowProject.getAsset(assetId));
            return true;
        }
        else {
            return false;
        }
    }

    private String getWorkflowProjectName(String elementInfo) {
        int firstHat = elementInfo.indexOf('^');
        return elementInfo.substring(0, firstHat);
    }

    private String getPackageName(String elementInfo) {
        int firstHat = elementInfo.indexOf('^');
        int secondHat = elementInfo.indexOf('^', firstHat + 1);
        if (elementInfo.substring(firstHat + 1, secondHat).length() == 0)
            return ""; // default package
        int pkgV = elementInfo.lastIndexOf('v', secondHat);
        return elementInfo.substring(firstHat + 1, pkgV - 1);
    }

    private String getPackageVersion(String elementInfo) {
        int firstHat = elementInfo.indexOf('^');
        int secondHat = elementInfo.indexOf('^', firstHat + 1);
        if (elementInfo.substring(firstHat + 1, secondHat).length() == 0)
            return ""; // default package
        int pkgV = elementInfo.lastIndexOf('v', secondHat);
        return elementInfo.substring(pkgV + 1, secondHat);
    }

    private String getProcessName(String processInfo) {
        int firstHat = processInfo.indexOf('^');
        int secondHat = processInfo.indexOf('^', firstHat + 1);
        int lastV = processInfo.lastIndexOf('v');
        return processInfo.substring(secondHat + 1, lastV - 1);
    }

    private String getProcessVersion(String processInfo) {
        int lastV = processInfo.lastIndexOf('v');
        return processInfo.substring(lastV + 1);
    }

    private Long getExternalEventId(String externalEventInfo) {
        int firstHat = externalEventInfo.indexOf('^');
        int secondHat = externalEventInfo.indexOf('^', firstHat + 1);
        return new Long(externalEventInfo.substring(secondHat + 1));
    }

    private Long getActivityImplId(String activityImplInfo) {
        int firstHat = activityImplInfo.indexOf('^');
        int secondHat = activityImplInfo.indexOf('^', firstHat + 1);
        return new Long(activityImplInfo.substring(secondHat + 1));
    }

    private Long getWorkflowAssetId(String workflowAssetInfo) {
        int firstHat = workflowAssetInfo.indexOf('^');
        int secondHat = workflowAssetInfo.indexOf('^', firstHat + 1);
        return new Long(workflowAssetInfo.substring(secondHat + 1));
    }

    private IEditorPart findOpenEditor(IEditorInput workflowElement) {
        return MdwPlugin.getActivePage().findEditor(workflowElement);
    }

}