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
package com.centurylink.mdw.services.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.xmlbeans.XmlException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.config.YamlBuilder;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.event.AdapterStubRequest;
import com.centurylink.mdw.model.event.AdapterStubResponse;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.ActivityStubRequest;
import com.centurylink.mdw.model.workflow.ActivityStubResponse;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.model.workflow.WorkStatuses;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskServices;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.test.PreFilter;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCase.Status;
import com.centurylink.mdw.test.TestCaseActivityStub;
import com.centurylink.mdw.test.TestCaseAdapterStub;
import com.centurylink.mdw.test.TestCaseEvent;
import com.centurylink.mdw.test.TestCaseFile;
import com.centurylink.mdw.test.TestCaseHttp;
import com.centurylink.mdw.test.TestCaseItem;
import com.centurylink.mdw.test.TestCaseMessage;
import com.centurylink.mdw.test.TestCaseProcess;
import com.centurylink.mdw.test.TestCaseResponse;
import com.centurylink.mdw.test.TestCaseTask;
import com.centurylink.mdw.test.TestCompare;
import com.centurylink.mdw.test.TestException;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.test.TestFailedException;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.FileHelper;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class TestCaseRun implements Runnable {

    static final String NODE_PACKAGE = "com.centurylink.mdw.node";

    private TestCase testCase;
    public TestCase getTestCase() { return testCase; }

    private String user;
    protected String getUser() { return user; }

    private File resultsDir;
    public File getResultsDir() { return resultsDir; }

    private int runNumber;
    public int getRunNumber() { return runNumber; }

    private String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String masterRequestId) { this.masterRequestId = masterRequestId; }

    private MasterRequestListener masterRequestListener;
    public void setMasterRequestListener(MasterRequestListener listener) { masterRequestListener = listener; }
    MasterRequestListener getMasterRequestListener() { return masterRequestListener; }

    private LogMessageMonitor monitor;
    protected LogMessageMonitor getMonitor() { return monitor; }

    private Map<String,Process> processCache;
    protected Map<String,Process> getProcessCache() { return processCache; }

    private TestExecConfig config;
    protected TestExecConfig getConfig() { return config; }

    public boolean isLoadTest() { return config.isLoadTest(); }
    public boolean isVerbose() { return config.isVerbose(); }
    public boolean isStubbing() { return config.isStubbing(); }
    public boolean isCreateReplace() { return config.isCreateReplace(); }

    private PrintStream log = System.out;
    public PrintStream getLog() { return log; }
    public void setLog(PrintStream outStream) { this.log = outStream; }

    private PreFilter preFilter = null;
    PreFilter getPreFilter() { return preFilter; }
    void setPreFilter(PreFilter preFilter) { this.preFilter = preFilter; }

    TestCaseProcess testCaseProcess;

    private List<ProcessInstance> processInstances;
    public List<ProcessInstance> getProcessInstances() { return processInstances; }

    private String message;
    private boolean oneThreadPerCase;
    private boolean passed;

    private WorkflowServices workflowServices;
    private TaskServices taskServices;

    public TestCaseRun(TestCase testCase, String user, File mainResultsDir, int run, String masterRequestId,
            LogMessageMonitor monitor, Map<String,Process> processCache, TestExecConfig config) throws IOException {
        this.testCase = testCase;
        this.user = user;
        this.resultsDir = new File(mainResultsDir + "/" + testCase.getPackage());
        this.runNumber = run;
        this.masterRequestId = masterRequestId;
        this.monitor = monitor;
        this.processCache = processCache;
        this.config = config;
        this.oneThreadPerCase = !config.isLoadTest();

        if (!resultsDir.exists()) {
            if (!resultsDir.mkdirs())
                throw new IOException("Cannot create test results directory: " + resultsDir);
        }

        this.log = new PrintStream(resultsDir + "/" + testCase.getAsset().getRootName() + ".log");
        if (isVerbose())
            log.format("===== prepare case %s (id=%s)\r\n", testCase.getPath(), masterRequestId);

        this.workflowServices = ServiceLocator.getWorkflowServices();
        this.taskServices = ServiceLocator.getTaskServices();
    }

    public void startExecution() {
        passed = true;
        message = null;
        testCase.setStatus(TestCase.Status.InProgress);
        testCase.setStart(new Date());
        if (testCase.getItems() != null) {
            for (TestCaseItem item : testCase.getItems()) {
                item.setStatus(TestCase.Status.InProgress);
            }
        }

        deleteResultsFile();
        log.format("===== execute case %s\r\n", testCase.getPath());
    }

    public void run() {
        startExecution();
        try {
            if (testCase.getAsset().isFormat(Asset.POSTMAN)) {
                String runnerClass = NODE_PACKAGE + ".TestRunner";
                Package pkg = PackageCache.getPackage(testCase.getPackage());
                Class<?> testRunnerClass = CompiledJavaCache.getResourceClass(runnerClass, getClass().getClassLoader(), pkg);
                Object runner = testRunnerClass.newInstance();
                Method runMethod = testRunnerClass.getMethod("run", TestCase.class);
                runMethod.invoke(runner, testCase);
                finishExecution(null);
            }
            else {
                String groovyScript = testCase.getText();
                CompilerConfiguration compilerConfig = new CompilerConfiguration();
                compilerConfig.setScriptBaseClass(TestCaseScript.class.getName());

                Binding binding = new Binding();
                binding.setVariable("testCaseRun", this);

                ClassLoader classLoader = this.getClass().getClassLoader();
                Package testPkg = PackageCache.getPackage(testCase.getPackage());
                if (testPkg != null)
                    classLoader = testPkg.getCloudClassLoader();

                GroovyShell shell = new GroovyShell(classLoader, binding, compilerConfig);
                Script gScript = shell.parse(groovyScript);
                gScript.setProperty("out", log);
                gScript.run();
                finishExecution(null);
            }
        }
        catch (Throwable ex) {
            finishExecution(ex);
        }
    }

    void startProcess(TestCaseProcess process) throws TestException {
        if (isVerbose())
            log.println("starting process " + process.getLabel() + "...");
        this.testCaseProcess = process;
        try {
            Process proc = process.getProcess();
            Map<String,String> params = process.getParams();
            if (proc.isService())
                workflowServices.invokeServiceProcess(proc, masterRequestId, OwnerType.TESTER, 0L, params);
            else
                workflowServices.launchProcess(proc, masterRequestId, OwnerType.TESTER, 0L, params);
        }
        catch (Exception ex) {
            throw new TestException("Failed to start " + process.getLabel(), ex);
        }
    }

    /**
     * Delete the (convention-based) result file if exists
     */
    protected void deleteResultsFile() {
        String resFileName = getTestCase().getName();
        if (resFileName.endsWith(".test")){
            int dotTest = resFileName.lastIndexOf(".test");
            resFileName = resFileName.substring(0, dotTest);
        }
        resFileName = resFileName + Asset.getFileExtension(Asset.YAML);
        File resFile = new File(resultsDir + "/" + resFileName);
        if (resFile.isFile())
            resFile.delete();
    }

    List<ProcessInstance> loadProcess(TestCaseProcess[] processes, Asset expectedResults) throws TestException {
        processInstances = null;
        if (isLoadTest())
            throw new TestException("Not supported for load tests");
        try {
            List<Process> processVos = new ArrayList<Process>();
            if (isVerbose())
                log.println("loading runtime data for processes:");
            for (TestCaseProcess process : processes) {
                if (isVerbose())
                    log.println("  - " + process.getLabel());
                processVos.add(process.getProcess());
            }
            processInstances = loadResults(processVos, expectedResults, processes[0].isResultsById());
            return processInstances;
        }
        catch (Exception ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage().trim();
            if (msg.startsWith("<")) {
                // xml message
                try {
                    MDWStatusMessageDocument statusMsgDoc = MDWStatusMessageDocument.Factory.parse(msg);
                    msg = statusMsgDoc.getMDWStatusMessage().getStatusMessage();
                }
                catch (XmlException xmlEx) {
                    // not an MDW status message -- just escape XML
                    msg = StringEscapeUtils.escapeXml(msg);
                }
            }
            throw new TestException(msg, ex);
        }
    }

    protected List<ProcessInstance> loadResults(List<Process> processes, Asset expectedResults, boolean orderById)
    throws DataAccessException, IOException, ServiceException, JSONException, ParseException {
        List<ProcessInstance> mainProcessInsts = new ArrayList<ProcessInstance>();
        Map<String,List<ProcessInstance>> fullProcessInsts = new TreeMap<String,List<ProcessInstance>>();
        Map<String,String> fullActivityNameMap = new HashMap<String,String>();
        for (Process proc : processes) {
            Query query = new Query();
            query.setFilter("masterRequestId", masterRequestId);
            query.setFilter("processId", proc.getId().toString());
            query.setDescending(true);
            List<ProcessInstance> procInstList = workflowServices.getProcesses(query).getProcesses();
            Map<Long,String> activityNameMap = new HashMap<Long,String>();
            for (Activity act : proc.getActivities()) {
                activityNameMap.put(act.getId(), act.getName());
                fullActivityNameMap.put(proc.getId() + "-" + act.getId(), act.getName());
            }
            if (proc.getSubprocesses() != null) {
                for (Process subproc : proc.getSubprocesses()) {
                    for (Activity act : subproc.getActivities()) {
                        activityNameMap.put(act.getId(), act.getName());
                        fullActivityNameMap.put(proc.getId() + "-" + act.getId(), act.getName());
                    }
                }
            }
            for (ProcessInstance procInst : procInstList) {
                procInst = workflowServices.getProcess(procInst.getId());
                mainProcessInsts.add(procInst);
                List<ProcessInstance> procInsts = fullProcessInsts.get(proc.getName());
                if (procInsts == null)
                    procInsts = new ArrayList<ProcessInstance>();
                procInsts.add(procInst);
                fullProcessInsts.put(proc.getName(), procInsts);
                if (proc.getSubprocesses() != null) {
                    Query q = new Query();
                    q.setFilter("owner", OwnerType.MAIN_PROCESS_INSTANCE);
                    q.setFilter("ownerId", procInst.getId().toString());
                    q.setFilter("processId", proc.getId().toString());
                    List<ProcessInstance> embeddedProcInstList = workflowServices.getProcesses(q).getProcesses();
                    for (ProcessInstance embeddedProcInst : embeddedProcInstList) {
                        ProcessInstance fullChildInfo = workflowServices.getProcess(embeddedProcInst.getId());
                        String childProcName = "unknown_subproc_name";
                        for (Process subproc : proc.getSubprocesses()) {
                            if (subproc.getId().toString().equals(embeddedProcInst.getComment())) {
                                childProcName = subproc.getName();
                                if (!childProcName.startsWith(proc.getName()))
                                    childProcName = proc.getName() + " " + childProcName;
                                break;
                            }
                        }
                        List<ProcessInstance> pis = fullProcessInsts.get(childProcName);
                        if (pis == null)
                            pis = new ArrayList<ProcessInstance>();
                        pis.add(fullChildInfo);
                        fullProcessInsts.put(childProcName, pis);
                    }
                }
            }
        }
        String newLine = "\n";
        if (!isCreateReplace()) {
            if (expectedResults == null || expectedResults.getRawFile() == null || !expectedResults.getRawFile().exists()) {
                throw new IOException("Expected results file not found for " + testCase.getPath());
            }
            else {
                // try to determine newline chars from expectedResultsFile
                if (expectedResults.getStringContent().indexOf("\r\n") >= 0)
                    newLine = "\r\n";
            }
        }
        String yaml = translateToYaml(fullProcessInsts, fullActivityNameMap, orderById, newLine);
        if (isCreateReplace()) {
            log.println("creating expected results: " + expectedResults.getRawFile());
            FileHelper.writeToFile(expectedResults.getRawFile().toString(), yaml, false);
            expectedResults.setStringContent(yaml);
        }
        String fileName = resultsDir + "/" + expectedResults.getName();
        if (isVerbose())
            log.println("creating actual results file: " + fileName);
        FileHelper.writeToFile(fileName, yaml, false);
        // set friendly statuses
        if (mainProcessInsts != null) {
            for (ProcessInstance pi : mainProcessInsts)
                pi.setStatus(WorkStatuses.getWorkStatuses().get(pi.getStatusCode()));
        }
        return mainProcessInsts;
    }

    protected String translateToYaml(Map<String,List<ProcessInstance>> processInstances, Map<String,String> activityNames, boolean orderById, String newLineChars)
    throws IOException, DataAccessException {

        YamlBuilder yaml = new YamlBuilder(newLineChars);

        for (String procName : processInstances.keySet()) {
            List<ProcessInstance> procInsts = processInstances.get(procName);
            for (int i = 0; i < procInsts.size(); i++) {
                ProcessInstance procInst = procInsts.get(i);
                yaml.append("process: # ").append(procInst.getId()).newLine();
                yaml.append("  name: ").append(procName).newLine();
                yaml.append("  instance: ").append((i + 1)).newLine();
                LinkedList<ActivityInstance> orderedList = new LinkedList<ActivityInstance>();
                for (ActivityInstance act : procInst.getActivities())
                    orderedList.add(act);
                if (orderById) {
                    Collections.sort(orderedList, new Comparator<ActivityInstance>() {
                        public int compare(ActivityInstance ai1, ActivityInstance ai2) {
                            return (int)(ai1.getActivityId() - ai2.getActivityId());
                        }
                    });
                }
                for (ActivityInstance act : orderedList) {
                    yaml.append("  activity: # ").append(act.getActivityId()).append(" \"").append(StringHelper.dateToString(act.getStartDate())).append("\"").newLine();
                    String actNameKey = procInst.getProcessId() + "-" + act.getActivityId();
                    yaml.append("    name: " ).appendMulti("      ", activityNames.get(actNameKey)).newLine();
                    yaml.append("    status: ").append(WorkStatuses.getWorkStatuses().get(act.getStatusCode())).newLine();
                    if (act.getMessage() != null) {
                        String msgLines[] = act.getMessage().split("\\r\\n|\\n|\\r");
                        String result = msgLines[0];
                        if (msgLines.length > 1)
                            result += "...";
                        yaml.append("    result: ").append(result).newLine();
                    }
                }
                for (VariableInstance var : procInst.getVariables()) {
                    yaml.append("  variable: # ").append(var.getInstanceId()).newLine();
                    yaml.append("    name: ").append(var.getName()).newLine();
                    yaml.append("    value: ");
                    try {
                        var.setProcessInstanceId(procInst.getId());
                        String val = getStringValue(var);
                        if (var.isDocument())
                            procInst.getVariable().put(var.getName(), val); // pre-populate document values
                        yaml.appendMulti("      ", val).newLine();
                    }
                    catch (Throwable t) {
                        log.println("Failed to translate variable instance: " + var.getInstanceId() + " to string with the following exception");
                        t.printStackTrace(log);
                        yaml.append(" \"").append(var.getStringValue()).append("\"").newLine();
                    }
                }
            }
        }
        return yaml.toString();
    }

    protected String getStringValue(VariableInstance var) throws TestException {
        String val = var.getStringValue();
        if (var.isDocument()) {
            try {
                val = workflowServices.getDocumentStringValue(new DocumentReference(val).getDocumentId());
            }
            catch (ServiceException ex) {
                throw new TestException(ex.getMessage(), ex);
            }
        }

        return val;
    }

    boolean verifyProcess(TestCaseProcess[] processes, Asset expectedResults) throws TestException {
        processInstances = null;
        if (isLoadTest())
            return true;
        if (!passed)
            return false;

        try {
            List<Process> processVos = new ArrayList<Process>();
            if (isVerbose())
                log.println("loading runtime data for processes:");
            for (TestCaseProcess process : processes) {
                if (isVerbose())
                    log.println("  - " + process.getLabel());
                processVos.add(process.getProcess());
            }
            processInstances = loadResults(processVos, expectedResults, processes[0].isResultsById());
            if (processInstances.isEmpty())
                throw new IllegalStateException("No process instances found for masterRequestId: " + masterRequestId);
            return verifyProcesses(expectedResults);
        }
        catch (Exception ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage().trim();
            if (msg.startsWith("<")) {
                // xml message
                try {
                    MDWStatusMessageDocument statusMsgDoc = MDWStatusMessageDocument.Factory.parse(msg);
                    msg = statusMsgDoc.getMDWStatusMessage().getStatusMessage();
                }
                catch (XmlException xmlEx) {
                    // not an MDW status message -- just escape XML
                    msg = StringEscapeUtils.escapeXml(msg);
                }
            }
            throw new TestException(msg, ex);
        }
    }

    protected boolean verifyProcesses(Asset resultsAsset)
    throws TestException, IOException, DataAccessException, ParseException {
        if (!resultsAsset.getRawFile().exists()) {
            message = "Expected results not found: " + resultsAsset;
            log.println("+++++ " + message);
            passed = false;
        }
        else {
            TestCaseFile actualResultsFile = new TestCaseFile(resultsDir + "/" + resultsAsset.getName());
            if (isVerbose())
                log.println("... compare " + resultsAsset + " with " + actualResultsFile + "\r\n");
            if (!actualResultsFile.exists()) {
                message = "Actual results not found: " + actualResultsFile;
                log.println("+++++ " + message);
                passed = false;
            }
            else {
                if (isVerbose()) {
                    log.println("expected:");
                    log.println(resultsAsset.getStringContent());
                    log.println("actual:");
                    log.println(actualResultsFile.getText());
                }

                TestCompare testCompare = new TestCompare(getPreFilter());
                int firstDiffLine = testCompare.doCompare(resultsAsset, actualResultsFile);
                if (firstDiffLine == 0) {
                    passed = true;
                }
                else {
                    passed = false;
                    message = "+++++ " + resultsAsset.getName() + ": differs from line " + firstDiffLine;
                    log.println(message);
                }
            }
        }

        return passed;
    }

    Process getProcess(String target) throws TestException {
        target = qualify(target);
        Process process = processCache.get(target);
        if (process == null) {
            try {
                Query query = getProcessQuery(target);
                process = workflowServices.getProcessDefinition(query.getPath(), query);
                if (process == null)
                    throw new FileNotFoundException("Process: " + target + " not found");
                processCache.put(target, process);
            }
            catch (Exception ex) {
                throw new TestException("Cannot load " + target, ex);
            }
        }
        return process;
    }

    Asset getAsset(String path) throws TestException {
        if (path.endsWith(".proc"))
            return getProcess(path);
        try {
            return AssetCache.getAsset(path.indexOf('/') > 0 ? path : getTestCase().getPackage() + '/' + path);
        }
        catch (Exception ex) {
            throw new TestException("Cannot load " + path, ex);
        }
    }

    TestCaseResponse sendMessage(TestCaseMessage message) throws TestException {
        if (isLoadTest()) // special subst for CSV
            message.setPayload(message.getPayload().replaceAll("#\\{", "#\\{\\$"));
        if (isVerbose()) {
            log.println("sending " + message.getProtocol() + " message...");
            log.println("message payload:");
            log.println(message.getPayload());
        }

        try {
            String endpoint = "services";
            if (Listener.METAINFO_PROTOCOL_SOAP.equals(message.getProtocol()))
                endpoint += "/SOAP";
            HttpHelper httpHelper = getHttpHelper("POST", endpoint);
            httpHelper.setHeaders(getDefaultMessageHeaders(message.getPayload()));
            String actual = httpHelper.post(message.getPayload());
            if (isVerbose()) {
                log.println("response:");
                log.println(actual);
            }
            TestCaseResponse response = new TestCaseResponse();
            response.setActual(actual);
            return response;
        }
        catch (Exception ex) {
            throw new TestException("Failed to send " + message.getProtocol() + " message", ex);
        }
    }

    TestCaseResponse http(TestCaseHttp http) throws TestException {
        if (isVerbose()) {
            log.println("http " + http.getMethod() + " request to " + http.getUri());
        }

        try {
            HttpHelper helper;
            if (http.getMessage() != null && http.getMessage().getUser() != null) {
                helper = getHttpHelper(http.getMethod(), http.getUri(), http.getMessage().getUser(), http.getMessage().getPassword());
            }
            else {
                helper = getHttpHelper(http.getMethod(), http.getUri());
            }

            Map<String,String> headers = new HashMap<String,String>();
            if (http.getMessage() != null) {
                if (http.getMessage().getPayload() != null && http.getMessage().getPayload().startsWith("{"))
                    headers.put("Content-Type", "application/json");
                if (http.getMessage().getHeaders() != null)
                    headers.putAll(http.getMessage().getHeaders());
            }
            if (!headers.isEmpty())
                helper.setHeaders(headers);

            if (http.getConnectTimeout() > 0)
                helper.setConnectTimeout(http.getConnectTimeout());
            if (http.getReadTimeout() > 0)
                helper.setReadTimeout(http.getReadTimeout());

            String actual;
            if (http.getMethod().equalsIgnoreCase("get")) {
                try {
                    actual = helper.get();
                }
                catch (IOException ex) {
                    actual = helper.getResponse();
                }
            }
            else {
                if (http.getMessage() == null) {
                    if (!http.getMethod().equalsIgnoreCase("delete"))
                        throw new TestException("Missing payload for HTTP: " + http.getMethod());
                }
                try {
                    if (http.getMethod().equalsIgnoreCase("post"))
                        actual = helper.post(http.getMessage().getPayload());
                    else if (http.getMethod().equalsIgnoreCase("put"))
                        actual = helper.put(http.getMessage().getPayload());
                    else if (http.getMethod().equalsIgnoreCase("patch"))
                        actual = helper.patch(http.getMessage().getPayload());
                    else if (http.getMethod().equalsIgnoreCase("delete"))
                        actual = helper.delete(http.getMessage() == null ? null : http.getMessage().getPayload());
                    else
                        throw new TestException("Unsupported http method: " + http.getMethod());
                }
                catch (IOException ex) {
                    actual = helper.getResponse();
                    if (helper.getResponseCode() <= 0)
                        ex.printStackTrace(log);
                }
            }

            if (isVerbose()) {
                log.println("http response:");
                log.println(actual);
            }
            TestCaseResponse response = new TestCaseResponse();
            response.setHeaders(helper.getHeaders());
            response.setCode(helper.getResponseCode());
            response.setActual(actual);
            return response;
        }
        catch (Exception ex) {
            throw new TestException("Failed to send http " + http.getMethod() + " request to " + http.getUri(), ex);
        }
    }

    void wait(TestCaseProcess process) throws TestException {
        if (process.getTimeout() == 0)
            throw new TestException("Missing property 'timeout' for process wait command");
        if (isVerbose())
            log.println("waiting for process: " + process.getLabel() + " (timeout=" + process.getTimeout() + "s)...");
        String waitKey = getWaitKey(process.getProcess(), process.getActivityLogicalId(), process.getStatus());
        performWait(waitKey, process.getTimeout());
    }

    protected void performWait(String key, int timeout) throws TestException {
        try {
            synchronized (this) {
                monitor.register(this, key);
                this.wait(timeout*1000);
            }
            Object stillthere = monitor.remove(key);
            if (stillthere != null) {
                log.println("wait command times out after: " + timeout + "s at: " + StringHelper.dateToString(new Date()));
            }
            else {
                Thread.sleep(2000);  // to get around race condition
                if (isVerbose())
                    log.println("wait command satisfied: " + key);
            }
        } catch (InterruptedException e) {
            log.println("Wait gets interrupted");
            testCase.setStatus(TestCase.Status.Errored);
        }
    }

    protected String getWaitKey(Process process, String activityLogicalId, String status) throws TestException {
        Long activityId = 0L;
        if (activityLogicalId != null)
            activityId = getActivityId(process, activityLogicalId);
        if (status == null)
            status = WorkStatus.STATUSNAME_COMPLETED;
        return monitor.createKey(masterRequestId, process.getId(), activityId, status);
    }

    private Long getActivityId(Process proc, String activityLogicalId) {
        for (Activity act : proc.getActivities()) {
            String lid = act.getLogicalId();
            if (activityLogicalId.equals(lid)) {
                return act.getId();
            }
        }
        return null;
    }

    boolean verifyResponse(TestCaseResponse response) throws TestException {
        if (isLoadTest())
            return true;
        if (!passed)
            return false;
        try {
            if (isVerbose()) {
                log.println("expected response:");
                log.println(response.getExpected());
                log.println("actual response:");
                log.println(response.getActual());
            }
            return executeVerifyResponse(response.getExpected(), response.getActual());
        }
        catch (Exception ex) {
            throw new TestException(ex.getMessage(), ex);
        }
    }

    protected boolean executeVerifyResponse(String expected, String actual)
    throws TestException,IOException,DataAccessException,ParseException {
        expected = expected.replaceAll("\r", "");
        if (isVerbose())
            log.println("comparing response...");
        if (actual != null) {
            actual = actual.replaceAll("\r", "");
        }
        if (!expected.equals(actual)) {
            int firstDiffLine = TestCompare.matchRegex(expected, actual);
            if (firstDiffLine != 0) {
                passed = false;
                message = "response differs from line: " + firstDiffLine;
                if (!isVerbose())  { // otherwise it was logged previously
                  log.format("+++++ " + message + "\r\n");
                  log.format("Actual response: %s\r\n", actual);
                }
                throw new TestFailedException(message);
            }
        }
        return passed;
    }

    void performTaskAction(TestCaseTask task) throws TestException {
        Query query = getTaskQuery(task.getName());
        try {
            TaskList taskList = taskServices.getTasks(query, user);
            List<TaskInstance> taskInstances = taskList.getTasks();
            if (taskInstances.isEmpty())
                throw new TestException("Cannot find task instances: " + query);

            TaskInstance taskInstance = taskInstances.get(0); // latest
            if (isVerbose())
                log.println("performing: " + task.getOutcome() + " on task '" + task.getName() + "' (" + taskInstance.getId() + ")");
            task.setId(taskInstance.getTaskInstanceId());

            UserTaskAction taskAction = new UserTaskAction();
            taskAction.setTaskAction(task.getOutcome());
            taskAction.setUser(user);
            taskAction.setTaskInstanceId(task.getId());
            if (task.getVariables() != null)
                workflowServices.setVariables(taskInstance.getOwnerId(), task.getVariables());
            taskServices.performTaskAction(taskAction);
        }
        catch (TestException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new TestException("Error actioning task: " + task.getId(), ex);
        }
    }

    void notifyEvent(TestCaseEvent event) throws TestException {
        if (isVerbose())
            log.println("notifying event: '" + event.getId() + "'");

        try {
            workflowServices.notify(event.getId(), event.getMessage(), 0);
        }
        catch (ServiceException ex) {
            throw new TestException("Unable to notifyEvent: " + ex.getMessage());
        }
    }

    void sleep(int seconds) {
        if (isVerbose())
            log.println("sleeping " + seconds + " seconds...");

        if (oneThreadPerCase) {
            try {
                Thread.sleep(seconds * 1000);
            }
            catch (InterruptedException e) {
                log.println("Sleep interrupted");
            }
        }
        // else do nothing - already slept
    }

    private List<TestCaseAdapterStub> adapterStubs = new ArrayList<TestCaseAdapterStub>();
    void addAdapterStub(TestCaseAdapterStub adapterStub) {
        adapterStubs.add(adapterStub);
    }

    /**
     * Callback method invoked from stub server when notified from server.
     * Default implementation to substitute values from request as super.getStubResponse();
     * Users can provide their own implementation as a Groovy closure.
     */
    public ActivityStubResponse getStubResponse(ActivityStubRequest request) throws JSONException, TestException {
        // activity stubbing
        ActivityRuntimeContext activityRuntimeContext = request.getRuntimeContext();
        if (testCaseProcess != null && testCaseProcess.getActivityStubs() != null) {
            for (TestCaseActivityStub activityStub : testCaseProcess.getActivityStubs()) {
                if (activityStub.getMatcher().call(activityRuntimeContext)) {
                    Closure<String> completer = activityStub.getCompleter();
                    completer.setResolveStrategy(Closure.DELEGATE_FIRST);
                    completer.setDelegate(activityStub);
                    String resultCode = null;
                    try {
                        resultCode = completer.call(request.getJson().toString(2));
                    }
                    catch (Throwable th) {
                        th.printStackTrace(log);
                        throw new TestException(th.getMessage(), th);
                    }
                    ActivityStubResponse activityStubResponse = new ActivityStubResponse();
                    activityStubResponse.setResultCode(resultCode);
                    if (activityStub.getSleep() > 0)
                        activityStubResponse.setDelay(activityStub.getSleep());
                    else if (activityStub.getDelay() > 0)
                        activityStubResponse.setDelay(activityStub.getDelay());
                    if (activityStub.getVariables() != null) {
                        Map<String,String> responseVariables = new HashMap<String,String>();
                        Map<String,Object> variables = activityStub.getVariables();
                        for (String name : variables.keySet()) {
                            Object value = variables.get(name);
                            // TODO: handle non-string objects
                            responseVariables.put(name, value.toString());
                        }
                        activityStubResponse.setVariables(responseVariables);
                    }
                    if (isVerbose())
                        log.println("Stubbing activity " + activityRuntimeContext.getProcess().getQualifiedName() + ":" +
                                activityRuntimeContext.getActivityLogicalId() + " with result code: " + resultCode);
                    return activityStubResponse;
                }
            }
        }

        ActivityStubResponse passthroughResponse = new ActivityStubResponse();
        passthroughResponse.setPassthrough(true);
        return passthroughResponse;
    }

    /**
     * Callback method invoked from stub server when notified from server.
     * Default implementation to substitute values from request as super.getStubResponse();
     * Users can provide their own implementation as a Groovy closure.
     */
    public AdapterStubResponse getStubResponse(AdapterStubRequest request) throws JSONException, TestException {
        // adapter stubbing
        for (TestCaseAdapterStub adapterStub : adapterStubs) {
            boolean match = false;
            try {
                if (adapterStub.isEndpoint()) {
                    match = adapterStub.getMatcher().call(request);
                }
                else {
                    match = adapterStub.getMatcher().call(request.getContent());
                }
            }
            catch (Throwable th) {
                th.printStackTrace(log);
                throw new TestException(th.getMessage(), th);
            }

            if (match) {
                try {
                    String stubbedResponseContent = adapterStub.getResponder().call(request.getContent());
                    if (isVerbose()) {
                        if (adapterStub.isEndpoint())
                            log.println("Stubbing endpoint " + request.getUrl() + " with:\n" + stubbedResponseContent);
                        else
                            log.println("Stubbing response with: " + stubbedResponseContent);
                    }
                    int delay = 0;
                    if (adapterStub.getDelay() > 0)
                        delay = adapterStub.getDelay();
                    else if (adapterStub.getSleep() > 0)
                        delay = adapterStub.getSleep();

                    AdapterStubResponse stubResponse = new AdapterStubResponse(stubbedResponseContent);
                    stubResponse.setDelay(delay);
                    stubResponse.setStatusCode(adapterStub.getStatusCode());
                    stubResponse.setStatusMessage(adapterStub.getStatusMessage());
                    return stubResponse;
                }
                catch (Throwable th) {
                    th.printStackTrace(log);
                    throw new TestException(th.getMessage(), th);
                }
            }
        }

        AdapterStubResponse passthroughResponse = new AdapterStubResponse(AdapterActivity.MAKE_ACTUAL_CALL);
        passthroughResponse.setPassthrough(true);
        return passthroughResponse;
    }

    public JSONObject submitItem(TestCase apiTestCase, TestCaseItem testItem) throws TestException {
        try {
            // close log stream and reopen after js runner to sync file access
            getLog().close();
            String runnerClass = TestCaseRun.NODE_PACKAGE + ".TestRunner";
            Package pkg = PackageCache.getPackage(apiTestCase.getPackage());
            Class<?> testRunnerClass = CompiledJavaCache.getResourceClass(runnerClass, getClass().getClassLoader(), pkg);
            Object runner = testRunnerClass.newInstance();
            Method runMethod = testRunnerClass.getMethod("run", TestCase.class);
            runMethod.invoke(runner, apiTestCase);
            setLog(new PrintStream(new FileOutputStream(getResultsDir()
                    + "/" + getTestCase().getAsset().getRootName() + ".log", true)));

            return testItem.getResponse();
        }
        catch (Exception ex) {
            throw new TestException(ex.getMessage(), ex);
        }
    }

    public boolean verifyItem(TestCase testCase, TestCaseItem item) throws TestException {
        // close log stream and reopen after js runner to sync file access
        getLog().close();
        String runnerClass = TestCaseRun.NODE_PACKAGE + ".TestRunner";
        Package pkg = PackageCache.getPackage(testCase.getPackage());
        try {
            Class<?> testRunnerClass = CompiledJavaCache.getResourceClass(runnerClass, getClass().getClassLoader(), pkg);
            Object runner = testRunnerClass.newInstance();
            Method runMethod = testRunnerClass.getMethod("run", TestCase.class);
            runMethod.invoke(runner, testCase);
            setLog(new PrintStream(new FileOutputStream(getResultsDir()
                    + "/" + getTestCase().getAsset().getRootName() + ".log", true)));
            testCase.setStatus(item.getStatus());
            testCase.setMessage(item.getMessage());
            passed = testCase.getStatus() == Status.Passed;
            return passed;
        }
        catch (Exception ex) {
            throw new TestException(ex.getMessage(), ex);
        }
    }

    public void finishExecution(Throwable e) {
        if (isLoadTest()) {
            if (e != null)
              e.printStackTrace(); // otherwise won't see errors
            if (log != System.out && log != System.err)
                log.close();
            return;
        }
        // function test only below
        if (e == null) {
        }
        else if (e instanceof TestException) {
            passed = false;
            message = firstLine(e.getMessage());
            log.println(message);
            if (e.getCause() instanceof TestFailedException) {
                // find the script line number
                for (StackTraceElement el : e.getStackTrace()) {
                    if (el.getFileName() != null && el.getFileName().endsWith(".groovy")) {
                        log.println(" --> at " + getTestCase().getPath() + ":" + el.getLineNumber());
                    }
                }
            }
            else {
                e.printStackTrace(log);
            }
        }
        else if (e instanceof ParseException) {
            passed = false;
            message = "Command syntax error at line "
                + ((ParseException)e).getErrorOffset() + ": " + e.getMessage();
            log.println(message);
            e.printStackTrace(log);
        }
        else {
            passed = false;
            message = firstLine(e.toString());
            if ("Assertion failed: ".equals(message))
                message += "See execution log for details.";
            log.println("Exception " + message);
            e.printStackTrace(log);
        }
        TestCase.Status status = testCase.getStatus();
        Date endDate = new Date();
        if (isVerbose()) {
            long seconds = (endDate.getTime() - testCase.getStart().getTime()) / 1000;
            if (status == TestCase.Status.Errored)
                log.println("===== case " + testCase.getPath() + " Errored after " + seconds + " seconds");
            else if (status == TestCase.Status.Stopped)
                log.println("===== case " + testCase.getPath() + " Stopped after " + seconds + " seconds");
            else
                log.println("===== case " + testCase.getPath() + (passed ? " Passed" : " Failed") + " after " + seconds + " seconds");
        }
        if (log != System.out && log != System.err)
            log.close();

        if (status != TestCase.Status.Errored && status != TestCase.Status.Stopped) {
            testCase.setEnd(endDate);
            testCase.setStatus(passed ? TestCase.Status.Passed : TestCase.Status.Failed);
        }

        if (testCase.getItems() != null) {
            if (e != null)
                e.printStackTrace();  // else would have to dig in testCase (not item) log
            for (TestCaseItem item : testCase.getItems()) {
                if (e != null) {
                    // don't leave unfinished items
                    if (item.getStatus() == Status.InProgress) {
                        item.setStatus(testCase.getStatus());
                        item.setMessage(testCase.getMessage());
                    }
                }
            }
        }
    }

    protected HttpHelper getHttpHelper(String method, String endpoint) throws MalformedURLException {
        return getHttpHelper(method, endpoint, null, null);
    }

    protected HttpHelper getHttpHelper(String method, String endpoint, String user, String password) throws MalformedURLException {
        HttpHelper helper = HttpHelper.getHttpHelper(method, getUrl(endpoint));
        if (user != null) {
            helper.getConnection().setUser(user);
            helper.getConnection().setPassword(password);
        }
        helper.getConnection().setHeader("Content-Type", "application/json");
        return helper;
    }

    protected URL getUrl(String endpoint) throws MalformedURLException {
        String url = endpoint;
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            url = PropertyManager.getProperty("mdw.test.base.url");
            if (url == null)
                url = ApplicationContext.getServicesUrl();
            if (!endpoint.startsWith("/"))
                endpoint = "/" + endpoint;
            url += endpoint;
        }
        return new URL(url);
    }

    /**
     * Not for HTTP method but for general sendMessage().
     */
    protected Map<String,String> getDefaultMessageHeaders(String payload) {
        if (getMasterRequestId() != null) {
            Map<String,String> headers = new HashMap<String,String>();
            headers.put("mdw-request-id", getMasterRequestId());
            if (payload != null && payload.startsWith("{"))
                headers.put("Content-Type", "application/json");

            return headers;
        }
        return null;
    }

    protected String qualify(String target) {
        if (target.indexOf('/') == -1 && getTestCase().getPackage() != null)
            return getTestCase().getPackage() + "/" + target;
        return target;
    }

    protected Query getProcessQuery(String target) {
        String procPath = target;
        int version = 0; // latest
        int spaceV = target.lastIndexOf(" v");
        if (spaceV > 0) {
            try {
                version = Asset.parseVersionSpec(procPath.substring(spaceV + 2));
                procPath = target.substring(0, spaceV);
            }
            catch (NumberFormatException ex) {
                // process name must contain space v
            }
        }
        Query query = new Query(procPath);
        query.setFilter("version", version);
        query.setFilter("app", "autotest");
        return query;
    }

    protected Query getTaskQuery(String name) {
        Query query = new Query("");
        query.setFilter("masterRequestId", masterRequestId);
        query.setFilter("name", name);
        query.setFilter("app", "autotest");
        query.setDescending(true);
        return query;
    }

    /**
     * Makes sure the message is embeddable in an XML attribute for results parsing.
     */
    private String firstLine(String msg) {
        if (msg == null)
            return msg;
        int newLine = msg.indexOf("\r\n");
        if (newLine == -1)
            newLine = msg.indexOf("\n");
        if (newLine == -1)
            return msg.replace("\"", "'");
        else
            return msg.substring(0, newLine).replace("\"", "'");
    }

    byte[] read(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
        finally {
            if (fis != null)
                fis.close();
        }
    }
}