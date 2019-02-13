package com.centurylink.mdw.microservice;

import java.sql.SQLException;
import java.time.Instant;
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
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
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
        ServiceSummary serviceSummary = getServiceSummary(true);
        ServiceSummary subServiceSummary = null;
        if (serviceSummary == null) {
            subServiceSummary = serviceSummary = new ServiceSummary(getMasterRequestId(), getActivityInstanceId());
        }
        else {
            // Find correct parent service summary to add new child to
            subServiceSummary = serviceSummary.findParent(getProcessInstanceId());
            if (subServiceSummary == null)
                subServiceSummary = serviceSummary;

            subServiceSummary.addServiceSummary(
                    subServiceSummary.getMasterRequestId(), getActivityInstanceId());
        }
        String serviceSummaryVar = getServiceSummaryVariableName();
        setVariableValue(serviceSummaryVar, serviceSummary);
        // update bindings with serviceSummary value
        boolean planNeedsUpdate = false;
        for (Microservice plannedService : servicePlan.getServices()) {
            Map<String,Object> bindings = plannedService.getBindings();
            if (bindings.containsKey(serviceSummaryVar) && bindings.get(serviceSummaryVar) == null) {
                bindings.put(serviceSummaryVar, getParameterStringValue(serviceSummaryVar));
                planNeedsUpdate = true;
            }
        }
        if (planNeedsUpdate) {
            setVariableValue(getServicePlanVariableName(), servicePlan);
        }

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
        return getAttribute(PARALLEL, false);
    }

    protected ServicePlan getServicePlan() throws ActivityException {
        return (ServicePlan)getRequiredVariableValue(getServicePlanVariableName());
    }

    /**
     * You'd need a custom .impl asset to set this through designer.
     * To view instances in MDWHub you'd also need to customize inspector-tabs.js.
     */
    protected String getServicePlanVariableName() {
        return getAttribute("servicePlanVariable", "servicePlan");
    }

    protected ServiceSummary getServiceSummary(boolean forUpdate) throws ActivityException {
        DocumentReference docRef = (DocumentReference) getParameterValue(getServiceSummaryVariableName());
        if (docRef == null)
            return null;
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

    /**
     * Can be a process or template.
     */
    protected Process getSubflow(Microservice service) throws ActivityException {
        AssetVersionSpec spec = new AssetVersionSpec(service.getSubflow());
        try {
            Process process = ProcessCache.getProcessSmart(spec);
            if (process == null)
                throw new ActivityException("Subflow not found: " + service.getSubflow());
            return process;
        }
        catch (DataAccessException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    protected ProcessInstance createProcessInstance(int index, Microservice service)
            throws ActivityException, DataAccessException, ProcessException {
        Process process = getSubflow(service);
        // create bindings
        List<Variable> childVars = process.getVariables();
        Map<String,String> parameters = createBindings(childVars, index, service, false);
        // create process instance
        return getEngine().createProcessInstance(
                process.getId(), OwnerType.PROCESS_INSTANCE,
                getProcessInstanceId(), OwnerType.ACTIVITY_INSTANCE, getActivityInstanceId(),
                getMasterRequestId(), parameters);
    }

    /**
     * Returns variable bindings to be passed into subprocess.
     */
    protected Map<String,String> createBindings(List<Variable> childVars, int index, Microservice service,
            boolean passDocumentContent) throws ActivityException {
        Map<String,String> parameters = new HashMap<>();
        for (int i = 0; i < childVars.size(); i++) {
            Variable childVar = childVars.get(i);
            if (childVar.isInput()) {
                String subflowVarName = childVar.getName();
                Object value = service.getBindings().get(subflowVarName);
                if (value != null) {
                    String stringValue = String.valueOf(value);
                    if (passDocumentContent) {
                        if (VariableTranslator.isDocumentReferenceVariable(getPackage(),
                                childVar.getType()) && stringValue.startsWith("DOCUMENT:")) {
                            stringValue = getDocumentContent(new DocumentReference(stringValue));
                        }
                    }
                    parameters.put(subflowVarName, stringValue);
                }
            }
        }
        String processName = getSubflow(service).getName();
        if (processName.startsWith("$") && parameters.get(processName) == null) {
            // template variable will populate process name
            parameters.put(processName, service.getName());
        }
        if (parameters.get("i") == null)
            parameters.put("i", String.valueOf(index));
        return parameters;
    }

    protected boolean resumeOnProcessFinish(InternalEvent msg, Integer status)
            throws ActivityException {
        boolean done;
        try {
            Long procInstId = msg.getWorkInstanceId();
            ServicePlan servicePlan = getServicePlan();
            done = true;
            ServiceSummary summary = getServiceSummary(true);
            ServiceSummary currentSummary = summary.findCurrent(getActivityInstanceId());
            if (currentSummary == null)
                currentSummary = summary;
            for (Microservice service : servicePlan.getServices()) {
                for (MicroserviceInstance instance : currentSummary.getMicroservices(service.getName()).getInstances()) {
                    if (!instance.getStatus().equals(WorkStatus.STATUSNAME_COMPLETED)
                            && !instance.getStatus().equals(WorkStatus.STATUSNAME_CANCELED)) {
                        done = false;
                    }
                }
            }
        }
        catch (Exception ex) {
            logexception(ex.getMessage(), ex);
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
        ServiceSummary currentSummary = summary.findCurrent(getActivityInstanceId());
        if (currentSummary == null)
            currentSummary = summary;
        for (Microservice service : plan.getServices()) {
            for (MicroserviceInstance instance : currentSummary.getMicroservices(service.getName()).getInstances()) {
                if (!instance.getStatus().equals(WorkStatus.STATUSNAME_COMPLETED)
                        && !instance.getStatus().equals(WorkStatus.STATUSNAME_CANCELED)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void executeServiceSubflowsInSequence(ServicePlan servicePlan) throws ActivityException {
        List<ProcessInstance> procInstList = new ArrayList<ProcessInstance>();
        ServiceSummary summary = getServiceSummary(true);
        ServiceSummary currentSummary = summary.findCurrent(getActivityInstanceId());
        try {
            for (Microservice service : servicePlan.getServices()) {
                if (service.getEnabled()) {
                    MicroserviceInstance instance = null;
                    for (int i = 0 ; i < service.getCount(); i++) {
                        try {
                            ProcessInstance processInstance = createProcessInstance(i, service);
                            instance = currentSummary.addMicroservice(service.getName(), processInstance.getId());
                            procInstList.add(processInstance);
                            instance.setId(processInstance.getId());
                            instance.setStatus(WorkStatus.STATUSNAME_IN_PROGRESS);
                            instance.setTriggered(Instant.now());
                        }
                        catch (Exception ex) {
                            if (instance != null)
                                instance.setStatus(WorkStatus.STATUSNAME_FAILED);
                            throw ex;
                        }
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

    protected void executeServiceSubflowsInParallel(ServicePlan servicePlan) throws ActivityException {
        // create runners and service summary
        List<SubprocessRunner> allRunners = new ArrayList<>();
        List<SubprocessRunner> activeRunners = new ArrayList<>();
        ServiceSummary summary = getServiceSummary(true);
        ServiceSummary currentSummary = summary.findCurrent(getActivityInstanceId());
        for (Microservice service : servicePlan.getServices()) {
            if (service.getEnabled()) {
                MicroserviceInstance instance = null;
                for (int i = 0; i < service.getCount(); i++) {
                    SubprocessRunner runner = new SubprocessRunner(i, service, activeRunners);
                    allRunners.add(runner);
                    activeRunners.add(runner);
                    // Add with id of 0, will get updated by microserviceRestAdapter in child
                    instance = currentSummary.addMicroservice(service.getName(), 0L);
                    instance.setId(0L);
                    instance.setStatus(WorkStatus.STATUSNAME_IN_PROGRESS);
                    instance.setTriggered(Instant.now());
                }
            }
        }

        setVariableValue(getServiceSummaryVariableName(), summary);
        try { // Need to commit the changes to serviceSummary to release DB lock - Only when running parallel
            this.getEngine().getDatabaseAccess().commit();
        } catch (SQLException e) {
            logexception("Error while committing Service Summary changes", e);
        }

        // spawn runner threads
        ThreadPoolProvider threadPool = ApplicationContext.getThreadPoolProvider();
        int pollInterval = PropertyManager.getIntegerProperty(
                PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL,
                PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL_DEFAULT);
        for (SubprocessRunner runner : allRunners) {
            while (!threadPool.execute(ThreadPoolProvider.WORKER_ENGINE, "ServiceSubProcess", runner)) {
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

        summary = getServiceSummary(true);
        currentSummary = summary.findCurrent(getActivityInstanceId());
        boolean hasFailedSubprocess = false;
        for (SubprocessRunner runner : allRunners) {
            MicroserviceInstance instance = null;
            // Update Microservice with child's ProcInstId in case in never happened in child
            if (runner.getProcInstId() != null) {
                instance = currentSummary.getMicroservice(runner.getService().getName(), runner.getProcInstId());
                if (runner.isSuccess())
                    instance.setStatus(WorkStatus.STATUSNAME_COMPLETED);
                else {
                    hasFailedSubprocess = true;
                    instance.setStatus(WorkStatus.STATUSNAME_FAILED);
                }
            }
            else
                hasFailedSubprocess = true;
        }
        setVariableValue(getServiceSummaryVariableName(), summary);
        if (hasFailedSubprocess)
            throw new ActivityException("At least one subflow is not completed");
        onFinish();
    }

    private class SubprocessRunner implements Runnable {
        private Microservice service;
        private List<SubprocessRunner> runners;
        private Long procInstId = null;
        private boolean success;
        private int index;

        private SubprocessRunner(int index, Microservice service, List<SubprocessRunner> runners) {
            this.index = index;
            this.runners = runners;
            this.service = service;
        }

        public void run() {
            ProcessEngineDriver engineDriver = null;
            String logicalProcName = service.getName();
            try {
                loginfo("New thread for executing service subprocess in parallel - " + logicalProcName);
                Process process = getSubflow(service);
                engineDriver = new ProcessEngineDriver();
                List<Variable> childVars = process.getVariables();
                int perfLevel = getEngine().getPerformanceLevel();
                Map<String,String> parameters = createBindings(childVars, index, service, perfLevel >= 5);
                success = engineDriver.invokeServiceAsSubprocess(process.getId(),
                        getProcessInstanceId(), getMasterRequestId(), parameters, perfLevel) != null;
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

        public Microservice getService() {return service;}
        public boolean isSuccess() {return success;}
        public Long getProcInstId() {return procInstId;}
    }
}
