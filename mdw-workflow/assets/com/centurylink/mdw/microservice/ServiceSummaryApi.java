package com.centurylink.mdw.microservice;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.workflow.ProcessInstance;
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
        MicroserviceAccess serviceAccess = new MicroserviceAccess();
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
                    serviceSummary = serviceAccess.findServiceSummary(ServiceLocator
                            .getProcessServices().getCallHierearchy(masterProcess.getId()));
                }
                catch (DataAccessException ex) {
                    throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                }
            }
            else if (query.getFilter("processInstanceId") != null) {
                try {
                    Long processInstanceId = query.getLongFilter("processInstanceId");
                    ProcessRuntimeContext runtimeContext = workflowServices.getContext(processInstanceId);
                    serviceSummary = serviceAccess.findServiceSummary(runtimeContext);
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
                ServiceSummary serviceSummary = serviceAccess.getServiceSummary(docId);

                if (segments.length == 7 && segments[6].equals("subflows")) {
                    return serviceAccess.getProcessList(serviceSummary).getJson();
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
}
