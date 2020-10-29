package com.centurylink.mdw.services.process;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.*;
import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.*;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.event.EventWaitInstance;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.request.Response;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.*;
import com.centurylink.mdw.model.workflow.WorkStatus.InternalLogMessage;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.OfflineMonitor;
import com.centurylink.mdw.monitor.ProcessMonitor;
import com.centurylink.mdw.service.data.activity.ImplementorCache;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.*;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.messenger.InternalMessenger;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.util.timer.TrackingTimer;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.centurylink.mdw.model.workflow.ProcessRuntimeContext.isExpression;

class ProcessExecutorImpl {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();
    private EngineLogger engineLogger;

    private static boolean uniqueMasterRequestId = PropertyManager.getBooleanProperty("mdw.process.uniqueMasterRequestId", false);

    protected EngineDataAccess edao;
    private final InternalMessenger internalMessenger;
    private final boolean inService;
    boolean activityTimings;

    ProcessExecutorImpl(EngineDataAccess edao, InternalMessenger internalMessenger, boolean forServiceProcess) {
        this.edao = edao;
        this.internalMessenger = internalMessenger;
        inService = forServiceProcess;
        engineLogger = new EngineLogger(logger, edao.getPerformanceLevel());
    }

    final EngineDataAccess getDataAccess() {
        return edao;
    }

    final DatabaseAccess getDatabaseAccess() {
        return edao.getDatabaseAccess();
    }

    ActivityInstance createActivityInstance(Long pActivityId, Long procInstId)
            throws SQLException, DataAccessException {
        ActivityInstance ai = new ActivityInstance();
        ai.setActivityId(pActivityId);
        ai.setProcessInstanceId(procInstId);
        ai.setStatusCode(WorkStatus.STATUS_IN_PROGRESS);
        edao.createActivityInstance(ai);
        return ai;
    }

