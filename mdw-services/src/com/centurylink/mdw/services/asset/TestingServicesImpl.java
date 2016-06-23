/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.asset;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.asset.Asset;
import com.centurylink.mdw.model.value.asset.PackageAssets;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.PackageTests;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseList;

public class TestingServicesImpl implements TestingServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private AssetServices assetServices;

    public TestingServicesImpl() {
        assetServices = new AssetServicesImpl();
    }

    public TestCaseList getTestCases() throws ServiceException {
        return getTestCases(RuleSetVO.getFileExtension(RuleSetVO.TEST).substring(1));
    }

    public TestCaseList getTestCases(String format) throws ServiceException {
        TestCaseList testCaseList = new TestCaseList(assetServices.getAssetRoot());
        testCaseList.setTestCases(new ArrayList<PackageTests>());
        List<TestCase> allTests = new ArrayList<TestCase>();
        Map<String,List<Asset>> pkgAssets = assetServices.getAssetsOfType(format);
        for (String pkgName : pkgAssets.keySet()) {
            List<Asset> assets = pkgAssets.get(pkgName);
            PackageTests pkgTests = new PackageTests(assetServices.getPackage(pkgName));
            pkgTests.setTestCases(new ArrayList<TestCase>());
            for (Asset asset : assets) {
                TestCase testCase = new TestCase(pkgName, asset);
                pkgTests.getTestCases().add(testCase);
                allTests.add(testCase);
            }
            testCaseList.getTestCases().add(pkgTests);
        }
        addStatusInfo(allTests);
        return testCaseList;
    }

    public TestCase getTestCase(String path) throws ServiceException {
        try {
            TestCase testCase = readTestCase(path);
            addStatusInfo(testCase);
            return testCase;
        }
        catch (IOException ex) {
            throw new ServiceException("IO Error reading test case: " + path, ex);
        }
    }

    private void addStatusInfo(List<TestCase> testCases) {
        try {
            if (!testCases.isEmpty()) {
                File suiteResults = getTestResultsFile(testCases.get(0).getAsset().getExtension());
                if (suiteResults != null && suiteResults.isFile())
                    processResultsFile(suiteResults, testCases);
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to get status info for testCases", ex);
        }
    }

    private void addStatusInfo(TestCase testCase) {
        try {
            File suiteResults = getTestResultsFile(testCase.getAsset().getExtension());
            if (suiteResults != null && suiteResults.isFile())
                processResultsFile(suiteResults, testCase);
        }
        catch (Exception ex) {
            logger.severeException("Unable to get status info for testCase: " + testCase.getName(), ex);
        }
    }

    private TestCase readTestCase(String path) throws ServiceException, IOException {
        String pkg = path.substring(0, path.lastIndexOf('/'));
        Asset testCaseAsset = assetServices.getAsset(path);
        String rootName = testCaseAsset.getRootName();
        TestCase testCase = new TestCase(pkg, testCaseAsset);
        PackageAssets pkgAssets = assetServices.getAssets(pkg);
        String yamlExt = RuleSetVO.getFileExtension(RuleSetVO.YAML);
        File resultsDir = getTestResultsDir();
        // TODO: support specified (non-convention) expected results
        for (Asset pkgAsset : pkgAssets.getAssets()) {
            if (pkgAsset.getName().endsWith(yamlExt) && rootName.equals(pkgAsset.getRootName())) {
                testCase.setExpected(pkg + "/" + pkgAsset.getName());
                if (resultsDir != null) {
                    if (new File(resultsDir + "/" + pkg + "/" + pkgAsset.getName()).isFile())
                        testCase.setActual(pkg + "/" + pkgAsset.getName());
                    if (new File(resultsDir + "/" + pkg + "/" + pkgAsset.getRootName() + ".log").isFile())
                        testCase.setExecuteLog(pkg + "/" + pkgAsset.getRootName() + ".log");
                }
            }
        }
        return testCase;
    }

    private void processResultsFile(File resultsFile, final List<TestCase> testCases) throws Exception {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(resultsFile);
            InputSource src = new InputSource(inputStream);
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();

            SAXParser parser = parserFactory.newSAXParser();
            parser.parse(src, new DefaultHandler() {
                TestCase currentTestCase = null;

                // attributes for workflow project
                public void startElement(String uri, String localName, String qName,
                        Attributes attrs) throws SAXException {
                    if (qName.equals("testcase")) {
                        for (TestCase testCase : testCases) {
                            if (testCase.getPath().equals(attrs.getValue("name"))) {
                                currentTestCase = testCase;
                                String timestampStr = attrs.getValue("timestamp");
                                if (timestampStr != null) {
                                    Calendar cal = DatatypeConverter.parseDateTime(timestampStr);
                                    testCase.setStart(cal.getTime());
                                    String timeStr = attrs.getValue("time");
                                    if (timeStr != null) {
                                        int ms = (int) Float.parseFloat(timeStr) * 1000;
                                        cal.add(Calendar.MILLISECOND, ms);
                                        testCase.setEnd(cal.getTime());
                                        testCase.setStatus(TestCase.Status.Passed); // assume pass
                                    }
                                }
                            }
                        }
                    }
                    else if (qName.equals("failure")) {
                        if (currentTestCase != null) {
                            currentTestCase.setStatus(TestCase.Status.Failed);
                            currentTestCase.setMessage(attrs.getValue("message"));
                        }
                    }
                    else if (qName.equals("error")) {
                        if (currentTestCase != null) {
                            currentTestCase.setStatus(TestCase.Status.Errored);
                            currentTestCase.setMessage(attrs.getValue("message"));
                        }
                    }
                }
            });
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void processResultsFile(File resultsFile, final TestCase testCase) throws Exception {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(resultsFile);
            InputSource src = new InputSource(inputStream);
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();

            SAXParser parser = parserFactory.newSAXParser();
            parser.parse(src, new DefaultHandler() {
                TestCase currentTestCase = null;

                // attributes for workflow project
                public void startElement(String uri, String localName, String qName,
                        Attributes attrs) throws SAXException {
                    if (qName.equals("testcase")) {
                        String name = attrs.getValue("name");
                        if (testCase.getPath().equals(name)) {
                            currentTestCase = testCase;
                            String timestampStr = attrs.getValue("timestamp");
                            if (timestampStr != null) {
                                Calendar cal = DatatypeConverter.parseDateTime(timestampStr);
                                testCase.setStart(cal.getTime());
                                String timeStr = attrs.getValue("time");
                                if (timeStr != null) {
                                    int ms = (int) Float.parseFloat(timeStr) * 1000;
                                    cal.add(Calendar.MILLISECOND, ms);
                                    testCase.setEnd(cal.getTime());
                                    testCase.setStatus(TestCase.Status.Passed); // assume pass
                                }
                            }
                        }
                        else {
                            currentTestCase = null;
                        }
                    }
                    else if (qName.equals("failure")) {
                        if (currentTestCase != null) {
                            currentTestCase.setStatus(TestCase.Status.Failed);
                            currentTestCase.setMessage(attrs.getValue("message"));
                        }
                    }
                    else if (qName.equals("error")) {
                        if (currentTestCase != null) {
                            currentTestCase.setStatus(TestCase.Status.Errored);
                            currentTestCase.setMessage(attrs.getValue("message"));
                        }
                    }
                }
            });
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public File getTestResultsDir() throws IOException {
        File resultsDir = null;
        String resultsLoc = PropertyManager.getProperty(PropertyNames.MDW_TEST_RESULTS_LOCATION);
        if (resultsLoc == null) {
            String gitLocalPath = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
            if (gitLocalPath != null)
                resultsLoc = gitLocalPath + "/testResults";
            else {
                File assetRoot = assetServices.getAssetRoot();
                String rootPath = assetRoot.toString().replace('\\', '/');
                if (rootPath.endsWith("mdw-workflow/assets"))
                    resultsLoc = assetRoot.getParentFile() + "/testResults";
                else if (rootPath.endsWith("workflow/assets"))
                    resultsLoc = assetRoot.getParentFile().getParentFile() + "/testResults";
                else if (rootPath.endsWith("assets"))
                    resultsLoc = assetRoot.getParentFile() + "/testResults";
            }
        }
        if (resultsLoc != null) {
            resultsDir = new File(resultsLoc);
            if (!resultsDir.isDirectory() && !resultsDir.mkdir())
                throw new IOException("Unable to create directory: " + resultsDir);
        }
        return resultsDir;
    }

    public File getTestResultsFile(String format) throws IOException {
        File resultsDir = getTestResultsDir();
        if (resultsDir == null)
            return null;
        String summaryFile = null;
        if (format == null || RuleSetVO.getFileExtension(RuleSetVO.TEST).equals("." + format)) {
            summaryFile = PropertyManager.getProperty(PropertyNames.MDW_FUNCTION_TESTS_SUMMARY_FILE);
            if (summaryFile == null)
                summaryFile = "mdw-function-test-results.xml";
        }
        else if (RuleSetVO.getFileExtension(RuleSetVO.FEATURE).equals("." + format)) {
            summaryFile = PropertyManager.getProperty(PropertyNames.MDW_FEATURE_TESTS_SUMMARY_FILE);
            if (summaryFile == null)
                summaryFile = "mdw-cucumber-test-results.xml";
        }

        return summaryFile == null ? null : new File(resultsDir + "/" + summaryFile);
    }

}
