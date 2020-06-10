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
package com.centurylink.mdw.workflow.activity.process;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.InvokeProcessActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.bpm.ParameterDocument.Parameter;
import com.centurylink.mdw.bpm.ProcessExecutionPlanDocument;
import com.centurylink.mdw.bpm.SubprocessInstanceDocument.SubprocessInstance;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.VariableConstants;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import org.yaml.snakeyaml.Yaml;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This activity implementor implements invocation of sub processes.
 *
 *
 */
@Tracked(LogLevel.TRACE)
@Activity(value="Invoke Multiple Subprocesses", category=InvokeProcessActivity.class, icon="com.centurylink.mdw.base/multisub.png",
        pagelet="com.centurylink.mdw.base/invokeMultiple.pagelet")
public class InvokeHeterogeneousProcessActivity extends InvokeProcessActivityBase {

    public static final String EXECUTION_PLAN_VARIABLE = "Execution Plan";
    private static final String DELAY_BETWEEN = "DELAY_BETWEEN";
    private static final String FORCE_PARALLEL = "Force Parallel Execution";

    private boolean emptyPlan;
    private boolean inService;
    private boolean forceParallel;
    @SuppressWarnings("squid:S1845")
    private boolean synchronous;

    private Map<String, Boolean> outputVariableUpdated = new HashMap<String, Boolean>();

    /**
     * Default constructor with params
     */
    public InvokeHeterogeneousProcessActivity(){
        super();
    }

    @Override
    public boolean needSuspend() {
        if (emptyPlan) return false;
        if (inService && forceParallel && synchronous) return false;
        return synchronous;
    }

    protected ProcessExecutionPlanDocument getProcessExecutionPlan()
            throws ActivityException, XmlException {
        String plan_varname = getAttributeValue(EXECUTION_PLAN_VARIABLE);
        Object binding = this.getParameterValue(plan_varname);
        if (binding==null || ! (binding instanceof DocumentReference))
            throw new ActivityException("InvokeHeterogenenousProcess: "
                    + "control variable is not bound to a process plan");
        DocumentReference docref = (DocumentReference)binding;
        binding = super.getDocumentForUpdate(docref, getParameterType(plan_varname));
        if  (binding instanceof ProcessExecutionPlanDocument) {
            return (ProcessExecutionPlanDocument)binding;
        } else if (binding instanceof XmlObject) {
            return (ProcessExecutionPlanDocument)((XmlObject)binding).changeType(ProcessExecutionPlanDocument.type);
        } else {
            Variable docVar = getProcessDefinition().getVariable(plan_varname);
            XmlDocumentTranslator docRefTrans = (XmlDocumentTranslator)getPackage().getTranslator(docVar.getType());
            Document doc = docRefTrans.toDomDocument(binding);
            return ProcessExecutionPlanDocument.Factory.parse(doc);
        }
    }

    private void updateExecutionPlan(ProcessExecutionPlanDocument exePlan)
        throws ActivityException {
        String plan_varname = getAttributeValue(EXECUTION_PLAN_VARIABLE);
        DocumentReference docref = (DocumentReference)getParameterValue(plan_varname);
        String varType = getProcessDefinition().getVariable(plan_varname).getType();
        String str;
        if (Yaml.class.getName().equals(varType))  { // TODO better way that supports other types like JSONObject and Jsonable
            DocumentReferenceTranslator translator = (DocumentReferenceTranslator)getPackage().getTranslator(varType);
            Object obj = ((XmlDocumentTranslator)translator).fromDomNode(exePlan.getDomNode());
            str = translator.toString(obj, varType);
        }
        else {
            str = exePlan.xmlText();
        }
        super.updateDocumentContent(docref, str, getParameterType(plan_varname));
    }

    private Process getSubprocess(String logicalProcName) throws Exception {
        List<String[]> procmap;
        if (getAttributes().containsKey(WorkAttributeConstant.PROCESS_MAP)) {
            procmap = getAttributes().getTable(WorkAttributeConstant.PROCESS_MAP, ',', ';', 3);
        }
        else {
            procmap = new ArrayList<>();
        }

        for (int i = 0; i < procmap.size(); i++) {
            if (procmap.get(i)[0].equals(logicalProcName)) {
                String subproc_name = procmap.get(i)[1];
                String v = procmap.get(i)[2];
                return super.getSubprocess(subproc_name, v);
            }
        }
        return null;
    }

