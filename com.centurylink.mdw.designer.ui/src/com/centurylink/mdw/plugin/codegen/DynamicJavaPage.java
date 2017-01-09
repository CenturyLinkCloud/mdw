/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.codegen.CodeGenWizard.CodeGenType;

public class DynamicJavaPage extends CodeGenWizardPage
{
  private Text classNameTextField;

  public DynamicJavaPage(String title, String description)
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
    createClassNameControls(composite, ncol);
    createHelpLinkControls(composite, ncol);

    setControl(composite);
  }

  protected void createClassNameControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Dynamic Java Class Name:");

    classNameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 250;
    gd.horizontalSpan = ncol - 1;
    classNameTextField.setLayoutData(gd);
    classNameTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getCodeElement().setClassName(classNameTextField.getText().trim());
        handleFieldChanged();
      }
    });
  }

  private void createHelpLinkControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("");
    Link link = new Link(parent, SWT.SINGLE);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_END);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = getMaxFieldWidth();
    gd.horizontalSpan = ncol - 1;
    gd.verticalIndent = 10;
    link.setText(" <A>Dynamic Java Help</A>");
    link.setLayoutData(gd);
    link.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        String href = "/" + MdwPlugin.getPluginId() + "/help/doc/dynamicJava.html";
        PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
      }
    });
  }

  @Override
  public void saveDataToModel()
  {
    // class name is saved directly from modify listener
    getCodeElement().setJavaPackage(getPackage() == null ? null : getPackage().getName());
  }

  @Override
  public boolean isPageComplete()
  {
    return getCodeGenWizard().getCodeGenType() != CodeGenType.dynamicJavaCode || isPageValid();
  }

  @Override
  protected boolean isPageValid()
  {
    if (getProject().getAsset(getCodeElement().getJavaPackage(), getCodeElement().getClassName()) != null
        || getProject().getAsset(getCodeElement().getJavaPackage(), getCodeElement().getClassName() + ".java") != null)
      return false;

    String className = getCodeElement().getClassName();

    if (!checkString(className))
      return false;

    if (className.indexOf('.') >= 0)
      return false;

    if (!className.equals(JavaNaming.getValidClassName(className)))
      return false;

    return getCodeGenWizard().validate() == null;
  }

  public IStatus[] getStatuses()
  {
    String msg = null;
    if (getProject() != null) // is null before inference
    {
      if (getProject().getAsset(getCodeElement().getJavaPackage(), getCodeElement().getClassName()) != null
          || getProject().getAsset(getCodeElement().getJavaPackage(), getCodeElement().getClassName() + ".java") != null)
        msg = "Dynamic Java class already exists: " + getCodeElement().getJavaPackage() + "." + getCodeElement().getClassName();
      else if (getCodeElement().getClassName().contains("."))
        msg = "Dynamic Java class name must not be qualified (Java package = workflow package).";
      else if (!getCodeElement().getClassName().equals(JavaNaming.getValidClassName(getCodeElement().getClassName())))
        msg = "Invalid character(s) in class name";
      else
        msg = getCodeGenWizard().validate();
    }

    if (msg == null)
      return null;
    else
      return new IStatus[] {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
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
