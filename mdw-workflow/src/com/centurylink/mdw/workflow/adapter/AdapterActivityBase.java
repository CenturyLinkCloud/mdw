/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.adapter.AdapterInvocationError;
import com.centurylink.mdw.adapter.HeaderAwareAdapter;
import com.centurylink.mdw.adapter.SimulationResponse;
import com.centurylink.mdw.common.constant.ActivityResultCodeConstant;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.ProcessVisibilityConstant;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.logger.StandardLogger.LogLevel;
import com.centurylink.mdw.common.utilities.timer.Tracked;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.monitor.AdapterMonitor;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.services.event.StubHelper;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * New implementation of Web Service Adapter which can be
 * configured through Designer and does not implement
 * ControlledAdapterActivity interface.
 *
 */
@Tracked(LogLevel.TRACE)
public abstract class AdapterActivityBase extends DefaultActivityImpl implements AdapterActivity, AdapterInvocationError
{
    protected static final String DO_LOGGING = "DO_LOGGING";
    protected static final String REQUEST_VARIABLE = "REQUEST_VARIABLE";
    protected static final String RESPONSE_VARIABLE = "RESPONSE_VARIABLE";
    protected static final String REQUEST_XSD = "REQUEST_XSD";
    protected static final String RESPONSE_XSD = "RESPONSE_XSD";

    protected static final int APPLICATION_ERROR = 41371;  // try to be unique

    private static Random random = null;

	/**
     * Subclasses do not have to but may override this method.
     * If you do override this method, you have to include
     * call to this super method.
     * The method (directly or indirectly)
     * invokes other abstract or overridable methods to
     * perform the execution of the activity.
     *
     * The primary method to implement are openConnection(),
     * invoke() and closeConnection()
     *
     * History note:
	 * 1. "Connector" was used only to verify legality of Adapter,
	 *    so no longer needed
	 * 2. "Adapter" was a separate interface. We combine it with
	 *    this class. For that, every adapter like this must implement
	 *    - stubAdapter
	 *    - invokeAdapter
	 *    - isSynchronous
	 *    - saveResponse (for asynchronous adapter used with synchronous protocol)
	 * 3. Removed pooling for Connector and Adapter
	 *
	 */
    @Override
    public void execute() throws ActivityException {
        Object requestData = this.getRequestData();
        Object responseData = null;
        Object connection = null;
    	StubHelper stubber = new StubHelper();
        boolean stubMode = stubber.isStubbing() || isStubMode();
        boolean logging = doLogging();
        try {
            if (logging && requestData != null)
                logMessage(externalRequestToString(requestData), false);
            if (stubMode) {
                loginfo("Adapter is running in StubMode");
                if (stubber.isStubbing()) {
            		responseData = stubber.getStubResponse(getMasterRequestId(), requestData.toString());
            		if (MAKE_ACTUAL_CALL.equals(responseData)) {
                    	loginfo("Stub server instructs to get real response");
            			connection = this.openConnection();
            			responseData = doInvoke(connection, requestData);
            		}
            		else {
            		    loginfo("Response received from stub server");
            		}
            	}
                else {
                    responseData = this.getStubResponse(requestData);
                }
            }
            else {
                connection = this.openConnection();
                responseData = doInvoke(connection, requestData);
            }
            if (logging && responseData != null)
                logMessage(externalResponseToString(responseData), true);
            handleAdapterSuccess(responseData);
            executePostScript(responseData);
        } catch (Exception ex) {
            this.handleAdapterInvocationError(ex);
        } finally {
            if (connection != null) closeConnection(connection);
        }
    }

    /**
     * Determines if external requests/responses should
     * be logged into adapter instance table.
     * The default method reads the attribute value
     * DO_LOGGING. If the attribute is missing, the default
     * is on for logging.
     * You may need to override externalRequestToString
     * and externalResponseToString in order to log the
     * information properly.
     *
     * @return whether logging is on or off
     */
    protected boolean doLogging() {
        String do_logging = this.getAttributeValue(DO_LOGGING);
        return do_logging==null||do_logging.equalsIgnoreCase("true");
    }

