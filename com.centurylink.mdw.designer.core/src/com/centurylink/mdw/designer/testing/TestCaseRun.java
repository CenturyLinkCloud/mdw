/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.designer.DesignerCompatibility;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.designer.utils.VariableHelper;
import com.centurylink.mdw.designer.utils.YamlBuilder;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengRuntime;
import com.qwest.mbeng.StreamLogger;
import com.qwest.mbeng.XmlPath;

/**
 * This class represents a single run of a test case.
 */
public class TestCaseRun extends ControlCommandShell implements Threadable {

    protected int runNumber;
    protected TestCase testcase;
    protected String masterRequestId;
    public String getMasterRequestId() { return masterRequestId; }
    public void setMasterRequestId(String masterRequestId) { this.masterRequestId = masterRequestId; }

    protected LogMessageMonitor monitor;
    protected boolean createReplace;
    protected boolean passed;
    protected boolean isBatch = false;
    protected boolean isLoadTest;
    protected boolean oneThreadPerCase;
    protected int stepsCompleted;
    protected String message;
    protected Map<String,ProcessVO> processCache;
    protected boolean singleServer;
    protected boolean stubbing;
    public boolean isStubbing() { return stubbing; }
    protected boolean oldNamespaces;
    protected List<String> serverList;
    protected int nextServer;

    private PreFilter preFilter = null;
    PreFilter getPreFilter() { return preFilter; }
    void setPreFilter(PreFilter preFilter) { this.preFilter = preFilter; }


    public TestCaseRun(TestCase testcase, int run, String masterRequestId,
            DesignerDataAccess dao, LogMessageMonitor monitor,
             Map<String,ProcessVO> processCache, boolean isLoadTest,
             boolean oneThreadPerCase, boolean oldNamespaces)
            throws DataAccessException {
        super(dao);
        this.testcase = testcase;
        this.runNumber = run;
        this.masterRequestId = masterRequestId;
        this.monitor = monitor;
        this.processCache = processCache;
        this.stepsCompleted = 0;
        this.isLoadTest = isLoadTest;
        this.oneThreadPerCase = oneThreadPerCase && !isLoadTest;
        this.oldNamespaces = oldNamespaces;
        dao.auditLog(Action.Run, Entity.TestCase, 0L, testcase.getCaseName());
    }

    public int getRunNumber() {
        return runNumber;
    }

    public int getStepsCompleted() {
        return stepsCompleted;
    }

    public TestCase getTestCase() {
        return testcase;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String msg) {
        this.message = msg;
    }

    public void setIsBache(boolean v) {
        isBatch = v;
    }

    public void setLog(PrintStream log) {
        if (this.log != System.out && this.log != System.err)
            this.log.close();
        this.log = log;
    }

    public void prepareTest(boolean createReplace, File resultDir, boolean verbose, boolean singleServer, boolean stubbing, PrintStream log) {
        this.createReplace = createReplace;
        this.verbose = verbose;
        this.singleServer = singleServer;
        if (!singleServer)
            this.serverList = dao.getPeerServerList();
        this.stubbing = stubbing;
        testcase.setResultDirectory(resultDir);
        testcase.setFirstRun(this);
        if (!resultDir.exists())
            resultDir.mkdirs();
        this.log = log;
        if (verbose)
            log.format("===== prepare case %s (id=%s)\r\n", testcase.getCaseName(), masterRequestId);
    }

    // Threadable interface
    public void run() {
        startExecution();
        try {
            for (TestFileLine cmd : testcase.getCommands().getLines()) {
                if (testcase.getStatus().equals(TestCase.STATUS_STOP)) return;
                if (testcase.getStatus().equals(TestCase.STATUS_ERROR)) {
                    if (!cmd.getCommand().equalsIgnoreCase(TestCase.VERIFY_PROCESS))
                        continue;
                }
                executeCommand(cmd);
            }
            finishExecution(null);
        } catch (Throwable e) {
            finishExecution(e);
        }
    }

    // Threadable interface
    public void stop() {
        testcase.setStatus(TestCase.STATUS_STOP);
    }

    public void startExecution() {
        passed = true;
        message = null;
        testcase.setStatus(TestCase.STATUS_RUNNING);
        testcase.setStartDate(new Date());
        log.format("===== execute case %s\r\n", testcase.getCaseName());
        for (File file : testcase.getResultDirectory().listFiles()) {
            if (file.getName().startsWith("R_")) {
                file.delete();
            }
        }
    }

