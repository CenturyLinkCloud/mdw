/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.workspace;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.centurylink.mdw.plugin.WizardPage;

public class WorkspaceConfigPage extends WizardPage
{
  private Combo codeFormatterComboBox;
  private Combo codeTemplatesComboBox;
  private Button disableEclipseAutobuildCheckbox;
  
  public WorkspaceConfigPage()
  {
    setTitle("MDW Workspace Settings");
    setDescription("Updates your Eclipse workspace preferences with typical MDW values.");
  }
  
  public void init(IStructuredSelection selection)
  {
    super.init(selection);
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

    createCodeFormatterControls(composite, ncol);
    createCodeTemplatesControls(composite, ncol);
    createSepLine(composite, ncol);
    createDisableEclipseAutobuildControls(composite, ncol);
    createSepLine(composite, ncol);
    createExplanatoryLabelControls(composite, ncol);
    
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
    if (!checkString(getWorkspaceConfig().getCodeFormatter()))
      msg = "Please select a value for Code Formatter";
    else if (!checkString(getWorkspaceConfig().getCodeTemplates()))
      msg = "Please select a value for Code Templates";
    
    IStatus[] is = {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
    return is;
  }
  
  protected WorkspaceConfig getWorkspaceConfig()
  {
    return ((WorkspaceConfigWizard)getWizard()).getWorkspaceConfig();
  }
  
  /**
   * sets the completed field on the wizard class when all the information 
   * on the page is entered
   */
  public boolean isPageComplete()
  {
    return checkString(getWorkspaceConfig().getCodeFormatter())
      && checkString(getWorkspaceConfig().getCodeTemplates());
  }
  
  private void createCodeFormatterControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Code Formatter:");
    codeFormatterComboBox = new Combo(parent, SWT.DROP_DOWN);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    gd.widthHint = 225;
    codeFormatterComboBox.setLayoutData(gd);
    
    codeFormatterComboBox.removeAll();
    for (int i = 0; i < WorkspaceConfig.CODE_FORMATTERS.length; i++)
      codeFormatterComboBox.add(WorkspaceConfig.CODE_FORMATTERS[i]);
    
    codeFormatterComboBox.setText(getWorkspaceConfig().getCodeFormatter());
    codeFormatterComboBox.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          String formatter = codeFormatterComboBox.getText();
          getWorkspaceConfig().setCodeFormatter(formatter);
          handleFieldChanged();
        }
      });
  }

  private void createCodeTemplatesControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Code Templates:");
    codeTemplatesComboBox = new Combo(parent, SWT.DROP_DOWN);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    gd.widthHint = 225;
    codeTemplatesComboBox.setLayoutData(gd);
    
    codeTemplatesComboBox.removeAll();
    for (int i = 0; i < WorkspaceConfig.CODE_TEMPLATES.length; i++)
      codeTemplatesComboBox.add(WorkspaceConfig.CODE_TEMPLATES[i]);
    
    codeTemplatesComboBox.setText(getWorkspaceConfig().getCodeTemplates());
    codeTemplatesComboBox.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          String templates = codeTemplatesComboBox.getText();
          getWorkspaceConfig().setCodeTemplates(templates);
          handleFieldChanged();
        }
      });
  }
  
  private void createDisableEclipseAutobuildControls(Composite parent, int ncol)
  {
    disableEclipseAutobuildCheckbox = new Button(parent, SWT.CHECK);
    disableEclipseAutobuildCheckbox.setText("Disable Eclipse AutoBuild");
    disableEclipseAutobuildCheckbox.setSelection(false);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol;
    disableEclipseAutobuildCheckbox.setLayoutData(gd);
    disableEclipseAutobuildCheckbox.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          boolean autobuild = !disableEclipseAutobuildCheckbox.getSelection();
          getWorkspaceConfig().setEclipseAutobuild(autobuild);
          handleFieldChanged();
        }
      });
  }
  
  private void createExplanatoryLabelControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE);

    Label explanatoryTextLabel = new Label(parent, SWT.WRAP);
    explanatoryTextLabel.setText("This only needs to be performed once for this workspace.\nYou can change these settings later using the Eclipse preference pages.");
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol;
    gd.widthHint = 350;
    explanatoryTextLabel.setLayoutData(gd);
  }
  
  
}
