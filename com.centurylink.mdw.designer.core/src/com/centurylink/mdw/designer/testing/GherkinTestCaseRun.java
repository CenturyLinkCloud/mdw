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
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.test.CucumberTestMain;

public class GherkinTestCaseRun extends TestCaseRun {

    public static final String[] DEFAULT_ARGS = new String[] {"--plugin", "pretty", "--monochrome", "--strict"};

    private List<String> args;
    private Map<String,String> sysProps;

    private static final Object lock = new Object();

    /**
     * TODO: custom sysProps and args
     */
    public GherkinTestCaseRun(TestCase testCase, int run, String masterRequestId,
            DesignerDataAccess dao, LogMessageMonitor monitor, Map<String,ProcessVO> processCache,
            boolean oldNamespaces, File resultsDir, boolean stubbing, int stubPort)
    throws DataAccessException {
        super(testCase, run, masterRequestId, dao, monitor, processCache, false, true, oldNamespaces);

        sysProps = new HashMap<String,String>();
        sysProps.put("mdw.test.case", testCase.getCaseName());
        sysProps.put("mdw.test.case.user", dao.getCurrentServer().getUser());
        sysProps.put("mdw.test.server.url", dao.getCurrentServer().getServerUrl());
        if (getMasterRequestId() != null){
            sysProps.put("mdw.test.masterRequestId", this.getMasterRequestId());
        }
        if (stubbing) {
            sysProps.put("mdw.test.server.stub", "true");
            sysProps.put("mdw.test.server.stubPort", "" + stubPort);
        }
        if (oldNamespaces)
            sysProps.put("mdw.test.old.namespaces", "true");
        if (singleServer)
            sysProps.put("mdw.test.old.namespaces", "true");
        if (verbose)
            sysProps.put("mdw.test.verbose", "true");

        if (dao.isVcsPersist())
            sysProps.put("mdw.test.workflow.dir", dao.getCurrentServer().getRootDirectory().toString().replace('\\', '/'));
          else
            sysProps.put("mdw.test.jdbc.url", dao.getCurrentServer().getDatabaseUrl());

        sysProps.put("mdw.test.results.dir", resultsDir.toString());

        args = new ArrayList<String>();
        args.addAll(Arrays.asList(DEFAULT_ARGS));

        if (testCase.getCaseDirectory() != null) {
            // legacy
            String caseDir = testCase.getCaseDirectory().toString().replace('\\', '/');
            String casesDir = testCase.getCaseDirectory().getParentFile().toString().replace('\\', '/');

            sysProps.put("mdw.test.cases.dir", casesDir);

            args.add("--glue");
            args.add(casesDir + "/steps.groovy");
            args.add(caseDir);
        }
        else if (testCase.getRuleSet() != null) {
            throw new DataAccessException("Cucumber tests not supported for non-VCS assets");
        }
        else {
            // vcs asset-based case
            sysProps.put("mdw.test.case.file", getTestCase().getCaseFile().toString().replace('\\', '/'));

            File assetDir = dao.getCurrentServer().getRootDirectory();
            List<File> testingPkgDirs = new ArrayList<File>();
            addTestingPkgDirs(assetDir, testingPkgDirs);
            for (File testingPkgDir : testingPkgDirs) {
                args.add("--glue");
                args.add(testingPkgDir.toString());
            }
            args.add(testCase.getCaseFile().toString());
        }
    }

    private void addTestingPkgDirs(File rootDir, List<File> addTo) {
        for (File child : rootDir.listFiles()) {
            if (child.isDirectory()) {
                if (child.getName().equals("testing") &&
                        (new File(child + "/" + PackageDir.PACKAGE_JSON_PATH).exists() || new File(child + "/" + PackageDir.PACKAGE_XML_PATH).exists()))
                    addTo.add(child);
                addTestingPkgDirs(child, addTo);
            }
        }
    }

    public void run() {
        synchronized(lock) {
            try {
                CucumberTestMain main = new CucumberTestMain(args.toArray(new String[0]), sysProps);
                getTestCase().setStartDate(new Date());
                byte res = main.execute();
                getTestCase().setStatus(res == 0 ? TestCase.STATUS_PASS : TestCase.STATUS_FAIL);
                getTestCase().setEndDate(new Date());
            }
            catch (Throwable ex) {
                ex.printStackTrace();
                getTestCase().setStatus(TestCase.STATUS_ERROR);
            }
        }
    }
}
