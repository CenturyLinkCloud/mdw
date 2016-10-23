/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.activity.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import org.yaml.snakeyaml.Yaml;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.bpm.ParameterDocument.Parameter;
import com.centurylink.mdw.bpm.ProcessExecutionPlanDocument;
import com.centurylink.mdw.bpm.SubprocessInstanceDocument.SubprocessInstance;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.constant.VariableConstants;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.container.ThreadPoolProvider;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.translator.XmlDocumentTranslator;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

/**
 * This activity implementor implements invocation of sub processes.
 *
 *
 */
@Tracked(LogLevel.TRACE)
public class InvokeHeterogeneousProcessActivity extends InvokeProcessActivityBase {

    public static final String EXECUTION_PLAN_VARIABLE = "Execution Plan";
    private static final String DELAY_BETWEEN = "DELAY_BETWEEN";
    private static final String SYNCHRONOUS = "synchronous";
    private static final String FORCE_PARALLEL = "Force Parallel Execution";

    private boolean emptyPlan;
    private boolean inService;
    private boolean forceParallel;
    private boolean synchronous;

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
            XmlDocumentTranslator docRefTrans = (XmlDocumentTranslator)VariableTranslator.getTranslator(getPackage(), docVar.getVariableType());
            Document doc = docRefTrans.toDomDocument(binding);
            return ProcessExecutionPlanDocument.Factory.parse(doc, Compatibility.namespaceOptions());
        }
    }

    private void updateExecutionPlan(ProcessExecutionPlanDocument process_plan)
        throws ActivityException {
        String plan_varname = getAttributeValue(EXECUTION_PLAN_VARIABLE);
        DocumentReference docref = (DocumentReference)getParameterValue(plan_varname);
        String varType = getProcessDefinition().getVariable(plan_varname).getVariableType();
        String str;
        if (Yaml.class.getName().equals(varType))  { // TODO better way that supports other types like JSONObject and Jsonable
            DocumentReferenceTranslator translator = (DocumentReferenceTranslator) VariableTranslator.getTranslator(getPackage(), varType);
            Object obj = ((XmlDocumentTranslator)translator).fromDomNode(process_plan.getDomNode());
            str = translator.realToString(obj);
        }
        else {
            str = process_plan.xmlText();
        }
        super.updateDocumentContent(docref, str, getParameterType(plan_varname));
    }

    private Process getSubProcessVO(String logicalProcName) throws Exception {
        String map = getAttributeValue(WorkAttributeConstant.PROCESS_MAP);
        List<String[]> procmap;
        if (map==null) procmap = new ArrayList<String[]>();
        else procmap = StringHelper.parseTable(map, ',', ';', 3);
        for (int i=0; i<procmap.size(); i++) {
            if (procmap.get(i)[0].equals(logicalProcName)) {
                String subproc_name = procmap.get(i)[1];
                String v = procmap.get(i)[2];
                return super.getSubProcessVO(subproc_name, v);
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
            String v = getAttributeValue(SYNCHRONOUS);
            synchronous = StringHelper.isEmpty(v) || v.equalsIgnoreCase("true");
            forceParallel = "true".equalsIgnoreCase(getAttributeValue(FORCE_PARALLEL));
            inService = engine.isInService();
            if (inService && forceParallel && synchronous) {
                emptyPlan = process_plan.getProcessExecutionPlan().getSubprocessInstanceList().size()==0;
                if (!emptyPlan) execute_service_subprocess_in_parallel(process_plan);
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
                    EventWaitInstance received = registerWaitEvents(false, true);
                    if (received!=null)
                        resume_on_other_event(getExternalEventInstanceDetails(received.getMessageDocumentId()), received.getCompletionCode());
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
            super.logexception(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    private ProcessInstance createProcessInstance(SubprocessInstance piplan)
            throws Exception {
        try {
            // prepare variable bindings
            Process processVO = getSubProcessVO(piplan.getLogicalProcessName());
            if (processVO==null) throw new Exception(
                    "Cannot find process with logical name " + piplan.getLogicalProcessName());
            List<Variable> childVars = processVO.getVariables();
            Map<String,String> parameters = createVariableBinding(childVars, piplan, false);
            // create ProcessInstance and its variable instances
//            InternalEventVO procStartEvent = InternalEventVO.createProcessStartMessage(processVO.getProcessId(),
//                    OwnerType.PROCESS_INSTANCE, getProcessInstanceId(), getMasterRequestId(), null,
//                    OwnerType.ACTIVITY_INSTANCE, getActivityInstanceId());
            ProcessInstance pi = getEngine().createProcessInstance(
                    processVO.getProcessId(), OwnerType.PROCESS_INSTANCE,
                    getProcessInstanceId(), OwnerType.ACTIVITY_INSTANCE, getActivityInstanceId(),
                    getMasterRequestId(), parameters);
            piplan.setInstanceId(pi.getId().toString());
            // create initial transition instance
            piplan.setStatusCode(WorkStatus.STATUS_IN_PROGRESS);
            return pi;
        } catch (Exception ex) {
            piplan.setStatusCode(WorkStatus.STATUS_FAILED);
            super.logexception(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage());
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
    private Map<String,String> createVariableBinding(List<Variable> childVars, SubprocessInstance piplan,
            boolean passDocumentContent)
            throws Exception {
        Map<String,String> parameters = new HashMap<String,String>();

        String vn, v;
        for (int k=0; k<childVars.size(); k++) {
            Variable childVar = childVars.get(k);
            if (!allowInput(childVar)) continue;
            vn = childVar.getVariableName();
            if (vn.equals(VariableConstants.REQUEST)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.REQUEST);
                v = varinst==null?null:varinst.getStringValue();
            } else if (vn.equals(VariableConstants.MASTER_DOCUMENT)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.MASTER_DOCUMENT);
                v = varinst==null?null:varinst.getStringValue();
            } else {
                Parameter p = this.getParameterBinding(piplan, vn);
                v = evaluateBindingValue(childVar, p==null?null:p.getStringValue());
            }
            if (v!=null && v.length()>0) {
                if (passDocumentContent) {
                    if (VariableTranslator.isDocumentReferenceVariable(childVar.getVariableType())
                            && v.startsWith("DOCUMENT:")) {
                        v = super.getDocumentContent(new DocumentReference(v));
                    }
                }
                parameters.put(vn, v);
            }
        }
        return parameters;
    }

    boolean resume_on_process_finish(InternalEvent msg, Integer status)
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
            super.logexception("InvokeHeterogeneousProcessActivity: cannot get variable instance", ex);
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

    private void execute_service_subprocess_in_parallel(ProcessExecutionPlanDocument execplan) throws ActivityException {
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
                    logger.warnException(msg, new Exception(msg));
                    logger.info(thread_pool.currentStatus());
                    Thread.sleep(poll_interval*1000);
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
                    if (p!=null)
                        bindVariable(p, outParameters.get(paramName), (getEngine().getPerformanceLevel() >= 5 || getEngine().isInService()));  // DHO
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
                Process processVO = getSubProcessVO(logicalProcName);
                if (processVO==null) throw new Exception("Cannot find process with logical name " + logicalProcName);
                engineDriver = new ProcessEngineDriver();
                List<Variable> childVars = processVO.getVariables();
                int perfLevel = getEngine().getPerformanceLevel();
                Map<String,String> parameters = createVariableBinding(childVars, piplan, perfLevel >= 5); // DHO
                outParameters = engineDriver.invokeServiceAsSubprocess(processVO.getProcessId(),
                        getProcessInstanceId(), getMasterRequestId(), parameters, perfLevel);

                procInstId = engineDriver.getMainProcessInstanceId();
            } catch (Exception e) {
                if (engineDriver!=null) procInstId = engineDriver.getMainProcessInstanceId();
                logexception("Failed to execute subprocess in thread - " + logicalProcName, e);
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
        Process procdef = getMainProcessDefinition();
        String binding = param.getStringValue();
        if (StringHelper.isEmpty(binding)) return;
        if (binding.equals("$")) {
            param.setStringValue(value);
        } else if (binding.startsWith("$")) {
            String varname = binding.substring(1).trim();
            Variable var = procdef.getVariable(varname);
            if (var!=null) {
                Object value0;
                if (passDocContent && VariableTranslator.isDocumentReferenceVariable(var.getVariableType())) {
                    if (StringHelper.isEmpty(value)) value0 = null;
                    else if (value.startsWith("DOCUMENT:"))
                        value0 = VariableTranslator.toObject(var.getVariableType(), value);
                    else {
                        DocumentReference docref = super.createDocument(var.getVariableType(),
                                value, OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId());
                        value0 = new DocumentReference(docref.getDocumentId());
                    }
                } else {
                    value0 = VariableTranslator.toObject(var.getVariableType(), value);
                }
                this.setParameterValue(varname, value0);
            }
        }
    }

}
