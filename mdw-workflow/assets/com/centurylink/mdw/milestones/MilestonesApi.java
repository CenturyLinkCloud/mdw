package com.centurylink.mdw.milestones;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;

@Path("/")
@Api("Workflow milestones")
public class MilestonesApi extends JsonRestService {

    @Path("/{masterRequestId}")
    @ApiOperation(value="Retrieve for a master request, or all milestones")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {

        Query query = getQuery(path, headers);
        String masterRequestId = getSegment(path, 4);
        if (masterRequestId != null) {
            // TODO
            return null;
        }
        else {
            return ServiceLocator.getWorkflowServices().getMilestones(query).getJson();
        }
    }
}
