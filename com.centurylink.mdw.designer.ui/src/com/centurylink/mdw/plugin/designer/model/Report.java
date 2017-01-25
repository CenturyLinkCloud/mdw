/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebLaunchAction;

public class Report extends WorkflowAsset
{
  public Report()
  {
    super();
  }

  public Report(RuleSetVO ruleSetVO, WorkflowPackage packageVersion)
  {
    super(ruleSetVO, packageVersion);
  }

  public Report(Report cloneFrom)
  {
    super(cloneFrom);
  }

  @Override
  public String getTitle()
  {
    return "Report";
  }

  @Override
  public String getIcon()
  {
    return "report.gif";
  }

  @Override
  public String getDefaultExtension()
  {
    return ".rptdesign";
  }

  private static List<String> reportLanguages;
  @Override
  public List<String> getLanguages()
  {
    if (reportLanguages == null)
    {
      reportLanguages = new ArrayList<String>();
      reportLanguages.add("BIRT");
    }
    return reportLanguages;
  }

  public void run()
  {
    WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(getProject(), WebApp.Reports);
    String packagePrefix = isInDefaultPackage() ? "" : (getPackage().getName() + "/");
    launchAction.launch(getProject(), "/reports/birt.jsf?mdwReport=" + packagePrefix + getName());
  }
}