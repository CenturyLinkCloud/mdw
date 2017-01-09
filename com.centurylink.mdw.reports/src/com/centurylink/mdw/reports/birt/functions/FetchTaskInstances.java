/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.project.EnvironmentVO;

public class FetchTaskInstances extends MdwScriptFunctionExecutor
{
  /**
   * Retrieves task instance related data (JSON format).
   * Expected arguments:
   *  0 - Instance Env Name
   *  1 - Start Date
   */
  @Override
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 2, "fetchTaskInstances");

    DatabaseAccess db = null;
    try
    {
      EnvironmentVO instanceEnv = getEnvironment((String)args[0]);
      Date startDate = (Date)args[1];


      db = new DatabaseAccess(instanceEnv.getJdbcUrl());

      String query
        = "select *\n" +
  		"from task_instance ti, user_info ui, task_status ts, task t\n" +
  		"where ti.task_id = t.task_id\n" +
  		"and ti.task_instance_status = ts.task_status_id\n" +
  		"and ti.task_claim_user_id(+) = ui.user_info_id\n" +
  		"and task_start_dt > ?\n" +
  		"order by ti.task_instance_id";

      Long before = System.currentTimeMillis();

      Object[] params = new Object[]{startDate};
      StringBuffer json = new StringBuffer();

      db.openConnection();
      ResultSet rs = db.runSelect(query, params);
      ResultSetMetaData rsmd = rs.getMetaData();
      Map<String,Integer> cols = new HashMap<String,Integer>();
      json.append("{ types: {");
      for (int i = 1; i <= rsmd.getColumnCount(); i++)
      {
        String colName = rsmd.getColumnName(i);
        if (!cols.containsKey(colName))
        {
          cols.put(colName, i);
          json.append(colName).append(":'").append(rsmd.getColumnTypeName(i)).append("'");
          if (i < rsmd.getColumnCount())
            json.append(", ");
        }
      }
      json.append("}, \n");

      json.append("  tasks: [ \n");
      while (rs.next())
      {

        json.append("  {");
        int count = 0;
        for (int i : cols.values())
        {
          String colName = rsmd.getColumnName(i);
          String colType = rsmd.getColumnTypeName(i);
          json.append(colName).append(":");
          if ("DATE".equals(colType))
            json.append(getJsonDate(rs.getTimestamp(i)));
          else if ("NUMBER".equals(colType))
            json.append(rs.getLong(i));
          else
            json.append("'").append(getStringVal(rs.getString(i))).append("'");

          if (count < cols.values().size() - 1)
            json.append(", ");
          count++;
        }
        json.append(" },\n");
      }
      json = json.replace(json.length() - 2, json.length(), "");
      json.append("] }");

      MdwReports.log("FetchTaskInstances elapsed ms: " + (System.currentTimeMillis() - before));

      return json.toString();
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new BirtException(MdwReports.PLUGIN_ID, "Error retrieving task instances", ex);
    }
    finally
    {
      if (db != null)
        db.closeConnection();
    }
  }
}