    /**
     * The method is invoked when the external system interaction is a success.
     * (i.e. the external system responded something, even an error code).
     * The method may convert external-system-detected errors into failure
     * by throwing an Adapter exception here. Throwing non-retryable
     * exception will lead to handleAdapterFailure to be called.
     *
     * This method is also the place to translate external responses
     * into internal format and persist them somewhere, if these are
     * needed.
     *
     * For one way communication protocols, you can leave the default
     * which does nothing.
     *
     * The default method does nothing if the response is null,
     * or set the response to the variable specified in RESPONSE_VARIABLE attribute.
     * @param pResponse, from the external system
     * @throws ActivityException thrown when the handler itself has system problems
     * @throws AdapterException thrown when the external system responded but
     *      the response needs to be handled as an error (for retry or fail the activity)
     * @throws TranslationException if the response cannot be translated correctly. This
     *      is a RuntimeException
     */
    protected void handleAdapterSuccess(Object pResponse)
        throws ActivityException,ConnectionException,AdapterException {
        if (pResponse==null) return;
        String varname = this.getAttributeValue(RESPONSE_VARIABLE);
        if (varname==null) return;
        String vartype = this.getParameterType(varname);
        if (VariableTranslator.isDocumentReferenceVariable(vartype) && !(pResponse instanceof DocumentReference)) {
            if (pResponse instanceof String) {
                Object doc = VariableTranslator.realToObject(vartype, (String)pResponse);
                setParameterValueAsDocument(varname, vartype, doc);
            }
            else {
                setParameterValueAsDocument(varname, vartype, pResponse);
            }
        } else setParameterValue(varname, pResponse);
    }

    /**
     * The method is invoked when the requests failed to send
     * or the response is not received from external systems
     * (for protocols where responses are expected).
     * The default method throws an ActivityException
     * which will lead to ERROR transitions.
     *
     * @param pErrorCause the exception captured
     * @param pErrorCode if the exception contains non-zero error code,
     *      it is passed in here. Otherwise it is set to
     *      either AdapterActivityBase.APPLICATION_ERROR
     *      or AdapterActivityBase.SYSTEM_ERROR
     * @throws ActivityException
     */
    protected void handleAdapterFailure(int pErrorCode, Throwable pErrorCause)
            throws ActivityException {
          throw new ActivityException(-1, pErrorCause.getMessage(), pErrorCause);
    }

    /**
     * Override this method if need to translate data from variable
     * or need to get data elsewhere. The default
     * method assumes the data is in the variable REQUEST_VARIABLE
     *
     * Also, if a translation is needed to convert data in internal
     * format to one required by external system, this is the place
     * for doing it.
     *
     * @return
     * @throws ActivityException when the attribute cannot be
     *      fetched for any reason
     * @throws TranslationException if the request cannot be translated correctly. This
     *      is a RuntimeException
     */
    protected Object getRequestData() throws ActivityException {
        String varname = getAttributeValue(REQUEST_VARIABLE);
        Object request = varname == null ? null : getParameterValue(varname);
        if (!StringHelper.isEmpty(getPreScript())) {
            Object returnVal = executePreScript(request);
            if (returnVal == null) {
                // nothing returned; requestVar may have been assigned by script
                request = getParameterValue(varname);
            }
            else {
                request = returnVal;
            }
        }
        return request;
    }


