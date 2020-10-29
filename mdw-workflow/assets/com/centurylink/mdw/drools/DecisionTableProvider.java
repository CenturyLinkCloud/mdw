package com.centurylink.mdw.drools;

import org.apache.commons.lang.StringUtils;
import org.drools.decisiontable.InputType;

import java.io.InputStream;

/**
 * Provider wired to handle Excel 2007 format.
 */
public class DecisionTableProvider {

    public String loadFromInputStream(InputStream inStream, String extension) {
        return loadFromInputStream(inStream, extension, null);
    }

    public String loadFromInputStream(InputStream inStream, String extension, String worksheetName) {

        SpreadsheetCompiler compiler = new SpreadsheetCompiler();

        if (extension.equals("xls") || extension.equals("xlsx")) {
            if (StringUtils.isBlank(worksheetName))
                return compiler.compile(inStream, extension);
            else
                return compiler.compile(inStream, extension, worksheetName);
        }
        else if (extension.equals("csv")) {
            return compiler.compile(inStream, InputType.CSV);
        }
        else {
            return null;
        }
    }
}
