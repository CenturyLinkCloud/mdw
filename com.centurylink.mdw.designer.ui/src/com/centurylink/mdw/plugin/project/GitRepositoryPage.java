package com.centurylink.mdw.plugin.project;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.project.model.VcsRepository;

public class GitRepositoryPage extends WizardPage implements IFacetWizardPage {
    public static final String PAGE_TITLE = "Git Remote Repository Settings";

    private Text repositoryUrlTextField;
    private Text gitBranchTextField;
    private Text gitUserTextField;
    private Text gitPasswordTextField;
    private Text localPathTextField;

    public GitRepositoryPage() {
        setTitle(PAGE_TITLE);
        setDescription("Enter your remote Git repository information.");
    }

    public void initValues() {
        getRepository().setProvider(VcsRepository.PROVIDER_GIT);

        String prevUrl = "";
        String prevUser = "";

        if (!getProject().isRemote()) {
            String prefix = "MDW" + getProject().getMdwVersion();
            prevUrl = MdwPlugin.getStringPref(prefix + "-" + ProjectPersist.MDW_VCS_REPO_URL);
            prevUser = MdwPlugin.getStringPref(prefix + "-" + ProjectPersist.MDW_VCS_USER);
        }

        if (prevUser.length() > 0)
            getRepository().setUser(prevUser);
        else
            getRepository().setUser(VcsRepository.DEFAULT_USER);
        gitUserTextField.setText(getRepository().getUser());

        if (prevUrl.length() > 0)
            getRepository().setRepositoryUrl(prevUrl);
        else
            getRepository().setRepositoryUrl(VcsRepository.DEFAULT_REPOSITORY_URL);
        repositoryUrlTextField.setText(getRepository().getRepositoryUrl());

        if (prevUser.length() == 0) {
            getRepository().setPassword(VcsRepository.DEFAULT_PASSWORD);
            gitPasswordTextField.setText(getRepository().getPassword());
        }

        gitBranchTextField.setText(VcsRepository.DEFAULT_BRANCH);
        localPathTextField.setText(VcsRepository.DEFAULT_LOCAL_PATH);
    }

    private VcsRepository getRepository() {
        if (getProject() == null)
            return null;
        return getProject().getMdwVcsRepository();
    }

    /**
     * draw the widgets using a grid layout
     * 
     * @param parent
     *            - the parent composite
     */
    public void drawWidgets(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NULL);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        int ncol = 4;
        gl.numColumns = ncol;
        composite.setLayout(gl);

        createRepositoryUrlControls(composite, ncol);
        createGitBranchControls(composite, ncol);
        createGitUserControls(composite, ncol);
        createGitPasswordControls(composite, ncol);
        createLocalPathControls(composite, ncol);
        setControl(composite);
    }

    /**
     * @see WizardPage#getStatuses()
     */
    public IStatus[] getStatuses() {
        if (isPageComplete())
            return null;

        String msg = null;
        if (containsWhitespace(getRepository().getRepositoryUrl()))
            msg = "Invalid value for Repository URL";

        if (msg == null)
            return null;

        IStatus[] is = { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
        return is;
    }

    /**
     * sets the completed field on the wizard class when all the information on
     * the page is entered
     */
    public boolean isPageComplete() {
        if (getRepository().getRepositoryUrl() == null)
            return true; // page may not be used
        return checkStringNoWhitespace(getRepository().getRepositoryUrl())
                && checkString(getRepository().getBranch())
                && checkString(getRepository().getLocalPath());
    }

    private void createRepositoryUrlControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Repository URL:");

        repositoryUrlTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 400;
        gd.horizontalSpan = ncol - 1;
        repositoryUrlTextField.setLayoutData(gd);

        repositoryUrlTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String repoUrl = repositoryUrlTextField.getText().trim();
                getRepository().setRepositoryUrl(repoUrl.isEmpty() ? null : repoUrl);
                handleFieldChanged();
            }
        });
    }

    private void createGitBranchControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Branch:");

        gitBranchTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.horizontalSpan = ncol - 1;
        gitBranchTextField.setLayoutData(gd);
        gitBranchTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getRepository().setBranch(gitBranchTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    private void createGitUserControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("User:");

        gitUserTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.horizontalSpan = ncol - 1;
        gitUserTextField.setLayoutData(gd);
        gitUserTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String text = gitUserTextField.getText().trim();
                getRepository().setUser(text.isEmpty() ? null : text);
                handleFieldChanged();
            }
        });
    }

    private void createGitPasswordControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Password:");

        gitPasswordTextField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.horizontalSpan = ncol - 1;
        gitPasswordTextField.setLayoutData(gd);
        gitPasswordTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String text = gitPasswordTextField.getText().trim();
                getRepository().setPassword(text.isEmpty() ? null : text);
                handleFieldChanged();
            }
        });
    }

    private void createLocalPathControls(Composite parent, int ncol) {
        new Label(parent, SWT.NONE).setText("Local Path:");

        localPathTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        gd.horizontalSpan = ncol - 1;
        localPathTextField.setLayoutData(gd);
        localPathTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getRepository().setLocalPath(localPathTextField.getText().trim());
                handleFieldChanged();
            }
        });
    }

    @Override
    public void setWizardContext(IWizardContext context) {
    }

    @Override
    public void transferStateToConfig() {
    }
}
