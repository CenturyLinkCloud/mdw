package com.centurylink.mdw.workflow.activity.process;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.InvokeProcessActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.constant.VariableConstants;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.service.data.process.EngineDataAccessDB;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.process.ProcessEngineDriver;
import com.centurylink.mdw.services.process.ProcessExecutor;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This activity implementor implements invocation of subprocesses.
 */
@Tracked(LogLevel.TRACE)
@Activity(value="Invoke Subprocess", category=InvokeProcessActivity.class, icon="com.centurylink.mdw.base/process.jpg",
        pagelet="com.centurylink.mdw.base/invokeSubprocess.pagelet")
public class InvokeSubProcessActivity extends InvokeProcessActivityBase {

    private static final String VARIABLES = "variables";
    private boolean subprocIsService = false;
    private boolean passingByReference = true;

    private static final String ERR_OUTPARA = "Actual parameter for OUTPUT parameter is not a variable";

    public boolean needSuspend() {
        return (!getEngine().isInService() && subprocIsService && isSynchronousCall() && !getProcessDefinition().isService() && (getEngine().getPerformanceLevel() < 9 || !passingByReference))?false:this.isSynchronousCall();
    }

    private boolean isSynchronousCall() {
        String v = getAttributeValue(SYNCHRONOUS);
        return (v == null || v.equalsIgnoreCase("TRUE"));
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
     * @return a map (name-value pairs) of variable bindings
     * @throws Exception various types of exceptions
     */
    protected Map<String,String> createVariableBinding(List<Variable> childVars)
            throws Exception {
        Map<String,String> validParams = new HashMap<String,String>();
        String map = getAttributeValue(VARIABLES);
        if (map == null)
            map = "";
        String vn, v;
        for (Variable childVar : childVars) {
            if (!allowInput(childVar)) continue;
            vn = childVar.getName();
            v = getMapValue(map, vn, ';');
            if (vn.equals(VariableConstants.REQUEST)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.REQUEST);
                v = varinst == null ? null : varinst.getStringValue(getPackage());
            } else if (vn.equals(VariableConstants.MASTER_DOCUMENT)) {
                VariableInstance varinst = getVariableInstance(VariableConstants.MASTER_DOCUMENT);
                v = varinst == null ? null : varinst.getStringValue(getPackage());
            } else {
                v = evaluateBindingValue(childVar, v);
            }
            if (v != null && v.length() > 0)
                validParams.put(vn, v);
        }
        return validParams;
    }

