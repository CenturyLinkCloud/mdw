package com.centurylink.mdw.zipkin.tests;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import io.swagger.annotations.Api;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.HashMap;
import java.util.Map;

@Path("/fortune")
@Api("Fortune teller service")
public class FortuneService extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static String process = "com.centurylink.mdw.zipkin.tests/FortuneMain.proc";

    @Override
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        logger.info(logtag(headers), "FortuneService >> get()");
        String name = getParameters(headers).get("name");
        if (name == null)
            name = "World";
        String fortuneRequest = "{ \"name\": \"" + name + "\"}";
        Map<String,Object> inputParams = new HashMap<>();
        inputParams.put("originator", this.getClass().getSimpleName());
        return invokeServiceProcess(process, fortuneRequest, masterRequestId(headers), inputParams, headers);
    }

    @Override
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        logger.info(logtag(headers), "FortuneService >> post()");
        Map<String,Object> inputParams = new HashMap<>();
        inputParams.put("originator", this.getClass().getSimpleName());
        launchProcess(process, masterRequestId(headers), inputParams, headers);
        return new StatusResponse(Status.ACCEPTED).getJson();
    }
}
