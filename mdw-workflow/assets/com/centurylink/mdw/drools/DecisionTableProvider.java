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
