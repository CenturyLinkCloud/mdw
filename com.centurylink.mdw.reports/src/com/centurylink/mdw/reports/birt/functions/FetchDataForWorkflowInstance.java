/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.project.EnvironmentVO;

/**
 * FetchDataForWorkflowInstance
 */
public class FetchDataForWorkflowInstance extends MdwScriptFunctionExecutor
{
  /**
   * Retrieves data (JSON format) for a specific workflow instance environment.
   * Expected arguments:
   *  0 - Instance Env Name
   *  1 - SQL
   */
  @Override
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 2, "fetchDataForWorkflowInstance");

    DatabaseAccess db = null;
    try
    {
      EnvironmentVO instanceEnv = getEnvironment((String)args[0]);
      String sql = (String)args[1];


      db = new DatabaseAccess(instanceEnv.getJdbcUrl());

      Long before = System.currentTimeMillis();

      StringBuffer json = new StringBuffer();

      db.openConnection();
      ResultSet rs = db.runSelect(sql, null);
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

      json.append("  values: [ \n");
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

      MdwReports.log("FetchDataForWorkflowInstance elapsed ms: " + (System.currentTimeMillis() - before));

      return json.toString();
    }
    catch (Exception ex)
    {
      MdwReports.log(ex);
      throw new BirtException(MdwReports.PLUGIN_ID, "Error loading workflow instance data", ex);
    }
    finally
    {
      if (db != null)
        db.closeConnection();
    }
  }
}
