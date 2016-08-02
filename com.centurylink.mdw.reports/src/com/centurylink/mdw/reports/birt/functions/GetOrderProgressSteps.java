/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.project.EnvironmentVO;

public class GetOrderProgressSteps extends MdwScriptFunctionExecutor
{
  /**
   * Retrieves the steps (JSON format) for a particular order.
   * Expected arguments:
   *  0 - Instance Env Name
   *  1 - Master Request ID
   */
  @Override
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 2, "getOrderProgressSteps");

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

      Map<String,Step> steps = new HashMap<String,Step>();

      String overallStartTime = null;
      String overallCompleteTime = null;

      db.openConnection();
      ResultSet rs = db.runSelect(query, new Object[] {masterRequestId});

      while (rs.next())
      {
        String name = rs.getString(4);
        String time = rs.getString(9);

        if (name.equals("Submit"))
          overallStartTime =  time;

        overallCompleteTime = time; // last one will stick

        String subCat = rs.getString(6);
        if (subCat != null && (subCat.equals("Start") || subCat.equals("Complete")))
        {
          Step step = steps.get(name);
          if (step == null)
          {
            step = new Step();
            step.name = name;
            step.sourceSystem = rs.getString(2);
            step.category = rs.getString(5);
            step.eventId = rs.getString(7);
            step.data = ""; // TODO
            steps.put(name, step);
          }

          if (subCat.equals("Start") && step.startTime == null)
            step.startTime = time;
          else if (subCat.equals("Complete") && step.startTime != null)
            step.endTime = time;
        }
      }

      StringBuffer json = new StringBuffer();
      json.append("{ masterRequestId: '" + masterRequestId + "',");
      json.append(" overallStartTime: '" + overallStartTime + "',");
      json.append(" overallCompleteTime: '" + overallCompleteTime + "',\n");
      json.append(" steps: [ \n");
      for (String stepName : steps.keySet())
      {
        Step step = steps.get(stepName);
        json.append("  { name: '" + getStringVal(step.name) + "',\n");
        json.append("    sourceSystem: '" + getStringVal(step.sourceSystem) + "',\n");
        json.append("    category: '" + getStringVal(step.category) + "',\n");
        json.append("    eventId: '" + getStringVal(step.eventId) + "',\n");
        json.append("    data: '" + getStringVal(step.data) + "',\n");
        json.append("    startTime: '" + getStringVal(step.startTime) + "',\n");
        json.append("    endTime: '" + getStringVal(step.endTime) + "' },\n");
      }
      json.replace(json.length() - 2, json.length(), "\n");  // remove the last comma
      json.append("] }");

      MdwReports.log("GetOrderProgressSteps elapsed ms: " + (System.currentTimeMillis() - before));

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

  private class Step
  {
    String name;
    String sourceSystem;
    String category;
    String eventId;
    String data;
    String startTime;
    String endTime;
  }
}
