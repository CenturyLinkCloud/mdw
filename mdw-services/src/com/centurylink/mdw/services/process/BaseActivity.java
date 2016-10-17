/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.process;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.Compatibility.SubstitutionResult;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.exception.ServiceLocatorException;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.TransactionWrapper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.TrackingTimer;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.groovy.GroovyNaming;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.data.task.TaskAction;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.BamMessageDefinition;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceInfo;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.monitor.ActivityMonitor;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.monitor.OfflineMonitor;
import com.centurylink.mdw.script.ExecutionException;
import com.centurylink.mdw.script.GroovyExecutor;
import com.centurylink.mdw.script.ScriptEvaluator;
import com.centurylink.mdw.script.ScriptExecutor;
import com.centurylink.mdw.services.OfflineMonitorTrigger;
import com.centurylink.mdw.services.dao.process.EngineDataAccess;
import com.centurylink.mdw.services.dao.process.EngineDataAccessCache;
import com.centurylink.mdw.services.dao.process.cache.ProcessVOCache;
import com.centurylink.mdw.services.event.StubHelper;
import com.centurylink.mdw.services.mbeng.MbengMDWRuntime;
import com.centurylink.mdw.services.messenger.IntraMDWMessenger;
import com.centurylink.mdw.services.messenger.MessengerFactory;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengRuntime;
import com.qwest.mbeng.MbengVariable;

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
    public static final String MAGIC_BOX = "MagicBox";
    public static final String JAVA_EL = "javax.el";
    public static final String OUTPUTDOCS = "Output Documents";

    private static String BAM_URL = null;

    private Long workTransitionInstanceId;
    private String returnCode;
    private String returnMessage;
    private String entryCode;
    private List<VariableInstanceInfo> parameters;
    private List<AttributeVO> attributes;
    private ProcessExecuter engine;
    private TrackingTimer timer;
    private String[] outputDocuments;
    private ActivityVO activityDef;
    private ProcessInstanceVO processInst;
    private ActivityInstanceVO activityInst;
	private ActivityRuntimeContext _runtimeContext;
    private PackageVO pkg;

    /**
     * Repopulates variable values in case they've changed during execution.
     */
    public ActivityRuntimeContext getRuntimeContext() throws ActivityException {
        for (VariableInstanceInfo var : getParameters()) {
            _runtimeContext.getVariables().put(var.getName(), getVariableValue(var.getName()));
        }
        return _runtimeContext;
    }

    /**
     * This version is used by the new engine (ProcessExecuter).
     * @param parameters variable instances of the process instance,
     *    or of the parent process instance when this is in an embedded process
     */
    void prepare(ActivityVO actVO, ProcessInstanceVO pi, ActivityInstanceVO ai,
    		List<VariableInstanceInfo> parameters,
            Long transInstId, String entryCode, TrackingTimer timer, ProcessExecuter engine) {
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
                pkg = PackageVOCache.getProcessPackage(getMainProcessDefinition().getId());
                _runtimeContext = new ActivityRuntimeContext(pkg, getProcessDefinition(), processInst, activityDef, activityInst);
                for (VariableInstanceInfo var : getParameters())
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
        engine = new ProcessExecuter(edao, null, false);

        this.procDef = this.mainProcDef = runtimeContext.getProcess();
        this.processInst = runtimeContext.getProcessInstance();
        this.pkg = runtimeContext.getPackage();
        this._runtimeContext = runtimeContext;
        this.activityDef = runtimeContext.getActivity();
        this.activityInst = runtimeContext.getActivityInstance();

        if (runtimeContext.getAttributes() != null) {
            attributes = new ArrayList<AttributeVO>();
            for (String attrName : runtimeContext.getAttributes().keySet())
                attributes.add(new AttributeVO(attrName, runtimeContext.getAttribute(attrName)));
        }

        if (runtimeContext.getVariables() != null) {
            parameters = new ArrayList<VariableInstanceInfo>();
            for (String varName : runtimeContext.getVariables().keySet())
                parameters.add(new VariableInstanceInfo(varName, runtimeContext.getVariables().get(varName)));
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
    protected VariableInstanceInfo getVariableInstance(String pName) {
        for (int i = 0; i < parameters.size(); i++) {
            if (pName.equalsIgnoreCase(parameters.get(i).getName())) {
                return parameters.get(i);
            }
        }
        return null;
    }

    void execute(ProcessExecuter engine) throws ActivityException {
        this.engine = engine;
        initialize(_runtimeContext);
        Object ret = execute(_runtimeContext);
        if (ret != null)
            setReturnCode(String.valueOf(ret));
    }

    void executeTimed(ProcessExecuter engine) throws ActivityException {
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

    protected ActivityInstanceVO getActivityInstance() {
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
        return this.activityDef.getActivityId();
    }

    /**
     * Return the name of the activity (definition).
     * @return name of the activity
     */
    protected String getActivityName() {
        return this.activityDef.getActivityName();
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
    		ProcessInstanceVO parentProcInst = this.getEngine().getProcessInstance(parentProcInstId);
    		return parentProcInst.getProcessId();
        } catch (Exception ex) {
            throw new ActivityException(0, ex.getMessage(),ex);
        }
    }

    protected ProcessInstanceVO getProcessInstance() {
    	return this.processInst;
    }

    private ProcessVO procDef;
    protected ProcessVO getProcessDefinition() {
        if (procDef == null) {
        	procDef = ProcessVOCache.getProcessVO(processInst.getProcessId());
        	if (processInst.isNewEmbedded())
        		procDef = procDef.getSubProcessVO(new Long(processInst.getComment()));
        }
    	return procDef;
    }

    private ProcessVO mainProcDef;
    protected ProcessVO getMainProcessDefinition() throws ActivityException {
        if (mainProcDef == null) {
        	ProcessVO procdef = ProcessVOCache.getProcessVO(processInst.getProcessId());
        	mainProcDef = procdef;
        	if (processInst.isNewEmbedded() || procdef.isEmbeddedProcess()) {
                Long parentOwnerID = getParentProcessId();
                mainProcDef = ProcessVOCache.getProcessVO(parentOwnerID);
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
    	VariableInstanceInfo var = this.getVariableInstance(name);
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
    	VariableInstanceInfo varInst = engine.getVariableInstance(processInstId, name);
    	return varInst==null?null:varInst.getData();

    }

    protected String getParameterStringValue(String name) {
    	VariableInstanceInfo var = this.getVariableInstance(name);
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
    	for (VariableInstanceInfo varinst : parameters) {
    		if (varinst.getName().equals(name)) return varinst.getType();
    	}
    	ProcessVO procdef = getMainProcessDefinition();
        List<VariableVO> vs = procdef.getVariables();
        String varName;
        for (int i=0; i<vs.size(); i++) {
            varName = vs.get(i).getVariableName();
            if (varName.equals(name))
                return vs.get(i).getVariableType();
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
    protected List<VariableInstanceInfo> getParameters() {
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
            VariableInstanceInfo varInst = this.getVariableInstance(name);
            if (varInst != null) {
            	varInstId = varInst.getInstanceId();
            	varInst.setData(value);
            	engine.updateVariableInstance(varInst);
            } else {
            	varInst = engine.createVariableInstance(processInst, name, value);
            	varInstId = varInst.getInstanceId();
            	parameters.add(varInst);	// This adds to ProcessInstanceVO as well
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
    		VariableInstanceInfo varInst = engine.getVariableInstance(processInstId, name);
    		if (varInst != null) {
    			varInstId = varInst.getInstanceId();
    			if (value instanceof String) varInst.setStringValue((String)value);
    			else varInst.setData(value);
              	engine.updateVariableInstance(varInst);
    		} else {
                ProcessInstanceVO procInst = engine.getProcessInstance(processInstId);
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
        if (docref==null) {
            docref = createDocument(varType, value, OwnerType.VARIABLE_INSTANCE, new Long(0), null, null);
            Long varInstId = this.setParameterValue(name, docref);
            this.updateDocumentInfo(docref, null, null, null, varInstId, null, null);
        } else {
            if (docref.getServer()==null) updateDocumentContent(docref, value, varType);
            else throw new ActivityException("Cannot update remote document reference");
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
        DocumentReference docref = new DocumentReference(externalEventInstId, null);
        DocumentVO docvo;
        try {
        	docvo = engine.getDocument(docref, false);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
        return docvo==null?null:docvo.getContent();
    }

    /**
     * Get all attributes
     * @return all attributes of the activity
     */
    protected List<AttributeVO> getAttributes() {
        return attributes;
    }

    /**
     * Get the value of the attribute with the given name
     * @param name attribute name
     * @return the attribute value
     */
    protected String getAttributeValue(String name) {
        return AttributeVO.findAttribute(attributes, name);
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

    protected String getAttributeValueSmart(String name) throws PropertyException {
        return getValueSmart(AttributeVO.findAttribute(attributes, name), "A:"+name);
    }

    @Deprecated
    protected String geValueSmart(String value, String tag) throws PropertyException {
        return getValueSmart(value, tag);
    }

    protected String getValueSmart(String value, String tag) throws PropertyException {

        if (value==null) return null;
        if (value.startsWith("prop:")) {
        	value = this.getProperty(value.substring(5));
        } else if (valueIsVariable(value)) {
            Object valueObj = this.getParameterValue(value.substring(1).trim());
            value = valueObj == null ? null : valueObj.toString();
        } else if (value.startsWith("string:")) {
            value = value.substring(7);
        } else if (value.startsWith("magic:")) {
            try {
                Object obj = evaluateExpression(getActivityId().toString()+":"+tag, MAGIC_BOX, value.substring(6));
                value = obj == null ? null : obj.toString();
            } catch (ExecutionException ex) {
                logger.severeException(ex.getMessage(), ex);
                throw new PropertyException(-1, ex.getMessage(), ex);
            }
        } else if (value.startsWith("groovy:") || value.startsWith("g:") || value.startsWith("javascript:") || value.startsWith("js:")) {
            String name = GroovyNaming.getValidClassName(getActivityName() + "_" + getActivityId() + "_" + tag);
            try {
                Object obj = evaluateExpression(name, value.startsWith("j") ? JAVASCRIPT : GROOVY, value.substring(value.indexOf(':') + 1));
                value = obj == null ? null : obj.toString();
            }
            catch (ExecutionException ex) {
                throw new PropertyException(-1, ex.getMessage(), ex);
            }
        } else if (valueIsPlaceHolder(value)) {
            value = this.translatePlaceHolder(value);
        } else if (valueIsJavaExpression(value)) {
            try {
                Object obj = evaluateExpression(tag, JAVA_EL, value);
                value = obj == null ? null : obj.toString();
            }
            catch (ExecutionException ex) {
                throw new PropertyException(-1, ex.getMessage(), ex);
            }
        } else if (valueIsMagicBoxExpression(value)) {
            try {
                Object obj = evaluateExpression(getActivityId().toString()+":"+tag, MAGIC_BOX, value);
                value = obj == null ? null : obj.toString();
            } catch (ExecutionException ex) {
            	logwarn("getValueSmart fails to evaluate expression - return expression as value");
                // fall through instead of throwing errors, to handle things
                // such as "http://..."
            }
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
        String value = AttributeVO.findAttribute(attributes, attrname);
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
            if (evaluator instanceof MagicRulesEvaluator) {
                return evaluator.evaluate(expression, null);
            } else {
                ProcessVO processVO = getMainProcessDefinition();
                List<VariableVO> varVOs = processVO.getVariables();
            	Map<String,Object> bindings = new HashMap<String,Object>();
                for (VariableVO varVO: varVOs) {
                    Object value = getParameterValue(varVO.getVariableName());
                    if (value instanceof DocumentReference) {
                        DocumentReference docref = (DocumentReference) value;
                        value = getDocument(docref, varVO.getVariableType());
                    }
                    bindings.put(varVO.getVariableName(), value);
                }
                bindings.put(VariableVO.MASTER_REQUEST_ID, getMasterRequestId());
                return evaluator.evaluate(expression, bindings);
            }
        }
        catch (PropertyException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
        catch (ActivityException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * General-purpose expression evaluation.
     * Language is one of MagicBox, Groovy or JavaScript.
     * Variables for this activity instance are bound.
     * Overridden this method to keep backward compatibility
     */
    protected Object evaluateExpression(String name, String language, String expression,
    		Map<String,Object> addlBinding) throws ExecutionException {

        try {
            ScriptEvaluator evaluator = getScriptEvaluator(name, language);
            if (evaluator instanceof MagicRulesEvaluator) {
                return evaluator.evaluate(expression, null);
            } else {
                ProcessVO processVO = getMainProcessDefinition();
                List<VariableVO> varVOs = processVO.getVariables();
                for (VariableVO varVO: varVOs) {
                    Object value = getParameterValue(varVO.getVariableName());
                    if (value instanceof DocumentReference) {
                        DocumentReference docref = (DocumentReference) value;
                        value = getDocument(docref, varVO.getVariableType());
                    }
                    addlBinding.put(varVO.getVariableName(), value);
                }
                addlBinding.put(VariableVO.MASTER_REQUEST_ID, getMasterRequestId());
                return evaluator.evaluate(expression, addlBinding);
            }
        }
        catch (PropertyException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
        catch (ActivityException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
    }

    protected ScriptEvaluator getScriptEvaluator(String name, String language) throws PropertyException {
        if (language == null)
            throw new NullPointerException("Missing script evaluator language");

        ScriptEvaluator evalImpl = null;
        if ("Magic".equals(language) || "MagicBox".equals(language)) {
            evalImpl = new MagicRulesEvaluator(); // compatibility
        }
        else {
            String propName = PropertyNames.MDW_SCRIPT_EXECUTOR + "." + language.toLowerCase();
            String evalImplClassName = getProperty(propName);
            if (evalImplClassName == null)
                evalImplClassName = getProperty("MDWFramework.ScriptExecutors/" + language); // compatibility
            if (evalImplClassName == null) {
                if ("Groovy".equals(language))
                    evalImpl = new GroovyExecutor();  // don't require property for default language
                else
                    throw new PropertyException("No script executor property value found: " + propName);
            }
            else
                evalImpl = (ScriptEvaluator) ApplicationContext.getClassInstance(evalImplClassName);

        }
        evalImpl.setName(name);
        return evalImpl;
    }

    /**
     * evaluate a condition in MagicBox rule language
     * @param name
     * @param cond
     * @param vs
     * @return
     * @throws MbengException
     */
    protected boolean evaluateCondition(String name, String cond,
            List<VariableVO> vs) throws MbengException {
        MbengMDWRuntime runtime = new MbengMDWRuntime(name, cond,
        		MbengRuleSet.RULESET_COND, vs, engine, logger) {
        	protected Object getParameterValue(MbengVariable var) {
        		return BaseActivity.this.getParameterValue(var.getName());
        	}
        	protected String getParameterString(MbengVariable var) {
        		return BaseActivity.this.getParameterStringValue(var.getName());
        	}
        	protected void setParameterString(String varname, String v) throws ActivityException {
        		BaseActivity.this.setParameterValue(varname, v);
        	}
        	protected void setParameterDocument(String varname, String vartype, Object v) throws ActivityException {
        		BaseActivity.this.setParameterValueAsDocument(varname, vartype, v);
        	}
			protected String getPseudoVariableValue(String varname) {
				if (varname.equals(VariableVO.MASTER_REQUEST_ID)) return getMasterRequestId();
				else return null;
			}
        };
//        runtime.bind(VariableVO.MASTER_REQUEST_ID, this.getMasterRequestId());
        return runtime.verify();
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
     * @deprecated - Use valueIsMagicBoxExpression or valueIsJavaExpression.
     */
    @Deprecated
    protected boolean valueIsExpression(String v) {
      return valueIsMagicBoxExpression(v);
    }

    /**
     * The method checks if the string is an MagicBox expression, versus a static value.
     * The method uses a simplistic check, i.e. when the string contains a dollar
     * sign and is not a Java expression, so it may not be a valid expression for any language
     * when the method returns true.
     *
     * The method is primarily used to determine if an attribute has
     * an expression as its value that is not a Java expression.
     *
     * @param v
     * @return
     */

    protected boolean valueIsMagicBoxExpression(String v) {
        if (v == null)
            return false;
        return (v.indexOf('$') >= 0 && !valueIsJavaExpression(v));
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
                        if (varname.equalsIgnoreCase(VariableVO.PROCESS_INSTANCE_ID)) value = this.getProcessInstanceId().toString();
                        else if (varname.equalsIgnoreCase(VariableVO.MASTER_REQUEST_ID)) value = this.getMasterRequestId();
                        else if (varname.equalsIgnoreCase(VariableVO.ACTIVITY_INSTANCE_ID)) value = this.getActivityInstanceId().toString();
                        else {
                            Object binding = this.getParameterValue(varname);
                            if (binding!=null) value = binding.toString();
                            else value = "";
                        }
                    } else {
                         try {
                        	 value = (String)evaluateExpression(getActivityId().toString()+":"+expression, MAGIC_BOX, expression);
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
        DocumentVO docvo;
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
    	 DocumentVO docvo;
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
        DocumentVO docvo;
        try {
        	docvo = engine.getDocument(docref, true);
        	// deserialize here to support package aware translator providers
        	docvo.setObject(VariableTranslator.realToObject(getPackage(), type, docvo.getContent()));
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException("Failed to lock document for update", ex);
        }
        return docvo == null ? null : docvo.getObject(type, getPackage());
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
     * @param searchKey1 optional search key
     * @param searchKey2 optional additional search key
     * @return document reference object
     * @throws ActivityException
     */
    protected DocumentReference createDocument(String docType, Object document, String ownerType, Long ownerId,
                    String searchKey1, String searchKey2)
            throws ActivityException {
        DocumentReference docref;
        try {
            if (!(document instanceof String)) {
                // do the serialization here to take advantage of version aware translator providers
                document = VariableTranslator.realToString(getPackage(), docType, document);
            }

        	docref = engine.createDocument(docType, this.getProcessInstanceId(),
        			ownerType, ownerId, searchKey1, searchKey2, document);
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
        	engine.updateDocumentContent(docref, document, type);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Update document information (everything but document content itself).
     * The method will update only the arguments that have non-null values.
     * @param docref
     * @param processInstId
     * @param documentType
     * @param ownerType
     * @param ownerId
     * @param searchKey1
     * @param searchKey2
     * @throws ActivityException
     */
    protected void updateDocumentInfo(DocumentReference docref, Long processInstId, String documentType, String ownerType, Long ownerId,
            String searchKey1, String searchKey2) throws ActivityException {
        try {
        	engine.updateDocumentInfo(docref, processInstId, documentType, ownerType, ownerId, searchKey1, searchKey2);
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Find documents matching the specified criteria.
     * @param procInstId null if to be excluded from criteria
     * @param type the runtime type of the document variable
     * @param searchKey1 null if to be excluded
     * @param searchKey2 null if to be excluded
     * @param ownerType null if to be excluded
     * @param ownerId null if to be excluded
     * @param createDateStart null if to be excluded
     * @param createDateEnd null if to be excluded
     * @param orderBy null if none
     * @return the IDs of matching documents
     */
    protected List<DocumentVO> findDocuments(Long procInstId, String type, String searchKey1, String searchKey2,
            String ownerType, Long ownerId, Date createDateStart, Date createDateEnd, String orderBy) throws ActivityException {
        try {
            return engine.findDocuments(procInstId, type, searchKey1,
            		searchKey2, ownerType, ownerId, createDateStart, createDateEnd, orderBy);
        }
        catch (Exception ex) {
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

    protected PackageVO getPackage() {
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

    protected RuleSetVO getRuleSet(String name, String language, int version) {
        return RuleSetCache.getRuleSet(name, language, version);
    }

    /**
     * Find the latest ruleSet whose attributeValues match the specified set of criteria.
     * @param name
     * @param language
     * @param attributeValues
     * @return list of matching ruleSets
     */
    public RuleSetVO getLatestRuleSet(String name, String language, Map<String,String> attributeValues) {
        return RuleSetCache.getLatestRuleSet(name, language, attributeValues);
    }

    void sendMessageToBam(String bamAttributeName) {
        String msgdef = AttributeVO.findAttribute(attributes, bamAttributeName);
        if (msgdef==null || msgdef.length()==0) return;
        if ((new StubHelper()).isStubbing()) return;
    	if (BAM_URL==null) {
    		String bamurl = getProperty(PropertyNames.MDW_BAM_URL);
    		if (bamurl==null) bamurl = "";
    		else if (!bamurl.startsWith("t3:")) {
    			int k = bamurl.indexOf("://");
    			if (k>0) bamurl = "t3" + bamurl.substring(k);
    		}
    		BAM_URL = bamurl;
    	}
    	String msg = null;
    	try {
        	msgdef = translatePlaceHolder(msgdef);
			BamMessageDefinition bammsg = new BamMessageDefinition(msgdef);
			msg = bammsg.getMessageInstance(getMasterRequestId());
			if (BAM_URL.length()==0) throw new Exception("mdw.bam.url is not specified");
			if ("log".equals(BAM_URL)) {
			    loginfo("BAM Message:\n" + msg);
			}
			else {
    			IntraMDWMessenger msgbroker = MessengerFactory.newIntraMDWMessenger(BAM_URL);
    			msgbroker.sendMessage(msg);
			}
		} catch (Exception e) {
			StandardLogger bamlogger = LoggerUtil.getStandardLogger("com.centurylink.mdw.common.AnyClass");
			bamlogger.warn("Failed to send BAM message - " + e.getMessage());
			if (msg!=null) bamlogger.warn("MESSAGE: " + msg);
		}
    }

    /**
     * @return completion code if onExecute() returns non-null
     */
    String notifyMonitors(String event) {
        for (ActivityMonitor monitor : MonitorRegistry.getInstance().getActivityMonitors()) {
            try {
                Map<String, Object> updates = null;

                if (monitor instanceof OfflineMonitor) {
                    @SuppressWarnings("unchecked")
                    OfflineMonitor<ActivityRuntimeContext> activityOfflineMonitor = (OfflineMonitor<ActivityRuntimeContext>) monitor;
                    new OfflineMonitorTrigger<ActivityRuntimeContext>(activityOfflineMonitor, getRuntimeContext()).fire(event);
                }
                else {
                    if (event.equals(WorkStatus.LOGMSG_START))
                        updates = monitor.onStart(getRuntimeContext());
                    else if (event.equals(WorkStatus.LOGMSG_EXECUTE)) {
                        String compCode = monitor.onExecute(getRuntimeContext());
                        if (compCode != null) {
                            loginfo("Activity short-circuited by monitor: " + monitor.getClass().getName() + " with code: " + compCode);
                            return compCode;
                        }
                    }
                    else if (event.equals(WorkStatus.LOGMSG_COMPLETE)) {
                        updates = monitor.onFinish(getRuntimeContext());
                    }
                    else if (event.equals(WorkStatus.LOGMSG_FAILED))
                        monitor.onError(getRuntimeContext());
                }

                if (updates != null) {
                    for (String varName : updates.keySet()) {
                        loginfo("Variable: " + varName + " updated by ActivityMonitor: " + monitor.getClass().getName());
                        setVariableValue(varName, updates.get(varName));
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

    /**
     * Returns the TaskManager EJB Ref
     * @return TaskManager
     * @throws ServiceLocatorException
     */
//    protected TaskManager getTaskManager() throws ServiceLocatorException {
//        try {
//            ServiceLocator locator = ServiceLocator.getInstance();
//            return (TaskManager) locator.getEJB(ServiceConstants.TASK_MANAGER);
//        }
//        catch (ServiceLocatorException ex) {
//            logger.severeException(ex.getMessage(), ex);
//            throw new ServiceLocatorException(-1, ex.getMessage(), ex);
//        }
//    }

    protected ProcessExecuter getEngine() {
    	return engine;
    }

    /**
     * For backward compatibility, this implementation honors the RETURN_CODE attribute
     * for specifying a variable name that will contain the return code, and also the
     * vReturnCode binding value for explicitly assigning the return code value.
     *
     * Both of these are deprecated in favor of simply returning a value or calling
     * setReturnCode() on the implicit activity variable.
     *
     * @param script - the script content
     * @param language - built-in support for MagicBox, Groovy, and JavaScript (default is MagicBox)
     */
    protected Object executeScript(String script, String language) throws ActivityException {
    	return executeScript(script, language, null);
    }

    /**
     * Executes a script, passing additional bindings to be made available to the script.
     * @see executeScript(String, String)
     */
    protected Object executeScript(String script, String language, Map<String,Object> addlBindings) throws ActivityException {

        String temp = getAttributeValue(OUTPUTDOCS);
        outputDocuments = temp == null ? new String[0] : temp.split("#");
        Object retObj = null;
        try {
            if (Compatibility.hasCodeSubstitutions())
                script = doCompatibilityCodeSubstitutions(script);

            ProcessVO processVO = getMainProcessDefinition();
            List<VariableVO> varVOs = processVO.getVariables();
            Map<String,Object> bindings = new HashMap<String,Object>();
            for (VariableVO varVO: varVOs) {
                bindings.put(varVO.getVariableName(), getVariableValue(varVO.getVariableName()));
            }
            bindings.put("runtimeContext", _runtimeContext);
            bindings.put(VariableVO.MASTER_REQUEST_ID, getMasterRequestId());
            if (addlBindings != null) {
                bindings.putAll(addlBindings);
            }

            ScriptExecutor executor = getScriptExecutor(language);
            retObj = executor.execute(script, bindings);

            for (VariableVO variableVO: varVOs) {
                String variableName = variableVO.getVariableName();
                Object bindValue = bindings.get(variableName);
                String varType = variableVO.getVariableType();
                Object value = bindValue;
                if (varType.equals("java.lang.String") && value != null)
                    value = value.toString();  // convert to string
                setVariableValue(variableName, varType, value);
            }

            //RETURN_CODE and vReturnCode is deprecated functionality and the following code
            //exists only for backward compatibility purpose.
            String returnCodeAttrValue = getAttributeValue("RETURN_CODE");
            if (returnCodeAttrValue != null && bindings.containsKey(returnCodeAttrValue)) {
                Object object = bindings.get(returnCodeAttrValue);
                if (object != null)
                    return object;
            }
            else if (bindings.containsKey("vReturnCode")) {
                Object object = bindings.get("vReturnCode");
               if (object != null) {
                   return object;
               }
            }
        }
        catch (Exception ex) {
//          logger.severeException(ex.getMessage(), ex);
          throw new ActivityException(-1, ex.getMessage(), ex);
        }
        return retObj;
    }

    protected ScriptExecutor getScriptExecutor(String language) throws PropertyException {
        if (language == null)
            throw new NullPointerException("Missing script executor language");

        ScriptExecutor exeImpl = null;
        if ("Magic".equals(language) || "MagicBox".equals(language)) {
            exeImpl = new MagicRulesExecutor(); // compatibility
        }
        else {
            String propName = PropertyNames.MDW_SCRIPT_EXECUTOR + "." + language.toLowerCase();
            String exeImplClassName = getProperty(propName);
            if (exeImplClassName == null)
                exeImplClassName = getProperty("MDWFramework.ScriptExecutors/" + language); // compatibility
            if (exeImplClassName == null) {
                if ("Groovy".equals(language))
                    exeImpl = new GroovyExecutor();  // don't require property for default language
                else
                    throw new PropertyException("No script executor property value found: " + propName);
            }
            else
                exeImpl = (ScriptExecutor) ApplicationContext.getClassInstance(exeImplClassName);
        }
        String name = GroovyNaming.getValidClassName(getProcessDefinition().getLabel() + "_" + getActivityName() + "_" + getActivityId());
        exeImpl.setName(name);
        return exeImpl;
    }

    /**
     * Convenience method that returns a variable value, dereferencing doc types.
     * Note: to update a document value it must be included in getOutputDocuments().
     * @param varName
     * @return the variable value
     */
    protected Object getVariableValue(String varName) throws ActivityException {
        VariableInstanceInfo var = getVariableInstance(varName);
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
        VariableVO varVO = getProcessDefinition().getVariable(varName);
        if (varVO == null)
            throw new ActivityException("No such variable defined for process: " + varName);
        String varType = varVO.getVariableType();
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
                    boolean changed;
                    if (value instanceof MbengDocument)
                        changed = ((MbengDocument)value).isDirty();
                    else
                        changed = hasDocumentValueChanged(varName, value);

                    if (changed){
                        if (!isOutputDoc) {
                            String msg = "Attempt to change value of non-output document '" + varName + "'";
                            if (Object.class.getName().equals(varType))
                              msg += ".  Please make sure and implement an equals() comparator in your Object.";
                            throw new ActivityException(msg);
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

        DocumentVO docVO = getEngine().loadDocument(docRef, false);

        if (docVO == null)
            return newValue != null;

        if (newValue == null)
            return true;  // we already know old value is not null

        String oldString = docVO.getContent();
        Object oldObject = VariableTranslator.realToObject(getPackage(), docVO.getDocumentType(), oldString);

        if (docVO.getDocumentType().equals(Object.class.getName()))
            return !oldObject.equals(newValue);

        // general comparison involves reserializing since round-trip results are not guaranteed
        oldString = VariableTranslator.realToString(getPackage(), docVO.getDocumentType(), oldObject);
        String newString = VariableTranslator.realToString(getPackage(), docVO.getDocumentType(), newValue);
        return !oldString.equals(newString);
    }

    /**
     * evaluate an expression in MagicBox rule language or XPath
     * TODO cache compiled version
     */
    class MagicRulesEvaluator implements ScriptEvaluator {

        private String name;
        public String getName() { return name; }
        /**
         * @param name a name for the expression. Needs to be unique
         *      among all expressions for caching purpose.
         *      suggest to use activity ID unless there are more
         *      than one expressions used by an activity.
         */
        public void setName(String name) { this.name = name; }

        public Object evaluate(String expression, Map<String,Object> bindings) throws ExecutionException {
            try {
                ProcessVO procVO = BaseActivity.this.getMainProcessDefinition();
                MbengMDWRuntime runtime = new MbengMDWRuntime(name, expression,
                        MbengRuleSet.RULESET_EXPR, procVO.getVariables(), engine, logger) {
                	protected Object getParameterValue(MbengVariable var) {
                		return BaseActivity.this.getParameterValue(var.getName());
                	}
                	protected String getParameterString(MbengVariable var) {
                		return BaseActivity.this.getParameterStringValue(var.getName());
                	}
                	protected void setParameterString(String varname, String v) throws ActivityException {
                		BaseActivity.this.setParameterValue(varname, v);
                	}
                	protected void setParameterDocument(String varname, String vartype, Object v) throws ActivityException {
                		BaseActivity.this.setParameterValueAsDocument(varname, vartype, v);
                	}
					protected String getPseudoVariableValue(String varname) {
						if (varname.equals(VariableVO.MASTER_REQUEST_ID)) return getMasterRequestId();
						else return null;
					}
                };
                return runtime.evaluate();
            }
            catch (MbengException ex) {
                throw new ExecutionException("Error evaluating Magic Rule: " + ex.getMessage(), ex);
            } catch (ActivityException e) {
                throw new ExecutionException("Error evaluating Magic Rule - failed to get parent process ID", e);
			}
        }
    }

    /**
     * This is implemented as an inner class since it has dependencies on
     * mdw-services and activityId.
     */
    class MagicRulesExecutor implements ScriptExecutor {

        private String name;
        public String getName() { return name; }
        public void setName(String name)  { this.name = name; }

        public Object execute(String script, Map<String,Object> bindings) throws ExecutionException {

            try {
                ProcessVO processVO = getMainProcessDefinition();
            	MbengMDWRuntime runtime = new MbengMDWRuntime(getProcessId().toString()+"."+getActivityId().toString(),
                    script, MbengRuleSet.RULESET_RULE, processVO.getVariables(), getEngine(), logger) {
                	protected Object getParameterValue(MbengVariable var) {
                		return BaseActivity.this.getParameterValue(var.getName());
                	}
                	protected String getParameterString(MbengVariable var) {
                		return BaseActivity.this.getParameterStringValue(var.getName());
                	}
                	protected void setParameterString(String varname, String v) throws ActivityException {
                		BaseActivity.this.setParameterValue(varname, v);
                	}
                	protected void setParameterDocument(String varname, String vartype, Object v) throws ActivityException {
                		BaseActivity.this.setParameterValueAsDocument(varname, vartype, v);
                	}
					protected String getPseudoVariableValue(String varname) {
						if (varname.equals(VariableVO.MASTER_REQUEST_ID)) return getMasterRequestId();
						else return null;
					}
                };
                int retcode = runtime.run();
                if (retcode != MbengRuntime.EXECSTATUS_OK) {
                	if (retcode == MbengRuntime.EXECSTATUS_EXCEPTION)
                		throw new ExecutionException("RULE EXEC ERROR: " + runtime.getErrorMsg(), runtime.getException());
                	else throw new ExecutionException("RULE EXEC ERROR: " + runtime.getErrorMsg());
                }
                runtime.saveDocuments();
                return runtime.getErrorMsg();
            }
            catch (MbengException ex) {
                throw new ExecutionException("Error executing Magic Rule: " + ex.getMessage(), ex);
            } catch (ActivityException e) {
                throw new ExecutionException("Error executing Magic Rule - failed to get parent process ID", e);
			}
        }
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
}
