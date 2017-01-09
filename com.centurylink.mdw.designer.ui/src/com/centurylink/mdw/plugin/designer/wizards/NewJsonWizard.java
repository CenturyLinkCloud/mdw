/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.Json;

public class NewJsonWizard extends WorkflowAssetWizard
{
  public static final String WIZARD_ID = "mdw.designer.new.json";

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new Json());
  }
}