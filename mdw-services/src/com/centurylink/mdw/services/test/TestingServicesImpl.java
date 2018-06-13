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
package com.centurylink.mdw.services.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.asset.PackageAssets;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.services.asset.AssetServicesImpl;
import com.centurylink.mdw.test.PackageTests;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseItem;
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
        return getTestCases(new String[] { Asset.getFileExtension(Asset.TEST).substring(1),
                Asset.getFileExtension(Asset.POSTMAN).substring(1) });
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

    public TestCaseList getTestCases(String[] formats) throws ServiceException {
        TestCaseList testCaseList = new TestCaseList(assetServices.getAssetRoot());
        testCaseList.setPackageTests(new ArrayList<PackageTests>());
        List<TestCase> allTests = new ArrayList<TestCase>();
        Map<String,List<AssetInfo>> pkgAssets = assetServices.getAssetsOfTypes(formats);
        for (String pkgName : pkgAssets.keySet()) {
            List<AssetInfo> assets = pkgAssets.get(pkgName);
            PackageTests pkgTests = new PackageTests(assetServices.getPackage(pkgName));
            pkgTests.setTestCases(new ArrayList<TestCase>());
            for (AssetInfo asset : assets) {
                TestCase testCase = new TestCase(pkgName, asset);
                if (testCase.getAsset().isFormat(Asset.POSTMAN)) {
                    try {
                        String json = new String(FileHelper.read(testCase.file()));
                        JSONObject coll = new JSONObject(json);
                        if (coll.has("item")) {
                            JSONArray items = coll.getJSONArray("item");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                TestCaseItem testCaseItem = new TestCaseItem(item.getString("name"));
                                if (item.has("request")) {
                                    JSONObject request = item.getJSONObject("request");
                                    JSONObject req = new JSONObject();
                                    if (request.has("method"))
                                        req.put("method", request.getString("method"));
                                    testCaseItem.getObject().put("request", req);
                                }
                                testCase.addItem(testCaseItem);
                            }
                        }
                    }
                    catch (Exception ex) {
                        logger.severeException(ex.getMessage(), ex);
                    }
                }
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
            return testCase;
        }
        catch (Exception ex) {
            throw new ServiceException("IO Error reading test case: " + path, ex);
        }
    }

    public TestCaseItem getTestCaseItem(String path) throws ServiceException {
        try {
            String assetPath = path.substring(0, path.lastIndexOf('/'));
            String pkg = assetPath.substring(0, assetPath.lastIndexOf('/'));
            String itemName = path.substring(path.lastIndexOf('/') + 1).replace('~', '/');
            String method = null;
            String meth = null;
            int colon = itemName.indexOf(':');
            if (colon > 0) {
                method = meth = itemName.substring(0, colon);
                itemName = itemName.substring(colon + 1);
                if (method.equals("DEL"))
                    method = "DELETE";
                else if (method.equals("OPT"))
                    method = "OPTIONS";
            }
            AssetInfo testCaseAsset = assetServices.getAsset(assetPath);
            TestCaseItem item = null;
            String json = new String(FileHelper.read(testCaseAsset.getFile()));
            JSONObject coll = new JSONObject(json);
            if (coll.has("item")) {
                JSONArray items = coll.getJSONArray("item");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject itemObj = items.getJSONObject(i);
                    String itemObjName = itemObj.getString("name");
                    if (itemName.equals(itemObjName)) {
                        if (method == null) {
                            item = new TestCaseItem(itemName);
                        }
                        else {
                            if (itemObj.has("request")) {
                                JSONObject request = itemObj.getJSONObject("request");
                                if (request.has("method") && request.getString("method").equals(method))
                                    item = new TestCaseItem(itemName);
                            }
                        }
                        if (item != null) {
                            item.setObject(itemObj);
                            break;
                        }
                    }
                }
            }
            if (item != null) {
                PackageAssets pkgAssets = assetServices.getAssets(pkg);
                String yamlExt = Asset.getFileExtension(Asset.YAML);
                File resultsDir = getTestResultsDir();
                String rootName = item.getName().replace('/', '_');
                if (meth != null)
                    rootName = meth + '_' + rootName;
                for (AssetInfo pkgAsset : pkgAssets.getAssets()) {
                    if (pkgAsset.getName().endsWith(yamlExt) && pkgAsset.getRootName().equals(rootName)) {
                        item.setExpected(pkg + "/" + pkgAsset.getName());
                        if (resultsDir != null) {
                            if (new File(resultsDir + "/" + pkg + "/" + pkgAsset.getName()).isFile())
                                item.setActual(pkg + "/" + pkgAsset.getName());
                        }
                    }
                }
                if (new File(resultsDir + "/" + pkg + "/" + rootName + ".log").isFile())
                    item.setExecuteLog(pkg + "/" + rootName + ".log");
            }

            if (item != null)
                addStatusInfo(new TestCase(pkg, testCaseAsset));
            return item;
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
                File resultsFile = getTestResultsFile(testCases.get(0).getAsset().getExtension());
                if (resultsFile != null && resultsFile.isFile()) {
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
                processResultsFile(resultsFile, testCase);
            }
        }
        catch (Exception ex) {
            logger.severeException("Unable to get status info for testCase: " + testCase.getName(), ex);
        }
    }

    private TestCase readTestCase(String path) throws ServiceException, IOException {
        String pkg = path.substring(0, path.lastIndexOf('/'));
        AssetInfo testCaseAsset = assetServices.getAsset(path, false);
        String rootName = testCaseAsset.getRootName();
        TestCase testCase = new TestCase(pkg, testCaseAsset);
        PackageAssets pkgAssets = assetServices.getAssets(pkg, false);
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
        TestCaseList testCaseList = new TestCaseList(ApplicationContext.getAssetRoot(), new JsonObject(jsonString));
        for (TestCase testCase : testCases) {
            TestCase caseFromFile = testCaseList.getTestCase(testCase.getPath());
            if (caseFromFile != null) {
                addInfo(testCase, caseFromFile);
            }
        }
    }

    private void processResultsFile(File resultsFile, final TestCase testCase) throws Exception {
        String jsonString = new String(Files.readAllBytes(resultsFile.toPath()));
        TestCaseList testCaseList = new TestCaseList(ApplicationContext.getAssetRoot(), new JsonObject(jsonString));
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
        if (testCase.getItems() != null) {
            for (TestCaseItem item : testCase.getItems()) {
                TestCaseItem sourceItem = sourceCase.getItem(item.getName());
                if (item.getObject().has("request")) {
                    JSONObject request = item.getObject().getJSONObject("request");
                    if (request.has("method"))
                        sourceItem = sourceCase.getItem(item.getName(), request.getString("method"));
                }
                if (sourceItem != null) {
                    item.setStatus(sourceItem.getStatus());
                    item.setStart(sourceItem.getStart());
                    item.setEnd(sourceItem.getEnd());
                    item.setMessage(sourceItem.getMessage());
                }
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
            return new JsonObject(new String(FileHelper.read(file)));
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
        if (format == null || Asset.getFileExtension(Asset.TEST).equals("." + format) || Asset.getFileExtension(Asset.POSTMAN).equals("." + format)) {
            summaryFile = PropertyManager.getProperty(PropertyNames.MDW_TEST_SUMMARY_FILE);
            if (summaryFile == null)
                summaryFile = "mdw-function-test-results.json";
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

    private static TestExecConfig testExecConfig;
    public TestExecConfig getTestExecConfig() throws ServiceException {
        if (testExecConfig == null) {
            try {
                File configFile = getTestConfigFile();
                if (configFile != null && configFile.isFile())
                    testExecConfig = new TestExecConfig(new JSONObject(new String(Files.readAllBytes(Paths.get(configFile.getPath())))));
                else
                    testExecConfig = new TestExecConfig();
            }
            catch (IOException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
            }
        }
        return testExecConfig;
    }
    public void setTestExecConfig(TestExecConfig config) throws ServiceException {
        testExecConfig = config;
        try {
            File configFile = getTestConfigFile();
            if (configFile != null) {
                Files.write(Paths.get(configFile.getPath()), config.getJson().toString(2).getBytes());
            }
        }
        catch (IOException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
        }
    }

    private File getTestConfigFile() throws IOException {
        File resultsDir = getTestResultsDir();
        if (resultsDir == null)
            return null;
        return new File(resultsDir + "/mdw-test-config.json");
    }
}
