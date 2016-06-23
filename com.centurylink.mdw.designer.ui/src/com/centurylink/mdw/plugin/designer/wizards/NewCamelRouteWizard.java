/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.CamelRoute;
import com.centurylink.mdw.plugin.designer.model.DocumentTemplate;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class NewCamelRouteWizard extends WorkflowAssetWizard
{
  public static final String WIZARD_ID = "mdw.designer.new.camelRoute";

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new CamelRoute());
  }

  @Override
  public DocumentTemplate getNewDocTemplate()
  {
    if (RuleSetVO.SPRING.equals(getWorkflowAsset().getLanguage()))
      return new DocumentTemplate("Camel", RuleSetVO.getFileExtension(RuleSetVO.SPRING), "templates/spring");
    else
      return new DocumentTemplate("Route", getWorkflowAsset().getExtension(), "templates/camel");
  }
}