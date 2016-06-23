/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionExecutor;

import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.model.value.project.EnvironmentException;
import com.centurylink.mdw.model.value.project.EnvironmentVO;

public abstract class MdwScriptFunctionExecutor implements IScriptFunctionExecutor
{
  protected void checkArgs(Object[] args, int required, String functionName) throws BirtException
  {
    boolean valid = true;

    if (args == null || args.length < required)
    {
      valid = false;
    }
    else
    {
      for (int i = 0; i < required; i++)
      {
        if (args[i] == null || args[i].toString().trim().length() == 0)
        {
          valid = false;
          break;
        }
      }
    }

    if (!valid)
    {
      BirtException be = new BirtException(MdwReports.PLUGIN_ID, "Missing argument(s) to " + functionName + " function.", new Object[] {""});
      MdwReports.log(be, args);
      throw be;
    }
  }

  protected EnvironmentVO getEnvironment(String name) throws EnvironmentException
  {
    return MdwReports.getInstance().getEnvironmentLocator().getEnvironment(name);
  }

  protected EnvironmentVO getDesignEnvironment(String jdbcUrl) throws EnvironmentException
  {
    return MdwReports.getInstance().getEnvironmentLocator().getDesignEnvironment(jdbcUrl);
  }

  protected URL getReportsWebAppUrl(String jdbcUrl) throws EnvironmentException
  {
    return MdwReports.getInstance().getEnvironmentLocator().getReportsWebAppUrl(jdbcUrl);
  }

  protected URL getTaskManagerUrl(String jdbcUrl) throws EnvironmentException
  {
    return MdwReports.getInstance().getEnvironmentLocator().getTaskManagerUrl(jdbcUrl);
  }

  protected String getStringVal(Object val)
  {
    if (val == null)
      return "";
    else
      return val.toString();
  }

  private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  //private DateFormat df = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
  //private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

  public String getJsonDate(Date date)
  {
    if (date == null)
      return "''";

    return "'" + df.format(date) + "'";
  }

}
