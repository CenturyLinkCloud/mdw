package com.centurylink.mdw.microservice;

import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.LinkedProcessInstance;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.ProcessInstance;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/summary")
@Api("Service summary retrieval")
public class ServiceSummaryApi extends JsonRestService {

    @Path("/{id}/{subInfo}")
    @ApiOperation(value="Retrieve a service summary or list of process instances")
    @ApiImplicitParams({
        @ApiImplicitParam(name="masterRequestId", paramType="query"),
        @ApiImplicitParam(name="processInstanceId", paramType="query", dataType="Long")})
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {
        String[] segments = getSegments(path);
        WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
        Query query = getQuery(path, headers);
        if (segments.length == 5) {
            // no {id} -- must have a supported query filter
            ServiceSummary serviceSummary = null;
            String masterRequestId = query.getFilter("masterRequestId");
            if (masterRequestId != null) {
                // Do not assume serviceSummary defined in master process instance
                ProcessInstance masterProcess = workflowServices.getMasterProcess(masterRequestId);
                if (masterProcess == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Master process not found for: " + masterRequestId);
                try {
                    serviceSummary = findServiceSummary(ServiceLocator.getProcessServices().getCallHierearchy(masterProcess.getId()));
                }
                catch (DataAccessException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                }
            }
            else if (query.getFilter("processInstanceId") != null) {
                try {
                    Long processInstanceId = query.getLongFilter("processInstanceId");
                    ProcessRuntimeContext runtimeContext = workflowServices.getContext(processInstanceId);
                    serviceSummary = findServiceSummary(runtimeContext);
                }
                catch (NumberFormatException ex) {
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Bad processInstanceId: " + query.getFilter("processInstanceId"));
                }
            }
            else {
                throw new ServiceException(ServiceException.BAD_REQUEST, "Missing query filter");
            }
            if (serviceSummary == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Service summary not found");
            return serviceSummary.getJson();
        }
        else if (segments.length == 6 || segments.length == 7) {
            String id = segments[5];
            try {
                Long docId = Long.parseLong(id);
                Document document = workflowServices.getDocument(docId);
                ServiceSummary serviceSummary = (ServiceSummary) document.getObject(
                        Jsonable.class.getName(),
                        PackageCache.getPackage(this.getClass().getPackage().getName()));
                if (segments.length == 7 && segments[6].equals("subflows")) {
                    ProcessList processList = new ProcessList("processInstances", new ArrayList<>());
                    for (MicroserviceHistory history : serviceSummary.getMicroservices()) {
                        Long instanceId = history.getInstanceId();
                        processList.addProcess(workflowServices.getProcess(instanceId));
                    }
                    return processList.getJson();
                }
                else {
                    return serviceSummary.getJson();
                }
            }
            catch (NumberFormatException ex) {
                throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid id: " + id);
            }
        }

        throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + path);
    }

    private ServiceSummary findServiceSummary(LinkedProcessInstance parentInstance) throws ServiceException {
        ProcessRuntimeContext runtimeContext = ServiceLocator.getWorkflowServices().getContext(parentInstance.getProcessInstance().getId());
        ServiceSummary serviceSummary = findServiceSummary(runtimeContext);
        if (serviceSummary == null && !parentInstance.getChildren().isEmpty()) {
            for (LinkedProcessInstance childInstance : parentInstance.getChildren()) {
                serviceSummary = findServiceSummary(childInstance);
                if (serviceSummary != null)
                    return serviceSummary;
            }
        }
        return serviceSummary;
    }

    private ServiceSummary findServiceSummary(ProcessRuntimeContext runtimeContext) {
        String variableName = "serviceSummary";
        Variable var = runtimeContext.getProcess().getVariable(variableName);
        if (var == null) {
            variableName = getServiceSummaryVariableName(runtimeContext.getProcess());
            var = runtimeContext.getProcess().getVariable(variableName);
        }
        if (var == null)
            return null;
        else
            return (ServiceSummary) runtimeContext.getVariables().get(variableName);
    }

    /**
     * Walks through all activities looking for the attribute.
     */
    private String getServiceSummaryVariableName(Process processDefinition) {
        for (Activity activity : processDefinition.getActivities()) {
            String attr = activity.getAttribute("serviceSummaryVariable");
            if (attr != null)
                return attr;
        }
        return null;
    }
}
