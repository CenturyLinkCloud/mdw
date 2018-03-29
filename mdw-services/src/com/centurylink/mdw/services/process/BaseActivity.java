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
package com.centurylink.mdw.services.process;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.app.Compatibility.SubstitutionResult;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.cloud.CloudClassLoader;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.groovy.GroovyNaming;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.task.TaskAction;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.VariableInstance;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.ActivityInstance;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.OfflineMonitor;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.GroovyExecutor;
import com.centurylink.mdw.script.ScriptEvaluator;
import com.centurylink.mdw.script.ScriptExecutor;
import com.centurylink.mdw.service.data.process.EngineDataAccess;
import com.centurylink.mdw.service.data.process.EngineDataAccessCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.OfflineMonitorTrigger;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.TransactionWrapper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.TrackingTimer;

/**
 * Base class that implements the Controlled Activity.
 * All the controlled activities should extend this class.
 */
public abstract class BaseActivity implements GeneralActivity {

    protected static StandardLogger logger = null;
    // Note 1: do not use static initializer for logger, so that
    //   unit tester can set unit test property manager
    // Note 2: logger should be private as we expect applications to use canned methods
    //   such as logsevere. It is left as "protected" to avoid a lot of changes
    //   for older applications to upgrade

    public static final String JAVASCRIPT = "JavaScript";
    public static final String GROOVY = "Groovy";
    public static final String JAVA_EL = "javax.el";
    public static final String OUTPUTDOCS = "Output Documents";
    public static final String DISABLED = "disabled";

    private Long workTransitionInstanceId;
    private String returnCode;
    private String returnMessage;
    private String entryCode;
    private List<VariableInstance> parameters;
    private List<Attribute> attributes;
    private ProcessExecutor engine;
    private TrackingTimer timer;
    private String[] outputDocuments;
    private Activity activityDef;
    private ProcessInstance processInst;
    private ActivityInstance activityInst;
    private ActivityRuntimeContext _runtimeContext;
    private Package pkg;

    /**
     * Repopulates variable values in case they've changed during execution.
     */
    public ActivityRuntimeContext getRuntimeContext() throws ActivityException {
        for (VariableInstance var : getParameters()) {
            _runtimeContext.getVariables().put(var.getName(), getVariableValue(var.getName()));
        }
        return _runtimeContext;
    }

