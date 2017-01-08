/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;

public class NewTestCaseWizard extends WorkflowAssetWizard
{
  public static final String WIZARD_ID = "mdw.designer.new.test.case";

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new AutomatedTestCase());
  }

  @Override
  public boolean performFinish()
  {
    boolean res = super.performFinish();
    if (res)
    {
      AutomatedTestCase autoTestCase = ((AutomatedTestCase)getWorkflowAsset());
      if (autoTestCase.getProject().isFilePersist())
        autoTestCase.setTestCase(new TestCase(autoTestCase.getPackage().getName(), autoTestCase.getRawFile()));
      else
        autoTestCase.setTestCase(new TestCase(autoTestCase.getPackage().getName(), autoTestCase.getRuleSetVO()));
    }
    return res;
  }


}