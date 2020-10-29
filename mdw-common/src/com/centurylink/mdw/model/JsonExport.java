package com.centurylink.mdw.model;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.util.DateHelper;
import com.centurylink.mdw.util.JsonUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Export JSON to Excel.
 */
public class JsonExport {

    private static final int ZIP_BUFFER_KB = 16;

    private Jsonable jsonable;
    private String name;
    private JSONObject filters;

    public JsonExport(Jsonable json, String name, JSONObject filters) {
        this.jsonable = json;
        this.name = name;
        this.filters = filters;
    }

    private List<String> names = new ArrayList<>();

    public String exportXlsxBase64() throws JSONException, IOException {
        return exportXlsxBase64(name);
    }

    /**
     * Default behavior adds zip entries for each property on the first (non-mdw) top-level object.
     */
    public String exportZipBase64() throws JSONException, IOException {
        Map<String,JSONObject> objectMap = JsonUtil.getJsonObjects(jsonable.getJson());
        JSONObject mdw = null;
        JSONObject contents = null;
        for (String name : objectMap.keySet()) {
            if ("mdw".equals(name))
                mdw = objectMap.get(name);
            else if (contents == null)
                contents = objectMap.get(name);
        }
        if (contents == null)
            throw new IOException("Cannot find expected contents property");
        else
            objectMap = JsonUtil.getJsonObjects(contents);
        if (mdw != null)
            objectMap.put(".mdw", mdw);

        byte[] buffer = new byte[ZIP_BUFFER_KB * 1024];
        ZipOutputStream zos = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            zos = new ZipOutputStream(outputStream);
            for (String name : objectMap.keySet()) {
                JSONObject json = objectMap.get(name);
                ZipEntry ze = new ZipEntry(name);
                zos.putNextEntry(ze);
                InputStream inputStream = new ByteArrayInputStream(json.toString(2).getBytes());
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
        }
        finally {
            if (zos != null) {
                zos.closeEntry();
                zos.close();
            }
        }
        byte[] bytes = outputStream.toByteArray();
        return new String(Base64.encodeBase64(bytes));
    }

    public String exportXlsxBase64(String name) throws JSONException, IOException {
        Workbook xlsx = exportXlsx(name);
        byte[] bytes = writeExcel(xlsx);
        return new String(Base64.encodeBase64(bytes));
    }

    @SuppressWarnings("squid:S2095")
    public Workbook exportXlsx(String name) throws JSONException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(name == null ? jsonable.getClass().getSimpleName() : name);

        int begin = 0;
        if (filters != null) {
            begin += printFilters(sheet, filters, begin);
        }

