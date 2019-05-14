package com.centurylink.mdw.milestones;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.workflow.Linked;
import com.centurylink.mdw.model.workflow.Milestone;
import com.centurylink.mdw.service.data.process.HierarchyCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;

@Path("/")
@Api("Workflow milestones")
public class MilestonesApi extends JsonRestService {

    @Path("/{masterRequestId}")
    @ApiOperation(value="Retrieve for a master request, or all milestones")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException {

        Query query = getQuery(path, headers);
        String seg4 = getSegment(path, 4);
        if (seg4 != null) {
            if (seg4.equals("definitions")) {
                String procId = getSegment(path, 5);
                if (procId == null)
                    throw new ServiceException(ServiceException.BAD_REQUEST, "{processId} required");
                return getDefinition(Long.parseLong(procId));
            }
            else {
                // by masterRequestId
                return ServiceLocator.getWorkflowServices().getMilestones(seg4).getJson();
            }
        } else {

            return ServiceLocator.getWorkflowServices().getMilestones(query).getJson();
        }
    }

    @Path("/definitions/{processId}")
    public JSONObject getDefinition(Long processId) throws ServiceException {
        Linked<Milestone> milestones = HierarchyCache.getMilestones(processId);
        if (milestones == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Milestones not found: " + processId);
        return milestones.getJson();
    }
}
