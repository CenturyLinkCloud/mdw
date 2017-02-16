/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.MDWActivity;
import com.centurylink.mdw.bpm.MDWProcess;
import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.PackageDocument;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.dataaccess.file.ImporterExporterJson;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.Discoverer;
import com.centurylink.mdw.plugin.designer.dialogs.MdwProgressMonitorDialog;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.preferences.UrlsPreferencePage;

public class ImportPackagePage extends WizardPage {
    private Button importFileRadio;
    private Button discoverRadio;
    private Text filePathText;
    private Button browseImportFileButton;
    private Text discoveryUrlText;
    private Button latestVersionsCheckbox;

    private Folder packageFolder;
    private WorkflowElement preselected;

    public ImportPackagePage() {
        setTitle("Import MDW Workflow Package(s)");
        setDescription("Import design assets into your workflow project.");
    }

    public void init(IStructuredSelection selection) {
        super.init(selection);
    }

    @Override
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 3;
        gl.numColumns = ncol;
        composite.setLayout(gl);

        createWorkflowProjectControls(composite, ncol, true);
        createSpacer(composite, ncol);
        createImportControls(composite, ncol);

        // TODO option to launch directly to Discovery mode
        enableDiscoveryControls(isDiscovery());
        enableFileControls(!isDiscovery());

        setControl(composite);

