package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.AutomatedTestResults;

public class NewYamlWizard extends WorkflowAssetWizard
{
  public static final String WIZARD_ID = "mdw.designer.new.yaml";

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    // TODO: not all yaml assets are automated test results
    super.init(workbench, selection, new AutomatedTestResults());
  }
}