        if (jsonable instanceof JsonArray) {
            JSONArray jsonArray = ((JsonArray)jsonable).getArray();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                addNames(jsonObj);
                printRowValues(sheet, i + begin + 1, jsonObj, names);
            }
            printColumnLabels(sheet, names, begin);
        }
        else if (jsonable instanceof InstanceList) {
            InstanceList<?> instanceList = (InstanceList<?>) jsonable;
            List<? extends Jsonable> items = instanceList.getItems();
            for (int i = 0; i < items.size(); i++) {
                JSONObject jsonObj = items.get(i).getJson();
                addNames(jsonObj);
                printRowValues(sheet, i + begin + 1, jsonObj, names);
            }
            printColumnLabels(sheet, names, begin);
        }
        else if (jsonable instanceof JsonList) {
            JsonList<?> jsonList = (JsonList<?>) jsonable;
            List<? extends Jsonable> items = jsonList.getList();
            for (int i = 0; i < items.size(); i++) {
                JSONObject jsonObj = items.get(i).getJson();
                addNames(jsonObj);
                printRowValues(sheet, i + begin + 1, jsonObj, names);
            }
            printColumnLabels(sheet, names, begin);
        }
        else if (jsonable instanceof JsonListMap) {
            JsonListMap<?> listMap = (JsonListMap<?>) jsonable;
            int row = begin;
            List<String> keys = new ArrayList<>(listMap.getJsonables().keySet());
            Collections.sort(keys);
            for (String key : keys) {
                List<? extends Jsonable> jsonableList = listMap.getJsonables().get(key);
                for (Jsonable jsonable : jsonableList) {
                    addNames(jsonable.getJson());
                }
                if (!names.isEmpty() && !"".equals(names.get(0))) {
                    names.add(0, ""); // key column
                }
                for (Jsonable jsonable : jsonableList) {
                    Row valuesRow = printRowValues(sheet, row, jsonable.getJson(), names, 1);
                    Cell cell = valuesRow.createCell(0);
                    cell.setCellValue(key);
                    row++;
                }
            }
            printColumnLabels(sheet, names, begin);
        }
        else {
            throw new UnsupportedOperationException("Unsupported JSON type: " + jsonable);
        }

        return workbook;
    }

    private void addNames(JSONObject json) {
        for (String name: JSONObject.getNames(json)) {
            if (!names.contains(name)) {
                if ("id".equals(name) || "count".equals(name))
                    names.add(0, name);
                else
                    names.add(name);
            }
        }
    }

    private int printFilters(Sheet sheet, JSONObject filters, int begin) {
        int rownum = begin;
        Font bold = sheet.getWorkbook().createFont();
        bold.setBold(true);
        CellStyle labelRowStyle = sheet.getWorkbook().createCellStyle();
        labelRowStyle.setFont(bold);
        labelRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        labelRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row labelRow = sheet.createRow(rownum);
        Cell cell = labelRow.createCell(0);
        cell.setCellValue("Filters:");
        cell.setCellStyle(labelRowStyle);
        labelRow.setRowStyle(labelRowStyle);

        CellStyle nameStyle = sheet.getWorkbook().createCellStyle();
        nameStyle.setFont(bold);

        rownum++;
        for (String name : JSONObject.getNames((filters))){
            Row row = sheet.createRow(rownum);
            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(name);
            nameCell.setCellStyle(nameStyle);
            Object jsonValue = filters.get(name);
            if (jsonValue != null) {
                Cell valueCell = row.createCell(1);
                setCellValue(valueCell, jsonValue);
            }
            rownum++;
        }
        rownum++;

        labelRow = sheet.createRow(rownum);
        cell = labelRow.createCell(0);
        cell.setCellValue("Data:");
        cell.setCellStyle(labelRowStyle);
        labelRow.setRowStyle(labelRowStyle);
        rownum++;
        return rownum;
    }

    private void printColumnLabels(Sheet sheet, List<String> labels, int row) {
        Font bold = sheet.getWorkbook().createFont();
        bold.setBold(true);
        CellStyle boldStyle = sheet.getWorkbook().createCellStyle();
        boldStyle.setFont(bold);

        Row headerRow = sheet.createRow(row);
        for (int i = 0; i < labels.size(); i++) {
            Cell cell = headerRow.createCell(i);
            String label = labels.get(i);
            cell.setCellValue(label);
            cell.setCellStyle(boldStyle);
            if (!"message".equals(label))
                sheet.autoSizeColumn(i);
        }
    }

    private CellStyle dateCellStyle;
    private CellStyle getDateCellStyle(Sheet sheet) {
        if (dateCellStyle == null) {
            dateCellStyle = sheet.getWorkbook().createCellStyle();
            CreationHelper createHelper = sheet.getWorkbook().getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm/dd/yyyy hh:mm:ss"));  // TODO flexible
        }
        return dateCellStyle;
    }

    private Row printRowValues(Sheet sheet, int row, JSONObject json, List<String> names) throws JSONException {
        return printRowValues(sheet, row, json, names, 0);
    }

    private Row printRowValues(Sheet sheet, int row, JSONObject json, List<String> names, int startCol) throws JSONException {
        Row valueRow = sheet.createRow(row);
        for (int i = startCol; i < names.size(); i++) {
            Cell cell = valueRow.createCell(i);
            String name = names.get(i);
            if (json.has(name)) {
                Object jsonValue = json.get(name);
                setCellValue(cell, jsonValue);
            }
        }
        return valueRow;
    }

    private void setCellValue(Cell cell, Object jsonValue) {
        if (jsonValue instanceof Long || jsonValue instanceof Integer) {
            cell.setCellValue(new Double(jsonValue.toString()));
        }
        else if (jsonValue instanceof Boolean) {
            cell.setCellValue((Boolean)jsonValue);
        }
        else if (jsonValue instanceof Date) {
            cell.setCellValue((Date)jsonValue);
            cell.setCellStyle(getDateCellStyle(cell.getSheet()));
        }
        else {
            String stringVal = jsonValue.toString();
            if (stringVal != null && (name.endsWith("Date") || "date".equals(name))) {
                // try to parse as Query date
                try {
                    cell.setCellValue(Query.getDate(stringVal));
                    cell.setCellStyle(getDateCellStyle(cell.getSheet()));
                }
                catch (ParseException ex) {
                    // try StringHelper date
                    Date d = DateHelper.stringToDate(stringVal);
                    if (d == null) {
                        cell.setCellValue(stringVal);
                    }
                    else {
                        cell.setCellValue(d);
                        cell.setCellStyle(getDateCellStyle(cell.getSheet()));
                    }
                }
            }
            else {
                cell.setCellValue(stringVal);
            }
        }
    }

    private byte[] writeExcel(Workbook workBook) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        workBook.write(bytes);
        return bytes.toByteArray();
    }
}
