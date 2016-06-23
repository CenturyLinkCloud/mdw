/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.codegen.CodeGenWizard.CodeGenType;

public class StaticJavaPage extends CodeGenWizardPage
{
  public StaticJavaPage(String title, String description)
  {
    setTitle(title);
    setDescription(description);
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

    createInfoControls(composite, ncol, getCodeGenWizard().getInfoLabelLabel());
    createSepLine(composite, ncol);

    createContainerControls(composite, ncol);
    createPackageControls(composite, ncol);
    createTypeNameControls(composite, ncol);

    setControl(composite);
  }

  protected String getTypeNameLabel()
  {
    return "Class Name:";
  }

  @Override
  public void saveDataToModel()
  {
    getCodeElement().setPackageFragmentRoot(getPackageFragmentRoot());
    getCodeElement().setPackageFragment(getPackageFragment());
    getCodeElement().setJavaPackage(getPackageFragment() == null ? null : getPackageFragment().getElementName());
    getCodeElement().setClassName(getTypeName());
  }

  @Override
  public boolean isPageComplete()
  {
    return getCodeGenWizard().getCodeGenType() != CodeGenType.staticJavaCode || isPageValid();
  }

  @Override
  protected boolean isPageValid()
  {
    if (getPackageFragmentRoot() == null)
      return false;
    if (getPackageFragmentRoot().getJavaProject() == null)
      return false;

    if (!checkString(getCodeElement().getJavaPackage()))
      return false;

    if (!checkString(getCodeElement().getClassName()))
      return false;

    return getCodeGenWizard().validate() == null;
  }

  public IStatus[] getStatuses()
  {
    String msg = null;
    if (getProject() != null) // is null before inference
    {
      if (getPackageFragmentRoot() == null
          || getPackageFragmentRoot().getJavaProject() == null
          || !getPackageFragmentRoot().getJavaProject().exists())
      {
        msg = "Please select a source folder and package inside a local MDW Workflow source project.";
      }
      else
      {
        msg = getCodeGenWizard().validate();
      }
    }

    if (msg != null)
      return new IStatus[] {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
    else
      return new IStatus[] { fPackageStatus, fTypeNameStatus };
  }

  @Override
  public IWizardPage getNextPage()
  {
    CodeGenWizard codeGenWizard = getCodeGenWizard();
    if (codeGenWizard != null)
      return codeGenWizard.getPageAfterJavaImplCodeGenPage();

    return super.getNextPage();
  }

}
