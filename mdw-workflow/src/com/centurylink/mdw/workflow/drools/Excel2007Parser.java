/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.workflow.drools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.drools.decisiontable.parser.DecisionTableParser;
import org.drools.template.parser.DataListener;
import org.drools.template.parser.DecisionTableParseException;

public class Excel2007Parser implements DecisionTableParser {
    
    public static final String DEFAULT_RULESHEET_NAME = "Decision Tables";
    
    private boolean useFirstSheet;
    private Map<String, List<DataListener>> listeners = new HashMap<String, List<DataListener>>();

    public Excel2007Parser(final Map<String, List<DataListener>> sheetListeners) {
        this.listeners = sheetListeners;
    }

    public Excel2007Parser(final List<DataListener> sheetListeners) {
        this.listeners.put(DEFAULT_RULESHEET_NAME, sheetListeners);
        this.useFirstSheet = true;
    }

    public Excel2007Parser(final DataListener listener) {
        List<DataListener> listeners = new ArrayList<DataListener>();
        listeners.add(listener);
        this.listeners.put(DEFAULT_RULESHEET_NAME, listeners);
        this.useFirstSheet = true;
    }
    
    public void parseFile(InputStream inStream) {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(inStream);
            if (useFirstSheet) {
                XSSFSheet sheet = workbook.getSheetAt(0);
                processSheet(sheet, listeners.get(DEFAULT_RULESHEET_NAME));
            }
            else {
                for (String sheetName : listeners.keySet()) {
                    XSSFSheet sheet = workbook.getSheet(sheetName);
                    processSheet(sheet, listeners.get(sheetName));
                }
            }
        }
        catch (IOException ex) {
            throw new DecisionTableParseException(ex.getMessage(), ex);
        }
    }
    
    
    private void processSheet(XSSFSheet sheet, List<? extends DataListener> listeners) {
        
        int mergedRegionCount = sheet.getNumMergedRegions();
        CellRangeAddress[] mergedRanges = new CellRangeAddress[mergedRegionCount];
        for (int i = 0; i < mergedRegionCount; i++) {
            mergedRanges[i] = sheet.getMergedRegion(i);
        }

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                newRow(listeners, i, row.getLastCellNum());
                for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
                    XSSFCell cell = row.getCell(cellNum);
                    if (cell != null) {
                        CellRangeAddress merged = getRangeIfMerged(cell, mergedRanges);
        
                        if (merged != null) {
                            XSSFRow topRow = sheet.getRow(merged.getFirstRow());
                            XSSFCell topLeft = topRow.getCell(merged.getFirstColumn());
                            newCell(listeners, i, cellNum, topLeft.getStringCellValue(), topLeft.getColumnIndex());
                        }
                        else {
                            String cellValue = null;
                            if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN)
                              cellValue = String.valueOf(cell.getBooleanCellValue());
                            else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
                              cellValue = String.valueOf(cell.getNumericCellValue());
                            else
                              cellValue = cell.getStringCellValue();
                            newCell(listeners, i, cellNum, cellValue, DataListener.NON_MERGED);
                        }
                    }
                }
            }
        }
        finishSheet(listeners);
    }
    
    
    private CellRangeAddress getRangeIfMerged(XSSFCell cell, CellRangeAddress[] mergedRanges) {
        for (int i = 0; i < mergedRanges.length; i++) {
            CellRangeAddress range = mergedRanges[i];
            if (range.isInRange(cell.getRowIndex(), cell.getColumnIndex()))
                return range;
        }
        return null;
    }
    
    private void newRow(List<? extends DataListener> listeners, int row, int cols) {
        for (DataListener listener : listeners) {
            listener.newRow(row, cols);
        }
    }
    
    private void newCell(List<? extends DataListener> listeners, int row, int column, String value, int mergedColStart) {
        for (DataListener listener : listeners) {
            listener.newCell(row, column, value, mergedColStart);
        }
    }
    
    private void finishSheet(List<? extends DataListener> listeners) {
        for (DataListener listener : listeners) {
            listener.finishSheet();
        }
    }
    
}