    TransitionInstance createTransitionInstance(Transition transition, Long processInstanceId)
            throws DataAccessException {
        try {
            TransitionInstance transitionInstance = new TransitionInstance();
            transitionInstance.setTransitionID(transition.getId());
            transitionInstance.setProcessInstanceID(processInstanceId);
            transitionInstance.setStatusCode(TransitionStatus.STATUS_INITIATED);
            transitionInstance.setDestinationID(transition.getToId());
            edao.createTransitionInstance(transitionInstance);
            return transitionInstance;
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    VariableInstance createVariableInstance(ProcessInstance processInstance, String variableName, Object value)
            throws SQLException, DataAccessException {
        Process process = getMainProcessDefinition(processInstance);
        Variable variable = process.getVariable(variableName);
        if (variable == null) {
            throw new DataAccessException("Variable " + variableName + " is not defined for process " + process.getId());
        }
        VariableInstance variableInstance = new VariableInstance();
        variableInstance.setName(variable.getName());
        variableInstance.setVariableId(variable.getId());
        variableInstance.setType(variable.getType());
        if (value instanceof String)
            variableInstance.setStringValue((String)value);
        else
            variableInstance.setData(value);
        if (processInstance.isEmbedded() || (!processInstance.getProcessId().equals(process.getId()) && processInstance.getInstanceDefinitionId() <= 0))
            edao.createVariableInstance(variableInstance, processInstance.getOwnerId(), getPackage(process));
        else
            edao.createVariableInstance(variableInstance, processInstance.getId(), getPackage(process));
        return variableInstance;
    }

    DocumentReference createDocument(String variableType, String ownerType, Long ownerId, Object docObj, Package pkg)
            throws DataAccessException {
        return createDocument(variableType, ownerType, ownerId, null, null, docObj, pkg);
    }

    DocumentReference createDocument(String variableType, String ownerType, Long ownerId,
            Integer statusCode, String statusMessage, Object docObj, Package pkg) throws DataAccessException {
        return createDocument(variableType, ownerType, ownerId, statusCode, statusMessage, null, docObj, pkg);
    }

    DocumentReference createDocument(String variableType, String ownerType, Long ownerId,
            Integer statusCode, String statusMessage, String path, Object docObj, Package pkg)
            throws DataAccessException {
        DocumentReference docRef;
        try {
            Document doc = new Document();
            if (docObj instanceof String)
                doc.setContent((String)docObj);
            else
                doc.setObject(docObj);
            doc.setType(docObj == null || docObj instanceof String ? variableType : docObj.getClass().getName());
            doc.setVariableType(variableType);
            doc.setOwnerType(ownerType);
            doc.setOwnerId(ownerId);
            doc.setStatusCode(statusCode);
            doc.setStatusMessage(statusMessage);
            doc.setPath(path);
            edao.createDocument(doc, pkg);
            docRef = new DocumentReference(doc.getId());
        } catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
        return docRef;
    }

    Document getDocument(DocumentReference documentReference, boolean forUpdate) throws DataAccessException {
        try {
            return edao.getDocument(documentReference.getDocumentId(), forUpdate);
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    Document loadDocument(DocumentReference documentReference, boolean forUpdate) throws DataAccessException {
        try {
            return edao.loadDocument(documentReference.getDocumentId(), forUpdate);
        } catch (SQLException e) {
            throw new DataAccessException(-1, e.getMessage(), e);
        }
    }

    void updateDocumentContent(DocumentReference docRef, Object docObj, Package pkg)
            throws DataAccessException {
        try {
            Document doc = edao.getDocument(docRef.getDocumentId(), false);
            if (docObj instanceof String)
                doc.setContent((String)docObj);
            else
                doc.setObject(docObj);
            edao.updateDocumentContent(doc.getId(), doc.getContent(pkg));
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    /**
     * Create a process instance. The status is PENDING_PROCESS
     */
    ProcessInstance createProcessInstance(Long processId, String ownerType,
            Long ownerId, String secondaryOwnerType, Long secondaryOwnerId,
            String masterRequestId, Map<String,Object> values, String label, String template)
            throws ProcessException, DataAccessException {
        ProcessInstance pi = null;
        try {
            Process process;
            if (OwnerType.MAIN_PROCESS_INSTANCE.equals(ownerType)) {
                ProcessInstance parentPi = getDataAccess().getProcessInstance(ownerId);
                Process parentProcdef = ProcessCache.getProcess(parentPi.getProcessId());
                process = parentProcdef.getSubProcess(processId);
                pi = new ProcessInstance(parentPi.getProcessId(), process.getName());
                String comment = processId.toString();
                if (parentPi.getInstanceDefinitionId() > 0L)  // indicates instance definition
                    comment += "|HasInstanceDef|" + parentPi.getInstanceDefinitionId();
                pi.setComment(comment);
            } else {
                if (uniqueMasterRequestId && !(OwnerType.PROCESS_INSTANCE.equals(ownerType) || OwnerType.ERROR.equals(ownerType))) {
                    // check for uniqueness of master request id before creating top level process instance, if enabled
                    List<ProcessInstance> list = edao.getProcessInstancesByMasterRequestId(masterRequestId);
                    if (list != null && list.size() > 0) {
                        String msg = "Could not launch process instance for " + (label != null ? label : template) + " because Master Request ID " + masterRequestId + " is not unique";
                        logger.error(msg);
                        throw new ProcessException(msg);
                    }
                }
                process = ProcessCache.getProcess(processId);
                pi = new ProcessInstance(processId, process.getName());
            }
            pi.setOwner(ownerType);
            pi.setOwnerId(ownerId);
            pi.setSecondaryOwner(secondaryOwnerType);
            pi.setSecondaryOwnerId(secondaryOwnerId);
            pi.setMasterRequestId(masterRequestId);
            pi.setStatusCode(WorkStatus.STATUS_PENDING_PROCESS);
            if (label != null)
                pi.setComment(label);
            if (template != null)
                pi.setTemplate(template);
            edao.createProcessInstance(pi);
            createVariableInstances(pi, values);
        } catch (IOException ex) {
            throw new ProcessException("Cannot load process " + processId, ex);
        } catch (SQLException ex) {
            if (pi != null && pi.getId() != null && pi.getId() > 0L)
                try {
                    edao.setProcessInstanceStatus(pi.getId(), WorkStatus.STATUS_FAILED);
                } catch (SQLException e) {
                    logger.error("Exception while updating process status to 'Failed'", e);
                }
            throw new DataAccessException(-1, ex.getMessage(), ex);
        }
        return pi;
    }

    private void createVariableInstances(ProcessInstance pi, Map<String,Object> values)
            throws ProcessException, DataAccessException, SQLException {
        Process process = getProcessDefinition(pi);
        Package pkg = getPackage(getMainProcessDefinition(pi));
        pi.setVariables(new ArrayList<>());
        if (values != null) {
            for (String variableName : values.keySet()) {
                Variable variable = process.getVariable(variableName);
                if (variable == null)
                    throw new ProcessException("Variable " + variableName + " not defined for process " + process.getLabel());
                Object value = values.get(variableName);
                if (value != null && !value.toString().isEmpty()) {
                    VariableInstance variableInstance = new VariableInstance();
                    variableInstance.setName(variable.getName());
                    variableInstance.setVariableId(variable.getId());
                    variableInstance.setType(variable.getType());
                    boolean isDocument = pkg.getTranslator(variable.getType()).isDocumentReferenceVariable();
                    if (isDocument) {
                        if (value instanceof String && ((String) value).startsWith("DOCUMENT:")) {
                            variableInstance.setStringValue((String) value);
                        } else {
                            DocumentReference docRef = createDocument(variable.getType(), OwnerType.PROCESS_INSTANCE, pi.getId(), value, pkg);
                            variableInstance.setData(docRef);
                        }
                    } else {
                        if (value instanceof String)
                            variableInstance.setStringValue((String)value);
                        else
                            variableInstance.setData(value);
                    }
                    pi.getVariables().add(variableInstance);
                    edao.createVariableInstance(variableInstance, pi.getId(), pkg);
                    if (isDocument) {
                        DocumentReference docRef = new DocumentReference(variableInstance.getStringValue(pkg));
                        String type = (value instanceof String) ? null : value.getClass().getName();
                        updateDocumentInfo(docRef, type, OwnerType.VARIABLE_INSTANCE, variableInstance.getId(), null, null);
                    }
                }
            }
        }
    }

    void updateDocumentInfo(DocumentReference docRef, String documentType, String ownerType,
            Long ownerId, Integer statusCode, String statusMessage) throws DataAccessException {
        try {
            boolean dirty = false;
            Document doc = edao.getDocument(docRef.getDocumentId(), false);
            if (documentType != null && !documentType.equals(doc.getType())) {
                doc.setType(documentType);
                dirty = true;
            }
            if (ownerId != null && !ownerId.equals(doc.getOwnerId())) {
                // DO NOT UPDATE THE OWNER_ID IF IT'S A PROCESS VARIABLE ALREADY OWNED BY DIFFERENT PROCESS INSTANCE
                if (!("VARIABLE_INSTANCE".equalsIgnoreCase(ownerType) && ownerType.equalsIgnoreCase(doc.getOwnerType()) && doc.getOwnerId() > 0L)) {
                    doc.setOwnerId(ownerId);
                    dirty = true;
                }
            }
            if (ownerType != null && !ownerType.equalsIgnoreCase(doc.getOwnerType())) {
                if (edao.getDocumentDbAccess() != null)
                    edao.getDocumentDbAccess().updateDocumentDbOwnerType(doc, ownerType);
                doc.setOwnerType(ownerType);
                dirty = true;
            }
            if (statusCode != null && !statusCode.equals(doc.getStatusCode())) {
                doc.setStatusCode(statusCode);
                dirty = true;
            }
            if (statusMessage != null && !statusMessage.equals(doc.getStatusMessage())) {
                doc.setStatusMessage(statusMessage);
                dirty = true;
            }
            if (dirty)
                edao.updateDocumentInfo(doc);
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    void cancelEventWaitInstances(Long activityInstanceId) throws DataAccessException {
        try {
            getDataAccess().removeEventWaitForActivityInstance(activityInstanceId, "Cancel due to timeout");
        } catch (Exception ex) {
            throw new DataAccessException("Failed to cancel event waits", ex);
        }
    }

    Response getServiceProcessResponse(Long procInstId, String varName, Package pkg) throws DataAccessException {
        try {
            VariableInstance varInst;
            if (varName == null) {
                varInst = getDataAccess().getVariableInstance(procInstId, VariableConstants.RESPONSE);
                if (varInst == null)
                    varInst = getDataAccess().getVariableInstance(procInstId, VariableConstants.MASTER_DOCUMENT);
                if (varInst == null)
                    varInst = getDataAccess().getVariableInstance(procInstId, VariableConstants.REQUEST);
            } else {
                varInst = getDataAccess().getVariableInstance(procInstId, varName);
            }
            if (varInst == null)
                return null;
            Response response = new Response();
            if (varInst.isDocument(pkg)) {
                Document doc = getDocument((DocumentReference)varInst.getData(pkg), false);
                response.setContent(doc.getContent(pkg));
                response.setObject(doc.getObject(varInst.getType(), pkg));
            } else {
                response.setContent(varInst.getStringValue(pkg));
                response.setObject(varInst.getData(pkg));
            }
            return response;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to get value for variable " + varName, ex);
        }
    }

    void updateProcessInstanceStatus(Long processInstanceId, Integer status)
            throws DataAccessException,ProcessException {
        try {
            getDataAccess().setProcessInstanceStatus(processInstanceId, status);
            if (status.equals(WorkStatus.STATUS_COMPLETED) ||
                    status.equals(WorkStatus.STATUS_CANCELLED) ||
                    status.equals(WorkStatus.STATUS_FAILED)) {
                getDataAccess().removeEventWaitForProcessInstance(processInstanceId);
            }
        } catch (SQLException ex) {
            throw new ProcessException("Failed to update process instance status", ex);
        }
    }

    protected Process getProcessDefinition(ProcessInstance processInstance) {
        Process process = null;
        if (processInstance.getInstanceDefinitionId() > 0L)
            process = ProcessCache.getInstanceDefinition(processInstance.getProcessId(), processInstance.getInstanceDefinitionId());
        if (process == null) {
            try {
                process = ProcessCache.getProcess(processInstance.getProcessId());
            } catch (IOException ex) {
                logger.error("Error loading process definition for instance " + processInstance.getId(), ex);
            }
        }
        if (processInstance.isEmbedded() && process != null)
            process = process.getSubProcess(new Long(processInstance.getComment()));
        return process;
    }

    /**
     * Finds the instance process definition (or the containing process if embedded).
     */
    protected Process getMainProcessDefinition(ProcessInstance processInstance) {
        Process process = null;
        if (processInstance.getInstanceDefinitionId() > 0L)
            process = ProcessCache.getInstanceDefinition(processInstance.getProcessId(), processInstance.getInstanceDefinitionId());
        if (process == null) {
            try {
                process = ProcessCache.getProcess(processInstance.getProcessId());
            } catch (IOException ex) {
                logger.error("Error loading definition for process instance " + processInstance.getId(), ex);
            }
        }
        return process;
    }

    boolean deleteInternalEvent(String eventName) throws DataAccessException {
        if (eventName == null)
            return false;
        try {
            int count = getDataAccess().deleteEventInstance(eventName);
            return count > 0;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to delete internal event" + eventName, ex);
        }
    }

    InternalMessenger getInternalMessenger() {
        return internalMessenger;
    }

    /**
     * Handles the work Transitions for the passed in collection of Items
     */
    void createTransitionInstances(ProcessInstance processInstance, List<Transition> transitions, Long fromActInstId)
            throws ProcessException {
        TransitionInstance transInst;
        for (Transition transition : transitions) {
            try {
                if (tooManyTransitions(transition, processInstance)) {
                    // Look for a error transition at this time
                    // In case we find it, raise the error event
                    // Otherwise do not do anything
                    handleWorkTransitionError(processInstance, transition.getId(), fromActInstId);
                } else {
                    transInst = createTransitionInstance(transition, processInstance.getId());
                    String tag = EngineLogger.logtag(processInstance.getProcessId(), processInstance.getId(), transInst);
                    String msg = InternalLogMessage.TRANSITION_INIT.message + " from " + transition.getFromId() + " to " + transition.getToId();
                    engineLogger.info(tag, processInstance.getId(), msg);

                    InternalEvent eventMsg = InternalEvent.createActivityStartMessage(
                            transition.getToId(), processInstance.getId(),
                            transInst.getTransitionInstanceID(), processInstance.getMasterRequestId(),
                            transition.getLabel());
                    String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + processInstance.getId()
                            + "start" + transition.getToId() + "by" + transInst.getTransitionInstanceID();
                    int delay = getTransitionDelay(transition, processInstance);
                    if (delay > 0)
                        sendDelayedInternalEvent(eventMsg, delay, msgid, false);
                    else
                        sendInternalEvent(eventMsg);
                }
            } catch (SQLException | MdwException ex) {
                throw new ProcessException(-1, ex.getMessage(), ex);
            }
        }
    }

    public int getTransitionDelay(Transition transition, ProcessInstance processInstance) {
        int delaySecs = 0;
        String delayAttr = transition.getAttribute(WorkTransitionAttributeConstant.TRANSITION_DELAY);
        if (delayAttr != null) {
            if (isExpression(delayAttr)) {
                String expr = delayAttr.endsWith("s") ? delayAttr.substring(0, delayAttr.length() - 1) : delayAttr;
                delaySecs = (Integer) evaluate(expr, processInstance);
            }
            else {
                // moved from Transition.getTransitionDelay()
                int k, n = delayAttr.length();
                for (k = 0; k < n; k++) {
                    if (!Character.isDigit(delayAttr.charAt(k)))
                        break;
                }
                if (k < n) {
                    String unit = delayAttr.substring(k).trim();
                    delayAttr = delayAttr.substring(0,k);
                    if (unit.startsWith("s"))
                        delaySecs = Integer.parseInt(delayAttr);
                    else if (unit.startsWith("h"))
                        delaySecs = 3600 * Integer.parseInt(delayAttr);
                    else
                        delaySecs = 60 * Integer.parseInt(delayAttr);
                } else {
                    delaySecs = 60 * Integer.parseInt(delayAttr);
                }
            }
        }
        return delaySecs;
    }

    private boolean tooManyTransitions(Transition trans, ProcessInstance processInstance) throws SQLException {
        if (inService)
            return false;
        int retryCount = -1;
        String retryAttr = trans.getAttribute(WorkTransitionAttributeConstant.TRANSITION_RETRY_COUNT);
        if (retryAttr != null) {
            if (isExpression(retryAttr))
                retryCount = (Integer) evaluate(retryAttr, processInstance);
            else
                retryCount = Integer.parseInt(retryAttr);
        }
        if (retryCount < 0)
            return false;
        int count = edao.countTransitionInstances(processInstance.getId(), trans.getId());
        if (count > 0 && count >= retryCount) {
            String msg = "Transition " + trans.getId() + " not made - exceeded allowed retry count of " + retryCount;
            // log as exception since this message is often overlooked
            logger.error(msg, new ProcessException(msg));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Supports simple expressions only (does not deserialize documents).
     */
    private Object evaluate(String expression, ProcessInstance processInstance) {
        Process process = getProcessDefinition(processInstance);
        Package pkg = getPackage(getMainProcessDefinition(processInstance));
        Map<String,Object> vars = new HashMap<>();
        for (VariableInstance vi : processInstance.getVariables())
            vars.put(vi.getName(), vi.getData(pkg));
        return new ProcessRuntimeContext(null, pkg, process, processInstance,
                getDataAccess().getPerformanceLevel(), isInService(), vars).evaluate(expression);
    }

    private void handleWorkTransitionError(ProcessInstance processInstance, Long workTransitionId, Long fromActInstId)
            throws ProcessException, DataAccessException, SQLException {
        edao.setProcessInstanceStatus(processInstance.getId(), WorkStatus.STATUS_WAITING);
        Process process = getMainProcessDefinition(processInstance);
        Process embeddedProcess = process.findSubprocess(EventType.ERROR, null);
        while (embeddedProcess == null && processInstance.getOwner().equals(OwnerType.PROCESS_INSTANCE)) {
            processInstance = edao.getProcessInstance(processInstance.getOwnerId());
            process = getMainProcessDefinition(processInstance);
            embeddedProcess = process.findSubprocess(EventType.ERROR, null);
        }
        if (embeddedProcess == null) {
            logger.warn("Error subprocess does not exist. Transition failed. TransitionId-->"
                    + workTransitionId + " ProcessInstanceId-->" + processInstance.getId());
            return;
        }
        String msg = "Transition to error subprocess " + embeddedProcess.getQualifiedName();
        engineLogger.info(processInstance.getProcessId(), processInstance.getId(), processInstance.getMasterRequestId(), msg);
        String secondaryOwnerType;
        Long secondaryOwnerId;
        if (fromActInstId == null || fromActInstId == 0L) {
            secondaryOwnerType = OwnerType.WORK_TRANSITION;
            secondaryOwnerId = workTransitionId;
        } else {
            secondaryOwnerType = OwnerType.ACTIVITY_INSTANCE;
            secondaryOwnerId = fromActInstId;
        }
        String ownerType = OwnerType.MAIN_PROCESS_INSTANCE;
        ProcessInstance procInst = createProcessInstance(embeddedProcess.getId(),
                ownerType, processInstance.getId(), secondaryOwnerType, secondaryOwnerId,
                processInstance.getMasterRequestId(), null, null, null);
        startProcessInstance(procInst, 0);
    }

    /**
     * Starting a process instance, which has been created already.
     * The method sets the status to "In Progress",
     * find the start activity, and sends an internal message to start the activity
     *
     * @param processInstance process instance.
     */
    void startProcessInstance(ProcessInstance processInstance, int delay) throws ProcessException {
        try {
            Process process = getProcessDefinition(processInstance);
            edao.setProcessInstanceStatus(processInstance.getId(), WorkStatus.STATUS_PENDING_PROCESS);
            // setProcessInstanceStatus will really set to STATUS_IN_PROGRESS - hint to set START_DT as well
            if (logger.isInfoEnabled()) {
                String msg = InternalLogMessage.PROCESS_START + " - " + process.getQualifiedName()
                        + (processInstance.isEmbedded() ? (" (embedded process " + process.getId() + ")") : ("/" + process.getVersionString()));
                engineLogger.info(processInstance.getProcessId(), processInstance.getId(), processInstance.getMasterRequestId(), msg);
                engineLogger.info(processInstance.getProcessId(), processInstance.getId(), processInstance.getMasterRequestId(), "Performance level = " + engineLogger.getPerformanceLevel());
            }
            notifyMonitors(processInstance, InternalLogMessage.PROCESS_START);
            // get start activity ID
            Long startActivityId;
            if (processInstance.isEmbedded()) {
                edao.setProcessInstanceStatus(processInstance.getId(), WorkStatus.STATUS_PENDING_PROCESS);
                startActivityId = process.getStartActivity().getId();
            } else {
                Activity startActivity = process.getStartActivity();
                if (startActivity == null) {
                    throw new ProcessException("Transition has not been defined for START event! ProcessID = " + process.getId());
                }
                startActivityId = startActivity.getId();
            }
            InternalEvent event = InternalEvent.createActivityStartMessage(
                    startActivityId, processInstance.getId(),
                    null, processInstance.getMasterRequestId(),
                    EventType.EVENTNAME_START + ":");
            if (delay > 0) {
                String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + processInstance.getId() + "start" + startActivityId;
                sendDelayedInternalEvent(event, delay, msgid, false);
            } else {
                sendInternalEvent(event);
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ProcessException(ex.getMessage());
        }
    }

    /**
     * determine if activity needs to wait (such as synchronous
     * process invocation, wait activity, synchronization)
     */
    private boolean activityNeedsWait(GeneralActivity activity)
            throws ActivityException {
        if (activity instanceof SuspendableActivity)
            return ((SuspendableActivity) activity).needSuspend();
        return false;
    }

    /**
     * Reports the error status of the activity instance to the activity manager
     */
    void failActivityInstance(InternalEvent event, ProcessInstance processInst, Long activityId, Long activityInstId,
            BaseActivity activity, Throwable cause) throws MdwException, SQLException {

        String tag = EngineLogger.logtag(processInst.getProcessId(), processInst.getId(), activityId, activityInstId);
        String msg = "Failed to execute activity - " + cause.getClass().getName();
        engineLogger.error(processInst.getProcessId(), processInst.getId(), activityId, activityInstId, msg, cause);

        String compCode = null;
        String statusMsg = buildStatusMessage(cause);
        try {
            ActivityInstance actInstVO = null;
            if (activity != null && activityInstId != null) {
                activity.setReturnMessage(statusMsg);
                actInstVO = edao.getActivityInstance(activityInstId);
                failActivityInstance(actInstVO, statusMsg, processInst, tag, cause.getClass().getName());
                compCode = activity.getReturnCode();
            }
            if (!AdapterActivity.COMPCODE_AUTO_RETRY.equals(compCode)) {
                DocumentReference docRef = createActivityExceptionDocument(processInst, actInstVO, activity, cause);
                InternalEvent outgoingMsg =
                        InternalEvent.createActivityErrorMessage(activityId, activityInstId, processInst.getId(), compCode,
                                event.getMasterRequestId(), statusMsg.length() > 2000 ? statusMsg.substring(0, 1999) : statusMsg, docRef.getDocumentId());
                sendInternalEvent(outgoingMsg);
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            ActivityLogger.persist(processInst.getId(), activityInstId, LogLevel.ERROR, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String buildStatusMessage(Throwable t) {
        if (t == null)
            return "";
        StringBuilder message = new StringBuilder(t.toString());
        String v = PropertyManager.getProperty("MDWFramework.WorkflowEngine/ActivityStatusMessage.ShowStackTrace");
        boolean includeStackTrace = !"false".equalsIgnoreCase(v);
        if (includeStackTrace) {
            // get the root cause
            Throwable cause = t;
            while (cause.getCause() != null)
                cause = cause.getCause();
            if (t != cause)
                message.append("\nCaused by: ").append(cause);
            for (StackTraceElement element : cause.getStackTrace()) {
                message.append("\n").append(element.toString());
            }
        }

        if (message.length() > 4000) {
            return message.toString().substring(0, 3998);
        }

        return message.toString();
    }

    void cancelActivityInstance(ActivityInstance actInst, ProcessInstance procinst, String statusMsg)
            throws DataAccessException, SQLException {
        String logtag = EngineLogger.logtag(procinst.getProcessId(), procinst.getId(), actInst.getActivityId(), actInst.getId());
        try {
            cancelActivityInstance(actInst, statusMsg, procinst, logtag);
        } catch (Exception ex) {
            engineLogger.error(procinst.getProcessId(), procinst.getId(), actInst.getActivityId(), actInst.getId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    void holdActivityInstance(ActivityInstance actInst, Long procId) throws DataAccessException, SQLException {
        String logtag = EngineLogger.logtag(procId, actInst.getProcessInstanceId(), actInst.getActivityId(), actInst.getId());
        try {
            holdActivityInstance(actInst, logtag);
        } catch (Exception ex) {
            String msg = "Exception thrown during holdActivityInstance";
            engineLogger.error(procId, actInst.getProcessInstanceId(), actInst.getActivityId(), actInst.getId(), msg, ex);
            throw ex;
        }
    }

    private ActivityInstance waitForActivityDone(ActivityInstance actInst)
            throws DataAccessException, InterruptedException, SQLException {
        int maxRetry = 10;
        int retryInterval = 2;
        int count = 0;
        while (count<maxRetry && actInst.getStatusCode()==WorkStatus.STATUS_IN_PROGRESS) {
            logger.debug("wait for sync activity to finish: " + actInst.getId());
            Thread.sleep(retryInterval*1000L);
            actInst = getDataAccess().getActivityInstance(actInst.getId());
            count++;
        }
        return actInst;
    }

    ActivityRuntime prepareActivityInstance(InternalEvent event, ProcessInstance procInst) throws ProcessException {
        try {
            // for asynch engine, procInst is always null
            ActivityRuntime ar = new ActivityRuntime();
            Long activityId = event.getWorkId();
            Long workTransInstanceId = event.getTransitionInstanceId();

            // check if process instance is still alive
            ar.procinst = procInst;
            if (WorkStatus.STATUS_CANCELLED.equals(ar.procinst.getStatusCode())
                    || WorkStatus.STATUS_COMPLETED.equals(ar.procinst.getStatusCode())) {
                ar.startCase = ActivityRuntime.STARTCASE_PROCESS_TERMINATED;
                return ar;
            }

            Process process = getProcessDefinition(ar.procinst);
            Activity activity = process.getActivity(activityId);
            Package pkg = PackageCache.getPackage(getMainProcessDefinition(procInst).getPackageName());
            try {
                ar.activity = (BaseActivity)getActivityInstance(pkg, activity.getImplementor());
            } catch (Throwable e) {
                String tag = EngineLogger.logtag(procInst.getProcessId(), procInst.getId(), activityId, 0L);
                String msg = "Failed to create activity implementor instance";
                engineLogger.error(tag, procInst.getId(),null, msg, e);
                ar.activity = null;
            }
            boolean isSyncActivity = ar.activity instanceof SynchronizationActivity;
            if (isSyncActivity)
                getDataAccess().lockProcessInstance(procInst.getId());

            List<ActivityInstance> actInsts;
            if (this.inService)
                actInsts = null;
            else
                actInsts = getDataAccess().getActivityInstances(activityId, procInst.getId(), true, isSyncActivity);

            if (actInsts == null || actInsts.isEmpty()) {
                // create activity instance and prepare it
                ar.actinst = createActivityInstance(activityId, procInst.getId());
                prepareActivitySub(process, activity, ar.procinst, ar.actinst, workTransInstanceId, event, ar.activity);
                if (ar.activity == null) {
                    String msg = "Failed to load the implementor class or create instance: " + activity.getImplementor();
                    engineLogger.error(process.getId(), procInst.getId(), procInst.getMasterRequestId(), msg);
                    ar.startCase = ActivityRuntime.STARTCASE_ERROR_IN_PREPARE;
                } else {
                    ar.startCase = ActivityRuntime.STARTCASE_NORMAL;
                    // notify registered monitors
                    ar.activity.notifyMonitors(InternalLogMessage.ACTIVITY_START);
                }
            } else if (isSyncActivity) {
                ar.actinst = actInsts.get(0);
                if (ar.actinst.getStatusCode() == WorkStatus.STATUS_IN_PROGRESS)
                    ar.actinst = waitForActivityDone(ar.actinst);
                if (ar.actinst.getStatusCode() == WorkStatus.STATUS_WAITING) {
                    if (workTransInstanceId != null && workTransInstanceId > 0L) {
                        getDataAccess().completeTransitionInstance(workTransInstanceId, ar.actinst.getId());
                    }
                    ar.startCase = ActivityRuntime.STARTCASE_SYNCH_WAITING;
                } else if (ar.actinst.getStatusCode() == WorkStatus.STATUS_HOLD) {
                    if (workTransInstanceId != null && workTransInstanceId > 0L) {
                        getDataAccess().completeTransitionInstance(workTransInstanceId, ar.actinst.getId());
                    }
                    ar.startCase = ActivityRuntime.STARTCASE_SYNCH_WAITING;
                } else {    // completed - possible when there are OR conditions
                    if (workTransInstanceId != null && workTransInstanceId > 0L) {
                        getDataAccess().completeTransitionInstance(workTransInstanceId, ar.actinst.getId());
                    }
                    ar.startCase = ActivityRuntime.STARTCASE_SYNCH_COMPLETE;
                }
            } else {
                ActivityInstance onHoldActInst = null;
                for (ActivityInstance actInst : actInsts) {
                    if (actInst.getStatusCode() == WorkStatus.STATUS_HOLD) {
                        onHoldActInst = actInst;
                        break;
                    }
                }
                if (onHoldActInst != null) {
                    if (workTransInstanceId != null && workTransInstanceId > 0L) {
                        getDataAccess().completeTransitionInstance(workTransInstanceId, onHoldActInst.getId());
                    }
                    ar.startCase = ActivityRuntime.STARTCASE_RESUME_WAITING;
                    ar.actinst = onHoldActInst;
                } else {    // WAITING or IN_PROGRESS
                    ar.startCase = ActivityRuntime.STARTCASE_INSTANCE_EXIST;
                }
            }
            return ar;
        } catch (SQLException | MdwException | InterruptedException e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }

    private void prepareActivitySub(Process procDef, Activity actDef, ProcessInstance pi, ActivityInstance ai,
            Long workTransInstId, InternalEvent event, BaseActivity activity) throws SQLException, MdwException {

        if (logger.isInfoEnabled()) {
            String msg = InternalLogMessage.ACTIVITY_START + " - " + actDef.getName();
            engineLogger.info(pi.getProcessId(), pi.getId(), ai.getActivityId(), ai.getId(), msg);
        }
        if (workTransInstId != null && workTransInstId != 0)
            edao.completeTransitionInstance(workTransInstId, ai.getId());

        if (activity == null) {
            edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_FAILED, "Failed to instantiate activity implementor");
            return;
            // note cannot throw exception here, as when implementor is not defined,
            // the error handling itself will throw exception. We failed the activity outright here.
        }
        Class<?> implClass = activity.getClass();
        TrackingTimer activityTimer = null;
        Tracked t = implClass.getAnnotation(Tracked.class);
        if (t != null) {
            String logTag = EngineLogger.logtag(pi.getProcessId(), pi.getId(), ai.getActivityId(), ai.getId());
            activityTimer = new TrackingTimer(logTag, actDef.getImplementor(), t.value());
        }

        List<VariableInstance> vars;
        if (procDef.isEmbeddedProcess())
            vars = edao.getProcessInstanceVariables(pi.getOwnerId());
        else
            vars = edao.getProcessInstanceVariables(pi.getId());

        event.setWorkInstanceId(ai.getId());

        activity.prepare(actDef, pi, ai, vars, workTransInstId,
                activityTimer, new ProcessExecutor(this));
    }

    private void removeActivitySLA(ActivityInstance ai, ProcessInstance procInst) {
        Process process = getProcessDefinition(procInst);
        Activity activity = process.getActivity(ai.getActivityId());
        String sla = activity == null ? null : activity.getAttribute(WorkAttributeConstant.SLA);
        if (sla != null && !"0".equals(sla)) {
            ScheduledEventQueue eventQueue = ScheduledEventQueue.getSingleton();
            try {
                eventQueue.unscheduleEvent(ScheduledEvent.INTERNAL_EVENT_PREFIX+ai.getId());
            } catch (Exception ex) {
                if (logger.isDebugEnabled()) logger.debug("Failed to unschedule SLA", ex);
            }
        }
    }

    private void failActivityInstance(ActivityInstance ai, String statusMsg, ProcessInstance pi,
            String logtag, String abbrStatusMsg) throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_FAILED, statusMsg);
        removeActivitySLA(ai, pi);
        engineLogger.info(logtag, pi.getId(), ai.getId(), InternalLogMessage.ACTIVITY_FAIL + " - " + abbrStatusMsg);
    }

    private void completeActivityInstance(ActivityInstance ai, String compcode, ProcessInstance pi, String logtag)
            throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_COMPLETED, compcode);
        if (activityTimings)
            edao.setActivityCompletionTime(ai);

        removeActivitySLA(ai, pi);
        String msg = InternalLogMessage.ACTIVITY_COMPLETE + " - completion code " + (compcode == null ? "null" : ("'" + compcode + "'"));
        engineLogger.info(logtag, pi.getId(), ai.getId(), msg);
    }

    private void cancelActivityInstance(ActivityInstance ai, String statusMsg, ProcessInstance pi, String logtag)
            throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_CANCELLED, statusMsg);
        if (activityTimings)
            edao.setActivityCompletionTime(ai);
        removeActivitySLA(ai, pi);
        engineLogger.info(logtag, pi.getId(), ai.getId(), InternalLogMessage.ACTIVITY_CANCEL + " - " + statusMsg);
    }

    private void holdActivityInstance(ActivityInstance ai, String logtag) throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_HOLD, null);
        engineLogger.info(logtag, ai.getProcessInstanceId(), ai.getId(), InternalLogMessage.ACTIVITY_HOLD.message);
    }

    private void suspendActivityInstance(BaseActivity activity, ActivityInstance ai, String logtag, String additionalMsg)
            throws DataAccessException, SQLException {
        edao.setActivityInstanceStatus(ai, WorkStatus.STATUS_WAITING, null);
        String msg = InternalLogMessage.ACTIVITY_SUSPEND + (additionalMsg != null ? " - " + additionalMsg : "");
        engineLogger.info(logtag, ai.getProcessInstanceId(), ai.getId(), msg);
        activity.notifyMonitors(InternalLogMessage.ACTIVITY_SUSPEND);
    }

    CompletionCode finishActivityInstance(BaseActivity activity, ProcessInstance pi, ActivityInstance ai,
            InternalEvent event, boolean bypassWait) throws ProcessException {
        try {
            if (activity.getTimer() != null)
                activity.getTimer().start("Finish Activity");

            // Step 3  get and parse completion code
            boolean mayNeedWait = !bypassWait && activityNeedsWait(activity);
            String origCompCode = activity.getReturnCode();
            CompletionCode compCode = new CompletionCode();
            compCode.parse(origCompCode);
            Integer actInstStatus = compCode.getActivityInstanceStatus();
            if (actInstStatus == null && mayNeedWait)
                actInstStatus = WorkStatus.STATUS_WAITING;
            String logtag = EngineLogger.logtag(pi.getProcessId(), pi.getId(), ai.getActivityId(), ai.getId());

            // Step 3a if activity not successful
            if (compCode.getEventType().equals(EventType.ERROR)) {
                failActivityInstance(ai, activity.getReturnMessage(), pi, logtag, activity.getReturnMessage());
                if (!AdapterActivity.COMPCODE_AUTO_RETRY.equals(compCode.getCompletionCode())) {
                    DocumentReference docRef = createActivityExceptionDocument(pi, ai, activity, new ActivityException("Activity failed: " + ai.getId()));
                    InternalEvent outmsg = InternalEvent.createActivityErrorMessage(ai.getActivityId(),
                            ai.getId(), pi.getId(), compCode.getCompletionCode(),
                            event.getMasterRequestId(), activity.getReturnMessage(), docRef.getDocumentId());
                    sendInternalEvent(outmsg);
                }
            }

            // Step 3b if activity needs to wait
            else if (mayNeedWait && !actInstStatus.equals(WorkStatus.STATUS_COMPLETED)) {
                if (actInstStatus.equals(WorkStatus.STATUS_HOLD)) {
                    holdActivityInstance(ai, logtag);
                    InternalEvent outmsg = InternalEvent.createActivityNotifyMessage(ai, compCode.getEventType(),
                            pi.getMasterRequestId(), compCode.getCompletionCode());
                    sendInternalEvent(outmsg);
                } else if (actInstStatus.equals(WorkStatus.STATUS_WAITING) &&
                        (compCode.getEventType().equals(EventType.ABORT) || compCode.getEventType().equals(EventType.CORRECT)
                                || compCode.getEventType().equals(EventType.ERROR))) {
                    suspendActivityInstance(activity, ai, logtag, null);
                    InternalEvent outmsg =  InternalEvent.createActivityNotifyMessage(ai, compCode.getEventType(),
                            pi.getMasterRequestId(), compCode.getCompletionCode());
                    sendInternalEvent(outmsg);
                }
                else if (actInstStatus.equals(WorkStatus.STATUS_CANCELLED)) {
                    cancelActivityInstance(ai, compCode.getCompletionCode(), pi,  logtag);
                    InternalEvent outmsg =  InternalEvent.createActivityNotifyMessage(ai, compCode.getEventType(),
                            pi.getMasterRequestId(), compCode.getCompletionCode());
                    sendInternalEvent(outmsg);
                }
                else {
                    suspendActivityInstance(activity, ai, logtag, null);
                }
            }

            // Step 3c. otherwise, activity is successful and complete it
            else {
                completeActivityInstance(ai, origCompCode, pi, logtag);

                // notify registered monitors
                activity.notifyMonitors(InternalLogMessage.ACTIVITY_COMPLETE);

                if (activity instanceof FinishActivity) {
                    String compcode = ((FinishActivity)activity).getProcessCompletionCode();
                    boolean noNotify = ((FinishActivity)activity).doNotNotifyCaller();
                    completeProcessInstance(pi, compcode, noNotify);
                    List<ProcessInstance> subProcessInsts = getDataAccess().getProcessInstances(pi.getProcessId(), OwnerType.MAIN_PROCESS_INSTANCE, pi.getId());
                    for (ProcessInstance subProcessInstanceVO : subProcessInsts) {
                        if (!subProcessInstanceVO.getStatusCode().equals(WorkStatus.STATUS_COMPLETED) &&
                                !subProcessInstanceVO.getStatusCode().equals(WorkStatus.STATUS_CANCELLED)) {
                            completeProcessInstance(subProcessInstanceVO, compcode, noNotify);
                        }
                    }
                } else {
                    InternalEvent outmsg = InternalEvent.createActivityNotifyMessage(ai,
                            compCode.getEventType(), event.getMasterRequestId(), compCode.getCompletionCode());
                    sendInternalEvent(outmsg);
                }
            }
            return compCode;    // not used by asynch engine
        } catch (Exception ex) {
            throw new ProcessException(ex.getMessage(), ex);
        } finally {
            if (activity.getTimer() != null)
                activity.getTimer().stopAndLogTiming();
        }
    }

    void handleProcessFinish(InternalEvent event) throws ProcessException {
        try {
            String ownerType = event.getOwnerType();
            String secondaryOwnerType = event.getSecondaryOwnerType();
            if (!OwnerType.ACTIVITY_INSTANCE.equals(secondaryOwnerType)) {
                // top level processes (non-remote) or ABORT embedded processes
                ProcessInstance pi = edao.getProcessInstance(event.getWorkInstanceId());
                Process subProcVO = getProcessDefinition(pi);
                if (pi.isEmbedded()) {
                    subProcVO.getSubProcess(event.getWorkId());
                    String embeddedProcType = subProcVO.getAttribute(WorkAttributeConstant.EMBEDDED_PROCESS_TYPE);
                    if (ProcessVisibilityConstant.EMBEDDED_ABORT_PROCESS.equals(embeddedProcType)) {
                        Long parentProcInstId = event.getOwnerId();
                        pi = edao.getProcessInstance(parentProcInstId);
                        cancelProcessInstanceTree(pi);
                        engineLogger.info(pi.getProcessId(), pi.getId(), pi.getMasterRequestId(), "Process cancelled");
                        InternalEvent procFinishMsg = InternalEvent.createProcessFinishMessage(pi);
                        if (OwnerType.ACTIVITY_INSTANCE.equals(pi.getSecondaryOwner())) {
                            procFinishMsg.setSecondaryOwnerType(pi.getSecondaryOwner());
                            procFinishMsg.setSecondaryOwnerId(pi.getSecondaryOwnerId());
                        }
                        sendInternalEvent(procFinishMsg);
                    }
                }
            } else if (ownerType.equals(OwnerType.PROCESS_INSTANCE)
                    || ownerType.equals(OwnerType.MAIN_PROCESS_INSTANCE)
                    || ownerType.equals(OwnerType.ERROR)) {
                // local process call or call to error/correction/delay handler
                Long activityInstId = event.getSecondaryOwnerId();
                ActivityInstance actInst = edao.getActivityInstance(activityInstId);
                ProcessInstance procInst = edao.getProcessInstance(actInst.getProcessInstanceId());
                BaseActivity cntrActivity = prepareActivityForResume(event,procInst, actInst);
                if (cntrActivity!=null) {
                    resumeProcessInstanceForSecondaryOwner(event, cntrActivity);
                }    // else the process is completed/cancelled
            }
        } catch (Exception ex) {
            throw new ProcessException(ex.getMessage(), ex);
        }
    }

    private void handleResumeOnHold(GeneralActivity cntrActivity, ActivityInstance actInst, ProcessInstance procInst)
            throws MdwException {
        try {
            InternalEvent event = InternalEvent.createActivityNotifyMessage(actInst,
                    EventType.RESUME, procInst.getMasterRequestId(), actInst.getStatusCode()==WorkStatus.STATUS_COMPLETED? "Completed" : null);
            boolean finished = ((SuspendableActivity)cntrActivity).resumeWaiting(event);
            resumeActivityFinishSub(actInst, (BaseActivity)cntrActivity, procInst, finished, true);
        } catch (Exception ex) {
            logger.error("Resume failed", ex);
            String statusMsg = "activity failed during resume";
            try {
                String logtag = EngineLogger.logtag(procInst.getProcessId(), procInst.getId(), actInst.getActivityId(), actInst.getId());
                failActivityInstance(actInst, statusMsg, procInst, logtag, statusMsg);
            } catch (SQLException ex1) {
                throw new DataAccessException(-1, ex1.getMessage(), ex1);
            }
            DocumentReference docRef = createActivityExceptionDocument(procInst, actInst, (BaseActivity)cntrActivity, ex);
            InternalEvent event = InternalEvent.createActivityErrorMessage(
                    actInst.getActivityId(), actInst.getId(), procInst.getId(), null,
                    procInst.getMasterRequestId(), statusMsg, docRef.getDocumentId());
            sendInternalEvent(event);
        }
    }

    /**
     * Resumes the process instance for the secondary owner
     */
    private void resumeProcessInstanceForSecondaryOwner(InternalEvent event, BaseActivity activity) throws Exception {
        Long actInstId = event.getSecondaryOwnerId();
        ActivityInstance actInst = edao.getActivityInstance(actInstId);
        String masterRequestId = event.getMasterRequestId();
        Long parentInstId = actInst.getProcessInstanceId();
        ProcessInstance parentInst = edao.getProcessInstance(parentInstId);
        String logtag = EngineLogger.logtag(parentInst.getProcessId(), parentInstId, actInst.getActivityId(), actInstId);
        boolean isEmbeddedProcess;
        if (event.getOwnerType().equals(OwnerType.MAIN_PROCESS_INSTANCE)) {
            isEmbeddedProcess = true;
        }
        else if (event.getOwnerType().equals(OwnerType.PROCESS_INSTANCE)) {
            try {
                Process subprocdef = ProcessCache.getProcess(event.getWorkId());
                isEmbeddedProcess = subprocdef.isEmbeddedProcess();
            } catch (Exception ex) {
                // can happen when the subprocess is remote
                String msg = "subprocess definition cannot be found - treat it as a remote process - id " + event.getWorkId();
                engineLogger.info(logtag, actInst.getProcessInstanceId(), actInstId, msg);
                isEmbeddedProcess = false;
            }
        } else {
            isEmbeddedProcess = false;    // including the case the subprocess is remote
        }
        String compCode = event.getCompletionCode();
        if (isEmbeddedProcess || event.getOwnerType().equals(OwnerType.ERROR)) {
            // mark parent process instance in progress
            edao.setProcessInstanceStatus(parentInst.getId(), WorkStatus.STATUS_IN_PROGRESS);
            String msg = "Activity resumed from embedded subprocess, which returns completion code " + compCode;
            engineLogger.info(logtag, actInst.getProcessInstanceId(), actInstId, msg);
            CompletionCode parsedCompCode = new CompletionCode();
            parsedCompCode.parse(event.getCompletionCode());
            Transition outgoingWorkTransVO = null;
            if (compCode == null || parsedCompCode.getEventType().equals(EventType.RESUME)) {
                // default behavior
                if (actInst.getStatusCode()==WorkStatus.STATUS_HOLD ||
                        actInst.getStatusCode()==WorkStatus.STATUS_COMPLETED) {
                    handleResumeOnHold(activity, actInst, parentInst);
                } else if (actInst.getStatusCode()==WorkStatus.STATUS_FAILED) {
                    completeActivityInstance(actInst, compCode, parentInst, logtag);
                    // notify registered monitors
                    activity.notifyMonitors(InternalLogMessage.ACTIVITY_FAIL);

                    InternalEvent jmsmsg = InternalEvent.createActivityNotifyMessage(actInst,
                            EventType.FINISH, masterRequestId, null);
                    sendInternalEvent(jmsmsg);
                }
            } else if (parsedCompCode.getEventType().equals(EventType.ABORT)) {
                // TaskAction.ABORT and TaskAction.CANCEL
                String comment = actInst.getMessage() + "  \nException handler returns " + compCode;
                if (actInst.getStatusCode() != WorkStatus.STATUS_COMPLETED) {
                    cancelActivityInstance(actInst, comment, parentInst, logtag);
                }
                if (parsedCompCode.getCompletionCode()!=null && parsedCompCode.getCompletionCode().startsWith("process"))    {// TaskAction.ABORT
                    InternalEvent outgoingMsg = InternalEvent.createActivityNotifyMessage(actInst,
                            EventType.ABORT, parentInst.getMasterRequestId(), null);
                    sendInternalEvent(outgoingMsg);
                }
            } else if (parsedCompCode.getEventType().equals(EventType.START)) {        // TaskAction.RETRY
                String comment = actInst.getMessage() + "  \nException handler returns " + compCode;
                if (actInst.getStatusCode() != WorkStatus.STATUS_COMPLETED) {
                    cancelActivityInstance(actInst, comment, parentInst, logtag);
                }
                retryActivity(parentInst, actInst.getActivityId(), masterRequestId);
            } else {
                // event type must be FINISH
                if (parsedCompCode.getCompletionCode() != null)
                    outgoingWorkTransVO = findTaskActionWorkTransition(parentInst, actInst, parsedCompCode.getCompletionCode());
                if (actInst.getStatusCode() != WorkStatus.STATUS_COMPLETED && actInst.getStatusCode() != WorkStatus.STATUS_CANCELLED) {
                    completeActivityInstance(actInst, compCode, parentInst, logtag);
                    activity.notifyMonitors(InternalLogMessage.ACTIVITY_COMPLETE);
                    InternalEvent jmsmsg;
                    int delay = 0;
                    if (outgoingWorkTransVO != null) {
                        // is custom action (RESUME), transition accordingly
                        TransitionInstance workTransInst = createTransitionInstance(outgoingWorkTransVO, parentInstId);
                        jmsmsg = InternalEvent.createActivityStartMessage(
                                outgoingWorkTransVO.getToId(), parentInstId,
                                workTransInst.getTransitionInstanceID(), masterRequestId,
                                outgoingWorkTransVO.getLabel());
                        delay = getTransitionDelay(outgoingWorkTransVO, parentInst);
                    } else {
                        jmsmsg = InternalEvent.createActivityNotifyMessage(actInst,
                                EventType.FINISH, masterRequestId, null);
                    }
                    if (delay > 0) {
                        String msgid = ScheduledEvent.INTERNAL_EVENT_PREFIX + parentInstId
                                + "start" + outgoingWorkTransVO.getToId();
                        sendDelayedInternalEvent(jmsmsg, delay, msgid, false);
                    } else sendInternalEvent(jmsmsg);
                }
            }
        } else {
            // must be InvokeProcessActivity
            if (actInst.getStatusCode() == WorkStatus.STATUS_WAITING || actInst.getStatusCode() == WorkStatus.STATUS_HOLD) {
                boolean isSynchronized = ((InvokeProcessActivity)activity).resume(event);
                if (isSynchronized) {   // all subprocess instances terminated
                    // mark parent process instance in progress
                    edao.setProcessInstanceStatus(parentInst.getId(), WorkStatus.STATUS_IN_PROGRESS);
                    // complete the activity and send activity FINISH message
                    CompletionCode parsedCompCode = new CompletionCode();
                    parsedCompCode.parse(event.getCompletionCode());
                    if (parsedCompCode.getEventType().equals(EventType.ABORT)) {
                        cancelActivityInstance(actInst, "Subprocess is cancelled", parentInst, logtag);
                    } else {
                        completeActivityInstance(actInst, compCode, parentInst, logtag);
                        activity.notifyMonitors(InternalLogMessage.ACTIVITY_COMPLETE);
                    }
                    InternalEvent jmsmsg = InternalEvent.createActivityNotifyMessage(actInst,
                            EventType.FINISH, masterRequestId, compCode);
                    sendInternalEvent(jmsmsg);
                }  else {
                    // multiple instances and not all terminated - do nothing
                    String msg = "Activity continue suspend - not all child processes have completed";
                    engineLogger.info(logtag, actInst.getProcessInstanceId(), actInstId, msg);
                }
            } else {  // status is COMPLETED or others
                // do nothing - asynchronous subprocess call
                String msg = "Activity not waiting for subprocess - asynchronous subprocess call";
                engineLogger.info(logtag, actInst.getProcessInstanceId(), actInstId, msg);
            }
        }
    }

    private void completeProcessInstance(ProcessInstance procInst) throws Exception {
        edao.setProcessCompletionTime(procInst);
        edao.setProcessInstanceStatus(procInst.getId(), WorkStatus.STATUS_COMPLETED);
        if (!inService) {
            edao.removeEventWaitForProcessInstance(procInst.getId());
            cancelTasksOfProcessInstance(procInst);
        }
    }

    private void completeProcessInstance(ProcessInstance processInst, String completionCode, boolean noNotify)
            throws Exception {

        Process process = getProcessDefinition(processInst);
        InternalEvent retMsg = InternalEvent.createProcessFinishMessage(processInst);

        if (OwnerType.ACTIVITY_INSTANCE.equals(processInst.getSecondaryOwner())) {
            retMsg.setSecondaryOwnerType(processInst.getSecondaryOwner());
            retMsg.setSecondaryOwnerId(processInst.getSecondaryOwnerId());
        }

        if (completionCode==null) completionCode = processInst.getCompletionCode();
        if (completionCode!=null) retMsg.setCompletionCode(completionCode);

        boolean isCancelled = false;
        if (completionCode==null) {
            completeProcessInstance(processInst);
        } else if (process.isEmbeddedProcess()) {
            completeProcessInstance(processInst);
            retMsg.setCompletionCode(completionCode);
        } else {
            CompletionCode parsedCompCode = new CompletionCode();
            parsedCompCode.parse(completionCode);
            if (parsedCompCode.getEventType().equals(EventType.ABORT)) {
                cancelProcessInstanceTree(processInst);
                isCancelled = true;
            } else if (parsedCompCode.getEventType().equals(EventType.FINISH)) {
                completeProcessInstance(processInst);
                if (parsedCompCode.getCompletionCode()!=null) {
                    completionCode = parsedCompCode.getCompletionCode();
                    retMsg.setCompletionCode(completionCode);
                } else completionCode = null;
            } else {
                completeProcessInstance(processInst);
                retMsg.setCompletionCode(completionCode);
            }
        }
        if (!noNotify)
            sendInternalEvent(retMsg);
        String msg = (isCancelled ? InternalLogMessage.PROCESS_CANCEL.message : InternalLogMessage.PROCESS_COMPLETE.message) + " - " + process.getQualifiedName()
                + (isCancelled ? "" : completionCode == null ? " completion code is null" : (" completion code = " + completionCode));
        engineLogger.info(process.getId(), processInst.getId(), processInst.getMasterRequestId(), msg);
        notifyMonitors(processInst, InternalLogMessage.PROCESS_COMPLETE);
    }

    /**
     * Look up the appropriate work transition for an embedded exception handling subprocess.
     * @param parentInstance the parent process
     * @param activityInstance the activity in the main process
     * @param taskAction the selected task action
     * @return the matching work transition, if found
     */
    private Transition findTaskActionWorkTransition(ProcessInstance parentInstance,
            ActivityInstance activityInstance, String taskAction) {
        if (taskAction == null)
            return null;

        Process processVO = getProcessDefinition(parentInstance);
        Transition workTransVO = processVO.getTransition(activityInstance.getActivityId(), EventType.RESUME, taskAction);
        if (workTransVO == null) {
            // try upper case
            workTransVO = processVO.getTransition(activityInstance.getActivityId(), EventType.RESUME, taskAction.toUpperCase());
        }
        if (workTransVO == null) {
            workTransVO = processVO.getTransition(activityInstance.getActivityId(), EventType.FINISH, taskAction);
        }
        return workTransVO;
    }

    private void retryActivity(ProcessInstance procInst, Long actId, String masterRequestId)
            throws SQLException, MdwException {
        // make sure any other activity instances are closed
        List<ActivityInstance> activityInstances = edao.getActivityInstances(actId, procInst.getId(),
                true, false);
        for (ActivityInstance actInst :  activityInstances) {
            if (actInst.getStatusCode() == WorkStatus.STATUS_IN_PROGRESS
                    || actInst.getStatusCode()== WorkStatus.STATUS_PENDING_PROCESS) {
                String logtag = EngineLogger.logtag(procInst.getProcessId(), procInst.getId(), actId, actInst.getId());
                failActivityInstance(actInst, "Retry Activity Action", procInst, logtag, "Retry Activity Action");
            }
        }
        // start activity again
        InternalEvent event = InternalEvent.createActivityStartMessage(actId,
                procInst.getId(), null, masterRequestId, EventType.EVENTNAME_START);
        sendInternalEvent(event);
    }

    private boolean validateProcessInstance(ProcessInstance processInstance) {
        Integer status = processInstance.getStatusCode();
        if (WorkStatus.STATUS_CANCELLED.equals(status)) {
            logger.info("ProcessInstance has been cancelled. ProcessInstanceId = " + processInstance.getId());
            return false;
        } else if (WorkStatus.STATUS_COMPLETED.equals(status)) {
            logger.info("ProcessInstance has been completed. ProcessInstanceId = " + processInstance.getId());
            return false;
        } else {
            return true;
        }
    }

    private BaseActivity prepareActivityForResume(InternalEvent event,
            ProcessInstance procInst, ActivityInstance actInst) {
        Long actId = actInst.getActivityId();
        Long procInstId = actInst.getProcessInstanceId();

        if (!validateProcessInstance(procInst)) {
            String msg = "Activity would resume, but process is no longer alive";
            engineLogger.info(procInst.getProcessId(), procInstId, actId, actInst.getId(), msg);
            return null;
        }
        String msg = "Activity to resume";
        engineLogger.info(procInst.getProcessId(), procInstId, actId, actInst.getId(), msg);

        Process process = getProcessDefinition(procInst);
        Activity activity = process.getActivity(actId);

        TrackingTimer activityTimer = null;
        try {
            // use design-time package
            Package pkg = PackageCache.getPackage(getMainProcessDefinition(procInst).getPackageName());
            BaseActivity activityInstance = (BaseActivity)getActivityInstance(pkg, activity.getImplementor());
            Tracked t = activityInstance.getClass().getAnnotation(Tracked.class);
            if (t != null) {
                String logTag = EngineLogger.logtag(procInst.getProcessId(), procInst.getId(), actId, actInst.getId());
                activityTimer = new TrackingTimer(logTag, activityInstance.getClass().getName(), t.value());
                activityTimer.start("Prepare Activity for Resume");
            }
            List<VariableInstance> vars = process.isEmbeddedProcess()?
                    edao.getProcessInstanceVariables(procInst.getOwnerId()):
                    edao.getProcessInstanceVariables(procInstId);
            // procInst.setVariables(vars);     set inside edac method
            Long workTransitionInstId = event.getTransitionInstanceId();
            activityInstance.prepare(activity, procInst, actInst, vars, workTransitionInstId,
                    activityTimer, new ProcessExecutor(this));
            return activityInstance;
        } catch (Exception e) {
            engineLogger.error(procInst.getProcessId(), procInst.getId(), actInst.getActivityId(), actInst.getId(),
                    "Unable to instantiate implementer " + activity.getImplementor(), e);
            return null;
        }
        finally {
            if (activityTimer != null) {
                activityTimer.stopAndLogTiming();
            }
        }
    }

    private boolean isProcessInstanceResumable(ProcessInstance pInstance) {
        int statusCd = pInstance.getStatusCode();
        if (statusCd == WorkStatus.STATUS_COMPLETED) {
            return false;
        } else {
            return statusCd != WorkStatus.STATUS_CANCELLED;
        }
    }

    ActivityRuntime resumeActivityPrepare(ProcessInstance procInst, InternalEvent event, boolean resumeOnHold)
            throws ProcessException, DataAccessException {
        Long actInstId = event.getWorkInstanceId();
        try {
            ActivityRuntime ar = new ActivityRuntime();
            ar.startCase = ActivityRuntime.RESUMECASE_NORMAL;
            ar.actinst = edao.getActivityInstance(actInstId);
            ar.procinst = procInst;
            if (!isProcessInstanceResumable(ar.procinst)) {
                ar.startCase = ActivityRuntime.RESUMECASE_PROCESS_TERMINATED;
                String msg = "Cannot resume activity instance as the process is completed/canceled";
                engineLogger.info(ar.procinst.getProcessId(), ar.procinst.getId(), ar.actinst.getActivityId(), actInstId, msg);
                return ar;
            }
            if (!resumeOnHold && ar.actinst.getStatusCode() != WorkStatus.STATUS_WAITING) {
                String msg = "Cannot resume activity instance as it is not waiting any more";
                engineLogger.info(ar.procinst.getProcessId(), ar.procinst.getId(), ar.actinst.getActivityId(), actInstId, msg);
                ar.startCase = ActivityRuntime.RESUMECASE_ACTIVITY_NOT_WAITING;
                return ar;
            }
            ar.activity = prepareActivityForResume(event, ar.procinst, ar.actinst);
            if (resumeOnHold)
                event.setEventType(EventType.RESUME);
            else
                event.setEventType(EventType.FINISH);
            return ar;
        } catch (SQLException ex) {
            throw new ProcessException(ex.getMessage(), ex);
        }
    }

    private void resumeActivityFinishSub(ActivityInstance actInst, BaseActivity activity, ProcessInstance procInst,
            boolean finished, boolean resumeOnHold)  throws SQLException, MdwException {
        String logtag = EngineLogger.logtag(procInst.getProcessId(),procInst.getId(), actInst.getActivityId(),actInst.getId());
        if (finished) {
            CompletionCode completionCode = new CompletionCode();
            completionCode.parse(activity.getReturnCode());
            if (WorkStatus.STATUS_HOLD.equals(completionCode.getActivityInstanceStatus())) {
                holdActivityInstance(actInst, logtag);
            } else if (WorkStatus.STATUS_WAITING.equals(completionCode.getActivityInstanceStatus())) {
                suspendActivityInstance(activity, actInst, logtag, "continue suspend");
            } else if (WorkStatus.STATUS_CANCELLED.equals(completionCode.getActivityInstanceStatus())) {
                cancelActivityInstance(actInst, "Cancelled upon resume", procInst, logtag);
            } else if (WorkStatus.STATUS_FAILED.equals(completionCode.getActivityInstanceStatus())) {
                failActivityInstance(actInst, "Failed upon resume", procInst, logtag, activity.getReturnMessage());
            } else {    // status is null or Completed
                completeActivityInstance(actInst, completionCode.toString(), procInst, logtag);
                // notify registered monitors
                activity.notifyMonitors(InternalLogMessage.ACTIVITY_COMPLETE);
            }
            InternalEvent event = InternalEvent.createActivityNotifyMessage(actInst,
                    completionCode.getEventType(), procInst.getMasterRequestId(),
                    completionCode.getCompletionCode());
            sendInternalEvent(event);
        } else {
            if (resumeOnHold) {
                suspendActivityInstance(activity, actInst, logtag, "resume waiting after hold");
            } else {
                engineLogger.info(logtag, actInst.getProcessInstanceId(), actInst.getId(), "continue suspend");
            }
        }
    }

    void resumeActivityFinish(ActivityRuntime ar, boolean finished, InternalEvent event, boolean resumeOnHold)
            throws ProcessException {
        try {
            if (ar.activity.getTimer() != null)
                ar.activity.getTimer().start("Resume Activity Finish");
            resumeActivityFinishSub(ar.actinst, ar.activity, ar.procinst, finished, resumeOnHold);
        } catch (SQLException | MdwException ex) {
            throw new ProcessException(ex.getMessage(), ex);
        } finally {
            if (ar.activity.getTimer() != null)
                ar.activity.getTimer().stopAndLogTiming();
        }
    }

    boolean resumeActivityExecute(ActivityRuntime ar, InternalEvent event, boolean resumeOnHold)
            throws ActivityException {
        boolean finished;
        try {
            if (ar.activity.getTimer() != null)
                ar.activity.getTimer().start("Resume Activity");
            if (resumeOnHold)
                finished = ((SuspendableActivity)ar.activity).resumeWaiting(event);
            else
                finished = ((SuspendableActivity)ar.activity).resume(event);
        }
        finally {
            if (ar.activity.getTimer() != null)
                ar.activity.getTimer().stopAndLogTiming();
        }
        return finished;
    }

    Map<String, String> getOutputParameters(Long procInstId, Long procId)
            throws IOException, SQLException, DataAccessException {
        Process subprocDef = ProcessCache.getProcess(procId);
        Package pkg = getPackage(subprocDef);
        Map<String,String> params = new HashMap<>();
        boolean passDocContent = (isInService() && getDataAccess().getPerformanceLevel() >= 5) || getDataAccess().getPerformanceLevel() >= 9 ;  // DHO  (if not serviceProc then lvl9)
        for (Variable var : subprocDef.getVariables()) {
            if (var.getVariableCategory() == Variable.CAT_OUTPUT
                    || var.getVariableCategory() == Variable.CAT_INOUT) {
                VariableInstance vi = getDataAccess().getVariableInstance(procInstId, var.getName());
                if (vi != null) {
                    if (passDocContent && vi.isDocument(pkg)) {
                        Document docvo = getDocument((DocumentReference)vi.getData(pkg), false);
                        if (docvo != null)
                            params.put(var.getName(), docvo.getContent(getPackage(subprocDef)));
                    }
                    else {
                        params.put(var.getName(), vi.getStringValue(pkg));
                    }
                }
            }
        }
        return params;
    }

    void resumeActivityException(ProcessInstance procInst, Long actInstId, BaseActivity activity, Throwable cause) {
        String compCode = null;
        try {
            String statusMsg = buildStatusMessage(cause);
            ActivityInstance actInst = edao.getActivityInstance(actInstId);
            String logtag = EngineLogger.logtag(procInst.getProcessId(), procInst.getId(), actInst.getActivityId(), actInst.getId());
            failActivityInstance(actInst, statusMsg, procInst, logtag, "Exception in resume");
            if (activity == null || !AdapterActivity.COMPCODE_AUTO_RETRY.equals(activity.getReturnCode())) {
                Throwable th = cause == null ? new ActivityException("Resume activity: " + actInstId) : cause;
                DocumentReference docRef = createActivityExceptionDocument(procInst, actInst, activity, th);
                InternalEvent outgoingMsg = InternalEvent.createActivityErrorMessage(
                        actInst.getActivityId(), actInst.getId(),
                        procInst.getId(), compCode, procInst.getMasterRequestId(),
                        statusMsg, docRef.getDocumentId());
                sendInternalEvent(outgoingMsg);
            }
        }
        catch (Exception ex) {
            engineLogger.error(procInst.getProcessId(), procInst.getId(), activity.getActivityId(), actInstId,
                    "**Failed in handleResumeException**", ex);
        }
    }

    /**
     * Abort a single process instance by process instance ID,
     * or abort potentially multiple (but typically one) process instances
     * by process ID and owner ID.
     */
    void abortProcessInstance(InternalEvent event) throws ProcessException {
        Long processId = event.getWorkId();
        String processOwner = event.getOwnerType();
        Long processOwnerId = event.getOwnerId();
        Long processInstId = event.getWorkInstanceId();
        try {
            if (processInstId != null && processInstId != 0L) {
                ProcessInstance pi = edao.getProcessInstance(processInstId);
                cancelProcessInstanceTree(pi);
                engineLogger.info(pi.getProcessId(), pi.getId(), pi.getMasterRequestId(), "Process cancelled");
            } else {
                List<ProcessInstance> coll = edao.getProcessInstances(processId, processOwner, processOwnerId);
                if (coll == null || coll.isEmpty()) {
                    logger.info("No Process Instances for the Process and Owner");
                    return;
                }
                for (ProcessInstance pi : coll) {
                    // there really should have only one
                    cancelProcessInstanceTree(pi);
                }
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new ProcessException(ex.getMessage());
        }
    }

    /**
     * Cancels the process instance as well as all descendant process instances.
     * Deregisters associated event wait instances.
     */
    private void cancelProcessInstanceTree(ProcessInstance pi) throws Exception {
        if (pi.getStatusCode().equals(WorkStatus.STATUS_COMPLETED) ||
                pi.getStatusCode().equals(WorkStatus.STATUS_CANCELLED)) {
            throw new ProcessException("ProcessInstance is not in a cancellable state");
        }
        List<ProcessInstance> childInstances = edao.getChildProcessInstances(pi.getId());
        for (ProcessInstance child : childInstances) {
            if (!child.getStatusCode().equals(WorkStatus.STATUS_COMPLETED)
                    && !child.getStatusCode().equals(WorkStatus.STATUS_CANCELLED)) {
                cancelProcessInstanceTree(child);
            } else {
                logger.info("Descendent ProcessInstance in not in a cancellable state. ProcessInstanceId=" + child.getId());
            }
        }
        cancelProcessInstance(pi);
    }

    /**
     * Cancels a single process instance.
     * It cancels all active transition instances, all event wait instances,
     * and sets the process instance into canceled status.
     *
     * The method does not cancel task instances
     *
     * @param processInstance process instance.
     */
    private void cancelProcessInstance(ProcessInstance processInstance) throws Exception {
        edao.cancelTransitionInstances(processInstance.getId(),
                "ProcessInstance has been cancelled.", null);
        edao.setProcessInstanceStatus(processInstance.getId(), WorkStatus.STATUS_CANCELLED);
        edao.removeEventWaitForProcessInstance(processInstance.getId());
        cancelErrorHandlers(processInstance);
        cancelExceptionHandlers(processInstance);
        cancelTasksOfProcessInstance(processInstance);
    }

    private void cancelErrorHandlers(ProcessInstance procInst) throws Exception {
        Query query = new Query();
        procInst = ServiceLocator.getWorkflowServices().getProcess(procInst.getId());
        for (ActivityInstance activity : procInst.getActivities()) {
            query.setFilter("owner", "ERROR");
            query.setFilter("secondaryOwner", "ACTIVITY_INSTANCE");
            query.setFilter("secondaryOwnerId", activity.getId());
            query.setSort("process_instance_id");
            query.setDescending(true);
            List<ProcessInstance> processInstanceList = ServiceLocator.getWorkflowServices().getProcesses(query).getProcesses();
            for (ProcessInstance pi : processInstanceList) {
                cancelProcessInstance(pi);
            }
        }
    }

    private void cancelExceptionHandlers(ProcessInstance procInst) throws Exception {
        Query query = new Query();
        query.setFilter("owner", "MAIN_PROCESS_INSTANCE");
        query.setFilter("ownerId", procInst.getId());
        query.setFilter("secondaryOwner", "ACTIVITY_INSTANCE");
        query.setSort("process_instance_id");
        query.setDescending(true);
        List<ProcessInstance> processInstanceList = ServiceLocator.getWorkflowServices().getProcesses(query).getProcesses();
        for (ProcessInstance pi : processInstanceList) {
            cancelProcessInstance(pi);
        }
    }

    private void cancelTasksOfProcessInstance(ProcessInstance procInst) throws SQLException, MdwException {
        List<ProcessInstance> processInstanceList =
                edao.getChildProcessInstances(procInst.getId());
        List<Long> procInstIds = new ArrayList<>();
        procInstIds.add(procInst.getId());
        for (ProcessInstance pi : processInstanceList) {
            Process pidef = getProcessDefinition(pi);
            if (pidef.isEmbeddedProcess())
                procInstIds.add(pi.getId());
        }
        TaskServices taskServices = ServiceLocator.getTaskServices();
        for (Long procInstId : procInstIds) {
            taskServices.cancelTaskInstancesForProcess(procInstId);
        }
    }

    EventWaitInstance createEventWaitInstance(Long procInstId, Long actInstId, String pEventName, String compCode,
            boolean recurring, boolean notifyIfArrived) throws ProcessException {
        return createEventWaitInstance(procInstId, actInstId, pEventName, compCode, recurring, notifyIfArrived, false);
    }

    EventWaitInstance createEventWaitInstance(Long procInstId, Long actInstId, String eventName, String compCode,
            boolean recurring, boolean notifyIfArrived, boolean reregister) throws ProcessException {
        try {
            String finish = EventType.getEventTypeName(EventType.FINISH);
            if (compCode == null || compCode.length() == 0)
                compCode = finish;
            EventWaitInstance ret = null;
            Long documentId = edao.recordEventWait(eventName, !recurring, 3600, actInstId, compCode);

            String msg = "registered event wait event='" + eventName + "' actInst=" + actInstId + (recurring ? " as recurring" : " as broadcast-waiting");
            engineLogger.info(procInstId, actInstId, msg);

            if (documentId != null && !reregister) {
                msg = (notifyIfArrived ? "notify" : "return") + " event before registration: event='" + eventName + "' actInst=" + actInstId;
                engineLogger.info(procInstId, actInstId, msg);
                if (notifyIfArrived) {
                    if (compCode.equals(finish)) compCode = null;
                    ActivityInstance actInst = edao.getActivityInstance(actInstId);
                    resumeActivityInstance(actInst, compCode, documentId, null, 0);
                    edao.removeEventWaitForActivityInstance(actInstId, "activity notified");
                } else {
                    edao.removeEventWaitForActivityInstance(actInstId, "activity to notify is returned");
                }
                ret = new EventWaitInstance();
                ret.setMessageDocumentId(documentId);
                ret.setCompletionCode(compCode);
                Document doc = edao.getDocument(documentId, true);
                edao.updateDocumentInfo(doc);
            }
            return ret;
        } catch (MdwException | SQLException ex) {
            throw new ProcessException(ex.getMessage(), ex);
        }
    }

    EventWaitInstance createEventWaitInstances(Long procInstId, Long actInstId, String[] eventNames,
            String[] wakeUpEventTypes, boolean[] eventOccurances, boolean notifyIfArrived, boolean reregister)
            throws ProcessException {
        try {
            EventWaitInstance ret = null;
            Long documentId = null;
            String compCode = null;
            int i;
            for (i = 0; i < eventNames.length; i++) {
                compCode = wakeUpEventTypes[i];
                documentId = edao.recordEventWait(eventNames[i],
                        !eventOccurances[i],
                        3600,       // TODO set this value in Studio
                        actInstId, wakeUpEventTypes[i]);
                String msg = "registered event wait event='" + eventNames[i] + "' actInst=" + actInstId +
                        (eventOccurances[i] ? " as recurring" : " as broadcast-waiting");
                engineLogger.info(procInstId, actInstId, msg);

                if (documentId != null && !reregister)
                    break;
            }
            if (documentId != null && !reregister) {
                String msg = (notifyIfArrived ? "notify" : "return") + " event before registration: event='" +
                        eventNames[i] + "' actInst=" + actInstId;
                engineLogger.info(procInstId, actInstId, msg);
                if (compCode != null && compCode.length() == 0)
                    compCode = null;
                if (notifyIfArrived) {
                    ActivityInstance actInst = edao.getActivityInstance(actInstId);
                    resumeActivityInstance(actInst, compCode, documentId, null, 0);
                    edao.removeEventWaitForActivityInstance(actInstId, "activity notified");
                } else {
                    edao.removeEventWaitForActivityInstance(actInstId, "activity to notify is returned");
                }
                ret = new EventWaitInstance();
                ret.setMessageDocumentId(documentId);
                ret.setCompletionCode(compCode);
                Document docvo = edao.getDocument(documentId, true);
                edao.updateDocumentInfo(docvo);
            }
            return ret;
        } catch (SQLException | MdwException ex) {
            throw new ProcessException(ex.getMessage(), ex);
        }
    }

    Integer notifyProcess(String eventName, Long docId, String message, int delay)
            throws EventException, SQLException {

        List<EventWaitInstance> waiters = edao.recordEventArrive(eventName, docId);

        if (waiters != null && !waiters.isEmpty()) {
            boolean hasFailures = false;
            try {
                for (EventWaitInstance inst : waiters) {
                    String compCode = inst.getCompletionCode();
                    if (compCode != null && compCode.length() == 0)
                        compCode = null;

                    ActivityInstance actInst = edao.getActivityInstance(inst.getActivityInstanceId());
                    String msg = "notify event after registration: event='" + eventName + "' actInst=" + inst.getActivityInstanceId();
                    engineLogger.info(actInst.getProcessInstanceId(), actInst.getId(), msg);

                    if (actInst.getStatusCode() == WorkStatus.STATUS_IN_PROGRESS) {
                        // assuming it is a service process waiting for message
                        JSONObject json = new JsonObject();
                        json.put("ACTION", "NOTIFY");
                        json.put("CORRELATION_ID", eventName);
                        json.put("MESSAGE", message);
                        internalMessenger.broadcastMessage(json.toString());
                    } else {
                        resumeActivityInstance(actInst, compCode, docId, message, delay);
                    }
                    // deregister wait instances
                    edao.removeEventWaitForActivityInstance(inst.getActivityInstanceId(), "activity notified");
                    if (docId != null && docId > 0) {
                        Document docvo = edao.getDocument(docId, true);
                        edao.updateDocumentInfo(docvo);
                    }
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new EventException(ex.getMessage(), ex);
            }
            if (hasFailures)
                return EventInstance.RESUME_STATUS_PARTIAL_SUCCESS;
            else
                return EventInstance.RESUME_STATUS_SUCCESS;
        } else {
            return EventInstance.RESUME_STATUS_NO_WAITERS;
        }
    }

    private boolean isProcessInstanceProgressable(ProcessInstance processInstance) {
        int statusCd = processInstance.getStatusCode();
        if (statusCd == WorkStatus.STATUS_COMPLETED) {
            return false;
        } else if (statusCd == WorkStatus.STATUS_CANCELLED) {
            return false;
        } else {
            return statusCd != WorkStatus.STATUS_HOLD;
        }
    }

    /**
     * Sends a RESUME internal event to resume the activity instance.
     *
     * This may be called in the following cases:
     *   1) received an external event (including the case the message is received before registration)
     *       In this case, the argument message is populated.
     *   2) when register even wait instance, and the even has already arrived. In this case
     *       the argument message null.
     */
    private void resumeActivityInstance(ActivityInstance actInst, String compCode, Long docId, String message, int delay)
            throws MdwException, SQLException {
        ProcessInstance pi = edao.getProcessInstance(actInst.getProcessInstanceId());
        if (!isProcessInstanceResumable(pi)) {
            logger.info("ProcessInstance in NOT resumable. ProcessInstanceId:" + pi.getId());
        }
        InternalEvent outgoingMsg = InternalEvent.
                createActivityNotifyMessage(actInst, EventType.RESUME, pi.getMasterRequestId(), compCode);
        if (docId != null) {        // should be always true
            outgoingMsg.setSecondaryOwnerType(OwnerType.DOCUMENT);
            outgoingMsg.setSecondaryOwnerId(docId);
        }
        if (message != null && message.length() < 2500) {
            outgoingMsg.addParameter("ExternalEventMessage", message);
        }
        if (isProcessInstanceProgressable(pi)) {
            edao.setProcessInstanceStatus(pi.getId(), WorkStatus.STATUS_IN_PROGRESS);
        }
        if (delay > 0) {
            sendDelayedInternalEvent(outgoingMsg, delay,
                    ScheduledEvent.INTERNAL_EVENT_PREFIX+actInst.getId(), false);
        } else {
            sendInternalEvent(outgoingMsg);
        }
    }

    void sendInternalEvent(InternalEvent event) throws MdwException {
        internalMessenger.sendMessage(event, edao);
    }

    void sendDelayedInternalEvent(InternalEvent event, int delaySeconds, String msgid, boolean isUpdate)
            throws MdwException {
        internalMessenger.sendDelayedMessage(event, delaySeconds, msgid, isUpdate, edao);
    }

    boolean isInService() {
        return inService;
    }

    boolean isInMemory() {
        return null != edao && edao.getPerformanceLevel() >= 9;
    }

    /**
     * Notify registered ProcessMonitors.
     */
    public void notifyMonitors(ProcessInstance processInstance, InternalLogMessage logMessage) {
        // notify registered monitors
        Process process = getMainProcessDefinition(processInstance);
        Package pkg = PackageCache.getPackage(process.getPackageName());
        // runtime context for enablement does not contain hydrated variables map (too expensive)
        List<ProcessMonitor> monitors = MonitorRegistry.getInstance()
                .getProcessMonitors(new ProcessRuntimeContext(null, pkg, process, processInstance,
                        getDataAccess().getPerformanceLevel(), isInService(), new HashMap<>()));
        if (!monitors.isEmpty()) {
            Map<String,Object> vars = new HashMap<>();
            if (processInstance.getVariables() != null) {
                for (VariableInstance var : processInstance.getVariables()) {
                    Object value = var.getData(pkg);
                    if (value instanceof DocumentReference) {
                        try {
                            Document doc = getDocument((DocumentReference) value, false);
                            value = doc == null ? null : doc.getObject(var.getType(), pkg);
                        }
                        catch (DataAccessException ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                    vars.put(var.getName(), value);
                }
            }
            ProcessRuntimeContext runtimeContext = new ProcessRuntimeContext(null, pkg, process, processInstance,
                    getDataAccess().getPerformanceLevel(), isInService(), vars);

            for (ProcessMonitor monitor : monitors) {
                try {
                    if (monitor instanceof OfflineMonitor) {
                        @SuppressWarnings("unchecked")
                        OfflineMonitor<ProcessRuntimeContext> processOfflineMonitor = (OfflineMonitor<ProcessRuntimeContext>) monitor;
                        new OfflineMonitorTrigger<>(processOfflineMonitor, runtimeContext).fire(logMessage);
                    }
                    else {
                        if (logMessage == InternalLogMessage.PROCESS_START) {
                            Map<String,Object> updated = monitor.onStart(runtimeContext);
                            if (updated != null) {
                                for (String varName : updated.keySet()) {
                                    if (processInstance.getVariables() == null)
                                        processInstance.setVariables(new ArrayList<>());
                                    Variable variable = process.getVariable(varName);
                                    if (variable == null || !variable.isInput())
                                        throw new ProcessException("Process '" + process.getQualifiedLabel() + "' has no such input variable defined: " + varName);
                                    if (processInstance.getVariable(varName) != null)
                                        throw new ProcessException("Process '" + process.getQualifiedLabel() + "' input variable already populated: " + varName);
                                    if (runtimeContext.getPackage().getTranslator(variable.getType()).isDocumentReferenceVariable()) {
                                        DocumentReference docRef = createDocument(variable.getType(), OwnerType.VARIABLE_INSTANCE, 0L, updated.get(varName), getPackage(process));
                                        VariableInstance varInst = createVariableInstance(processInstance, varName, docRef);
                                        updateDocumentInfo(docRef, process.getVariable(varInst.getName()).getType(), OwnerType.VARIABLE_INSTANCE, varInst.getId(), null, null);
                                        processInstance.getVariables().add(varInst);
                                    }
                                    else {
                                        VariableInstance varInst = createVariableInstance(processInstance, varName, updated.get(varName));
                                        processInstance.getVariables().add(varInst);
                                    }
                                }
                            }
                        }
                        else if (logMessage == InternalLogMessage.PROCESS_ERROR) {
                            monitor.onError(runtimeContext);
                        }
                        else if (logMessage == InternalLogMessage.PROCESS_COMPLETE) {
                            monitor.onFinish(runtimeContext);
                        }
                    }
                }
                catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
    }

    public DocumentReference createActivityExceptionDocument(ProcessInstance processInst,
            ActivityInstance activityInst, BaseActivity activityImpl, Throwable th) throws DataAccessException {
        ActivityException actEx;
        if (th instanceof ActivityException) {
            actEx = (ActivityException) th;
        }
        else {
            if (th instanceof MdwException)
                actEx = new ActivityException(((MdwException)th).getCode(), th.toString(), th.getCause());
            else
                actEx = new ActivityException(th.toString(), th.getCause());
            actEx.setStackTrace(th.getStackTrace());
        }

        Package pkg = getPackage(getMainProcessDefinition(processInst));
        // populate activity context
        if (activityInst != null) {
            Process process = getProcessDefinition(processInst);
            if (pkg != null)
                processInst.setPackageName(pkg.getName());
            Activity activity = process.getActivity(activityInst.getActivityId());

            ActivityImplementor activityImplementor = ImplementorCache.get(activity.getImplementor());
            String category = activityImplementor == null ? GeneralActivity.class.getName() : activityImplementor.getCategory();
            ActivityRuntimeContext runtimeContext = new ActivityRuntimeContext(null, pkg, process, processInst,
                    getDataAccess().getPerformanceLevel(), isInService(), activity, category, activityInst,
                    activityImpl instanceof SuspendableActivity);
            // TODO option to suppress variables
            if (activityImpl == null) {
                try {
                    processInst.setVariables(getDataAccess().getProcessInstanceVariables(processInst.getId()));
                } catch (SQLException ignored) {}
            }
            for (Variable var : process.getVariables()) {
                try {
                    if (activityImpl != null)
                        runtimeContext.getValues().put(var.getName(), activityImpl.getValue(var.getName()));
                    else if (processInst.getVariable(var.getName()) != null) {
                        Object value = processInst.getVariable(var.getName()).getData(pkg);
                        if (value instanceof DocumentReference) {
                            Document doc = getDocument((DocumentReference)value, false);
                            value = doc == null ? null : doc.getObject(var.getType(), pkg);
                        }
                        runtimeContext.getValues().put(var.getName(), processInst.getVariable(var.getName()).getData(pkg));
                    }
                }
                catch (ActivityException | DataAccessException ex) {
                    engineLogger.error(processInst.getProcessId(), processInst.getId(), activityInst.getActivityId(), activityInst.getId(), ex.getMessage(), ex);
                }
            }

            actEx.setRuntimeContext(runtimeContext);
        }

        return createDocument(Exception.class.getName(), OwnerType.ACTIVITY_INSTANCE, activityInst.getId(), actEx, pkg);
    }

    public DocumentReference createProcessExceptionDocument(ProcessInstance processInst, Throwable th)
            throws DataAccessException {
        ProcessException procEx;
        if (th instanceof ProcessException) {
            procEx = (ProcessException) th;
        }
        else {
            if (th instanceof MdwException)
                procEx = new ProcessException(((MdwException)th).getCode(), th.toString(), th.getCause());
            else
                procEx = new ProcessException(th.toString(), th.getCause());
            procEx.setStackTrace(th.getStackTrace());
        }
        Package pkg = getPackage(getMainProcessDefinition(processInst));
        Long procId = processInst.getId();
        Process process = getProcessDefinition(processInst);
        if (pkg != null)
            processInst.setPackageName(pkg.getName());

        ProcessRuntimeContext runtimeContext = new ProcessRuntimeContext(null, pkg, process, processInst,
                getDataAccess().getPerformanceLevel(), isInService());

        try {
            processInst.setVariables(getDataAccess().getProcessInstanceVariables(processInst.getId()));
        } catch (SQLException ignored) {}

        for (VariableInstance var : processInst.getVariables()) {
            Object value = var.getData(pkg);
            if (value instanceof DocumentReference) {
                try {
                    Document doc = getDocument((DocumentReference) value, false);
                    value = doc == null ? null : doc.getObject(var.getType(), pkg);
                }
                catch (DataAccessException ex) {
                    engineLogger.error(processInst.getProcessId(), processInst.getId(), processInst.getMasterRequestId(), ex.getMessage(), ex);
                }
            }
            runtimeContext.getValues().put(var.getName(), value);
        }

        procEx.setRuntimeContext(runtimeContext);

        return createDocument(Exception.class.getName(), OwnerType.PROCESS_INSTANCE, procId, procEx, pkg);
    }

    private Package getPackage(Process process) {
        return PackageCache.getPackage(process.getPackageName());
    }

    private GeneralActivity getActivityInstance(Package pkg, String implClass) throws Exception {
        ActivityImplementor activityImplementor = ImplementorCache.get(implClass);
        if (activityImplementor != null && activityImplementor.getSupplier() != null) {
            return activityImplementor.getSupplier().get();
        }
        return pkg.getActivityImplementor(implClass);
    }
}
