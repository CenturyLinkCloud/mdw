/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.test.PackageTests;
import com.centurylink.mdw.test.TestCaseList;

public class TestResultsParser {

    private File suiteResults;
    private List<TestCase> testCases;

    public TestResultsParser(File resultsFile, List<TestCase> testCases) {
        this.suiteResults = resultsFile;
        this.testCases = testCases;
    }

    public void parseJson(File assetRoot) throws JSONException, IOException {
        String json = new String(FileHelper.read(suiteResults));
        TestCaseList testCaseList = new TestCaseList(assetRoot, new JSONObject(json));
        for (PackageTests pkgTests : testCaseList.getPackageTests()) {
            for (com.centurylink.mdw.test.TestCase pkgTest : pkgTests.getTestCases()) {
                for (TestCase testCase : testCases) {
                    if (testCase.getPath().equals(pkgTest.getPackage() + "/" + pkgTest.getName())) {
                        testCase.setStatus(pkgTest.getStatus());
                        testCase.setStartDate(pkgTest.getStart());
                        testCase.setEndDate(pkgTest.getEnd());
                        testCase.setMessage(pkgTest.getMessage());
                    }
                }
            }
            testCaseList.setCount(testCaseList.getTestCases().size());
        }
    }

    public void parseXml() throws Exception {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(suiteResults);
            InputSource src = new InputSource(inputStream);
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();

            SAXParser parser = parserFactory.newSAXParser();
            parser.parse(src, new DefaultHandler() {
                TestCase currentTestCase = null;

                // attributes for workflow project
                public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
                    if (qName.equals("testcase")) {
                        for (TestCase testCase : testCases) {
                            if (testCase.getPath().equals(attrs.getValue("name"))) {
                                currentTestCase = testCase;
                                String timestampStr = attrs.getValue("timestamp");
                                if (timestampStr != null) {
                                    Calendar cal = DatatypeConverter.parseDateTime(timestampStr);
                                    testCase.setStartDate(cal.getTime());
                                    String timeStr = attrs.getValue("time");
                                    if (timeStr != null) {
                                        int ms = (int) Float.parseFloat(timeStr) * 1000;
                                        cal.add(Calendar.MILLISECOND, ms);
                                        testCase.setEndDate(cal.getTime());
                                        testCase.setStatus(TestCase.STATUS_PASS); // assume pass
                                    }
                                }
                            }
                        }
                    }
                    else if (qName.equals("failure")) {
                        if (currentTestCase != null) {
                            currentTestCase.setStatus(TestCase.STATUS_FAIL);
                            TestCaseRun firstRun = currentTestCase.getFirstRun();
                            if (firstRun != null)
                                firstRun.setMessage(attrs.getValue("message"));
                        }
                    }
                    else if (qName.equals("error")) {
                        if (currentTestCase != null) {
                            currentTestCase.setStatus(TestCase.STATUS_ERROR);
                            TestCaseRun firstRun = currentTestCase.getFirstRun();
                            if (firstRun != null)
                                firstRun.setMessage(attrs.getValue("message"));
                        }
                    }
                }
            });
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException ex) {
                }
            }
        }
    }
}
