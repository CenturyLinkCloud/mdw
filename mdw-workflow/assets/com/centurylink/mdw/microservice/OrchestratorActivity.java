package com.centurylink.mdw.microservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.VariableConstants;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.process.InvokeProcessActivityBase;

/**
 * Microservice orchestrator adapter activity.
 * ServicePlan, unlike ProcessExecutionPlan, is not updated with instance data.
 * Instead, instance info is saved to the ServiceSummary.
 */
@Tracked(LogLevel.TRACE)
public class OrchestratorActivity extends InvokeProcessActivityBase {

    private static final String SYNCHRONOUS = "synchronous";
    private static final String PARALLEL = "parallel";
    private static final String DELAY = "delay";

    // for execute() and needSuspend() -- otherwise use getServicePlan()
    private ServicePlan servicePlan;

    public void execute() throws ActivityException {
        servicePlan = getServicePlan();
        if (getEngine().isInService() && isParallel() && isSynchronous()) {
            executeServiceSubflowsInParallel(servicePlan);
        }
        else {
            executeServiceSubflowsInSequence(servicePlan);
        }
    }

    @Override
    public boolean needSuspend() {
        if (servicePlan.getServices().isEmpty()
                || (getEngine().isInService() && isParallel() && isSynchronous())) {
            return false;
        }
        else {
            return isSynchronous();
        }
    }

    protected int getDelay() {
        int delay = getAttribute(DELAY, 0);
        if (isSynchronous() && getEngine().isInService())
            return 0;
        else if (isParallel() && delay <= 0)
            return 1;
        else
            return delay;
    }

    protected boolean isSynchronous() {
        return getAttribute(SYNCHRONOUS, true);
    }

    protected boolean isParallel() {
        return getAttribute(PARALLEL, true);
    }

    protected ServicePlan getServicePlan() throws ActivityException {
        return (ServicePlan)getRequiredVariableValue(getServicePlanVariableName());
    }

    /**
     * You'd need a custom .impl asset to set this through designer
     */
    protected String getServicePlanVariableName() {
        return getAttribute("servicePlanVariable", "servicePlan");
    }

    protected ServiceSummary getServiceSummary(boolean forUpdate) throws ActivityException {
        DocumentReference docRef = (DocumentReference)getParameterValue(getServiceSummaryVariableName());
        if (forUpdate)
            return (ServiceSummary) getDocumentForUpdate(docRef, Jsonable.class.getName());
        else
            return (ServiceSummary) getDocument(docRef, Jsonable.class.getName());
    }

    /**
     * You'd need a custom .impl asset to set this through designer
     */
    protected String getServiceSummaryVariableName() {
        return getAttribute("serviceSummaryVariable", "serviceSummary");
    }

    protected Process getTemplateProcess(Microservice service) throws ActivityException {
        AssetVersionSpec spec = new AssetVersionSpec(service.getTemplate());
        Process process = ProcessCache.getProcessSmart(spec);
        if (process == null)
            throw new ActivityException("Template not found: " + service.getTemplate());
        return process;
    }

    private ProcessInstance createProcessInstance(Microservice service)
            throws ActivityException, DataAccessException, ProcessException {
        // prepare variable bindings
        AssetVersionSpec spec = new AssetVersionSpec(service.getTemplate());
        Process process = ProcessCache.getProcessSmart(spec);
        if (process == null)
            throw new ActivityException("Template not found: " + service.getTemplate());
        List<Variable> childVars = process.getVariables();
        Map<String,String> parameters = createVariableBindings(childVars, service, false);
        // create ProcessInstance and its variable instances
        return getEngine().createProcessInstance(
                process.getId(), OwnerType.PROCESS_INSTANCE,
                getProcessInstanceId(), OwnerType.ACTIVITY_INSTANCE, getActivityInstanceId(),
                getMasterRequestId(), parameters);
    }

