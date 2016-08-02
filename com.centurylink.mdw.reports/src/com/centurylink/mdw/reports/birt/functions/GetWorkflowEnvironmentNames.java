/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.model.value.project.EnvironmentVO;

public class GetWorkflowEnvironmentNames extends MdwScriptFunctionExecutor
{
  /**
   * Retrieves a list of Workflow Environment names.
   * Expected arguments: (none)
   */
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    try
    {
      List<EnvironmentVO> envVOs = MdwReports.getInstance().getEnvironmentLocator().getEnvironments();
      List<String> envNames = new ArrayList<String>(envVOs.size());
      for (EnvironmentVO envVO : envVOs)
      {
        envNames.add(envVO.getLabel());
      }
      return envNames;
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new BirtException(ex.toString());
    }
  }
}