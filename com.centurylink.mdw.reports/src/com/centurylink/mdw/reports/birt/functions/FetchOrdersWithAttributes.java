/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;

import com.centurylink.mdw.reports.MdwReports;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.value.project.EnvironmentVO;

public class FetchOrdersWithAttributes extends MdwScriptFunctionExecutor
{
  /**
   * Retrieves the order values (JSON format) along with attributes.
   * Expected arguments:
   *  0 - Instance Env Name
   *  1 - Start Date
   *  2 - End Date
   *  3 - Source System (optional)
   *  4 - Attributes (optional)
   */
  @Override
  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    checkArgs(args, 3, "fetchOrdersWithAttributes");

    DatabaseAccess db = null;
    try
    {
      EnvironmentVO instanceEnv = getEnvironment((String)args[0]);
      Date startDate = (Date)args[1];
      Date endDate = (Date)args[2];
      String sourceSystem = args.length < 4 ? null : (String)args[3];
      Object[] attributes = args.length < 5 ? null : (Object[])args[4];


      db = new DatabaseAccess(instanceEnv.getJdbcUrl());

      String query
        = "select mr.master_request_id, mr.master_request_rowid, mr.source_system, "
        + "to_char(mr.request_time, 'YYYY-MM-DD HH24:MI:SS') as request_time, to_char(mr.record_time, 'YYYY-MM-DD HH24:MI:SS') as record_time, "
        + "a.attribute_name, a.attribute_value\n"
        + "from bam_master_request mr, bam_attribute a\n"
        + "where mr.request_time >= ?\n"
        + "and mr.request_time <= ?\n"
        + "and mr.master_request_rowid = a.master_request_rowid\n";

      if (sourceSystem != null && sourceSystem.length() > 0)
        query += "and mr.source_system = ?\n";

      if (attributes != null && attributes.length > 0)
      {
        for (int i = 0; i < attributes.length; i++)
        {
          Object[] nameVal = (Object[]) attributes[i];

          if (nameVal[0] != null && nameVal[0].toString().length() > 0
              && nameVal[1] != null && nameVal[1].toString().length() > 0)
          {
            query += "and a.master_request_rowid in "
              + "(select a" + i + ".master_request_rowid from bam_attribute a" + i
              + " where a" + i + ".attribute_name = '" + nameVal[0] + "' "
              + "and a" + i + ".attribute_value = '" + nameVal[1] + "')";
          }
        }
      }

      query += "\norder by mr.record_time desc";

      Map<String,MasterRequest> mrs = new TreeMap<String,MasterRequest>(Collections.reverseOrder());

      Long before = System.currentTimeMillis();

      Object[] params = (sourceSystem == null || sourceSystem.length() == 0) ? new Object[]{startDate, endDate} : new Object[]{startDate, endDate, sourceSystem};

      db.openConnection();
      ResultSet rs = db.runSelect(query, params);
      while (rs.next())
      {
        String masterReqId = rs.getString(1);
        MasterRequest mr = mrs.get(masterReqId);
        if (mr == null)
        {
          mr = new MasterRequest();
          mr.masterRequestId = masterReqId;
          mr.rowId = rs.getLong(2);
          mr.sourceSystem = rs.getString(3);
          mr.requestTime = rs.getString(4);
          mr.recordTime = rs.getString(5);
          mr.attributes = new HashMap<String,String>();
          mrs.put(masterReqId, mr);
        }
        mr.attributes.put(rs.getString(6), rs.getString(7));
      }

      StringBuffer json = new StringBuffer();
      json.append("{ masterRequests: [ \n");
      int masterRequestCount = 0;
      for (String masterRequestId : mrs.keySet())
      {
        MasterRequest mr = mrs.get(masterRequestId);
        json.append("  { masterRequestId: '" + masterRequestId + "',\n");
        json.append("    rowId: '" + mr.rowId + "',\n");
        json.append("    sourceSystem: '" + getStringVal(mr.sourceSystem) + "',\n");
        json.append("    requestTime: '" + mr.requestTime + "',\n");
        json.append("    recordTime: '" + mr.recordTime + "',\n");
        json.append("    attributes: [\n");
        int attrCount = 0;
        for (String attrName : mr.attributes.keySet())
        {
          json.append("      { name: '" + attrName + "', value: '" +  getStringVal(mr.attributes.get(attrName)) + "'}");
          if (attrCount < mr.attributes.size() - 1)
            json.append(',');
          json.append('\n');
          attrCount++;
        }
        json.append("    ]\n  }");
        if (masterRequestCount < mrs.size() - 1)
          json.append(',');
        json.append('\n');
        masterRequestCount++;
      }
      json.append("] }");

      MdwReports.log("FetchOrdersWithAttributes elapsed ms: " + (System.currentTimeMillis() - before));

      return json.toString();
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

  @SuppressWarnings("unused")
  private class MasterRequest
  {
    Long rowId;
    String masterRequestId;
    String sourceSystem;
    String requestTime;
    String recordTime;

    Map<String,String> attributes;
  }
}
