package com.centurylink.mdw.service.rest;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.JsonList;
import com.centurylink.mdw.model.request.HandlerSpec;
import com.centurylink.mdw.services.request.HandlerCache;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;

@Path("/Handlers")
@Api("Request handlers")
public class Handlers extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    @ApiOperation(value="Retrieve request handlers.", response=HandlerSpec.class, responseContainer="List")
    public JSONObject get(String path, Map<String,String> headers)
            throws ServiceException, JSONException {
        return new JsonList(HandlerCache.getAllHandlers(), "handlers").getJson();
    }
}
