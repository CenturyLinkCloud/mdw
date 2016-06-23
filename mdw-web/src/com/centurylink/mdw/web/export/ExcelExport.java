/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.export;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.list.ColumnHeader;
import com.centurylink.mdw.web.ui.list.ListItem;
import com.centurylink.mdw.web.ui.list.SortableList;

public class ExcelExport
{
  public static final String XLS_FORMAT = "xls";
  public static final String XLSX_FORMAT = "xlsx";

  private SortableList list;
  public SortableList getList() { return list; }

  public ExcelExport(SortableList list)
  {
    this.list = list;
  }

  public Workbook generateXls() throws UIException
  {
    Workbook workbook = new HSSFWorkbook();
    Sheet sheet = workbook.createSheet(list.getName());
    addColumnHeaders(sheet);
    addColumnValues(sheet);

    return workbook;
  }

  public Workbook generateXlsx() throws UIException
  {
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet(list.getName());
    addColumnHeaders(sheet);
    addColumnValues(sheet);

    return workbook;
  }

  private void addColumnValue(Row rowHeader, Object value, int index)
  {
    Cell cell = rowHeader.createCell(index);
    if (value instanceof Long || value instanceof Integer)
    {
      cell.setCellValue(new Double(value.toString()));
    }
    else if (value instanceof Boolean)
    {
      cell.setCellValue((Boolean)value);
    }
    else if (value instanceof Date)
    {
      cell.setCellValue((Date)value);
    }
    else
    {
      String stringValue = value == null ? "" : value.toString();
      cell.setCellValue(stringValue);
    }
  }

  private void addColumnHeaders(Sheet sheet)
  {
    Row rowHeader = sheet.createRow(0);

    List<?> headers = (List<?>) list.getHeaders().getWrappedData();
    int column = 0;
    for (int i = 0; i < headers.size(); i++)
    {
      ColumnHeader header = (ColumnHeader) headers.get(i);
      if (!header.isCheckbox())
      {
        addColumnValue(rowHeader, header.getLabel(), column);
        column++;
      }
    }
  }

  private void addColumnValues(Sheet sheet)
  {
    List<?> headers = (List<?>) list.getHeaders().getWrappedData();
    List<?> items = (List<?>) list.getItems().getWrappedData();
    for (int i = 0; items != null && i < items.size(); i++)
    {
      ListItem item = (ListItem) items.get(i);
      Row row = sheet.createRow(i + 1);
      int column = 0;
      for (int j = 0 ; j < headers.size(); j++)
      {
        ColumnHeader header = (ColumnHeader) headers.get(j);
        if (!header.isCheckbox())
        {
          Object columnValue = item.getAttributeValue(j);
          String dateFormat = header.getDateFormat();
          if (dateFormat != null && columnValue != null)
          {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            columnValue = sdf.format((Date)columnValue);
          }
          addColumnValue(row, columnValue, column);
          column++;
        }
      }
    }
  }
}
