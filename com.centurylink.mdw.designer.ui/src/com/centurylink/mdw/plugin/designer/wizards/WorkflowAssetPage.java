/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.plugin.WizardPage;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class WorkflowAssetPage extends WizardPage
{
  private Text nameTextField;
  public Text getNameTextField() { return nameTextField; }

  private Combo languageCombo;
  public Combo getLanguageCombo() { return languageCombo; }

  private Button generateNewButton;
  private Combo templateCombo;
  private Button importFileButton;
  private Text importFileText;
  private Button importFileBrowseButton;

  public WorkflowAssetPage(WorkflowAsset workflowAsset)
  {
    setTitle("New " + workflowAsset.getTitle());
    setDescription("Enter the info for your new " + workflowAsset.getTitle().toLowerCase() + ".");
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
    if (getProject().checkRequiredVersion(5, 0))
      createWorkflowPackageControls(composite, ncol);
    else
      setPackage(getProject().getDefaultPackage());

    createNameControls(composite, ncol);
    createLanguageControls(composite, ncol);
    createDocumentControls(composite, ncol);
    enableFileSelect(false);

    setControl(composite);
    nameTextField.forceFocus();
  }

  protected void createNameControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText(getWorkflowAsset().getTitle() + " Name:");

    nameTextField = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 250;
    gd.horizontalSpan = ncol - 1;
    nameTextField.setLayoutData(gd);
    nameTextField.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          getWorkflowAsset().setName(nameTextField.getText().trim());
          handleFieldChanged();
        }
      });
  }

  protected void createLanguageControls(Composite parent, int ncol)
  {
    new Label(parent, SWT.NONE).setText("Language/Format:");
    languageCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = ncol - 1;
    gd.widthHint = 200;
    languageCombo.setLayoutData(gd);

    languageCombo.removeAll();
    for (String language : getWorkflowAsset().getLanguages())
    {
      languageCombo.add(language);
    }

    languageCombo.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          getWorkflowAsset().setLanguageFriendly(languageCombo.getText());
          if (templateCombo != null)
          {
            // allow dynamic template determination based on language
            templateCombo.removeAll();
            for (String tempOpt : getWorkflowAssetWizard().getTemplateOptions())
              templateCombo.add(tempOpt);
          }
          handleFieldChanged();
        }
      });
    if (getWorkflowAsset() != null && getWorkflowAsset().getLanguageFriendly() != null)
        languageCombo.setText(getWorkflowAsset().getLanguageFriendly());
    else
        languageCombo.select(0);
    getWorkflowAsset().setLanguageFriendly(languageCombo.getText());
  }

  protected void createDocumentControls(Composite parent, int ncol)
  {
    Group radioGroup = new Group(parent, SWT.NONE);
    radioGroup.setText("Initial Content");
    GridLayout gl = new GridLayout();
    gl.numColumns = 4;
    gl.marginTop = 5;
    radioGroup.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = ncol;
    gd.verticalIndent = 10;
    radioGroup.setLayoutData(gd);

    if (!getWorkflowAsset().isBinary() || hasTemplateOptions())
    {
      generateNewButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
      gd = new GridData(GridData.BEGINNING);
      gd.horizontalSpan = 4;
      gl = new GridLayout();
      generateNewButton.setLayoutData(gd);
      generateNewButton.setText("Generate " + (hasTemplateOptions() ? "from Template" : "Blank"));
      generateNewButton.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          enableFileSelect(!generateNewButton.getSelection());
          setImportFile(!generateNewButton.getSelection());
          handleFieldChanged();
        }
      });
      generateNewButton.setSelection(true);
    }

    if (hasTemplateOptions())
    {
      // template combo
      Label templateLabel = new Label(radioGroup, SWT.NONE);
      templateLabel.setText("Template:");
      gd = new GridData(GridData.BEGINNING);
      gd.horizontalIndent = 25;
      templateLabel.setLayoutData(gd);
      templateCombo = new Combo(radioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
      gd = new GridData(GridData.BEGINNING);
      gd.horizontalSpan = ncol - 2;
      gd.widthHint = 200;
      templateCombo.setLayoutData(gd);

      templateCombo.removeAll();
      for (String template : getTemplateOptions())
        templateCombo.add(template);

      templateCombo.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          setTemplateName(templateCombo.getText());
          handleFieldChanged();
        }
      });
      templateCombo.select(0);
      setTemplateName(templateCombo.getText());
    }

    importFileButton = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 4;
    importFileButton.setLayoutData(gd);
    importFileButton.setSelection(true);
    importFileButton.setText("Import from File");
    importFileButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        setImportFile(importFileButton.getSelection());
        enableFileSelect(importFileButton.getSelection());
        handleFieldChanged();
      }
    });
    importFileButton.setSelection(false);

    // file path
    Label label = new Label(radioGroup, SWT.NONE);
    label.setText("File:");
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalIndent = 25;
    label.setLayoutData(gd);

    importFileText = new Text(radioGroup, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.widthHint = 300;
    gd.horizontalSpan = ncol - 2;
    importFileText.setLayoutData(gd);
    importFileText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        setImportFilePath(importFileText.getText().trim());
        handleFieldChanged();
      }
    });

    importFileBrowseButton = new Button(radioGroup, SWT.PUSH);
    importFileBrowseButton.setText("Browse...");
    importFileBrowseButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        FileDialog dlg = new FileDialog(getShell());
        dlg.setFilterExtensions(new String[]{"*" + getWorkflowAsset().getExtension(), "*.*"});
        String path = dlg.open();
        if (path != null)
        {
          importFileText.setText(path);
          if (nameTextField.getText().trim().isEmpty())
            nameTextField.setText(path.substring(path.lastIndexOf(System.getProperty("file.separator")) + 1));
        }
      }
    });
  }

  protected void enableFileSelect(boolean enabled)
  {
    if (!enabled)
    {
      setImportFilePath("");
      importFileText.setText("");
    }
    importFileText.setEnabled(enabled);
    importFileBrowseButton.setEnabled(enabled);
  }

  protected boolean isImportFile()
  {
    return getWorkflowAssetWizard().isImportFile();
  }
  protected void setImportFile(boolean importFile)
  {
    getWorkflowAssetWizard().setImportFile(importFile);
  }

  protected String getImportFilePath()
  {
    return getWorkflowAssetWizard().getImportFilePath();
  }
  protected void setImportFilePath(String path)
  {
    getWorkflowAssetWizard().setImportFilePath(path);
  }

  protected boolean hasTemplateOptions()
  {
    return getTemplateOptions() != null && getTemplateOptions().size() > 0;
  }
  protected List<String> getTemplateOptions()
  {
    return getWorkflowAssetWizard().getTemplateOptions();
  }

  protected void setTemplateName(String name)
  {
    getWorkflowAssetWizard().setTemplateName(name);
  }

  protected WorkflowAssetWizard getWorkflowAssetWizard()
  {
    return (WorkflowAssetWizard)getWizard();
  }

  @Override
  public boolean isPageComplete()
  {
    return isPageValid();
  }

  boolean isPageValid()
  {
    return (getProject() != null
        && checkStringDisallowChars(getWorkflowAsset().getName(), "/")
        && (!isImportFile() || checkFile(getImportFilePath()))
        && getWorkflowAsset().getPackage() != null
        && getWorkflowAsset().getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN)
        && !getWorkflowAsset().getPackage().workflowAssetNameExists(getWorkflowAsset().getName())
        && getWorkflowAsset().validate() == null);
  }

  public IStatus[] getStatuses()
  {
    String msg = null;
    if (getProject() == null)
      msg = "Please select a valid workflow project";
    else if (!checkString(getWorkflowAsset().getName()))
      msg = "Please enter a name";
    else if (!checkStringDisallowChars(getWorkflowAsset().getName(), "/"))
      msg = "Invalid characters in name (/ not allowed)";
    else if (isImportFile() && !checkFile(getImportFilePath()))
      msg = "Please enter a valid import file path";
    else if (!getWorkflowAsset().getPackage().isUserAuthorized(UserRoleVO.ASSET_DESIGN))
      msg = "You're not authorized to add a new " + getWorkflowAsset().getName().toLowerCase() + " to this workflow package.";
    else if (getWorkflowAsset().getPackage().workflowAssetNameExists(getWorkflowAsset().getName()))
      msg = "Name already exists";
    else if (getProject().isRequireWorkflowPackage() && getWorkflowAsset().isInDefaultPackage())
      msg = "Please select a valid workflow package";
    else
      msg = getWorkflowAsset().validate();

    if (msg == null)
    {
      // check for warnings
      String name = getWorkflowAsset().getName();
      if (name.indexOf(".") == -1)
      {
        msg = "Assets without a filename extension are discouraged";
        IStatus[] isWarn = {new Status(IStatus.WARNING, getPluginId(), 0, msg, null)};
        return isWarn;
      }

      return null;
    }
    else
    {
      IStatus[] isErr = {new Status(IStatus.ERROR, getPluginId(), 0, msg, null)};
      return isErr;
    }
  }

  public WorkflowAsset getWorkflowAsset()
  {
    return ((WorkflowAssetWizard)getWizard()).getWorkflowAsset();
  }

  public void setWorkflowAsset(WorkflowAsset asset)
  {
    ((WorkflowAssetWizard)getWizard()).setWorkflowAsset(asset);
  }

  @Override
  public WorkflowElement getElement()
  {
    return getWorkflowAsset();
  }
}