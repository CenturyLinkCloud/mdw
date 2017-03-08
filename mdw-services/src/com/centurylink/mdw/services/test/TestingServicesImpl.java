/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.PackageAssets;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.services.asset.AssetServicesImpl;
import com.centurylink.mdw.test.PackageTests;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class TestingServicesImpl implements TestingServices {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private AssetServices assetServices;

    public TestingServicesImpl() {
        assetServices = new AssetServicesImpl();
    }

    public TestCaseList getTestCases() throws ServiceException {
        return getTestCases(Asset.getFileExtension(Asset.TEST).substring(1));
    }

    public TestCaseList getTestCaseList(TestCase testCase) throws ServiceException {
        AssetServices assetServices = ServiceLocator.getAssetServices();
        PackageDir pkgDir = assetServices.getPackage(testCase.getPackage());
        PackageTests pkgTests = new PackageTests(pkgDir);
        List<PackageTests> packageTests = new ArrayList<PackageTests>();
        packageTests.add(pkgTests);
        List<TestCase> testCases = new ArrayList<TestCase>();
        testCases.add(testCase);
        pkgTests.setTestCases(testCases);
        TestCaseList testCaseList = new TestCaseList(assetServices.getAssetRoot());
        testCaseList.setPackageTests(packageTests);
        testCaseList.setCount(1);
        return testCaseList;
    }

    public TestCaseList getTestCases(String format) throws ServiceException {
        TestCaseList testCaseList = new TestCaseList(assetServices.getAssetRoot());
        testCaseList.setPackageTests(new ArrayList<PackageTests>());
        List<TestCase> allTests = new ArrayList<TestCase>();
        Map<String,List<AssetInfo>> pkgAssets = assetServices.getAssetsOfType(format);
        for (String pkgName : pkgAssets.keySet()) {
            List<AssetInfo> assets = pkgAssets.get(pkgName);
            PackageTests pkgTests = new PackageTests(assetServices.getPackage(pkgName));
            pkgTests.setTestCases(new ArrayList<TestCase>());
            for (AssetInfo asset : assets) {
                TestCase testCase = new TestCase(pkgName, asset);
                pkgTests.getTestCases().add(testCase);
                allTests.add(testCase);
            }
            testCaseList.getPackageTests().add(pkgTests);
        }
        testCaseList.setCount(allTests.size());
        long lastMod = addStatusInfo(allTests);
        if (lastMod != -1)
            testCaseList.setRetrieveDate(new Date(lastMod));
        // sort
        testCaseList.sort();
        return testCaseList;
    }

    public TestCase getTestCase(String path) throws ServiceException {
        try {
            TestCase testCase = readTestCase(path);
            addStatusInfo(testCase);
            AssetServices assetServices = ServiceLocator.getAssetServices();
            VersionControlGit vcGit = (VersionControlGit) assetServices.getVersionControl();
            if (vcGit != null && PropertyManager.getProperty(PropertyNames.MDW_GIT_USER) != null) {
                testCase.getAsset()
                    .setCommitInfo(vcGit.getCommitInfo(
                            vcGit.getRelativePath(new File(assetServices.getAssetRoot() + "/"
                                    + testCase.getPackage().replace('.', '/') + "/"
                                    + testCase.getAsset().getName()))));
            }
            return testCase;
        }
        catch (Exception ex) {
            throw new ServiceException("IO Error reading test case: " + path, ex);
        }
    }

    /**
     * Returns the last modified timestamp for the results file.
     */
    private long addStatusInfo(List<TestCase> testCases) {
        try {
            if (!testCases.isEmpty()) {
                //

                File resultsFile = getTestResultsFile(testCases.get(0).getAsset().getExtension());
                if (resultsFile != null && resultsFile.isFile()) {
                    if (resultsFile.getName().endsWith(".xml"))
                        processResultsFileXml(resultsFile, testCases);
                    else
                        processResultsFile(resultsFile, testCases);
                    return resultsFile.lastModified();
                }
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to get status info for testCases", ex);
        }
        return -1;
    }

    private void addStatusInfo(TestCase testCase) {
        try {
            File resultsFile = getTestResultsFile(testCase.getAsset().getExtension());
            if (resultsFile != null && resultsFile.isFile()) {
                if (resultsFile.getName().endsWith(".xml"))
                    processResultsFileXml(resultsFile, testCase);
                else
                    processResultsFile(resultsFile, testCase);
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to get status info for testCase: " + testCase.getName(), ex);
        }
    }

    private TestCase readTestCase(String path) throws ServiceException, IOException {
        String pkg = path.substring(0, path.lastIndexOf('/'));
        AssetInfo testCaseAsset = assetServices.getAsset(path);
        String rootName = testCaseAsset.getRootName();
        TestCase testCase = new TestCase(pkg, testCaseAsset);
        PackageAssets pkgAssets = assetServices.getAssets(pkg);
        String yamlExt = Asset.getFileExtension(Asset.YAML);
        File resultsDir = getTestResultsDir();
        // TODO: support specified (non-convention) expected results
        for (AssetInfo pkgAsset : pkgAssets.getAssets()) {
            if (pkgAsset.getName().endsWith(yamlExt) && rootName.equals(pkgAsset.getRootName())) {
                testCase.setExpected(pkg + "/" + pkgAsset.getName());
                if (resultsDir != null) {
                    if (new File(resultsDir + "/" + pkg + "/" + pkgAsset.getName()).isFile())
                        testCase.setActual(pkg + "/" + pkgAsset.getName());
                }
            }
        }
        if (new File(resultsDir + "/" + pkg + "/" + testCaseAsset.getRootName() + ".log").isFile())
            testCase.setExecuteLog(pkg + "/" + testCaseAsset.getRootName() + ".log");
        return testCase;
    }

    private void processResultsFile(File resultsFile, List<TestCase> testCases) throws Exception {
        String jsonString = new String(Files.readAllBytes(resultsFile.toPath()));
        TestCaseList testCaseList = new TestCaseList(ApplicationContext.getAssetRoot(), new JSONObject(jsonString));
        for (TestCase testCase : testCases) {
            TestCase caseFromFile = testCaseList.getTestCase(testCase.getPath());
            if (caseFromFile != null) {
                addInfo(testCase, caseFromFile);
            }
        }
    }

    private void processResultsFile(File resultsFile, final TestCase testCase) throws Exception {
        String jsonString = new String(Files.readAllBytes(resultsFile.toPath()));
        TestCaseList testCaseList = new TestCaseList(ApplicationContext.getAssetRoot(), new JSONObject(jsonString));
        TestCase caseFromFile = testCaseList.getTestCase(testCase.getPath());
        if (caseFromFile != null) {
            addInfo(testCase, caseFromFile);
        }
    }

    private void addInfo(TestCase testCase, TestCase sourceCase) {
        testCase.setStatus(sourceCase.getStatus());
        testCase.setStart(sourceCase.getStart());
        testCase.setEnd(sourceCase.getEnd());
        testCase.setMessage(sourceCase.getMessage());
    }


    private void processResultsFileXml(File resultsFile, final List<TestCase> testCases) throws Exception {
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
                    else if (qName.equals("running")) {
                        if (currentTestCase != null) {
                            currentTestCase.setStatus(TestCase.Status.InProgress);
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

    private void processResultsFileXml(File resultsFile, final TestCase testCase) throws Exception {
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
                    else if (qName.equals("running")) {
                        if (currentTestCase != null) {
                            currentTestCase.setStatus(TestCase.Status.InProgress);
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
        return getMainResultsDir(); // call through to static method
    }

    public static File getMainResultsDir() throws IOException {
        File resultsDir = null;
        String resultsLoc = PropertyManager.getProperty(PropertyNames.MDW_TEST_RESULTS_LOCATION);
        if (resultsLoc == null) {
            String gitLocalPath = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
            if (gitLocalPath != null)
                resultsLoc = gitLocalPath + "/testResults";
            else {
                File assetRoot = ApplicationContext.getAssetRoot();
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

    public JSONObject getTestResultsJson() throws ServiceException, JSONException {
        try {
            File file = getTestResultsFile(Asset.getFileExtension(Asset.TEST).substring(1));
            if (!file.isFile())
                throw new ServiceException(ServiceException.NOT_FOUND, "Results file not found: " + file);
            else if (!file.getName().endsWith(".json"))
                throw new ServiceException(ServiceException.NOT_IMPLEMENTED, "Results file must be JSON: " + file);
            return new JSONObject(new String(FileHelper.read(file)));
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    public File getTestResultsFile(String format) throws IOException {
        File resultsDir = getTestResultsDir();
        if (resultsDir == null)
            return null;
        String summaryFile = null;
        if (format == null || Asset.getFileExtension(Asset.TEST).equals("." + format)) {
            summaryFile = PropertyManager.getProperty(PropertyNames.MDW_FUNCTION_TESTS_SUMMARY_FILE);
            if (summaryFile == null) {
                summaryFile = "mdw-function-test-results.json";
                // fall back to old XML results
                if (!new File(resultsDir + "/" + summaryFile).exists() && new File(resultsDir + "/mdw-function-test-results.xml").exists())
                    summaryFile = "mdw-function-test-results.xml";
            }
        }
        else if (Asset.getFileExtension(Asset.FEATURE).equals("." + format)) {
            summaryFile = PropertyManager.getProperty(PropertyNames.MDW_FEATURE_TESTS_SUMMARY_FILE);
            if (summaryFile == null) {
                summaryFile = "mdw-cucumber-test-results.json";
                // fall back to old XML results
                if (!new File(resultsDir + "/" + summaryFile).exists() && new File(resultsDir + "/mdw-cucumber-test-results.xml").exists())
                    summaryFile = "mdw-cucumber-test-results.xml";
            }
        }

        return summaryFile == null ? null : new File(resultsDir + "/" + summaryFile);
    }

    public void executeCase(TestCase testCase, String user, TestExecConfig config) throws ServiceException, IOException {
        TestCaseList testCaseList = getTestCaseList(testCase);
        executeCases(testCaseList, user, config);
    }

    private static TestRunner testRunner;
    public void executeCases(TestCaseList testCaseList, String user, TestExecConfig config) throws ServiceException, IOException {
        for (TestCase testCase : testCaseList.getTestCases()) {
            if (testCase.getName().endsWith(Asset.getFileExtension(Asset.FEATURE)))
                throw new ServiceException(ServiceException.BAD_REQUEST, "Cucumber test cases currently not supported: " + testCase.getPath());
        }
        if (testRunner == null) {
            testRunner = new TestRunner();
        }
        else if (testRunner.isRunning()) {
             throw new ServiceException(ServiceException.FORBIDDEN, "Automated tests already running");
        }

        testRunner.init(testCaseList, user, getTestResultsFile(null), config);
        new Thread(testRunner).start();
    }

    public void cancelTestExecution(String user) throws ServiceException {
        if (testRunner == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Automated tests not running");
        testRunner.terminate();
        logger.info("Test execution canceled by: " + user);
    }

    private static TestExecConfig testExecConfig = new TestExecConfig(); // default options
    public TestExecConfig getTestExecConfig() { return testExecConfig; }
    public void setTestExecConfig(TestExecConfig config) { testExecConfig = config; }
}
