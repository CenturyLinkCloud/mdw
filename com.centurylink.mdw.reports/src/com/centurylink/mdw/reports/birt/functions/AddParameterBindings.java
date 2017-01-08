/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.reports.birt.functions;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;
import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetParameterHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.StructureFactory;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.elements.structures.OdaDataSetParameter;

import com.centurylink.mdw.reports.MdwReports;

public class AddParameterBindings extends MdwScriptFunctionExecutor
{
  private static Pattern tokenPattern = Pattern.compile("(\\$\\{.*?\\})");
  private int paramCount = 0;

  public Object execute(Object[] args, IScriptFunctionContext context) throws BirtException
  {
    if (args.length < 1)
      throw new BirtException(MdwReports.PLUGIN_ID, "Missing argument(s) to addParameterBindings function", new Object[] {""});

    IReportContext rptContext = (IReportContext) args[0];

    // do not run this code if in the dataSet editor
    //if (isDataSetEditor(rptContext))
    //return null;

    ReportDesignHandle designHandle = (ReportDesignHandle) rptContext.getReportRunnable().getDesignHandle();
    
    @SuppressWarnings("unchecked")
    final List<DataSetHandle> dsAll = designHandle.getAllDataSets();
    for (DataSetHandle dataSetHandle : dsAll)
    {
      if (dataSetHandle instanceof OdaDataSetHandle)
      {
        bindParameters(rptContext, (OdaDataSetHandle) dataSetHandle);
      }
    }

    return null;
  }

  /**
   * For each OdaDataSet walk through looking for appropriate SQL pattern and change query text to
   * use ? where appropriate and re-order parameters.
   */
  private void bindParameters(IReportContext rptContext, OdaDataSetHandle odaDataSetHandle)
  throws SemanticException
  {
    String sqlText = odaDataSetHandle.getQueryText();
    if (sqlText.endsWith("?"))
      sqlText += "\n";

    DynamicQueryText qryTextObject = new DynamicQueryText(rptContext, odaDataSetHandle);
    String newSql = qryTextObject.processQueryText(sqlText);

    odaDataSetHandle.setQueryText(newSql);
    reOrderParameters(odaDataSetHandle);
  }

  /**
   * As parameter bindings are added, the position is accurately tracked,
   * but the ODA does not pay attention to the position when running.
   * <p>
   * Re-orders the parameters in the list to match the position variable.
   */
  private void reOrderParameters(OdaDataSetHandle curDataSet)
  {
    int pos = 0;
    Iterator<?> paramsIter = curDataSet.parametersIterator();
    while (paramsIter.hasNext())
    {
      OdaDataSetParameterHandle handle = (OdaDataSetParameterHandle) paramsIter.next();
      pos++;
      handle.setPosition(pos);
    }
  }

  /**
   * Inner class that represents a SQL string that may contain both standard parameters
   * (?) and dynamic parameters in the form ${processParamName}.
   * <p>
   * Handles the string substitutions required to change all of the dynamic
   * parameters into standard JDBC parameters.
   * <p>
   * In addition, adds parameter bindings in the appropriate locations for all of the
   * dynamic parameters. The dynamic parameters will be hard-coded to the actual value 
   * of the passed parameter.
   * 
   */
  private final class DynamicQueryText
  {
    int originalParamIndex = 0;
    int addedParamCount = 0;
    StringBuffer sb = new StringBuffer();
    String paramToken = "";
    final IReportContext rptContext;
    final OdaDataSetHandle odaDataSetHandle;

    public DynamicQueryText(final IReportContext rptContext, final OdaDataSetHandle odaDataSetHandle)
    {
      this.rptContext = rptContext;
      this.odaDataSetHandle = odaDataSetHandle;
    }

    public String processQueryText(final String sqlText) throws SemanticException
    {
      originalParamIndex = 0;
      addedParamCount = 0;
      sb = new StringBuffer();
      paramToken = "";
      String[] sqlParts = sqlText.split("\\?");
      for (String part : sqlParts)
      {
        processSqlPart(part);
      }
      return sb.toString();
    }

