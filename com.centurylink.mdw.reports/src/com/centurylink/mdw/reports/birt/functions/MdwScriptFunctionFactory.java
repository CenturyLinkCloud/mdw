/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionExecutor;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionFactory;

import com.centurylink.mdw.reports.MdwReports;

public class MdwScriptFunctionFactory implements IScriptFunctionFactory
{
  public static final String SCRIPT_FUNCTION_PACKAGE = "com.centurylink.mdw.reports.birt.functions";
  
  public IScriptFunctionExecutor getFunctionExecutor(String functionName) throws BirtException
  {
    try
    {
      String className = SCRIPT_FUNCTION_PACKAGE + "." + functionName.substring(0, 1).toUpperCase() + functionName.substring(1);
      Class<? extends IScriptFunctionExecutor> functionClass = Class.forName(className).asSubclass(IScriptFunctionExecutor.class);
      return functionClass.newInstance();
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new BirtException(MdwReports.PLUGIN_ID, "Can't instantiate function executor", ex);
    }
  }
}
