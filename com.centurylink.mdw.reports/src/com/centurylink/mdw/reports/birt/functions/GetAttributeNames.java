/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.project.EnvironmentVO;

public class GetAttributeNames extends MdwScriptFunctionExecutor
{
  /**
   * Defines the columns available on the virtual Attributes table.
   * Expected arguments:
   *  0 - Instance Env Name
   *  1 - Start Date
   *  2 - End Date
   */
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 3, "getAttributeNames");

    DatabaseAccess db = null;
    try
    {
      EnvironmentVO instanceEnv = getEnvironment((String)args[0]);

      Object[] dateParams = new Object[] {args[1], args[2]};
      db = new DatabaseAccess(instanceEnv.getJdbcUrl());

      String query
        = "select distinct(attribute_name)\n"
        + "from bam_attribute\n"
        + "where master_request_rowid in\n"
        + "  (select master_request_rowid\n"
        + "   from bam_master_request\n"
        + "   where request_time >= ?\n"
        + "   and request_time <= ?)\n"
        + "   order by lower(attribute_name)";

      List<String> attrNames = new ArrayList<String>();
      db.openConnection();
      ResultSet attrNamesRs = db.runSelect(query, dateParams);
      while (attrNamesRs.next())
        attrNames.add(attrNamesRs.getString(1));

      return attrNames;
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new BirtException(MdwReports.PLUGIN_ID, "Error loading attributes", ex);
    }
    finally
    {
      if (db != null)
        db.closeConnection();
    }
  }
}