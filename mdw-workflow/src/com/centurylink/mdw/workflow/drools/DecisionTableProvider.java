/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.drools;

import java.io.InputStream;

import org.drools.decisiontable.InputType;

import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

/**
 * Provider wired to handle Excel 2007 format.
 */
public class DecisionTableProvider {

    public String loadFromInputStream(InputStream inStream, String format) {
        return loadFromInputStream(inStream, format, null);
    }

    public String loadFromInputStream(InputStream inStream, String format, String worksheetName) {

        SpreadsheetCompiler compiler = new SpreadsheetCompiler();

        if (format.equals(RuleSetVO.EXCEL) || format.equals(RuleSetVO.EXCEL_2007)) {
            if (StringHelper.isEmpty(worksheetName))
                return compiler.compile(inStream, format);
            else
                return compiler.compile(inStream, format, worksheetName);
        }
        else if (format.equals(RuleSetVO.CSV)) {
            return compiler.compile(inStream, InputType.CSV);
        }
        else {
            return null;
        }
    }
}