        filePathText.forceFocus();
    }

    private void createImportControls(Composite parent, int ncol) {
        Group radioGroup = new Group(parent, SWT.NONE);
        radioGroup.setText("Import Type");
        GridLayout gl = new GridLayout();
        gl.numColumns = 3;
        radioGroup.setLayout(gl);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol;
        radioGroup.setLayoutData(gd);

        importFileRadio = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 3;
        importFileRadio.setLayoutData(gd);
        importFileRadio.setSelection(true);
        importFileRadio.setText("From File");
        importFileRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = importFileRadio.getSelection();
                discoverRadio.setSelection(!selected);
                enableDiscoveryControls(!selected);
                enableFileControls(selected);
                handleFieldChanged();
            }
        });

        Label label = new Label(radioGroup, SWT.NONE);
        label.setText("Package File:");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 25;
        label.setLayoutData(gd);

        filePathText = new Text(radioGroup, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 350;
        filePathText.setLayoutData(gd);
        filePathText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                handleFieldChanged();
            }
        });

        browseImportFileButton = new Button(radioGroup, SWT.PUSH);
        browseImportFileButton.setText("Browse...");
        browseImportFileButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dlg = new FileDialog(getShell());
                dlg.setFilterExtensions(new String[] { "*.json", "*.xml" });
                String res = dlg.open();
                if (res != null)
                    filePathText.setText(res);
            }
        });

        discoverRadio = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 3;
        discoverRadio.setLayoutData(gd);
        discoverRadio.setSelection(false);
        discoverRadio.setText("Discover");
        discoverRadio.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean selected = discoverRadio.getSelection();
                importFileRadio.setSelection(!selected);
                enableFileControls(!selected);
                enableDiscoveryControls(selected);
                handleFieldChanged();
            }
        });

        label = new Label(radioGroup, SWT.NONE);
        label.setText("Asset Discovery URL:");
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalIndent = 25;
        label.setLayoutData(gd);

        discoveryUrlText = new Text(radioGroup, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 350;
        gd.horizontalSpan = 2;
        discoveryUrlText.setLayoutData(gd);
        String discUrl = MdwPlugin.getSettings().getDiscoveryUrl();
        discoveryUrlText.setText(discUrl.endsWith("/") ? discUrl + "Assets" : discUrl + "/assets");
        discoveryUrlText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                handleFieldChanged();
            }
        });

        createSpacer(radioGroup, 3);

        createSpacer(radioGroup, 1);
        latestVersionsCheckbox = new Button(radioGroup, SWT.CHECK | SWT.LEFT);
        gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        latestVersionsCheckbox.setLayoutData(gd);
        latestVersionsCheckbox.setText("Show only latest released versions");
        latestVersionsCheckbox.setSelection(true);

        new Label(radioGroup, SWT.NONE);
        Link link = new Link(radioGroup, SWT.SINGLE);
        link.setText(" Configure <A>Default Discovery base URL</A>");
        link.setLayoutData(new GridData(GridData.END));
        link.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(getShell(),
                        UrlsPreferencePage.PREF_PAGE_ID, null, null);
                if (pref != null) {
                    pref.open();
                    String discUrl = MdwPlugin.getSettings().getDiscoveryUrl();
                    discoveryUrlText.setText(
                            discUrl.endsWith("/") ? discUrl + "Assets" : discUrl + "/assets");
                }
            }
        });
    }

    private void enableFileControls(boolean enabled) {
        if (filePathText.isEnabled() != enabled) {
            filePathText.setEnabled(enabled);
            browseImportFileButton.setEnabled(enabled);
        }
    }

    private void enableDiscoveryControls(boolean enabled) {
        if (discoveryUrlText.isEnabled() != enabled)
            discoveryUrlText.setEnabled(enabled);
        if (latestVersionsCheckbox.isEnabled() != enabled)
            latestVersionsCheckbox.setEnabled(enabled);
    }

    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    @Override
    public boolean isPageComplete() {
        return isPageValid();
    }

    boolean isPageValid() {
        if (getProject() == null)
            return false;
        if (!getProject().isFilePersist() && !getProject().getDesignerDataModel()
                .userHasRoleInAnyGroup(UserRoleVO.PROCESS_DESIGN))
            return false;
        if (isDiscovery()) {
            return discoveryUrlText != null && checkUrl(discoveryUrlText.getText());
        }
        else {
            return filePathText != null && checkFile(filePathText.getText());
        }
    }

    boolean isDiscovery() {
        return discoverRadio != null && discoverRadio.getSelection();
    }

    public IStatus[] getStatuses() {
        String msg = null;
        if (getProject() == null)
            msg = "Please select a valid workflow project";
        else if (!getProject().isFilePersist() && !getProject().getDesignerDataModel()
                .userHasRoleInAnyGroup(UserRoleVO.PROCESS_DESIGN))
            msg = "You're not authorized to import into this workflow project.";
        else {
            if (isDiscovery()) {
                if (!checkUrl(discoveryUrlText.getText()))
                    msg = "Please enter a valid Discovery URL.";
            }
            else {
                if (!checkFile(filePathText.getText()))
                    msg = "Please enter a valid file path.";
            }
        }

        if (msg == null)
            return null;

        IStatus[] is = { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
        return is;
    }

    @Override
    public WorkflowElement getElement() {
        return ((ImportPackageWizard) getWizard()).getTopFolder();
    }

    @Override
    public IWizardPage getNextPage() {
        ImportPackageWizard wiz = (ImportPackageWizard) getWizard();

        if (isDiscovery()) {
            preselected = null;
            ((ImportPackageWizard) getWizard()).setHasOldImplementors(false);

            final String url = discoveryUrlText.getText().trim();
            final boolean latestVersionsOnly = latestVersionsCheckbox.getSelection();

            // display a progress dialog since this can take a while
            ProgressMonitorDialog pmDialog = new MdwProgressMonitorDialog(getShell());
            try {
                pmDialog.run(true, true, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                        monitor.beginTask("Crawling for workflow assets...", 100);
                        try {
                            packageFolder = populateTopFolder(url, latestVersionsOnly, monitor);
                            monitor.done();
                        }
                        catch (InterruptedException ex) {
                            throw ex;
                        }
                        catch (Exception ex) {
                            throw new InvocationTargetException(ex);
                        }
                    }
                });
            }
            catch (InterruptedException iex) {
            }
            catch (Exception ex) {
                PluginMessages.uiError(getShell(), ex, "Discover Packages", getProject());
                return null;
            }
        }
        else {
            BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                public void run() {
                    try {
                        packageFolder = populateTopFolder(null, false, null);
                    }
                    catch (Exception ex) {
                        PluginMessages.uiError(getShell(), ex, "Import Packages", getProject());
                    }
                }
            });
        }

        if (packageFolder == null) {
            return null;
        }
        else {
            wiz.setFolder(packageFolder);
            wiz.initializePackageSelectPage(preselected);
            return ((ImportPackageWizard) getWizard()).getImportPackageSelectPage();
        }
    }

    public Folder populateTopFolder(String discoveryUrl, boolean latestVersionsOnly,
            IProgressMonitor progressMonitor) throws Exception {
        Folder folder = null;
        getImportPackageWizard().getImportPackageSelectPage().clear();
        if (discoveryUrl != null) {
            folder = new Discoverer(new URL(discoveryUrl)).getAssetTopFolder(latestVersionsOnly,
                    progressMonitor);
            if (getProject().isRemote() && getProject().isGitVcs()) {
                List<Folder> emptyFolders = removeGitVersionedPackages(folder);
                List<Folder> emptyParents = new ArrayList<Folder>();
                for (Folder emptyFolder : emptyFolders) {
                    if (emptyFolder.getParent() instanceof Folder) {
                        Folder parent = (Folder) emptyFolder.getParent();
                        parent.getChildren().remove(emptyFolder);
                        // go one more level up
                        if (parent.getChildren().isEmpty() && !emptyParents.contains(parent))
                            emptyParents.add(parent);
                    }
                }
                for (Folder emptyParent : emptyParents) {
                    if (emptyParent.getParent() instanceof Folder)
                        ((Folder) emptyParent.getParent()).getChildren().remove(emptyParent);
                }
            }
        }
        else {
            String filepath = filePathText.getText().trim();
            String contents = FileHelper.getFileContents(filepath);
            folder = new Folder(filepath);
            boolean hasOldImpls = false;
            if (contents.trim().startsWith("{")) {
                ImporterExporterJson importer = new ImporterExporterJson();
                List<PackageVO> packages = importer.importPackages(contents);
                for (PackageVO pkg : packages) {
                    if (getProject().isRemote() && getProject().isGitVcs()) {
                        for (WorkflowPackage existingVcs : getProject().getTopLevelPackages()) {
                            if (existingVcs.getName().equals(pkg.getName()))
                                getImportPackageWizard().getImportPackageSelectPage()
                                        .setError("Package already exists in version control: "
                                                + pkg.getName());
                        }
                    }
                    File aFile = new File(folder, pkg.getName() + " v" + pkg.getVersionString());
                    ImporterExporterJson jsonExporter = new ImporterExporterJson();
                    List<PackageVO> pkgs = new ArrayList<PackageVO>();
                    pkgs.add(pkg);
                    JSONObject pkgJson = new JSONObject(jsonExporter.exportPackages(pkgs));
                    pkgJson.put("name", pkg.getName());
                    aFile.setContent(pkgJson.toString(2));
                    folder.addChild(aFile);
                }
                preselected = folder;
            }
            else {
                try {
                    // try and parse as multiple packages
                    PackageDocument pkgDoc = PackageDocument.Factory.parse(contents);
                    QName docElement = new QName("http://mdw.centurylink.com/bpm",
                            "processDefinition");
                    for (MDWProcessDefinition pkgDef : pkgDoc.getPackage()
                            .getProcessDefinitionList()) {
                        if (getProject().isRemote() && getProject().isGitVcs()) {
                            for (WorkflowPackage existingVcs : getProject().getTopLevelPackages()) {
                                if (existingVcs.getName().equals(pkgDef.getPackageName()))
                                    getImportPackageWizard().getImportPackageSelectPage()
                                            .setError("Package already exists in version control: "
                                                    + pkgDef.getPackageName());
                            }
                        }
                        if (!hasOldImpls && getProject().isFilePersist()
                                && !getProject().isRemote())
                            hasOldImpls = checkForOldImplementors(pkgDef);
                        File aFile = new File(folder,
                                pkgDef.getPackageName() + " v" + pkgDef.getPackageVersion());
                        aFile.setContent(pkgDef.xmlText(new XmlOptions().setSaveOuter()
                                .setSaveSyntheticDocumentElement(docElement)));
                        folder.addChild(aFile);
                    }
                    preselected = folder;
                }
                catch (XmlException ex) {
                    // unparseable -- assume single package
                    if (getProject().isRemote() && getProject().isGitVcs()) {
                        MDWProcessDefinition procDef = ProcessDefinitionDocument.Factory
                                .parse(contents, Compatibility.namespaceOptions())
                                .getProcessDefinition();
                        for (WorkflowPackage existingVcs : getProject().getTopLevelPackages()) {
                            if (existingVcs.getName().equals(procDef.getPackageName()))
                                getImportPackageWizard().getImportPackageSelectPage()
                                        .setError("Package already exists in version control: "
                                                + procDef.getPackageName());
                        }
                    }
                    if (getProject().isFilePersist() && !getProject().isRemote())
                        hasOldImpls = checkForOldImplementors(ProcessDefinitionDocument.Factory
                                .parse(contents, Compatibility.namespaceOptions())
                                .getProcessDefinition());
                    File file = new File(folder, filepath);
                    file.setContent(contents);
                    folder.addChild(file);
                    preselected = file;
                }
            }
            getImportPackageWizard().setHasOldImplementors(hasOldImpls);
        }

        return folder;
    }

    private ImportPackageWizard getImportPackageWizard() {
        return ((ImportPackageWizard) getWizard());
    }

    /**
     * Not foolproof since it relies on asset XML naming convention. returns
     * emptyFolders to be pruned.
     */
    private List<Folder> removeGitVersionedPackages(Folder folder) {
        List<Folder> emptyFolders = new ArrayList<Folder>();
        Map<File, Folder> toRemove = new HashMap<File, Folder>();
        for (WorkflowElement child : folder.getChildren()) {
            if (child instanceof Folder) {
                for (Folder emptyFolder : removeGitVersionedPackages((Folder) child)) {
                    if (!emptyFolders.contains(emptyFolder))
                        emptyFolders.add(emptyFolder);
                }
            }
            else if (child instanceof File) {
                File file = (File) child;
                String pkgName = file.getName();
                if (file.getParent() instanceof Folder && pkgName.endsWith(".xml")) {
                    pkgName = pkgName.substring(0, pkgName.length() - 3);
                    int lastDash = pkgName.lastIndexOf('-');
                    if (lastDash > 0) {
                        pkgName = pkgName.substring(0, lastDash);
                        for (WorkflowPackage gitPackage : getProject().getTopLevelPackages()) {
                            if (pkgName.equals(gitPackage.getName())) {
                                PluginMessages.log("Import excludes VCS package: " + pkgName);
                                toRemove.put(file, (Folder) file.getParent());
                            }
                        }
                    }
                }
            }
        }
        if (!toRemove.isEmpty()) {
            getImportPackageWizard().getImportPackageSelectPage().setInfo(
                    "Some packages are not displayed since they exist in version control.");
            for (File file : toRemove.keySet()) {
                Folder removeFrom = toRemove.get(file);
                removeFrom.getChildren().remove(file);
                if (removeFrom.getChildren().isEmpty())
                    emptyFolders.add(removeFrom);
            }
        }
        return emptyFolders;
    }

    private boolean checkForOldImplementors(MDWProcessDefinition pkgDef) {
        try {
            for (MDWProcess proc : pkgDef.getProcessList()) {
                for (MDWActivity act : proc.getActivityList()) {
                    if (Compatibility.isOldImplementor(act.getImplementation()))
                        return true;
                }
            }
        }
        catch (Exception ex) {
            PluginMessages.log(ex); // silently fail and return false
        }
        return false;
    }
}