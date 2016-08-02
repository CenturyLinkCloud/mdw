/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;

public class GetReportsWebAppUrl extends MdwScriptFunctionExecutor
{
  /**
   * Retrieves the reports runtime environment.
   * Expected arguments:
   *  0 - JDBC URL
   */
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 1, "getReportsWebAppUrl");
    
    try
    {
      return getReportsWebAppUrl((String)args[0]).toString();
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new BirtException(MdwReports.PLUGIN_ID, "Error getting reports webapp URL", ex);
    }
  }
}