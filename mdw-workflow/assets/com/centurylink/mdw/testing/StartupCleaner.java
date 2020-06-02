package com.centurylink.mdw.testing;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TestingServices;
import com.centurylink.mdw.services.test.TestRunner;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCase.Status;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Reset InProgress tests to Stopped on startup.
 */
@RegisteredService(StartupService.class)
public class StartupCleaner implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public void onStartup() throws StartupException {
        try {
            TestingServices testingServices = ServiceLocator.getTestingServices();
            File resultsFile = testingServices.getTestResultsFile();
            if (resultsFile != null && resultsFile.isFile()) {
                String jsonString = new String(Files.readAllBytes(resultsFile.toPath()));
                TestCaseList testCaseList = new TestCaseList(new JsonObject(jsonString));
                boolean modified = false;
                for (TestCase testCase : testCaseList.getTestCases()) {
                    if (testCase.getStatus() == Status.Waiting || testCase.getStatus() == Status.InProgress) {
                        testCase.setStatus(Status.Stopped);
                        modified = true;
                    }
                }
                if (modified) {
                    Files.write(resultsFile.toPath(), testCaseList.getJson().toString(2).getBytes(),
                            StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }

        TestRunner testRunner = new TestRunner();
        testRunner.terminate();

    }

    @Override
    public void onShutdown() {
    }
}
