/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.XmlPath;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.DelegatingScript;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;

public abstract class GroovyTestCaseScript extends Script {
    // values for placeholder access
    public String getMasterRequestId() {
        return getTestCaseRun().getMasterRequestId();
    }
    public void setMasterRequestId(String masterRequestId) {
        getTestCaseRun().setMasterRequestId(masterRequestId);
    }
    /**
     * In case it's assigned through binding.
     */
    private void syncMasterRequestId() {
        if (getBinding().hasVariable("masterRequestId")) {
            Object defined = getBinding().getVariable("masterRequestId");
            TestCaseRun run = getTestCaseRun();
            if (!run.getMasterRequestId().equals(defined)) {
                String old = run.getMasterRequestId();
                run.setMasterRequestId(String.valueOf(defined));
                if (run.getMasterRequestListener() != null)
                    run.getMasterRequestListener().syncMasterRequestId(old, run.getMasterRequestId());
                if (run.verbose)
                  run.log.println("masterRequestId: " + getMasterRequestId());
            }
        }
    }

    // server-side placeholders: simply return server-side syntax for replacement on the server
    public String getProcessInstanceId() { return "{$ProcessInstanceId}"; }
    public String getActivityInstanceId() { return "{$ActivityInstanceId}"; }
    // TODO public Map<String,String> getVariables()

    private TestCaseProcess process; // current top level process
    public TestCaseProcess getProcess() {
        return process;
    }
    private TestCaseResponse response;
    public TestCaseResponse getResponse() {
        return response;
    }

    public String getResponseMessage() {
        return response == null ? null : response.getActual();
    }

    public List<ProcessInstanceVO> getProcessInstances() {
        return getTestCaseRun().processInstances;
    }
    public ProcessInstanceVO getMasterProcessInstance() {
        List<ProcessInstanceVO> instances = getProcessInstances();
        if (instances != null) {
            for (ProcessInstanceVO instance : instances) {
                if (!OwnerType.PROCESS_INSTANCE.equals(instance.getOwner()))
                    return instance;
            }
        }
        return null;
    }

    // no-op methods for keywords
    public TestCaseProcess process() { return process; }
    public TestCaseResponse response() { return response; }

    public TestCaseProcess start(String target) throws TestException {
        return start(process(target));
    }

    public TestCaseProcess start(TestCaseProcess process) throws TestException {
        syncMasterRequestId();
        if (getTestCaseRun().isLoadTest) {
            send(new TestCaseMessage(getMasterRequestId(), process));
            return null;
        }

        getTestCaseRun().startProcess(process);
        return process;
    }

    public TestCaseProcess process(String target) throws TestException {
        return process(target, null);
    }

    public TestCaseProcess process(String target, Closure<?> cl) throws TestException {
        process = new TestCaseProcess(getTestCaseRun().getProcess(target));
        if (cl != null) {
            cl.setResolveStrategy(Closure.DELEGATE_FIRST);
            cl.setDelegate(process);
            cl.call();
        }
        return process;
    }

    public TestCaseProcess process(Closure<?> cl) throws TestException {
        cl.setResolveStrategy(Closure.DELEGATE_FIRST);
        cl.setDelegate(process);
        cl.call();
        return process;
    }

    public TestCaseProcess[] processes(String... targets) throws TestException {
        List<TestCaseProcess> processes = new ArrayList<TestCaseProcess>();
        for (int i = 0; i < targets.length; i++) {
            try {
                ProcessVO processVo = getTestCaseRun().getProcess(targets[i]);
                processes.add(new TestCaseProcess(processVo));
            }
            catch (TestException ex) {
                getTestCaseRun().log.println("Failed to load process " + targets[i]);
                if (i == 0)  // by convention > 0 are subprocesses where exception is expected
                    throw ex;
            }
        }
        return processes.toArray(new TestCaseProcess[0]);
    }

    public Verifiable verify(Object object) throws TestException {
        if (object instanceof TestCaseProcess[])
            return verify((TestCaseProcess[])object, null);
        if (object instanceof TestCaseProcess)
            return verify(new TestCaseProcess[]{(TestCaseProcess)object}, null);
        else if (object instanceof TestCaseResponse)
            return verify((TestCaseResponse)object);
        else if (object instanceof File) {
            try {
                response.setExpected(new String(getTestCaseRun().read((File)object)));
                return verify(response);
            }
            catch (IOException ex) {
                throw new TestException(ex.getMessage(), ex);
            }
        }
        else if (object instanceof String) {
            response.setExpected((String)object);
            return verify(response);
        }
        else
            throw new TestException("Unsupported verification object: " + object);
    }

