/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.adapter.AdapterInvocationError;
import com.centurylink.mdw.adapter.PoolableAdapter;
import com.centurylink.mdw.adapter.SimulationResponse;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.connector.adapter.ConnectionException;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.event.InternalEvent;
import com.centurylink.mdw.model.monitor.ScheduledEvent;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.monitor.AdapterMonitor;
import com.centurylink.mdw.monitor.MonitorRegistry;
import com.centurylink.mdw.services.event.ScheduledEventQueue;
import com.centurylink.mdw.services.event.StubHelper;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;

/**
 * This class is the common super class for poolable adapters.
 */
@Tracked(LogLevel.TRACE)
public abstract class PoolableAdapterBase extends DefaultActivityImpl
implements AdapterActivity, PoolableAdapter, AdapterInvocationError {
    static final String PROP_RETRY_EXCEPTIONS = "RETRY_EXCEPTIONS";
    private static Random random = null;
    boolean isStubbing;        // expose only to allow CompositeSynchronousAdapter to access

    /**
     * Timeout value for waiting for responses. Used for synchronous mode only.
     * The default method reads from the attribute "timeout", and
     * return -1 seconds if the attribute is not defined,
     * and the default value to be used is
     *
     * @return timeout value in seconds
     */
    protected int getTimeoutForResponse() {
        String timeout_s = null;
        int timeout;
        try {
            timeout_s = this.getAttributeValueSmart(PROP_TIMEOUT);
            timeout = timeout_s==null?-1:Integer.parseInt(timeout_s);
        } catch (NumberFormatException e) {
            logger.severeException("Cannot parse timeout value " + timeout_s, e);
            timeout = -1;
        } catch (PropertyException e) {
            logger.severeException("Cannot read timeout attribute " + PROP_TIMEOUT, e);
            timeout = -1;
        }
        return timeout;
    }

    abstract protected boolean canBeSynchronous();

    abstract protected boolean canBeAsynchronous();

    protected boolean canBeCertified() {
        return false;
    }

    /**
     * The method overrides the one from the super class and perform the followings:
     * <ul>
     *   <li>It gets the value of the variable with the name specified in the attribute
     *      REQUEST_VARIABLE. The value is typically an XML document or a string</li>
     *   <li>It invokes the variable translator to convert the value into a string
     *      and then return the string value.</li>
     * </ul>
     * The method will through exception if the variable is not bound,
     * or the value is not bound to a DocumentReference or String.
     */
    protected String getRequestData() throws ActivityException {
        String varname = getAttributeValue(REQUEST_VARIABLE);
        Object request = varname == null ? null : getParameterValue(varname);

        if (hasPreScript()) {
            Object ret = executePreScript(request);
            if (ret == null) {
                // nothing returned; requestVar may have been assigned by script
                request = getParameterValue(varname);
            }
            else {
                request = ret;
            }
        }
        if (request == null)
            throw new ActivityException("Request data is null");

        // TODO handle general document types via Variable translator mechanism
        // instead of just these specific types
        if (request instanceof DocumentReference)
            request = getDocumentContent((DocumentReference)request);
        if (request instanceof String)
            return (String)request;
        else if (request instanceof Document)
            return VariableTranslator.realToString(getPackage(), Document.class.getName(), request);
        else if (request instanceof XmlObject)
            return ((XmlObject)request).xmlText();
        else
            throw new ActivityException(
                "Cannot handle request of type " + request.getClass().getName());
    }

    /**
     * The method is used to determine if the current usage is synchronous (waiting for response) or not.
     * The method is used only when both canBeSynchronous() and canBeAsynchronous() are true
     */
    @Override
    public boolean isSynchronous() {
        if (canBeSynchronous()) {
            if (this.canBeAsynchronous()) {
                String attr = this.getAttributeValue(PROP_SYNCHRONOUS_RESPONSE);
                return (attr!=null && attr.equalsIgnoreCase("true"));
            } else return true;
        } else return false;
    }

    /**
     * The method is invoked when the external system interaction is a success.
     * (i.e. the external system responded something, even an error code).
     * The method may convert external-system-detected errors into failure
     * by throwing an exception here. Throwing an exception here
     * also triggers the method onFailure to be called.
     *
     * This method is also the place to translate external responses
     * into internal format and persist them somewhere, if these are needed.
     *
     * For asynchronous and certified messages, you should not attempt to override
     * this method.
     *
     * The default method does nothing if the response is null,
     * or set the response to the variable specified in RESPONSE_VARIABLE attribute.
     * @param response response message from the external system
     * @throws ConnectionException this exception indicates a system connection
     *         failure (most likely between the direct interfacing system to down
     *         stream systems, as direct connection failure will not some to this method).
     *        The exception is auto-retriable.
     * @throws AdapterException thrown when the external system responded but
     *      the response needs to be handled as a non-auto-retriable error.
     * @throws ActivityException thrown when the post processing
     *         logic hits an exception that needs to fail the activity.
     *         This error is not auto-retriable, either.
     */
    public void onSuccess(String response)
    throws ActivityException, ConnectionException, AdapterException {
        if (response==null) return;
        String varname = this.getAttributeValue(RESPONSE_VARIABLE);
        if (varname==null) return;
        String vartype = this.getParameterType(varname);
        if (VariableTranslator.isDocumentReferenceVariable(getPackage(), vartype))
            this.setParameterValueAsDocument(varname, vartype, response);
        else this.setParameterValue(varname, response);
    }

    /**
     * The method is invoked on the failure of each try.
     * The outcome of the method must be one of the following:
     * a) throw ConnectionException. The engine will put the activity in error status,
     *             and schedule automatically retry of the activity
     * b) throw AdapterException. The engine will put the activity in error status,
     *             and transition based on ERROR event (typically lead to exception handler)
     * c) return a completion code w/o throwing exception. The engine will complete
     *             the activity and transition accordingly
     *
     * The default implementation does the the following
     *  - if errorCause is an AdapterException, throws it.
     *  - if errorCause is a ConnectionException, throws it.
     *  - in any other case, throw an AdapterException wrapping the original exception
     *
     * If you override this method, you typically should re-throw ConnectionException,
     * so that the internal retry mechanism will not be impacted. More specifically,
     * the following are some special scenarios that you may need to know:
     *  - If the maximum number of retries has been reached, errorCause is an AdapterException
     *    with error code AdapterException.EXCEED_MAXTRIES. Original ConnectionException is its cause.
     *    If you do not want to put the activity in error status and invoke exception handler,
     *    rather would like to handle the logic within process definition, then return
     *    a completion code without re-throwing the exception.
     *  - If the activity is a ConnectionPoolAdapter and
     *    all connections are used in a connection pool, errorCause is a ConnectionException
     *    with error code ConnectionException.POOL_EXHAUSTED. You typically should just
     *    re-throw this exception so the engine will put activity in waiting status
     *      and will resume after a connection becomes available.
     *  - If the activity is a ConnectionPoolAdapter and the connection pool is disabled
     *    (manually or automatically shut down), errorCause is a ConnectionException
     *    with error code ConnectionException.POOL_DISABLED. You typically should just
     *    re-throw this exception so the engine will put activity in waiting status,
     *      and will resume after the pool is enabled.
     *
     * @param errorCause the exception received
     * @return the completion code for the activity, if no exception is thrown
     * @throws AdapterException when no retry is expected
     * @throws ConnectionException when retry is expected
     */
    public String onFailure(Throwable errorCause)
    throws AdapterException,ConnectionException {
        if (errorCause instanceof AdapterException)
            throw (AdapterException)errorCause;
        if (errorCause instanceof ConnectionException)
            throw (ConnectionException)errorCause;
        throw new AdapterException(AdapterActivityBase.APPLICATION_ERROR, errorCause.getMessage(), errorCause);
    }

    /**
     * This is only used by activities, not connection pools
     * @return
     */
    private int countTries() throws ActivityException {
        Integer[] statuses = { WorkStatus.STATUS_FAILED };
        // note the current activity is at in-progress status. Failed status must be counted
        // It is debatable if we should include other statuses
        int count;
        try {
            count = this.getEngine().countActivityInstances(getProcessInstanceId(),
                    this.getActivityId(), statuses);
            count += 1;        // add the current instance - it is not yet in failed status
        } catch (Exception e) {
            setReturnCode(null);    // override "RETRY"
            throw new ActivityException(-1, "Failed to get count on failed tries", e);
        }
        return count;
    }

    /**
     * This is only used by activities, not connection pools
     * @return
     */
    protected int getMaxTries() throws ActivityException {
        try {
            String v = getAttributeValueSmart(PROP_MAX_TRIES);
            int ret = StringHelper.getInteger(v, 1);
            return ret<1?1:ret;
        }
        catch (PropertyException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * This is only used by activities, not connection pools
     * @return
     */
    protected int getRetryInterval() throws ActivityException {
        try {
            String v = getAttributeValueSmart(PROP_RETRY_INTERVAL);
            return StringHelper.getInteger(v, 600);
        }
        catch (PropertyException ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Subclasses do not normally override this method.
     * If you do override this method, you have to include
     * call to this super method.
     * The method (directly or indirectly)
     * invokes other abstract or overridable methods to
     * perform the execution of the activity.
     *
     * The primary method to implement are openConnection(),
     * invoke() and closeConnection()
     *
     * Notes: pollable adapter assumes request data is always string
     *
     */
    @Override
    public void execute() throws ActivityException {
        String requestData = getRequestData();
        // when connection pool is down
        Response responseData = null;
        Object connection = null;
        StubHelper stubber = new StubHelper();
        isStubbing = stubber.isStubbing() || isStubMode();
        boolean logging = doLogging();
        try {
            init();
            if (!StringHelper.isEmpty(requestData) && doLogging())
                logRequest(requestData);
            if (isStubbing) {
                loginfo("Adapter is running in StubMode");
                if (stubber.isStubbing()) {
                    // TODO: stubber full Response
                    responseData = new Response(stubber.getStubResponse(getMasterRequestId(), requestData));
                    if (MAKE_ACTUAL_CALL.equals(responseData)) {
                        loginfo("Stub server instructs to get real response");
                        isStubbing = false;
                        connection = this.openConnection();
                        responseData = doInvoke(connection, requestData, getTimeoutForResponse(), getRequestHeaders());
                    } else {
                        loginfo("Response received from stub server");
                    }
                }
                else {
                    responseData = getStubbedResponse(requestData);
                }
            }
            else {
                connection = this.openConnection();
                responseData = doInvoke(connection, requestData, getTimeoutForResponse(), getRequestHeaders());
            }
            if (!responseData.isEmpty() && (logging || !isSynchronous())) {
                logResponse(responseData);
            }
            onSuccess(responseData.getContent());
            if (hasPostScript())
                executePostScript();
        } catch (Throwable ex) {
            handleException(ex);
        } finally {
            if (connection != null) {
                closeConnection(connection);
            }
        }
    }

    protected int getErrorCode(Throwable ex) {
        if (ex instanceof AdapterException) return ((AdapterException)ex).getErrorCode();
        if (ex instanceof ConnectionException) return ((ConnectionException)ex).getCode();
        return AdapterActivityBase.APPLICATION_ERROR;
    }

    /**
     * Added so retry in executed for certain exceptions.
     * @param pErrorCause
     * @throws ActivityException
     */
    protected void handleRetry(int errorCode, Throwable originalCause)
    throws ActivityException {
           handleConnectionException(-1, originalCause);
    }

    /**
     * Typically you should not override this method. ConnectionPoolAdapter
     * does override this with internal MDW logic.
     * @param pErrorCause
     * @throws ActivityException
     */
    protected void handleConnectionException(int errorCode, Throwable originalCause)
    throws ActivityException {
        InternalEvent message = InternalEvent.createActivityStartMessage(getActivityId(),
                getProcessInstanceId(), getWorkTransitionInstanceId(), getMasterRequestId(),
                COMPCODE_AUTO_RETRY);
        ScheduledEventQueue eventQueue = ScheduledEventQueue.getSingleton();
        int retry_interval = this.getRetryInterval();
        Date scheduledTime = new Date(DatabaseAccess.getCurrentTime()+retry_interval*1000);
        super.loginfo("The activity failed, set to retry at " + StringHelper.dateToString(scheduledTime));
        eventQueue.scheduleInternalEvent(ScheduledEvent.INTERNAL_EVENT_PREFIX+this.getActivityInstanceId(),
                scheduledTime, message.toString(), "procinst:"+this.getProcessInstanceId().toString());
        this.setReturnCode(COMPCODE_AUTO_RETRY);
        // the above is to prevent engine from making transitions (typically to exception handler)
        throw new ActivityException(errorCode, originalCause.getMessage(), originalCause);
    }

    protected void handleException(Throwable errorCause)
    throws ActivityException {
        logger.severeException(getAdapterInvocationErrorMessage(), errorCause);
        int max_tries = this.getMaxTries();
        int count = countTries();
        boolean isRetryEnabled = isRetryable(errorCause);
        boolean isConnectionException = errorCause instanceof ConnectionException;
        String completionCode;
        try {
            // convert ConnectionException to AdapterException or throw AdapterException when retry enabled when exceeding max number of retries,
            if (isConnectionException||isRetryEnabled) {
                int errorCode =isConnectionException?((ConnectionException)errorCause).getCode():-1;
                if (count >= max_tries && errorCode != ConnectionException.POOL_EXHAUSTED) {
                    if (max_tries > 1)
                        errorCause = new AdapterException(AdapterException.EXCEED_MAXTRIES, "Maximum number of tries/retries reached", errorCause);
                    else
                        errorCause = new AdapterException(-1, errorCause.getMessage(), errorCause);
                } else {
                if(isRetryEnabled&&!isConnectionException)
                    handleRetry(-1, errorCause);
                }
            }
            for (AdapterMonitor monitor : MonitorRegistry.getInstance().getAdapterMonitors()) {
                String errResult = (String)monitor.onError(getRuntimeContext(), errorCause);
                if (errResult != null) {
                    this.setReturnCode(errResult);
                    return;
                }
            }

            completionCode = onFailure(errorCause);
            this.setReturnCode(completionCode);
        } catch (ConnectionException e) {
            handleConnectionException(e.getCode(), errorCause);
        } catch (AdapterException e) {
            this.setReturnCode(null);        // override "RETRY"
            throw new ActivityException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    /**
     * Default behavior returns true if Throwable is included in RETRY_EXCEPTIONS
     * attribute (if declared).  If this attribute is not declared, any IOExceptions are
     * considered retryable.
     */
    public boolean isRetryable(Throwable ex) {
        try {
            String retryableAttr = getAttributeValueSmart(PROP_RETRY_EXCEPTIONS);
            if (retryableAttr != null) {
                for (String retryableExClass : retryableAttr.split(",")) {
                    if (ex.getClass().getName().equals(retryableExClass)
                            || (ex.getCause() != null && ex.getCause().getClass().getName()
                                    .equals(retryableExClass)))
                        return true;
                }
                return false;
            }
        }
        catch (Exception e) {
            logger.severeException(e.getMessage(), e);
        }

        return ex instanceof IOException;
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


    protected Long logRequest(String message) {
        try {
            DocumentReference docref = createDocument(String.class.getName(), message,
                    OwnerType.ADAPTER_REQUEST, getActivityInstanceId());
            return docref.getDocumentId();
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    protected Long logResponse(String message) {
        try {
            DocumentReference docref = createDocument(String.class.getName(), message,
                    OwnerType.ADAPTER_RESPONSE, getActivityInstanceId());
            return docref.getDocumentId();
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    protected Long logResponse(Response response) {
        try {
            DocumentReference docref = createDocument(String.class.getName(), response.getContent(),
                    OwnerType.ADAPTER_RESPONSE, getActivityInstanceId(), response.getStatusCode(), response.getStatusMessage());
            return docref.getDocumentId();
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    protected boolean isStubMode() {
        String stubModeString = getAttributeValue(WorkAttributeConstant.SIMULATION_STUB_MODE);
        return (stubModeString != null && stubModeString.equalsIgnoreCase("on"));
    }

    /**
     * TODO implements this for simulation response
     * @param unfiltered
     * @param request
     * @return
     */
    protected String filterStubResponse(String unfiltered, Object request) {
        return unfiltered;
    }

    protected Response getStubbedResponse(String requestData) {
        // compatibility
        String resp = getStubResponse(requestData);
        if (resp != null)
            return new Response(resp);

        List<SimulationResponse> responses = new ArrayList<SimulationResponse>();
        for (Attribute attr : this.getAttributes()) {
            if (attr.getAttributeName().startsWith(WorkAttributeConstant.SIMULATION_RESPONSE)) {
                SimulationResponse r = new SimulationResponse(attr.getAttributeValue());
                responses.add(r);
            }
        }
        String unfilteredResponse = null;
        String returnCode = null;
        if (responses.size() == 0) {
            unfilteredResponse = null;
        }
        else if (responses.size() == 1) {
            unfilteredResponse = responses.get(0).getResponse();
            returnCode = responses.get(0).getReturnCode();
        }
        else {    // randomly pick a response based on chances
            int total_chances = 0;
            for (SimulationResponse r : responses) {
                total_chances += r.getChance().intValue();
            }
            if (random == null)
                random = new Random();
            int ran = random.nextInt(total_chances);
            int k = 0;
            for (SimulationResponse r : responses) {
                if (ran >= k && ran < k + r.getChance().intValue()) {
                    unfilteredResponse = r.getResponse();
                    returnCode = r.getReturnCode();
                    break;
                }
                k += r.getChance().intValue();
            }
        }
        Response response = new Response(filterStubResponse(unfilteredResponse, requestData));
        if (returnCode != null) {
            try {
                response.setStatusCode(Integer.parseInt(returnCode));
            }
            catch (NumberFormatException ex) {
            }
        }
        return response;
    }


    /**
     * @deprecated Use {@link #getStubbedResponse(String) getStubbedResponse}
     */
    @Deprecated
    protected String getStubResponse(String requestData) {
        return null;
    }

    /**
     * This method is used for directly invoke the adapter activity
     * from code, rather than as part of process execution flow.
     */
    public Response directInvoke(Properties props, String request, int timeout, Map<String,String> meta_data)
            throws AdapterException, ConnectionException {
        init(props);
        if (logger == null)
            logger = LoggerUtil.getStandardLogger();
        Object connection = null;
        try {
            connection = openConnection();
            return doInvoke(connection, request, timeout, meta_data);
        } finally {
            if (connection != null) closeConnection(connection);
        }
    }

    protected String getAttribute(String name, String defval, boolean isSmart) throws AdapterException {
        try {
            String value = (isSmart)?getAttributeValueSmart(name):getAttributeValue(name);
            return (value==null)?defval:value;
        } catch (PropertyException e) {
            throw new AdapterException(-1, "Failed to initialize the adapter", e);
        }
    }

    protected boolean hasPreScript() {
        return getAttributeValue(WorkAttributeConstant.PRE_SCRIPT) != null;
    }

    protected Object executePreScript(Object requestData) throws ActivityException {
        String preScript = getAttributeValue(WorkAttributeConstant.PRE_SCRIPT);
        if (!StringHelper.isEmpty(preScript)) {
            String preScriptLanguage = getAttributeValue(WorkAttributeConstant.PRE_SCRIPT_LANGUAGE);
            if (StringHelper.isEmpty(preScriptLanguage)) {
                throw new ActivityException(-1, "Language not defined for the PreScript");
            }
            Object retObj = executeScript(preScript, preScriptLanguage, null);
            if (retObj != null)
                return retObj;
        }
        return requestData;
    }

    protected boolean hasPostScript() {
        return getAttributeValue(WorkAttributeConstant.POST_SCRIPT) != null;
    }

    protected void executePostScript() throws ActivityException {
        String postScript = getAttributeValue(WorkAttributeConstant.POST_SCRIPT);
        if (!StringHelper.isEmpty(postScript)){
            String postScriptLanguage = getAttributeValue(WorkAttributeConstant.POST_SCRIPT_LANGUAGE);
            if (StringHelper.isEmpty(postScriptLanguage)) {
                throw new ActivityException(-1, "PostScript Language not defined for the PostScript");
            }
            Object retObj = executeScript(postScript, postScriptLanguage, null);
            if (null != retObj) {
                setReturnCode(retObj.toString());
            }
        }
    }

    @Override
    public Response doInvoke(Object connection, String request, int timeout,
            Map<String,String> headers) throws AdapterException, ConnectionException {
        // TODO change method signature in MDW 6 to avoid try/catch
        try {
            ActivityRuntimeContext runtimeContext = getRuntimeContext();
            List<AdapterMonitor> monitors = MonitorRegistry.getInstance().getAdapterMonitors();

            String altRequest = null;
            for (AdapterMonitor monitor : monitors) {
                altRequest = (String)monitor.onRequest(runtimeContext, request, headers);
                if (altRequest != null)
                    request = altRequest;
            }

            String altResponse = null;
            for (AdapterMonitor monitor : monitors) {
                altResponse = (String)monitor.onInvoke(runtimeContext, request, headers);
                if (altResponse != null)
                    return new Response(altResponse); // TODO monitor full Response
            }

            String responseStr = invoke(connection, request, timeout, headers);

            for (AdapterMonitor monitor : monitors) {
                altResponse = (String)monitor.onResponse(runtimeContext, responseStr, getResponseHeaders());
                if (altResponse != null)
                    responseStr = altResponse; // TODO monitor full Response
            }

            return getResponse(connection, responseStr);
        }
        catch (IOException ex) {
            throw new AdapterException(ex.getMessage(), ex);
        }
        catch (ActivityException ex) {
            throw new AdapterException(ex.getMessage(), ex);
        }
    }

    /**
     * Override to set protocol response status/message.
     */
    protected Response getResponse(Object connection, String responseString) throws IOException {
        return new Response(responseString);
    }

    /**
     * Override to return protocol-specific headers to be included on the request.
     * Protocol adapter must have logic to make use of these values.
     */
    protected Map<String,String> getRequestHeaders() {
        return null;
    }

    private Map<String,String> responseHeaders;
    /**
     * Protocol adapter must call setResponseHeaders() with appropriate values extracted from the response.
     */
    protected Map<String,String> getResponseHeaders() { return responseHeaders; }
    protected void setResponseHeaders(Map<String,String> headers) { this.responseHeaders = headers; }

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


}
