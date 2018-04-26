package com.centurylink.mdw.microservice;

import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.workflow.ProcessList;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.WorkflowServices;
import com.centurylink.mdw.services.rest.JsonRestService;

@Path("/summary")
public class ServiceSummaryApi extends JsonRestService {

    @Path("/{id}/{subInfo}")
    public JSONObject get(String path, Map<String,String> headers)
    throws ServiceException, JSONException {

        String[] segments = getSegments(path);
        Query query = getQuery(path, headers);
        if (segments.length == 5) {
            // no {id} -- must have a supported query filter
            String masterRequestId = query.getFilter("masterRequestId");
            if (masterRequestId == null)
                throw new ServiceException(ServiceException.BAD_REQUEST, "Missing query filter");

            // TODO: retrieve by masterRequestId (do not assume master process instance)
            return null;
        }
        else if (segments.length == 6 || segments.length == 7) {
            String id = segments[5];
            try {
                Long docId = Long.parseLong(id);
                WorkflowServices workflowServices = ServiceLocator.getWorkflowServices();
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
}
