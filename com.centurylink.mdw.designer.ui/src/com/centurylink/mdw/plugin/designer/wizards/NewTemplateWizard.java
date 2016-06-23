/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import com.centurylink.mdw.plugin.designer.model.DocumentTemplate;
import com.centurylink.mdw.plugin.designer.model.Template;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class NewTemplateWizard extends WorkflowAssetWizard
{
  public static final String WIZARD_ID = "mdw.designer.new.template";

  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    super.init(workbench, selection, new Template());
  }

  @Override
  public String getTemplateLocation()
  {
    String languageSel = getWorkflowAssetPage().getLanguageCombo().getText();
    if (languageSel.equalsIgnoreCase(RuleSetVO.FACELET))
      return "/templates/facelet";
    else
      return null;
  }

  @Override
  public List<String> getTemplateOptions()
  {
    String languageSel = getWorkflowAssetPage().getLanguageCombo().getText();
    if (languageSel.equalsIgnoreCase(RuleSetVO.FACELET))
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