    /**
     * This method should not normally be overriden. When you override execute() method,
     * you will need to invoke this for any exception caught.
     * @param errorCause
     * @throws ActivityException
     */
    protected void handleAdapterInvocationError(Throwable errorCause)
        throws ActivityException {
        logger.severeException(getAdapterInvocationErrorMessage(), errorCause);
        boolean isErrorRetryable;
        int errorCode;
        if (errorCause instanceof AdapterException) {
            AdapterException adEx = (AdapterException) errorCause;
            isErrorRetryable = adEx.isRetryableError();
            if (adEx.getErrorCode() != 0) {
                errorCode = adEx.getErrorCode();
            } else errorCode = APPLICATION_ERROR;
        } else if (errorCause instanceof ConnectionException) {
            errorCode = ((ConnectionException)errorCause).getCode();
            // if process is invoked sync style, no need to retry due to connection failure
            ProcessVO procVO = getProcessDefinition();
            if (procVO.getProcessType().equals(ProcessVisibilityConstant.SERVICE)) {
            	super.setReturnCode("ERROR:");
            	return;
            }
            isErrorRetryable = true;
        } else {
            errorCode = APPLICATION_ERROR;
            isErrorRetryable = false;
        }
        if (isErrorRetryable) {
            super.setReturnCode(ActivityResultCodeConstant.RESULT_RETRY);
        }

        for (AdapterMonitor monitor : MonitorRegistry.getInstance().getAdapterMonitors()) {
            String errResult = (String)monitor.onError(getRuntimeContext(), errorCause);
            if (errResult != null) {
                this.setReturnCode(errResult);
                return;
            }
        }

        this.handleAdapterFailure(errorCode, errorCause);
    }

    /**
     * <p>
     * This method allows the concrete adapters to define whatever they need for
     * a decent error message. For instance they can include endpoint urls etc
     * </p>
     * @return a default error of "Adapter invocation exception"
     */
    @Override
    public String getAdapterInvocationErrorMessage() {
         return "Adapter invocation exception";
    }

