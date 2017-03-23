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

import java.io.InputStream;

import org.drools.decisiontable.InputType;

import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.util.StringHelper;

/**
 * Provider wired to handle Excel 2007 format.
 */
public class DecisionTableProvider {

    public String loadFromInputStream(InputStream inStream, String format) {
        return loadFromInputStream(inStream, format, null);
    }

    public String loadFromInputStream(InputStream inStream, String format, String worksheetName) {

        SpreadsheetCompiler compiler = new SpreadsheetCompiler();

        if (format.equals(Asset.EXCEL) || format.equals(Asset.EXCEL_2007)) {
            if (StringHelper.isEmpty(worksheetName))
                return compiler.compile(inStream, format);
            else
                return compiler.compile(inStream, format, worksheetName);
        }
        else if (format.equals(Asset.CSV)) {
            return compiler.compile(inStream, InputType.CSV);
        }
        else {
            return null;
        }
    }
}
