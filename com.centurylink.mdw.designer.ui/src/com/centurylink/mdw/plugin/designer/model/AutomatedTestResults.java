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
package com.centurylink.mdw.plugin.designer.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.plugin.PluginUtil;

/**
 * For old-style results, see LegacyExpectedResults.
 */
public class AutomatedTestResults extends Yaml {
    public AutomatedTestResults() {
        super();
    }

    public AutomatedTestResults(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public AutomatedTestResults(AutomatedTestResults cloneFrom) {
        super(cloneFrom);
    }

    private boolean actual;

    public boolean isActual() {
        return actual;
    }

    public void setActual(boolean actual) {
        this.actual = actual;
    }

    public File getActualResults() {
        return new File(getProject().getTestResultsDir(AutomatedTestCase.FUNCTION_TEST) + "/"
                + getPackage().getName() + "/" + getName());
    }

    /**
     * Parses the main process instance ID from the YAML results file. Returns
     * null if the actual results file does not exist.
     */
    public Long getActualProcessInstanceId() throws IOException {
        File actualResults = getActualResults();
        if (!actualResults.exists())
            return null;
        String yaml = new String(PluginUtil.readFile(actualResults));
        String id = yaml.substring(yaml.indexOf('#') + 1, yaml.indexOf('\n')).trim();
        return Long.parseLong(id);
    }

    /**
     * Relies on the convention that the test case has the same name and is in
     * the same package as the expected results.
     */
    public AutomatedTestCase getTestCase() {
        String baseName = getPackage().getName() + "/"
                + getName().substring(0, getName().lastIndexOf('.'));
        AutomatedTestCase testCase = (AutomatedTestCase) getProject()
                .getAsset(baseName + RuleSetVO.getFileExtension(RuleSetVO.TEST));
        if (testCase == null)
            testCase = (AutomatedTestCase) getProject()
                    .getAsset(baseName + RuleSetVO.getFileExtension(RuleSetVO.FEATURE));
        return testCase;
    }

    @Override
    public String getTitle() {
        if (isActual())
            return "Test Results";
        else
            return "Expected Results";
    }

    @Override
    public String getIcon() {
        if (isActual())
            return "result.gif";

        if (getActualResults().exists())
            return "result_with.gif";
        else
            return "result.gif";
    }

    @Override
    public String getDefaultExtension() {
        return RuleSetVO.getFileExtension(RuleSetVO.YAML);
    }

    private static List<String> testCaseLanguages;

    @Override
    public List<String> getLanguages() {
        if (testCaseLanguages == null) {
            testCaseLanguages = new ArrayList<String>();
            testCaseLanguages.add(RuleSetVO.YAML);
        }
        return testCaseLanguages;
    }
}
