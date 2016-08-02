/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.DocumentTemplate;
import com.centurylink.mdw.plugin.designer.model.Page;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class NewPageWizard extends WorkflowAssetWizard
{
  public static final String WIZARD_ID = "mdw.designer.new.page";

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new Page());
  }

  @Override
  public String getTemplateLocation()
  {
    String languageSel = getWorkflowAssetPage().getLanguageCombo().getText();
    if (languageSel.equalsIgnoreCase(RuleSetVO.FACELET) || languageSel.equalsIgnoreCase("XHTML"))
      return "/templates/facelet";
    else
      return null;
  }

  @Override
  public List<String> getTemplateOptions()
  {
    String languageSel = getWorkflowAssetPage().getLanguageCombo().getText();
    if (languageSel.equalsIgnoreCase(RuleSetVO.FACELET) || languageSel.equalsIgnoreCase("XHTML"))
    {
      return super.getTemplateOptions();
    }
    else
    {
      List<String> options = new ArrayList<String>();
      options.add(BLANK_TEMPLATE);
      return options;
    }
  }

  @Override
  public DocumentTemplate getNewDocTemplate()
  {
    if (getWorkflowAsset().getLanguage().equals(RuleSetVO.FACELET))
      return super.getNewDocTemplate();
    else
      return null;
  }

}