    public List<ProcessInstanceVO> load(TestCaseProcess process) throws TestException {
        return load(new TestCaseProcess[]{process});
    }

    public List<ProcessInstanceVO> load(TestCaseProcess[] processes) throws TestException {
        TestCaseAsset expectedResults = process.getExpectedResults();
        if (!getTestCase().isLegacy() && expectedResults == null)
            expectedResults = getDefaultExpectedResults(process);
        return getTestCaseRun().loadProcess(processes, expectedResults);
    }

    public Verifiable verify(TestCaseProcess process, Closure<?> cl) throws TestException {
        return verify(new TestCaseProcess[]{(TestCaseProcess)process}, cl);
    }

    public Verifiable verify(TestCaseProcess[] processes, Closure<?> cl) throws TestException {
        getTestCaseRun().setPreFilter(new PreFilter(){
            public String apply(String before) {
                return substitute(before);
            }
        });

        if (cl != null) {
            cl.setResolveStrategy(Closure.DELEGATE_FIRST);
            cl.setDelegate(process);
            cl.call();
        }
        if (!getTestCase().isLegacy() && process.getExpectedResults() == null)
            process.setExpectedResults(getDefaultExpectedResults(process));

        process.setSuccess(getTestCaseRun().verifyProcess(processes, process.getExpectedResults()));
        return process;
    }

    /**
     * Default naming convention for expected results asset (need not exist).
     */
    public TestCaseAsset getDefaultExpectedResults(TestCaseProcess process) throws TestException {
        String testAssetName = getTestCaseRun().testCaseAsset.getName();
        String resultsAssetName = testAssetName.substring(0, testAssetName.lastIndexOf('.')) + RuleSetVO.getFileExtension(RuleSetVO.YAML);
        return asset(resultsAssetName);
    }

    public TestCaseResponse send(String payload) throws TestException {
        syncMasterRequestId();
        return send(message(payload));
    }

    public TestCaseResponse send(File file) throws TestException {
        try {
            syncMasterRequestId();
            return send(message(new String(getTestCaseRun().read(file))));
        }
        catch (IOException ex) {
            throw new TestException(ex.getMessage(), ex);
        }
    }

    public TestCaseResponse send(TestCaseAsset asset) throws TestException {
        try {
            syncMasterRequestId();
            return send(message(asset.getText()));
        }
        catch (IOException ex) {
            throw new TestException(ex.getMessage(), ex);
        }
    }

    public TestCaseResponse send(TestCaseMessage message) throws TestException {
        message.setPayload(substitute(message.getPayload()));
        response = getTestCaseRun().sendMessage(message);
        return response;
    }

    public TestCaseResponse get(TestCaseHttp http) throws TestException {
        http.setMethod("get");
        response = getTestCaseRun().http(http);
        return response;
    }

    public TestCaseResponse post(TestCaseHttp http) throws TestException {
        http.setMethod("post");
        response = getTestCaseRun().http(http);
        return response;
    }

    public TestCaseResponse put(TestCaseHttp http) throws TestException {
        http.setMethod("put");
        response = getTestCaseRun().http(http);
        return response;
    }

    public TestCaseResponse delete(TestCaseHttp http) throws TestException {
        http.setMethod("delete");
        response = getTestCaseRun().http(http);
        return response;
    }

    public TestCaseHttp http(String uri) {
        return http(uri, null);
    }

    public TestCaseHttp http(String uri, Closure<?> cl) {
        syncMasterRequestId();
        TestCaseHttp http = new TestCaseHttp(uri);
        if (cl != null) {
            TestCaseMessage message = new TestCaseMessage();
            cl.setResolveStrategy(Closure.DELEGATE_FIRST);
            cl.setDelegate(message);
            cl.call();
            if (message.getPayload() != null)
                message.setPayload(substitute(message.getPayload()));
            http.setMessage(message);
        }
        return http;
    }

    public TestCaseResponse response(Closure<?> cl) throws TestException {
        if (response == null)
            response = new TestCaseResponse();
        if (cl != null) {
            cl.setResolveStrategy(Closure.DELEGATE_FIRST);
            cl.setDelegate(response);
            cl.call();
        }
        return response;
    }

