package com.centurylink.mdw.milestones;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;

@Path("/milestones")
@Api("Workflow milestones")
public class MilestonesApi extends JsonRestService {

    @Path("/{processAsset}")
    @ApiOperation(value="Retrieve past and future milestones from the designated starting point")
    @ApiImplicitParams({
            @ApiImplicitParam(name="futures", paramType="query", dataType="Boolean", defaultValue="true")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        return super.get(path, headers);
    }
}
