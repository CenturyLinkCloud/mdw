/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class LegacyExpectedResults extends WorkflowElement
        implements Comparable<LegacyExpectedResults> {
    private AutomatedTestCase testCase;

    public AutomatedTestCase getTestCase() {
        return testCase;
    }

    private File expectedResultFile;

    public File getExpectedResultFile() {
        return expectedResultFile;
    }

    public LegacyExpectedResults(AutomatedTestCase testCase, File expectedResultFile) {
        this.testCase = testCase;
        this.expectedResultFile = expectedResultFile;
    }

    @Override
    public WorkflowProject getProject() {
        return testCase.getProject();
    }

    public String getTitle() {
        return "Expected Result";
    }

    public String getName() {
        return getExpectedResultFileName().substring(2, getExpectedResultFileName().length() - 4);
    }

    public File getActualResultFile() {
        return getActualResult().getLocation().toFile();
    }

    public String getExpectedResultFileName() {
        return expectedResultFile.getName();
    }

    public String getActualResultFileName() {
        return getExpectedResultFileName().replaceFirst("E_", "R_");
    }

    public IFile getExpectedResult() {
        return getProject().getOldTestCasesFolder().getFolder(testCase.getName())
                .getFile(getExpectedResultFileName());
    }

    public IFile getActualResult() {
        try {
            testCase.getActualResultsFolder().refreshLocal(1, new NullProgressMonitor());
        }
        catch (CoreException ex) {
            PluginMessages.uiError(ex, "Actual Results", getProject());
        }
        return testCase.getActualResultsFolder().getFile(getActualResultFileName());
    }

    @Override
    public String getIcon() {
        IFile actual = getActualResult();
        if (actual != null && actual.exists())
            return "result_with.gif";
        else
            return "result.gif";
    }

    public Long getId() {
        return new Long(-1);
    }

    public boolean hasInstanceInfo() {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    public boolean isLocal() {
        return true;
    }

    public Entity getActionEntity() {
        return Entity.TestCase;
    }

    public boolean equals(Object o) {
        if (!(o instanceof LegacyExpectedResults) || o == null)
            return false;
        LegacyExpectedResults other = (LegacyExpectedResults) o;

        if (!getProject().equals(other.getProject()))
            return false;
        if (!testCase.equals(other.getTestCase()))
            return false;

        return getName().equals(other.getName());
    }

    public int compareTo(LegacyExpectedResults other) {
        return this.getName().compareTo(other.getName());
    }
}