    /**
     * This method returns variable bindings to be passed into subprocess.
     * The method uses the attribute "variables" values as a mapping.
     * The binding of each variable is an expression in the Magic Box rule language.
     * Example bindings: "var1=12*12;var2=$parent_var.LIST.LN"
     * Subclass may override this method to obtain variable binding in other ways.
     *
     * @param childVars variables defined for the child process
     * @param prMgr process manager remote EJB handle
     * @param varMgr variable manager remote EJB handle
     * @return a map (name-value pairs) of variable bindings
     * @throws Exception various types of exceptions
     */
    private Map<String,String> createVariableBindings(List<Variable> childVars, Microservice service,
            boolean passDocumentContent) throws ActivityException {
        Map<String,String> parameters = new HashMap<>();
        for (int i = 0; i < childVars.size(); i++) {
            Variable childVar = childVars.get(i);
            if (!allowInput(childVar))
                continue;
            String subflowVarName = childVar.getName();
            String value;
            if (subflowVarName.equals(VariableConstants.REQUEST)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.REQUEST);
                value = varinst == null ? null : varinst.getStringValue();
            }
            else if (subflowVarName.equals(VariableConstants.MASTER_DOCUMENT)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.MASTER_DOCUMENT);
                value = varinst == null ? null : varinst.getStringValue();
            }
            else {
                String bindingExpression = getBindingExpression(service, subflowVarName);
                value = evaluateBindingValue(childVar, bindingExpression);
            }
            if (value != null && value.length() > 0) {
                if (passDocumentContent) {
                    if (VariableTranslator.isDocumentReferenceVariable(getPackage(),
                            childVar.getType()) && value.startsWith("DOCUMENT:")) {
                        value = super.getDocumentContent(new DocumentReference(value));
                    }
                }
                parameters.put(subflowVarName, value);
            }
        }
        return parameters;
    }

    protected String getBindingExpression(Microservice service, String subflowVar) {
        for (String bindingExpression : service.getBindings().keySet()) {
            String varName = service.getBindings().get(bindingExpression);
            if (varName.equals(subflowVar))
                return bindingExpression;
        }
        return null;
    }

    protected boolean resumeOnProcessFinish(InternalEvent msg, Integer status)
            throws ActivityException {
        boolean done;
        try {
            Long procInstId = msg.getWorkInstanceId();
            ServicePlan servicePlan = getServicePlan();
            done = true;
            ServiceSummary summary = getServiceSummary(true);
            for (Microservice service : servicePlan.getServices()) {
                MicroserviceHistory history = summary.getMicroservice(service.getName());
                if (history.getInstanceId().equals(procInstId)) {
                    history.setStatusCode(WorkStatus.STATUS_COMPLETED.intValue());
                    // TODO OUTPUT PARAMS
                    Map<String,String> params = getOutputParameters(procInstId, msg.getWorkId());
                    if (params != null) {
                        for (String paramName : params.keySet()) {
                            String bindingExpression = getBindingExpression(service, paramName);
                            if (bindingExpression != null)
                                bindVariable(bindingExpression, params.get(paramName), false);
                        }
                    }
                }
                if (history.getStatusCode() != WorkStatus.STATUS_COMPLETED.intValue()
                        && history.getStatusCode() != WorkStatus.STATUS_CANCELLED.intValue()) {
                    done = false;
                }
            }
            setVariableValue(getServiceSummaryVariableName(), summary);
        }
        catch (Exception ex) {
            logexception("InvokeHeterogeneousProcessActivity: cannot get variable instance", ex);
            if (ex instanceof ActivityException)
                throw (ActivityException)ex;
            else
                throw new ActivityException(-1, ex.getMessage(), ex);
        }
        if (done && status.equals(WorkStatus.STATUS_HOLD))
            done = false;
        if (done)
            deregisterEvents();
        return done;
    }

    protected boolean allSubProcessCompleted() throws ActivityException, XmlException {
        ServicePlan plan = getServicePlan();
        ServiceSummary summary = getServiceSummary(false);
        for (Microservice service : plan.getServices()) {
            MicroserviceHistory history = summary.getMicroservice(service.getName());
            if (history.getStatusCode() != WorkStatus.STATUS_COMPLETED.intValue()
                    && history.getStatusCode() != WorkStatus.STATUS_CANCELLED.intValue()) {
                return false;
            }
        }
        return true;
    }

    private void executeServiceSubflowsInSequence(ServicePlan servicePlan) throws ActivityException {
        List<ProcessInstance> procInstList = new ArrayList<ProcessInstance>();
        ServiceSummary summary = getServiceSummary(true);
        try {
            for (Microservice service : servicePlan.getServices()) {
                if (summary.getMicroservice(service.getName()).getStatusCode() == WorkStatus.STATUS_PENDING_PROCESS) {
                    MicroserviceHistory history = summary.getMicroservice(service.getName());
                    try {
                        ProcessInstance processInstance = createProcessInstance(service);
                        procInstList.add(processInstance);
                        history.setInstanceId(processInstance.getId());
                        history.setStatusCode(WorkStatus.STATUS_IN_PROGRESS);
                    }
                    catch (Exception ex) {
                        history.setStatusCode(WorkStatus.STATUS_FAILED);
                        throw ex;
                    }
                }
            }
        }
        catch (ProcessException | DataAccessException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
        finally {
            setVariableValue(getServiceSummaryVariableName(), summary);
        }

        if (!getEngine().isInService()) {
            EventWaitInstance received = registerWaitEvents(false, true);
            if (received != null) {
                resumeOnOtherEvent(
                    getExternalEventInstanceDetails(received.getMessageDocumentId()),
                    received.getCompletionCode());
            }
        }
        // send JMS message at the end to ensure database changes are committed
        try {
            int delay = getDelay();
            for (int k = 0; k < procInstList.size(); k++) {
                getEngine().startProcessInstance(procInstList.get(k), delay * k + delay);
            }
        }
        catch (ProcessException | DataAccessException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    private void executeServiceSubflowsInParallel(ServicePlan servicePlan) throws ActivityException {
        List<Microservice> serviceList = servicePlan.getServices();
        SubprocessRunner[] allRunners = new SubprocessRunner[(serviceList.size())];
        List<SubprocessRunner> activeRunners = new ArrayList<SubprocessRunner>(serviceList.size());
        for (int i = 0; i < serviceList.size(); i++) {
            SubprocessRunner runner = new SubprocessRunner(serviceList.get(i), i, activeRunners);
            allRunners[i] = runner;
            activeRunners.add(runner);
        }
        ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
        int pollInterval = PropertyManager.getIntegerProperty(
                PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL,
                PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL_DEFAULT);
        for (int i = 0; i < allRunners.length; i++) {
            while (!threadPool.execute(ThreadPoolProvider.WORKER_ENGINE, "ServiceSubProcess",
                    allRunners[i])) {
                try {
                    String msg = ThreadPoolProvider.WORKER_ENGINE  + " has no thread available to launch subflow";
                    logexception(msg, new Exception(msg));
                    loginfo(threadPool.currentStatus());
                    Thread.sleep(pollInterval * 1000);
                }
                catch (InterruptedException e) {
                }
            }
        }
        synchronized (activeRunners) {
            while (activeRunners.size() > 0) {
                try {
                    activeRunners.wait();
                }
                catch (InterruptedException e) {
                }
            }
        }

        ServiceSummary summary = getServiceSummary(true);
        boolean hasFailedSubprocess = false;
        for (int i = 0; i < allRunners.length; i++) {
            Microservice service = serviceList.get(i);
            Long subprocInstId = allRunners[i].procInstId;
            Map<String,String> outParameters = allRunners[i].outParameters;
            MicroserviceHistory history = summary.getMicroservice(service.getName());
            if (subprocInstId != null)
                history.setInstanceId(subprocInstId);
            if (outParameters == null) {
                hasFailedSubprocess = true;
                history.setStatusCode(WorkStatus.STATUS_FAILED);
            }
            else {
                history.setStatusCode(WorkStatus.STATUS_COMPLETED);
                for (String paramName : outParameters.keySet()) {
                    String bindingExpression = getBindingExpression(service, paramName);
                    if (bindingExpression != null) {
                        bindVariable(bindingExpression, outParameters.get(paramName),
                                (getEngine().getPerformanceLevel() >= 5 || getEngine().isInService()));
                    }
                }
            }
        }
        setVariableValue(getServiceSummaryVariableName(), summary);
        if (hasFailedSubprocess)
            throw new ActivityException("At least one subflow is not completed");
        onFinish();
    }

    private List<String> updatedOutputVariables = new ArrayList<>();

    private void bindVariable(String bindingExpression, String value, boolean passDocContent) throws ActivityException {
        Process procdef = getMainProcessDefinition();
        if (bindingExpression.equals("$")) {
            // TODO
            // param.setStringValue(value);
        }
        else if (bindingExpression.startsWith("$")) {
            String varName = bindingExpression.substring(1).trim();
            if (varName.startsWith("{") && varName.endsWith("}"))
                varName = varName.substring(1, varName.length() - 1);
            Variable var = procdef.getVariable(varName);
            if (var != null) {
                Object varValue = null;
                if (passDocContent && VariableTranslator.isDocumentReferenceVariable(getPackage(), var.getType())) {
                    if (value != null && !value.isEmpty()) {
                        if (value.startsWith("DOCUMENT:")) {
                            varValue = VariableTranslator.toObject(var.getType(), value);
                        }
                        else {
                            synchronized (updatedOutputVariables) {
                                if (updatedOutputVariables.contains(varName)) {
                                    throw new ActivityException("Output variable: " + varName + " already updated by another subflow");
                                }
                                else {
                                    updatedOutputVariables.add(varName);
                                }
                            }
                            DocumentReference docref = super.createDocument(var.getType(), value,
                                    OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId());
                            varValue = new DocumentReference(docref.getDocumentId());
                        }
                    }
                }
                else {
                    varValue = VariableTranslator.toObject(var.getType(), value);
                }
                this.setParameterValue(varName, varValue);
            }
        }
    }

    private class SubprocessRunner implements Runnable {
        private Microservice service;
        private List<SubprocessRunner> runners;
        private Map<String,String> outParameters = null;
        private Long procInstId = null;

        private SubprocessRunner(Microservice service, int index, List<SubprocessRunner> runners) {
            this.runners = runners;
            this.service = service;
        }

        public void run() {
            ProcessEngineDriver engineDriver = null;
            String logicalProcName = service.getName();
            try {
                loginfo("New thread for executing service subprocess in parallel - " + logicalProcName);
                Process process = getTemplateProcess(service);
                engineDriver = new ProcessEngineDriver();
                List<Variable> childVars = process.getVariables();
                int perfLevel = getEngine().getPerformanceLevel();
                Map<String,String> parameters = createVariableBindings(childVars, service, perfLevel >= 5);
                outParameters = engineDriver.invokeServiceAsSubprocess(process.getId(),
                        getProcessInstanceId(), getMasterRequestId(), parameters, perfLevel);
                procInstId = engineDriver.getMainProcessInstanceId();
            }
            catch (Exception e) {
                if (engineDriver != null)
                    procInstId = engineDriver.getMainProcessInstanceId();
                logexception("Failed to execute subprocess in thread - " + logicalProcName, e);
            }
            finally {
                loginfo("Thread for executing subflow in parallel terminates - " + logicalProcName);
                synchronized (runners) {
                    runners.remove(SubprocessRunner.this);
                    runners.notifyAll();
                }
            }
        }
    }
}
