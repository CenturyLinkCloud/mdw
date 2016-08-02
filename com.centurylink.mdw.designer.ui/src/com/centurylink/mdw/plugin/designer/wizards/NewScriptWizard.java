/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.Script;

public class NewScriptWizard extends WorkflowAssetWizard 
{
  public static final String WIZARD_ID = "mdw.designer.new.script";
  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new Script());
  }
}