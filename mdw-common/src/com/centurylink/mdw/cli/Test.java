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
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames="test", commandDescription="Run Automated Test(s)", separators="=")
public class Test extends Setup {

    @Parameter(names="--include", description="Test case include(s)")
    private String include = "**/*.test,**/*.postman";
    public String getInclude() { return include; }
    public void setInclude(String include) { this.include = include; }

    @Parameter(names="--exclude", description="Test case exclude(s)")
    private String exclude;
    public String getExclude() { return exclude; }
    public void setExclude(String exclude) { this.exclude = exclude; }

    @Parameter(names="--threads", description="Thread pool size")
    private int threads = 5; // thread pool size
    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }

    @Parameter(names="--interval", description="Run interval in seconds")
    private int interval = 2; // seconds
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    @Parameter(names="--stubbing", description="Use stubbing")
    private boolean stubbing;
    public boolean isStubbing() { return stubbing; }
    public void setStubbing(boolean stubbing) { this.stubbing = stubbing; }

    @Parameter(names="--stubPort", description="Stub port")
    private int stubPort;
    public int getStubPort() { return stubPort; }
    public void setStubPort(int port) { this.stubPort = port; }

    private boolean running;

    @Override
    public Operation run(ProgressMonitor... progressMonitors) throws IOException {
        List<File> caseFiles = findCaseFiles();
        if (caseFiles.isEmpty()) {
            System.out.println("No tests found");
            return this;
        }

        if (isDebug()) {
            System.out.println("Test Cases:");
            for (File caseFile : caseFiles)
                System.out.println("  " + caseFile);
        }

        String path = "/com/centurylink/mdw/testing/AutomatedTests";
        JSONObject configRequest = getConfigRequest();
        new Fetch(new URL(getServicesUrl() + path + "/config")).put(configRequest.toString(2));

        // TODO threading
        // runTests(caseFiles);
        JSONObject caseRequest = getCaseRequest(caseFiles);
        new Fetch(new URL(getServicesUrl() + path + "/exec")).post(caseRequest.toString(2));
        return this;
    }

    protected List<File> findCaseFiles() throws IOException {
        List<File> caseFiles = new ArrayList<>();
        List<PathMatcher> inMatchers = getIncludeMatchers();
        List<PathMatcher> exMatchers = getExcludeMatchers();
        Files.walkFileTree(Paths.get(getProjectDir().getPath()), new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (matches(inMatchers, path) && !matches(exMatchers, path)) {
                    caseFiles.add(path.toFile());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return caseFiles;
    }

    List<PathMatcher> getIncludeMatchers() {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String in : include.trim().split("\\s*,\\s*")) {
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + in));
        }
        return matchers;
    }

    List<PathMatcher> getExcludeMatchers() {
        List<PathMatcher> matchers = new ArrayList<>();
        if (exclude != null) {
            for (String ex : exclude.trim().split("\\s*,\\s*")) {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + ex));
            }
        }
        return matchers;
    }

    private boolean matches(List<PathMatcher> matchers, Path path) {
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path))
                return true;
        }
        return false;
    }

    private JSONObject getCaseRequest(List<File> caseFiles) throws IOException {
        Map<String,List<String>> pkgTests = new HashMap<>();
        for (File caseFile : caseFiles) {
            String assetPath = getAssetPath(caseFile);
            String pkg = getPackageName(assetPath);
            List<String> assets = pkgTests.get(pkg);
            if (assets == null) {
                assets = new ArrayList<>();
                pkgTests.put(pkg, assets);
            }
            assets.add(getAssetName(assetPath));
        }
        JSONObject json = new JSONObject();
        JSONArray pkgs = new JSONArray();
        json.put("packages", pkgs);
        if (!pkgTests.isEmpty()) {
            for (String pkg : pkgTests.keySet()) {
                JSONObject pkgObj = new JSONObject();
                pkgObj.put("name", pkg);
                pkgs.put(pkgObj);
                JSONArray tcs = new JSONArray();
                List<String> caseNames = pkgTests.get(pkg);
                for (String caseName : caseNames) {
                    JSONObject tcObj = new JSONObject();
                    tcObj.put("name", caseName);
                    tcs.put(tcObj);
                }
                pkgObj.put("testCases", tcs);
            }
        }
        return json;
    }

    private JSONObject getConfigRequest() throws IOException {
        JSONObject json = new JSONObject();
        json.put("threads", threads);
        json.put("interval", interval);
        if (stubbing) {
            json.put("stubbing", stubbing);
            if (stubPort > 0)
                json.put("stubPort", stubPort);
        }
        return json;
    }
}
