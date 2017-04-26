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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.Importer;
import com.centurylink.mdw.plugin.designer.SwtProgressMonitor;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ImportPackageWizard extends Wizard implements IImportWizard {
    private ImportPackagePage importPackagePage;
    private ImportPackageSelectPage importPackageSelectPage;

    private List<java.io.File> fileList = new ArrayList<java.io.File>();

    ImportPackageSelectPage getImportPackageSelectPage() {
        return importPackageSelectPage;
    }

    private IWorkbench workbench;

    public IWorkbench getWorkbench() {
        return workbench;
    }

    private Folder topFolder;

    public Folder getTopFolder() {
        return topFolder;
    }

    public Folder getFolder() {
        return (Folder) topFolder.getChildren().get(0);
    }

    void setFolder(Folder folder) {
        if (topFolder.getChildren() != null)
            topFolder.getChildren().clear();
        topFolder.addChild(folder);
    }

    private boolean discovery;

    public boolean isDiscovery() {
        return discovery;
    }

    void setDiscovery(boolean disc) {
        this.discovery = disc;
    }

    boolean upgradeAssets;

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setDefaultPageImageDescriptor(MdwPlugin.getImageDescriptor("icons/mdw_wiz.png"));
        setNeedsProgressMonitor(true);
        setWindowTitle("MDW Import");

        importPackagePage = new ImportPackagePage();
        importPackageSelectPage = new ImportPackageSelectPage();

        topFolder = new Folder("assets");

        if (selection != null && selection.getFirstElement() instanceof WorkflowProject) {
            WorkflowProject workflowProject = (WorkflowProject) selection.getFirstElement();
            topFolder.setProject(workflowProject);
        }
        else if (selection != null && selection.getFirstElement() instanceof WorkflowPackage) {
            WorkflowPackage packageVersion = (WorkflowPackage) selection.getFirstElement();
            topFolder.setProject(packageVersion.getProject());
        }
        else {
            WorkflowProject workflowProject = WorkflowProjectManager.getInstance()
                    .findWorkflowProject(selection);
            if (workflowProject != null)
                topFolder.setProject(workflowProject);
        }
    }

    @Override
    public boolean performFinish() {
        final List<WorkflowPackage> importedPackages = new ArrayList<WorkflowPackage>();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                try {
                    WorkflowProject wfp = topFolder.getProject();
                    DesignerProxy designerProxy = wfp.getDesignerProxy();

                    monitor.beginTask("Import Packages",
                            100 * importPackageSelectPage.getSelectedPackages().size());
                    monitor.subTask("Importing selected packages...");
                    monitor.worked(10);

                    StringBuffer sb = new StringBuffer();
                    ProgressMonitor progressMonitor = new SwtProgressMonitor(
                             new SubProgressMonitor(monitor, 100));
                    for (File pkgFile : importPackageSelectPage.getSelectedPackages()) {
                        if (pkgFile.getContent() == null) { // download
                                                            // postponed
                                                            // for
                                                            // discovered
                            if (pkgFile.getUrl() != null) { // assets
                                HttpHelper httpHelper = new HttpHelper(pkgFile.getUrl());
                                httpHelper.setConnectTimeout(
                                        MdwPlugin.getSettings().getHttpConnectTimeout());
                                httpHelper.setReadTimeout(
                                        MdwPlugin.getSettings().getHttpReadTimeout());
                                pkgFile.setContent(httpHelper.get());
                            }
                            else {
                                String fileName = pkgFile.getName();
                                if (sb.length() > 0)
                                    sb.append("," + fileName.substring(0, fileName.indexOf(" ")));
                                else
                                    sb.append(fileName.substring(0, fileName.indexOf(" ")));
                            }
                        }
                        String pkgFileContent = pkgFile.getContent();
                        if (pkgFileContent != null) {
                            Importer importer = new Importer(designerProxy.getPluginDataAccess(),
                                    wfp.isFilePersist() && wfp.isRemote() ? null : getShell());
                            WorkflowPackage importedPackage = importer.importPackage(wfp,
                                    pkgFileContent, progressMonitor);
                            if (importedPackage == null) // canceled
                            {
                                progressMonitor.done();
                                break;
                            }
                            else {
                                if (upgradeAssets) {
                                    progressMonitor.subTask(
                                            "Upgrading activity implementors and other assets...");
                                    designerProxy.upgradeAssets(importedPackage);
                                }

                                if (wfp.isFilePersist()) // file system eclipse
                                                         // sync
                                    wfp.getSourceProject().refreshLocal(2, null);
                                // TODO refresh Archive in case existing package
                                // was
                                // moved there

                                importedPackage.addElementChangeListener(wfp);
                                for (WorkflowProcess pv : importedPackage.getProcesses())
                                    pv.addElementChangeListener(wfp);
                                for (ExternalEvent ee : importedPackage.getExternalEvents())
                                    ee.addElementChangeListener(wfp);
                                for (ActivityImpl ai : importedPackage.getActivityImpls())
                                    ai.addElementChangeListener(wfp);
                                for (WorkflowAsset wa : importedPackage.getAssets())
                                    wa.addElementChangeListener(wfp);
                                importedPackages.add(importedPackage);

                                if (wfp.isRemote() && wfp.isFilePersist()) {
                                    // zip and upload imported packages to
                                    // server
                                    java.io.File tempDir = wfp.getTempDir();
                                    if (!tempDir.exists()) {
                                        if (!tempDir.mkdirs())
                                            throw new IOException(
                                                    "Unable to create temp directory: " + tempDir);
                                    }
                                    java.io.File zipFile = new java.io.File(tempDir + "/packages"
                                            + StringHelper.filenameDateToString(new Date())
                                            + ".zip");
                                    java.io.File assetDir = wfp.getAssetDir();
                                    List<java.io.File> includes = new ArrayList<java.io.File>();
                                    for (WorkflowPackage pkg : importedPackages)
                                        includes.add(new java.io.File(
                                                assetDir + "/" + pkg.getName().replace('.', '/')));
                                    // TODO populate excludes with non-imported
                                    // package dirs (why, since these are not in
                                    // includes?)
                                    FileHelper.createZipFileWith(assetDir, zipFile, includes);
                                    String uploadUrl = wfp.getServiceUrl()
                                            + "/upload?overwrite=true&assetZip=true&user="
                                            + importPackagePage.getProject().getUser()
                                                    .getUsername();
                                    InputStream is = new FileInputStream(zipFile);
                                    try {
                                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                                        int read = 0;
                                        byte[] bytes = new byte[1024];
                                        while ((read = is.read(bytes)) != -1)
                                            os.write(bytes, 0, read);

                                        String encryptedPassword = CryptUtil
                                                .encrypt(wfp.getMdwDataSource().getDbPassword());
                                        HttpHelper httpHelper = new HttpHelper(new URL(uploadUrl),
                                                wfp.getMdwDataSource().getDbUser(),
                                                encryptedPassword);
                                        byte[] resp = httpHelper.postBytes(os.toByteArray());
                                        PluginMessages
                                                .log("Asset download respose: " + new String(resp));
                                    }
                                    finally {
                                        is.close();
                                    }
                                }
                            }
                            progressMonitor.done();
                        }
                    }
                    if (sb.length() > 0) {
                        java.io.File tempDir = wfp.getTempDir();
                        if (!tempDir.exists()) {
                            if (!tempDir.mkdirs())
                                throw new IOException(
                                        "Unable to create temp directory: " + tempDir);
                        }
                        java.io.File tempFile = new java.io.File(tempDir + "/pkgDownload_"
                                + StringHelper.filenameDateToString(new Date()) + ".zip");
                        String url = MdwPlugin.getSettings().getDiscoveryUrlMdw6()
                                + "/asset/packages?packages=" + sb.toString();
                        HttpHelper httpHelper = new HttpHelper(new URL(url));
                        httpHelper
                                .setConnectTimeout(MdwPlugin.getSettings().getHttpConnectTimeout());
                        httpHelper.setReadTimeout(MdwPlugin.getSettings().getHttpReadTimeout());
                        httpHelper.download(tempFile);
                        VersionControl vcs = new VersionControlGit();
                        vcs.connect(null, null, null, wfp.getAssetDir());
                        progressMonitor.subTask("Archive existing assets...");
                        java.io.File assetDir = wfp.getAssetDir();
                        VcsArchiver archiver = new VcsArchiver(assetDir, tempDir, vcs,
                                progressMonitor);
                        archiver.backup();
                        PluginMessages.log("Unzipping " + tempFile + " into: " + assetDir);
                        FileHelper.unzipFile(tempFile, assetDir, null, null, true);
                        archiver.archive();
                        progressMonitor.done();
                        FileHelper.unzipFile(tempFile, tempDir, null, null, true);
                        wfp.getSourceProject().refreshLocal(2, null);
                        java.io.File explodedDir = new java.io.File(tempDir + "/com");
                        if (explodedDir.isDirectory()) {
                            for (java.io.File file : getFilesRecursive(explodedDir,
                                    ".json")) {
                                WorkflowPackage workflowPackage = new WorkflowPackage();
                                workflowPackage.setProject(wfp);
                                workflowPackage.setPackageVO(new PackageVO(new JSONObject(
                                        FileHelper.getFileContents(file.getPath()))));
                                workflowPackage.addElementChangeListener(wfp);
                                importedPackages.add(workflowPackage);
                            }
                            FileHelper.deleteRecursive(explodedDir);
                        }
                        tempFile.delete();
                    }
                    wfp.getDesignerProxy().getCacheRefresh().doRefresh(true);
                }
                catch (ActionCancelledException ex) {
                    throw new OperationCanceledException();
                }
                catch (Exception ex) {
                    PluginMessages.log(ex);
                    throw new InvocationTargetException(ex);
                }
            }
        };

        try {
            getContainer().run(true, true, op);
            for (WorkflowPackage importedPackage : importedPackages)
                importedPackage.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, importedPackage);
            if (importedPackages.size() > 0)
                DesignerPerspective.promptForShowPerspective(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
                        importedPackages.get(0));
            return true;
        }
        catch (InterruptedException ex) {
            MessageDialog.openInformation(getShell(), "Import Package", "Import Cancelled");
            return true;
        }
        catch (Exception ex) {
            PluginMessages.uiError(getShell(), ex, "Import Package",
                    importPackagePage.getProject());
            return false;
        }
    }

    @Override
    public void addPages() {
        addPage(importPackagePage);
        addPage(importPackageSelectPage);
    }

    void initializePackageSelectPage(WorkflowElement preselected) {
        importPackageSelectPage.initialize(preselected);
    }

    void setHasOldImplementors(boolean hasOldImplementors) {
        importPackageSelectPage.showUpgradeAssetsComposite(hasOldImplementors);
    }

    public List<java.io.File> getFilesRecursive(java.io.File src, String extn) {
        for (java.io.File file : src.listFiles()) {
            if (file.isFile() && file.getName().endsWith(extn))
                fileList.add(file);
            else if (file.isDirectory())
                getFilesRecursive(file, extn);
        }
        return fileList;
    }
}
