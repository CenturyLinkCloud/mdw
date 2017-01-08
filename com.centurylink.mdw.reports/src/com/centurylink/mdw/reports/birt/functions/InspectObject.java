/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;

public class InspectObject extends MdwScriptFunctionExecutor
{
  /**
   * Useful in debugging to allow inspection of a JavaScript object.
   * Expected arguments:
   *  0 - object to inspect
   */
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 1, "inspectObject");
    
    Object toInspect = args[0];
    MdwReports.log("Inspecting : " + toInspect);
    return null;
  }
}