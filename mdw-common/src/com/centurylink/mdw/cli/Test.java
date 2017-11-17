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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;;

@Parameters(commandNames="test", commandDescription="Run Automated Test(s)", separators="=")
public class Test extends Setup {
    private static final String PATH = "/com/centurylink/mdw/testing/AutomatedTests";

    @Parameter(names="--include", description="Test case include(s).  Comma-delimited glob patterns (relative to asset root).")
    private String include = "**/*.test,**/*.postman";
    public String getInclude() { return include; }
    public void setInclude(String include) { this.include = include; }

    @Parameter(names="--exclude", description="Test case exclude(s).  Comma-delimited glob patterns (relative to asset root).")
    private String exclude;
    public String getExclude() { return exclude; }
    public void setExclude(String exclude) { this.exclude = exclude; }

    @Parameter(names="--ignore", description="Exit code ignores failure for these tests.  Comma-delimited glob patterns (relative to asset root).")
    private String ignore;
    public String getIgnore() { return ignore; }
    public void setIgnore(String ignore) { this.ignore = ignore; }

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

    @Parameter(names="--json", description="Print JSON summary to stdout")
    private boolean json;
    public boolean isJson() { return json; }
    public void setJson(boolean json) { this.json = json; }

    private boolean success = true;
    public boolean isSuccess() { return success; }

    /**
     * TODO: Option for websocket updates instead of polling.
     */
    @Override
    public Operation run(ProgressMonitor... progressMonitors) throws IOException {
        if (!json)
            System.out.println("Finding tests...");
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

        JSONObject configRequest = getConfigRequest();
        new Fetch(new URL(getServicesUrl() + PATH + "/config")).put(configRequest.toString(2));

        Map<String,List<String>> pkgTests = new HashMap<>();
        Map<String,String> statuses = new HashMap<>();
        for (File caseFile : caseFiles) {
            String assetPath = caseFile.getParent().replace('\\', '/').replace('/', '.') + '/' + caseFile.getName();
            String pkg = getPackageName(assetPath);
            List<String> assets = pkgTests.get(pkg);
            if (assets == null) {
                assets = new ArrayList<>();
                pkgTests.put(pkg, assets);
            }
            String name = getAssetName(assetPath);
            assets.add(name);
            statuses.put(pkg + "/" + name, null);
        }

        JSONObject caseRequest = getCaseRequest(pkgTests);
        boolean done = false;
        new Fetch(new URL(getServicesUrl() + PATH + "/exec")).post(caseRequest.toString(2));

        if (!json)
            System.out.println("Running tests...");

        long before = System.currentTimeMillis();
        File resultsFile = getResultsSummaryFile();
        Path summaryPath = Paths.get(resultsFile.getParentFile().getPath());
        WatchService watcher = FileSystems.getDefault().newWatchService();
        summaryPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);

