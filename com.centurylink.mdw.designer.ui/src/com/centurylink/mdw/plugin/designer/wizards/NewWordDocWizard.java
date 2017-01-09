/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.DocumentTemplate;
import com.centurylink.mdw.plugin.designer.model.WordDoc;

public class NewWordDocWizard extends WorkflowAssetWizard
{
  public static final String WIZARD_ID = "mdw.designer.new.wordDoc";

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new WordDoc());
  }

  @Override
  public DocumentTemplate getNewDocTemplate()
  {
    return new DocumentTemplate("Empty", getWorkflowAsset().getExtension(), "templates/word");
  }
}