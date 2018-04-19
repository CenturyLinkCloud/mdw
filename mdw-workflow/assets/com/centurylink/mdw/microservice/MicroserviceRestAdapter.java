package com.centurylink.mdw.microservice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.connector.adapter.AdapterException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.translator.JsonTranslator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.workflow.adapter.rest.RestServiceAdapter;

/**
 * REST adapter overridden to support microservices and
 * populate headers, response and serviceSummary (if it exists)
 */
public class MicroserviceRestAdapter extends RestServiceAdapter {

    public static final String JSON_RESPONSE_VARIABLE = "JSON Response Variable";

    /**
     * Overridden to append JSON headers.
     */
    @Override
    public Map<String, String> getRequestHeaders() {
        Map<String, String> requestHeaders = super.getRequestHeaders();
        if (requestHeaders == null)
            requestHeaders = new HashMap<String, String>();
        try {
            requestHeaders.put(Request.REQUEST_ID, getMasterRequestId());
            String httpMethod = getHttpMethod();
            if ("GET".equals(httpMethod))
                requestHeaders.put("Accept", "application/json");
            else
                requestHeaders.put("Content-Type", "application/json");
        }
        catch (ActivityException ex) {
            logexception(ex.getMessage(), ex);
        }
        return requestHeaders;
    }

    protected void populateResponseVariable(StatusResponse response)
            throws ActivityException, JSONException {
        Variable responseVariable = null;
        String responseVarName = getAttributeValue(JSON_RESPONSE_VARIABLE);
        if (responseVarName != null) {
            responseVariable = getProcessDefinition().getVariable(responseVarName);
            if (responseVariable == null)
                throw new ActivityException("No variable defined: " + responseVarName);
        }
        else {
            // default response variable name
            responseVariable = getProcessDefinition().getVariable("response");
        }

        if (responseVariable != null && VariableTranslator.getTranslator(getPackage(),
                responseVariable.getType()) instanceof JsonTranslator) {
            if (Jsonable.class.getName().equals(responseVariable.getType()))
                setVariableValue(responseVariable.getName(), response);
            else if (JSONObject.class.getName().equals(responseVariable.getType()))
                setVariableValue(responseVariable.getName(), response.getJson());
            else
                throw new JSONException(
                        "Unrecognized JSON variable type: " + responseVariable.getType());
        }
    }

    /**
     * Add request-id header
     *
     * @throws ActivityException
     */
    protected void populateResponseHeaders() throws AdapterException, ActivityException {
        Variable responseHeadersVar = getProcessDefinition().getVariable("responseHeaders");
        if (responseHeadersVar != null) {
            try {
                Map<String, String> responseHeaders = super.getResponseHeaders();
                if (responseHeaders == null)
                    responseHeaders = new HashMap<String, String>();
                responseHeaders.put(Request.REQUEST_ID, getMasterRequestId());
                setVariableValue("responseHeaders", responseHeaders);
            }
            catch (ActivityException ex) {
                throw new AdapterException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Populate response variable and serviceSummary
     * @return response Id
     */
    @Override
    protected Long logResponse(Response response) {
        Long responseId = super.logResponse(response);
        int code = response.getStatusCode() == null ? 0 : response.getStatusCode();
        Status status = new Status(code, response.getStatusMessage());
        // Ensure that we get the most up-to-date serviceSummary
        String[] outDocs = new String[1];
        outDocs[0] = ServiceSummary.SERVICE_SUMMARY;
        setOutputDocuments(outDocs);
        //
        try {
            populateResponseVariable(new StatusResponse(status));
            populateResponseHeaders();
            updateServiceSummary(status, responseId);
        }
        catch (Exception ex) {
            logexception(ex.getMessage(), ex);
        }

        return responseId;
    }

    /**
     * TODO: Do we really have to save requestId var?  Or can we get it from db?
     */
    @Override
    protected Long logRequest(String message) {
        Long requestId = super.logRequest(message);
        try {
            Variable requestIdVar = getProcessDefinition().getVariable("requestId");
            if (requestIdVar != null && Long.class.getName().equals(requestIdVar.getType()))
                setParameterValue("requestId", requestId);
        }
        catch (ActivityException ex) {
            logexception(ex.getMessage(), ex);
        }
        return requestId;
    }

    public void updateServiceSummary(Status status, Long responseId)
            throws ActivityException, ServiceException, DataAccessException {

        ServiceSummary serviceSummary = getServiceSummary();
        if (serviceSummary != null) {
            String microservice = getMicroservice();
            List<Invocation> invocations = serviceSummary.getInvocations(microservice);
            if (invocations == null)
                throw new ActivityException("No invocations for: " + microservice);

            // if last invocation does not have a response, add it there
            if (invocations.size() > 0
                    && invocations.get(invocations.size() - 1).getStatus() == null) {
                invocations.get(invocations.size() - 1).setStatus(status);
            }
            else {
                Invocation invocation = new Invocation(getRequestId(), status, Instant.now(), responseId);
                serviceSummary.addInvocation(microservice, invocation);
            }

            setVariableValue(ServiceSummary.SERVICE_SUMMARY, serviceSummary);
            // Do any notifications
            notifyServiceSummaryUpdate(serviceSummary);
        }
    }

    /**
     * <p>
     * This is left to the implementor if any kind of notification
     * needs to be sent out whenever the service summary is updated
     * </p>
     * @param serviceSummary
     * @throws ServiceException
     * @throws DataAccessException
     */
    public void notifyServiceSummaryUpdate(ServiceSummary serviceSummary) {
    }

    /**
     * This returns the microservice that is to be updated in the serviceSummary
     * It defaults to the value of "packageName/processName".
     * In the case where this won't work (i.e we are in a deep subprocess)
     * it can be overridden by defining the value in the activity for "Microservice name"
     * @return the microservice to be updated in the serviceSummary
     */
    public String getMicroservice() {
        String microservice = getAttributeValue(ServiceSummary.MICROSERVICE);
        if (microservice == null)
            microservice = getPackage().getName() + "/" + getProcessDefinition().getName();
        return microservice;
    }

    /**
     * Looks up and returns the ServiceSummary variable value
     * @return ServiceSummary object
     * @throws ActivityException
     */
    public ServiceSummary getServiceSummary() throws ActivityException {
        ServiceSummary serviceSummary = (ServiceSummary) getVariableValue(
                ServiceSummary.SERVICE_SUMMARY);
        if (serviceSummary == null) {
            return null;
        }
        else {
            return serviceSummary;
        }
    }

    /**
     * Returns the requestId that we will use to populate the serviceSummary
     * @return requestId used to populate the serviceSummary
     * @throws ActivityException
     */
    public Long getRequestId() throws ActivityException {

        String requestIdVarName = getAttributeValue(ServiceSummary.REQUEST_ID_VAR);
        if (requestIdVarName == null)
            requestIdVarName = ServiceSummary.DEFAULT_REQUEST_ID_VAR;

        Variable requestIdVar = getProcessDefinition().getVariable(requestIdVarName);
        if (requestIdVar == null && !"GET".equals(getHttpMethod()))
            throw new ActivityException("Request ID variable not defined: " + requestIdVarName);

        Object requestIdObj = getVariableValue(requestIdVarName);
        if (requestIdObj == null)
            return null;

        if (requestIdObj instanceof Long) {
            return (Long) requestIdObj;
        }
        else {
            try {
                return Long.valueOf(requestIdObj.toString());
            }
            catch (NumberFormatException ex) {
                throw new ActivityException(
                        "Invalid value for " + requestIdVarName + ": " + requestIdObj);
            }
        }
    }

}
