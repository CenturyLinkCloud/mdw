package com.centurylink.mdw.microservice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.workflow.adapter.rest.RestServiceAdapter;

/**
 * REST adapter overridden to support microservices and
 * populate headers, response and serviceSummary (if it exists)
 */
public class MicroserviceRestAdapter extends RestServiceAdapter {

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

    /**
     * Populates serviceSummary
     * @return responseId
     */
    @Override
    protected Long logResponse(Response response) {
        Long responseId = super.logResponse(response);
        int code = response.getStatusCode() == null ? 0 : response.getStatusCode();
        Status status = new Status(code, response.getStatusMessage());
        try {
            updateServiceSummary(status, responseId);
        }
        catch (ActivityException ex) {
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
            throws ActivityException {

        ServiceSummary serviceSummary = getServiceSummary(true);
        if (serviceSummary != null) {
            String microservice = getMicroservice();
            List<Invocation> invocations = serviceSummary.getInvocations(microservice, getProcessInstanceId());
            if (invocations == null)
                throw new ActivityException("No invocations for: " + microservice);

            // if last invocation does not have a response, add it there
            if (invocations.size() > 0
                    && invocations.get(invocations.size() - 1).getStatus() == null) {
                invocations.get(invocations.size() - 1).setStatus(status);
            }
            else {
                Invocation invocation = new Invocation(getRequestId(), status, Instant.now(), responseId);
                serviceSummary.addInvocation(microservice, getProcessInstanceId(), invocation);
            }

            setVariableValue(getServiceSummaryVariableName(), serviceSummary);
            notifyServiceSummaryUpdate(serviceSummary);
        }
    }

    /**
     * TODO: default behavior
     */
    public void notifyServiceSummaryUpdate(ServiceSummary serviceSummary) {
    }

    /**
     * Standard behavior is to read from a String variable (defaultName='microservice').
     * If no variable is defined, fallback is the 'microservice' design attribute.
     */
    protected String getMicroservice() throws ActivityException {
        String microservice = null;
        String microserviceVarName = getAttribute("microserviceVariable", "microservice");
        if (getMainProcessDefinition().getVariable(microserviceVarName) == null) {
            // configured through attribute
            microservice = getAttributeValueSmart("microservice");
        }
        else {
            microservice = getParameterStringValue(microserviceVarName);
        }
        if (microservice == null)
            throw new ActivityException("Cannot discern microservice");
        return microservice;
    }

    protected ServiceSummary getServiceSummary(boolean forUpdate) throws ActivityException {
        DocumentReference docRef = (DocumentReference)getParameterValue(getServiceSummaryVariableName());
        if (docRef == null)
            return null;
        if (forUpdate)
            return (ServiceSummary) getDocumentForUpdate(docRef, Jsonable.class.getName());
        else
            return (ServiceSummary) getDocument(docRef, Jsonable.class.getName());
    }

    /**
     * You'd need a custom .impl asset to set this through designer
     */
    protected String getServiceSummaryVariableName() {
        return getAttribute("serviceSummaryVariable", "serviceSummary");
    }

    /**
     * Returns the requestId that we will use to populate the serviceSummary
     * @return requestId used to populate the serviceSummary
     */
    public Long getRequestId() throws ActivityException {

        String requestIdVarName = getAttribute("requestIdVariable", "requestId");

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
