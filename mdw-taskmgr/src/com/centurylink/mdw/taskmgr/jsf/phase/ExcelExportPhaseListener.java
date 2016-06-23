/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.phase;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;

import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.taskmgr.ui.list.SortableList;
import com.centurylink.mdw.web.export.ExcelExport;

public class ExcelExportPhaseListener implements PhaseListener
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  public void afterPhase(PhaseEvent phaseEvent)
  {
    FacesContext facesContext = phaseEvent.getFacesContext();
    Map<String,String> paramMap = facesContext.getExternalContext().getRequestParameterMap();
    Map<String,Object> sessionMap = facesContext.getExternalContext().getSessionMap();

    if (sessionMap.containsKey("excelExportListId")
        && containsExcelExportKey(paramMap)
        && getServletUrlProperty() == null)
    {
      try
      {
        String listId = (String) sessionMap.get("excelExportListId");
        SortableList list = ListManager.getInstance().getList(listId);
        String format = list.getExportFormat();
        ExcelExport exporter = new ExcelExport(list);

        Workbook generatedExcel;
        if (ExcelExport.XLS_FORMAT.equals(format))
          generatedExcel = exporter.generateXls();
        else
          generatedExcel = exporter.generateXlsx();

        Object contextResponse = facesContext.getExternalContext().getResponse();
        if (contextResponse instanceof HttpServletResponse)
          writeExcelOutput(generatedExcel, (HttpServletResponse) contextResponse, format);

        facesContext.getApplication().getStateManager().saveView(facesContext);
        facesContext.responseComplete();
      }
      catch (Exception ex)
      {
        logger.severeException(ex.getMessage(), ex);
        throw new RuntimeException(ex);
      }
    }
  }

  public void beforePhase(PhaseEvent phaseEvent)
  {
    // do nothing
  }

  public PhaseId getPhaseId()
  {
    return PhaseId.RESTORE_VIEW;
  }

  private void writeExcelOutput(Workbook workBook, HttpServletResponse response, String format) throws IOException
  {
    if (ExcelExport.XLS_FORMAT.equals(format))
      response.setContentType("application/vnd.ms-excel");
    else
      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    workBook.write(response.getOutputStream());
  }

  private boolean containsExcelExportKey(Map<String,String> paramMap)
  {
    for (Iterator<String> keys = paramMap.keySet().iterator(); keys.hasNext(); )
    {
      String key = keys.next();
      if (key.endsWith("excelExport"))
        return true;
    }
    return false;
  }

  private String getServletUrlProperty()
  {
    try
    {
      // if servlet url is specified, delegate export to the servlet
      PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
      String servletUrl = propMgr.getStringProperty("MDWFramework.TaskManagerWeb", "excel.export.servlet.url");
      return servletUrl;
    }
    catch (PropertyException ex)
    {
      if (logger.isDebugEnabled())
        logger.debug("Property not found: excel.export.servlet.url");

      return null;
    }
  }

}