        while (!done) {
            try {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (((Path)event.context()).getFileName().toString().equals(resultsFile.getName())) {
                        String contents = new String(Files.readAllBytes(Paths.get(resultsFile.getPath())));
                        if (contents.isEmpty())
                            continue; // why does this happen?
                        JSONObject jsonObj = new JSONObject(contents);
                        JSONArray pkgs = jsonObj.getJSONArray("packages");
                        for (int i = 0; i < pkgs.length(); i++) {
                            JSONObject pkg = pkgs.getJSONObject(i);
                            JSONArray tests = pkg.getJSONArray("testCases");
                            for (int j = 0; j < tests.length(); j++) {
                                JSONObject test = tests.getJSONObject(j);
                                String asset = pkg.getString("name") + "/" + test.getString("name");
                                if (statuses.containsKey(asset) && test.has("status")) {
                                    String status = test.getString("status");
                                    String oldStatus = statuses.get(asset);
                                    if (oldStatus == null || !oldStatus.equals(status)) {
                                        statuses.put(asset, status);
                                        if (isFinished(status)) {
                                            if (!json) {
                                                int finished = finished(statuses);
                                                if (isSuccess(status))
                                                    System.out.print("   ");
                                                else
                                                    System.out.print("  *");
                                                System.out.println(status + " (" + finished + "/" + statuses.size() + ") - " + asset);
                                            }
                                            if (!isSuccess(status)) {
                                                boolean ignoreFailure = false;
                                                if (ignore != null) {
                                                    int lastSlash = asset.lastIndexOf('/');
                                                    Path assetPath = Paths.get(new File(asset.substring(0, lastSlash).replace('.', '/') + asset.substring(lastSlash)).getPath());
                                                    if (matches(getIgnoreMatchers(), assetPath))
                                                        ignoreFailure = true;
                                                }
                                                if (!ignoreFailure)
                                                    this.success = false;
                                                if (test.has("message") && !json)
                                                    System.out.println("    (" + test.getString("message"));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        done = finished(statuses) == statuses.size();
                        if (done) {
                            printSummary(statuses, System.currentTimeMillis() - before);
                            continue;
                        }
                    }
                }
                key.reset();
            }
            catch (InterruptedException ex) {
                System.err.println("Test execution canceled");
                done = true;
            }
        }
        return this;
    }

    private int finished(Map<String,String> statuses) {
        int finished = 0;
        for (String test : statuses.keySet()) {
            if (isFinished(statuses.get(test))) {
                finished++;
            }
        }
        return finished;
    }
    protected void printSummary(Map<String,String> statuses, long elapsed) {
        Map<String,Integer> counts = new HashMap<>();
        for (String status : statuses.values()) {
            Integer count = counts.get(status);
            if (count == null) {
                counts.put(status, 1);
            }
            else {
                counts.put(status, ++count);
            }
        }
        if (json) {
            JSONObject json = new JSONObject();
            json.put("URL", getServicesUrl() + PATH);
            json.put("Total", statuses.size());
            json.put("Time", elapsed);
            for (String status : counts.keySet()) {
                json.put(status, counts.get(status));
            }
            System.out.println(json.toString(2));
        }
        else {
            System.out.println("\nSummary (" + getServicesUrl() + PATH + "):");
            for (String status : counts.keySet()) {
                System.out.print("  " + status + ": ");
                for (int i = status.length(); i < 16; i++)
                    System.out.print(" ");
                System.out.println(counts.get(status));
            }
            System.out.println("  Total:            " + statuses.size() + " (in " + elapsed + " ms)");
        }
    }

    protected List<File> findCaseFiles() throws IOException {
        List<File> caseFiles = new ArrayList<>();
        List<PathMatcher> inMatchers = getIncludeMatchers();
        List<PathMatcher> exMatchers = getExcludeMatchers();
        Files.walkFileTree(Paths.get(getAssetRoot().getPath()),
                EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        Path p = Paths.get(getAssetPath(path.toFile()));
                        if (matches(inMatchers, p) && !matches(exMatchers, p)) {
                            caseFiles.add(p.toFile());
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

    List<PathMatcher> getIgnoreMatchers() {
        List<PathMatcher> matchers = new ArrayList<>();
        if (ignore != null) {
            for (String ig : ignore.trim().split("\\s*,\\s*")) {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + ig));
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

    private JSONObject getCaseRequest(Map<String,List<String>> pkgTests) throws IOException {
        JSONObject json = new JSONObject() {
            @Override // ordered keys makes the tests run in sequence
            public Set<String> keySet() {
                return new TreeSet<String>(super.keySet());
            }
        };
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
                    if (caseName.endsWith(".postman")) {
                        JSONArray items = new JSONArray();
                        // add items
                        File caseFile = new File(getAssetRoot() + "/" + pkg.replace('.', '/') + "/" + caseName);
                        String contents = new String(Files.readAllBytes(Paths.get(caseFile.getPath())));
                        JSONObject postman = new JSONObject(contents);
                        JSONArray postmanItems = postman.getJSONArray("item");
                        for (int i = 0; i < postmanItems.length(); i++) {
                            JSONObject postmanItem = postmanItems.getJSONObject(i);
                            JSONObject obj = new JSONObject();
                            obj.put("name", postmanItem.getString("name"));
                            JSONObject req = new JSONObject();
                            req.put("method", postmanItem.getJSONObject("request").getString("method"));
                            obj.put("request", req);
                            JSONObject item = new JSONObject();
                            item.put("object", obj);
                            items.put(item);
                        }
                        tcObj.put("items", items);
                    }
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

    private boolean isFinished(String status) {
        return "Passed".equals(status) || "Errored".equals(status)
                || "Failed".equals(status);
    }

    private boolean isSuccess(String status) {
        return "Passed".equals(status);
    }

    private File getResultsSummaryFile() throws IOException {
        Props props = new Props(getProjectDir(), this);
        String testResultsLoc = props.get("mdw.test.results.location");
        if (testResultsLoc == null)
            testResultsLoc = getGitRoot() + "/testResults";
        File resultsDir = new File(testResultsLoc);
        if (!resultsDir.isDirectory() && !resultsDir.mkdirs())
            throw new IOException(resultsDir.getAbsolutePath() + " is not a directory and cannot be created.");
        String summaryFile = props.get("mdw.function.tests.summary.file");
        if (summaryFile == null)
            summaryFile = "mdw-function-test-results.json";
        return new File(testResultsLoc + "/" + summaryFile);
    }
}
