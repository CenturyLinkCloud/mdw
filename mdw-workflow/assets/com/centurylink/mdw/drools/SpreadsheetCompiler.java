package com.centurylink.mdw.drools;

import org.drools.decisiontable.parser.DecisionTableParser;
import org.drools.decisiontable.parser.DefaultRuleSheetListener;
import org.drools.decisiontable.parser.RuleSheetListener;
import org.drools.decisiontable.parser.xls.ExcelParser;
import org.drools.template.model.DRLOutput;
import org.drools.template.model.Package;
import org.drools.template.parser.DataListener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extends Drools SpreadsheetCompiler to provide Excel 2007 support.
 */
public class SpreadsheetCompiler extends org.drools.decisiontable.SpreadsheetCompiler {

    @Override
    public String compile(InputStream xlsStream, String extension) {
        RuleSheetListener listener = new DefaultRuleSheetListener();
        DecisionTableParser parser = null;

        if (extension.equals("xls"))
            parser = new ExcelParser(listener);
        else if (extension.equals("xlsx"))
            parser = new Excel2007Parser(listener);

        if (parser != null)
            parser.parseFile(xlsStream);
        Package rulePackage = listener.getRuleSet();
        DRLOutput out = new DRLOutput();
        rulePackage.renderDRL(out);
        return out.getDRL();
    }

    public String compile(InputStream stream, String extension, String worksheetName) {
        RuleSheetListener listener = getRuleSheetListener(stream, extension, worksheetName);
        if (listener != null) {
            Package rulePackage = listener.getRuleSet();
            DRLOutput out = new DRLOutput();
            rulePackage.renderDRL(out);
            return out.getDRL();
        }
        return null;
    }

    private RuleSheetListener getRuleSheetListener(InputStream stream, String extension, String worksheetName) {
        RuleSheetListener listener = new DefaultRuleSheetListener();
        Map<String, List<DataListener>> sheetListeners = new HashMap<>();
        List<DataListener> listeners = new ArrayList<>();
        listeners.add(listener);
        sheetListeners.put(worksheetName, listeners);

        if (extension.equals("xls")) {
            ExcelParser parser = new ExcelParser(sheetListeners);
            parser.parseFile(stream);
            return listener;
        }
        else if (extension.equals("xlsx")) {
            Excel2007Parser parser = new Excel2007Parser(sheetListeners);
            parser.parseFile(stream);
            return listener;
        }
        else {
            return null;
        }
    }

}
