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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

;

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

    @Parameter(names="--stub-port", description="Stub port")
    private int stubPort;
    public int getStubPort() { return stubPort; }
    public void setStubPort(int port) { this.stubPort = port; }

    @Parameter(names="--postman-env", description="Postman env asset")
    private String postmanEnv;
    public String getPostmanEnv() { return postmanEnv; }
    public void setPostmanEnv(String env) { this.postmanEnv = env; }

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
            getOut().println("Finding tests...");
        List<File> caseFiles = findCaseFiles();
        if (caseFiles.isEmpty()) {
            getOut().println("No tests found");
            return this;
        }

        if (isDebug()) {
            getOut().println("Test Cases:");
            for (File caseFile : caseFiles)
                getOut().println("  " + caseFile);
        }

        JSONObject configRequest = getConfigRequest();
        new Fetch(new URL(getServicesUrl() + PATH + "/config")).put(configRequest.toString(2));

        Map<String,List<String>> pkgTests = new LinkedHashMap<>();
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
            getOut().println("Running tests...");

        long before = System.currentTimeMillis();
        File resultsFile = getResultsSummaryFile();
        Path resultsPath = Paths.get(resultsFile.getPath());
        clearResults(resultsPath, statuses);

        Path resultsDir = Paths.get(resultsFile.getParentFile().getPath());
        WatchService watcher = FileSystems.getDefault().newWatchService();
        resultsDir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);

        while (!done) {
            try {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (((Path)event.context()).getFileName().toString().equals(resultsFile.getName())) {
                        String contents = new String(Files.readAllBytes(resultsPath));
                        if (contents.isEmpty())
                            continue; // why does this happen?
                        JSONObject jsonObj = null;
                        try {
                            jsonObj = new JSONObject(contents);
                        }
                        catch (JSONException ex) {
                            // cannot be parsed (why?)
                            continue;
                        }
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
                                                    getOut().print("   ");
                                                else
                                                    getOut().print("  *");
                                                getOut().println(status + " (" + finished + "/" + statuses.size() + ") - " + asset);
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
                                                    getOut().println("    (" + test.getString("message"));
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
                getErr().println("Test execution canceled");
                watcher.close();
                done = true;
            }
        }
        watcher.close();
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

    protected final void clearResults(Path resultsPath, Map<String,String> statuses) throws IOException {
        JSONObject jsonObj;
        if (resultsPath.toFile().exists()) {
            // clear info for cases to be run
            jsonObj = new JSONObject(new String(Files.readAllBytes(resultsPath)));
            JSONArray pkgs = jsonObj.getJSONArray("packages");
            for (int i = 0; i < pkgs.length(); i++) {
                JSONObject pkg = pkgs.getJSONObject(i);
                JSONArray tests = pkg.getJSONArray("testCases");
                JSONArray newTests = new JSONArray();
                for (int j = 0; j < tests.length(); j++) {
                    JSONObject test = tests.getJSONObject(j);
                    String asset = pkg.getString("name") + "/" + test.getString("name");
                    if (statuses.containsKey(asset)) {
                        JSONObject newTest = new JSONObject();
                        newTest.put("name", test.getString("name"));
                        newTests.put(newTest);
                    }
                    else {
                        newTests.put(test);
                    }
                }
                pkg.put("testCases", newTests);
            }
        }
        else {
            jsonObj = new JSONObject(); // create blank to avoid parsing issues
        }
        Files.write(resultsPath, jsonObj.toString(2).getBytes());
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
            getOut().println(json.toString(2));
        }
        else {
            getOut().println("\nSummary (" + getServicesUrl() + PATH + "):");
            for (String status : counts.keySet()) {
                getOut().print("  " + status + ": ");
                for (int i = status.length(); i < 16; i++)
                    getOut().print(" ");
                getOut().println(counts.get(status));
            }
            getOut().println("  Total:            " + statuses.size() + " (in " + elapsed + " ms)");
        }
    }

    protected List<File> findCaseFiles() throws IOException {
        List<File> caseFiles = new ArrayList<>();
        List<PathMatcher> inMatchers = getIncludeMatchers();
        List<PathMatcher> exMatchers = getExcludeMatchers();
        Files.walkFileTree(Paths.get(getAssetRoot().getPath()),
                EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (dir.getFileName().equals("node_modules"))
                            return FileVisitResult.SKIP_SUBTREE;
                        else
                            return FileVisitResult.CONTINUE;
                    }
                    @Override
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
        if (isDebug()) {
            json.put("verbose", true);
        }
        if (stubbing) {
            json.put("stubbing", stubbing);
            if (stubPort > 0)
                json.put("stubPort", stubPort);
        }
        if (postmanEnv != null) {
            json.put("postmanEnv", postmanEnv);
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
        Props props = new Props(this);
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