    private final void processSqlPart(String part) throws SemanticException
    {
      sb.append(paramToken);
      paramToken = "?";
      final Matcher matcher = tokenPattern.matcher(part);
      int index = 0;
      while (matcher.find())
      {
        String match = matcher.group();
        String prefix = part.substring(index, matcher.start());
        String paramName = match.substring(2, match.length() - 1);
        index = matcher.end();
        
        Object paramObject = rptContext.getParameterValue(paramName);
        int newParamIndex = originalParamIndex + addedParamCount;
        if (paramObject != null)
        {
          if (paramObject instanceof Object[])
          {
            Object[] paramObjectArray = (Object[]) paramObject;
            StringBuffer subsb = new StringBuffer();
            String sep = "";
            int subParamCount = 0;
            for (Object subParamObject : paramObjectArray)
            {
              if (addParameterBinding(odaDataSetHandle, newParamIndex, subParamObject))
              {
                subsb.append(sep);
                sep = ", ";
                subsb.append("?");
                subParamCount++;
              }
            }
            if (subParamCount > 0)
            {
              addedParamCount += subParamCount;
              sb.append(prefix + subsb);
            }
          }
          else if (addParameterBinding(odaDataSetHandle, newParamIndex, paramObject))
          {
            sb.append(prefix + "?");
            addedParamCount++;
          }
        }
      }

      sb.append(part.substring(index));
      originalParamIndex++;
    }

    /**
     * For a given DataSet add a parameter binding to the static value that is passed to the report.
     * <p>
     * Use the position variable to determine the appropriate location for the parameter (as it
     * appears in the query),
     */
    private boolean addParameterBinding(OdaDataSetHandle odaDataSetHandle, int newParamIndex, Object paramObject)
    throws SemanticException
    {
      if (paramObject instanceof Double)
      {
        addParameterBinding(odaDataSetHandle, newParamIndex, "float", paramObject.toString());
        return true;
      }
      if (paramObject instanceof Date)
      {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(((Date) paramObject).getTime());
        if (calendar.get(Calendar.YEAR) == 0 && calendar.get(Calendar.MONTH) == 0 && calendar.get(Calendar.DAY_OF_MONTH) == 0)
        {
          SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
          addParameterBinding(odaDataSetHandle, newParamIndex, "time", dateFormat.format((Date) paramObject));
        }
        else if (calendar.get(Calendar.HOUR) == 0 && calendar.get(Calendar.MINUTE) == 0
            && calendar.get(Calendar.SECOND) == 0 && calendar.get(Calendar.MILLISECOND) == 0)
        {
          SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
          addParameterBinding(odaDataSetHandle, newParamIndex, "date", dateFormat
              .format((Date) paramObject));
        }
        else
        {
          SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
          addParameterBinding(odaDataSetHandle, newParamIndex, "datetime", dateFormat
              .format((Date) paramObject));
        }
        return true;
      }
      if (paramObject instanceof Boolean)
      {
        addParameterBinding(odaDataSetHandle, newParamIndex, "boolean", paramObject.toString());
        return true;
      }
      if (paramObject instanceof BigDecimal)
      {
        addParameterBinding(odaDataSetHandle, newParamIndex, "decimal", paramObject.toString());
        return true;
      }
      if (paramObject instanceof Integer)
      {
        addParameterBinding(odaDataSetHandle, newParamIndex, "integer", paramObject.toString());
        return true;
      }
      if (paramObject instanceof String)
      {
        String string = (String) paramObject;
        if ("null".equals(string))
          return false;
        addParameterBinding(odaDataSetHandle, newParamIndex, "string", quote(string));
        return true;
      }
      addParameterBinding(odaDataSetHandle, newParamIndex, "string", quote(paramObject.toString()));
      return true;
    }

    private String quote(String string)
    {
      return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void addParameterBinding(OdaDataSetHandle odaDataSetHandle, int paramIndex,
        String dataType, String dataValue) throws SemanticException
    {
      OdaDataSetParameter parameter = StructureFactory.createOdaDataSetParameter();
      parameter.setName("rsp_param_" + paramCount++);
      parameter.setPosition(paramIndex);
      parameter.setDataType(dataType);
      parameter.setDefaultValue(dataValue);
      parameter.setIsInput(true);
      parameter.setIsOutput(false);
      PropertyHandle parameterHandle = odaDataSetHandle.getPropertyHandle(DataSetHandle.PARAMETERS_PROP);
      parameterHandle.insertItem(parameter, paramIndex);
    }

  }

}
