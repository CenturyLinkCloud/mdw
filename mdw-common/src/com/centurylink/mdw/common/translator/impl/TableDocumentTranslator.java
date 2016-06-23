/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengTableArray;
import com.qwest.mbeng.MbengTableSchema;

public class TableDocumentTranslator extends DocumentReferenceTranslator {

    private char field_delimiter = ',';
    private char row_delimiter = '#';
    private char escape_char = '\\';
    
    public Object realToObject(String str) throws TranslationException {
        if (str==null) return null;
        int n = str.length();
        List<String> header = null;
        List<String> row;
        int max_columns = 0;
        try {
            MbengTableArray table = new MbengTableArray();
            int k;
            header = new ArrayList<String>();
            k = parseRow(str, n, 0, header);
            max_columns = header.size();
            while (k<n) {
                row = new ArrayList<String>();
                k = parseRow(str, n, k, row);
                if (header==null && max_columns<row.size())
                    max_columns = row.size();
                table.addRow(row.toArray(new String[0]));
            }
            table.setSchema(new TableDocumentSchema(header));
            return table;
        } catch (MbengException e) {
            throw new TranslationException(e.getMessage());
        }
    }
    
    private int parseRow(String str, int n, int k, List<String> res) {
        char ch;
        boolean escaped = false;
        StringBuffer sb = new StringBuffer();
        while (k<n) {
            ch = str.charAt(k);
            if (escaped) {
                escaped = false;
                sb.append(ch);
                k++;
            } else if (ch==escape_char) {
                escaped = true;
                k++;
            } else if (ch==field_delimiter) {
                res.add(sb.toString());
                sb = new StringBuffer();
                k++;

            } else if (ch==row_delimiter) {
                res.add(sb.toString());
                k++;
                return k;   // need to return now w/o add the last field
            } else {
                sb.append(ch);
                k++;
            }
        }
        res.add(sb.toString());
        return k;
    }

    public String realToString(Object object) throws TranslationException {
        try {
            MbengTableArray table = (MbengTableArray)object;
            StringBuffer sb = new StringBuffer();
            MbengTableSchema schema = table.getSchema();
            int n = schema.getColumnCount();
            for (int j=0; j<n; j++) {
                append_escape(sb, schema.getColumnName(j));
                if (j<n-1) sb.append(field_delimiter);
                else sb.append(row_delimiter);
            }
            for (int i=0; i<table.getRowCount(); i++) {
                String[] row = (String[])table.getRow(i);
                n = row.length;
                for (int j=0; j<n; j++) {
                    append_escape(sb, row[j]);
                    if (j<n-1) sb.append(field_delimiter);
                    else sb.append(row_delimiter);
                }
            }
            return sb.toString();
        } catch (MbengException e) {
            throw new TranslationException(e.getMessage());
        }
    }
    
    private void append_escape(StringBuffer sb, String str) {
        if (str==null) return;
        int i, n=str.length();
        for (i=0; i<n; i++) {
            char ch = str.charAt(i);
            if (ch==field_delimiter || ch==row_delimiter || ch==escape_char)
                sb.append(escape_char);
            sb.append(ch);
        }
    }
    
    class TableDocumentSchema implements MbengTableSchema {
        List<String> header;
        TableDocumentSchema(List<String> header) {
            this.header = header;
        }
        public int getColumnCount() {
            return header.size();
        }
        public String getColumnName(int column) {
            return header.get(column);
        }
        public boolean isKey(int column) {
            return column==0;
        }
    }

}
