package com.centurylink.mdw.tests.services;

import com.centurylink.mdw.services.rest.JsonRestService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;

@Path("/")
public class RootPath extends JsonRestService {

    @Override
    public JSONObject get(String path, Map<String,String> headers) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("impl", getClass().getName());
        return json;
    }
}
