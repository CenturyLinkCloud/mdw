/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
import cucumber.api.DataTable
import com.centurylink.mdw.designer.testing.GroovyTestCaseRun
import com.centurylink.mdw.designer.testing.GroovyTestCaseScript
import com.centurylink.mdw.common.exception.DataAccessException
import com.centurylink.mdw.designer.DesignerDataAccess
import com.centurylink.mdw.model.value.process.ProcessVO
import com.centurylink.mdw.designer.testing.TestCase
import com.centurylink.mdw.designer.utils.RestfulServer
import com.centurylink.mdw.dataaccess.file.VersionControlGit

import java.util.HashMap
import java.util.Map;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.common.cache.impl.VariableTypeCache
import com.centurylink.mdw.designer.testing.LogMessageMonitor
import com.centurylink.mdw.designer.testing.StubServer;
import com.centurylink.mdw.designer.testing.TestException
import com.centurylink.mdw.designer.testing.StubServer.Stubber;
import com.centurylink.mdw.designer.testing.TestCaseMessage
import com.centurylink.mdw.designer.testing.TestCaseRun;

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

/**
 * Unfortunately, each test is run in sequence until:
 * https://github.com/cucumber/cucumber-jvm/issues/630
 */
class DefaultTestCaseExec extends GroovyTestCaseScript {

    def monitor
    def verbose = true
    def inputVariables = [:]
    def today = new Date().format("M/d/yyyy")
    def boolean stubbing
    def String stubPort
    def RestfulServer restServer
    def Stubber stubber

    DefaultTestCaseExec(TestCase testCase, int run, String masterRequestId,
    DesignerDataAccess dao, LogMessageMonitor monitor, Map<String, ProcessVO> processCache,
    boolean isLoadTest, boolean oneThreadPerCase, boolean oldNamespaces, boolean stubbing, String stubPort, RestfulServer restServer)
    throws DataAccessException {
        this.monitor = monitor
        this.testCase = testCase
        this.stubbing = stubbing
        this.stubPort = stubPort
        this.restServer = restServer
        this.testCaseRun = new GroovyTestCaseRun(testCase, run, masterRequestId, dao, monitor, processCache, isLoadTest, oneThreadPerCase, oldNamespaces);
    }

    Object run() {
        return null; // dummy
    }

    String subst(String inp) {
        return super.substitute(inp)
    }
    String readFile(String v) {
        def inputFile = v.toString().startsWith('${file(') ? true : false;
        def val;
        if(inputFile)
        {
            val = v.substring('${file('.length() + 1, v.length() - 3);
            val = subst(file(val).text);
        }
        else {
            val = v.toString().startsWith('${') ? subst(v) : v;
        }
        if (verbose)
            println '  input-> ' + v + ': ' + val;
        return val;
    }
}

World {
    // use system properties
    // (cucumber-jvm does not seem to support env vars on the command line)
    def sysProps = System.properties

    // props for entire run
    def user = sysProps['mdw.test.case.user']
    // TODO: authentication and authorization (look at AutomatedTestAntTask)
    def caseFile = sysProps['mdw.test.case.file']
    boolean legacy = caseFile == null
    def casesDir = sysProps['mdw.test.cases.dir']
    def resultsDir = sysProps['mdw.test.results.dir']
    def jdbcUrl = sysProps['mdw.test.jdbc.url']
    def workflowDir = sysProps['mdw.test.workflow.dir']
    def serverUrl = sysProps['mdw.test.server.url']
    def stubbing = "true".equalsIgnoreCase(sysProps['mdw.test.server.stub'])
    def stubPort = sysProps['mdw.test.server.stubPort']
    def oldNamespaces = "true".equalsIgnoreCase(sysProps['mdw.test.old.namespaces'])
    def pinToServer = "true".equalsIgnoreCase(sysProps['mdw.test.pin.to.server'])
    def createReplace = "true".equalsIgnoreCase(sysProps['mdw.test.create.replace'])
    def masterRequestId = sysProps['mdw.test.masterRequestId']

    def restServer = new RestfulServer(jdbcUrl == null ? 'dummy' : jdbcUrl, user, serverUrl)
    def dataAccess
    if (workflowDir != null) {
        def versionControl = new VersionControlGit()
        versionControl.connect(null, null, null, new File(workflowDir))
        restServer.setVersionControl(versionControl)
        restServer.setRootDirectory(new File(workflowDir))
        restServer.setDatabaseUrl('jdbc://dummy')
        dataAccess = new DesignerDataAccess(restServer, null, user, false)
        dataAccess.setCurrentServer(restServer)
    }
    else {
        dataAccess = new DesignerDataAccess(restServer, null, user, oldNamespaces)
    }
    VariableTypeCache.loadCache(dataAccess.getVariableTypes())

    def caseName = sysProps['mdw.test.case']
    def testCase
    if (legacy) {
        testCase = new TestCase('Legacy', new File(casesDir + '/' + caseName))
    }
    else {
        // prefix is package name
        def cFile = new File(caseFile);
        def wfDirLen = new File(workflowDir).toString().length();
        def prefix = cFile.toString().substring(wfDirLen + 1, cFile.toString().length() - cFile.getName().length() - 1).replace('\\', '.').replace('/', '.')
        testCase = new TestCase(prefix, cFile)
    }
    // testCase.prepare()
    if (masterRequestId == null)
      masterRequestId = user + '-' + new Date().format('yyyyMMdd-HHmmss')
    def resultDir = new File(resultsDir);
    def monitor = new LogMessageMonitor(dataAccess, oldNamespaces)
    def processCache = new HashMap<String,ProcessVO>()
    def tcExec = new DefaultTestCaseExec(testCase, 0, masterRequestId, new DesignerDataAccess(dataAccess), monitor, processCache, false, true, oldNamespaces, stubbing, stubPort, restServer)
    def verbose = 'true'.equalsIgnoreCase(sysProps['mdw.test.verbose'])
    tcExec.verbose = verbose
    tcExec.testCaseRun.prepareTest(createReplace, resultDir, verbose, pinToServer, stubbing, System.out)
    return tcExec
}

