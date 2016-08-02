/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.data.oda.jdbc.JDBCDriverManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebLaunchAction;
import com.centurylink.mdw.plugin.designer.views.ProcessExplorerView;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

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

  @Override
  public String validate()
  {
    if (!MdwPlugin.workspaceHasBirtSupport())
      return "The Eclipse BIRT plugin is required to create a BIRT report.";
    else
      return super.validate();
  }

  @Override
  protected void beforeFileOpened()
  {
    try
    {
      if (getProject().getMdwDataSource().isMySql())
      {
        URL localUrl = PluginUtil.getCoreResourceUrl("lib/mysql-connector-java-5.1.29.jar");
        List<String> driverClasspath = new ArrayList<String>();
        driverClasspath.add(localUrl.toString().substring(6));
        JDBCDriverManager manager = JDBCDriverManager.getInstance( );
        manager.loadAndRegisterDriver("com.mysql.jdbc.Driver", driverClasspath);
      }
      else if (getProject().getMdwDataSource().isMariaDb())
      {
        URL localUrl = PluginUtil.getCoreResourceUrl("lib/mariadb-java-client-1.2.0.jar");
        List<String> driverClasspath = new ArrayList<String>();
        driverClasspath.add(localUrl.toString().substring(6));
        JDBCDriverManager manager = JDBCDriverManager.getInstance( );
        manager.loadAndRegisterDriver("org.mariadb.jdbc.Driver", driverClasspath);
      }
      else if (getProject().getMdwDataSource().isOracle())
      {
        URL localUrl = PluginUtil.getCoreResourceUrl("lib/ojdbc6-11.2.0.3.jar");
        List<String> driverClasspath = new ArrayList<String>();
        driverClasspath.add(localUrl.toString().substring(6));
        JDBCDriverManager manager = JDBCDriverManager.getInstance( );
        manager.loadAndRegisterDriver("oracle.jdbc.OracleDriver", driverClasspath);
        manager.loadAndRegisterDriver("oracle.jdbc.driver.OracleDriver", driverClasspath);
      }
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Create Report", getProject());
    }
  }

  @Override
  protected void afterFileOpened(IEditorPart tempFileEditor)
  {
    if ("BIRT".equals(getLanguage()))
    {
      IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      if (page != null)
      {
        try
        {
          page.showView("org.eclipse.gef.ui.palette_view");
          page.showView("org.eclipse.birt.report.designer.ui.attributes.AttributeView");
          page.showView("org.eclipse.birt.report.designer.ui.views.data.DataView");
          page.showView(ProcessExplorerView.VIEW_ID);
        }
        catch (PartInitException ex)
        {
          PluginMessages.uiError(ex, "Open Temp File", getProject());
        }
      }
    }
  }

  public void run()
  {
    WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(getProject(), WebApp.Reports);
    String packagePrefix = isInDefaultPackage() ? "" : (getPackage().getName() + "/");
    launchAction.launch(getProject(), "/reports/birt.jsf?mdwReport=" + packagePrefix + getName());
  }
}