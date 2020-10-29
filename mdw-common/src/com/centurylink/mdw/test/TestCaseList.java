package com.centurylink.mdw.test;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.util.DateHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TestCaseList implements Jsonable {

    private String suite;
    public String getSuite() { return suite; }
    public void setSuite(String suite) { this.suite = suite; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    private List<PackageTests> packageTests;
    public List<PackageTests> getPackageTests() { return packageTests; }
    public void setPackageTests(List<PackageTests> packageTests) { this.packageTests = packageTests; }

    public PackageTests getPackageTests(String packageName) {
        if (packageTests != null) {
            for (PackageTests pkgTests : packageTests) {
                if (pkgTests.getPackageName().equals(packageName))
                    return pkgTests;
            }
        }
        return null;
    }

    public void addPackageTests(PackageTests pkgTests) {
        if (packageTests == null) {
            packageTests = new ArrayList<>();
        }
        packageTests.add(pkgTests);
    }

    public List<TestCase> getTestCases() {
        List<TestCase> testCases = new ArrayList<>();
        for (PackageTests pkgTestCases : getPackageTests()) {
            testCases.addAll(pkgTestCases.getTestCases());
        }
        return testCases;
    }

    public TestCase getTestCase(String path) {
        for (PackageTests pkgTests : packageTests) {
            if (path.startsWith(pkgTests.getPackageName() + "/")) {
                for (TestCase pkgTest : pkgTests.getTestCases()) {
                    if (pkgTest.getPath().equals(path))
                        return pkgTest;
                }
            }
        }
        return null;
    }

    public TestCase addTestCase(TestCase testCase) {
        for (PackageTests pkgTests : packageTests) {
            if (testCase.getPath().startsWith(pkgTests.getPackageName() + "/")) {
                pkgTests.getTestCases().add(testCase);
                return testCase;
            }
        }
        return null;
    }

    private File assetRoot;

    public TestCaseList(File assetRoot) {
        this.assetRoot = assetRoot;
    }

    public TestCaseList(JSONObject json) throws JSONException {
        if (json.has("suite"))
            this.suite = json.getString("suite");
        if (json.has("assetRoot"))
            this.assetRoot = new File(json.getString("assetRoot"));
        if (json.has("retrieveDate"))
            this.retrieveDate = DateHelper.serviceStringToDate(json.getString("retrieveDate"));
        if (json.has("count"))
            this.count = json.getInt("count");
        if (json.has("packages")) {
            JSONArray pkgsArr = json.getJSONArray("packages");
            this.packageTests = new ArrayList<>();
            for (int i = 0; i < pkgsArr.length(); i++) {
                this.packageTests.add(new PackageTests(pkgsArr.getJSONObject(i)));
            }
        }
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (suite != null)
            json.put("suite", suite);
        if (assetRoot != null)
            json.put("assetRoot", assetRoot);
        json.put("retrieveDate", DateHelper.serviceDateToString(getRetrieveDate()));
        json.put("count", count);
        JSONArray array = new JSONArray();
        if (packageTests != null) {
            for (PackageTests pkgTests : packageTests)
                array.put(pkgTests.getJson());
        }
        json.put("packages", array);
        return json;
    }

    public String getJsonName() {
        return "AutomatedTests";
    }

    public void sort() {
        Collections.sort(getPackageTests());
        for (PackageTests pkgTests : getPackageTests()) {
            if (pkgTests.getTestCases() != null)
                Collections.sort(pkgTests.getTestCases());
        }
    }

}
