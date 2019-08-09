package com.centurylink.mdw.testing;

import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.test.PackageTests;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.File;
import java.util.*;

/**
 * If enabled, run daily at 1:30 am.
 */
@ScheduledJob(value="RandomTests", schedule="30 1 * * ? *", enabled="${props['mdw.random.tests.enabled']}")
public class RandomTests implements com.centurylink.mdw.model.monitor.ScheduledJob {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static final String[] EXCLUDES = new String[] {
        "com.centurylink.mdw.tests.services/InflightHandling.test"
    };

    @Override
    public void run(CallURL args) {
        if (!PropertyManager.getBooleanProperty("mdw.random.tests.enabled", false)) {
            return;
        }

        logger.info("Running random tests...");

        try {
            File assetRoot = getAssetServices().getAssetRoot();
            TestCaseList testCaseList = new TestCaseList(getAssetServices().getAssetRoot());
            testCaseList.setPackageTests(new ArrayList<>());
            Map<String,List<String>> randomCases = getRandomCases(50);
            for (String pkg : randomCases.keySet()) {
                PackageDir pkgDir = new PackageDir(assetRoot, pkg);
                PackageTests pkgTests = new PackageTests(pkgDir);
                testCaseList.getPackageTests().add(pkgTests);
                List<TestCase> pkgCases = new ArrayList<>();
                pkgTests.setTestCases(pkgCases);
                for (String asset : randomCases.get(pkg)) {
                    pkgCases.add(getTestingServices().getTestCase(pkg + "/" + asset));
                }
            }

            TestExecConfig config = new TestExecConfig();
            config.setStubbing(true);
            getTestingServices().executeCases(testCaseList, "mdwapp", config);
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    /**
     * Returns map of package name to list of asset names.
     */
    protected Map<String, List<String>> getRandomCases(int count) throws ServiceException {
        List<String> allCases = new ArrayList<>();
        Map<String,List<AssetInfo>> testAssets = getAssetServices().getAssetsOfType("test");
        for (String pkg : testAssets.keySet()) {
            for (AssetInfo testAsset : testAssets.get(pkg)) {
                String path = pkg + "/" + testAsset.getName();
                if (Arrays.binarySearch(EXCLUDES, path) < 0) {
                    allCases.add(path);
                }
            }
        }

        Random random = new Random(); // NOSONAR
        List<Integer> randomIndexes = new ArrayList<>();
        while (randomIndexes.size() < count) {
            randomIndexes.add(random.nextInt(allCases.size()));
        }

        Map<String,List<String>> randomCases = new HashMap<>();
        for (int index : randomIndexes) {
            String randomCase = allCases.get(index);
            int lastSlash = randomCase.lastIndexOf('/');
            String pkg = randomCase.substring(0, lastSlash);
            String asset = randomCase.substring(lastSlash + 1);
            List<String> pkgCases = randomCases.get(pkg);
            if (pkgCases == null) {
                pkgCases = new ArrayList<>();
                randomCases.put(pkg, pkgCases);
            }
            pkgCases.add(asset);
        }
        return randomCases;
    }

    private AssetServices assetServices;
    public AssetServices getAssetServices() {
        if (assetServices == null)
            assetServices = ServiceLocator.getAssetServices();
        return assetServices;
    }

    private TestingServices testingServices;
    public TestingServices getTestingServices() {
        if (testingServices == null)
            testingServices = ServiceLocator.getTestingServices();
        return testingServices;
    }
}
