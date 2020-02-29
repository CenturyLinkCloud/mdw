package com.centurylink.mdw.tests.services;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.rest.JsonRestService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;

@Path("/unparseable")
public class UnparseableService extends JsonRestService {


    @Override
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        throw new ServiceException(ServiceException.INTERNAL_ERROR, "Retry me");
    }
}