    /**
     * This version is used by the new engine (ProcessExecuter).
     * @param parameters variable instances of the process instance,
     *    or of the parent process instance when this is in an embedded process
     */
    void prepare(Activity actVO, ProcessInstance pi, ActivityInstance ai,
            List<VariableInstance> parameters,
            Long transInstId, String entryCode, TrackingTimer timer, ProcessExecutor engine) {
        try {
            if (timer != null)
                timer.start("Prepare Activity");
            if (logger == null)
                logger = LoggerUtil.getStandardLogger();
            this.engine = engine;
            this.processInst = pi;
            this.activityDef = actVO;
            this.activityInst = ai;
            this.workTransitionInstanceId = transInstId;
            this.parameters = parameters;
            this.attributes = actVO.getAttributes();
            this.timer = timer;
            this.entryCode = entryCode;
            try {
                pkg = PackageCache.getProcessPackage(getMainProcessDefinition().getId());
                _runtimeContext = new ActivityRuntimeContext(pkg, getProcessDefinition(), processInst, activityDef, activityInst);
                for (VariableInstance var : getParameters())
                    _runtimeContext.getVariables().put(var.getName(), getVariableValue(var.getName()));
            }
            catch (NullPointerException ex) {
                logger.severeException(ex.getMessage(), ex);
            }
            catch (ActivityException ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        finally {
            if (timer != null)
                timer.stopAndLogTiming();
        }
    }

    /**
     * This version is used to initialize adapter activities to be called
     * from API (not from runtime engine) -- mainly for unit testing
     *
     * @param attrs
     */
    public void prepare(ActivityRuntimeContext runtimeContext) {
        if (logger == null)
            logger = LoggerUtil.getStandardLogger();

        EngineDataAccess edao = EngineDataAccessCache.getInstance(true, 9);
        // InternalMessenger msgBroker = MessengerFactory.newInternalMessenger();
        engine = new ProcessExecutor(edao, null, false);

        this.procDef = this.mainProcDef = runtimeContext.getProcess();
        this.processInst = runtimeContext.getProcessInstance();
        this.pkg = runtimeContext.getPackage();
        this._runtimeContext = runtimeContext;
        this.activityDef = runtimeContext.getActivity();
        this.activityInst = runtimeContext.getActivityInstance();

        if (runtimeContext.getAttributes() != null) {
            attributes = new ArrayList<Attribute>();
            for (String attrName : runtimeContext.getAttributes().keySet())
                attributes.add(new Attribute(attrName, runtimeContext.getAttribute(attrName)));
        }

        if (runtimeContext.getVariables() != null) {
            parameters = new ArrayList<VariableInstance>();
            for (String varName : runtimeContext.getVariables().keySet())
                parameters.add(new VariableInstance(varName, runtimeContext.getVariables().get(varName)));
        }
    }

    /**
     * Provides a hook to allow activity implementors to initialize themselves.
     * @param runtimeContext the activity runtime context
     */
    protected void initialize(ActivityRuntimeContext runtimeContext) throws ActivityException {
        // TODO consider funneling all client runtime info access
    }

    /**
     * Given a variable name, return the variable instance associated
     * with the process instance.
     *
     * @param pName name of the variable
     * @return variable instance object
     */
    protected VariableInstance getVariableInstance(String pName) {
        for (int i = 0; i < parameters.size(); i++) {
            if (pName.equalsIgnoreCase(parameters.get(i).getName())) {
                return parameters.get(i);
            }
        }
        return null;
    }

    void execute(ProcessExecutor engine) throws ActivityException {
        this.engine = engine;
        if (isDisabled()) {
            loginfo("Skipping disabled activity: " + getActivityName());
        }
        else {
            try {
                initialize(_runtimeContext);
                Object ret = execute(_runtimeContext);
                if (ret != null)
                    setReturnCode(String.valueOf(ret));
            }
            catch (ActivityException ex) {
                throw ex;
            }
            finally
            {
                if (Thread.currentThread().getContextClassLoader() instanceof CloudClassLoader)
                    ApplicationContext.resetContextClassLoader();
            }
        }
    }

    void executeTimed(ProcessExecutor engine) throws ActivityException {
        try {
            if (timer != null)
                timer.start("Execute Activity");
            execute(engine);
        }
        finally {
            if (timer != null)
                timer.stopAndLogTiming();
        }
    }

    /**
     * Executes the workflow activity.
     * This method is the main method that subclasses need to override.
     * The implementation in the default implementation does nothing.
     *
     * @throws ActivityException
     */
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        // compatibility dictates that the default implementation delegate to execute()
        execute();
        return runtimeContext.getCompletionCode();
    }

    /**
     * Return the activity instance ID
     * @return activity instance ID
     */
    protected Long getActivityInstanceId() {
        return activityInst.getId();
    }

    protected ActivityInstance getActivityInstance() {
        return activityInst;
    }

    /**
     * Return the owner type of the process instance.
     * The owner is typically an external event or another process
     * instance, but can be other things as well.
     * @return the owner type
     */
    protected String getProcessInstanceOwner() {
        return this.processInst.getOwner();
    }

    /**
     * Return the ID of the owner of the process instance.
     * The owner is typically an external event or another process
     * instance, but can be other things as well.
     * @return the process instance owner ID.
     */
    protected Long getProcessInstanceOwnerId() {
        return this.processInst.getOwnerId();
    }

    /**
     * Return the process instance ID
     * @return process instance ID
     */
    protected Long getProcessInstanceId() {
        return processInst.getId();
    }

    /**
     * This method is used internally by MDW.
     * @return the ID of the transition instance leading to the activity
     *      instance.
     */
    protected Long getWorkTransitionInstanceId() {
        return workTransitionInstanceId;
    }

    /**
     * Return the activity (definition) ID
     * @return activity ID
     */
    protected Long getActivityId() {
        return this.activityDef.getId();
    }

    /**
     * Return the name of the activity (definition).
     * @return name of the activity
     */
    protected String getActivityName() {
        return this.activityDef.getName();
    }

    /**
     * Return the process (definition) ID
     * @return process ID
     */
    protected Long getProcessId() {
        return this.processInst.getProcessId();
    }


    /**
     * Return the master request ID.
     * @return master request ID
     */
    protected String getMasterRequestId() {
        return this.processInst.getMasterRequestId();
    }

    /**
     * Return the return code, a.k.a completion code
     * @return the return code.
     */
    protected String getReturnCode() {
        return this.returnCode;
    }

    protected final String getEntryCode() {
        return entryCode;
    }

    /**
     * @param message
     */
    protected void setReturnMessage(String pMessage) {
        this.returnMessage = pMessage;
    }

    /**
     * @return the evaluation result code
     */
    protected String getReturnMessage() {
        return this.returnMessage;
    }

    /**
     * @param code the evaluation result code
     */
    protected void setReturnCode(String code) {
        this.returnCode = code;
    }

    protected void setProcessInstanceCompletionCode(String code)
            throws ActivityException {
        try {
            if (code!=null) {
                if (code.equals(TaskAction.CANCEL)) code = EventType.EVENTNAME_ABORT;
                else if (code.equals(TaskAction.ABORT))
                    code = EventType.EVENTNAME_ABORT + ":process";
                else if (code.equals(TaskAction.COMPLETE)) code = EventType.EVENTNAME_FINISH;
                else if (code.equals(TaskAction.RETRY )) code = EventType.EVENTNAME_START;
                else code = EventType.EVENTNAME_FINISH + ":" + code;
            }
            engine.setProcessInstanceCompletionCode(getProcessInstanceId(), code);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(0, ex.getMessage(),ex);
        }
    }

    protected Long getParentProcessId() throws ActivityException {
        try {
            Long parentProcInstId = this.getProcessInstanceOwnerId();
            ProcessInstance parentProcInst = this.getEngine().getProcessInstance(parentProcInstId);
            return parentProcInst.getProcessId();
        } catch (Exception ex) {
            throw new ActivityException(0, ex.getMessage(),ex);
        }
    }

    protected ProcessInstance getProcessInstance() {
        return this.processInst;
    }

    private Process procDef;
    protected Process getProcessDefinition() {
        if (procDef == null) {
            procDef = ProcessCache.getProcess(processInst.getProcessId());
            if (processInst.isEmbedded())
                procDef = procDef.getSubProcessVO(new Long(processInst.getComment()));
        }
        return procDef;
    }

    private Process mainProcDef;
    protected Process getMainProcessDefinition() throws ActivityException {
        if (mainProcDef == null) {
            Process procdef = ProcessCache.getProcess(processInst.getProcessId());
            mainProcDef = procdef;
            if (processInst.isEmbedded()) {
                Long parentOwnerID = getParentProcessId();
                mainProcDef = ProcessCache.getProcess(parentOwnerID);
            }
        }
        return mainProcDef;
    }

    /**
     * Return the value of the variable instance with the given
     * name. Despite the name of the method, the method works for
     * all variables, not just parameters of the process instances.
     * @param name the variable name
     * @return the variable value
     */
    protected Object getParameterValue(String name) {
        VariableInstance var = this.getVariableInstance(name);
        return var==null?null:var.getData();
    }

    /**
     * This method is used to get the value of a variable that does not
     * belong to the current process instance. The feature is mainly
     * for backward compatibility and should not be used for new code.
     * Warning: this method should *not* be used for getting variable values
     * for the current process instance, as it does not read cached variable instance.
     *
     * @param processInstId process instance ID where the variable belongs.
     * @param name variable name
     * @return variable data as an object
     * @throws ActivityException
     */
    protected Object getParameterValue(Long processInstId, String name)
        throws DataAccessException {
        VariableInstance varInst = engine.getVariableInstance(processInstId, name);
        return varInst==null?null:varInst.getData();

    }

    protected String getParameterStringValue(String name) {
        VariableInstance var = this.getVariableInstance(name);
        return var==null?null:var.getStringValue();
    }

    /**
     * Get the variable instance ID from its name.
     * Despite the name of the method, the method works for
     * all variables, not just parameters of the process instances.
     * @param name variable name
     * @return the variable instance ID
     */
    protected Long getParameterId(String name) {
        for (int i = 0; i < parameters.size(); i++) {
            if (name.equalsIgnoreCase(parameters.get(i).getName())) {
                return parameters.get(i).getInstanceId();
            }
        }
        return null;
    }

    /**
     * Get the variable instance ID from its name.
     * Despite the name of the method, the method works for
     * all variables, not just parameters of the process instances.
     * @param name variable name
     * @return the variable instance ID
     */
    protected String getParameterType(String name) throws ActivityException {
        for (VariableInstance varinst : parameters) {
            if (varinst.getName().equals(name)) return varinst.getType();
        }
        Process procdef = getMainProcessDefinition();
        List<Variable> vs = procdef.getVariables();
        String varName;
        for (int i=0; i<vs.size(); i++) {
            varName = vs.get(i).getName();
            if (varName.equals(name))
                return vs.get(i).getType();
        }
        return null;
    }

    /**
     * Returns all variable instances (a.k.a. parameters).
     * Note that the method does not return instances of variables
     * that have not been instantiated (assigned a value).
     *
     * @return an array of variable instances.
     */
    protected List<VariableInstance> getParameters() {
        return parameters;
    }

    protected void setParameterValues(Map<String,Object> map)
        throws ActivityException {
        for (String name : map.keySet()) {
            setParameterValue(name, map.get(name));
        }
    }

    /**
     * Set the value of the variable instance.
     * @param name variable name
     * @param value variable value; the value must not be null or
     *    empty string, which will cause database not-null constraint
     *    violation
     * @return variable instance ID
     * @throws ActivityException
     */
    protected Long setParameterValue(String name, Object value)
    throws ActivityException {
        Long varInstId;
        try {
            VariableInstance varInst = this.getVariableInstance(name);
            if (varInst != null) {
                varInstId = varInst.getInstanceId();
                varInst.setData(value);
                engine.updateVariableInstance(varInst);
            } else {
                varInst = engine.createVariableInstance(processInst, name, value);
                varInstId = varInst.getInstanceId();
                parameters.add(varInst);    // This adds to ProcessInstanceVO as well
            }
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(0, ex.getMessage(),ex);
        }
        return varInstId;
    }

    /**
     * This method is used to set the value of a variable that does not
     * belong to the current process instance. The feature is mainly
     * for backward compatibility and should not be used for new code.
     * Warning: this method should *not* be used for setting variable values
     * for the current process instance, as it does not update cached variable instance.
     *
     * @param processInstId process instance ID where the variable belongs.
     * @param name variable name
     * @param value variable value; the value must not be null or
     *    empty string, which will cause database not-null constraint
     *    violation
     * @return variable instance ID
     * @throws ActivityException
     */
    protected Long setParameterValue(Long processInstId, String name, Object value)
        throws ActivityException {
        Long varInstId;
        try {
            VariableInstance varInst = engine.getVariableInstance(processInstId, name);
            if (varInst != null) {
                varInstId = varInst.getInstanceId();
                if (value instanceof String) varInst.setStringValue((String)value);
                else varInst.setData(value);
                  engine.updateVariableInstance(varInst);
            } else {
                ProcessInstance procInst = engine.getProcessInstance(processInstId);
                varInst = engine.createVariableInstance(procInst, name, value);
                  varInstId = varInst.getInstanceId();
            }
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(0, ex.getMessage(),ex);
        }
        return varInstId;
    }

    /**
     * Set the value of the variable as a document reference to the given
     * document. If the variable is already bound to a document reference,
     * the method updates the content of the referred document.
     * The method will throw an exception if the document reference points to
     * a remote document.
     * @param name
     * @param varType
     * @param value this is the document
     * @throws ActivityException
     */
    protected DocumentReference setParameterValueAsDocument(String name, String varType, Object value)
    throws ActivityException {
        DocumentReference docref = (DocumentReference)this.getParameterValue(name);
        if (docref == null) {
            docref = createDocument(varType, value, OwnerType.VARIABLE_INSTANCE, new Long(0));
            Long varInstId = setParameterValue(name, docref);
            updateDocumentInfo(docref, null, OwnerType.VARIABLE_INSTANCE, varInstId);
        } else {
            updateDocumentContent(docref, value, varType);
        }
        return docref;
    }

    /**
     * Method that returns the external event instance data
     *
     * @param externalEventInstId
     * @return EventDetails as String
     */
    protected String getExternalEventInstanceDetails(Long externalEventInstId)
      throws ActivityException {
        DocumentReference docref = new DocumentReference(externalEventInstId);
        Document docvo;
        try {
            docvo = engine.getDocument(docref, false);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
        return docvo==null?null:docvo.getContent(getPackage());
    }

    /**
     * Get all attributes
     * @return all attributes of the activity
     */
    protected List<Attribute> getAttributes() {
        return attributes;
    }

    /**
     * Get the value of the attribute with the given name
     * @param name attribute name
     * @return the attribute value
     */
    protected String getAttributeValue(String name) {
        return Attribute.findAttribute(attributes, name);
    }


    protected String getVariableValueSmart(String name) throws PropertyException {
        return getValueSmart(name,name);
    }

    /**
     * Same as getAttributeValue() except performing translations in
     * the following cases:
     * <ul>
     *  <li>If the value starts with &ldquo;<code>prop:</code>&rdquo;, treat it as a property
     *   specification and return the property value.
     *   The property specification has the syntax
     *  &ldquo;<code>prop:<i>property-group</i>/<i>property-name</i></code>&rdquo;.</li>
     *  <li>If the value starts with a dollar sign followed by an identifier,
     *    it is treated as a variable name and the method returns the variable instance
     *    value as the result.</li>
     *  <li>If the value starts with <code>string:</code>,
     *    the rest of the value is returned without interpretation.
     *    This may be needed when the literal string contains special
     *    characters that may be misinterpreted as expression.</li>
     *  <li>If the value starts with <code>magic:</code>,
     *      the rest of the value is considered an expression
     *      in Magic Box language. It evaluates the expression and return the result.
     *      If it fails to evaluate, the method throws a PropertyException.</li>
     *  <li>If the value contains { and }, and the text in between is a variable
     *    (a dollar sign followed by variable name), then the value is
     *    obtained by replacing the place holders by corresponding variable values</li>
     *  <li>If the value looks like a Magic expression (if it contains dollar sign
     *      or colon), evaluate the expression and return the result.
     *      If it fails to evaluate, return the original value (instead
     *      of throwing an exception in
     *      the case when the value starts with <code>magic:</code>).</li>
     *  <li>If none of above applies, return the value as it is.</li>
     * </ul>
     *
     * @param name
     * @return attribute value (literal or translated).
     * @throws PropertyException if a translation rule is applied
     *      and translation fails.
     */

    protected String getAttributeValueSmart(String name) {
        return getValueSmart(Attribute.findAttribute(attributes, name), "A:"+name);
    }

    protected String getValueSmart(String value, String tag) {

        if (value==null) return null;
        if (value.startsWith("prop:")) {
            value = this.getProperty(value.substring(5));
        } else if (valueIsVariable(value)) {
            Object valueObj = this.getParameterValue(value.substring(1).trim());
            value = valueObj == null ? null : valueObj.toString();
        } else if (value.startsWith("string:")) {
            value = value.substring(7);
        } else if (value.startsWith("groovy:") || value.startsWith("g:") || value.startsWith("javascript:") || value.startsWith("js:")) {
            String name = GroovyNaming.getValidClassName(getActivityName() + "_" + getActivityId() + "_" + tag);
            Object obj = evaluateExpression(name, value.startsWith("j") ? JAVASCRIPT : GROOVY, value.substring(value.indexOf(':') + 1));
            value = obj == null ? null : obj.toString();
        } else if (valueIsPlaceHolder(value)) {
            value = this.translatePlaceHolder(value);
        } else if (valueIsJavaExpression(value)) {
            Object obj = evaluateExpression(tag, JAVA_EL, value);
            value = obj == null ? null : obj.toString();
        }
        return value == null ? null : value.trim();
    }

    /**
     * This method is the same as getAttributeValue except
     * it expects a variable name, and when the specification
     * is a '$' followed by variable name, it removes the '$'
     * @param attrname
     * @return
     */
    protected String getAttributeValueAsVariableName(String attrname) {
        String value = Attribute.findAttribute(attributes, attrname);
        if (value==null) return null;
        value = value.trim();
        if (value.startsWith("$")) return value.substring(1);
        else return value;
    }

    /**
     * General-purpose expression evaluation.
     * Language is one of MagicBox, Groovy or JavaScript.
     * Variables for this activity instance are bound.
     */
    protected Object evaluateExpression(String name, String language, String expression)
    throws ExecutionException {
        try {
            if (JAVA_EL.equals(language)) {
                return _runtimeContext.evaluate(expression);
            }
            ScriptEvaluator evaluator = getScriptEvaluator(name, language);
            Process processVO = getMainProcessDefinition();
            List<Variable> varVOs = processVO.getVariables();
            Map<String,Object> bindings = new HashMap<String,Object>();
            for (Variable varVO: varVOs) {
                Object value = getParameterValue(varVO.getName());
                if (value instanceof DocumentReference) {
                    DocumentReference docref = (DocumentReference) value;
                    value = getDocument(docref, varVO.getType());
                }
                bindings.put(varVO.getName(), value);
            }
            bindings.put(Variable.MASTER_REQUEST_ID, getMasterRequestId());
            return evaluator.evaluate(expression, bindings);
        }
        catch (ActivityException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * General-purpose expression evaluation.
     * Language is one of Groovy or JavaScript.
     * Variables for this activity instance are bound.
     * Overridden this method to keep backward compatibility
     */
    protected Object evaluateExpression(String name, String language, String expression,
            Map<String,Object> addlBinding) throws ExecutionException {

        try {
            ScriptEvaluator evaluator = getScriptEvaluator(name, language);
            Process processVO = getMainProcessDefinition();
            List<Variable> varVOs = processVO.getVariables();
            for (Variable varVO: varVOs) {
                Object value = getParameterValue(varVO.getName());
                if (value instanceof DocumentReference) {
                    DocumentReference docref = (DocumentReference) value;
                    value = getDocument(docref, varVO.getType());
                }
                addlBinding.put(varVO.getName(), value);
            }
            addlBinding.put(Variable.MASTER_REQUEST_ID, getMasterRequestId());
            return evaluator.evaluate(expression, addlBinding);
        }
        catch (PropertyException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
        catch (ActivityException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
    }

    protected ScriptEvaluator getScriptEvaluator(String name, String language) throws ExecutionException {
        if (language == null)
            throw new NullPointerException("Missing script evaluator language");

        ScriptEvaluator evalImpl = null;
        String propName = PropertyNames.MDW_SCRIPT_EXECUTORS + "." + language.toLowerCase();
        String evalImplClassName = getProperty(propName);
        if (evalImplClassName == null)
            evalImplClassName = getProperty("mdw.script.executor." + language); // compatibility
        if (evalImplClassName == null) {
            if ("Groovy".equals(language))
                evalImpl = new GroovyExecutor();  // don't require property for default language
            else
                throw new PropertyException("No script executor property value found: " + propName);
        }
        else {
            try {
                Class<? extends ScriptEvaluator> evalClass = getPackage().getCloudClassLoader()
                        .loadClass(evalImplClassName).asSubclass(ScriptEvaluator.class);
                evalImpl = evalClass.newInstance();
            }
            catch (ReflectiveOperationException ex) {
                throw new ExecutionException("Cannot instantiate " + evalImplClassName + " (needs an optional asset package?)", ex);
            }
        }
        evalImpl.setName(name);
        return evalImpl;
    }

    /**
     * The method checks if a string is of the form of a variable reference,
     * i.e. a dollar sign followed by an identifier.
     * This is typically used to check if an attribute specifies a variable.
     * @param v
     * @return true if the argument is a dollar sign followed by an identifier
     */
    protected boolean valueIsVariable(String v) {
        if (v==null || v.length()<2) return false;
        if (v.charAt(0)!='$') return false;
        for (int i=1; i<v.length(); i++) {
            char ch = v.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch!='_') return false;
        }
        return true;
    }

    /**
     * Checks if the string contains #{ or ${ with a corresponding closing }
     * to determine if it is a Java expression
     */
    protected boolean valueIsJavaExpression(String v) {
        if (v == null)
            return false;
        int start = v.indexOf("#{") >= 0 ? v.indexOf("#{") : v.indexOf("${");
        return ((start != -1) && (start < v.indexOf('}')));
    }

    private boolean valueIsPlaceHolder(String v) {
        int n = v.length();
        for (int i=0; i<n; i++) {
            char ch = v.charAt(i);
            if (ch=='{') {
                int k = i+1;
                while (k<n) {
                    ch = v.charAt(k);
                    if (ch=='}') break;
                    k++;
                }
                if (k<n) {
                    if (v.charAt(i+1)=='$') {
                        String varname = v.substring(i+2,k).trim();
                        boolean isIdentifier = true;
                        for (int j=0; isIdentifier && j<varname.length(); j++) {
                            ch = varname.charAt(j);
                            if (!Character.isLetterOrDigit(ch) && ch!='_') isIdentifier = false;
                        }
                        if (isIdentifier) return true;
                    }
                } // else  '{' without '}' - ignore string after '{'
                i = k;
            }
        }
        return false;
    }

    /**
     * The method translates place holders for attributes such as
     * event names.
     * TODO: combine this with getAttributeValueSmart?
     * @param eventName
     * @return
     */
    protected String translatePlaceHolder(String eventName) {
        // honor java EL expressions
        if (this.valueIsJavaExpression(eventName)) {
            try {
                Object o = evaluateExpression(getActivityId().toString(), JAVA_EL, eventName);
                return o == null ? "" : o.toString();
            }
            catch (ExecutionException ex) {
                logger.severeException(ex.getMessage(), ex);
                return "";
            }
        }
        int k, i, n;
        StringBuffer sb = new StringBuffer();
        n = eventName.length();
        for (i=0; i<n; i++) {
            char ch = eventName.charAt(i);
            if (ch=='{') {
                k = i+1;
                while (k<n) {
                    ch = eventName.charAt(k);
                    if (ch=='}') break;
                    k++;
                }
                if (k<n) {
                    String expression = eventName.substring(i+1,k);
                    String value;
                    if (this.valueIsVariable(expression)) {
                        String varname = expression.substring(1);
                        if (varname.equalsIgnoreCase(Variable.PROCESS_INSTANCE_ID)) value = this.getProcessInstanceId().toString();
                        else if (varname.equalsIgnoreCase(Variable.MASTER_REQUEST_ID)) value = this.getMasterRequestId();
                        else if (varname.equalsIgnoreCase(Variable.ACTIVITY_INSTANCE_ID)) value = this.getActivityInstanceId().toString();
                        else {
                            Object binding = this.getParameterValue(varname);
                            if (binding!=null) value = binding.toString();
                            else value = "";
                        }
                    } else {
                         try {
                             value = (String)evaluateExpression(getActivityId().toString()+":"+expression, GROOVY, expression);
                         } catch (ExecutionException ex) {
                             logwarn("Exception in evaluating expression " + expression + ": " + ex.getMessage());
                             value = "";
                         }
                    }
                    sb.append(value);
                } // else  '{' without '}' - ignore string after '{'
                i = k;
            } else if (ch == '\\' ) {
                ++i;
                sb.append(eventName.charAt(i));

            } else sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Given a document reference object (typically bound to a
     * document variable), returns the actual document object.
     * @param docref
     * @return
     * @throws ActivityException
     */
    protected Object getDocument(DocumentReference docref, String type) throws ActivityException {
        Document docvo;
        try {
            docvo = engine.getDocument(docref, false);
            // deserialize here to support package aware translator providers
            docvo.setObject(VariableTranslator.realToObject(getPackage(), type, docvo.getContent(getPackage())));
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException("Failed to get document", ex);
        }
        return docvo == null ? null : docvo.getObject(type, getPackage());
    }

    protected String getDocumentContent(DocumentReference docref) throws ActivityException {
         Document docvo;
         try {
             docvo = engine.getDocument(docref, false);
         } catch (Exception ex) {
             logger.severeException(ex.getMessage(), ex);
             throw new ActivityException("Failed to get document", ex);
         }
         return docvo==null?null:docvo.getContent(getPackage());
    }

    /**
     * Same as getDocument() but also puts a write lock on it.
     * The lock will be released at the end of the transaction.
     *
     * @param docref
     * @return
     * @throws ActivityException
     */
    protected Object getDocumentForUpdate(DocumentReference docref, String type)
            throws ActivityException {
        Document docvo;
        try {
            docvo = engine.getDocument(docref, true);
            // deserialize here to support package aware translator providers
            docvo.setObject(VariableTranslator.realToObject(getPackage(), type, docvo.getContent(getPackage())));
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException("Failed to lock document for update", ex);
        }
        return docvo == null ? null : docvo.getObject(type, getPackage());
    }

    protected DocumentReference createDocument(String docType, Object document, String ownerType,
            Long ownerId) throws ActivityException {
        return createDocument(docType, document, ownerType, ownerId, null, null);
    }

    /**
     * Create a new document, persisted in database, and return a document
     * reference object, to be bound to a variable.
     *
     * @param docType type of the document (i.e. variable type)
     * @param document document object itself
     * @param ownerType owner type. More than likely this will be OwnerType.VARIABLE_INSTANCE
     *      other possible types are LISTENER_REQUEST, LISTENER_RESPONSE,
     *      ADAPTOR_REQUEST, ADAPTOR_RESPONSE
     * @param ownerId ID of the owner, dependent on owner type.
     * @param statusCode
     * @param statusMessage
     * @return document reference
     */
    protected DocumentReference createDocument(String docType, Object document, String ownerType,
            Long ownerId, Integer statusCode, String statusMessage) throws ActivityException {
        DocumentReference docref;
        try {
            if (!(document instanceof String)) {
                // do the serialization here to take advantage of version aware translator providers
                document = VariableTranslator.realToString(getPackage(), docType, document);
            }

            docref = engine.createDocument(docType, ownerType, ownerId, statusCode, statusMessage, document);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
        return docref;
    }

    /**
     * Update the content (actual document object) bound to the given
     * document reference object.
     * @param docref
     * @param document
     * @throws ActivityException
     */
    protected void updateDocumentContent(DocumentReference docref, Object document, String type)
        throws ActivityException {
        try {
            if (!(document instanceof String)) {
                // serialize here to support package aware translator providers
                document = VariableTranslator.realToString(getPackage(), type, document);
            }
            engine.updateDocumentContent(docref, document, type, getPackage());
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    protected void updateDocumentInfo(DocumentReference docref, String documentType,
            String ownerType, Long ownerId) throws ActivityException {
        updateDocumentInfo(docref, documentType, ownerType, ownerId, null, null);
    }

    /**
     * Update document information (everything but document content itself).
     * The method will update only the arguments that have non-null values.
     */
    protected void updateDocumentInfo(DocumentReference docref, String documentType,
            String ownerType, Long ownerId, Integer statusCode, String statusMessage) throws ActivityException {
        try {
            engine.updateDocumentInfo(docref, documentType, ownerType, ownerId, statusCode, statusMessage);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }


    /**
     * Open a database connection.
     * @param database_name it must be null if you are accessing
     *          MDW database (the default database where MDW schemas reside).
     *          For other databases, you need to pass in either a data source name
     *         or JDBC URL.
     * @return
     * @throws SQLException
     */
    protected DatabaseAccess openDatabaseAccess(String database_name) throws SQLException {
        DatabaseAccess db;
        if (engine!=null && database_name==null) {
            db = engine.getDatabaseAccess();
        } else {
            db = new DatabaseAccess(database_name);
            db.openConnection();
        }
        return db;
    }

    protected void closeDatabaseAccess(DatabaseAccess db) {
        if (db!=null && (!db.isDefaultDatabase() || engine==null)) db.closeConnection();
    }

    protected Package getPackage() {
        return pkg;
    }

    /**
     * Get MDW system property value (not java system property value,
     * which can be retrieved using System.getProperty), using configured
     * property manager.
     *
     * If package-specific properties are defined, then those will be used.
     *
     * MDW allows to supply a custom property manager in place of the out-of-box
     * property manager, using java system property "property_manager", which
     * takes the class name of the custom property manager as its value.
     *
     * @param propertyName
     * @return value of the property, or null if the property does not exist.
     */
    protected String getProperty(String propertyName) {
       return getPackage().getProperty(propertyName);
    }

    protected Asset getAsset(String name, String language, int version) {
        return AssetCache.getAsset(name, language, version);
    }

    /**
     * @return completion code if onExecute() returns non-null
     */
    String notifyMonitors(String event) {
        ActivityRuntimeContext runtimeContext = null;

        for (ActivityMonitor monitor : MonitorRegistry.getInstance().getActivityMonitors()) {
            try {
                Map<String, Object> updates = null;

                //TODO Implement a way to determine if the monitor applies, before we even update the runtimeContext to avoid
                // needless processing, which includes variable serialization

                // Since there is no guaranteed order to the multiple monitors at this point, there cannot be an expectation to keep
                // the runtimeContext process variables map up-to-date from one monitor to the next.  Only update map once
                // TODO Implement a way to determine priority/order when having multiple monitors
                if (runtimeContext == null) {
                    // LOGMSG_START happens right after creating the activityRuntimeContext, so assume variables are up-to-date
                    if (event.equals(WorkStatus.LOGMSG_START))
                        runtimeContext = _runtimeContext;
                    else
                        runtimeContext = getRuntimeContext();
                }

                if (monitor instanceof OfflineMonitor) {
                    @SuppressWarnings("unchecked")
                    OfflineMonitor<ActivityRuntimeContext> activityOfflineMonitor = (OfflineMonitor<ActivityRuntimeContext>) monitor;
                    new OfflineMonitorTrigger<ActivityRuntimeContext>(activityOfflineMonitor, runtimeContext).fire(event);
                }
                else {
                    if (event.equals(WorkStatus.LOGMSG_START))
                        updates = monitor.onStart(runtimeContext);
                    else if (event.equals(WorkStatus.LOGMSG_EXECUTE)) {
                        String compCode = monitor.onExecute(runtimeContext);
                        if (compCode != null) {
                            loginfo("Activity short-circuited by monitor: " + monitor.getClass().getName() + " with code: " + compCode);
                            return compCode;
                        }
                    }
                    else if (event.equals(WorkStatus.LOGMSG_COMPLETE)) {
                        updates = monitor.onFinish(runtimeContext);
                    }
                    else if (event.equals(WorkStatus.LOGMSG_FAILED))
                        monitor.onError(runtimeContext);
                }

                if (updates != null) {
                    for (String varName : updates.keySet()) {
                        loginfo("Variable: " + varName + " updated by ActivityMonitor: " + monitor.getClass().getName());
                        setVariableValue(varName, updates.get(varName));
                        // TODO Once ordering of monitors is implemented, update runtimeContext's variables map here
                    }
                }
            }
            catch (Exception ex) {
                logexception(ex.getMessage(), ex);
            }
        }
        return null;
    }

    protected String logtag() {
        StringBuffer sb = new StringBuffer();
        sb.append("p");
        sb.append(this.getProcessId());
        sb.append(".");
        sb.append(this.getProcessInstanceId());
        sb.append(" a");
        sb.append(this.getActivityId());
        sb.append(".");
        sb.append(this.getActivityInstanceId());
        return sb.toString();
    }

    public void loginfo(String message) {
        logger.info(logtag(), message);
    }

    public void logdebug(String message) {
        logger.debug(logtag(), message);
    }

    public void logwarn(String message) {
        logger.warn(logtag(), message);
    }

    public void logsevere(String message) {
        logger.severe(logtag(), message);
    }

    public void logexception(String msg, Exception e) {
        logger.exception(logtag(), msg, e);
    }

    public boolean isLogInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isLogDebugEnabled() {
        return logger.isDebugEnabled();
    }

    protected Integer lockActivityInstance() throws ActivityException {
        try {
            return engine.lockActivityInstance(this.getActivityInstanceId());
        } catch (Exception e) {
            throw new ActivityException(-1, "Failed to lock activity instance", e);
        }
    }

    protected Integer lockProcessInstance() throws ActivityException {
        try {
            return engine.lockProcessInstance(this.getProcessInstanceId());
        } catch (Exception e) {
            throw new ActivityException(-1, "Failed to lock process instance", e);
        }
    }

    TrackingTimer getTimer() {
        return timer;
    }

    protected ProcessExecutor getEngine() {
        return engine;
    }

    /**
     * Executes a script, passing additional bindings to be made available to the script.
     * Script should return a value for the result code if the default (null is not desired).
     *
     * @param script - the script content
     * @param language - built-in support for Groovy, and JavaScript (default is Groovy)
     */
    protected Object executeScript(String script, String language, Map<String,Object> addlBindings, String qualifier)
            throws ActivityException {

        String temp = getAttributeValue(OUTPUTDOCS);
        outputDocuments = temp == null ? new String[0] : StringHelper.parseList(temp).toArray(new String[0]);
        Object retObj = null;
        try {
            if (Compatibility.hasCodeSubstitutions())
                script = doCompatibilityCodeSubstitutions(script);

            Process processVO = getMainProcessDefinition();
            List<Variable> varVOs = processVO.getVariables();
            Map<String,Object> bindings = new HashMap<String,Object>();
            for (Variable varVO: varVOs) {
                bindings.put(varVO.getName(), getVariableValue(varVO.getName()));
            }
            bindings.put("runtimeContext", _runtimeContext);
            bindings.put(Variable.MASTER_REQUEST_ID, getMasterRequestId());
            if (addlBindings != null) {
                bindings.putAll(addlBindings);
            }

            ScriptExecutor executor = getScriptExecutor(language, qualifier);
            retObj = executor.execute(script, bindings);

            for (Variable variableVO: varVOs) {
                String variableName = variableVO.getName();
                Object bindValue = bindings.get(variableName);
                String varType = variableVO.getType();
                Object value = bindValue;
                if (varType.equals("java.lang.String") && value != null)
                    value = value.toString();  // convert to string
                setVariableValue(variableName, varType, value);
            }
        }
        catch (Exception ex) {
          throw new ActivityException(-1, ex.getMessage(), ex);
        }
        return retObj;
    }

    protected ScriptExecutor getScriptExecutor(String language, String qualifier) throws PropertyException {
        if (language == null)
            throw new NullPointerException("Missing script executor language");

        ScriptExecutor exeImpl = null;
        String propName = PropertyNames.MDW_SCRIPT_EXECUTORS + "." + language.toLowerCase();
        String exeImplClassName = getProperty(propName);
        if (exeImplClassName == null)
            exeImplClassName = getProperty("mdw.script.executor." + language.toLowerCase()); // compatibility
        if (exeImplClassName == null) {
            if ("Groovy".equals(language))
                exeImpl = new GroovyExecutor();  // don't require property for default language
            else
                throw new PropertyException("No script executor property value found: " + propName);
        }
        else {
            try {
                Class<? extends ScriptExecutor> exeClass = getPackage().getCloudClassLoader()
                        .loadClass(exeImplClassName).asSubclass(ScriptExecutor.class);
                exeImpl = exeClass.newInstance();
            }
            catch (ReflectiveOperationException ex) {
                throw new ExecutionException("Cannot instantiate " + exeImplClassName + " (needs an optional asset package?)", ex);
            }
        }
        exeImpl.setName(getScriptExecClassName(qualifier));
        return exeImpl;
    }

    protected String getScriptExecClassName(String qualifier) {
        String name = getProcessDefinition().getLabel() + "_" + getActivityName() + "_" + getActivityId();
        if (qualifier != null)
            name += "_" + qualifier;
        return GroovyNaming.getValidClassName(name);
    }

    /**
     * Convenience method that returns a variable value, dereferencing doc types.
     * Note: to update a document value it must be included in getOutputDocuments().
     * @param varName
     * @return the variable value
     */
    protected Object getVariableValue(String varName) throws ActivityException {
        VariableInstance var = getVariableInstance(varName);
        if (var == null) return null;
        Object value = var.getData();
        if (var.isDocument()) {
            DocumentReference docref = (DocumentReference)value;
            if (isOutputDocument(varName))
                value = getDocumentForUpdate(docref, var.getType());
            else
                value = getDocument(docref, var.getType());
        }
        return value;
    }

    /**
     * Convenience method to set variable value regardless of type.
     */
    protected void setVariableValue(String varName, Object value) throws ActivityException {
        Variable varVO = getProcessDefinition().getVariable(varName);
        if (varVO == null)
            throw new ActivityException("No such variable defined for process: " + varName);
        String varType = varVO.getType();
        if (VariableTranslator.isDocumentReferenceVariable(getPackage(), varType))
            setParameterValueAsDocument(varName, varType, value);
        else
            setParameterValue(varName, value);
    }


    /**
     * Convenience method that sets a variable value, including document content.
     * Meant to be used by Script, Dynamic Java and other activities where it is
     * not known whether the value of a document has changed after execution.
     * Note: to update a document value through this method it must be included in getOutputDocuments().
     * @param varName
     * @param varType
     * @param value new value to set
     */
    protected void setVariableValue(String varName, String varType, Object value) throws ActivityException {
        if (value == null)
            return;
        if (VariableTranslator.isDocumentReferenceVariable(pkg, varType)) {
            try {
                boolean isOutputDoc = isOutputDocument(varName);
                boolean doUpdate = isOutputDoc;

                // don't check in production (or for cache-only perf level since old values are not retained)
                if (!ApplicationContext.isProduction() && (getPerformanceLevel() < 5)) {
                    boolean changed = hasDocumentValueChanged(varName, value);

                    if (changed){
                        if (!isOutputDoc) {
                            String msg = "Attempt to change value of non-output document '" + varName + "'";
                            if (Object.class.getName().equals(varType))
                              msg += ".  Please make sure and implement an equals() comparator in your Object.";
                            // Removed ActivityException, now just log it
                            logger.debug(msg);
                        }
                    }
                    else {
                        doUpdate = false;
                    }
                }

                if (doUpdate) {
                    setParameterValueAsDocument(varName, varType, value);
                }
            }
            catch (DataAccessException ex) {
                throw new ActivityException(ex.getMessage(), ex);
            }
        }
        else {
            this.setParameterValue(varName, value);
        }
    }

    public int getPerformanceLevel() {
        return getEngine().getPerformanceLevel();
    }

    public String[] getOutputDocuments() { return outputDocuments; }
    public void setOutputDocuments(String[] outputDocs) { this.outputDocuments = outputDocs; }

    protected boolean isOutputDocument(String variableName) {
        if (outputDocuments == null)
            return false;
        for (String outputDoc : outputDocuments) {
            if (outputDoc.equals(variableName))
                return true;
        }
        return false;
    }

    private boolean hasDocumentValueChanged(String varName, Object newValue) throws DataAccessException {
        DocumentReference docRef = (DocumentReference) getParameterValue(varName);

        if (docRef == null)
            return newValue != null;

        Document docVO = getEngine().loadDocument(docRef, false);

        if (docVO == null)
            return newValue != null;

        if (newValue == null)
            return true;  // we already know old value is not null

        String oldString = docVO.getContent(getPackage());
        Object oldObject = VariableTranslator.realToObject(getPackage(), docVO.getDocumentType(), oldString);

        if (docVO.getDocumentType().equals(Object.class.getName()))
            return !oldObject.equals(newValue);

        // general comparison involves reserializing since round-trip results are not guaranteed
        oldString = VariableTranslator.realToString(getPackage(), docVO.getDocumentType(), oldObject);
        String newString = VariableTranslator.realToString(getPackage(), docVO.getDocumentType(), newValue);
        return !oldString.equals(newString);
    }

    /**
     * The method records an event flag. If the event flag is already recorded,
     * The method simply returns true; o/w it returns false.
     * The method is intended to be used to coordinate an action that should be executed
     * only once by multiple potentially independent threads/processes/clustered servers.
     *
     * @param eventName a unique event name that should also be different from non-flag events.
     * @param preserveInterval for clean-up routines - how long this entry should be preserved after creation
     * @return true when the event is already recorded, false otherwise
     * @throws ActivityException
     */
    protected boolean recordEventFlag(String eventName, int preserveInterval)
        throws ActivityException {
        try {
            return true; // engine.getDataAccess().recordEventFlag(eventName, preserveInterval);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(0, ex.getMessage(),ex);
        }
    }

    public TransactionWrapper startTransaction()
    throws ActivityException {
        try {
            return engine.startTransaction();
        } catch (DataAccessException e) {
            throw new ActivityException(0, e.getMessage(), e);
        }
    }

    public void stopTransaction(TransactionWrapper transaction)
    throws ActivityException {
        try {
            engine.stopTransaction(transaction);
        } catch (DataAccessException e) {
            throw new ActivityException(0, e.getMessage(), e);
        }
    }

    protected String doCompatibilityCodeSubstitutions(String in) throws IOException {
        SubstitutionResult substitutionResult = Compatibility.getInstance().performCodeSubstitutions(in);
        if (!substitutionResult.isEmpty()) {
            logwarn("Compatibility substitutions applied for code in activity " + getActivityName()
                + " (details logged at debug level). Please update the code for this activity as otherwise these substitutions are applied on every execution.");
            if (isLogDebugEnabled())
                logdebug("Compatibility substitutions for " + getActivityName() + ":\n" + substitutionResult.getDetails());
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Substitution output for " + getActivityName() + ":\n" + substitutionResult.getOutput());
            return substitutionResult.getOutput();
        }
        return in;
    }

    protected boolean isDisabled() throws ActivityException {
        try {
            return "true".equalsIgnoreCase(getAttributeValueSmart(DISABLED));
        }
        catch (PropertyException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Get a runtime value.
     * @param spec can be a variable name or expression
     */
    protected Object getValue(String spec) throws ActivityException {
        if (ProcessRuntimeContext.isExpression(spec)) {
            return getRuntimeContext().evaluate(spec);
        }
        else {
            return getVariableValue(spec);
        }
    }

    /**
     * Set a runtime value.
     * @param spec can be a variable name or expression
     * @param value the value to set
     */
    protected void setValue(String spec, Object value) throws ActivityException {
        if (ProcessRuntimeContext.isExpression(spec)) {
            // create or update document variable referenced by expression
            ActivityRuntimeContext runtimeContext = getRuntimeContext();
            runtimeContext.set(spec, value);
            String rootVar = spec.substring(2, spec.indexOf('.'));
            Variable doc = runtimeContext.getProcess().getVariable(rootVar);
            String stringValue = VariableTranslator.realToString(runtimeContext.getPackage(), doc.getType(), runtimeContext.evaluate("#{" + rootVar + "}"));
            setParameterValueAsDocument(rootVar, doc.getType(), stringValue);
        }
        else {
            setVariableValue(spec, value);
        }
    }
}