    public Verifiable verify(TestCaseResponse response) throws TestException {
        if (!getTestCaseRun().isLoadTest) {
            this.response = response;
            response.setExpected(substitute(response.getExpected()));
            response.setSuccess(getTestCaseRun().verifyResponse(response));
        }
        return response;
    }

    public TestCaseMessage message(String payload) throws TestException {
        TestCaseMessage message = new TestCaseMessage();
        message.setPayload(substitute(payload));
        return message;
    }

    public TestCaseMessage message(Closure<?> cl) throws TestException {
        return message(null, cl);
    }

    public TestCaseMessage message(String protocol, Closure<?> cl) throws TestException {
        TestCaseMessage message = protocol == null ? new TestCaseMessage() : new TestCaseMessage(protocol);
        if (cl != null) {
            cl.setResolveStrategy(Closure.DELEGATE_FIRST);
            cl.setDelegate(message);
            cl.call();
        }
        return message;
    }

    public TestCaseTask action(TestCaseTask task) throws TestException {
        getTestCaseRun().performTaskAction(task);
        return task;
    }

    public TestCaseTask task(String name, Closure<?> cl) {
        TestCaseTask tcTask = new TestCaseTask(name);
        if (cl != null) {
            cl.setResolveStrategy(Closure.DELEGATE_FIRST);
            cl.setDelegate(tcTask);
            cl.call();
        }
        return tcTask;
    }

    public TestCaseEvent notify(TestCaseEvent event) throws TestException {
        getTestCaseRun().notifyEvent(event);
        return event;
    }

    public TestCaseEvent event(String eventId) throws TestException {
        return event(eventId, null);
    }

    public TestCaseEvent event(String eventId, Closure<?> cl) throws TestException {
        TestCaseEvent event = new TestCaseEvent(eventId);
        if (cl != null) {
            cl.setResolveStrategy(Closure.DELEGATE_FIRST);
            cl.setDelegate(event);
            cl.call();
        }
        return event;
    }

    public TestCaseAdapterStub stub(TestCaseAdapterStub adapterStub) throws TestException {
        getTestCaseRun().addAdapterStub(adapterStub);
        return adapterStub;
    }

    public TestCaseAdapterStub adapter(Closure<Boolean> matcher, Closure<?> init) throws TestException {
        return adapter(matcher, null, init);
    }

    /**
     * responder closure call is delayed until stub server calls back
     */
    public TestCaseAdapterStub adapter(Closure<Boolean> matcher, Closure<String> responder, Closure<?> init) throws TestException {
        final TestCaseAdapterStub adapterStub = new TestCaseAdapterStub(matcher, responder);
        if (init != null) {
            init.setResolveStrategy(Closure.DELEGATE_FIRST);
            init.setDelegate(adapterStub);
            init.call();
        }
        if (responder == null) {
            adapterStub.setResponder(new Closure<String>(this, adapterStub) {
                @Override
                public String call(Object request) {
                    if (adapterStub.getResponse().indexOf("${") >= 0) {
                        try {
                            GPathResult gpathRequest = new XmlSlurper().parseText(request.toString());
                            Binding binding = getBinding();
                            binding.setVariable("request", gpathRequest);
                            CompilerConfiguration compilerCfg = new CompilerConfiguration();
                            compilerCfg.setScriptBaseClass(DelegatingScript.class.getName());
                            GroovyShell shell = new GroovyShell(GroovyTestCaseScript.class.getClassLoader(), binding, compilerCfg);
                            DelegatingScript script = (DelegatingScript) shell.parse("return \"\"\"" + adapterStub.getResponse() + "\"\"\"");
                            script.setDelegate(GroovyTestCaseScript.this);
                            return script.run().toString();
                        }
                        catch (Exception ex) {
                            getTestCaseRun().log.println("Cannot perform stub substitutions for request: " + request);
                            ex.printStackTrace(getTestCaseRun().log);
                        }
                    }
                    return adapterStub.getResponse();
                }
            });
        }
        return adapterStub;
    }

    public TestCaseActivityStub activity(Closure<Boolean> matcher, Closure<String> completer) throws TestException {
        return new TestCaseActivityStub(matcher, completer);
    }

