/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.assembly.ProjectConfigurator;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class Rule extends WorkflowAsset
{
  public Rule()
  {
    super();
  }

  public Rule(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public Rule(Rule cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "Rule";
  }

  public boolean isExcel()
  {
    return getRuleSetVO().isExcel();
  }

  public boolean isExcel2007()
  {
    return getRuleSetVO().isExcel2007();
  }

  @Override
  public String getIcon()
  {
    if (isExcel() || isExcel2007())
      return "excel.gif";
    else
      return "drools.gif";
  }

  @Override
  public String getLanguageFriendly()
  {
    if (isExcel2007())
      return "Excel";
    else
      return super.getLanguageFriendly();
  }

  @Override
  public void setLanguageFriendly(String friendly)
  {
    if (friendly.equals("Excel"))
      super.setLanguageFriendly(RuleSetVO.EXCEL_2007);
    else
      super.setLanguageFriendly(friendly);
  }

  @Override
  public String getDefaultExtension()
  {
    return ".drl";
  }

  private static List<String> rulesLanguages;
  @Override
  public List<String> getLanguages()
  {
    if (rulesLanguages == null)
    {
      rulesLanguages = new ArrayList<String>();
      rulesLanguages.add("Drools");
      rulesLanguages.add("Guided");
      rulesLanguages.add("Excel");
    }
    return rulesLanguages;
  }

  @Override
  protected void beforeFileOpened()
  {
    ProjectConfigurator projConf = new ProjectConfigurator(getProject(), MdwPlugin.getSettings());
    try
    {
      projConf.setJava(new NullProgressMonitor());
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Java Support", getProject());
    }
  }

  @Override
  public boolean isForceExternalEditor()
  {
    // eclipse bug can lead to lost changes if user saves via the embedded save button
    return (isExcel() || isExcel2007()) && !MdwPlugin.getSettings().isUseEmbeddedEditorForExcelAssets();
  }
}