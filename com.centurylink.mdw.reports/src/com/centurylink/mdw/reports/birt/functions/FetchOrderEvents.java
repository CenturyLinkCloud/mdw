/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.sql.ResultSet;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.project.EnvironmentVO;

public class FetchOrderEvents extends MdwScriptFunctionExecutor
{
  /**
   * Retrieves the events (JSON format) for a particular order.
   * Expected arguments:
   *  0 - Instance Env Name
   *  1 - Master Request ID
   */
  @Override
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 2, "fetchOrderEvents");

    DatabaseAccess db = null;
    try
    {
      EnvironmentVO instanceEnv = getEnvironment((String)args[0]);
      String masterRequestId = (String)args[1];

      db = new DatabaseAccess(instanceEnv.getJdbcUrl());

      String query
        = "select bmr.master_request_id, bmr.source_system, to_char(bmr.request_time, 'YYYY-MM-DD HH24:MI:SS') as request_time,"
        + "be.event_name, be.event_category, be.event_subcategory, be.event_id, be.event_data, to_char(be.event_time, 'YYYY-MM-DD HH24:MI:SS') as event_time\n"
        + "from bam_event be, bam_master_request bmr\n"
        + "where bmr.master_request_id = ?\n"
        + "and be.master_request_rowid = bmr.master_request_rowid\n"
        + "order by be.event_time";

      Long before = System.currentTimeMillis();

      db.openConnection();
      ResultSet rs = db.runSelect(query, new Object[] {masterRequestId});
      StringBuffer json = new StringBuffer();
      json.append("{ events: [ \n");
      while (rs.next())
      {
        json.append("  { masterRequestId: '" + masterRequestId + "',\n");
        json.append("    sourceSystem: '" + getStringVal(rs.getString(2)) + "',\n");
        json.append("    requestTime: '" + rs.getString(3) + "',\n");
        json.append("    name: '" + rs.getString(4) + "',\n");
        json.append("    category: '" + getStringVal(rs.getString(5)) + "',\n");
        json.append("    subCategory: '" + getStringVal(rs.getString(6)) + "',\n");
        json.append("    id: '" + getStringVal(rs.getString(7)) + "',\n");
        json.append("    data: '',\n");  // TODO - escape and populate
        json.append("    eventTime: '" + rs.getString(9) + "' },\n");
      }
      json.replace(json.length() - 2, json.length(), "\n");  // remove the last comma
      json.append("] }");

      MdwReports.log("FetchOrderEvents elapsed ms: " + (System.currentTimeMillis() - before));

      return json.toString();
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new BirtException(MdwReports.PLUGIN_ID, "Error loading events", ex);
    }
    finally
    {
      if (db != null)
        db.closeConnection();
    }
  }
}