    public TestCaseActivityStub activity(final String match, Closure<String> completer) throws TestException {
        return new TestCaseActivityStub(new Closure<Boolean>(this, this) {
            @Override
            public Boolean call(Object context) {
                ActivityRuntimeContext runtimeContext = (ActivityRuntimeContext) context;
                String matchProcess = process.getProcessVo().getName();
                String matchActivity = match;
                int colon = match.lastIndexOf(':');
                if (colon > 0) {
                    matchActivity = match.substring(colon + 1);
                    int slash = match.lastIndexOf('/');
                    if (slash > 0) {
                        matchProcess = match.substring(slash + 1, colon);
                    }
                    else {
                        matchProcess = match.substring(0, colon);
                    }
                }

                return runtimeContext.getProcess().getName().equals(matchProcess) &&
                        (runtimeContext.getActivity().getActivityName().equals(matchActivity) ||
                            runtimeContext.getActivityLogicalId().equals(matchActivity));
            }
        }, completer);
    }

    /**
     * Matches according to MDW XPath.
     */
    public Closure<Boolean> xpath(final String condition) throws TestException {
        return new Closure<Boolean>(this, this) {
            @Override
            public Boolean call(Object request) {
                try {
                    XmlPath xpath = new XmlPath(condition);
                    String v = xpath.evaluate(TestDataFilter.parseRequest(request.toString()));
                    return v != null;
                }
                catch (MbengException ex) {
                    getTestCaseRun().log.println("Failed to parse request as XML/JSON. Stub response: " + AdapterActivity.MAKE_ACTUAL_CALL);
                    return false;
                }
            }
        };
    }

    /**
     * Matches according to GPath.
     */
    public Closure<Boolean> gpath(final String condition) throws TestException {
        return new Closure<Boolean>(this, this) {
            @Override
            public Boolean call(Object request) {
                try {
                    GPathResult gpathRequest = new XmlSlurper().parseText(request.toString());
                    Binding binding = getBinding();
                    binding.setVariable("request", gpathRequest);
                    return (Boolean) new GroovyShell(binding).evaluate(condition);
                }
                catch (Exception ex) {
                    getTestCaseRun().log.println("Failed to parse request as XML/JSON. Stub response: " + AdapterActivity.MAKE_ACTUAL_CALL);
                    return false;
                }
            }
        };
    }

    public void sleep(int seconds) {
        getTestCaseRun().sleep(seconds);
    }

    public void wait(Object object) throws TestException {
        if (object instanceof TestCaseProcess)
            wait((TestCaseProcess)object);
        else
            throw new TestException("Unsupported wait object: " + object);
    }

    public void wait(TestCaseProcess process) throws TestException {
        getTestCaseRun().wait(process);
    }

    @Deprecated
    public File file(String name) throws TestException {
        if (getTestCase().isLegacy())
            return new File(getTestCase().getCaseDirectory() + "/" + name);
        else
            return asset(name).file();
    }

    public TestCaseAsset asset(String path) throws TestException {
        return getTestCaseRun().getAsset(path);
    }

    protected GroovyTestCaseRun getTestCaseRun() {
        return (GroovyTestCaseRun) getBinding().getVariable("testCaseRun");
    }

    protected TestCase getTestCase() {
        return getTestCaseRun().getTestCase();
    }

    /**
     * performs groovy substitutions with this as delegate and inherited bindings
     */
    protected String substitute(String before) {
        if (before.indexOf("${") == -1)
            return before;
        // escape all $ not followed by curly braces on same line
        before = before.replaceAll("\\$(?!\\{)", "\\\\\\$");
        // escape all regex -> ${~
        before = before.replaceAll("\\$\\{~", "\\\\\\$\\{~");

        CompilerConfiguration compilerCfg = new CompilerConfiguration();
        compilerCfg.setScriptBaseClass(DelegatingScript.class.getName());
        GroovyShell shell = new GroovyShell(GroovyTestCaseScript.class.getClassLoader(), getBinding(), compilerCfg);
        DelegatingScript script = (DelegatingScript) shell.parse("return \"\"\"" + before + "\"\"\"");
        script.setDelegate(GroovyTestCaseScript.this);
        // restore escaped \$ to $ for comparison
        return script.run().toString().replaceAll("\\\\$", "\\$");
    }

    public boolean isStubbing() {
        return getTestCaseRun().isStubbing();
    }

    public boolean isHasTestingPackage() throws TestException {
        return asset("com.centurylink.mdw.testing/readme.md").exists();
    }

}
