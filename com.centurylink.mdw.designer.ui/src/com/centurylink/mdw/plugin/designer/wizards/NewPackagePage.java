/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
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

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class NewPackagePage extends WizardPage
{
  private Text packageNameTextField;
  private Label mdwVersionLabel;
  private Combo groupCombo;
  private Label groupLabel;

  public NewPackagePage()
  {
    setTitle("New MDW Package");
    setDescription("Enter the name and MDW Version for your package.");
  }

  public void init(IStructuredSelection selection)
  {
    super.init(selection);
  }

  @Override
  public void drawWidgets(Composite parent)
  {
    // create the composite to hold the widgets
    Composite composite = new Composite(parent, SWT.NULL);

    // create the layout for this wizard page
    GridLayout gl = new GridLayout();
    int ncol = 4;
    gl.numColumns = ncol;
    composite.setLayout(gl);

    createWorkflowProjectControls(composite, ncol);
    createNameControls(composite, ncol);
    if (getProject() != null && !getProject().isRemote())
      createMdwVersionControls(composite, ncol);
    if (getProject().checkRequiredVersion(5, 5, 8))
    {
      createGroupControls(composite, ncol);
      showGroupControls(true);
    }
    setControl(composite);

    packageNameTextField.forceFocus();
  }

  private void createNameControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Package Name:");

    packageNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 300;
    gd.horizontalSpan = ncol - 1;
    packageNameTextField.setLayoutData(gd);
    packageNameTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          getPackage().setName(packageNameTextField.getText().trim());
          handleFieldChanged();
        }
      });
  }

  private void createMdwVersionControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("MDW Version:");
    mdwVersionLabel = new Label(parent, SWT.NONE);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    mdwVersionLabel.setLayoutData(gd);
    mdwVersionLabel.setText(getProject().getMdwVersion());
    getPackage().setSchemaVersion(getProject().getMdwSchemaVersion());
  }

  private void createGroupControls(Composite parent, int ncol)
  {
    groupLabel = new Label(parent, SWT.NONE);
    groupLabel.setText("Workgroup:");
    GridData gd = new GridData(GridData.BEGINNING);
    gd.verticalIndent = 10;
    groupLabel.setLayoutData(gd);
    groupCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    gd.widthHint = 250;
    gd.verticalIndent = 10;
    groupCombo.setLayoutData(gd);
    populateGroupCombo();
    groupCombo.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        getPackage().setGroup(groupCombo.getText());
      }
    });
  }

  private void showGroupControls(boolean visible)
  {
    if (!visible)
    {
      getPackage().setGroup(null);
      groupCombo.setText("");
    }
    groupLabel.setVisible(visible);
    groupCombo.setVisible(visible);
  }

  private void populateGroupCombo()
  {
    groupCombo.removeAll();
    groupCombo.add("");
    if (getProject() != null)
    {
      for (String group : getProject().getDesignerDataModel().getWorkgroupNames())
        groupCombo.add(group);
    }
  }
  @Override
  public boolean isPageComplete()
  {
    return isPageValid();
  }

  boolean isPageValid()
  {
    return (getProject() != null
        && getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN)
        && checkStringDisallowChars(getPackage().getName(), "/"))
        && !getProject().packageNameExists(getPackage().getName());
  }

  public IStatus[] getStatuses()
  {
    String msg = null;
    if (getProject() == null)
      msg = "Please select a valid workflow project";
    else if (!getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN))
      msg = "You're not authorized to create packages for this workflow project.";
    else if (!checkString(getPackage().getName()))
      msg = "Please enter a package name";
    else if (!checkStringDisallowChars(getPackage().getName(), "/"))
      msg = "Invalid characters in name (/ not allowed)";
    else if (getProject().packageNameExists(getPackage().getName()))
      msg = "Package name already exists";

    if (msg == null)
      return null;

    IStatus[] is = {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
    return is;
  }

  public WorkflowPackage getPackage()
  {
    return ((NewPackageWizard)getWizard()).getPackage();
  }

  @Override
  public WorkflowElement getElement()
  {
    return getPackage();
  }
}