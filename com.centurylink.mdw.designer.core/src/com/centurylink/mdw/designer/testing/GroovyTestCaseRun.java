/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.xmlbeans.XmlException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.dataaccess.file.AssetFile;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.activity.ActivityStubRequest;
import com.centurylink.mdw.model.value.activity.ActivityStubResponse;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.AdapterStubRequest;
import com.centurylink.mdw.model.value.event.AdapterStubResponse;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class GroovyTestCaseRun extends TestCaseRun {

    private boolean runOnServer;
    TestCaseProcess testCaseProcess;
    TestCaseAsset testCaseAsset;

    private List<String> classpathList;

    public GroovyTestCaseRun(TestCase testcase, int run, String masterRequestId,
            DesignerDataAccess dao, LogMessageMonitor monitor, Map<String, ProcessVO> processCache,
            boolean isLoadTest, boolean oneThreadPerCase, boolean oldNamespaces, List<String> classpathList)
    throws DataAccessException {
        super(testcase, run, masterRequestId, dao, monitor, processCache, isLoadTest, oneThreadPerCase, oldNamespaces);
        this.classpathList = classpathList;
        if (!testcase.isLegacy()) {
            if (dao.isVcsPersist()) {
                File caseFile = testcase.getCaseFile();
                PackageDir pkgDir = new PackageDir(dao.getVcsBase(), caseFile.getParentFile(), dao.getVersionControl());
                pkgDir.parse();
                try {
                    AssetFile assetFile = pkgDir.getAssetFile(caseFile);
                    testCaseAsset = new TestCaseAsset(pkgDir, assetFile);
                }
                catch (IOException ex) {
                    throw new DataAccessException(ex.getMessage(), ex);
                }
            }
            else {
                // non-vcs assets
                RuleSetVO caseRuleSet = testcase.getRuleSet();
                testCaseAsset = new TestCaseAsset(caseRuleSet);
            }
        }
    }

    public void run() {
        startExecution();
        try {
            String groovyScript = getTestCase().getGroovyScript();

            runOnServer = testCaseAsset != null && testCaseAsset.getPackageName().endsWith(".server");

            if (runOnServer) {
                JSONObject actionRequest = new JSONObject();
                JSONObject action = new JSONObject();
                action.put("name", "UnitTest");
                JSONArray parameters = new JSONArray();
                JSONObject appName = new JSONObject();
                appName.put("appName", "MDW Automated Testing");
                parameters.put(appName);
                action.put("parameters", parameters);
                actionRequest.put("Action", action);
                JSONObject script = new JSONObject();
                script.put("name", getTestCase().getCaseName());
                script.put("groovy", groovyScript);
                actionRequest.put("Script", script);

                String response = dao.sendMessage("REST", actionRequest.toString(2), getDefaultMessageHeaders());
                JSONObject responseMsg = new JSONObject(response);
                JSONObject status = responseMsg.getJSONObject("status");
                if (status.getInt("code") != 0) {
                    String msg = status.getString("message");
                    if (status.has("location"))
                        msg = status.getString("location") + " " + msg;
                    throw new TestException(msg);
                }
            }
            else {
                CompilerConfiguration compilerConfig = new CompilerConfiguration();
                if (classpathList != null)
                  compilerConfig.setClasspathList(classpathList);
                compilerConfig.setScriptBaseClass(GroovyTestCaseScript.class.getName());

                Binding binding = new Binding();
                binding.setVariable("testCaseRun", this);

                GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding, compilerConfig);
                Script gScript = shell.parse(groovyScript);
                gScript.setProperty("out", log);
                gScript.run();
            }
            finishExecution(null);
        }
        catch (Throwable ex) {
            finishExecution(ex);
        }
    }

    void startProcess(TestCaseProcess process) throws TestException {
        if (verbose)
            log.println("starting process " + process.getLabel() + "...");
        this.testCaseProcess = process;
        try {
            if (!getTestCase().isLegacy()) {
              // delete the (convention-based) result file if exists
              String resFileName = getTestCase().getName() + RuleSetVO.getFileExtension(RuleSetVO.YAML);
              File resFile = new File(getTestCase().getResultDirectory() + "/" + resFileName);
              if (resFile.isFile())
                resFile.delete();
            }

            ProcessVO vo = process.getProcessVo();
            Map<String,String> params = process.getParams();
            String server = getNextServer();
            String response;
            if (server == null) {
                response = dao.launchProcess(vo, getMasterRequestId(), null, params, false, oldNamespaces);
            }
            else {
                String request = dao.getCurrentServer().buildLaunchProcessRequest(vo, getMasterRequestId(), null, params, false, oldNamespaces);
                response = dao.engineCall(dao.getPeerServerUrl(server), request);
            }

            validateEngineCallResponse(response);
        }
        catch (Exception ex) {
            throw new TestException("Failed to start " + process.getLabel(), ex);
        }
    }

    List<ProcessInstanceVO> processInstances;

    List<ProcessInstanceVO> loadProcess(TestCaseProcess[] processes, TestCaseAsset expectedResults) throws TestException {
        processInstances = null;
        if (isLoadTest)
            throw new TestException("Not supported for load tests");
        try {
            List<ProcessVO> processVos = new ArrayList<ProcessVO>();
            if (verbose)
                log.println("loading runtime data for processes:");
            for (TestCaseProcess process : processes) {
                if (verbose)
                    log.println("  - " + process.getLabel());
                processVos.add(process.getProcessVo());
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

    boolean verifyProcess(TestCaseProcess[] processes, TestCaseAsset expectedResults) throws TestException {
        processInstances = null;
        if (isLoadTest)
            return true;
        if (!passed)
            return false;

        try {
            List<ProcessVO> processVos = new ArrayList<ProcessVO>();
            if (verbose)
                log.println("loading runtime data for processes:");
            for (TestCaseProcess process : processes) {
                if (verbose)
                    log.println("  - " + process.getLabel());
                processVos.add(process.getProcessVo());
            }
            if (getTestCase().isLegacy()) {
                processInstances = loadResults(processVos, null, false);
                return legacyVerifyProcesses();
            }
            else {
                processInstances = loadResults(processVos, expectedResults, processes[0].isResultsById());
                if (processInstances.isEmpty())
                    throw new IllegalStateException("No process instances found for masterRequestId: " + masterRequestId);
                return verifyProcesses(expectedResults);
            }
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

    ProcessVO getProcess(String target) throws TestException {
        // qualify vcs asset processes
        if (target.indexOf('/') == -1 && testCaseAsset != null && testCaseAsset.exists())
            target = testCaseAsset.getPackageName() + "/" + target;
        ProcessVO vo = processCache.get(target);
        if (vo == null) {
            try {
                String procName = target;
                int version = 0; // latest
                int spaceV = target.lastIndexOf(" v");
                if (spaceV > 0) {
                    try {
                        version = RuleSetVO.parseVersionSpec(procName.substring(spaceV + 2));
                        procName = target.substring(0, spaceV);
                    }
                    catch (NumberFormatException ex) {
                        // process name must have space v
                    }
                }
                if (testCaseAsset == null || !testCaseAsset.isVcs()) {
                    // trim qualifying package name for old-style
                    int lastSlash = procName.lastIndexOf('/');
                    if (lastSlash >= 0)
                        procName = procName.substring(lastSlash + 1);
                }

                vo = dao.getProcessDefinition(procName, version);
                if (vo == null)
                    throw new TestException("Process: " + target + " not found");
                vo = dao.getProcess(vo.getId(), vo);
            }
            catch (TestException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new TestException("Cannot load " + target, ex);
            }
        }
        return vo;
    }

    TestCaseAsset getAsset(String path) throws TestException {
            try {
                int lastSlash = path.lastIndexOf('/');
                String name = path;
                String pkg = null;
                if (lastSlash >= 0) {
                    pkg = path.substring(0, lastSlash);
                    name = name.substring(lastSlash + 1);
                }
                if (pkg == null && testcase.isLegacy())
                    throw new TestException("Asset path must be fully-qualified for legacy tests");

                if (testcase.isLegacy() || !testCaseAsset.isVcs()) {
                    int lastDot = name.lastIndexOf('.');
                    if (lastDot < 0)
                        throw new TestException("Asset format must be inferable from extension: " + name);
                    String language = RuleSetVO.getLanguage(name.substring(lastDot));
                    RuleSetVO ruleSet = dao.getRuleSet(name, language, 0);
                    if (ruleSet == null) { // placeholder
                        ruleSet = new RuleSetVO();
                        ruleSet.setName(name);
                    }
                    ruleSet.setPackageName(pkg == null ? testCaseAsset.getPackageName() : pkg);
                    return new TestCaseAsset(ruleSet);
                }
                else { // VCS
                    PackageDir pkgDir = testCaseAsset.getPackageDir();
                    if (pkg != null && !pkg.equals(pkgDir.getPackageName()))
                        pkgDir = new PackageDir(dao.getVcsBase(), new File(dao.getVcsBase() + "/" + pkg.replace('.', '/')), dao.getVersionControl());
                    if (!pkgDir.exists())
                        return null;
                    AssetFile assetFile = pkgDir.getAssetFile(new File(pkgDir + "/" + name));
                    return new TestCaseAsset(pkgDir, assetFile);
                }
            }
            catch (Exception ex) {
                throw new TestException("Cannot load " + path, ex);
            }
    }

    TestCaseResponse sendMessage(TestCaseMessage message) throws TestException {
        if (isLoadTest) // special subst for CSV
            message.setPayload(message.getPayload().replaceAll("#\\{", "#\\{\\$"));
        if (verbose) {
            log.println("sending " + message.getProtocol() + " message...");
            log.println("message payload:");
            log.println(message.getPayload());
        }

        try {
            String actual = sendMessage(testCaseAsset, message.getProtocol(), message.getPayload(), null); // TODO reference doc for subst
            if (verbose) {
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
        if (verbose) {
            log.println("http " + http.getMethod() + " request to " + http.getUri());
        }

        try {
            String url;
            if (http.getUri().startsWith("http://") || http.getUri().startsWith("https://")) {
                url =  http.getUri();
            }
            else {
                url = dao.getCurrentServer().getServerUrl();
                if (http.getUri().startsWith("/"))
                    url += http.getUri();
                else
                    url += "/" + http.getUri();
            }

            HttpHelper helper;
            if (http.getMessage() != null && http.getMessage().getUser() != null)
                helper = new HttpHelper(new URL(url), http.getMessage().getUser(), http.getMessage().getPassword());
            else
                helper = new HttpHelper(new URL(url));

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
                }
            }

            if (verbose) {
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
        if (verbose)
            log.println("waiting for process: " + process.getLabel() + " (timeout=" + process.getTimeout() + "s)...");
        String waitKey = getWaitKey(process.getProcessVo(), process.getActivityLogicalId(), process.getStatus());
        performWait(waitKey, process.getTimeout());
    }

    boolean verifyResponse(TestCaseResponse response) throws TestException {
        if (isLoadTest)
            return true;
        if (!passed)
            return false;
        try {
            if (verbose) {
                log.println("expected response:");
                log.println(response.getExpected());
                log.println("actual response:");
                log.println(response.getActual());
            }
            return executeVerifyResponse(response.getExpected(), response.getActual(), null); // TODO reference doc for subst
        }
        catch (Exception ex) {
            throw new TestException(ex.getMessage(), ex);
        }
    }

    void performTaskAction(TestCaseTask task) throws TestException {
        if (verbose)
            log.println("performing " + task.getOutcome() + " on task '" + task.getName() + "'");
        JSONObject json = performTask(task.getName(), task.getOutcome(), task.getParams());
        if (json != null && json.has("taskInstanceId")) {
            try {
                task.setId(json.getLong("taskInstanceId"));
            }
            catch (JSONException ex) {
                throw new TestException(ex.getMessage(), ex);
            }

        }
    }

    void notifyEvent(TestCaseEvent event) throws TestException {
        if (verbose)
            log.println("notifying event: '" + event.getId() + "'");
        performNotify(event.getId(), event.getMessage(), event.getProcessName(), event.getActivityLogicalId());
    }

    void sleep(int seconds) {
        if (verbose)
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
    @Override
    public String getStubResponse(String masterRequestId, String request, int run) throws JSONException, TestException {

        JSONObject requestJson = null;
        ActivityStubRequest activityStubRequest = null; // mdw6
        ActivityRuntimeContext activityRuntimeContext = null;
        AdapterStubRequest adapterStubRequest = null; // mdw6
        if (request != null && request.trim().startsWith("{")) {
            try {
                requestJson = new JSONObject(request);
            }
            catch (JSONException ex) {
                // unparseable -- handle old way for adapter stubbing
            }
            if (requestJson.has(ActivityStubRequest.JSON_NAME)) {
                activityStubRequest = new ActivityStubRequest(requestJson);
                activityRuntimeContext = activityStubRequest.getRuntimeContext();
            }
            else if (requestJson.has("ActivityRuntimeContext")) {
                activityRuntimeContext = new ActivityRuntimeContext(requestJson);
            }
            else if (requestJson.has(AdapterStubRequest.JSON_NAME)) {
                adapterStubRequest = new AdapterStubRequest(requestJson);
            }
        }

        if (activityRuntimeContext != null) {
            // activity stubbing
            if (testCaseProcess != null && testCaseProcess.getActivityStubs() != null) {
                for (TestCaseActivityStub activityStub : testCaseProcess.getActivityStubs()) {
                    if (activityStub.getMatcher().call(activityRuntimeContext)) {
                        Closure<String> completer = activityStub.getCompleter();
                        completer.setResolveStrategy(Closure.DELEGATE_FIRST);
                        completer.setDelegate(activityStub);
                        String resultCode = null;
                        try {
                            resultCode = completer.call(request);
                        }
                        catch (Throwable th) {
                            th.printStackTrace(log);
                            throw new TestException(th.getMessage(), th);
                        }
                        ActivityStubResponse activityStubResponse = new ActivityStubResponse();
                        activityStubResponse.setResultCode(resultCode);
                        if (activityStub.getSleep() > 0)
                            activityStubResponse.setSleep(activityStub.getSleep());
                        else if (activityStub.getDelay() > 0)
                            activityStubResponse.setSleep(activityStub.getDelay());
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
                        if (verbose)
                            log.println("Stubbing activity " + activityRuntimeContext.getProcess().getProcessName() + ":" +
                                    activityRuntimeContext.getActivityLogicalId() + " with result code: " + resultCode);
                        if (activityStubRequest != null) {
                            // mdw6+
                            return activityStubResponse.getJson().toString(2);
                        }
                        else {
                            return "RESPONSE~" + activityStubResponse.getSleep() + "~" + activityStubResponse.getJson();
                        }
                    }
                }
            }
            if (activityStubRequest != null) {
                // mdw6+
                ActivityStubResponse activityStubResponse = new ActivityStubResponse();
                activityStubResponse.setPassthrough(true);
                return activityStubResponse.getJson().toString(2);
            }
            else {
                return "(EXECUTE_ACTIVITY)";
            }
        }
        else {
            // adapter stubbing
            for (TestCaseAdapterStub adapterStub : adapterStubs) {
                String requestContent = adapterStubRequest == null ? request : adapterStubRequest.getContent();
                boolean match = false;
                try {
                    if (adapterStub.isEndpoint() && adapterStubRequest != null) {
                        match = adapterStub.getMatcher().call(adapterStubRequest);
                    }
                    else {
                        match = adapterStub.getMatcher().call(requestContent);
                    }
                }
                catch (Throwable th) {
                    th.printStackTrace(log);
                    throw new TestException(th.getMessage(), th);
                }
                if (match) {
                    try {
                        String stubbedResponseContent = adapterStub.getResponder().call(requestContent);
                        int delay = 0;
                        if (adapterStub.getDelay() > 0)
                            delay = adapterStub.getDelay();
                        else if (adapterStub.getSleep() > 0)
                            delay = adapterStub.getSleep();

                        if (adapterStubRequest != null) {
                            // mdw6+
                            AdapterStubResponse stubResponse = new AdapterStubResponse(stubbedResponseContent);
                            stubResponse.setDelay(delay);
                            stubResponse.setStatusCode(adapterStub.getStatusCode());
                            stubResponse.setStatusMessage(adapterStub.getStatusMessage());
                            if (verbose)
                                log.println("Stubbing endpoint " + adapterStubRequest.getUrl() + " with:\n" + stubbedResponseContent);
                            return stubResponse.getJson().toString(2);
                        }
                        else {
                            if (verbose)
                                log.println("Stubbing response with: " + stubbedResponseContent);
                            return "RESPONSE~" + delay + "~" + stubbedResponseContent;
                        }
                    }
                    catch (Throwable th) {
                        th.printStackTrace(log);
                        throw new TestException(th.getMessage(), th);
                    }
                }
            }
            if (verbose)
                log.println("Stubbing response with: " + AdapterActivity.MAKE_ACTUAL_CALL);
            if (adapterStubRequest != null) {
                // mdw6+
                AdapterStubResponse stubResponse = new AdapterStubResponse(AdapterActivity.MAKE_ACTUAL_CALL);
                stubResponse.setPassthrough(true);
                return stubResponse.getJson().toString(2);
            }
            else {
                return AdapterActivity.MAKE_ACTUAL_CALL;
            }
        }
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