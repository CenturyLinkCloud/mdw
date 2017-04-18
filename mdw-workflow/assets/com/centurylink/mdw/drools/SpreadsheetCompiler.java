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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.decisiontable.parser.DecisionTableParser;
import org.drools.decisiontable.parser.DefaultRuleSheetListener;
import org.drools.decisiontable.parser.RuleSheetListener;
import org.drools.decisiontable.parser.xls.ExcelParser;
import org.drools.template.model.DRLOutput;
import org.drools.template.model.Package;
import org.drools.template.parser.DataListener;

import com.centurylink.mdw.model.asset.Asset;

/**
 * Extends Drools SpreadsheetCompiler to provide Excel 2007 support.
 */
public class SpreadsheetCompiler extends org.drools.decisiontable.SpreadsheetCompiler {

    @Override
    public String compile(InputStream xlsStream, String format) {
        RuleSheetListener listener = new DefaultRuleSheetListener();
        DecisionTableParser parser = null;

        if (format.equals(Asset.EXCEL))
            parser = new ExcelParser(listener);
        else if (format.equals(Asset.EXCEL_2007))
            parser = new Excel2007Parser(listener);


        parser.parseFile(xlsStream);
        Package rulePackage = listener.getRuleSet();
        DRLOutput out = new DRLOutput();
        rulePackage.renderDRL(out);
        return out.getDRL();
    }

    public String compile(InputStream stream, String format, String worksheetName) {
        RuleSheetListener listener = getRuleSheetListener(stream, format, worksheetName);
        Package rulePackage = listener.getRuleSet();
        DRLOutput out = new DRLOutput();
        rulePackage.renderDRL(out);
        return out.getDRL();
    }

    private RuleSheetListener getRuleSheetListener(InputStream stream, String format, String worksheetName) {
        RuleSheetListener listener = new DefaultRuleSheetListener();
        Map<String, List<DataListener>> sheetListeners = new HashMap<String, List<DataListener>>();
        List<DataListener> listeners = new ArrayList<DataListener>();
        listeners.add(listener);
        sheetListeners.put(worksheetName, listeners);

        if (format.equals(Asset.EXCEL)) {
            ExcelParser parser = new ExcelParser(sheetListeners);
            parser.parseFile(stream);
            return listener;
        }
        else if (format.equals(Asset.EXCEL_2007)) {
            Excel2007Parser parser = new Excel2007Parser(sheetListeners);
            parser.parseFile(stream);
            return listener;
        }
        else {
            return null;
        }
    }

}
