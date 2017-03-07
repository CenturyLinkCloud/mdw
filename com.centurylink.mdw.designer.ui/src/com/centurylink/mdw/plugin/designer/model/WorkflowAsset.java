/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import com.centurylink.mdw.common.exception.MDWException;
import com.centurylink.mdw.common.utilities.ExpressionUtil;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerResult;
import com.centurylink.mdw.plugin.designer.DesignerRunner.RunnerStatus;
import com.centurylink.mdw.plugin.designer.dialogs.VersionableSaveDialog;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;
import com.centurylink.mdw.plugin.workspace.TempFileRemover;

public class WorkflowAsset extends WorkflowElement implements AttributeHolder, Versionable,
        Comparable<WorkflowAsset>, IResourceChangeListener {
    private RuleSetVO ruleSetVO;

    public RuleSetVO getRuleSetVO() {
        return ruleSetVO;
    }

    protected void setRuleSetVO(RuleSetVO ruleSetVO) {
        this.ruleSetVO = ruleSetVO;
    }

    public void setPackage(WorkflowPackage packageVersion) {
        super.setPackage(packageVersion);
        if (packageVersion != null) {
            setProject(packageVersion.getProject());
            ruleSetVO.setPackageName(packageVersion.getName());
        }
    }

    public Entity getActionEntity() {
        return Entity.RuleSet;
    }

    public WorkflowAsset() {
        ruleSetVO = new RuleSetVO();
        ruleSetVO.setVersion(1);
        ruleSetVO.setId(new Long(-1));
    }

    public WorkflowAsset(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        this.ruleSetVO = ruleSetVO;
        setPackage(packageVersion);
    }

    public WorkflowAsset(WorkflowAsset cloneFrom) {
        this(new RuleSetVO(cloneFrom.getRuleSetVO()), cloneFrom.getPackage());
    }

    public List<WorkflowAsset> getAllVersions() {
        List<WorkflowAsset> allVersions = new ArrayList<WorkflowAsset>();

        for (WorkflowAsset asset : getProject().getAllWorkflowAssets()) {
            if (this.getName().equals(asset.getName())
                    && ((this.getLanguage() == null && asset.getLanguage() == null)
                            || this.getLanguage().equals(asset.getLanguage()))
                    && (this.isInDefaultPackage() || asset.isInDefaultPackage()
                            || this.getPackage().getName().equals(asset.getPackage().getName()))
                    && !allVersions.contains(asset)) {
                allVersions.add(asset);
            }
        }

        Collections.sort(allVersions, new Comparator<WorkflowAsset>() {
            public int compare(WorkflowAsset dd1, WorkflowAsset dd2) {
                return dd2.getVersion() - dd1.getVersion();
            }
        });

        return allVersions;
    }

    public boolean isLatestVersion() {
        List<WorkflowAsset> allVers = getAllVersions();
        if (allVers == null || allVers.isEmpty())
            return true;
        return this.equals(allVers.get(0));
    }

    public boolean isLoaded() {
        return getContent() != null;
    }

    @Override
    public String getTitle() {
        return "Workflow Asset";
    }

    @Override
    public Long getId() {
        return ruleSetVO.getId();
    }

    public void setId(Long id) {
        ruleSetVO.setId(id);
    }

    public String getIdLabel() {
        if (getProject().getPersistType() == PersistType.Git)
            return getId() + " (" + getHexId() + ")";
        else
            return String.valueOf(getId());
    }

    public String getName() {
        return ruleSetVO.getName();
    }

    public void setName(String name) {
        ruleSetVO.setName(name);
    }

    public String getLabel() {
        if (getProject().checkRequiredVersion(5, 0))
            return getName() + " v" + getVersionString();
        else
            return getName();
    }

    public String getContent() {
        return ruleSetVO.getRuleSet();
    }

    public void setContent(String content) {
        ruleSetVO.setRuleSet(content);
    }

    public byte[] getDecodedContent() {
        return RuleSetVO.decode(getContent());
    }

    public void encodeAndSetContent(byte[] bytes) {
        ruleSetVO.setRuleSet(RuleSetVO.encode(bytes));
    }

    public void substituteAndSetContent(String raw) throws MDWException {
        setContent(ExpressionUtil.substitute(raw, this));
    }

    public String getLanguage() {
        return ruleSetVO.getLanguage();
    }

    public void setLanguage(String language) {
        ruleSetVO.setLanguage(language);
    }

    public String getLanguageFriendly() {
        return getLanguage();
    }

    public void setLanguageFriendly(String friendly) {
        setLanguage(friendly.toUpperCase().replace(" ", "_"));
    }

    public String getCreateUser() {
        return ruleSetVO.getCreateUser();
    }

    public void setCreateUser(String user) {
        ruleSetVO.setCreateUser(user);
    }

    public Date getCreateDate() {
        return ruleSetVO.getCreateDate();
    }

    public String getFormattedCreateDate() {
        if (getCreateDate() == null)
            return "";
        return PluginUtil.getDateFormat().format(getCreateDate());
    }

    public String getLockingUser() {
        return ruleSetVO.getModifyingUser();
    }

    public void setLockingUser(String lockUser) {
        ruleSetVO.setModifyingUser(lockUser);
    }

    public Date getModifyDate() {
        return ruleSetVO.getModifyDate();
    }

    public void setModifyDate(Date modDate) {
        ruleSetVO.setModifyDate(modDate);
    }

    public String getFormattedModifyDate() {
        if (getModifyDate() == null)
            return "";
        return PluginUtil.getDateFormat().format(getModifyDate());
    }

    public int getVersion() {
        return ruleSetVO.getVersion();
    }

    public void setVersion(int version) {
        ruleSetVO.setVersion(version);
    }

    public String getVersionLabel() {
        return "v" + getVersionString();
    }

    public String getVersionString() {
        return ruleSetVO.getVersionString();
    }

    public String formatVersion(int version) {
        return RuleSetVO.formatVersion(version);
    }

    public int parseVersion(String versionString) throws NumberFormatException {
        return RuleSetVO.parseVersion(versionString);
    }

    public int getNextMajorVersion() {
        return (ruleSetVO.getVersion() / 1000 + 1) * 1000;
    }

    public int getNextMinorVersion() {
        return ruleSetVO.getVersion() + 1;
    }

    public String getComment() {
        return ruleSetVO.getComment();
    }

    public void setComment(String comment) {
        ruleSetVO.setComment(comment);
    }

    public String getRevisionComment() {
        return ruleSetVO.getRevisionComment();
    }

    public void setRevisionComment(String revComment) {
        ruleSetVO.setRevisionComment(revComment);
    }

    public List<AttributeVO> getAttributes() {
        return ruleSetVO.getAttributes();
    }

    public void setAttributes(List<AttributeVO> attrs) {
        ruleSetVO.setAttributes(attrs);
    }

    public String getAttribute(String name) {
        return ruleSetVO.getAttribute(name);
    }

    public void setAttribute(String name, String value) {
        ruleSetVO.setAttribute(name, value);
        fireAttributeValueChanged(name, value);
    }

    public void removeAttribute(String name) {
        ruleSetVO.removeAttribute(name);
        fireAttributeValueChanged(name, null);
    }

    protected ListenerList attributeValueChangeListeners = new ListenerList();

    public void addAttributeValueChangeListener(AttributeValueChangeListener listener) {
        attributeValueChangeListeners.add(listener);
    }

    public void removeAttributeValueChangeListener(AttributeValueChangeListener listener) {
        attributeValueChangeListeners.remove(listener);
    }

    public void fireAttributeValueChanged(String attrName, String newValue) {
        for (int i = 0; i < attributeValueChangeListeners.getListeners().length; ++i) {
            AttributeValueChangeListener listener = (AttributeValueChangeListener) attributeValueChangeListeners
                    .getListeners()[i];
            if (listener.getAttributeName().equals(attrName))
                listener.attributeValueChanged(newValue);
        }
    }

    public boolean isReadOnly() {
        return !isLockedToUser();
    }

    private boolean dirty;

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean hasInstanceInfo() {
        return false;
    }

    public int compareTo(WorkflowAsset other) {
        int res = this.getLanguage().compareTo(other.getLanguage());
        if (res == 0)
            res = this.getName().compareToIgnoreCase(other.getName());
        if (res == 0)
            res = this.getVersion() - other.getVersion();

        return res;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof WorkflowAsset))
            return false;

        if (!super.equals(o))
            return false;

        WorkflowAsset other = (WorkflowAsset) o;

        // package comparison
        if (this.isInDefaultPackage()) {
            if (!other.isInDefaultPackage())
                return false;
        }
        else {
            if (!this.getPackage().equals(other.getPackage()))
                return false;
        }

        return this.getName().equals(other.getName()) && this.getVersion() == other.getVersion();
    }

    @Override
    public String getIcon() {
        return "doc.gif";
    }

    public boolean isLockedToUser() {
        if (getProject().isFilePersist())
            return !getProject().isRemote();

        String currentUser = getProject().getUser().getUsername();
        String lockingUser = getLockingUser();
        return currentUser.equalsIgnoreCase(lockingUser);
    }

    public String getTempFileName() {
        String name = getName();
        name = FileHelper.stripDisallowedFilenameChars(name);
        String ext = getExtension();
        return name.indexOf('.') > 0 ? name : name + ext;
    }

    public String getExtension() {
        String ext = WorkflowElement.getArtifactFileExtensions().get(getLanguage());
        if (ext == null)
            ext = getDefaultExtension();
        return ext;
    }

    public String getDefaultExtension() {
        return ".txt";
    }

    public IFolder getTempFolder() {
        IFolder tempFolder = getProject().getTempFolder();
        if (!isInDefaultPackage())
            tempFolder = tempFolder.getFolder(getPackage().getName());

        return tempFolder;
    }

    public IFile getTempFile(IFolder folder) {
        String filename = getTempFileName();
        if (isInDefaultPackage() || !isLatestVersion())
            filename = filename.substring(0, filename.lastIndexOf('.')) + "_v" + getVersion()
                    + filename.substring(filename.lastIndexOf('.'));
        return folder.getFile(filename);
    }

    private IEditorPart assetFileEditor;

    public IEditorPart getFileEditor() {
        return assetFileEditor;
    }

    public IEditorPart findOpenEditor() {
        if (assetFileEditor == null)
            return null;

        IWorkbenchPage activePage = MdwPlugin.getActivePage();
        return activePage.findEditor(assetFileEditor.getEditorInput());
    }

    public boolean isRawEdit() {
        return getProject().isFilePersist();
    }

    public java.io.File getRawFile() {
        if (!isRawEdit())
            throw new UnsupportedOperationException("Only for VCS assets");
        String pkgFolder;
        if (getPackage().isArchived())
            pkgFolder = "Archive/" + getPackage().getLabel();
        else
            pkgFolder = getPackage().getName().replace('.', '/');
        return new java.io.File(getProject().getAssetDir() + "/" + pkgFolder + "/" + getName());
    }

    public IFile getAssetFile() {
        if (isRawEdit()) {
            String pkgFolder;
            if (getPackage().isArchived())
                pkgFolder = "Archive/" + getPackage().getLabel();
            else
                pkgFolder = getPackage().getName().replace('.', '/');

            return getProject().getAssetFolder().getFolder(pkgFolder).getFile(getName());
        }
        else {
            return getTempFile(getTempFolder());
        }
    }

    public String getVcsAssetPath() {
        if (isRawEdit()) {
            return getPackage().getVcsAssetPath() + "/" + getName();
        }
        else {
            throw new UnsupportedOperationException("Only for VCS Assets");
        }
    }

    public void openFile(IProgressMonitor monitor) {
        if (!isRawEdit()) {
            openTempFile(monitor);
            return;
        }

        assetFileEditor = null;

        try {
            final IFile file = getAssetFile();
            file.refreshLocal(0, monitor);
            boolean readOnly = getProject().isReadOnly()
                    || !isUserAuthorized(UserRoleVO.ASSET_DESIGN);
            PluginUtil.setReadOnly(file, readOnly);

            final IWorkbenchPage activePage = MdwPlugin.getActivePage();
            IEditorInput editorInput = new FileEditorInput(file);
            assetFileEditor = activePage.findEditor(editorInput);
            if (assetFileEditor != null) {
                assetFileEditor = IDE.openEditor(activePage, file, true); // activate
                                                                          // existing
                                                                          // editor
            }
            else {
                load();
                final Display display = Display.getCurrent();
                if (display != null) {
                    display.syncExec(new Runnable() {
                        public void run() {
                            try {
                                final IWorkbenchPage activePage = MdwPlugin.getActivePage();
                                beforeFileOpened();
                                if (isForceExternalEditor()) {
                                    activePage.openEditor(new FileEditorInput(file),
                                            IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID, true);
                                }
                                else {
                                    assetFileEditor = IDE.openEditor(activePage, file, true);
                                }
                                afterFileOpened(assetFileEditor);
                            }
                            catch (PartInitException ex) {
                                PluginMessages.log(ex);
                            }
                        }
                    });
                }
            }
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Open Asset File", getProject());
        }
    }

    @SuppressWarnings("restriction")
    public void openTempFile(IProgressMonitor monitor) {
        assetFileEditor = null;

        try {
            IFolder folder = getTempFolder();
            folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            if (!folder.exists())
                PluginUtil.createFoldersAsNeeded(getProject().getSourceProject(), folder, monitor);

            final IFile file = getTempFile(folder);
            final IWorkbenchPage activePage = MdwPlugin.getActivePage();
            boolean externalEditorAlreadyOpen = false;

            if (file.exists()) {
                IEditorInput editorInput = new FileEditorInput(file);
                assetFileEditor = activePage.findEditor(editorInput);

                if (assetFileEditor == null) {
                    // we'll refresh from saved value
                    WorkflowAssetFactory.deRegisterWorkbenchListener(file);
                    try {
                        new TempFileRemover(folder, file).remove(monitor);
                    }
                    catch (org.eclipse.core.internal.resources.ResourceException ex) {
                        if (isForceExternalEditor()) {
                            // assume this is because the temp file is already
                            // open in an external editor; reactivate
                            PluginMessages.log(ex);
                            externalEditorAlreadyOpen = true;
                            activePage.openEditor(new FileEditorInput(file),
                                    IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID, true);
                        }
                        else {
                            throw ex;
                        }
                    }
                }
                else {
                    // activate existing editor
                    assetFileEditor = IDE.openEditor(activePage, file, true);
                }
            }

            if (assetFileEditor == null) {
                // either the file didn't exist or it was not currently open,
                // set from value
                if (!externalEditorAlreadyOpen)
                    createTempFile(file, monitor);

                final Display display = Display.getCurrent();
                if (display != null) {
                    display.syncExec(new Runnable() {
                        public void run() {
                            try {
                                beforeFileOpened();
                                if (isForceExternalEditor())
                                    activePage.openEditor(new FileEditorInput(file),
                                            IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID, true);
                                else
                                    assetFileEditor = IDE.openEditor(activePage, file, true);

                                if (assetFileEditor != null) {
                                    assetFileEditor.addPropertyListener(
                                            new AssetEditorPropertyListener(assetFileEditor));
                                    WorkflowAssetFactory.registerWorkbenchListener(file,
                                            new AssetWorkbenchListener(assetFileEditor));
                                }
                                afterFileOpened(assetFileEditor);
                            }
                            catch (PartInitException ex) {
                                PluginMessages.log(ex);
                            }
                        }
                    });
                }
            }
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Open Temp File", getProject());
        }
    }

    public boolean isForceExternalEditor() {
        return false;
    }

    public void createTempFile(IFile file, IProgressMonitor monitor) throws CoreException {
        load();
        file.create(new ByteArrayInputStream(getFileContent()), true, monitor);

        if (isReadOnly()) {
            ResourceAttributes resourceAttrs = file.getResourceAttributes();
            resourceAttrs.setReadOnly(true);
            file.setResourceAttributes(resourceAttrs);
        }
    }

    public void load() {
        if (MdwPlugin.isUiThread()) {
            BusyIndicator.showWhile(MdwPlugin.getDisplay(), new Runnable() {
                public void run() {
                    WorkflowAsset loaded = getProject().getDesignerProxy()
                            .loadWorkflowAsset(WorkflowAsset.this);
                    ruleSetVO = loaded.getRuleSetVO();
                }
            });
        }
        else {
            WorkflowAsset loaded = getProject().getDesignerProxy()
                    .loadWorkflowAsset(WorkflowAsset.this);
            ruleSetVO = loaded.getRuleSetVO();
        }
    }

    /**
     * Override to perform actions before the asset file is opened.
     */
    protected void beforeFileOpened() {
    }

    /**
     * Override to show views, etc. after asset file is opened.
     */
    protected void afterFileOpened(IEditorPart tempFileEditor) {
    }

    public byte[] getFileContent() {
        byte[] fileContent = null;
        if (isBinary()) {
            fileContent = getDecodedContent();
        }
        else {
            fileContent = getContent() == null ? getDefaultContent().getBytes()
                    : getContent().getBytes();
        }
        return fileContent;
    }

    public String getDefaultContent() {
        return " ";
    }

    public boolean isArchived() {
        return getPackage() == null || getPackage().isArchived();
    }

    public boolean isBinary() {
        return ruleSetVO.isBinary();
    }

    public List<String> getLanguages() {
        // should be overridden
        return null;
    }

    /**
     * override for special validation
     */
    public String validate() {
        if (getProject().isRequireAssetExtension()) {
            int lastDot = getName().lastIndexOf('.');
            if (lastDot == -1)
                return "Assets require a filename extension";
            String ext = RuleSetVO.getFileExtension(getLanguage());
            if (!getName().substring(lastDot).equals(ext))
                return getLanguage() + " assets must have extension " + ext;
        }
        return null;
    }

    public boolean meetsVersionSpec(String versionSpec) {
        return ruleSetVO.meetsVersionSpec(versionSpec);
    }

    public CustomAttributeVO getCustomAttribute() {
        return getProject().getDataAccess().getAssetCustomAttribute(getLanguage());
    }

    public class AssetEditorPropertyListener implements IPropertyListener {
        private IEditorPart fileEditor;
        private boolean previouslyDirty;
        private boolean fileIsReadOnly;

        public AssetEditorPropertyListener(IEditorPart fileEditor) {
            this.fileEditor = fileEditor;
            this.fileIsReadOnly = true;
        }

        public void propertyChanged(Object source, int propId) {
            if (source instanceof EditorPart && propId == IWorkbenchPartConstants.PROP_DIRTY) {
                fileEditor = (EditorPart) source;
                final Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();

                if (fileEditor.isDirty()) {
                    // making file dirty
                    previouslyDirty = true;

                    if (!isUserAuthorized(UserRoleVO.ASSET_DESIGN)) {
                        shell.getDisplay().asyncExec(new Runnable() {
                            public void run() {
                                MessageDialog.openWarning(shell, "Won't Update " + getTitle(),
                                        "You're not authorized to update '" + getLabel()
                                                + "'\nin workflow project '"
                                                + getProject().getName() + "'.");
                            }
                        });
                    }
                    else if (!isLockedToUser()) {
                        if (fileIsReadOnly) {
                            shell.getDisplay().asyncExec(new Runnable() {
                                public void run() {
                                    if (getLockingUser() == null) {
                                        fileIsReadOnly = false;
                                        boolean lock = MessageDialog.openQuestion(shell,
                                                "Lock " + getTitle(),
                                                "Lock resource '" + getLabel() + "' to user "
                                                        + getProject().getUser().getUsername()
                                                        + "?");
                                        if (lock) {
                                            try {
                                                getProject().getDesignerProxy()
                                                        .toggleWorkflowAssetLock(WorkflowAsset.this,
                                                                true);
                                                setLockingUser(
                                                        getProject().getUser().getUsername());
                                                setModifyDate(new Date());
                                                fireElementChangeEvent(ChangeType.PROPERTIES_CHANGE,
                                                        null);
                                            }
                                            catch (Exception ex) {
                                                PluginMessages.uiError(shell, ex, "Lock Resource",
                                                        getProject());
                                            }
                                        }
                                        else {
                                            MessageDialog.openWarning(shell,
                                                    "Won't Update " + getTitle(),
                                                    "Resource '" + getLabel()
                                                            + "' is not locked by you, so updates will not be persisted.");
                                        }
                                    }
                                    else {
                                        MessageDialog.openWarning(shell,
                                                "Won't Update " + getTitle(),
                                                "Resource '" + getLabel()
                                                        + "' is not locked by you, so updates will not be persisted.");
                                    }
                                }
                            });
                        }
                    }
                    return;
                }
                else {
                    if (previouslyDirty)
                        previouslyDirty = false; // file is being saved
                }
            }
        }
    }

    /**
     * Change listener so we'll know when the resource is changed in the
     * workspace.
     */
    public void resourceChanged(IResourceChangeEvent event) {
        if (isForceExternalEditor())
            return;

        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            final IFile file = getAssetFile();
            IResourceDelta rootDelta = event.getDelta();
            IResourceDelta assetDelta = rootDelta.findMember(file.getFullPath());
            if (assetDelta != null && assetDelta.getKind() == IResourceDelta.CHANGED
                    && (assetDelta.getFlags() & IResourceDelta.CONTENT) != 0) {
                // the file has been changed
                final Display display = Display.getCurrent();
                if (display != null) {
                    display.syncExec(new Runnable() {
                        public void run() {
                            if (isRawEdit()) {
                                if (getProject().isReadOnly())
                                    MessageDialog.openWarning(display.getActiveShell(),
                                            "Not Editable",
                                            "Your changes to " + getFile().getName()
                                                    + " will be overwritten the next time project '"
                                                    + getProject().getLabel() + "' is refreshed.");
                            }
                            else {
                                if (!isUserAuthorized(UserRoleVO.ASSET_DESIGN)) {
                                    MessageDialog.openWarning(display.getActiveShell(),
                                            "Can't Update " + getTitle(),
                                            "You're not authorized to update '" + getLabel()
                                                    + "'\nin workflow project '"
                                                    + getProject().getName() + "'.");
                                    return;
                                }
                                else if (!isLockedToUser()) {
                                    MessageDialog.openWarning(display.getActiveShell(),
                                            "Can't Update " + getTitle(), "Resource '" + getLabel()
                                                    + "' is not locked by you, so updates are not allowed.");
                                    return;
                                }
                            }

                            if (isBinary())
                                encodeAndSetContent(PluginUtil.readFile(file));
                            else
                                setContent(new String(PluginUtil.readFile(file)));

                            Increment versionIncrement = Increment.Overwrite;
                            int previousVersion = getVersion();
                            String versionComment = null;
                            if (getProject().checkRequiredVersion(5, 0)) {
                                VersionableSaveDialog saveDialog = new VersionableSaveDialog(
                                        display.getActiveShell(), WorkflowAsset.this);
                                int res = saveDialog.open();
                                if (res == VersionableSaveDialog.CANCEL) {
                                    if (isRawEdit()) {
                                        String message = "Version for '"
                                                + WorkflowAsset.this.getName() + "' remains "
                                                + WorkflowAsset.this.getVersionLabel();
                                        MessageDialog.openInformation(display.getActiveShell(),
                                                WorkflowAsset.this.getTitle() + " Overwrite",
                                                message);
                                        return;
                                    }
                                    else {
                                        String message = "Database save for '"
                                                + WorkflowAsset.this.getName()
                                                + "' was canceled.\nTemp file changes were not persisted.";
                                        MessageDialog.openWarning(display.getActiveShell(),
                                                WorkflowAsset.this.getTitle() + " Not Saved",
                                                message);
                                        return;
                                    }
                                }
                                versionIncrement = saveDialog.getVersionIncrement();
                                if (versionIncrement != Increment.Overwrite) {
                                    setVersion(versionIncrement == Increment.Major
                                            ? getNextMajorVersion() : getNextMinorVersion());
                                    versionComment = saveDialog.getVersionComment();
                                }
                            }
                            if (isRawEdit()) {
                                if (versionIncrement == Increment.Overwrite) {
                                    // just fire cache refresh
                                    if (!getProject().isRemote())
                                        getProject().getDesignerProxy().getCacheRefresh()
                                                .fireRefresh(RuleSetVO.JAVA.equals(getLanguage()));
                                }
                                else {
                                    setRevisionComment(versionComment);
                                    getProject().getDesignerProxy().saveWorkflowAssetWithProgress(
                                            WorkflowAsset.this, false);
                                    fireElementChangeEvent(ChangeType.VERSION_CHANGE, getVersion());
                                }
                            }
                            else {
                                try {
                                    IPreferenceStore prefsStore = MdwPlugin.getDefault()
                                            .getPreferenceStore();
                                    boolean keepLocked = prefsStore.getBoolean(
                                            PreferenceConstants.PREFS_KEEP_RESOURCES_LOCKED_WHEN_SAVING);

                                    if (versionIncrement != Increment.Overwrite) {
                                        RuleSetVO prevVO = new RuleSetVO(
                                                WorkflowAsset.this.getRuleSetVO());
                                        prevVO.setId(getId());
                                        prevVO.setVersion(previousVersion);
                                        setComment(versionComment);
                                        if (!isInDefaultPackage()) {
                                            getPackage().removeAsset(WorkflowAsset.this);
                                            getProject().getUnpackagedWorkflowAssets()
                                                    .add(new WorkflowAsset(prevVO,
                                                            getProject().getDefaultPackage()));
                                        }
                                        RunnerResult result = getProject().getDesignerProxy()
                                                .createNewWorkflowAsset(WorkflowAsset.this,
                                                        keepLocked);
                                        if (result.getStatus() == RunnerStatus.SUCCESS) {
                                            getRuleSetVO().setPrevVersion(prevVO);
                                            fireElementChangeEvent(ChangeType.VERSION_CHANGE,
                                                    getVersion());
                                        }
                                        else if (result.getStatus() == RunnerStatus.DISALLOW) {
                                            // deregister since save never
                                            // happened
                                            WorkflowAssetFactory
                                                    .deRegisterAsset(WorkflowAsset.this);
                                        }
                                    }
                                    else {
                                        getProject().getDesignerProxy()
                                                .saveWorkflowAssetWithProgress(WorkflowAsset.this,
                                                        keepLocked);
                                        fireElementChangeEvent(ChangeType.SETTINGS_CHANGE, null);
                                    }
                                    if (!keepLocked)
                                        fireElementChangeEvent(ChangeType.PROPERTIES_CHANGE, null);
                                }
                                catch (Exception ex) {
                                    PluginMessages.uiError(ex, "Save Definition Doc", getProject());
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    public class AssetWorkbenchListener implements IWorkbenchListener {
        private IEditorPart fileEditor;

        public AssetWorkbenchListener(IEditorPart fileEditor) {
            this.fileEditor = fileEditor;
        }

        public boolean preShutdown(IWorkbench workbench, boolean forced) {
            // close open editor and remove temp file
            IFolder tempFolder = getTempFolder();
            IFile tempFile = getTempFile(tempFolder);
            MdwPlugin.getActivePage().closeEditor(fileEditor, true);
            try {
                new TempFileRemover(tempFolder, tempFile).remove(null);
            }
            catch (Exception ex) {
                PluginMessages.log(ex);
            }
            return true;
        }

        public void postShutdown(IWorkbench workbench) {
        }

        public WorkflowAsset getAsset() {
            return WorkflowAsset.this;
        }
    }

    public IFile getFile() {
        assert getProject().isFilePersist();
        return getProject().getSourceProject().getFile("/" + getVcsAssetPath());
    }

    public void ensureFileWritable() throws CoreException {
        IFile file = getFile();
        if (file.isReadOnly()) {
            ResourceAttributes resourceAttrs = file.getResourceAttributes();
            resourceAttrs.setReadOnly(false);
            file.setResourceAttributes(resourceAttrs);
        }
    }

    @Override
    public void fireElementChangeEvent(ChangeType changeType, Object newValue) {
        super.fireElementChangeEvent(changeType, newValue);
        if (getProject().isFilePersist()) {
            if (changeType == ChangeType.ELEMENT_CREATE || changeType == ChangeType.ELEMENT_DELETE
                    || changeType == ChangeType.RENAME)
                getPackage().refreshFolder();
            else if (changeType == ChangeType.VERSION_CHANGE)
                getPackage().refreshMdwMetaFolder();
        }
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        if (adapter.equals(IFile.class) && getFile() != null)
            return getFile();
        else
            return null;
    }

    /**
     * don't change the format of this output since it is use for drag-and-drop
     * support
     */
    public String toString() {
        String packageLabel = getPackage() == null || getPackage().isDefaultPackage() ? ""
                : getPackage().getLabel();
        String projectName = getProject() == null ? "" : getProject().getName();
        return "WorkflowAsset~" + projectName + "^" + packageLabel + "^" + getId();
    }
}
