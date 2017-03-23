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
package com.centurylink.mdw.plugin.ant;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer.Format;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.centurylink.mdw.ant.taskdef.AutoTestReport;
import com.centurylink.mdw.ant.taskdef.AutoTestTransformer;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class TestResultsFormatter {
    private WorkflowProject project;

    public TestResultsFormatter(WorkflowProject project) {
        this.project = project;
    }

    public void formatFunctionTestResults() throws IOException, PartInitException {
        File resultsDir = project.getFunctionTestResultsDir();
        File resultsXml = project.getFunctionTestResultsFile();
        File resultsHtml = new File(resultsDir + "/mdw-function-test-results.html");
        File stylesheet = getXslFile("function-test-results.xsl");
        formatResults(resultsDir, resultsXml, resultsHtml, stylesheet);
    }

    public void formatLoadTestResults() throws IOException, PartInitException {
        File resultsDir = project.getLoadTestResultsDir();
        File resultsXml = project.getLoadTestResultsFile();
        File resultsHtml = new File(resultsDir + "/mdw-load-test-results.html");
        File stylesheet = getXslFile("load-test-results.xsl");
        formatResults(resultsDir, resultsXml, resultsHtml, stylesheet);
    }

    private File getXslFile(String filename) throws IOException {
        File stylesheet = null;
        if (project.isFilePersist()) {
            // prefer override (non-MDW testing package); fall back to MDW
            // testing package
            WorkflowPackage mdwTestingPackage = null;
            WorkflowPackage otherTestingPackage = null;
            for (WorkflowPackage pkg : project.getTopLevelPackages()) {
                if (pkg.getName().equals("com.centurylink.mdw.testing"))
                    mdwTestingPackage = pkg;
                else if (pkg.getName().endsWith(".testing"))
                    otherTestingPackage = pkg;
            }
            if (otherTestingPackage != null) {
                WorkflowAsset asset = otherTestingPackage.getAsset(filename);
                if (asset != null)
                    stylesheet = asset.getRawFile();
            }
            if (stylesheet == null && mdwTestingPackage != null) {
                WorkflowAsset asset = mdwTestingPackage.getAsset(filename);
                if (asset != null)
                    stylesheet = asset.getRawFile();
            }
        }

        if (stylesheet == null) {
            // fall back to template version in designer
            URL localUrl = PluginUtil.getLocalResourceUrl("templates/xsl/" + filename);
            try {
                stylesheet = new File(new URI(localUrl.toString()));
            }
            catch (URISyntaxException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }
        return stylesheet;
    }

    private void formatResults(File resultsDir, File resultsXml, File resultsHtml, File stylesheet)
            throws IOException, PartInitException {
        File htmlFile = new File(resultsDir + "/junit-noframes.html");

        AutoTestReport reportTask = new AutoTestReport();
        AntBuilder antBuilder = new AntBuilder(reportTask);
        reportTask.setTestOutput(new String(PluginUtil.readFile(resultsXml)));
        reportTask.setTodir(resultsDir);
        AutoTestTransformer transformer = (AutoTestTransformer) reportTask.createReport();
        transformer.setXsl(new String(PluginUtil.readFile(stylesheet)));
        transformer.setTodir(resultsDir);
        Format format = new Format();
        format.setValue("noframes");
        transformer.setFormat(format);

        antBuilder.getAntProject().setBaseDir(resultsDir);
        antBuilder.executeTask();

        if (htmlFile.exists()) {
            // TODO support specifying output file name
            if (resultsHtml.exists()) {
                if (!resultsHtml.delete())
                    throw new IOException("Failed to delete old: " + resultsHtml);
            }
            if (!htmlFile.renameTo(resultsHtml))
                throw new IOException("Failed to rename: " + htmlFile);

            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browser = support.createBrowser("org.eclipse.ui.browser.view");
            browser.openURL(new URL("file://" + resultsHtml));
        }
        else {
            throw new IOException("HTML output file not found: " + htmlFile);
        }
    }

}
