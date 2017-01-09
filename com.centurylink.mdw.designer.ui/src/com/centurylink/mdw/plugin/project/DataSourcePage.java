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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.project.model.JdbcDataSource;

/**
 * JDBC DataSource page of the MDW workflow project wizard.
 */
public class DataSourcePage extends WizardPage implements IFacetWizardPage
{
  public static final String PAGE_TITLE = "JDBC DataSource Settings";

  private Combo driverComboBox;
  private Text jdbcUrlTextField;
  private Text dbUserTextField;
  private Text dbPasswordTextField;
  private Text schemaOwnerTextField;

  public DataSourcePage()
  {
    setTitle(PAGE_TITLE);
    setDescription("Enter your MDW DataSource information.\n"
        + "This will be written to your local settings.");
  }

  public void initValues()
  {
    String prevDriver = "";
    String prevUrl = "";
    String prevUser = "";
    if (!getProject().isRemote())
    {
      String prefix = "MDW" + getProject().getMdwVersion();
      prevDriver = MdwPlugin.getStringPref(prefix + "-" + ProjectPersist.MDW_DB_DRIVER);
      prevUrl = MdwPlugin.getStringPref(prefix + "-" + ProjectPersist.MDW_DB_URL);
      prevUser = MdwPlugin.getStringPref(prefix + "-" + ProjectPersist.MDW_DB_USER);
    }

    if (prevDriver.length() > 0)
      getDataSource().setDriver(prevDriver);
    else
      getDataSource().setDriver(JdbcDataSource.DEFAULT_DRIVER);
    driverComboBox.setText(getDataSource().getDriver());

    if (prevUrl.length() > 0)
      getDataSource().setJdbcUrl(prevUrl);
    else
      getDataSource().setJdbcUrl(JdbcDataSource.DEFAULT_JDBC_URL);
    jdbcUrlTextField.setText(getDataSource().getJdbcUrl());

    if (prevUser.length() > 0)
    {
      getDataSource().setDbUser(prevUser);
    }
    else
    {
      getDataSource().setDbUser(JdbcDataSource.DEFAULT_DB_USER);
      getDataSource().setDbPassword(JdbcDataSource.DEFAULT_DB_PASSWORD);
      if (!getProject().isRemote())
      {
        // triggers server call to retrieve mdw version -- not for remote
        if (!getProject().checkRequiredVersion(5, 5))
        {
          getDataSource().setDbUser(JdbcDataSource.DEFAULT_DB_USER_OLD);
          getDataSource().setDbPassword(JdbcDataSource.DEFAULT_DB_PASSWORD_OLD);
        }
      }
    }

    dbUserTextField.setText(getDataSource().getDbUser());

    if (prevUser.length() == 0)
      dbPasswordTextField.setText(getDataSource().getDbPassword());
  }

  private JdbcDataSource getDataSource()
  {
    if (getProject() == null)
      return null;
    return getProject().getMdwDataSource();
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

    createDriverControls(composite, ncol);
    createJdbcUrlControls(composite, ncol);
    createDbUserControls(composite, ncol);
    createDbPasswordControls(composite, ncol);
    createSpacer(composite, ncol);

    setControl(composite);
  }

  /**
   * @see WizardPage#getStatuses()
   */
  public IStatus[] getStatuses()
  {
    if (isPageComplete())
      return null;

    String msg = null;
    if (containsWhitespace(getDataSource().getName()))
      msg = "Invalid value for DataSource Name";
    else if (containsWhitespace(getDataSource().getDriver()))
      msg = "Invalid value for JDBC Driver";
    else if (containsWhitespace(getDataSource().getJdbcUrl()))
      msg = "Invalid value for JDBC URL";
    else if (containsWhitespace(getDataSource().getDbUser()))
      msg = "Invalid value for DB User";
    else if (containsWhitespace(getDataSource().getDbPassword()))
      msg = "Invalid value for DB Password";

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
    return checkStringNoWhitespace(getDataSource().getName())
      && checkStringNoWhitespace(getDataSource().getDriver())
      && checkStringNoWhitespace(getDataSource().getJdbcUrl())
      && checkStringNoWhitespace(getDataSource().getDbUser())
      && checkStringNoWhitespace(getDataSource().getDbPassword())
      && (schemaOwnerTextField == null || (!schemaOwnerTextField.isEnabled() || checkStringNoWhitespace(getDataSource().getSchemaOwner())));
  }

  private void createDriverControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Driver:");
    driverComboBox = new Combo(parent, SWT.DROP_DOWN);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    gd.widthHint = 200;
    driverComboBox.setLayoutData(gd);

    driverComboBox.removeAll();
    for (int i = 0; i < JdbcDataSource.JDBC_DRIVERS.length; i++)
      driverComboBox.add(JdbcDataSource.JDBC_DRIVERS[i]);

    driverComboBox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        String driver = driverComboBox.getText();
        getDataSource().setDriver(driver);
        handleFieldChanged();
      }
    });
  }

  private void createJdbcUrlControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("JDBC URL:");

    jdbcUrlTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 400;
    gd.horizontalSpan = ncol - 1;
    jdbcUrlTextField.setLayoutData(gd);

    jdbcUrlTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getDataSource().setJdbcUrl(jdbcUrlTextField.getText().trim());
        handleFieldChanged();
      }
    });
  }

  private void createDbUserControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("DB User:");

    dbUserTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    dbUserTextField.setLayoutData(gd);
    dbUserTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getDataSource().setDbUser(dbUserTextField.getText().trim());
        handleFieldChanged();
      }
    });
  }

  private void createDbPasswordControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("DB Password:");

    dbPasswordTextField = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    dbPasswordTextField.setLayoutData(gd);
    dbPasswordTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getDataSource().setDbPassword(dbPasswordTextField.getText().trim());
        handleFieldChanged();
      }
    });
  }

  public void setWizardContext(IWizardContext context)
  {
  }

  public void transferStateToConfig()
  {
  }

  @Override
  public IWizardPage getNextPage()
  {
    if (getProject().isRemote() || getProject().isOsgi()
        || !getProject().checkRequiredVersion(5, 1))
    {
      return null;
    }
    else
    {
      for (IWizardPage page : getWizard().getPages())
      {
        if (page.getTitle().equals(ExtensionModulesWizardPage.PAGE_TITLE))
          return page;
      }
      return super.getNextPage();
    }
  }
}
