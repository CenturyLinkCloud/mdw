package com.centurylink.mdw.tests.services;

import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.services.rest.JsonRestService;

/**
 * Handle HTTP an PATCH request.
 */
@Path("/PatchHandler")
public class PatchHandler extends JsonRestService {

    public JSONObject patch(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String rcHeader =  headers.get("send-response-code");
        if (rcHeader != null)
            throw new ServiceException(Integer.parseInt(rcHeader), "Sending response code " + rcHeader);
        // echo back the request
        return new Workgroup(content).getJson();
    }
}