    public void execute() throws ActivityException{
        emptyPlan = false;
        try {
            ProcessExecutionPlanDocument process_plan = getProcessExecutionPlan();
            String delayStr = getAttributeValue(DELAY_BETWEEN);
            int pDelay = (delayStr==null)?0:Integer.parseInt(delayStr);
            ProcessExecutor engine = getEngine();
            String v = getAttributeValueSmart(SYNCHRONOUS);
            synchronous = StringUtils.isBlank(v) || v.equalsIgnoreCase("true");
            forceParallel = "true".equalsIgnoreCase(getAttributeValue(FORCE_PARALLEL));
            inService = engine.isInService();
            if (inService && forceParallel && synchronous) {
                emptyPlan = process_plan.getProcessExecutionPlan().getSubprocessInstanceList().size()==0;
                if (!emptyPlan) executeServiceSubprocessesInParallel(process_plan);
            } else {
                int i = 0;
                List<ProcessInstance> procInstList = new ArrayList<ProcessInstance>();
                for (SubprocessInstance piplan : process_plan.getProcessExecutionPlan().getSubprocessInstanceList()) {
                    if (piplan.getStatusCode()!=WorkStatus.STATUS_PENDING_PROCESS) continue;
                    procInstList.add(createProcessInstance(piplan));
                    i++;
                }
                if (i==0) emptyPlan = true;
                this.updateExecutionPlan(process_plan);
                if (!engine.isInService()) {
                    EventWaitInstance received = registerWaitEvents(false);
                    if (received!=null)
                        resumeOnOtherEvent(getExternalEventInstanceDetails(received.getMessageDocumentId()), received.getCompletionCode());
                }
                // send JMS message at the end to ensure database changes are committed
                if (!emptyPlan) {
                    if (synchronous && inService) pDelay = 0;
                    else if (forceParallel && pDelay<=0) pDelay = 1;
                    for (int k=0; k<procInstList.size(); k++) {
                        engine.startProcessInstance(procInstList.get(k), pDelay*k+pDelay);
                    }
                }
            }
        } catch (ActivityException ex) {
            throw ex;
        }catch(Exception ex){
            logError(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    private ProcessInstance createProcessInstance(SubprocessInstance piplan)
            throws Exception {
        try {
            // prepare variable bindings
            Process process = getSubprocess(piplan.getLogicalProcessName());
            if (process == null)
                throw new Exception("Cannot find process with logical name " + piplan.getLogicalProcessName());
            List<Variable> childVars = process.getVariables();
            Map<String,String> parameters = createVariableBindings(childVars, piplan, false);
            // create ProcessInstance and its variable instances
            ProcessInstance pi = getEngine().createProcessInstance(
                    process.getId(), OwnerType.PROCESS_INSTANCE,
                    getProcessInstanceId(), OwnerType.ACTIVITY_INSTANCE, getActivityInstanceId(),
                    getMasterRequestId(), new HashMap<>(parameters));
            piplan.setInstanceId(pi.getId().toString());
            // create initial transition instance
            piplan.setStatusCode(WorkStatus.STATUS_IN_PROGRESS);
            return pi;
        } catch (Exception ex) {
            piplan.setStatusCode(WorkStatus.STATUS_FAILED);
            logError(ex.getMessage(), ex);
            throw new ActivityException("Error loading subproc: " + piplan.getLogicalProcessName(), ex);
        }
    }

    private Parameter getParameterBinding(SubprocessInstance piplan, String pn) {
        for (Parameter p : piplan.getParameterList()) {
            if (pn.equals(p.getName())) return p;
        }
        return null;
    }

    /**
     * This method returns variable bindings to be passed into subprocess.
     * The method uses the attribute "variables" values as a mapping.
     * The binding of each variable is an expression.
     * Example: "myVar=${greeterVar.greeting}"
     * Subclass may override this method to obtain variable binding in other ways.
     *
     * @param childVars variables defined for the child process
     * @param subprocInstance subprocess instance
     * @param passDocumentContent whether to pass string content
     * @return a map (name-value pairs) of variable bindings
     * @throws Exception various types of exceptions
     */
    private Map<String,String> createVariableBindings(List<Variable> childVars,
            SubprocessInstance subprocInstance, boolean passDocumentContent) throws Exception {
        Map<String,String> parameters = new HashMap<>();

        String vn, v;
        for (int k= 0; k < childVars.size(); k++) {
            Variable childVar = childVars.get(k);
            if (!allowInput(childVar))
                continue;
            vn = childVar.getName();
            if (vn.equals(VariableConstants.REQUEST)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.REQUEST);
                v = varinst == null ? null : varinst.getStringValue(getPackage());
            } else if (vn.equals(VariableConstants.MASTER_DOCUMENT)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.MASTER_DOCUMENT);
                v = varinst == null ? null : varinst.getStringValue(getPackage());
            } else {
                Parameter p = this.getParameterBinding(subprocInstance, vn);
                v = evaluateBindingValue(childVar, p == null ? null : p.getStringValue());
            }
            if (v!=null && v.length()>0) {
                if (passDocumentContent) {
                    if (getPackage().getTranslator(childVar.getType()).isDocumentReferenceVariable()
                            && v.startsWith("DOCUMENT:")) {
                        v = super.getDocumentContent(new DocumentReference(v));
                    }
                }
                parameters.put(vn, v);
            }
        }
        return parameters;
    }

    protected boolean resumeOnProcessFinish(InternalEvent msg, Integer status)
            throws ActivityException {
        boolean done;
        try {
            Long procInstId = msg.getWorkInstanceId();
            ProcessExecutionPlanDocument plan = this.getProcessExecutionPlan();
            done = true;
            for (SubprocessInstance piplan : plan.getProcessExecutionPlan().getSubprocessInstanceList()) {
                if (piplan.getInstanceId().equals(procInstId.toString())) {
                    piplan.setStatusCode(WorkStatus.STATUS_COMPLETED.intValue());
                    Map<String,String> params = super.getOutputParameters(procInstId, msg.getWorkId());
                    if (params!=null) {
                        for (String paramName : params.keySet()) {
                            Parameter p = this.getParameterBinding(piplan, paramName);
                            if (p!=null) bindVariable(p, params.get(paramName), false);
                        }
                    }
                }
                if (piplan.getStatusCode()!=WorkStatus.STATUS_COMPLETED.intValue()
                        && piplan.getStatusCode()!=WorkStatus.STATUS_CANCELLED.intValue())
                    done = false;
            }
            this.updateExecutionPlan(plan);
        } catch (Exception ex) {
            logError("InvokeHeterogeneousProcessActivity: cannot get variable instance", ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
        if (done && status.equals(WorkStatus.STATUS_HOLD)) done = false;
        if (done) super.deregisterEvents();
        return done;
    }

    protected boolean allSubProcessCompleted() throws ActivityException, XmlException {
        ProcessExecutionPlanDocument plan = this.getProcessExecutionPlan();
        for (SubprocessInstance piplan : plan.getProcessExecutionPlan().getSubprocessInstanceList()) {
            if (piplan.getStatusCode()!=WorkStatus.STATUS_COMPLETED.intValue()
                    && piplan.getStatusCode()!=WorkStatus.STATUS_CANCELLED.intValue())
                return false;
        }
        return true;
    }

    private void executeServiceSubprocessesInParallel(ProcessExecutionPlanDocument execplan) throws ActivityException {
        List<SubprocessInstance> piplanList = execplan.getProcessExecutionPlan().getSubprocessInstanceList();
        SubprocessRunner[] allRunners = new SubprocessRunner[(piplanList.size())];
        List<SubprocessRunner> activeRunners = new ArrayList<SubprocessRunner>(piplanList.size());
        for (int k=0; k<piplanList.size(); k++) {
            SubprocessRunner runner = new SubprocessRunner(piplanList.get(k), k, activeRunners);
            allRunners[k] = runner;
            activeRunners.add(runner);
        }
        ThreadPoolProvider thread_pool = ApplicationContext.getThreadPoolProvider();
        int poll_interval = PropertyManager.getIntegerProperty(PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL, PropertyNames.MDW_JMS_LISTENER_POLL_INTERVAL_DEFAULT);
        for (int k=0; k<allRunners.length; k++) {
            while (!thread_pool.execute(ThreadPoolProvider.WORKER_ENGINE, "ServiceSubProcess", allRunners[k])) {
                try {
                    String msg = ThreadPoolProvider.WORKER_ENGINE + " has no thread available to launch heterogeneous process in parallel";
                    // make this stand out
                    logger.warn(msg, new Exception(msg));
                    logger.info(thread_pool.currentStatus());
                    Thread.sleep(poll_interval*1000L);
                }
                catch (InterruptedException e) {}
            }
        }
        synchronized (activeRunners) {
            while (activeRunners.size()>0) {
                try {
                    activeRunners.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        boolean hasFailedSubprocess = false;
        for (int k=0; k<allRunners.length; k++) {
            SubprocessInstance piplan = piplanList.get(k);
            Long subprocInstId = allRunners[k].procInstId;
            Map<String,String> outParameters = allRunners[k].outParameters;
            if (subprocInstId!=null) piplan.setInstanceId(subprocInstId.toString());
            if (outParameters==null) {
                hasFailedSubprocess = true;
                piplan.setStatusCode(WorkStatus.STATUS_FAILED.intValue());
            } else {
                piplan.setStatusCode(WorkStatus.STATUS_COMPLETED.intValue());
                for (String paramName : outParameters.keySet()) {
                    Parameter p = getParameterBinding(piplan, paramName);
                    if (p!=null)  // Since child executed in different engine, need to update this engine's documentCache (last arg passed in below)
                        bindVariable(p, outParameters.get(paramName), (getEngine().getPerformanceLevel() >= 5 || getEngine().isInService()), true);  // DHO
                }
            }
        }
        this.updateExecutionPlan(execplan);
        if (hasFailedSubprocess) throw new ActivityException("At least one subprocess is not completed");
        onFinish();
    }

    private class SubprocessRunner implements Runnable {
        SubprocessInstance piplan;
        List<SubprocessRunner> runners;
        Map<String,String> outParameters = null;
        Long procInstId = null;
        private SubprocessRunner(SubprocessInstance piplan, int index, List<SubprocessRunner> runners) {
            this.runners = runners;
            this.piplan = piplan;
        }
        public void run() {
            ProcessEngineDriver engineDriver = null;
            String logicalProcName = piplan.getLogicalProcessName();
            try {
                logger.info("New thread for executing service subprocess in parallel - " + logicalProcName);
                Process process = getSubprocess(logicalProcName);
                if (process == null)
                    throw new Exception("Cannot find process with logical name " + logicalProcName);
                engineDriver = new ProcessEngineDriver();
                List<Variable> childVars = process.getVariables();
                int perfLevel = getEngine().getPerformanceLevel();
                Map<String,String> parameters = createVariableBindings(childVars, piplan, perfLevel >= 5); // DHO
                outParameters = engineDriver.invokeSubprocess(process.getId(),
                        getProcessInstanceId(), getMasterRequestId(), new HashMap<>(parameters), perfLevel);

                procInstId = engineDriver.getMainProcessInstanceId();
            } catch (Exception e) {
                if (engineDriver != null)
                    procInstId = engineDriver.getMainProcessInstanceId();
                logError("Failed to execute subprocess in thread - " + logicalProcName, e);
            } finally {
                logger.info("Thread for executing subprocess in parallel terminates - " + logicalProcName);
                synchronized (runners) {
                    runners.remove(SubprocessRunner.this);
                    runners.notifyAll();
                }
            }
        }
    }

    private void bindVariable(Parameter param, String value, boolean passDocContent) throws ActivityException {
        bindVariable(param, value, passDocContent, false);
    }

    private void bindVariable(Parameter param, String value, boolean passDocContent, boolean refreshDocCache) throws ActivityException {
        Process procdef = getMainProcessDefinition();
        String binding = param.getStringValue();
        if (StringUtils.isBlank(binding)) return;
        if (binding.equals("$")) {
            param.setStringValue(value);
        } else if (binding.startsWith("$")) {
            String varname = binding.substring(1).trim();
            Variable var = procdef.getVariable(varname);
            if (var != null) {
                Object value0;
                if (passDocContent && getPackage().getTranslator(var.getType()).isDocumentReferenceVariable()) {
                    if (StringUtils.isBlank(value)) {
                        value0 = null;
                    }
                    else if (value.startsWith("DOCUMENT:")) {
                        value0 = getPackage().getObjectValue(var.getType(), value);
                    }
                    else {
                        synchronized(outputVariableUpdated) {
                            if (outputVariableUpdated.get(varname))
                                throw new ActivityException("Output Document variable value already returned by another sub process");
                            else
                                outputVariableUpdated.put(varname, Boolean.TRUE);
                        }
                        DocumentReference docref = createDocument(var.getType(), value, OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId());
                        value0 = new DocumentReference(docref.getDocumentId());

                     // check map for variable, not allowed multiple children to update same docs
                    }
                } else {
                    value0 = getPackage().getObjectValue(var.getType(), value);
                }

                this.setParameterValue(varname, value0);

                // Clear from this engine's documentCache map (forces getting from DB when next needed)
                if (refreshDocCache && value0 instanceof DocumentReference) {
                    try {
                        DatabaseAccess db = getEngine().getDatabaseAccess();
                        if (db != null && db.isMySQL() && db.connectionIsOpen())
                            db.commit();  // This is so that the current transaction can see newly created/updated documents from other transactions
                    }
                    catch (SQLException e) { // If for some reason db is null - Still try to load document below
                        logger.debug(e.getMessage());
                    }
                    try {
                        getEngine().loadDocument((DocumentReference)value0, false);
                    }
                    catch (DataAccessException e) {  // In mySQL, New documents created after this activity started its transaction are not findable in DB at this point, unless performing commit above
                        logger.debug(e.getMessage());
                    }
                }
            }
        }
    }

}
