/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.activity;

import noNamespace.PAGELETDocument;

import org.apache.xmlbeans.XmlException;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.codegen.CodeGenWizardPage;
import com.centurylink.mdw.plugin.codegen.meta.Activity;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class ActivityPage extends CodeGenWizardPage
{
  private Text labelTextField;
  private Text iconTextField;
  private Combo baseClassComboBox;
  private Text attributeXmlTextField;

  public ActivityPage()
  {
    setTitle("MDW Activity Settings");
    setDescription("Enter the base information for your Activity.\n"
      + "This will be used to registery your custom activity and make it visible in the Toolbox.");
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

    createWorkflowProjectControls(composite, ncol, new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        if (baseClassComboBox != null)
          setBaseClassOptions();
      }
    });
    createWorkflowPackageControls(composite, ncol);

    createLabelControls(composite, ncol);
    createIconControls(composite, ncol);
    if (getActivity().getBaseClass() == null)
      createBaseClassControls(composite, ncol);
    createAttrXmlControls(composite, ncol);
    createSepLine(composite, ncol);
    createCodeGenerationControls(composite, true, ncol);
    createHelpLinkControls(composite, ncol);
    setControl(composite);
  }

  private void createLabelControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Label:");

    labelTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 3;
    labelTextField.setLayoutData(gd);
    labelTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          getActivity().setLabel(labelTextField.getText().trim());
          handleFieldChanged();
        }
      });

    new Label(parent, SWT.WRAP).setText("(as it will appear in the Toolbox)");
    new Label(parent, SWT.NONE).setText("");
  }

  private void createIconControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Icon:");

    iconTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 200;
    gd.horizontalSpan = ncol - 1;
    iconTextField.setLayoutData(gd);
    iconTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getActivity().setIcon(iconTextField.getText().trim());
        handleFieldChanged();
      }
    });

    if (getActivity().getIcon() != null)
      iconTextField.setText(getActivity().getIcon());
    else
      iconTextField.setText("shape:activity");
    getActivity().setIcon(iconTextField.getText());

  }

  private void createBaseClassControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Activity Category:");
    baseClassComboBox = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    gd.widthHint = 385;
    baseClassComboBox.setLayoutData(gd);
    baseClassComboBox.removeAll();
    baseClassComboBox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        String baseClass = baseClassComboBox.getText().trim();
        getActivity().setBaseClass(baseClass);
        handleFieldChanged();
      }
    });

    setBaseClassOptions();

    baseClassComboBox.select(0);
    getActivity().setBaseClass(baseClassComboBox.getText().trim());
  }

  private void createAttrXmlControls(Composite parent, int ncol)
  {
    Label label = new Label(parent, SWT.NONE);
    label.setText("Pagelet:");
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    label.setLayoutData(gd);

    attributeXmlTextField = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 400;
    gd.heightHint = 100;
    gd.horizontalSpan = ncol - 1;
    attributeXmlTextField.setLayoutData(gd);
    if (getWizard() instanceof StartActivityWizard)
    {
      WorkflowProject workflowProject = getProject();
      String baseStartClass =  "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity";
      if (workflowProject != null && !workflowProject.checkRequiredVersion(5, 5))
          baseStartClass = "com.qwest.mdw.workflow.activity.impl.process.ProcessStartControlledActivity";
      ActivityImpl startImpl = workflowProject == null ? null : workflowProject.getActivityImpl(baseStartClass);
      if (startImpl != null && startImpl.getAttrDescriptionXml() != null)
      {
        attributeXmlTextField.setText(startImpl.getAttrDescriptionXml());
        getActivity().setAttrXml(startImpl.getAttrDescriptionXml());
      }
    }
    attributeXmlTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        getActivity().setAttrXml(attributeXmlTextField.getText().trim());
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
    link.setText(" <A>Activity Implementor Help</A>");
    link.setLayoutData(gd);
    link.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          String href = "/" + MdwPlugin.getPluginId() + "/help/doc/implementor.html";
          PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
        }
      });
  }

  private void setBaseClassOptions()
  {
    baseClassComboBox.removeAll();
    if (getProject() != null && !getProject().checkRequiredVersion(5, 5))
    {
      for (String oldBaseClass : ActivityImpl.getOldBaseClasses())
        baseClassComboBox.add(oldBaseClass);
    }
    else
    {
      for (Class<?> baseClass : ActivityImpl.getBaseClasses())
        baseClassComboBox.add(baseClass.getName());
    }
  }

  protected ActivityWizard getActivityWizard()
  {
    return (ActivityWizard)getWizard();
  }

  public Activity getActivity()
  {
    return (Activity)getCodeElement();
  }

  @Override
  protected boolean isPageValid()
  {
    if (getProject() == null)
      return false;
    if (getActivity().getPackage() == null)
      return false;
    if (!getActivity().getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN))
      return false;
    String pageletXml = getActivity().getAttrXml();
    if (pageletXml != null && pageletXml.length() > 0 && validatePagelet(pageletXml) != null)
      return false;
    if (getProject().activityImplLabelExists(getActivity().getLabel()))
      return false;

    return getProject() != null
      && getPackage() != null
      && checkString(getActivity().getLabel())
      && checkString(getActivity().getIcon())
      && checkString(getActivity().getBaseClass());
  }

  public IStatus[] getStatuses()
  {
    String msg = null;
    if (getProject() != null) // is null before inference
    {
      if (getPackage() == null)
        msg = "Please select a workflow package.";
      else if (!getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN))
        msg = "You're not authorized to create activities for this workflow package.";
      else if (getProject().activityImplLabelExists(getActivity().getLabel()))
        msg = "An activity implementor named '" + getActivity().getLabel() + "' already exists for project " + getProject().getLabel();
      else if (getProject().activityImplClassExists(getActivity().getJavaPackage() + "." + getActivity().getClassName()))
        msg = "An activity implementor with class '" + getActivity().getJavaPackage() + "." + getActivity().getClassName() + "' already exists for project " + getProject().getLabel();
      else if (getActivity().getAttrXml() != null && getActivity().getAttrXml().length() > 0)
        msg = validatePagelet(getActivity().getAttrXml());
    }
    if (msg == null)
      return null;
    else
      return new IStatus[] {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
  }

  /**
   * Currently only validate for well-formed XML since we have many existing activities
   * with Pagelet definitions that do not pass XSD validation.
   */
  private String validatePagelet(String pageletXml)
  {
    String error = null;
    if (pageletXml != null && pageletXml.length() > 0)
    {
      try
      {
        PAGELETDocument.Factory.parse(pageletXml);
      }
      catch (XmlException e1)
      {
        error = e1.getMessage();
      }
    }
    return error;
  }

  @Override
  public IWizardPage getNextPage()
  {
    if (getWizard() != null)
    {
      CodeGenWizardPage javaImplPage = selectJavaImplCodeGenPage();
      if (javaImplPage != null)
        return javaImplPage;
    }

    return super.getNextPage();
  }
}
