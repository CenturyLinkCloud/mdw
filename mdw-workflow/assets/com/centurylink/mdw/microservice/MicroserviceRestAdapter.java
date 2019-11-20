package com.centurylink.mdw.microservice;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.Response;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.model.variable.DocumentReference;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.workflow.adapter.rest.RestServiceAdapter;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST adapter overridden to support microservices and
 * populate headers, response and serviceSummary (if it exists)
 */
@Activity(value="Microservice REST Adapter", category= AdapterActivity.class, icon="com.centurylink.mdw.base/adapter.png",
        pagelet="com.centurylink.mdw.microservice/microserviceRest.pagelet")
public class MicroserviceRestAdapter extends RestServiceAdapter {

    protected Long requestId = null;
    protected boolean changedIsolation = false;
    /**
     * Overridden to append JSON headers.
     */
    @Override
    public Map<String, String> getRequestHeaders() {
        Map<String, String> requestHeaders = super.getRequestHeaders();
        if (requestHeaders == null)
            requestHeaders = new HashMap<>();
        try {
            requestHeaders.put(Request.REQUEST_ID, getMasterRequestId());
            String httpMethod = getHttpMethod();
            if ("GET".equals(httpMethod))
                requestHeaders.put("Accept", "application/json");
            else
                requestHeaders.put("Content-Type", "application/json");
        }
        catch (ActivityException ex) {
            logError(ex.getMessage(), ex);
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
        String message = response.getStatusMessage() == null ? StatusResponse.forCode(code).getStatus().getMessage() :
                response.getStatusMessage();
        Status status = new Status(code, message);
        try {
            updateServiceSummary(status, responseId);
        }
        catch (ActivityException | SQLException ex) {
            logError(ex.getMessage(), ex);
        }
        return responseId;
    }

    /**
     * TODO: Do we really have to save requestId var?  Or can we get it from db?
     */
    @Override
    protected Long logRequest(Request request) {
        requestId = super.logRequest(request);
        try {
            Variable requestIdVar = getProcessDefinition().getVariable("requestId");
            if (requestIdVar != null && Long.class.getName().equals(requestIdVar.getType()))
                setParameterValue("requestId", requestId);
        }
        catch (ActivityException ex) {
            logError(ex.getMessage(), ex);
        }
        try { // Add microservice, or Update microservice if it has id=0 (when executing in parallel from OrchestratorActivity)
            updateServiceSummary(null, null);
        }
        catch (ActivityException | SQLException ex) {
            logError(ex.getMessage(), ex);
        }
        return requestId;
    }

    public void updateServiceSummary(Status status, Long responseId)
            throws ActivityException, SQLException {

        ServiceSummary serviceSummary = getServiceSummary(true);
        if (serviceSummary != null) {
            try {
                doServiceSummaryUpdate(serviceSummary, status, responseId);
                getEngine().getDatabaseAccess().commit();
                // For mySQL, we need to change the isolation level so that the next read (once we have response)
                // is not using the DB snapshot that got created when we were in here first for the request.
                // Otherwise, we risk overwriting any service summary updates performed by a different thread.
                if (status == null && getEngine().getDatabaseAccess().isMySQL()) {
                    getEngine().getDatabaseAccess().runUpdate("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
                    changedIsolation = true;
                }
            }
            finally {
                if (status != null) {
                    getEngine().getDatabaseAccess().commit();
                    // For mySQL, now (after getting response) we need to restore isolation level back to default
                    if (changedIsolation && getEngine().getDatabaseAccess().connectionIsOpen())
                        getEngine().getDatabaseAccess().runUpdate("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ");
                }
            }
            if (status != null) {
                // Only notify after performing invocation
                notifyServiceSummaryUpdate(serviceSummary);
            }
        }
    }

    protected void doServiceSummaryUpdate(ServiceSummary serviceSummary, Status status, Long responseId)
            throws ActivityException {
        ServiceSummary currentSummary = serviceSummary.findParent(getProcessInstanceId());
        if (currentSummary == null)
            currentSummary = serviceSummary;

        String microservice = getMicroservice(serviceSummary);
        List<Invocation> invocations = currentSummary.getInvocations(microservice, getProcessInstanceId());
        if (invocations == null)
            throw new ActivityException("No invocations for: " + microservice);

        // if last invocation does not have a response, add it there
        if (invocations.size() > 0 && invocations.get(invocations.size() - 1).getStatus() == null) {
            invocations.get(invocations.size() - 1).setStatus(status);
            invocations.get(invocations.size() - 1).setResponseId(responseId);
        }
        else {
            Invocation invocation = new Invocation(getRequestId(), status, Instant.now(), responseId);
            invocations.add(invocation);
        }

        setVariableValue(getServiceSummaryVariableName(), serviceSummary);
    }

    /**
     * Standard behavior is to publish event fitting standard pattern of default event
     * used in DependenciesWaitActivity (Microservice Dependencies Wait)
     */
    protected void notifyServiceSummaryUpdate(ServiceSummary serviceSummary) throws ActivityException {
        WorkflowServices wfs = ServiceLocator.getWorkflowServices();
        try {
            wfs.notify("service-summary-update-" + getMasterRequestId(), null, 2);
        } catch (ServiceException e) {
            throw new ActivityException("Cannot publish Service Summary update event", e);
        }
    }

    /**
     * Standard behavior is to read from a String variable (defaultName='microservice').
     * If no variable is defined, fallback is the 'microservice' design attribute.
     */
    protected String getMicroservice(ServiceSummary summary) throws ActivityException {
        // configured through attribute - attribute default is supposed to be $microservice
        String microservice = getAttributeValueSmart("Microservice");
        if (StringUtils.isBlank(microservice)) {
            String microserviceVarName = getAttribute("microserviceVariable", "microservice");
            if (getMainProcessDefinition().getVariable(microserviceVarName) == null) {
                microservice = getMicroserviceById(summary);
                if (microservice == null && summary.getChildServiceSummaryList() != null) {
                    for (ServiceSummary child : summary.getChildServiceSummaryList()) {
                        microservice = getMicroserviceById(child);
                        if (microservice != null)
                            break;
                    }
                }
                if (microservice == null) // configured through activity name/label
                    microservice = getActivityName();
            }
            else {
                microservice = getParameterStringValue(microserviceVarName);
            }
        }
        if (microservice == null)
            throw new ActivityException("Cannot discern microservice");
        return microservice;
    }

    protected String getMicroserviceById(ServiceSummary summary) {
        // Get it from service summary based on procInstId
        for (String microserviceName : summary.getMicroservices().keySet()) {
            for (MicroserviceInstance instance : summary.getMicroservices(microserviceName).getInstances()) {
                if (getProcessInstanceId().equals(instance.getId()))
                    return instance.getMicroservice();
            }
        }
        return null;
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

        if (requestId != null)
            return requestId;

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
