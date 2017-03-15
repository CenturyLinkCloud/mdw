/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.model.workflow.ProcessRun;
import com.centurylink.mdw.model.workflow.WorkStatuses;
import com.centurylink.mdw.task.types.TaskList;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseProcess;
import com.centurylink.mdw.test.TestCaseTask;
import com.centurylink.mdw.test.TestException;
import com.centurylink.mdw.test.TestExecConfig;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.file.FileHelper;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;

/**
 * @author dxoakes
 *
 */
public class StandaloneTestCaseRun extends TestCaseRun {

    public StandaloneTestCaseRun(TestCase testCase, String user, File mainResultsDir, int run,
            String masterRequestId, LogMessageMonitor monitor, Map<String, Process> processCache,
            TestExecConfig config) throws IOException {
        super(testCase, user, mainResultsDir, run, masterRequestId, monitor, processCache, config);
        // TODO Auto-generated constructor stub
    }

    /**
     * Standalone execution for Designer and Gradle.
     */
    public void run() {
        startExecution();

        CompilerConfiguration compilerConfig = new CompilerConfiguration(System.getProperties());
        compilerConfig.setScriptBaseClass(TestCaseScript.class.getName());
        Binding binding = new Binding();
        binding.setVariable("testCaseRun", this);

        ClassLoader classLoader = this.getClass().getClassLoader();
        GroovyShell shell = new GroovyShell(classLoader, binding, compilerConfig);
        shell.setProperty("out", getLog());
        setupContextClassLoader(shell);
        try {
            shell.run(new GroovyCodeSource(getTestCase().file()), new String[0]);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    void startProcess(TestCaseProcess process) throws TestException {
        if (isVerbose())
            getLog().println("starting process " + process.getLabel() + "...");
        this.testCaseProcess = process;
        try {
            Process proc = process.getProcess();
            Map<String,String> params = process.getParams();
            HttpHelper httpHelper = getHttpHelper("services/Processes/run/" + proc.getId() + "?app=autotest");
            ProcessRun run = new ProcessRun();
            run.setDefinitionId(proc.getId());
            run.setMasterRequestId(getMasterRequestId());
            run.setOwnerType(OwnerType.TESTER);
            run.setOwnerId(0L);
            if (params != null && !params.isEmpty()) {
                Map<String,Value> values = new HashMap<>();
                for (String name : params.keySet())
                    values.put(name, new Value(name, params.get(name)));
                run.setValues(values);
            }
            String response = httpHelper.post(run.getJson().toString(2));
            run = new ProcessRun(new JSONObject(response));
            if (run.getInstanceId() == null)
                throw new TestException("Failed to start " + process.getLabel());
            else
                getLog().println(process.getLabel() + " instance " + run.getInstanceId() + " started");
        }
        catch (Exception ex) {
            throw new TestException("Failed to start " + process.getLabel(), ex);
        }
    }

    protected List<ProcessInstance> loadResults(List<Process> processes, Asset expectedResults, boolean orderById)
    throws DataAccessException, IOException, ServiceException, JSONException, ParseException {
        List<ProcessInstance> mainProcessInsts = new ArrayList<ProcessInstance>();
        Map<String,List<ProcessInstance>> fullProcessInsts = new TreeMap<String,List<ProcessInstance>>();
        Map<String,String> fullActivityNameMap = new HashMap<String,String>();
        for (Process proc : processes) {
            Query query = new Query();
            query.setFilter("masterRequestId", getMasterRequestId());
            query.setFilter("processId", proc.getId().toString());
            query.setDescending(true);
            query.setFilter("app", "autotest");
            HttpHelper httpHelper = getHttpHelper("services/Processes?" + query);
            String response = httpHelper.get();
            List<ProcessInstance> procInstList = new ProcessList(ProcessList.PROCESS_INSTANCES, new JSONObject(response)).getProcesses();
            Map<Long,String> activityNameMap = new HashMap<Long,String>();
            for (Activity act : proc.getActivities()) {
                activityNameMap.put(act.getActivityId(), act.getActivityName());
                fullActivityNameMap.put(proc.getId() + "-" + act.getActivityId(), act.getActivityName());
            }
            if (proc.getSubProcesses() != null) {
                for (Process subproc : proc.getSubProcesses()) {
                    for (Activity act : subproc.getActivities()) {
                        activityNameMap.put(act.getActivityId(), act.getActivityName());
                        fullActivityNameMap.put(proc.getId() + "-" + act.getActivityId(), act.getActivityName());
                    }
                }
            }
            for (ProcessInstance procInst : procInstList) {
                httpHelper = getHttpHelper("services/Processes/" + procInst.getId() + "?app=autotest");
                procInst = new ProcessInstance(new JSONObject(httpHelper.get()));
                mainProcessInsts.add(procInst);
                List<ProcessInstance> procInsts = fullProcessInsts.get(proc.getName());
                if (procInsts == null)
                    procInsts = new ArrayList<ProcessInstance>();
                procInsts.add(procInst);
                fullProcessInsts.put(proc.getName(), procInsts);
                if (proc.getSubProcesses() != null) {
                    Query q = new Query();
                    q.setFilter("owner", OwnerType.MAIN_PROCESS_INSTANCE);
                    q.setFilter("ownerId", procInst.getId().toString());
                    q.setFilter("processId", proc.getProcessId().toString());
                    List<ProcessInstance> embeddedProcInstList;
                    q.setFilter("app", "autotest");
                    httpHelper = getHttpHelper("services/Processes?" + q);
                    response = httpHelper.get();
                    embeddedProcInstList = new ProcessList(ProcessList.PROCESS_INSTANCES, new JSONObject(response)).getProcesses();
                    for (ProcessInstance embeddedProcInst : embeddedProcInstList) {
                        httpHelper = getHttpHelper("services/Processes/" + embeddedProcInst.getId() + "?app=autotest");
                        ProcessInstance fullChildInfo = new ProcessInstance(new JSONObject(httpHelper.get()));
                        String childProcName = "unknown_subproc_name";
                        for (Process subproc : proc.getSubProcesses()) {
                            if (subproc.getProcessId().toString().equals(embeddedProcInst.getComment())) {
                                childProcName = subproc.getProcessName();
                                if (!childProcName.startsWith(proc.getProcessName()))
                                    childProcName = proc.getProcessName() + " " + childProcName;
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
                throw new IOException("Expected results file not found for " + getTestCase().getPath());
            }
            else {
                // try to determine newline chars from expectedResultsFile
                if (expectedResults.getStringContent().indexOf("\r\n") >= 0)
                    newLine = "\r\n";
            }
        }
        String yaml = translateToYaml(fullProcessInsts, fullActivityNameMap, orderById, newLine);
        if (isCreateReplace()) {
            getLog().println("creating expected results: " + expectedResults.getRawFile());
            FileHelper.writeToFile(expectedResults.getRawFile().toString(), yaml, false);
            expectedResults.setStringContent(yaml);
        }
        String fileName = getResultsDir() + "/" + expectedResults.getName();
        if (isVerbose())
            getLog().println("creating actual results file: " + fileName);
        FileHelper.writeToFile(fileName, yaml, false);
        // set friendly statuses
        if (mainProcessInsts != null) {
            for (ProcessInstance pi : mainProcessInsts)
                pi.setStatus(WorkStatuses.getWorkStatuses().get(pi.getStatusCode()));
        }
        return mainProcessInsts;
    }

    Process getProcess(String target) throws TestException {
        target = qualify(target);
        Process process = getProcessCache().get(target);
        if (process == null) {
            try {
                Query query = getProcessQuery(target);
                query.setFilter("summary", true);
                query.setFilter("app", "autotest");
                HttpHelper httpHelper = getHttpHelper("services/Workflow/" + query);
                String response = httpHelper.get();
                JSONObject json = new JSONObject(response);
                process = new Process(json);
                if (json.has("id"))
                    process.setId(json.getLong("id"));
                if (json.has("package"))
                    process.setPackageName(json.getString("package"));
                getProcessCache().put(target, process);
            }
            catch (Exception ex) {
                throw new TestException("Cannot load " + target, ex);
            }
        }
        return process;
    }

    void performTaskAction(TestCaseTask task) throws TestException {
        if (isVerbose())
            getLog().println("performing " + task.getOutcome() + " on task '" + task.getName() + "'");

        try {
            Query query = getTaskQuery(task.getName());
            HttpHelper httpHelper = getHttpHelper("services/Tasks/" + query);
            String response = httpHelper.get();
            TaskList taskList = new TaskList(TaskList.TASKS, new JSONObject(response));
            List<TaskInstance> taskInstances = taskList.getTasks();
            if (taskInstances.isEmpty())
                throw new TestException("Cannot find task instances: " + query);

            TaskInstance taskInstance = taskInstances.get(0); // latest
            task.setId(taskInstance.getTaskInstanceId());

            UserTaskAction taskAction = new UserTaskAction();
            taskAction.setAction(task.getOutcome());
            taskAction.setUser(getUser());
            taskAction.setTaskInstanceId(task.getId());
            if (task.getVariables() != null) {
                JSONObject valuesJson = new JSONObject();
                for (String name : task.getVariables().keySet())
                    valuesJson.put(name, new Value(name, task.getVariables().get(name).toString()).getJson());
                httpHelper = getHttpHelper("services/Workflow/" + taskInstance.getOwnerId() + "/values?app=autotest");
                response = httpHelper.put(valuesJson.toString(2));
                StatusMessage statusMsg = new StatusMessage(new JSONObject(response));
                if (!statusMsg.isSuccess())
                    throw new TestException("Error updating task values: " + statusMsg.getMessage());
            }
            httpHelper = getHttpHelper("services/Tasks/" + task.getOutcome() + "?app=autotest");
            response = httpHelper.post(taskAction.getJson().toString(2));
            StatusMessage statusMsg = new StatusMessage(new JSONObject(response));
            if (!statusMsg.isSuccess())
                throw new TestException("Error actioning task: " + task.getId() + ": " + statusMsg.getMessage());
        }
        catch (TestException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new TestException("Error actioning task: " + task.getId(), ex);
        }
    }

    protected HttpHelper getHttpHelper(String endpoint) throws MalformedURLException {
        String url = getConfig().getServerUrl();
        if (!endpoint.startsWith("/"))
            endpoint = "/" + endpoint;
        HttpHelper helper = new HttpHelper(new URL(url + endpoint));
        helper.setHeader("Content-Type", "application/json");
        return helper;
    }

    // GROOVY-6771
    @SuppressWarnings("unchecked")
    private static void setupContextClassLoader(GroovyShell shell) {
        final Thread current = Thread.currentThread();
        @SuppressWarnings("rawtypes")
        class DoSetContext implements PrivilegedAction {
            ClassLoader classLoader;
            public DoSetContext(ClassLoader loader) {
                classLoader = loader;
            }
            public Object run() {
                current.setContextClassLoader(classLoader);
                return null;
            }
        }
        AccessController.doPrivileged(new DoSetContext(shell.getClassLoader()));
    }
}