    public void execute() throws ActivityException {
        try {
            Process subprocdef = getSubprocess();
            if (isLogDebugEnabled())
              logDebug("Invoking subprocess: " + subprocdef.getLabel());
            subprocIsService = subprocdef.getProcessType().equals(ProcessVisibilityConstant.SERVICE);
            List<Variable> childVars = subprocdef.getVariables();
            Map<String,String> validParams = createVariableBinding(childVars);
            passingByReference = containsDocReference(validParams);
            String ownerType = OwnerType.PROCESS_INSTANCE;
            String secondaryOwnerType = OwnerType.ACTIVITY_INSTANCE;
            Long secondaryOwnerId = getActivityInstanceId();
            Long ownerId = this.getProcessInstanceId();
            if (!needSuspend()) {
                secondaryOwnerId = null;
                secondaryOwnerType = null;
            }
            ProcessExecutor engine = getEngine();
            //If it is an asynchronous call to sub process (or executing in new engine) and the
            //parent process is running in a Memory Only thread, then the subprocess won't have any handle
            //to the parent process. So mark the Owner/OwnerId as the root process instance Owner/OwnerId
            if ((!isSynchronousCall() && engine.isInMemory()) ||
                (engine.isInMemory() && !engine.isInService() && subprocIsService && !passingByReference && !getProcessDefinition().isService())) {
                Object[] rootProcessOwner = getRootProcessOwner(getProcessInstanceOwnerId(), getProcessInstanceOwner());
                ownerId = (Long) rootProcessOwner[0];
                ownerType = (String) rootProcessOwner[1];
            }
            InternalEvent evMsg = InternalEvent.createProcessStartMessage(
                    subprocdef.getId(), ownerType,
                    ownerId, getMasterRequestId(), null,
                    secondaryOwnerType, secondaryOwnerId);

            if (engine.isInService()) {  // Current process is Non-service
                if (subprocIsService && isSynchronousCall()) {
                    ProcessInstance pi = getEngine().createProcessInstance(
                            subprocdef.getId(), OwnerType.PROCESS_INSTANCE,
                            getProcessInstanceId(), secondaryOwnerType, secondaryOwnerId,
                            getMasterRequestId(), new HashMap<>(validParams));

                    engine.startProcessInstance(pi, 0);  // call directly using same engine so documents are visible to child when PerfLvl >= 5  */
                }
                else if (!isSynchronousCall()) {
                    if (engine.getPerformanceLevel() < 5 || !passingByReference) {
                        String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + getActivityInstanceId() + "startproc" + subprocdef.getId();
                        evMsg.setParameters(validParams);    // TODO this can be large!
                        engine.sendDelayedInternalEvent(evMsg, 0, msgid, false);
                    }
                    else {
                        // Trying to call a any sub process async when parent is service process and documents are cache-only / not visible to child
                        throw new ActivityException("Invalid attempt to asynchronously launch sub process from a Service process running at performance level 5 or greater with Document variable bindings");
                    }
                }
                else  {
                    // Trying to call a non-service sub process sync when parent is service process - Regular proc could hold up/delay parent service process with event waits or manual task activities
                    throw new ActivityException("Invalid attempt to synchronously launch Non-Service sub process from a Service process");
                }
            }
            else {  // Current process is Non-service (regular)
                if (subprocIsService && isSynchronousCall() && !getProcessDefinition().isService() && (engine.getPerformanceLevel() < 9 || !passingByReference)) {
                    // Documents exist in DB and so are visible to child
                    ProcessEngineDriver engineDriver = new ProcessEngineDriver();   // Execute in new separate engine
                    Map<String,String> params =  engineDriver.invokeSubprocess(subprocdef.getId(), ownerId, getMasterRequestId(),
                            new HashMap<>(validParams), subprocdef.getPerformanceLevel());
                    // Documents modified in child are not reflected in this engine's documentCache, so force retrieval from DB if needed by removing from cache
                    this.bindVariables(params, true, true);        // passDocContent arg should be true only when perf_level>=9 (DHO:actually 5), but this works
                }
                else if (isSynchronousCall() && engine.getPerformanceLevel() >= 9) {  // Documents are cache-only, need to execute in same thread and same engine - ignore child's perf lvl, if any specified
                        ProcessInstance pi = getEngine().createProcessInstance(
                                subprocdef.getId(), OwnerType.PROCESS_INSTANCE,
                                getProcessInstanceId(), secondaryOwnerType, secondaryOwnerId,
                                getMasterRequestId(), new HashMap<>(validParams));

                        engine.startProcessInstance(pi, 0);  // call directly using same engine so documents are visible to child (PerfLvl 9)  */
                }
                else if (!passingByReference || isSynchronousCall() || (!isSynchronousCall() && engine.getPerformanceLevel() < 9)) {  // Sub is regular process (or service but parent is service running as regular, so execute child also as regular)
                    int perfLevel = subprocdef.getPerformanceLevel();
                    if (isSynchronousCall() && (perfLevel==0 || perfLevel==engine.getPerformanceLevel())) {
                        ProcessInstance pi = getEngine().createProcessInstance(
                                subprocdef.getId(), OwnerType.PROCESS_INSTANCE,
                                getProcessInstanceId(), secondaryOwnerType, secondaryOwnerId,
                                getMasterRequestId(), new HashMap<>(validParams));
                        engine.startProcessInstance(pi, 0);
                    } else {   // Either Async, or mismatch of perf lvls between parent and child - run on new thread and engine
                        String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + getActivityInstanceId()
                            + "startproc" + subprocdef.getId();
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
            logger.error("Exception in InvokeSubProcessActivity", ex);
            throw new ActivityException(-1, "Exception in InvokeSubProcessActivity", ex);
        }

    }

    @Deprecated
    boolean resume_on_process_finish(InternalEvent msg, Integer status) throws ActivityException {
        return resumeOnProcessFinish(msg, status);
    }

    protected boolean resumeOnProcessFinish(InternalEvent msg, Integer status)
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
            logger.error(ex.getMessage(), ex);
            throw new ActivityException(-1, ex.getMessage(), ex);
        }
    }

    private void bindVariables(Map<String,String> params, boolean passDocContent) throws ActivityException {
        bindVariables(params, passDocContent, false);
    }

    private void bindVariables(Map<String,String> params, boolean passDocContent, boolean refreshDocCache) throws ActivityException {
        String map = getAttributeValue(VARIABLES);
        if (map==null) map = "";
        if (params!=null) {
            Process procdef = getMainProcessDefinition();
            for (String varname : params.keySet()) {
                String para = getActualParameterVariable(map, varname);
                if (para != null) {
                    Variable var = procdef.getVariable(para);
                    if (var == null)
                        throw new ActivityException("Bound variable: '" + para + "' not found in process definition " + procdef.getLabel());
                    String varvalue = params.get(varname);
                    Object value;
                    if (passDocContent && getPackage().getTranslator(var.getType()).isDocumentReferenceVariable()) {
                        if (StringUtils.isBlank(varvalue)) {
                            value = null;
                        }
                        else if (varvalue.startsWith("DOCUMENT:")) {
                            value = getPackage().getObjectValue(var.getType(), varvalue);
                        }
                        else {
                            DocumentReference docref = createDocument(var.getType(),
                                    varvalue, OwnerType.PROCESS_INSTANCE, this.getProcessInstanceId());
                            value = new DocumentReference(docref.getDocumentId());
                        }
                    } else {
                        value = getPackage().getObjectValue(var.getType(), varvalue);
                    }

                    this.setParameterValue(para, value);

                    // Clear from this engine's documentCache map (forces getting from DB when next needed)
                    if (refreshDocCache && value instanceof DocumentReference) {
                        try {
                            getEngine().loadDocument((DocumentReference)value, false);
                        }
                        catch (DataAccessException e) {
                            throw new ActivityException(e.getMessage(), e);
                        }
                    }
                }
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
        String v = getMapValue(map, parameterName, ';');
        if (v == null)
            return null;
        else if (v.startsWith("${") && v.endsWith("}"))
            return v.substring(2, v.length() - 1);
        else if (v.length() < 2 || (v.charAt(0) != '$' && v.charAt(0) != '#'))
            throw new ActivityException(ERR_OUTPARA + ": " + parameterName);
        for (int i = 1; i < v.length(); i++) {
            if (!Character.isLetterOrDigit(v.charAt(i)) && v.charAt(i) != '_')
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
                logger.error("InvokeSubprocess->getRootProcessOwner() -> Supposedly unreachable code");
            }
            if (null == pi) {
                //This means that the pi is not present in cache and can be found in DB
                pi = this.getProcInstFromDB(ownerId);
                if (null != pi)
                    return getRootProcessOwner(pi.getOwnerId(), pi.getOwner());
                else {
                    // Shouldn't happen as pi has to be there either in memory or in DB
                    logger.error("getRootProcessOwner-> pi not found in DB for:" + ownerId);
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
        } catch (SQLException ex) {
            logger.error("InvokeSubProcessActivity -> Failed to load process instance for " + procInstId);
            return null;
        } finally {
            edao.stopTransaction(transaction);
        }
    }

    protected Process getSubprocess() throws ActivityException, DataAccessException {
        String name = getAttributeValue(WorkAttributeConstant.PROCESS_NAME);
        if (name == null)
            throw new ActivityException("Missing attribute: " + WorkAttributeConstant.PROCESS_NAME);
        String verSpec = getAttributeValue(WorkAttributeConstant.PROCESS_VERSION);
        return getSubprocess(name, verSpec);
    }

    private boolean containsDocReference(Map<String, String> params) {
        for (String value : params.values()) {
            if (value != null && value.startsWith("DOCUMENT:"))
                return true;
        }
        return false;
    }

    public static String getMapValue(String map, String name, char delimiter) {
        if (map.startsWith("{")) {
            JSONObject json = new JsonObject(map);
            if (json.has(name))
                return json.getString(name);
            else
                return null;
        }
        else {
            int name_start=0;
            int n = map.length();
            int m = name.length();
            while (name_start>=0) {
                name_start = map.indexOf(name, name_start);
                if (name_start>=0) {
                    if ((name_start==0||map.charAt(name_start-1)==delimiter)
                            && (name_start+m==n || map.charAt(name_start+m)=='=')) {
                        int value_start = name_start+m+1;
                        int k;
                        char ch;
                        boolean escaped = false;
                        for (k=value_start; k<n; k++) {
                            if (escaped) escaped = false;
                            else {
                                ch = map.charAt(k);
                                if (ch=='\\') escaped = true;
                                else if (ch==delimiter) break;
                            }
                        }
                        return map.substring(value_start, k);
                    } else name_start += m;
                }
            }
            return null;
        }
    }
}
