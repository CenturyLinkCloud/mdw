/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.WorkflowProject.PersistType;

public class RemoteHostInfoPage extends WizardPage
{
  public static final String PAGE_TITLE = "Remote Host";
  public static final String DEFAULT_HOST = "ne1itcdrhews10.dev.intranet";
  public static final int DEFAULT_PORT = 12081;
  public static final String DEFAULT_CONTEXT_ROOT = "MDWWeb";
  public static final String DEFAULT_CLOUD_CONTEXT_ROOT = "mdw";

  private Text remoteHostTextField;
  private Text remotePortTextField;
  private Text webContextRootTextField;
  private Button gitCheckbox;
  private Button productionCheckbox;

  /**
   * Constructor.
   */
  public RemoteHostInfoPage()
  {
    setTitle(PAGE_TITLE);
    setDescription("Enter the MDW Remote Host (Managed Server) information");
  }

  /**
   * draw the widgets using a grid layout
   * @param parent - the parent composite
   */
  public void drawWidgets(Composite parent)
  {
    // create the composite to hold the widgets
    Composite composite = new Composite(parent, SWT.NULL);

    // create the layout for this wizard page
    GridLayout gl = new GridLayout();
    int ncol = 4;
    gl.numColumns = ncol;
    composite.setLayout(gl);

    createRemoteHostControls(composite, ncol);
    createRemotePortControls(composite, ncol);
    createSpacer(composite, ncol);
    createWebContextRootControls(composite, ncol);
    createSpacer(composite, ncol);
    createGitControls(composite, ncol);
    createSpacer(composite, ncol);
    createProductionControls(composite, ncol);
    setControl(composite);
    populateDefaults();
  }

  /**
   * @see WizardPage#getStatuses()
   */
  public IStatus[] getStatuses()
  {
    if (isPageComplete())
      return null;

    String msg = null;
    ServerSettings wlSettings = getProject().getServerSettings();
    if (containsWhitespace(wlSettings.getHost()))
      msg = "Invalid value for Remote Host";
    else if (wlSettings.getPort() <= 0)
      msg = "Invalid value for Remote Port";

    if (msg == null)
      return null;

    IStatus[] is = {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
    return is;
  }

  /**
   * sets the completed field on the wizard class when all the information
   * on the page is entered
   */
  public boolean isPageComplete()
  {
    ServerSettings wlSettings = getProject().getServerSettings();
    return checkStringNoWhitespace(wlSettings.getHost())
      && checkInt(wlSettings.getPort());
  }

  private void createRemoteHostControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Host:");
    remoteHostTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    remoteHostTextField.setLayoutData(gd);
    remoteHostTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          getProject().getServerSettings().setHost(remoteHostTextField.getText().trim());
          handleFieldChanged();
        }
      });
  }

  private void createRemotePortControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Port:");

    remotePortTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    remotePortTextField.setLayoutData(gd);
    remotePortTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          getProject().getServerSettings().setPort(Integer.parseInt(remotePortTextField.getText().trim()));
          handleFieldChanged();
        }
      });
  }

  private void createWebContextRootControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Context Root:");
    webContextRootTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    webContextRootTextField.setLayoutData(gd);
    webContextRootTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          getProject().setWebContextRoot(webContextRootTextField.getText().trim());
          handleFieldChanged();
        }
      });
  }

  private void createProductionControls(Composite parent, int ncol)
  {
    productionCheckbox = new Button(parent, SWT.CHECK | SWT.LEFT);
    productionCheckbox.setText("This is a production environment");
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol;
    productionCheckbox.setLayoutData(gd);
    productionCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        getProject().setProduction(productionCheckbox.getSelection());
        handleFieldChanged();
      }
    });
  }

  private void createGitControls(Composite parent, int ncol)
  {
    gitCheckbox = new Button(parent, SWT.CHECK | SWT.LEFT);
    gitCheckbox.setText("This deployment uses VCS assets (eg: Git)");
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol;
    gitCheckbox.setLayoutData(gd);
  }

  private void populateDefaults()
  {
    remoteHostTextField.setText(DEFAULT_HOST);
    remotePortTextField.setText(Integer.toString(DEFAULT_PORT));
    webContextRootTextField.setText(DEFAULT_CLOUD_CONTEXT_ROOT);
    gitCheckbox.setSelection(true);
  }

  @Override
  public IWizardPage getNextPage()
  {
    boolean vcsAssets = gitCheckbox.getSelection();
    if (vcsAssets)
      getProject().setPersistType(PersistType.Git);
    String nextPageTitle = vcsAssets ? GitRepositoryPage.PAGE_TITLE : DataSourcePage.PAGE_TITLE;
    for (IWizardPage page : getWizard().getPages())
    {
      if (page.getTitle().equals(nextPageTitle))
      {
        ((WizardPage)page).initValues(); // version specific
        return page;
      }
    }
    return super.getNextPage();
  }

  @Override
  protected void handleFieldChanged()
  {
    // any changes here invalidate retrieved app summary
    getProject().clearRemoteAppSummary();
    super.handleFieldChanged();
  }

}