    public void finishExecution(Throwable e) {
        if (isLoadTest) {
            if (e != null)
              e.printStackTrace(); // otherwise won't see errors
            if (log != System.out && log != System.err)
                log.close();
            return;
        }
        // function test only below
        if (e==null) {
        } else if (e instanceof TestException) {
            passed = false;
            message = firstLine(e.getMessage());
            log.println(message);
            e.printStackTrace(log);
        } else if (e instanceof ParseException) {
            passed = false;
            message = "Command syntax error at line "
                + ((ParseException)e).getErrorOffset() + ": " + e.getMessage();
            log.println(message);
            e.printStackTrace(log);
        } else {
            passed = false;
            message = firstLine(e.toString());
            if ("Assertion failed: ".equals(message))
                message += "See execution log for details.";
            log.println("Exception " + message);
            e.printStackTrace(log);
        }
        String status = testcase.getStatus();
        Date endDate = new Date();
        if (verbose) {
            long seconds = (endDate.getTime() - testcase.getStartDate().getTime()) / 1000;
            if (status.equals(TestCase.STATUS_ERROR))
                log.println("===== case " + testcase.getCaseName() + "Errored after " + seconds + " seconds");
            else if (status.equals(TestCase.STATUS_STOP))
                log.println("===== case " + testcase.getCaseName() + "Stopped after " + seconds + " seconds");
            else
                log.println("===== case " + testcase.getCaseName() + (passed?" Passed":" Failed") + " after " + seconds + " seconds");
        }
        if (log != System.out && log != System.err)
            log.close();
        if (isBatch) {
            System.out.println("===== case " + testcase.getCaseName() + " " + (passed?"Passed":"Failed"));
        }

        if (!status.equals(TestCase.STATUS_ERROR) && !status.equals(TestCase.STATUS_STOP)) {
            testcase.setEndDate(endDate);
            testcase.setStatus(passed?TestCase.STATUS_PASS:TestCase.STATUS_FAIL);
        }
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

    @Override
    public void executeCommand(TestFileLine line) throws Exception {
        String cmd = line.getCommand();
        if (cmd.equalsIgnoreCase(TestCase.VERIFY_PROCESS)) {
            if (!isLoadTest) executeAssertProcess(line);
        } else if (cmd.equalsIgnoreCase(TestCase.VERIFY_RESPONSE)) {
            if (!isLoadTest) executeAssertResponse(line);
        } else if (cmd.equalsIgnoreCase(TestCase.START)) {
            performStart(line);
        } else if (cmd.equalsIgnoreCase(MESSAGE)) {
            legacyPerformMessage(line);
        } else if (cmd.equalsIgnoreCase(TestCase.NOTIFY)) {
            performNotify(line);
        } else if (cmd.equalsIgnoreCase(TestCase.SIGNAL)) {
            performNotify(line);
        } else if (cmd.equalsIgnoreCase(TestCase.TASK)) {
            performTask(line);
        } else if (cmd.equalsIgnoreCase(TestCase.WAIT)) {
            if (oneThreadPerCase) performWait(line);
            else executeWaitTimeout(line);
        } else if (cmd.equalsIgnoreCase(TestCase.STUB)) {
            performStub(line);
        } else if (cmd.equalsIgnoreCase(TestCase.SLEEP)) {
            if (oneThreadPerCase) super.executeCommand(line);
            // else do nothing - already slept
        } else {
            if (!isLoadTest) super.executeCommand(line);
        }
        stepsCompleted++;
    }

    protected Map<String,String> getMessageHeaders() {
        if (getMasterRequestId() != null) {
            Map<String,String> headers = new HashMap<String,String>();
            headers.put("MasterRequestID", getMasterRequestId());
            return headers;
        }
        return null;
    }

    private void translateToLegacyTestFile(String procName, ProcessInstanceVO procInst, int instIndex, Map<Long,String> activityNameMap)
    throws IOException, DataAccessException {
        TestFile actualFile = new TestFile(null, testcase.getResultDirectory().getPath()+"/R_"+procName+"_I"+instIndex+".txt");
        TestFile expectedFileToCreate = null;
        if (createReplace) {
            expectedFileToCreate = new TestFile(null, testcase.getCaseDirectory().getPath()+"/E_"+procName+"_I"+instIndex+".txt");
            log.println("Creating expected results file: " + expectedFileToCreate);
        }
        TestFileLine line = new TestFileLine("PROC");
        line.addWord(Integer.toString(instIndex));
        line.addWord("#");
        line.addWord(procInst.getId().toString());
        actualFile.addLine(line);
        if (expectedFileToCreate != null)
            expectedFileToCreate.addLine(line);
        LinkedList<ActivityInstanceVO> reversedList = new LinkedList<ActivityInstanceVO>();
        for (ActivityInstanceVO act : procInst.getActivities()) {
            reversedList.add(0, act);
        }
        for (ActivityInstanceVO act : reversedList) {
            String status = WorkStatuses.getWorkStatuses().get(act.getStatusCode());
            String actName = activityNameMap.get(act.getDefinitionId());
            if (actName==null) actName = act.getDefinitionId().toString();
            line = new TestFileLine("ACT");
            line.addWord(actName);
            line.addWord(status);
            line.addWord("#");
            line.addWord(act.getId().toString());
            line.addWord(act.getStartDate().toString());
            actualFile.addLine(line);
            if (expectedFileToCreate != null)
                expectedFileToCreate.addLine(line);
        }
        for (VariableInstanceInfo var : procInst.getVariables()) {
            line = new TestFileLine("VAR");
            line.addWord(var.getName());
            try {
                String value = var.getStringValue();
                if (VariableHelper.isDocumentVariable(var.getType(),value)) {
                    DocumentReference docref = new DocumentReference(value);
                    String docVal = dao.getDocumentContent(docref, var.getType());
                    line.addWord(docVal);
                    procInst.getVariable().put(var.getName(), docVal); // pre-populate document values
                } else line.addWord(var.getStringValue());
            } catch (Throwable e) {
                log.println("Failed to translate variable to string with the following exception");
                e.printStackTrace(log);
                line.addWord(var.getStringValue());
            }
            line.addWord("#");
            line.addWord(var.getInstanceId().toString());
            actualFile.addLine(line);
            if (expectedFileToCreate != null)
                expectedFileToCreate.addLine(line);
        }
        actualFile.save();
        if (expectedFileToCreate != null)
            expectedFileToCreate.save();
    }

    private String translateToYaml(Map<String,List<ProcessInstanceVO>> processInstances, Map<String,String> activityNames, String newLineChars)
    throws IOException, DataAccessException {

        YamlBuilder yaml = new YamlBuilder(newLineChars);

        for (String procName : processInstances.keySet()) {
            List<ProcessInstanceVO> procInsts = processInstances.get(procName);
            for (int i = 0; i < procInsts.size(); i++) {
                ProcessInstanceVO procInst = procInsts.get(i);
                yaml.append("process: # ").append(procInst.getId()).newLine();
                yaml.append("  name: ").append(procName).newLine();
                yaml.append("  instance: ").append((i + 1)).newLine();
                LinkedList<ActivityInstanceVO> reversedList = new LinkedList<ActivityInstanceVO>();
                for (ActivityInstanceVO act : procInst.getActivities())
                    reversedList.add(0, act);
                for (ActivityInstanceVO act : reversedList) {
                    yaml.append("  activity: # ").append(act.getDefinitionId()).append(" \"").append(act.getStartDate()).append("\"").newLine();
                    String actNameKey = procInst.getProcessId() + "-" + act.getDefinitionId();
                    yaml.append("    name: " ).appendMulti("      ", activityNames.get(actNameKey)).newLine();
                    yaml.append("    status: ").append(WorkStatuses.getWorkStatuses().get(act.getStatusCode())).newLine();
                }
                for (VariableInstanceInfo var : procInst.getVariables()) {
                    yaml.append("  variable: # ").append(var.getInstanceId()).newLine();
                    yaml.append("    name: ").append(var.getName()).newLine();
                    yaml.append("    value: ");
                    try {
                        String val = var.getStringValue();
                        if (VariableHelper.isDocumentVariable(var.getType(), val)) {
                            DocumentReference docref = new DocumentReference(val);
                            val = dao.getDocumentContent(docref, var.getType());
                            procInst.getVariable().put(var.getName(), val); // pre-populate document values
                        }
                        yaml.appendMulti("      ", val).newLine();
                    }
                    catch (Throwable t) {
                        log.println("Failed to translate variable to string with the following exception");
                        t.printStackTrace(log);
                        yaml.append(" \"").append(var.getStringValue()).append("\"").newLine();
                    }
                }
            }
        }
        return yaml.toString();
    }

    protected List<ProcessInstanceVO> loadResults(List<ProcessVO> processes, TestCaseAsset expectedResults)
    throws DataAccessException, IOException {
        List<ProcessInstanceVO> mainProcessInsts = new ArrayList<ProcessInstanceVO>();
        Map<String,List<ProcessInstanceVO>> fullProcessInsts = new TreeMap<String,List<ProcessInstanceVO>>();
        Map<String,String> fullActivityNameMap = new HashMap<String,String>();
        for (ProcessVO proc : processes) {
            Map<String,String> criteria = new HashMap<String,String>();
            criteria.put("masterRequestId", masterRequestId);
            criteria.put("processId", proc.getId().toString());
            List<ProcessInstanceVO> procInstList = dao.getProcessInstanceList(criteria, 1, 100, proc, null).getItems();
            int n = procInstList.size();
            Map<Long,String> activityNameMap = new HashMap<Long,String>();
            for (ActivityVO act : proc.getActivities()) {
                activityNameMap.put(act.getActivityId(), act.getActivityName());
                fullActivityNameMap.put(proc.getId() + "-" + act.getActivityId(), act.getActivityName());
            }
            if (proc.getSubProcesses()!=null) {
                for (ProcessVO subproc : proc.getSubProcesses()) {
                    for (ActivityVO act : subproc.getActivities()) {
                        activityNameMap.put(act.getActivityId(), act.getActivityName());
                        fullActivityNameMap.put(proc.getId() + "-" + act.getActivityId(), act.getActivityName());
                    }
                }
            }
            for (ProcessInstanceVO procInst : procInstList) {
                procInst = dao.getProcessInstanceAll(procInst.getId(), proc);
                mainProcessInsts.add(procInst);
                if (getTestCase().isLegacy()) {
                    translateToLegacyTestFile(proc.getProcessName(), procInst, n, activityNameMap);
                }
                else {
                    List<ProcessInstanceVO> procInsts = fullProcessInsts.get(proc.getName());
                    if (procInsts == null)
                        procInsts = new ArrayList<ProcessInstanceVO>();
                    procInsts.add(procInst);
                    fullProcessInsts.put(proc.getName(), procInsts);
                }
                if (proc.getSubProcesses()!=null) {
                    criteria.clear();
                    if (proc.isInRuleSet()) {
                        criteria.put("owner", OwnerType.MAIN_PROCESS_INSTANCE);
                        criteria.put("ownerId", procInst.getId().toString());
                        criteria.put("processId", proc.getProcessId().toString());
                        List<ProcessInstanceVO> embeddedProcInstList =
                            dao.getProcessInstanceList(criteria, 0, QueryRequest.ALL_ROWS, proc, null).getItems();
                        int m = embeddedProcInstList.size();
                        for (ProcessInstanceVO embeddedProcInst : embeddedProcInstList) {
                            ProcessInstanceVO fullChildInfo = dao.getProcessInstanceAll(embeddedProcInst.getId(),proc);
                            String childProcName = "unknown_subproc_name";
                            for (ProcessVO subproc : proc.getSubProcesses()) {
                                if (subproc.getProcessId().toString().equals(embeddedProcInst.getComment())) {
                                    childProcName = subproc.getProcessName();
                                    if (!childProcName.startsWith(proc.getProcessName()))
                                        childProcName = proc.getProcessName() + " " + childProcName;
                                    break;
                                }
                            }
                            if (getTestCase().isLegacy()) {
                                translateToLegacyTestFile(childProcName, fullChildInfo, m, activityNameMap);
                            }
                            else {
                                List<ProcessInstanceVO> procInsts = fullProcessInsts.get(childProcName);
                                if (procInsts == null)
                                    procInsts = new ArrayList<ProcessInstanceVO>();
                                procInsts.add(fullChildInfo);
                                fullProcessInsts.put(childProcName, procInsts);
                            }
                            m--;
                        }
                    } else {
                        StringBuffer sb = new StringBuffer();
                        sb.append("(");
                        for (ProcessVO subproc : proc.getSubProcesses()) {
                            if (sb.length()>1) sb.append(",");
                            sb.append(subproc.getProcessId());
                        }
                        sb.append(")");
                        criteria.put("owner", OwnerType.PROCESS_INSTANCE);
                        criteria.put("ownerId", procInst.getId().toString());
                        criteria.put("processIdList", sb.toString());
                        List<ProcessInstanceVO> embeddedProcInstList =
                            dao.getProcessInstanceList(criteria, 0, QueryRequest.ALL_ROWS, proc, null).getItems();
                        int m = embeddedProcInstList.size();
                        for (ProcessInstanceVO embeddedProcInst : embeddedProcInstList) {
                            ProcessInstanceVO fullChildInfo =
                                dao.getProcessInstanceAll(embeddedProcInst.getId(),proc);
                            String childProcName = "unknown_subproc_name";
                            for (ProcessVO subproc : proc.getSubProcesses()) {
                                if (subproc.getProcessId().equals(embeddedProcInst.getProcessId())) {
                                    childProcName = subproc.getProcessName();
                                    if (!childProcName.startsWith(proc.getProcessName()))
                                        childProcName = proc.getProcessName() + " " + childProcName;
                                    break;
                                }
                            }
                            if (getTestCase().isLegacy()) {
                                translateToLegacyTestFile(childProcName, fullChildInfo, m, activityNameMap);
                            }
                            m--;
                        }
                    }
                }
                n--;
            }
        }
        if (!getTestCase().isLegacy()) {
            String newLine = "\n";
            if (!createReplace) {
                // try to determine newline chars from expectedResultsFile
                if (expectedResults.exists()) {
                    if (expectedResults.text().indexOf("\r\n") >= 0)
                        newLine = "\r\n";
                }
            }
            String yaml = translateToYaml(fullProcessInsts, fullActivityNameMap, newLine);
            if (createReplace) {
                log.println("creating expected results: " + expectedResults);
                FileHelper.writeToFile(expectedResults.toString(), yaml, false);
            }
            String fileName = testcase.getResultDirectory() + "/" + expectedResults.getName();
            if (verbose)
                log.println("creating actual results file: " + fileName);
            FileHelper.writeToFile(fileName, yaml, false);
        }
        // set friendly statuses
        if (mainProcessInsts != null) {
            for (ProcessInstanceVO pi : mainProcessInsts)
                pi.setStatus(WorkStatuses.getWorkStatuses().get(pi.getStatusCode()));
        }
        return mainProcessInsts;
    }

    private void executeAssertProcess(TestFileLine line)
    throws TestException,IOException,DataAccessException,ParseException {
        if (!passed) return;
        log_command(line);
        List<ProcessVO> processes = new ArrayList<ProcessVO>();
        for (int i=1; i<line.getWordCount(); i++) {
            try {
                processes.add(getProcess(line, line.getWord(i)));
            } catch (Exception e) {
                log.println("Failed to load process " + line.getWord(i));
            }
        }
        loadResults(processes, null);
        legacyVerifyProcesses();
    }

    /**
     * Not called for legacy cases -- see legacyVerifyProcesses().
     */
    protected boolean verifyProcesses(TestCaseAsset resultsAsset)
    throws TestException, IOException, DataAccessException, ParseException {
        if (!resultsAsset.exists()) {
            message = "Expected results not found: " + resultsAsset;
            log.println("+++++ " + message);
            passed = false;
        }
        else {
            TestCaseFile actualResultsFile = new TestCaseFile(testcase.getResultDirectory() + "/" + resultsAsset.getName());
            if (verbose)
                log.println("... compare " + resultsAsset + " with " + actualResultsFile + "\r\n");
            if (!actualResultsFile.exists()) {
                message = "Actual results not found: " + actualResultsFile;
                log.println("+++++ " + message);
                passed = false;
            }
            else {
                if (verbose) {
                    log.println("expected:");
                    log.println(resultsAsset.getText());
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

    protected boolean legacyVerifyProcesses()
    throws TestException, IOException, DataAccessException, ParseException {
        if (createReplace) {
            passed = true;
        } else {
            File[] files = testcase.getCaseDirectory().listFiles();
            passed = true;
            Map<String,String> map = new HashMap<String,String>();
            map.put(TestDataFilter.MasterRequestId, this.masterRequestId);
            map.put(TestDataFilter.RunNumber, Integer.toString(runNumber));
            for (int i=0; i<files.length; i++) {
                String fileName = files[i].getName();
                if (fileName.startsWith("E_")) {
                    String resultFileName = "R_"+fileName.substring(2);
                    if (verbose)
                        log.format("... compare %s with %s\r\n", fileName, resultFileName);
                    TestFile expectedOne = new TestFile(testcase.getCaseDirectory(), fileName);
                    TestFile resultOne = new TestFile(testcase.getResultDirectory(), resultFileName);
                    String expected = expectedOne.load(getPreFilter());
                    String actual = resultOne.load(null);
                    if (verbose) {
                        log.println("expected:");
                        log.println(expected);
                        log.println("actual:");
                        log.println(actual);
                    }
                    int diff = resultOne.firstDiffLine(expectedOne, map, null, log);    // TODO allow reference doc
                    if (diff>=0) {
                        passed = false;
                        log.format("+++++ %s: differ from line %d\r\n", fileName.substring(2), diff);
                        message = fileName.substring(2) + ": differ from line " + diff;
                    }
                }
            }
            files = testcase.getResultDirectory().listFiles();
            for (int i=0; i<files.length; i++) {
                String fileName = files[i].getName();
                if (fileName.startsWith("R_")) {
                    File expectedFile = new File(testcase.getCaseDirectory().getPath()+"/E_"+fileName.substring(2));
                    if (!expectedFile.exists()) {
                        passed = false;
                        log.format("+++++ %s: expected result file does not exist\r\n", fileName.substring(2));
                        message = fileName.substring(2) + ": expected result file does not exist";
                    }
                }
            }
        }
        return passed;
    }

    private void executeAssertResponse(TestFileLine line)
    throws TestException,IOException,DataAccessException,ParseException {
        if (!passed)
            return;
        log_command(line);
        String expected = line.getWord(1);
        String actual = response;
        if (verbose) {
            log.println("actual response:");
            log.println(actual);
        }
        String reference = line.getWordCount() >= 3 ? line.getWord(2) : null;
        executeVerifyResponse(expected, actual, reference);
    }

    protected boolean executeVerifyResponse(String expected, String actual, String reference)
    throws TestException,IOException,DataAccessException,ParseException {
        expected = expected.replaceAll("\r", "");
        if (verbose)
            log.println("comparing response...");
        if (actual != null) {
            actual = actual.replaceAll("\r", "");
            Map<String,String> map = new HashMap<String,String>();
            map.put(TestDataFilter.MasterRequestId, this.masterRequestId);
            map.put(TestDataFilter.RunNumber, Integer.toString(runNumber));
            TestDataFilter filter = new TestDataFilter(expected, log, true);
            MbengDocument refdoc;
            if (reference != null) {
                try {
                    refdoc = TestDataFilter.parseRequest(reference);
                } catch (MbengException e) {
                    log.println("Failed to parse reference document as XML/JSON: " + e.getMessage());
                    refdoc = null;
                }
            } else refdoc = null;
            expected = filter.applyFilters(map, refdoc);
            expected = filter.applyAnyNumberFilters(expected, actual);
        }
        if (!(expected.equals(actual) || TestCompare.matchRegex(expected, actual))) {
            passed = false;
            log.format("+++++ response is different\r\n");
            if (!verbose)  // otherwise it was logged previously
              log.format("Actual response: %s\r\n", actual);
            message = "response is different";
        }
        return passed;
    }

    private ProcessVO getProcess(TestFileLine line, String word) throws TestException {
        ProcessVO vo = processCache.get(word);
        if (vo==null) {
            try {
                String procName = parseProcessName(word);
                int version = parseProcessVersion(word);
                vo = dao.getProcessDefinition(procName, version);
                vo = dao.getProcess(vo.getProcessId(), vo);
                processCache.put(word, vo);
            } catch (Exception e) {
                throw new TestException(line, e.getMessage());
            }
        }
        return vo;
    }

    private Long getActivityId(ProcessVO vo, String activityLogicalId) {
        for (ActivityVO act : vo.getActivities()) {
            String lid = act.getLogicalId();
            if (activityLogicalId.equals(lid)) {
                return act.getActivityId();
            }
        }
        return null;
    }

    private String getWaitKey(TestFileLine line, String masterRequestId) throws TestException {
        ProcessVO vo = getProcess(line, line.getWord(1));
        Long activityId;
        String status;
        if (line.getWordCount()==5) {        // wait process_name act_logical_id status timeout
            activityId = getActivityId(vo, line.getWord(2));
            status = line.getWord(3);
        } else if (line.getWordCount()==4) {        // wait process_name act_logical_id timeout
            activityId = getActivityId(vo, line.getWord(2));
            status = WorkStatus.STATUSNAME_COMPLETED;
        } else {            // wait process_name timeout
            activityId = 0L;
            status = WorkStatus.STATUSNAME_COMPLETED;
        }
        if (activityId==null) throw new TestException(line,
                "Cannot find activity with logical id " + line.getWord(2));
        return monitor.createKey(masterRequestId, vo.getProcessId(), activityId, status);
    }

    protected String getWaitKey(ProcessVO process, String activityLogicalId, String status) throws TestException {
        Long activityId = 0L;
        if (activityLogicalId != null)
            activityId = getActivityId(process, activityLogicalId);
        if (status == null)
            status = WorkStatus.STATUSNAME_COMPLETED;
        return monitor.createKey(masterRequestId, process.getId(), activityId, status);
    }

    public int executeWaitRegister(TestCommandRun event) throws TestException {
        TestFileLine line = event.getCommandLine();
        String key = getWaitKey(line, event.getMasterRequestId());
        int timeout = Integer.parseInt(line.getWord(line.getWordCount()-1));
        if (verbose) log.println("Wait key: " + key);
        monitor.register(event, key);
        return timeout;
    }

    public void executeWaitTimeout(TestFileLine line) throws TestException {
        log_command(line);
        String key = getWaitKey(line, masterRequestId);
        Object stillthere = monitor.remove(key);
        if (stillthere!=null) {
            log.println("Wait command times out");
            testcase.setStatus(TestCase.STATUS_ERROR);
        } else {
            try {
                Thread.sleep(2000);        // just to make sure the database info is committed
            } catch (InterruptedException e) {
            }
        }
    }

    private void performWait(TestFileLine line) throws TestException {
        log_command(line);
        String key = getWaitKey(line, masterRequestId);
        int timeout = Integer.parseInt(line.getWord(line.getWordCount()-1));
        performWait(key, timeout);
    }

    protected void performWait(String key, int timeout) throws TestException {
        try {
            synchronized (this) {
                monitor.register(this, key);
                this.wait(timeout*1000);
            }
            Object stillthere = monitor.remove(key);
            if (stillthere != null) {
                log.println("wait command times out after: " + timeout + "s");
            } else {
                Thread.sleep(2000);        // to get around race condition
                if (verbose)
                    log.println("wait command satisfied: " + key);
            }
        } catch (InterruptedException e) {
            log.println("Wait gets interrupted");
            testcase.setStatus(TestCase.STATUS_ERROR);
        }
    }

    public void performStub(TestFileLine line) {
        // do nothing - handled by the method getStubResponse() below which
        // is invoked from stub server.
    }

    public String getStubResponse(String masterRequestId, String request, int run) {

        if (request != null && request.startsWith("{\"ActivityRuntimeContext\":"))
            return "(EXECUTE_ACTIVITY)"; // no activity stubbing in old-style test syntax

        MbengDocument reqdoc;
        if (verbose) log.println("Stub request: " + request);
        try {
            reqdoc = TestDataFilter.parseRequest(request);
        } catch (MbengException e1) {
            log.println("Failed to parse request as XML/JSON. Stub response: "
                    + AdapterActivity.MAKE_ACTUAL_CALL);
            return AdapterActivity.MAKE_ACTUAL_CALL;
        }
        for (TestFileLine cmd : testcase.getCommands().getLines()) {
            if (cmd.getCommand().equalsIgnoreCase(TestCase.STUB)) {
                String condition = cmd.getWord(1);
                if (stubMatch(condition, reqdoc)) {
                    int delay;
                    String response;
                    if (cmd.getWordCount()==4) {
                        delay = Integer.parseInt(cmd.getWord(2));
                        response = cmd.getWord(3);
                    } else {
                        response = cmd.getWord(2);
                        delay = 0;
                    }
                    response = stubResponseFilter(response, masterRequestId, reqdoc, run);
                    response = "RESPONSE~" + delay + "~" + response;
                    if (verbose) log.println("Stub response: " + response);
                    return response;
                }
            }
        }
        if (verbose) log.println("Stub response: " + AdapterActivity.MAKE_ACTUAL_CALL);
        return AdapterActivity.MAKE_ACTUAL_CALL;
    }

    private class MyRuleSet extends MbengRuleSet {
        MyRuleSet(String name, char type) throws MbengException {
            super(name, type, true, false);
        }

        @Override
        protected boolean isSectionName(String name) {
            return true;
        }
    }

    protected boolean stubMatch(String condition, MbengDocument reqdoc) {
        try {
            if (condition.contains("$")) {
                MbengRuleSet ruleset = new MyRuleSet("stub_condition", MbengRuleSet.RULESET_COND);
                ruleset.parse(condition);
                MbengRuntime runtime = new MbengRuntime(ruleset, new StreamLogger(System.out));
                runtime.bind("$", reqdoc);
                return runtime.verify();
            } else {
                XmlPath xpath = new XmlPath(condition);
                String v = xpath.evaluate(reqdoc);
                return v!=null;
            }
        } catch (MbengException e) {
            log.println("Exception in evaluating condition in stub: " + e.getMessage());
            return false;
        }
    }

    private String stubResponseFilter(String response, String masterRequestId,
            MbengDocument reqdoc, int run) {
        TestDataFilter filter = new TestDataFilter(response, log, true);
        HashMap<String,String> parameters = new HashMap<String,String>();
        parameters.put(TestDataFilter.MasterRequestId, masterRequestId);
        parameters.put(TestDataFilter.RunNumber, Integer.toString(runNumber));
        File mapfile = new File(testcase.getCaseDirectory().getPath()+"/"+TestCase.PLACEHOLDER_MAP_FILENAME);
        try {
            TestDataFilter.loadPlaceHolderMap(mapfile, parameters, run);
        } catch (IOException e) {
            log.println("Failed to load place holder map: " + e.getMessage());
        }
        return filter.applyFilters(parameters, reqdoc);
    }

    protected String getNextServer() {
        if (singleServer)
            return null;
        String ret = serverList.get(nextServer);
        nextServer++;
        if (nextServer >= serverList.size())
            nextServer = 0;
        return ret;
    }

    public void performStart(TestFileLine line) throws TestException
    {
        log_command(line);
        ProcessVO vo = getProcess(line, line.getWord(1));
        Map<String,String> params = new HashMap<String,String>();
        try {
            for (int i=2; i<line.getWordCount(); i++) {
                int k = line.getWord(i).indexOf('=');
                params.put(line.getWord(i).substring(0,k),
                        getParameterValue(line.getWord(i).substring(k+1)));
            }
            String server = getNextServer();
            if (server==null) dao.launchProcess(vo, masterRequestId, null, params, false, oldNamespaces);
            else {
                String request = dao.getCurrentServer().buildLaunchProcessRequest(vo,
                    masterRequestId, null, params, false, oldNamespaces);
                dao.engineCall(dao.getPeerServerUrl(server), request);
            }
        } catch (Exception e) {
            throw new TestException(line, "failed to launch process", e);
        }
    }

    @Override
    public void legacyPerformMessage(TestFileLine cmd) throws TestException {
        log_command(cmd);
        try {
            String protocol = cmd.getWord(1);
            String message = cmd.getWord(2);
            String reference = cmd.getWordCount() >= 4 ? cmd.getWord(3) : null;
            response = sendMessage(null, protocol, message, reference);
            log.println("Response: " + response);
        } catch (Exception e) {
            throw new TestException(cmd, "failed to send message", e);
        }
    }

    protected String sendMessage(TestCaseAsset testCaseAsset, String protocol, String message, String reference) throws IOException, DataAccessException {
        TestDataFilter filter = new TestDataFilter(message, log, true);
        Map<String,String> map = new HashMap<String,String>();
        map.put(TestDataFilter.MasterRequestId, this.masterRequestId);
        map.put(TestDataFilter.RunNumber, Integer.toString(runNumber));
        MbengDocument refdoc;
        if (reference != null) {
            try {
                refdoc = TestDataFilter.parseRequest(reference);
            } catch (MbengException e) {
                log.println("Failed to parse reference document as XML/JSON: " + e.getMessage());
                refdoc = null;
            }
        } else refdoc = null;
        // TODO: XLSX asset instead of CSV
        File mapfile;
        if (testcase.isLegacy())
            mapfile = new File(testcase.getCaseDirectory().getPath() + "/" + TestCase.PLACEHOLDER_MAP_FILENAME);
        else
            mapfile = new File(testCaseAsset.getPackageDir() + "/" + TestCase.PLACEHOLDER_MAP_FILENAME);
        TestDataFilter.loadPlaceHolderMap(mapfile, map, runNumber);
        message = filter.applyFilters(map, refdoc);
        String server = getNextServer();
        if (server==null)
            return dao.sendMessage(protocol, message, getMessageHeaders());
        else
            return dao.engineCall(dao.getPeerServerUrl(server), message, getMessageHeaders());
    }

    private void addActionParameter(com.centurylink.mdw.service.Action action, String name, String value) {
        Parameter param = action.addNewParameter();
        param.setName(name);
        param.setStringValue(value);
    }

    public void performNotify(TestFileLine cmd) throws TestException {
        log_command(cmd);
        String eventName = cmd.getWord(1);
        String message = cmd.getWord(2);
        String procName = cmd.getWordCount()>3?cmd.getWord(3):null;
        String actLogicalId = cmd.getWordCount()>4?cmd.getWord(4):null;
        performNotify(eventName, message, procName, actLogicalId);
    }

    public void performNotify(String eventName, String message, String procName, String actLogicalId) throws TestException {
        try {
            TestDataFilter filter = new TestDataFilter(message, log, true);
            Map<String,String> map = new HashMap<String,String>();
            map.put(TestDataFilter.MasterRequestId, this.masterRequestId);
            map.put(TestDataFilter.RunNumber, Integer.toString(runNumber));
            // FIXME: placeholders for non-legacy
            if (testcase.isLegacy()) {
                File mapfile = new File(testcase.getCaseDirectory().getPath() + "/" + TestCase.PLACEHOLDER_MAP_FILENAME);
                TestDataFilter.loadPlaceHolderMap(mapfile, map, runNumber);
                message = filter.applyFilters(map, null);
            }

            ActionRequestDocument msgdoc = ActionRequestDocument.Factory.newInstance();
            ActionRequest actionRequest = msgdoc.addNewActionRequest();
            com.centurylink.mdw.service.Action act = actionRequest.addNewAction();
            act.setName("RegressionTest");
            addActionParameter(act, "SubAction", "NotifyProcess");
            addActionParameter(act, "EventName", eventName);
            addActionParameter(act, "Message", message);
            addActionParameter(act, "MasterRequestId", masterRequestId);
            if (procName!=null)
                addActionParameter(act, "ProcessName", procName);
            if (actLogicalId!=null)
                addActionParameter(act, "ActivityLogicalId", actLogicalId);
            String request;
            if (oldNamespaces)
                request = DesignerCompatibility.getInstance().getOldActionRequest(msgdoc);
            else
                request = msgdoc.xmlText();
            String server = getNextServer();
            if (server==null)
                response = dao.sendMessage("DefaultProtocol", request, getMessageHeaders());
            else
                response = dao.engineCall(dao.getPeerServerUrl(server), request);
            log.println("Response: " + response);
        } catch (Exception e) {
            throw new TestException(e.getMessage(), e);
        }
    }

    private String getParameterValue(String word) throws IOException {
        if (word.startsWith("@"))
            word = testcase.readParameterValueFromFile(word.substring(1));
        return word;
    }

    /**
     * action can be standard ones specified in TaskAction.java, such as "Claim",
     * custom actions defined through process definitions,
     * or general task action which is identified by "Action/" + command, such as
     * "Action/@START_PROCESS?PROCESS_NAME=MyServiceProcess"
     * @param line
     * @throws TestException
     */
    public void performTask(TestFileLine line) throws TestException {
        log_command(line);
        String taskName = line.getWord(1);
        String action = line.getWord(2);
        Map<String,String> params = new HashMap<String,String>();
        try {
            for (int i=3; i<line.getWordCount(); i++) {
                int k = line.getWord(i).indexOf('=');
                params.put(line.getWord(i).substring(0,k),
                        getParameterValue(line.getWord(i).substring(k+1)));
            }
            performTask(taskName, action, params);
        } catch (IOException e) {
            throw new TestException(line, "failed to perform task action", e);
        }
    }

    public JSONObject performTask(String taskName, String action, Map<String,String> variables) throws TestException {
        try {
            ActionRequestDocument msgdoc = ActionRequestDocument.Factory.newInstance();
            ActionRequest actionRequest = msgdoc.addNewActionRequest();
            com.centurylink.mdw.service.Action act = actionRequest.addNewAction();
            act.setName("RegressionTest");
            addActionParameter(act, "SubAction", "TaskAction");
            addActionParameter(act, "TaskName", taskName);
            addActionParameter(act, "MasterRequestId", masterRequestId);
            addActionParameter(act, "User", dao.getCuid());
            if (action.startsWith("Action/")) {
                addActionParameter(act, "FormAction", action.substring("Action/".length()));
            } else {
                addActionParameter(act, "DirectAction", action);
            }
            for (String dataname : variables.keySet()) {
                addActionParameter(act, "formdata." + dataname, variables.get(dataname));
            }
            String request;
            if (oldNamespaces)
                request = DesignerCompatibility.getInstance().getOldActionRequest(msgdoc);
            else
                request = msgdoc.xmlText();
            String server = getNextServer();
            String response;
            if (server==null)
                response = dao.engineCall(request);
            else
                response = dao.engineCall(dao.getPeerServerUrl(server), request);

            return validateEngineCallResponse(response);


        } catch (Exception e) {
            throw new TestException(e.getMessage(), e);
        }
    }

    protected JSONObject validateEngineCallResponse(String response) throws ValidationException, XmlException, JSONException {
        MDWStatusMessageDocument statusMessageDoc;
        if (response.startsWith("{")) {
            StatusMessage statusMessage = new StatusMessage(new JSONObject(response));
            statusMessageDoc = statusMessage.getStatusDocument();
        }
        else {
            statusMessageDoc = MDWStatusMessageDocument.Factory.parse(response, Compatibility.namespaceOptions());
        }
        MDWStatusMessage statusMessage = statusMessageDoc.getMDWStatusMessage();
        if (statusMessage.getStatusCode() == -3) {
            // event handler not registered
            throw new ValidationException("No event handler is registered for regression test actions");
        }
        else if (statusMessage.getStatusCode() != 0) {
            throw new ValidationException("Error response from server: " + statusMessage.getStatusMessage());
        }
        if (statusMessage.getStatusMessage().startsWith("{"))
            return new JSONObject(statusMessage.getStatusMessage());
        else
            return null;
    }
}
