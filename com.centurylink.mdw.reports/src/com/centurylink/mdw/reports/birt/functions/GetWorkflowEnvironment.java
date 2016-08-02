/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;

public class GetWorkflowEnvironment extends MdwScriptFunctionExecutor
{
  /**
   * Retrieves a WorkflowEnvironment based on its name.
   * Expected arguments:
   *  0 - Workflow Environment Name
   */
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 1, "getWorkflowEnvironment");

    try
    {
      return getEnvironment((String)args[0]);
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new BirtException(MdwReports.PLUGIN_ID, "Error loading Workflow Environment", ex);
    }
  }
}