/**
 * Executed before every scenario.
 * Causes same-named testCase to be run for each scenarios.
 */
Before() {
    if (stubbing) {
        final TestCaseRun run = testCaseRun;
        stubber = new Stubber() {
                    public String processMessage(String masterRequestId2, String request) {
                        if (run == null) {
                            return AdapterActivity.MAKE_ACTUAL_CALL;
                        }
                        return run.getStubResponse(masterRequestId2, request, run.getRunNumber());
                    }
                };
        if (StubServer.isRunning()){
            StubServer.stop();
        }
        StubServer.start(restServer, new Integer(stubPort).intValue(), stubber, false);
    }
    if (verbose)
    println 'starting log monitor'
    monitor.start(true)
    testCaseRun.startExecution()
}

After() {
    testCaseRun.finishExecution()
    if (stubber != null) {
        StubServer.stop();
    }
    if (verbose)
        println 'stopping log monitor'
    monitor.shutdown()
}

Given(~'^process input values:$') {DataTable inputs ->
    inputs.asMap(String, String).each { k, v ->
        def val = readFile(v);
        inputVariables[k] = val
    }
}

Given(~/^I notify event "([^"]*)" with message "([^"]*)"$/) { String eventName, String payLoad ->
    notify event(subst(eventName)) {
        message = subst(payLoad)
    }
}

Given(~/^masterRequestId "([^"]*)"$/) { String masterRequestId ->
  testCase.setMasterRequestId(masterRequestId); //This does nothing
}

When(~'^the "([^\"]*)" workflow is invoked$') { String procName ->
    start process(procName) {
        variables = inputVariables
    }
}

When(~'^I wait (\\d+) seconds$') { int seconds ->
    testCaseRun.sleep(seconds)
}

When(~'^I (\\w+) task "([^\"]*)"$') { String actionName, String taskName ->
    action task(taskName) {
        outcome = actionName
    }
}

When(~'^I (\\w+) task "([^\"]*)" with values:$') {String actionName, String taskName, DataTable values ->
    def vars = [:]
    values.asMap(String, String).each { k, v ->
        def val = v.toString().startsWith('${') ? subst(v) : v
        if (verbose)
            println '  task value -> ' + k + ': ' + val
        vars[k] = val
    }

    action task(taskName) {
        outcome = actionName
        variables = vars
    }
}

When(~'^I send "([^"]*)"$') { String fileName ->
    TestCaseMessage message = new TestCaseMessage("REST");
    String requestFile = readFile(fileName);
    message.setPayload(requestFile);
    send(message)
}

When(~'^I send "([^"]*)" message "([^"]*)"$') { String protocol, String fileName ->
    TestCaseMessage message = new TestCaseMessage(protocol);
    String requestFile = readFile(fileName);
    message.setPayload(requestFile);
    send(message)
}

Then(~'the response should match "([^"]*)"$') { responseFile ->
    String resFile = readFile(responseFile);
    def testProc = verify resFile
    testCase.setStatus(testProc.isSuccess() ? TestCase.STATUS_PASS : TestCase.STATUS_FAIL)
    assert testProc.isSuccess()
}

When(~/^I await process completion$/) { ->
    wait process
}

When(~/^I stub "([^"]*)" after delay of (\d+) seconds with a response "([^"]*)"$/) { String node, int delay, String responseFile ->
    String resFile = readFile(responseFile);
    stub adapter(xpath(node)) {
        delay = delay
        response = resFile
    }
}

Then(~'the results should match "([^"]*)"$') { String targets ->
    String [] processList = targets.split(",");
    def testProc = verify processes(processList);
    testCase.setStatus(testProc.isSuccess() ? TestCase.STATUS_PASS : TestCase.STATUS_FAIL)
    assert testProc.isSuccess()
}