    protected Long logMessage(String message, boolean isResponse) {
        if (message == null || message.isEmpty())
            return null;
        try {
            DocumentReference docref = createDocument(String.class.getName(), message,
            		isResponse?OwnerType.ADAPTOR_RESPONSE:OwnerType.ADAPTOR_REQUEST,
                    this.getActivityInstanceId(), null, null);
            return docref.getDocumentId();
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * This method is used to serialize external system requests for
     * logging purpose.
     *
     * @param requestData data for going to external system
     * @return serialized form of the request
     */
    protected String externalRequestToString(Object requestData) {
        return requestData.toString();
    }

    /** This method is used to serialize external system responses for
     * logging purpose.
     *
     * @param responseData data directly from the external system
     * @return serialized form of the response
     */
    protected String externalResponseToString(Object responseData) {
        if (responseData==null) return null;
        return responseData.toString();
    }

    /**
     * This is used when the adapter is asynchronous but
     * the protocol is synchronous, where response data is
     * logged in adapter instance table. This method is invoked
     * to translate serialized data back to the actual data.
     *
     * This method is also used to translate stubbed data
     * from strings to objects.
     *
     * @param stringData
     * @return origial external system response
     */
    protected Object externalResponseFromString(String stringData) {
        return stringData;
    }

    /**
     * Return stubbed response from external system.
     *
     * @param requestData
     * @return
     */
    protected Object getStubResponse(Object requestData) {
        List<SimulationResponse> responses = new ArrayList<SimulationResponse>();
        for (AttributeVO attr : this.getAttributes()) {
            if (attr.getAttributeName().startsWith(WorkAttributeConstant.SIMULATION_RESPONSE)) {
                SimulationResponse r = new SimulationResponse(attr.getAttributeValue());
                responses.add(r);
            }
        }
        String unfilteredResponse = null;
        if (responses.size()==0) unfilteredResponse = null;
        else if (responses.size()==1) {
            unfilteredResponse = responses.get(0).getResponse();
        } else {    // randomly pick a response based on chances
            int total_chances = 0;
            for (SimulationResponse r : responses) {
                total_chances += r.getChance().intValue();
            }
            if (random==null) random = new Random();
            int ran = random.nextInt(total_chances);
            int k = 0;
            for (SimulationResponse r : responses) {
                if (ran>=k && ran<k+r.getChance().intValue()) {
                    unfilteredResponse = r.getResponse();
                    break;
                }
                k += r.getChance().intValue();
            }
        }
        return filter(unfilteredResponse, requestData);
    }

    /**
     * TODO: implement this!
     * @param unfiltered
     * @param request
     * @return
     */
    private String filter(String unfiltered, Object request) {
        return unfiltered;
    }

    /**
     * Determine if the adapter itself is synchronous, i.e.
     * waiting for responses.
     *
     * @return true if it needs to wait for responses
     */
    public abstract boolean isSynchronous();


    /**
     * This method must be implemented to perform the
     * real invocation of external system service.
     *
     * Note it is preferred to do data translation
     * in getRequest() and handleAdapterSuccess() rather than
     * in this method, as logging is done outside this
     * method and typically we prefer the logged messages
     * to be as close to raw messages as possible.
     *
     * Of course some minimal translation may be needed, for
     * instance in the case the request is not a single
     * object. In case of RMI call, the request may
     * be an array of arguments.
     *
     * If the adapter is synchronous but the underlying protocol
     * is asynchronous, the method needs to wait for responses
     * and return them. If the adapter is asynchronous but
     * the protocol is synchronous (e.g. RMI, Web Service),
     * then the execute() method logs the response in
     * adapter instance table for later use.
     *
     * @param pConnection
     * @param request
     * @return response if the adapter is synchronous,
     *    or the underlying protocol is synchronous. Return null
     *    if the adapter is asynchronous and the underlying
     *    protocol is asynchronous
     * @throws AdapterException Adapter exception may or
     *      may not be retriable, which you can set in the
     *      exception itself. Default is not retriable.
     * @throws ConnectionException certain protocol may also
     *      throw ConnectionException here. ConnectionException
     *      can be retried.
     */
    protected abstract Object invoke(Object pConnection, Object request)
    throws AdapterException,ConnectionException;

    private Object doInvoke(Object connection, Object request)
            throws AdapterException, ConnectionException {
        // TODO change method signature in MDW 6 to avoid try/catch
        try {
            Map<String, String> headers = null;
            if (this instanceof HeaderAwareAdapter)
                headers = ((HeaderAwareAdapter) this).getRequestHeaders();

            ActivityRuntimeContext runtimeContext = getRuntimeContext();
            List<AdapterMonitor> monitors = MonitorRegistry.getInstance().getAdapterMonitors();

            Object altRequest = null;
            for (AdapterMonitor monitor : monitors) {
                altRequest = monitor.onInvoke(runtimeContext, request, headers);
                if (altRequest != null)
                    request = altRequest;
            }

            Object altResponse = null;
            for (AdapterMonitor monitor : monitors) {
                altResponse = monitor.onInvoke(runtimeContext, request, headers);
                if (altResponse != null)
                    return altResponse;
            }

            Object response;
            Map<String,String> responseHeaders = null;
            if (this instanceof HeaderAwareAdapter) {
                response = ((HeaderAwareAdapter)this).invoke(connection, request, headers);
                responseHeaders = ((HeaderAwareAdapter)this).getResponseHeaders();
            }
            else {
                response = invoke(connection, request);
            }

            for (AdapterMonitor monitor : monitors) {
                altResponse = monitor.onResponse(getRuntimeContext(), response, responseHeaders);
                if (altResponse != null)
                    response = altResponse;
            }
            return response;
        }
        catch (ActivityException ex) {
            throw new AdapterException(ex.getMessage(), ex);
        }
    }

    /**
     * Open a connection to the external system.
     * You may implement this through a connection pool you implement,
     * then this method is to get an instance from the pool.
     * @return
     * @throws ConnectionException retriable connection failure
     * @throws AdapterException non-retriable connection failure
     */
    protected abstract Object openConnection()
        throws ConnectionException, AdapterException;


    /**
     * Close a connection opened.
     * If you have implemented your connection pool, then
     * this method should return the connection instance
     * to the pool.
     * @param conneciton
     */
    protected abstract void closeConnection(Object connection);

    /**
     * This method is used for directly invoke the adapter activity
     * from code, rather than as part of process execution flow.
     * If logging is desired, extenders should override logMessage().
     *
     * @param attrs	attributes to be passed to configure the activity.
     *
     * @param request request message
     * @return response message
     * @throws AdapterException
     * @throws ConnectionException
     */
    public Object directInvoke(ActivityRuntimeContext runtimeContext, Object request)
    		throws AdapterException, ConnectionException {

        prepare(runtimeContext);

        if (isStubMode()) {
            logger.info("Adapter is running in StubMode. AdapterName:" + this.getClass().getName());
            return getStubResponse(request);
        }
        else {
            Object connection = null;
            try {
                connection = openConnection();
                if (doLogging()){
                     String requestString = externalRequestToString(request);
                     logMessage(requestString, false);
                }
                Object response = invoke(connection, request);
                if (response != null && doLogging()) {
                     String responseString = externalResponseToString(response);
                     logMessage(responseString, true);
                }
                return response;
            }
            finally {
                if (connection != null)
                    closeConnection(connection);
            }
        }
    }

    protected boolean isStubMode() {
        String stubModeString = getAttributeValue(WorkAttributeConstant.SIMULATION_STUB_MODE);
        return (stubModeString != null && stubModeString.equalsIgnoreCase("on"));
    }

    protected Object executePreScript(Object request) throws ActivityException {
        if (!StringHelper.isEmpty(getPreScript())){
            String preScriptLanguage = getAttributeValue(WorkAttributeConstant.PRE_SCRIPT_LANGUAGE);
            if (StringHelper.isEmpty(preScriptLanguage)) {
                throw new ActivityException(-1, "PreScript Language not defined for the PreScript");
            }
            Object retObj = executeScript(getPreScript(), preScriptLanguage, getPreScriptBindings(request));
            Object variableReq = (retObj != null ? retObj : request);
            if (variableReq != null)
                return variableReq;
        }
        return request;
    }

    protected void executePostScript(Object response) throws ActivityException {
        if (!StringHelper.isEmpty(getPostScript())){
            String postScriptLanguage = getAttributeValue(WorkAttributeConstant.POST_SCRIPT_LANGUAGE);
            if (StringHelper.isEmpty(postScriptLanguage)) {
                throw new ActivityException(-1, "PostScript Language not defined for the PostScript");
            }
            Object retObj = executeScript(getPostScript(), postScriptLanguage, getPostScriptBindings(response));
            if (null != retObj) {
                setReturnCode(retObj.toString());
            }
        }
    }

    /**
     * Extra bindings for pre-script beyond process variable values.
     */
    protected Map<String,Object> getPreScriptBindings(Object request) throws ActivityException {
        Map<String,Object> binding = new HashMap<String,Object>();
        String requestVar = this.getAttributeValue(REQUEST_VARIABLE);
        if (requestVar != null && request instanceof String) {
            binding.put("request", getVariableValue(requestVar));
        }
        else {
            binding.put("request", request);
        }
        return binding;
    }

    /**
     * Extra bindings for post-script beyond process variable values.
     */
    protected Map<String,Object> getPostScriptBindings(Object response) throws ActivityException {
        Map<String,Object> binding = new HashMap<String,Object>();
        String varname = this.getAttributeValue(RESPONSE_VARIABLE);
        if (varname != null && response instanceof String) {
            binding.put("response", getVariableValue(varname));
        }
        else {
            binding.put("response", response);
        }

        return binding;
    }

    protected String getPreScript() {
        return getAttributeValue(WorkAttributeConstant.PRE_SCRIPT);
    }

    protected String getPostScript() {
        return getAttributeValue(WorkAttributeConstant.POST_SCRIPT);
    }
}
