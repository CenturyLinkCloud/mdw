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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.constant.VariableConstants;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

/**
 * This activity implementor implements invocation of subprocesses.
 */
@Tracked(LogLevel.TRACE)
public class InvokeSubProcessActivity extends InvokeProcessActivityBase {

    private static final String VARIABLES = "variables";
    private static final String SYNCHRONOUS = "synchronous";
    private boolean subprocIsService = false;
    private boolean passingByReference = true;

    private static final String ERR_OUTPARA = "Actual parameter for OUTPUT parameter is not a variable";

    public boolean needSuspend() {
        return (!getEngine().isInService() && subprocIsService && isSynchronousCall() && (getEngine().getPerformanceLevel() < 9 || !passingByReference))?false:this.isSynchronousCall();
    }

    private boolean isSynchronousCall() {
        String v = getAttributeValue(SYNCHRONOUS);
        return (v==null || v.equalsIgnoreCase("TRUE"));
    }

    /**
     * This method returns variable bindings to be passed into subprocess.
     * The method uses the attribute "variables" values as a mapping.
     * The binding of each variable is an expression in the Java Expression Language (beginning with '#'),
     * or the Magic Box rule language (beginning with '$').
     * Example bindings: #{myVar}_suffix, "var1=12*12;var2=$parent_var.LIST.LN"
     * Subclass may override this method to obtain variable binding in other ways.
     *
     * @param childVars variables defined for the child process
     * @param prMgr process manager remote EJB handle
     * @param varMgr variable manager remote EJB handle
     * @return a map (name-value pairs) of variable bindings
     * @throws Exception various types of exceptions
     */
    protected Map<String,String> createVariableBinding(List<Variable> childVars)
            throws Exception {
        Map<String,String> validParams = new HashMap<String,String>();
        String map = getAttributeValue(VARIABLES);
        if (map==null) map = "";
        String vn, v;
        for (Variable childVar : childVars) {
            if (!allowInput(childVar)) continue;
            vn = childVar.getVariableName();
            v = StringHelper.getMapValue(map, vn, ';');
            if (vn.equals(VariableConstants.REQUEST)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.REQUEST);
                v = varinst==null?null:varinst.getStringValue();
            } else if (vn.equals(VariableConstants.MASTER_DOCUMENT)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.MASTER_DOCUMENT);
                v = varinst==null?null:varinst.getStringValue();
            } else {
                v = evaluateBindingValue(childVar, v);
            }
            if (v != null && v.length() > 0)
                validParams.put(vn, v);
        }
        return validParams;
    }

    public void execute() throws ActivityException{
        try{
            Process subprocdef = getSubProcessVO();
            if (isLogDebugEnabled())
              logdebug("Invoking subprocess: " + subprocdef.getLabel());
            subprocIsService = subprocdef.getProcessType().equals(ProcessVisibilityConstant.SERVICE);
            List<Variable> childVars = subprocdef.getVariables();
            Map<String,String> validParams = createVariableBinding(childVars);
            passingByReference = containsDocReference(validParams);
            String ownerType = OwnerType.PROCESS_INSTANCE;
            String secondaryOwnerType = OwnerType.ACTIVITY_INSTANCE;
            Long secondaryOwnerId = getActivityInstanceId();
            Long ownerId = this.getProcessInstanceId();
            if (! this.needSuspend()) {
                secondaryOwnerId = null;
                secondaryOwnerType = null;
            }
            //If it is an asynchronous call to a regular process and the
            //parent process is running in a Memory Only thread, then the subprocess won't have any handle
            //to the parent process. So mark the Owner/OwnerId as the root process instance Owner/OwnerId
            if ((!isSynchronousCall() && getEngine().isInMemory()) ||
                    (!getEngine().isInService() && getEngine().isInMemory() && subprocIsService)) {
                Object[] rootProcessOwner = getRootProcessOwner(getProcessInstanceOwnerId(), getProcessInstanceOwner());
                ownerId = (Long) rootProcessOwner[0];
                ownerType = (String) rootProcessOwner[1];
            }
            InternalEvent evMsg = InternalEvent.createProcessStartMessage(
                    subprocdef.getProcessId(), ownerType,
                    ownerId, getMasterRequestId(), null,
                    secondaryOwnerType, secondaryOwnerId);
            ProcessExecutor engine = getEngine();

            if (engine.isInService()) {  // Current process is Non-service
                if (subprocIsService && isSynchronousCall()) {
                    ProcessInstance pi = getEngine().createProcessInstance(
                            subprocdef.getProcessId(), OwnerType.PROCESS_INSTANCE,
                            getProcessInstanceId(), secondaryOwnerType, secondaryOwnerId,
                            getMasterRequestId(), validParams);

                    engine.startProcessInstance(pi, 0);  // call directly using same engine so documents are visible to child when PerfLvl >= 5  */
                }
                else if (!isSynchronousCall()) {
                    if (engine.getPerformanceLevel() < 5 || !passingByReference) {
                        String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + secondaryOwnerId + "startproc" + subprocdef.getProcessId();
                        evMsg.setParameters(validParams);    // TODO this can be large!
                        engine.sendDelayedInternalEvent(evMsg, 0, msgid, false);
                    }
                    else // Trying to call a any sub process async when parent is service process and documents are cache-only / not visible to child
                        throw new ActivityException("Invalid attempt to asynchrounously launch sub process from a Service process running at performace level 5 or greater with Document variable bindings");
                }
                else  // Trying to call a non-service sub process sync when parent is service process - Regular proc could hold up/delay parent service process with event waits or manual task activities
                    throw new ActivityException("Invalid attempt to synchrounously launch Non-Service sub process from a Service process");
            }
            else {  // Current process is Non-service (regular)
                if (subprocIsService && isSynchronousCall() && (engine.getPerformanceLevel() < 9 || !passingByReference)) {
                    // Documents exist in DB and so are visible to child
                    ProcessEngineDriver engineDriver = new ProcessEngineDriver();
                    Map<String,String> params =  engineDriver.invokeServiceAsSubprocess(subprocdef.getProcessId(), ownerId, getMasterRequestId(),
                            validParams, subprocdef.getPerformanceLevel());
                    this.bindVariables(params, true);        // last arg should be true only when perf_level>=9 (DHO:actually 5), but this works
                }
                else if (isSynchronousCall() && engine.getPerformanceLevel() >= 9) {  // Documents are cache-only, need to execute in same thread and same engine - ignore child's perf lvl, if any specified
                        ProcessInstance pi = getEngine().createProcessInstance(
                                subprocdef.getProcessId(), OwnerType.PROCESS_INSTANCE,
                                getProcessInstanceId(), secondaryOwnerType, secondaryOwnerId,
                                getMasterRequestId(), validParams);

                        engine.startProcessInstance(pi, 0);  // call directly using same engine so documents are visible to child (PerfLvl 9)  */
                }
                else if (!passingByReference || isSynchronousCall() || (!isSynchronousCall() && engine.getPerformanceLevel() < 9)) {  // Sub is regular process
                    int perfLevel = subprocdef.getPerformanceLevel();
                    if (isSynchronousCall() && (perfLevel==0 || perfLevel==engine.getPerformanceLevel())) {
                        ProcessInstance pi = getEngine().createProcessInstance(
                                subprocdef.getProcessId(), OwnerType.PROCESS_INSTANCE,
                                getProcessInstanceId(), secondaryOwnerType, secondaryOwnerId,
                                getMasterRequestId(), validParams);
                        engine.startProcessInstance(pi, 0);
                    } else {   // Either Async, or mismatch of perf lvls between parent and child - run on new thread and engine
                        String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + secondaryOwnerId
                            + "startproc" + subprocdef.getProcessId();
                        evMsg.setParameters(validParams);    // TODO this can be large!
                        engine.sendDelayedInternalEvent(evMsg, 0, msgid, false);
                    }
                }
                else // Trying to call a any sub process async when parent is non-service process and documents are cache-only (lvl9) / not visible to child
                    throw new ActivityException("Invalid attempt to asynchrounously launch sub process from a Non-Service process running at performance level 9 with Document variable bindings");
            }
        } catch (ActivityException ex) {
            throw ex;
        }catch(Exception ex){
            logger.severeException("Exception in InvokeSubProcessActivity", ex);
            throw new ActivityException(-1, "Exception in InvokeSubProcessActivity", ex);
        }

    }

    boolean resume_on_process_finish(InternalEvent msg, Integer status)
        throws ActivityException {
        try{
            Long subprocInstId = msg.getWorkInstanceId();
            Map<String,String> params = super.getOutputParameters(subprocInstId, msg.getWorkId());
            this.bindVariables(params, false);

            String compcode = msg.getCompletionCode();
            if (compcode!=null && compcode.length()>0) this.setReturnCode(compcode);
            return true;
        } catch (ActivityException ex) {
            throw ex;
        }catch(Exception ex){
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }

    }

    private void bindVariables(Map<String,String> params, boolean passDocContent) throws ActivityException {
        String map = getAttributeValue(VARIABLES);
        if (map==null) map = "";
        if (params!=null) {
            Process procdef = getMainProcessDefinition();
            for (String varname : params.keySet()) {
                String para = getActualParameterVariable(map, varname);
                Variable var = procdef.getVariable(para);
                if (var == null)
                    throw new ActivityException("Bound variable: '" + para + "' not found in process definition " + procdef.getLabel());
                String varvalue = params.get(varname);
                Object value;
                if (passDocContent && VariableTranslator.isDocumentReferenceVariable(getPackage(), var.getVariableType())) {
                    if (StringHelper.isEmpty(varvalue)) value = null;
                    else if (varvalue.startsWith("DOCUMENT:"))
                        value = VariableTranslator.toObject(var.getVariableType(), varvalue);
                    else {
                        DocumentReference docref = super.createDocument(var.getVariableType(),
                                varvalue, OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId());
                        value = new DocumentReference(docref.getDocumentId());
                    }
                } else {
                    value = VariableTranslator.toObject(var.getVariableType(), varvalue);
                }
                this.setParameterValue(para, value);
            }
        }
    }

    /**
     * Override this as for single process invocation, no need to lock
     */
    @Override
    protected Integer lockActivityInstance() {
        return null;
    }

    private String getActualParameterVariable(String map, String parameterName)
            throws ActivityException {
        String v = StringHelper.getMapValue(map, parameterName, ';');
        if (v==null || v.length()<2 || (v.charAt(0)!='$' && v.charAt(0)!='#'))
            throw new ActivityException(ERR_OUTPARA + ": " + parameterName);
        for (int i=1; i<v.length(); i++) {
            if (!Character.isLetterOrDigit(v.charAt(i))&& v.charAt(i)!='_')
                throw new ActivityException(ERR_OUTPARA + ": " + parameterName);
        }
        return v.substring(1);
    }

    // currently not used - invoke single process does not handle other events
    protected boolean allSubProcessCompleted() throws Exception {
        return true;
    }

    protected Object[] getRootProcessOwner(Long ownerId, String owner) throws DataAccessException {
        if (!OwnerType.PROCESS_INSTANCE.equals(owner)) {
            return new Object[]{ownerId, owner};
        } else {
            ProcessInstance pi = null;
            try {
                pi = getEngine().getProcessInstance(ownerId);
            } catch (ProcessException e) {
                //This shouldn't happen as Engine has to be in memory only mode and Process Exception
                //can get thrown only if a DB call is being made
                logger.severe("InvokeSubprocess->getRootProcessOwner() -> Supposedly unreachable code");
            }
            if (null == pi) {
                //This means that the pi is not present in cache and can be found in DB
                pi = this.getProcInstFromDB(ownerId);
                if (null != pi)
                    return getRootProcessOwner(pi.getOwnerId(), pi.getOwner());
                else {
                    //Shouldn't happen as pi has to be there either in memory or in DB
                    logger.severe("getRootProcessOwner-> pi not found in DB for:" + ownerId);
                    return new Object[]{new Long(0), OwnerType.DOCUMENT};
                }
            } else {
                return getRootProcessOwner(pi.getOwnerId(), pi.getOwner());
            }
        }
    }

    /**
     * Method to get the Process Instance from the database
     * @param procInstId
     * @return
     * @throws DataAccessException
     */
    private ProcessInstance getProcInstFromDB(Long procInstId) throws DataAccessException {
        TransactionWrapper transaction = null;
        EngineDataAccessDB edao = new EngineDataAccessDB();
        try {
            transaction = edao.startTransaction();
            return edao.getProcessInstance(procInstId);
        } catch (SQLException e) {
            logger.severe("InvokeSubProcessActivity -> Failed to load process instance for " + procInstId);
            return null;
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    protected Process getSubProcessVO() throws DataAccessException {

        String name = getAttributeValue(WorkAttributeConstant.PROCESS_NAME);
        String verSpec = getAttributeValue(WorkAttributeConstant.PROCESS_VERSION);
        return getSubProcessVO(name, verSpec);
    }

    private boolean containsDocReference(Map<String, String> params) {
        for (String value : params.values()) {
            if (value != null && value.startsWith("DOCUMENT:"))
                return true;
        }
        return false;
    }
}
